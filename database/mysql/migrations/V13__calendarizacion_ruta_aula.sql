-- HU-13. Planificación por aula y versión de ruta. Fechas almacenadas en UTC.
USE rupi;
CREATE TABLE IF NOT EXISTS escuela_planificacion_ruta (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 aula_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 version_ruta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 tipo ENUM('UNIDAD','EVALUACION') NOT NULL,
 referencia_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 titulo VARCHAR(180) NOT NULL,
 inicia_en DATETIME(6) NOT NULL,
 termina_en DATETIME(6) NOT NULL,
 actualizado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uq_planificacion (aula_id, version_ruta_id, tipo, referencia_id),
 FOREIGN KEY (aula_id) REFERENCES escuela_aula(id),
 FOREIGN KEY (version_ruta_id) REFERENCES aprendizaje_version_ruta(id),
 FOREIGN KEY (actualizado_por) REFERENCES identidad_cuenta_usuario(id),
 CHECK (termina_en >= inicia_en)
) ENGINE=InnoDB;
