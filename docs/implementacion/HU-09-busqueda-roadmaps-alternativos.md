# HU-09 · Búsqueda rápida y autocompletado de roadmaps alternativos por materia

## Historia

> Como usuario web, yo debería de contar con una barra de búsqueda para encontrar roadmaps alternativos ingresando el nombre de una materia a fin de explorar contenidos que me interesen rápidamente.

## Ámbito Pedagógico y Cultural

En el modelo educativo peruano (CNEB/MINEDU), las niñas y niños de primaria desarrollan competencias en múltiples áreas curriculares fundamentales: **Matemática**, **Comunicación**, **Ciencia y Tecnología**, **Personal Social** y **Arte y Cultura**. 

Para fomentar la autonomía y la curiosidad por el aprendizaje guiado con Rupi, la plataforma permite al estudiante buscar y explorar de forma ágil rutas escolares alternativas por el nombre de la materia de su interés, facilitando el descubrimiento de nuevas aventuras pedagógicas contextualizadas en la riqueza cultural y geográfica del Perú.

## Tareas y Criterios de Aceptación

### 1. Barra de Búsqueda Rápida y Autocompletado en Frontend (Tarea 1)
- **Barra de búsqueda táctil (≥48px):** Diseño artesanal siguiendo `human-crafted-frontend` con la paleta oficial (Verde bosque `#173e30`, Naranja Rupi `#f95738`, Amarillo sol `#ffd26a`, Pergamino `#faf8f5`).
- **Autocompletado dinámico:** Al escribir las primeras letras (ej. *"com"*, *"cien"*, *"mat"*), el sistema despliega sugerencias automáticas de materias con iconos didácticos y debounce (200ms).
- **Atajos de accesibilidad:** Acceso inmediato mediante atajo de teclado (`/` o `Ctrl+K`), botón de limpieza rápida (`X`), tecla `Esc` para cerrar y soporte completo para navegación por teclado.
- **Resultados de roadmaps alternativos:** Despliegue de tarjetas con título, descripción pedagógica, píldora temática de la materia, grado escolar, total de paradas y distinción clara entre rutas ya inscritas (*«Tu ruta activa»*) y rutas para explorar (*«Ruta alternativa»*).
- **Síntesis de voz accesible:** Botón para que la mascota Rupi narre los resultados en voz alta mediante `speakText`.

### 2. Índices de Búsqueda de Texto Completo (Full-Text Search) en MySQL (Tarea 2)
- **Migración `V08__busqueda_fulltext_materias_roadmaps.sql`:**
  - Creación del índice `FULLTEXT KEY ft_area_busqueda (nombre)` en la tabla `curriculo_area`.
  - Aseguramiento del índice `FULLTEXT KEY ft_ruta_busqueda (titulo, descripcion)` en `aprendizaje_ruta`.
  - Alta del catálogo oficial de materias del CNEB (`COM`, `CYT`, `SOC`, `ART`) vinculadas al catálogo vigente `CNEB-2017`.
  - Catálogo de roadmaps alternativos publicados con paradas reales para exploración.
  - Ejecución completamente idempotente.

### 3. API Optimizada de Consulta y Filtrado en Spring Boot (Tarea 3)
- **Endpoint de búsqueda:** `GET /api/v1/student/learning-routes/search?q={query}&grade={grade}&limit={limit}`
  - Consulta SQL optimizada combinando `MATCH(...) AGAINST(... IN BOOLEAN MODE)` con fallback `LIKE` para coincidencia de prefijos en palabras cortas.
  - Ordenamiento por relevancia pedagógica (coincidencia exacta > prefijo de materia > prefijo de título > texto completo).
  - Verificación atómica del estado de inscripción del estudiante (`isEnrolled: boolean`, `enrollmentState`).
- **Endpoint de autocompletado:** `GET /api/v1/student/learning-routes/suggestions?q={query}&limit={limit}`
  - Retorna lista de sugerencias de materias en tiempo de respuesta inferior a 10ms.

### 4. Pruebas de Rendimiento con Alto Volumen (Tarea 4)
- **Script de benchmark:** [`scripts/Verificar-Rendimiento-HU09.ps1`](../../scripts/Verificar-Rendimiento-HU09.ps1)
  - Medición de 60+ peticiones automatizadas midiendo latencia mínima, media, mediana (P50), P90 y P99.
  - Verificación del umbral de rendimiento: tiempo medio de respuesta < 50ms.
- **Script de integración:** [`scripts/Verificar-HU09.ps1`](../../scripts/Verificar-HU09.ps1)
  - Validación de 6 escenarios incluyendo seguridad (401), autocompletado, búsqueda por materia, filtro por grado y estados vacíos.

---

## Contratos de API

### 1. `GET /api/v1/student/learning-routes/search`

#### Parámetros de consulta
- `q` (string, opcional): Término de búsqueda (nombre de materia o palabras clave).
- `grade` (integer, opcional): Número de grado de primaria (1 a 6).
- `limit` (integer, opcional, por defecto 20): Límite máximo de resultados.

#### Respuesta exitosa (`200 OK`)
```json
{
  "query": "comunicación",
  "suggestedSubjects": [
    "Comunicación"
  ],
  "routes": [
    {
      "versionRouteId": "b0000000-0000-4000-8000-000000000002",
      "routeTitle": "Comunicación: Cuentos y Leyendas del Valle Sagrado",
      "description": "Recorrido de lectura y narración oral descubriendo las tradiciones, fábulas y leyendas andinas de Ayacucho.",
      "gradeNumber": 2,
      "gradeName": "2.° de primaria",
      "areaCode": "COM",
      "areaName": "Comunicación",
      "totalNodes": 1,
      "isEnrolled": false,
      "enrollmentState": "NO_INSCRITO"
    }
  ]
}
```

### 2. `GET /api/v1/student/learning-routes/suggestions`

#### Parámetros de consulta
- `q` (string, opcional): Prefijo o término de materia.
- `limit` (integer, opcional, por defecto 6): Límite de sugerencias.

#### Respuesta exitosa (`200 OK`)
```json
[
  "Comunicación",
  "Ciencia y Tecnología"
]
```

---

## Componentes y Archivos Creados / Modificados

| Capa | Archivo | Modificación |
|---|---|---|
| **Base de Datos** | [`database/mysql/migrations/V08__busqueda_fulltext_materias_roadmaps.sql`](../../database/mysql/migrations/V08__busqueda_fulltext_materias_roadmaps.sql) | Índices FTS en `curriculo_area` y `aprendizaje_ruta`; semillas de materias CNEB y roadmaps alternativos. |
| **Base de Datos** | [`database/mysql/schema.sql`](../../database/mysql/schema.sql) | Índice `FULLTEXT KEY ft_area_busqueda (nombre)`. |
| **Base de Datos** | [`database/mysql/rupi.dbml`](../../database/mysql/rupi.dbml) | Documentación relacional del índice FTS en área. |
| **Backend API** | [`LearningRouteDtos.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/LearningRouteDtos.java) | Records `RouteSearchResult` y `SearchAutocompleteResponse`. |
| **Backend API** | [`LearningRouteRepository.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/LearningRouteRepository.java) | Consultas optimizadas `searchAlternativeRoutes` y `findSubjectSuggestions`. |
| **Backend API** | [`LearningRouteService.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/LearningRouteService.java) | Lógica de negocio y sanitización de búsqueda. |
| **Backend API** | [`LearningRouteController.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/LearningRouteController.java) | Endpoints `/search` y `/suggestions`. |
| **Backend Tests**| [`LearningRouteServiceTest.java`](../../backend/api-spring-boot/src/test/java/pe/rupi/api/learningroute/LearningRouteServiceTest.java) | Pruebas unitarias de búsqueda y sugerencias (39 pruebas JUnit pasando al 100%). |
| **Frontend API** | [`learningRoutesApi.ts`](../../frontend/web-react/src/services/learningRoutesApi.ts) | Cliente tipado `searchAlternativeRoutes` y `fetchSubjectSuggestions`. |
| **Frontend UI**  | [`RoadmapSearchBar.tsx`](../../frontend/web-react/src/components/RoadmapSearchBar.tsx) | Componente de barra de búsqueda rápida y autocompletado táctil. |
| **Frontend CSS** | [`RoadmapSearchBar.css`](../../frontend/web-react/src/components/RoadmapSearchBar.css) | Estilos artesanales y accesibles con paleta peruana. |
| **Frontend App** | [`App.tsx`](../../frontend/web-react/src/App.tsx) | Integración de `RoadmapSearchBar` en la cabecera del aula. |
| **Frontend Tests**| [`searchApi.test.ts`](../../frontend/web-react/tests/searchApi.test.ts) | Pruebas unitarias en Node para query params y categorías. |
| **Scripts**      | [`Verificar-HU09.ps1`](../../scripts/Verificar-HU09.ps1) | Validación integral de 6 escenarios HTTP/API. |
| **Scripts**      | [`Verificar-Rendimiento-HU09.ps1`](../../scripts/Verificar-Rendimiento-HU09.ps1) | Benchmark de rendimiento con cálculo de P50, P90 y P99. |
