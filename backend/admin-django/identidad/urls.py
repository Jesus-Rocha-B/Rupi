from django.urls import path, register_converter

from . import vistas


class UuidTexto:
    """Los ids son CHAR(36) en minúsculas; se conservan como texto para compararlos tal cual."""
    regex = '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'

    def to_python(self, valor):
        return valor

    def to_url(self, valor):
        return valor


register_converter(UuidTexto, 'uuid36')

urlpatterns = [
    path('sesion', vistas.sesion_actual),
    path('sesion/2fa', vistas.sesion_2fa),
    path('2fa/docente/iniciar', vistas.docente_2fa_iniciar),
    path('2fa/docente/confirmar', vistas.docente_2fa_confirmar),
    path('seguridad/2fa', vistas.seguridad_2fa),
    path('seguridad/2fa/mi-rotacion', vistas.seguridad_2fa_rotar),
    path('seguridad/2fa/mi-confirmacion', vistas.seguridad_2fa_confirmar),
    path('seguridad/2fa/cuentas/<uuid36:cuenta_id>/reiniciar', vistas.seguridad_2fa_reiniciar),
    path('cuentas', vistas.lista_cuentas),
    path('cuentas/<uuid36:cuenta_id>', vistas.cuenta),
    path('cuentas/<uuid36:cuenta_id>/baja', vistas.baja),
    path('cuentas/<uuid36:cuenta_id>/reactivar', vistas.reactivacion),
    path('cuentas/<uuid36:cuenta_id>/roles', vistas.roles_de_cuenta),
    path('cuentas/<uuid36:cuenta_id>/cerrar-sesion', vistas.cerrar_sesion_de_cuenta),
    path('cuentas/<uuid36:cuenta_id>/clave', vistas.clave_de_cuenta),
    path('cuentas/<uuid36:cuenta_id>/bloquear', vistas.bloqueo),
    path('cuentas/<uuid36:cuenta_id>/desbloquear', vistas.desbloqueo),
    path('roles', vistas.catalogo_roles),
    path('sesiones', vistas.sesiones_activas),
    path('acceso/politica', vistas.politica_de_acceso),
]
