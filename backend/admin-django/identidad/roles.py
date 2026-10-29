"""Roles y permisos estrictos por perfil (HU-42).

Los permisos de cada rol vienen de las migraciones (V31, V32…): la consola no los edita, solo asigna roles a
las cuentas. Las reglas protegen a los menores y evitan que la plataforma se quede sin administración.
"""
from django.db import transaction
from django.db.models import Count
from django.utils import timezone

from operaciones import auditoria

from .cuentas import detalle, obtener, otro_administrador_activo
from .models import Permiso, Rol, RolPermiso, UsuarioRol
from .respuestas import ErrorDePeticion
from .sesion import PERMISO_CONSOLA, ROL_ADMINISTRADOR, revocar_sesiones

ROL_ESTUDIANTE = 'ESTUDIANTE'
ORDEN = ('ESTUDIANTE', 'DOCENTE', 'SOPORTE', 'ADMINISTRADOR')


def _orden(codigo):
    return ORDEN.index(codigo) if codigo in ORDEN else len(ORDEN)


def catalogo():
    permisos_por_rol = {}
    for rol_id, codigo in RolPermiso.objects.values_list('rol_id', 'permiso__codigo'):
        permisos_por_rol.setdefault(rol_id, []).append(codigo)
    cuentas = dict(Rol.objects.annotate(n=Count('usuariorol')).values_list('id', 'n'))
    roles = sorted(Rol.objects.all(), key=lambda r: (_orden(r.codigo), r.codigo))
    return {
        'roles': [{
            'codigo': rol.codigo,
            'nombre': rol.nombre,
            'descripcion': rol.descripcion,
            'permisos': sorted(permisos_por_rol.get(rol.id, [])),
            'consola': PERMISO_CONSOLA in permisos_por_rol.get(rol.id, []),
            'cuentas': cuentas.get(rol.id, 0),
        } for rol in roles],
        'permisos': [{'codigo': p.codigo, 'descripcion': p.descripcion}
                     for p in Permiso.objects.order_by('codigo')],
    }


def problema_combinacion(codigos):
    """Devuelve por qué no se puede usar esta combinación de roles, o None si es válida."""
    if not codigos:
        return 'Toda cuenta necesita al menos un rol.'
    if ROL_ESTUDIANTE in codigos and len(codigos) > 1:
        return 'Una cuenta de estudiante no puede tener otros roles: es de una niña o un niño.'
    return None


@transaction.atomic
def asignar(cuenta_id, codigos, actor, ver_datos=True):
    if not isinstance(codigos, list) or not all(isinstance(c, str) for c in codigos):
        raise ErrorDePeticion('Envía la lista de roles.')
    cuenta = obtener(cuenta_id)
    nuevos = set(codigos)
    roles = {r.codigo: r for r in Rol.objects.filter(codigo__in=nuevos)}
    desconocidos = nuevos - roles.keys()
    if desconocidos:
        raise ErrorDePeticion(f'No existe el rol {", ".join(sorted(desconocidos))}.')
    problema = problema_combinacion(nuevos)
    if problema:
        raise ErrorDePeticion(problema, 409)

    actuales = set(UsuarioRol.objects.filter(usuario_id=cuenta.id).values_list('rol__codigo', flat=True))
    if nuevos == actuales:
        return detalle(cuenta.id, ver_datos)
    ahora = timezone.now()
    if ROL_ADMINISTRADOR in actuales - nuevos:
        if cuenta.id == actor.id:
            raise ErrorDePeticion('No puedes quitarte tu propio rol de administración.', 409)
        if not otro_administrador_activo(cuenta.id, ahora):
            raise ErrorDePeticion('Es la única cuenta de administración activa: Rupi se quedaría sin administración.', 409)

    retirados = actuales - nuevos
    UsuarioRol.objects.filter(usuario_id=cuenta.id, rol__codigo__in=retirados).delete()
    for codigo in sorted(nuevos - actuales, key=_orden):
        UsuarioRol.objects.create(usuario_id=cuenta.id, rol=roles[codigo], asignado_por_id=actor.id, asignado_en=ahora)
    for codigo in sorted(nuevos - actuales, key=_orden):
        auditoria.registrar(actor.id, f'ROL_ASIGNADO:{codigo}', cuenta.id)
    for codigo in sorted(retirados, key=_orden):
        auditoria.registrar(actor.id, f'ROL_RETIRADO:{codigo}', cuenta.id)
    # Con otro rol cambia lo que la persona puede abrir: se cierra su sesión para que vuelva a ingresar
    # con sus permisos nuevos. Si es la misma administración, conserva la sesión con la que está trabajando.
    revocar_sesiones(cuenta.id, ahora, excepto=actor.sesion_id if cuenta.id == actor.id else None)
    return detalle(cuenta.id, ver_datos)
