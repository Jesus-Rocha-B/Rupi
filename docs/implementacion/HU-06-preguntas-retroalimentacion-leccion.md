# HU-06 · Preguntas interactivas y retroalimentación formativa de la lección

## Historia

> Como estudiante web de primaria, quiero responder preguntas didácticas interactivas al final o durante mi actividad pedagógica y recibir retroalimentación formativa inmediata y empática de Rupi, para verificar lo aprendido sin miedo al error.

## Criterios de Aceptación

1. **Preguntas pedagógicas estructuradas:** Cada versión de actividad puede presentar preguntas didácticas vinculadas (`evaluacion_pregunta`) con enunciado claro, puntuación ponderada y opciones múltiples estructuradas (`evaluacion_opcion`).
2. **Siembra pedagógica automática contextualizada:** Si una lección no cuenta con preguntas cargadas en la base de datos, el backend genera y siembra automáticamente preguntas contextualizadas en la cultura y geografía de Ayacucho (ej. conteo con retablos ayacuchanos, geometría de iglesias coloniales, flora y fauna andina).
3. **Opciones táctiles accesibles (≥48px):** Cada opción es un botón con una altura mínima de 48px, borde redondeado artesanal y relieve 3D físico (`box-shadow: 0 4px 0 ...`, `:active` translateY(2px)), cumpliendo con WCAG AAA y la guía `human-crafted-frontend`.
4. **Evaluación formativa no punitiva:**
   - Al responder mediante `POST /api/v1/student/activities/{activityVersionId}/questions/answer`, se registra el intento y la respuesta en `evaluacion_intento_evaluacion` y `evaluacion_respuesta_estudiante`.
   - Se devuelve de inmediato si la respuesta es correcta, los puntos obtenidos y una explicación pedagógica clara.
   - Si el estudiante se equivoca, la mascota Rupi brinda retroalimentación empática explicando el razonamiento correcto sin castigos ni deducciones de puntos, siguiendo la guía `educational-game-ux`.
5. **Voz accesible integrada:** El mensaje pedagógico de Rupi se acompaña de síntesis de voz en español (`speakText`) para apoyar a estudiantes de 1.° y 2.° de primaria en proceso de consolidación de la lectura.

## Contratos de API

### 1. `GET /api/v1/student/activities/{activityVersionId}/questions`

Obtiene las preguntas disponibles para la actividad actual.

#### Respuesta exitosa (`200 OK`)

```json
[
  {
    "id": "q1-1111-2222-3333-444444444444",
    "prompt": "¿Cuántas flores decoran el retablo si contamos 4 claveles y 3 cantutas?",
    "points": 10,
    "options": [
      {
        "id": "opt-1",
        "text": "7 flores en total",
        "order": 1
      },
      {
        "id": "opt-2",
        "text": "6 flores",
        "order": 2
      },
      {
        "id": "opt-3",
        "text": "8 flores",
        "order": 3
      }
    ]
  }
]
```

### 2. `POST /api/v1/student/activities/{activityVersionId}/questions/answer`

Envía la respuesta seleccionada por el estudiante y recibe la retroalimentación pedagógica.

#### Solicitud (`POST`)

```json
{
  "questionId": "q1-1111-2222-3333-444444444444",
  "selectedOptionId": "opt-1"
}
```

#### Respuesta exitosa (`200 OK`)

```json
{
  "questionId": "q1-1111-2222-3333-444444444444",
  "isCorrect": true,
  "pointsEarned": 10,
  "explanation": "¡Exacto! 4 flores más 3 flores son 7 flores andinas en el retablo.",
  "rupiFeedback": "¡Excelente razonamiento! Resolviste el desafío matemático con Rupi."
}
```

## Implementación Técnica

- **Backend (Spring Boot):**
  - [`EvaluationDtos.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/evaluation/EvaluationDtos.java): DTOs de preguntas, opciones, solicitudes de respuesta y respuestas de feedback.
  - [`EvaluationService.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/evaluation/EvaluationService.java):
    - `findQuestionsForActivity`: Búsqueda y siembra de preguntas.
    - `seedDefaultQuestions`: Creación transaccional e idempotente de preguntas contextualizadas si la actividad carece de ellas.
    - `checkAndRecordAnswer`: Validación contra `evaluacion_opcion.es_correcta`, registro de intento en `evaluacion_intento_evaluacion` y registro en `evaluacion_respuesta_estudiante`.
  - [`EvaluationController.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/evaluation/EvaluationController.java): Endpoints REST bajo `/api/v1/student/activities`.
  - [`ActivityService.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/ActivityService.java): Entrega las preguntas de la versión de actividad en el inicio de la lección (`start`).
- **Frontend (React):**
  - [`learningRoutesApi.ts`](../../frontend/web-react/src/services/learningRoutesApi.ts): Tipos `QuestionDto`, `OptionDto`, `AnswerFeedbackResponse` y función `submitQuestionAnswer`.
  - [`ActivityBlocks.tsx`](../../frontend/web-react/src/features/roadmap/ActivityBlocks.tsx): Renderizado interactivo de preguntas didácticas, selección con botones táctiles de 48px, envío asíncrono, feedback en tiempo real con tarjeta de Rupi y síntesis de voz accesible.
  - [`App.css`](../../frontend/web-react/src/App.css): Clases `.question-option-btn`, `.question-feedback-box` y efectos táctiles 3D según los estándares de diseño artesanal.

## Principios Pedagógicos y Accesibilidad

1. **El error como oportunidad:** Cuando un estudiante responde incorrectamente, el sistema no muestra mensajes rojos de fracaso ("Incorrecto", "Perdiste"). En su lugar, Rupi ofrece una explicación amable y orientadora para ayudarle a reflexionar.
2. **Claridad tipográfica:** Fuentes legibles y espaciado generoso para evitar la fatiga visual en pantallas de tabletas escolares.
3. **Multimodalidad:** Se combinan estímulos visuales (íconos, colores de alto contraste) y auditivos (voz de Rupi) para apoyar la diversidad de ritmos de aprendizaje.
