# HU-06 · Reorganizar Secuencia de Aprendizaje con Arrastrar y Soltar (Docente)

> **Historia de Usuario (Scrum Backlog):**
> *Como usuario web con rol profesor, yo quiero arrastrar y soltar módulos temáticos dentro de la interfaz web para reorganizar la secuencia de aprendizaje de mis estudiantes.*

---

## 1. Alcance y Criterios de Aceptación Cumplidos

| # | Tarea del Backlog | Implementación en RUPI | Estado |
|---|---|---|---|
| **T1** | Implementar la funcionalidad de arrastrar y soltar en el frontend para permitir el movimiento interactivo de los módulos temáticos en la pantalla. | En [`TeacherAuthoringView.tsx`](../../frontend/web-react/src/features/teacher/TeacherAuthoringView.tsx) y [`TeacherAuthoring.css`](../../frontend/web-react/src/features/teacher/TeacherAuthoring.css): soporte nativo de HTML5 Drag and Drop (`draggable`, `onDragStart`, `onDragOver`, `onDrop`), asa visual con `GripVertical`, estados visuales (`.is-dragging`, `.drag-over`) y controles complementarios de movimiento vertical (`ChevronUp` / `ChevronDown`) para accesibilidad en tabletas táctiles escolares y teclado (WCAG AA). | **Completada** |
| **T2** | Desarrollar la lógica de gestión de estados en la interfaz para actualizar temporalmente el orden visual de la secuencia antes de confirmar los cambios. | Estado local `orderedNodes` y bandera `isReorderDirty`: al arrastrar o pulsar flechas, las tarjetas se reordenan al instante en pantalla y actualizan su numeración (`#1, #2, #3...`). Se despliega una barra de acción con las opciones de "Guardar Orden" o "Restablecer" a la versión persistida. | **Completada** |
| **T3** | Crear el servicio en el backend encargado de recibir las nuevas posiciones de los módulos y actualizar el índice de ordenamiento en la base de datos. | Endpoint `PUT /api/v1/teacher/learning-routes/{routeId}/nodes/reorder` en [`TeacherRouteController.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/teacher/TeacherRouteController.java) y método transaccional `reorderNodes` en [`TeacherRouteRepository.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/teacher/TeacherRouteRepository.java) aplicando técnica de desplazamiento `+1000` para prevenir colisiones con la clave única `uq_nodo_orden` y recalculando las coordenadas `mapa_x` y `mapa_y` del camino. | **Completada** |
| **T4** | Ejecutar pruebas de usabilidad y validación de datos para comprobar que la nueva secuencia se guarde de forma persistente y se refleje correctamente en la vista de los estudiantes. | 11 pruebas unitarias JUnit 5 en [`TeacherRouteServiceTest.java`](../../backend/api-spring-boot/src/test/java/pe/rupi/api/teacher/TeacherRouteServiceTest.java) y suite automatizada [`scripts/Verificar-HU06-Reordenamiento.ps1`](../../scripts/Verificar-HU06-Reordenamiento.ps1) con 9/9 pruebas aprobadas al 100%. Las consultas del estudiante en [`LearningRouteRepository.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/LearningRouteRepository.java) ordenan por `numero_secuencia ASC`, reflejando la nueva secuencia inmediatamente. | **Completada** |

---

## 2. Arquitectura de la Solución

### 2.1 Flujo de Reordenamiento y Base de Datos

```text
[Profesor arrastra parada B antes de A]
                  │
                  ▼
[React: orderedNodes se actualiza al instante (#1 B, #2 A)]
                  │
[isReorderDirty = true -> Aparece barra "Guardar Orden"]
                  │
[Profesor confirma -> PUT /teacher/learning-routes/{id}/nodes/reorder]
                  │
                  ▼
[Spring Boot TeacherRouteService -> TeacherRouteRepository]
   1. Valida propiedad del docente y existencia de todos los nodos.
   2. Desplaza: UPDATE aprendizaje_nodo_ruta SET numero_secuencia = numero_secuencia + 1000 (evita uq_nodo_orden)
   3. Asigna nueva secuencia: UPDATE ... SET numero_secuencia = 1..N, mapa_x = X(seq), mapa_y = Y(seq)
                  │
                  ▼
[Estudiantes abren la ruta]:
   SELECT ... FROM aprendizaje_nodo_ruta WHERE version_ruta_id = ? ORDER BY numero_secuencia ASC
   -> Reciben la nueva secuencia pedagógica de forma persistente.
```

### 2.2 Prevención de Colisiones de Integridad Relacional

En [`schema.sql`](../../database/mysql/schema.sql), la tabla `aprendizaje_nodo_ruta` define:
```sql
UNIQUE KEY uq_nodo_orden (version_ruta_id, numero_secuencia)
```
Si se actualizara el nodo 1 a la posición 2 mientras el nodo 2 aún conserva el valor 2, MySQL rechazaría la operación por duplicado. Para resolverlo de manera robusta y atómica:
1. En el mismo `@Transactional`, se elevan los índices de la versión en `+1000`.
2. Se asignan las posiciones definitivas `1, 2, 3...` a cada nodo según el arreglo recibido.
3. Se recalculan armoniosamente las coordenadas del mapa (`calculateDefaultMapX` y `calculateDefaultMapY`) para que el camino interactivo conserve su trazado estético en zigzag.

### 2.3 Experiencia de Usuario Artesanal y Accesibilidad

- **Grip Táctil (`GripVertical`):** Asa ergonómica con cursor interactivo `grab` / `grabbing`.
- **Feedback Visual de Arrastre:** El nodo en movimiento reduce su opacidad al 45% con borde discontinuo; la zona de destino se ilumina con acento amarillo maíz (`#f95738` / `#fefce8`).
- **Botones de Movimiento Vertical:** Flechas táctiles físicas con relieve 3D (`:active` con desplazamiento de 1px) diseñadas para tabletas escolares y navegación asistida por teclado.
- **Confirmación Explícita:** Barra de notificación con aviso claro, botón verde de guardado y botón de cancelación para descartar cambios accidentales sin tocar la base de datos.

---

## 3. Pruebas y Verificación

1. **Pruebas Unitarias Backend (JUnit 5):**
   - `TeacherRouteServiceTest.reorderNodesSuccess`: Reordena paradas y valida retorno ordenado.
   - `TeacherRouteServiceTest.reorderNodesEmptyThrowsBadRequest`: Rechaza arreglos vacíos o nulos.
   - `TeacherRouteServiceTest.reorderNodesRepositoryErrorThrowsBadRequest`: Valida errores de pertenencia.
2. **Calidad Frontend:**
   - `npm run build`: compilación limpia en 637ms.
   - `npm run lint`: sin advertencias de ESLint.
3. **Suite Automatizada:**
   - [`scripts/Verificar-HU06-Reordenamiento.ps1`](../../scripts/Verificar-HU06-Reordenamiento.ps1): 9/9 pruebas aprobadas al 100%.
