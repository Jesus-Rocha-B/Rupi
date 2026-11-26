"""Adaptador DRF de las sesiones existentes; no crea usuarios ni sesiones paralelas."""
from rest_framework.authentication import BaseAuthentication
from rest_framework.exceptions import AuthenticationFailed
from rest_framework.permissions import BasePermission

class SesionRupiAuthentication(BaseAuthentication):
    def authenticate(self, request):
        actor = getattr(request._request, 'administrador', None)
        if actor is None:
            raise AuthenticationFailed('Tu sesión de administración terminó. Vuelve a ingresar.')
        return actor, None

    def authenticate_header(self, request):
        return 'Session'

class PuedeConsultarCurriculo(BasePermission):
    message = 'Tu rol no tiene permiso para consultar el catálogo.'

    def has_permission(self, request, view):
        return bool(request.user and request.user.puede('curriculo.ver'))
