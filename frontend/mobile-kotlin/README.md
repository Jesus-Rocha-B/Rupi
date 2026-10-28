# Rupi Android · HU-21

Cliente Kotlin nativo con vistas Android, sin WebView. Permite iniciar sesión como estudiante, listar temas accesibles, recuperar/generar prácticas, responder, ver explicación y reportar preguntas. Consume Spring Boot; nunca se conecta directamente a TiDB ni lleva una clave de IA.

## Abrir y ejecutar
1. Abrir esta carpeta como proyecto en Android Studio.
2. Instalar/configurar JDK 17 y Android SDK 36; seleccionar un dispositivo API 26 o superior.
3. Sincronizar Gradle (wrapper 8.13, AGP 8.13, Kotlin 2.3.0).
4. Ejecutar app o usar gradlew.bat :app:assembleDebug.
5. Introducir la URL base de Spring Boot y credenciales de estudiante existentes. En emulador Android, la dirección local es http://10.0.2.2:8081. La versión release exige HTTPS; debug permite HTTP solo para loopback/emulador. Para teléfono físico usar un servidor HTTPS accesible.
6. Elegir un tema y Practicar con Rupi.

La sesión permanece en memoria y se solicita de nuevo al cerrar el proceso. Los avances pertenecen al alumno autenticado y se recuperan del servidor. No se guardan claves del alumno en preferencias. La disponibilidad de IA depende de la configuración del backend; sin proveedor no se simula generación.

## Estado verificado
Fuentes Kotlin compiladas con stubs Android; no es una compilación APK. El intento Gradle falló al establecer su conexión local (Unable to establish loopback connection). Falta verificar build Android completo y flujo en dispositivo/emulador. Revisar también insets y accesibilidad con SDK 36.

La HU completa y configuración de backend están en ../../docs/implementacion/HU-21-cuestionarios-ia.md.

## HU-22: tarjetas de memoria
Desde un tema, elegir Repasar con tarjetas. Permite revelar pregunta/respuesta, deslizar horizontalmente, navegar con botones y marcar Lo recuerdo o Necesito practicar. El avance se guarda en TiDB mediante Spring Boot. El docente publica las tarjetas revisadas desde la web. Fuentes Kotlin compiladas; APK y dispositivo pendientes de validación. Detalles en ../../docs/implementacion/HU-22-tarjetas-memoria.md.

## HU-23: resúmenes visuales
Tema → Ver resumen visual. Reutiliza IA de Spring Boot y descarga ZIP de HU-12, verifica SHA-256 y conserva el contenido educativo en almacenamiento privado para lectura nativa sin conexión. Resúmenes descargados también está disponible antes del login; no contiene datos personales. Al detectar conexión con el resumen abierto, comprueba su vigencia. Falta proveedor real y validación en dispositivo/APK. Ver ../../docs/implementacion/HU-23-resumenes-visuales.md.

## Validacion actualizada
APK real compilado y probado en emulador Pixel_6: login, cuestionarios, flashcards, persistencia, descarga sin red y retirada. Ejecutar powershell -File scripts/Compilar-Android.ps1 -Lint desde la raiz. El fallo Gradle anterior esta resuelto. Solo falta proveedor/clave para IA real. Detalles: ../../docs/implementacion/VALIDACION-ANDROID-Y-CONFIGURACION-IA.md.

## HU-35 a HU-40: meta, comparación, pistas, voz y reto diario
Desde «Mis temas»:
- **Mi meta de hoy:** anillo (`DailyGoalRing.kt`) y barra de la meta diaria, selector de meta y «Hoy vs. ayer» con el XP de ambos días (HU-35, HU-36).
- **¿Por qué me equivoqué?:** en la práctica, tras un error. Abre la pista adaptada al grado con un ejemplo de la vida real (HU-37, HU-38).
- **🔊 Escuchar:** lee consignas, alternativas, explicaciones, tarjetas, resúmenes y el reto con la voz del sistema (`RupiVoice.kt`, `es-PE` o `es`) (HU-39). Si el dispositivo no tiene voz en español, la app indica cómo activarla en Texto a voz.
- **Reto del día:** razonamiento lógico o comprensión lectora con contexto peruano; se termina al responder todas las preguntas (+30 XP) (HU-40).

Verificado el 28/10/2026 con `assembleDebug` y en emulador Pixel_8 (API 35) contra la API local. Detalles en `../../docs/implementacion/HU-35-indicador-meta-movil.md` a `HU-40-retos-diarios-peru.md`.
