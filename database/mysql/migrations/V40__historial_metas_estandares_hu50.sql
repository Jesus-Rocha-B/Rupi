-- HU-50: Django registra correcciones sin cambiar los vínculos curriculares existentes.
CREATE TABLE IF NOT EXISTS curriculo_registro_revision (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 tipo VARCHAR(16) NOT NULL,
 registro_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 descripcion_anterior TEXT NULL,
 descripcion_nueva TEXT NOT NULL,
 motivo VARCHAR(500) NOT NULL,
 actor_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 INDEX idx_registro_revision (registro_id,tipo,creado_en),
 FOREIGN KEY (actor_id) REFERENCES identidad_cuenta_usuario(id)
) ENGINE=InnoDB;
