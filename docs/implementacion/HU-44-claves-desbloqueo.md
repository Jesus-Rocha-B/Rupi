# HU-44 · Establecer contraseñas y desbloquear cuentas a pedido

Implementación: 29 de octubre de 2026 · commit `62cca73`.

> Como administrador del sistema, quiero establecer contraseñas y desbloquear cuentas institucionales a pedido, para asegurar la continuidad del acceso de docentes y alumnos.

## Criterios de aceptación (definidos en la implementación)
- **Restablecer la contraseña** de una cuenta, a pedido de la docente o de la familia:
  - generando una clave temporal fácil de dictar, que se muestra una sola vez;
  - o escribiendo una (8 caracteres como mínimo, sin el nombre de usuario y sin repetir siempre lo mismo). La clave escrita no se devuelve en ninguna respuesta.
- Al cambiar la clave se cierra la sesión abierta y la clave anterior deja de servir.
- **Bloquear** una cuenta (por ejemplo, si la clave se compartió) y **desbloquearla** cuando se pida.
- Quien tiene la cuenta bloqueada y escribe bien su clave ve «Tu cuenta está bloqueada. Pide a tu profe que solicite el desbloqueo.». Con una clave equivocada recibe el mensaje de siempre, sin revelar el estado.
- El panel muestra cuándo se cambió la clave y cuántos intentos fallidos hubo en los últimos 15 minutos.
- **Reglas:**
  - nadie cambia el acceso de su propia cuenta;
  - una cuenta dada de baja primero se reactiva;
  - la mesa de ayuda no toca cuentas de consola;
  - nunca se bloquea a la última administración activa.
- Todo queda auditado: `CLAVE_RESTABLECIDA`, `CUENTA_BLOQUEADA` y `CUENTA_DESBLOQUEADA`.

## Datos · `V34__credenciales_desbloqueo_hu44.sql`
Permiso `credenciales.restablecer` para administración y mesa de ayuda. No crea tablas: usa `hash_clave`, `clave_cambiada_en`, `estado` y `bloqueado_hasta` de `identidad_cuenta_usuario`.

## Django · `identidad/credenciales.py`
`restablecer_clave`, `bloquear` y `desbloquear`. `_proteger` aplica las reglas.

¿Por qué la mesa de ayuda no puede tocar cuentas de consola? Porque si pudiera cambiar la clave de una administradora, entraría con su cuenta y tendría más permisos que los suyos.

## Spring
`AuthenticationResult.locked`. Si la clave es correcta y la cuenta está `BLOQUEADO` (o con `bloqueado_hasta` futuro), el login responde **423** con el texto anterior. La web lo muestra en el ingreso y Android lo toma del campo `detail`.

## API
- `POST /cuentas/{id}/clave` con `{"clave"?: "..."}`.
- `POST /cuentas/{id}/bloquear` y `POST /cuentas/{id}/desbloquear`.
- `GET /cuentas/{id}` agrega `claveCambiadaEn` e `intentosFallidos`.

## Uso
Bloque «Acceso» en el panel de cada cuenta:
- datos de la clave e intentos fallidos;
- «Generar clave temporal», que muestra la boleta punteada;
- «Escribir una contraseña», con confirmación;
- «Bloquear cuenta», con confirmación, o «Desbloquear».

## Pruebas
- `LockedAccountTest` (2): 423 sin cookie y bloqueada no equivale a autenticada.
- `verify_admin_cuentas.py` (+20 comprobaciones de HU-44):
  - bloquear da 423 solo con la clave correcta;
  - desbloquear muestra los intentos fallidos;
  - la clave temporal corta la sesión y anula la clave anterior;
  - se rechazan claves cortas o con el usuario;
  - la clave escrita funciona y no se devuelve;
  - nadie cambia su propio acceso (409);
  - la mesa de ayuda restablece a una estudiante, pero no puede tocar ni bloquear a la administración (403);
  - una cuenta de baja da 409;
  - todo queda auditado.
- Playwright a 375 y 1280 px.

## Pendiente
- El freno de Spring (5 fallos en 5 minutos por cuenta y equipo) no se levanta al desbloquear: se libera solo a los 5 minutos. Las políticas de bloqueo automático son la HU-58.
