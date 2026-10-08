# HU-05 · Completar lección pedagógica, otorgar XP y desbloquear siguiente parada

## Historia

> Como estudiante web, quiero completar mi actividad pedagógica para ganar puntos de experiencia (XP), celebrar mi logro con Rupi y desbloquear la siguiente parada del camino.

## Criterios de Aceptación

1. **Finalización pedagógica:** El estudiante puede marcar la actividad como terminada desde el modal de la lección mediante un botón físico accesible de 48px (*«¡Terminé esta lección! (+50 XP)»*).
2. **Actualización del progreso a COMPLETADO:** La parada pasa a estado `COMPLETADO` en `aprendizaje_progreso_nodo` con fecha UTC `completado_en`.
3. **Desbloqueo automático del siguiente nivel:** El nodo inmediatamente posterior en secuencia pasa automáticamente de `BLOQUEADO` a `DISPONIBLE` con fecha `desbloqueado_en`.
4. **Asignación idempotente de XP (+50 XP):** Se insertan +50 puntos de experiencia en `gamificacion_movimiento_experiencia` con clave de idempotencia única `uq_xp_idempotencia (usuario_id, evento_idempotencia)`. Si se vuelve a enviar la solicitud, no se duplican puntos.
5. **Celebración accesible y afectuosa:** La interfaz muestra una tarjeta de celebración con:
   - Mascota Rupi en pose de festejo (`cheering`).
   - Píldora dorada con los XP ganados (+50 XP).
   - Tarjeta informativa de la siguiente parada desbloqueada.
   - Mensaje de aliento narrado por voz mediante síntesis de voz web (`speakText`).
6. **Actualización reactiva sin recargar:** El mapa, el contador de estrellas del Hero, el badge de XP acumulado y las bandas de unidad se actualizan de inmediato en la aplicación React.

## Contrato de API

### `POST /api/v1/student/learning-routes/{versionRouteId}/nodes/{nodeId}/complete`

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
  "rupiMessage": "¡Felicitaciones! Has completado esta parada, sumaste 50 XP y desbloqueaste la siguiente aventura con Rupi."
}
```

## Implementación Técnica

- **Backend (Spring Boot):**
  - [`ActivityController.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/ActivityController.java): Expone el endpoint `/complete`.
  - [`ActivityService.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/ActivityService.java): Transactor que actualiza `aprendizaje_progreso_nodo`, desbloquea el siguiente nodo, registra el movimiento en `gamificacion_movimiento_experiencia` y calcula el total de XP.
- **Frontend (React):**
  - [`learningRoutesApi.ts`](../../frontend/web-react/src/services/learningRoutesApi.ts): Función `completeActivity` y tipos `ActivityCompleteResult`.
  - [`ActivityBlocks.tsx`](../../frontend/web-react/src/features/roadmap/ActivityBlocks.tsx): Botón de finalización, vista de celebración, audio de voz e interacción.
  - [`App.tsx`](../../frontend/web-react/src/App.tsx): Función `handleActivityCompleted` para actualizar el estado global en tiempo real.
  - [`RouteOverview.tsx`](../../frontend/web-react/src/components/RouteOverview.tsx): Badge dinámico con estrellas y puntos XP acumulados.
