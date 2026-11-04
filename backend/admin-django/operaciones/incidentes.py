"""Gestión de incidentes de infraestructura y ciclo de vida de alertas (HU-47).

Registra caídas en operaciones_incidente, dispara alertas tempranas por correo y SMS,
y coordina las medidas correctivas de autoreinicio inmediato.
"""
import logging
import uuid
from datetime import datetime, timezone

from django.conf import settings
from django.utils import timezone as dj_timezone

from . import auditoria
from .autoreinicio import ejecutar_reinicio_servicio
from .models import Incidente
from .notificaciones_alertas import (
    enviar_alerta_caida_servicio,
    enviar_notificacion_recuperacion,
)

logger = logging.getLogger(__name__)

MAPA_CATEGORIAS = {
    'base': 'BASE_DATOS',
    'mysql': 'BASE_DATOS',
    'ia': 'IA',
    'api': 'DISPONIBILIDAD',
    'consola': 'DISPONIBILIDAD',
}


def registrar_caida(
    servicio: str,
    severidad: str = 'CRITICA',
    resumen: str | None = None,
    detalle_error: str | None = None,
    intentar_autoreinicio: bool = True,
    actor_id: str | None = None,
) -> dict:
    """Registra una caída de servicio, dispara alertas tempranas y ejecuta autoreinicio de emergencia."""
    clave_serv = servicio.lower()
    categoria = MAPA_CATEGORIAS.get(clave_serv, 'DISPONIBILIDAD')
    ahora = dj_timezone.now()

    # 1. Verificar si ya existe un incidente activo abierto para evitar alertas duplicadas
    incidente_existente = Incidente.objects.filter(
        servicio=clave_serv,
        estado__in=['ABIERTO', 'EN_ATENCION'],
    ).order_by('-iniciado_en').first()

    if incidente_existente:
        return {
            'id': incidente_existente.id,
            'servicio': incidente_existente.servicio,
            'estado': incidente_existente.estado,
            'severidad': incidente_existente.severidad,
            'resumen': incidente_existente.resumen,
            'iniciadoEn': incidente_existente.iniciado_en.isoformat(),
            'esNuevo': False,
            'mensaje': f'El incidente para {clave_serv} ya se encuentra abierto.',
        }

    # 2. Resumen descriptivo por defecto si no se especificó
    if not resumen:
        if clave_serv in ('base', 'mysql'):
            resumen = 'La base de datos MySQL no responde a consultas en el puerto 3306.'
        elif clave_serv == 'ia':
            resumen = 'El microservicio de IA (FastAPI/Gemini) no responde a chequeos de salud en el puerto 8000.'
        elif clave_serv == 'api':
            resumen = 'La API central Spring Boot no responde a peticiones en el puerto 8081.'
        else:
            resumen = f'El servicio {clave_serv} se encuentra fuera de línea.'

    incidente_id = str(uuid.uuid4())
    incidente = Incidente(
        id=incidente_id,
        servicio=clave_serv,
        categoria=categoria,
        severidad=severidad,
        estado='ABIERTO',
        resumen=resumen[:500],
        referencia_externa=detalle_error[:255] if detalle_error else None,
        iniciado_en=ahora,
    )
    incidente.save()

    # Auditoría
    auditoria.registrar(actor_id, f'INCIDENTE_ABIERTO:{clave_serv.upper()}', incidente_id, resultado='CORRECTO')

    # 3. Autoreinicio de emergencia inmediato si está habilitado
    autoreinicio_info = None
    autoreinicio_exitoso = False
    if intentar_autoreinicio and getattr(settings, 'RUPI_AUTOREINICIO_HABILITADO', True):
        logger.info('Disparando medida correctiva automática para %s...', clave_serv)
        resultado_reinicio = ejecutar_reinicio_servicio(clave_serv, actor_id=actor_id)
        if resultado_reinicio.get('exito'):
            autoreinicio_exitoso = True
            autoreinicio_info = f"Autoreinicio automático completado con éxito en {resultado_reinicio.get('tiempoMs')} ms."
            incidente.estado = 'RESUELTO'
            incidente.resuelto_en = dj_timezone.now()
            incidente.referencia_externa = (incidente.referencia_externa or '') + f' [Autoreparado: {resultado_reinicio.get("tiempoMs")}ms]'
            incidente.save()
            auditoria.registrar(actor_id, f'INCIDENTE_AUTORESUELTO:{clave_serv.upper()}', incidente_id, resultado='CORRECTO')
        else:
            autoreinicio_info = f"Autoreinicio intentado sin éxito: {resultado_reinicio.get('mensaje')}"

    # 4. Disparar alertas tempranas al responsable (Correo + SMS/Webhook)
    notificacion_res = enviar_alerta_caida_servicio(
        servicio=clave_serv,
        categoria=categoria,
        severidad=severidad,
        resumen=resumen,
        detalle_error=detalle_error,
        autoreinicio_info=autoreinicio_info,
    )

    return {
        'id': incidente.id,
        'servicio': incidente.servicio,
        'estado': incidente.estado,
        'severidad': incidente.severidad,
        'resumen': incidente.resumen,
        'iniciadoEn': incidente.iniciado_en.isoformat(),
        'resueltoEn': incidente.resuelto_en.isoformat() if incidente.resuelto_en else None,
        'esNuevo': True,
        'autoreinicio': autoreinicio_info,
        'autoreinicioExitoso': autoreinicio_exitoso,
        'notificacion': notificacion_res,
    }


def registrar_recuperacion(servicio: str, actor_id: str | None = None) -> dict | None:
    """Detecta que un servicio volvió a estar operativo y resuelve su incidente pendiente."""
    clave_serv = servicio.lower()
    incidente_abierto = Incidente.objects.filter(
        servicio=clave_serv,
        estado__in=['ABIERTO', 'EN_ATENCION'],
    ).order_by('-iniciado_en').first()

    if not incidente_abierto:
        return None

    ahora = dj_timezone.now()
    duracion_segundos = int((ahora - incidente_abierto.iniciado_en).total_seconds())

    incidente_abierto.estado = 'RESUELTO'
    incidente_abierto.resuelto_en = ahora
    incidente_abierto.save()

    auditoria.registrar(actor_id, f'INCIDENTE_RESUELTO:{clave_serv.upper()}', incidente_abierto.id, resultado='CORRECTO')

    # Notificar resolución al responsable
    notif = enviar_notificacion_recuperacion(clave_serv, duracion_segundos=duracion_segundos)

    return {
        'id': incidente_abierto.id,
        'servicio': incidente_abierto.servicio,
        'estado': 'RESUELTO',
        'iniciadoEn': incidente_abierto.iniciado_en.isoformat(),
        'resueltoEn': incidente_abierto.resuelto_en.isoformat(),
        'duracionSegundos': duracion_segundos,
        'notificacion': notif,
    }


def listar_incidentes(estado: str | None = None, limite: int = 50) -> list[dict]:
    """Retorna el historial de incidentes de infraestructura con formateo limpio para la consola."""
    query = Incidente.objects.all()
    if estado and estado.strip():
        query = query.filter(estado=estado.strip().upper())
    
    incidentes = query.order_by('-iniciado_en')[:limite]
    resultado = []
    for inc in incidentes:
        duracion = None
        if inc.resuelto_en and inc.iniciado_en:
            duracion = int((inc.resuelto_en - inc.iniciado_en).total_seconds())
        elif inc.iniciado_en:
            duracion = int((dj_timezone.now() - inc.iniciado_en).total_seconds())

        resultado.append({
            'id': inc.id,
            'servicio': inc.servicio,
            'categoria': inc.categoria,
            'severidad': inc.severidad,
            'estado': inc.estado,
            'resumen': inc.resumen,
            'referenciaExterna': inc.referencia_externa,
            'iniciadoEn': inc.iniciado_en.isoformat(),
            'resueltoEn': inc.resuelto_en.isoformat() if inc.resuelto_en else None,
            'duracionSegundos': duracion,
        })
    return resultado


def resolver_incidente_manual(incidente_id: str, actor_id: str | None = None, comentario: str | None = None) -> dict:
    """Permite al responsable de infraestructura resolver manualmente un incidente."""
    incidente = Incidente.objects.filter(id=incidente_id).first()
    if not incidente:
        raise ValueError(f'No se encontró el incidente {incidente_id}.')

    ahora = dj_timezone.now()
    incidente.estado = 'RESUELTO'
    incidente.resuelto_en = ahora
    if comentario:
        incidente.referencia_externa = (incidente.referencia_externa or '') + f' [Nota: {comentario[:150]}]'
    incidente.save()

    auditoria.registrar(actor_id, f'INCIDENTE_MANUAL_RESUELTO:{incidente.servicio.upper()}', incidente.id, resultado='CORRECTO')

    return {
        'id': incidente.id,
        'servicio': incidente.servicio,
        'estado': incidente.estado,
        'resueltoEn': incidente.resuelto_en.isoformat(),
        'mensaje': 'Incidente marcado como resuelto con éxito.',
    }
