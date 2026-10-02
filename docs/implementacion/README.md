# Plan de implementación · primeras cuatro historias

## Propósito y alcance

Este plan organiza la primera entrega funcional de RUPI para estudiantes web. Implementaremos cuatro historias en orden, partiendo de la ruta y el mapa que ya aparecen en el prototipo.

El modelo MySQL contempla las 60 historias del backlog general; esta entrega cubre solo HU-01 a HU-04. Kotlin y el administrador Django tienen carpetas propias y se incorporarán cuando las historias requieran funciones móviles o administrativas.

## Arquitectura de estas historias

```text
frontend/web-react → backend/api-spring-boot → database/mysql (MySQL `rupi`)
```

- React presenta rutas, mapa, actividad y estados de carga/error.
- Spring Boot valida la sesión, aplica permisos, consulta/actualiza el progreso y verifica la coherencia curricular.
- MySQL conserva catálogo, ruta publicada, inscripción, último nodo y progreso por nodo.
- Django es propietario del catálogo curricular oficial, pero su panel no es requisito de estas cuatro historias. Las rutas y el progreso son propiedad de Spring Boot.
- Kotlin no participa porque estas historias están planteadas para estudiante web.

React no recibe `estudianteId` desde el navegador ni accede a MySQL directamente. Spring Boot determina la identidad desde el contexto autenticado y solo entrega datos de inscripciones autorizadas.

## Secuencia y estado actual

| Orden | HU | Resultado | Dependencia | Estado |
|---|---|---|---|---|
| 1 | HU-01 · Mapa interactivo | Rutas inscritas y avance real representados en el mapa. | Identidad estudiante, MySQL con datos iniciales y conexión React/API. | Completada. |
| 2 | HU-02 · Hacer clic en un nivel | Iniciar la actividad correcta y marcarla en curso. | HU-01 y contenido publicado. | Pendiente. |
| 3 | HU-03 · Regresar al punto exacto | Restaurar mapa/actividad donde se quedó el estudiante. | HU-02 guarda visita y avance en el servidor. | Pendiente. |
| 4 | HU-04 · Ruta según unidades MINEDU | Mostrar ruta ordenada y agrupada por unidades curriculares. | Catálogo versionado y nodos vinculados a sus unidades. | Pendiente. |

La historia HU-03 depende de que HU-02 registre la visita. HU-04 depende de datos oficiales/versionados; no se cumple con texto decorativo ni ordenado solo en React.

## HU-01 · Mapa interactivo

### Historia

> Como estudiante web, visualizar la ruta de aprendizaje como mapa interactivo para dar a conocer mi avance escolar visualmente.

### Alcance

- Listar rutas publicadas donde el estudiante tiene una inscripción activa o completada.
- Mostrar título, grado, área, paradas, orden, actividad, duración estimada, ubicación del nodo y progreso individual.
- Calcular avance en la consulta/presentación desde los nodos completados, sin guardar un total o porcentaje duplicado.
- Mostrar estados de carga, error/reintento, falta de sesión y ausencia de rutas.
- Mantener la ambientación visual peruana ya planteada y alimentar su contenido desde la API.

### Criterios de aceptación

1. Solo aparecen rutas para las que el estudiante autenticado tiene inscripción válida.
2. La versión de ruta está publicada y su inscripción está activa o completada.
3. Los nodos salen en `numero_secuencia` y muestran el estado guardado para esa inscripción.
4. El total y completados coinciden con los nodos de esa versión de ruta.
5. El porcentaje es derivado y no una fuente persistida independiente.
6. Sin rutas, React presenta un estado vacío y no inventa una ruta ni avance de ejemplo.
7. El mapa se adapta a escritorio/móvil y diferencia estados con texto/icono además de color.

### Trabajo técnico

- **Spring Boot:** `GET /api/v1/student/learning-routes` y `GET /api/v1/student/learning-routes/{versionRouteId}`; filtrar por estudiante autenticado, inscripción activa/completada y versión publicada; devolver grado, área, nodos y progreso.
- **React:** crear cliente tipado; sustituir el origen estático `lessons.ts` de la vista; manejar loading/error/empty/unauthorized y calcular el avance desde la respuesta.
- **MySQL:** aplicar el modelo; preparar datos de desarrollo curriculares y una ruta, actividades, diez nodos, inscripción y estados de progreso.
- **Django/Kotlin:** sin cambios en esta historia.

### Estado actual

Existe la interfaz del mapa y el código fuente inicial de las consultas/endpoints. Todavía no se ha compilado ni ejecutado contra MySQL. React sigue mostrando datos estáticos, y faltan principal autenticado, instancia y fixture real.

## HU-02 · Hacer clic en un nivel

### Historia

> Como estudiante web, hacer clic en un nivel del roadmap para iniciar la lección de ese día.

### Alcance

- Permitir iniciar nodos disponibles o en curso; un nodo bloqueado no abre contenido.
- Abrir la versión de actividad vinculada a ese nodo y sus bloques ordenados.
- En el primer inicio, cambiar progreso a `EN_CURSO`, guardar `primer_acceso_en` y actualizar `ultimo_nodo_visitado_id` dentro de una transacción.
- Reabrir una actividad en curso sin reiniciar o duplicar su avance.
- Aplicar autorización por estudiante, inscripción, ruta y nodo.

### Criterios de aceptación

1. Al tocar un nodo disponible, se abre su actividad correcta de la versión publicada.
2. Un nodo bloqueado no entrega contenido ni modifica progreso.
3. El primer inicio persiste estado `EN_CURSO`, primer acceso y último nodo visitado.
4. Si el estudiante vuelve a abrir una actividad en curso, conserva sus datos.
5. Alterar el UUID en la URL no permite entrar a la ruta de otro estudiante.
6. Contenido retirado/no publicado genera una respuesta controlada y una opción de retorno al mapa.

### Trabajo técnico

- **Spring Boot:** leer `aprendizaje_version_actividad` y `aprendizaje_bloque_contenido`; añadir operación transaccional idempotente para inicio; verificar permisos y estado.
- **React:** habilitar solo nodos apropiados; abrir la lección asociada; tratar sesión vencida y fallos de red.
- **MySQL:** reutilizar `aprendizaje_nodo_ruta`, `aprendizaje_version_actividad`, `aprendizaje_bloque_contenido`, `aprendizaje_progreso_nodo` y `aprendizaje_inscripcion_ruta`. No añadir una tabla sin necesidad demostrada.
- **Django/Kotlin:** sin cambios para la experiencia web.

La calificación de respuestas, finalización y entrega de XP quedan fuera de esta historia salvo que el texto completo de aceptación de la HU los exija.

## HU-03 · Regresar al punto exacto del mapa

### Historia

> Como estudiante web, regresar al punto exacto del mapa donde cerré sesión para no perder mi progreso ni repetir lecciones.

### Alcance

- Recuperar la ruta y el nodo visitado desde información persistida del estudiante.
- Al volver a la ruta, desplazar y enfocar visualmente el mapa en ese nodo.
- Si el estudiante estaba dentro de una actividad, permitir continuarla con su estado guardado.
- Resolver de forma segura un nodo recordado que ya no pertenezca a la ruta publicada.

### Criterios de aceptación

1. Cerrar y reabrir sesión conserva el identificador de ruta y último nodo válido.
2. La vista desplaza al nodo guardado y lo resalta de forma accesible.
3. Una actividad `EN_CURSO` se puede retomar sin crear sesiones/intentos duplicados.
4. Un nodo eliminado, retirado o inaccesible se resuelve hacia un punto seguro y válido.
5. El servidor es fuente de verdad; `localStorage` no reemplaza el progreso.

### Trabajo técnico

- **Spring Boot:** registrar el último nodo visitado de forma transaccional y ofrecer destino de reanudación autorizado.
- **React:** restaurar scroll/foco al recibir los datos; mostrar «Continuar» cuando corresponda.
- **MySQL:** usar `aprendizaje_inscripcion_ruta.ultimo_nodo_visitado_id`, `aprendizaje_progreso_nodo` y `aprendizaje_sesion_estudio` si la reanudación necesita más detalle.
- Guardar localmente solo preferencias de interfaz no sensibles.

## HU-04 · Ver la ruta según las unidades del MINEDU

### Historia

> Como estudiante web, ver la ruta según las unidades del MINEDU para seguir el mismo orden que las clases del colegio.

### Alcance

- Usar catálogo curricular versionado relacionado a la ruta, grado y área.
- Agrupar nodos por unidad curricular y presentar unidades en orden curricular.
- Mantener orden de nodos dentro de cada unidad.
- No mezclar unidades de distinto grado, área o versión de catálogo.
- Informar si faltan vínculos o la versión curricular dejó de estar vigente.

### Criterios de aceptación

1. El grado/área de la ruta coinciden con sus unidades curriculares.
2. Las unidades respetan `curriculo_unidad.numero_secuencia`; los nodos internos, `aprendizaje_nodo_ruta.numero_secuencia`.
3. Unidades y competencias pertenecen a la misma `curriculo_version_catalogo` de la ruta.
4. Nodos sin unidad siguen una regla de producto explícita; no se insertan en posiciones arbitrarias.
5. Ruta/catálogo retirado o incompleto produce un estado claro sin combinar con otra versión.
6. Agrupar por unidad conserva el progreso individual y su representación en el mapa.

### Trabajo técnico

- **Spring Boot:** consultar y validar coherencia de catálogo, grado, área, unidad y ruta; devolver unidades agrupadas en un orden estable.
- **React:** presentar agrupaciones sin romper continuidad visual ni perder la navegación al nodo.
- **MySQL:** reutilizar `curriculo_version_catalogo`, `curriculo_unidad`, `curriculo_area`, `curriculo_grado`, `curriculo_competencia`, `curriculo_unidad_competencia`, `aprendizaje_version_ruta`, `aprendizaje_nodo_ruta.unidad_id` y `aprendizaje_actividad_unidad`.
- **Django:** administrará catálogo oficial en historias administrativas posteriores; no se implementa su panel aquí.

## Proceso y lista de avance

### Preparación

- [x] Separar carpetas de React web, Kotlin móvil, Django, Spring Boot, MySQL y documentación.
- [x] Documentar el modelo MySQL, responsabilidades de los servicios y tablas relacionadas con HU-01–HU-04.
- [x] Documentar criterios, dependencias y trabajo por carpeta de las cuatro historias.
- [x] Definir el contrato de lectura inicial y escribir consultas fuente de HU-01 en Spring Boot.

### HU-01 · Hacer funcional el mapa con datos reales

- [x] Elegir/provisionar MySQL 8.4 de desarrollo y aplicar/validar el esquema.
- [x] Preparar datos curriculares, ruta, nodos, inscripción y progreso inicial.
- [x] Integrar principal de Spring con autenticación de estudiante.
- [x] Compilar y ejecutar API contra la base de desarrollo.
- [x] Conectar React a la API y retirar el progreso ficticio de la vista.
- [x] Completar carga, error/reintento, falta de sesión y estado vacío.

### HU-02 · Iniciar una lección

- [ ] Leer actividad y contenido ordenado.
- [ ] Implementar inicio autorizado, transaccional e idempotente.
- [ ] Abrir actividades desde nodos disponibles/en curso.

### HU-03 · Reanudar donde se quedó

- [ ] Persistir el último nodo y estado de estudio relevante.
- [ ] Restaurar mapa o actividad al volver.
- [ ] Resolver cambios/retirada del nodo recordado.

### HU-04 · Alinear con el currículo

- [ ] Preparar catálogo vigente de prueba y vinculaciones curriculares.
- [ ] Validar grado, área, versión curricular y pertenencia de unidades en backend/publicación.
- [ ] Agrupar y ordenar la ruta en API/React conservando el progreso individual.

## Definición de terminado

Cada historia se marca terminada cuando sus criterios pueden demostrarse en la web, la API aplica autenticación y autorización, MySQL conserva los cambios tras recargar, no se presentan datos de demostración como reales y se ha comprobado el comportamiento con MySQL de desarrollo. Los requisitos de privacidad y acceso para menores se resuelven antes de publicar.

## Referencias

- [HU-01: mapa, criterios y contrato API](HU-01-mapa-interactivo.md)
- [README de MySQL](../../database/mysql/README.md)
- [DDL MySQL](../../database/mysql/schema.sql)
- [Diagrama ER](../../database/mysql/rupi.dbml)
- [Modelo relacional ampliado](../../database/mysql/modelo.md)
