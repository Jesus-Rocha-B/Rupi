# HU-22 · Tarjetas de memoria

> Actualizacion posterior: APK compilado y probado en emulador con datos aislados. Falta proveedor/clave para validar IA real. Ver [validacion actual](VALIDACION-ANDROID-Y-CONFIGURACION-IA.md). Los limites Android de abajo corresponden a la entrega inicial.

Implementación: 26 de octubre de 2026.

## Tareas
1. Crear tarjetas con preguntas y respuestas vinculadas a una actividad.
2. Voltear y navegar con gestos táctiles o botones.
3. Registrar la autoevaluación y priorizar conceptos que requieren práctica.

## Recorrido
- Profesor: Reportes → aula y ruta → Crear tarjetas de memoria revisadas. Elegir actividad, pregunta, respuesta, fuente y confirmar revisión pedagógica; publicar.
- Estudiante web: ruta → lección disponible → Repasar con tarjetas. Pensar, revelar, marcar Lo recuerdo o Necesito practicar y navegar.
- Android Kotlin: tema → Repasar con tarjetas; mismos permisos y persistencia mediante Spring Boot.
- No requiere IA. No se publicaron tarjetas ficticias ni se atribuyó contenido propio al MINEDU. El docente debe publicar las tarjetas de su aula.

## Modelo y alcance
Se reutilizan aprendizaje_tarjeta_memoria (frente, reverso, versión de actividad y orden) y aprendizaje_repaso_tarjeta (usuario y tarjeta). V24 añade únicamente aprendizaje_tarjeta_revision (autor, aula/ruta/nodo, fuente y revisión) y aprendizaje_tarjeta_sesion (posición, cara activa, revisión de concurrencia y última solicitud).

V24 se probó en MySQL local aislado y se aplicó a TiDB compartido, sin ALTER, UPDATE ni DELETE de datos existentes. Las tablas nuevas quedaron vacías; no hubo pruebas ficticias en la base compartida. El primer borrador de prueba encontró el modelo previo y se sustituyó por la reutilización descrita antes de aplicar V24 a TiDB.

Las tarjetas se publican como versiones inmutables. Corregir implica retirar y crear otra; el historial anterior se conserva y la nueva tarjeta no hereda la autoevaluación. Las tarjetas antiguas sin revisión explícita no se muestran automáticamente. La aprobación docente es una declaración explícita, no una garantía automática de exactitud.

El alumno debe tener inscripción y nodo disponibles, además de pertenecer al aula que publicó las tarjetas. El docente debe tener acceso al aula/ruta. Spring Boot es el único escritor. No se crean intentos ni respuestas calificables y no se modifican notas, progreso oficial ni alertas HU-16 a HU-20.

## Regla de repaso
Orden: Necesito practicar → nuevas → Lo recuerdo; desempate por publicación e ID. Se conserva la tarjeta actual aunque cambie su posición en el orden. Volver a repasar comienza desde la primera de ese orden, sin borrar historia.
- Necesito practicar: intervalo_dias=0, próximo repaso ahora.
- Lo recuerdo: intervalo_dias=1, próximo repaso al día siguiente.
No es un algoritmo completo de repetición espaciada: la prioridad es sencilla y no oculta tarjetas por fecha. El usuario puede repasarlas siempre. Estos datos son autoevaluación, no medición objetiva de dominio.

La navegación y el giro no registran dominio. Para marcar una tarjeta se exige haber revelado su respuesta. Se guarda posición y cara al navegar. Un bloqueo transaccional y revisión optimista impiden que dos dispositivos sobrescriban cambios silenciosamente. La última solicitud repetida devuelve el estado guardado; un reintento antiguo genera conflicto y pide recuperar el repaso.

## Interfaz y accesibilidad
Web adaptativa, botones táctiles, giro breve con movimiento reducido, solo cara activa en el árbol accesible, teclado mediante botones nativos. Swipe horizontal mayor de 60 px y dominante sobre vertical; desplazamiento vertical sin preventDefault. Hay botones independientes para todas las acciones. Los errores conservan la sesión del servidor y permiten recuperación/reintento.
Kotlin usa vistas nativas y gestos; guarda el modo de pantalla durante recreación. Tras cierre del proceso requiere nueva autenticación y recupera el avance del servidor. Si falla una respuesta de red, Recuperar mi repaso consulta lo persistido antes de continuar.

## Validación
- Backend: 249 pruebas descubiertas, 205 ejecutadas aprobadas, 44 omitidas por configuración. Incluye siete pruebas SQL HU-22: revelación obligatoria, persistencia, duplicados y revisión antigua, orden, navegación sin calificación, permisos, retiro y nueva versión, publicación idempotente y membresía.
- TiDB: prueba de lectura independiente aprobada sobre tres temas accesibles y las dos tablas nuevas.
- Web: build, lint y 27 pruebas aprobadas. Permanece advertencia de bundle superior a 500 kB.
- Navegador con API simulada: publicación docente, estado vacío, giro por teclado, autoevaluación, recarga, gestos sintéticos horizontales/verticales, falla de guardado/reintento, anchos 375/1280 y movimiento reducido.
- Kotlin: fuentes compiladas contra stubs Android; no equivale a APK ni prueba en dispositivo. Sigue pendiente el build Android completo: el intento Gradle anterior falló con Unable to establish loopback connection. No se afirma que la experiencia nativa esté validada.
- Servidor local actualizado. No publicación en GitHub sin autorización expresa.

## Pruebas SQL reproducibles
FlashcardIntegrationTest reutiliza el fixture aislado de HU-21 y exige RUPI_HU21_TEST_DB=isolated, URL local rupi_hu21_test y credenciales locales. Aplicar V23 y V24 al esquema local antes de ejecutar. Nunca apuntar fixtures al servidor compartido.
FlashcardSharedReadTest requiere RUPI_HU22_SHARED_READ=true y configuración TiDB; solo ejecuta SELECT.
