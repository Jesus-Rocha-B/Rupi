-- HU-57. Programar respaldos automáticos de la base de datos principal fuera de horas punta escolares.
-- Escritor único: Django administra operaciones_politica_respaldo y operaciones_respaldo (y registra alertas/auditoría).
-- Idempotente. Después de aplicarla hay que volver a ejecutar grants-django-admin.sql.
USE rupi;

CREATE TABLE IF NOT EXISTS operaciones_politica_respaldo (
  id TINYINT UNSIGNED PRIMARY KEY,
  habilitado BOOLEAN NOT NULL DEFAULT TRUE,
  hora_programada CHAR(5) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '02:30',
  frecuencia ENUM('DIARIO','SEMANAL') NOT NULL DEFAULT 'DIARIO',
  dia_semana TINYINT UNSIGNED NULL,
  retencion_dias SMALLINT UNSIGNED NOT NULL DEFAULT 30,
  almacenamiento_externo_tipo ENUM('CARPETA_SEGURA','S3_COMPATIBLE','SFTP_REMOTO') NOT NULL DEFAULT 'CARPETA_SEGURA',
  almacenamiento_externo_ruta VARCHAR(500) NOT NULL DEFAULT 'backups-externos/rupi',
  cifrado_activo BOOLEAN NOT NULL DEFAULT TRUE,
  alertar_si_falla BOOLEAN NOT NULL DEFAULT TRUE,
  email_alerta VARCHAR(180) NULL,
  ultima_ejecucion_en DATETIME(6) NULL,
  proximo_respaldo_en DATETIME(6) NULL,
  actualizado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  FOREIGN KEY (actualizado_por) REFERENCES identidad_cuenta_usuario(id),
  CHECK (id = 1),
  CHECK (hora_programada REGEXP '^0[0-5]:[0-5][0-9]$'),
  CHECK (dia_semana IS NULL OR dia_semana BETWEEN 0 AND 6),
  CHECK (retencion_dias BETWEEN 7 AND 365)
) ENGINE=InnoDB;

INSERT IGNORE INTO operaciones_politica_respaldo (id) VALUES (1);

CREATE TABLE IF NOT EXISTS operaciones_respaldo (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  tipo ENUM('COMPLETO','INCREMENTAL','BINLOG') NOT NULL,
  iniciado_en DATETIME(6) NOT NULL,
  finalizado_en DATETIME(6) NULL,
  estado ENUM('EN_CURSO','CORRECTO','FALLIDO','VERIFICADO') NOT NULL,
  ubicacion_referencia VARCHAR(500) NULL,
  checksum_sha256 BINARY(32) NULL,
  tamano_bytes BIGINT UNSIGNED NULL,
  version_esquema VARCHAR(60) NULL
) ENGINE=InnoDB;

SET @rupi_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='operaciones_respaldo' AND column_name='origen'),
  'SELECT 1', "ALTER TABLE operaciones_respaldo ADD COLUMN origen ENUM('PROGRAMADO','MANUAL') NOT NULL DEFAULT 'PROGRAMADO' AFTER tipo");
PREPARE rupi_stmt FROM @rupi_ddl;
EXECUTE rupi_stmt;
DEALLOCATE PREPARE rupi_stmt;

SET @rupi_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='operaciones_respaldo' AND column_name='almacenamiento_externo'),
  'SELECT 1', 'ALTER TABLE operaciones_respaldo ADD COLUMN almacenamiento_externo VARCHAR(60) NULL AFTER ubicacion_referencia');
PREPARE rupi_stmt FROM @rupi_ddl;
EXECUTE rupi_stmt;
DEALLOCATE PREPARE rupi_stmt;

SET @rupi_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='operaciones_respaldo' AND column_name='detalle_error'),
  'SELECT 1', 'ALTER TABLE operaciones_respaldo ADD COLUMN detalle_error VARCHAR(500) NULL AFTER version_esquema');
PREPARE rupi_stmt FROM @rupi_ddl;
EXECUTE rupi_stmt;
DEALLOCATE PREPARE rupi_stmt;

SET @rupi_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='operaciones_respaldo' AND column_name='alerta_enviada'),
  'SELECT 1', 'ALTER TABLE operaciones_respaldo ADD COLUMN alerta_enviada BOOLEAN NOT NULL DEFAULT FALSE AFTER detalle_error');
PREPARE rupi_stmt FROM @rupi_ddl;
EXECUTE rupi_stmt;
DEALLOCATE PREPARE rupi_stmt;

SET @rupi_ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='operaciones_respaldo' AND index_name='ix_respaldo_iniciado_en'),
  'SELECT 1', 'ALTER TABLE operaciones_respaldo ADD KEY ix_respaldo_iniciado_en (iniciado_en)');
PREPARE rupi_stmt FROM @rupi_ddl;
EXECUTE rupi_stmt;
DEALLOCATE PREPARE rupi_stmt;

INSERT IGNORE INTO identidad_permiso (id, codigo, descripcion) VALUES
  ('a0570000-0000-4000-8000-000000000101', 'respaldos.ver', 'Consultar la programación de respaldos automáticos, almacenamiento externo e historial'),
  ('a0570000-0000-4000-8000-000000000102', 'respaldos.gestionar', 'Programar respaldos nocturnos automáticos, configurar alertas y ejecutar copias manuales');

INSERT IGNORE INTO identidad_rol_permiso (rol_id, permiso_id)
  SELECT r.id, p.id FROM identidad_rol r
  JOIN identidad_permiso p ON p.codigo IN ('respaldos.ver', 'respaldos.gestionar')
  WHERE r.codigo = 'ADMINISTRADOR';

INSERT IGNORE INTO identidad_rol_permiso (rol_id, permiso_id)
  SELECT r.id, p.id FROM identidad_rol r
  JOIN identidad_permiso p ON p.codigo = 'respaldos.ver'
  WHERE r.codigo = 'SOPORTE';
