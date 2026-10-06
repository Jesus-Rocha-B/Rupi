-- V04: Contexto cultural y datos curiosos de la aventura (Dominio Curricular - Django es escritor único)
-- UTF-8 / MySQL 8.4 InnoDB

USE rupi;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS curriculo_contexto_cultural (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  ruta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  codigo VARCHAR(60) NOT NULL UNIQUE,
  ciudad VARCHAR(100) NOT NULL,
  lugar VARCHAR(150) NOT NULL,
  titulo VARCHAR(180) NOT NULL,
  descripcion TEXT NULL,
  mensaje_animo VARCHAR(255) NOT NULL,
  imagen_url VARCHAR(1000) NOT NULL,
  imagen_alt VARCHAR(500) NOT NULL,
  credito_autor VARCHAR(150) NOT NULL,
  credito_fuente VARCHAR(150) NOT NULL,
  credito_licencia VARCHAR(80) NOT NULL,
  credito_url VARCHAR(1000) NOT NULL,
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  KEY ix_contexto_ruta (ruta_id),
  FOREIGN KEY (ruta_id) REFERENCES aprendizaje_ruta(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS curriculo_contexto_cultural_dato (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  contexto_cultural_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  parada_secuencia SMALLINT UNSIGNED NULL,
  orden SMALLINT UNSIGNED NOT NULL DEFAULT 1,
  titulo VARCHAR(150) NOT NULL,
  contenido TEXT NOT NULL,
  icono VARCHAR(60) NOT NULL,
  fuente VARCHAR(255) NOT NULL,
  estado ENUM('BORRADOR','PUBLICADO','RETIRADO') NOT NULL DEFAULT 'BORRADOR',
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  KEY ix_dato_contexto (contexto_cultural_id),
  KEY ix_dato_secuencia (contexto_cultural_id, parada_secuencia),
  FOREIGN KEY (contexto_cultural_id) REFERENCES curriculo_contexto_cultural(id)
) ENGINE=InnoDB;
