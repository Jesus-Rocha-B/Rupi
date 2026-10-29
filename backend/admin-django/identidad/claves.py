"""Contraseñas compatibles con PasswordService de Spring Boot: BCrypt ($2a$, $2b$, $2y$) y pbkdf2_sha256 de Django."""
import base64
import hashlib
import hmac
import secrets

import bcrypt

# Hash de una clave que no existe: iguala el tiempo de respuesta cuando la cuenta no existe.
_HASH_SIMULADO = bcrypt.hashpw(b'rupi-simulacion', bcrypt.gensalt(rounds=12))

# Palabras peruanas cortas y sin tildes, fáciles de dictar a una niña o un niño de primaria.
_PALABRAS = (
    'alpaca', 'andes', 'cacao', 'camote', 'canoa', 'chala', 'choclo', 'condor', 'cuy', 'yuca',
    'garua', 'huaca', 'inca', 'lago', 'llama', 'lucuma', 'maiz', 'mango', 'molle', 'nevado',
    'olluco', 'oca', 'palta', 'pampa', 'papa', 'puma', 'puna', 'quena', 'quinua', 'rio',
    'selva', 'sol', 'taya', 'totora', 'tuna', 'vicuna', 'zampona', 'caoba', 'cumbre', 'charango',
    'paiche', 'pisco', 'colibri', 'gallito', 'chirimoya', 'aguaje', 'camu', 'tambo', 'ichu', 'kiwicha',
)

LONGITUD_MINIMA = 8
# BCrypt solo considera los primeros 72 bytes; se limita antes para que dos claves distintas no coincidan.
LONGITUD_MAXIMA = 64


def cifrar(clave):
    return bcrypt.hashpw(clave.encode('utf-8'), bcrypt.gensalt(rounds=12)).decode('ascii')


def verificar(clave, hash_guardado):
    if not clave or not hash_guardado:
        return False
    if hash_guardado.startswith(('$2a$', '$2b$', '$2y$')):
        try:
            return bcrypt.checkpw(clave.encode('utf-8'), hash_guardado.encode('ascii'))
        except ValueError:
            return False
    if hash_guardado.startswith('pbkdf2_sha256$'):
        return _verificar_pbkdf2(clave, hash_guardado)
    return False


def simular_verificacion(clave):
    try:
        bcrypt.checkpw((clave or 'rupi').encode('utf-8')[:72], _HASH_SIMULADO)
    except ValueError:
        pass


def _verificar_pbkdf2(clave, hash_guardado):
    partes = hash_guardado.split('$')
    if len(partes) != 4:
        return False
    try:
        iteraciones = int(partes[1])
        esperado = base64.b64decode(partes[3])
    except ValueError:
        return False
    calculado = hashlib.pbkdf2_hmac('sha256', clave.encode('utf-8'), partes[2].encode('utf-8'), iteraciones,
                                    len(esperado))
    return hmac.compare_digest(calculado, esperado)


def generar_clave_temporal():
    """Tres palabras y dos cifras, p. ej. «condor-maiz-totora-48»: unos 24 bits, suficientes para una clave
    de un solo uso que la API protege con el límite de intentos por cuenta y equipo."""
    palabras = [secrets.choice(_PALABRAS) for _ in range(3)]
    return '-'.join(palabras) + f'-{secrets.randbelow(90) + 10}'


def problema_clave_nueva(clave, nombre_usuario):
    """Devuelve el motivo por el que la clave no sirve, o None si se puede usar."""
    if not clave or len(clave) < LONGITUD_MINIMA:
        return f'La contraseña debe tener al menos {LONGITUD_MINIMA} caracteres.'
    if len(clave.encode('utf-8')) > LONGITUD_MAXIMA:
        return f'La contraseña no puede pasar de {LONGITUD_MAXIMA} caracteres.'
    if clave.strip() != clave:
        return 'La contraseña no puede empezar ni terminar con espacios.'
    if nombre_usuario and nombre_usuario.lower() in clave.lower():
        return 'La contraseña no puede contener el nombre de usuario.'
    if len(set(clave)) < 4:
        return 'La contraseña repite demasiado los mismos caracteres.'
    return None
