-- V31__consola_administracion_cuentas_hu41.sql
-- Consola de administración: altas, bajas y modificaciones de cuentas (HU-41)
--
-- Agrega el rol ADMINISTRADOR y los dos primeros permisos de la consola. La primera cuenta de
-- administración se crea con `python manage.py crear_administrador` (backend/admin-django), nunca
-- con una contraseña escrita en una migración.
-- Escritor: Django (identidad_*). Idempotente con INSERT IGNORE. No crea tablas.

INSERT IGNORE INTO identidad_rol (id, codigo, nombre, descripcion) VALUES
('a0410000-0000-4000-8000-000000000001', 'ADMINISTRADOR', 'Administración de la plataforma',
 'Gestiona cuentas, roles, sesiones y la salud de Rupi desde la consola de administración.');

INSERT IGNORE INTO identidad_permiso (id, codigo, descripcion) VALUES
('a0410000-0000-4000-8000-000000000101', 'cuentas.ver', 'Consultar cuentas de docentes y estudiantes'),
('a0410000-0000-4000-8000-000000000102', 'cuentas.gestionar', 'Crear, modificar, dar de baja y reactivar cuentas');

INSERT IGNORE INTO identidad_rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM identidad_rol r
JOIN identidad_permiso p ON p.codigo IN ('cuentas.ver', 'cuentas.gestionar')
WHERE r.codigo = 'ADMINISTRADOR';
