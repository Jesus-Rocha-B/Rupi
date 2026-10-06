# HU-01 · Mapa interactivo

## Historia

Como estudiante web, quiero visualizar mi ruta de aprendizaje como un mapa interactivo para conocer visualmente mi avance escolar.

## Criterios de aceptación

1. Al entrar a «Mi ruta», el estudiante ve las rutas en las que está inscrito, ordenadas por grado y área.
2. Al abrir una ruta, ve su título, el grado y área correspondientes, y sus nodos en el orden definido por la versión publicada.
3. Cada nodo muestra su actividad y el estado de progreso guardado para ese estudiante: bloqueado, disponible, en curso o completado.
4. El resumen de avance se calcula a partir de los nodos completados de esa inscripción; no se guarda un porcentaje duplicado.
5. Si no hay una inscripción activa, se presenta un estado vacío que invita a explorar o unirse a una ruta.
6. Los datos visibles provienen del backend y MySQL; al recargar la página se mantiene el mismo progreso.
7. La vista se adapta a móvil y escritorio y comunica los estados sin depender únicamente del color.

## Alcance

Esta historia cubre la visualización de la ruta y el progreso persistido. Iniciar una lección, retomar exactamente el último punto y navegar por rutas curriculares se implementan en HU-02, HU-03 y HU-04, respectivamente. La pantalla actual ya dibuja un mapa de diez nodos ambientado en Ayacucho, pero sus datos y estados son demostrativos.

## Correspondencia con MySQL

- `aprendizaje_inscripcion_ruta`: estudiante, versión de ruta inscrita, estado y último nodo visitado.
- `aprendizaje_version_ruta`: versión publicada, grado y área de la ruta.
- `aprendizaje_nodo_ruta`: orden y actividad asociada a cada parada.
- `aprendizaje_version_actividad`: título y metadatos de la actividad.
- `aprendizaje_progreso_nodo`: estado persistido por nodo e inscripción.

El resumen se deriva de `aprendizaje_progreso_nodo`; no se agrega una columna para porcentaje ni para total completado.

## Contrato inicial de API

Los endpoints son de solo lectura para HU-01. No reciben un `estudianteId` enviado por el navegador: Spring Boot identifica al estudiante desde su contexto de autenticación y comprueba la inscripción antes de devolver los datos.

### `GET /api/v1/student/learning-routes`

Devuelve las rutas publicadas en las que está inscrito el estudiante actual, en las que su inscripción se encuentre activa o completada. Sirve para elegir cuál ruta abrir.

```json
{
  "items": [
    {
      "versionRouteId": "uuid",
      "title": "Matemática: aventura en la plaza",
      "grade": { "number": 2, "name": "2.º grado" },
      "area": { "code": "MAT", "name": "Matemática" },
      "enrollmentState": "ACTIVO",
      "completedNodes": 2,
      "totalNodes": 10
    }
  ]
}
```

### `GET /api/v1/student/learning-routes/{versionRouteId}`

Devuelve los metadatos de la ruta inscrita y los nodos ordenados por `numero_secuencia`. Los estados son `BLOQUEADO`, `DISPONIBLE`, `EN_CURSO` y `COMPLETADO`. La interfaz calcula el porcentaje a partir de `completedNodes / totalNodes`.

```json
{
  "versionRouteId": "uuid",
  "title": "Matemática: aventura en la plaza",
  "grade": { "number": 2, "name": "2.º grado" },
  "area": { "code": "MAT", "name": "Matemática" },
  "enrollment": {
    "id": "uuid",
    "state": "ACTIVO",
    "lastVisitedNodeId": "uuid"
  },
  "progress": { "completedNodes": 2, "totalNodes": 10 },
  "nodes": [
    {
      "id": "uuid",
      "sequence": 1,
      "title": "Números en la plaza",
      "activityType": "RETO",
      "estimatedMinutes": 8,
      "state": "COMPLETADO",
      "optional": false,
      "position": { "x": 50, "y": 16 }
    }
  ]
}
```

En HU-01 `totalNodes` cuenta todos los nodos de la versión, incluidos los opcionales, para que el total sea consistente entre la lista y el mapa. El porcentaje no se persiste. La inscripción debe tener una fila de progreso por nodo; el proceso que crea la inscripción inicializa sus estados en una transacción.

### Respuestas y estados de pantalla

- `200`: datos de rutas o detalle solicitados.
- `401`: no hay sesión válida; la aplicación muestra el acceso.
- `404`: la versión no existe, no está publicada o el estudiante no está inscrito. La respuesta no revela cuál condición aplica.
- `503`: base de datos o API temporalmente no disponible; React muestra error y opción de reintento.
- Lista `items` vacía: React presenta el estado vacío para explorar o unirse a una ruta.

React muestra carga mientras espera la respuesta, ordena los nodos con el orden recibido y comunica cada estado con texto/icono además del color. Abrir una lección y cambiar progreso quedan para HU-02.

## Arquitectura de implementación

```text
React web (`frontend/web-react`) → API Spring Boot (`backend/api-spring-boot`) → MySQL (`database/mysql`, schema `rupi`)
React administración (Django) ────────┘
Kotlin móvil (`frontend/mobile-kotlin`) ─┘
```

Para esta historia, Spring Boot es el servicio lector de inscripción, versión, nodos y progreso. No se asigna a Django la escritura de estos datos. La primera entrega no requiere cambios al esquema integral existente.

## Seguimiento

- [x] Revisar el proyecto, el backlog actual y el esquema MySQL.
- [x] Delimitar HU-01 y definir sus criterios de aceptación.
- [x] Separar React, Kotlin, Django, Spring Boot y MySQL en carpetas del monorepo.
- [x] Definir el contrato de consulta de Spring Boot y el comportamiento de carga/error de React.
- [x] Implementar consulta autorizada en Spring Boot usando el esquema MySQL existente.
- [x] Integrar el principal de Spring Boot con la autenticación de estudiante.
- [x] Aplicar el esquema a una instancia MySQL y cargar catálogo, ruta, nodos, inscripción y progreso iniciales.
- [x] Sustituir en React los datos demostrativos por la respuesta del backend.
- [x] Verificar visualmente estados, avance calculado, diseño adaptable y persistencia tras recargar.

## Estado actual

La historia HU-01 está implementada y funcional de extremo a extremo:
- El esquema MySQL `rupi` y los datos semilla (`seed-hu01.sql`) están cargados con el estudiante Mateo Quispe, la ruta de Matemática de 2.º grado y 10 paradas interactivas en la plaza de Ayacucho.
- La API de Spring Boot (`rupi-api` en puerto 8081) autentica al estudiante mediante su Principal, autoriza las consultas al esquema existente y entrega los contratos de `GET /api/v1/student/learning-routes` y `GET /api/v1/student/learning-routes/{versionRouteId}` con soporte CORS.
- La interfaz React (`frontend/web-react`) consume la API mediante `learningRoutesApi.ts`, calcula el avance derivado (20% a partir de 2 de 10 nodos completados) y maneja los estados de carga, error de conexión con reintento, sesión no iniciada (401) y estado vacío.
- La navegación responde de forma adaptable en móvil y escritorio y comunica los estados mediante iconos, texto y color.

## Revisión posterior de implementación

La verificación actual, correcciones y límites de autenticación se documentan en [HU-01-verificacion-local.md](HU-01-verificacion-local.md). La sesión demo no constituye un sistema de inicio de sesión de producción.

