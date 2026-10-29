# HU-46 · Auditar el tráfico de sincronización entre Web y Celular

Implementación completa: 29 de octubre de 2026.

> Como encargado de la plataforma, yo debería auditar el tráfico de información que viaja entre la versión de escritorio y la aplicación celular a fin de garantizar que las lecciones y puntajes de los niños estén siempre sincronizados en ambos lados.

## Subtareas completadas al 100%

1. **Configurar registros en la base de datos para el tráfico entre web y móvil:**
   - Migración `database/mysql/migrations/V36__auditoria_sincronizacion_web_movil_hu46.sql`.
   - Ampliación de `operaciones_evento_sincronizacion` con columnas para identificar origen (`cliente_tipo`: 'WEB' / 'MOVIL'), métricas reportadas (`puntaje_xp`, `lecciones_completadas`), bandera de discrepancia (`discrepancia_detectada`) y traza extendida (`detalle_payload`).
   - Permiso `sincronizacion.auditar` concedido a `ADMINISTRADOR` y `SOPORTE`.

2. **Desarrollar el servicio backend para auditar y comparar puntajes entre los dispositivos sincronizados:**
   - Módulo en Django (`backend/admin-django/operaciones/sincronizacion.py`):
     - `registrar_evento`: registra los eventos de tráfico y progreso de cada plataforma.
     - `auditar_sincronizacion`: compara alumno por alumno el puntaje XP y las lecciones completadas reportadas por el celular frente a las reportadas por la web y la fuente de verdad central de la base de datos (`gamificacion_movimiento_experiencia` y `aprendizaje_progreso_nodo`).
     - Endpoints en `operaciones/vistas.py`: `GET /api/admin/v1/sincronizacion` y `POST /api/admin/v1/sincronizacion`.

3. **Diseñar un panel web para visualizar posibles errores de sincronización en tiempo real:**
   - Componente en React (`frontend/web-react/src/features/admin/SyncAuditView.tsx`):
     - Fichas de indicadores clave: tráfico total últimas 24h, desglose Web vs Celular, porcentaje de éxito de sincronización, discrepancias activas y errores.
     - Pestaña de comparativa por estudiante con alertas de desfase (XP o lecciones).
     - Pestaña de bitácora de tráfico en tiempo real con filtrado y estados de entrega.
     - Integrado en la barra superior de la consola administrativa (`AdminApp.tsx`).

4. **Programar alertas automáticas cuando se detecten fallas de sincronización entre ambas plataformas:**
   - Motor de alertas preventivas en `sincronizacion.py` (`evaluar_alertas_sincronizacion`):
     - Alerta crítica por errores de transmisión (`sync_errores_transmision`): informa qué fallos impiden que el estudiante vea su avance reflejado al cambiar de equipo.
     - Alerta por desfase de puntajes (`sync_discrepancia_puntajes`): identifica a los estudiantes con discrepancias entre su celular y su computadora.
     - Alerta por tasa degradada (`sync_tasa_degradada`): advierte si el éxito de sincronización cae por debajo del 95 %.
     - Sección destacada con badges de estado en el panel web.

## Pruebas
- Pruebas unitarias en Django (`backend/admin-django/operaciones/tests.py` - `SincronizacionTest`):
  - Detección de errores de transmisión críticos.
  - Detección de discrepancias de puntaje entre web y móvil.
  - Comprobación de estado limpio sin falsos positivos.
- Verificación de compilación de frontend con TypeScript y Vite (`npm run build`).
- Verificación de pruebas unitarias de TypeScript/Node (`node --test`).
