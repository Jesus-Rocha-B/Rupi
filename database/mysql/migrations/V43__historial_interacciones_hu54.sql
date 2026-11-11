-- HU-54. Django es el único escritor del historial de moderación y su revisión.
CREATE TABLE IF NOT EXISTS ia_interaccion_auditoria (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 origen VARCHAR(40) NOT NULL,
 recurso VARCHAR(80) NOT NULL,
 proveedor VARCHAR(80) NULL,
 modelo VARCHAR(120) NULL,
 duracion_ms INT UNSIGNED NULL,
 pregunta TEXT NULL,
 respuesta MEDIUMTEXT NOT NULL,
 alternativa TEXT NULL,
 permitida BOOLEAN NOT NULL,
 reglas JSON NOT NULL,
 version_reglas INT UNSIGNED NOT NULL,
 creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 revisada_en DATETIME(6) NULL,
 revisor_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
 KEY ix_interaccion_fecha(creada_en,id),
 KEY ix_interaccion_revision(revisada_en,creada_en),
 FOREIGN KEY (revisor_id) REFERENCES identidad_cuenta_usuario(id)
) ENGINE=InnoDB;
INSERT IGNORE INTO identidad_permiso (id,codigo,descripcion) VALUES
 ('a0540000-0000-4000-8000-000000000101','interacciones.ver','Consultar historial restringido de IA'),
 ('a0540000-0000-4000-8000-000000000102','interacciones.revisar','Marcar interacciones de IA como revisadas');
INSERT IGNORE INTO identidad_rol_permiso (rol_id,permiso_id)
 SELECT r.id,p.id FROM identidad_rol r JOIN identidad_permiso p ON p.codigo IN ('interacciones.ver','interacciones.revisar') WHERE r.codigo='ADMINISTRADOR';
