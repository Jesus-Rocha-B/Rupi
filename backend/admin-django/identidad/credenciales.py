"""Establecer contraseñas y desbloquear cuentas institucionales a pedido (HU-44).

Una docente avisa que un estudiante olvidó su clave o que su cuenta quedó bloqueada; la administración o la
mesa de ayuda le da una clave temporal o la desbloquea. Cambiar la clave o bloquear cierra la sesión abierta.
"""
from django.db import transaction
from django.utils import timezone

from operaciones import auditoria

from . import claves, notificaciones
from .cuentas import detalle, obtener, otro_administrador_activo
from .models import CuentaUsuario, UsuarioRol
from .respuestas import ErrorDePeticion
from .sesion import ROL_ADMINISTRADOR, revocar_sesiones

ROLES_DE_CONSOLA = {'ADMINISTRADOR', 'SOPORTE'}


def _proteger(cuenta, actor):
    if cuenta.id == actor.id:
        raise ErrorDePeticion('No puedes cambiar el acceso de tu propia cuenta desde aquí. Pídeselo a otra persona '
                              'de administración.', 409)
    roles = set(UsuarioRol.objects.filter(usuario_id=cuenta.id).values_list('rol__codigo', flat=True))
    # La mesa de ayuda no asigna roles: si pudiera cambiar la clave de una cuenta de consola, entraría con ella.
    if roles & ROLES_DE_CONSOLA and not actor.puede('roles.asignar'):
        raise ErrorDePeticion('Solo la administración puede cambiar el acceso de otra cuenta de la consola.', 403)
    if cuenta.estado == 'DESHABILITADO':
        raise ErrorDePeticion('La cuenta está dada de baja. Reactívala antes de cambiar su acceso.', 409)
    return roles


@transaction.atomic
def restablecer_clave(cuenta_id, datos, actor, ver_datos=True):
    cuenta = obtener(cuenta_id)
    _proteger(cuenta, actor)
    clave = datos.get('clave')
    generada = clave is None or (isinstance(clave, str) and not clave.strip())
    if generada:
        clave = claves.generar_clave_temporal()
    elif not isinstance(clave, str):
        raise ErrorDePeticion('La contraseña debe ser texto.')
    problema = claves.problema_clave_nueva(clave, cuenta.nombre_usuario)
    if problema:
        raise ErrorDePeticion(problema)
    ahora = timezone.now()
    CuentaUsuario.objects.filter(id=cuenta.id).update(hash_clave=claves.cifrar(clave), clave_cambiada_en=ahora,
                                                      actualizado_en=ahora)
    revocar_sesiones(cuenta.id, ahora)
    auditoria.registrar(actor.id, 'CLAVE_RESTABLECIDA', cuenta.id)
    correo_enviado = notificaciones.notificar_recuperacion_clave(
        cuenta.correo, cuenta.nombre_usuario, cuenta.nombre, clave if generada else None
    )
    resultado = detalle(cuenta.id, ver_datos)
    resultado['correoNotificado'] = correo_enviado
    if cuenta.correo:
        resultado['correoDestino'] = cuenta.correo
    # Una clave escrita por la persona no se devuelve: ya la conoce y así no queda en ninguna respuesta.
    if generada:
        resultado['claveTemporal'] = clave
    return resultado


@transaction.atomic
def bloquear(cuenta_id, actor, ver_datos=True):
    cuenta = obtener(cuenta_id)
    roles = _proteger(cuenta, actor)
    ahora = timezone.now()
    if ROL_ADMINISTRADOR in roles and not otro_administrador_activo(cuenta.id, ahora):
        raise ErrorDePeticion('Es la única cuenta de administración activa: Rupi se quedaría sin administración.', 409)
    if cuenta.estado != 'BLOQUEADO':
        CuentaUsuario.objects.filter(id=cuenta.id).update(estado='BLOQUEADO', actualizado_en=ahora)
        auditoria.registrar(actor.id, 'CUENTA_BLOQUEADA', cuenta.id)
    revocar_sesiones(cuenta.id, ahora)
    return detalle(cuenta.id, ver_datos)


@transaction.atomic
def desbloquear(cuenta_id, actor, ver_datos=True):
    cuenta = obtener(cuenta_id)
    _proteger(cuenta, actor)
    ahora = timezone.now()
    bloqueada = cuenta.estado == 'BLOQUEADO' or (cuenta.bloqueado_hasta is not None and cuenta.bloqueado_hasta > ahora)
    correo_enviado = False
    if bloqueada:
        CuentaUsuario.objects.filter(id=cuenta.id).update(estado='ACTIVO', bloqueado_hasta=None, actualizado_en=ahora)
        auditoria.registrar(actor.id, 'CUENTA_DESBLOQUEADA', cuenta.id)
        correo_enviado = notificaciones.notificar_cuenta_desbloqueada(
            cuenta.correo, cuenta.nombre_usuario, cuenta.nombre
        )
    resultado = detalle(cuenta.id, ver_datos)
    resultado['correoNotificado'] = correo_enviado
    if cuenta.correo:
        resultado['correoDestino'] = cuenta.correo
    return resultado
