# Cambios desde el commit `48a9357` · RUPI

Este documento resume lo que entró al repositorio entre nuestro último commit (`48a9357`, 13 de octubre de 2026) y `75a62b4` (14 de octubre de 2026). También registra los errores encontrados al revisarlo y las correcciones aplicadas después.

- **Rango:** `48a9357..75a62b4`, 12 commits, 90 archivos, +8437 / −776 líneas.
- **Autores:** JContrerasC24 (HU-12, HU-13, HU-14) y Jesus-Rocha-B (roadmaps, perfil, lección y correcciones).
- **Detalle por historia:** [`docs/implementacion/HU-12-biblioteca-minedu.md`](../implementacion/HU-12-biblioteca-minedu.md), [`HU-13-calendarizacion.md`](../implementacion/HU-13-calendarizacion.md) y [`HU-14-recordatorios-automaticos.md`](../implementacion/HU-14-recordatorios-automaticos.md).

## 1. Commits incluidos

| Fecha | Commit | Autor | Resumen |
|---|---|---|---|
| 13 oct | `85ac0b1` | JContrerasC24 | HU-12: biblioteca MINEDU por grado y repaso descargable |
| 13 oct | `2334e5b` | JContrerasC24 | HU-12: descarga de materiales por nodo y consulta sin conexión |
| 13 oct | `2422f7c` | JContrerasC24 | HU-13: fechas de unidades y evaluaciones por aula |
| 13 oct | `d37efbf` | JContrerasC24 | HU-14: recordatorios automáticos de evaluaciones sin duplicados |
| 14 oct | `42eb482` | Jesus-Rocha-B | Corrige la creación de roadmaps y el ingreso a nodos; rediseño accesible |
| 14 oct | `b958876` | Jesus-Rocha-B | Restaura tildes UTF-8 en la base y desactiva la voz automática |
| 14 oct | `8cabcb4` | Jesus-Rocha-B | Roadmaps Oficial vs Comunidad, navbar multipantalla, avatar y vitrina de insignias |
| 14 oct | `ee715b7` | Jesus-Rocha-B | Separa el grado escolar de la ruta explorada; tarjetas ilustradas y videos |
| 14 oct | `ce069d7` | Jesus-Rocha-B | Contextos Costa, Sierra y Selva (CNEB); grado fijo; Rupi en el nivel actual |
| 14 oct | `9cf375f` | Jesus-Rocha-B | Autor «Creado por RUPI» en rutas oficiales; rutas docentes en Comunidad |
| 14 oct | `e3f4a5b` | Jesus-Rocha-B | Pantalla de lección con video previo y práctica interactiva |
| 14 oct | `75a62b4` | Jesus-Rocha-B | Temporizador de seguridad en la pantalla de carga |

## 2. Qué cambió por capa

### Backend · Spring Boot (`backend/api-spring-boot`)

| Módulo | Cambio |
|---|---|
| `studypackage/` | HU-12. Catálogo `study/primary-resources.json` (recursos MINEDU por grado) y página de estudio sin conexión. Endpoints `GET …/learning-routes/{id}/study-resources` y `GET …/nodes/{nodeId}/package`, además del paquete por unidad que ya existía. |
| `calendar/` (nuevo) | HU-13. `GET /api/v1/student/learning-routes/{route}/calendar`, y en `/api/v1/teacher/…/calendar` los métodos `GET`, `PUT` y `DELETE /{event}?classroomId=`. Solo el docente propietario de la ruta y responsable del aula puede asignar fechas. |
| `reminder/` (nuevo) | HU-14. `ReminderScheduler` revisa cada 60 s y crea un aviso 24 h antes de cada evaluación. La clave única evita duplicados aunque haya dos servidores. Endpoints `GET …/reminders` y `PUT …/reminders/{id}/read`. Se desactiva con `rupi.reminders.enabled=false`. |
| `learningroute/` | Las rutas devuelven `category` (`OFICIAL` o `COMUNIDAD`) y `authorName`. La primera parada aparece `DISPONIBLE` aunque aún no haya progreso. |
| `shared/StudentSessionService` | La sesión devuelve `gradeNumber` y `gradeName` del aula activa (2.° si el alumno no tiene aula). |
| `shared/` | UTF-8 forzado en las respuestas JSON (`JsonUtf8ResponseAdvice`, convertidor Jackson y `application.yml`). CORS admite `PUT` y `DELETE`. |
| `material/`, `teacher/` | Columnas alineadas con `schema.sql`: `tamano_bytes` y `url_externa` en `recurso_archivo_multimedia`; `tipo` pasa a `aprendizaje_actividad`. Esto corrige los 503 al abrir lecciones y subir materiales, registrados el 13 de octubre. |

Pruebas nuevas: `CalendarServiceTest`, `ReminderServiceTest`, `ReminderRepositoryIntegrationTest` (desactivada por defecto, requiere una base aislada), `PrimaryStudyCatalogTest`, `StudyPackageControllerTest` y `StudyPackageServiceTest`.

### Base de datos (`database/mysql`)

| Archivo | Cambio |
|---|---|
| `V13__calendarizacion_ruta_aula.sql` | Nueva tabla `escuela_planificacion_ruta` (fechas por aula, versión de ruta y unidad o evaluación, en UTC). |
| `V14__recordatorios_evaluacion.sql` | Nueva tabla `escuela_recordatorio_evaluacion` (un aviso por alumno, evaluación y fecha). |
| `V15__diversificar_contextos_costa_sierra_selva.sql` | Actualiza 18 contextos culturales de `curriculo_contexto_cultural`. |
| `V09`, `V10` | **Editadas después de publicarse:** se corrigieron tildes y se añadieron campos a los `ON DUPLICATE KEY UPDATE`. |
| `schema.sql` | Incluye las tablas de V13 y V14. |
| `seed-hu01.sql` | El `ON DUPLICATE KEY UPDATE` de la ruta demo actualiza título y descripción. |

### Frontend · React (`frontend/web-react`)

- **Pantallas nuevas:**
  - `LessonScreenView`: video explicativo antes de la práctica.
  - `ExploreRoadmapsView`: catálogo Oficial y Comunidad.
  - `StudentProfileView`: perfil con vitrina de insignias.
  - `RouteCalendar` (HU-13) y `EvaluationReminders` (HU-14).
  - `OfficialStudyResources` (HU-12).
- **Componentes nuevos:** `StudentAvatar` y `RoadmapCard`.
- **`App.tsx` reestructurado:** navegación entre «Mi Camino», «Explorar Roadmaps» y «Mi Perfil», selección de grado y curso, y pantalla de carga.
- **Servicios nuevos:** `calendarApi.ts` y `remindersApi.ts`. `learningRoutesApi.ts` añade el grado y la categoría.
- **Prueba nueva:** `tests/calendarDates.test.ts` (fechas en hora de Lima).

### Scripts (`scripts/`)

| Script | Uso |
|---|---|
| `Actualizar-HU13.ps1` / `Actualizar-HU14.ps1` | Aplican V13 o V14 y vuelven a otorgar permisos a la cuenta de la API. Buscan `mysql.exe`, así que en Linux hay que aplicar la migración a mano. |
| `Probar-Login.ps1`, `Probar-Crear-Ruta.ps1`, `Probar-Entrar-Nodo.ps1` | Pruebas manuales contra la API con las cuentas demo. |
| `Verificar-DB.ps1`, `Iniciar-Backend-Daemon.ps1` | Diagnóstico de la base y arranque de la API en segundo plano. |
| `verify_hu12_resources.py` | Verifica los recursos HU-12 desde la API. |

## 3. Cómo poner al día una copia local

1. Ejecuta `git pull --ff-only`.
2. Aplica las migraciones nuevas en orden: V13, V14 y V15.
3. Vuelve a ejecutar V09 y V10, que son idempotentes, para recibir las tildes corregidas. Si la base ya las tenía aplicadas, no las recibe de otro modo.
4. Vuelve a ejecutar `database/mysql/grants-spring-boot.sql` con `SET @api_usuario = '<usuario>'`. Los permisos se otorgan por prefijo de tabla, así que cubren `escuela_planificacion_ruta` y `escuela_recordatorio_evaluacion`. Sin ellos, el calendario responde 503 y el proceso de recordatorios registra un error en cada ciclo.
5. Reconstruye la API (`mvn -f backend/api-spring-boot/pom.xml package`) y el frontend (`npm install && npm run build` en `frontend/web-react`).

## 4. Revisión: errores encontrados

Verificación del rango `48a9357..75a62b4`: `mvn test` pasa, y en el frontend pasan `npm run build`, `npm run lint` y las pruebas de Node.

### Corregidos en este bloque (sobre `App.tsx`, `AuthViews.css` y `package.json`)

| # | Problema | Corrección |
|---|---|---|
| 1 | El grado elegido en la pantalla de selección se guardaba en `localStorage` (`rupi_student_manual_grade`), tenía prioridad sobre el grado del servidor y no se borraba al cerrar sesión. En una tableta compartida, el siguiente alumno heredaba el grado y el curso. | El grado escolar sale siempre de la sesión (`gradeNumber`). Elegir un grado solo cambia la ruta que se explora. Al iniciar o cerrar sesión se borran las preferencias de navegación y las claves antiguas. El acceso a `localStorage` está protegido con `try/catch` y los grados leídos se validan (1 a 6). |
| 2 | El temporizador de 4 s ocultaba la pantalla de carga aunque la carga siguiera en curso. Con conexión lenta, un alumno con sesión abierta veía el inicio de sesión. | La pantalla de carga se mantiene. A los 8 s aparece el aviso «La conexión está lenta» con un botón «Reintentar» de 48 px. Cada petición ya expira a los 15 s, así que la carga siempre termina. |
| 3 | Si el alumno no tenía rutas, la app lo inscribía automáticamente en una ruta de Matemática al cargar la página, incluso de otro grado, por el error 1. | Ya no se inscribe a nadie al cargar. El estado «Aún no tienes rutas» ofrece «Explorar rutas de mi grado» y «Consultar mis inscripciones». Inscribirse sigue siendo explícito al elegir una ruta del catálogo. |
| 4 | Las pruebas de Node del frontend no tenían comando. | Nuevo script `npm test` (`node --experimental-strip-types --test tests/*.test.ts`), con 21 pruebas en verde. |

### Pendientes (no corregidos aún)

| # | Problema | Propuesta |
|---|---|---|
| 5 | La categoría Comunidad se decide por `r.codigo LIKE 'RO-DOC-%' OR nombre_usuario LIKE 'profesor%'` (`LearningRouteRepository`, duplicado en dos consultas). | Usar el rol del propietario o una columna `origen` en `aprendizaje_ruta` (migración V16). |
| 6 | La sesión asigna 2.° de primaria al alumno sin aula. Si el alumno tiene varias aulas activas, el grado y el colegio se toman de una fila cualquiera. | Elegir la membresía con una regla fija y devolver `null` cuando no haya grado. |
| 7 | V09 y V10 se editaron después de publicarse. V12 define `recurso_archivo_multimedia` con columnas distintas a `schema.sql`; su `CREATE IF NOT EXISTS` no tiene efecto, pero confunde. | No editar migraciones publicadas: cada corrección va en una V nueva. Alinear o retirar el bloque de V12. |
| 8 | HU-13: solo el autor de la ruta puede asignar fechas, así que un docente no puede calendarizar una ruta oficial para su aula. | Decisión de producto pendiente con el equipo. |
| 9 | Las contraseñas demo (`123456`) aparecen en `LoginView.tsx` y en los scripts `Probar-*.ps1`. | Mostrar el acceso demo solo si `demoAvailable` es verdadero y leer la clave de una variable de entorno. |
