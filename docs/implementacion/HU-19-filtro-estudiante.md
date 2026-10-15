# HU-19 · Filtrar resultados por estudiante individual

Como profesor, consultar el avance y las dificultades de un estudiante de mi aula para orientar su acompañamiento.

## Uso

Panel docente → Reportes de resultados → seleccionar aula y ruta → buscar y seleccionar **Estudiante**.
La opción inicial es **Todos los estudiantes**. Buscar acorta la lista; el reporte cambia al seleccionar. Los nombres repetidos se distinguen por el usuario escolar, sin mostrar correo, contraseñas ni UUID en la interfaz.

La selección se conserva al alternar Resultados y atención / Calificaciones promedio y al cambiar de ruta. Cambiar de aula la restablece; Ver todos los estudiantes limpia selección y búsqueda. Un alumno sin inscripción permanece seleccionable y se identifica como tal, sin inventarle notas.

## Tareas implementadas

1. **Filtro individual** compartido por HU-17 y HU-18, con búsqueda sin distinción de tildes/mayúsculas, lista de miembros activos y validación en servidor.
2. **Indicadores filtrados**: paradas completas, preguntas respondidas, aciertos, unidades por revisar y promedios disponibles. Se reutilizan los cálculos y límites de HU-17/HU-18; cero y ausencia de información se distinguen.
3. **Adicional: resumen de acompañamiento** con unidades en alerta, promedios, actividades con respuestas y refuerzos HU-16 con estado pendiente/realizado. Enlaces al detalle existente conservan el filtro. Consulta docente de solo lectura; no crea refuerzos, no los completa y no usa endpoints del estudiante.
4. **Adicional: CSV individual** de resultados y de calificaciones, según la vista consultada. Ambas exportaciones respetan aula/ruta/alumno; sin selección continúan siendo grupales. Incluyen aula, ruta/versión, fecha, estudiante y unidad. Conservan UTF-8 BOM, CRLF y protección de fórmulas. Sin resultados exportables: botón deshabilitado y explicación; API devuelve 204 si se solicita directamente.

## Backend

Bajo `/api/v1/teacher/results-reports`:

- `GET /classrooms/{room}/students`: miembros estudiantes activos de un aula autorizada, con ID interno, nombre y usuario escolar.
- `GET ?classroomId=&routeId=&studentId=`: HU-17 acepta `studentId` opcional.
- `GET /grades?...`: HU-18 acepta el mismo filtro opcional.
- `GET /student-overview?classroomId=&routeId=&studentId=`: resumen individual; el alumno es obligatorio.
- `GET /export?...`: CSV de resultados, con filtro opcional.
- `GET /grades/export?...`: CSV de promedios y letras orientativas, con filtro opcional.

La ausencia de `studentId` conserva el reporte grupal. El servidor comprueba sesión (401), rol docente (403), acceso a aula/ruta y pertenencia activa del alumno (404 cuando está fuera del ámbito o retirado). Cuenta desactivada tampoco pertenece al listado activo. Los endpoints mantienen no-store.

La autorización y las consultas están dentro de transacciones de lectura REPEATABLE_READ. Los filtros de alumno se aplican también en SQL: nómina, respuestas, progreso e intentos. Los refuerzos se restringen por alumno, aula activa autorizada, membresía activa, inscripción y nodos de la ruta. No hay consultas por cada alumno ni se almacenan indicadores derivados.

El resumen cuenta preguntas y nodos distintos, evitando sumar dos veces una respuesta si una actividad está vinculada a varias unidades. Las alertas y notas por unidad reutilizan `ResultsReportService.summarize` y `GradesService.summarize`. Los puntajes, reintentos y AD/A/B/C siguen los criterios aprobados en HU-18.

El título y enlace de cada refuerzo provienen del catálogo oficial ya usado por HU-16. Si una referencia histórica ya no aparece en él, se conserva el estado y se indica que está fuera del catálogo; no se inventa una URL. «Realizado» es un registro del estudiante, no una acreditación de competencia.

## Interfaz y concurrencia

Se mantiene el diseño del cuaderno docente: Nunito, verde bosque/hoja, amarillo, botones táctiles, foco visible y controles de al menos 48 px. Tabla y tarjetas móviles de HU-18 se conservan. El resumen ofrece bloques y secciones desplegables sin rankings.

Las peticiones llevan una clave que incluye ruta/alumno y se cancelan al cambiar selección. Los componentes de detalle se reinician con esa clave; no se reutilizan detalles del alumno anterior. Una descarga pendiente también se cancela al salir de la selección. Los errores ofrecen reintento y los datos anteriores se ocultan mientras se carga el nuevo ámbito.

## Integración con el equipo

Se incorporó HU-18 (`a65b48d`) y se integraron los commits remotos `5fcb499` y `0f00205` sobre unidades y cabecera del roadmap. La integración no tuvo conflictos.
El último cambio del roadmap conservaba un cálculo `unitBands` y su importación sin uso: se retiraron porque bloqueaban TypeScript y ESLint. No se modificó el nuevo diseño del roadmap.
No se requieren migraciones para HU-19. La base debe tener las migraciones previas, especialmente V18 para refuerzos.

## Pruebas y entrega

- Backend: **179 pruebas ejecutadas, todas aprobadas**, de 196 detectadas. Las 17 omitidas corresponden a otros grupos de integración no activados para esta ejecución.
- Incluye **20 pruebas con MySQL real aislado 3308**: 8 de HU-17, 6 de HU-18 y 6 nuevas de HU-19. Los datos transaccionales se revierten.
- HU-19 añade 7 pruebas unitarias de autorización, resumen sin duplicación, ausencia de inscripción, refuerzos históricos y CSV; además 3 pruebas de búsqueda/homónimos/selección en frontend.
- Frontend: **27 pruebas aprobadas**, lint y build correctos. Backend empaquetado correctamente.
- Edge con API/MySQL reales aislados: 401/403/404/204, alumnos activos y retirados, cero frente a falta de inscripción, resumen/refuerzos, exportación individual, nombres repetidos, permanencia del filtro entre vistas/rutas, reinicio al cambiar aula, recuperación ante 503 y respuesta tardía inyectada tras cambiar alumno.
- Vista comprobada a 1280, 768 y 375 px sin desbordes; capturas de revisión en `.local/hu19-*.png` (no versionadas).
- Fixtures de navegador limitadas a la base aislada 3308 y retiradas al finalizar. La base habitual no recibe notas ni alumnos ficticios.

Comandos habituales: `npm.cmd run build`, `npm.cmd run lint`, `npm.cmd test --workspace @rupi/web-react` y `mvn.cmd -f backend/api-spring-boot/pom.xml test`.
Para pruebas MySQL definir las variables de una instancia descartable RUPI_DB_* con URL `jdbc:mysql://127.0.0.1:3308/rupi?...` y RUPI_HU17_TEST_DB/RUPI_HU18_TEST_DB/RUPI_HU19_TEST_DB=isolated. Nunca ejecutar esas fixtures contra la base compartida.

## Límites conservados

Los reportes reflejan los registros disponibles al consultar; no incorporan actualización en vivo. El filtro por periodo continúa pendiente de una relación académica fiable. Las letras son orientativas internas, no calificaciones oficiales de competencias. Se conservan las limitaciones históricas de HU-18 sobre respuestas sobrescritas por práctica o eliminadas por el editor anterior; HU-19 no realiza esas escrituras.
