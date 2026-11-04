"""Orquestador de monitoreo continuo y vigilancia activa (HU-47).

Evalúa periódicamente la disponibilidad de la base de datos MySQL, el servicio de IA y la API central.
Si detecta caídas, registra incidentes, envía alertas tempranas y dispara las medidas correctivas inmediatas.
"""
import logging
from django.utils import timezone

from . import incidentes, salud

logger = logging.getLogger(__name__)


def ejecutar_ciclo_monitoreo(intentar_autoreinicio: bool = True, actor_id: str | None = None) -> dict:
    """Ejecuta una ronda de inspección integral y reacciona de inmediato ante caídas."""
    diagnostico = salud.revisar()
    servicios = diagnostico.get('servicios', [])
    ahora_iso = timezone.now().isoformat()

    acciones = []
    novedades = []

    for s in servicios:
        clave = s.get('clave')
        estado = s.get('estado')
        detalle = s.get('detalle', {})
        error_msg = detalle.get('error') if isinstance(detalle, dict) else None

        if estado == salud.CAIDO:
            severidad = 'CRITICA' if clave in ('base', 'api') else 'ALTA'
            resultado_caida = incidentes.registrar_caida(
                servicio=clave,
                severidad=severidad,
                resumen=f"El servicio {s.get('nombre')} no responde a verificaciones de salud.",
                detalle_error=error_msg,
                intentar_autoreinicio=intentar_autoreinicio,
                actor_id=actor_id,
            )
            novedades.append({
                'servicio': clave,
                'evento': 'CAIDA_REGISTRADA',
                'incidenteId': resultado_caida.get('id'),
                'esNuevo': resultado_caida.get('esNuevo'),
                'autoreinicio': resultado_caida.get('autoreinicio'),
            })
            if resultado_caida.get('autoreinicio'):
                acciones.append({
                    'servicio': clave,
                    'accion': 'AUTOREINICIO_EJECUTADO',
                    'exito': resultado_caida.get('autoreinicioExitoso'),
                    'detalle': resultado_caida.get('autoreinicio'),
                })

        elif estado in (salud.OK, salud.LENTO):
            # Si el servicio ahora responde, verificar si tenía un incidente abierto y resolverlo
            recup = incidentes.registrar_recuperacion(clave, actor_id=actor_id)
            if recup:
                novedades.append({
                    'servicio': clave,
                    'evento': 'RECUPERACION_REGISTRADA',
                    'incidenteId': recup.get('id'),
                    'duracionSegundos': recup.get('duracionSegundos'),
                })

    incidentes_abiertos = incidentes.listar_incidentes(estado='ABIERTO', limite=20)

    return {
        'revisadoEn': ahora_iso,
        'estadoGeneral': diagnostico.get('estado'),
        'servicios': servicios,
        'alertas': diagnostico.get('alertas', []),
        'novedades': novedades,
        'accionesTomadas': acciones,
        'incidentesAbiertos': incidentes_abiertos,
        'totalIncidentesAbiertos': len(incidentes_abiertos),
    }
