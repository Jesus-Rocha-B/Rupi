"""Altas, bajas y modificaciones de cuentas de docentes y estudiantes (HU-41).

Ninguna cuenta se borra: las filas tienen progreso, notas y auditoría que dependen de ellas. Dar de baja
deja la cuenta en DESHABILITADO con su fecha y cierra su sesión; reactivarla la devuelve a ACTIVO.
"""
import re

from django.core.exceptions import ValidationError
from django.core.validators import validate_email
from django.db import IntegrityError, transaction
from django.db.models import Max, Q
from django.utils import timezone

from operaciones import auditoria

from . import claves
from .models import CuentaUsuario, IntentoAcceso, PerfilUsuario, Rol, SesionUsuario, UsuarioRol
from .respuestas import ErrorDePeticion
from .sesion import ROL_ADMINISTRADOR, cuenta_disponible, revocar_sesiones

POR_PAGINA = 20
ROLES_DE_ALTA = ('ESTUDIANTE', 'DOCENTE')
ESTADOS = {codigo for codigo, _ in CuentaUsuario.ESTADOS}
_USUARIO = re.compile(r'^[a-z0-9](?:[a-z0-9._-]{1,78}[a-z0-9])$')


def _texto(datos, campo, maximo, obligatorio=False, etiqueta=None):
    valor = datos.get(campo)
    if valor is None or (isinstance(valor, str) and not valor.strip()):
        if obligatorio:
            raise ErrorDePeticion(f'Falta {etiqueta or campo}.')
        return None
    if not isinstance(valor, str):
        raise ErrorDePeticion(f'{(etiqueta or campo).capitalize()} debe ser texto.')
    valor = ' '.join(valor.split())
    if len(valor) > maximo:
        raise ErrorDePeticion(f'{(etiqueta or campo).capitalize()} no puede pasar de {maximo} caracteres.')
    return valor


def normalizar_usuario(valor):
    usuario = (valor or '').strip().lower() if isinstance(valor, str) else ''
    if not _USUARIO.match(usuario):
        raise ErrorDePeticion('El usuario debe tener de 3 a 80 caracteres: letras sin tildes, números, punto, '
                              'guion o guion bajo, y empezar y terminar con letra o número.')
    return usuario


def normalizar_correo(valor):
    if valor is None or (isinstance(valor, str) and not valor.strip()):
        return None
    if not isinstance(valor, str) or len(valor.strip()) > 254:
        raise ErrorDePeticion('El correo no es válido.')
    correo = valor.strip().lower()
    try:
        validate_email(correo)
    except ValidationError:
        raise ErrorDePeticion('El correo no es válido.')
    return correo


def _ocupado(campo, valor, excepto=None):
    consulta = CuentaUsuario.objects.filter(**{campo: valor})
    if excepto:
        consulta = consulta.exclude(id=excepto)
    return consulta.exists()


def roles_por_cuenta(ids):
    roles = {}
    for usuario_id, codigo in UsuarioRol.objects.filter(usuario_id__in=ids).values_list('usuario_id', 'rol__codigo'):
        roles.setdefault(usuario_id, []).append(codigo)
    return {usuario_id: sorted(codigos) for usuario_id, codigos in roles.items()}


def _sesiones_activas(ids, ahora):
    sesiones = SesionUsuario.objects.filter(usuario_id__in=ids, revocada_en__isnull=True, expira_en__gt=ahora)
    return {s.usuario_id: s for s in sesiones}


def _iso(fecha):
    return fecha.isoformat() if fecha else None


PERMISO_DATOS = 'cuentas.datos_personales'


def enmascarar_correo(correo):
    """«lucia.huaman@rupi.pe» → «l•••@rupi.pe»: mesa de ayuda reconoce la cuenta sin leer el dato completo."""
    if not correo or '@' not in correo:
        return correo
    local, dominio = correo.split('@', 1)
    return f'{local[:1]}•••@{dominio}'


def ve_datos(actor):
    return actor.puede(PERMISO_DATOS)


def resumen(cuenta, roles, sesion, ahora, ver_datos=True):
    perfil = getattr(cuenta, 'perfil', None)
    bloqueada = cuenta.estado == 'BLOQUEADO' or (cuenta.bloqueado_hasta is not None and cuenta.bloqueado_hasta > ahora)
    return {
        'id': cuenta.id,
        'usuario': cuenta.nombre_usuario,
        'nombre': perfil.nombre if perfil else None,
        'apellido': perfil.apellido if perfil else None,
        'nombrePreferido': perfil.nombre_preferido if perfil else None,
        'correo': cuenta.correo if ver_datos else enmascarar_correo(cuenta.correo),
        'datosCompletos': ver_datos,
        'estado': cuenta.estado,
        'bloqueada': bloqueada,
        'bloqueadoHasta': _iso(cuenta.bloqueado_hasta),
        'roles': roles,
        'creadoEn': _iso(cuenta.creado_en),
        'desactivadoEn': _iso(cuenta.desactivado_en),
        'sesionActiva': {
            'cliente': sesion.cliente,
            'desde': _iso(sesion.creada_en),
            'actividad': _iso(sesion.actividad_en),
        } if sesion else None,
    }


def listar(q=None, rol=None, estado=None, pagina=1, ver_datos=True):
    ahora = timezone.now()
    consulta = CuentaUsuario.objects.select_related('perfil')
    if q:
        q = q.strip()[:80]
        filtro = (Q(nombre_usuario__icontains=q) | Q(perfil__nombre__icontains=q) | Q(perfil__apellido__icontains=q)
                  | Q(perfil__nombre_preferido__icontains=q))
        # Sin permiso de datos personales no se busca por correo: probar letras revelaría el dato oculto.
        if ver_datos:
            filtro |= Q(correo__icontains=q)
        consulta = consulta.filter(filtro)
    if rol:
        consulta = consulta.filter(roles_asignados__rol__codigo=rol)
    if estado == 'BLOQUEADO':
        consulta = consulta.filter(Q(estado='BLOQUEADO') | Q(bloqueado_hasta__gt=ahora))
    elif estado in ESTADOS:
        consulta = consulta.filter(estado=estado)
    consulta = consulta.distinct().order_by('perfil__apellido', 'perfil__nombre', 'nombre_usuario')
    total = consulta.count()
    pagina = max(1, pagina)
    cuentas = list(consulta[(pagina - 1) * POR_PAGINA: pagina * POR_PAGINA])
    ids = [c.id for c in cuentas]
    roles = roles_por_cuenta(ids)
    sesiones = _sesiones_activas(ids, ahora)
    return {
        'items': [resumen(c, roles.get(c.id, []), sesiones.get(c.id), ahora, ver_datos) for c in cuentas],
        'total': total,
        'pagina': pagina,
        'porPagina': POR_PAGINA,
    }


def obtener(cuenta_id):
    cuenta = CuentaUsuario.objects.select_related('perfil').filter(id=cuenta_id).first()
    if cuenta is None:
        raise ErrorDePeticion('No encontramos esa cuenta.', 404)
    return cuenta


def detalle(cuenta_id, ver_datos=True):
    ahora = timezone.now()
    cuenta = obtener(cuenta_id)
    datos = resumen(cuenta, roles_por_cuenta([cuenta.id]).get(cuenta.id, []),
                    _sesiones_activas([cuenta.id], ahora).get(cuenta.id), ahora, ver_datos)
    datos['ultimoIngreso'] = _iso(IntentoAcceso.objects.filter(usuario_id=cuenta.id, resultado='CORRECTO')
                                  .aggregate(m=Max('ocurrido_en'))['m'])
    datos['historial'] = [{'accion': e.accion, 'resultado': e.resultado, 'fecha': _iso(e.ocurrido_en),
                           'actor': e.actor_usuario_id} for e in auditoria.historial(cuenta.id)]
    return datos


@transaction.atomic
def crear(datos, actor):
    usuario = normalizar_usuario(datos.get('usuario'))
    nombre = _texto(datos, 'nombre', 100, obligatorio=True, etiqueta='el nombre')
    apellido = _texto(datos, 'apellido', 100, obligatorio=True, etiqueta='el apellido')
    preferido = _texto(datos, 'nombrePreferido', 100, etiqueta='el nombre preferido')
    correo = normalizar_correo(datos.get('correo'))
    rol_codigo = datos.get('rol')
    if rol_codigo not in ROLES_DE_ALTA:
        raise ErrorDePeticion('Elige si la cuenta es de estudiante o de docente.')
    if _ocupado('nombre_usuario', usuario):
        raise ErrorDePeticion('Ese usuario ya existe. Prueba con otro.', 409)
    if correo and _ocupado('correo', correo):
        raise ErrorDePeticion('Ese correo ya pertenece a otra cuenta.', 409)
    rol = Rol.objects.filter(codigo=rol_codigo).first()
    if rol is None:
        raise ErrorDePeticion('El rol no está configurado en la base. Aplica las migraciones.', 409)

    ahora = timezone.now()
    clave_temporal = claves.generar_clave_temporal()
    try:
        cuenta = CuentaUsuario.objects.create(nombre_usuario=usuario, correo=correo,
                                              hash_clave=claves.cifrar(clave_temporal), estado='ACTIVO',
                                              clave_cambiada_en=ahora, creado_en=ahora, actualizado_en=ahora)
    except IntegrityError:
        raise ErrorDePeticion('El usuario o el correo ya están en uso.', 409)
    PerfilUsuario.objects.create(usuario=cuenta, nombre=nombre, apellido=apellido, nombre_preferido=preferido)
    UsuarioRol.objects.create(usuario=cuenta, rol=rol, asignado_por_id=actor.id, asignado_en=ahora)
    auditoria.registrar(actor.id, 'CUENTA_CREADA', cuenta.id)
    resultado = detalle(cuenta.id, ve_datos(actor))
    resultado['claveTemporal'] = clave_temporal
    return resultado


@transaction.atomic
def modificar(cuenta_id, datos, actor):
    cuenta = obtener(cuenta_id)
    ahora = timezone.now()
    cambios = {}
    if 'usuario' in datos:
        usuario = normalizar_usuario(datos.get('usuario'))
        if usuario != cuenta.nombre_usuario:
            if _ocupado('nombre_usuario', usuario, excepto=cuenta.id):
                raise ErrorDePeticion('Ese usuario ya existe. Prueba con otro.', 409)
            cambios['nombre_usuario'] = usuario
    if 'correo' in datos:
        correo = normalizar_correo(datos.get('correo'))
        if correo != cuenta.correo:
            if correo and _ocupado('correo', correo, excepto=cuenta.id):
                raise ErrorDePeticion('Ese correo ya pertenece a otra cuenta.', 409)
            cambios['correo'] = correo
    perfil_cambios = {}
    for campo, columna, etiqueta, obligatorio in (('nombre', 'nombre', 'el nombre', True),
                                                  ('apellido', 'apellido', 'el apellido', True),
                                                  ('nombrePreferido', 'nombre_preferido', 'el nombre preferido', False)):
        if campo in datos:
            perfil_cambios[columna] = _texto(datos, campo, 100, obligatorio=obligatorio, etiqueta=etiqueta)
    if not cambios and not perfil_cambios:
        return detalle(cuenta.id, ve_datos(actor))
    try:
        CuentaUsuario.objects.filter(id=cuenta.id).update(actualizado_en=ahora, **cambios)
    except IntegrityError:
        raise ErrorDePeticion('El usuario o el correo ya están en uso.', 409)
    if perfil_cambios:
        PerfilUsuario.objects.update_or_create(usuario_id=cuenta.id, defaults=perfil_cambios)
    auditoria.registrar(actor.id, 'CUENTA_MODIFICADA', cuenta.id)
    return detalle(cuenta.id, ve_datos(actor))


def otro_administrador_activo(cuenta_id, ahora):
    otros = (CuentaUsuario.objects.filter(roles_asignados__rol__codigo=ROL_ADMINISTRADOR)
             .exclude(id=cuenta_id).distinct())
    return any(cuenta_disponible(c, ahora) for c in otros)


@transaction.atomic
def dar_de_baja(cuenta_id, actor):
    cuenta = obtener(cuenta_id)
    ahora = timezone.now()
    if cuenta.id == actor.id:
        raise ErrorDePeticion('No puedes dar de baja tu propia cuenta.', 409)
    if cuenta.estado == 'DESHABILITADO':
        return detalle(cuenta.id, ve_datos(actor))
    es_admin = UsuarioRol.objects.filter(usuario_id=cuenta.id, rol__codigo=ROL_ADMINISTRADOR).exists()
    if es_admin and not otro_administrador_activo(cuenta.id, ahora):
        raise ErrorDePeticion('Es la única cuenta de administración activa: Rupi se quedaría sin administración.', 409)
    CuentaUsuario.objects.filter(id=cuenta.id).update(estado='DESHABILITADO', desactivado_en=ahora,
                                                      actualizado_en=ahora)
    revocar_sesiones(cuenta.id, ahora)
    auditoria.registrar(actor.id, 'CUENTA_DADA_DE_BAJA', cuenta.id)
    return detalle(cuenta.id, ve_datos(actor))


@transaction.atomic
def reactivar(cuenta_id, actor):
    cuenta = obtener(cuenta_id)
    if cuenta.estado != 'DESHABILITADO':
        return detalle(cuenta.id, ve_datos(actor))
    ahora = timezone.now()
    CuentaUsuario.objects.filter(id=cuenta.id).update(estado='ACTIVO', desactivado_en=None, actualizado_en=ahora)
    auditoria.registrar(actor.id, 'CUENTA_REACTIVADA', cuenta.id)
    return detalle(cuenta.id, ve_datos(actor))
