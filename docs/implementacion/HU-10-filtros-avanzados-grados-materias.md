# HU-10 · Filtros avanzados por grado escolar y materia curricular

## Historia

> Como usuario web, yo debería poder aplicar filtros por grado escolar (ej. 3ro de primaria, 4to de primaria) a fin de asegurar que la dificultad de la ruta de estudio sea adecuada para mi nivel.

## Ámbito Pedagógico y Accesibilidad

En la educación primaria peruana (grados 1.° a 6.°), la graduación de la dificultad pedagógica es crucial para el éxito escolar y la autoestima de los estudiantes. Un estudiante de 3.° de primaria no debe enfrentarse a ecuaciones algebraicas de 6.° ni aburrirse con vocales básicas de 1.°. 

Mediante **filtros laterales interactivos por grado y materia**, las niñas, niños y docentes pueden personalizar su experiencia seleccionando su nivel exacto o combinando grados contiguos (por ejemplo, multigrado rural 3.° y 4.°), asegurando que los retos, lecturas y problemas matemáticos correspondan a su zona de desarrollo próximo según los estándares oficiales del CNEB 2017.

## Tareas y Criterios de Aceptación

### 1. Componente de Filtros Laterales en Frontend (`RoadmapFilterSidebar.tsx`)
- **Checkboxes táctiles accesibles (≥48px):** Diseño artesanal siguiendo `human-crafted-frontend` con la paleta andina oficial (Verde bosque `#173e30`, Verde hoja `#31724b`, Naranja Rupi `#f95738`, Amarillo sol `#ffd26a`).
- **Selección de múltiples grados:** Checkboxes interactivos para marcar de 1.° a 6.° de primaria de forma individual o con botón rápido "Seleccionar todos".
- **Filtros por materias curriculares:** Chips y casillas para Matemática, Comunicación, Ciencia y Tecnología, Personal Social y Arte y Cultura.
- **Chips de filtros activos:** Visualización inmediata de los filtros seleccionados con botón para retirarlos individualmente y botón de "Restablecer filtros".
- **Resultados en tiempo real:** Despliegue de tarjetas de rutas adaptadas al nivel con título, paradas, grado y botón táctil "Abrir ruta".
- **Síntesis de voz accesible:** La mascota Rupi lee en voz alta el resumen de los filtros activos mediante `speakText`.

### 2. Sincronización del Estado con la URL
- **Persistencia en URL:** Los filtros se sincronizan automáticamente con los parámetros de la URL (`window.location.search`):
  - `?grades=3,4` o `?grade=3`
  - `?areas=MATEMATICA,COMUNICACION` o `?area=MAT`
  - `?q=fracciones`
- **Compartir enlaces:** Al recargar la página o al compartir un enlace con parámetros de consulta, el estado del frontend se inicializa exactamente con los filtros especificados.
- **Navegación sin recarga:** Utiliza `window.history.replaceState` para mantener una experiencia fluida sin parpadeos.

### 3. API de Consulta con Múltiples Parámetros Simultáneos en Spring Boot
- **Endpoint:** `GET /api/v1/student/learning-routes/search`
- **Parámetros soportados:**
  - `grades` (lista de enteros o valores separados por coma, ej. `3,4` o `grades=3&grades=4`)
  - `grade` (entero individual para retrocompatibilidad)
  - `areas` (lista de cadenas de materias, ej. `MATEMATICA,COMUNICACION`)
  - `area` (cadena individual, ej. `MAT`)
  - `q` (término de búsqueda de texto opcional)
  - `limit` (número máximo de resultados, por defecto 20)
- **Consulta SQL:** Ejecuta `WHERE g.numero_grado IN (:gradeNumbers)` y `AND (LOWER(a.codigo) IN (:areaCodes) OR LOWER(a.nombre) IN (:areaCodes) ...)`.
- **DTO de respuesta:** `SearchAutocompleteResponse` devuelve `routes`, `selectedGrades`, `selectedAreas`, `suggestedSubjects` y `totalResults`.

### 4. Precisión y Pruebas Automatizadas
- **Suite de pruebas Spring Boot:** `LearningRouteServiceTest.java` (40 pruebas JUnit aprobadas al 100%).
- **Script de verificación E2E:** [`scripts/Verificar-HU10.ps1`](../../scripts/Verificar-HU10.ps1) compatible con Windows PowerShell 5.1 y 7+:
  1. Seguridad sin sesión (401 Unauthorized).
  2. Autenticación de estudiante demo.
  3. Filtro por grado individual (3.°).
  4. Filtro por múltiples grados simultáneos (3.° y 4.°).
  5. Filtro por materia (Matemática).
  6. Filtro combinado grado + materia (3.°/4.° y Matemática).
  7. Filtro combinado grado + materia + texto libre.
  8. Filtro sin coincidencias (lista vacía y `totalResults: 0`).

---

## Verificación Local

```powershell
# 1. Pruebas unitarias de Backend
mvn -f backend/api-spring-boot/pom.xml test

# 2. Compilación y linter de Frontend
cd frontend/web-react
npm run build
npm run lint

# 3. Verificación de integración E2E (con servidor activo)
powershell -ExecutionPolicy Bypass -File scripts/Verificar-HU10.ps1
```
