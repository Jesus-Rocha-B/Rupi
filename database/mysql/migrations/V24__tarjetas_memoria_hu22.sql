-- HU-22: tarjetas revisadas e inmutables y autoevaluación independiente de notas.
CREATE TABLE IF NOT EXISTS aprendizaje_tarjeta_revision (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 solicitud_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 docente_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 aula_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 ruta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 nodo_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 fuente VARCHAR(1000) NOT NULL,
 revisado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 retirado_en DATETIME(6) NULL,
 UNIQUE KEY uq_tarjeta_solicitud(docente_id,solicitud_id),
 KEY ix_tarjeta_nodo(nodo_id),
 FOREIGN KEY (id) REFERENCES aprendizaje_tarjeta_memoria(id),
 FOREIGN KEY (nodo_id) REFERENCES aprendizaje_nodo_ruta(id),
 FOREIGN KEY (ruta_id) REFERENCES aprendizaje_version_ruta(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS aprendizaje_tarjeta_sesion (
 estudiante_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 ruta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 nodo_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 tarjeta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
 revelada BOOLEAN NOT NULL DEFAULT FALSE,
 revision BIGINT NOT NULL DEFAULT 0,
 ultima_solicitud CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
 actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY(estudiante_id,ruta_id,nodo_id),
 FOREIGN KEY(tarjeta_id) REFERENCES aprendizaje_tarjeta_memoria(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
