"""Salud de los servidores y tiempos de respuesta (HU-45).

La consola junta en una sola respuesta lo que necesita la administración para anticipar fallas antes de que
afecten a las clases: si la base, la API y el servicio de IA responden, cuánto tardan, qué rutas están lentas,
el uso de CPU/RAM del servidor y las alertas automáticas preventivas.
Nada se guarda: cada consulta mide en el momento.
"""
import json
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor

try:
    import psutil
except ImportError:
    psutil = None

from django.conf import settings
from django.db import connection
from django.utils import timezone

# Umbrales pensados para una clase: más de un segundo ya se nota en una tableta escolar.
LATENCIA_LENTA_MS = 800
P95_LENTO_MS = 1500
ERRORES_ALERTA = 0.05
CPU_ALERTA_PERCENT = 70.0
CPU_CRITICA_PERCENT = 85.0
MEMORIA_ALERTA_PERCENT = 80.0
MEMORIA_CRITICA_PERCENT = 85.0
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


def evaluar_alertas(servicios):
    """Evalúa condiciones de riesgo, saturación de CPU/RAM o tiempos altos para anticipar fallas."""
    alertas = []
    por_clave = {s['clave']: s for s in servicios}

    # 1. Caídas de servicios críticos
    base = por_clave.get('base')
    if base and base['estado'] == CAIDO:
        alertas.append({
            'id': 'base_caida',
            'nivel': 'CRITICA',
            'servicio': 'base',
            'titulo': 'Base de datos MySQL desconectada',
            'mensaje': 'No hay conexión con la base de datos MySQL. Las clases no pueden cargar rutas ni registrar notas.',
            'impacto': 'Interrupción total de actividades escolares en todas las aulas.',
            'sugerencia': 'Verificar el servicio MySQL en el puerto 3306 e iniciarlo de inmediato.',
        })

    api = por_clave.get('api')
    if api:
        if api['estado'] == CAIDO:
            alertas.append({
                'id': 'api_caida',
                'nivel': 'CRITICA',
                'servicio': 'api',
                'titulo': 'API de Rupi no responde',
                'mensaje': 'El servidor central de Spring Boot está fuera de línea.',
                'impacto': 'Los estudiantes no pueden iniciar sesión ni realizar lecciones.',
                'sugerencia': 'Reiniciar el backend de Spring Boot y verificar .local/api.log.',
            })
        elif api['estado'] == SIN_CONFIGURAR:
            alertas.append({
                'id': 'api_sin_token',
                'nivel': 'ADVERTENCIA',
                'servicio': 'api',
                'titulo': 'Falta sincronizar token de monitoreo en la API',
                'mensaje': 'La consola administrativa no puede consultar la telemetría interna de Spring Boot.',
                'impacto': 'Métricas de tiempo de respuesta y rutas lentas no disponibles.',
                'sugerencia': 'Verificar que RUPI_MONITOR_TOKEN sea idéntico en la consola y la API.',
            })

    ia = por_clave.get('ia')
    if ia and ia['estado'] == CAIDO:
        alertas.append({
            'id': 'ia_caida',
            'nivel': 'ADVERTENCIA',
            'servicio': 'ia',
            'titulo': 'Servicio de Inteligencia Artificial desconectado',
            'mensaje': 'El tutor virtual Rupi y el generador adaptativo de preguntas no responden.',
            'impacto': 'Las lecciones básicas siguen activas, pero sin tutor ni ejercicios generados.',
            'sugerencia': 'Verificar el proceso de FastAPI/Python en el puerto 8000.',
        })

    # 2. Tiempos de respuesta altos
    if base and base.get('latenciaMs') is not None and base['latenciaMs'] >= LATENCIA_LENTA_MS:
        alertas.append({
            'id': 'base_latencia_alta',
            'nivel': 'ADVERTENCIA',
            'servicio': 'base',
            'titulo': f'Latencia alta en base de datos ({base["latenciaMs"]} ms)',
            'mensaje': f'El tiempo de consulta a MySQL superó el umbral pedagógico de {LATENCIA_LENTA_MS} ms.',
            'impacto': 'Posibles demoras al cargar listas de ejercicios o perfiles estudiantiles.',
            'sugerencia': 'Revisar consultas concurrentes o estado de conexiones del motor MySQL.',
        })

    if api and api.get('latenciaMs') is not None and api['latenciaMs'] >= LATENCIA_LENTA_MS:
        alertas.append({
            'id': 'api_latencia_alta',
            'nivel': 'ADVERTENCIA',
            'servicio': 'api',
            'titulo': f'Latencia alta en respuesta de la API ({api["latenciaMs"]} ms)',
            'mensaje': f'La respuesta general de la API superó los {LATENCIA_LENTA_MS} ms.',
            'impacto': 'Respuestas lentas en tabletas y computadoras escolares.',
            'sugerencia': 'Comprobar carga de red y procesos concurrentes en el servidor.',
        })

    # 3. Percentil p95 y Errores 5xx en la API
    if api and 'peticiones' in api:
        ventana = api['peticiones'].get('ventana', {})
        p95 = ventana.get('p95Ms', 0)
        if p95 >= P95_LENTO_MS:
            alertas.append({
                'id': 'api_p95_alto',
                'nivel': 'ADVERTENCIA',
                'servicio': 'api',
                'titulo': f'Tiempo p95 elevado en la API ({p95} ms)',
                'mensaje': f'El 5 % de las peticiones más lentas supera los {P95_LENTO_MS} ms en los últimos 15 min.',
                'impacto': 'Escolares experimentan congelamiento o pausas en medio de actividades interactivas.',
                'sugerencia': 'Consultar la lista de rutas lentas en el panel de telemetría.',
            })
        server_errors = ventana.get('serverErrors', 0)
        requests_count = ventana.get('requests', 0)
        if server_errors > 0 and requests_count > 0 and (server_errors / requests_count) >= ERRORES_ALERTA:
            alertas.append({
                'id': 'api_errores_5xx',
                'nivel': 'CRITICA',
                'servicio': 'api',
                'titulo': f'Tasa elevada de errores 5xx en la API ({server_errors} fallos)',
                'mensaje': 'Más del 5 % de las solicitudes resultaron en excepciones internas del servidor.',
                'impacto': 'Riesgo de pérdida de avance o calificaciones no guardadas de los escolares.',
                'sugerencia': 'Inspeccionar .local/api-error.log de inmediato para corregir la causa raíz.',
            })

    # 4. Saturación de CPU y RAM (fallos inminentes)
    if api and 'detalle' in api:
        sistema_api = api['detalle'].get('sistema')
        if sistema_api and isinstance(sistema_api, dict):
            cpu_api = sistema_api.get('cpuPercent', -1)
            if cpu_api >= CPU_CRITICA_PERCENT:
                alertas.append({
                    'id': 'api_cpu_critica',
                    'nivel': 'CRITICA',
                    'servicio': 'api',
                    'titulo': f'Uso crítico de CPU en servidor ({cpu_api} %)',
                    'mensaje': f'El procesamiento del servidor superó el {CPU_CRITICA_PERCENT} % de capacidad.',
                    'impacto': 'Riesgo inminente de degradación o bloqueo general de solicitudes.',
                    'sugerencia': 'Detener procesos intensivos secundarios o incrementar CPU asignada.',
                })
            elif cpu_api >= CPU_ALERTA_PERCENT:
                alertas.append({
                    'id': 'api_cpu_alerta',
                    'nivel': 'ADVERTENCIA',
                    'servicio': 'api',
                    'titulo': f'Uso elevado de CPU en servidor ({cpu_api} %)',
                    'mensaje': f'El consumo de procesamiento se mantiene por encima del {CPU_ALERTA_PERCENT} %.',
                    'impacto': 'Podría generar lentitud si aumenta la concurrencia escolar.',
                    'sugerencia': 'Monitorear el inicio de clases simultáneas.',
                })

            mem_pct = sistema_api.get('memoryPercent', 0)
            if mem_pct >= MEMORIA_CRITICA_PERCENT:
                alertas.append({
                    'id': 'api_memoria_critica',
                    'nivel': 'CRITICA',
                    'servicio': 'api',
                    'titulo': f'Memoria RAM del servidor al límite ({mem_pct} %)',
                    'mensaje': f'El servidor ha consumido más del {MEMORIA_CRITICA_PERCENT} % de la memoria física disponible.',
                    'impacto': 'Peligro inminente de fallo OutOfMemory y cierre forzado de procesos.',
                    'sugerencia': 'Liberar memoria en desuso o reiniciar la máquina antes del horario lectivo.',
                })
            elif mem_pct >= MEMORIA_ALERTA_PERCENT:
                alertas.append({
                    'id': 'api_memoria_alerta',
                    'nivel': 'ADVERTENCIA',
                    'servicio': 'api',
                    'titulo': f'Consumo alto de memoria RAM ({mem_pct} %)',
                    'mensaje': f'El uso de memoria física superó el {MEMORIA_ALERTA_PERCENT} %.',
                    'impacto': 'El margen para picos de uso escolar es reducido.',
                    'sugerencia': 'Vigilar la acumulación de memoria en la pestaña de salud.',
                })

    consola = por_clave.get('consola')
    if consola and 'detalle' in consola:
        recursos = consola['detalle'].get('recursos')
        if recursos and isinstance(recursos, dict) and not any(a['id'].startswith('api_cpu_') for a in alertas):
            cpu_host = recursos.get('cpuPercent', -1)
            if cpu_host >= CPU_CRITICA_PERCENT:
                alertas.append({
                    'id': 'consola_cpu_critica',
                    'nivel': 'CRITICA',
                    'servicio': 'consola',
                    'titulo': f'Uso crítico de CPU en host ({cpu_host} %)',
                    'mensaje': f'El procesador del host superó el {CPU_CRITICA_PERCENT} % de saturación.',
                    'impacto': 'Riesgo de lentitud en todos los servicios locales.',
                    'sugerencia': 'Identificar procesos con alto consumo en el administrador de tareas.',
                })

    return alertas


def estado_general(servicios, alertas=None):
    """FALLA si cae la base o la API o hay alerta crítica; ATENCION si algo está lento, falta la IA o hay advertencias."""
    por_clave = {s['clave']: s['estado'] for s in servicios}
    if CAIDO in (por_clave.get('base'), por_clave.get('api')):
        return 'FALLA'
    if alertas and any(a['nivel'] == 'CRITICA' for a in alertas):
        return 'FALLA'
    if any(estado in (LENTO, CAIDO, SIN_CONFIGURAR) for estado in por_clave.values()):
        return 'ATENCION'
    if alertas and any(a['nivel'] == 'ADVERTENCIA' for a in alertas):
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
                        'baseDesdeApi': datos['database'], 'memoria': datos['jvm'],
                        'sistema': datos.get('system')},
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
    recursos_consola = None
    if psutil:
        try:
            cpu = psutil.cpu_percent(interval=None)
            mem = psutil.virtual_memory()
            recursos_consola = {
                'cpuPercent': round(float(cpu), 1),
                'memoria': {
                    'usadaMb': round(mem.used / (1024 * 1024)),
                    'totalMb': round(mem.total / (1024 * 1024)),
                    'porcentaje': round(mem.percent, 1),
                },
            }
        except Exception:
            pass

    consola = {'clave': 'consola', 'nombre': 'Consola de administración (Django)', 'estado': OK, 'latenciaMs': None,
               'detalle': {'segundosActiva': int(time.monotonic() - _INICIO), 'recursos': recursos_consola}}
    with ThreadPoolExecutor(max_workers=2) as hilos:
        api = hilos.submit(_api)
        ia = hilos.submit(_ia)
        base = _base_de_datos()
        servicios = [base, api.result(), ia.result(), consola]
    alertas = evaluar_alertas(servicios)
    return {
        'estado': estado_general(servicios, alertas),
        'revisadoEn': timezone.now().isoformat(),
        'umbrales': {
            'latenciaLentaMs': LATENCIA_LENTA_MS,
            'p95LentoMs': P95_LENTO_MS,
            'erroresAlerta': ERRORES_ALERTA,
            'cpuAlertaPercent': CPU_ALERTA_PERCENT,
            'cpuCriticaPercent': CPU_CRITICA_PERCENT,
            'memoriaAlertaPercent': MEMORIA_ALERTA_PERCENT,
            'memoriaCriticaPercent': MEMORIA_CRITICA_PERCENT,
        },
        'alertas': alertas,
        'servicios': servicios,
    }
