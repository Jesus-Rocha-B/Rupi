-- V32__roles_permisos_estrictos_hu42.sql
-- Roles y permisos estrictos por perfil (HU-42)
--
-- Los permisos de cada rol se fijan aquí, versionados y revisados; la consola solo asigna roles a
-- las cuentas. Se agrega SOPORTE (mesa de ayuda), que entra a la consola sin poder crear cuentas,
-- asignar roles ni ver datos personales completos. ESTUDIANTE y DOCENTE no entran a la consola.
-- Escritor: Django (identidad_*). Idempotente con INSERT IGNORE. No crea tablas.

INSERT IGNORE INTO identidad_rol (id, codigo, nombre, descripcion) VALUES
('a0420000-0000-4000-8000-000000000001', 'SOPORTE', 'Mesa de ayuda',
 'Atiende pedidos de docentes y familias: consulta cuentas sin ver sus datos personales completos.');

INSERT IGNORE INTO identidad_permiso (id, codigo, descripcion) VALUES
('a0420000-0000-4000-8000-000000000101', 'consola.ingresar', 'Ingresar a la consola de administración'),
('a0420000-0000-4000-8000-000000000102', 'roles.asignar', 'Asignar y retirar roles de las cuentas'),
('a0420000-0000-4000-8000-000000000103', 'cuentas.datos_personales', 'Ver correos y datos personales completos de otras personas');

INSERT IGNORE INTO identidad_rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM identidad_rol r
JOIN identidad_permiso p ON p.codigo IN ('consola.ingresar', 'roles.asignar', 'cuentas.datos_personales')
WHERE r.codigo = 'ADMINISTRADOR';

INSERT IGNORE INTO identidad_rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM identidad_rol r
JOIN identidad_permiso p ON p.codigo IN ('consola.ingresar', 'cuentas.ver')
WHERE r.codigo = 'SOPORTE';
