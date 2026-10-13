# HU-05 · Autoría y Gestión de Roadmaps Personalizados para Profesores

> **Historia de Usuario (Scrum Backlog):**
> *Como usuario web con rol de profesor, yo debería contar con una herramienta de gestión y autoría en la interfaz web para diseñar roadmaps personalizados y asociarlos a los materiales educativos propios, adaptando la plataforma a la planificación curricular de las clases presenciales.*

---

## 1. Alcance y Criterios de Aceptación Cumplidos

| # | Tarea del Backlog | Implementación en RUPI | Estado |
|---|---|---|---|
| **T1** | Diseñar y programar la interfaz gráfica de autoría para visualizar, crear y organizar la estructura del mapa interactivo. | `TeacherAuthoringView.tsx` y `TeacherAuthoring.css`: Panel docente para crear roadmaps, definir grado (1.° a 6.° de primaria), materia oficial CNEB (Matemática, Comunicación, CYT, Personal Social, Arte) y organizar paradas cronológicas con duraciones estimadas. | **Completada** |
| **T2** | Desarrollar un componente de subida de archivos en frontend y su respectivo servicio en el servidor para almacenar de forma segura los materiales educativos. | `MaterialUploadZone.tsx` y `MaterialStorageService.java`: Subida multipart con hash SHA-256, sanitización de nombres, validación de extensiones permitidas (`.pdf`, `.ppt`, `.pptx`, imágenes, enlaces web) y descarga pública segura. | **Completada** |
| **T3** | Construir la lógica relacional en base de datos y rutas API para vincular documentos subidos con nodos específicos del roadmap. | Migración `V12__rol_docente_autoria_hu05.sql` creando tabla relacional `aprendizaje_nodo_recurso`, repositorios y controladores REST `/api/v1/teacher/learning-routes/**` y `/api/v1/teacher/materials/**`. | **Completada** |
| **T4** | Realizar pruebas de flujo y guardado para garantizar que la planificación curricular diseñada por el profesor se despliegue sin errores en las cuentas de sus estudiantes. | Doble enlace relacional a `aprendizaje_bloque_contenido` con tipo `ADJUNTO`, renderizado táctil en `ActivityBlocks.tsx`, 63 pruebas JUnit 5 en Spring Boot y suite de verificación `scripts/Verificar-HU05-Docente.ps1` aprobada 7/7 (100%). | **Completada** |

---

## 2. Arquitectura de la Solución

### 2.1 Modelo de Datos (MySQL `rupi`)

```text
identidad_rol (DOCENTE)
       │
identidad_usuario_rol
       │
identidad_cuenta (profesor.demo / 123456)
       │
curriculo_ruta_aprendizaje (creador_usuario_id)
       │
curriculo_version_ruta (version borrador / publicada)
       │
curriculo_nodo_ruta (paradas escolares)
       ├── aprendizaje_nodo_recurso (vinculación permanente)
       └── recurso_archivo_multimedia (hash SHA-256, ruta de disco, tipo mime)
```

- **Invariante Single Writer:** Spring Boot escribe las tablas de aprendizaje, recursos y rutas docentes sin vulnerar los catálogos base de Django.
- **Seguridad y Roles:** El filtro `StudentAuthenticationFilter` inspecciona los roles del usuario autenticado; cualquier intento de acceder a `/api/v1/teacher/**` sin rol `DOCENTE` devuelve `403 Forbidden`.

### 2.2 Endpoints REST Implementados

- `GET /api/v1/teacher/session`: Retorna la sesión y datos del profesor autenticado.
- `GET /api/v1/teacher/learning-routes`: Lista de roadmaps creados por el docente (borradores y publicados).
- `POST /api/v1/teacher/learning-routes`: Crea un nuevo roadmap borrador con título, grado escolar y área curricular CNEB.
- `GET /api/v1/teacher/learning-routes/{routeId}`: Detalle de la versión con sus nodos y recursos asociados.
- `POST /api/v1/teacher/learning-routes/{routeId}/nodes`: Agrega una nueva parada pedagógica a la ruta.
- `POST /api/v1/teacher/materials/upload`: Almacena documentos PDF, presentaciones o enlaces.
- `POST /api/v1/teacher/learning-routes/{routeId}/nodes/{nodeId}/attachments`: Asocia un recurso a una parada.
- `POST /api/v1/teacher/learning-routes/{routeId}/publish`: Publica la versión para que esté disponible en las aulas de los estudiantes.
- `GET /api/v1/materials/{id}/download`: Permite a los estudiantes abrir o descargar los archivos adjuntos.

### 2.3 Experiencia de Usuario Artesanal (Frontend React)

- **Diseño sin AI Slop:** Sin halos morados ni plantillas corporativas. Fondos cálidos inspirados en papel y piedra andina, tipografía Nunito con jerarquía visual nítida.
- **Botones Táctiles con Relieve 3D:** Desplazamiento de 2px y reducción de sombra al presionar (`:active`).
- **Alternancia Docente / Alumno:** El profesor puede alternar entre el **Panel de Autoría** y la **Vista Previa de Estudiante** para comprobar en vivo cómo sus alumnos experimentarán las paradas y abrirán los documentos PDF/PPT.
- **Credenciales Demo Integradas:** En `LoginView.tsx` se integró el acceso rápido tanto para `Estudiante (Carlos Rivas)` como para `Docente (Prof. Carlos Mendoza)`.

---

## 3. Pruebas y Verificación

1. **Pruebas Unitarias Backend (JUnit 5):**
   - `TeacherRouteServiceTest`: 8 pruebas cubriendo validación de títulos, áreas, grados, publicación y control de acceso.
   - `MaterialStorageServiceTest`: 4 pruebas cubriendo sanitización, cálculo SHA-256 y rechazo de tipos no permitidos.
   - `ActivityServiceTest`: Validación de carga de bloques didácticos con adjuntos.
   - Total suite: 63 pruebas ejecutadas con `mvn test` aprobadas al 100%.

2. **Calidad Frontend:**
   - Compilación TypeScript y Vite: `npm run build` sin errores.
   - Linter ESLint: `npm run lint` sin advertencias.

3. **Script de Verificación E2E:**
   - `scripts/Verificar-HU05-Docente.ps1`: 7 pruebas aprobadas, 0 fallidas.
