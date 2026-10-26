# Validación Android y configuración privada de IA

Actualización posterior a HU-21, HU-22 y HU-23 · 26 de octubre de 2026.

## Android: pendiente resuelto
APK debug real construido con Gradle 8.13, Kotlin 2.3.0 y SDK 36. Se migró jvmTarget al DSL compilerOptions. Se resolvió el fallo PipeImpl/AF_UNIX de Windows mediante el fallback TCP del JDK; scripts/Compilar-Android.ps1 aplica la opción al proceso y restaura el entorno.

Android Lint: cero errores, siete advertencias (icono, extracción de datos, accesibilidad estática y textos/localización). APK instalado y ejecutado en el emulador Pixel_6 existente, Android 37.1. No se afirma validación en teléfono físico ni distribución de producción.

Desde la raíz:

    powershell -File scripts/Compilar-Android.ps1 -Lint

Requiere JDK 17 o 21, SDK y licencias aceptadas por el usuario. En este equipo Java 21 compiló el APK; Gradle comprobó que la licencia SDK 36 ya estaba aceptada.

APK: frontend/mobile-kotlin/app/build/outputs/apk/debug/app-debug.apk. Artefacto local de desarrollo, sin claves de IA/BD y excluido de Git.

Referencia del problema de sockets: https://mail.openjdk.org/pipermail/nio-dev/2023-March/013297.html

## Pruebas nativas ejecutadas
Exclusivamente rupi_hu21_test local, backend separado en puerto 8082 y contenido ficticio de prueba. No se modificó TiDB compartido.
1. Inicio de sesión real y contraseña oculta (se corrigió su campo).
2. HU-21: selección de respuesta y persistencia comprobada por SQL.
3. HU-22: giro, swipe, Necesito practicar y recuperación de tarjeta después de cerrar y entrar. Autoevaluación comprobada por SQL.
4. HU-23: descarga ZIP con SHA-256, guardado, Wi-Fi/datos desactivados, cierre forzado y apertura desde Resúmenes descargados.
5. Con conexión: reporte, retirada e invalidación de copia local; reporte verificado por SQL.
6. Ajuste de performClick y contraste de iconos del sistema.

AndroidScenarioFixtureTest es optativo: RUPI_ANDROID_FIXTURE=isolated y la guarda de URL local de QuizIntegrationTest. Las credenciales son ficticias, no sirven en TiDB. Las generaciones del fixture son simuladas.

## IA: bloqueo externo
El equipo confirmó que todavía no dispone de proveedor ni clave. No se contrataron servicios ni se hicieron llamadas de pago. No puede validarse generación real hasta disponer de acceso.

Cuando tengan proveedor compatible con Chat Completions y JSON mode:

    powershell -File scripts/Configurar-IA.ps1

El asistente pide endpoint HTTPS, proveedor, modelo y clave oculta. La clave se cifra con DPAPI del usuario Windows en .local/ai-key.dpapi; la configuración no secreta va a .local/ai-config.json. .local está excluido de Git. Cada integrante configura su equipo; el cifrado no se copia entre usuarios/máquinas.

Los iniciadores cargan scripts/Cargar-IA.ps1. Reiniciar el backend tras configurar; un proceso ya iniciado conserva su entorno anterior. Ninguna clave se incluye en argumentos ni en React/Kotlin. Cifrado/descifrado probado con una credencial ficticia en carpeta aislada.

El backend aplica también el workaround de sockets para inicializar HttpClient; Selector/HttpClient fueron comprobados sin llamadas externas. Iniciar-Backend-Daemon.ps1 ya no incluye la contraseña de BD en argumentos.

Pendiente externo: generar un cuestionario y un resumen con proveedor real, comprobar pertinencia/costos y revisar con el docente. No se presenta esta verificación como terminada.
