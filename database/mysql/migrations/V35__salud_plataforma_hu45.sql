-- V35__salud_plataforma_hu45.sql
-- Monitorear la salud de los servidores y el tiempo de respuesta de las operaciones (HU-45)
--
-- Solo la administración ve la salud de la plataforma. Los tiempos de respuesta se miden en memoria en la
-- API Spring Boot (pe.rupi.api.monitor) y la consola los consulta con RUPI_MONITOR_TOKEN: no se crean tablas.
-- Escritor: Django (identidad_*). Idempotente con INSERT IGNORE.

INSERT IGNORE INTO identidad_permiso (id, codigo, descripcion) VALUES
('a0450000-0000-4000-8000-000000000101', 'plataforma.monitorear', 'Ver la salud de los servidores y los tiempos de respuesta');

INSERT IGNORE INTO identidad_rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM identidad_rol r
JOIN identidad_permiso p ON p.codigo = 'plataforma.monitorear'
WHERE r.codigo = 'ADMINISTRADOR';
