# HU-28 · Alertas cuando el docente publica materiales o fechas de examen

Implementación: 26 de octubre de 2026.

> Como usuario móvil con rol de estudiante, necesito recibir alertas en mi dispositivo cuando el profesor publique o habilite nuevos materiales o fechas de examen.

## Qué se avisa
1. **Nueva fecha de evaluación:** el docente programa o reprograma una evaluación en el calendario del aula (HU-13). Usa la misma elegibilidad de HU-14: aula activa, matrícula de estudiante activa, inscripción activa o completada, ruta publicada del grado y actividad RETO o CUESTIONARIO publicada. La fecha debe ser futura.
2. **Nuevo material:** el docente adjunta un material o enlace a una parada (HU-07) de una ruta publicada en la que el estudiante está inscrito. Solo materiales posteriores a la inscripción, para no llenar la bandeja de quien recién se une.

Se consideran novedades de los últimos 7 días. HU-14 sigue enviando además su recordatorio 24 horas antes; son avisos distintos.

## Deduplicación y vigencia
- Evaluación: clave `FECHA:<ruta>:<nodo>:<inicio>`. Reprogramar la fecha genera otro aviso; volver a guardar la misma fecha o cambiar solo el título no repite.
- Material: clave `MATERIAL:<id del recurso>`.
- Las claves únicas de `notificacion_evento_usuario` (HU-25) evitan duplicados entre ciclos y servidores.
- Al leer la bandeja se vuelve a comprobar que la novedad siga vigente: si la fecha pasó o cambió, se retiró el material, la ruta se pausó o archivó, o el estudiante dejó la ruta, el aviso se oculta. Para las fechas también cuenta dejar el aula; el material depende solo de la inscripción en la ruta.
- Vencimiento: la evaluación al comenzar; el material a los 7 días.

## Preferencias
Usa el interruptor de evaluaciones de HU-25 (categoría TAREA), ahora rotulado «Avisos de mi docente: nuevas fechas de evaluación, materiales nuevos y evaluaciones de las próximas 24 horas» en la web y «Avisos de mi docente: fechas de evaluación y materiales nuevos» en Android. Respeta el descanso y la entrega única por dispositivo de HU-25.

## Servidor y datos
Spring Boot solo lee `escuela_planificacion_ruta`, `aprendizaje_nodo_recurso` y la inscripción, y escribe en `notificacion_*` como en HU-25. No hay migración nueva ni cambios en la autoría docente: la novedad se detecta por consulta, sin depender de que el panel docente llame a algo extra. Las fechas se comparan en UTC (`actualizado_en` y `creado_en` se escriben con `UTC_TIMESTAMP(6)`).

## Límites
- «Habilitar» una parada por fecha (`disponible_desde`, V20) todavía no genera aviso.
- Los materiales de biblioteca MINEDU y los recursos generados por IA (HU-21 a HU-23) no se avisan con esta historia.
- Android no recibe avisos instantáneos: depende del ciclo de JobScheduler descrito en HU-25.

## Pruebas
- Las dos consultas nuevas se validaron con `EXPLAIN` en MySQL 8.4 local: usan índices por estudiante.
- Suite Maven completa: 279 casos, 0 fallos y 81 omitidos. Build y 27 pruebas web correctas.
- No hay prueba de integración con MySQL aislado ni prueba de punta a punta para esta historia todavía.
