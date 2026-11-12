-- HU-58. La administración configura el límite de intentos fallidos y el bloqueo automático de cuentas
-- para frenar ataques de fuerza bruta en la web, el celular y la consola.
-- Escritor único: Django escribe la política (identidad_*). Spring solo la lee: el bloqueo automático se calcula
-- con identidad_intento_acceso al iniciar sesión, así Spring nunca escribe identidad_cuenta_usuario.
-- Idempotente. Después de aplicarla hay que volver a ejecutar grants-django-admin.sql.
USE rupi;

CREATE TABLE IF NOT EXISTS identidad_politica_acceso (
 id TINYINT UNSIGNED PRIMARY KEY,
 ventana_minutos SMALLINT UNSIGNED NOT NULL DEFAULT 5,
 max_fallos_equipo_cuenta SMALLINT UNSIGNED NOT NULL DEFAULT 5,
 max_fallos_equipo SMALLINT UNSIGNED NOT NULL DEFAULT 20,
 bloqueo_automatico BOOLEAN NOT NULL DEFAULT TRUE,
 max_fallos_cuenta SMALLINT UNSIGNED NOT NULL DEFAULT 10,
 minutos_bloqueo SMALLINT UNSIGNED NOT NULL DEFAULT 15,
 actualizado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
 actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY (actualizado_por) REFERENCES identidad_cuenta_usuario(id),
 CHECK (id = 1),
 CHECK (ventana_minutos BETWEEN 1 AND 60),
 CHECK (max_fallos_equipo_cuenta BETWEEN 3 AND 20),
 CHECK (max_fallos_equipo BETWEEN 10 AND 200),
 CHECK (max_fallos_equipo >= max_fallos_equipo_cuenta),
 CHECK (max_fallos_cuenta BETWEEN 5 AND 50),
 CHECK (minutos_bloqueo BETWEEN 5 AND 1440)
) ENGINE=InnoDB;

-- Los valores iniciales son los que Spring y Django usaban fijos: 5 por cuenta y equipo, 20 por equipo en 5 minutos.
INSERT IGNORE INTO identidad_politica_acceso (id) VALUES (1);

-- Al desbloquear una cuenta desde la consola, los fallos anteriores dejan de contar para el bloqueo automático.
SET @rupi_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='identidad_cuenta_usuario' AND column_name='intentos_reiniciados_en'),
 'SELECT 1', 'ALTER TABLE identidad_cuenta_usuario ADD COLUMN intentos_reiniciados_en DATETIME(6) NULL AFTER bloqueado_hasta');
PREPARE rupi_stmt FROM @rupi_ddl;
EXECUTE rupi_stmt;
DEALLOCATE PREPARE rupi_stmt;

-- Para listar las cuentas bloqueadas sin recorrer todo el historial de intentos.
SET @rupi_ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics
 WHERE table_schema=DATABASE() AND table_name='identidad_intento_acceso' AND index_name='ix_intento_resultado_fecha'),
 'SELECT 1', 'ALTER TABLE identidad_intento_acceso ADD KEY ix_intento_resultado_fecha (resultado, ocurrido_en)');
PREPARE rupi_stmt FROM @rupi_ddl;
EXECUTE rupi_stmt;
DEALLOCATE PREPARE rupi_stmt;

INSERT IGNORE INTO identidad_permiso (id,codigo,descripcion) VALUES
 ('a0580000-0000-4000-8000-000000000101','acceso.ver','Consultar la política de bloqueo y las cuentas bloqueadas'),
 ('a0580000-0000-4000-8000-000000000102','acceso.configurar','Configurar el límite de intentos y el bloqueo automático');
INSERT IGNORE INTO identidad_rol_permiso (rol_id,permiso_id)
 SELECT r.id,p.id FROM identidad_rol r JOIN identidad_permiso p ON p.codigo IN ('acceso.ver','acceso.configurar') WHERE r.codigo='ADMINISTRADOR';
INSERT IGNORE INTO identidad_rol_permiso (rol_id,permiso_id)
 SELECT r.id,p.id FROM identidad_rol r JOIN identidad_permiso p ON p.codigo='acceso.ver' WHERE r.codigo='SOPORTE';
