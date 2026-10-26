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
