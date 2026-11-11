"""HU-54: registro mínimo, consulta restringida y revisión, exclusivamente por Django."""
import json
import re
import uuid
from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo
from django.db import connection, transaction
from curriculo.metas import filas
from identidad.respuestas import ErrorDePeticion
from operaciones import auditoria
from . import servicio

CATALOGO = 'a0540000-0000-4000-8000-000000000001'
RECURSOS = {'GENERACION_IA', 'TUTOR', 'CUESTIONARIO', 'REVISION_AUTOMATICA', 'RECURSO_JSON'}


def redactar(texto):
    # Protección básica, no anonimización garantizada. Nunca guardar prompts de sistema o claves.
    if not isinstance(texto, str): return None
    texto = re.sub(r'[\w.+-]+@[\w.-]+\.[\w-]+', '[correo oculto]', texto)
    texto = re.sub(r'(?<!\w)\+?\d[\d .()-]{6,}\d(?!\w)', '[número personal oculto]', texto)
    texto = re.sub(r'(?i)((?:contraseña|password|clave|token|direcci[oó]n)\s*[:=]\s*)[^\n,;]+', r'\1[dato oculto]', texto)
    return texto


def cadena(meta, key, maximum):
    value = meta.get(key)
    if value is None: return None
    if not isinstance(value, str) or len(value) > maximum: raise ErrorDePeticion('Metadatos de IA inválidos.')
    return redactar(value)


@transaction.atomic
def registrar(datos):
    texto = datos.get('texto')
    decision = servicio.evaluar_publicada(texto)
    meta = datos.get('auditoria') or {}
    if not isinstance(meta, dict): raise ErrorDePeticion('Metadatos de IA inválidos.')
    origen = meta.get('origen', 'SERVICIO_INTERNO')
    recurso = meta.get('recurso', 'GENERACION_IA')
    if not isinstance(origen,str) or not isinstance(recurso,str) or origen not in {'SPRING', 'SERVICIO_IA', 'SERVICIO_INTERNO'} or recurso not in RECURSOS:
        raise ErrorDePeticion('Origen o recurso desconocido.')
    duracion = meta.get('duracionMs')
    if duracion is not None and (type(duracion) is not int or not 0 <= duracion <= 3600000):
        raise ErrorDePeticion('Duración inválida.')
    proveedor = cadena(meta,'proveedor',80)
    modelo = cadena(meta,'modelo',120)
    pregunta = cadena(meta,'pregunta',4000)
    identifier = str(uuid.uuid4())
    with connection.cursor() as c:
        c.execute("""INSERT INTO ia_interaccion_auditoria
          (id,origen,recurso,proveedor,modelo,duracion_ms,pregunta,respuesta,alternativa,permitida,reglas,version_reglas)
          VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)""",
          [identifier, origen, recurso, proveedor, modelo, duracion,
           pregunta, redactar(texto), redactar(decision.get('alternativa')),
           decision['permitida'], json.dumps(decision['reglas']), decision['version']])
    return {**decision, 'interaccionId': identifier}


def filtros(params):
    conditions = ['1=1']; values = []
    q = params.get('q','').strip()
    if len(q) > 80: raise ErrorDePeticion('Busca con un código de hasta 80 caracteres.')
    if q:
        conditions.append('id LIKE %s'); values.append(q.replace('%','').replace('_','')+'%')
    for key, sql in [('resultado', {'permitida':'permitida=1','bloqueada':'permitida=0'}),
                     ('revision', {'pendiente':'revisada_en IS NULL','revisada':'revisada_en IS NOT NULL'})]:
        value = params.get(key,'')
        if value and value not in sql: raise ErrorDePeticion('Filtro inválido.')
        if value: conditions.append(sql[value])
    dates = {}
    for key, op in [('desde','>='),('hasta','<')]:
        if params.get(key):
            try: day = datetime.strptime(params[key], '%Y-%m-%d')
            except ValueError: raise ErrorDePeticion('Fecha inválida.')
            dates[key] = day
            if key == 'hasta':
                try: day += timedelta(days=1)
                except OverflowError: raise ErrorDePeticion('Fecha fuera de rango.')
            conditions.append('creada_en '+op+' %s')
            values.append(day.replace(tzinfo=ZoneInfo('America/Lima')).astimezone(timezone.utc).replace(tzinfo=None))
    if len(dates)==2 and dates['desde'] > dates['hasta']: raise ErrorDePeticion('La fecha inicial debe ser anterior a la final.')
    try: page = int(params.get('pagina','1'))
    except (ValueError, TypeError): raise ErrorDePeticion('Página inválida.')
    if not 1 <= page <= 100000: raise ErrorDePeticion('Página inválida.')
    return ' AND '.join(conditions), values, page


def serializar(row):
    for key, value in row.items():
        if isinstance(value, datetime): row[key] = value.replace(tzinfo=timezone.utc).isoformat()
    row['permitida'] = bool(row['permitida'])
    if 'reglas' in row: row['reglas'] = servicio.decode(row['reglas'])
    return row


def listar(params, actor):
    where, values, page = filtros(params)
    stats = filas('SELECT COUNT(*) total, COALESCE(SUM(revisada_en IS NULL),0) pendientes, AVG(duracion_ms) promedio FROM ia_interaccion_auditoria WHERE '+where, values)[0]
    items = filas('SELECT id,recurso,origen,permitida,creada_en,revisada_en,duracion_ms FROM ia_interaccion_auditoria WHERE '+where+' ORDER BY creada_en DESC,id DESC LIMIT 20 OFFSET %s', values+[(page-1)*20])
    auditoria.registrar(actor.id,'IA_HISTORIAL_CONSULTADO',CATALOGO,entidad_tipo='INTERACCION_IA',dominio='ia')
    return {'items':[serializar(x) for x in items], 'total':stats['total'], 'pendientes':int(stats['pendientes']),
            'promedio':float(stats['promedio']) if stats['promedio'] is not None else None,'pagina':page,'porPagina':20}


def obtener(identifier, lock=False):
    try: identifier = str(uuid.UUID(str(identifier)))
    except ValueError: raise ErrorDePeticion('Interacción inválida.')
    rows = filas('SELECT * FROM ia_interaccion_auditoria WHERE id=%s'+(' FOR UPDATE' if lock else ''), [identifier])
    if not rows: raise ErrorDePeticion('Interacción no encontrada.',404)
    return rows[0]


def detalle(identifier, actor):
    row = obtener(identifier)
    auditoria.registrar(actor.id,'IA_INTERACCION_CONSULTADA',identifier,entidad_tipo='INTERACCION_IA',dominio='ia')
    row['traza'] = [{'accion':x.accion,'fecha':x.ocurrido_en.isoformat()} for x in auditoria.historial(identifier,50)]
    return serializar(row)


@transaction.atomic
def revisar(identifier, actor):
    row = obtener(identifier, True)
    if row['revisada_en'] is None:
        with connection.cursor() as c:
            c.execute('UPDATE ia_interaccion_auditoria SET revisada_en=UTC_TIMESTAMP(6),revisor_id=%s WHERE id=%s', [actor.id, identifier])
        auditoria.registrar(actor.id,'IA_INTERACCION_REVISADA',identifier,entidad_tipo='INTERACCION_IA',dominio='ia')
    return detalle(identifier,actor)
