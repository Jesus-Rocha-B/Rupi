-- HU-25: reutiliza preferencias, programaciones, metas y mensajes existentes.
CREATE TABLE IF NOT EXISTS gamificacion_preferencia_estudio (
 usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 objetivo_actividades TINYINT UNSIGNED NOT NULL DEFAULT 1,
 revision BIGINT NOT NULL DEFAULT 0,
 actualizada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY(usuario_id) REFERENCES identidad_cuenta_usuario(id),
 CHECK(objetivo_actividades BETWEEN 1 AND 5)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS notificacion_evento_usuario (
 usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 clave_evento VARCHAR(180) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 mensaje_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 categoria ENUM('TAREA','RECORDATORIO') NOT NULL,
 reclamada_en DATETIME(6) NULL,
 PRIMARY KEY(usuario_id,clave_evento),
 UNIQUE KEY uq_evento_mensaje(mensaje_id),
 FOREIGN KEY(usuario_id) REFERENCES identidad_cuenta_usuario(id),
 FOREIGN KEY(mensaje_id) REFERENCES notificacion_mensaje(id)
) ENGINE=InnoDB;
