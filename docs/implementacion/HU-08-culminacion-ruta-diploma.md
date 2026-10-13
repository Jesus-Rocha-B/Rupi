# HU-08 · Culminación de ruta escolar, Gran Gallito de Oro y Diploma de Honor

## Historia

> Como estudiante web de primaria, quiero celebrar la culminación de toda mi ruta escolar con Rupi, recibir mi Gran Insignia de Graduado «Gran Gallito de Oro» (+100 XP de bonificación) y visualizar mi Diploma Escolar con las competencias oficiales logradas del MINEDU, para compartir mi esfuerzo con mi familia y profesores.

## Ámbito Cultural y Pedagógico

Esta historia rinde homenaje a nuestra ave nacional, el **Gallito de las Rocas** (*Rupicola peruvianus* o *Tunqui* en quechua), y su hábitat en los bosques de neblina andino-amazónicos del Perú. Así como el gallito emprende su vuelo con energía y plumaje encendido, los estudiantes de primaria completan su travesía escolar venciendo cada reto pedagógico con esfuerzo y perseverancia.

## Criterios de Aceptación

1. **Detección Atómica de Ruta Culminada:**
   - Al completar la última parada en `POST /api/v1/student/learning-routes/{versionRouteId}/nodes/{nodeId}/complete`, el backend detecta que no existen paradas posteriores y marca la inscripción en `aprendizaje_inscripcion_ruta` con estado `COMPLETADO` y fecha UTC `completado_en`.
2. **Gran Insignia Escolar del Gallito de las Rocas (`GRAN_TUNQUI_ORO`):**
   - El backend siembra y otorga de forma idempotente en `gamificacion_insignia` y `gamificacion_insignia_usuario` la máxima medalla de ruta:
     - Código: `GRAN_TUNQUI_ORO`.
     - Nombre: *«Gran Gallito de Oro»*.
     - Descripción: *«¡Alcanzaste la cima de la ruta! Volaste como el gallito de las rocas por toda la aventura escolar de Ayacucho.»*.
     - Icono: Pluma del Tunqui de oro (`feather`).
3. **Bonificación de Experiencia de Fin de Ruta (+100 XP):**
   - Se registra una entrada adicional de +100 XP en el libro de eventos `gamificacion_movimiento_experiencia` con motivo `INSIGNIA`, referencia a la versión de ruta y clave de idempotencia `(usuario_id:version_ruta_id:GRADUACION_RUTA)`.
4. **Pantalla de Gran Celebración:**
   - Al completar la parada final, la vista de celebración despliega:
     - Mascota Rupi en su máxima expresión de júbilo (`cheering`).
     - Distintivo de la Gran Insignia *«Gran Gallito de Oro»*.
     - Indicador del bono de experiencia de fin de ruta (+100 XP).
     - Botón de acceso directo: *«Ver mi Diploma de Logro Escolar»*.
     - Locución afectuosa de felicitación por síntesis de voz (`speakText`).
5. **Endpoint de Diploma Escolar:**
   - `GET /api/v1/student/learning-routes/{versionRouteId}/diploma`:
     - Retorna el nombre del estudiante, grado, área, fecha de finalización, total de paradas logradas, total de XP acumulados, competencias del MINEDU desarrolladas y mensaje de honor de Rupi.
     - Si la ruta no está completada por el estudiante, devuelve un error `400 Bad Request` indicando que aún quedan paradas por culminar.
6. **Diploma de Honor Escolar Artesanal e Imprimible:**
   - Componente React con marco artesanal inspirado en los retablos y tejidos andinos (tonos tierra, sol maíz `#ffd26a`, verde bosque `#173e30` y naranja Tunqui `#f95738`), sin plantillas genéricas de IA.
   - Botón físico de ≥48px para imprimir o descargar en PDF (`window.print()`).
   - Botón accesible para que Rupi lea el diploma completo por voz.
   - Sello oficial del *Gallito de las Rocas* y firma de *Rupi, Compañero de Aprendizaje*.
7. **Acceso Permanente desde el Roadmap:**
   - En `RouteOverview.tsx`, cuando la ruta está al 100%, el botón principal de acción se transforma en: *«Ver mi Diploma de Logro Escolar»*.
   - El estudiante conserva acceso libre a todas las paradas para repasar sin perder su estado de graduado.

## Contratos de API

### 1. `GET /api/v1/student/learning-routes/{versionRouteId}/diploma`

#### Respuesta exitosa (`200 OK`)
```json
{
  "versionRouteId": "c0000000-0000-4000-8000-000000000001",
  "routeTitle": "Aventura Matemática en Ayacucho",
  "studentName": "Estudiante Demo",
  "schoolName": "Colegio Primario Bicentenario",
  "gradeNumber": 2,
  "gradeName": "2.° de primaria",
  "areaName": "Matemática",
  "totalCompletedNodes": 10,
  "totalNodes": 10,
  "totalExperienceEarned": 650,
  "completedAt": "2026-10-11T22:00:00Z",
  "graduationBadge": {
    "code": "GRAN_TUNQUI_ORO",
    "name": "Gran Gallito de Oro",
    "description": "¡Alcanzaste la cima de la ruta! Volaste como el gallito de las rocas por toda la aventura escolar de Ayacucho.",
    "icon": "feather"
  },
  "competencies": [
    {
      "code": "COM-01",
      "name": "Resuelve problemas de cantidad"
    },
    {
      "code": "COM-02",
      "name": "Resuelve problemas de regularidad, equivalencia y cambio"
    },
    {
      "code": "COM-03",
      "name": "Resuelve problemas de forma, movimiento y localización"
    },
    {
      "code": "COM-04",
      "name": "Resuelve problemas de gestión de datos e incertidumbre"
    }
  ],
  "rupiHonorMessage": "¡Honor al mérito! Con dedicación y perseverancia, superaste todos los retos de la ruta con la fuerza del Gallito de las Rocas."
}
```

### 2. Actualización de `POST .../nodes/{nodeId}/complete`

La respuesta ahora incluye las propiedades de culminación de ruta:
```json
{
  "nodeId": "ca000000-0000-4000-8000-000000000010",
  "state": "COMPLETADO",
  "experienceEarned": 50,
  "totalExperience": 650,
  "nextUnlockedNode": null,
  "rupiMessage": "¡Misión cumplida! Completaste las 10 paradas de la ruta escolar como un auténtico Gallito de las Rocas. ¡Ganaste la Gran Insignia 'Gran Gallito de Oro' y tu Diploma de Honor!",
  "badgeEarned": {
    "code": "GRAN_TUNQUI_ORO",
    "name": "Gran Gallito de Oro",
    "description": "¡Alcanzaste la cima de la ruta! Volaste como el gallito de las rocas por toda la aventura escolar de Ayacucho.",
    "icon": "feather"
  },
  "isRouteCompleted": true,
  "routeBonusExperience": 100
}
```
