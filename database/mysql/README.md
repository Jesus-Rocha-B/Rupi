# Base de datos MySQL de RUPI

## Para qué sirve este documento

Este README describe el modelo de datos que servirá a RUPI y cómo lo usarán Django y Spring Boot. El DDL completo está en [`schema.sql`](schema.sql), el diagrama para dbdiagram.io en [`rupi.dbml`](rupi.dbml) y las decisiones relacionales ampliadas en [`modelo.md`](modelo.md).

## Estado y alcance

- Nombre de esquema: `rupi`.
- Motor objetivo: MySQL 8.4 con InnoDB y `utf8mb4`.
- El DDL define 77 tablas para la plataforma completa, considerando las 60 historias del backlog general. Es el modelo objetivo; no significa que todas las funciones estén implementadas.
- `schema.sql` crea el esquema y agrega los seis grados. No contiene todavía una ruta completa con actividades ni progreso real.
- El SQL se revisó estructuralmente, pero todavía debe ejecutarse y validarse en una instancia MySQL 8.4.

No se crean tablas nuevas solo para satisfacer las primeras cuatro historias: el modelo ya contempla rutas, progreso y catálogo. Las historias avanzarán por etapas, aplicando únicamente lo requerido y documentando cualquier cambio al modelo.

## Arquitectura y propiedad de escritura

```text
frontend/web-react ──────────────┐
frontend/mobile-kotlin ──────────┼──> backend/api-spring-boot ──┐
                                │                              │
React de administración ───────────> backend/admin-django ─────┼──> MySQL `rupi`
                                                               │
                  cada dominio tiene un solo servicio escritor ┘
```

React web de estudiantes/docentes y Kotlin consultan la API Spring Boot. La aplicación React administrativa se conecta a Django. Compartir MySQL no significa que ambos backends puedan modificar cualquier tabla: cada tabla tiene un único propietario.

| Dominio | Servicio escritor | Responsabilidad principal |
|---|---|---|
| Cuentas, roles y permisos | Django | Identidad, asignación de roles y autorización administrativa. |
| Catálogo curricular oficial | Django | Versiones del catálogo, grados, áreas, competencias, unidades y metas oficiales. |
| Escuelas, aulas y membresías | Spring Boot | Organización del aula y participación de estudiantes/docentes. |
| Rutas, nodos y actividades | Spring Boot | Rutas de aprendizaje, versiones, nodos y contenido. |
| Inscripción y progreso | Spring Boot | Inscripción por estudiante, avance por nodo y sesiones de estudio. |
| Evaluación, tareas y gamificación | Spring Boot | Intentos, respuestas, entregas, XP, insignias y metas. |
| IA, notificaciones y sincronización | Spring Boot | Conversaciones moderadas, mensajes, preferencias y eventos del cliente. |
| Configuración global de IA | Django | Políticas y referencias a secretos de proveedores. |

El servicio sin propiedad de escritura solo lee los datos que su función requiere. Para producción se configurarán permisos mínimos por servicio. No se habilitará `ddl-auto=update`, migración automática de ORM ni dos ejecutores que alteren el mismo esquema.

### Migraciones

`schema.sql` es la referencia inicial del modelo. Antes de operar la base compartida se debe elegir un solo mecanismo/propietario de migraciones, fijar el orden para Django y Spring Boot y acordar cómo se coordinan cambios compatibles. Los cambios se guardan como migraciones versionadas y revisadas; no se ejecuta DDL destructivo directamente en producción.

## Modelo por dominios

| Prefijo o grupo | Datos cubiertos |
|---|---|
| `identidad_*` | Cuenta, perfil, roles, permisos, sesiones, MFA, intentos de acceso y recuperación de cuenta. |
| `escuela_*` | Institución, año escolar, aula, membresía, consentimiento de tutor y periodo académico. |
| `curriculo_*` | Versiones de catálogo, grados, áreas, competencias, unidades, metas y estándares. |
| `aprendizaje_*` | Actividades/versiones, bloques, rutas/versiones, nodos, códigos, inscripciones, progreso, sesiones, tareas, entregas, retos y repasos. |
| `evaluacion_*` | Preguntas, opciones, intentos, respuestas y opciones seleccionadas. |
| `recurso_archivo_multimedia` | Metadatos, ubicación, tipo, tamaño, hash y texto accesible de recursos; el binario vive fuera de MySQL. |
| `gamificacion_*` | Metas, movimientos XP, insignias y accesorios/inventario. XP se conserva como movimientos, no como saldo fuente duplicado. |
| `notificacion_*` | Preferencias, programación, dispositivos, mensajes y entregas por canal. |
| `ia_*` | Políticas, configuración externa referenciada, conversaciones, mensajes, generación y moderación. |
| `operaciones_*` | Metadatos de sincronización, respaldos, recuperación, incidentes y eventos de auditoría de bajo volumen. |

El archivo `rupi.dbml` contiene las tablas y relaciones completas. `modelo.md` explica la normalización e invariantes que también requieren validación de negocio.

## Soporte de las primeras cuatro historias

Las HU-01 a HU-04 son experiencias web de estudiante. React no accede directamente a MySQL: solicita datos a Spring Boot. Django administra el catálogo oficial, pero no escribe las tablas de rutas ni progreso.

| Tabla | Uso en HU-01–HU-04 |
|---|---|
| `identidad_cuenta_usuario` | Identidad del estudiante propietario de la inscripción. |
| `identidad_sesion_usuario` | Sesión revocable que permite autenticar al estudiante. |
| `curriculo_version_catalogo` | Versión del currículo usada por la ruta. |
| `curriculo_grado`, `curriculo_area` | Nivel y área asignados a la ruta. |
| `curriculo_competencia`, `curriculo_unidad`, `curriculo_unidad_competencia` | Competencias y unidades curriculares. |
| `aprendizaje_ruta` | Identidad estable y título general de una ruta. |
| `aprendizaje_version_ruta` | Edición publicada vinculada con catálogo, grado y área. |
| `aprendizaje_nodo_ruta` | Paradas, orden, actividad, unidad, prerrequisito y coordenadas del mapa. |
| `aprendizaje_actividad`, `aprendizaje_version_actividad` | Tipo y contenido versionado de la actividad de cada parada. |
| `aprendizaje_bloque_contenido` | Partes ordenadas del contenido que se abrirá en HU-02. |
| `aprendizaje_inscripcion_ruta` | Inscripción del estudiante, estado y último nodo visitado para HU-03. |
| `aprendizaje_progreso_nodo` | Estado por nodo e inscripción: bloqueado, disponible, en curso o completado. |
| `aprendizaje_sesion_estudio` | Sesiones de estudio cuando la continuidad requiera más detalle que el último nodo. |
| `aprendizaje_actividad_unidad` | Relación de actividades con unidades. |

### Avance calculado

El total de nodos se obtiene de `aprendizaje_nodo_ruta`; los completados, de `aprendizaje_progreso_nodo`. El porcentaje se calcula al consultar y presentar esos conteos. No se guarda como una segunda fuente que pueda desactualizarse.

Para que HU-01 muestre un mapa completo, la inscripción debe tener una fila de progreso por nodo. La operación que crea la inscripción debe inicializar esos estados en una transacción. Los estados iniciales y las reglas para desbloquear nodos se validan en Spring Boot; una lectura no inventa progreso ausente.

### Alineación curricular

Cada versión de ruta apunta a una versión de catálogo, grado y área. Un nodo se puede asociar a una unidad mediante `unidad_id`; la unidad mantiene `numero_secuencia`. HU-04 valida que cada unidad pertenezca al mismo catálogo, grado y área que la ruta, ordena las unidades por su secuencia curricular y conserva el orden de nodos dentro de cada unidad. Las FK actuales no expresan todas esas reglas entre tablas; Spring Boot y el proceso de publicación las verifican.

## Normalización e integridad

- Entidades con ciclos de vida propios están separadas: cuenta/perfil, ruta/versión, actividad/versión, inscripción/progreso.
- Relaciones muchos-a-muchos usan tablas puente, por ejemplo unidad/competencia y versión de actividad/unidad.
- Roles, opciones, preferencias, miembros y otras relaciones repetibles no se guardan como listas dentro de una columna.
- El nombre de grado, área o actividad se mantiene en su catálogo/versión y no se repite en cada progreso.
- La ruta y las actividades versionadas permiten conservar la experiencia publicada mientras se prepara una edición nueva.
- Claves foráneas, restricciones `CHECK`, índices únicos y claves compuestas acotan combinaciones inválidas.
- Categorías pequeñas y estables usan `ENUM`; los catálogos de negocio versionables usan tablas.

La aplicación debe validar además la autorización por aula, la pertenencia curricular de unidades, la publicación válida y el desbloqueo secuencial. Las FK por sí solas no ofrecen aislamiento entre instituciones.

## Identificadores, texto y fechas

- Los IDs usan `CHAR(36)` con collation ASCII binaria para interoperar entre Java, Kotlin y Django.
- Las fechas usan `DATETIME(6)` en UTC; la zona horaria del usuario se configura por separado.
- MySQL usa InnoDB y `utf8mb4` para contenido en español.
- Antes de producción se probará la representación UUID y semántica de fechas en ambos backends.

## Privacidad y operación

- Contraseñas, sesiones, códigos de acceso/recuperación y tokens push nunca se guardan en claro. Los valores verificables se guardan como hash; los secretos recuperables usan cifrado y una clave administrada fuera de la base.
- La configuración de IA guarda una referencia a secretos externos, no la API key.
- Las primeras cuatro historias no requieren DNI, domicilio ni fecha de nacimiento del estudiante.
- Conversaciones y moderación de IA para menores requieren controles de acceso, retención y eliminación antes de habilitarse.
- Archivos y respaldos viven en almacenamiento externo; MySQL conserva metadatos o referencias.
- Métricas de infraestructura de alta frecuencia viven en monitoreo externo.
- Cada ambiente usa credenciales separadas y con privilegios mínimos. No se versionan archivos `.env` reales.

## Preparación para desarrollo

1. Provisiona MySQL 8.4 y crea credenciales locales con privilegios mínimos.
2. Revisa [`schema.sql`](schema.sql) y [`modelo.md`](modelo.md); no lo apliques sobre datos existentes sin respaldo y revisión.
3. Aplica el modelo a una base vacía usando el mecanismo de migración acordado. El archivo inicializa seis grados, pero no crea estudiantes, aulas ni una ruta de ejemplo.
4. Carga un catálogo versionado y un conjunto pequeño de actividades, ruta, unidades y nodos de desarrollo, sin datos personales reales.
5. Crea la inscripción y las filas de progreso con una operación transaccional de Spring Boot.
6. Configura `RUPI_DB_URL`, `RUPI_DB_USERNAME` y `RUPI_DB_PASSWORD` para `backend/api-spring-boot`; Django tendrá sus variables propias. Activa TLS según el proveedor.
7. Comprueba restricciones y permisos en MySQL objetivo antes de conectar la web.

## Archivos relacionados

- [`schema.sql`](schema.sql): DDL MySQL integral y grados iniciales.
- [`rupi.dbml`](rupi.dbml): diagrama para dbdiagram.io.
- [`modelo.md`](modelo.md): relaciones, normalización, invariantes y decisiones pendientes.
- [`../../docs/implementacion/README.md`](../../docs/implementacion/README.md): plan detallado de HU-01 a HU-04.
