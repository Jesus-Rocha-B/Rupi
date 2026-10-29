# HU-42 · Roles y permisos estrictos por perfil

Implementación: 28 de octubre de 2026 · commit `153b53b`.

> Como administrador del sistema, quiero asignar roles y permisos estrictos a cada perfil de usuario, para restringir el acceso a módulos administrativos o a datos personales de terceros.

## Criterios de aceptación (definidos en la implementación)
- Cada rol trae permisos fijos y la consola solo asigna roles a las cuentas.
- Solo entra a la consola quien tenga el permiso `consola.ingresar`. Cada sección y cada botón dependen de un permiso.
- **Mesa de ayuda (`SOPORTE`):**
  - puede consultar cuentas;
  - no puede crearlas, darlas de baja ni asignar roles;
  - ve los correos enmascarados.
- **Reglas al asignar roles:**
  - toda cuenta tiene al menos un rol;
  - `ESTUDIANTE` no se combina con otros roles;
  - nadie se quita su propio rol de administración;
  - siempre queda una administración activa.
- Cambiar los roles cierra la sesión abierta de esa cuenta. Los permisos se leen en cada petición, así que retirar un rol corta el acceso de inmediato.
- Una cuenta que solo tiene roles de consola no entra a la web ni al celular.

## Datos · `V32__roles_permisos_estrictos_hu42.sql`
- Rol `SOPORTE` (Mesa de ayuda).
- Permisos `consola.ingresar`, `roles.asignar` y `cuentas.datos_personales`.

| Permiso | Estudiante | Docente | Mesa de ayuda | Administración |
|---|---|---|---|---|
| `consola.ingresar` | — | — | ✓ | ✓ |
| `cuentas.ver` | — | — | ✓ | ✓ |
| `cuentas.gestionar` | — | — | — | ✓ |
| `cuentas.datos_personales` | — | — | — | ✓ |
| `roles.asignar` | — | — | — | ✓ |

Cambiar lo que puede un rol exige una migración revisada: nadie amplía permisos desde la consola sin dejar rastro en el repositorio. `grants-django-admin.sql` suma `DELETE` solo sobre `identidad_usuario_rol`, para poder retirar roles.

## Datos personales de terceros
- Sin `cuentas.datos_personales`, el correo llega como `l•••@rupi.pe` y `datosCompletos` es `false`.
- Tampoco se puede buscar por correo, porque probar letras revelaría el dato.
- En ese caso el formulario de edición no reenvía el correo.

## Spring
- `StudentSessionService.isConsoleOnly`: si la cuenta tiene roles y ninguno es `ESTUDIANTE` o `DOCENTE`, el ingreso a la web o al celular responde 403 con el texto «Esta cuenta es de administración: ingresa desde la consola /admin.».
- Las cuentas sin ningún rol siguen entrando como estudiantes, igual que antes.
- La web muestra ese mensaje en el ingreso.

## API
- `GET /roles`: roles con sus permisos, si usan la consola y cuántas cuentas tienen.
- `PUT /cuentas/{id}/roles` con `{"roles": [...]}`: exige `roles.asignar`. Audita `ROL_ASIGNADO:<rol>` y `ROL_RETIRADO:<rol>`.

## Uso
- **Pestaña «Roles y permisos»:** fichas de cada rol y la matriz de permisos (solo lectura).
- **Panel de cada cuenta:** bloque «Roles» con casillas. Avisa de las reglas antes de guardar.

## Pruebas
- `ConsoleOnlyAccountTest` (4): solo consola, docente que también administra, cuenta sin roles y 403 sin cookie.
- `manage.py test` (+4): combinaciones de roles y correo enmascarado.
- `verify_admin_cuentas.py` (+18 comprobaciones de HU-42):
  - estudiante con otro rol: 409;
  - lista vacía: 409;
  - rol inexistente: 400;
  - quitarse el propio rol de administración: 409;
  - pasar de docente a mesa de ayuda cierra la sesión web;
  - Spring responde 403;
  - mesa de ayuda ve el correo enmascarado y no puede crear, dar de baja ni ascenderse;
  - al retirarle el rol pierde la consola en la siguiente petición.
- Playwright a 375 y 1280 px.
