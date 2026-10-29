-- V34__credenciales_desbloqueo_hu44.sql
-- Establecer contraseñas y desbloquear cuentas institucionales a pedido (HU-44)
--
-- Agrega el permiso para restablecer contraseñas, bloquear y desbloquear cuentas. Lo tienen la
-- administración y la mesa de ayuda; la mesa de ayuda no puede tocar el acceso de cuentas de consola
-- (regla en backend/admin-django/identidad/credenciales.py), así nadie se apropia de una cuenta con más permisos.
-- Escritor: Django (identidad_*). Idempotente con INSERT IGNORE. No crea tablas.

INSERT IGNORE INTO identidad_permiso (id, codigo, descripcion) VALUES
('a0440000-0000-4000-8000-000000000101', 'credenciales.restablecer', 'Restablecer contraseñas, bloquear y desbloquear cuentas');

INSERT IGNORE INTO identidad_rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM identidad_rol r
JOIN identidad_permiso p ON p.codigo = 'credenciales.restablecer'
WHERE r.codigo IN ('ADMINISTRADOR', 'SOPORTE');
