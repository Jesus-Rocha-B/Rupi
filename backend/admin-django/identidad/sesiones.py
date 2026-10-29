"""Sesión única sincronizada entre la web y el celular (HU-43).

Cada cuenta tiene como mucho una sesión activa: lo asegura el índice UNIQUE de usuario_activo_id. Abrir
Rupi en el celular cierra la sesión de la web y al revés; Spring explica el motivo en el 401. Desde la consola
se ve dónde está abierta cada cuenta y se puede cerrar su sesión (p. ej., si una tableta del aula se perdió).
"""
from django.db import transaction
from django.utils import timezone

from operaciones import auditoria

from .cuentas import detalle, obtener, roles_por_cuenta
from .models import SesionUsuario
from .respuestas import ErrorDePeticion
from .sesion import nombre_visible, revocar_sesiones

CLIENTES = ('WEB_USUARIO', 'MOVIL', 'WEB_ADMIN')
LIMITE = 200


def activas(cliente=None):
    ahora = timezone.now()
    consulta = (SesionUsuario.objects.select_related('usuario', 'usuario__perfil')
                .filter(revocada_en__isnull=True, expira_en__gt=ahora))
    totales = {c: 0 for c in CLIENTES}
    for codigo in consulta.values_list('cliente', flat=True):
        totales[codigo] = totales.get(codigo, 0) + 1
    if cliente in CLIENTES:
        consulta = consulta.filter(cliente=cliente)
    sesiones = list(consulta.order_by('-actividad_en')[:LIMITE])
    roles = roles_por_cuenta([s.usuario_id for s in sesiones])
    return {
        'totales': totales,
        'items': [{
            'cuentaId': s.usuario_id,
            'usuario': s.usuario.nombre_usuario,
            'nombre': nombre_visible(s.usuario),
            'roles': roles.get(s.usuario_id, []),
            'cliente': s.cliente,
            'desde': s.creada_en.isoformat(),
            'actividad': s.actividad_en.isoformat(),
            'expira': s.expira_en.isoformat(),
        } for s in sesiones],
    }


@transaction.atomic
def cerrar(cuenta_id, actor, ver_datos=True):
    cuenta = obtener(cuenta_id)
    if cuenta.id == actor.id:
        raise ErrorDePeticion('Para cerrar tu propia sesión usa «Salir».', 409)
    if revocar_sesiones(cuenta.id):
        auditoria.registrar(actor.id, 'SESION_CERRADA', cuenta.id)
    return detalle(cuenta.id, ver_datos)
