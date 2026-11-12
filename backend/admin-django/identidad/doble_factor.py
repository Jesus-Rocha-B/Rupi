"""Autenticación de doble factor TOTP obligatoria (HU-60).

Cubre todas las cuentas con roles ADMINISTRADOR, SOPORTE o DOCENTE.
Django es el único escritor de identidad_doble_factor (AGENTS.md); Spring Boot
solo la consulta al autenticar docentes en el portal web y celular.
"""
import base64
import hashlib
import hmac
import secrets
import struct
import time
from urllib.parse import quote

from django.core.signing import BadSignature, SignatureExpired, TimestampSigner
from django.db import transaction
from django.utils import timezone

from operaciones import auditoria

from .models import CuentaUsuario, DobleFactorUsuario, SesionUsuario, UsuarioRol
from .respuestas import ErrorDePeticion

ROLES_OBLIGATORIOS = ('ADMINISTRADOR', 'SOPORTE', 'DOCENTE')
PERIODO_SEGUNDOS = 30
DIGITOS_TOTP = 6
VENTANA_PASOS = 1
TTL_DESAFIO_SEGUNDOS = 300
EMISOR_TOTP = 'Rupi'

_SIGNER = TimestampSigner(salt='rupi.identidad.doble_factor.desafio')


def _limpiar_base32(secreto):
    texto = (secreto or '').strip().replace(' ', '').upper()
    relleno = (-len(texto)) % 8
    return texto + ('=' * relleno)


def generar_secreto():
    """Genera un secreto aleatorio de 160 bits codificado en Base32 sin relleno."""
    return base64.b32encode(secrets.token_bytes(20)).decode('ascii').rstrip('=')


def construir_uri_otpauth(nombre_usuario, secreto_base32, emisor=EMISOR_TOTP):
    etiqueta = f'{emisor}:{nombre_usuario}'
    return (
        f'otpauth://totp/{quote(etiqueta, safe=":")}'
        f'?secret={quote(secreto_base32, safe="")}'
        f'&issuer={quote(emisor, safe="")}'
        f'&algorithm=SHA1&digits={DIGITOS_TOTP}&period={PERIODO_SEGUNDOS}'
    )


def paso_actual(instante=None, periodo=PERIODO_SEGUNDOS):
    ts = int(time.time()) if instante is None else int(instante)
    return ts // periodo


def codigo_totp(secreto_base32, instante=None, paso=None, periodo=PERIODO_SEGUNDOS, digitos=DIGITOS_TOTP):
    """Calcula el token RFC 6238 (HMAC-SHA1) para un instante o paso dado."""
    clave = base64.b32decode(_limpiar_base32(secreto_base32), casefold=True)
    contador = paso_actual(instante, periodo) if paso is None else int(paso)
    mensaje = struct.pack('>Q', contador)
    resumen = hmac.new(clave, mensaje, hashlib.sha1).digest()
    desplazamiento = resumen[-1] & 0x0F
    valor = struct.unpack('>I', resumen[desplazamiento:desplazamiento + 4])[0] & 0x7FFFFFFF
    return str(valor % (10 ** digitos)).zfill(digitos)


def normalizar_codigo(codigo):
    texto = ''.join(ch for ch in str(codigo or '').strip() if ch.isdigit())
    return texto if len(texto) == DIGITOS_TOTP else None


def verificar_codigo(secreto_base32, codigo, ultimo_paso_usado=None, instante=None, ventana=VENTANA_PASOS):
    """Valida el código de 6 dígitos en ventana ±1 y bloquea reutilización del mismo paso."""
    limpio = normalizar_codigo(codigo)
    if limpio is None or not secreto_base32:
        return False, None
    try:
        base64.b32decode(_limpiar_base32(secreto_base32), casefold=True)
    except Exception:
        return False, None

    centro = paso_actual(instante)
    for delta in range(-ventana, ventana + 1):
        candidato_paso = centro + delta
        if candidato_paso < 0:
            continue
        esperado = codigo_totp(secreto_base32, paso=candidato_paso)
        if hmac.compare_digest(limpio, esperado):
            if ultimo_paso_usado is not None and candidato_paso <= int(ultimo_paso_usado):
                return False, None
            return True, candidato_paso
    return False, None


def requiere_doble_factor(usuario_id):
    return UsuarioRol.objects.filter(usuario_id=usuario_id, rol__codigo__in=ROLES_OBLIGATORIOS).exists()


def crear_desafio(usuario_id, proposito):
    return _SIGNER.sign_object({'u': str(usuario_id), 'p': str(proposito)})


def leer_desafio(token, proposito):
    if not token or not isinstance(token, str):
        raise ErrorDePeticion('La verificación temporal expiró. Vuelve a iniciar sesión.')
    try:
        datos = _SIGNER.unsign_object(token, max_age=TTL_DESAFIO_SEGUNDOS)
    except SignatureExpired:
        raise ErrorDePeticion('Tu verificación 2FA venció. Vuelve a ingresar usuario y contraseña.')
    except BadSignature:
        raise ErrorDePeticion('La verificación 2FA no es válida. Vuelve a intentarlo.')
    if not isinstance(datos, dict) or datos.get('p') != proposito or not datos.get('u'):
        raise ErrorDePeticion('La verificación 2FA no corresponde a este acceso.')
    return datos['u']


@transaction.atomic
def preparar_enrolamiento(cuenta, forzar_rotacion=False, actor=None):
    ahora = timezone.now()
    registro = DobleFactorUsuario.objects.filter(usuario_id=cuenta.id).first()
    if registro is None:
        registro = DobleFactorUsuario.objects.create(
            usuario_id=cuenta.id,
            secreto_base32=generar_secreto(),
            verificado=False,
            ultimo_paso_usado=None,
            activado_en=None,
            actualizado_en=ahora,
        )
    elif forzar_rotacion or not registro.verificado:
        if forzar_rotacion:
            registro.secreto_base32 = generar_secreto()
            registro.verificado = False
            registro.ultimo_paso_usado = None
            registro.activado_en = None
            registro.actualizado_en = ahora
            registro.save()
    if forzar_rotacion and actor is not None:
        auditoria.registrar(actor.id, '2FA_ROTACION_QR', cuenta.id)
    return registro


@transaction.atomic
def confirmar_enrolamiento(cuenta, codigo, actor=None):
    registro = DobleFactorUsuario.objects.filter(usuario_id=cuenta.id).first()
    if registro is None:
        raise ErrorDePeticion('Primero genera tu código QR de doble factor.')
    ahora = timezone.now()
    ok, paso = verificar_codigo(registro.secreto_base32, codigo, ultimo_paso_usado=None)
    if not ok:
        return False, None
    registro.verificado = True
    registro.ultimo_paso_usado = paso
    registro.activado_en = ahora
    registro.actualizado_en = ahora
    registro.save()
    actor_id = actor.id if actor is not None else cuenta.id
    auditoria.registrar(actor_id, '2FA_ACTIVAR', cuenta.id)
    return True, registro


@transaction.atomic
def validar_inicio_sesion(cuenta, codigo):
    registro = DobleFactorUsuario.objects.filter(usuario_id=cuenta.id).first()
    if registro is None or not registro.verificado:
        return False
    ok, paso = verificar_codigo(registro.secreto_base32, codigo, ultimo_paso_usado=registro.ultimo_paso_usado)
    if not ok:
        return False
    registro.ultimo_paso_usado = paso
    registro.actualizado_en = timezone.now()
    registro.save()
    return True


def _payload_qr(cuenta, registro, incluir_secreto=False):
    datos = {
        'requerido': requiere_doble_factor(cuenta.id),
        'verificado': bool(registro and registro.verificado),
        'activadoEn': registro.activado_en.isoformat() if registro and registro.activado_en else None,
        'actualizadoEn': registro.actualizado_en.isoformat() if registro and registro.actualizado_en else None,
    }
    if incluir_secreto and registro is not None:
        datos['secretoBase32'] = registro.secreto_base32
        datos['otpauthUri'] = construir_uri_otpauth(cuenta.nombre_usuario, registro.secreto_base32)
    return datos


def resumen_propio(administrador):
    cuenta = CuentaUsuario.objects.filter(id=administrador.id).first()
    if cuenta is None:
        raise ErrorDePeticion('No encontramos tu cuenta.', 404)
    registro = DobleFactorUsuario.objects.filter(usuario_id=cuenta.id).first()
    if registro is None:
        registro = preparar_enrolamiento(cuenta, forzar_rotacion=False, actor=administrador)
    return _payload_qr(cuenta, registro, incluir_secreto=not registro.verificado)


def rotar_propio(administrador):
    cuenta = CuentaUsuario.objects.filter(id=administrador.id).first()
    if cuenta is None:
        raise ErrorDePeticion('No encontramos tu cuenta.', 404)
    registro = preparar_enrolamiento(cuenta, forzar_rotacion=True, actor=administrador)
    return _payload_qr(cuenta, registro, incluir_secreto=True)


def confirmar_propio(administrador, codigo):
    cuenta = CuentaUsuario.objects.filter(id=administrador.id).first()
    if cuenta is None:
        raise ErrorDePeticion('No encontramos tu cuenta.', 404)
    ok, registro = confirmar_enrolamiento(cuenta, codigo, actor=administrador)
    if not ok:
        raise ErrorDePeticion('El código temporal de 6 dígitos es incorrecto o ya venció.')
    return _payload_qr(cuenta, registro, incluir_secreto=False)


def listar_cuentas_obligatorias():
    asignaciones = (
        UsuarioRol.objects.select_related('usuario', 'usuario__perfil', 'rol')
        .filter(rol__codigo__in=ROLES_OBLIGATORIOS)
        .order_by('usuario__nombre_usuario', 'rol__codigo')
    )
    por_usuario = {}
    for asignacion in asignaciones:
        u = asignacion.usuario
        entrada = por_usuario.setdefault(u.id, {
            'id': u.id,
            'usuario': u.nombre_usuario,
            'nombre': _nombre(u),
            'estadoCuenta': u.estado,
            'roles': [],
        })
        if asignacion.rol.codigo not in entrada['roles']:
            entrada['roles'].append(asignacion.rol.codigo)

    registros = {
        df.usuario_id: df
        for df in DobleFactorUsuario.objects.filter(usuario_id__in=list(por_usuario.keys()))
    }
    cuentas = []
    for uid, entrada in por_usuario.items():
        df = registros.get(uid)
        entrada['verificado'] = bool(df and df.verificado)
        entrada['pendienteQr'] = bool(df is None or not df.verificado)
        entrada['activadoEn'] = df.activado_en.isoformat() if df and df.activado_en else None
        cuentas.append(entrada)
    return cuentas


@transaction.atomic
def reiniciar_cuenta(cuenta_id, administrador):
    cuenta = CuentaUsuario.objects.filter(id=cuenta_id).first()
    if cuenta is None:
        raise ErrorDePeticion('No encontramos esa cuenta.', 404)
    if not requiere_doble_factor(cuenta.id):
        raise ErrorDePeticion('Solo las cuentas con rol administrativo, soporte o docente requieren 2FA obligatorio.')
    ahora = timezone.now()
    registro = preparar_enrolamiento(cuenta, forzar_rotacion=True, actor=None)
    # Si se reinicia el 2FA de otra cuenta o de la propia, se cierran sus sesiones activas para exigir el nuevo QR.
    if cuenta.id != administrador.id:
        SesionUsuario.objects.filter(usuario_id=cuenta.id, revocada_en__isnull=True).update(revocada_en=ahora)
    auditoria.registrar(administrador.id, '2FA_REINICIAR', cuenta.id)
    return {
        'cuentaId': cuenta.id,
        'usuario': cuenta.nombre_usuario,
        'verificado': False,
        'pendienteQr': True,
        'secretoBase32': registro.secreto_base32,
        'otpauthUri': construir_uri_otpauth(cuenta.nombre_usuario, registro.secreto_base32),
    }


def _nombre(cuenta):
    perfil = getattr(cuenta, 'perfil', None)
    if perfil is None:
        return cuenta.nombre_usuario
    completo = f'{perfil.nombre or ""} {perfil.apellido or ""}'.strip()
    return perfil.nombre_preferido or completo or cuenta.nombre_usuario
