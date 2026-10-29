"""Auditoría de tráfico y sincronización entre versión web y aplicación celular (HU-46).

Permite al encargado de la plataforma auditar el flujo de información bidireccional,
comparar puntajes y avance de lecciones entre dispositivos sincronizados, detectar
discrepancias y levantar alertas tempranas automáticas antes de que afecten a los estudiantes.
"""
import uuid
from datetime import timedelta
from django.db import connection
from django.utils import timezone

from .models import EventoSincronizacion


def _iso(fecha):
    return fecha.isoformat() if fecha else None


def registrar_evento(usuario_id, cliente_tipo, tipo_evento, puntaje_xp=0, lecciones_completadas=0,
                     estado='APLICADO', codigo_resultado=None, detalle=None, evento_cliente_id=None):
    """Registra un evento de sincronización de datos entre cliente y servidor."""
    cliente = 'MOVIL' if str(cliente_tipo).upper() == 'MOVIL' else 'WEB'
    ahora = timezone.now()
    evento_id = str(uuid.uuid4())
    cliente_evt_id = evento_cliente_id or str(uuid.uuid4())

    with connection.cursor() as cursor:
        cursor.execute(
            """
            INSERT INTO operaciones_evento_sincronizacion
            (id, usuario_id, cliente_tipo, evento_cliente_id, tipo_evento, puntaje_xp,
             lecciones_completadas, estado, codigo_resultado, discrepancia_detectada, detalle_payload, recibido_en)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """,
            [
                evento_id,
                usuario_id,
                cliente,
                cliente_evt_id,
                tipo_evento,
                puntaje_xp,
                lecciones_completadas,
                estado,
                codigo_resultado,
                1 if estado == 'ERROR' or codigo_resultado == 'DISCREPANCIA' else 0,
                detalle if isinstance(detalle, str) else None,
                ahora,
            ],
        )
    return evento_id


def auditar_sincronizacion():
    """Audita el tráfico de información y compara lecciones/puntajes entre dispositivos."""
    ahora = timezone.now()
    hace_24h = ahora - timedelta(hours=24)

    with connection.cursor() as cursor:
        # 1. Métricas de tráfico general (últimas 24h)
        cursor.execute(
            """
            SELECT
                COUNT(*) as total,
                SUM(CASE WHEN cliente_tipo = 'WEB' THEN 1 ELSE 0 END) as web,
                SUM(CASE WHEN cliente_tipo = 'MOVIL' THEN 1 ELSE 0 END) as movil,
                SUM(CASE WHEN estado = 'APLICADO' THEN 1 ELSE 0 END) as aplicados,
                SUM(CASE WHEN estado = 'DUPLICADO' THEN 1 ELSE 0 END) as duplicados,
                SUM(CASE WHEN estado = 'ERROR' THEN 1 ELSE 0 END) as errores,
                SUM(CASE WHEN discrepancia_detectada = 1 THEN 1 ELSE 0 END) as discrepancias
            FROM operaciones_evento_sincronizacion
            WHERE recibido_en >= %s
            """,
            [hace_24h],
        )
        fila_trafico = cursor.fetchone()
        total = fila_trafico[0] or 0
        web = fila_trafico[1] or 0
        movil = fila_trafico[2] or 0
        aplicados = fila_trafico[3] or 0
        duplicados = fila_trafico[4] or 0
        errores = fila_trafico[5] or 0
        discrepancias = fila_trafico[6] or 0

        tasa_exito = round(((total - errores) / total) * 100.0, 1) if total > 0 else 100.0

        # 2. Últimos eventos de tráfico (para la bitácora en tiempo real)
        cursor.execute(
            """
            SELECT
                s.id,
                s.usuario_id,
                COALESCE(u.nombre_usuario, 'desconocido') as usuario,
                COALESCE(u.nombre, 'Estudiante') as nombre,
                s.cliente_tipo,
                s.tipo_evento,
                s.puntaje_xp,
                s.lecciones_completadas,
                s.estado,
                s.codigo_resultado,
                s.discrepancia_detectada,
                s.recibido_en
            FROM operaciones_evento_sincronizacion s
            LEFT JOIN identidad_cuenta_usuario u ON u.id = s.usuario_id
            ORDER BY s.recibido_en DESC
            LIMIT 50
            """
        )
        eventos_recientes = []
        for r in cursor.fetchall():
            eventos_recientes.append({
                'id': r[0],
                'usuarioId': r[1],
                'usuario': r[2],
                'nombre': r[3],
                'clienteTipo': r[4],
                'tipoEvento': r[5],
                'puntajeXp': r[6],
                'leccionesCompletadas': r[7],
                'estado': r[8],
                'codigoResultado': r[9],
                'discrepanciaDetectada': bool(r[10]),
                'recibidoEn': _iso(r[11]),
            })

        # 3. Comparación y auditoría de puntajes entre dispositivos sincronizados
        # Buscar usuarios con actividad sincronizada o cuentas de estudiantes
        cursor.execute(
            """
            SELECT DISTINCT s.usuario_id
            FROM operaciones_evento_sincronizacion s
            ORDER BY s.recibido_en DESC
            LIMIT 20
            """
        )
        usuarios_auditar = [r[0] for r in cursor.fetchall()]

        # Si aún no hay eventos en BD, incluir alumnos de prueba del sistema para que el panel no esté vacío
        if not usuarios_auditar:
            cursor.execute(
                """
                SELECT id FROM identidad_cuenta_usuario
                WHERE estado = 'ACTIVO'
                LIMIT 5
                """
            )
            usuarios_auditar = [r[0] for r in cursor.fetchall()]

        comparaciones = []
        for uid in usuarios_auditar:
            cursor.execute(
                """
                SELECT u.id, u.nombre_usuario, u.nombre, u.apellido
                FROM identidad_cuenta_usuario u WHERE u.id = %s
                """,
                [uid],
            )
            usr = cursor.fetchone()
            if not usr:
                continue

            # Puntaje real en Base de Datos (fuente de verdad)
            cursor.execute(
                "SELECT COALESCE(SUM(cantidad), 0) FROM gamificacion_movimiento_experiencia WHERE usuario_id = %s",
                [uid],
            )
            xp_bd = int(cursor.fetchone()[0] or 0)

            # Lecciones completadas en BD
            cursor.execute(
                "SELECT COUNT(*) FROM aprendizaje_progreso_nodo WHERE usuario_id = %s AND estado = 'COMPLETADO'",
                [uid],
            )
            lecciones_bd = int(cursor.fetchone()[0] or 0)

            # Último reporte desde WEB
            cursor.execute(
                """
                SELECT puntaje_xp, lecciones_completadas, recibido_en
                FROM operaciones_evento_sincronizacion
                WHERE usuario_id = %s AND cliente_tipo = 'WEB'
                ORDER BY recibido_en DESC LIMIT 1
                """,
                [uid],
            )
            rep_web = cursor.fetchone()

            # Último reporte desde MOVIL
            cursor.execute(
                """
                SELECT puntaje_xp, lecciones_completadas, recibido_en
                FROM operaciones_evento_sincronizacion
                WHERE usuario_id = %s AND cliente_tipo = 'MOVIL'
                ORDER BY recibido_en DESC LIMIT 1
                """,
                [uid],
            )
            rep_movil = cursor.fetchone()

            xp_web = rep_web[0] if rep_web else xp_bd
            lec_web = rep_web[1] if rep_web else lecciones_bd
            xp_movil = rep_movil[0] if rep_movil else xp_bd
            lec_movil = rep_movil[1] if rep_movil else lecciones_bd

            hay_desfase_xp = abs(xp_web - xp_movil) > 0 or abs(xp_web - xp_bd) > 0 or abs(xp_movil - xp_bd) > 0
            hay_desfase_lec = abs(lec_web - lec_movil) > 0 or abs(lec_web - lecciones_bd) > 0 or abs(lec_movil - lecciones_bd) > 0

            if hay_desfase_xp or hay_desfase_lec:
                estado_sync = 'DISCREPANCIA'
            elif rep_web and rep_movil:
                estado_sync = 'SINCRONIZADO'
            else:
                estado_sync = 'ALINEADO'

            comparaciones.append({
                'usuarioId': usr[0],
                'usuario': usr[1],
                'nombreCompleto': f"{usr[2] or ''} {usr[3] or ''}".strip() or usr[1],
                'puntajeBd': xp_bd,
                'leccionesBd': lecciones_bd,
                'reporteWeb': {
                    'puntajeXp': xp_web,
                    'lecciones': lec_web,
                    'fecha': _iso(rep_web[2]) if rep_web else None,
                },
                'reporteMovil': {
                    'puntajeXp': xp_movil,
                    'lecciones': lec_movil,
                    'fecha': _iso(rep_movil[2]) if rep_movil else None,
                },
                'estado': estado_sync,
                'diferenciaXp': abs(xp_web - xp_movil),
                'diferenciaLecciones': abs(lec_web - lec_movil),
            })

    resumen = {
        'totalTrafico': total,
        'traficoWeb': web,
        'traficoMovil': movil,
        'eventosAplicados': aplicados,
        'eventosDuplicados': duplicados,
        'eventosError': errores,
        'discrepanciasDetectadas': discrepancias,
        'tasaExitoPercent': tasa_exito,
    }

    alertas = evaluar_alertas_sincronizacion(resumen, comparaciones, eventos_recientes)

    return {
        'revisadoEn': ahora.isoformat(),
        'resumen': resumen,
        'alertas': alertas,
        'comparaciones': comparaciones,
        'eventosRecientes': eventos_recientes,
    }


def evaluar_alertas_sincronizacion(resumen, comparaciones, eventos_recientes):
    """Evalúa condiciones de alerta automática por fallas o desincronización entre web y móvil."""
    alertas = []

    # 1. Alerta crítica por errores de transmisión o procesamiento
    errores = resumen.get('eventosError', 0)
    if errores > 0:
        alertas.append({
            'id': 'sync_errores_transmision',
            'nivel': 'CRITICA',
            'titulo': f'Falla de sincronización crítica ({errores} errores detectados)',
            'mensaje': 'Se han producido errores al intentar sincronizar progreso entre el celular y la web.',
            'impacto': 'Los estudiantes afectados no verán reflejados sus avances de clase al cambiar de dispositivo.',
            'sugerencia': 'Comprobar conectividad móvil y reenviar eventos pendientes desde la aplicación escolar.',
        })

    # 2. Alerta por discrepancia de puntajes o lecciones entre dispositivos
    discrepancias = [c for c in comparaciones if c['estado'] == 'DISCREPANCIA']
    if discrepancias:
        total_disc = len(discrepancias)
        nombres = ', '.join(d['usuario'] for d in discrepancias[:3])
        if total_disc > 3:
            nombres += f' y {total_disc - 3} más'
        alertas.append({
            'id': 'sync_discrepancia_puntajes',
            'nivel': 'ADVERTENCIA',
            'titulo': f'Desfase de puntajes detectado ({total_disc} cuentas desalineadas)',
            'mensaje': f'El puntaje reportado por el celular difiere de la versión web en: {nombres}.',
            'impacto': 'Los alumnos pueden percibir que perdieron puntos XP o que sus lecciones no se guardaron.',
            'sugerencia': 'Aplicar conciliación de sincronización para alinear el estado con la base de datos central.',
        })

    # 3. Alerta por tasa de sincronización degradada
    tasa = resumen.get('tasaExitoPercent', 100.0)
    total_trafico = resumen.get('totalTrafico', 0)
    if total_trafico >= 10 and tasa < 95.0:
        alertas.append({
            'id': 'sync_tasa_degradada',
            'nivel': 'ADVERTENCIA',
            'titulo': f'Tasa de sincronización degradada ({tasa} %)',
            'mensaje': 'Más del 5 % de las solicitudes de sincronización han fallado o presentado anomalías.',
            'impacto': 'Incremento de tráfico repetido y demoras de sincronización en tabletas escolares.',
            'sugerencia': 'Verificar estabilidad de la red escolar o reiniciar el servicio de recepción.',
        })

    return alertas
