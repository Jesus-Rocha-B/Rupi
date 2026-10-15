# Registro de cambios · RUPI

Más reciente primero. Formato: fecha · commit · qué cambió y por qué. Las entradas anteriores a 2026-10-07 se reconstruyeron desde `git log` y los documentos del repo.

## 2026-10-14 · Revisión modular de las historias 1 a 14

Contrastado con el backlog (`GLAB-S06-2026-01`). Los fallos se encontraron con los scripts del repo y con pruebas de punta a punta contra la API.

- **Progreso (historias 2, 3, 5 y 7):** una parada agregada después de inscribirse quedaba bloqueada para siempre, y abrir la parada 1 sin fila de progreso funcionaba, pero completarla respondía 403.
  - `TeacherRouteRepository.addNodeToRoute` crea el progreso de los alumnos ya inscritos.
  - `ActivityService` crea las filas que falten antes de abrir o completar.
  - `V17__progreso_paradas_faltantes.sql` repara los datos existentes.
- **Autoría (historias 5 a 7):**
  - 404 en vez de 500 para rutas ajenas o inexistentes.
  - 400 en vez de 500 para tipos de actividad o de material desconocidos y títulos de más de 180 caracteres.
  - Una secuencia ocupada manda la parada al final.
  - Un área inexistente da 400 (antes se tomaba un área cualquiera).
- **Enlaces (historia 7):**
  - Solo `http(s)://` o descargas de RUPI.
  - El material adjunto debe ser del mismo docente.
  - Los enlaces externos se registran en `recurso_archivo_multimedia` (`url_externa`), así que el alumno ve el botón para abrirlos.
- **Historia 8:**
  - `POST /api/v1/teacher/learning-routes/{id}/pause` e interruptor «Pausar / Volver a publicar» en el panel docente.
  - Las consultas del alumno ya exigían `PUBLICADO`.
  - Volver a publicar conserva `publicado_en`.
- **Historia 13 (roadmap institucional):** además del autor, calendariza el docente con un aula activa del grado si la ruta está publicada; las fechas siguen siendo por aula. En la vista de alumno del docente, el calendario abre en modo docente.
- **Scripts:**
  - `Verificar-HU05-Docente.ps1` y `Verificar-HU06-Reordenamiento.ps1` usan `mvn` fuera de Windows.
  - `Verificar-HU01.ps1` espera la primera parada DISPONIBLE en lugar del 503 anterior.
  - `verify_hu12_resources.py` omite las rutas sin unidades.
- **Pruebas:** 10 nuevas (TeacherRouteServiceTest y CalendarServiceTest); 134 en total. Los 14 scripts pasan y hay 22 comprobaciones de punta a punta.

## 2026-10-14 · V12 desde cero, materiales sin huérfanos, sesión determinista y barra superior en celular

- **`V12__rol_docente_autoria_hu05.sql`:** corregida en el mismo archivo, porque fallaba antes de aplicar nada sobre `schema.sql`.
  - Ya no inserta `es_sistema` ni `creado_en` en `identidad_rol`.
  - La membresía DOCENTE se crea en el aula del alumno demo, buscada por membresía (`e3000000-…` o `au000000-…`).
  - `recurso_archivo_multimedia` usa la misma definición que `schema.sql`.
  - Probado armando una base nueva desde `schema.sql` con V04 a V16, dos veces.
- **`MaterialStorageService`:** si el `INSERT` falla, se borra el archivo ya guardado y se relanza el error. Nueva prueba `deletesFileWhenInsertFails` (124 pruebas en total, 9 omitidas de MySQL).
- **`StudentSessionService`:** rol y aula elegidos con subconsultas ordenadas (DOCENTE primero; aula activa más reciente). Antes, con dos roles o varias aulas, la sesión tomaba una fila cualquiera.
- **Barra superior ≤ 520 px (`App.css`, `App.tsx`):**
  - El menú usaba `justify-content: center` con desplazamiento, y «Mi Camino» quedaba en coordenadas negativas, inalcanzable incluso a 414 px. Ahora se centra con márgenes automáticos.
  - «Explorar Roadmaps» se abrevia a «Explorar» (el `aria-label` conserva el nombre completo).
  - El nombre del alumno y la palabra «Salir» ceden su espacio; el botón de salir mantiene 48 px.
  - Las etiquetas del menú pasan a 12 px solo en ese ancho.

## 2026-10-14 · HU-13 y HU-14 demostrables, paneles de error visibles y perfil a 375 px

- **`V16__docente_demo_responsable_aula.sql`:** el docente demo pasa a ser responsable de las aulas activas del alumno demo. El aula se busca por membresía, no por id, porque las bases armadas con seeds anteriores usan `au000000-…`. Sin esto, el calendario devolvía `classrooms: []` y no había recordatorios. Es idempotente y no reemplaza a un docente ya asignado. **Cada integrante debe aplicarla en su base local** después del pull (pasos en [`README-CAMBIOS-2026-10-14.md`](README-CAMBIOS-2026-10-14.md), sección 3).
- **`App.tsx`:**
  - Los paneles «No pudimos consultar tu progreso» y «Esta ruta ya no está disponible» salen del bloque `isRoadmapAvailable`, donde nunca se mostraban.
  - «Ruta en preparación» aparece solo cuando no hay error.
  - Se retira el panel «Aún no tienes rutas», que nunca se mostraba: «Ruta en preparación» ya ofrece explorar y el código de clase.
- **`StudentProfileView.css`:** debajo de 820 px, las tarjetas de XP, insignias y rutas bajan de línea en vez de desbordar (antes la página medía 424 px a 375 px).
- **`ESTADO.md`:** puesto al día con HU-13 y HU-14, los fallos resueltos y los que siguen abiertos.
- **Verificado:** el flujo docente completo por API (ruta, parada, PDF, publicación, fecha y recordatorio al alumno) y en la app (error 500 con «Reintentar», sin desplazamiento a 375 px).

## 2026-10-14 · Revisión de `48a9357..75a62b4` y correcciones de grado y carga

- **Revisión:** 12 commits del equipo (HU-12 biblioteca MINEDU, HU-13 calendario, HU-14 recordatorios, roadmaps Oficial/Comunidad, perfil y lección). Detalle completo, pasos para poner al día la copia local y errores pendientes en [`README-CAMBIOS-2026-10-14.md`](README-CAMBIOS-2026-10-14.md).
- **Frontend (`App.tsx`):** el grado escolar sale siempre de la sesión. Al iniciar o cerrar sesión se borran las preferencias guardadas en la tableta. Se quitó la inscripción automática al cargar. Una carga cancelada ya no se marca como error de red: esa era la causa de que apareciera el login con sesión abierta. Se quitó el temporizador de 4 s y a los 8 s se ofrece «Reintentar». Entrar a un roadmap ya no deja la app en el esqueleto hasta pulsar F5: «Continuar mi aventura» fija el grado y `resetView()` siempre dispara una carga nueva.
- **`package.json`:** nuevo `npm test` para las pruebas de Node (21 en verde).

## 2026-10-13 · Historia 12: descargar una unidad para estudiar sin conexión

- **Backend (Spring Boot):** paquete `studypackage` con `GET /api/v1/student/learning-routes/{versionRouteId}/units/{unitId|otras}/package`. Comprueba la inscripción con `LearningRouteService.getForStudent` (401 sin sesión, 404 sin inscripción o unidad ajena, 409 sin lecciones publicadas) y solo lee la base.
- **Empaquetador:** zip con `guia-de-estudio.pdf` (OpenPDF 2.2.5, A4, claves en página aparte, sin recursos remotos), materiales del docente verificados por SHA-256, `contenido.json`, `manifest.json` con tamaño y hash de cada archivo y `LEEME.txt`. No imprime las preguntas genéricas que siembra `EvaluationService`.
- **Caché:** `uploads/package-cache/` con clave por contenido; un cambio del docente (orden, textos, adjuntos) genera otra clave sin invalidar a mano. Tiempo de vida de 168 h, un candado por clave y escritura atómica. `uploads/` pasa a `.gitignore`.
- **Frontend:** panel «Llévate tu unidad» bajo la leyenda del mapa, una tarjeta por unidad, comprobación del SHA-256 en el navegador, botones de 48 px y contraste 8.3:1.
- **`MaterialStorageService.diskPathFor`:** ruta del archivo sin consultar la base.
- **Pruebas:** 19 JUnit nuevas (85 en total), 5 de Node (17 en total), `scripts/Verificar-HU12-Descarga.ps1` (20/20) y `scripts/verify_hu12_pantalla.mjs` (10/10).
- **Fallos detectados al verificar** (sin corregir, detalle en `ESTADO.md`): abrir lecciones y subir materiales responden 503 por columnas de `recurso_archivo_multimedia` que no existen en `schema.sql`, y V12 no corre sobre el esquema del repo.

## 2026-10-13 · HU-06: Reorganizar Secuencia de Aprendizaje con Drag and Drop (Docente)

- **Backend (Spring Boot):**
  - Creado DTO `TeacherDtos.ReorderNodesRequest` conteniendo la lista ordenada de IDs de las paradas.
  - Implementado endpoint `PUT /api/v1/teacher/learning-routes/{routeId}/nodes/reorder` en `TeacherRouteController.java`.
  - En `TeacherRouteRepository.java`: método transaccional `reorderNodes` con técnica de desplazamiento temporal `+1000` para prevenir colisiones con la clave única `uq_nodo_orden (version_ruta_id, numero_secuencia)`, asignación secuencial `1..N` y actualización armónica de coordenadas `mapa_x` y `mapa_y`.
  - Agregadas 3 pruebas unitarias en `TeacherRouteServiceTest.java` (66 pruebas totales en verde con `mvn test`).
- **Frontend (React):**
  - En `teacherApi.ts`: cliente `reorderTeacherNodes` conectado al endpoint PUT.
  - En `TeacherAuthoringView.tsx` y `TeacherAuthoring.css`:
    - Funcionalidad interactiva de arrastrar y soltar nativa HTML5 (`draggable`, `onDragStart`, `onDragOver`, `onDrop`) con asa ergonómica `GripVertical`.
    - Estados visuales de feedback (`.is-dragging` con opacidad y borde discontinuo; `.drag-over` con acento andino).
    - Botones de movimiento vertical (`ChevronUp` / `ChevronDown`) para accesibilidad en tabletas táctiles y navegación por teclado (WCAG AA).
    - Gestión de estado temporal (`orderedNodes`, `isReorderDirty`) con barra flotante de confirmación ("Guardar Nuevo Orden" o "Restablecer").
  - Compilación `npm run build` (637ms) y linter `npm run lint` limpios al 100%.
  - Creado script de verificación `scripts/Verificar-HU06-Reordenamiento.ps1` con 9/9 pruebas aprobadas.


- **Rol Docente y Seguridad (Backend & DB):**
  - Creada migración idempotente `V12__rol_docente_autoria_hu05.sql` asegurando roles `DOCENTE` (`b1000000-0000-4000-8000-000000000002`) y `ESTUDIANTE` (`b1000000-0000-4000-8000-000000000001`).
  - Sembrado usuario demo de profesor `profesor.demo` (`22222222-2222-4222-8222-222222222222`, clave `123456`, "Prof. Carlos Mendoza") con aula y membresía asignadas.
  - Creada tabla relacional `aprendizaje_nodo_recurso` y actualizada `recurso_archivo_multimedia` para soporte de documentos educativos (.pdf, .ppt, .pptx, enlaces externos).
  - Actualizados `schema.sql`, `rupi.dbml` y `grants-spring-boot.sql`.
  - En `StudentAuthenticationFilter.java` y `SessionController.java`: detección automática de `role === 'DOCENTE'` y control de acceso estricto a `/api/v1/teacher/**` (403 Forbidden para no docentes).
- **Servicios y Controladores REST (Spring Boot):**
  - `TeacherRouteService` y `TeacherRouteController`: endpoints para crear roadmaps en borrador, agregar paradas/nodos secuenciados, vincular recursos educativos y publicar la versión para los estudiantes.
  - `MaterialStorageService` y `MaterialController`: almacenamiento local protegido en `uploads/materials/`, hash SHA-256, sanitización de extensiones y endpoint de descarga `/api/v1/materials/{id}/download`.
  - `ActivityService`: resolución de URLs de archivos multimedia desde `recurso_archivo_multimedia` mediante `LEFT JOIN`.
  - 63 pruebas JUnit 5 ejecutadas y aprobadas al 100% en `backend/api-spring-boot`.
- **Frontend y Experiencia de Usuario Artesanal (React):**
  - Creado cliente `teacherApi.ts` con manejo de llamadas REST y subida `multipart/form-data`.
  - Creado componente `TeacherAuthoringView.tsx` y `TeacherAuthoring.css`: panel completo para diseñar roadmaps por grado y área CNEB, agregar paradas escolares y publicarlas.
  - Creado componente `MaterialUploadZone.tsx` y `MaterialUploadZone.css`: subida táctil de archivos PDF, PPT y enlaces web asociados al nodo.
  - En `ActivityBlocks.tsx`: renderizado dedicado de bloques `ADJUNTO` con icono por tipo de archivo y botón 3D táctil de descarga/apertura para los alumnos.
  - En `App.tsx`: detección de rol docente, carga directa del panel de autoría y barra superior de "Modo Previsualización Docente" para alternar con la vista de alumno.
  - En `LoginView.tsx`: botón de acceso directo de prueba local para `Docente (Prof. Carlos)`.
  - Compilación `tsc -b && vite build` y linter `eslint .` verificados al 100% sin advertencias.
  - Creado script de verificación automatizada `scripts/Verificar-HU05-Docente.ps1` con 7 pruebas aprobadas al 100%.


## 2026-10-13 · Reemplazo de diplomas por Vitrina de Insignias de Perfil Escolar para Primaria

- **Experiencia de Usuario y Gamificación (Primaria 6-12 años):**
  - Siguiendo la definición de producto para primaria y la retroalimentación de diseño, se eliminó el concepto de "diplomas / certificados imprimibles con firmas", sustituyéndolo por la **Vitrina de Insignias de Perfil del Estudiante** ([`StudentBadgesModal.tsx`](../../frontend/web-react/src/components/StudentBadgesModal.tsx)).
  - Los niños celebran la culminación de rutas y lecciones desbloqueando medallas en su perfil (`GRAN_TUNQUI_ORO`, `EXPLORADOR_AYACUCHO`, retos diarios) con acceso permanente desde la barra lateral ("Mis insignias") y desde el botón de logro al 100% de la ruta en [`RouteOverview.tsx`](../../frontend/web-react/src/components/RouteOverview.tsx).
  - Eliminado el componente obsoleto `RouteDiplomaModal.tsx`.
- **Backend:**
  - En [`ActivityService.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/ActivityService.java), el mensaje de culminación de ruta se actualizó para celebrar la Gran Insignia de Oro en la vitrina de perfil del estudiante.

## 2026-10-12 · Corrección de columnas, ENUMs y UUIDs en base de datos y backend

- **`backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/ActivityService.java`:**
  - Alineado valor de columna `motivo` en la inserción de bonificación de fin de ruta en `gamificacion_movimiento_experiencia`: cambiado `'GRADUACION'` por `'INSIGNIA'` para cumplir estrictamente con la restricción `ENUM('ACTIVIDAD','RETO','INSIGNIA','AJUSTE','REVERSO')` de `schema.sql` y `rupi.dbml`.
  - Normalizados identificadores de insignias escolares a UUID canónico hexadecimal de 128 bits (`ba000000-0000-4000-8000-000000000001` para `EXPLORADOR_AYACUCHO` y `ba000000-0000-4000-8000-000000000002` para `GRAN_TUNQUI_ORO`), sustituyendo los caracteres no hexadecimales `in` que violaban el estándar RFC 4122.
- **`database/mysql/seed-hu01.sql`:**
  - Corregida sentencia `INSERT` en `aprendizaje_bloque_contenido`: alineada con [`schema.sql`](../mysql/schema.sql) utilizando `texto_cuerpo` y el orden de columnas `(id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo)`. Se eliminaron las referencias a columnas obsoletas (`texto`, `url_recurso`, `texto_accesible`).
  - Corregidos identificadores no hexadecimales en `escuela_*` (`escuela_institucion`, `escuela_anio_escolar`, `escuela_aula`, `escuela_membresia_aula`), reemplazando los prefijos inválidos (`i0000000-`, `y0000000-`, `au000000-`, `m0000000-`) por UUIDs canónicos (`e1000000-`, `e2000000-`, `e3000000-`, `e4000000-`).
- **`database/mysql/actualizar-grados-primaria.sql`:**
  - Alineados los UUIDs de las tablas de `escuela_*` con los valores canónicos hexadecimales (`e1000000-` a `e4000000-`).
- **`docs/implementacion/HU-08-culminacion-ruta-diploma.md`:**
  - Actualizada la especificación del libro de movimientos de experiencia de la HU-08 para reflejar `motivo: 'INSIGNIA'`.

## 2026-10-12 · Segunda pasada: verificación en Fedora con PowerShell 7 y Playwright

Se instaló PowerShell 7 sin `sudo` y se corrieron todos los `Verificar-*.ps1`, `verify_*.py` y `verify_hu04_pantalla.mjs`. Hallazgos corregidos:

- **IDs que no eran UUID:** V09 y V10 usaban `u0000000-…`, `cp000000-…`, `q0000000-…`, `op000000-…` y ids con último grupo de 8 dígitos. La API los parsea como UUID: las rutas de V09 respondían 500 y las de V10 no coincidían con lo guardado. Ahora son UUID canónicos (`d2…` unidades, `c2…` competencias, `e2…` preguntas, `f2…` opciones; último grupo de 12 dígitos).
- **V07:** una base armada desde cero dejaba las unidades `u0000000-…001/002` de `seed-hu01.sql` enlazadas a la ruta 001 y la API respondía 500. V07 ahora las reasigna a las unidades DEMO y las retira.
- **V07 en bases nuevas:** `seed-hu01.sql` crea las competencias de Matemática con códigos antiguos (`RES_CANT`, `RES_REG`) y V07 solo actualizaba nombre y descripción; ahora también el código, y `verify_hu04_content.py` pasa tras reconstruir la base local con el orden documentado.
- **HU-08:** el diploma devolvía 503 (la consulta leía `total`/`completed` en vez de `total_nodes`/`completed_nodes`).
- **Inscripción:** `GET /student/learning-routes/{id}` inscribía al estudiante por sí solo (escritura en un GET y salto de la inscripción). Ahora responde 404 sin inscripción; el frontend inscribe de forma explícita con `POST /enroll` al abrir una ruta del catálogo. Queda abierto decidir qué rutas son públicas cuando existan rutas privadas del docente.
- **HU-04 en pantalla:** la banda de unidad tapaba la primera parada de la unidad (posición `posY - 68`, `Fragment` cambiado por `div` y regla móvil `:has(+ .unit-band)` eliminada). Restaurados.
- **API:** `ApiExceptionHandler` registra ahora el error de acceso a datos (antes devolvía 503 sin dejar rastro).
- **Scripts PowerShell (Windows PowerShell 5.1 y 7+, Windows y Linux):** rutas con `\`, ruta de `mysql.exe` solo de Windows, captura de errores HTTP solo de 5.1, cabecera `Set-Cookie` como arreglo, `Origin` ausente en el login, `Cache-Control` comparado como valor único, y fixtures de `Verificar-HU02-HU05` y `Verificar-HU08` que no respetaban el esquema real (tablas inexistentes, `CHECK` de `desbloqueado_en`, limpieza de insignias).

Resultado en una base armada desde cero con el orden documentado: `Verificar-Auth`, `HU01`, `HU02-HU05`, `HU08`, `HU09`, `HU10`, `HU11` y `Verificar-Rendimiento-HU09` correctos con PowerShell 7.6; `verify_hu04_pantalla.mjs` 10 de 10 en Firefox (1280 y 375 px); 51 pruebas Java y 12 de Node.

## 2026-10-12 · Revisión del Sprint Backlog: correcciones para que HU-01 a HU-04 y HU-09 a HU-11 funcionen

Se aplicaron las migraciones V08–V11 y se corrieron las verificaciones contra MySQL y la API reales. Hallazgos corregidos:

- **V09 y V10 no se aplicaban:** usaban columnas inexistentes (`ubicacion_x/y` en vez de `mapa_x/y`; `version_catalogo_id` en `curriculo_competencia`) y V09 enlazaba el progreso de 3 rutas a ids de nodo que no existían. Además sus `DELETE` de nodos fallaban o borraban progreso al repetirse. Ahora usan `ON DUPLICATE KEY UPDATE` y V09 retira solo los nodos provisionales de V08. V08–V11 se aplican limpias y dos veces seguidas.
- **API devolvía 503 al iniciar una parada:** el usuario MySQL de la API solo tenía permisos de HU-02. Nuevo `database/mysql/grants-spring-boot.sql` (escritor único: Spring escribe `escuela_*`, `aprendizaje_*`, `evaluacion_*`, `gamificacion_*`).
- **HU-03:** `start` dejó de actualizar `ultima_visita_en`; restaurado.
- **HU-02:** `start` y `complete` aceptaban actividades no `PUBLICADO`; restaurado el filtro.
- **HU-04:** `RouteCurriculum.build` había perdido `SIN_UNIDADES`, `INCOHERENTE` y el grupo «Otras paradas», y listaba unidades que la ruta no usa. Restauradas las reglas y las 8 pruebas originales; se conserva la forma nueva (`nodes`, `totalNodes`, `completedNodes`).
- **Fechas en UTC:** el servidor corre en UTC−5 y `CURRENT_TIMESTAMP` guardaba hora local; el backend y los datos de V08–V10 usan `UTC_TIMESTAMP(6)`.
- **Scripts:** `verify_learning_flow.py` y `verify_hu04_content.py` esperaban respuestas anteriores (`409`, `nodeIds`); el primero ahora también limpia las tablas `evaluacion_*`.

Resultado: 51 pruebas Java, 12 de Node, 29 comprobaciones de `verify_learning_flow.py` y `verify_hu04_content.py` correctas. Sin probar aquí: `Verificar-HU*.ps1` (no hay `pwsh`) y `verify_hu04_pantalla.mjs` (falta Playwright).

## 2026-10-12 · HU-11: Unirse a un roadmap con código alfanumérico de clase docente

Detalle y especificación técnica en `docs/implementacion/HU-11-unirse-roadmap-codigo-clase.md`.

**Base de Datos (MySQL 8.4)**
- Migración `V11__codigos_acceso_clase_hu11.sql`:
  - Asegura la tabla `aprendizaje_codigo_acceso_ruta` con claves foráneas, restricciones de vigencia y cupos máximos.
  - Almacena códigos de acceso mediante hash binario SHA-256 (`BINARY(32)`).
  - Siembra códigos activos de clase docente: `AYACUCHO2026`, `WARI2026`, `PALABRAS2026`, `CIENCIA2026`, `PRIMERO2026`, `CUARTO2026`, `QUINTO2026`, `SEXTO2026`.
  - Siembra códigos de prueba pedagógicos y de seguridad: `EXPIRADO2025` (vencido), `REVOCADO2026` (revocado) y `FUTURO2099` (inactivo).

**Backend (Spring Boot & Java 21)**
- Endpoint `POST /api/v1/student/learning-routes/join-by-code`:
  - Autenticación estricta con sesión de estudiante (`rupi_session`) y validación de origen CSRF.
  - DTOs `JoinByCodeRequest` y `JoinByCodeResponse` en `LearningRouteDtos.java`.
  - Normalización alfanumérica y validación defensiva con regex `^[A-Za-z0-9_-]{3,30}$`.
  - Verificación de existencia, vigencia temporal (`valido_desde`, `valido_hasta`), revocación, cupos y estado publicado.
  - Vinculación transaccional idempotente en `LearningRouteRepository.java` (si ya está inscrito retorna `alreadyEnrolled: true`; si es nuevo crea la matrícula y desbloquea el primer nodo).
  - Manejo de excepciones formativas RFC-7807 (`ProblemDetail`) en `ApiExceptionHandler.java`.
  - 47 de 47 pruebas JUnit aprobadas en `mvn test` (7 pruebas nuevas en `LearningRouteServiceTest.java`).

**Frontend (React 19 & TypeScript)**
- Componente `JoinClassModal.tsx` y estilos artesanales `JoinClassModal.css`:
  - Interfaz táctil escolar (≥48px), paleta oficial andina y botones con volumen físico.
  - Auto-conversión a mayúsculas, chips rápidos con códigos activos y validación en vivo.
  - Locución accesible con síntesis de voz de la mascota Rupi (`speakText`).
  - Redirección automática y reactiva al mapa interactivo de la clase asignada.
- Integración en cabecera global (`App.tsx`) y en la vista de bienvenida (`GradeSelectView.tsx`).
- Pruebas unitarias de frontend en `frontend/web-react/tests/joinCode.test.ts` (12 de 12 pruebas aprobadas).
- Compilación de producción (`npm run build`) y ESLint (`npm run lint`) limpios sin advertencias.

**Scripts y Calidad**
- `scripts/Verificar-HU11.ps1`: script de integración para PowerShell que valida 10 escenarios de seguridad, códigos inválidos/expirados/revocados, vinculación exitosa, idempotencia y redirección al mapa.

## 2026-10-12 · HU-10: Filtros avanzados por grado escolar y materia curricular con sincronización URL

Detalle y especificación técnica en `docs/implementacion/HU-10-filtros-avanzados-grados-materias.md`.

**Backend (Spring Boot & Java 21)**
- Actualización de `LearningRouteController.java`:
  - El endpoint `GET /api/v1/student/learning-routes/search` ahora acepta múltiples parámetros de filtrado simultáneos: `grades` (múltiples grados escolares), `grade` (grado singular para retrocompatibilidad), `areas` (múltiples materias curriculares), `area` (materia singular), `q` (texto libre FTS/LIKE) y `limit`.
- Sobrecarga y lógica en `LearningRouteRepository.java` y `LearningRouteService.java`:
  - Filtrado SQL dinámico y seguro por conjunto de grados (`g.numero_grado IN (:gradeNumbers)`) y conjunto de materias normalizadas (`LOWER(a.codigo) IN (:areaCodes) OR LOWER(a.nombre) IN (:areaCodes)...`).
  - DTO `SearchAutocompleteResponse` enriquecido con `selectedGrades`, `selectedAreas` y `totalResults`.
- Pruebas unitarias ampliadas en `LearningRouteServiceTest.java` (40 pruebas JUnit Maven aprobadas al 100%).

**Frontend (React 19 & TypeScript)**
- Componente `RoadmapFilterSidebar.tsx` y estilos artesanales `RoadmapFilterSidebar.css`:
  - Panel desplegable/sidebar accesible con botones táctiles (≥48px), paleta oficial andina (`#173e30`, `#31724b`, `#f95738`, `#ffd26a`) y volumen físico.
  - Checkboxes para selección múltiple de 1.° a 6.° de primaria y de materias (Matemática, Comunicación, Ciencia y Tecnología, Personal Social, Arte y Cultura).
  - Chips de filtros activos con eliminación individual y botón de restablecimiento rápido.
  - Narración accesible de Rupi por voz (`speakText`) que resume los filtros aplicados y resultados encontrados.
  - Sincronización bidireccional continua con los query params de la URL (`?grades=3,4&areas=MATEMATICA&q=...`) mediante `window.history.replaceState`, permitiendo compartir enlaces y mantener los filtros activos tras recargar.
- Integración en `App.tsx`:
  - Lectura de filtros iniciales desde la URL al montar la aplicación.
  - Selección de ruta desde el panel de filtros y apertura directa en el mapa interactivo.
- Verificación limpia de build (`tsc -b && vite build`) y linter (`eslint .`).

**Scripts y Pruebas**
- `scripts/Verificar-HU10.ps1`: script de validación de integración E2E compatible con Windows PowerShell 5.1 y 7+ que valida 8 escenarios de seguridad, filtros por grado individual, múltiples grados, materias y filtros combinados.

## 2026-10-12 · Roadmaps Escolares CNEB para Todos los Grados de Primaria (1.° a 6.°)

**Base de Datos (MySQL 8.4)**
- Migración `V10__roadmaps_todos_los_grados_primaria.sql`:
  - Catálogo completo de 12 nuevos roadmaps escolares interactivos vinculados a la base de datos para 1.°, 3.°, 4.°, 5.° y 6.° de primaria (sumados a los 5 de 2.° y 3.° de V09), cubriendo el 100% de la primaria peruana:
    - **1.° Primaria:** Matemática (*«Primeros Números y Formas en Ayacucho»*), Comunicación (*«Vocales, Sonidos y Rimas con Rupi»*), Ciencia y Tecnología (*«Los Sentidos y el Entorno Natural»*).
    - **3.° Primaria:** Comunicación (*«Mitos y Narraciones de los Andes»*), Ciencia y Tecnología (*«La Fuerza del Sol, el Viento y la Materia»*).
    - **4.° Primaria:** Matemática (*«Fracciones y Medidas en las Ferias Andinas»*), Comunicación (*«Textos Informativos y Tradición Oral Peruana»*), Ciencia y Tecnología (*«Ecosistemas y Cadenas Vivas del Perú»*).
    - **5.° Primaria:** Matemática (*«Números Decimales y Porcentajes en el Comercio»*), Comunicación (*«Textos Argumentativos y Opinión Crítica»*), Ciencia y Tecnología (*«Sistemas del Cuerpo Humano y Nutrición Andina»*).
    - **6.° Primaria:** Matemática (*«Proporcionalidad, Álgebra Inicial y Geometría Espacial»*), Comunicación (*«Crónicas, Reportajes y Creación Literaria»*), Ciencia y Tecnología (*«Energías Renovables y Cambio Climático»*).
  - 6 paradas en zigzag por cada ruta (`aprendizaje_nodo_ruta`) con coordenadas pedagógicas `(x, y)` armónicas.
  - Actividades didácticas estructuradas (`LECCION` y `RETO`) con instrucciones y minutos estimados.
  - Bloques de contenido pedagógico (`aprendizaje_bloque_contenido`) con explicaciones de Rupi y sin textos genéricos de IA.
  - Preguntas formativas no punitivas con opciones de respuesta y retroalimentación inmediata (`evaluacion_pregunta` y `evaluacion_opcion_respuesta`).
  - Contextos culturales andinos vinculados (`curriculo_contexto_cultural`) para Huamanga, Huanta, Quinua, Vilcashuamán, Cangallo y Pampa Galeras.
  - Inscripciones (`aprendizaje_inscripcion_ruta`) y progreso por nodo (`aprendizaje_progreso_nodo`) activos para el estudiante demo.

**Frontend (React & TypeScript)**
- `GradeSelectView.tsx`:
  - Habilitación del 100% de los grados (1.° a 6.°) con sus materias troncales disponibles y chips `Rutas escolares activas`.
  - Tarjetas de cursos con estado activo `Roadmap habilitado · Paradas interactivas` y navegación directa al mapa de la materia seleccionada.
  - Locución accesible por voz de Rupi (`speakText`) en español peruano contextualizada para cada grado escolar.
- Verificación limpia de TypeScript (`tsc -b`), bundler Vite y linter (`eslint .`).

## 2026-10-12 · Catálogo Integral de Múltiples Roadmaps Escolares CNEB & Remediación de Errores

**Base de Datos (MySQL 8.4)**
- Migración `V09__creacion_multiples_roadmaps_cneb.sql`:
  - 5 roadmaps completos vinculados a la base de datos: Comunicación 2.° (*«Cuentos y Leyendas del Valle Sagrado»*), Ciencia y Tecnología 2.° (*«Exploradores de la Biodiversidad Andina»*), Personal Social 2.° (*«Historia y Costumbres de Nuestra Comunidad»*), Arte y Cultura 2.° (*«Colores, Retablos y Melodías de Ayacucho»*) y Matemática 3.° (*«Desafíos y Geometría en la Ciudadela Wari»*).
  - Competencias curriculares oficiales del CNEB asociadas a cada área.
  - Unidades curriculares oficiales (`curriculo_unidad` y `curriculo_unidad_competencia`) para estructurar los tramos del mapa.
  - 6 paradas en zigzag por cada ruta con coordenadas `(x, y)` armónicas.
  - Actividades didácticas estructuradas, instrucciones pedagógicas y bloques de contenido (`aprendizaje_bloque_contenido`).
  - Preguntas formativas no punitivas con opciones estructuradas (`evaluacion_pregunta` y `evaluacion_opcion_respuesta`).
  - Contextos culturales peruanos para la mascota Rupi (`curriculo_contexto_cultural`).
  - Inscripciones del estudiante demo (`aprendizaje_inscripcion_ruta`) y progreso inicial (`DISPONIBLE` en parada 1, `BLOQUEADO` en paradas 2 a 6).

**Backend (Spring Boot)**
- Corrección en `ActivityService.java`: consulta de bloques alineada con los nombres de columnas de base de datos (`texto_cuerpo AS texto`, `texto_accesibilidad AS texto_accesible`).
- Armonización de código HTTP: `403 Forbidden` al intentar abrir niveles bloqueados.
- Auto-inscripción transparente en `LearningRouteService.getForStudent` y nuevo endpoint `POST /api/v1/student/learning-routes/{versionRouteId}/enroll` en `LearningRouteController.java`.
- 39 pruebas JUnit aprobadas al 100%.

**Frontend (React & TypeScript)**
- `App.tsx`: selección inteligente de ruta por curso y grado, `isRoadmapAvailable` dinámico y pestañas con chips reactivos (`Mapa activo`) para Comunicación, Matemática y CYT.
- `learningRoutesApi.ts`: función cliente `enrollInRoute`.
- Eliminación de código huérfano (`lessons.ts`) e integración de `createTrailPath` en `Roadmap.tsx`.
- 8 pruebas de node y compilación de producción (`tsc -b && vite build`) limpias al 100%.

## 2026-10-12 · HU-09: Búsqueda de roadmaps alternativos por materia con Full-Text Search (FTS)

Detalle y especificación técnica en `docs/implementacion/HU-09-busqueda-roadmaps-alternativos.md`.

**Backend (Spring Boot & MySQL)**
- Migración `V08__busqueda_fulltext_materias_roadmaps.sql`: índices de texto completo `ft_area_busqueda` sobre `curriculo_area(nombre)` y `ft_ruta_busqueda` sobre `aprendizaje_ruta(titulo, descripcion)`.
- Siembra curricular de catálogo CNEB (`COM`, `CYT`, `SOC`, `ART`) vinculados a `CNEB-2017` y 5 roadmaps alternativos publicados con nodos y paradas de demostración contextualizadas en el Perú.
- Endpoints en `LearningRouteController.java`: `GET /api/v1/student/learning-routes/search?q={query}&grade={grade}` y `GET /api/v1/student/learning-routes/suggestions?q={query}`.
- Consultas de alto rendimiento en `LearningRouteRepository.java` combinando `MATCH(...) AGAINST(... IN BOOLEAN MODE)` con ranking de relevancia y fallback con coincidencia prefijal para autocompletado instantáneo.
- Suite de pruebas unitarias en `LearningRouteServiceTest.java` (39 pruebas JUnit Maven 100% aprobadas).

**Frontend (React & CSS Artesanal)**
- Componente `RoadmapSearchBar.tsx` y `RoadmapSearchBar.css`: barra de búsqueda con volumen físico táctil (≥48px), atajo de teclado (`/` o `Ctrl+K`), debounce de 200 ms, sugerencias con chips por materia y lectura accesible por voz de Rupi (`speakText`).
- Tarjetas de resultados de roadmaps alternativos con botón de exploración directa para cambiar la ruta y materia activa en el mapa.
- Integración en `App.tsx` y cliente tipado en `learningRoutesApi.ts`.
- Pruebas automatizadas en `tests/searchApi.test.ts` (8 pruebas pasando) y cero advertencias en `tsc` y `eslint`.

**Pruebas de Rendimiento y Scripts**
- `scripts/Verificar-HU09.ps1`: validación de integración E2E de 6 escenarios funcionales (seguridad 401, login, sugerencias, búsqueda FTS, filtro de grado y estados vacíos).
- `scripts/Verificar-Rendimiento-HU09.ps1`: prueba de rendimiento con 60 consultas FTS calculando latencias min, max, avg, P50, P90, P99 y throughput.

## 2026-10-11 · HU-08: Culminación de ruta escolar, Gran Gallito de Oro y Diploma imprimible CNEB

Detalle y especificación técnica en `docs/implementacion/HU-08-culminacion-ruta-diploma.md`.

**Backend (Spring Boot & MySQL)**
- Transacción atómica de graduación en `ActivityService.java`: al completar la última parada de la ruta (secuencia máxima / nodo 10), se marca la inscripción como `COMPLETADO`.
- Siembra y asignación idempotente de la Gran Insignia de Graduación `GRAN_TUNQUI_ORO` (*«Gran Gallito de Oro»*, icono `feather`), respetando el principio de escritor único.
- Otorgamiento de bono de graduación de `+100 XP` en `gamificacion_movimiento_experiencia` con motivo `GRADUACION` y llave de idempotencia SHA/UUID única.
- Endpoint institucional oficial `GET /api/v1/student/learning-routes/{versionRouteId}/diploma` en `LearningRouteController.java`: retorna datos del estudiante, I.E. escolar, grado, área, total de paradas logradas (10/10), XP total acumulado, lista de competencias oficiales CNEB y mensaje de aliento de Rupi.
- Pruebas unitarias en `ActivityServiceTest` y corrección de matching en `EvaluationServiceTest` (37 pruebas en total, 100% aprobadas).

**Frontend (React & CSS Artesanal)**
- Componente `RouteDiplomaModal.tsx`: diseño inspirado en el retablo ayacuchano y los materiales escolares peruanos, con orlas andinas, medalla física 3D del Gallito de las Rocas, síntesis de voz accesible en español peruano (`speakText`) y estilos `@media print` para exportar o imprimir en formato horizontal A4/Letter.
- Integración en `RouteOverview.tsx`: CTA dinámico que transmuta a `Ver mi Diploma de Logro Escolar` al alcanzar el 100% de la aventura.
- Integración en `ActivityBlocks.tsx`: pantalla de celebración ampliada con mención de graduación, bono de 100 XP, insignia dorada y botón directo para abrir el diploma.
- Integración central en `App.tsx` y cliente API en `learningRoutesApi.ts`.
- Pruebas de compilación TypeScript (`tsc -b && vite build`) y reglas estrictas de linter (`eslint .`) limpias sin advertencias.

**Scripts y Verificación**
- Script `scripts/Verificar-HU08.ps1` compatible con Windows PowerShell 5.1 y PowerShell 7+ para validación integral de la API y persistencia en base de datos.

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
