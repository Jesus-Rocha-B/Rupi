"""Ejecución de medidas correctivas de emergencia y scripts de autoreinicio (HU-47).

Permite restablecer servicios críticos (MySQL, IA FastAPI, Spring Boot) ante caídas detectadas,
devolviendo la operatividad al instante para evitar interrupciones escolares.
"""
import json
import logging
import os
import subprocess
import time
from pathlib import Path

from django.conf import settings
from django.utils import timezone

from . import auditoria

logger = logging.getLogger(__name__)

SERVICIOS_VALIDOS = {'mysql', 'base', 'ia', 'api', 'todos'}
MAPA_CLAVES = {
    'base': 'mysql',
    'mysql': 'mysql',
    'ia': 'ia',
    'api': 'api',
    'todos': 'todos',
}


def ejecutar_reinicio_servicio(servicio: str, actor_id: str | None = None, forzar: bool = True) -> dict:
    """Ejecuta el script PowerShell scripts/Reiniciar-Servicio.ps1 de forma segura."""
    clave_normalizada = MAPA_CLAVES.get(servicio.lower())
    if not clave_normalizada:
        return {
            'exito': False,
            'servicio': servicio,
            'mensaje': f'Servicio no reconocido. Válidos: {", ".join(sorted(SERVICIOS_VALIDOS))}',
            'tiempoMs': 0,
        }

    root_dir = Path(settings.BASE_DIR).parent.parent
    script_path = root_dir / 'scripts' / 'Reiniciar-Servicio.ps1'

    if not script_path.exists():
        logger.error('No se encontró el script de reinicio en %s', script_path)
        return {
            'exito': False,
            'servicio': clave_normalizada,
            'mensaje': f'No existe el script {script_path}',
            'tiempoMs': 0,
        }

    cmd = [
        'powershell.exe',
        '-NoProfile',
        '-ExecutionPolicy',
        'Bypass',
        '-File',
        str(script_path),
        '-Servicio',
        clave_normalizada,
    ]
    if forzar:
        cmd.append('-Forzar')

    t0 = time.monotonic()
    try:
        proc = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=70,
            cwd=str(root_dir),
        )
        tiempo_ms = round((time.monotonic() - t0) * 1000, 1)

        # Parsear salida JSON si el script la emitió
        resultado_json = None
        for line in reversed(proc.stdout.strip().splitlines()):
            line_str = line.strip()
            if line_str.startswith('{') or line_str.endswith('}'):
                try:
                    resultado_json = json.loads(proc.stdout[proc.stdout.find('{'):proc.stdout.rfind('}') + 1])
                    break
                except Exception:
                    pass

        exito = proc.returncode == 0 and (resultado_json.get('TodosExitosos', True) if resultado_json else True)
        mensaje = 'Servicio restablecido con éxito.' if exito else f'Fallo en el script de reinicio (código {proc.returncode}).'
        if not exito and proc.stderr:
            mensaje += f' Detalle: {proc.stderr[:300]}'

        # Registrar en auditoría
        auditoria.registrar(
            actor_id,
            f'AUTOREINICIO_SERVICIO:{clave_normalizada.upper()}',
            clave_normalizada,
            resultado='CORRECTO' if exito else 'ERROR',
        )

        return {
            'exito': exito,
            'servicio': clave_normalizada,
            'tiempoMs': tiempo_ms,
            'mensaje': mensaje,
            'detalle': resultado_json or proc.stdout[:500],
        }

    except subprocess.TimeoutExpired:
        tiempo_ms = round((time.monotonic() - t0) * 1000, 1)
        logger.warning('El script de reinicio para %s excedió el tiempo límite.', clave_normalizada)
        auditoria.registrar(actor_id, f'AUTOREINICIO_TIMEOUT:{clave_normalizada.upper()}', clave_normalizada, resultado='ERROR')
        return {
            'exito': False,
            'servicio': clave_normalizada,
            'tiempoMs': tiempo_ms,
            'mensaje': 'Tiempo de espera agotado al reiniciar el servicio.',
        }
    except Exception as e:
        tiempo_ms = round((time.monotonic() - t0) * 1000, 1)
        logger.exception('Excepción al ejecutar autoreinicio de %s: %s', clave_normalizada, e)
        auditoria.registrar(actor_id, f'AUTOREINICIO_FALLO:{clave_normalizada.upper()}', clave_normalizada, resultado='ERROR')
        return {
            'exito': False,
            'servicio': clave_normalizada,
            'tiempoMs': tiempo_ms,
            'mensaje': f'Error de ejecución: {e}',
        }
