# Historia 12 · Descargar una unidad para estudiar sin conexión

> Como usuario web, debería poder descargar los materiales y ejercicios del roadmap en un archivo a fin de
> poder estudiar, repasar o imprimir los recursos cuando no tenga conexión a internet.

Épica 3 del backlog (descubrimiento y acceso a rutas). Con esta historia la épica queda completa (9, 10, 11 y 12).

## Tareas del backlog y cómo se cumplen

| Tarea | Implementación |
|---|---|
| 1. Empaquetador en formatos accesibles (PDF/JSON comprimido) | `StudyPackageBuilder`: zip con guía PDF imprimible, materiales del docente, `contenido.json`, `manifest.json` y `LEEME.txt`. |
| 2. Servicio backend que genere paquetes de una unidad | `GET /api/v1/student/learning-routes/{versionRouteId}/units/{unitId \| otras}/package` (`StudyPackageController`, `StudyPackageService`, `StudyPackageRepository`). |
| 3. Caché temporal para descargas recurrentes | `StudyPackageCache`: zips en disco con clave por contenido, tiempo de vida y escritura atómica. |
| 4. Integridad y legibilidad sin conexión | SHA-256 por archivo en `manifest.json` y del zip en cabecera; el navegador lo comprueba. PDF sin recursos remotos. Pruebas JUnit, Node, PowerShell y Playwright. |

## Qué contiene el paquete

| Archivo | Contenido |
|---|---|
| `guia-de-estudio.pdf` | Portada (ruta, grado, área, unidad, competencias MINEDU y «Cómo usar esta guía»), cada parada con instrucciones, textos, destacados, preguntas y materiales del profesor, y al final «Claves para revisar» en página aparte. |
| `materiales/parada-NN-<nombre>` | Archivos que el docente adjuntó a las paradas (HU-05/07). Solo entran si existen en disco y su SHA-256 coincide con el guardado al subirlos. |
| `contenido.json` | El mismo contenido estructurado, para una futura lectura offline dentro de la app. |
| `manifest.json` | Tamaño y SHA-256 de cada archivo y lista `materialesNoIncluidos`. |
| `LEEME.txt` | Explicación breve para familias. |

Reglas de contenido:

- Entran todas las paradas **publicadas** de la unidad, aunque en el mapa sigan bloqueadas: sirve para repasar e imprimir por adelantado. Las actividades en borrador o archivadas no entran.
- Solo se imprimen las preguntas guardadas en `evaluacion_pregunta`. El paquete no usa la siembra de preguntas genéricas de `EvaluationService`.
- Los enlaces web se imprimen como texto con el aviso «necesita internet». El PDF no tiene hipervínculos ni recursos remotos y usa fuentes estándar (Helvetica, con tildes, ñ, ¿ y ¡).
- El paquete no lleva datos del estudiante (nombre, avance), por eso se comparte en caché.

## Endpoint

```
GET /api/v1/student/learning-routes/{versionRouteId}/units/{unitId | otras}/package
```

| Caso | Respuesta |
|---|---|
| Sin sesión | 401 (filtro de `/api/v1/student/**`) |
| Ruta sin inscripción o no publicada | 404 (reutiliza `LearningRouteService.getForStudent`) |
| Unidad que no es de la ruta o identificador inválido | 404 |
| Unidad sin lecciones publicadas | 409 |
| Correcto | 200 `application/zip`, `Content-Disposition: attachment` |

Cabeceras propias: `X-Rupi-Paquete-Sha256` (hash del zip), `X-Rupi-Paquete-Version` (clave de contenido) y `X-Rupi-Paquete-Cache` (`HIT`/`MISS`). Se mantiene el `Cache-Control: no-store` global.

`otras` es la clave del grupo «Otras paradas» de HU-04 (paradas sin unidad).

## Caché

- Carpeta `uploads/package-cache/` (propiedad `rupi.storage.package-cache-path`). `uploads/` está en `.gitignore`.
- Clave: SHA-256 de todo el contenido de la unidad (paradas y su orden, textos, preguntas, adjuntos y hash de cada archivo) más `StudyPackageBuilder.FORMAT_VERSION`. Si el docente reordena (HU-06), cambia textos o adjunta materiales, la clave cambia sola: no hace falta invalidar a mano.
- Tiempo de vida: 168 h sin pedirse (`rupi.storage.package-cache-ttl-hours`). Cada acierto renueva la fecha; la limpieza borra zips vencidos y temporales abandonados.
- Un candado por clave: varias descargas simultáneas arman el paquete una sola vez. Se escribe en `.tmp` y se mueve de forma atómica.
- Cambiar la plantilla del PDF o la estructura del zip exige subir `FORMAT_VERSION`.

## Interfaz

Panel «Llévate tu unidad» debajo de la leyenda del mapa (`features/roadmap/UnitDownloadPanel.tsx`), una tarjeta por unidad en el orden de las bandas. El botón pasa por «Descargar», «Preparando» y «Otra vez». El navegador recalcula el SHA-256 y muestra «¡Listo y completo!» o pide repetir la descarga si no coincide.

Accesibilidad: botón amarillo maíz con texto verde bosque (contraste 8.3:1), mínimo 48 × 48 px, foco azul cielo de 3 px, sombra sólida que baja en `:active`, sin animación con `prefers-reduced-motion` y a ancho completo en móvil.

## Archivos

- Backend: `backend/api-spring-boot/src/main/java/pe/rupi/api/studypackage/` y `MaterialStorageService.diskPathFor`. Dependencia nueva: OpenPDF 2.2.5 (`com.github.librepdf:openpdf`, sin dependencias transitivas).
- Pruebas Java: `src/test/java/pe/rupi/api/studypackage/` (`StudyPackageBuilderTest`, `StudyPackageCacheTest`, `StudyPackageServiceTest`).
- Frontend: `features/roadmap/UnitDownloadPanel.tsx|.css`, `features/roadmap/unitDownloads.ts`, `downloadUnitPackage` en `services/learningRoutesApi.ts`. Prueba: `tests/unitDownloads.test.ts`.
- Scripts: `scripts/Verificar-HU12-Descarga.ps1` y `scripts/verify_hu12_pantalla.mjs`.

## Verificación (2026-10-13, Fedora 44)

| Prueba | Resultado |
|---|---|
| JUnit nuevas (6 + 5 + 8) | 19/19 |
| Suite Java completa | 85/85 |
| Node (`tests/*.test.ts`) | 17/17 |
| `npm run lint` y `npm run build` | correctos |
| `Verificar-HU12-Descarga.ps1` contra la API real (PowerShell 7.6) | 20/20 |
| `verify_hu12_pantalla.mjs` (Firefox, 1280 y 375 px) | 10/10 |
| Material del docente adjunto a una parada | entra al zip, el manifest cuadra y la versión del paquete cambia sola |

```bash
mvn -f backend/api-spring-boot/pom.xml test -Dtest='StudyPackage*'
node --experimental-strip-types --test frontend/web-react/tests/*.test.ts
~/pwsh-7/pwsh -NoProfile -File scripts/Verificar-HU12-Descarga.ps1
PLAYWRIGHT_DIR=~/pw-tmp node scripts/verify_hu12_pantalla.mjs http://127.0.0.1:5173 /tmp
```

Verificado el 13/10/2026 en Windows PowerShell 5.1 contra API/MySQL aislados: 10/10 comprobaciones para una ruta con una unidad.

## Ampliación: materiales de primaria y repaso

Ver [Biblioteca MINEDU y repaso descargable](HU-12-biblioteca-minedu.md). Añade 18 referencias por grado/materia y 36 ejercicios originales; formato del paquete 2. Los libros oficiales se descargan desde su fuente y los ejercicios Rupi se incluyen en el ZIP.
