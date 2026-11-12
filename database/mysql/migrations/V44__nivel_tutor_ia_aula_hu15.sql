-- HU-15. El docente elige cómo ayuda el tutor Rupi a su aula: pistas guiadas o explicación directa.
-- Spring es el único escritor (escuela_*). Un aula sin fila usa PISTAS_GUIADAS, el comportamiento anterior.
CREATE TABLE IF NOT EXISTS escuela_aula_tutor_ia (
 aula_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 modo_asistencia VARCHAR(30) NOT NULL,
 actualizado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY (aula_id) REFERENCES escuela_aula(id),
 FOREIGN KEY (actualizado_por) REFERENCES identidad_cuenta_usuario(id),
 CHECK (modo_asistencia IN ('PISTAS_GUIADAS','EXPLICACION_DIRECTA'))
) ENGINE=InnoDB;
