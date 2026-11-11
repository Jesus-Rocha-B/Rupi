# HU-52 · Parametrizar los periodos

Acceso: Administración → Catálogo MINEDU → Periodos escolares.

El administrador selecciona un año escolar ya registrado, configura cuatro bimestres o tres trimestres y guarda el borrador. No se precargan fechas inventadas como calendario oficial. Se muestran las materias del catálogo compartido, agrupadas por nombre para las distintas versiones curriculares.

Después selecciona grados y materias, revisa las unidades y evaluaciones afectadas y pulsa **Aplicar periodos**. Se asocia cada planificación cuyo intervalo completo está dentro de un periodo; las demás se señalan como fuera de rango y se conservan sin cambios. No se mueven fechas ni se recrean recordatorios. El calendario del aula de docentes y estudiantes muestra la etiqueta aplicada.

## Integridad y persistencia

- Django escribe únicamente `curriculo_calendario_periodos`, `curriculo_periodo_planificacion` y su auditoría. Lee los años, aulas y planificaciones de Spring sin modificarlos.
- La configuración maestra se guarda por año escolar (que pertenece a una institución). No se usa la tabla operativa `escuela_periodo_academico` como segundo almacén: su escritor es Spring.
- Un borrador nuevo no sustituye las asociaciones aplicadas. Cada asociación conserva una instantánea de nombre y fechas y la revisión del catálogo aplicada.
- Las fechas civiles son de Perú; la planificación sigue almacenada en UTC. Se rechazan cruces, fechas invertidas, fuera del año y fuera de los límites del calendario cuando existen.
- Una revisión evita sobrescribir cambios de otro administrador. La vista previa contiene un token del alcance, las fechas de planificación y la revisión; si cambian, se exige revisar otra vez.
- Aplicar otra vez actualiza la asociación por planificación, sin duplicarla. Las nuevas planificaciones requieren revisar y aplicar de nuevo. Si el docente mueve un evento fuera de su periodo, Spring deja de mostrar la etiqueta anterior.
- No se elimina una planificación desde Django. Las asociaciones de eventos retirados no se consultan; no bloquean su eliminación en Spring.
- El permiso `curriculo.ver` permite consultar y revisar. `curriculo.gestionar` permite guardar y aplicar. Un año cerrado queda de solo lectura. Las escrituras exigen un Origin permitido.

## Contrato HTTP

Base: `/api/admin/v1`, sesión administrativa existente.

| Método y ruta | Uso |
|---|---|
| GET `curriculo/periodos/opciones` | Años e instituciones, grados, materias |
| GET `curriculo/periodos/{anioEscolarId}` | Borrador con `id`, `tipo`, `periodos`, `revision`, `editable` |
| PUT `curriculo/periodos/{anioEscolarId}` | Guarda `{tipo, periodos: [{comienza, termina}], revision}`; fechas ISO YYYY-MM-DD |
| POST `curriculo/periodos/{anioEscolarId}/revisar` | `{grado: UUID o "", areas: UUID[]}` → lista, `aplicables`, `token`, `revision` |
| POST `curriculo/periodos/{anioEscolarId}/aplicar` | Mismo alcance y `token` → `aplicadas`, `omitidas` |

Errores: 400 datos inválidos, 401 sin sesión, 403 permiso/origen, 404 año inexistente, 409 concurrencia/año cerrado/sin planes aplicables. El GET del calendario de rutas de Spring añade `periodName` nullable a cada evento sin cambiar sus campos existentes.

## Despliegue del equipo

1. Aplicar `database/mysql/migrations/V41__periodos_curriculares_hu52.sql` en la base compartida **antes de actualizar Spring**. Es idempotente y no modifica los datos anteriores.
2. Si se usan usuarios restringidos, volver a ejecutar los scripts de grants de Django y Spring; Django necesita escritura sobre las dos tablas nuevas y Spring solo lectura. Mantener credenciales fuera de Git.
3. Reiniciar Django, recompilar/reiniciar Spring y actualizar el frontend. Los servidores locales de cada compañero deben actualizarse aunque la base sea compartida.
4. Introducir las fechas del calendario aprobado, guardar, revisar y aplicar el alcance deseado.

## Verificación

- `python manage.py test curriculo`: validación de fechas, zona de Perú, concurrencia y permisos, además de regresiones HU-49/50.
- `mvn -Dtest=CalendarServiceTest,CalendarPeriodTest test`: calendario existente y etiquetas según fechas de Perú.
- `npm run build`: tipos TypeScript y producción React.
- Prueba transaccional en TiDB con rollback: guardar, rechazar revisión antigua, revisar, aplicar dos veces sin duplicados y conservar íntegra la planificación original.
- Navegador con respuestas controladas: guardar/revisar/aplicar, cambio a trimestres, bloqueo de fechas cruzadas, soporte de solo consulta y ausencia de desbordamiento en 390/768 px. Esta prueba no constituye una sesión autenticada real contra los tres servicios.
