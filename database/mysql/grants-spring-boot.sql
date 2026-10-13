-- Permisos del usuario de la API Spring Boot (desarrollo local). Idempotente.
-- Respeta el escritor único de AGENTS.md:
--   Spring escribe escuela_*, aprendizaje_*, evaluacion_* y gamificacion_*.
--   Django escribe identidad_* y curriculo_*; Spring solo los lee, salvo la
--   excepción documentada del login (identidad_sesion_usuario e identidad_intento_acceso).
-- Uso (como root de MySQL), cambiando el usuario si hace falta:
--   mysql -e "SET @api_usuario='rupi'; SOURCE database/mysql/grants-spring-boot.sql"
-- Se concede a 'localhost' y a '127.0.0.1' solo si esa cuenta ya existe.

USE rupi;

DROP PROCEDURE IF EXISTS rupi_otorgar_permisos_api;
DELIMITER //
CREATE PROCEDURE rupi_otorgar_permisos_api(IN usuario VARCHAR(32), IN equipo VARCHAR(60))
BEGIN
  DECLARE terminado INT DEFAULT 0;
  DECLARE tabla VARCHAR(64);
  DECLARE cur CURSOR FOR
    SELECT table_name FROM information_schema.tables
     WHERE table_schema = 'rupi' AND table_type = 'BASE TABLE'
       AND (table_name LIKE 'escuela\_%' OR table_name LIKE 'aprendizaje\_%'
         OR table_name LIKE 'evaluacion\_%' OR table_name LIKE 'gamificacion\_%'
         OR table_name LIKE 'recurso\_%');
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET terminado = 1;

  IF EXISTS (SELECT 1 FROM mysql.user WHERE user = usuario AND host = equipo) THEN
    SET @g = CONCAT('GRANT SELECT ON rupi.* TO `', usuario, '`@`', equipo, '`');
    PREPARE s FROM @g; EXECUTE s; DEALLOCATE PREPARE s;

    OPEN cur;
    leer: LOOP
      FETCH cur INTO tabla;
      IF terminado = 1 THEN LEAVE leer; END IF;
      SET @g = CONCAT('GRANT INSERT, UPDATE, DELETE ON rupi.`', tabla, '` TO `', usuario, '`@`', equipo, '`');
      PREPARE s FROM @g; EXECUTE s; DEALLOCATE PREPARE s;
    END LOOP;
    CLOSE cur;

    SET @g = CONCAT('GRANT INSERT ON rupi.identidad_intento_acceso TO `', usuario, '`@`', equipo, '`');
    PREPARE s FROM @g; EXECUTE s; DEALLOCATE PREPARE s;
    SET @g = CONCAT('GRANT INSERT, UPDATE ON rupi.identidad_sesion_usuario TO `', usuario, '`@`', equipo, '`');
    PREPARE s FROM @g; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
END //
DELIMITER ;

SET @api_usuario = IFNULL(@api_usuario, 'rupi');
CALL rupi_otorgar_permisos_api(@api_usuario, 'localhost');
CALL rupi_otorgar_permisos_api(@api_usuario, '127.0.0.1');
DROP PROCEDURE rupi_otorgar_permisos_api;
