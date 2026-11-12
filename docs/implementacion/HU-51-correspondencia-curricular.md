# HU-51 · Actualizar la correspondencia curricular ante resoluciones ministeriales

Implementación: 11 de noviembre de 2026.

> Como administrador de la plataforma, yo debería actualizar la correspondencia curricular ante resoluciones ministeriales anuales a fin de mantener el contenido de RUPI vigente en los colegios.

## Criterios de aceptación (definidos en la implementación)
- La administración registra la norma: número, fecha de emisión, resumen de cambios, enlace oficial, nombre de la nueva versión y el inicio de su vigencia.
- Registrar no cambia nada para docentes ni estudiantes: se crea una versión **BORRADOR** copiada de la vigente.
- La correspondencia muestra cada área, competencia y unidad como **se mantiene**, **modificada**, **retirada** o **nueva**, con el sustento de la norma.
- Publicar exige escribir el número exacto de la resolución. La versión nueva queda vigente y la anterior **retirada** como registro, sin borrarse.
- Cada docente ve en el editor de su ruta qué pasa con sus unidades y la actualiza en un paso, sin perder paradas ni el avance de sus estudiantes.
- SOPORTE solo consulta (`curriculo.ver`); registrar, corregir y publicar exige `curriculo.gestionar`.

## Flujo
1. **Consola → Catálogo MINEDU → Resoluciones → Registrar resolución.** Se copian áreas, competencias, capacidades, unidades, metas, estándares y sus vínculos, y se crea una fila de correspondencia por cada elemento. Solo puede haber una resolución en borrador a la vez.
2. **Revisar la correspondencia.**
   - *Unidades:* se mantiene, cambia el título o se retira; retirar y corregir piden sustento.
   - *Competencias:* se mantiene o se retira. Para renombrarlas o agregar áreas y competencias se usa «Grados y competencias» con la versión en borrador (HU-49), y la correspondencia las muestra como modificadas o nuevas.
   - El impacto indica cuántas rutas siguen en la versión anterior y cuántas paradas quedarán sin unidad.
3. **Publicar.** Lo marcado como retirado se quita del borrador (con sus vínculos, capacidades, metas y estándares), lo renombrado queda guardado como `MODIFICADA` y cambian los estados de las versiones.
4. **Docente → editor de la ruta.** Si la versión de la ruta está retirada aparece «El MINEDU actualizó el currículo de esta ruta», con la norma, el detalle por unidad y el botón «Actualizar mi ruta a …».

## Datos · `V45__correspondencia_curricular_hu51.sql`
- `curriculo_resolucion`: norma, versión de origen y destino, quién la registró y quién la publicó (fechas en UTC).
- `curriculo_correspondencia`: `tipo` (AREA, COMPETENCIA, CAPACIDAD, UNIDAD, META, ESTANDAR), `origen_id`, `destino_id`, `cambio` y `nota`. Lo nuevo no tiene fila: se calcula al consultar.

Escritor único: Django (`curriculo_*`). Después de V45 hay que volver a ejecutar `grants-django-admin.sql`. El script agrega DELETE en las tablas del catálogo que se depuran al publicar; el código solo borra filas de la versión en borrador.

## API
**Django** (`/api/admin/v1`, sesión administrativa y Origin permitido en escrituras):

| Método y ruta | Uso |
|---|---|
| `GET curriculo/resoluciones` | Lista, versión vigente y si se puede registrar otra. |
| `POST curriculo/resoluciones` | Registra la norma y copia la versión vigente (201). |
| `GET/PATCH curriculo/resoluciones/{id}` | Detalle con correspondencia, resumen e impacto / corrige los datos de la norma en borrador. |
| `PATCH curriculo/resoluciones/{id}/correspondencias/{fila}` | `{accion: MANTENER \| CORREGIR \| RETIRAR, titulo?, nota?}`. |
| `POST curriculo/resoluciones/{id}/publicar` | `{confirmacion: "<número exacto>"}`. |

**Spring** (docente dueño de la ruta):

| Método y ruta | Uso |
|---|---|
| `GET /api/v1/teacher/learning-routes/{ruta}/curriculum-update` | Plan: normas, área, cambio por unidad y paradas que quedarán sin unidad. |
| `POST /api/v1/teacher/learning-routes/{ruta}/curriculum-update` | Aplica el plan (exige Origin). Actualiza `aprendizaje_version_ruta`, `aprendizaje_nodo_ruta.unidad_id` y la unidad de lo programado en `escuela_planificacion_ruta`. |

Spring sigue la cadena de resoluciones publicadas desde la versión de la ruta hasta la vigente, así que una ruta que se saltó un año también se actualiza. Si la norma retiró el área de la ruta, no se ofrece la actualización y se indica crear una ruta nueva.

## Límites conocidos
- Las paradas de unidades retiradas quedan en «Otras paradas» hasta que el docente les asigne otra unidad.
- Las recomendaciones de refuerzo y la relación actividad–unidad anteriores conservan la unidad de la versión retirada como historial.
- Las competencias se renombran desde «Grados y competencias», no desde esta pantalla.
- Una resolución en borrador no se descarta: se corrige hasta publicarla.

## Verificación
- `curriculo/test_resoluciones.py` (10 pruebas): fechas, enlace https, campos extra, cambio efectivo, sustento obligatorio, publicada de solo lectura, confirmación del número y permisos de SOPORTE.
- `scripts/verify_hu51_correspondencia.py` (29 comprobaciones contra Django, Spring y MySQL reales): copia completa, borrador único, retirar y corregir, publicación, plan y actualización de la ruta, 403 sin Origin y estudiante sin acceso. Deja la base como estaba.
- Playwright a 1280 y 375 px: consola de resoluciones y aviso del docente sin desborde.
