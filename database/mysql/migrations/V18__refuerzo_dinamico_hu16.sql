-- HU-16: refuerzo personal por nodo y fuente oficial; sin duplicar porcentajes.
USE rupi;
CREATE TABLE IF NOT EXISTS aprendizaje_refuerzo_nodo (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  estudiante_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  nodo_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  recurso_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  vinculado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  realizado_en DATETIME(6) NULL,
  UNIQUE KEY uq_refuerzo_estudiante_nodo_recurso (estudiante_id, nodo_id, recurso_id),
  FOREIGN KEY (estudiante_id) REFERENCES identidad_cuenta_usuario(id),
  FOREIGN KEY (nodo_id) REFERENCES aprendizaje_nodo_ruta(id)
) ENGINE=InnoDB;
