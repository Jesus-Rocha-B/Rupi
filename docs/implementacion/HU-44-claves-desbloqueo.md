# HU-44 · Establecer contraseñas y desbloquear cuentas a pedido

Implementación completa: 29 de octubre de 2026.

> Como administrador del sistema, yo debería establecer contraseñas y desbloquear cuentas institucionales a solicitud a fin de asegurar la continuidad del acceso de docentes y alumnos.

## Subtareas completadas al 100%
1. **Diseñar la interfaz web para buscar usuarios, establecer contraseñas y desbloquear cuentas institucionales:**
   - Panel en React (`AccountPanel.tsx`, `AccessBlock.tsx`) con estados de clave, intentos fallidos, generación de claves temporales con boleta copiable (`TemporaryPasswordTicket.tsx`) y desbloqueo.
2. **Desarrollar el servicio backend que procese restablecimiento y desbloqueo:**
   - Django (`identidad/credenciales.py`): endpoints `POST /api/admin/v1/cuentas/{id}/clave` y `POST /api/admin/v1/cuentas/{id}/desbloquear`.
   - Cierre inmediato de sesiones activas concurrentes.
3. **Modificar la base de datos para guardar la nueva contraseña y el estado activo:**
   - `V34__credenciales_desbloqueo_hu44.sql` sobre `identidad_cuenta_usuario` con hash seguro (`bcrypt` / `pbkdf2`), `clave_cambiada_en`, `bloqueado_hasta` y estados.
4. **Programar el envío de correos automáticos notificando al usuario la recuperación de su cuenta:**
   - Django (`identidad/notificaciones.py`): `notificar_recuperacion_clave` y `notificar_cuenta_desbloqueada`.
   - Envía correo automático al usuario institucional notificando la nueva clave/desbloqueo e informa al administrador en el panel web.

## Reglas de seguridad
- Nadie cambia el acceso de su propia cuenta.
- Una cuenta dada de baja primero se reactiva.
- La mesa de ayuda no toca cuentas de consola.
- Nunca se bloquea a la última administración activa.
- Todo queda auditado en `operaciones_evento_auditoria`.
