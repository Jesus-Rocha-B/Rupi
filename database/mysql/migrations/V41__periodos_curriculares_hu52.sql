-- HU-52: configuración maestra (Django) y asociación a planificación existente.
CREATE TABLE IF NOT EXISTS curriculo_calendario_periodos (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 anio_escolar_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
 tipo ENUM('BIMESTRE','TRIMESTRE') NOT NULL,
 periodos JSON NOT NULL,
 revision INT UNSIGNED NOT NULL DEFAULT 1,
 actualizado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY (anio_escolar_id) REFERENCES escuela_anio_escolar(id),
 FOREIGN KEY (actualizado_por) REFERENCES identidad_cuenta_usuario(id)
) ENGINE=InnoDB;

-- Instantánea aplicada: editar un borrador no cambia lo ya aplicado.
-- Sin FK a escuela_planificacion_ruta: Spring puede retirar fechas sin escribir curriculo_*.
CREATE TABLE IF NOT EXISTS curriculo_periodo_planificacion (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 planificacion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
 calendario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 revision INT UNSIGNED NOT NULL,
 nombre VARCHAR(80) NOT NULL,
 comienza DATE NOT NULL,
 termina DATE NOT NULL,
 aplicado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 aplicado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY (calendario_id) REFERENCES curriculo_calendario_periodos(id),
 FOREIGN KEY (aplicado_por) REFERENCES identidad_cuenta_usuario(id),
 CHECK (termina >= comienza)
) ENGINE=InnoDB;
