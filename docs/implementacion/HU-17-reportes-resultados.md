# HU-17 · Consultar reportes de resultados

**Historia:** Como profesor, consultar reportes de resultados de mis estudiantes para identificar dificultades.

## Tareas completadas

1. **Resumir por estudiante y unidad:** paradas completadas/total, preguntas respondidas/disponibles, aciertos, porcentaje y estado de seguimiento.
2. **Destacar competencias que requieren revisión docente:** se muestran las competencias curriculares válidas de las unidades con estudiantes que requieren atención. La interfaz y el CSV explican que es evidencia indirecta de la unidad, no una nota por competencia.
3. **Tarea adicional: filtrar por aula y ruta/versionado.** Solo aulas activas del docente y rutas con inscripciones de sus estudiantes del grado correspondiente. Cambiar filtros cancela las consultas anteriores y retira el reporte previo para no mezclar datos.
4. **Tarea adicional: descargar CSV.** Exportación desde el servidor, con los mismos controles de acceso, todas las unidades, fecha UTC, UTF-8 con BOM y protección contra fórmulas en nombres/títulos. El profesor puede abrirlo en una hoja de cálculo o importarlo con separador coma.

## Uso

1. Iniciar sesión como docente y pulsar **Reportes de resultados** en el panel de autoría.
2. Seleccionar aula y ruta. Se distingue la versión y si está publicada.
3. Abrir cada unidad para revisar el detalle por estudiante y las competencias señaladas.
4. Usar **Actualizar reporte** para consultar nuevos resultados, o **Descargar CSV**.
5. Volver a las rutas con el botón de navegación. Las tablas admiten desplazamiento horizontal y teclado en móvil.

Si el docente no tiene aulas activas, se explica cómo obtener acceso. Un aula sin estudiantes inscritos en rutas de su grado muestra un estado vacío, sin inventar alumnos ni resultados. Las asignaciones institucionales existentes no se cambian al consultar reportes.

## Criterios de cálculo

- Rendimiento = aciertos / preguntas respondidas corregidas; no es porcentaje de paradas completadas ni una nota oficial del MINEDU.
- Se usa la respuesta más reciente por estudiante y pregunta, con orden por fecha de respuesta, fecha de intento e identificador. Los reintentos no suman múltiples aciertos de la misma pregunta.
- Se consideran opción única y verdadero/falso, con puntos mayores a cero y corrección disponible. Se excluyen intentos abandonados, respuestas sin corrección y tipos aún no evaluables de forma fiable por el corrector actual.
- **Requiere atención:** al menos 2 preguntas respondidas y menos del 70 % de aciertos. Exactamente 70 % es «En avance». Se decide con la proporción sin redondear; el porcentaje visible tiene un decimal.
- Cero respuestas = **Sin respuestas**, porcentaje vacío. Una respuesta = **Falta evidencia**. El alumno activo del aula sin inscripción a esa ruta aparece como **Sin inscripción**, nunca como rendimiento cero.
- Se muestran también inscripciones suspendidas/completadas, identificando su estado, para conservar la lectura histórica. Una ruta pausada o archivada con inscripciones puede seguir consultándose. El listado de alumnos es el de miembros ESTUDIANTE activos con cuenta activa en la actualidad; no reconstruye matrículas históricas de alumnos retirados.
- Cada pregunta se cuenta una sola vez dentro de una unidad aunque se reutilice su actividad en varios nodos. El progreso sí cuenta nodos distintos.
- Las preguntas disponibles son las configuradas en la base. El reporte no crea preguntas ni modifica progreso. Si todavía no hay preguntas evaluables configuradas, lo dice expresamente.

## Competencias y límites de la evidencia

Las preguntas actuales no tienen una relación directa con competencias. Se consulta `curriculo_unidad_competencia` únicamente para unidades cuyo catálogo, grado y área coincidan con la ruta, y competencias del área de la ruta. Se resaltan como **competencias que conviene revisar** si la unidad tiene resultados bajos; no se afirma que cada competencia esté desaprobada.

Los nodos sin unidad o con una asignación curricular incoherente se agrupan en **Sin unidad curricular válida**. No se inventa una competencia ni se mezclan competencias de otros grados/áreas.

Los intentos con `inscripcion_id` se atribuyen a esa inscripción. Por compatibilidad, un intento antiguo sin inscripción solo se usa si su versión de actividad pertenece exclusivamente a esa ruta. Si la misma actividad está en varias rutas, las respuestas antiguas sin inscripción se excluyen por ambigüedad. El reporte indica que solo utiliza preguntas atribuibles a la ruta; para disponer de medición directa por competencia y contexto completo de cada intento harán falta sus vínculos en el modelo de evaluación.

## Seguridad y API

Endpoints de solo lectura:

- `GET /api/v1/teacher/results-reports/classrooms`
- `GET /api/v1/teacher/results-reports/classrooms/{room}/routes`
- `GET /api/v1/teacher/results-reports?classroomId={room}&routeId={route}`
- `GET /api/v1/teacher/results-reports/export?classroomId={room}&routeId={route}`

La sesión debe ser de un DOCENTE (401 sin sesión, 403 para alumno). El docente debe ser responsable o miembro DOCENTE activo del aula activa; conocer un UUID no otorga acceso. Aula ajena o ruta no asociada devuelve 404 antes de leer resultados. Reporte y CSV comparten la misma autorización.

Las consultas usan transacción de solo lectura con `REPEATABLE_READ`; la fecha de generación se incluye en cada reporte y fila del CSV. Los porcentajes se calculan, no se almacenan. Los resultados se entregan con las cabeceras `no-store` existentes de la API y el CSV con `Content-Disposition: attachment`.

**No requiere una migración nueva:** lee el esquema y las relaciones existentes, respetando el escritor único. No modifica datos curriculares, respuestas ni matrículas.

## Actualización local

```powershell
git pull --ff-only
npm.cmd run build
```

Detener la instancia anterior de la API y compilar `mvn.cmd -f backend/api-spring-boot/pom.xml package`; iniciar los servicios con el procedimiento habitual. La base debe estar al día con las historias anteriores. No borrar ni recrear la base para HU-17.

## Verificación realizada

- 10 pruebas unitarias de resumen: evidencia mínima, umbral exacto, separación entre avance/aciertos, no duplicación por actividad, separación de unidades/alumnos, ausencia de inscripción/unidad, autorización y CSV (comillas, saltos de línea, fórmulas, porcentaje vacío).
- 8 pruebas MySQL reales en una base aislada de puerto 3308: aulas autorizadas y rutas asociadas, separación entre aulas, alumnos retirados, última respuesta, intentos ambiguos frente a inscritos, currículo incoherente, historial de rutas pausadas y miembros sin inscripción. Los cambios de fixture se revierten con transacciones.
- Suite Java: 168 pruebas descubiertas, 151 ejecutadas correctamente, 17 omitidas de integraciones HU-14/HU-16 que requieren su activación propia. Las 8 integraciones HU-17 están incluidas entre las ejecutadas.
- Frontend: 21 pruebas, build y lint.
- HTTP real: reporte y CSV con igual número de filas, BOM, MIME, descarga y `no-store`; 401, 403 y 404.
- Edge real: acceso docente por teclado, alertas con competencias, filtros sin mezcla, aula sin inscripciones, descarga CSV, recuperación de 503, navegación de retorno y pantalla móvil de 375 px sin desborde de página. Tabla desplazable por teclado. Fecha de Perú probada con navegador configurado en Tokio.

Para repetir la integración se requiere una base aislada con el esquema/catálogo y un docente responsable de un aula con alumno inscrito (fixtures de prueba):

```powershell
# RUPI_DB_URL debe comenzar con jdbc:mysql://127.0.0.1:3308/rupi?.
$env:RUPI_HU17_TEST_DB = 'isolated'
mvn.cmd -f backend/api-spring-boot/pom.xml '-Dtest=ResultsReportRepositoryIntegrationTest' test
```

Las capturas y archivos temporales de validación se guardaron en `.local`, fuera del commit. No se introdujeron resultados de prueba en la base local del usuario.
