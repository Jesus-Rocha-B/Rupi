# Registro de cambios · RUPI

Más reciente primero. Formato: fecha · commit · qué cambió y por qué. Las entradas anteriores a 2026-10-07 se reconstruyeron desde `git log` y los documentos del repo.

## 2026-10-07 · HU-04: contenido de las paradas 6 a 10 alineado

- V07 y `seed-hu02.sql`: los textos de las paradas 6 a 10 no coincidían con su título (p. ej. «Formas en la piedra» hablaba de decenas). Se reescribieron sobre longitud, formas, días de la semana, medir con pasos y un reto de la cosecha, ligados a desempeños oficiales de 2.°. Demostración, sin validar con un docente.
- Unidades 2 y 3 renombradas: «patrones, medidas y formas» y «tiempo, pasos y retos»; desempeños vinculados ajustados (13 vínculos).
- `scripts/fixtures/hu04-orden-esperado.json` y `HU-04-ruta-por-unidades.md` actualizados. `verify_hu04_content.py`, `verify_hu04_pantalla.mjs` y `verify_learning_flow.py` pasan.

## 2026-10-07 · HU-04: ruta agrupada por unidades

Detalle, sustentación y evidencia en `docs/implementacion/HU-04-ruta-por-unidades.md`. Fuentes y límites en `HU-04-fuentes-curriculares.md`.

**Datos**
- `migrations/V07__unidades_curriculares_hu04.sql`: datos **oficiales** del MINEDU (Programa curricular de Educación Primaria, RM 281-2016 modificada por RM 159-2017) para Matemática de 2.°: 4 competencias con capacidades, 4 estándares del ciclo III y 23 desempeños. Más 3 unidades **de demostración** (`DEMO-MAT2-U1..U3`) con sus vínculos a nodos, actividades, estándares y desempeños. Idempotente; no toca progreso.
- Corrige la procedencia del catálogo: `CNEB-2024`, sin fuente, pasa a `CNEB-2017`, vigente desde 2017 (también en `seed-hu01.sql`).
- Aplicada a la base local con copia previa de las tablas de progreso; el progreso quedó igual.

**API**
- `GET .../learning-routes/{id}` añade `curriculum` (`status`, `message`, `units`). Es aditivo: `nodes`, `progress` y `enrollment` no cambian. Estados: `COMPLETA`, `PARCIAL`, `SIN_UNIDADES`, `NO_VIGENTE`, `INCOHERENTE`. Una unidad de otro catálogo, grado o área nunca se mezcla. Las paradas sin unidad van a un grupo final «Otras paradas».
- Código: `RouteCurriculum.java` (nuevo), `findCurriculum` en el repositorio, DTOs y servicio. Prueba `RouteCurriculumTest` (8).

**Web**
- Bandas de unidad en el mapa con título, competencias oficiales («Currículo MINEDU: …») y avance por unidad; aviso si el estado no es `COMPLETA`. Archivos: `unitGroups.ts`, `UnitBands.css`, `Roadmap.tsx`, `learningRoutesApi.ts`. Prueba `tests/unitGroups.test.ts` (3).
- Corregidos en móvil: bandas sobre las paradas y conector punteado sobre el título.

**Pruebas**
- `verify_learning_flow.py`: 6 escenarios nuevos (29 en total).
- Nuevos `scripts/verify_hu04_content.py` (11), `scripts/verify_hu04_pantalla.mjs` (10, Playwright instalado fuera del repo) y `scripts/fixtures/hu04-orden-esperado.json`.

**Documentación**
- Nuevos `HU-04-ruta-por-unidades.md` y `HU-04-fuentes-curriculares.md`; actualizados `README.md` de implementación, `entorno-linux.md`, `MAPA-DE-CODIGO.md`, `CONTEXTO.md` y `ESTADO.md`.

## 2026-10-07 · `f90307e`, `fad6e2c`

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
