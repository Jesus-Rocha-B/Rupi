-- V33__sesion_unica_web_movil_hu43.sql
-- Sesión única sincronizada entre la web y el celular (HU-43)
--
-- La regla ya la garantiza MySQL: identidad_sesion_usuario.usuario_activo_id (columna generada) es UNIQUE,
-- así que una cuenta no puede tener dos sesiones activas a la vez. Spring registra si la sesión es de la web
-- (WEB_USUARIO) o del celular (MOVIL) y explica en el 401 por qué se cerró la anterior.
-- Esta migración solo agrega el permiso para ver y cerrar sesiones desde la consola.
-- Escritor: Django (identidad_*). Idempotente con INSERT IGNORE. No crea tablas.

INSERT IGNORE INTO identidad_permiso (id, codigo, descripcion) VALUES
('a0430000-0000-4000-8000-000000000101', 'sesiones.gestionar', 'Ver dónde está abierta cada cuenta y cerrar su sesión');

INSERT IGNORE INTO identidad_rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM identidad_rol r
JOIN identidad_permiso p ON p.codigo = 'sesiones.gestionar'
WHERE r.codigo IN ('ADMINISTRADOR', 'SOPORTE');
