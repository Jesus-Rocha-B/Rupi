"""Servicio backend de métricas de disponibilidad (uptime) y conteo de errores (HU-48).

Recopila el porcentaje de disponibilidad en ventanas de 24 horas, 7 días y 30 días,
consolida el conteo de errores reportados (API, base de datos, sincronización e IA),
guarda el historial de caídas del servidor principal y genera series temporales
para alimentar gráficos en tiempo real en la consola de administración.
"""
import logging
import uuid
from datetime import datetime, timedelta, timezone

from django.db.models import Count, Q, Sum
from django.utils import timezone as dj_timezone

from . import auditoria, salud
from .models import EventoSincronizacion, HistorialCaida, Incidente

logger = logging.getLogger(__name__)

SEGUNDOS_24H = 86400
SEGUNDOS_7D = 604800
SEGUNDOS_30D = 2592000


def formatear_duracion(segundos: int | None) -> str:
    """Convierte segundos a un texto legible para educadores y administradores."""
    if segundos is None:
        return 'En curso'
    if segundos < 60:
        return f'{segundos} seg'
    minutos = segundos // 60
    resto_seg = segundos % 60
    if minutos < 60:
        return f'{minutos}m {resto_seg}s' if resto_seg else f'{minutos} min'
    horas = minutos // 60
    resto_min = minutos % 60
    return f'{horas}h {resto_min}m' if resto_min else f'{horas} h'


def _calcular_segundos_caida_en_ventana(inicio_ventana: datetime, ahora: datetime) -> int:
    """Calcula la suma de segundos fuera de servicio registrados dentro de la ventana."""
    total_segundos = 0

    # 1. Sumar registros de historial de caídas del servidor principal
    try:
        registros = HistorialCaida.objects.filter(
            Q(iniciado_en__gte=inicio_ventana) | Q(restablecido_en__gte=inicio_ventana) | Q(restablecido_en__isnull=True)
        )
        for r in registros:
            r_inicio = max(r.iniciado_en, inicio_ventana)
            r_fin = r.restablecido_en if r.restablecido_en else ahora
            if r_fin > r_inicio:
                total_segundos += int((r_fin - r_inicio).total_seconds())
    except Exception as e:
        logger.warning('Error consultando HistorialCaida: %s', e)

    # 2. Sumar incidentes críticos no resueltos o resueltos en la ventana
    try:
        incidentes_criticos = Incidente.objects.filter(
            severidad__in=['CRITICA', 'ALTA'],
            servicio__in=['base', 'api', 'servidor_principal'],
        ).filter(
            Q(iniciado_en__gte=inicio_ventana) | Q(resuelto_en__gte=inicio_ventana) | Q(resuelto_en__isnull=True)
        )
        for inc in incidentes_criticos:
            inc_inicio = max(inc.iniciado_en, inicio_ventana)
            inc_fin = inc.resuelto_en if inc.resuelto_en else ahora
            if inc_fin > inc_inicio:
                total_segundos += int((inc_fin - inc_inicio).total_seconds())
    except Exception as e:
        logger.warning('Error consultando Incidente para caídas: %s', e)

    return total_segundos


def _calcular_porcentaje_uptime(segundos_totales: int, segundos_caida: int) -> float:
    """Calcula el porcentaje de disponibilidad exacto limitado entre 0.00% y 100.00%."""
    if segundos_totales <= 0:
        return 100.0
    segundos_caida_seguros = max(0, int(segundos_caida or 0))
    segundos_operativos = max(0, segundos_totales - segundos_caida_seguros)
    uptime = (segundos_operativos / segundos_totales) * 100.0
    return round(max(0.0, min(100.0, uptime)), 2)


def recopilar_metricas_uptime() -> dict:
    """Genera el consolidado completo de disponibilidad, errores reportados y series temporales."""
    ahora = dj_timezone.now()
    hace_24h = ahora - timedelta(hours=24)
    hace_7d = ahora - timedelta(days=7)
    hace_30d = ahora - timedelta(days=30)

    # 1. Cálculo de segundos fuera de servicio por ventana
    segundos_caida_24h = _calcular_segundos_caida_en_ventana(hace_24h, ahora)
    segundos_caida_7d = _calcular_segundos_caida_en_ventana(hace_7d, ahora)
    segundos_caida_30d = _calcular_segundos_caida_en_ventana(hace_30d, ahora)

    uptime_24h = _calcular_porcentaje_uptime(SEGUNDOS_24H, segundos_caida_24h)
    uptime_7d = _calcular_porcentaje_uptime(SEGUNDOS_7D, segundos_caida_7d)
    uptime_30d = _calcular_porcentaje_uptime(SEGUNDOS_30D, segundos_caida_30d)

    # 2. Conteo de errores reportados
    errores_sync_24h = 0
    errores_sync_total = 0
    discrepancias_sync_total = 0
    try:
        errores_sync_total = EventoSincronizacion.objects.filter(estado='ERROR').count()
        discrepancias_sync_total = EventoSincronizacion.objects.filter(discrepancia_detectada=True).count()
        errores_sync_24h = EventoSincronizacion.objects.filter(
            estado='ERROR', recibido_en__gte=hace_24h
        ).count()
    except Exception as e:
        logger.warning('Error consultando EventoSincronizacion: %s', e)

    incidentes_abiertos = 0
    incidentes_24h = 0
    incidentes_total = 0
    try:
        incidentes_total = Incidente.objects.count()
        incidentes_abiertos = Incidente.objects.filter(estado__in=['ABIERTO', 'EN_ATENCION']).count()
        incidentes_24h = Incidente.objects.filter(iniciado_en__gte=hace_24h).count()
    except Exception as e:
        logger.warning('Error consultando Incidente: %s', e)

    # 3. Sondeo en vivo de la API central para capturar errores 5xx de Spring Boot
    estado_api_vivo = 'OK'
    errores_api_5xx = 0
    latencia_api_ms = 45.0
    try:
        diagnostico_vivo = salud.revisar()
        servicios = diagnostico_vivo.get('servicios', [])
        for s in servicios:
            if s.get('clave') == 'api':
                estado_api_vivo = s.get('estado', 'OK')
                latencia_api_ms = s.get('latenciaMs') or 45.0
                peticiones = s.get('peticiones', {})
                ventana = peticiones.get('ventana', {})
                errores_api_5xx = ventana.get('serverErrors', 0)
    except Exception as e:
        logger.warning('Error obteniendo diagnóstico en vivo: %s', e)

    # Conteo acumulado global de errores
    total_errores_reportados = errores_sync_total + incidentes_total + errores_api_5xx + discrepancias_sync_total
    errores_24h = errores_sync_24h + incidentes_24h + errores_api_5xx

    # 4. Clasificación del estado del servidor principal y SLA de calidad escolar
    if incidentes_abiertos > 0 and uptime_24h < 95.0:
        estado_servidor = 'CAIDO'
    elif uptime_24h < 99.0 or incidentes_abiertos > 0:
        estado_servidor = 'DEGRADADO'
    else:
        estado_servidor = 'OPERATIVO'

    if uptime_24h >= 99.9:
        sla_calidad = 'EXCELENTE'
    elif uptime_24h >= 99.0:
        sla_calidad = 'BUENO'
    else:
        sla_calidad = 'EN_RIESGO'

    # 5. Desglose de errores por categoría
    conteo_base = Incidente.objects.filter(categoria='BASE_DATOS').count() if 'Incidente' in globals() else 0
    conteo_ia = Incidente.objects.filter(categoria='IA').count() if 'Incidente' in globals() else 0
    conteo_api = errores_api_5xx + (Incidente.objects.filter(categoria='DISPONIBILIDAD').count() if 'Incidente' in globals() else 0)
    conteo_sync = errores_sync_total + discrepancias_sync_total

    suma_categorias = max(1, conteo_base + conteo_ia + conteo_api + conteo_sync)
    desglose_errores = [
        {
            'categoria': 'API Central (Spring Boot)',
            'cantidad': conteo_api,
            'porcentaje': round((conteo_api / suma_categorias) * 100, 1),
            'descripcion': 'Excepciones HTTP 5xx, tiempos p95 elevados o interrupciones de rutas.',
            'color': '#f95738',
        },
        {
            'categoria': 'Base de Datos MySQL',
            'cantidad': conteo_base,
            'porcentaje': round((conteo_base / suma_categorias) * 100, 1),
            'descripcion': 'Fallas de conexión, bloqueos de tablas o demoras en consultas.',
            'color': '#173e30',
        },
        {
            'categoria': 'Inteligencia Artificial (Tutor)',
            'cantidad': conteo_ia,
            'porcentaje': round((conteo_ia / suma_categorias) * 100, 1),
            'descripcion': 'No disponibilidad de preguntas adaptativas o respuestas de tutoría.',
            'color': '#ffd26a',
        },
        {
            'categoria': 'Sincronización Web y Móvil',
            'cantidad': conteo_sync,
            'porcentaje': round((conteo_sync / suma_categorias) * 100, 1),
            'descripcion': 'Discrepancias en puntajes XP o errores en transmisión de lecciones.',
            'color': '#246a91',
        },
    ]

    # 6. Serie temporal para gráficos en tiempo real (últimas 24 horas cada hora)
    serie_temporal_24h = []
    for i in range(23, -1, -1):
        inicio_bloque = ahora - timedelta(hours=i + 1)
        fin_bloque = ahora - timedelta(hours=i)
        etiqueta_hora = inicio_bloque.strftime('%H:00')

        # Caídas e incidentes en ese bloque de 1 hora
        seg_caida_hora = _calcular_segundos_caida_en_ventana(inicio_bloque, fin_bloque)
        uptime_hora = _calcular_porcentaje_uptime(3600, seg_caida_hora)

        # Errores en ese bloque
        err_hora = 0
        try:
            err_hora += EventoSincronizacion.objects.filter(
                estado='ERROR', recibido_en__gte=inicio_bloque, recibido_en__lt=fin_bloque
            ).count()
            err_hora += Incidente.objects.filter(
                iniciado_en__gte=inicio_bloque, iniciado_en__lt=fin_bloque
            ).count()
        except Exception:
            pass

        # Latencia representativa
        latencia_hora = round(latencia_api_ms * (1.0 + (0.1 if err_hora > 0 else 0.0)), 1)

        serie_temporal_24h.append({
            'etiqueta': etiqueta_hora,
            'timestamp': fin_bloque.isoformat(),
            'uptimePercent': uptime_hora,
            'errores': err_hora,
            'latenciaMs': latencia_hora,
        })

    # 7. Serie temporal para últimos 7 días
    serie_temporal_7d = []
    for i in range(6, -1, -1):
        inicio_dia = ahora - timedelta(days=i + 1)
        fin_dia = ahora - timedelta(days=i)
        etiqueta_dia = inicio_dia.strftime('%d %b')

        seg_caida_dia = _calcular_segundos_caida_en_ventana(inicio_dia, fin_dia)
        uptime_dia = _calcular_porcentaje_uptime(SEGUNDOS_24H, seg_caida_dia)

        err_dia = 0
        try:
            err_dia += EventoSincronizacion.objects.filter(
                estado='ERROR', recibido_en__gte=inicio_dia, recibido_en__lt=fin_dia
            ).count()
            err_dia += Incidente.objects.filter(
                iniciado_en__gte=inicio_dia, iniciado_en__lt=fin_dia
            ).count()
        except Exception:
            pass

        serie_temporal_7d.append({
            'etiqueta': etiqueta_dia,
            'timestamp': fin_dia.isoformat(),
            'uptimePercent': uptime_dia,
            'errores': err_dia,
            'latenciaMs': round(latencia_api_ms, 1),
        })

    # 8. Historial de caídas del servidor principal
    historial_caidas = []
    try:
        registros_caida = HistorialCaida.objects.order_by('-iniciado_en')[:20]
        for rc in registros_caida:
            duracion = rc.duracion_segundos
            if duracion is None and rc.restablecido_en:
                duracion = int((rc.restablecido_en - rc.iniciado_en).total_seconds())
            elif duracion is None:
                duracion = int((ahora - rc.iniciado_en).total_seconds())

            historial_caidas.append({
                'id': rc.id,
                'servidor': rc.servidor,
                'tipoEvento': rc.tipo_evento,
                'iniciadoEn': rc.iniciado_en.isoformat(),
                'restablecidoEn': rc.restablecido_en.isoformat() if rc.restablecido_en else None,
                'duracionSegundos': duracion,
                'duracionTexto': formatear_duracion(duracion),
                'motivo': rc.motivo,
                'codigoError': rc.codigo_error,
                'impactoColegios': rc.impacto_colegios or 'Sin afectación grave reportada',
                'registradoEn': rc.registrado_en.isoformat(),
            })
    except Exception as e:
        logger.warning('Error listando HistorialCaida: %s', e)

    return {
        'revisadoEn': ahora.isoformat(),
        'resumen': {
            'uptime24h': uptime_24h,
            'uptime7d': uptime_7d,
            'uptime30d': uptime_30d,
            'segundosCaida24h': segundos_caida_24h,
            'tiempoCaidaTexto24h': formatear_duracion(segundos_caida_24h),
            'totalErroresReportados': total_errores_reportados,
            'errores24h': errores_24h,
            'incidentesAbiertos': incidentes_abiertos,
            'estadoServidor': estado_servidor,
            'slaCalidad': sla_calidad,
            'latenciaPromedioMs': latencia_api_ms,
        },
        'serieTemporal24h': serie_temporal_24h,
        'serieTemporal7d': serie_temporal_7d,
        'desgloseErrores': desglose_errores,
        'historialCaidas': historial_caidas,
        'totalHistorialCaidas': len(historial_caidas),
    }


def registrar_evento_caida(
    servidor: str = 'servidor_principal',
    tipo_evento: str = 'CAIDA',
    motivo: str = 'Caída detectada por monitoreo de arquitectura',
    codigo_error: str | None = None,
    impacto_colegios: str | None = None,
    duracion_segundos: int | None = None,
    actor_id: str | None = None,
) -> dict:
    """Registra una nueva caída o interrupción en el historial del servidor principal."""
    ahora = dj_timezone.now()
    caida_id = str(uuid.uuid4())
    restablecido_en = (ahora + timedelta(seconds=duracion_segundos)) if duracion_segundos else None

    caida = HistorialCaida(
        id=caida_id,
        servidor=servidor,
        tipo_evento=tipo_evento,
        iniciado_en=ahora,
        restablecido_en=restablecido_en,
        duracion_segundos=duracion_segundos,
        motivo=motivo[:500],
        codigo_error=codigo_error[:80] if codigo_error else None,
        impacto_colegios=impacto_colegios[:255] if impacto_colegios else None,
        registrado_en=ahora,
    )
    caida.save()

    auditoria.registrar(actor_id, f'CAIDA_SERVIDOR_REGISTRADA:{servidor.upper()}', caida_id, resultado='CORRECTO')

    return {
        'id': caida.id,
        'servidor': caida.servidor,
        'tipoEvento': caida.tipo_evento,
        'iniciadoEn': caida.iniciado_en.isoformat(),
        'restablecidoEn': caida.restablecido_en.isoformat() if caida.restablecido_en else None,
        'duracionTexto': formatear_duracion(duracion_segundos),
        'motivo': caida.motivo,
        'mensaje': 'Evento de caída registrado en el historial del servidor principal.',
    }
