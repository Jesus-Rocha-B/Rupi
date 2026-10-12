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
| `learningroute/LearningRouteController.java` | `GET /api/v1/student/learning-routes`, `GET .../{versionRouteId}`, `GET .../search` y `GET .../suggestions`. |
| `learningroute/LearningRouteService.java` | Arma la ruta con progreso derivado, contexto cultural y búsqueda de roadmaps alternativos. |
| `learningroute/LearningRouteRepository.java` | SQL de rutas, nodos, progreso, contexto cultural y consultas FTS (`searchAlternativeRoutes`, `findSubjectSuggestions`). |
| `learningroute/RouteCurriculum.java` | HU-04: agrupa nodos por unidad, ordena por `numero_secuencia` y valida catálogo, grado y área. |
| `learningroute/LearningRouteDtos.java` | Registros de respuesta, incluidos `CulturalContext`, `RouteSearchResult` y `SearchAutocompleteResponse`. |
| `learningroute/ActivityController.java` | `POST .../{routeId}/nodes/{nodeId}/start`. |
| `learningroute/ActivityService.java` | Inicio transaccional e idempotente, y último nodo visitado. |

Configuración: `src/main/resources/application.yml`. Variables: `RUPI_DB_URL`, `RUPI_DB_USERNAME`, `RUPI_DB_PASSWORD` y `rupi.auth.demo-session-token`. Pruebas en `src/test/java/pe/rupi/api/` (`ActivityServiceTest`, `LearningRouteServiceTest`, `PasswordServiceTest`, `StudentAuthenticationFilterTest`).

## Web React · `frontend/web-react/src/`

| Archivo | Responsabilidad |
|---|---|
| `App.tsx` | Estado global y flujo: login → selección de grado y curso → búsqueda de materias → mapa → actividad. |
| `services/learningRoutesApi.ts` | Cliente tipado de la API (incluye búsqueda de roadmaps, sugerencias y contexto cultural). |
| `components/RoadmapSearchBar.tsx`, `RoadmapSearchBar.css` | HU-09: barra de búsqueda física con atajo (`/` o `Ctrl+K`), autocompletado y voz de Rupi. |
| `components/RoadmapFilterSidebar.tsx`, `RoadmapFilterSidebar.css` | HU-10: panel lateral de filtros avanzados por grado (1.°–6.°) y materia con sincronización URL y checkboxes táctiles. |
| `features/auth/LoginView.tsx` | Login, animación de Rupi cada 10 s y selector de materia. |
| `features/auth/GradeSelectView.tsx` | Selección de grado (1.º–6.º) y de curso. |
| `features/roadmap/Roadmap.tsx` | Mapa interactivo y enfoque en el nodo recordado. |
| `features/roadmap/ActivityBlocks.tsx` | Bloques de actividad (sin HTML interpretado). |
| `features/roadmap/unitGroups.ts`, `UnitBands.css` | HU-04: bandas de unidad sobre el mapa. Prueba en `tests/unitGroups.test.ts`. |
| `features/roadmap/nodeStates.ts` | Estados de nodo (bloqueado, disponible, en curso, completado). |
| `features/roadmap/routeSelection.ts` | Elige la ruta más reciente. Prueba en `tests/routeSelection.test.ts`. |
| `features/roadmap/RoadmapSkeleton.tsx` | Estado de carga. |
| `components/RupiCharacter.tsx` | Mascota con roles y estados de ánimo. |
| `components/RouteCompanion.tsx` | Tarjeta cultural (datos curiosos, audio y ánimo). |
| `components/RouteOverview.tsx` | Resumen de la ruta. |
| `utils/speech.ts` | `speakText` (síntesis de voz del navegador). |
| `App.css`, `features/auth/AuthViews.css` | Estilos (unas 2300 líneas cada uno). |

Recursos: `public/images/` (fondo de Ayacucho con su atribución). Pruebas en `tests/` (`unitGroups.test.ts`, `routeSelection.test.ts`, `searchApi.test.ts`).

## Base de datos · `database/mysql/`

- `schema.sql`: DDL de 77 tablas más los grados. `rupi.dbml`: diagrama. `modelo.md`: normalización e invariantes.
- `migrations/`: `V04` contexto cultural, `V05` última visita, `V06` reparar hash demo, `V07` unidades y competencias de prueba de HU-04, `V08` índices FTS en áreas/rutas y roadmaps alternativos de HU-09, `V09` catálogo integral de múltiples roadmaps escolares CNEB (COM 2.°, CYT 2.°, SOC 2.°, ART 2.°, MAT 3.°), `V10` catálogo completo de roadmaps escolares CNEB para todos los grados de primaria (1.° a 6.°), `V11` códigos alfanuméricos de acceso de clase docente (HU-11).
- `seed-hu01.sql` (ruta, 10 nodos, inscripción y progreso demo; **no repetir sobre datos reales**) y `seed-hu02.sql` (contenido de actividades).

## Django · `backend/admin-django/`

Solo `curriculo/models.py` y `curriculo/admin.py`, como esqueleto. Sin app funcional todavía.

## Scripts y entorno

- `iniciar-rupi.sh` / `detener-rupi.sh` (Linux) y `Iniciar-Rupi.cmd|ps1` / `Detener-Rupi.ps1` (Windows).
- `scripts/verify_learning_flow.py` (29 escenarios HTTP/MySQL, 6 de HU-04), `scripts/Verificar-HU01.ps1` y `scripts/Verificar-Auth.ps1`.
- HU-04: `scripts/verify_hu04_content.py`, `scripts/verify_hu04_pantalla.mjs` y `scripts/fixtures/hu04-orden-esperado.json`.
- HU-09: `scripts/Verificar-HU09.ps1` (6 escenarios de búsqueda y autocompletado) y `scripts/Verificar-Rendimiento-HU09.ps1` (benchmark FTS).
- HU-10: `scripts/Verificar-HU10.ps1` (8 escenarios de filtros combinados de grado y materia con precisión).
- HU-11: `scripts/Verificar-HU11.ps1` (10 escenarios de unirse a clase con código alfanumérico, expiración, revocación e idempotencia).
- `.local/` (ignorado): `env.sh`, `my.cnf`, `root.cnf`, `mysql-data`, logs y pids.

## Skills de diseño

`.agents/skills/human-crafted-frontend/` (anti-IA, tokens peruanos, componentes táctiles) y `.agents/skills/educational-game-ux/`.
