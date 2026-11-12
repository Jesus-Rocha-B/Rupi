"""HU-50: registros curriculares por unidad; las correcciones nunca cambian vínculos."""
import hashlib
import re
import uuid
from datetime import timezone
from django.db import connection, transaction, IntegrityError
from identidad.respuestas import ErrorDePeticion
from operaciones import auditoria
from .catalogo import _id, _texto

TABLAS = {'META': ('curriculo_meta_aprendizaje', 'curriculo_unidad_meta', 'meta_id'),
          'ESTANDAR': ('curriculo_estandar', 'curriculo_unidad_estandar', 'estandar_id')}


def filas(sql, params=()):
    with connection.cursor() as cursor:
        cursor.execute(sql, params)
        return [dict(zip([c[0] for c in cursor.description], row)) for row in cursor.fetchall()]


def revision(texto):
    return hashlib.sha256(texto.encode('utf-8')).hexdigest()


def tipo_valido(tipo):
    if not isinstance(tipo, str) or tipo not in TABLAS:
        raise ErrorDePeticion('El tipo debe ser META o ESTANDAR.')
    return TABLAS[tipo]


def unidades(version):
    return filas('''SELECT u.id,u.codigo,u.titulo,u.grado_id AS gradoId,u.area_id AS areaId,
        g.nombre AS grado,a.nombre AS area,v.estado
        FROM curriculo_unidad u JOIN curriculo_grado g ON g.id=u.grado_id
        JOIN curriculo_area a ON a.id=u.area_id
        JOIN curriculo_version_catalogo v ON v.id=u.version_catalogo_id
        WHERE u.version_catalogo_id=%s ORDER BY g.numero_grado,a.nombre,u.numero_secuencia,u.titulo''',
        [_id(version, 'la versión del catálogo')])


def unidad(unidad_id, escritura=False):
    found = filas('''SELECT u.*, v.estado FROM curriculo_unidad u
        JOIN curriculo_version_catalogo v ON v.id=u.version_catalogo_id WHERE u.id=%s'''
        + (' FOR UPDATE' if escritura else ''), [_id(unidad_id, 'la unidad')])
    if not found:
        raise ErrorDePeticion('No encontramos esa unidad.', 404)
    if escritura and found[0]['estado'] == 'RETIRADO':
        raise ErrorDePeticion('El catálogo está retirado. Solo puedes consultarlo.', 409)
    return found[0]


def consultar(unidad_id):
    u = unidad(unidad_id)
    competencias = filas('''SELECT c.id,c.codigo,c.nombre FROM curriculo_competencia c
        JOIN curriculo_unidad_competencia uc ON uc.competencia_id=c.id
        WHERE uc.unidad_id=%s AND c.area_id=%s ORDER BY c.codigo''', [unidad_id, u['area_id']])
    registros = []
    for tipo, (tabla, enlace, campo) in TABLAS.items():
        rows = filas(f'''SELECT r.id,r.codigo,r.descripcion,r.competencia_id AS competenciaId,
            c.nombre AS competencia,(SELECT COUNT(*) FROM {enlace} x WHERE x.{campo}=r.id) AS unidadesVinculadas
            FROM {tabla} r JOIN {enlace} l ON l.{campo}=r.id
            JOIN curriculo_competencia c ON c.id=r.competencia_id WHERE l.unidad_id=%s ORDER BY r.codigo''', [unidad_id])
        for row in rows:
            row.update(tipo=tipo, revision=revision(row['descripcion']))
            row['historial'] = filas('''SELECT h.descripcion_anterior AS anterior,h.descripcion_nueva AS nueva,
                h.motivo,h.creado_en AS fecha,COALESCE(a.nombre_usuario,'Administrador') AS autor
                FROM curriculo_registro_revision h LEFT JOIN identidad_cuenta_usuario a ON a.id=h.actor_id
                WHERE h.registro_id=%s AND h.tipo=%s ORDER BY h.creado_en DESC,h.id DESC LIMIT 20''', [row['id'], tipo])
            for event in row['historial']:
                if event.get('fecha') and event['fecha'].tzinfo is None:
                    event['fecha'] = event['fecha'].replace(tzinfo=timezone.utc)
        registros.extend(rows)
    return {'competencias': competencias, 'registros': registros, 'editable': u['estado'] != 'RETIRADO'}


def validar(datos, editando=False):
    if not isinstance(datos, dict):
        raise ErrorDePeticion('Envía los datos del registro como un objeto JSON.')
    permitidos = {'descripcion', 'motivo', 'revision'} if editando else {'tipo', 'codigo', 'descripcion', 'competenciaId'}
    if set(datos) - permitidos:
        raise ErrorDePeticion('No se permite modificar la unidad, el grado, el tipo, el código ni la competencia del registro.')
    descripcion = _texto(datos, 'descripcion', 10, 4000, 'la descripción')
    if editando:
        return descripcion, _texto(datos, 'motivo', 5, 500, 'el motivo de la corrección')
    codigo = _texto(datos, 'codigo', 2, 60, 'el código').upper()
    if not re.fullmatch(r'[A-Z0-9][A-Z0-9_-]{1,59}', codigo):
        raise ErrorDePeticion('El código admite letras, números, guiones y guiones bajos.')
    return descripcion, codigo


def historial(tipo, registro_id, antes, despues, motivo, actor):
    with connection.cursor() as c:
        c.execute('''INSERT INTO curriculo_registro_revision
            (id,tipo,registro_id,descripcion_anterior,descripcion_nueva,motivo,actor_id)
            VALUES (%s,%s,%s,%s,%s,%s,%s)''', [str(uuid.uuid4()), tipo, registro_id, antes, despues, motivo, actor.id])
    auditoria.registrar(actor.id, 'REGISTRO_CORREGIDO' if antes is not None else 'REGISTRO_CREADO', registro_id,
                       entidad_tipo='CURRICULO_' + tipo, dominio='curriculo')


@transaction.atomic
def crear(unidad_id, datos, actor):
    tabla, enlace, campo = tipo_valido(datos.get('tipo'))
    descripcion, codigo = validar(datos)
    competencia = _id(datos.get('competenciaId'), 'la competencia')
    u = unidad(unidad_id, True)
    if not filas('''SELECT c.id FROM curriculo_unidad_competencia uc
        JOIN curriculo_competencia c ON c.id=uc.competencia_id
        JOIN curriculo_area a ON a.id=c.area_id
        WHERE uc.unidad_id=%s AND c.id=%s AND c.area_id=%s AND a.version_catalogo_id=%s''',
        [unidad_id, competencia, u['area_id'], u['version_catalogo_id']]):
        raise ErrorDePeticion('Elige una competencia vinculada a esta unidad.')
    registro_id = str(uuid.uuid4())
    try:
        with transaction.atomic(), connection.cursor() as c:
            c.execute(f'INSERT INTO {tabla} (id,competencia_id,grado_id,codigo,descripcion) VALUES (%s,%s,%s,%s,%s)',
                      [registro_id, competencia, u['grado_id'], codigo, descripcion])
            c.execute(f'INSERT INTO {enlace} (unidad_id,{campo}) VALUES (%s,%s)', [unidad_id, registro_id])
    except IntegrityError as error:
        if error.args and error.args[0] == 1062:
            raise ErrorDePeticion('Ese código ya existe para la competencia. Usa otro código.', 409)
        raise
    historial(datos['tipo'], registro_id, None, descripcion, 'Registro inicial', actor)
    return consultar(unidad_id)


@transaction.atomic
def modificar(unidad_id, tipo, registro_id, datos, actor):
    tabla, enlace, campo = tipo_valido(tipo)
    descripcion, motivo = validar(datos, True)
    unidad(unidad_id, True)
    found = filas(f'''SELECT r.* FROM {tabla} r JOIN {enlace} l ON l.{campo}=r.id
        WHERE r.id=%s AND l.unidad_id=%s FOR UPDATE''', [registro_id, unidad_id])
    if not found:
        raise ErrorDePeticion('El registro no pertenece a esta unidad.', 404)
    actual = found[0]
    if datos.get('revision') != revision(actual['descripcion']):
        raise ErrorDePeticion('Otra persona modificó este registro. Recarga la unidad antes de guardar.', 409)
    if filas(f'''SELECT u.id FROM {enlace} l JOIN curriculo_unidad u ON u.id=l.unidad_id
        JOIN curriculo_version_catalogo v ON v.id=u.version_catalogo_id
        WHERE l.{campo}=%s AND v.estado='RETIRADO' ''', [registro_id]):
        raise ErrorDePeticion('El registro también pertenece a un catálogo retirado y no se puede corregir.', 409)
    if descripcion != actual['descripcion']:
        with connection.cursor() as c:
            c.execute(f'UPDATE {tabla} SET descripcion=%s WHERE id=%s', [descripcion, registro_id])
        historial(tipo, registro_id, actual['descripcion'], descripcion, motivo, actor)
    return consultar(unidad_id)
