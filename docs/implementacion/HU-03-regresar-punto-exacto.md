# HU-03 · Regresar al punto exacto del mapa

## Historia

> Como estudiante web, quiero regresar al punto exacto del mapa donde cerré sesión para no perder mi progreso ni repetir lecciones.

## Criterios de Aceptación

1. **Persistencia del último nodo:** Al cerrar sesión o recargar la página, se conserva el identificador `ultimo_nodo_visitado_id` registrado en `aprendizaje_inscripcion_ruta`.
2. **Desplazamiento y centrado suave:** Al cargar el mapa, la vista enfoca automáticamente el nodo activo/visitado (`.trail-stop.is-current-active`) con animación fluida `scrollIntoView({ behavior: 'smooth', block: 'center' })`.
3. **Botón directo "Tu siguiente paso":** El hero en `RouteOverview` prioriza el nodo `ultimo_nodo_visitado_id` si se encuentra en curso o disponible, permitiendo reanudar con un solo toque.
4. **Continuidad sin duplicados:** Reanudar una actividad en curso no altera el contador de intentos ni crea duplicados en las tablas de progreso.
5. **Servidor como única fuente de verdad:** La persistencia reside en MySQL; la interfaz web no depende de caches frágiles en el navegador.

## Implementación Técnica

- **Backend (Spring Boot):**
  - [`ActivityService.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/ActivityService.java): Actualiza `ultimo_nodo_visitado_id` atómicamente en cada interacción de inicio o avance.
  - [`LearningRouteRepository.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/LearningRouteRepository.java): Entrega `lastVisitedNodeId` en el detalle de la ruta (`RouteDetail.enrollment`).
- **Frontend (React):**
  - [`Roadmap.tsx`](../../frontend/web-react/src/features/roadmap/Roadmap.tsx):
    - `activeIndex` prioriza `route.enrollment.lastVisitedNodeId`.
    - `useEffect` desplaza suavemente el mapa a `.trail-stop.is-current-active`.
  - [`RouteOverview.tsx`](../../frontend/web-react/src/components/RouteOverview.tsx): Prioriza `lastVisitedNodeId` para el botón CTA "Tu siguiente paso".
