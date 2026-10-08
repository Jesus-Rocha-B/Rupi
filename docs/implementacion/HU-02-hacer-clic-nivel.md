# HU-02 · Hacer clic en un nivel

## Historia

> Como estudiante web, quiero hacer clic en un nivel del roadmap para iniciar la lección de ese día.

## Criterios de Aceptación

1. **Apertura autorizada:** Al hacer clic en un nodo con estado `DISPONIBLE` o `EN_CURSO`, se abre el diálogo interactivo con el contenido didáctico de la actividad.
2. **Restricción de nodos bloqueados:** Un nodo `BLOQUEADO` no entrega contenido ni modifica el progreso (retorna HTTP 403 Forbidden).
3. **Transición a EN_CURSO:** El primer inicio de un nodo disponible persiste el estado `EN_CURSO`, `primer_acceso_en` y actualiza `ultimo_nodo_visitado_id` en `aprendizaje_inscripcion_ruta` dentro de una transacción en MySQL.
4. **Reapertura idempotente:** Si el estudiante reabre una actividad ya en curso, conserva su estado sin duplicar registros.
5. **Aislamiento por estudiante:** Solo el estudiante autenticado mediante sesión activa puede acceder a sus actividades e inscripciones.

## Contrato de API

### `POST /api/v1/student/learning-routes/{versionRouteId}/nodes/{nodeId}/start`

Inicia la actividad asociada a la parada del roadmap o recupera su contenido estructurado.

#### Respuesta exitosa (`200 OK`)

```json
{
  "nodeId": "ca000000-0000-4000-8000-000000000001",
  "title": "Números en la plaza",
  "instructions": "Observa la plaza de Ayacucho y cuenta los elementos que la rodean.",
  "state": "EN_CURSO",
  "blocks": [
    {
      "id": "bc000000-0000-4000-8000-000000000001",
      "type": "DESTACADO",
      "text": "¡Bienvenido a la Plaza Mayor de Ayacucho!...",
      "url": null,
      "accessibleText": null
    }
  ]
}
```

#### Respuestas de error

- `401 Unauthorized`: Sin sesión activa.
- `403 Forbidden`: El nodo está `BLOQUEADO`.
- `404 Not Found`: Ruta o nodo inexistente o no publicado.

## Implementación Técnica

- **Backend (Spring Boot):**
  - [`ActivityController.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/ActivityController.java): Expone el endpoint `/start`.
  - [`ActivityService.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/ActivityService.java): Método `start` transaccional con bloqueo `FOR UPDATE` para garantizar consistencia.
- **Frontend (React):**
  - [`learningRoutesApi.ts`](../../frontend/web-react/src/services/learningRoutesApi.ts): Función `startActivity`.
  - [`Roadmap.tsx`](../../frontend/web-react/src/features/roadmap/Roadmap.tsx): Manejo táctil del clic en nodos interactivos.
  - [`ActivityBlocks.tsx`](../../frontend/web-react/src/features/roadmap/ActivityBlocks.tsx): Renderizado accesible de bloques didácticos.
