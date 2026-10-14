# HU-13 · Gestionar la calendarización

El docente registra y modifica períodos de unidades y evaluaciones de una ruta para cada aula. El estudiante ve el calendario mensual y la agenda junto al mapa. Los datos se guardan en MySQL y persisten al cerrar sesión o recargar.

## Uso

1. Como docente, abre **Mis Roadmaps → Editar y Adjuntar Materiales**. En **Calendario del aula**, selecciona tu aula.
2. Selecciona una unidad o evaluación e indica inicio y fin en hora de Perú. **Guardar fechas** crea la programación o actualiza la del mismo elemento y aula, sin duplicarla.
3. **Editar fechas** carga los valores existentes; **Cancelar edición** descarta los cambios del formulario. **Retirar fecha** elimina solo la programación, no la unidad ni la evaluación.
4. El estudiante inscrito encuentra el calendario al lado de la ruta (debajo en pantallas estrechas). Puede cambiar de mes, seleccionar entre sus aulas y pulsar **Actualizar fechas**. No puede editar.

Las unidades son las `curriculo_unidad` enlazadas a nodos de la versión de ruta. Las evaluaciones son paradas cuya actividad es `CUESTIONARIO` o `RETO`. Una ruta sin estos contenidos muestra instrucciones, sin inventar unidades o evaluaciones.

## Aula y permisos

El docente debe ser propietario de la ruta y responsable del aula o miembro DOCENTE ACTIVO; aula y ruta deben coincidir en grado. El alumno debe estar inscrito en una ruta publicada y tener membresía ESTUDIANTE ACTIVO en el aula. Un aula inactiva no se ofrece. El calendario no asigna membresías, no matricula alumnos, no modifica autorías y no bloquea automáticamente las lecciones por fecha.

Los cambios de fecha de una sección no afectan a otra aunque compartan la misma ruta. Los títulos de eventos se guardan como fotografía del título al programarlos. Una misma unidad/evaluación tiene un único período por aula y versión de ruta; volver a guardarla actualiza ese período.

## Base de datos y actualización

`database/mysql/migrations/V13__calendarizacion_ruta_aula.sql` crea `escuela_planificacion_ruta`; también se incluye en el esquema base. El escritor es Spring Boot. La tabla de eventos generales del colegio se conserva para otros tipos de eventos; esta tabla relaciona explícitamente aula, versión de ruta y objetivo curricular/evaluativo.

Desde la raíz del proyecto y con MySQL iniciado:

```powershell
.\scripts\Actualizar-HU13.ps1
.\Iniciar-Rupi.ps1 -Recompilar
```

El actualizador usa `.local/root.cnf`, obtiene la cuenta de API de `.local/env.ps1` y aplica los permisos del dominio mediante el script existente. También acepta `-RootConfig` y `-ApiUser`. Es compatible con Windows PowerShell 5.1 y se puede repetir. No contiene contraseñas ni crea fechas ficticias. Si ya hay una API corriendo, reiníciala para cargar el nuevo JAR; el iniciador no reemplaza un proceso en ejecución.

En esta implementación V13 y sus permisos ya se aplicaron a la base local del proyecto. Los datos de validación se crearon en otra instancia temporal, sin modificar aulas ni rutas reales.

## API

Todos los identificadores de ruta siguientes son **versionRouteId**, no routeId.

- `GET /api/v1/student/learning-routes/{versionRouteId}/calendar`: aulas del alumno y eventos permitidos.
- `GET /api/v1/teacher/learning-routes/{versionRouteId}/calendar`: aulas gestionables, unidades/evaluaciones disponibles y eventos.
- `PUT /api/v1/teacher/learning-routes/{versionRouteId}/calendar`: `{ classroomId, type, targetId, start, end }`. `type` es `UNIDAD` o `EVALUACION`; fechas ISO-8601 con offset, por ejemplo `2026-10-21T09:00:00-05:00`.
- `DELETE /api/v1/teacher/learning-routes/{versionRouteId}/calendar/{eventId}?classroomId={aulaId}`: retira la programación del aula indicada.

Fechas almacenadas como UTC DATETIME(6), presentadas en America/Lima. Se admiten fechas entre 2000 y 2100, sin final anterior al inicio. Se permiten períodos simultáneos: las unidades pueden contener evaluaciones. Se validan pertenencia del objetivo a la ruta y permisos antes de escribir. Respuestas: 401 sin sesión, 403 sin rol/aula permitidos, 404 ruta/evento ajeno, 400 tipo/objetivo/fechas inválidos.

## Validación

- Suite Java: 107 pruebas correctas, incluidas 10 nuevas sobre acceso, fechas, objetivos y eliminación.
- Suite TypeScript: 21 pruebas correctas; incluye cambio de año, febrero bisiesto, períodos entre meses y conversiones Lima/UTC.
- Compilación React y lint correctos.
- API real con MySQL 8.4 aislado: alta de unidad/evaluación, persistencia, edición conservando ID, aislamiento entre dos aulas, eliminación y respuestas 400/401/403/404. Migración repetida y script PowerShell ejecutados correctamente.
- Navegador Edge con aplicación completa y API real: docente guarda, edita y recarga; alumno consulta sin controles de edición; pantalla de 375 px sin desbordamiento del calendario. Horas verificadas con el navegador docente en Asia/Tokyo.

Se corrigió la lectura de `activity_type` del detalle docente para consultar `aprendizaje_actividad.tipo`, necesaria para acceder a la pantalla donde se registra el calendario. Se habilitaron PUT/DELETE y X-Requested-With en la configuración CORS local para la escritura de fechas.
