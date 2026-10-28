# HU-37 · Pistas y explicaciones de la IA según mi nivel

Implementación: 28 de octubre de 2026 · commit `5d0ed25`.

> Como estudiante móvil, quiero recibir pistas y explicaciones de la IA adaptadas a mi nivel para entender mis errores en distintos cursos.

## Criterios de aceptación (definidos en la implementación)
- Tras una respuesta equivocada en la práctica con IA (HU-21) aparece **¿Por qué me equivoqué?**.
- La explicación dice qué eligió, cuál era la correcta y por qué, en pasos.
- El lenguaje depende del grado: **INICIAL** (1.° y 2.°), **INTERMEDIO** (3.° y 4.°) o **AVANZADO** (5.° y 6.°).
- Si la práctica va por debajo de la mitad de aciertos (con dos o más respuestas), los pasos son más cortos («paso a paso»).
- Sin IA configurada, la pista se arma con la explicación revisada de la pregunta: nunca inventa contenido.

## API
`POST /api/v1/student/ai-quizzes/{practica}/questions/{pregunta}/hint` (exige `Origin`, como el resto de escrituras).
Respuesta: `{ questionId, grade, level, reinforced, hint, steps[], realLifeExample, source: IA|EXPLICACION_REVISADA }`.
- 409 si la pregunta aún no se responde o si se respondió bien. 404 si no es de su práctica.
- Con IA: una generación y una revisión automática (correcta, apropiada para el nivel, segura). Si la revisión falla, se usa la explicación revisada.
- La pista se guarda en memoria por intento y pregunta (máximo 500): una sola llamada de pago por error.

## Tutor de chat
`service-ia/prompts.py` suma la regla 7 (lenguaje por nivel) y `main.py` envía `NIVEL DEL ESTUDIANTE` calculado del grado (`level_for_grade`).

## Uso
Web (`QuizPractice.tsx`, panel azul cielo con nivel, pasos y origen) y Android (diálogo «Rupi te explica · 3.° y 4.° grado»).

## Pruebas
- `QuizHintServiceTest` (9): niveles, refuerzo, explicación de respaldo, solo errores, IA validada y en caché, IA rechazada.
- **No verificado de punta a punta:** la base local no tiene cuestionarios con IA ni la IA configurada.
