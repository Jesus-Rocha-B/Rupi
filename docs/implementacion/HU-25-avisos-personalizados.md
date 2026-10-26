# HU-25 · Alertas y notificaciones personalizadas

Implementación: 26 de octubre de 2026.

## Tareas
1. Permitir elegir metas y tipos de recordatorio.
2. Enviar avisos solo de acuerdo con las preferencias guardadas.
3. **Respetar horarios de descanso y evitar avisos duplicados.** Tarea adicional.

## Uso
Web: perfil del estudiante → Mi ritmo de estudio.
Android: Mis temas → Mi ritmo de estudio y avisos.
El estudiante elige una meta de 1–5 actividades, recordatorios de meta pendiente, evaluaciones próximas y horas de descanso. Los horarios usan America/Lima. Las preferencias se guardan por cuenta, compartidas entre dispositivos. Un conflicto de edición exige recargar para no sobrescribir cambios de otro dispositivo.

Los avisos nuevos están desactivados hasta guardar preferencias. La meta no cambia las notas y el avance se consulta desde los nodos completados; no se almacena otro porcentaje.

En Android, guardar con avisos del sistema habilitados solicita el permiso de notificaciones. Negarlo no impide consultar la bandeja dentro de la aplicación. La web muestra una bandeja interna; no implementa Web Push.

## Servidor y datos
Spring Boot gestiona el dominio de notificaciones y gamificación. No modifica el catálogo de identidades o currículo. V26__preferencias_recordatorios_hu25.sql reutiliza las tablas existentes de preferencias, categorías, horarios, mensajes, entregas y metas. Añade:
- gamificacion_preferencia_estudio: meta elegida y revisión de preferencias.
- notificacion_evento_usuario: clave única por cuenta y evento, con reclamación de entrega.

Migración aplicada a la base TiDB compartida, con TLS y credenciales privadas, después de probar el esquema en MySQL aislado. No se añadieron cuentas ni avisos ficticios a TiDB.

Un barrido del servidor prepara avisos cada minuto para cuentas configuradas. La lectura de la bandeja también los prepara. La evaluación debe estar en las próximas 24 horas y conservar matrícula, aula activa y ruta/actividad publicada. Cambiar la fecha crea un evento nuevo y oculta el anterior. La HU-14 respeta ahora la preferencia explícita de evaluaciones; para cuentas aún no configuradas conserva su comportamiento anterior.

La meta pendiente produce como máximo un aviso por día local. Una evaluación usa ruta, nodo y fecha como identidad. Bloqueo transaccional de preferencias y claves únicas evitan duplicados entre servidores; la reclamación atómica impide que varios dispositivos presenten el mismo aviso.

## Android y límites
JobScheduler consulta con red disponible en ciclos de al menos 15 minutos. El sistema puede demorar la ejecución por batería, reposo o restricciones. No es entrega instantánea ni una integración FCM. Forzar la detención, perder conexión o expirar la sesión puede impedir avisos hasta reabrir/iniciar sesión. La bandeja sigue disponible al volver con conexión.

Solo se conserva la cookie de sesión, cifrada con AES/GCM y clave en Android Keystore, durante un máximo de 24 horas y siempre sujeta a validación del servidor. No se guardan contraseñas. Cerrar sesión borra la copia privada, cancela el trabajo y retira avisos del dispositivo. Las notificaciones usan visibilidad privada en pantalla bloqueada.

ENVIADA indica entrega al cliente Android, no confirmación de lectura ni garantía de presentación del sistema. Si el proceso se interrumpe después de reclamar, no se vuelve a emitir: el aviso permanece en la bandeja. Se prioriza no duplicar. La restricción es por cuenta; un aviso se presenta en un solo dispositivo.

Configuración del servidor: rupi.study-notifications.enabled (predeterminado true), poll-ms (60000), initial-delay-ms (30000). No se necesita proveedor ni clave de IA.

## Pruebas
- Maven: 271 casos, 225 ejecutados correctamente y 46 omitidos por condiciones de otros entornos.
- Nueve pruebas de integración HU-25 con MySQL aislado: valores iniciales, persistencia, concurrencia, descanso, validaciones, revisión optimista, lectura, meta completada, nuevo día, preferencias de evaluación, reprogramación y baja de matrícula.
- Web: 27 pruebas existentes, build y comprobaciones de interfaz adaptable con datos simulados.
- Android: APK y lint sin errores; permiso concedido en emulador; notificación del sistema comprobada; dos ejecuciones del trabajo dejaron una sola entrega PUSH en la base aislada; restauración cifrada de sesión después de reiniciar el proceso.
- No se enviaron notificaciones a estudiantes reales durante las pruebas.

Referencias: [permisos de notificaciones Android](https://developer.android.com/develop/ui/views/notifications/notification-permission) y [JobInfo](https://developer.android.com/reference/android/app/job/JobInfo.Builder#setPeriodic(long)).
