# HU-04 · Ver la ruta según las unidades del MINEDU

## Historia

> Como estudiante web, quiero ver la ruta según las unidades del MINEDU para seguir el mismo orden que las clases del colegio.

## Criterios de Aceptación

1. **Alineación curricular estricta:** La ruta agrupa sus paradas en unidades pertenecientes al Currículo Nacional de la Educación Básica (CNEB).
2. **Coherencia de catálogo, grado y área:** Se valida en backend que las unidades pertenezcan a la misma `version_catalogo_id`, `grado_id` y `area_id` que la versión de ruta.
3. **Orden curricular:** Las unidades se presentan según `curriculo_unidad.numero_secuencia`, y las paradas dentro de cada unidad según `aprendizaje_nodo_ruta.numero_secuencia`.
4. **Visibilidad de competencias:** Cada franja de unidad muestra las competencias curriculares que se desarrollan (por ejemplo, *«Resuelve problemas de cantidad»*).
5. **Progreso por unidad:** Se calcula y muestra el avance de cada unidad (*«X de Y paradas»*) sin duplicar columnas en MySQL.

## Estructura Relacional en MySQL

```text
curriculo_version_catalogo (CNEB-2024, ACTIVO)
       │
       ├── curriculo_area (MAT, Matemática)
       │         └── curriculo_competencia (RES_CANT, RES_REG)
       │
       └── curriculo_unidad (U1-2P-MAT, U2-2P-MAT)
                 ├── curriculo_unidad_competencia (M:N)
                 └── aprendizaje_nodo_ruta.unidad_id
```

## Implementación Técnica

- **Backend (Spring Boot):**
  - [`RouteCurriculum.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/RouteCurriculum.java): Valida consistencia curricular, agrupa nodos y calcula progreso por unidad.
  - [`LearningRouteRepository.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/LearningRouteRepository.java): Consulta `findCurriculumData` uniendo `aprendizaje_version_ruta`, `curriculo_unidad`, `curriculo_competencia` y `curriculo_unidad_competencia`.
  - [`LearningRouteService.java`](../../backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/LearningRouteService.java): Integra la información curricular en el objeto de detalle de la ruta.
- **Frontend (React):**
  - [`unitGroups.ts`](../../frontend/web-react/src/features/roadmap/unitGroups.ts): Mapea la primera parada de cada unidad para encabezar el tramo en el mapa.
  - [`UnitBands.css`](../../frontend/web-react/src/features/roadmap/UnitBands.css): Estilos visuales táctiles y de alto contraste inspirados en la escuela peruana.
  - [`Roadmap.tsx`](../../frontend/web-react/src/features/roadmap/Roadmap.tsx): Renderiza las bandas curriculares sin romper la continuidad del sendero en zigzag.
