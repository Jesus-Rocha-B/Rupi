-- V39__catalogo_curricular_minedu_hu49.sql
-- Catálogo general de grados, áreas curriculares y competencias del MINEDU (HU-49)
--
-- Completa el CNEB 2017 de primaria para que los roadmaps sigan la normativa oficial: las nueve áreas
-- del Programa Curricular de Educación Primaria en su orden, las 28 competencias de área con su número
-- oficial y sus capacidades. Las competencias transversales (30 y 31) quedan para cuando el equipo
-- decida cómo mostrarlas, porque no pertenecen a un área. La 27 (emprendimiento) es solo de secundaria.
-- Agrega los permisos curriculo.ver y curriculo.gestionar de la consola.
-- Escritor: Django (curriculo_* e identidad_*). Idempotente. Los ids existentes no cambian.
-- Después de aplicarla, volver a ejecutar grants-django-admin.sql para que la consola escriba en curriculo_capacidad.

-- Orden oficial del área en el programa curricular
SET @col_orden = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'curriculo_area' AND COLUMN_NAME = 'orden');
SET @alter_sql = IF(@col_orden = 0, 'ALTER TABLE curriculo_area ADD COLUMN orden TINYINT UNSIGNED NULL AFTER nombre', 'SELECT 1');
PREPARE stmt FROM @alter_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Número de la competencia en el CNEB (1 a 31)
SET @col_numero = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'curriculo_competencia' AND COLUMN_NAME = 'numero_cneb');
SET @alter_sql = IF(@col_numero = 0, 'ALTER TABLE curriculo_competencia ADD COLUMN numero_cneb TINYINT UNSIGNED NULL AFTER codigo', 'SELECT 1');
PREPARE stmt FROM @alter_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Los nombres oficiales de Educación Religiosa superan los 180 caracteres
SET @largo_nombre = (SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'curriculo_competencia' AND COLUMN_NAME = 'nombre');
SET @alter_sql = IF(@largo_nombre < 255, 'ALTER TABLE curriculo_competencia MODIFY COLUMN nombre VARCHAR(255) NOT NULL', 'SELECT 1');
PREPARE stmt FROM @alter_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Capacidades de cada competencia, en el orden del CNEB
CREATE TABLE IF NOT EXISTS curriculo_capacidad (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  competencia_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  orden TINYINT UNSIGNED NOT NULL,
  nombre VARCHAR(255) NOT NULL,
  UNIQUE KEY uq_capacidad_competencia_orden (competencia_id, orden),
  FOREIGN KEY (competencia_id) REFERENCES curriculo_competencia(id)
) ENGINE=InnoDB;

-- Áreas que faltaban en el CNEB 2017
INSERT IGNORE INTO curriculo_area (id, version_catalogo_id, codigo, nombre) VALUES
('a0000000-0000-4000-8000-000000000007', 'c0000000-0000-4000-8000-000000000001', 'EF', 'Educación Física'),
('a0000000-0000-4000-8000-000000000008', 'c0000000-0000-4000-8000-000000000001', 'CAS2', 'Castellano como Segunda Lengua'),
('a0000000-0000-4000-8000-000000000009', 'c0000000-0000-4000-8000-000000000001', 'REL', 'Educación Religiosa');

UPDATE curriculo_area SET orden = CASE codigo
    WHEN 'SOC' THEN 1 WHEN 'EF' THEN 2 WHEN 'COM' THEN 3 WHEN 'ART' THEN 4 WHEN 'CAS2' THEN 5
    WHEN 'ING' THEN 6 WHEN 'MAT' THEN 7 WHEN 'CYT' THEN 8 WHEN 'REL' THEN 9 END
WHERE version_catalogo_id = 'c0000000-0000-4000-8000-000000000001' AND orden IS NULL
  AND codigo IN ('SOC', 'EF', 'COM', 'ART', 'CAS2', 'ING', 'MAT', 'CYT', 'REL');

-- Nombre oficial completo (antes estaba recortado)
UPDATE curriculo_competencia
SET nombre = 'Explica el mundo físico basándose en conocimientos sobre los seres vivos, materia y energía, biodiversidad, Tierra y universo'
WHERE id = 'c2000000-0000-4000-8000-000000000021'
  AND nombre = 'Explica el mundo físico basándose en conocimientos sobre seres vivos, materia y energía';

-- Competencias de área que faltaban (ids c4900000-…-0000000000NN, NN = número CNEB)
INSERT IGNORE INTO curriculo_competencia (id, area_id, codigo, numero_cneb, nombre) VALUES
('c4900000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000007', 'EF-MOTRIZ', 2, 'Se desenvuelve de manera autónoma a través de su motricidad'),
('c4900000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000007', 'EF-SALUD', 3, 'Asume una vida saludable'),
('c4900000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000007', 'EF-SOCIO', 4, 'Interactúa a través de sus habilidades sociomotrices'),
('c4900000-0000-4000-8000-000000000010', 'a0000000-0000-4000-8000-000000000008', 'CAS2-ORAL', 10, 'Se comunica oralmente en castellano como segunda lengua'),
('c4900000-0000-4000-8000-000000000011', 'a0000000-0000-4000-8000-000000000008', 'CAS2-LEE', 11, 'Lee diversos tipos de textos escritos en castellano como segunda lengua'),
('c4900000-0000-4000-8000-000000000012', 'a0000000-0000-4000-8000-000000000008', 'CAS2-ESC', 12, 'Escribe diversos tipos de textos en castellano como segunda lengua'),
('c4900000-0000-4000-8000-000000000013', 'a0000000-0000-4000-8000-000000000006', 'ING-ORAL', 13, 'Se comunica oralmente en inglés como lengua extranjera'),
('c4900000-0000-4000-8000-000000000014', 'a0000000-0000-4000-8000-000000000006', 'ING-LEE', 14, 'Lee diversos tipos de textos escritos en inglés como lengua extranjera'),
('c4900000-0000-4000-8000-000000000015', 'a0000000-0000-4000-8000-000000000006', 'ING-ESC', 15, 'Escribe diversos tipos de textos en inglés como lengua extranjera'),
('c4900000-0000-4000-8000-000000000017', 'a0000000-0000-4000-8000-000000000004', 'SOC-HIST', 17, 'Construye interpretaciones históricas'),
('c4900000-0000-4000-8000-000000000018', 'a0000000-0000-4000-8000-000000000004', 'SOC-AMBIENTE', 18, 'Gestiona responsablemente el espacio y el ambiente'),
('c4900000-0000-4000-8000-000000000019', 'a0000000-0000-4000-8000-000000000004', 'SOC-ECONOMIA', 19, 'Gestiona responsablemente los recursos económicos'),
('c4900000-0000-4000-8000-000000000022', 'a0000000-0000-4000-8000-000000000003', 'CYT-DISENA', 22, 'Diseña y construye soluciones tecnológicas para resolver problemas de su entorno'),
('c4900000-0000-4000-8000-000000000028', 'a0000000-0000-4000-8000-000000000009', 'REL-IDENT', 28, 'Construye su identidad como persona humana, amada por Dios, digna, libre y trascendente, comprendiendo la doctrina de su propia religión, abierto al diálogo con las que le son cercanas'),
('c4900000-0000-4000-8000-000000000029', 'a0000000-0000-4000-8000-000000000009', 'REL-FE', 29, 'Asume la experiencia del encuentro personal y comunitario con Dios en su proyecto de vida en coherencia con su creencia religiosa');

-- Número CNEB de las competencias que ya existían
UPDATE curriculo_competencia SET numero_cneb = 1 WHERE id = 'c2000000-0000-4000-8000-000000000030' AND numero_cneb IS NULL;
UPDATE curriculo_competencia SET numero_cneb = 5 WHERE id = 'c2000000-0000-4000-8000-000000000040' AND numero_cneb IS NULL;
UPDATE curriculo_competencia SET numero_cneb = 6 WHERE id = 'c2000000-0000-4000-8000-000000000041' AND numero_cneb IS NULL;
UPDATE curriculo_competencia SET numero_cneb = 7 WHERE id = 'c2000000-0000-4000-8000-000000000010' AND numero_cneb IS NULL;
UPDATE curriculo_competencia SET numero_cneb = 8 WHERE id = 'c2000000-0000-4000-8000-000000000011' AND numero_cneb IS NULL;
UPDATE curriculo_competencia SET numero_cneb = 9 WHERE id = 'c2000000-0000-4000-8000-000000000012' AND numero_cneb IS NULL;
UPDATE curriculo_competencia SET numero_cneb = 16 WHERE id = 'c2000000-0000-4000-8000-000000000031' AND numero_cneb IS NULL;
UPDATE curriculo_competencia SET numero_cneb = 20 WHERE id = 'c2000000-0000-4000-8000-000000000020' AND numero_cneb IS NULL;
UPDATE curriculo_competencia SET numero_cneb = 21 WHERE id = 'c2000000-0000-4000-8000-000000000021' AND numero_cneb IS NULL;
UPDATE curriculo_competencia SET numero_cneb = 23 WHERE id = 'c1000000-0000-4000-8000-000000000001' AND numero_cneb IS NULL;
UPDATE curriculo_competencia SET numero_cneb = 24 WHERE id = 'c1000000-0000-4000-8000-000000000002' AND numero_cneb IS NULL;
UPDATE curriculo_competencia SET numero_cneb = 25 WHERE id = 'c1000000-0000-4000-8000-000000000003' AND numero_cneb IS NULL;
UPDATE curriculo_competencia SET numero_cneb = 26 WHERE id = 'c1000000-0000-4000-8000-000000000004' AND numero_cneb IS NULL;

-- Capacidades (ids c4910000-…-00000000NNOO: competencia NN, capacidad OO)
INSERT IGNORE INTO curriculo_capacidad (id, competencia_id, orden, nombre) VALUES
('c4910000-0000-4000-8000-000000000101', 'c2000000-0000-4000-8000-000000000030', 1, 'Se valora a sí mismo'),
('c4910000-0000-4000-8000-000000000102', 'c2000000-0000-4000-8000-000000000030', 2, 'Autorregula sus emociones'),
('c4910000-0000-4000-8000-000000000103', 'c2000000-0000-4000-8000-000000000030', 3, 'Reflexiona y argumenta éticamente'),
('c4910000-0000-4000-8000-000000000104', 'c2000000-0000-4000-8000-000000000030', 4, 'Vive su sexualidad de manera integral y responsable de acuerdo a su etapa de desarrollo y madurez'),
('c4910000-0000-4000-8000-000000000201', 'c4900000-0000-4000-8000-000000000002', 1, 'Comprende su cuerpo'),
('c4910000-0000-4000-8000-000000000202', 'c4900000-0000-4000-8000-000000000002', 2, 'Se expresa corporalmente'),
('c4910000-0000-4000-8000-000000000301', 'c4900000-0000-4000-8000-000000000003', 1, 'Comprende las relaciones entre la actividad física, alimentación, postura e higiene personal y del ambiente, y la salud'),
('c4910000-0000-4000-8000-000000000302', 'c4900000-0000-4000-8000-000000000003', 2, 'Incorpora prácticas que mejoran su calidad de vida'),
('c4910000-0000-4000-8000-000000000401', 'c4900000-0000-4000-8000-000000000004', 1, 'Se relaciona utilizando sus habilidades sociomotrices'),
('c4910000-0000-4000-8000-000000000402', 'c4900000-0000-4000-8000-000000000004', 2, 'Crea y aplica estrategias y tácticas de juego'),
('c4910000-0000-4000-8000-000000000501', 'c2000000-0000-4000-8000-000000000040', 1, 'Percibe manifestaciones artístico-culturales'),
('c4910000-0000-4000-8000-000000000502', 'c2000000-0000-4000-8000-000000000040', 2, 'Contextualiza manifestaciones artístico-culturales'),
('c4910000-0000-4000-8000-000000000503', 'c2000000-0000-4000-8000-000000000040', 3, 'Reflexiona creativa y críticamente sobre manifestaciones artístico-culturales'),
('c4910000-0000-4000-8000-000000000601', 'c2000000-0000-4000-8000-000000000041', 1, 'Explora y experimenta los lenguajes artísticos'),
('c4910000-0000-4000-8000-000000000602', 'c2000000-0000-4000-8000-000000000041', 2, 'Aplica procesos creativos'),
('c4910000-0000-4000-8000-000000000603', 'c2000000-0000-4000-8000-000000000041', 3, 'Evalúa y comunica sus procesos y proyectos'),
('c4910000-0000-4000-8000-000000000701', 'c2000000-0000-4000-8000-000000000010', 1, 'Obtiene información del texto oral'),
('c4910000-0000-4000-8000-000000000702', 'c2000000-0000-4000-8000-000000000010', 2, 'Infiere e interpreta información del texto oral'),
('c4910000-0000-4000-8000-000000000703', 'c2000000-0000-4000-8000-000000000010', 3, 'Adecúa, organiza y desarrolla las ideas de forma coherente y cohesionada'),
('c4910000-0000-4000-8000-000000000704', 'c2000000-0000-4000-8000-000000000010', 4, 'Utiliza recursos no verbales y paraverbales de forma estratégica'),
('c4910000-0000-4000-8000-000000000705', 'c2000000-0000-4000-8000-000000000010', 5, 'Interactúa estratégicamente con distintos interlocutores'),
('c4910000-0000-4000-8000-000000000706', 'c2000000-0000-4000-8000-000000000010', 6, 'Reflexiona y evalúa la forma, el contenido y contexto del texto oral'),
('c4910000-0000-4000-8000-000000000801', 'c2000000-0000-4000-8000-000000000011', 1, 'Obtiene información del texto escrito'),
('c4910000-0000-4000-8000-000000000802', 'c2000000-0000-4000-8000-000000000011', 2, 'Infiere e interpreta información del texto'),
('c4910000-0000-4000-8000-000000000803', 'c2000000-0000-4000-8000-000000000011', 3, 'Reflexiona y evalúa la forma, el contenido y contexto del texto'),
('c4910000-0000-4000-8000-000000000901', 'c2000000-0000-4000-8000-000000000012', 1, 'Adecúa el texto a la situación comunicativa'),
('c4910000-0000-4000-8000-000000000902', 'c2000000-0000-4000-8000-000000000012', 2, 'Organiza y desarrolla las ideas de forma coherente y cohesionada'),
('c4910000-0000-4000-8000-000000000903', 'c2000000-0000-4000-8000-000000000012', 3, 'Utiliza convenciones del lenguaje escrito de forma pertinente'),
('c4910000-0000-4000-8000-000000000904', 'c2000000-0000-4000-8000-000000000012', 4, 'Reflexiona y evalúa la forma, el contenido y contexto del texto escrito'),
('c4910000-0000-4000-8000-000000001001', 'c4900000-0000-4000-8000-000000000010', 1, 'Obtiene información del texto oral'),
('c4910000-0000-4000-8000-000000001002', 'c4900000-0000-4000-8000-000000000010', 2, 'Infiere e interpreta información del texto oral'),
('c4910000-0000-4000-8000-000000001003', 'c4900000-0000-4000-8000-000000000010', 3, 'Adecúa, organiza y desarrolla las ideas de forma coherente y cohesionada'),
('c4910000-0000-4000-8000-000000001004', 'c4900000-0000-4000-8000-000000000010', 4, 'Utiliza recursos no verbales y paraverbales de forma estratégica'),
('c4910000-0000-4000-8000-000000001005', 'c4900000-0000-4000-8000-000000000010', 5, 'Interactúa estratégicamente con distintos interlocutores'),
('c4910000-0000-4000-8000-000000001006', 'c4900000-0000-4000-8000-000000000010', 6, 'Reflexiona y evalúa la forma, el contenido y contexto del texto oral'),
('c4910000-0000-4000-8000-000000001101', 'c4900000-0000-4000-8000-000000000011', 1, 'Obtiene información del texto escrito'),
('c4910000-0000-4000-8000-000000001102', 'c4900000-0000-4000-8000-000000000011', 2, 'Infiere e interpreta información del texto'),
('c4910000-0000-4000-8000-000000001103', 'c4900000-0000-4000-8000-000000000011', 3, 'Reflexiona y evalúa la forma, el contenido y contexto del texto'),
('c4910000-0000-4000-8000-000000001201', 'c4900000-0000-4000-8000-000000000012', 1, 'Adecúa el texto a la situación comunicativa'),
('c4910000-0000-4000-8000-000000001202', 'c4900000-0000-4000-8000-000000000012', 2, 'Organiza y desarrolla las ideas de forma coherente y cohesionada'),
('c4910000-0000-4000-8000-000000001203', 'c4900000-0000-4000-8000-000000000012', 3, 'Utiliza convenciones del lenguaje escrito de forma pertinente'),
('c4910000-0000-4000-8000-000000001204', 'c4900000-0000-4000-8000-000000000012', 4, 'Reflexiona y evalúa la forma, el contenido y contexto del texto escrito'),
('c4910000-0000-4000-8000-000000001301', 'c4900000-0000-4000-8000-000000000013', 1, 'Obtiene información del texto oral'),
('c4910000-0000-4000-8000-000000001302', 'c4900000-0000-4000-8000-000000000013', 2, 'Infiere e interpreta información del texto oral'),
('c4910000-0000-4000-8000-000000001303', 'c4900000-0000-4000-8000-000000000013', 3, 'Adecúa, organiza y desarrolla las ideas de forma coherente y cohesionada'),
('c4910000-0000-4000-8000-000000001304', 'c4900000-0000-4000-8000-000000000013', 4, 'Utiliza recursos no verbales y paraverbales de forma estratégica'),
('c4910000-0000-4000-8000-000000001305', 'c4900000-0000-4000-8000-000000000013', 5, 'Interactúa estratégicamente con distintos interlocutores'),
('c4910000-0000-4000-8000-000000001306', 'c4900000-0000-4000-8000-000000000013', 6, 'Reflexiona y evalúa la forma, el contenido y contexto del texto oral'),
('c4910000-0000-4000-8000-000000001401', 'c4900000-0000-4000-8000-000000000014', 1, 'Obtiene información del texto escrito'),
('c4910000-0000-4000-8000-000000001402', 'c4900000-0000-4000-8000-000000000014', 2, 'Infiere e interpreta información del texto'),
('c4910000-0000-4000-8000-000000001403', 'c4900000-0000-4000-8000-000000000014', 3, 'Reflexiona y evalúa la forma, el contenido y contexto del texto'),
('c4910000-0000-4000-8000-000000001501', 'c4900000-0000-4000-8000-000000000015', 1, 'Adecúa el texto a la situación comunicativa'),
('c4910000-0000-4000-8000-000000001502', 'c4900000-0000-4000-8000-000000000015', 2, 'Organiza y desarrolla las ideas de forma coherente y cohesionada'),
('c4910000-0000-4000-8000-000000001503', 'c4900000-0000-4000-8000-000000000015', 3, 'Utiliza convenciones del lenguaje escrito de forma pertinente'),
('c4910000-0000-4000-8000-000000001504', 'c4900000-0000-4000-8000-000000000015', 4, 'Reflexiona y evalúa la forma, el contenido y contexto del texto escrito'),
('c4910000-0000-4000-8000-000000001601', 'c2000000-0000-4000-8000-000000000031', 1, 'Interactúa con todas las personas'),
('c4910000-0000-4000-8000-000000001602', 'c2000000-0000-4000-8000-000000000031', 2, 'Construye normas y asume acuerdos y leyes'),
('c4910000-0000-4000-8000-000000001603', 'c2000000-0000-4000-8000-000000000031', 3, 'Maneja conflictos de manera constructiva'),
('c4910000-0000-4000-8000-000000001604', 'c2000000-0000-4000-8000-000000000031', 4, 'Delibera sobre asuntos públicos'),
('c4910000-0000-4000-8000-000000001605', 'c2000000-0000-4000-8000-000000000031', 5, 'Participa en acciones que promueven el bienestar común'),
('c4910000-0000-4000-8000-000000001701', 'c4900000-0000-4000-8000-000000000017', 1, 'Interpreta críticamente fuentes diversas'),
('c4910000-0000-4000-8000-000000001702', 'c4900000-0000-4000-8000-000000000017', 2, 'Comprende el tiempo histórico'),
('c4910000-0000-4000-8000-000000001703', 'c4900000-0000-4000-8000-000000000017', 3, 'Elabora explicaciones sobre procesos históricos'),
('c4910000-0000-4000-8000-000000001801', 'c4900000-0000-4000-8000-000000000018', 1, 'Comprende las relaciones entre los elementos naturales y sociales'),
('c4910000-0000-4000-8000-000000001802', 'c4900000-0000-4000-8000-000000000018', 2, 'Maneja fuentes de información para comprender el espacio geográfico y el ambiente'),
('c4910000-0000-4000-8000-000000001803', 'c4900000-0000-4000-8000-000000000018', 3, 'Genera acciones para conservar el ambiente local y global'),
('c4910000-0000-4000-8000-000000001901', 'c4900000-0000-4000-8000-000000000019', 1, 'Comprende las relaciones entre los elementos del sistema económico y financiero'),
('c4910000-0000-4000-8000-000000001902', 'c4900000-0000-4000-8000-000000000019', 2, 'Toma decisiones económicas y financieras'),
('c4910000-0000-4000-8000-000000002001', 'c2000000-0000-4000-8000-000000000020', 1, 'Problematiza situaciones para hacer indagación'),
('c4910000-0000-4000-8000-000000002002', 'c2000000-0000-4000-8000-000000000020', 2, 'Diseña estrategias para hacer indagación'),
('c4910000-0000-4000-8000-000000002003', 'c2000000-0000-4000-8000-000000000020', 3, 'Genera y registra datos o información'),
('c4910000-0000-4000-8000-000000002004', 'c2000000-0000-4000-8000-000000000020', 4, 'Analiza datos e información'),
('c4910000-0000-4000-8000-000000002005', 'c2000000-0000-4000-8000-000000000020', 5, 'Evalúa y comunica el proceso y resultados de su indagación'),
('c4910000-0000-4000-8000-000000002101', 'c2000000-0000-4000-8000-000000000021', 1, 'Comprende y usa conocimientos sobre los seres vivos, materia y energía, biodiversidad, Tierra y universo'),
('c4910000-0000-4000-8000-000000002102', 'c2000000-0000-4000-8000-000000000021', 2, 'Evalúa las implicancias del saber y del quehacer científico y tecnológico'),
('c4910000-0000-4000-8000-000000002201', 'c4900000-0000-4000-8000-000000000022', 1, 'Determina una alternativa de solución tecnológica'),
('c4910000-0000-4000-8000-000000002202', 'c4900000-0000-4000-8000-000000000022', 2, 'Diseña la alternativa de solución tecnológica'),
('c4910000-0000-4000-8000-000000002203', 'c4900000-0000-4000-8000-000000000022', 3, 'Implementa y valida la alternativa de solución tecnológica'),
('c4910000-0000-4000-8000-000000002204', 'c4900000-0000-4000-8000-000000000022', 4, 'Evalúa y comunica el funcionamiento y los impactos de su alternativa de solución tecnológica'),
('c4910000-0000-4000-8000-000000002301', 'c1000000-0000-4000-8000-000000000001', 1, 'Traduce cantidades a expresiones numéricas'),
('c4910000-0000-4000-8000-000000002302', 'c1000000-0000-4000-8000-000000000001', 2, 'Comunica su comprensión sobre los números y las operaciones'),
('c4910000-0000-4000-8000-000000002303', 'c1000000-0000-4000-8000-000000000001', 3, 'Usa estrategias y procedimientos de estimación y cálculo'),
('c4910000-0000-4000-8000-000000002304', 'c1000000-0000-4000-8000-000000000001', 4, 'Argumenta afirmaciones sobre las relaciones numéricas y las operaciones'),
('c4910000-0000-4000-8000-000000002401', 'c1000000-0000-4000-8000-000000000002', 1, 'Traduce datos y condiciones a expresiones algebraicas y gráficas'),
('c4910000-0000-4000-8000-000000002402', 'c1000000-0000-4000-8000-000000000002', 2, 'Comunica su comprensión sobre las relaciones algebraicas'),
('c4910000-0000-4000-8000-000000002403', 'c1000000-0000-4000-8000-000000000002', 3, 'Usa estrategias y procedimientos para encontrar equivalencias y reglas generales'),
('c4910000-0000-4000-8000-000000002404', 'c1000000-0000-4000-8000-000000000002', 4, 'Argumenta afirmaciones sobre relaciones de cambio y equivalencia'),
('c4910000-0000-4000-8000-000000002501', 'c1000000-0000-4000-8000-000000000003', 1, 'Modela objetos con formas geométricas y sus transformaciones'),
('c4910000-0000-4000-8000-000000002502', 'c1000000-0000-4000-8000-000000000003', 2, 'Comunica su comprensión sobre las formas y relaciones geométricas'),
('c4910000-0000-4000-8000-000000002503', 'c1000000-0000-4000-8000-000000000003', 3, 'Usa estrategias y procedimientos para orientarse en el espacio'),
('c4910000-0000-4000-8000-000000002504', 'c1000000-0000-4000-8000-000000000003', 4, 'Argumenta afirmaciones sobre relaciones geométricas'),
('c4910000-0000-4000-8000-000000002601', 'c1000000-0000-4000-8000-000000000004', 1, 'Representa datos con gráficos y medidas estadísticas o probabilísticas'),
('c4910000-0000-4000-8000-000000002602', 'c1000000-0000-4000-8000-000000000004', 2, 'Comunica su comprensión de los conceptos estadísticos y probabilísticos'),
('c4910000-0000-4000-8000-000000002603', 'c1000000-0000-4000-8000-000000000004', 3, 'Usa estrategias y procedimientos para recopilar y procesar datos'),
('c4910000-0000-4000-8000-000000002604', 'c1000000-0000-4000-8000-000000000004', 4, 'Sustenta conclusiones o decisiones con base en la información obtenida'),
('c4910000-0000-4000-8000-000000002801', 'c4900000-0000-4000-8000-000000000028', 1, 'Conoce a Dios y asume su identidad religiosa y espiritual como persona digna, libre y trascendente'),
('c4910000-0000-4000-8000-000000002802', 'c4900000-0000-4000-8000-000000000028', 2, 'Cultiva y valora las manifestaciones religiosas de su entorno argumentando su fe de manera comprensible y respetuosa'),
('c4910000-0000-4000-8000-000000002901', 'c4900000-0000-4000-8000-000000000029', 1, 'Transforma su entorno desde el encuentro personal y comunitario con Dios y desde la fe que profesa'),
('c4910000-0000-4000-8000-000000002902', 'c4900000-0000-4000-8000-000000000029', 2, 'Actúa coherentemente en razón de su fe según los principios de su conciencia moral en situaciones concretas de la vida');

-- Permisos de la consola: ver el catálogo y gestionarlo
INSERT IGNORE INTO identidad_permiso (id, codigo, descripcion) VALUES
('a0490000-0000-4000-8000-000000000101', 'curriculo.ver', 'Consultar el catálogo de grados, áreas y competencias del MINEDU'),
('a0490000-0000-4000-8000-000000000102', 'curriculo.gestionar', 'Registrar y corregir áreas, competencias y capacidades del catálogo');

INSERT IGNORE INTO identidad_rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM identidad_rol r
JOIN identidad_permiso p ON p.codigo IN ('curriculo.ver', 'curriculo.gestionar')
WHERE r.codigo = 'ADMINISTRADOR';

INSERT IGNORE INTO identidad_rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM identidad_rol r
JOIN identidad_permiso p ON p.codigo = 'curriculo.ver'
WHERE r.codigo = 'SOPORTE';
