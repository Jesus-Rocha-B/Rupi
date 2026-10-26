# HU-24 · Recursos optimizados para pantalla táctil

Implementación: 26 de octubre de 2026. Prioridad: muy alta.

## Tareas
1. Adaptar controles táctiles al tamaño de pantalla.
2. Revisar que el contenido no requiera desplazamiento lateral.
3. **Validar la accesibilidad táctil con texto ampliado, orientación horizontal y pantallas pequeñas.** Tarea adicional.

## Cambios
Los recursos de repaso de la lección (cuestionarios, tarjetas, resúmenes y refuerzos) usan controles de al menos 48 × 48 CSS px, texto que se ajusta al ancho disponible, foco visible y acciones apiladas en móviles. Se conserva el zoom del navegador y el movimiento reducido; el swipe de las tarjetas permite desplazamiento vertical y zoom táctil. No se oculta el desbordamiento para disimular contenido recortado.

Android usa controles nativos, botones y respuestas multilínea, ancho del contenedor limitado a la pantalla, objetivos de 48 dp y márgenes para las barras del sistema. La identidad verde y los materiales escolares se mantienen.

## Validación
- Componentes React reales montados con respuestas API simuladas locales: 320, 375, 768 y 1024 px; texto a 100 % y 200 %. Sin scroll horizontal; controles visibles de al menos 48 px.
- Guardar preferencias y marcar leído en la misma pantalla adaptable: sin errores JavaScript.
- Emulador Pixel 6: orientación horizontal, escala de fuente 2.0; lectura y controles ajustados al ancho.
- Compilación Android y lint; revisión de capturas locales. Compilación web y pruebas existentes.
- La revisión de interfaz no certifica accesibilidad completa ni reemplaza pruebas con estudiantes y lectores de pantalla.

## Ubicación
Web: ruta → lección disponible → recursos de repaso.
Android: Mis temas → tema → cuestionario, tarjetas o resumen visual.
No requiere cambios propios en base de datos: reutiliza el contenido compartido existente.
