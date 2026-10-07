# Registro de contexto · RUPI

Esta carpeta existe para retomar el trabajo después de un `/clear` o `/compact` sin releer todo el código. Se lee en este orden:

| Archivo | Para qué sirve | Cuándo leerlo |
|---|---|---|
| [`CONTEXTO.md`](CONTEXTO.md) | Qué es Rupi, arquitectura, comandos, convenciones y decisiones vigentes. | Siempre, al empezar una sesión. |
| [`ESTADO.md`](ESTADO.md) | Qué está hecho, qué está pendiente y qué sigue. | Siempre; es corto. |
| [`CHANGELOG.md`](CHANGELOG.md) | Historial de cambios por fecha y commit. | Solo si hay que saber cuándo o por qué cambió algo. |
| [`MAPA-DE-CODIGO.md`](MAPA-DE-CODIGO.md) | Dónde está cada cosa en el código (archivos, endpoints, tablas). | Antes de tocar código, para no explorar a ciegas. |

## Cómo pedir contexto a Claude

Frase corta para una sesión nueva:

> Lee `docs/registro/CONTEXTO.md` y `docs/registro/ESTADO.md`, y dime en 5 líneas dónde nos quedamos.

Si la tarea toca código concreto, añade: «y `MAPA-DE-CODIGO.md`».

## Cómo mantenerlo al día

Al cerrar una sesión o un bloque de trabajo, pedir: «actualiza `docs/registro/` con lo que hicimos». Se agrega una entrada arriba en `CHANGELOG.md`, se ajusta `ESTADO.md` y, si cambió la estructura, `MAPA-DE-CODIGO.md`. No se duplica aquí lo que ya documentan `docs/implementacion/` y `database/mysql/`: se enlaza.

Última actualización: 2026-10-07.
