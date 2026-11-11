# HU-53 · Configurar reglas estrictas de evaluación de lenguaje

Acceso: Administración → Moderación IA. Conserva el logo original, las tablas compactas y la paleta verde, amarillo maíz y naranja del diseño aprobado.

## Flujo administrativo

1. Las cuatro protecciones base (respeto, contenido para menores, privacidad y orientación segura) permanecen activas. No hay un interruptor que permita desactivar la protección infantil.
2. El administrador agrega hasta 20 frases literales por categoría y elige uno de los mensajes alternativos revisados. El alcance inicial es toda primaria; no se muestran selectores de grado que no tengan efecto.
3. Guarda el borrador en TiDB. Los cambios aún no afectan a los proveedores.
4. Prueba un ejemplo manual y ejecuta la batería de casos respetuosos, humillantes, privacidad, contenido adulto y conductas peligrosas. La batería debe pasar antes de publicar; una edición invalida las pruebas anteriores.
5. Publica la revisión. Se guarda una instantánea con versión, autor y fecha. Publicar otra vez el mismo contenido no duplica el historial. La revisión optimista y el bloqueo transaccional evitan sobrescrituras.

El permiso `moderacion.ver` permite consultar y evaluar ejemplos; `moderacion.gestionar` permite guardar, ejecutar pruebas registradas y publicar. La migración asigna ambos al administrador y solo consulta a soporte. Los ejemplos manuales no se persisten ni se envían a proveedores.

## Alcance y límites del filtro

Es un **filtro determinista de patrones**, no un clasificador semántico ni una garantía de seguridad total. Normaliza mayúsculas, tildes, espacios y caracteres de ancho cero; desescapa cadenas JSON; usa límites de palabra y escapa las frases adicionales. Puede producir falsos positivos en citas educativas y omitir formulaciones nuevas, otros idiomas, ofuscación o contexto. No evalúa imágenes, exactitud académica ni intenciones. La interfaz muestra esta limitación: se mantiene la revisión docente y, al contar con proveedor, deben añadirse evaluaciones semánticas especializadas.

Sin publicación todavía se aplica la protección base. Los borradores se evalúan solo en la consola. Las nuevas respuestas generadas se verifican contra la versión publicada en cada llamada, sin caché de reglas. El material revisado ya guardado y las pistas anteriormente almacenadas en caché no se vuelven a clasificar con esta HU.

## Integración real

- Django es el único escritor de `ia_configuracion_lenguaje` e `ia_revision_lenguaje`, además de la auditoría administrativa. No altera las tablas de conversaciones o las reglas antiguas reservadas para otros flujos.
- `ChatQuizProvider` de Spring verifica cada salida del proveedor antes de devolver JSON al generador de cuestionarios, pistas y resúmenes. Un bloqueo o fallo de verificación rechaza esa salida y conserva los mecanismos existentes de contenido revisado/error controlado.
- `gemini_client` verifica la salida usada por el tutor y el generador de roadmaps. Una respuesta de chat bloqueada se sustituye por la alternativa revisada. En JSON se rechaza la generación completa para no presentar un roadmap roto.
- Ambos clientes usan HTTP interno con tiempo límite, sin redirecciones. Exigen `permitida: true` booleano. Error HTTP, respuesta inválida, ausencia de token o caída del evaluador no permiten entregar el texto generado sin verificar.
- No activa ni simula un proveedor de IA. Puede probarse la moderación sin clave y usar el banco existente como antes.

## Contrato

Base administrativa `/api/admin/v1`, cookie existente y Origin permitido en escrituras:

| Ruta | Solicitud / resultado |
|---|---|
| GET `moderacion/reglas` | Configuración del borrador, revisión, versión publicada, pruebas, casos e historial |
| PUT `moderacion/reglas` | `{revision, configuracion:{extras:{RESPETO:[],MENORES:[],PRIVACIDAD:[],SEGURIDAD:[]},alternativa}}` |
| POST `moderacion/evaluar` | `{revision,texto}` → `permitida`, reglas coincidentes, alternativa y aviso de limitaciones |
| POST `moderacion/probar` | `{revision}` → resultados de batería y estado actualizado |
| POST `moderacion/publicar` | `{revision}` → estado e historial; 409 si falta probar o la revisión es antigua |

Endpoint exclusivo entre servidores: POST `/api/internal/moderacion/evaluar` con `{texto}` y `X-Rupi-Monitor-Token`. Usa las reglas publicadas y devuelve la decisión y versión. Token ausente/incorrecto → 401; base no disponible → 503. Nunca enviar ese token al navegador. El cuerpo comparte el límite HTTP de 32 KiB de Django; el motor acepta hasta 30000 caracteres, y cualquier exceso se rechaza, no se omite la evaluación.

## Actualizar los equipos

1. Aplicar V42 de `database/mysql/migrations` una sola vez en la base compartida (idempotente). Inicializa un borrador y permisos; no publica cambios de un administrador ficticio.
2. Con usuarios MySQL restringidos, ejecutar `grants-django-admin.sql`: ahora incluye las dos tablas nuevas de IA. Los otros servicios acceden a la política por HTTP, no escriben esas tablas.
3. Django, Spring y el servicio IA deben compartir **en cada equipo** el mismo `RUPI_MONITOR_TOKEN` privado. El lanzador principal ya lo carga desde `.local/monitor.token`; el lanzador independiente de IA también lo carga. No es la clave del proveedor.
4. Configurar `RUPI_ADMIN_URL` con la dirección de Django en ese equipo. Para este entorno es `http://127.0.0.1:8182`. Si es un host remoto, usar HTTPS. No se modifican servicios ajenos en otros puertos.
5. Reiniciar Django, compilar/reiniciar Spring y reiniciar servicio IA. Actualizar React. Consultar la nueva sección con una cuenta administrativa.

## Evidencia y pruebas reproducibles

- Django: `python manage.py test moderacion curriculo`.
- Spring: `mvn -Dtest=ModerationClientTest,ChatQuizProviderTest,SummaryProviderTest,QuizHintServiceTest,QuizValidationTest test`.
- Servicio IA: instalar `requirements-dev.txt` y ejecutar `python -m unittest discover -p "test*.py"`.
- React: `npm run build`.
- Los clientes se prueban contra servidores HTTP temporales reales, con aprobaciones, bloqueos, 503 y JSON inválido. No se llama a proveedores externos ni se consumen claves. Postman CLI no está instalado; se usan las pruebas del repositorio.
- Integración transaccional TiDB con rollback: borrador separado de la versión activa, publicación bloqueada sin pruebas, publicación efectiva e idempotente y restauración de datos originales.
- Navegador con respuestas controladas: guardar, evaluar, ejecutar pruebas, publicar, consultar historial, soporte sin escritura y tamaños 390/768 px. No se presentó esa prueba como una sesión administrativa real con contraseña.
