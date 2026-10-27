# HU-27 · Aviso motivacional antes de perder la racha

Implementación: 26 de octubre de 2026.

> Como usuario móvil con rol de estudiante, deseo recibir notificaciones motivacionales cuando esté a punto de perder mi racha diaria de resolver ejercicios (nodos).

## Tareas
1. Calcular la racha diaria del estudiante.
2. Detectar cuándo la racha está en riesgo y avisar una sola vez por día.
3. Respetar las preferencias y el horario de descanso de HU-25.

## Regla de la racha
Un día cuenta cuando el estudiante completa al menos una parada (`aprendizaje_progreso_nodo.completado_en`), contado en hora de Perú (America/Lima) aunque la fecha se guarde en UTC. La racha son los días seguidos que terminan hoy; si hoy todavía no hay actividad, terminan ayer. Así no se pierde durante el día: queda **en espera** hasta completar algo o hasta que termine el día.

La racha se calcula en cada consulta (`StudyStreakService`) a partir de las paradas completadas del último año. No se guarda ningún contador ni porcentaje en otra columna.

## Aviso
- Se prepara en el mismo barrido de HU-25 (cada minuto) y al leer la bandeja.
- Solo si la racha está en espera: hay racha de ayer y hoy no hay ninguna parada completada.
- Hora: dos horas antes del inicio del descanso. Nunca antes de la hora de recordatorio que eligió el estudiante ni dentro del descanso. Con descanso a las 21:00 y recordatorio a las 17:00, el aviso sale a las 19:00.
- Clave `RACHA:<fecha local>`: como máximo un aviso por día, también con varios servidores. Vence al terminar el día.
- Desaparece de la bandeja en cuanto el estudiante completa una actividad.
- Depende del interruptor «Recordar mi meta pendiente y mi racha». Sin preferencias guardadas no se envía nada, igual que en HU-25.
- Android lo recibe por la misma bandeja y la misma reclamación de HU-25, con su permiso y límites de JobScheduler.

Texto: «Tu racha te espera · Llevas N días seguidos aprendiendo. Si hoy completas una actividad, tu racha sigue creciendo. ¡Tú marcas el ritmo!». Sigue la guía `educational-game-ux`: celebra el hábito, sin culpa ni presión, y nunca anuncia que la racha «se perdió».

## Pruebas
- `StudyStreakServiceTest` (5): sin actividad, días seguidos hasta hoy, racha en espera desde ayer, día perdido y varias actividades el mismo día.
- `StreakReminderTimeTest` (3): dos horas antes del descanso, nunca antes de la hora elegida y descanso después de medianoche.
- Suite Maven completa: 279 casos, 0 fallos y 81 omitidos por requerir bases aisladas.
- No se probó de punta a punta contra la API ni en el emulador Android en esta entrega.
