# HU-15 · Nivel de asistencia del tutor IA por aula

Implementación: 11 de noviembre de 2026.

> Como docente web, yo debería configurar el nivel de asistencia del tutor IA (mediante pistas guiadas o explicación directa) para mi grupo de clase a fin de adaptar el soporte automatizado al grado pedagógico del aula.

## Criterios de aceptación (definidos en la implementación)
- El docente elige, por cada aula a su cargo, entre **pistas guiadas** y **explicación directa**.
- Un aula sin configurar usa pistas guiadas: es el comportamiento que el tutor ya tenía (método socrático).
- El cambio se aplica en la siguiente pregunta de los estudiantes del aula, sin recargar ni reiniciar servicios.
- El estudiante ve en el chat cómo lo ayudará Rupi y que lo eligió su docente.
- Solo el docente responsable o con membresía activa de DOCENTE puede consultar o cambiar el aula.

## Dónde está
- **Docente:** Reportes de resultados (Cuaderno docente) → elegir aula → tarjeta «Ayuda del tutor Rupi». Muestra un ejemplo de cada modo y la última actualización en hora de Perú.
- **Estudiante:** al abrir «Pregúntale a Rupi» aparece una franja bajo la cabecera: verde con pistas guiadas, maíz con explicación directa.

## Datos · `V44__nivel_tutor_ia_aula_hu15.sql`
Tabla `escuela_aula_tutor_ia` (una fila por aula): `modo_asistencia` (`PISTAS_GUIADAS` | `EXPLICACION_DIRECTA`), `actualizado_por` y `actualizado_en` en UTC. Escritor único: Spring Boot. La ausencia de fila equivale a pistas guiadas. Después de la migración hay que volver a ejecutar `grants-spring-boot.sql`.

## API (Spring Boot)
| Método y ruta | Uso |
|---|---|
| `GET /api/v1/teacher/tutor-support/classrooms/{aula}` | Modo actual, si fue configurado, fecha y autor. 404 si el aula no es del docente. |
| `PUT /api/v1/teacher/tutor-support/classrooms/{aula}` | `{ "mode": "EXPLICACION_DIRECTA" }`. Exige Origin permitido; 400 con un modo desconocido. |
| `GET /api/v1/student/tutor-support?routeId={versión}` | Modo del aula activa del estudiante con el grado de la ruta. Sin aula: `PISTAS_GUIADAS` con `source: PREDETERMINADO`. |

Si el estudiante tiene varias aulas activas del mismo grado y alguna pide pistas, se usan pistas: ante la duda se conserva la ayuda que menos resuelve por el niño. En la vista previa, el docente ve el modo de sus propias aulas del grado.

## Servicio de IA
`ChatContext.assistanceMode` acepta solo los dos modos (422 con otro valor) y llega al prompt como «MODO DE ASISTENCIA DEL AULA». La regla 4 de `SYSTEM_RUPI_TUTOR` describe ambos modos:
- **Pistas guiadas:** una pista con un ejemplo cotidiano y una pregunta corta; nunca la respuesta final.
- **Explicación directa:** pasos numerados, «**Resultado:**» y un ejercicio parecido para practicar.

La web pide el modo a Spring al abrir el chat y lo reenvía en cada pregunta. Si la consulta falla, se usa pistas guiadas.

## Límites conocidos
- El servicio de IA todavía no valida la sesión (pendiente general): el modo viaja desde la web, igual que el resto del contexto. Cambiarlo a mano solo altera el estilo de la explicación del propio estudiante.
- Las pistas de «¿Por qué me equivoqué?» (HU-37) no cambian con este ajuste: ya explican el error de una respuesta enviada.

## Verificación
- `TutorSupportServiceTest` (7 pruebas): valor predeterminado, normalización, modo inválido, aula ajena, estudiante sin aula, varias aulas y modo del aula.
- `test_servicio_ia.ModoDeAsistenciaTest` (4 pruebas): modo en el prompt, valor predeterminado y rechazo de modos inventados.
- `scripts/verify_hu15_pantalla.mjs` (Playwright, 1280 y 375 px): elegir con teclado, guardar, persistencia, botón de 48 px, sin desborde y franja visible para el estudiante del aula. Deja el aula en pistas guiadas.
- Con curl: 403 sin Origin, 403 de estudiante en la ruta docente y 404 con un aula ajena.
