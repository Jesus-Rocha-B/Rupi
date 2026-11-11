# HU-50 · Registrar y modificar metas y estándares

Acceso: `/admin` → Catálogo curricular → Metas y estándares. Reutiliza el logo original `/rupi-logo.jpg`, botones administrativos compactos, filtros de catálogo/grado/área/unidad y edición lateral. SOPORTE consulta; el permiso `curriculo.gestionar` habilita registro y corrección.

Se reutilizan `curriculo_meta_aprendizaje`, `curriculo_estandar` y sus tablas de vínculo por unidad. Django es el único escritor. La migración V40 agrega `curriculo_registro_revision` para guardar antes/después, motivo, autor y fecha; las acciones también se auditan. No se crean ejemplos ficticios como estándares oficiales.

## Contrato HTTP

Base `/api/admin/v1`, cookie de administrador y Origin permitido en escrituras:

* `GET curriculo/unidades?version=UUID`: unidades de esa versión, con grado y área.
* `GET curriculo/unidades/{unidad}/registros`: competencias vinculadas, metas, estándares e historial (últimas 20 entradas por registro).
* `POST curriculo/unidades/{unidad}/registros`: `{tipo: "META" | "ESTANDAR", codigo, descripcion, competenciaId}`. Devuelve 201 y la lista actualizada.
* `PATCH curriculo/unidades/{unidad}/registros/{tipo}/{registro}`: `{descripcion, motivo, revision}`. El token `revision` recibido en GET protege contra sobrescritura concurrente; un conflicto devuelve 409.

Se valida que la competencia pertenezca a la unidad, área y versión. La creación toma el grado de la unidad. No se aceptan campos de vínculo en PATCH. El código es estable, de 2 a 60 caracteres; descripción de 10 a 4000; motivo de 5 a 500. No se escribe en versiones retiradas. Los registros compartidos se corrigen en todas sus unidades, con aviso explícito. Un registro vinculado a una versión retirada no puede modificarse. No se eliminan registros.

Aplicar V40 en la misma base compartida y actualizar los permisos del usuario Django mediante el script de grants si es una cuenta restringida. Reiniciar Django después de actualizar el código. No requiere cambios de Spring.
