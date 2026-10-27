# HU-29 · Puntos de experiencia y medallas

Revisión: 26 de octubre de 2026.

> Como estudiante móvil, yo debería acumular puntos de experiencia y medallas virtuales al completar actividades a fin de mantenerme motivado a seguir practicando diariamente.

## Estado
El otorgamiento ya existía desde HU-05, HU-07 y HU-08 del repo:
- +50 XP al completar una parada, una sola vez gracias a la clave de idempotencia (`gamificacion_movimiento_experiencia`).
- Insignias por lección y la Gran Insignia «Gran Gallito de Oro» con +100 XP al terminar una ruta (`gamificacion_insignia_usuario`).
- Vitrina de insignias en el perfil.

## Corrección de esta revisión
La vitrina web estimaba el XP en el navegador como paradas completadas × 50, más 100 si tenía la Gran Insignia. Esa cuenta no veía los movimientos de retos, ajustes ni reversos, ni las rutas que el estudiante ya no tiene en la lista.

Ahora `GET /api/v1/student/experience` devuelve `totalXp` (suma del libro de movimientos) y `badges` (insignias obtenidas). Se calcula en consulta, sin total persistido. La vitrina lo consulta al abrirse; si falla, conserva la estimación anterior para no mostrar un cero falso.

## Pendiente
La app Android aún no muestra XP ni medallas: hoy se centra en repaso, cuestionarios y avisos. El endpoint ya sirve para incorporarlos.

## Pruebas
- Consultas validadas en MySQL 8.4 local.
- Suite Maven completa: 279 casos, 0 fallos y 81 omitidos. Build, lint de los archivos tocados y 27 pruebas web correctas.
