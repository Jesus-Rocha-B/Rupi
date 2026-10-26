# HU-23 · Infografías y resúmenes visuales

> Actualizacion posterior: APK compilado y probado en emulador con datos aislados. Falta proveedor/clave para validar IA real. Ver [validacion actual](VALIDACION-ANDROID-Y-CONFIGURACION-IA.md). Los limites Android de abajo corresponden a la entrega inicial.

Implementación: 26 de octubre de 2026.

## Tareas
1. Resumir el contenido en una infografía adecuada al grado.
2. Mostrar el recurso visual junto al tema correspondiente.
3. **Guardar los recursos para el repaso posterior y su consulta sin conexión.** Esta es la tarea adicional.

## Recorrido y estado real
Estudiante web: ruta → lección disponible → Ver resumen visual. Preparar resumen cuando haya proveedor configurado; leer conceptos, pasos, ejemplo y recordatorio. Se incluye texto continuo, descarga y reporte.
Profesor: reportes → aula/ruta → Revisión docente de resúmenes visuales. Aprobar o retirar con una nota de revisión.
Kotlin: tema → Ver resumen visual → Guardar para consultar sin conexión. Las copias se abren desde Resúmenes descargados, incluso sin sesión; solo contienen contenido educativo compartido, nunca credenciales ni datos del estudiante.

No hay proveedor ni clave de IA configurados. La generación real permanece deshabilitada. No se generaron recursos reales ni se insertaron ejemplos ficticios en TiDB; los ejemplos usados en las pruebas son locales y simulados.

## Arquitectura
Se reutiliza el transporte de ChatQuizProvider (HU-21) y su configuración privada RUPI_AI_ENDPOINT, RUPI_AI_KEY, RUPI_AI_PROVIDER, RUPI_AI_MODEL. Endpoint HTTPS compatible con Chat Completions y JSON mode. Las claves solo se usan en Spring Boot. Dos llamadas acotadas: generación estructurada y revisión automática; no hay reintentos automáticos pagados.

El contexto se obtiene mediante la autorización de lecciones existente: inscripción, cuenta, versión publicada, progreso del nodo y fechas. Solo se envía grado, área, tema y contenido educativo saneado. El material se trata como dato no confiable; nunca como instrucciones. Los autores no deben incluir información personal en el material educativo.

El resumen contiene título, 2–4 conceptos sustentados en citas del material, 1–4 pasos, ejemplo explicado e idea final. Validación estructural más revisión automática de pertinencia, exactitud, lenguaje y seguridad. No garantiza exactitud pedagógica: el recurso se identifica como asistido por IA y distingue revisión automática de docente. El render es texto escapado/React, sin HTML arbitrario del modelo. Las operaciones y símbolos se presentan como texto.

## Base compartida
V25__resumen_visual_hu23.sql agrega:
- aprendizaje_resumen_visual: versión inmutable de contenido publicado, contexto/hash, actividad/nodo/ruta, proveedor/modelo/prompt y estado de revisión.
- aprendizaje_resumen_solicitud: identidad de solicitud y control persistente de uso.
- aprendizaje_resumen_reporte: reporte individual y motivo.

Migración probada en MySQL aislado y aplicada a TiDB compartido. Solo CREATE TABLE; no se alteraron datos anteriores. Las tres tablas se verificaron vacías tras la migración.

Una versión de contexto/nodo se comparte entre estudiantes autorizados. Un índice único y reserva transaccional evitan generaciones concurrentes duplicadas. Se reutiliza el bloqueo de cuota de HU-21; HU-23 contabiliza sus solicitudes por separado (cinco/hora). Los fallos pueden reintentarse después de un minuto; generaciones de más de dos minutos se consideran fallidas. No se mantiene una transacción abierta durante la llamada externa.

Los reportes retiran el contenido de consultas y descargas hasta revisión. El docente solo puede revisar rutas de sus aulas; el estado del recurso compartido afecta a todos los alumnos de ese tema. El reporte no expone identidades de otros estudiantes en las respuestas del API.

Abrir, generar o descargar no crea intentos, respuestas calificables ni dominio del tema; no modifica promedios o alertas de otras historias.

## HU-12 y uso sin conexión
Se amplía StudyPackageBuilder y se extrae el cliente downloadStudyPackage para reutilizar el formato ZIP, manifest, nombres de archivo y comprobación SHA-256.
El paquete incluye ABRIR.html, contenido.json, manifest.json y LEEME.txt. HTML autónomo, sin scripts, fuentes, imágenes ni solicitudes remotas. Contiene grado, tema, procedencia, versión y estado de revisión. Extraer el ZIP y abrir ABRIR.html; no requiere que React esté cargado.

Web: al recuperar la conexión con el tema abierto, se consulta de nuevo la versión/estado del servidor. Un HTML descargado es una copia estática: indica volver a Rupi para comprobar vigencia. No se promete actualizar o eliminar un archivo que el usuario conserva sin conexión.
Kotlin: descarga el mismo paquete con comprobación SHA-256 y guarda contenido estructurado mediante archivo temporal. La pantalla nativa vuelve a consultar al detectar conexión; una copia retirada o de otra versión se invalida al comprobarla. Si no se puede comprobar, se identifica explícitamente como copia descargada potencialmente desactualizada. Las copias en otras pantallas se comprueban al abrir el tema.

## Validación
- Suite backend: 260 pruebas descubiertas, 216 ejecutadas aprobadas, 44 omitidas por configuración; incluye ocho SQL HU-23 y tres de proveedor/validación simulados.
- Prueba adicional TiDB exclusivamente SELECT aprobada: tres temas accesibles y tres tablas nuevas; proveedor deshabilitado. Se añadió como prueba optativa independiente tras la suite.
- Casos: persistencia/reutilización, concurrencia, proveedor ausente/salida inválida, permisos/fechas, reporte/revisión, versiones antiguas, ZIP autónomo y límite durable.
- Web: build/lint correctos y 27 pruebas aprobadas. Advertencia de tamaño de bundle superior a 500 kB.
- Navegador con API simulada: estado sin IA, generación, texto accesible, vistas 375/1280, descarga verificada, retirada al reconectar. HTML real del generador abierto con red deshabilitada y cero solicitudes HTTP.
- Kotlin: fuentes compiladas con stubs Android; no equivale a APK ni ejecución física. Sigue pendiente validar APK, conectividad y descargas en dispositivo/emulador. El intento Gradle previo quedó bloqueado por Unable to establish loopback connection.
- No se afirma prueba con IA real porque falta proveedor/clave.

## Entrega
Commit con HU-23 en el cuerpo y publicación directa a main autorizada por el usuario en esta solicitud. Servidor local actualizado. Los secretos y archivos de prueba locales quedan fuera de Git.
