# Estado actual · RUPI

Actualizado: 2026-10-12 (cierre de sesión) · rama `main` = `origin/main` · último commit `c3200a3`.

## Cómo retomar (5 minutos)

1. Leer este archivo y `CONTEXTO.md`. Si se va a tocar código, también `MAPA-DE-CODIGO.md`.
2. Levantar el entorno: `./iniciar-rupi.sh` (MySQL 3307, API 8081, web 5173). Cuenta demo: `estudiante.demo` / `123456`.
3. Comprobar que todo sigue en verde (comandos en `CONTEXTO.md`, sección Verificaciones).
4. Si la base local se ve rara (rutas que dan 500, ids viejos), reconstruirla con el orden de `docs/implementacion/entorno-linux.md` (schema → V04 → seed-hu01 → V06 → seed-hu02 → V07 a V11 → `grants-spring-boot.sql`). El script usado el 2026-10-12 quedó en `~/reconstruir-bd-rupi.sh`, y el respaldo previo en `~/rupi-respaldo-2026-10-12.sql`.

## Fuente de verdad del alcance

El backlog y el Sprint Backlog están en dos documentos de Google Docs del curso (Laboratorio SCRUM, Construcción y Pruebas de Software, Tecsup): `GLAB-S06-2026-01` y `GLAB-S07-2026-02-1`. Son 60 historias en 15 épicas. El Sprint 1 son las historias 1 a 4 (30 h de capacidad: 3 integrantes × 10 h). Los enlaces los guarda el usuario; hay copia del resumen en `CHANGELOG.md` y en la memoria de Claude.

La numeración HU-xx del repo coincide con el backlog solo en **HU-01 a HU-04** y **HU-09 a HU-11**. HU-05 a HU-08 del repo (completar lección con XP, preguntas, insignias, diploma) son mejoras de gamificación web que el backlog no numera así; en el backlog, las historias 5 a 8 son de autoría docente y siguen pendientes.

## Historias de usuario del repo

| HU | Resultado | Estado |
|---|---|---|
| HU-01 | Mapa interactivo con datos reales por estudiante | Completada y verificada |
| HU-02 | Clic en un nivel: abrir actividad y marcarla EN_CURSO | Completada y verificada |
| HU-03 | Regresar al punto exacto del mapa | Completada y verificada |
| HU-04 | Ruta agrupada por unidades MINEDU | Completada y verificada, incluida la pantalla. Competencias, estándares y desempeños son oficiales; las unidades son de demostración |
| HU-05 | Completar lección: +50 XP y desbloqueo del siguiente nivel | Verificada (mejora de gamificación) |
| HU-06 | Preguntas didácticas con retroalimentación no punitiva y audio de Rupi | Verificada (mejora) |
| HU-07 | Insignias escolares y resumen formativo | Verificada (mejora) |
| HU-08 | Fin de ruta, Gran Gallito de Oro (+100 XP) y diploma imprimible | Verificada (mejora) |
| HU-09 | Búsqueda de roadmaps por materia con FTS y autocompletado | Completada y verificada |
| HU-10 | Filtros por grado y materia con URL sincronizada | Completada y verificada |
| HU-11 | Unirse a un roadmap con código de clase | Completada y verificada |

«Verificada» significa probada contra la API y MySQL reales en Fedora. No sustituye la aceptación del docente ni la validación pedagógica.

## Qué sigue (en este orden sugerido)

1. **Historia 12** del backlog: descargar materiales de la ruta para estudiar sin conexión (cierra la épica 3).
2. **Rol docente y login (historias 41 y 42):** es la dependencia de la autoría docente (historias 5 a 8), el calendario (13 y 14), la IA configurable (15 y 16) y los reportes (17 a 20). Hoy solo existe el estudiante.
3. **Historias 5, 7 y 8:** crear roadmaps propios, subir PDF/PPT/enlaces a un nodo y publicar o pausar. Con ellas HU-11 deja de depender de códigos sembrados.
4. Después: móvil Kotlin (29 a 36), IA (21 a 24, 37 a 40, 53 a 56), administración y seguridad (45 a 60).

Decisión abierta: qué rutas del catálogo serán públicas cuando existan rutas privadas del docente. Hoy cualquier ruta publicada se puede abrir con `POST /enroll`.

## Pendiente menor

- Documento del Sprint: las filas 6 y 7 del Sprint Backlog repiten las historias 1 y 2, la fila 8 mezcla el texto de la historia 21 con tareas de la 8, la tarea «Modelar BD para unidades MINEDU» está bajo la historia 3 en vez de la 4, y una tarea nombra Node.js/Express (el proyecto usa Spring Boot). El Burndown está vacío.
- HU-04: reemplazar las unidades `DEMO-*` por la programación real cuando Django cargue el catálogo oficial; seguir la actualización del CNEB (RM 393-2026-MINEDU); confirmar la regla de paradas sin unidad (hoy: grupo final «Otras paradas»).
- `seed-hu01.sql` falla en la línea 277 (columna `texto`, hoy `texto_cuerpo`); `seed-hu02.sql` y V07 lo compensan, pero conviene corregirlo.
- Ids de `seed-hu01.sql` para `escuela_*` no son UUID válidos (`i0000000-…`, `au000000-…`, `m0000000-…`, `y0000000-…`); hoy ningún endpoint los lee.
- Las pruebas de Node son 12 (el CHANGELOG de HU-10/HU-11 anuncia más).

## Deuda técnica conocida (sin priorizar)

- Spring escribe `identidad_sesion_usuario` e `identidad_intento_acceso` (login), contra el escritor único; reconciliar con Django.
- Elegir un único mecanismo de migraciones y fijar el orden Django/Spring. Hoy se aplican a mano.
- Privacidad y acceso para menores antes de publicar. La cuenta demo y los datos demo no son para despliegue.
- `App.css` y `AuthViews.css` superan las 1700 líneas cada una; conviene dividirlas.
- El endpoint de unirse a clase acepta peticiones sin cabecera `Origin`; decidir si es intencional.

## Última evidencia de pruebas (2026-10-12, Fedora 44, base armada desde cero)

- 51 pruebas Java y 12 de Node; lint y build correctos.
- `scripts/Verificar-Auth.ps1`, `HU01`, `HU02-HU05`, `HU08`, `HU09`, `HU10`, `HU11` y `Verificar-Rendimiento-HU09` con PowerShell 7.6.
- `scripts/verify_learning_flow.py` (29), `verify_hu04_content.py` y `verify_hu04_pantalla.mjs` (10/10, Firefox 1280 y 375 px).
- Detalle de lo corregido ese día en `CHANGELOG.md`.

El entorno Linux se probó en Fedora 44. Sin probar: otras distribuciones y ARM.
