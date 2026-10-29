"""Salud de los servidores y tiempos de respuesta (HU-45).

La consola junta en una sola respuesta lo que necesita la administración para anticipar fallas antes de que
afecten a las clases: si la base, la API y el servicio de IA responden, cuánto tardan y qué rutas están lentas.
Nada se guarda: cada consulta mide en el momento.
"""
import json
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor

from django.conf import settings
from django.db import connection
from django.utils import timezone

# Umbrales pensados para una clase: más de un segundo ya se nota en una tableta escolar.
LATENCIA_LENTA_MS = 800
P95_LENTO_MS = 1500
ERRORES_ALERTA = 0.05
TIEMPO_ESPERA_S = 3

OK, LENTO, CAIDO, SIN_CONFIGURAR = 'OK', 'LENTO', 'CAIDO', 'SIN_CONFIGURAR'
_INICIO = time.monotonic()


def _ms(desde):
    return round((time.monotonic() - desde) * 1000, 1)


def estado_por_latencia(latencia_ms, p95_ms=0.0, errores=0, peticiones=0):
    if latencia_ms >= LATENCIA_LENTA_MS or p95_ms >= P95_LENTO_MS:
        return LENTO
    if peticiones and errores / peticiones >= ERRORES_ALERTA:
        return LENTO
    return OK


def estado_general(servicios):
    """FALLA si cae la base o la API (sin ellas no hay clases); ATENCION si algo está lento o falta la IA."""
    por_clave = {s['clave']: s['estado'] for s in servicios}
    if CAIDO in (por_clave.get('base'), por_clave.get('api')):
        return 'FALLA'
    if any(estado in (LENTO, CAIDO, SIN_CONFIGURAR) for estado in por_clave.values()):
        return 'ATENCION'
    return 'OK'


def _base_de_datos():
    inicio = time.monotonic()
    try:
        with connection.cursor() as cursor:
            cursor.execute('SELECT 1')
            cursor.fetchone()
            latencia = _ms(inicio)
            cursor.execute('SELECT VERSION()')
            version = cursor.fetchone()[0]
            cursor.execute("SHOW GLOBAL STATUS LIKE 'Threads_connected'")
            fila = cursor.fetchone()
        return {'clave': 'base', 'nombre': 'Base de datos MySQL', 'estado': estado_por_latencia(latencia),
                'latenciaMs': latencia, 'detalle': {'version': version, 'conexiones': int(fila[1]) if fila else None}}
    except Exception:
        return {'clave': 'base', 'nombre': 'Base de datos MySQL', 'estado': CAIDO, 'latenciaMs': _ms(inicio),
                'detalle': {'error': 'Revisa que MySQL esté encendido.'}}


def _pedir_json(url, cabeceras=None):
    peticion = urllib.request.Request(url, headers=cabeceras or {})
    with urllib.request.urlopen(peticion, timeout=TIEMPO_ESPERA_S) as respuesta:
        return json.loads(respuesta.read(512_000) or b'null')


def _api():
    nombre = 'API de Rupi (Spring Boot)'
    if not settings.RUPI_MONITOR_TOKEN:
        return {'clave': 'api', 'nombre': nombre, 'estado': SIN_CONFIGURAR, 'latenciaMs': None,
                'detalle': {'error': 'Falta RUPI_MONITOR_TOKEN en la consola y en la API.'}}
    inicio = time.monotonic()
    try:
        datos = _pedir_json(f'{settings.RUPI_API_URL}/api/v1/monitor/health',
                            {'X-Rupi-Monitor-Token': settings.RUPI_MONITOR_TOKEN})
    except urllib.error.HTTPError as error:
        mensaje = ('La API rechazó el token del monitor: debe ser el mismo en las dos.' if error.code in (401, 404)
                   else f'La API respondió {error.code}.')
        return {'clave': 'api', 'nombre': nombre, 'estado': SIN_CONFIGURAR if error.code in (401, 404) else CAIDO,
                'latenciaMs': _ms(inicio), 'detalle': {'error': mensaje}}
    except Exception:
        return {'clave': 'api', 'nombre': nombre, 'estado': CAIDO, 'latenciaMs': _ms(inicio),
                'detalle': {'error': 'Las clases no pueden cargar rutas ni guardar avance.'}}
    latencia = _ms(inicio)
    ventana = datos['requests']['lastFifteenMinutes']
    estado = estado_por_latencia(latencia, ventana['p95Ms'], ventana['serverErrors'], ventana['requests'])
    if datos.get('state') != 'UP':
        estado = LENTO
    return {'clave': 'api', 'nombre': nombre, 'estado': estado, 'latenciaMs': latencia,
            'detalle': {'activaDesde': datos['requests']['startedAt'], 'segundosActiva': datos['uptimeSeconds'],
                        'baseDesdeApi': datos['database'], 'memoria': datos['jvm']},
            'peticiones': {'ventana': ventana, 'total': datos['requests']['totalRequests'],
                           'rutasLentas': datos['requests']['slowestRoutes']}}


def _ia():
    nombre = 'Servicio de IA (tutor y cuestionarios)'
    inicio = time.monotonic()
    try:
        datos = _pedir_json(f'{settings.RUPI_IA_URL}/health')
    except Exception:
        return {'clave': 'ia', 'nombre': nombre, 'estado': CAIDO, 'latenciaMs': _ms(inicio),
                'detalle': {'error': 'Las lecciones siguen, pero sin tutor ni preguntas generadas.'}}
    latencia = _ms(inicio)
    # Solo datos no secretos: si hay proveedor configurado, nunca la clave.
    detalle = {k: v for k, v in datos.items() if k in ('status', 'model', 'geminiConfigured')} \
        if isinstance(datos, dict) else {}
    return {'clave': 'ia', 'nombre': nombre, 'estado': estado_por_latencia(latencia), 'latenciaMs': latencia,
            'detalle': detalle}


def revisar():
    consola = {'clave': 'consola', 'nombre': 'Consola de administración (Django)', 'estado': OK, 'latenciaMs': None,
               'detalle': {'segundosActiva': int(time.monotonic() - _INICIO)}}
    with ThreadPoolExecutor(max_workers=2) as hilos:
        api = hilos.submit(_api)
        ia = hilos.submit(_ia)
        base = _base_de_datos()
        servicios = [base, api.result(), ia.result(), consola]
    return {
        'estado': estado_general(servicios),
        'revisadoEn': timezone.now().isoformat(),
        'umbrales': {'latenciaLentaMs': LATENCIA_LENTA_MS, 'p95LentoMs': P95_LENTO_MS,
                     'erroresAlerta': ERRORES_ALERTA},
        'servicios': servicios,
    }
