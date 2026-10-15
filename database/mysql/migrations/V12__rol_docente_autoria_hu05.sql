-- =============================================================================
-- Migración V12: Rol Docente, Autoría de Roadmaps y Recursos Didácticos (HU-05)
-- Principio de Escritor Único (AGENTS.md):
--   Django escribe identidad_* y curriculo_*.
--   Spring Boot escribe aprendizaje_*, evaluacion_*, gamificacion_* y escuela_*.
-- =============================================================================

-- Corrección 2026-10-14: la versión original no corría sobre schema.sql (columnas es_sistema y creado_en
-- inexistentes en identidad_rol, y aula e3000000-… fija). Se corrige aquí y no en una V nueva porque
-- la migración fallaba antes de aplicar nada: ninguna base armada desde schema.sql pudo tenerla aplicada.

USE rupi;
SET NAMES utf8mb4;

-- 1. Asegurar roles de sistema en identidad_rol
INSERT INTO identidad_rol (id, codigo, nombre, descripcion)
VALUES
  ('b1000000-0000-4000-8000-000000000001', 'ESTUDIANTE', 'Estudiante de Primaria', 'Estudiante de primaria que realiza las rutas escolares'),
  ('b1000000-0000-4000-8000-000000000002', 'DOCENTE', 'Profesor de Primaria', 'Docente responsable de aula y autor de rutas de aprendizaje')
ON DUPLICATE KEY UPDATE
  nombre = VALUES(nombre),
  descripcion = VALUES(descripcion);

-- 2. Asignar rol ESTUDIANTE al estudiante demo
INSERT INTO identidad_usuario_rol (usuario_id, rol_id, asignado_en)
VALUES (
  '11111111-1111-4111-8111-111111111111',
  'b1000000-0000-4000-8000-000000000001',
  UTC_TIMESTAMP(6)
) ON DUPLICATE KEY UPDATE asignado_en = VALUES(asignado_en);

-- 3. Crear cuenta demo para Profesor (profesor.demo / 123456)
INSERT INTO identidad_cuenta_usuario (
  id, nombre_usuario, correo, hash_clave, estado, desactivado_en, creado_en
) VALUES (
  '22222222-2222-4222-8222-222222222222',
  'profesor.demo',
  'profesor.demo@rupi.pe',
  'pbkdf2_sha256$600000$rupi-dev-hu01$8LkMHYbhvu/QFEd17e3Ilkb37nI2vSgtJUg4jaCaNjE=',
  'ACTIVO',
  NULL,
  UTC_TIMESTAMP(6)
) ON DUPLICATE KEY UPDATE
  estado = 'ACTIVO',
  hash_clave = VALUES(hash_clave);

-- 4. Perfil del docente Prof. Carlos Mendoza
INSERT INTO identidad_perfil_usuario (
  usuario_id, nombre, apellido, nombre_preferido, alias_publico, idioma, zona_horaria
) VALUES (
  '22222222-2222-4222-8222-222222222222',
  'Carlos',
  'Mendoza',
  'Prof. Carlos',
  'prof.carlos',
  'es-PE',
  'America/Lima'
) ON DUPLICATE KEY UPDATE
  nombre = 'Carlos',
  nombre_preferido = 'Prof. Carlos';

-- 5. Asignar rol DOCENTE al profesor demo
INSERT INTO identidad_usuario_rol (usuario_id, rol_id, asignado_en)
VALUES (
  '22222222-2222-4222-8222-222222222222',
  'b1000000-0000-4000-8000-000000000002',
  UTC_TIMESTAMP(6)
) ON DUPLICATE KEY UPDATE asignado_en = VALUES(asignado_en);

-- 6. Membresía DOCENTE en el aula del alumno demo (I.E. 38001 Mariscal Sucre).
-- El aula se busca por la membresía del alumno: según el seed, su id es e3000000-… o au000000-….
INSERT INTO escuela_membresia_aula (
  id, aula_id, usuario_id, rol, estado, ingreso_en
)
SELECT
  'e4000000-0000-4000-8000-000000000002',
  m.aula_id,
  '22222222-2222-4222-8222-222222222222',
  'DOCENTE',
  'ACTIVO',
  UTC_TIMESTAMP(6)
FROM escuela_membresia_aula m
WHERE m.usuario_id = '11111111-1111-4111-8111-111111111111'
  AND m.rol = 'ESTUDIANTE'
  AND m.estado = 'ACTIVO'
ORDER BY m.ingreso_en, m.aula_id
LIMIT 1
ON DUPLICATE KEY UPDATE rol = 'DOCENTE', estado = 'ACTIVO', retiro_en = NULL;

-- 7. Tabla de recursos multimedia (archivos PDF, PPT, imágenes): misma definición que schema.sql.
-- Solo crea la tabla si la base no la tiene; el código usa tamano_bytes, clave_almacenamiento y url_externa.
CREATE TABLE IF NOT EXISTS recurso_archivo_multimedia (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  creado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  proveedor_almacenamiento VARCHAR(40) NULL,
  clave_almacenamiento VARCHAR(500) NULL,
  url_externa VARCHAR(1000) NULL,
  nombre_original VARCHAR(255) NOT NULL,
  tipo_mime VARCHAR(120) NOT NULL,
  tamano_bytes BIGINT UNSIGNED NULL,
  hash_sha256 BINARY(32) NULL,
  texto_accesibilidad VARCHAR(500) NULL,
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  FOREIGN KEY (creado_por) REFERENCES identidad_cuenta_usuario(id),
  CHECK ((clave_almacenamiento IS NOT NULL) <> (url_externa IS NOT NULL))
) ENGINE=InnoDB;

-- 8. Tabla de recursos adjuntos de autoría vinculados a nodos de rutas
CREATE TABLE IF NOT EXISTS aprendizaje_nodo_recurso (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  nodo_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  tipo ENUM('DOCUMENTO', 'ENLACE', 'PRESENTACION', 'IMAGEN') NOT NULL,
  titulo VARCHAR(180) NOT NULL,
  url_recurso VARCHAR(500) NOT NULL,
  archivo_multimedia_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  FOREIGN KEY (nodo_id) REFERENCES aprendizaje_nodo_ruta(id) ON DELETE CASCADE,
  FOREIGN KEY (archivo_multimedia_id) REFERENCES recurso_archivo_multimedia(id) ON DELETE SET NULL
) ENGINE=InnoDB;
