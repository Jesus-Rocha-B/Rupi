# Registro de cambios · RUPI

Más reciente primero. Formato: fecha · commit · qué cambió y por qué. Las entradas anteriores a 2026-10-07 se reconstruyeron desde `git log` y los documentos del repo.

## 2026-10-07 · sin commit

- Soporte de entorno **Linux (probado en Fedora)**: `iniciar-rupi.sh` (MySQL 8.4 portable en 3307, API en 8081, Vite en 5173) y `detener-rupi.sh`. Equivalen a los `.ps1` de Windows.
- Documentación del entorno Linux: `LEEME-LOCAL-LINUX.md` y `docs/implementacion/entorno-linux.md`. Usa Temurin 21 en `~/jdks/`, sin `sudo`, y convive con el MariaDB del sistema.
- Nueva carpeta `docs/registro/` para retomar contexto tras `/clear` o `/compact`.

## 2026-10-06

### `6b0049b` · test(HU-02, HU-03): verificar flujo real y documentar entorno local
- `scripts/verify_learning_flow.py`: 23 escenarios HTTP contra MySQL real. Limpia solo los registros ficticios que crea.
- `LEEME-LOCAL.md` (Windows) y `docs/implementacion/HU-02-HU-03.md`.

### `e599886` · feat(HU-02, HU-03): iniciar actividades y reanudar rutas
- `POST /api/v1/student/learning-routes/{routeId}/nodes/{nodeId}/start`: transaccional e idempotente, con bloqueo de filas. Valida sesión, origen, matrícula, publicación y estado.
- Migraciones `V05__ultima_visita.sql` (`ultima_visita_en` en la inscripción) y `V06__reparar_hash_demo.sql`. Semilla `seed-hu02.sql`.
- Backend: `ActivityController` y `ActivityService`; pruebas `ActivityServiceTest`.
- Frontend: `ActivityBlocks.tsx`, `routeSelection.ts` (ruta más reciente por última visita) y su prueba. El mapa enfoca el último nodo válido.

### `d6826a3` · fix(auth): corregir login y validar credenciales reales
- `PasswordService`, `StudentSessionService` y `StudentAuthenticationFilter`, con sus pruebas. Login con límite de intentos, logout y sesión por cookie.
- `/api/v1/auth/demo` solo existe si hay token demo configurado y la petición es local.

### `73ed96f` · Rupi temático y selección escolar
- `RupiCharacter.tsx` con roles `default`, `scientist`, `mathematician` y `reader`.
- El login alterna cada 10 s la pose de Rupi y su frase pedagógica, con un selector interactivo.
- `GradeSelectView.tsx` (1.º a 6.º) y vista de cursos por grado con las tres materias base.

### `1392935` · Mapa y contexto cultural
- Etiquetas y Rupi reubicados para despejar el camino. Resumen compactado para 1366×768.
- `V04__curriculum_cultural_context.sql`: `curriculo_contexto_cultural` y `curriculo_contexto_cultural_dato`, con crédito de fuente y licencia.
- `RouteCompanion.tsx`: una sola tarjeta con datos curiosos de Ayacucho, audio (`utils/speech.ts`, síntesis de voz) y mensaje de ánimo.
- Foto de fondo `ayacucho-plaza.jpg` (CC BY-SA 4.0, atribución en `public/images/ATTRIBUTION.md`).

## 2026-10-05 · `d40d170`
- Corrección de errores de lógica de la web y cierre de HU-01.

## 2026-10-02 · `f3f4079`, `86c59f8`, `e3cc6a6`
- Mapa de HU-01 integrado con la API Spring Boot: estados de carga, error/reintento, sin sesión y vacío.
- Arquitectura declarada y esquema MySQL creado (`schema.sql`, `rupi.dbml`, `modelo.md`, `seed-hu01.sql`).
- Endpoints `GET /api/v1/student/learning-routes` y `GET .../{versionRouteId}`.

## 2026-09-29 · `a1dd4d9`, `6327196`
- Prototipo inicial de la página de Rupi y estructura del monorepo.
