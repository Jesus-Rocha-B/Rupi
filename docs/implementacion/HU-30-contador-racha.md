# HU-30 · Contador de racha junto a Rupi

Implementación: 26 de octubre de 2026.

> Como estudiante móvil, yo debería visualizar un contador dinámico de rachas de días consecutivos junto al avatar de Rupi a fin de comprometerme con un hábito constante de aprendizaje.

## Uso
En «Mi Camino», la tarjeta de Rupi que acompaña la ruta muestra una insignia de racha debajo de su mensaje:
- **N días seguidos · ¡Hoy ya sumaste un día!** cuando hoy ya completó una parada.
- **N días seguidos · Una actividad hoy y tu racha sigue.** cuando la racha viene de ayer y espera actividad (borde punteado).
- **Tu racha empieza hoy · Completa una actividad para empezarla.** sin racha.

El contador se vuelve a consultar cada vez que cambia el número de paradas completadas de la ruta abierta. En la vista de alumno que usa un docente no se muestra. Si la consulta falla, Rupi sigue visible sin el contador y sin mostrar un error que distraiga.

## API
`GET /api/v1/student/streak` → `{ days, activeToday, atRisk, localDate }`. 401 sin sesión. Usa el mismo cálculo de HU-27 (`StudyStreakService`): paradas completadas, días en hora de Perú, sin contador persistido.

## Diseño
Sigue `human-crafted-frontend` y `educational-game-ux`: insignia de papel con borde de 2 px y sombra sólida, llama de Lucide (sin emojis), Nunito 900 y colores de la paleta (amarillo sol, naranja Rupi y verde bosque). El estado en espera se distingue con texto y borde, no solo con color. La llama solo se anima si el sistema no pide movimiento reducido. Es informativo y no interactivo; el texto cumple contraste AA. Estilos en `StreakBadge.css`, fuera de `App.css`.

## Pendiente
- Android aún no muestra el contador: el endpoint ya está disponible para la app.
- No se hizo prueba visual en navegador a 375 y 1280 px en esta entrega.

## Pruebas
- Cálculo cubierto por `StudyStreakServiceTest` (HU-27).
- Build, lint de los archivos nuevos y 27 pruebas web correctas.
