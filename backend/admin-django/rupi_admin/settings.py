"""Configuración de la API administrativa de Rupi.

Django es el único escritor de identidad_* y curriculo_* (AGENTS.md). No usa las apps de Django que crean
tablas propias (auth, sessions, admin, contenttypes): las cuentas, los roles y las sesiones son las tablas
identidad_* del esquema compartido, y los modelos no son gestionados por migraciones de Django.
"""
import os
import secrets
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent


def _lista(nombre, defecto):
    valor = os.environ.get(nombre, '')
    return [item.strip() for item in valor.split(',') if item.strip()] or defecto


DEBUG = os.environ.get('RUPI_ADMIN_DEBUG', '').lower() in {'1', 'true', 'si'}
# No se firma nada persistente (las sesiones viven en identidad_sesion_usuario), así que una clave por
# proceso basta en desarrollo. En despliegue se fija para que varias instancias respondan igual.
SECRET_KEY = os.environ.get('RUPI_ADMIN_SECRET_KEY') or secrets.token_urlsafe(50)
ALLOWED_HOSTS = _lista('RUPI_ADMIN_ALLOWED_HOSTS', ['127.0.0.1', 'localhost'])

# Orígenes que pueden enviar POST, PATCH, PUT o DELETE además del propio servidor (la web en Vite).
RUPI_ORIGENES_PERMITIDOS = _lista('RUPI_ADMIN_ORIGINS', ['http://localhost:5173', 'http://127.0.0.1:5173'])

INSTALLED_APPS = [
    'identidad',
    'operaciones',
]

MIDDLEWARE = [
    'django.middleware.security.SecurityMiddleware',
    'identidad.sesion.SesionAdministradorMiddleware',
]

ROOT_URLCONF = 'rupi_admin.urls'
WSGI_APPLICATION = 'rupi_admin.wsgi.application'
APPEND_SLASH = False

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.mysql',
        'NAME': os.environ.get('RUPI_ADMIN_DB_NAME', 'rupi'),
        'HOST': os.environ.get('RUPI_ADMIN_DB_HOST', '127.0.0.1'),
        'PORT': os.environ.get('RUPI_ADMIN_DB_PORT', '3306'),
        'USER': os.environ.get('RUPI_ADMIN_DB_USERNAME', 'rupi_admin'),
        'PASSWORD': os.environ.get('RUPI_ADMIN_DB_PASSWORD', ''),
        'CONN_MAX_AGE': 60,
        'OPTIONS': {'charset': 'utf8mb4'},
    }
}

# Ningún modelo se crea ni altera desde Django: el esquema se versiona en database/mysql/migrations.
MIGRATION_MODULES = {app: None for app in INSTALLED_APPS}
DEFAULT_AUTO_FIELD = 'django.db.models.BigAutoField'

LANGUAGE_CODE = 'es-pe'
TIME_ZONE = 'America/Lima'
USE_I18N = True
USE_TZ = True

SECURE_CONTENT_TYPE_NOSNIFF = True
SECURE_REFERRER_POLICY = 'strict-origin-when-cross-origin'
X_FRAME_OPTIONS = 'DENY'

# Salud de la plataforma (HU-45). La API Spring Boot solo entrega sus tiempos a quien presente este token.
RUPI_API_URL = os.environ.get('RUPI_API_URL', 'http://127.0.0.1:8081').rstrip('/')
RUPI_IA_URL = os.environ.get('RUPI_IA_URL', 'http://127.0.0.1:8000').rstrip('/')
RUPI_MONITOR_TOKEN = os.environ.get('RUPI_MONITOR_TOKEN', '')

LOGGING = {
    'version': 1,
    'disable_existing_loggers': False,
    'handlers': {'consola': {'class': 'logging.StreamHandler'}},
    'root': {'handlers': ['consola'], 'level': 'WARNING'},
}
