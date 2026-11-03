# Banco compartido de repaso

La migración `database/mysql/migrations/V37__banco_repaso_cursos_primaria.sql` agrega 84 ejercicios originales (no atribuidos al MINEDU ni generados por un proveedor de IA). Son recursos de práctica complementaria, separados de las evaluaciones calificadas: 36 de Matemática, 24 de Comunicación y 24 de Ciencia y Tecnología, distribuidos entre primero y sexto grado.

Spring Boot lee `evaluacion_banco_repaso`. Cada ejercicio tiene tema, grado, curso, tres opciones, respuesta correcta y explicación. La migración es aditiva e idempotente y no cambia progreso, notas ni preguntas existentes.

## Contrato de repaso

`GET /api/v1/student/learning-routes/{routeId}/nodes/{nodeId}/review` conserva `{questions, feedback}`. Requiere sesión del estudiante, inscripción y parada completada y publicada/disponible. Devuelve el banco activo del curso y grado de la ruta, sin iniciar actividades ni sumar XP. No devuelve preguntas de la evaluación original. Un banco vacío devuelve listas vacías, sin sustituirlo por otro grado o curso.

La web ofrece una pregunta de selección y otra distinta de corrección. “Probar otros ejercicios” recorre pares del banco y vuelve al comienzo al agotarlos. La posición se conserva durante la sesión del navegador por estudiante, grado y curso. No se promete contenido infinito ni se guardan notas de estas prácticas.

Las nuevas rutas de esos cursos y grados reutilizan automáticamente el banco. Para ver esta interfaz, los compañeros necesitan también esta versión del código; compartir la base de datos no actualiza sus aplicaciones locales.
