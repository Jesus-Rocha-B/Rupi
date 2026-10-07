# HU-04 · Ver la ruta según las unidades del MINEDU

Diseño, sustentación, evidencia y registro de cambios. Cierre técnico: 2026-10-07. Fuentes curriculares y sus límites: [HU-04-fuentes-curriculares.md](HU-04-fuentes-curriculares.md).

## 1. Historia y resultado

> Como estudiante web, ver la ruta según las unidades del MINEDU para seguir el mismo orden que las clases del colegio.

El mapa de aventuras divide sus paradas en tramos por unidad curricular. Cada tramo lleva una banda con el título de la unidad, las competencias oficiales del MINEDU que trabaja y el avance del estudiante en ese tramo («2 de 4»). El orden y la validación los decide la API, no React.

**Estado:** implementada y verificada en local, con datos oficiales del MINEDU y unidades de demostración. Falta la aceptación del equipo. No equivale a validación pedagógica ni a aceptación del docente.

## 2. Tareas del backlog

| Tarea | Cómo se cumplió | Estado |
|---|---|---|
| Modelar el esquema para sílabos, competencias y unidades oficiales | El esquema de 77 tablas ya tenía `curriculo_version_catalogo`, `curriculo_competencia`, `curriculo_estandar`, `curriculo_meta_aprendizaje`, `curriculo_unidad` y sus tablas puente. No se creó ninguna tabla. V07 carga los datos oficiales. | Cumplida |
| Servicio de backend con lecciones ordenadas por bloques temáticos | `RouteCurriculum` agrupa por unidad, ordena por `numero_secuencia` y valida catálogo, grado y área. La API devuelve el campo `curriculum`. | Cumplida |
| Interfaz que agrupe y divida los nodos según las unidades | Bandas de unidad en el mapa (`unitGroups.ts`, `UnitBands.css`, `Roadmap.tsx`), en escritorio y móvil. | Cumplida |
| Pruebas de integración y validación de contenido para que la secuencia en pantalla coincida con el orden de la currícula | Pruebas de Java y Node, 6 escenarios contra MySQL real, `verify_hu04_content.py` (base y API) y `verify_hu04_pantalla.mjs` (navegador). Los dos últimos comparan con `scripts/fixtures/hu04-orden-esperado.json`. | Cumplida, con un orden esperado definido por el equipo. No sustituye la validación con un docente |

## 3. Criterios de aceptación

| # | Criterio | Cómo se cumple | Dónde se prueba |
|---|---|---|---|
| 1 | Grado y área de la ruta coinciden con sus unidades | `RouteCurriculum` compara catálogo, grado y área de cada unidad con los de la ruta. Si no coinciden, devuelve `INCOHERENTE` y no agrupa. | `unitFromAnotherGradeAreaOrCatalogIsNeverMixed`; escenario «unidad de otro grado» |
| 2 | Unidades por `curriculo_unidad.numero_secuencia`, nodos por `aprendizaje_nodo_ruta.numero_secuencia` | El orden se calcula en el backend. Las unidades sin secuencia van después de las numeradas. | `groupsNodesByUnitInCurricularOrderNotInInsertionOrder`, `unitsWithoutSequenceGoAfterNumberedOnes`; `verify_hu04_content.py`; `verify_hu04_pantalla.mjs` |
| 3 | Unidades y competencias de la misma versión de catálogo | La validación revisa también el catálogo y el área de cada competencia vinculada. | `competencyFromAnotherCatalogMakesTheCurriculumInconsistent`; `verify_hu04_content.py` |
| 4 | Regla explícita para nodos sin unidad | Las paradas sin unidad van a un grupo final «Otras paradas» (`id` nulo) y el estado es `PARCIAL`. Regla provisional. | `nodesWithoutUnitGoToAnExplicitFinalGroup`; escenario «parada sin unidad» |
| 5 | Catálogo retirado o incompleto da un estado claro | Estados `NO_VIGENTE`, `SIN_UNIDADES` e `INCOHERENTE`, con mensaje. Nunca se combina con otra versión. | `retiredCatalogIsFlaggedButKeepsItsOwnUnits`, `routeWithoutAnyUnitIsReportedAndNotGrouped` |
| 6 | Agrupar conserva el progreso individual | `nodes`, `progress` y `enrollment` no cambian. La banda deriva su avance del estado de cada nodo. | Escenario «agrupar no altera progreso ni estados»; `unitGroups.test.ts` |

## 4. Decisiones y su sustentación

- **Cambio aditivo en la API.** El detalle de ruta conserva `nodes`, `progress` y `enrollment` y añade `curriculum`. HU-01 a HU-03 dependen de esos campos y ya estaban verificadas. Si `curriculum` falla o llega vacío, el mapa se dibuja como antes.
- **Validación en el backend y no en la base.** Las FK de `curriculo_unidad` aseguran que su área pertenezca a su catálogo, pero no que la unidad de un nodo coincida con el catálogo, grado y área de la ruta. El modelo ya lo asume (`database/mysql/README.md`) y Spring es quien publica la ruta. Triggers o FK compuestas habrían exigido tocar el esquema por una regla que el servicio comprueba en cada lectura.
- **Si hay incoherencia, no se agrupa.** `INCOHERENTE` devuelve `units` vacío. Es mejor un mapa sin agrupar que una unidad de otro grado o catálogo, porque un niño de 7 años no puede evaluar si el orden es correcto.
- **Catálogo retirado: se muestra con aviso.** `NO_VIGENTE` conserva sus propias unidades y añade un mensaje. Siguen siendo coherentes entre sí y ocultarlas dejaría al estudiante sin ruta a mitad de curso.
- **Paradas sin unidad al final.** El criterio 4 pide una regla explícita. El final no interrumpe el recorrido de las unidades organizadas y deja claro que faltan vínculos. Decisión provisional, a confirmar con el equipo.
- **El mapa sigue el orden de la ruta.** La banda aparece donde cambia la unidad entre paradas consecutivas. Reordenar por unidad habría cambiado la numeración, el zigzag y el desbloqueo. Con los datos actuales cada unidad es un tramo contiguo.
- **Sin tablas nuevas.** Estándares, desempeños y vínculos caben en las tablas existentes (decisión vigente de `CONTEXTO.md`).
- **Escritor único.** V07 escribe `curriculo_*` (Django) y `aprendizaje_*` (Spring) porque es una carga de desarrollo, como V04 y `seed-hu01.sql`. En producción `curriculo_*` lo cargará Django. La API solo lee.
- **Unidades de demostración.** Inventar unidades «oficiales» sería falso y no cargar ninguna impediría demostrar la historia. Llevan el código `DEMO-*` y están documentadas.
- **Orden esperado independiente.** Las pruebas de contenido y de pantalla comparan con un archivo escrito aparte de la base, para no comparar la base consigo misma.

## 5. Investigación del caso

Consulta del 2026-10-07. Detalle y páginas en [HU-04-fuentes-curriculares.md](HU-04-fuentes-curriculares.md).

1. **El currículo vigente es el CNEB**, aprobado por RM 281-2016-MINEDU y modificado por RM 159-2017-MINEDU. Rige desde el año escolar 2017.
2. **El MINEDU no publica «unidades» para primaria.** Publica competencias, capacidades, estándares por ciclo y desempeños por grado. Las unidades las programa cada docente o colegio. «Las unidades del MINEDU» son, en rigor, la secuencia que el colegio arma sobre el currículo oficial. De ahí la separación entre lo oficial y lo de demostración.
3. **Matemática tiene cuatro competencias**: cantidad; regularidad, equivalencia y cambio; forma, movimiento y localización; gestión de datos e incertidumbre. Los estándares y desempeños de 2.º son del ciclo III.
4. **Hay una actualización en marcha.** El 19 de julio de 2026 se creó una comisión para actualizar el currículo (RM 393-2026-MINEDU, hasta el 31 de diciembre de 2027). No se encontró una versión posterior publicada.
5. **Calendario 2026.** Según medios, cuatro bloques lectivos de nueve semanas (RM 501-2025-MINEDU). Fuente secundaria, no usada en los datos.

**Cómo se obtuvo.** Se descargó el PDF oficial del Programa curricular de Educación Primaria (396 páginas), se extrajo su texto y se transcribieron estándares y desempeños. Cada texto se comparó con el PDF y, donde la extracción era ambigua, con la imagen de la página. Esa comparación detectó una diferencia («continuar y completar patrones»), ya corregida. La comparación con el PDF se hizo a mano y no quedó como prueba repetible.

**No se pudo confirmar:** que el CNEB actual deje de regir cuando termine la actualización; que el reparto de nodos coincida con el de algún docente real; y que los textos de las paradas 6 a 10, reescritos para que coincidan con su título y con los desempeños oficiales, sean adecuados para el aula: no los revisó un docente.

## 6. Diseño

### Datos

```text
curriculo_version_catalogo (CNEB-2017)
 ├─ curriculo_area (MAT) ── curriculo_competencia (4) ── curriculo_estandar (4, ciclo III)
 │                                   └──────────────── curriculo_meta_aprendizaje (23, 2.° grado)
 └─ curriculo_unidad (3, DEMO-*) ── puente a competencias, estándares y metas
        ▲
aprendizaje_nodo_ruta.unidad_id ─ aprendizaje_actividad_unidad
```

Migración: [`V07__unidades_curriculares_hu04.sql`](../../database/mysql/migrations/V07__unidades_curriculares_hu04.sql). Idempotente. Solo completa `unidad_id` en nodos que lo tengan nulo y no toca `aprendizaje_progreso_nodo`.

### Contrato de la API

`GET /api/v1/student/learning-routes/{versionRouteId}` añade:

```json
{
  "curriculum": {
    "status": "COMPLETA",
    "message": null,
    "units": [
      {
        "id": "d1000000-0000-4000-8000-000000000002",
        "code": "DEMO-MAT2-U2",
        "title": "Unidad 2: patrones, medidas y formas",
        "sequence": 2,
        "competencies": [
          { "code": "MAT-C2", "name": "Resuelve problemas de regularidad, equivalencia y cambio" },
          { "code": "MAT-C3", "name": "Resuelve problemas de forma, movimiento y localización" }
        ],
        "nodeIds": ["ca000000-…-000000000005", "ca000000-…-000000000006", "…"]
      }
    ]
  }
}
```

| `status` | Cuándo ocurre | `units` |
|---|---|---|
| `COMPLETA` | Todos los nodos tienen unidad y todo es coherente. | Unidades agrupadas |
| `PARCIAL` | Algunos nodos no tienen unidad. | Unidades más el grupo final «Otras paradas» (`id` nulo) |
| `SIN_UNIDADES` | Ningún nodo tiene unidad, o no hay datos curriculares. | Vacío |
| `NO_VIGENTE` | El catálogo de la ruta no está `ACTIVO`. | Sus propias unidades, con mensaje |
| `INCOHERENTE` | Una unidad o competencia no coincide en catálogo, grado o área. | Vacío, con mensaje |

Precedencia cuando coinciden varios: `INCOHERENTE`, `SIN_UNIDADES`, `NO_VIGENTE`, `PARCIAL`, `COMPLETA`. El acceso exige sesión de estudiante y matrícula activa o completada. La API solo lee.

### Interfaz

- La banda usa la paleta oficial: amarillo maíz de fondo, verde bosque en borde y texto, sombra sólida y sin halos. Muestra título, «Currículo MINEDU: …» y «N de M».
- Con 640 px o menos el mapa pasa a una columna (`App.css`) y la banda entra en el flujo. En esa vista se oculta el conector punteado que llegaba hasta la banda.
- Si el estado no es `COMPLETA`, un aviso `role="status"` con el mensaje aparece sobre el mapa.
- La lógica de bandas es una función pura (`unitBandsByNode`), con prueba propia.
- Es de solo lectura: las bandas no responden a toques y no hay vista para editar unidades.

## 7. Verificación

| Comprobación | Resultado |
|---|---|
| `mvn test` | 36 pruebas, sin fallos (8 nuevas en `RouteCurriculumTest`) |
| `npm run build` y `npm run lint` | Aprobados |
| `node --experimental-strip-types --test` (`routeSelection`, `unitGroups`) | 5 aprobadas |
| `scripts/verify_learning_flow.py` | 29 comprobaciones HTTP/MySQL (23 anteriores y 6 de unidades) |
| `scripts/verify_hu04_content.py` | 11 comprobaciones sobre la ruta demo: catálogo vigente, 4 estándares y 23 desempeños, coherencia de catálogo, grado y área, y orden en base y API frente al orden esperado |
| `scripts/verify_hu04_pantalla.mjs` (Firefox, 1280 y 375 px) | 10 comprobaciones: orden de unidades y paradas dibujadas, orden visual de arriba abajo, bandas que no tapan paradas, competencias y «N de M» en cada banda, paradas de 48 px o más |
| Detección de errores | Con el orden esperado alterado, el validador de contenido detectó 4 diferencias y el de pantalla 2 |
| V07 en base nueva y aplicada dos veces | 4 competencias, 4 estándares, 23 metas, 3 unidades, sin duplicados |
| V07 en la base local con progreso | Progreso idéntico antes y después (2 completados, 1 en curso, 1 disponible, 6 bloqueados) |

El orden esperado vive en `scripts/fixtures/hu04-orden-esperado.json`. Si cambian las unidades a propósito, se actualiza ese archivo. Define el orden del equipo, no el de un colegio real.

```bash
JAVA_HOME=~/jdks/jdk-21.0.12.1+1 mvn -f backend/api-spring-boot/pom.xml test
npm run build && npm run lint
node --experimental-strip-types --test frontend/web-react/tests/routeSelection.test.ts frontend/web-react/tests/unitGroups.test.ts
python3 -I scripts/verify_learning_flow.py --root-config .local/root.cnf --mysql .local/mysql-8.4.11-linux-glibc2.28-x86_64/bin/mysql
python3 -I scripts/verify_hu04_content.py --root-config .local/root.cnf --mysql .local/mysql-8.4.11-linux-glibc2.28-x86_64/bin/mysql
# Pantalla: Playwright fuera del repo (instrucciones en el encabezado del archivo)
PLAYWRIGHT_DIR=/tmp/pw node scripts/verify_hu04_pantalla.mjs
```

**Lo que no se probó:** los estados `PARCIAL` e `INCOHERENTE` solo se ejercen en la API y en Java, no en pantalla; el contraste de la banda (verde bosque `#173e30` sobre amarillo maíz `#ffd26a`) no se midió con herramienta; no se probó en un teléfono real, solo en escritorio y con el modo responsive; la consulta SQL de `findCurriculum` se ejercita solo con los scripts de Python, no dentro de `mvn test`.

## 8. Registro de cambios

**Base de datos**
- Nueva `migrations/V07__unidades_curriculares_hu04.sql` (incluye el contenido alineado de las paradas 6 a 10, ver más abajo): 4 competencias con capacidades, 4 estándares del ciclo III, 23 desempeños de 2.°, 3 unidades de demostración y sus vínculos (nodos, actividades, competencias, estándares y desempeños).
- V07 corrige la procedencia del catálogo: `CNEB-2024`, vigente desde 2024 y sin fuente, pasa a `CNEB-2017`, vigente desde 2017, con la referencia a RM 281-2016 y RM 159-2017.
- `seed-hu01.sql` queda alineado con esa corrección para las bases nuevas.
- **Contenido de las paradas 6 a 10:** la semilla antigua traía textos que no coincidían con el título (por ejemplo, «Formas en la piedra» hablaba de decenas). V07 los reemplaza por textos sobre longitud, formas, días de la semana, medir con pasos y un reto de la cosecha, cada uno ligado a un desempeño oficial de 2.°. `seed-hu02.sql` trae los mismos textos para las bases nuevas. Las unidades 2 y 3 pasaron a llamarse «patrones, medidas y formas» y «tiempo, pasos y retos», y se ajustaron sus desempeños vinculados (13 vínculos unidad–desempeño en total).
- **Historial de V07:** la primera versión solo tenía 4 competencias sin texto y 3 unidades de prueba. Se rehízo antes del primer commit con los textos oficiales, estándares y desempeños, y se le añadió la corrección del catálogo.

**Backend (`learningroute/`)**
- Nuevo `RouteCurriculum.java`: agrupación, orden, estados y validación de coherencia.
- `LearningRouteRepository.findCurriculum`: tres consultas de solo lectura.
- `LearningRouteDtos` y `LearningRouteService`: campo `curriculum`, aditivo.
- Nueva prueba `RouteCurriculumTest` (8).

**Web**
- `learningRoutesApi.ts`: tipos de `curriculum`.
- Nuevos `unitGroups.ts` y `UnitBands.css`; `Roadmap.tsx` con bandas, aviso de estado y separación vertical por banda.
- Nueva prueba `tests/unitGroups.test.ts` (3).
- Se corrigieron dos fallos de la vista móvil encontrados al revisar en el navegador: bandas sobre las paradas y conector punteado que cruzaba el título.

**Pruebas y scripts**
- `verify_learning_flow.py`: 6 escenarios nuevos (29 en total).
- Nuevos `verify_hu04_content.py`, `verify_hu04_pantalla.mjs` y `scripts/fixtures/hu04-orden-esperado.json`.

**Documentación**
- Nuevos `HU-04-ruta-por-unidades.md` (este documento) y `HU-04-fuentes-curriculares.md`.
- Actualizados `docs/implementacion/README.md`, `entorno-linux.md`, `docs/registro/CHANGELOG.md`, `CONTEXTO.md`, `ESTADO.md` y `MAPA-DE-CODIGO.md`.
- Se corrigió una afirmación propia: la tarea 4 se había marcado como cumplida antes de tener pruebas de pantalla y de contenido. Se marcó parcial y se cerró después de crearlas.

## 9. Límites, riesgos y pendientes

- **Las unidades no son oficiales.** Un docente debe validar el reparto de nodos y los desempeños asignados.
- **El orden esperado lo definimos nosotros.** Las pruebas garantizan que pantalla, API y base lo respetan. Que sea el de un colegio real solo lo confirma un docente.
- **Los textos de las paradas 6 a 10 son de demostración.** Se alinearon con su título y con los desempeños oficiales, pero no los revisó un docente.
- **Nodos sin unidad al final** es una regla provisional.
- **Unidad partida en tramos:** la banda se repetiría y no se reordena. Con los datos actuales no ocurre.
- **Actualización del CNEB en curso.** Una versión nueva se cargaría como otro catálogo, sin sobrescribir `CNEB-2017`.
- **Solo lectura:** estándares y desempeños no llegan a la pantalla del estudiante y no hay forma de editar unidades. El administrador de Django sigue siendo un esqueleto.
- **Pendientes del proyecto:** privacidad y acceso de menores, y el mecanismo único de migraciones, antes de publicar.

Pendientes concretos de HU-04: confirmar con el equipo la regla de nodos sin unidad y el reparto de unidades; cargar las unidades reales desde Django cuando exista ese módulo; probar en un teléfono real y medir el contraste; revisar si la cabecera móvil, que ya desbordaba antes de HU-04, necesita arreglo; y seguir el proceso de actualización del CNEB.
