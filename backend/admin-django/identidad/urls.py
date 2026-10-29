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
    path('cuentas', vistas.lista_cuentas),
    path('cuentas/<uuid36:cuenta_id>', vistas.cuenta),
    path('cuentas/<uuid36:cuenta_id>/baja', vistas.baja),
    path('cuentas/<uuid36:cuenta_id>/reactivar', vistas.reactivacion),
    path('cuentas/<uuid36:cuenta_id>/roles', vistas.roles_de_cuenta),
    path('roles', vistas.catalogo_roles),
]
