# HU-07 · Insignias escolares ganadas y resumen de evaluación formativa

## Historia

> Como estudiante web de primaria, quiero ver un resumen claro de mi evaluación formativa y desbloquear mi primera insignia escolar al completar la parada, para celebrar mi esfuerzo y ver mis logros en mi panel de aprendizaje.

## Criterios de Aceptación

1. **Resumen formativo consolidado:** Al responder las preguntas de la lección, el estudiante puede visualizar un resumen claro con su total de preguntas respondidas, respuestas correctas, puntos acumulados y porcentaje de logro.
2. **Cálculo en tiempo de consulta:** El porcentaje de aciertos y estado formativo se calculan en tiempo de ejecución (`GET /api/v1/student/activities/{activityVersionId}/summary`) para preservar la invariante de no duplicar columnas calculadas en base de datos.
3. **Desbloqueo e idempotencia de insignia escolar:**
   - Al completar la actividad escolar mediante `POST /api/v1/student/learning-routes/{versionRouteId}/nodes/{nodeId}/complete`, el backend verifica el catálogo de insignias en `gamificacion_insignia` y siembra la primera insignia si no existe: `EXPLORADOR_AYACUCHO` (*«Explorador de Ayacucho»*, icono `⭐`, color `#f59e0b`).
   - Se otorga la insignia al estudiante en `gamificacion_insignia_usuario`. Si el estudiante ya la tenía, la operación es idempotente gracias a la restricción única `uq_insignia_usuario (usuario_id, insignia_id)`.
4. **Pantalla de celebración con medalla dorada:**
   - La pantalla de felicitaciones incluye la tarjeta de la insignia ganada (`.celebration-badge-card`) con marco dorado, destellos visuales y descripción del logro escolar.
   - La mascota Rupi festeja con animación de alegría y alienta al estudiante por voz.
5. **Bandeja de insignias en el Hero del Roadmap:**
   - El endpoint `GET /api/v1/student/badges` lista todas las insignias ganadas por el estudiante.
   - En la cabecera principal (`RouteOverview.tsx`), se muestra una bandeja accesible con las medallas e insignias obtenidas por el estudiante, mostrando su nombre e icono.

## Contratos de API

### 1. `GET /api/v1/student/activities/{activityVersionId}/summary`

Obtiene el resumen formativo de las respuestas del estudiante en la actividad.

#### Respuesta exitosa (`200 OK`)

```json
{
  "activityVersionId": "ac000000-0000-4000-8000-000000000001",
  "totalQuestions": 1,
  "correctAnswers": 1,
  "score": 10,
  "maxScore": 10,
  "percentage": 100.0,
  "feedbackMessage": "¡Excelente trabajo! Has demostrado gran comprensión del tema.",
  "performanceLevel": "EXCELENTE"
}
```

### 2. `POST /api/v1/student/learning-routes/{versionRouteId}/nodes/{nodeId}/complete`

Completa la parada, asigna +50 XP y desbloquea la insignia escolar si corresponde.

#### Respuesta exitosa (`200 OK`)

```json
{
  "nodeId": "ca000000-0000-4000-8000-000000000001",
  "state": "COMPLETADO",
  "experienceEarned": 50,
  "totalExperience": 150,
  "nextUnlockedNode": {
    "id": "ca000000-0000-4000-8000-000000000002",
    "sequence": 2,
    "title": "Sumas con retablos"
  },
  "badgeEarned": {
    "id": "badge-ayacucho-001",
    "code": "EXPLORADOR_AYACUCHO",
    "name": "Explorador de Ayacucho",
    "description": "Completaste tu primera lección en la ruta de Ayacucho.",
    "iconUrl": "⭐",
    "color": "#f59e0b"
  },
  "rupiMessage": "¡Felicitaciones! Has completado esta parada, sumaste 50 XP, desbloqueaste la insignia 'Explorador de Ayacucho' y avanzaste a la siguiente aventura con Rupi."
}
```

### 3. `GET /api/v1/student/badges`

Lista todas las insignias ganadas por el estudiante autenticado.

#### Respuesta exitosa (`200 OK`)

```json
[
  {
    "id": "badge-ayacucho-001",
    "code": "EXPLORADOR_AYACUCHO",
    "name": "Explorador de Ayacucho",
    "description": "Completaste tu primera lección en la ruta de Ayacucho.",
    "iconUrl": "⭐",
    "color": "#f59e0b"
  }
]
```

## Implementación Técnica

- **Backend (Spring Boot):**
  - [`LearningRouteDtos.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/LearningRouteDtos.java): Soporte para `BadgeDto` y campo opcional `badgeEarned` en `ActivityCompleteResponse`.
  - [`ActivityService.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/ActivityService.java): En el método transaccional `complete()`, verifica y otorga la insignia `EXPLORADOR_AYACUCHO` insertando en `gamificacion_insignia` y `gamificacion_insignia_usuario` de manera idempotente.
  - [`GamificationController.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/gamification/GamificationController.java): Nuevo endpoint `GET /api/v1/student/badges` para consultar la vitrina de medallas del alumno.
  - [`EvaluationService.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/evaluation/EvaluationService.java): Método `getEvaluationSummary()` para compilar estadísticas formativas.
- **Frontend (React):**
  - [`learningRoutesApi.ts`](../../frontend/web-react/src/services/learningRoutesApi.ts): Tipos `BadgeDto`, `EvaluationSummaryDto` y métodos `fetchEvaluationSummary` y `fetchStudentBadges`.
  - [`ActivityBlocks.tsx`](../../frontend/web-react/src/features/roadmap/ActivityBlocks.tsx): Resumen formativo antes de finalizar la lección y tarjeta de insignia en el modal de celebración con Rupi.
  - [`App.tsx`](../../frontend/web-react/src/App.tsx): Carga de insignias con `fetchStudentBadges`, actualización reactiva al recibir una nueva insignia y pase de estado hacia el Hero.
  - [`RouteOverview.tsx`](../../frontend/web-react/src/components/RouteOverview.tsx): Renderizado de la bandeja de insignias ganadas (`.earned-badges-tray`) con íconos, títulos y tooltip de descripción.
  - [`App.css`](../../frontend/web-react/src/App.css): Estilos para `.celebration-badge-card`, `.badge-icon-badge`, `.badge-gold-ring` y `.earned-badges-tray`.

## Respeto a las Reglas de Arquitectura

- **Single Writer:** Spring Boot es el único escritor de `gamificacion_insignia` y `gamificacion_insignia_usuario`.
- **Invariantes:** Los porcentajes y cantidades de insignias no se duplican en columnas desnormalizadas.
- **Identidad Artesanal:** Se emplean colores cálidos de la paleta peruana (`#f59e0b`, `#173e30`, `#ffd26a`) evitando gradientes morados/azules genéricos de IA slop.
