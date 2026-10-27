-- V29__progreso_diario_meta_hu34.sql
-- Indicador gráfico de progreso diario y meta del día en tiempo real (HU-34)

-- 1. Añadir columna meta_diaria_actividades en identidad_perfil_usuario si no existe
SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS 
  WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'identidad_perfil_usuario' 
    AND COLUMN_NAME = 'meta_diaria_actividades'
);
SET @sql = IF(@col_exists = 0, 
  'ALTER TABLE identidad_perfil_usuario ADD COLUMN meta_diaria_actividades INT NOT NULL DEFAULT 3', 
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Crear tabla de historial de progreso diario
CREATE TABLE IF NOT EXISTS gamificacion_progreso_diario (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    fecha_local DATE NOT NULL,
    meta_actividades INT NOT NULL DEFAULT 3,
    actividades_completadas INT NOT NULL DEFAULT 0,
    meta_cumplida TINYINT(1) NOT NULL DEFAULT 0,
    cumplida_en DATETIME(6) NULL,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_progreso_diario_usuario_fecha (usuario_id, fecha_local),
    KEY idx_progreso_diario_fecha (fecha_local),
    CONSTRAINT fk_progreso_diario_usuario FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Sembrar historial demostrativo de los últimos días para el estudiante Mateo Quispe
INSERT INTO gamificacion_progreso_diario (id, usuario_id, fecha_local, meta_actividades, actividades_completadas, meta_cumplida, cumplida_en)
VALUES
  (UUID(), '11111111-1111-4111-8111-111111111111', DATE_SUB(CURDATE(), INTERVAL 3 DAY), 3, 3, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
  (UUID(), '11111111-1111-4111-8111-111111111111', DATE_SUB(CURDATE(), INTERVAL 2 DAY), 3, 4, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
  (UUID(), '11111111-1111-4111-8111-111111111111', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 3, 3, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
  (UUID(), '11111111-1111-4111-8111-111111111111', CURDATE(), 3, 1, 0, NULL)
ON DUPLICATE KEY UPDATE
  meta_actividades = VALUES(meta_actividades),
  actividades_completadas = VALUES(actividades_completadas),
  meta_cumplida = VALUES(meta_cumplida);
