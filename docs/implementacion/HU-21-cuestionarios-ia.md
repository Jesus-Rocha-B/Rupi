# HU-21 · Cuestionarios cortos asistidos por IA

Implementado el 26 de octubre de 2026. Sin proveedor ni clave de IA configurados: la generación real está deshabilitada. No se han generado cuestionarios reales ni introducido ejemplos en TiDB.

## Tareas y recorrido
1. Definir preguntas desde el tema, grado, área y contenido de un nodo accesible.
2. Generar y revisar automáticamente entre tres y cinco preguntas; permitir revisión docente.
3. Guardar respuestas y mostrar retroalimentación formativa.

Web: estudiante → ruta → lección disponible → Practicar con Rupi. Profesor → reportes del aula y ruta → revisión de cuestionarios. Android: iniciar sesión como estudiante → elegir tema accesible → practicar. La práctica no modifica notas oficiales ni alertas HU-16 a HU-20.

## Arquitectura y persistencia
Spring Boot controla autorización, contexto, generación y corrección. React y Kotlin llaman únicamente a la API de Rupi. La migración V23 agrega evaluacion_cuestionario_ia, evaluacion_practica_ia, evaluacion_limite_ia y evaluacion_reporte_pregunta_ia. Fue probada en MySQL aislado y aplicada a TiDB compartido; las cuatro tablas quedaron vacías. No modifica tablas ni datos anteriores.

Las preguntas, alternativas, intentos y respuestas reutilizan evaluacion_* existente. Cada cuestionario tiene una versión inmutable de actividad separada de los nodos oficiales, contexto, proveedor, modelo y versión de instrucciones. Las soluciones permanecen ocultas hasta responder. Cada alumno solo consulta sus prácticas; la revisión docente se limita a sus aulas/rutas.

UUID de solicitud, bloqueo transaccional y un intento activo por nodo evitan duplicados. Límite de cinco solicitudes por hora y estudiante; generación caducada a los dos minutos recuperable. Dos llamadas acotadas al proveedor (generación y revisión), sin reintentos automáticos. Si falla, solo se reutiliza material revisado por docente del mismo contexto. Los reportes retiran el cuestionario hasta revisión.

## Configuración pendiente
Configurar PRIVADAMENTE en el entorno de Spring Boot:
- RUPI_AI_ENDPOINT: URL HTTPS completa de un endpoint compatible con Chat Completions.
- RUPI_AI_PROVIDER: nombre real del proveedor.
- RUPI_AI_MODEL: identificador de modelo compatible con JSON mode.
- RUPI_AI_KEY: credencial secreta, nunca en Git, React, Kotlin ni capturas.

Reiniciar backend después de configurar. No hay proveedor contratado ni acceso supuesto a Tecsup. Verificar compatibilidad, tratamiento de datos y costos del proveedor elegido. Solo se envían grado, área, tema y contenido educativo; no el registro del alumno. El contenido debe ser educativo y no contener información personal. La limpieza de correos y números no sustituye esa revisión editorial.

Validación estructural más segunda revisión automática de pertinencia, lenguaje y respuestas; esto NO garantiza exactitud pedagógica. El docente puede aprobar o rechazar y los alumnos reportar errores.

Referencia de protocolo: https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=chat

## Verificación
- Backend: 241 pruebas descubiertas; 198 ejecutadas correctamente y 43 omitidas por configuración. Incluye 11 pruebas SQL aisladas y 10 de validación/proveedor simulado.
- TiDB: prueba adicional exclusivamente SELECT aprobada; tres temas accesibles y cuatro tablas disponibles.
- Web: 27 pruebas, build y lint correctos; advertencia existente de tamaño de bundle.
- Navegador con API simulada: proveedor deshabilitado, generación, respuesta, recuperación tras recarga y vistas 375/1280 px; revisión visual corrigió ancho de radios.
- Kotlin: fuentes compiladas con Kotlin 2.3.0 y stubs Android. No equivale a APK ni ejecución en dispositivo.
- Gradle assembleDebug bloqueado por IOException: Unable to establish loopback connection. SDK/emulador no verificados; validación nativa completa pendiente.
- Sin pruebas reales de IA porque falta proveedor/clave.

## Entrega
Commit local en rama codex/hu21-cuestionarios-ia. No publicar en GitHub hasta autorización expresa. Las pruebas ficticias permanecen únicamente en rupi_hu21_test local y en mocks.
