# HU-36 · Comparación del puntaje de hoy con el de ayer

Implementación: 28 de octubre de 2026 · commit `ff897fd`.

> Como usuario de una app móvil, quiero ver una comparación breve entre el puntaje de hoy y el de ayer para comprobar si supero mi propio ritmo de estudio.

## Criterios de aceptación (definidos en la implementación)
- Se compara el XP ganado hoy con el de ayer, en horario de Lima.
- Gastar XP en la tienda de Rupi (HU-31) no baja el puntaje del día: solo cuentan movimientos positivos.
- Se indica si va mejor, igual o por debajo de ayer, con un mensaje de Rupi y las paradas de cada día.
- La comparación está en la web y en Android, y se puede escuchar.

## API
`GET /api/v1/student/daily-goal/comparison` →
`{ today, yesterday, todayExperience, yesterdayExperience, difference, trend: SUBE|IGUAL|BAJA, todayLessons, yesterdayLessons, message }`.
Se calcula en la consulta desde `gamificacion_movimiento_experiencia` y `aprendizaje_progreso_nodo`: no se guarda ningún total (invariante de AGENTS.md).

## Uso
- **Web:** tarjeta «Hoy vs. ayer» debajo de la meta del día en «Mi Panel», con dos barras proporcionales, flecha de tendencia y botón Escuchar. Se vuelve a consultar al terminar el reto diario (HU-40).
- **Android:** sección «Hoy vs. ayer» en «Mi meta de hoy».

## Código
`DailyGoalProgressService.getScoreComparison`, `ScoreComparison.tsx`, `features/dashboard/scoreComparison.ts` (cálculo de barras) y `MainActivity.scoreComparison`.

## Pruebas
- `DailyGoalProgressServiceTest`: sube, baja y sin actividad (7 pruebas).
- `tests/scoreComparison.test.ts`: barras proporcionales, mínimo visible y sin división entre cero.
- Punta a punta contra MySQL: 0 XP, luego 30 XP tras el reto diario con tendencia SUBE (web con Playwright y Android en emulador).
