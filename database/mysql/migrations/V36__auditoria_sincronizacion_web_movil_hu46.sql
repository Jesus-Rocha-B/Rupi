-- V36__auditoria_sincronizacion_web_movil_hu46.sql
-- Auditoría de tráfico y sincronización entre web y móvil (HU-46)
--
-- Registra el tráfico de sincronización que viaja entre la versión de escritorio
-- y la aplicación celular para garantizar que las lecciones y puntajes de los niños
-- estén siempre sincronizados en ambos lados.
-- Escritor: Django y Spring Boot. Idempotente.

-- Asegurar tabla base si no existiera
CREATE TABLE IF NOT EXISTS operaciones_evento_sincronizacion (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  dispositivo_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  evento_cliente_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  tipo_evento VARCHAR(80) NOT NULL,
  estado ENUM('RECIBIDO','APLICADO','DUPLICADO','RECHAZADO','ERROR') NOT NULL,
  codigo_resultado VARCHAR(80) NULL,
  recibido_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sync_usuario_evento (usuario_id, evento_cliente_id),
  KEY ix_sync_estado_fecha (estado, recibido_en),
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id),
  FOREIGN KEY (dispositivo_id) REFERENCES identidad_dispositivo(id)
) ENGINE=InnoDB;

-- Columnas de auditoría de tráfico y sincronización web/móvil
SET @col_cliente_tipo = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'operaciones_evento_sincronizacion' AND COLUMN_NAME = 'cliente_tipo');
SET @alter_sql = IF(@col_cliente_tipo = 0, 'ALTER TABLE operaciones_evento_sincronizacion ADD COLUMN cliente_tipo ENUM(\'WEB\', \'MOVIL\') NOT NULL DEFAULT \'WEB\' AFTER dispositivo_id', 'SELECT 1');
PREPARE stmt FROM @alter_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_puntaje_xp = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'operaciones_evento_sincronizacion' AND COLUMN_NAME = 'puntaje_xp');
SET @alter_sql = IF(@col_puntaje_xp = 0, 'ALTER TABLE operaciones_evento_sincronizacion ADD COLUMN puntaje_xp INT NOT NULL DEFAULT 0 AFTER tipo_evento', 'SELECT 1');
PREPARE stmt FROM @alter_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_lecciones_completadas = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'operaciones_evento_sincronizacion' AND COLUMN_NAME = 'lecciones_completadas');
SET @alter_sql = IF(@col_lecciones_completadas = 0, 'ALTER TABLE operaciones_evento_sincronizacion ADD COLUMN lecciones_completadas INT NOT NULL DEFAULT 0 AFTER puntaje_xp', 'SELECT 1');
PREPARE stmt FROM @alter_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_discrepancia = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'operaciones_evento_sincronizacion' AND COLUMN_NAME = 'discrepancia_detectada');
SET @alter_sql = IF(@col_discrepancia = 0, 'ALTER TABLE operaciones_evento_sincronizacion ADD COLUMN discrepancia_detectada TINYINT(1) NOT NULL DEFAULT 0 AFTER codigo_resultado', 'SELECT 1');
PREPARE stmt FROM @alter_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_detalle = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'operaciones_evento_sincronizacion' AND COLUMN_NAME = 'detalle_payload');
SET @alter_sql = IF(@col_detalle = 0, 'ALTER TABLE operaciones_evento_sincronizacion ADD COLUMN detalle_payload JSON NULL AFTER discrepancia_detectada', 'SELECT 1');
PREPARE stmt FROM @alter_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Permiso para auditar sincronización en la consola
INSERT IGNORE INTO identidad_permiso (id, codigo, descripcion) VALUES
('a0460000-0000-4000-8000-000000000101', 'sincronizacion.auditar', 'Auditar tráfico y comparar sincronización entre web y celular');

INSERT IGNORE INTO identidad_rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM identidad_rol r
JOIN identidad_permiso p ON p.codigo = 'sincronizacion.auditar'
WHERE r.codigo IN ('ADMINISTRADOR', 'SOPORTE');
