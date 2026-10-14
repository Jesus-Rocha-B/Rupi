# HU-14 · Enviar recordatorios automáticos

Rupi crea un aviso interno para cada alumno durante las 24 horas anteriores al inicio de una evaluación registrada en el calendario HU-13. El servidor revisa las fechas cada minuto; no depende de que un docente o estudiante abra la pantalla.

## Cómo usarlo

1. El docente registra una evaluación y sus fechas en **Calendario del aula**. La ruta y la actividad deben estar publicadas.
2. Cuando faltan 24 horas o menos, el proceso automático deja un aviso a los estudiantes activos del aula inscritos en esa ruta.
3. El alumno encuentra **Tus próximas evaluaciones** junto al calendario. La bandeja se actualiza cada minuto mientras la página está visible y al volver a ella; también ofrece **Actualizar avisos**.
4. **Ya lo leí** guarda la lectura en MySQL y permanece al recargar o iniciar otra sesión.

Son avisos dentro de Rupi, sin correo, SMS ni notificaciones del sistema operativo. Los horarios se muestran en Perú y se almacenan como UTC. El servidor debe estar encendido: al reiniciarse recupera las evaluaciones todavía futuras dentro de la ventana; no manda recordatorios atrasados de evaluaciones que ya comenzaron.

## Programación y deduplicación

La fuente de programación es `escuela_planificacion_ruta`. Se considera `tipo='EVALUACION'`, vinculada a una parada RETO o CUESTIONARIO publicada. No se generan avisos de unidades, actividades retiradas, aulas inactivas, usuarios deshabilitados o inscripciones suspendidas/retiradas.

`escuela_recordatorio_evaluacion` conserva el aviso y su lectura. La restricción única comprende alumno + aula + versión de ruta + nodo de evaluación + fecha y hora de inicio. Un INSERT atómico con ON DUPLICATE KEY UPDATE sin cambios evita duplicados entre ciclos, procesos concurrentes y reinicios. No se usa solo una variable en memoria ni se depende de deshabilitar un botón.

- Guardar otra vez la misma fecha no genera otro aviso ni borra la lectura.
- Cambiar la fecha de inicio oculta inmediatamente el aviso viejo en la siguiente consulta; la nueva fecha puede generar un aviso cuando entra en la ventana.
- Cambiar únicamente el fin o el título no repite el aviso.
- Retirar el evento lo oculta. Borrarlo y volver a crearlo para la misma evaluación, aula y fecha reutiliza el aviso original.
- Dos evaluaciones distintas el mismo día mantienen avisos independientes.
- Al comenzar la evaluación, el aviso deja de aparecer. Los registros se conservan como historial de deduplicación; no se borra ese historial al limpiar la bandeja.

Las consultas vuelven a comprobar la membresía, inscripción, publicación y coincidencia con la fecha actual. Un cambio de aula o suspensión revoca el acceso. El alumno solo puede marcar sus avisos vigentes como leídos.

## API y proceso automático

- `GET /api/v1/student/learning-routes/{versionRouteId}/reminders`: avisos vigentes, con aula, título, inicio y estado de lectura.
- `PUT /api/v1/student/learning-routes/{versionRouteId}/reminders/{reminderId}/read`: confirma lectura de forma idempotente, responde 204.
- 401 sin sesión; 404 para una ruta o aviso ajeno, eliminado, vencido o reprogramado.

`ReminderScheduler` usa `@Scheduled`: intervalo predeterminado de 60000 ms y primera ejecución a los 10000 ms. Se puede desactivar con `--rupi.reminders.enabled=false`; los intervalos operativos usan `rupi.reminders.poll-ms` y `rupi.reminders.initial-delay-ms`. La anticipación funcional es de 24 horas. Un fallo de base de datos queda registrado y se reintenta en el próximo ciclo.

La escritura de estos recordatorios escolares pertenece a Spring Boot (`escuela_*`). Las tablas generales `notificacion_*` de historias posteriores se conservan para su propia implementación.

## Actualización local

Requiere HU-13/V13 y MySQL iniciado. Desde la raíz:

```powershell
.scriptsActualizar-HU14.ps1
```

El script aplica V14 y los permisos de la cuenta API, es repetible y compatible con PowerShell 5.1. Acepta `-RootConfig` y `-ApiUser` para otros entornos. No crea fechas de prueba ni modifica las aulas.

Detén la API antes de recompilar en Windows, porque el proceso mantiene abierto el JAR. Luego inicia con `.Iniciar-Rupi.ps1 -Recompilar`. En esta entrega la migración ya se aplicó a la base local y la API se reinició con HU-14.

## Pruebas

- 123 pruebas Java correctas: 107 existentes, 7 nuevas de servicio/planificador y 9 de integración MySQL.
- 21 pruebas TypeScript existentes correctas; compilación y lint correctos.
- Integración MySQL real: borde exacto de 24 horas, fechas vencidas, exclusión de unidades, ciclos repetidos, dos trabajadores simultáneos, lectura persistente, reprogramación/cancelación/recreación, aislamiento de alumno, aula inactiva, ruta no publicada e inscripción suspendida.
- API real: se comprobó que la fila se crea por el planificador antes de abrir la bandeja; lectura repetida, respuestas 401/404 y ausencia de duplicados tras varios ciclos.
- Navegador Edge con API/MySQL reales: 375 px, botones accesibles por teclado, lectura tras recargar, actualización automática tras reprogramar, recuperación de error y horario de Perú en navegador configurado en Tokio.

Las pruebas MySQL están desactivadas por defecto. Para ejecutarlas se necesita una instancia **aislada y descartable** en 127.0.0.1:3308/rupi con el esquema, V14 y las semillas de rutas/alumno demo (HU-01/HU-02). Define `RUPI_DB_URL`, `RUPI_DB_USERNAME`, `RUPI_DB_PASSWORD` de esa instancia y `RUPI_HU14_TEST_DB=isolated`. Ejecuta `mvn.cmd -f backend/api-spring-boot/pom.xml test`. La prueba valida la URL y limpia únicamente las dos tablas de calendario/recordatorios de esa instancia; no la ejecutes contra una base compartida. Mantén detenida su API durante las pruebas para que su reloj real no interfiera con el reloj fijo de los casos de prueba.
