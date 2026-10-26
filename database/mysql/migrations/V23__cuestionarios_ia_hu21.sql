-- HU-21. Tablas nuevas; reutiliza preguntas, opciones, intentos y respuestas existentes.
CREATE TABLE IF NOT EXISTS evaluacion_cuestionario_ia (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 version_actividad_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
 nodo_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 ruta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 contexto_json JSON NOT NULL,
 contexto_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 proveedor VARCHAR(80) NOT NULL,
 modelo VARCHAR(120) NOT NULL,
 version_prompt VARCHAR(40) NOT NULL,
 estado VARCHAR(30) NOT NULL DEFAULT 'VALIDADO_AUTOMATICO',
 creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 revisado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
 revisado_en DATETIME(6) NULL,
 nota_revision VARCHAR(1000) NULL,
 KEY ix_cuestionario_contexto(nodo_id,contexto_hash,estado),
 FOREIGN KEY (version_actividad_id) REFERENCES aprendizaje_version_actividad(id),
 FOREIGN KEY (nodo_id) REFERENCES aprendizaje_nodo_ruta(id),
 FOREIGN KEY (ruta_id) REFERENCES aprendizaje_version_ruta(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS evaluacion_limite_ia (
 estudiante_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 FOREIGN KEY (estudiante_id) REFERENCES identidad_cuenta_usuario(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS evaluacion_practica_ia (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 estudiante_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 solicitud_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 nodo_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 ruta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 cuestionario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
 intento_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL UNIQUE,
 estado VARCHAR(20) NOT NULL,
 origen VARCHAR(20) NOT NULL DEFAULT 'IA',
 creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 nodo_activo CHAR(36) CHARACTER SET ascii COLLATE ascii_bin
   GENERATED ALWAYS AS (CASE WHEN estado IN ('GENERANDO','LISTO') THEN nodo_id ELSE NULL END) STORED,
 UNIQUE KEY uq_practica_solicitud(estudiante_id,solicitud_id),
 UNIQUE KEY uq_practica_activa(estudiante_id,nodo_activo),
 KEY ix_practica_cuota(estudiante_id,creado_en),
 FOREIGN KEY (estudiante_id) REFERENCES identidad_cuenta_usuario(id),
 FOREIGN KEY (nodo_id) REFERENCES aprendizaje_nodo_ruta(id),
 FOREIGN KEY (ruta_id) REFERENCES aprendizaje_version_ruta(id),
 FOREIGN KEY (cuestionario_id) REFERENCES evaluacion_cuestionario_ia(id),
 FOREIGN KEY (intento_id) REFERENCES evaluacion_intento(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS evaluacion_reporte_pregunta_ia (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 cuestionario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 pregunta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 estudiante_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 motivo VARCHAR(500) NOT NULL,
 creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 resuelto_en DATETIME(6) NULL,
 UNIQUE KEY uq_reporte_pregunta(estudiante_id,pregunta_id),
 FOREIGN KEY (cuestionario_id) REFERENCES evaluacion_cuestionario_ia(id),
 FOREIGN KEY (pregunta_id) REFERENCES evaluacion_pregunta(id),
 FOREIGN KEY (estudiante_id) REFERENCES identidad_cuenta_usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
