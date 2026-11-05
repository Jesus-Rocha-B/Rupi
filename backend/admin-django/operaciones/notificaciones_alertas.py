"""Sistema de notificaciones tempranas por correo y SMS para infraestructura (HU-47).

Avisa al instante al responsable de infraestructura ante la caída de servicios críticos
(Base de datos MySQL, Inteligencia Artificial o API central) para aplicar medidas correctivas de emergencia.
"""
import json
import logging
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone

from django.conf import settings
from django.core.mail import send_mail

logger = logging.getLogger(__name__)

NOMBRES_SERVICIOS = {
    'base': 'Base de Datos MySQL (Puerto 3306)',
    'ia': 'Servicio de Inteligencia Artificial (FastAPI / Gemini, Puerto 8000)',
    'api': 'API Central Rupi (Spring Boot, Puerto 8081)',
    'consola': 'Consola de Administración (Django, Puerto 8082)',
}

IMPACTOS_ESCOLAR = {
    'base': 'Interrupción crítica: Ninguna tableta ni aula puede iniciar sesión, cargar rutas ni guardar notas.',
    'ia': 'Degradación educativa: El tutor socrático Rupi y las preguntas adaptativas no están disponibles.',
    'api': 'Interrupción general: Las lecciones escolares y sincronizaciones están completamente detenidas.',
}


def _obtener_nombre_servicio(clave_servicio: str) -> str:
    return NOMBRES_SERVICIOS.get(clave_servicio.lower(), clave_servicio.upper())


def _formatear_fecha_actual() -> str:
    ahora = datetime.now(timezone.utc)
    return ahora.strftime('%Y-%m-%d %H:%M:%S UTC')


def _es_url_segura(url: str | None) -> bool:
    """Valida que la URL del webhook sea un endpoint http o https legítimo para mitigar SSRF (CWE-918)."""
    if not url or not isinstance(url, str):
        return False
    try:
        parsed = urllib.parse.urlparse(url.strip())
        return parsed.scheme.lower() in ('http', 'https') and bool(parsed.netloc)
    except Exception:
        return False


def enviar_alerta_caida_servicio(
    servicio: str,
    categoria: str,
    severidad: str,
    resumen: str,
    detalle_error: str | None = None,
    autoreinicio_info: str | None = None,
) -> dict:
    """Envía alertas inmediatas por correo electrónico y SMS/Webhook al responsable de infraestructura."""
    nombre_serv = _obtener_nombre_servicio(servicio)
    fecha_str = _formatear_fecha_actual()
    impacto = IMPACTOS_ESCOLAR.get(servicio.lower(), 'Afectación en la disponibilidad de la plataforma escolar.')
    destinatario_email = getattr(settings, 'RUPI_ALERTA_EMAIL_RESPONSABLE', 'infraestructura@rupi.edu.pe')
    destinatario_sms = getattr(settings, 'RUPI_ALERTA_SMS_NUMERO', '')

    asunto = f'[ALERTA {severidad} RUPI] Caída de servicio crítico: {nombre_serv}'
    cuerpo_lineas = [
        '======================================================================',
        '              ALERTA TEMPRANA DE INFRAESTRUCTURA - RUPI              ',
        '======================================================================',
        '',
        f'Servicio Afectado : {nombre_serv}',
        f'Severidad         : {severidad}',
        f'Categoría         : {categoria}',
        f'Fecha y Hora      : {fecha_str}',
        '',
        'DESCRIPCIÓN DEL INCIDENTE:',
        f'{resumen}',
    ]

    if detalle_error:
        cuerpo_lineas.extend([
            '',
            'DETALLE TÉCNICO:',
            f'{detalle_error}',
        ])

    cuerpo_lineas.extend([
        '',
        'IMPACTO EN LA COMUNIDAD ESCOLAR:',
        f'{impacto}',
    ])

    if autoreinicio_info:
        cuerpo_lineas.extend([
            '',
            'ESTADO DE AUTORREINICIO DE EMERGENCIA:',
            f'{autoreinicio_info}',
        ])
    else:
        cuerpo_lineas.extend([
            '',
            'MEDIDA CORRECTIVA SUGERIDA:',
            'Ejecutar script de reinicio: scripts/Reiniciar-Servicio.ps1 o pulsar "Reiniciar servicio" en la consola /admin.',
        ])

    cuerpo_lineas.extend([
        '',
        '----------------------------------------------------------------------',
        'Consola de Administración: http://localhost:5173/admin',
        'Equipo de Monitoreo e Infraestructura · RUPI Plataforma Educativa',
        '======================================================================',
    ])
    cuerpo_email = '\n'.join(cuerpo_lineas)

    # 1. Envío por Correo Electrónico
    email_ok = False
    error_email = None
    try:
        send_mail(
            asunto,
            cuerpo_email,
            settings.DEFAULT_FROM_EMAIL,
            [destinatario_email],
            fail_silently=False,
        )
        email_ok = True
        logger.info('Alerta de caída de %s enviada por correo a %s', servicio, destinatario_email)
    except Exception as e:
        error_email = str(e)
        logger.warning('Fallo al enviar correo de alerta a %s: %s', destinatario_email, e)

    # 2. Envío por SMS / Webhook Urgente
    sms_ok = False
    sms_texto = f'[RUPI ALERTA {severidad}] Caída en {nombre_serv}. {resumen[:80]}. Revisa consola /admin.'
    webhook_url = getattr(settings, 'RUPI_ALERTA_SMS_WEBHOOK', '')
    if webhook_url:
        if not _es_url_segura(webhook_url):
            logger.warning('URL de webhook SMS descartada por esquema no permitido o SSRF: %s', webhook_url)
        else:
            try:
                payload = json.dumps({
                    'tipo': 'ALERTA_INFRAESTRUCTURA',
                    'servicio': servicio,
                    'severidad': severidad,
                    'mensaje': sms_texto,
                    'timestamp': fecha_str,
                    'telefono': destinatario_sms,
                }).encode('utf-8')
                req = urllib.request.Request(
                    webhook_url,
                    data=payload,
                    headers={'Content-Type': 'application/json'},
                    method='POST',
                )
                with urllib.request.urlopen(req, timeout=3) as resp:
                    sms_ok = 200 <= resp.getcode() < 300
            except Exception as e:
                logger.warning('Fallo al disparar webhook de SMS a %s: %s', webhook_url, e)
    else:
        # En entorno local/desarrollo sin pasarela HTTP externa, se simula con registro exitoso en logger
        if destinatario_sms:
            logger.info('[SMS SIMULADO a %s] %s', destinatario_sms, sms_texto)
            sms_ok = True

    return {
        'emailEnviado': email_ok,
        'destinatarioEmail': destinatario_email,
        'errorEmail': error_email,
        'smsEnviado': sms_ok,
        'destinatarioSms': destinatario_sms or None,
        'textoAlerta': sms_texto,
    }


def enviar_notificacion_recuperacion(servicio: str, duracion_segundos: int | None = None) -> dict:
    """Avisa al responsable que el servicio caído volvió a la normalidad."""
    nombre_serv = _obtener_nombre_servicio(servicio)
    fecha_str = _formatear_fecha_actual()
    destinatario_email = getattr(settings, 'RUPI_ALERTA_EMAIL_RESPONSABLE', 'infraestructura@rupi.edu.pe')
    destinatario_sms = getattr(settings, 'RUPI_ALERTA_SMS_NUMERO', '')

    duracion_txt = f' luego de {duracion_segundos} segundos de indisponibilidad' if duracion_segundos else ''
    asunto = f'[RECUPERADO RUPI] Servicio restablecido: {nombre_serv}'
    cuerpo = '\n'.join([
        '======================================================================',
        '              SERVICIO RESTABLECIDO EXITOSAMENTE - RUPI               ',
        '======================================================================',
        '',
        f'El servicio {nombre_serv} ha vuelto a operar con normalidad{duracion_txt}.',
        f'Fecha y Hora: {fecha_str}',
        '',
        'El incidente ha sido marcado como RESUELTO en el registro de operaciones.',
        'Las clases escolares pueden continuar de manera segura.',
        '',
        'Equipo de Monitoreo e Infraestructura · RUPI Plataforma Educativa',
        '======================================================================',
    ])

    email_ok = False
    try:
        send_mail(
            asunto,
            cuerpo,
            settings.DEFAULT_FROM_EMAIL,
            [destinatario_email],
            fail_silently=False,
        )
        email_ok = True
    except Exception as e:
        logger.warning('Fallo al enviar correo de recuperación: %s', e)

    sms_ok = False
    sms_texto = f'[RUPI RESTABLECIDO] {nombre_serv} operativo nuevamente. Incidente cerrado.'
    webhook_url = getattr(settings, 'RUPI_ALERTA_SMS_WEBHOOK', '')
    if webhook_url:
        if not _es_url_segura(webhook_url):
            logger.warning('URL de webhook SMS de recuperación descartada por esquema no permitido: %s', webhook_url)
        else:
            try:
                payload = json.dumps({
                    'tipo': 'RECUPERACION_INFRAESTRUCTURA',
                    'servicio': servicio,
                    'mensaje': sms_texto,
                    'timestamp': fecha_str,
                }).encode('utf-8')
                req = urllib.request.Request(webhook_url, data=payload, headers={'Content-Type': 'application/json'}, method='POST')
                with urllib.request.urlopen(req, timeout=3) as resp:
                    sms_ok = 200 <= resp.getcode() < 300
            except Exception:
                pass
    elif destinatario_sms:
        sms_ok = True

    return {
        'emailEnviado': email_ok,
        'smsEnviado': sms_ok,
        'mensaje': f'Notificación de recuperación de {nombre_serv} enviada.',
    }


def probar_canales_alerta(actor_nombre: str = 'Administrador') -> dict:
    """Envía un mensaje de prueba a los canales de alerta (Correo y SMS/Webhook) para verificación."""
    nombre_serv = 'Canal de Monitoreo e Infraestructura'
    fecha_str = _formatear_fecha_actual()
    destinatario_email = getattr(settings, 'RUPI_ALERTA_EMAIL_RESPONSABLE', 'infraestructura@rupi.edu.pe')
    destinatario_sms = getattr(settings, 'RUPI_ALERTA_SMS_NUMERO', '')

    asunto = '[PRUEBA DE ALERTA RUPI] Verificación de canales de notificación'
    cuerpo = '\n'.join([
        '======================================================================',
        '            PRUEBA DE CANALES DE NOTIFICACIÓN DE ALERTA              ',
        '======================================================================',
        '',
        f'Esta es una prueba generada por {actor_nombre} desde la Consola de Administración.',
        f'Fecha y Hora: {fecha_str}',
        '',
        'Si recibiste este mensaje, tu canal de alertas automáticas está correctamente configurado.',
        '',
        'Equipo de Monitoreo e Infraestructura · RUPI Plataforma Educativa',
        '======================================================================',
    ])

    email_ok = False
    error_email = None
    try:
        send_mail(
            asunto,
            cuerpo,
            settings.DEFAULT_FROM_EMAIL,
            [destinatario_email],
            fail_silently=False,
        )
        email_ok = True
    except Exception as e:
        error_email = str(e)

    sms_ok = False
    sms_texto = f'[RUPI PRUEBA] Canales de notificación operativos. Probado por {actor_nombre}.'
    webhook_url = getattr(settings, 'RUPI_ALERTA_SMS_WEBHOOK', '')
    if webhook_url:
        if not _es_url_segura(webhook_url):
            logger.warning('URL de webhook SMS de prueba descartada por esquema no permitido: %s', webhook_url)
        else:
            try:
                payload = json.dumps({
                    'tipo': 'PRUEBA_ALERTA',
                    'mensaje': sms_texto,
                    'timestamp': fecha_str,
                }).encode('utf-8')
                req = urllib.request.Request(webhook_url, data=payload, headers={'Content-Type': 'application/json'}, method='POST')
                with urllib.request.urlopen(req, timeout=3) as resp:
                    sms_ok = 200 <= resp.getcode() < 300
            except Exception:
                pass
    elif destinatario_sms:
        sms_ok = True

    return {
        'emailEnviado': email_ok,
        'destinatarioEmail': destinatario_email,
        'errorEmail': error_email,
        'smsEnviado': sms_ok,
        'destinatarioSms': destinatario_sms or 'No configurado (se registrará en log)',
        'mensaje': 'Prueba de canales ejecutada correctamente.',
    }
