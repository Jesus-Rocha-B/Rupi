-- HU-60: Autenticación de doble factor (2FA) obligatoria para administración, soporte y docentes.
-- Escritor único (AGENTS.md): Django escribe identidad_doble_factor; Spring Boot solo la lee en el login docente.
USE rupi;

CREATE TABLE IF NOT EXISTS identidad_doble_factor (
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  secreto_base32 VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  verificado TINYINT(1) NOT NULL DEFAULT 0,
  ultimo_paso_usado BIGINT NULL,
  activado_en DATETIME(6) NULL,
  actualizado_en DATETIME(6) NOT NULL,
  PRIMARY KEY (usuario_id),
  CONSTRAINT fk_doble_factor_usuario
    FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario (id)
) ENGINE=InnoDB;

INSERT INTO identidad_permiso (id, codigo, descripcion)
VALUES (
  '60000000-0000-4000-8000-000000000001',
  'seguridad.2fa',
  'Consultar y reiniciar la configuración de doble factor (2FA) de cuentas obligatorias'
)
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion);

INSERT IGNORE INTO identidad_rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM identidad_rol r
JOIN identidad_permiso p ON p.codigo = 'seguridad.2fa'
WHERE r.codigo = 'ADMINISTRADOR';
