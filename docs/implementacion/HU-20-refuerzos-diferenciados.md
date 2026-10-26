# HU-20 · Asignar tareas de refuerzo diferenciadas

Implementación local: 26 de octubre de 2026. Base de integración: `4fa9edc`.

## Alcance y recorrido
Como profesor, asignar prácticas según las dificultades de cada estudiante y consultar su seguimiento.

1. Identificar estudiantes que requieren refuerzo.
2. Asignar actividades distintas según la necesidad detectada.
3. Consultar el estado y los resultados declarados de los refuerzos asignados.

**Docente:** Reportes de resultados → aula y ruta → «Refuerzos para cada estudiante». El filtro individual de HU-19 también filtra este panel. Seleccionar parada, material MINEDU y estudiantes; escribir título, ejercicios/páginas concretas y motivo; confirmar. Para otra necesidad, preparar otra tarea con instrucciones diferentes. El seguimiento muestra evidencia inicial, fechas, estado y reflexión.
**Alumno:** «Mis tareas de refuerzo» en el área principal → comenzar → abrir material → practicar → escribir una reflexión → marcar como terminado. Solo aparecen asignaciones propias con membresía/inscripción vigente y parada accesible.

Se reutiliza el criterio de HU-16 (mínimo dos respuestas, menos del 70 % de aciertos) y la evidencia de los reportes HU-17/HU-19, que atribuyen respuestas a la ruta. La evidencia insuficiente y la falta de inscripción no se convierten en cero. El material pertenece al catálogo existente, según grado y área; su pertinencia concreta para el nodo la confirma el docente al definir ejercicios. No se inventaron enlaces ni se añadió una API MINEDU.

Las actividades asignadas en esta entrega son prácticas con los materiales externos MINEDU disponibles. No incluyen evaluaciones automáticas nuevas: el resultado es la reflexión declarada por el alumno, no una calificación. No se compara con un examen ni se modifica el promedio HU-18. Abrir el enlace no completa la tarea. El registro automático de HU-16 se conserva separado de la asignación explícita del docente.

## Implementación
- `AssignmentService`: permisos de aula, evidencia, catálogo, validación del lote y seguimiento.
- `AssignmentRepository`: consultas y persistencia mediante JDBC.
- `AssignmentController`: endpoints docentes y personales protegidos por el filtro de sesiones existente y comprobación de Origin en escrituras.
- `AssignedTasks.tsx`: formularios docentes y tareas del alumno, integrados en App y TeacherReportsView.
- `V22__refuerzos_diferenciados_hu20.sql`: una nueva tabla del dominio aprendizaje; no modifica migraciones previas.

Endpoints:
- GET/POST `/api/v1/teacher/reinforcement-assignments?classroomId=...&routeId=...`
- GET `/api/v1/student/reinforcement-assignments`
- PUT `/api/v1/student/reinforcement-assignments/{id}/start`
- PUT `/api/v1/student/reinforcement-assignments/{id}/complete`, con `reflection`.

La asignación admite entre 1 y 50 alumnos por petición y es transaccional. Una clave única evita asignaciones activas duplicadas del mismo alumno/nodo/recurso incluso con solicitudes simultáneas. Otra clave, solicitud/alumno, conserva la idempotencia después de completar una tarea. Un segundo recurso o una nueva asignación tras completar la anterior conserva el historial. Las fechas se guardan en UTC; la interfaz muestra hora de Perú. Se guardan cantidades de respuestas/aciertos iniciales, no porcentajes derivados.

## TiDB compartido y activación pendiente
La conexión privada existente a TiDB se conserva. Se verificaron por SELECT diez combinaciones reales de aula/ruta, incluyendo permisos, evidencia y catálogo. La tabla nueva no está presente: `available=false`. No se hicieron escrituras, pruebas de login ni cambios de esquema en TiDB.

**El prompt del usuario excluye expresamente aplicar el esquema compartido y publicar en main.** La migración quedó preparada, no aplicada. El panel docente explica la falta de habilitación y desactiva las asignaciones; el alumno conserva su experiencia habitual.

Para activar, después de autorización:
1. El administrador revisa V22 y la aplica una vez sobre la base compartida rupi, mediante su canal seguro habitual.
2. Comprobar permisos SELECT/INSERT/UPDATE del usuario de aplicación sobre la nueva tabla.
3. Actualizar los backends a este código. El frontend ya detecta la disponibilidad al recargar, sin una bandera adicional.
4. Validar una asignación con cuentas de prueba acordadas por el equipo, sin reutilizar simultáneamente la misma sesión.

La migración utiliza columna generada almacenada e índice único. Referencias oficiales: [columnas generadas de TiDB](https://docs.pingcap.com/tidb/v7.5/generated-columns/), [esquema e índices](https://docs.pingcap.com/tidbcloud/database-schema-concepts/). Su ejecución se probó en MySQL local; la ejecución real del DDL en TiDB queda pendiente de autorización.

## Verificación
- Backend: 219 pruebas descubiertas; 181 ejecutadas sin fallos, 38 omitidas (37 integraciones anteriores y una comprobación compartida optativa).
- Prueba compartida SELECT-only ejecutada por separado: aprobada, diez paneles reales; no modifica datos.
- 14 pruebas nuevas de servicio: falta de evidencia, umbral, permisos, asignación distinta por estudiante, catálogo, duplicados, estados, reflexión y falta de migración.
- Cuatro pruebas SQL locales: concurrencia real, recursos diferentes, aislamiento, historial, repetición tras completar y retirada de acceso. Ejecutan la migración real dos veces en `rupi_hu20_test`, nunca en rupi ni TiDB.
- Tres pruebas de acceso: 401 sin sesión, 403 a alumno en endpoint docente y rechazo de origen externo.
- Frontend: 27 pruebas existentes, build y lint correctos.
- Navegador con API simulada: asignación grupal, tareas personales, comienzo/reflexión/finalización; capturas a 1280 y 375 px sin desbordamiento horizontal ni errores JavaScript. No es una prueba E2E de escritura sobre TiDB.
- Empaquetado final correcto tras detener únicamente la API local que mantenía bloqueado el JAR; API reiniciada con TiDB. Se mantiene desactivado el scheduler local de recordatorios.
- Aviso preexistente de bundle superior a 500 kB, sin impedir build.

Pruebas normales:
```powershell
mvn.cmd -B -f backend/api-spring-boot/pom.xml test
npm.cmd run test --workspace @rupi/web-react
npm.cmd run build
npm.cmd run lint
```

Integración local optativa: `RUPI_HU20_TEST_DB=isolated`, `RUPI_HU20_TEST_URL=jdbc:mysql://127.0.0.1:3307/rupi_hu20_test?serverTimezone=UTC`, usuario/clave de prueba en `RUPI_HU20_TEST_USER` y `RUPI_HU20_TEST_PASSWORD`. El test rechaza otras bases/servidores. Esa base debe existir y estar reservada exclusivamente para pruebas.
Comprobación compartida de lectura: `RUPI_HU20_SHARED_READ=true`, configuración privada RUPI_DB_* y `-Dtest=AssignmentSharedReadTest`. No ejecuta login, DDL ni DML.

## Límites y revisión pendiente
- El catálogo actual tiene pocos recursos por grado/área; la diferenciación se concreta mediante parada e instrucciones del docente.
- No se afirma mejora de aprendizaje solo por completar un cuadernillo.
- Historial de asignaciones conservado; miembros retirados dejan de aparecer en las consultas del aula/alumno.
- Los materiales oficiales externos necesitan internet. Esta historia no añade sincronización offline.
- No se ejecutó la migración compartida ni se publicó en GitHub.
