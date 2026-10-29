-- Permisos del usuario de la API administrativa Django (desarrollo local). Idempotente.
-- Respeta el escritor único de AGENTS.md: Django escribe identidad_* y curriculo_*; lee el resto.
-- Además registra sus propias acciones en operaciones_evento_auditoria.
-- Uso (como root de MySQL), cambiando el usuario si hace falta:
--   mysql -e "SET @admin_usuario='rupi_admin'; SOURCE database/mysql/grants-django-admin.sql"
-- Se concede a 'localhost' y a '127.0.0.1' solo si esa cuenta ya existe.

USE rupi;

DROP PROCEDURE IF EXISTS rupi_otorgar_permisos_admin;
DELIMITER //
CREATE PROCEDURE rupi_otorgar_permisos_admin(IN usuario VARCHAR(32), IN equipo VARCHAR(60))
BEGIN
  DECLARE terminado INT DEFAULT 0;
  DECLARE tabla VARCHAR(64);
  DECLARE cur CURSOR FOR
    SELECT table_name FROM information_schema.tables
     WHERE table_schema = 'rupi' AND table_type = 'BASE TABLE'
       AND (table_name LIKE 'identidad\_%' OR table_name LIKE 'curriculo\_%');
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET terminado = 1;

  IF EXISTS (SELECT 1 FROM mysql.user WHERE user = usuario AND host = equipo) THEN
    SET @g = CONCAT('GRANT SELECT ON rupi.* TO `', usuario, '`@`', equipo, '`');
    PREPARE s FROM @g; EXECUTE s; DEALLOCATE PREPARE s;

    OPEN cur;
    leer: LOOP
      FETCH cur INTO tabla;
      IF terminado THEN LEAVE leer; END IF;
      SET @g = CONCAT('GRANT INSERT, UPDATE ON rupi.`', tabla, '` TO `', usuario, '`@`', equipo, '`');
      PREPARE s FROM @g; EXECUTE s; DEALLOCATE PREPARE s;
    END LOOP;
    CLOSE cur;

    SET @g = CONCAT('GRANT INSERT ON rupi.operaciones_evento_auditoria TO `', usuario, '`@`', equipo, '`');
    PREPARE s FROM @g; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
END//
DELIMITER ;

SET @admin_usuario = COALESCE(@admin_usuario, 'rupi_admin');
CALL rupi_otorgar_permisos_admin(@admin_usuario, 'localhost');
CALL rupi_otorgar_permisos_admin(@admin_usuario, '127.0.0.1');
DROP PROCEDURE rupi_otorgar_permisos_admin;
