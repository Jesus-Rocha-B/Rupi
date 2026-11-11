# HU-54 · Consultar el historial de interacciones de IA

## Uso
Administración → Moderación IA → Historial IA. Lista paginada de 20 registros, búsqueda por inicio del UUID, fechas inclusivas en hora de Perú, resultado y estado de revisión. Los tres indicadores usan los filtros aplicados; el promedio excluye duraciones desconocidas.

El detalle se abre en un panel lateral modal con Conversación, Análisis IA y Trazabilidad. Incluye proveedor, modelo solicitado, duración de generación, versión de reglas y coincidencias. No inventa confianza, análisis semántico ni identidad del estudiante. La revisión es idempotente y no libera respuestas bloqueadas. Escape cierra el panel y devuelve el foco; controles compactos para administración.

## Alcance real del registro
- Las nuevas respuestas del adaptador ChatQuizProvider (Spring: recursos JSON y revisiones) y Gemini (tutor y recursos JSON) se evalúan y registran en Django antes de ser devueltas. Si la evaluación o persistencia falla, no se libera la respuesta.
- Registra respuestas generadas, no confirma entrega o visualización del estudiante. La alternativa de política solo se entrega en el tutor de texto; el generador JSON rechaza la salida bloqueada.
- El tutor conserva la pregunta actual (máximo 2000 caracteres). En los recursos JSON no se guarda el prompt de sistema, archivos adjuntos ni todo el contexto curricular. La pestaña explica cuando no hay pregunta individual.
- No se reconstruyen interacciones anteriores, cachés, bancos locales, fallos de proveedor previos a obtener una respuesta ni pruebas manuales de HU-53. El historial estará vacío mientras no se generen respuestas reales con un proveedor configurado.
- Los servicios actuales no aportan una identidad autenticada al adaptador de IA: se muestra «Sin identidad vinculada», sin atribuir mensajes a un alumno a partir del cuerpo enviado por el navegador.

## Acceso y datos
Django es el único escritor de `ia_interaccion_auditoria`. Spring e IA solo envían el contenido al endpoint interno protegido por `X-Rupi-Monitor-Token`. No se exponen nuevas credenciales al navegador.

Permisos nuevos: `interacciones.ver` y `interacciones.revisar`, asignados inicialmente solo a ADMINISTRADOR. El acceso a la sección sigue requiriendo `moderacion.ver`. Soporte no accede automáticamente al historial. Cada búsqueda, apertura y revisión registra un evento en `operaciones_evento_auditoria`; el detalle muestra hasta 50 eventos recientes. Consultas HTTP con `Cache-Control: private, no-store`.

Se enmascaran patrones de correo, secuencias numéricas personales y secretos etiquetados antes de guardar. Es una protección básica, no anonimización garantizada. Los textos se presentan como texto React, nunca como HTML. No se incluye exportación masiva. No hay eliminación automática implementada; la política de conservación debe acordarse antes de desplegar datos reales de menores.

## Despliegue
1. Aplicar `database/mysql/migrations/V43__historial_interacciones_hu54.sql` sobre la base compartida. Es aditiva e idempotente, sin registros de ejemplo.
2. Si Django usa cuenta limitada, conceder SELECT, INSERT y UPDATE en `ia_interaccion_auditoria`; el script MySQL de grants incluye la tabla. Mantener INSERT en auditoría y lectura de identidad.
3. Actualizar juntos Django, Spring y servicio IA. Conservan el token interno y URL administrativa existentes. Reiniciar los servicios.
4. Actualizar React. Iniciar sesión como administrador y abrir Moderación IA → Historial IA.

## API
- GET `/api/admin/v1/moderacion/interacciones`: q, desde, hasta, resultado (permitida/bloqueada), revision (pendiente/revisada), pagina. SQL parametrizado y paginación limitada.
- GET `/api/admin/v1/moderacion/interacciones/{uuid}`: detalle restringido.
- POST misma URL: marcar revisada; requiere además `interacciones.revisar` y Origin permitido.
- POST interno `/api/internal/moderacion/evaluar`: texto y auditoria opcional (origen, recurso, proveedor, modelo, duracionMs, pregunta). El UUID lo genera Django. Evalúa texto original, guarda copia protegida y devuelve decisión e interaccionId.

## Verificación
- Django: `python manage.py test moderacion` (validación, privacidad y permisos).
- IA: `python -m unittest test_servicio_ia test_moderation` (HTTP interno, metadatos, bloqueo ante fallo).
- Spring: pruebas ModerationClientTest, ChatQuizProviderTest, QuizHintServiceTest y SummaryProviderTest.
- Build React y comprobación de navegador: filtros, detalle, revisión, Escape/foco y tamaños 390/768/1440 px. Datos de navegador simulados solo para pruebas.
- Integración TiDB con transacción revertida: permitido/bloqueado, redacción, filtros, duración, revisión idempotente y eventos. Ninguna interacción de prueba permanece en la base compartida.
