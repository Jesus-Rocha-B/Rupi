# HU-18 · Ver una tabla con las calificaciones promedio

Como profesor, consultar los promedios por unidad de mis estudiantes para orientar su acompañamiento.

## Entrega y acceso

Panel docente → **Reportes de resultados** → **Calificaciones promedio** → seleccionar aula y ruta/version.
Se conserva HU-17 (resultados, atención docente y CSV) como vista independiente con los mismos filtros.
La HU-18 no necesita migraciones ni modifica respuestas históricas.
Rama: `codex/hu18-calificaciones-promedio`, basada en `0c355f4` de main (incluye la corrección de rutas/videos del equipo).

## Cuatro tareas

1. **Calcular promedios por unidad:** puntajes registrados y atribuibles a una ruta, con ponderación por pregunta y promedio simple entre actividades.
2. **Tabla legible y adaptativa:** estudiantes en filas y unidades en columnas, encabezados/nombres fijos, desplazamiento contenido en tablet, tarjetas desplegables en celular.
3. **Adicional: filtros y ordenamiento:** aula/ruta autorizadas; nombre o promedio de unidad, ascendente/descendente; sin calificaciones al final al ordenar por promedio. Ordenar conserva aula/ruta.
4. **Adicional: detalle de promedio:** cada celda con calificación abre un diálogo con actividades consideradas, puntos obtenidos/posibles, nota normalizada, fecha de última respuesta y cantidad incluida.

## Criterio aprobado por el equipo

- Nota por actividad = suma de puntos obtenidos / suma de puntos posibles × 20.
- Promedio por unidad = media simple de las notas de sus actividades válidas; todas pesan igual, independientemente de su cantidad de preguntas/puntos máximos.
- Preguntas de una actividad pesan según `evaluacion_pregunta.puntos`. Solo preguntas puntuables (`puntos > 0`) de opción única o verdadero/falso; una actividad con preguntas puntuables de otros tipos no se califica automáticamente aquí.
- AD: 18 ≤ nota ≤ 20; A: 14 ≤ nota < 18; B: 11 ≤ nota < 14; C: 0 ≤ nota < 11.
- Letra **orientativa interna de Rupi**, no conversión oficial del MINEDU ni nivel de logro acreditado de una competencia. Los niveles oficiales dependen de valoración docente de evidencias.
- Sin calificaciones se representa con `—` y texto, nunca cero. Un puntaje real cero sí se muestra como `0.0 / 20 · C`.
- Se calcula con fracciones exactas para que ni el promedio ni la clasificación dependan del redondeo intermedio. Solo se presenta un decimal (HALF_UP); por ejemplo 17.999 se ve 18.0 pero conserva A. La interfaz lo explica. El campo auxiliar de orden no se almacena en BD.
- La cantidad incluida/total deja claro que puede ser un promedio parcial. El total cuenta actividades distintas de la unidad, incluso aquellas que todavía no tienen preguntas calificables.

Referencias sobre evaluación por competencias y escala literal:
- [MINEDU: significado de AD/A/B/C](https://www.gob.pe/institucion/minedu/noticias/880490-colegios-utilizaran-este-ano-escala-literal-para-calificar-a-estudiantes).
- [RVM 048-2024-MINEDU](https://www.gob.pe/institucion/minedu/normas-legales/5518274-048-2-24-minedu).
Los intervalos numéricos anteriores fueron acordados para Rupi; no se atribuyen a estas normas.

## Intentos, datos históricos y límites

Se elige el **último intento completo válido** por alumno y versión de actividad, ordenado por última respuesta, fecha de inicio e ID como desempate. Un intento parcial posterior no reemplaza uno completo previo. No se mezclan respuestas de distintos intentos.

El flujo heredado `EvaluationService` deja los intentos EN_CURSO aun con todas sus preguntas respondidas. Por compatibilidad, HU-18 verifica completitud a partir de las respuestas calificadas, tanto en EN_CURSO como COMPLETADO; ABANDONADO queda excluido. Se requieren todas las preguntas puntuables con `es_correcta` y `puntaje_obtenido` no nulos, y cada puntaje dentro de 0..puntos posibles. No se cambia el estado del intento ni el flujo de práctica.

Los reintentos dentro del mismo registro que ya fueron sobrescritos por el flujo heredado no se pueden reconstruir. Asimismo, el editor docente actual puede eliminar respuestas al reemplazar preguntas (riesgo previo documentado en el registro del equipo). HU-18 no ejecuta esas escrituras ni puede recuperar evidencia ya eliminada. Una mejora futura requiere versionar preguntas y conservar intentos de práctica completos.

Una actividad repetida en varios nodos de la misma unidad se cuenta una sola vez. Si está vinculada explícitamente a distintas unidades se incluye una vez en cada una. Nodos sin unidad coherente en grado/área/catálogo se excluyen y se muestra su cantidad.

Se respetan intentos con inscripción correspondiente a la ruta. Intentos antiguos sin inscripción solo se admiten si la actividad no se comparte entre otras rutas, para evitar atribuciones ambiguas. Se conservan resultados históricos de inscripciones pausadas/completadas, como HU-17.

No se incluye filtro por periodo: tener fechas en respuestas y una tabla de periodos no demuestra una relación académica válida entre ambos. Tampoco se generan notas a partir del porcentaje de aciertos de HU-17, ni se agregan datos de demostración a la base habitual.

## Arquitectura y seguridad

- `GradesRepository`: consulta de puntajes, completitud, pertenencia y fechas; una fila por intento, sin multiplicación por nodos.
- `GradeCalculator`: función pura de normalización/media/clasificación.
- `GradesService`: reutiliza autorización de HU-17, agrupa estudiantes/unidades y genera promedio/detalle desde la misma evidencia. Transacción de lectura REPEATABLE_READ.
- `GET /api/v1/teacher/results-reports/grades?classroomId=UUID&routeId=UUID`.
- Sesión obligatoria (401), rol DOCENTE (403), aulas/rutas ajenas (404); no-store heredado de la API.
- React reutiliza filtros, cliente de reportes y hook de lectura con AbortController y clave de petición para descartar respuestas tardías.
- Tabla semántica con aria-sort, controles de 48 px, foco visible, diálogo nativo con Escape y devolución de foco; colores y tipografía del panel actual.

## Verificación realizada

- Backend completo: 152 pruebas ejecutadas correctamente; 25 de integración anteriores omitidas por depender de instancias aisladas.
- HU-18: 9 pruebas unitarias (vacío/cero, límites exactos, ponderación, rango, reintentos, aislamiento, permisos).
- HU-18: 6 pruebas con MySQL real aislado 3308, todas aprobadas tras corregir la fixture de retiro para incluir `retiro_en`. Fixtures transaccionales con rollback: completitud, puntos, reintentos, tipos no soportados, datos inválidos, abandonos, miembros retirados, ámbito de inscripción y nodos duplicados.
- Frontend: 24 pruebas aprobadas (3 nuevas de ordenamiento), lint y build correctos.
- Edge con API y MySQL aislados: nota 16/A y detalle, 401/403/404, no-store, teclado/retorno de foco, cambios de filtros, orden, error 503/reintento. Revisado en 1280, 768 y 375 px sin desborde de página. Capturas en `.local/hu18-*.png` (no versionadas).
- La fixture de navegador se creó únicamente en la base aislada 3308 y se retiró al terminar; no se alteró la base local habitual 3307.

Para ejecutar las pruebas SQL: preparar una base de pruebas descartable con esquema/semillas de rutas y alumno demo, un aula con docente y membresía activa. Definir RUPI_HU18_TEST_DB=isolated y las variables RUPI_DB_*; la URL debe comenzar con `jdbc:mysql://127.0.0.1:3308/rupi?`. Ejecutar `mvn.cmd -f backend/api-spring-boot/pom.xml -Dtest=GradesRepositoryIntegrationTest test`. Nunca apuntar las pruebas a una base compartida.
