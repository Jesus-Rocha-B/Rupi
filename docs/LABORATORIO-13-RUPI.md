# Laboratorio 13 aplicado a Rupi

## Criterio de adaptación
Rupi es una plataforma educativa, no una tienda. Se adapta el flujo solicitado al registro y corrección de metas curriculares (HU-50), usando dominios reales del producto. La consola React llama a Django; la base compartida es TiDB compatible con MySQL por decisión arquitectónica del equipo. No se crea un sistema paralelo ni se usa SQLite en el desarrollo normal.

## Tipo, canales, roles y módulos
Rupi se despliega institucionalmente (B2B2C: instituciones habilitan la plataforma para docentes y estudiantes; esto describe su modelo de adopción, no ventas implementadas). Los canales son web React y Android Kotlin. El administrador gestiona cuentas, permisos, currículo, moderación y monitoreo; docentes organizan el aprendizaje; estudiantes recorren rutas y actividades.

| Módulo | Responsabilidad | Implementación |
|---|---|---|
| Identidad | Cuentas, roles, permisos, sesiones | Django, `identidad_*` |
| Catálogo MINEDU | Versiones, grados, áreas, competencias y unidades | Django, HU-49 |
| Metas y estándares | Registrar/corregir preservando vínculos | Django, HU-50 |
| Periodos escolares | Calendarios, validación y aplicación | Django, HU-52 |
| Aprendizaje | Rutas, paradas, lecciones, progreso | Spring Boot y React/Android |
| Moderación IA | Reglas, evaluación y publicación | Django y servicio IA |
| Operaciones | Auditoría, salud, incidencias y sincronización | Django/Spring por dominio |

## Requisitos verificables
| ID | Tipo | Requisito | Prioridad | Aceptación |
|---|---|---|---|---|
| RF-01 | Funcional | Consultar catálogo curricular con permiso | Alta | GET devuelve árbol JSON; sin permiso, 403 |
| RF-02 | Funcional | Filtrar unidades por versión | Alta | GET `/api/admin/v1/curriculo/unidades?version={uuid}` devuelve arreglo ordenado |
| RF-03 | Funcional | Registrar meta/estándar ligado a unidad y competencia | Alta | POST devuelve 201 y conserva ambos vínculos |
| RF-04 | Funcional | Corregir descripción sin cambiar relaciones | Alta | PATCH exige motivo y revisión; conserva código, tipo, grado, unidad y competencia |
| RF-05 | Funcional | Configurar periodos escolares y previsualizar aplicación | Alta | Fechas cruzadas se rechazan y aplicar requiere vista previa vigente |
| RNF-01 | No funcional | 95% de lecturas del catálogo en menos de 2 s con DB de pruebas | Media | Medir 20 consultas autenticadas |
| RNF-02 | No funcional | Las escrituras se autorizan en servidor | Alta | Sin sesión 401; sin permiso 403 |
| RNF-03 | No funcional | Limitar JSON a 32 KiB y responder errores útiles | Media | Cuerpo sobredimensionado 413; inválido 400 con `detail` |

## Reglas de negocio y capa
| Regla | Aplicación |
|---|---|
| Solo `curriculo.gestionar` puede crear/corregir | Django, `curriculo/vistas.py` |
| Meta/estándar debe pertenecer a unidad y competencia válidas | `curriculo/metas.py` y base de datos |
| Corrección no altera código, tipo ni vínculos; exige motivo auditable | Django dentro de transacción |
| Revisión desactualizada produce conflicto 409 | Servicio de metas Django |
| Catálogo retirado queda solo lectura | Validación de Django |
| Periodos no se cruzan ni exceden el calendario | Cliente ayuda; Django decide y valida |
| React no escribe directamente `curriculo_*` | Django único escritor del dominio |

## Arquitectura y operación principal
```text
Administrador → React /admin → Axios src/api.js → Django URLConf/DRF Router
→ UnidadViewSet → FiltroUnidadesSerializer → ORM y relaciones → JSON/HTTP
→ React actualiza CurriculumGoalsView

Guardar/corregir meta: React → Axios → vista Django → permiso, JSON, reglas y revisión
→ transacción (SQL parametrizado del dominio y ORM para auditoría) → TiDB/MySQL
→ respuesta JSON 201/200 o error HTTP → React actualiza estado.
```
Entidades principales del recorrido: `VersionCatalogo`, `Grado`, `Area`, `Competencia`, `Unidad`; `MetaCurricular` y `EstandarCurricular` son detalles vinculados a unidad y competencia. En aprendizaje, `Ruta` agrupa `Paradas`.

## Estructura y rutas
`frontend/web-react/src/pages` presenta LearningPage/AdminPage; `components` comparte UI; `features/admin` implementa pantallas; `services/adminApi.ts` tipa operaciones; `api.js` centraliza Axios. Django registra `core`, `rest_framework`, `identidad`, `operaciones` y `curriculo`; `moderacion` conserva endpoints existentes. Modelos curriculares son no gestionados; el esquema se versiona en `database/mysql/migrations`.

| UI | Pantalla | API | Método/función |
|---|---|---|---|
| `/` | LearningPage | `/api/...` Spring | Aprendizaje |
| `/admin` | AdminPage → AdminApp | `/api/admin/v1/sesion` | Ingreso de administrador |
| Admin / Catálogo | CurriculumGoalsView | `/curriculo/catalogo` | GET árbol curricular |
| Admin / Catálogo | CurriculumGoalsView | `/curriculo/unidades?version={uuid}` | GET DRF Router, ViewSet y ORM |
| Admin / Catálogo | CurriculumGoalsView | `/curriculo/unidades/{uuid}/registros` | GET/POST vistas Django |
| Admin / Catálogo | CurriculumGoalsView | `/curriculo/unidades/{uuid}/registros/{tipo}/{uuid}` | PATCH corrección |
| Admin / Periodos | CurriculumPeriodsView | `/curriculo/periodos/...` | GET/PUT/revisar/aplicar |

## Comprobaciones y diferencias
La base compartida es TiDB/MySQL, no SQLite. El listado de unidades usa Axios, DRF Router, ViewSet, Serializer y ORM; el resto de rutas conserva vistas propias para compatibilidad con Rupi. `manage.py check`, lint y build se deben registrar con su salida al preparar la entrega. `node_modules` y `dist` están ignorados por Git. Agregar la URL vigente del repositorio al informe final; nunca incluir credenciales.
