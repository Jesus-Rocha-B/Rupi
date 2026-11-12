# HU-57 · Programar respaldos automáticos

Implementación: 12 de noviembre de 2026.

> Como administrador del sistema, yo quiero programar respaldas automatizadas de la base de datos principal fuera de las horas punta de tráfico escolar (horarios de colegio).

## Criterios de aceptación implementados
- **Tarea programada en el servidor (Django + scripts de programación):**
  - Comando Django `python manage.py ejecutar_respaldo_programado` (`--verificar-ventana` o `--forzar`).
  - Scripts PowerShell compatibles con 5.1 y 7+: `scripts/Ejecutar-Respaldo-Programado.ps1` y `scripts/Instalar-Tarea-Respaldo.ps1`.
  - Línea `cron` lista para Linux con `TZ=America/Lima`.
- **Horario exclusivo de madrugada para evitar tráfico escolar:**
  - La política restringe la programación a **00:00 – 05:59 (`America/Lima`)** mediante validación en Django (`operaciones/respaldos.py`), restricciones `CHECK` en MySQL (`V47`) y validación en la consola web y scripts de servidor. Cualquier intento entre 06:00 y 23:59 es rechazado con código `400`.
  - Si `--verificar-ventana` se ejecuta durante horario escolar o si ya hubo un respaldo programado en esa fecha, se omite de forma segura.
- **Almacenamiento externo y seguro:**
  - Volcado consistente InnoDB con `mysqldump --single-transaction --routines --triggers --quick`, credenciales pasadas mediante `--defaults-extra-file` temporal (`0600`, eliminado al finalizar), compresión `gzip`, cifrado opcional AES-256 (`RUPI_BACKUP_ENCRYPTION_KEY`) y verificación SHA-256 en destino.
  - Soporta carpeta externa montada fuera de `mysql-data` (`CARPETA_SEGURA`), endpoint `S3_COMPATIBLE` por HTTPS y transferencia `SFTP_REMOTO`, con limpieza automática por retención (`retencion_dias`, 7–365 días).
- **Sistema de alertas ante fallos nocturnos:**
  - Ante cualquier fallo en el volcado, cifrado o transferencia externa, Django marca el registro como `FALLIDO`, abre un incidente en `operaciones_incidente` (`servicio = 'respaldo_nocturno'`), audita en `operaciones_evento_auditoria` y despacha notificación inmediata por correo y SMS/Webhook usando la estrategia multicanal de `operaciones/notificaciones_alertas.py`.

## Datos · `database/mysql/migrations/V47__respaldos_automaticos_hu57.sql`
- `operaciones_politica_respaldo` (fila única `id = 1`): configuración horaria de madrugada, frecuencia (`DIARIO`/`SEMANAL`), retención, tipo y ruta de almacenamiento externo, cifrado y correo de alerta.
- Columnas adicionales en `operaciones_respaldo`: `origen` (`PROGRAMADO`/`MANUAL`), `almacenamiento_externo`, `detalle_error`, `alerta_enviada` e índice `ix_respaldo_iniciado_en`.
- Permisos: `respaldos.ver` (`ADMINISTRADOR` y `SOPORTE`) y `respaldos.gestionar` (`ADMINISTRADOR`).
- `database/mysql/grants-django-admin.sql` actualizado para otorgar `INSERT, UPDATE` al usuario `rupi_admin` sobre las tablas `operaciones_*` gestionadas por Django.

## API Django (`/api/admin/v1`)
| Método y ruta | Permiso | Uso |
|---|---|---|
| `GET respaldos` | `respaldos.ver` | Política vigente, límites de madrugada, comandos cron/Windows, canales de alerta, estadísticas 30d e historial. |
| `PUT respaldos/politica` | `respaldos.gestionar` | Guarda horario en madrugada, almacenamiento externo, retención y alertas por fallo (`POLITICA_RESPALDO_ACTUALIZADA`). |
| `POST respaldos/ejecutar` | `respaldos.gestionar` | Ejecuta un respaldo inmediato hacia almacenamiento externo (`201` si verificado, `500` con alerta si falla). |
| `POST respaldos/probar-alerta` | `respaldos.gestionar` | Envía notificación simulada de fallo nocturno para comprobar canales. |

## Consola Web de Administración
- **Monitoreo → Respaldos automáticos** (`frontend/web-react/src/features/admin/BackupsView.tsx`): formulario táctil para programación nocturna, almacenamiento externo, pruebas de alertas, copia de comandos `cron`/`Task Scheduler`, botón «Respaldar ahora» e historial con estado SHA-256 y alertas emitidas.

## Verificación
- Pruebas unitarias Django (`operaciones/test_respaldos.py`): validación de ventana madrugada, rutas seguras, cálculo de próxima ejecución en `America/Lima`, verificación SHA-256 externa, disparo de alertas/incidentes en fallo, comando programado y permisos RBAC.
- Verificación end-to-end contra MySQL y Django reales: `scripts/verify_hu57_respaldos.py` (incluye ejecución real de `mysqldump`, verificación `.sql.gz`/`.sha256` y prueba real de fallo con envío de correo).
