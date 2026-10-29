"""Sesiones de la consola de administración.

Usan la misma tabla que la web y el celular (identidad_sesion_usuario) con cliente WEB_ADMIN. Solo se guarda
el SHA-256 del token; el token viaja en una cookie HttpOnly limitada a /api/admin. La columna generada
usuario_activo_id es UNIQUE: abrir una sesión cierra la anterior de la misma cuenta, esté donde esté.
"""
import hashlib
import secrets
from dataclasses import dataclass
from datetime import timedelta
from functools import wraps

from django.db import transaction
from django.utils import timezone

from . import claves
from .models import CuentaUsuario, IntentoAcceso, RolPermiso, SesionUsuario, UsuarioRol
from .respuestas import json_error

COOKIE = 'rupi_admin'
RUTA_COOKIE = '/api/admin'
DURACION = timedelta(hours=8)
ROL_ADMINISTRADOR = 'ADMINISTRADOR'
# Igual que StudentSessionService: 5 fallos por cuenta y equipo, o 20 por equipo, en 5 minutos.
VENTANA_INTENTOS = timedelta(minutes=5)
_ACTIVIDAD_CADA = timedelta(minutes=5)


@dataclass(frozen=True)
class Administrador:
    id: str
    usuario: str
    nombre: str
    permisos: frozenset
    sesion_id: str

    def puede(self, permiso):
        return permiso in self.permisos


def sha256(texto):
    return hashlib.sha256(texto.encode('utf-8')).digest()


def permisos_de(usuario_id):
    roles = UsuarioRol.objects.filter(usuario_id=usuario_id).values('rol_id')
    return frozenset(RolPermiso.objects.filter(rol_id__in=roles).values_list('permiso__codigo', flat=True))


def es_administrador(usuario_id):
    return UsuarioRol.objects.filter(usuario_id=usuario_id, rol__codigo=ROL_ADMINISTRADOR).exists()


def cuenta_disponible(cuenta, ahora):
    return (cuenta.estado == 'ACTIVO' and cuenta.desactivado_en is None
            and (cuenta.bloqueado_hasta is None or cuenta.bloqueado_hasta <= ahora))


def nombre_visible(cuenta):
    perfil = getattr(cuenta, 'perfil', None)
    if perfil is not None:
        return perfil.nombre_preferido or perfil.nombre or cuenta.nombre_usuario
    return cuenta.nombre_usuario


def _intento(cuenta_id, id_hash, resultado, origen_hash, ahora):
    IntentoAcceso.objects.create(usuario_id=cuenta_id, identificador_hash=id_hash, resultado=resultado,
                                 origen_hash=origen_hash, ocurrido_en=ahora)


def iniciar(nombre_usuario, clave, ip):
    """Devuelve (token, administrador), 'LIMITE' si se superó el límite de intentos o None si no procede."""
    nombre_usuario = (nombre_usuario or '').strip()
    if not nombre_usuario or not clave:
        return None
    ahora = timezone.now()
    id_hash = sha256(nombre_usuario.lower())
    origen_hash = sha256(ip) if ip else None
    fallos = IntentoAcceso.objects.filter(resultado__in=('INCORRECTO', 'BLOQUEADO'),
                                          ocurrido_en__gt=ahora - VENTANA_INTENTOS)
    if origen_hash is not None and (
            fallos.filter(identificador_hash=id_hash, origen_hash=origen_hash).count() >= 5
            or fallos.filter(origen_hash=origen_hash).count() >= 20):
        _intento(None, id_hash, 'BLOQUEADO', origen_hash, ahora)
        return 'LIMITE'

    cuenta = CuentaUsuario.objects.select_related('perfil').filter(nombre_usuario=nombre_usuario).first()
    if cuenta is None:
        claves.simular_verificacion(clave)
        _intento(None, id_hash, 'INCORRECTO', origen_hash, ahora)
        return None
    if not claves.verificar(clave, cuenta.hash_clave):
        _intento(cuenta.id, id_hash, 'INCORRECTO', origen_hash, ahora)
        return None
    # Mismo resultado que una clave equivocada: la consola no confirma qué cuentas existen ni su rol.
    if not cuenta_disponible(cuenta, ahora) or not es_administrador(cuenta.id):
        _intento(cuenta.id, id_hash, 'BLOQUEADO', origen_hash, ahora)
        return None
    _intento(cuenta.id, id_hash, 'CORRECTO', origen_hash, ahora)

    token = secrets.token_urlsafe(32)
    with transaction.atomic():
        revocar_sesiones(cuenta.id, ahora)
        sesion = SesionUsuario.objects.create(usuario_id=cuenta.id, token_hash=sha256(token), cliente='WEB_ADMIN',
                                              creada_en=ahora, actividad_en=ahora, expira_en=ahora + DURACION)
    return token, Administrador(cuenta.id, cuenta.nombre_usuario, nombre_visible(cuenta), permisos_de(cuenta.id),
                                sesion.id)


def revocar_sesiones(usuario_id, ahora=None):
    return SesionUsuario.objects.filter(usuario_id=usuario_id, revocada_en__isnull=True).update(
        revocada_en=ahora or timezone.now())


def cerrar(administrador):
    SesionUsuario.objects.filter(id=administrador.sesion_id, revocada_en__isnull=True).update(
        revocada_en=timezone.now())


def resolver(token):
    if not token or len(token) > 128:
        return None
    ahora = timezone.now()
    sesion = (SesionUsuario.objects.select_related('usuario', 'usuario__perfil')
              .filter(token_hash=sha256(token), revocada_en__isnull=True, expira_en__gt=ahora,
                      cliente='WEB_ADMIN').first())
    if sesion is None or not cuenta_disponible(sesion.usuario, ahora) or not es_administrador(sesion.usuario_id):
        return None
    if sesion.actividad_en <= ahora - _ACTIVIDAD_CADA:
        SesionUsuario.objects.filter(id=sesion.id).update(actividad_en=ahora)
    cuenta = sesion.usuario
    return Administrador(cuenta.id, cuenta.nombre_usuario, nombre_visible(cuenta), permisos_de(cuenta.id), sesion.id)


def poner_cookie(respuesta, request, token):
    respuesta.set_cookie(COOKIE, token, max_age=int(DURACION.total_seconds()), path=RUTA_COOKIE,
                         secure=request.is_secure(), httponly=True, samesite='Strict')


def borrar_cookie(respuesta):
    respuesta.delete_cookie(COOKIE, path=RUTA_COOKIE, samesite='Strict')


class SesionAdministradorMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        request.administrador = None
        if request.path.startswith('/api/admin/'):
            request.administrador = resolver(request.COOKIES.get(COOKIE))
        respuesta = self.get_response(request)
        respuesta.headers.setdefault('Cache-Control', 'no-store')
        return respuesta


def requiere_permiso(permiso):
    def decorador(vista):
        @wraps(vista)
        def envoltura(request, *args, **kwargs):
            if request.administrador is None:
                return json_error('Tu sesión de administración terminó. Vuelve a ingresar.', 401)
            if not request.administrador.puede(permiso):
                return json_error('Tu rol no tiene permiso para esta acción.', 403)
            return vista(request, *args, **kwargs)
        return envoltura
    return decorador
