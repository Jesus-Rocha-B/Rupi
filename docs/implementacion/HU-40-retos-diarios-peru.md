# HU-40 · Retos diarios contextualizados a la realidad peruana

Implementación: 28 de octubre de 2026 · commit `8aaa347`.

> Como estudiante móvil, quiero acceder a retos diarios de razonamiento lógico y comprensión lectora, contextualizados a la realidad peruana, para aplicar lo aprendido en situaciones cotidianas.

## Criterios de aceptación (definidos en la implementación)
- Cada día hay un reto según el grado: **razonamiento lógico** o **comprensión lectora**.
- Las situaciones son peruanas y cotidianas, y el lenguaje corresponde a la banda de grados.
- Para terminar hay que responder todas las preguntas. Cada respuesta muestra su explicación.
- Terminar da +30 XP una sola vez al día y solo vale el reto que toca hoy.
- Está en la web y en Android, con lectura en voz alta (HU-39).

## Estado previo
Existían `DailyChallengeController` y `DailyChallengeService`, pero sin retos en la base, sin pantalla, con «hoy» en hora del servidor y dando XP sin comprobar respuestas.

## Datos · `V30__retos_diarios_peru_hu40.sql`
Banco de 12 retos (4 por banda: 1.°-2.°, 3.°-4.° y 5.°-6.°), cada uno con 2 preguntas de opción única y explicación:
- **Lógicos:** alpacas en Puno, panadería del barrio, feria de Huancayo, fila de la combi, viaje por la Panamericana Sur, chacra de quinua.
- **Lectura:** cometa en Pimentel, lonchera saludable, el colibrí, la minga del barrio, islas de totora del Titicaca, carta desde Iquitos.

Los de lectura guardan el texto en un bloque `TEXTO`; así el servicio sabe el tipo de reto sin columna nueva. Las alternativas están rotadas para que la correcta no quede siempre en el mismo lugar. Las filas de `aprendizaje_reto_diario` usan fechas del 1 al 4 de octubre como banco. Escritor: Spring Boot; `INSERT IGNORE` idempotente.

## Reglas del servicio
1. Grado del aula activa (2.° si no tiene aula, como antes).
2. Si hay un reto publicado con la fecha de hoy (Lima), se usa ese. Si no, se rota el banco del grado: `epochDay mod tamaño`, un reto distinto cada día.
3. `POST /{reto}/complete` exige que sea el reto de hoy (409 si no), que todas las preguntas tengan respuesta guardada hoy (409 «Responde todas… (1 de 2)») y da 30 XP con idempotencia diaria.

## API
- `GET /api/v1/student/daily-challenge` → suma `skill` (`RAZONAMIENTO_LOGICO` | `COMPRENSION_LECTORA`) y `readingText`. `date` ahora es el día de Lima.
- Respuestas: `POST /api/v1/student/activities/{actividad}/questions/answer` (el mismo de las lecciones).
- `POST /api/v1/student/daily-challenge/{reto}/complete` → suma `correctAnswers` y `totalQuestions`.

## Uso
- **Web:** tarjeta «Reto del día» en «Mi Panel», con banda naranja Rupi (lógico) o azul cielo (lectura), lectura en papel punteado, alternativas de 48 px y botón Terminar que se habilita al responder todo.
- **Android:** «Mis temas» → **Reto del día**.

## Pruebas
- `DailyChallengeServiceTest` (5): rotación diaria, mensaje, respuestas incompletas, XP una vez y solo el reto de hoy.
- `tests/dailyChallenge.test.ts`: etiquetas, texto para escuchar y regla para terminar.
- Punta a punta contra MySQL (copia local con V22 a V30):
  - API: terminar sin responder da 409, terminar da 30 XP, repetir da 0 XP y otro reto da 409.
  - Playwright a 375 y 1280 px: nada se desborda y todos los objetivos miden 48 px o más.
  - Android en emulador: «¡Acertaste las 2 preguntas! … +30 XP».

## Pendiente
- Panel docente para publicar retos propios por fecha. Hoy se insertan en `aprendizaje_reto_diario`.
- V28 (HU-32) falla en una base sin el aula `AULA-3A` del equipo, por sus datos de demostración. No afecta a V30.
