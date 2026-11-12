"""HU-51: actualizar la correspondencia curricular ante una resolución ministerial.

Flujo: el administrador registra la resolución y se crea una versión BORRADOR copiada de la vigente, con una
fila de correspondencia por cada área, competencia, capacidad, unidad, meta y estándar. En el borrador corrige
títulos de unidades, agrega o renombra áreas y competencias en «Grados y competencias» (HU-49) y marca lo que
la resolución retira. Al publicar, lo retirado se quita del borrador, la versión nueva pasa a ACTIVO y la
anterior a RETIRADO. Las rutas de Spring siguen apuntando a la versión anterior hasta que su docente las
actualiza con esta correspondencia.
"""
import re
import uuid
from datetime import date, timezone

from django.db import IntegrityError, connection, transaction

from identidad.respuestas import ErrorDePeticion
from operaciones import auditoria

from .catalogo import DOMINIO, _id, _texto

_NUMERO = re.compile(r'^[A-ZÁÉÍÓÚÑ0-9][A-ZÁÉÍÓÚÑ0-9 .°º/-]{4,79}$')
_ETIQUETA = re.compile(r'^[A-Z][A-Z0-9-]{2,59}$')
_ENLACE = re.compile(r'^https://[^\s<>"]{4,490}$')
# Tipos que el administrador corrige en esta pantalla. Áreas y competencias se agregan o renombran en HU-49.
EDITABLES = {'COMPETENCIA', 'UNIDAD'}
ORDEN_TIPOS = ('AREA', 'COMPETENCIA', 'UNIDAD')
# Nombre visible de cada elemento según su tabla.
NOMBRES = {
    'AREA': ('curriculo_area', 'nombre', 'codigo'),
    'COMPETENCIA': ('curriculo_competencia', 'nombre', 'codigo'),
    'CAPACIDAD': ('curriculo_capacidad', 'nombre', 'orden'),
    'UNIDAD': ('curriculo_unidad', 'titulo', 'codigo'),
    'META': ('curriculo_meta_aprendizaje', 'descripcion', 'codigo'),
    'ESTANDAR': ('curriculo_estandar', 'descripcion', 'codigo'),
}


def _filas(sql, params=()):
    with connection.cursor() as cursor:
        cursor.execute(sql, params)
        return [dict(zip([c[0] for c in cursor.description], row)) for row in cursor.fetchall()]


def _ejecutar(sql, params=()):
    with connection.cursor() as cursor:
        cursor.execute(sql, params)
        return cursor.rowcount


def _varios(sql, filas):
    if filas:
        with connection.cursor() as cursor:
            cursor.executemany(sql, filas)


def _utc(valor):
    if valor is not None and getattr(valor, 'tzinfo', None) is None:
        return valor.replace(tzinfo=timezone.utc)
    return valor


def _fecha(datos, campo, etiqueta):
    valor = datos.get(campo)
    if not isinstance(valor, str):
        raise ErrorDePeticion(f'Falta {etiqueta}.')
    try:
        return date.fromisoformat(valor)
    except ValueError:
        raise ErrorDePeticion(f'{etiqueta.capitalize()} no es una fecha válida.')


def validar_resolucion(datos, hoy=None, creando=True):
    """Valida los datos de la resolución. Al crear también pide la etiqueta y el inicio de vigencia."""
    permitidos = {'numero', 'fechaEmision', 'resumen', 'enlace'} | ({'etiqueta', 'vigenteDesde'} if creando else set())
    if set(datos) - permitidos:
        raise ErrorDePeticion('La petición trae campos que no corresponden a una resolución.')
    numero = _texto(datos, 'numero', 5, 80, 'el número de la resolución').upper()
    if not _NUMERO.match(numero):
        raise ErrorDePeticion('Escribe el número como figura en la norma (ej. RM 587-2026-MINEDU).')
    emision = _fecha(datos, 'fechaEmision', 'la fecha de emisión')
    if emision > (hoy or date.today()):
        raise ErrorDePeticion('La fecha de emisión no puede ser futura.')
    resumen = _texto(datos, 'resumen', 10, 600, 'el resumen de los cambios')
    enlace = _texto(datos, 'enlace', 12, 500, 'el enlace oficial', obligatorio=False)
    if enlace and not _ENLACE.match(enlace):
        raise ErrorDePeticion('El enlace oficial debe empezar con https:// (ej. el PDF publicado en gob.pe).')
    limpio = {'numero': numero, 'fecha_emision': emision, 'resumen': resumen, 'enlace': enlace}
    if creando:
        etiqueta = _texto(datos, 'etiqueta', 3, 60, 'el nombre de la nueva versión').upper().replace(' ', '-')
        if not _ETIQUETA.match(etiqueta):
            raise ErrorDePeticion('El nombre de la versión lleva mayúsculas, números y guiones (ej. CNEB-2027).')
        vigente = _fecha(datos, 'vigenteDesde', 'la fecha de inicio de vigencia')
        if vigente < emision:
            raise ErrorDePeticion('La vigencia no puede empezar antes de la emisión de la resolución.')
        limpio.update(etiqueta=etiqueta, vigente_desde=vigente)
    return limpio


def cambio_efectivo(fila):
    """RETIRADA si se marcó así; MODIFICADA si se marcó o si el nombre cambió en el borrador."""
    if fila['cambio'] == 'RETIRADA':
        return 'RETIRADA'
    if fila['cambio'] == 'MODIFICADA' or (fila.get('destino') is not None and fila['destino'] != fila['origen']):
        return 'MODIFICADA'
    return 'SE_MANTIENE'


# ----------------------------------------------------------------------------------------------- consultas

def _resolucion(resolucion_id, bloquear=False):
    found = _filas('''SELECT r.*, o.etiqueta_version AS origen_etiqueta, o.estado AS origen_estado,
        d.etiqueta_version AS destino_etiqueta, d.estado AS destino_estado, d.vigente_desde
        FROM curriculo_resolucion r
        JOIN curriculo_version_catalogo o ON o.id=r.version_origen_id
        JOIN curriculo_version_catalogo d ON d.id=r.version_destino_id
        WHERE r.id=%s''' + (' FOR UPDATE' if bloquear else ''), [_id(resolucion_id, 'la resolución')])
    if not found:
        raise ErrorDePeticion('No encontramos esa resolución.', 404)
    return found[0]


def _json_resolucion(r):
    return {
        'id': r['id'], 'numero': r['numero'], 'fechaEmision': r['fecha_emision'].isoformat(),
        'resumen': r['resumen'], 'enlace': r['enlace_oficial'],
        'origen': {'id': r['version_origen_id'], 'etiqueta': r['origen_etiqueta'], 'estado': r['origen_estado']},
        'destino': {'id': r['version_destino_id'], 'etiqueta': r['destino_etiqueta'], 'estado': r['destino_estado'],
                    'vigenteDesde': r['vigente_desde'].isoformat() if r['vigente_desde'] else None},
        'estado': 'PUBLICADA' if r['publicado_en'] else 'BORRADOR',
        'creadoEn': _utc(r['creado_en']), 'publicadoEn': _utc(r['publicado_en']),
    }


def listar():
    filas = _filas('''SELECT r.*, o.etiqueta_version AS origen_etiqueta, o.estado AS origen_estado,
        d.etiqueta_version AS destino_etiqueta, d.estado AS destino_estado, d.vigente_desde
        FROM curriculo_resolucion r
        JOIN curriculo_version_catalogo o ON o.id=r.version_origen_id
        JOIN curriculo_version_catalogo d ON d.id=r.version_destino_id
        ORDER BY r.creado_en DESC, r.id''')
    vigente = _filas("SELECT id, etiqueta_version AS etiqueta, vigente_desde FROM curriculo_version_catalogo "
                     "WHERE estado='ACTIVO' ORDER BY vigente_desde DESC LIMIT 1")
    abierta = any(f['publicado_en'] is None for f in filas)
    return {
        'resoluciones': [_json_resolucion(f) for f in filas],
        'vigente': ({'id': vigente[0]['id'], 'etiqueta': vigente[0]['etiqueta'],
                     'vigenteDesde': vigente[0]['vigente_desde'].isoformat() if vigente[0]['vigente_desde'] else None}
                    if vigente else None),
        'puedeRegistrar': bool(vigente) and not abierta,
    }


def _elementos(r):
    """Correspondencia de áreas, competencias y unidades, más lo NUEVO agregado en el borrador."""
    grupos = {}
    for tipo in ORDEN_TIPOS:
        tabla, campo, codigo = NOMBRES[tipo]
        extra = ", go.nombre AS grado" if tipo == 'UNIDAD' else ", NULL AS grado"
        join_grado = " LEFT JOIN curriculo_grado go ON go.id=o.grado_id" if tipo == 'UNIDAD' else ""
        filas = _filas(f'''SELECT c.id, c.origen_id, c.destino_id, c.cambio, c.nota,
            o.{codigo} AS codigo, o.{campo} AS origen, d.{campo} AS destino {extra}
            FROM curriculo_correspondencia c
            JOIN {tabla} o ON o.id=c.origen_id
            LEFT JOIN {tabla} d ON d.id=c.destino_id{join_grado}
            WHERE c.resolucion_id=%s AND c.tipo=%s ORDER BY o.{codigo}''', [r['id'], tipo])
        version = 'version_catalogo_id' if tipo != 'COMPETENCIA' else None
        if version:
            filtro = f'n.{version}=%s'
        else:
            filtro = 'n.area_id IN (SELECT id FROM curriculo_area WHERE version_catalogo_id=%s)'
        extra_n = ", gn.nombre AS grado" if tipo == 'UNIDAD' else ", NULL AS grado"
        join_n = " LEFT JOIN curriculo_grado gn ON gn.id=n.grado_id" if tipo == 'UNIDAD' else ""
        nuevos = _filas(f'''SELECT n.id, n.{codigo} AS codigo, n.{campo} AS destino {extra_n}
            FROM {tabla} n{join_n}
            WHERE {filtro} AND NOT EXISTS (SELECT 1 FROM curriculo_correspondencia c
              WHERE c.resolucion_id=%s AND c.tipo=%s AND c.destino_id=n.id) ORDER BY n.{codigo}''',
            [r['version_destino_id'], r['id'], tipo])
        grupos[tipo] = [{
            'id': f['id'], 'codigo': str(f['codigo']), 'grado': f['grado'], 'origen': f['origen'],
            'destino': f['destino'], 'cambio': cambio_efectivo(f), 'nota': f['nota'],
            'editable': tipo in EDITABLES,
        } for f in filas] + [{
            'id': None, 'codigo': str(n['codigo']), 'grado': n['grado'], 'origen': None, 'destino': n['destino'],
            'cambio': 'NUEVA', 'nota': None, 'editable': False,
        } for n in nuevos]
    return grupos


def _impacto(r):
    """Rutas de Spring que siguen en la versión de origen y paradas cuya unidad se retira."""
    rutas = _filas('''SELECT v.estado, COUNT(*) AS total FROM aprendizaje_version_ruta v
        WHERE v.version_catalogo_id=%s GROUP BY v.estado''', [r['version_origen_id']])
    paradas = _filas('''SELECT COUNT(*) AS total FROM aprendizaje_nodo_ruta n
        JOIN curriculo_correspondencia c ON c.origen_id=n.unidad_id AND c.tipo='UNIDAD' AND c.resolucion_id=%s
        WHERE c.cambio='RETIRADA' ''', [r['id']])
    por_estado = {f['estado']: int(f['total']) for f in rutas}
    return {'rutas': sum(por_estado.values()), 'rutasPublicadas': por_estado.get('PUBLICADO', 0),
            'paradasSinUnidad': int(paradas[0]['total']) if paradas else 0}


def detalle(resolucion_id):
    r = _resolucion(resolucion_id)
    grupos = _elementos(r)
    resumen = {}
    for tipo, elementos in grupos.items():
        cuenta = {'SE_MANTIENE': 0, 'MODIFICADA': 0, 'RETIRADA': 0, 'NUEVA': 0}
        for e in elementos:
            cuenta[e['cambio']] += 1
        resumen[tipo] = cuenta
    return {'resolucion': _json_resolucion(r), 'elementos': grupos, 'resumen': resumen,
            'impacto': _impacto(r), 'editable': r['publicado_en'] is None}


# ----------------------------------------------------------------------------------------------- escritura

def _copiar_version(origen_id, destino_id, resolucion_id, actor_id):
    """Copia la versión vigente al borrador y registra la correspondencia de cada elemento."""
    mapa = {}
    corr = []

    def nuevo(tipo, viejo):
        mapa[(tipo, viejo)] = str(uuid.uuid4())
        corr.append((str(uuid.uuid4()), resolucion_id, tipo, viejo, mapa[(tipo, viejo)], actor_id))
        return mapa[(tipo, viejo)]

    areas = _filas('SELECT * FROM curriculo_area WHERE version_catalogo_id=%s', [origen_id])
    _varios('INSERT INTO curriculo_area (id,version_catalogo_id,codigo,nombre,orden) VALUES (%s,%s,%s,%s,%s)',
            [(nuevo('AREA', a['id']), destino_id, a['codigo'], a['nombre'], a['orden']) for a in areas])
    competencias = _filas('''SELECT c.* FROM curriculo_competencia c JOIN curriculo_area a ON a.id=c.area_id
        WHERE a.version_catalogo_id=%s''', [origen_id])
    _varios('''INSERT INTO curriculo_competencia (id,area_id,codigo,numero_cneb,nombre,descripcion)
        VALUES (%s,%s,%s,%s,%s,%s)''',
        [(nuevo('COMPETENCIA', c['id']), mapa[('AREA', c['area_id'])], c['codigo'], c['numero_cneb'], c['nombre'],
          c['descripcion']) for c in competencias])
    capacidades = _filas('''SELECT k.* FROM curriculo_capacidad k JOIN curriculo_competencia c ON c.id=k.competencia_id
        JOIN curriculo_area a ON a.id=c.area_id WHERE a.version_catalogo_id=%s''', [origen_id])
    _varios('INSERT INTO curriculo_capacidad (id,competencia_id,orden,nombre) VALUES (%s,%s,%s,%s)',
            [(nuevo('CAPACIDAD', k['id']), mapa[('COMPETENCIA', k['competencia_id'])], k['orden'], k['nombre'])
             for k in capacidades])
    unidades = _filas('SELECT * FROM curriculo_unidad WHERE version_catalogo_id=%s', [origen_id])
    _varios('''INSERT INTO curriculo_unidad (id,version_catalogo_id,grado_id,area_id,codigo,titulo,numero_secuencia)
        VALUES (%s,%s,%s,%s,%s,%s,%s)''',
        [(nuevo('UNIDAD', u['id']), destino_id, u['grado_id'], mapa[('AREA', u['area_id'])], u['codigo'], u['titulo'],
          u['numero_secuencia']) for u in unidades])
    enlaces = _filas('''SELECT uc.* FROM curriculo_unidad_competencia uc JOIN curriculo_unidad u ON u.id=uc.unidad_id
        WHERE u.version_catalogo_id=%s''', [origen_id])
    _varios('INSERT INTO curriculo_unidad_competencia (unidad_id,competencia_id) VALUES (%s,%s)',
            [(mapa[('UNIDAD', e['unidad_id'])], mapa[('COMPETENCIA', e['competencia_id'])]) for e in enlaces
             if ('COMPETENCIA', e['competencia_id']) in mapa])
    for tipo, tabla, enlace, campo in (('META', 'curriculo_meta_aprendizaje', 'curriculo_unidad_meta', 'meta_id'),
                                       ('ESTANDAR', 'curriculo_estandar', 'curriculo_unidad_estandar', 'estandar_id')):
        registros = _filas(f'''SELECT r.* FROM {tabla} r JOIN curriculo_competencia c ON c.id=r.competencia_id
            JOIN curriculo_area a ON a.id=c.area_id WHERE a.version_catalogo_id=%s''', [origen_id])
        _varios(f'INSERT INTO {tabla} (id,competencia_id,grado_id,codigo,descripcion) VALUES (%s,%s,%s,%s,%s)',
                [(nuevo(tipo, x['id']), mapa[('COMPETENCIA', x['competencia_id'])], x['grado_id'], x['codigo'],
                  x['descripcion']) for x in registros])
        vinculos = _filas(f'''SELECT l.* FROM {enlace} l JOIN curriculo_unidad u ON u.id=l.unidad_id
            WHERE u.version_catalogo_id=%s''', [origen_id])
        _varios(f'INSERT INTO {enlace} (unidad_id,{campo}) VALUES (%s,%s)',
                [(mapa[('UNIDAD', v['unidad_id'])], mapa[(tipo, v[campo])]) for v in vinculos
                 if (tipo, v[campo]) in mapa])
    _varios('''INSERT INTO curriculo_correspondencia (id,resolucion_id,tipo,origen_id,destino_id,actualizado_por)
        VALUES (%s,%s,%s,%s,%s,%s)''', corr)
    return {tipo: sum(1 for (t, _) in mapa if t == tipo) for tipo in NOMBRES}


@transaction.atomic
def registrar(datos, actor, hoy=None):
    d = validar_resolucion(datos, hoy)
    vigentes = _filas("SELECT * FROM curriculo_version_catalogo WHERE estado='ACTIVO' FOR UPDATE")
    if len(vigentes) != 1:
        raise ErrorDePeticion('Debe haber exactamente una versión vigente del catálogo para copiarla.', 409)
    if _filas('SELECT id FROM curriculo_resolucion WHERE publicado_en IS NULL FOR UPDATE'):
        raise ErrorDePeticion('Ya hay una resolución en borrador. Publícala antes de registrar otra.', 409)
    origen = vigentes[0]
    if d['vigente_desde'] <= (origen['vigente_desde'] or date.min):
        raise ErrorDePeticion(f'La nueva vigencia debe empezar después de {origen["etiqueta_version"]}.')
    destino_id, resolucion_id = str(uuid.uuid4()), str(uuid.uuid4())
    try:
        with transaction.atomic():
            _ejecutar('''INSERT INTO curriculo_version_catalogo (id,etiqueta_version,referencia_origen,vigente_desde,estado)
                VALUES (%s,%s,%s,%s,'BORRADOR')''',
                [destino_id, d['etiqueta'], f"{d['numero']}: {d['resumen']}"[:255], d['vigente_desde']])
            _ejecutar('''INSERT INTO curriculo_resolucion
                (id,numero,fecha_emision,resumen,enlace_oficial,version_origen_id,version_destino_id,creado_por)
                VALUES (%s,%s,%s,%s,%s,%s,%s,%s)''',
                [resolucion_id, d['numero'], d['fecha_emision'], d['resumen'], d['enlace'], origen['id'], destino_id,
                 actor.id])
    except IntegrityError as error:
        if error.args and error.args[0] == 1062:
            raise ErrorDePeticion('Ya existe una resolución con ese número o una versión con ese nombre.', 409)
        raise
    _copiar_version(origen['id'], destino_id, resolucion_id, actor.id)
    auditoria.registrar(actor.id, 'RESOLUCION_REGISTRADA', resolucion_id, entidad_tipo='CURRICULO_RESOLUCION',
                        dominio=DOMINIO)
    return detalle(resolucion_id)


@transaction.atomic
def corregir_resolucion(resolucion_id, datos, actor, hoy=None):
    r = _resolucion(resolucion_id, bloquear=True)
    if r['publicado_en']:
        raise ErrorDePeticion('La resolución ya se publicó: queda como registro.', 409)
    d = validar_resolucion(datos, hoy, creando=False)
    try:
        _ejecutar('''UPDATE curriculo_resolucion SET numero=%s,fecha_emision=%s,resumen=%s,enlace_oficial=%s
            WHERE id=%s''', [d['numero'], d['fecha_emision'], d['resumen'], d['enlace'], r['id']])
    except IntegrityError as error:
        if error.args and error.args[0] == 1062:
            raise ErrorDePeticion('Ya existe una resolución con ese número.', 409)
        raise
    _ejecutar('UPDATE curriculo_version_catalogo SET referencia_origen=%s WHERE id=%s',
              [f"{d['numero']}: {d['resumen']}"[:255], r['version_destino_id']])
    auditoria.registrar(actor.id, 'RESOLUCION_CORREGIDA', r['id'], entidad_tipo='CURRICULO_RESOLUCION', dominio=DOMINIO)
    return detalle(r['id'])


def validar_correspondencia(datos, tipo):
    permitidos = {'accion', 'titulo', 'nota'}
    if set(datos) - permitidos:
        raise ErrorDePeticion('Solo se puede mantener, corregir el título o retirar el elemento.')
    accion = datos.get('accion')
    if accion not in ('MANTENER', 'CORREGIR', 'RETIRAR'):
        raise ErrorDePeticion('Elige mantener, corregir o retirar.')
    if accion == 'CORREGIR' and tipo != 'UNIDAD':
        raise ErrorDePeticion('Las competencias se renombran en «Grados y competencias» del borrador.')
    titulo = _texto(datos, 'titulo', 3, 180, 'el nuevo título') if accion == 'CORREGIR' else None
    nota = _texto(datos, 'nota', 5, 500, 'la nota con el sustento de la resolución', obligatorio=accion != 'MANTENER')
    return accion, titulo, nota


@transaction.atomic
def corregir_correspondencia(resolucion_id, correspondencia_id, datos, actor):
    r = _resolucion(resolucion_id, bloquear=True)
    if r['publicado_en'] or r['destino_estado'] != 'BORRADOR':
        raise ErrorDePeticion('La resolución ya se publicó: su correspondencia queda como registro.', 409)
    found = _filas('''SELECT * FROM curriculo_correspondencia WHERE id=%s AND resolucion_id=%s FOR UPDATE''',
                   [_id(correspondencia_id, 'el elemento'), r['id']])
    if not found:
        raise ErrorDePeticion('Ese elemento no pertenece a la resolución.', 404)
    fila = found[0]
    if fila['tipo'] not in EDITABLES:
        raise ErrorDePeticion('Este elemento sigue a su competencia o se corrige en «Grados y competencias».')
    accion, titulo, nota = validar_correspondencia(datos, fila['tipo'])
    if accion == 'CORREGIR':
        _ejecutar('UPDATE curriculo_unidad SET titulo=%s WHERE id=%s', [titulo, fila['destino_id']])
    cambio = {'MANTENER': 'SE_MANTIENE', 'CORREGIR': 'MODIFICADA', 'RETIRAR': 'RETIRADA'}[accion]
    if accion == 'MANTENER' and fila['tipo'] == 'UNIDAD':
        original = _filas('SELECT titulo FROM curriculo_unidad WHERE id=%s', [fila['origen_id']])
        _ejecutar('UPDATE curriculo_unidad SET titulo=%s WHERE id=%s', [original[0]['titulo'], fila['destino_id']])
    _ejecutar('''UPDATE curriculo_correspondencia SET cambio=%s, nota=%s, actualizado_por=%s,
        actualizado_en=UTC_TIMESTAMP(6) WHERE id=%s''', [cambio, nota, actor.id, fila['id']])
    auditoria.registrar(actor.id, 'CORRESPONDENCIA_' + cambio, fila['id'], entidad_tipo='CURRICULO_' + fila['tipo'],
                        dominio=DOMINIO)
    return detalle(r['id'])


def _quitar_retirados(r):
    """Quita del borrador lo que la resolución retira. Solo toca filas de la versión BORRADOR."""
    destino = r['version_destino_id']
    competencias = [f['destino_id'] for f in _filas('''SELECT c.destino_id FROM curriculo_correspondencia c
        JOIN curriculo_competencia k ON k.id=c.destino_id JOIN curriculo_area a ON a.id=k.area_id
        WHERE c.resolucion_id=%s AND c.tipo='COMPETENCIA' AND c.cambio='RETIRADA' AND a.version_catalogo_id=%s''',
        [r['id'], destino])]
    unidades = [f['destino_id'] for f in _filas('''SELECT c.destino_id FROM curriculo_correspondencia c
        JOIN curriculo_unidad u ON u.id=c.destino_id
        WHERE c.resolucion_id=%s AND c.tipo='UNIDAD' AND c.cambio='RETIRADA' AND u.version_catalogo_id=%s''',
        [r['id'], destino])]
    for competencia in competencias:
        hijos = []
        for tipo, tabla, enlace, campo in (('META', 'curriculo_meta_aprendizaje', 'curriculo_unidad_meta', 'meta_id'),
                                           ('ESTANDAR', 'curriculo_estandar', 'curriculo_unidad_estandar', 'estandar_id')):
            ids = [f['id'] for f in _filas(f'SELECT id FROM {tabla} WHERE competencia_id=%s', [competencia])]
            for registro in ids:
                _ejecutar(f'DELETE FROM {enlace} WHERE {campo}=%s', [registro])
                _ejecutar(f'DELETE FROM {tabla} WHERE id=%s', [registro])
            hijos += [(tipo, i) for i in ids]
        capacidades = [f['id'] for f in _filas('SELECT id FROM curriculo_capacidad WHERE competencia_id=%s', [competencia])]
        _ejecutar('DELETE FROM curriculo_capacidad WHERE competencia_id=%s', [competencia])
        hijos += [('CAPACIDAD', i) for i in capacidades]
        _ejecutar('DELETE FROM curriculo_unidad_competencia WHERE competencia_id=%s', [competencia])
        _ejecutar('DELETE FROM curriculo_competencia WHERE id=%s', [competencia])
        for tipo, hijo in hijos:
            _ejecutar('''UPDATE curriculo_correspondencia SET cambio='RETIRADA', destino_id=NULL
                WHERE resolucion_id=%s AND tipo=%s AND destino_id=%s''', [r['id'], tipo, hijo])
    for unidad in unidades:
        for enlace in ('curriculo_unidad_competencia', 'curriculo_unidad_meta', 'curriculo_unidad_estandar'):
            _ejecutar(f'DELETE FROM {enlace} WHERE unidad_id=%s', [unidad])
        _ejecutar('DELETE FROM curriculo_unidad WHERE id=%s', [unidad])
    _ejecutar('''UPDATE curriculo_correspondencia SET destino_id=NULL
        WHERE resolucion_id=%s AND cambio='RETIRADA' ''', [r['id']])


def _fijar_modificadas(r):
    """Guarda como MODIFICADA lo que cambió de nombre en el borrador, para que Spring lo lea sin recalcular."""
    for tipo in ('AREA', 'COMPETENCIA', 'UNIDAD'):
        tabla, campo, _ = NOMBRES[tipo]
        _ejecutar(f'''UPDATE curriculo_correspondencia c JOIN {tabla} o ON o.id=c.origen_id
            JOIN {tabla} d ON d.id=c.destino_id SET c.cambio='MODIFICADA'
            WHERE c.resolucion_id=%s AND c.tipo=%s AND c.cambio='SE_MANTIENE' AND o.{campo}<>d.{campo}''',
            [r['id'], tipo])


@transaction.atomic
def publicar(resolucion_id, datos, actor):
    r = _resolucion(resolucion_id, bloquear=True)
    if r['publicado_en']:
        raise ErrorDePeticion('Esta resolución ya se publicó.', 409)
    if set(datos) - {'confirmacion'} or datos.get('confirmacion') != r['numero']:
        raise ErrorDePeticion(f'Para publicar, escribe el número exacto de la resolución: {r["numero"]}.')
    versiones = {v['id']: v for v in _filas('SELECT * FROM curriculo_version_catalogo WHERE id IN (%s,%s) FOR UPDATE',
                                            [r['version_origen_id'], r['version_destino_id']])}
    if versiones[r['version_origen_id']]['estado'] != 'ACTIVO' or versiones[r['version_destino_id']]['estado'] != 'BORRADOR':
        raise ErrorDePeticion('Las versiones cambiaron desde que se registró la resolución. Recarga la página.', 409)
    if not _filas('''SELECT u.id FROM curriculo_unidad u WHERE u.version_catalogo_id=%s AND NOT EXISTS (
        SELECT 1 FROM curriculo_correspondencia c WHERE c.resolucion_id=%s AND c.tipo='UNIDAD'
        AND c.destino_id=u.id AND c.cambio='RETIRADA') LIMIT 1''', [r['version_destino_id'], r['id']]):
        raise ErrorDePeticion('La nueva versión se quedaría sin unidades. Revisa lo que marcaste como retirado.')
    _fijar_modificadas(r)
    _quitar_retirados(r)
    _ejecutar("UPDATE curriculo_version_catalogo SET estado='RETIRADO' WHERE id=%s", [r['version_origen_id']])
    _ejecutar("UPDATE curriculo_version_catalogo SET estado='ACTIVO' WHERE id=%s", [r['version_destino_id']])
    _ejecutar('UPDATE curriculo_resolucion SET publicado_por=%s, publicado_en=UTC_TIMESTAMP(6) WHERE id=%s',
              [actor.id, r['id']])
    auditoria.registrar(actor.id, 'RESOLUCION_PUBLICADA', r['id'], entidad_tipo='CURRICULO_RESOLUCION',
                        dominio=DOMINIO)
    return detalle(r['id'])
