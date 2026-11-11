import json
import uuid
from datetime import timezone
from django.db import connection, transaction
from identidad.respuestas import ErrorDePeticion
from operaciones import auditoria
from curriculo.metas import filas
from . import motor

ID = 'a0530000-0000-4000-8000-000000000001'


def decode(value):
    return json.loads(value) if isinstance(value, str) else value


def registro(lock=False):
    rows = filas('SELECT * FROM ia_configuracion_lenguaje WHERE id=%s' + (' FOR UPDATE' if lock else ''), [ID])
    if not rows: raise ErrorDePeticion('Falta inicializar las reglas de lenguaje del servidor.', 503)
    return rows[0]


def consultar():
    r = registro()
    history = filas('''SELECT h.version,h.publicado_en AS fecha,u.nombre_usuario AS autor
        FROM ia_revision_lenguaje h JOIN identidad_cuenta_usuario u ON u.id=h.autor_id
        WHERE h.configuracion_id=%s ORDER BY h.version DESC LIMIT 20''', [ID])
    for h in history:
        if h['fecha'].tzinfo is None: h['fecha'] = h['fecha'].replace(tzinfo=timezone.utc)
    return {'configuracion': decode(r['borrador']), 'revision': r['revision'], 'versionPublicada': r['version_publicada'],
            'probada': r['revision_probada'] == r['revision'], 'reglas': motor.REGLAS,
            'alternativas': motor.ALTERNATIVAS, 'casos': [{'nombre': n, 'texto': t} for n, t, _ in motor.CASOS], 'historial': history}


def comprobar_revision(r, datos):
    if type(datos.get('revision')) is not int or datos['revision'] != r['revision']:
        raise ErrorDePeticion('Otra persona cambió el borrador. Recarga antes de continuar.', 409)


@transaction.atomic
def guardar(datos, actor):
    config = motor.validar(datos.get('configuracion'))
    r = registro(True); comprobar_revision(r, datos)
    with connection.cursor() as c:
        c.execute('UPDATE ia_configuracion_lenguaje SET borrador=%s,revision=revision+1,revision_probada=NULL,actualizado_en=UTC_TIMESTAMP(6) WHERE id=%s', [json.dumps(config), ID])
    auditoria.registrar(actor.id, 'LENGUAJE_BORRADOR_GUARDADO', ID, entidad_tipo='REGLAS_IA', dominio='ia')
    return consultar()


@transaction.atomic
def probar(datos):
    r = registro(True); comprobar_revision(r, datos)
    results = motor.bateria(decode(r['borrador']))
    with connection.cursor() as c:
        c.execute('UPDATE ia_configuracion_lenguaje SET revision_probada=%s WHERE id=%s', [r['revision'] if all(x['correcto'] for x in results) else None, ID])
    return {'resultados': results, 'estado': consultar()}


@transaction.atomic
def publicar(datos, actor):
    r = registro(True); comprobar_revision(r, datos)
    if r['revision_probada'] != r['revision'] or not all(x['correcto'] for x in motor.bateria(decode(r['borrador']))):
        raise ErrorDePeticion('Ejecuta y supera las pruebas del borrador antes de publicarlo.', 409)
    if decode(r['publicada']) == decode(r['borrador']): return consultar()
    version = r['version_publicada'] + 1
    with connection.cursor() as c:
        c.execute('INSERT INTO ia_revision_lenguaje (id,configuracion_id,version,configuracion,autor_id) VALUES (%s,%s,%s,%s,%s)', [str(uuid.uuid4()), ID, version, r['borrador'], actor.id])
        c.execute('UPDATE ia_configuracion_lenguaje SET publicada=borrador,version_publicada=%s WHERE id=%s', [version, ID])
    auditoria.registrar(actor.id, 'LENGUAJE_PUBLICADO', ID, entidad_tipo='REGLAS_IA', dominio='ia')
    return consultar()


def evaluar_publicada(texto):
    r = registro()
    config = decode(r['publicada']) if r['publicada'] else motor.inicial()
    return {**motor.evaluar(texto, config), 'version': r['version_publicada']}
