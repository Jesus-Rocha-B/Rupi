# Estado actual · RUPI

Actualizado: 2026-10-13 · rama `main` · historia 12 del backlog (descarga offline por unidad) sobre `bf25c2d`.

## Cómo retomar (5 minutos)

1. Leer este archivo y `CONTEXTO.md`. Si se va a tocar código, también `MAPA-DE-CODIGO.md`.
2. Levantar el entorno: `./iniciar-rupi.sh` (MySQL 3307, API 8081, web 5173). Cuenta demo: `estudiante.demo` / `123456`.
3. Comprobar que todo sigue en verde (comandos en `CONTEXTO.md`, sección Verificaciones).
4. Si la base local se ve rara (rutas que dan 500, ids viejos), reconstruirla con el orden de `docs/implementacion/entorno-linux.md` (schema → V04 → seed-hu01 → V06 → seed-hu02 → V07 a V11 → `grants-spring-boot.sql`). El script usado el 2026-10-12 quedó en `~/reconstruir-bd-rupi.sh`, y el respaldo previo en `~/rupi-respaldo-2026-10-12.sql`.

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
| HU-08 | Fin de ruta y vitrina de insignias de perfil escolar / Publicación docente | Completada y verificada |
| HU-09 | Búsqueda de roadmaps por materia con FTS y autocompletado | Completada y verificada |
| HU-10 | Filtros por grado y materia con URL sincronizada | Completada y verificada |
| HU-11 | Unirse a un roadmap con código de clase docente | Completada y verificada |
| HU-12 | Descargar una unidad (guía PDF, materiales y ejercicios en zip) para estudiar sin conexión. Cierra la épica 3 | Completada y verificada (`Verificar-HU12-Descarga.ps1`, `verify_hu12_pantalla.mjs`) |

«Verificada» significa probada contra la API Spring Boot, MySQL y frontend React reales.

## Qué sigue

1. **Continuar con las Historias 7 y 8 de la Épica Docente:**
   - Historia 7: Subida de archivos PDF, PPT y enlaces externos a nodos específicos (verificar si se requiere algún ajuste o validación adicional a lo ya construido).
   - Historia 8: Gestión de publicación, visibilidad y asignación a aulas escolares.
2. **Corregir los fallos conocidos** de la sección siguiente: hoy impiden abrir lecciones y subir materiales sobre una base armada desde `schema.sql`.
3. **Calendario y planificación presencial** (historias 13 y 14) y reportes de aula (17 a 20).

Decisión abierta: qué rutas del catálogo serán públicas cuando existan rutas privadas del docente. Hoy cualquier ruta publicada se puede abrir con `POST /enroll`.

## Fallos conocidos (2026-10-13)

- **Abrir una lección responde 503** sobre una base armada desde `schema.sql`: `ActivityService` lee `m.url_almacenamiento` de `recurso_archivo_multimedia`, columna que no existe (el esquema tiene `clave_almacenamiento` y `url_externa`). Viene de `f280e73`.
- **Subir materiales responde 503**: `MaterialStorageService` inserta `tamanio_bytes` y `url_almacenamiento`; el esquema tiene `tamano_bytes` y exige `clave_almacenamiento` o `url_externa` (CHECK). Además guarda el archivo antes del `INSERT`, así que un fallo deja un archivo huérfano en `uploads/materials/`. `MaterialStorageService.findById` lee las mismas columnas.
- **`V12__rol_docente_autoria_hu05.sql` no corre sobre `schema.sql`**: inserta `es_sistema` y `creado_en` en `identidad_rol`, columnas que no existen. Su paso 6 usa el aula `e3000000-…`, que solo existe con el `seed-hu01.sql` actual. V12 también escribe `identidad_*` desde el lado Spring (escritor único: Django).
- Crear una parada como docente (`POST /teacher/learning-routes/{routeId}/nodes`) no devolvió `nodeId` en la prueba del 2026-10-13; sin investigar.
- A 375 px la página tiene desplazamiento horizontal por `search-submit-btn` (HU-09) y `mascot-badge`.

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

2026-10-13 (historia 12, base local del 2026-10-12 con las tablas de V12):

- 85 pruebas Java y 17 de Node; lint y build correctos.
- `scripts/Verificar-HU12-Descarga.ps1` (20/20, PowerShell 7.6) y `scripts/verify_hu12_pantalla.mjs` (10/10, Firefox 1280 y 375 px).

2026-10-12 (Fedora 44, base armada desde cero):

- 51 pruebas Java y 12 de Node; lint y build correctos.
- `scripts/Verificar-Auth.ps1`, `HU01`, `HU02-HU05`, `HU08`, `HU09`, `HU10`, `HU11` y `Verificar-Rendimiento-HU09` con PowerShell 7.6.
- `scripts/verify_learning_flow.py` (29), `verify_hu04_content.py` y `verify_hu04_pantalla.mjs` (10/10, Firefox 1280 y 375 px).
- Detalle de lo corregido ese día en `CHANGELOG.md`.

El entorno Linux se probó en Fedora 44. Sin probar: otras distribuciones y ARM.
