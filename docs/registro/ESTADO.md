# Estado actual · RUPI

Actualizado: 2026-10-14 · rama `main` · después de integrar HU-12 a HU-14 del equipo (`75a62b4`) y las correcciones de `0f8b83a` y `5719fb8`. Resumen de ese bloque en [`README-CAMBIOS-2026-10-14.md`](README-CAMBIOS-2026-10-14.md).

## Cómo retomar (5 minutos)

1. Leer este archivo y `CONTEXTO.md`. Si se va a tocar código, también `MAPA-DE-CODIGO.md`.
2. Levantar el entorno: `./iniciar-rupi.sh` (MySQL 3307, API 8081, web 5173). Cuenta demo: `estudiante.demo` / `123456`.
3. Comprobar que todo sigue en verde (comandos en `CONTEXTO.md`, sección Verificaciones).
4. Si la base local se ve rara (rutas que dan 500, ids viejos), reconstruirla con el orden de `docs/implementacion/entorno-linux.md` (schema → V04 → seed-hu01 → V06 → seed-hu02 → V07 a V17 → `grants-spring-boot.sql`). Para una base existente basta con aplicar V13 a V17, repetir V09 y V10 y volver a otorgar los permisos. El script usado el 2026-10-12 quedó en `~/reconstruir-bd-rupi.sh`, y el respaldo previo en `~/rupi-respaldo-2026-10-12.sql`.

## Fuente de verdad del alcance

El backlog y el Sprint Backlog están en dos documentos de Google Docs del curso (Laboratorio SCRUM, Construcción y Pruebas de Software, Tecsup): `GLAB-S06-2026-01` y `GLAB-S07-2026-02-1`. Son 60 historias en 15 épicas. El Sprint 1 son las historias 1 a 4 (30 h de capacidad: 3 integrantes × 10 h). Los enlaces los guarda el usuario; hay copia del resumen en `CHANGELOG.md` y en la memoria de Claude.

La numeración HU-xx del repo coincide con el backlog solo en **HU-01 a HU-04** y **HU-09 a HU-11**. HU-05 a HU-08 del repo (completar lección con XP, preguntas, insignias, diploma) son mejoras de gamificación web que el backlog no numera así; en el backlog, las historias 5 a 8 son de autoría docente y siguen pendientes.

## Historias de usuario del repo y del backlog

| HU | Alcance y Resultado | Estado |
|---|---|---|
| HU-01 | Mapa interactivo con datos reales por estudiante | Completada y verificada |
| HU-02 | Clic en un nivel: abrir actividad y marcarla EN_CURSO | Completada y verificada |
| HU-03 | Regresar al punto exacto del mapa | Completada y verificada |
| HU-04 | Ruta agrupada por unidades MINEDU | Completada y verificada |
| HU-05 | **Autoría y Gestión de Roadmaps Docente** (Scrum Backlog): Rol docente, creación de roadmaps personalizados por grado CNEB (1.° a 6.°) y materia | **Completada y verificada** (`Verificar-HU05-Docente.ps1`) |
| HU-06 | **Reorganizar Secuencia de Aprendizaje con Drag & Drop** (Scrum Backlog): Reordenamiento interactivo de paradas pedagógicas por el profesor | **Completada y verificada** (`Verificar-HU06-Reordenamiento.ps1`) |
| HU-07 | **Subida y vinculación de materiales educativos (PDF, PPT, enlaces) a nodos** (Scrum Backlog) / Insignias formativas | **Completada y verificada** |
| HU-08 | Repo: fin de ruta y vitrina de insignias. **Backlog (historia 8): publicar o pausar la visibilidad de una ruta** | Completada. Pausa agregada el 2026-10-14 (`POST /teacher/learning-routes/{id}/pause`, interruptor en el panel docente); verificado que el alumno pierde el acceso al instante y lo recupera al volver a publicar |
| HU-09 | Búsqueda de roadmaps por materia con FTS y autocompletado | Completada y verificada |
| HU-10 | Filtros por grado y materia con URL sincronizada | Completada y verificada |
| HU-11 | Unirse a un roadmap con código de clase docente | Completada y verificada |
| HU-12 | Descargar una unidad (guía PDF, materiales y ejercicios en zip) para estudiar sin conexión. Cierra la épica 3. El equipo agregó la biblioteca MINEDU por grado y la descarga por nodo | Completada y verificada (`Verificar-HU12-Descarga.ps1`, `verify_hu12_pantalla.mjs`) |
| HU-13 | Calendario: el docente asigna fechas a unidades y evaluaciones por aula (`escuela_planificacion_ruta`, V13) | Completada. Verificada de punta a punta el 2026-10-14 con el docente demo como responsable del aula (V16). Desde el 2026-10-14 también calendariza el docente con un aula activa del grado en cualquier ruta publicada (roadmap institucional), siempre solo para sus aulas |
| HU-14 | Recordatorio automático 24 h antes de cada evaluación, sin duplicados (`escuela_recordatorio_evaluacion`, V14) | Completada. Verificada el 2026-10-14: el alumno recibe el aviso en el siguiente ciclo (60 s) |

«Verificada» significa probada contra la API Spring Boot, MySQL y frontend React reales.

## Qué sigue

Las historias 1 a 14 del backlog están completas y verificadas (revisión modular del 2026-10-14). Siguen:

1. **Historia 15:** nivel de asistencia del tutor IA por aula. No hay nada de IA en el repo; el equipo debe decidir el motor.
2. **Historia 16:** actividades complementarias MINEDU donde los alumnos tienen bajo rendimiento.
3. **Decisiones con el equipo:**
   - No volver a editar migraciones publicadas (V09 y V10 se editaron).
   - Clasificar Oficial o Comunidad por rol o columna, no por el nombre de usuario `profesor%`.
   - Escritor único en `identidad_*`.
4. **Reportes de aula** (historias 17 a 20).

Decisión abierta: qué rutas del catálogo serán públicas cuando existan rutas privadas del docente. Hoy cualquier ruta publicada se puede abrir con `POST /enroll`.

## Fallos conocidos (2026-10-14)

- **Grado por defecto:** un alumno sin aula activa recibe 2.° de primaria (`StudentSessionService`). Con varias aulas, la elección ya es determinista.
- **A 320 px**, la pestaña «Mi Perfil» queda parcialmente fuera del menú. Se alcanza deslizando el menú. A 375 px y más caben las tres pestañas.
- **Iniciar sesión cierra las sesiones anteriores de la misma cuenta.** Es intencional: lo pide la historia 43 (sesión única). Dos pestañas o dos pruebas con `estudiante.demo` a la vez se expulsan entre sí (401).
- **Descargar un material** (`GET /api/v1/materials/{id}/download`) no exige sesión: basta con conocer el id (UUID). Decidir si debe exigir alumno inscrito o docente.
- Los enlaces externos adjuntados **antes** del 2026-10-14 siguen sin botón para el alumno (la URL solo está en el texto): hay que volver a adjuntarlos.
- **V12 escribe `identidad_*` desde una migración del lado Spring** (escritor único: Django). Pendiente de decidir con el equipo.

Resueltos el 2026-10-14:
- Abrir lecciones y subir materiales ya no responden 503: el equipo alineó las columnas.
- Crear una parada devuelve `nodeId`.
- Mi Perfil ya no desborda a 375 px.
- Los paneles de error del aula se muestran.
- Entrar a un roadmap ya no se queda en el esqueleto.
- V12 corre sobre `schema.sql`: sin `es_sistema` ni `creado_en`, con el aula buscada por membresía y con `recurso_archivo_multimedia` alineada a `schema.sql`. Probado armando una base desde cero con todas las migraciones, dos veces.
- Subir un material ya no deja archivos huérfanos: si el `INSERT` falla, se borra el archivo (prueba `deletesFileWhenInsertFails`).
- La sesión elige siempre el mismo rol (DOCENTE primero) y la misma aula (la activa más reciente).
- Barra superior a 375 px: las tres pestañas caben y «Mi Camino» ya no queda fuera de la pantalla.
- **Revisión modular de las historias 1 a 14:**
  - **Progreso:** una parada agregada después de inscribirse quedaba bloqueada para siempre (no tenía fila de progreso y el desbloqueo era un `UPDATE` sin efecto). Además, abrir la parada 1 sin fila funcionaba, pero completarla respondía 403. Ahora la API crea las filas que falten al agregar la parada y al abrir o completar una actividad, y V17 repara los datos anteriores.
  - **Autoría (historias 5 a 7):** ruta ajena o inexistente al agregar parada o publicar → 404 (antes 500); tipo de actividad o de material fuera del catálogo → 400 (antes 500); secuencia ocupada → la parada va al final (antes 500); área inexistente → 400 (antes tomaba un área cualquiera); título de más de 180 caracteres → 400.
  - **Materiales y enlaces (historia 7):** solo se aceptan enlaces `http(s)://` o descargas de RUPI (antes cualquier texto, incluido `javascript:`); el material adjunto debe ser del mismo docente; los enlaces externos llegan al alumno con botón «Abrir enlace» (antes la URL quedaba solo en el texto).
  - **Pausa (historia 8)** y **calendario institucional (historia 13):** ver la tabla de historias.
  - **Scripts:** `Verificar-HU05-Docente.ps1` y `Verificar-HU06-Reordenamiento.ps1` llamaban a `mvn.cmd` (solo Windows); `Verificar-HU01.ps1` esperaba el 503 anterior; `verify_hu12_resources.py` fallaba con rutas sin unidades.

La historia 12 no depende de esas consultas: lee `tamano_bytes` y calcula la ruta del archivo con `MaterialStorageService.diskPathFor`.

## Pendiente menor

- Documento del Sprint: las filas 6 y 7 del Sprint Backlog repiten las historias 1 y 2, la fila 8 mezcla el texto de la historia 21 con tareas de la 8, la tarea «Modelar BD para unidades MINEDU» está bajo la historia 3 en vez de la 4, y una tarea nombra Node.js/Express (el proyecto usa Spring Boot). El Burndown está vacío.
- HU-04: reemplazar las unidades `DEMO-*` por la programación real cuando Django cargue el catálogo oficial; seguir la actualización del CNEB (RM 393-2026-MINEDU); confirmar la regla de paradas sin unidad (hoy: grupo final «Otras paradas»).
- [x] Corregido `seed-hu01.sql` en línea 277 (columnas `texto_cuerpo` y `numero_secuencia` alineadas con `schema.sql`) y UUIDs canónicos para `escuela_*` (`e1000000-…` a `e4000000-…`), replicado en `actualizar-grados-primaria.sql`.
- [x] Corregido `ActivityService.java` para alinear el motivo ENUM a `'INSIGNIA'` y normalizar UUIDs de insignias a hexadecimal canónico (`ba000000-…`).
- Las pruebas de Node son 12 (todas pasando).

## Deuda técnica conocida (sin priorizar)

- Spring escribe `identidad_sesion_usuario` e `identidad_intento_acceso` (login), contra el escritor único; reconciliar con Django.
- Elegir un único mecanismo de migraciones y fijar el orden Django/Spring. Hoy se aplican a mano.
- Privacidad y acceso para menores antes de publicar. La cuenta demo y los datos demo no son para despliegue.
- `App.css` y `AuthViews.css` superan las 1700 líneas cada una; conviene dividirlas.
- El endpoint de unirse a clase acepta peticiones sin cabecera `Origin`; decidir si es intencional.

## Última evidencia de pruebas

2026-10-14, revisión modular de las historias 1 a 14 (base local con V17):

- 134 pruebas Java (9 omitidas, de MySQL); en el frontend, build, lint y 21 pruebas de Node.
- Los 14 scripts del repo pasan en Linux con PowerShell 7.6: `Verificar-Auth`, `HU01`, `HU02-HU05`, `HU05-Docente`, `HU06-Reordenamiento`, `HU08`, `HU09`, `HU10`, `HU11`, `HU12-Descarga`, `Rendimiento-HU09`, `verify_learning_flow.py` (29), `verify_hu04_content.py` y `verify_hu12_resources.py`.
- 22 comprobaciones de punta a punta contra la API (Playwright): validaciones de autoría, enlaces, progreso con paradas agregadas después, pausa y reanudación, y calendario institucional. El interruptor de pausa se probó también en el panel docente.

2026-10-14 (base local con V13 a V16 aplicadas, API recompilada en `75a62b4`):

- `mvn test` correcto. En el frontend, `npm run build`, `npm run lint` y `npm test` (21 de Node) correctos.
- **App real con Playwright y Firefox:**
  - Ingreso a roadmaps: «Continuar mi aventura», grado + materia y repetir la misma ruta desde Explorar.
  - Conexión lenta: aparece «Reintentar» y nunca el login.
  - Error 500 en la ruta: se muestra el panel de error de red y «Reintentar» recupera el mapa.
  - A 375 px no hay desplazamiento horizontal en Mi Camino, Explorar y Mi Perfil.
- **Flujo docente por API:** crear ruta, crear parada, subir PDF, adjuntar, publicar, poner fecha a una evaluación (HU-13) y recibir el recordatorio como alumno (HU-14). La ruta de prueba «Prueba revisión 14-oct» quedó en la base local.

2026-10-13 (historia 12, base local del 2026-10-12 con las tablas de V12):

- 85 pruebas Java y 17 de Node; lint y build correctos.
- `scripts/Verificar-HU12-Descarga.ps1` (20/20, PowerShell 7.6) y `scripts/verify_hu12_pantalla.mjs` (10/10, Firefox 1280 y 375 px).

2026-10-12 (Fedora 44, base armada desde cero):

- 51 pruebas Java y 12 de Node; lint y build correctos.
- `scripts/Verificar-Auth.ps1`, `HU01`, `HU02-HU05`, `HU08`, `HU09`, `HU10`, `HU11` y `Verificar-Rendimiento-HU09` con PowerShell 7.6.
- `scripts/verify_learning_flow.py` (29), `verify_hu04_content.py` y `verify_hu04_pantalla.mjs` (10/10, Firefox 1280 y 375 px).
- Detalle de lo corregido ese día en `CHANGELOG.md`.

El entorno Linux se probó en Fedora 44. Sin probar: otras distribuciones y ARM.
