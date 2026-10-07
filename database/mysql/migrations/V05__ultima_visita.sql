-- Ejecutar una vez en bases existentes. Las nuevas ya incluyen la columna en schema.sql.
USE rupi;
ALTER TABLE aprendizaje_inscripcion_ruta ADD COLUMN ultima_visita_en DATETIME(6) NULL AFTER ultimo_nodo_visitado_id;
