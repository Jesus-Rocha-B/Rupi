"""HU-52. Django configura el catálogo; nunca modifica las fechas de escuela_*."""
import hashlib
import json
import uuid
from datetime import date, timedelta
from django.db import connection, transaction
from identidad.respuestas import ErrorDePeticion
from operaciones import auditoria
from .catalogo import _id
from .metas import filas


def opciones():
    return {
        'calendarios': filas('''SELECT y.id,y.anio,y.comienza,y.termina,y.estado,i.nombre AS institucion
            FROM escuela_anio_escolar y JOIN escuela_institucion i ON i.id=y.institucion_id
            ORDER BY y.anio DESC,i.nombre'''),
        'grados': filas('SELECT id,nombre FROM curriculo_grado ORDER BY numero_grado'),
        'areas': filas('SELECT id,nombre FROM curriculo_area ORDER BY nombre,id'),
    }


def anio(anio_id, lock=False):
    rows = filas('SELECT * FROM escuela_anio_escolar WHERE id=%s' + (' FOR UPDATE' if lock else ''), [_id(anio_id, 'el año escolar')])
    if not rows:
        raise ErrorDePeticion('No encontramos el calendario escolar.', 404)
    if lock and rows[0]['estado'] == 'CERRADO':
        raise ErrorDePeticion('El año está cerrado. Solo puedes consultar sus periodos.', 409)
    return rows[0]


def consultar(anio_id):
    y = anio(anio_id)
    rows = filas('SELECT id,tipo,periodos,revision FROM curriculo_calendario_periodos WHERE anio_escolar_id=%s', [anio_id])
    data = rows[0] if rows else {'id': None, 'tipo': 'BIMESTRE', 'periodos': [], 'revision': 0}
    if isinstance(data['periodos'], str):
        data['periodos'] = json.loads(data['periodos'])
    data['editable'] = y['estado'] != 'CERRADO'
    return data


def validar(datos, y):
    tipo = datos.get('tipo')
    if tipo not in ('BIMESTRE', 'TRIMESTRE'):
        raise ErrorDePeticion('Selecciona bimestres o trimestres.')
    periods = datos.get('periodos')
    count = 4 if tipo == 'BIMESTRE' else 3
    if not isinstance(periods, list) or len(periods) != count:
        raise ErrorDePeticion(f'Completa los {count} periodos.')
    result, previous = [], None
    for index, p in enumerate(periods):
        try:
            start, end = date.fromisoformat(p['comienza']), date.fromisoformat(p['termina'])
        except (KeyError, TypeError, ValueError):
            raise ErrorDePeticion('Completa fechas válidas para todos los periodos.')
        if start.year != y['anio'] or end.year != y['anio'] or end < start:
            raise ErrorDePeticion('Las fechas deben estar ordenadas y pertenecer al año escolar.')
        if (y['comienza'] and start < y['comienza']) or (y['termina'] and end > y['termina']):
            raise ErrorDePeticion('Los periodos deben estar dentro de las fechas del calendario escolar.')
        if previous and start <= previous:
            raise ErrorDePeticion('Hay cruces de fechas: cada periodo debe comenzar después del anterior.')
        previous = end
        result.append({'nombre': f'{["I", "II", "III", "IV"][index]} {tipo.lower()}', 'comienza': start.isoformat(), 'termina': end.isoformat()})
    return result


@transaction.atomic
def guardar(anio_id, datos, actor):
    y = anio(anio_id, True)  # serializa incluso la primera creación
    current = consultar(anio_id)
    if type(datos.get('revision')) is not int or datos['revision'] != current['revision']:
        raise ErrorDePeticion('Otro administrador cambió los periodos. Recarga antes de guardar.', 409)
    periods = validar(datos, y)
    calendar_id = current['id'] or str(uuid.uuid4())
    with connection.cursor() as c:
        c.execute('''INSERT INTO curriculo_calendario_periodos
            (id,anio_escolar_id,tipo,periodos,revision,actualizado_por) VALUES (%s,%s,%s,%s,1,%s)
            ON DUPLICATE KEY UPDATE tipo=VALUES(tipo),periodos=VALUES(periodos),revision=revision+1,
            actualizado_por=VALUES(actualizado_por),actualizado_en=UTC_TIMESTAMP(6)''',
            [calendar_id, anio_id, datos['tipo'], json.dumps(periods), actor.id])
    auditoria.registrar(actor.id, 'PERIODOS_GUARDADOS', calendar_id, entidad_tipo='CALENDARIO', dominio='curriculo')
    return consultar(anio_id)


def filtros(datos):
    grado = _id(datos['grado'], 'el grado') if datos.get('grado') else None
    areas = datos.get('areas')
    if not isinstance(areas, list) or not 1 <= len(areas) <= 100:
        raise ErrorDePeticion('Selecciona al menos una materia.')
    return grado, sorted(set(_id(a, 'la materia') for a in areas))


def periodo_para(inicio, fin, periods):
    # La planificación está en UTC; los periodos usan días escolares de Perú (UTC-5).
    start = (inicio - timedelta(hours=5)).date().isoformat()
    end = (fin - timedelta(hours=5)).date().isoformat()
    return next((p for p in periods if p['comienza'] <= start <= end <= p['termina']), None)


def vista_previa(anio_id, datos, lock=False):
    calendar = consultar(anio_id)
    if not calendar['id']:
        raise ErrorDePeticion('Guarda los periodos antes de revisar la planificación.', 409)
    grado, areas = filtros(datos)
    params = [anio_id, *areas]
    where = ' AND a.grado_id=%s' if grado else ''
    if grado:
        params.append(grado)
    rows = filas('''SELECT p.id,p.titulo,p.inicia_en,p.termina_en,a.nombre AS aula,
        r.titulo AS ruta FROM escuela_planificacion_ruta p JOIN escuela_aula a ON a.id=p.aula_id
        JOIN aprendizaje_version_ruta v ON v.id=p.version_ruta_id
        JOIN aprendizaje_ruta r ON r.id=v.ruta_id
        WHERE a.anio_escolar_id=%s AND a.activa=TRUE AND r.archivado_en IS NULL
        AND v.area_id IN (''' + ','.join(['%s'] * len(areas)) + ')' + where +
        ' ORDER BY p.id' + (' FOR UPDATE' if lock else ''), params)
    result = []
    for row in rows:
        p = periodo_para(row['inicia_en'], row['termina_en'], calendar['periodos'])
        result.append({**row, 'periodo': p})
    signature = json.dumps([calendar['revision'], grado, areas, result], default=str, sort_keys=True)
    return {'revision': calendar['revision'], 'token': hashlib.sha256(signature.encode()).hexdigest(),
            'planificaciones': result, 'aplicables': sum(r['periodo'] is not None for r in result)}


@transaction.atomic
def aplicar(anio_id, datos, actor):
    anio(anio_id, True)
    preview = vista_previa(anio_id, datos, True)
    if not isinstance(datos.get('token'), str) or datos['token'] != preview['token']:
        raise ErrorDePeticion('Cambió la planificación o sus periodos. Revisa la vista previa otra vez.', 409)
    if not preview['aplicables']:
        raise ErrorDePeticion('No hay planificaciones dentro de estos periodos.', 409)
    calendar = consultar(anio_id)
    with connection.cursor() as c:
        for row in preview['planificaciones']:
            p = row['periodo']
            if not p:
                continue
            c.execute('''INSERT INTO curriculo_periodo_planificacion
                (id,planificacion_id,calendario_id,revision,nombre,comienza,termina,aplicado_por)
                VALUES (%s,%s,%s,%s,%s,%s,%s,%s) ON DUPLICATE KEY UPDATE
                calendario_id=VALUES(calendario_id),revision=VALUES(revision),nombre=VALUES(nombre),
                comienza=VALUES(comienza),termina=VALUES(termina),aplicado_por=VALUES(aplicado_por),aplicado_en=UTC_TIMESTAMP(6)''',
                [str(uuid.uuid4()), row['id'], calendar['id'], calendar['revision'], p['nombre'], p['comienza'], p['termina'], actor.id])
    auditoria.registrar(actor.id, 'PERIODOS_APLICADOS', calendar['id'], entidad_tipo='CALENDARIO', dominio='curriculo')
    return {'aplicadas': preview['aplicables'], 'omitidas': len(preview['planificaciones']) - preview['aplicables']}
