-- HU-23: versiones compartidas de resumen y solicitudes acotadas.
CREATE TABLE IF NOT EXISTS aprendizaje_resumen_visual (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 nodo_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 ruta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 version_actividad_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 contexto_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 contexto_json JSON NOT NULL,
 contenido_json JSON NULL,
 proveedor VARCHAR(80) NOT NULL,
 modelo VARCHAR(120) NOT NULL,
 version_prompt VARCHAR(40) NOT NULL,
 estado VARCHAR(30) NOT NULL,
 creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 revisado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
 nota_revision VARCHAR(1000) NULL,
 UNIQUE KEY uq_resumen_contexto(nodo_id,contexto_hash),
 FOREIGN KEY(nodo_id) REFERENCES aprendizaje_nodo_ruta(id),
 FOREIGN KEY(ruta_id) REFERENCES aprendizaje_version_ruta(id),
 FOREIGN KEY(version_actividad_id) REFERENCES aprendizaje_version_actividad(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS aprendizaje_resumen_solicitud (
 estudiante_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 solicitud_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 resumen_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY(estudiante_id,solicitud_id),
 KEY ix_resumen_limite(estudiante_id,creado_en),
 FOREIGN KEY(resumen_id) REFERENCES aprendizaje_resumen_visual(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS aprendizaje_resumen_reporte (
 resumen_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 estudiante_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 motivo VARCHAR(500) NOT NULL,
 creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY(resumen_id,estudiante_id),
 FOREIGN KEY(resumen_id) REFERENCES aprendizaje_resumen_visual(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
