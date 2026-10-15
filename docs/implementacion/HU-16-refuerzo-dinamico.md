# HU-16 · Vincular dinámicamente actividades complementarias

## Tareas implementadas

1. Detectar componentes con bajo rendimiento: en esta entrega, el componente medido es la actividad de un nodo, a partir de sus respuestas guardadas.
2. Vincular al nodo refuerzo del catálogo oficial MINEDU correspondiente al grado y área de la ruta.
3. **Adicional: evitar duplicados y recomendaciones ajenas al grado/área.** Una clave única por estudiante, nodo y fuente conserva el vínculo incluso con peticiones concurrentes.
4. **Adicional: registrar la realización del refuerzo y conservar su historial.** «Ya lo practiqué en mi cuaderno» persiste por alumno, sin otorgar XP ni sustituir una evaluación.

## Criterio y alcance pedagógico

- Se necesitan al menos dos preguntas respondidas; menos del 70 % de aciertos activa el refuerzo. Exactamente 70 % no lo activa. No responder todavía no equivale a bajo rendimiento.
- Se toma la última respuesta por pregunta entre intentos no abandonados. Se incluyen únicamente respuestas corregidas de opción única o verdadero/falso y preguntas con puntos mayores que cero. No se diagnostican respuestas abiertas ni opción múltiple con el corrector actual.
- El porcentaje se calcula al consultar; no se duplica en la base. El historial no se borra cuando el estudiante mejora.
- No se atribuyen dificultades a una competencia CNEB concreta: las preguntas actuales no están etiquetadas por competencia. Las pistas corresponden a las preguntas del nodo, no a MINEDU.
- El material se elige por grado y área, reutilizando `PrimaryStudyCatalog`: referencias oficiales de Matemática, Comunicación y Ciencia y Tecnología para 1.º–6.º. Un área sin fuente muestra un estado explícito y no recibe un material de otra materia.
- El vínculo abre el **cuadernillo oficial**, no una página o ejercicio específico: el estudiante elige la actividad con su docente. No se afirma una correspondencia automática por tema que el catálogo no contiene. No se inventan ejercicios atribuidos a MINEDU ni se usa una supuesta API pública.
- Los libros externos requieren internet; no forman parte del paquete sin conexión de HU-12. El repositorio MINEDU puede pedir verificación humana. El 14 de octubre de 2026 esa verificación impidió volver a inspeccionar automáticamente las fichas consultadas (10079 y 11800); se mantienen las referencias del catálogo de HU-12, sin afirmar una nueva validación de su contenido.

## Flujo

Al abrir la lección y después de guardar cada respuesta, el panel «Un ratito para practicar» solicita la actualización. Con evidencia insuficiente informa cómo empezar; con resultados bajos presenta las pistas y vincula el material. Con mejora muestra el avance y mantiene lo ya practicado. Carga, error y reintento no bloquean la lección. La celebración también incluye el refuerzo.

El cálculo y el vínculo se ejecutan en el servidor al solicitar la actualización, no mediante un planificador en segundo plano. Un resultado ya guardado se detecta al volver a abrir el nodo.

## API y persistencia

- `POST /api/v1/student/learning-routes/{route}/nodes/{node}/reinforcements/refresh`
- `PUT /api/v1/student/learning-routes/{route}/nodes/{node}/reinforcements/{id}/complete`

La identidad sale de la sesión. Se comprueban cuenta activa, inscripción ACTIVO/COMPLETADO, ruta/actividad publicada y no archivada, pertenencia del nodo a la ruta y progreso accesible. Una ruta ajena, pausada, un nodo bloqueado o un refuerzo ajeno devuelven 404; sin sesión, 401.

Migración **V18** (HU-16): `aprendizaje_refuerzo_nodo`, escrita exclusivamente por Spring Boot. UUID, fechas UTC `DATETIME(6)`, índice único `(estudiante_id,nodo_id,recurso_id)`. La fuente tiene identidad estable derivada de su URL; el título y enlace se resuelven desde el catálogo validado del servidor. Repetir el vínculo o la marca de realizado no reinicia su fecha.

## Actualizar otra copia local

```powershell
git pull --ff-only
.\scripts\Actualizar-HU16.ps1
```

El script aplica V18 de forma idempotente y actualiza permisos del usuario configurado en `.local/env.ps1`; permite `-RootConfig` y `-ApiUser`. Después, detener la API anterior, compilar con `mvn.cmd -f backend/api-spring-boot/pom.xml package` y arrancar el servidor de la forma habitual. No borrar ni recrear la base existente.

## Verificación

- Ocho pruebas unitarias: umbral, ausencia de evidencia, autorización, correspondencia, área sin recurso, mejora e historial, realización propia y rechazo de recursos ajenos.
- Ocho pruebas con MySQL real aislado en puerto 3308: última respuesta, deduplicación con nueva instancia, aislamiento, ruta pausada, nodo bloqueado e inscripción suspendida, nodo de otra ruta, ciclo de mejora y concurrencia. Fixtures transaccionales con rollback; concurrencia limpia únicamente su vínculo creado para la prueba.
- Suite Java general, pruebas del frontend, build y lint.
- Flujo real de API: guardar respuestas, vincular, repetir, marcar dos veces, mejorar, verificar historial; 401 y 404.
- Edge: 375 px sin desborde del panel, escritorio, abrir pistas, marcar con teclado, recargar y conservar el estado, error 503 y recuperación con reintento. En entorno aislado; no se alteraron respuestas del alumno real.

Para ejecutar las pruebas MySQL se requiere una base de prueba ya preparada (esquema, V18, catálogo y estudiante inscrito con un nodo accesible):

```powershell
# Cargar variables de la base aislada (URL debe comenzar con jdbc:mysql://127.0.0.1:3308/rupi?).
$env:RUPI_HU16_TEST_DB = 'isolated'
mvn.cmd -f backend/api-spring-boot/pom.xml '-Dtest=ReinforcementRepositoryIntegrationTest' test
```

## Prueba manual

1. Abrir un nodo con al menos dos preguntas de opción única en una ruta de Matemática, Comunicación o CyT.
2. Responder de modo que los aciertos sean menores al 70 %; revisar el panel de refuerzo.
3. Abrir las pistas y el vínculo MINEDU (se abre otra pestaña).
4. Marcar la práctica como realizada y volver a abrir el nodo: debe mantenerse una sola recomendación realizada.
5. Volver a responder correctamente; al actualizar el nodo debe indicar avance y mantener el historial.
