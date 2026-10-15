-- V20: fechas utilizadas por el editor docente y el acceso a actividades (71ed612).
-- Idempotente; conserva fechas existentes y no modifica resultados ni progreso.
USE rupi;
SET @rupi_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='aprendizaje_nodo_ruta' AND column_name='disponible_desde'),
 'SELECT 1', 'ALTER TABLE aprendizaje_nodo_ruta ADD COLUMN disponible_desde DATETIME(6) NULL');
PREPARE rupi_stmt FROM @rupi_ddl;
EXECUTE rupi_stmt;
DEALLOCATE PREPARE rupi_stmt;
SET @rupi_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='aprendizaje_nodo_ruta' AND column_name='bloqueado_desde'),
 'SELECT 1', 'ALTER TABLE aprendizaje_nodo_ruta ADD COLUMN bloqueado_desde DATETIME(6) NULL');
PREPARE rupi_stmt FROM @rupi_ddl;
EXECUTE rupi_stmt;
DEALLOCATE PREPARE rupi_stmt;
