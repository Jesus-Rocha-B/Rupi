-- V28__leaderboard_aula_hu32.sql
-- Tabla de logros y Leaderboard del aula con opciones de privacidad (HU-32)

-- 1. Añadir columna mostrar_como en identidad_perfil_usuario de forma idempotente
SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS 
  WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'identidad_perfil_usuario' 
    AND COLUMN_NAME = 'mostrar_como'
);
SET @sql = IF(@col_exists = 0, 
  'ALTER TABLE identidad_perfil_usuario ADD COLUMN mostrar_como ENUM(\'NOMBRE\', \'ALIAS\') NOT NULL DEFAULT \'ALIAS\'', 
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Asegurar que Mateo Quispe (estudiante demo) participe en la tabla por defecto con su alias
UPDATE identidad_perfil_usuario
SET mostrar_en_clasificacion = 1,
    mostrar_como = 'ALIAS'
WHERE usuario_id = '11111111-1111-4111-8111-111111111111';

-- 3. Crear compañeros de aula simulados para AULA-3A (3.° Primaria A: e3000000-0000-4000-8000-000000000003)
-- Cuentas de usuario
INSERT INTO identidad_cuenta_usuario (id, nombre_usuario, correo, hash_clave, estado) VALUES
  ('d3000000-0000-4000-8000-000000000001', 'valentina.flores', 'valentina.flores@rupi.pe', 'pbkdf2_sha256$600000$dummy$hash1', 'ACTIVO'),
  ('d3000000-0000-4000-8000-000000000002', 'thiago.huaman', 'thiago.huaman@rupi.pe', 'pbkdf2_sha256$600000$dummy$hash2', 'ACTIVO'),
  ('d3000000-0000-4000-8000-000000000003', 'camila.ramos', 'camila.ramos@rupi.pe', 'pbkdf2_sha256$600000$dummy$hash3', 'ACTIVO'),
  ('d3000000-0000-4000-8000-000000000004', 'rodrigo.mamani', 'rodrigo.mamani@rupi.pe', 'pbkdf2_sha256$600000$dummy$hash4', 'ACTIVO'),
  ('d3000000-0000-4000-8000-000000000005', 'lucia.quispe', 'lucia.quispe@rupi.pe', 'pbkdf2_sha256$600000$dummy$hash5', 'ACTIVO'),
  ('d3000000-0000-4000-8000-000000000006', 'diego.sanchez', 'diego.sanchez@rupi.pe', 'pbkdf2_sha256$600000$dummy$hash6', 'ACTIVO'),
  ('d3000000-0000-4000-8000-000000000007', 'valeria.mendoza', 'valeria.mendoza@rupi.pe', 'pbkdf2_sha256$600000$dummy$hash7', 'ACTIVO'),
  ('d3000000-0000-4000-8000-000000000008', 'joaquin.vargas', 'joaquin.vargas@rupi.pe', 'pbkdf2_sha256$600000$dummy$hash8', 'ACTIVO'),
  ('d3000000-0000-4000-8000-000000000009', 'mia.ccoyllo', 'mia.ccoyllo@rupi.pe', 'pbkdf2_sha256$600000$dummy$hash9', 'ACTIVO'),
  ('d3000000-0000-4000-8000-000000000010', 'gabriel.yupanqui', 'gabriel.yupanqui@rupi.pe', 'pbkdf2_sha256$600000$dummy$hash10', 'ACTIVO')
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

-- Perfiles con alias peruanos amigables
INSERT INTO identidad_perfil_usuario (usuario_id, nombre, apellido, nombre_preferido, alias_publico, mostrar_en_clasificacion, mostrar_como, idioma, zona_horaria) VALUES
  ('d3000000-0000-4000-8000-000000000001', 'Valentina', 'Flores', 'Valen', 'valen_estrella', 1, 'ALIAS', 'es-PE', 'America/Lima'),
  ('d3000000-0000-4000-8000-000000000002', 'Thiago', 'Huamán', 'Thiago', 'chaski_rapido', 1, 'NOMBRE', 'es-PE', 'America/Lima'),
  ('d3000000-0000-4000-8000-000000000003', 'Camila', 'Ramos', 'Cami', 'vicunita_andina', 1, 'ALIAS', 'es-PE', 'America/Lima'),
  ('d3000000-0000-4000-8000-000000000004', 'Rodrigo', 'Mamani', 'Rodri', 'puma_lector', 1, 'ALIAS', 'es-PE', 'America/Lima'),
  ('d3000000-0000-4000-8000-000000000005', 'Lucía', 'Quispe', 'Lu', 'flor_de_retablo', 1, 'NOMBRE', 'es-PE', 'America/Lima'),
  ('d3000000-0000-4000-8000-000000000006', 'Diego', 'Sánchez', 'Diego', 'explorador_sucre', 1, 'ALIAS', 'es-PE', 'America/Lima'),
  ('d3000000-0000-4000-8000-000000000007', 'Valeria', 'Mendoza', 'Vale', 'colibri_dorado', 1, 'ALIAS', 'es-PE', 'America/Lima'),
  ('d3000000-0000-4000-8000-000000000008', 'Joaquín', 'Vargas', 'Joaco', 'intiwawa_3a', 1, 'ALIAS', 'es-PE', 'America/Lima'),
  ('d3000000-0000-4000-8000-000000000009', 'Mia', 'Ccoyllo', 'Mia', 'runa_sabia', 1, 'ALIAS', 'es-PE', 'America/Lima'),
  ('d3000000-0000-4000-8000-000000000010', 'Gabriel', 'Yupanqui', 'Gabo', 'condor_andino', 0, 'ALIAS', 'es-PE', 'America/Lima')
ON DUPLICATE KEY UPDATE mostrar_en_clasificacion = VALUES(mostrar_en_clasificacion), mostrar_como = VALUES(mostrar_como);

-- Rol de estudiante
INSERT INTO identidad_usuario_rol (usuario_id, rol_id, asignado_en)
SELECT u.id, r.id, NOW(6)
FROM (
  SELECT 'd3000000-0000-4000-8000-000000000001' AS id UNION ALL
  SELECT 'd3000000-0000-4000-8000-000000000002' UNION ALL
  SELECT 'd3000000-0000-4000-8000-000000000003' UNION ALL
  SELECT 'd3000000-0000-4000-8000-000000000004' UNION ALL
  SELECT 'd3000000-0000-4000-8000-000000000005' UNION ALL
  SELECT 'd3000000-0000-4000-8000-000000000006' UNION ALL
  SELECT 'd3000000-0000-4000-8000-000000000007' UNION ALL
  SELECT 'd3000000-0000-4000-8000-000000000008' UNION ALL
  SELECT 'd3000000-0000-4000-8000-000000000009' UNION ALL
  SELECT 'd3000000-0000-4000-8000-000000000010'
) u
CROSS JOIN identidad_rol r
WHERE r.codigo = 'ESTUDIANTE'
ON DUPLICATE KEY UPDATE asignado_en = VALUES(asignado_en);

-- Membresías en el aula AULA-3A (e3000000-0000-4000-8000-000000000003)
INSERT INTO escuela_membresia_aula (id, aula_id, usuario_id, rol, estado, ingreso_en) VALUES
  ('m3000000-0000-4000-8000-000000000001', 'e3000000-0000-4000-8000-000000000003', 'd3000000-0000-4000-8000-000000000001', 'ESTUDIANTE', 'ACTIVO', NOW(6)),
  ('m3000000-0000-4000-8000-000000000002', 'e3000000-0000-4000-8000-000000000003', 'd3000000-0000-4000-8000-000000000002', 'ESTUDIANTE', 'ACTIVO', NOW(6)),
  ('m3000000-0000-4000-8000-000000000003', 'e3000000-0000-4000-8000-000000000003', 'd3000000-0000-4000-8000-000000000003', 'ESTUDIANTE', 'ACTIVO', NOW(6)),
  ('m3000000-0000-4000-8000-000000000004', 'e3000000-0000-4000-8000-000000000003', 'd3000000-0000-4000-8000-000000000004', 'ESTUDIANTE', 'ACTIVO', NOW(6)),
  ('m3000000-0000-4000-8000-000000000005', 'e3000000-0000-4000-8000-000000000005', 'd3000000-0000-4000-8000-000000000005', 'ESTUDIANTE', 'ACTIVO', NOW(6)),
  ('m3000000-0000-4000-8000-000000000006', 'e3000000-0000-4000-8000-000000000006', 'd3000000-0000-4000-8000-000000000006', 'ESTUDIANTE', 'ACTIVO', NOW(6)),
  ('m3000000-0000-4000-8000-000000000007', 'e3000000-0000-4000-8000-000000000007', 'd3000000-0000-4000-8000-000000000007', 'ESTUDIANTE', 'ACTIVO', NOW(6)),
  ('m3000000-0000-4000-8000-000000000008', 'e3000000-0000-4000-8000-000000000008', 'd3000000-0000-4000-8000-000000000008', 'ESTUDIANTE', 'ACTIVO', NOW(6)),
  ('m3000000-0000-4000-8000-000000000009', 'e3000000-0000-4000-8000-000000000003', 'd3000000-0000-4000-8000-000000000009', 'ESTUDIANTE', 'ACTIVO', NOW(6)),
  ('m3000000-0000-4000-8000-000000000010', 'e3000000-0000-4000-8000-000000000003', 'd3000000-0000-4000-8000-000000000010', 'ESTUDIANTE', 'ACTIVO', NOW(6))
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

-- Puntos de experiencia (XP) para los compañeros simulados en el libro mayor
INSERT INTO gamificacion_movimiento_experiencia (id, usuario_id, cantidad, motivo, referencia_tipo, evento_idempotencia, ocurrido_en) VALUES
  ('x3000000-0000-4000-8000-000000000001', 'd3000000-0000-4000-8000-000000000001', 1950, 'ACTIVIDAD', 'LECCION', 'e3000000-0000-4000-8000-000000000001', NOW(6)),
  ('x3000000-0000-4000-8000-000000000002', 'd3000000-0000-4000-8000-000000000002', 1700, 'ACTIVIDAD', 'LECCION', 'e3000000-0000-4000-8000-000000000002', NOW(6)),
  ('x3000000-0000-4000-8000-000000000003', 'd3000000-0000-4000-8000-000000000003', 1400, 'ACTIVIDAD', 'LECCION', 'e3000000-0000-4000-8000-000000000003', NOW(6)),
  ('x3000000-0000-4000-8000-000000000004', 'd3000000-0000-4000-8000-000000000004', 1250, 'ACTIVIDAD', 'LECCION', 'e3000000-0000-4000-8000-000000000004', NOW(6)),
  ('x3000000-0000-4000-8000-000000000005', 'd3000000-0000-4000-8000-000000000005', 1100, 'ACTIVIDAD', 'LECCION', 'e3000000-0000-4000-8000-000000000005', NOW(6)),
  ('x3000000-0000-4000-8000-000000000006', 'd3000000-0000-4000-8000-000000000006', 950,  'ACTIVIDAD', 'LECCION', 'e3000000-0000-4000-8000-000000000006', NOW(6)),
  ('x3000000-0000-4000-8000-000000000007', 'd3000000-0000-4000-8000-000000000007', 800,  'ACTIVIDAD', 'LECCION', 'e3000000-0000-4000-8000-000000000007', NOW(6)),
  ('x3000000-0000-4000-8000-000000000008', 'd3000000-0000-4000-8000-000000000008', 650,  'ACTIVIDAD', 'LECCION', 'e3000000-0000-4000-8000-000000000008', NOW(6)),
  ('x3000000-0000-4000-8000-000000000009', 'd3000000-0000-4000-8000-000000000009', 500,  'ACTIVIDAD', 'LECCION', 'e3000000-0000-4000-8000-000000000009', NOW(6)),
  ('x3000000-0000-4000-8000-000000000010', 'd3000000-0000-4000-8000-000000000010', 350,  'ACTIVIDAD', 'LECCION', 'e3000000-0000-4000-8000-000000000010', NOW(6))
ON DUPLICATE KEY UPDATE cantidad = VALUES(cantidad);

-- Accesorios iniciales equipados para los primeros puestos (para que Rupi luzca atuendos en el podio)
INSERT INTO gamificacion_inventario_usuario (usuario_id, accesorio_id, obtenido_en, equipado) VALUES
  ('d3000000-0000-4000-8000-000000000001', 'a1000000-0000-0000-0000-000000000003', NOW(6), 1),
  ('d3000000-0000-4000-8000-000000000001', 'b1000000-0000-0000-0000-000000000001', NOW(6), 1),
  ('d3000000-0000-4000-8000-000000000002', 'a1000000-0000-0000-0000-000000000001', NOW(6), 1),
  ('d3000000-0000-4000-8000-000000000002', 'b1000000-0000-0000-0000-000000000002', NOW(6), 1),
  ('d3000000-0000-4000-8000-000000000003', 'a1000000-0000-0000-0000-000000000004', NOW(6), 1)
ON DUPLICATE KEY UPDATE equipado = VALUES(equipado);
