-- ============================================================================
-- RUPI · Script de migración y actualización: Grados de Educación Primaria
-- Esquema: rupi (MySQL 8 / MariaDB)
--
-- Este script normaliza y asegura que todos los registros de curriculo_grado
-- correspondan al nivel de Educación Primaria (1.° a 6.° de primaria),
-- corrigiendo cualquier valor asignado a secundaria y garantizando que la
-- ruta activa de Matemática (MAT-2P-PLAZA) apunte al grado 2.° de primaria.
-- ============================================================================

USE rupi;

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 1. Insertar o actualizar los 6 grados de primaria oficiales en curriculo_grado
INSERT INTO curriculo_grado (id, numero_grado, nombre) VALUES
  ('00000000-0000-4000-8000-000000000001', 1, '1.° de primaria'),
  ('00000000-0000-4000-8000-000000000002', 2, '2.° de primaria'),
  ('00000000-0000-4000-8000-000000000003', 3, '3.° de primaria'),
  ('00000000-0000-4000-8000-000000000004', 4, '4.° de primaria'),
  ('00000000-0000-4000-8000-000000000005', 5, '5.° de primaria'),
  ('00000000-0000-4000-8000-000000000006', 6, '6.° de primaria')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- 2. Asegurar que los nombres queden formateados exactamente con tildes y caracteres peruanos
UPDATE curriculo_grado SET nombre = '1.° de primaria' WHERE numero_grado = 1;
UPDATE curriculo_grado SET nombre = '2.° de primaria' WHERE numero_grado = 2;
UPDATE curriculo_grado SET nombre = '3.° de primaria' WHERE numero_grado = 3;
UPDATE curriculo_grado SET nombre = '4.° de primaria' WHERE numero_grado = 4;
UPDATE curriculo_grado SET nombre = '5.° de primaria' WHERE numero_grado = 5;
UPDATE curriculo_grado SET nombre = '6.° de primaria' WHERE numero_grado = 6;

-- 3. Limpieza de cualquier registro histórico que contenga la palabra 'secundaria'
UPDATE curriculo_grado
SET nombre = REPLACE(nombre, 'secundaria', 'primaria')
WHERE nombre LIKE '%secundaria%';

-- 4. Verificar y asegurar que la versión de ruta de Matemática (MAT-2P-PLAZA) esté vinculada al grado 2.° de primaria
UPDATE aprendizaje_version_ruta
SET grado_id = '00000000-0000-4000-8000-000000000002'
WHERE id = 'b0000000-0000-4000-8000-000000000001';

-- 5. Registrar y asociar la institución educativa oficial de Ayacucho (I.E. 38001 Mariscal Sucre)
INSERT INTO escuela_institucion (id, codigo, nombre, nivel, ubigeo, activa) VALUES
  ('i0000000-0000-4000-8000-000000000001', '38001', 'I.E. 38001 Mariscal Sucre', 'PUBLICA', '050101', TRUE)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO escuela_anio_escolar (id, institucion_id, anio, estado) VALUES
  ('y0000000-0000-4000-8000-000000000001', 'i0000000-0000-4000-8000-000000000001', 2026, 'ACTIVO')
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

INSERT INTO escuela_aula (id, institucion_id, anio_escolar_id, grado_id, codigo, nombre) VALUES
  ('au000000-0000-4000-8000-000000000001', 'i0000000-0000-4000-8000-000000000001', 'y0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', '2A', '2.° Primaria A')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO escuela_membresia_aula (id, aula_id, usuario_id, rol, estado) VALUES
  ('m0000000-0000-4000-8000-000000000001', 'au000000-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', 'ESTUDIANTE', 'ACTIVO')
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

-- 6. Consulta de verificación
SELECT id, numero_grado, nombre FROM curriculo_grado ORDER BY numero_grado;

