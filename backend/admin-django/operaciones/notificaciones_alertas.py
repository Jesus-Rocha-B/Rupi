"""Sistema de notificaciones tempranas por correo y SMS para infraestructura (HU-47).

Aplica el Patrón Estrategia (Strategy Pattern) para el despacho multicanal de alertas,
permitiendo desacoplar la lógica de formateo y validación de seguridad de cada transporte concreto
(Correo SMTP, Webhook/SMS, o futuros proveedores como Slack/Telegram).
"""
import json
import logging
import urllib.error
import urllib.parse
import urllib.request
from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any

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


# =====================================================================
# Patrón Estrategia (Strategy Pattern) para Canales de Alerta
# =====================================================================

@dataclass
class MensajeAlerta:
    """Entidad que encapsula los datos normalizados de una alerta de infraestructura."""
    asunto: str
    cuerpo: str
    texto_sms: str
    servicio: str = ''
    severidad: str = ''
    tipo_evento: str = 'ALERTA_INFRAESTRUCTURA'
    fecha_str: str = ''
    destinatario_email: str = ''
    destinatario_sms: str = ''
    extra_payload: dict[str, Any] = field(default_factory=dict)


@dataclass
class ResultadoCanal:
    """Resultado devuelto por una estrategia concreta de notificación."""
    exito: bool
    destinatario: str | None = None
    error: str | None = None


class EstrategiaCanalAlerta(ABC):
    """Interfaz base (Strategy) para canales de notificación."""

    @abstractmethod
    def esta_habilitado(self) -> bool:
        """Determina si el canal cuenta con configuración suficiente para operar."""
        pass

    @abstractmethod
    def despachar(self, mensaje: MensajeAlerta) -> ResultadoCanal:
        """Realiza la transmisión física de la alerta a través del transporte."""
        pass


class CanalCorreoAlerta(EstrategiaCanalAlerta):
    """Estrategia de notificación vía Correo Electrónico SMTP institucional."""

    def esta_habilitado(self) -> bool:
        return True

    def despachar(self, mensaje: MensajeAlerta) -> ResultadoCanal:
        destinatario = mensaje.destinatario_email or getattr(
            settings, 'RUPI_ALERTA_EMAIL_RESPONSABLE', 'infraestructura@rupi.edu.pe'
        )
        try:
            send_mail(
                mensaje.asunto,
                mensaje.cuerpo,
                settings.DEFAULT_FROM_EMAIL,
                [destinatario],
                fail_silently=False,
            )
            logger.info('Alerta de infraestructura enviada por correo a %s', destinatario)
            return ResultadoCanal(exito=True, destinatario=destinatario)
        except Exception as e:
            logger.warning('Fallo al enviar correo de alerta a %s: %s', destinatario, e)
            return ResultadoCanal(exito=False, destinatario=destinatario, error=str(e))


class CanalWebhookSmsAlerta(EstrategiaCanalAlerta):
    """Estrategia de notificación vía SMS/Webhook HTTP seguro con validación SSRF."""

    def esta_habilitado(self) -> bool:
        webhook_url = getattr(settings, 'RUPI_ALERTA_SMS_WEBHOOK', '')
        destinatario_sms = getattr(settings, 'RUPI_ALERTA_SMS_NUMERO', '')
        return bool(webhook_url or destinatario_sms)

    def despachar(self, mensaje: MensajeAlerta) -> ResultadoCanal:
        webhook_url = getattr(settings, 'RUPI_ALERTA_SMS_WEBHOOK', '')
        destinatario_sms = mensaje.destinatario_sms or getattr(settings, 'RUPI_ALERTA_SMS_NUMERO', '')

        if webhook_url:
            if not _es_url_segura(webhook_url):
                logger.warning('URL de webhook SMS descartada por esquema no permitido o SSRF: %s', webhook_url)
                return ResultadoCanal(
                    exito=False,
                    destinatario=destinatario_sms or None,
                    error='URL descartada por esquema no permitido o SSRF',
                )
            try:
                payload_dict = {
                    'tipo': mensaje.tipo_evento,
                    'mensaje': mensaje.texto_sms,
                    'timestamp': mensaje.fecha_str,
                }
                if mensaje.servicio:
                    payload_dict['servicio'] = mensaje.servicio
                if mensaje.severidad:
                    payload_dict['severidad'] = mensaje.severidad
                if destinatario_sms:
                    payload_dict['telefono'] = destinatario_sms
                if mensaje.extra_payload:
                    payload_dict.update(mensaje.extra_payload)

                payload = json.dumps(payload_dict).encode('utf-8')
                req = urllib.request.Request(
                    webhook_url,
                    data=payload,
                    headers={'Content-Type': 'application/json'},
                    method='POST',
                )
                with urllib.request.urlopen(req, timeout=3) as resp:
                    exito = 200 <= resp.getcode() < 300
                    return ResultadoCanal(exito=exito, destinatario=destinatario_sms or None)
            except Exception as e:
                logger.warning('Fallo al disparar webhook de SMS a %s: %s', webhook_url, e)
                return ResultadoCanal(exito=False, destinatario=destinatario_sms or None, error=str(e))

        # En entorno local/desarrollo sin pasarela HTTP externa, se simula con registro en logger
        if destinatario_sms:
            logger.info('[SMS SIMULADO a %s] %s', destinatario_sms, mensaje.texto_sms)
            return ResultadoCanal(exito=True, destinatario=destinatario_sms)

        return ResultadoCanal(exito=False, destinatario=None, error='Canal SMS no configurado')


class DespachadorAlertas:
    """Contexto del patrón Estrategia: gestiona y ejecuta los canales de notificación configurados."""

    def __init__(self, canales: list[EstrategiaCanalAlerta] | None = None):
        self.canales = canales if canales is not None else [
            CanalCorreoAlerta(),
            CanalWebhookSmsAlerta(),
        ]

    def despachar(self, mensaje: MensajeAlerta) -> tuple[ResultadoCanal, ResultadoCanal]:
        """Despacha la alerta a los canales de correo y SMS, retornando el par de resultados."""
        correo_res = ResultadoCanal(exito=False, error='Canal correo no disponible')
        sms_res = ResultadoCanal(exito=False, error='Canal SMS no disponible')

        for canal in self.canales:
            if isinstance(canal, CanalCorreoAlerta):
                correo_res = canal.despachar(mensaje)
            elif isinstance(canal, CanalWebhookSmsAlerta):
                sms_res = canal.despachar(mensaje)
            elif canal.esta_habilitado():
                canal.despachar(mensaje)

        return correo_res, sms_res


# Instancia por defecto del despachador
despachador_default = DespachadorAlertas()


# =====================================================================
# API Pública de Notificaciones de Infraestructura (HU-47)
# =====================================================================

def enviar_alerta_caida_servicio(
    servicio: str,
    categoria: str,
    severidad: str,
    resumen: str,
    detalle_error: str | None = None,
    autoreinicio_info: str | None = None,
    despachador: DespachadorAlertas | None = None,
) -> dict:
    """Envía alertas inmediatas por correo y SMS usando el despachador de estrategias."""
    desp = despachador or despachador_default
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
    sms_texto = f'[RUPI ALERTA {severidad}] Caída en {nombre_serv}. {resumen[:80]}. Revisa consola /admin.'

    mensaje = MensajeAlerta(
        asunto=asunto,
        cuerpo=cuerpo_email,
        texto_sms=sms_texto,
        servicio=servicio,
        severidad=severidad,
        tipo_evento='ALERTA_INFRAESTRUCTURA',
        fecha_str=fecha_str,
        destinatario_email=destinatario_email,
        destinatario_sms=destinatario_sms,
    )

    res_correo, res_sms = desp.despachar(mensaje)

    return {
        'emailEnviado': res_correo.exito,
        'destinatarioEmail': res_correo.destinatario,
        'errorEmail': res_correo.error,
        'smsEnviado': res_sms.exito,
        'destinatarioSms': res_sms.destinatario,
        'textoAlerta': sms_texto,
    }


def enviar_notificacion_recuperacion(
    servicio: str,
    duracion_segundos: int | None = None,
    despachador: DespachadorAlertas | None = None,
) -> dict:
    """Avisa al responsable que el servicio caído volvió a la normalidad."""
    desp = despachador or despachador_default
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
    sms_texto = f'[RUPI RESTABLECIDO] {nombre_serv} operativo nuevamente. Incidente cerrado.'

    mensaje = MensajeAlerta(
        asunto=asunto,
        cuerpo=cuerpo,
        texto_sms=sms_texto,
        servicio=servicio,
        tipo_evento='RECUPERACION_INFRAESTRUCTURA',
        fecha_str=fecha_str,
        destinatario_email=destinatario_email,
        destinatario_sms=destinatario_sms,
    )

    res_correo, res_sms = desp.despachar(mensaje)

    return {
        'emailEnviado': res_correo.exito,
        'smsEnviado': res_sms.exito,
        'mensaje': f'Notificación de recuperación de {nombre_serv} enviada.',
    }


def probar_canales_alerta(
    actor_nombre: str = 'Administrador',
    despachador: DespachadorAlertas | None = None,
) -> dict:
    """Envía un mensaje de prueba a los canales de alerta (Correo y SMS/Webhook) para verificación."""
    desp = despachador or despachador_default
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
    sms_texto = f'[RUPI PRUEBA] Canales de notificación operativos. Probado por {actor_nombre}.'

    mensaje = MensajeAlerta(
        asunto=asunto,
        cuerpo=cuerpo,
        texto_sms=sms_texto,
        tipo_evento='PRUEBA_ALERTA',
        fecha_str=fecha_str,
        destinatario_email=destinatario_email,
        destinatario_sms=destinatario_sms,
    )

    res_correo, res_sms = desp.despachar(mensaje)

    return {
        'emailEnviado': res_correo.exito,
        'destinatarioEmail': res_correo.destinatario,
        'errorEmail': res_correo.error,
        'smsEnviado': res_sms.exito,
        'destinatarioSms': res_sms.destinatario or 'No configurado (se registrará en log)',
        'mensaje': 'Prueba de canales ejecutada correctamente.',
    }


def enviar_alerta_fallo_respaldo(
    respaldo_id: str,
    origen: str,
    almacenamiento: str,
    detalle_error: str,
    email_destino: str | None = None,
    despachador: DespachadorAlertas | None = None,
) -> dict:
    """Notifica de inmediato al administrador cuando falla el respaldo nocturno o manual (HU-57)."""
    desp = despachador or despachador_default
    fecha_str = _formatear_fecha_actual()
    destinatario_email = (email_destino or '').strip() or getattr(
        settings, 'RUPI_ALERTA_EMAIL_RESPONSABLE', 'infraestructura@rupi.edu.pe'
    )
    destinatario_sms = getattr(settings, 'RUPI_ALERTA_SMS_NUMERO', '')

    asunto = f'[ALERTA RESPALDO RUPI] Fallo en respaldo {origen.lower()} de base de datos'
    cuerpo = '\n'.join([
        '======================================================================',
        '         ALERTA CRÍTICA DE RESPALDO DE BASE DE DATOS - RUPI           ',
        '======================================================================',
        '',
        f'ID de Respaldo     : {respaldo_id}',
        f'Origen de Ejecución: {origen}',
        f'Destino Externo    : {almacenamiento}',
        f'Fecha y Hora       : {fecha_str}',
        '',
        'MOTIVO DEL FALLO:',
        f'{detalle_error}',
        '',
        'IMPACTO Y RECOMENDACIÓN:',
        'El respaldo nocturno fuera de horario escolar no pudo completarse.',
        'Verifica el espacio/conectividad del almacenamiento externo y ejecuta',
        'un respaldo manual desde Consola /admin -> Respaldos automáticos.',
        '',
        'Equipo de Seguridad y Respaldos · RUPI Plataforma Educativa',
        '======================================================================',
    ])
    sms_texto = (
        f'[RUPI ALERTA RESPALDO] Fallo en copia {origen.lower()} ({almacenamiento}): '
        f'{detalle_error[:90]}'
    )

    mensaje = MensajeAlerta(
        asunto=asunto,
        cuerpo=cuerpo,
        texto_sms=sms_texto,
        servicio='respaldo_nocturno',
        severidad='ALTA',
        tipo_evento='FALLO_RESPALDO_NOCTURNO',
        fecha_str=fecha_str,
        destinatario_email=destinatario_email,
        destinatario_sms=destinatario_sms,
        extra_payload={'respaldoId': respaldo_id, 'origen': origen, 'almacenamiento': almacenamiento},
    )

    res_correo, res_sms = desp.despachar(mensaje)

    return {
        'emailEnviado': res_correo.exito,
        'destinatarioEmail': res_correo.destinatario,
        'errorEmail': res_correo.error,
        'smsEnviado': res_sms.exito,
        'destinatarioSms': res_sms.destinatario,
        'textoAlerta': sms_texto,
    }


def probar_alerta_respaldo(
    actor_nombre: str = 'Administrador',
    email_destino: str | None = None,
    despachador: DespachadorAlertas | None = None,
) -> dict:
    """Envía una notificación simulada de fallo de respaldo para comprobar alertas (HU-57)."""
    res = enviar_alerta_fallo_respaldo(
        respaldo_id='PRUEBA-HU57',
        origen='PROGRAMADO (PRUEBA)',
        almacenamiento='Almacenamiento externo configurado',
        detalle_error=f'Simulación de alerta disparada por {actor_nombre} desde Consola /admin.',
        email_destino=email_destino,
        despachador=despachador,
    )
    res['mensaje'] = 'Alerta de prueba para respaldos nocturnos ejecutada correctamente.'
    return res
