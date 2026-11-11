-- HU-53. Django es el único escritor de estas dos tablas administrativas de IA.
CREATE TABLE IF NOT EXISTS ia_configuracion_lenguaje (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 codigo VARCHAR(40) NOT NULL UNIQUE,
 borrador JSON NOT NULL,
 revision INT UNSIGNED NOT NULL DEFAULT 1,
 publicada JSON NULL,
 version_publicada INT UNSIGNED NOT NULL DEFAULT 0,
 revision_probada INT UNSIGNED NULL,
 actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;
INSERT IGNORE INTO ia_configuracion_lenguaje (id,codigo,borrador) VALUES
 ('a0530000-0000-4000-8000-000000000001','PRIMARIA',
 JSON_OBJECT('extras',JSON_OBJECT('RESPETO',JSON_ARRAY(),'MENORES',JSON_ARRAY(),'PRIVACIDAD',JSON_ARRAY(),'SEGURIDAD',JSON_ARRAY()),
 'alternativa','Vamos a explicarlo de otra manera, con respeto y palabras claras.'));
CREATE TABLE IF NOT EXISTS ia_revision_lenguaje (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 configuracion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 version INT UNSIGNED NOT NULL,
 configuracion JSON NOT NULL,
 autor_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 publicado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uq_lenguaje_version(configuracion_id,version),
 FOREIGN KEY (configuracion_id) REFERENCES ia_configuracion_lenguaje(id),
 FOREIGN KEY (autor_id) REFERENCES identidad_cuenta_usuario(id)
) ENGINE=InnoDB;
INSERT IGNORE INTO identidad_permiso (id,codigo,descripcion) VALUES
 ('a0530000-0000-4000-8000-000000000101','moderacion.ver','Consultar reglas y probar respuestas de IA'),
 ('a0530000-0000-4000-8000-000000000102','moderacion.gestionar','Configurar y publicar reglas de lenguaje');
INSERT IGNORE INTO identidad_rol_permiso (rol_id,permiso_id)
 SELECT r.id,p.id FROM identidad_rol r JOIN identidad_permiso p ON p.codigo IN ('moderacion.ver','moderacion.gestionar') WHERE r.codigo='ADMINISTRADOR';
INSERT IGNORE INTO identidad_rol_permiso (rol_id,permiso_id)
 SELECT r.id,p.id FROM identidad_rol r JOIN identidad_permiso p ON p.codigo='moderacion.ver' WHERE r.codigo='SOPORTE';
