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

from . import claves, doble_factor, politica_acceso
from .models import CuentaUsuario, DobleFactorUsuario, IntentoAcceso, RolPermiso, SesionUsuario, UsuarioRol
from .respuestas import json_error

COOKIE = 'rupi_admin'
RUTA_COOKIE = '/api/admin'
DURACION = timedelta(hours=8)
ROL_ADMINISTRADOR = 'ADMINISTRADOR'
# Entran a la consola los roles que tienen este permiso (ADMINISTRADOR y SOPORTE desde V32), no un rol fijo.
PERMISO_CONSOLA = 'consola.ingresar'
# Los límites de intentos salen de la política de la consola (HU-58), la misma que aplica StudentSessionService.
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


@dataclass(frozen=True)
class Freno:
    """Intento rechazado antes de probar la clave: por el límite del equipo o por el bloqueo automático (HU-58)."""
    minutos: int
    cuenta_bloqueada: bool


@dataclass(frozen=True)
class Desafio2FA:
    """HU-60: la clave pasó, pero falta configurar o validar el código temporal TOTP antes de abrir sesión."""
    configurado: bool
    desafio: str
    usuario: str
    otpauth_uri: str | None = None
    secreto_base32: str | None = None


@dataclass(frozen=True)
class Error2FA:
    """HU-60: el código temporal 2FA es inválido o venció."""
    mensaje: str = 'El código de doble factor (2FA) es incorrecto o venció.'


def _crear_sesion_admin(cuenta, permisos, ahora):
    token = secrets.token_urlsafe(32)
    with transaction.atomic():
        revocar_sesiones(cuenta.id, ahora)
        sesion = SesionUsuario.objects.create(usuario_id=cuenta.id, token_hash=sha256(token), cliente='WEB_ADMIN',
                                              creada_en=ahora, actividad_en=ahora, expira_en=ahora + DURACION)
    return token, Administrador(cuenta.id, cuenta.nombre_usuario, nombre_visible(cuenta), permisos, sesion.id)


def _verificar_freno(nombre_usuario, ip, ahora):
    id_hash = sha256(nombre_usuario.lower())
    origen_hash = sha256(ip) if ip else None
    politica = politica_acceso.vigente()
    fallos = IntentoAcceso.objects.filter(resultado__in=('INCORRECTO', 'BLOQUEADO'),
                                          ocurrido_en__gt=ahora - politica.ventana)
    reinicio = (CuentaUsuario.objects.filter(nombre_usuario=nombre_usuario)
                .values_list('intentos_reiniciados_en', flat=True).first())
    de_la_cuenta = fallos.filter(identificador_hash=id_hash, origen_hash=origen_hash)
    if reinicio is not None:
        de_la_cuenta = de_la_cuenta.filter(ocurrido_en__gt=reinicio)
    if origen_hash is not None and (
            de_la_cuenta.count() >= politica.max_fallos_equipo_cuenta
            or fallos.filter(origen_hash=origen_hash).count() >= politica.max_fallos_equipo):
        _intento(None, id_hash, 'BLOQUEADO', origen_hash, ahora)
        return id_hash, origen_hash, Freno(politica.ventana_minutos, False)
    hasta = politica_acceso.bloqueo_de(nombre_usuario, politica, ahora)
    if hasta is not None:
        _intento(None, id_hash, 'BLOQUEADO', origen_hash, ahora)
        return id_hash, origen_hash, Freno(politica_acceso.minutos_restantes(hasta, ahora), True)
    return id_hash, origen_hash, None


def iniciar(nombre_usuario, clave, ip, codigo_totp=None):
    """Devuelve (token, administrador), Desafio2FA, Error2FA, Freno o None si las credenciales fallan."""
    nombre_usuario = (nombre_usuario or '').strip()
    if not nombre_usuario or not clave:
        return None
    ahora = timezone.now()
    id_hash, origen_hash, freno = _verificar_freno(nombre_usuario, ip, ahora)
    if freno is not None:
        return freno

    cuenta = CuentaUsuario.objects.select_related('perfil').filter(nombre_usuario=nombre_usuario).first()
    if cuenta is None:
        claves.simular_verificacion(clave)
        _intento(None, id_hash, 'INCORRECTO', origen_hash, ahora)
        return None
    # Solo vale la contraseña guardada: ninguna clave fija abre la consola, ni siquiera para admin.rupi.
    if not claves.verificar(clave, cuenta.hash_clave):
        _intento(cuenta.id, id_hash, 'INCORRECTO', origen_hash, ahora)
        return None
    # Mismo resultado que una clave equivocada: la consola no confirma qué cuentas existen ni su rol.
    permisos = permisos_de(cuenta.id)
    if not cuenta_disponible(cuenta, ahora) or PERMISO_CONSOLA not in permisos:
        _intento(cuenta.id, id_hash, 'BLOQUEADO', origen_hash, ahora)
        return None

    # HU-60: 2FA obligatorio para cuentas de administración, soporte o consola.
    registro_2fa = DobleFactorUsuario.objects.filter(usuario_id=cuenta.id).first()
    if registro_2fa is None or not registro_2fa.verificado:
        registro_2fa = doble_factor.preparar_enrolamiento(cuenta)
        return Desafio2FA(
            configurado=False,
            desafio=doble_factor.crear_desafio(cuenta.id, 'admin_login'),
            usuario=cuenta.nombre_usuario,
            otpauth_uri=doble_factor.construir_uri_otpauth(cuenta.nombre_usuario, registro_2fa.secreto_base32),
            secreto_base32=registro_2fa.secreto_base32,
        )

    if codigo_totp is None or not str(codigo_totp).strip():
        return Desafio2FA(
            configurado=True,
            desafio=doble_factor.crear_desafio(cuenta.id, 'admin_login'),
            usuario=cuenta.nombre_usuario,
        )

    if not doble_factor.validar_inicio_sesion(cuenta, codigo_totp):
        _intento(cuenta.id, id_hash, 'INCORRECTO', origen_hash, ahora)
        return Error2FA()

    _intento(cuenta.id, id_hash, 'CORRECTO', origen_hash, ahora)
    return _crear_sesion_admin(cuenta, permisos, ahora)


def completar_2fa(desafio_token, codigo, ip):
    """HU-60: verifica el código TOTP (activación inicial o login diario) y emite la sesión WEB_ADMIN."""
    cuenta_id = doble_factor.leer_desafio(desafio_token, 'admin_login')
    cuenta = CuentaUsuario.objects.select_related('perfil').filter(id=cuenta_id).first()
    if cuenta is None:
        return None
    ahora = timezone.now()
    id_hash, origen_hash, freno = _verificar_freno(cuenta.nombre_usuario, ip, ahora)
    if freno is not None:
        return freno
    permisos = permisos_de(cuenta.id)
    if not cuenta_disponible(cuenta, ahora) or PERMISO_CONSOLA not in permisos:
        _intento(cuenta.id, id_hash, 'BLOQUEADO', origen_hash, ahora)
        return None

    registro = DobleFactorUsuario.objects.filter(usuario_id=cuenta.id).first()
    if registro is None or not registro.verificado:
        ok, _ = doble_factor.confirmar_enrolamiento(cuenta, codigo, actor=cuenta)
    else:
        ok = doble_factor.validar_inicio_sesion(cuenta, codigo)
    if not ok:
        _intento(cuenta.id, id_hash, 'INCORRECTO', origen_hash, ahora)
        return Error2FA()

    _intento(cuenta.id, id_hash, 'CORRECTO', origen_hash, ahora)
    return _crear_sesion_admin(cuenta, permisos, ahora)


def iniciar_enrolamiento_docente(nombre_usuario, clave, ip):
    """HU-60: valida clave de docente antes de entregar su QR inicial en el portal escolar."""
    nombre_usuario = (nombre_usuario or '').strip()
    if not nombre_usuario or not clave:
        return None
    ahora = timezone.now()
    id_hash, origen_hash, freno = _verificar_freno(nombre_usuario, ip, ahora)
    if freno is not None:
        return freno
    cuenta = CuentaUsuario.objects.filter(nombre_usuario=nombre_usuario).first()
    if cuenta is None:
        claves.simular_verificacion(clave)
        _intento(None, id_hash, 'INCORRECTO', origen_hash, ahora)
        return None
    if not claves.verificar(clave, cuenta.hash_clave):
        _intento(cuenta.id, id_hash, 'INCORRECTO', origen_hash, ahora)
        return None
    if not cuenta_disponible(cuenta, ahora) or not doble_factor.requiere_doble_factor(cuenta.id):
        _intento(cuenta.id, id_hash, 'BLOQUEADO', origen_hash, ahora)
        return None
    registro = DobleFactorUsuario.objects.filter(usuario_id=cuenta.id).first()
    if registro is not None and registro.verificado:
        return {'yaConfigurado': True, 'usuario': cuenta.nombre_usuario}
    registro = doble_factor.preparar_enrolamiento(cuenta)
    return {
        'yaConfigurado': False,
        'desafio': doble_factor.crear_desafio(cuenta.id, 'docente_enrolar'),
        'usuario': cuenta.nombre_usuario,
        'otpauthUri': doble_factor.construir_uri_otpauth(cuenta.nombre_usuario, registro.secreto_base32),
        'secretoBase32': registro.secreto_base32,
    }


def revocar_sesiones(usuario_id, ahora=None, excepto=None):
    sesiones = SesionUsuario.objects.filter(usuario_id=usuario_id, revocada_en__isnull=True)
    if excepto:
        sesiones = sesiones.exclude(id=excepto)
    return sesiones.update(revocada_en=ahora or timezone.now())


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
    if sesion is None or not cuenta_disponible(sesion.usuario, ahora):
        return None
    # Los permisos se leen en cada petición: retirar un rol corta el acceso sin esperar a que expire la sesión.
    permisos = permisos_de(sesion.usuario_id)
    if PERMISO_CONSOLA not in permisos:
        return None
    if sesion.actividad_en <= ahora - _ACTIVIDAD_CADA:
        SesionUsuario.objects.filter(id=sesion.id).update(actividad_en=ahora)
    cuenta = sesion.usuario
    return Administrador(cuenta.id, cuenta.nombre_usuario, nombre_visible(cuenta), permisos, sesion.id)


_LUGARES = {'WEB_ADMIN': 'en otra consola de administración', 'WEB_USUARIO': 'en la web de Rupi',
            'MOVIL': 'en el celular'}


def motivo_cierre(token):
    """HU-43: mismo criterio que StudentSessionService.sessionEnd. Si la sesión se revocó justo cuando se abrió otra
    de la misma cuenta, se explica dónde; si no, se pide ingresar de nuevo."""
    if not token or len(token) > 128:
        return None
    sesion = SesionUsuario.objects.filter(token_hash=sha256(token)).first()
    if sesion is None:
        return None
    if sesion.revocada_en is not None:
        nueva = (SesionUsuario.objects.filter(usuario_id=sesion.usuario_id, creada_en__gt=sesion.creada_en,
                                              creada_en__gte=sesion.revocada_en - timedelta(seconds=1),
                                              creada_en__lte=sesion.revocada_en + timedelta(seconds=5))
                 .exclude(id=sesion.id).order_by('creada_en').first())
        if nueva is not None:
            return (f'Tu cuenta se abrió {_LUGARES.get(nueva.cliente, "en otro lugar")} y Rupi permite una sola '
                    'sesión por cuenta: aquí se cerró la consola.')
    return None


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


def sin_sesion(request):
    motivo = motivo_cierre(request.COOKIES.get(COOKIE))
    return json_error(motivo or 'Tu sesión de administración terminó. Vuelve a ingresar.', 401)


def requiere_permiso(permiso):
    def decorador(vista):
        @wraps(vista)
        def envoltura(request, *args, **kwargs):
            if request.administrador is None:
                return sin_sesion(request)
            if not request.administrador.puede(permiso):
                return json_error('Tu rol no tiene permiso para esta acción.', 403)
            return vista(request, *args, **kwargs)
        return envoltura
    return decorador
