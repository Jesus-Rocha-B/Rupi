# HU-38 · Ejemplos de la vida real en las explicaciones del tutor

Implementación: 28 de octubre de 2026 · commit `c285025`.

> Como estudiante móvil, quiero que el tutor virtual (IA) me dé explicaciones con ejemplos de la vida real.

## Criterios de aceptación (definidos en la implementación)
- Toda pista de HU-37 trae un **Ejemplo de la vida real** con una situación cotidiana peruana (mercado, bodega, chacra, combi, lonchera, fiesta del barrio) que usa la misma idea.
- El ejemplo no agrega datos nuevos al tema de la lección.
- El tutor de chat incluye en cada explicación un ejemplo marcado como **En la vida real:**.

## Cómo funciona
- **Con IA:** el JSON de la pista incluye `realLifeExample` (máximo 400 caracteres, sin enlaces). La revisión automática agrega `exampleFits`: si el ejemplo no encaja con el material, se descarta la pista generada.
- **Sin IA:** `QuizHintService.realLifeExample(area)` usa un ejemplo por área (MAT, COM, CYT, SOC, ART, ING y uno general) que conecta la habilidad con la vida diaria sin afirmar hechos del tema.
- **Tutor de chat:** regla 8 en `SYSTEM_RUPI_TUTOR`. El chat ya muestra la negrilla, así que «**En la vida real:**» se destaca solo.

## Uso
Web: bloque con borde amarillo maíz dentro de la pista. Android: el diálogo de la pista termina con «Ejemplo de la vida real».

## Pruebas
- `QuizHintServiceTest`: el ejemplo de la IA llega al alumno, se rechaza una pista sin ejemplo y cada área tiene uno propio.
- **No verificado:** respuestas reales de Gemini en el chat (la IA no está configurada en el entorno local).
