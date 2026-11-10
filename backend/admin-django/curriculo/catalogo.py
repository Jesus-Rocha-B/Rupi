"""Catálogo general de grados, áreas curriculares y competencias del MINEDU (HU-49).

La consola estructura el catálogo que siguen los roadmaps: áreas en el orden del programa curricular,
competencias con su número del CNEB y sus capacidades. Los códigos no cambian una vez creados porque las
unidades, las rutas y el banco de repaso de Spring los usan. Nada se borra salvo una capacidad mal escrita,
que ninguna otra tabla referencia. Una versión RETIRADA queda como registro: ya no se corrige.
"""
import re
import uuid

from django.db import connection, transaction

from identidad.respuestas import ErrorDePeticion
from operaciones import auditoria

from .models import Area, Capacidad, Competencia, Grado, Unidad, VersionCatalogo

_ID = re.compile(r'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$')
_CODIGO_AREA = re.compile(r'^[A-Z][A-Z0-9]{1,9}$')
_CODIGO_COMPETENCIA = re.compile(r'^[A-Z][A-Z0-9]*(?:-[A-Z0-9]+)+$')
MAX_CAPACIDADES = 8
NUMERO_CNEB_MAXIMO = 31
DOMINIO = 'curriculo'


def _nuevo_id():
    return str(uuid.uuid4())


def _texto(datos, campo, minimo, maximo, etiqueta, obligatorio=True):
    valor = datos.get(campo)
    if valor is None or (isinstance(valor, str) and not valor.strip()):
        if obligatorio:
            raise ErrorDePeticion(f'Falta {etiqueta}.')
        return None
    if not isinstance(valor, str):
        raise ErrorDePeticion(f'{etiqueta.capitalize()} debe ser texto.')
    valor = ' '.join(valor.split())
    if len(valor) < minimo:
        raise ErrorDePeticion(f'{etiqueta.capitalize()} debe tener al menos {minimo} caracteres.')
    if len(valor) > maximo:
        raise ErrorDePeticion(f'{etiqueta.capitalize()} no puede pasar de {maximo} caracteres.')
    return valor


def _entero(valor, minimo, maximo, etiqueta):
    if valor is None or valor == '':
        return None
    if isinstance(valor, bool) or not isinstance(valor, int):
        raise ErrorDePeticion(f'{etiqueta.capitalize()} debe ser un número entero.')
    if not minimo <= valor <= maximo:
        raise ErrorDePeticion(f'{etiqueta.capitalize()} va de {minimo} a {maximo}.')
    return valor


def _id(valor, etiqueta):
    if not isinstance(valor, str) or not _ID.match(valor):
        raise ErrorDePeticion(f'Falta {etiqueta}.')
    return valor


def normalizar_codigo_area(valor):
    codigo = valor.strip().upper() if isinstance(valor, str) else ''
    if not _CODIGO_AREA.match(codigo):
        raise ErrorDePeticion('El código del área lleva de 2 a 10 letras mayúsculas o números, sin espacios (ej. MAT).')
    return codigo


def normalizar_codigo_competencia(valor, codigo_area):
    codigo = valor.strip().upper() if isinstance(valor, str) else ''
    if len(codigo) > 60 or not _CODIGO_COMPETENCIA.match(codigo):
        raise ErrorDePeticion('El código de la competencia lleva letras mayúsculas y números separados por guiones '
                              '(ej. MAT-C1).')
    if not codigo.startswith(f'{codigo_area}-'):
        raise ErrorDePeticion(f'El código de la competencia empieza con el del área: {codigo_area}-…')
    return codigo


def normalizar_capacidades(valor):
    if not isinstance(valor, list) or not valor:
        raise ErrorDePeticion('Toda competencia del CNEB tiene al menos una capacidad.')
    if len(valor) > MAX_CAPACIDADES:
        raise ErrorDePeticion(f'Una competencia tiene como mucho {MAX_CAPACIDADES} capacidades.')
    capacidades, vistas = [], set()
    for posicion, texto in enumerate(valor, 1):
        nombre = _texto({'c': texto}, 'c', 3, 255, f'la capacidad {posicion}')
        clave = nombre.casefold()
        if clave in vistas:
            raise ErrorDePeticion(f'La capacidad «{nombre}» está repetida.')
        vistas.add(clave)
        capacidades.append(nombre)
    return capacidades


def _version(version_id):
    try:
        return VersionCatalogo.objects.get(id=version_id)
    except VersionCatalogo.DoesNotExist:
        raise ErrorDePeticion('No encontramos esa versión del catálogo.', 404)


def _editable(version):
    if version.estado == 'RETIRADO':
        raise ErrorDePeticion('Esta versión del catálogo está retirada: queda como registro y ya no se corrige.', 409)


def _version_por_defecto():
    versiones = list(VersionCatalogo.objects.all())
    if not versiones:
        return None
    activas = [v for v in versiones if v.estado == 'ACTIVO']
    candidatas = activas or versiones
    return max(candidatas, key=lambda v: (v.vigente_desde is not None, v.vigente_desde, v.etiqueta_version))


def _unidades_por_competencia(version_id):
    with connection.cursor() as cursor:
        cursor.execute(
            """
            SELECT uc.competencia_id, COUNT(*)
            FROM curriculo_unidad_competencia uc
            JOIN curriculo_unidad u ON u.id = uc.unidad_id
            WHERE u.version_catalogo_id = %s
            GROUP BY uc.competencia_id
            """,
            [version_id],
        )
        return dict(cursor.fetchall())


def _orden_area(area):
    return (area.orden is None, area.orden or 0, area.nombre)


def _orden_competencia(competencia):
    return (competencia.numero_cneb is None, competencia.numero_cneb or 0, competencia.codigo)


def _version_json(version):
    return {
        'id': version.id,
        'etiqueta': version.etiqueta_version,
        'referencia': version.referencia_origen,
        'vigenteDesde': version.vigente_desde.isoformat() if version.vigente_desde else None,
        'estado': version.estado,
    }


def catalogo(version_id=None):
    versiones = sorted(VersionCatalogo.objects.all(),
                       key=lambda v: (v.vigente_desde is None, -(v.vigente_desde.toordinal() if v.vigente_desde else 0)))
    version = _version(_id(version_id, 'la versión del catálogo')) if version_id else _version_por_defecto()
    grados = [{'id': g.id, 'numero': g.numero_grado, 'nombre': g.nombre}
              for g in Grado.objects.order_by('numero_grado')]
    if version is None:
        return {'versiones': [], 'version': None, 'editable': False, 'grados': grados, 'areas': [],
                'resumen': {'grados': len(grados), 'areas': 0, 'competencias': 0, 'capacidades': 0}}

    areas = sorted(Area.objects.filter(version_catalogo_id=version.id), key=_orden_area)
    competencias = list(Competencia.objects.filter(area__version_catalogo_id=version.id))
    capacidades = {}
    for capacidad in Capacidad.objects.filter(competencia__area__version_catalogo_id=version.id).order_by('orden'):
        capacidades.setdefault(capacidad.competencia_id, []).append(
            {'id': capacidad.id, 'orden': capacidad.orden, 'nombre': capacidad.nombre})
    unidades_area = {}
    for area_id in Unidad.objects.filter(version_catalogo_id=version.id).values_list('area_id', flat=True):
        unidades_area[area_id] = unidades_area.get(area_id, 0) + 1
    unidades_competencia = _unidades_por_competencia(version.id)

    por_area = {}
    for competencia in sorted(competencias, key=_orden_competencia):
        por_area.setdefault(competencia.area_id, []).append({
            'id': competencia.id,
            'codigo': competencia.codigo,
            'numeroCneb': competencia.numero_cneb,
            'nombre': competencia.nombre,
            'descripcion': competencia.descripcion,
            'unidades': unidades_competencia.get(competencia.id, 0),
            'capacidades': capacidades.get(competencia.id, []),
        })

    return {
        'versiones': [_version_json(v) for v in versiones],
        'version': _version_json(version),
        'editable': version.estado != 'RETIRADO',
        'grados': grados,
        'areas': [{
            'id': area.id,
            'codigo': area.codigo,
            'nombre': area.nombre,
            'orden': area.orden,
            'unidades': unidades_area.get(area.id, 0),
            'competencias': por_area.get(area.id, []),
        } for area in areas],
        'resumen': {
            'grados': len(grados),
            'areas': len(areas),
            'competencias': len(competencias),
            'capacidades': sum(len(c) for c in capacidades.values()),
        },
    }


@transaction.atomic
def crear_area(datos, actor):
    version = _version(_id(datos.get('version'), 'la versión del catálogo'))
    _editable(version)
    codigo = normalizar_codigo_area(datos.get('codigo'))
    nombre = _texto(datos, 'nombre', 3, 120, 'el nombre del área')
    orden = _entero(datos.get('orden'), 1, 20, 'el orden')
    if Area.objects.filter(version_catalogo_id=version.id, codigo=codigo).exists():
        raise ErrorDePeticion(f'Ya hay un área {codigo} en {version.etiqueta_version}.', 409)
    if Area.objects.filter(version_catalogo_id=version.id, nombre__iexact=nombre).exists():
        raise ErrorDePeticion(f'Ya hay un área llamada «{nombre}» en {version.etiqueta_version}.', 409)
    area = Area.objects.create(id=_nuevo_id(), version_catalogo_id=version.id, codigo=codigo, nombre=nombre,
                               orden=orden)
    auditoria.registrar(actor.id, 'AREA_CREADA', area.id, entidad_tipo='CURRICULO_AREA', dominio=DOMINIO)
    return catalogo(version.id)


def _area(area_id):
    try:
        return Area.objects.select_related('version_catalogo').get(id=area_id)
    except Area.DoesNotExist:
        raise ErrorDePeticion('No encontramos esa área.', 404)


@transaction.atomic
def modificar_area(area_id, datos, actor):
    area = _area(area_id)
    _editable(area.version_catalogo)
    if 'codigo' in datos and datos['codigo'] != area.codigo:
        raise ErrorDePeticion('El código del área no se cambia: las unidades y las rutas lo usan.')
    cambios = []
    if 'nombre' in datos:
        nombre = _texto(datos, 'nombre', 3, 120, 'el nombre del área')
        if nombre != area.nombre:
            if Area.objects.filter(version_catalogo_id=area.version_catalogo_id, nombre__iexact=nombre) \
                    .exclude(id=area.id).exists():
                raise ErrorDePeticion(f'Ya hay un área llamada «{nombre}».', 409)
            area.nombre = nombre
            cambios.append('nombre')
    if 'orden' in datos:
        orden = _entero(datos.get('orden'), 1, 20, 'el orden')
        if orden != area.orden:
            area.orden = orden
            cambios.append('orden')
    if cambios:
        area.save(update_fields=cambios)
        auditoria.registrar(actor.id, 'AREA_MODIFICADA', area.id, entidad_tipo='CURRICULO_AREA', dominio=DOMINIO)
    return catalogo(area.version_catalogo_id)


def _numero_libre(numero, version_id, excepto=None):
    if numero is None:
        return
    ocupada = Competencia.objects.filter(area__version_catalogo_id=version_id, numero_cneb=numero)
    if excepto:
        ocupada = ocupada.exclude(id=excepto)
    otra = ocupada.first()
    if otra:
        raise ErrorDePeticion(f'El número {numero} del CNEB ya es de {otra.codigo}.', 409)


def _guardar_capacidades(competencia, nombres):
    actuales = {c.orden: c for c in Capacidad.objects.filter(competencia_id=competencia.id)}
    if [actuales[o].nombre for o in sorted(actuales)] == nombres:
        return False
    Capacidad.objects.filter(competencia_id=competencia.id, orden__gt=len(nombres)).delete()
    for orden, nombre in enumerate(nombres, 1):
        existente = actuales.get(orden)
        if existente is None:
            Capacidad.objects.create(id=_nuevo_id(), competencia_id=competencia.id, orden=orden, nombre=nombre)
        elif existente.nombre != nombre:
            existente.nombre = nombre
            existente.save(update_fields=['nombre'])
    return True


@transaction.atomic
def crear_competencia(datos, actor):
    area = _area(_id(datos.get('area'), 'el área'))
    _editable(area.version_catalogo)
    codigo = normalizar_codigo_competencia(datos.get('codigo'), area.codigo)
    nombre = _texto(datos, 'nombre', 3, 255, 'el nombre de la competencia')
    descripcion = _texto(datos, 'descripcion', 3, 2000, 'la descripción', obligatorio=False)
    numero = _entero(datos.get('numeroCneb'), 1, NUMERO_CNEB_MAXIMO, 'el número del CNEB')
    capacidades = normalizar_capacidades(datos.get('capacidades'))
    if Competencia.objects.filter(area_id=area.id, codigo=codigo).exists():
        raise ErrorDePeticion(f'Ya hay una competencia {codigo} en {area.nombre}.', 409)
    _numero_libre(numero, area.version_catalogo_id)
    competencia = Competencia.objects.create(id=_nuevo_id(), area_id=area.id, codigo=codigo, numero_cneb=numero,
                                             nombre=nombre, descripcion=descripcion)
    _guardar_capacidades(competencia, capacidades)
    auditoria.registrar(actor.id, 'COMPETENCIA_CREADA', competencia.id, entidad_tipo='CURRICULO_COMPETENCIA',
                        dominio=DOMINIO)
    return catalogo(area.version_catalogo_id)


@transaction.atomic
def modificar_competencia(competencia_id, datos, actor):
    try:
        competencia = Competencia.objects.select_related('area__version_catalogo').get(id=competencia_id)
    except Competencia.DoesNotExist:
        raise ErrorDePeticion('No encontramos esa competencia.', 404)
    version = competencia.area.version_catalogo
    _editable(version)
    if 'codigo' in datos and datos['codigo'] != competencia.codigo:
        raise ErrorDePeticion('El código de la competencia no se cambia: las unidades y los reportes lo usan.')
    cambios = []
    if 'nombre' in datos:
        nombre = _texto(datos, 'nombre', 3, 255, 'el nombre de la competencia')
        if nombre != competencia.nombre:
            competencia.nombre = nombre
            cambios.append('nombre')
    if 'descripcion' in datos:
        descripcion = _texto(datos, 'descripcion', 3, 2000, 'la descripción', obligatorio=False)
        if descripcion != competencia.descripcion:
            competencia.descripcion = descripcion
            cambios.append('descripcion')
    if 'numeroCneb' in datos:
        numero = _entero(datos.get('numeroCneb'), 1, NUMERO_CNEB_MAXIMO, 'el número del CNEB')
        if numero != competencia.numero_cneb:
            _numero_libre(numero, version.id, excepto=competencia.id)
            competencia.numero_cneb = numero
            cambios.append('numero_cneb')
    capacidades_cambiaron = False
    if 'capacidades' in datos:
        capacidades_cambiaron = _guardar_capacidades(competencia, normalizar_capacidades(datos.get('capacidades')))
    if cambios:
        competencia.save(update_fields=cambios)
    if cambios or capacidades_cambiaron:
        auditoria.registrar(actor.id, 'COMPETENCIA_MODIFICADA', competencia.id,
                            entidad_tipo='CURRICULO_COMPETENCIA', dominio=DOMINIO)
    return catalogo(version.id)
