# Mapa de código · RUPI

Para ubicar archivos sin explorar. Rutas relativas a la raíz del repo.

## API Spring Boot · `backend/api-spring-boot/src/main/java/pe/rupi/api/`

| Archivo | Responsabilidad |
|---|---|
| `RupiApiApplication.java` | Arranque. |
| `shared/SessionController.java` | `GET /api/v1/auth/options`, `POST /auth/login`, `/auth/demo`, `/auth/logout`, `GET /student/session`. |
| `shared/StudentSessionService.java` | Sesiones por cookie, validación del estudiante. |
| `shared/StudentAuthenticationFilter.java` | Autentica cada petición `/api/v1/student/**`. |
| `shared/PasswordService.java` | Hash y verificación de contraseñas. |
| `shared/ApiExceptionHandler.java`, `ApiResponseHeadersFilter.java`, `WebCorsConfig.java` | Errores, cabeceras de seguridad y CORS. |
| `learningroute/LearningRouteController.java` | `GET /api/v1/student/learning-routes` y `GET .../{versionRouteId}`. |
| `learningroute/LearningRouteService.java` | Arma la ruta con progreso derivado y contexto cultural. |
| `learningroute/LearningRouteRepository.java` | SQL de rutas, nodos, progreso y contexto cultural (`curriculo_contexto_cultural*`). |
| `learningroute/LearningRouteDtos.java` | Registros de respuesta, incluido `CulturalContext`. |
| `learningroute/ActivityController.java` | `POST .../{routeId}/nodes/{nodeId}/start`. |
| `learningroute/ActivityService.java` | Inicio transaccional e idempotente, y último nodo visitado. |

Configuración: `src/main/resources/application.yml`. Variables: `RUPI_DB_URL`, `RUPI_DB_USERNAME`, `RUPI_DB_PASSWORD` y `rupi.auth.demo-session-token`. Pruebas en `src/test/java/pe/rupi/api/` (`ActivityServiceTest`, `LearningRouteServiceTest`, `PasswordServiceTest`, `StudentAuthenticationFilterTest`).

## Web React · `frontend/web-react/src/`

| Archivo | Responsabilidad |
|---|---|
| `App.tsx` | Estado global y flujo: login → selección de grado y curso → mapa → actividad. |
| `services/learningRoutesApi.ts` | Cliente tipado de la API (incluye el tipo del contexto cultural). |
| `features/auth/LoginView.tsx` | Login, animación de Rupi cada 10 s y selector de materia. |
| `features/auth/GradeSelectView.tsx` | Selección de grado (1.º–6.º) y de curso. |
| `features/roadmap/Roadmap.tsx` | Mapa interactivo y enfoque en el nodo recordado. |
| `features/roadmap/ActivityBlocks.tsx` | Bloques de actividad (sin HTML interpretado). |
| `features/roadmap/nodeStates.ts` | Estados de nodo (bloqueado, disponible, en curso, completado). |
| `features/roadmap/routeSelection.ts` | Elige la ruta más reciente. Prueba en `tests/routeSelection.test.ts`. |
| `features/roadmap/RoadmapSkeleton.tsx` | Estado de carga. |
| `features/roadmap/lessons.ts` | Origen estático antiguo. Verificar si sigue en uso. |
| `components/RupiCharacter.tsx` | Mascota con roles y estados de ánimo. |
| `components/RouteCompanion.tsx` | Tarjeta cultural (datos curiosos, audio y ánimo). |
| `components/RouteOverview.tsx` | Resumen de la ruta. |
| `utils/speech.ts` | `speakText` (síntesis de voz del navegador). |
| `App.css`, `features/auth/AuthViews.css` | Estilos (unas 2300 líneas cada uno). |

Recursos: `public/images/` (fondo de Ayacucho con su atribución).

## Base de datos · `database/mysql/`

- `schema.sql`: DDL de 77 tablas más los grados. `rupi.dbml`: diagrama. `modelo.md`: normalización e invariantes.
- `migrations/`: `V04` contexto cultural, `V05` última visita, `V06` reparar hash demo.
- `seed-hu01.sql` (ruta, 10 nodos, inscripción y progreso demo; **no repetir sobre datos reales**) y `seed-hu02.sql` (contenido de actividades).

## Django · `backend/admin-django/`

Solo `curriculo/models.py` y `curriculo/admin.py`, como esqueleto. Sin app funcional todavía.

## Scripts y entorno

- `iniciar-rupi.sh` / `detener-rupi.sh` (Linux) y `Iniciar-Rupi.cmd|ps1` / `Detener-Rupi.ps1` (Windows).
- `scripts/verify_learning_flow.py` (23 escenarios HTTP), `scripts/Verificar-HU01.ps1` y `scripts/Verificar-Auth.ps1`.
- `.local/` (ignorado): `env.sh`, `my.cnf`, `root.cnf`, `mysql-data`, logs y pids.

## Skills de diseño

`.agents/skills/human-crafted-frontend/` (anti-IA, tokens peruanos, componentes táctiles) y `.agents/skills/educational-game-ux/`.
