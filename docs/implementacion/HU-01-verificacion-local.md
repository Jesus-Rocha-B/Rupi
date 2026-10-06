# HU-01: verificación local y límites

## Diseño y comportamiento

Se conserva la ambientación peruana del mapa y el gallito de las rocas. El resumen muestra el nombre obtenido de la sesión, el porcentaje calculado y las cantidades por estado desde MySQL. En móvil, las paradas forman un recorrido vertical con etiquetas legibles; en escritorio se conserva el trazado del mapa. El detalle utiliza un diálogo nativo con foco contenido, cierre con Escape y retorno al botón original.

Paleta: bosque #173e30, hoja #31724b, cielo #246a91, naranja del personaje y sol #ffd26a. Se reutilizan las familias tipográficas existentes y se aumenta la lectura de las etiquetas. No se ofrecen premios XP ficticios ni botones para iniciar actividades todavía no implementadas.

Las rutas inscritas se ordenan por grado, área, título e identificador. Las solicitudes se cancelan al cambiar de vista y tienen un límite de espera. El progreso del detalle se deriva de los mismos nodos leídos en una transacción. Si falta progreso de un nodo, la API devuelve 503 en vez de inventar un bloqueo.

## Seguridad aplicada

- Se rechazan UUID enviados como Bearer y el encabezado X-Student-Id.
- Se valida el hash SHA-256 del token contra una sesión de MySQL vigente, no revocada y de una cuenta activa.
- Cookie HttpOnly, SameSite=Strict y Secure cuando la conexión es HTTPS; ningún token se almacena en localStorage ni en React.
- El acceso demo requiere configuración explícita del servidor, conexión loopback, host local y origen coincidente en POST.
- Respuestas API con Cache-Control: no-store y nosniff. CORS acotado a los dos orígenes locales del frontend.
- Consultas parametrizadas, autorización por inscripción y usuario SQL de lectura.

## Evidencia

- 10 pruebas unitarias Java sin fallos.
- Compilación web y ESLint correctos.
- `scripts/Verificar-HU01.ps1` (PowerShell 7) comprueba 10 respuestas HTTP contra MySQL: sin sesión, UUID y encabezado falsos, lista vacía, ruta ajena, progreso incompleto, aislamiento de progreso, inscripción suspendida, sesión vencida y revocada. Crea y elimina exclusivamente sus propios registros temporales.
- EXPLAIN de lectura de nodos: índice uq_nodo_orden (ref) y PRIMARY del progreso (eq_ref). No se añadieron índices redundantes. Datos de prueba pequeños: no es una prueba de carga.
- Revisión móvil a 390 px: sin desbordamiento horizontal, nodo disponible abre detalle, Escape restaura el foco.

## Pendientes antes de producción

La demostración local no sustituye el sistema de identidad de Django: falta implementar el ingreso real, emisión/rotación/revocación de sesiones y recuperación de cuentas. El botón de salida elimina la cookie del navegador; no revoca el token compartido de la demostración. La sesión demo local vence a los 30 días de su creación. No habilitar RUPI_DEMO_SESSION_TOKEN en producción.

Falta validar el despliegue con HTTPS, política CSP del servidor de archivos estáticos, límites de solicitudes y gestión operacional de secretos. Estas comprobaciones no equivalen a una auditoría de seguridad completa. HU-02 (iniciar actividades), HU-03 y HU-04 siguen pendientes.
