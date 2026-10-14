-- HU-14: recordatorios internos duraderos, un aviso por alumno/evaluación/fecha.
-- Requiere V13. No guarda ni envía correos o notificaciones push.
USE rupi;
CREATE TABLE IF NOT EXISTS escuela_recordatorio_evaluacion (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 estudiante_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 aula_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 version_ruta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 nodo_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 fecha_evaluacion DATETIME(6) NOT NULL,
 creado_en DATETIME(6) NOT NULL,
 leido_en DATETIME(6) NULL,
 UNIQUE KEY uq_recordatorio_fecha (estudiante_id,aula_id,version_ruta_id,nodo_id,fecha_evaluacion),
 KEY ix_recordatorio_estudiante (estudiante_id,version_ruta_id,fecha_evaluacion),
 FOREIGN KEY (estudiante_id) REFERENCES identidad_cuenta_usuario(id),
 FOREIGN KEY (aula_id) REFERENCES escuela_aula(id),
 FOREIGN KEY (version_ruta_id) REFERENCES aprendizaje_version_ruta(id)
) ENGINE=InnoDB;
