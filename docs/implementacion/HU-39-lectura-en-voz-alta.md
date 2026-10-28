# HU-39 · Escuchar consignas y explicaciones

Implementación: 28 de octubre de 2026 · commit `f75d438`.

> Como estudiante móvil, quiero escuchar las consignas y explicaciones leídas por voz, con soporte auditivo, para comprender los problemas.

## Criterios de aceptación (definidos en la implementación)
- Cada consigna (pregunta con sus alternativas) y cada explicación tiene un botón **Escuchar**.
- En la web el botón cambia a **Detener** mientras lee; tiene 48 × 48 px como mínimo y foco visible.
- La voz es en español del Perú cuando el dispositivo la tiene, y si no, otra voz en español, a ritmo pausado.
- En Android funciona con la voz del sistema. Si no hay voz en español, la app explica cómo activarla.

## Dónde se puede escuchar
| Pantalla | Web | Android |
|---|---|---|
| Práctica con IA: pregunta y alternativas, explicación, pista de HU-37 | ✓ | ✓ |
| Lección: retroalimentación de la respuesta | ✓ (nuevo) | — |
| Tarjetas de memoria: cara visible | ✓ | ✓ |
| Resumen visual completo | ✓ | ✓ |
| Meta del día y comparación | ✓ | ✓ |
| Reto del día: consigna, lectura, preguntas y explicaciones (HU-40) | ✓ | ✓ |

La lección web ya leía título, instrucciones, bloques y preguntas desde HU-24.

## Código
- Web: `components/ListenButton.tsx` y `utils/speech.ts` (`speechText` quita la negrilla del tutor y los símbolos, `questionSpeech` arma «Alternativa a: …», `stopSpeaking`, aviso al terminar).
- Android: `RupiVoice.kt` (`TextToSpeech`, `es-PE` o `es`, velocidad 0.92). Se detiene al cambiar de pantalla y se libera en `onDestroy`. El manifiesto declara `TTS_SERVICE` en `<queries>` (Android 11+).

## Pruebas
- `tests/speech.test.ts`: limpieza de texto, lectura de alternativas y navegador sin voz.
- Emulador Pixel 8: el motor de Google TTS cargó la voz `es-us` al pulsar «Escuchar la lectura» (el emulador no trae `es-PE`, así que se usó el respaldo previsto).
- Playwright: el reto del día tiene al menos 3 botones Escuchar y todos miden 48 px o más.
