# HU-04 · Fuentes curriculares de la demostración

Consulta del 2026-10-07. Sirve para saber qué datos de la ruta de Matemática de 2.° son oficiales del MINEDU y cuáles son de demostración. El diseño y la evidencia de la historia están en [HU-04-ruta-por-unidades.md](HU-04-ruta-por-unidades.md).

## Qué es oficial

Todo sale del **Programa curricular de Educación Primaria** del Currículo Nacional de la Educación Básica (CNEB), aprobado por RM 281-2016-MINEDU y modificado por RM 159-2017-MINEDU. Se descargó el PDF oficial de `minedu.gob.pe/curriculo/pdf/programa-curricular-educacion-primaria.pdf` (396 páginas). El CNEB rige desde el año escolar 2017.

| Dato | Dónde está en el PDF (página impresa) | Dónde está en la base |
|---|---|---|
| Las 4 competencias de Matemática y sus capacidades | Presentación de cada competencia en el área de Matemática (la de cantidad, p. 232) | `curriculo_competencia` |
| Estándar del ciclo III (nivel esperado al final de 2.°) | Cantidad p. 236, regularidad p. 246, forma p. 256, datos p. 266 | `curriculo_estandar` (4) |
| Desempeños de 2.° grado | Cantidad p. 237, regularidad p. 247, forma pp. 256-257, datos p. 266 | `curriculo_meta_aprendizaje` (23) |

Los textos están transcritos tal cual, sin resumir. Cada uno se comparó con el PDF y se corrigió una diferencia («continuar y completar patrones», no «o»). Esa comparación se hizo a mano y no es una prueba repetible. Lo que sí es repetible es que la base conserve los conteos y el orden esperados (`scripts/verify_hu04_content.py`).

## Qué es demostración

- **Las tres unidades** (`DEMO-MAT2-U1` a `U3`). El MINEDU no publica unidades para primaria: cada docente o colegio las programa a partir de competencias, estándares y desempeños. Aquí agrupan los diez nodos de la ruta y enlazan los desempeños que trabaja cada tramo (`curriculo_unidad_meta`, `curriculo_unidad_estandar`).
- **El reparto de nodos** en unidades 1–4, 5–7 y 8–10 y el vínculo entre cada unidad y sus desempeños. Es criterio del equipo y falta validarlo con un docente.
- **Los nodos, sus actividades y su contenido**, que vienen de `seed-hu01.sql` y `seed-hu02.sql`.

## Corrección hecha al catálogo

La semilla antigua llamaba al catálogo `CNEB-2024`, con vigencia desde 2024-01-01 y sin fuente. Eso no tenía respaldo. V07 y `seed-hu01.sql` lo dejan como `CNEB-2017`, vigente desde 2017, con la referencia a las dos resoluciones.

## Lo que no se pudo confirmar

- **Actualización del CNEB.** El 19 de julio de 2026 el MINEDU creó una comisión para actualizar el currículo (RM 393-2026-MINEDU, vigente hasta el 31 de diciembre de 2027). No se encontró una versión posterior publicada, y la nota consultada no dice que la actual deje de regir. Si se publica una nueva, hay que cargarla como otra `curriculo_version_catalogo` y no sobrescribir esta.
- **Calendario 2026.** Los medios reportan cuatro bloques lectivos de nueve semanas (RM 501-2025-MINEDU). Es una fuente secundaria y no se usó en los datos. Podría servir para ordenar unidades por bloque.
- **Contenido de los nodos 7 a 10.** En `seed-hu02.sql` el texto no siempre coincide con el título del nodo. Por ejemplo, «Formas en la piedra» trae contenido sobre decenas. Las unidades se agruparon por título.

## Cómo reemplazarlo por datos reales

1. Django (dueño de `curriculo_*`) carga las unidades reales con su `numero_secuencia`.
2. Una migración o una carga administrativa vincula los nodos con `aprendizaje_nodo_ruta.unidad_id`.
3. Se actualiza `scripts/fixtures/hu04-orden-esperado.json` con el orden acordado con el docente.
4. La API y el mapa no cambian: agrupan por lo que encuentren en la base.
