-- V10: Catálogo completo de Roadmaps Escolares CNEB para TODOS los grados de primaria (1.° a 6.°).
-- Genera rutas publicadas con unidades curriculares, competencias oficiales, paradas en zigzag,
-- bloques didácticos pedagógicos, preguntas de evaluación formativa y contextos culturales.
-- Esquema: rupi (MySQL 8.4 InnoDB / UTF-8 mb4). Idempotente.

USE rupi;
SET NAMES utf8mb4;

-- ============================================================================
-- 1. RUTAS, VERSIONES DE RUTA Y UNIDADES CURRICULARES CNEB (1.° a 6.°)
-- ============================================================================

-- Ruta y Versión: Matemática: Primeros Números y Formas en Ayacucho
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000101', '11111111-1111-4111-8111-111111111111', 'MAT-1P-INICIO',
   'Matemática: Primeros Números y Formas en Ayacucho',
   'Aventura de conteo, nociones espaciales y primeras figuras geométricas guiadas por Rupi.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000101', 'd0000000-0000-4000-8000-000000000101', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001', 1, 'PUBLICADO', UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, numero_secuencia, titulo) VALUES
  ('d2000000-0000-4000-8000-000000000101', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001', 'U1-MAT1', 1, 'Contamos y jugamos en nuestra comunidad')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- Ruta y Versión: Comunicación: Vocales, Sonidos y Rimas con Rupi
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000102', '11111111-1111-4111-8111-111111111111', 'COM-1P-LETRAS',
   'Comunicación: Vocales, Sonidos y Rimas con Rupi',
   'Ruta interactiva de iniciación a la lectura y escritura con rimas, vocales y sonidos de la naturaleza.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000102', 'd0000000-0000-4000-8000-000000000102', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000002', 1, 'PUBLICADO', UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, numero_secuencia, titulo) VALUES
  ('d2000000-0000-4000-8000-000000000102', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000002', 'U1-COM1', 1, 'Jugando con los sonidos de las palabras')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- Ruta y Versión: Ciencia y Tecnología: Los Sentidos y el Entorno Natural
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000103', '11111111-1111-4111-8111-111111111111', 'CYT-1P-EXPLOR',
   'Ciencia y Tecnología: Los Sentidos y el Entorno Natural',
   'Observación y exploración de los cinco sentidos, plantas y animales de nuestro entorno escolar.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000103', 'd0000000-0000-4000-8000-000000000103', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000003', 1, 'PUBLICADO', UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, numero_secuencia, titulo) VALUES
  ('d2000000-0000-4000-8000-000000000103', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000003', 'U1-CYT1', 1, 'Explorando el mundo con nuestros sentidos')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- Ruta y Versión: Comunicación: Mitos y Narraciones de los Andes
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000302', '11111111-1111-4111-8111-111111111111', 'COM-3P-MITOS',
   'Comunicación: Mitos y Narraciones de los Andes',
   'Lectura comprensiva de mitos ancestrales, identificación de ideas principales y redacción creativa.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000302', 'd0000000-0000-4000-8000-000000000302', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000002', 1, 'PUBLICADO', UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, numero_secuencia, titulo) VALUES
  ('d2000000-0000-4000-8000-000000000302', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000002', 'U1-COM3', 1, 'Mitos y leyendas de la tradición oral peruana')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- Ruta y Versión: Ciencia y Tecnología: La Fuerza del Sol, el Viento y la Materia
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000303', '11111111-1111-4111-8111-111111111111', 'CYT-3P-ENERGIA',
   'Ciencia y Tecnología: La Fuerza del Sol, el Viento y la Materia',
   'Indagación científica sobre energías naturales, propiedades de los materiales y estados de la materia.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000303', 'd0000000-0000-4000-8000-000000000303', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000003', 1, 'PUBLICADO', UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, numero_secuencia, titulo) VALUES
  ('d2000000-0000-4000-8000-000000000303', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000003', 'U1-CYT3', 1, 'Materia y fuerzas en nuestro entorno andino')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- Ruta y Versión: Matemática: Fracciones y Medidas en las Ferias Andinas
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000401', '11111111-1111-4111-8111-111111111111', 'MAT-4P-FRACC',
   'Matemática: Fracciones y Medidas en las Ferias Andinas',
   'Desafíos numéricos con fracciones, cálculo de áreas y perímetros y resolución de problemas cotidianos.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000401', 'd0000000-0000-4000-8000-000000000401', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000001', 1, 'PUBLICADO', UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, numero_secuencia, titulo) VALUES
  ('d2000000-0000-4000-8000-000000000401', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000001', 'U1-MAT4', 1, 'Fracciones y mediciones en la vida cotidiana')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- Ruta y Versión: Comunicación: Textos Informativos y Tradición Oral Peruana
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000402', '11111111-1111-4111-8111-111111111111', 'COM-4P-TEXTOS',
   'Comunicación: Textos Informativos y Tradición Oral Peruana',
   'Comprensión de artículos, afiches y crónicas con técnicas de redacción y expresión oral.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000402', 'd0000000-0000-4000-8000-000000000402', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000002', 1, 'PUBLICADO', UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, numero_secuencia, titulo) VALUES
  ('d2000000-0000-4000-8000-000000000402', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000002', 'U1-COM4', 1, 'Lectura crítica de textos informativos y afiches')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- Ruta y Versión: Ciencia y Tecnología: Ecosistemas y Cadenas Vivas del Perú
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000403', '11111111-1111-4111-8111-111111111111', 'CYT-4P-ECOSIS',
   'Ciencia y Tecnología: Ecosistemas y Cadenas Vivas del Perú',
   'Exploración de la cadena trófica, conservación de especies en peligro y el rol del agua en los ecosistemas.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000403', 'd0000000-0000-4000-8000-000000000403', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000003', 1, 'PUBLICADO', UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, numero_secuencia, titulo) VALUES
  ('d2000000-0000-4000-8000-000000000403', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000003', 'U1-CYT4', 1, 'Ecosistemas andinos y redes tróficas')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- Ruta y Versión: Matemática: Números Decimales y Porcentajes en el Comercio
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000501', '11111111-1111-4111-8111-111111111111', 'MAT-5P-DECIM',
   'Matemática: Números Decimales y Porcentajes en el Comercio',
   'Operaciones con decimales, cálculo de porcentajes y análisis de tablas de datos estadísticos.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000501', 'd0000000-0000-4000-8000-000000000501', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000001', 1, 'PUBLICADO', UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, numero_secuencia, titulo) VALUES
  ('d2000000-0000-4000-8000-000000000501', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000001', 'U1-MAT5', 1, 'Decimales, razones y porcentajes comerciales')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- Ruta y Versión: Comunicación: Textos Argumentativos y Opinión Crítica
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000502', '11111111-1111-4111-8111-111111111111', 'COM-5P-ARGUM',
   'Comunicación: Textos Argumentativos y Opinión Crítica',
   'Desarrollo de posturas críticas, ensayos breves, debates escolares y declamación de poesía peruana.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000502', 'd0000000-0000-4000-8000-000000000502', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000002', 1, 'PUBLICADO', UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, numero_secuencia, titulo) VALUES
  ('d2000000-0000-4000-8000-000000000502', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000002', 'U1-COM5', 1, 'Argumentación, debates y expresión ciudadana')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- Ruta y Versión: Ciencia y Tecnología: Sistemas del Cuerpo Humano y Nutrición Andina
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000503', '11111111-1111-4111-8111-111111111111', 'CYT-5P-CUERPO',
   'Ciencia y Tecnología: Sistemas del Cuerpo Humano y Nutrición Andina',
   'Estudio de la función digestiva, circulatoria y respiratoria vinculada a la alimentación con granos andinos.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000503', 'd0000000-0000-4000-8000-000000000503', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000003', 1, 'PUBLICADO', UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, numero_secuencia, titulo) VALUES
  ('d2000000-0000-4000-8000-000000000503', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000003', 'U1-CYT5', 1, 'Sistemas biológicos y nutrición con súper granos andinos')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- Ruta y Versión: Matemática: Proporcionalidad, Álgebra Inicial y Geometría Espacial
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000601', '11111111-1111-4111-8111-111111111111', 'MAT-6P-PROPOR',
   'Matemática: Proporcionalidad, Álgebra Inicial y Geometría Espacial',
   'Retos avanzados de proporcionalidad, ecuaciones, volumen de prismas y pirámides y análisis probabilístico.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000601', 'd0000000-0000-4000-8000-000000000601', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-000000000001', 1, 'PUBLICADO', UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, numero_secuencia, titulo) VALUES
  ('d2000000-0000-4000-8000-000000000601', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-000000000001', 'U1-MAT6', 1, 'Proporcionalidad, ecuaciones y cuerpos geométricos')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- Ruta y Versión: Comunicación: Crónicas, Reportajes y Creación Literaria
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000602', '11111111-1111-4111-8111-111111111111', 'COM-6P-CRONIC',
   'Comunicación: Crónicas, Reportajes y Creación Literaria',
   'Producción periodística escolar, elaboración de crónicas y dominio de recursos literarios para la graduación.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000602', 'd0000000-0000-4000-8000-000000000602', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-000000000002', 1, 'PUBLICADO', UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, numero_secuencia, titulo) VALUES
  ('d2000000-0000-4000-8000-000000000602', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-000000000002', 'U1-COM6', 1, 'Crónicas vivas y reportajes periodísticos escolares')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- Ruta y Versión: Ciencia y Tecnología: Energías Renovables y Cambio Climático
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000603', '11111111-1111-4111-8111-111111111111', 'CYT-6P-CLIMA',
   'Ciencia y Tecnología: Energías Renovables y Cambio Climático',
   'Investigación sobre calentamiento global, biotecnología andina y proyectos sostenibles para el futuro del Perú.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000603', 'd0000000-0000-4000-8000-000000000603', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-000000000003', 1, 'PUBLICADO', UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, numero_secuencia, titulo) VALUES
  ('d2000000-0000-4000-8000-000000000603', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-000000000003', 'U1-CYT6', 1, 'Cambio climático y soluciones ecológicas andinas')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- ============================================================================
-- 2. CONTEXTOS CULTURALES ANDINOS VINCULADOS A CADA RUTA
-- ============================================================================

INSERT INTO curriculo_contexto_cultural (
  id, ruta_id, codigo, ciudad, lugar, titulo, descripcion, mensaje_animo,
  imagen_url, imagen_alt, credito_autor, credito_fuente, credito_licencia, credito_url
) VALUES
  ('cc000000-0000-4000-8000-000000000101', 'd0000000-0000-4000-8000-000000000101', 'AYACUCHO_INICIO_1P',
   'Huamanga', 'Alameda de Huamanga', 'Primeros pasos en la ciudad de piedra',
   'Entre arcos coloniales y jardines floridos, comenzamos nuestra travesía escolar con la alegría de Rupi.',
   '¡Bienvenido a primaria! Cada número es una estrella que ilumina tu camino.',
   '/images/ayacucho-plaza.jpg', 'Alameda de Huamanga', 'MINCETUR / MinCul', 'PromPerú', 'CC BY-SA 4.0', 'https://rupi.pe')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO curriculo_contexto_cultural (
  id, ruta_id, codigo, ciudad, lugar, titulo, descripcion, mensaje_animo,
  imagen_url, imagen_alt, credito_autor, credito_fuente, credito_licencia, credito_url
) VALUES
  ('cc000000-0000-4000-8000-000000000102', 'd0000000-0000-4000-8000-000000000102', 'CANTUTA_LETRAS_1P',
   'Huanta', 'Río Cachi', 'Sonidos alegres del agua andina',
   'El agua cristalina que baja de las cumbres canta melodías suaves que nos invitan a leer y jugar con las palabras.',
   '¡Tus primeras palabras tienen la fuerza y belleza del río andino!',
   '/images/ayacucho-plaza.jpg', 'Río Cachi', 'MINCETUR / MinCul', 'PromPerú', 'CC BY-SA 4.0', 'https://rupi.pe')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO curriculo_contexto_cultural (
  id, ruta_id, codigo, ciudad, lugar, titulo, descripcion, mensaje_animo,
  imagen_url, imagen_alt, credito_autor, credito_fuente, credito_licencia, credito_url
) VALUES
  ('cc000000-0000-4000-8000-000000000103', 'd0000000-0000-4000-8000-000000000103', 'JARDIN_BOTANICO_1P',
   'Huamanga', 'Jardín Escolar', 'Pequeños exploradores de la naturaleza',
   'Con lupa en mano, descubrimos los colores de las flores, el canto de las aves y las texturas de las hojas.',
   '¡Tus ojos curiosos son el instrumento más valioso de la ciencia!',
   '/images/ayacucho-plaza.jpg', 'Jardín Escolar', 'MINCETUR / MinCul', 'PromPerú', 'CC BY-SA 4.0', 'https://rupi.pe')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO curriculo_contexto_cultural (
  id, ruta_id, codigo, ciudad, lugar, titulo, descripcion, mensaje_animo,
  imagen_url, imagen_alt, credito_autor, credito_fuente, credito_licencia, credito_url
) VALUES
  ('cc000000-0000-4000-8000-000000000302', 'd0000000-0000-4000-8000-000000000302', 'CUEVA_PIKIMACHAY_COM',
   'Pacaycasa', 'Pikimachay', 'Voces milenarias en la roca andina',
   'En este abrigo rocoso habitaron los primeros peruanos hace más de doce mil años, dejando huellas de sus vivencias.',
   '¡Cada historia que lees te conecta con la memoria viva de nuestros ancestros!',
   '/images/ayacucho-plaza.jpg', 'Pikimachay', 'MINCETUR / MinCul', 'PromPerú', 'CC BY-SA 4.0', 'https://rupi.pe')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO curriculo_contexto_cultural (
  id, ruta_id, codigo, ciudad, lugar, titulo, descripcion, mensaje_animo,
  imagen_url, imagen_alt, credito_autor, credito_fuente, credito_licencia, credito_url
) VALUES
  ('cc000000-0000-4000-8000-000000000303', 'd0000000-0000-4000-8000-000000000303', 'MOLINOS_RIO_CYT',
   'Huamanga', 'Río Alameda', 'La fuerza invisible del agua y del viento',
   'Los antiguos molinos de piedra aprovechaban la corriente del río para moler granos de maíz y trigo sin contaminar.',
   '¡Investiga con rigor y descubre la física oculta en la vida cotidiana!',
   '/images/ayacucho-plaza.jpg', 'Río Alameda', 'MINCETUR / MinCul', 'PromPerú', 'CC BY-SA 4.0', 'https://rupi.pe')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO curriculo_contexto_cultural (
  id, ruta_id, codigo, ciudad, lugar, titulo, descripcion, mensaje_animo,
  imagen_url, imagen_alt, credito_autor, credito_fuente, credito_licencia, credito_url
) VALUES
  ('cc000000-0000-4000-8000-000000000401', 'd0000000-0000-4000-8000-000000000401', 'PAMPA_FERIA_4P',
   'Quinua', 'Feria Tradicional de Quinua', 'El mercado de los saberes',
   'Campesinos y alfareros comparten sus productos en canastas de carrizo y balanzas de bronce.',
   '¡Las fracciones son el arte de repartir con justicia y hermandad!',
   '/images/ayacucho-plaza.jpg', 'Feria Tradicional de Quinua', 'MINCETUR / MinCul', 'PromPerú', 'CC BY-SA 4.0', 'https://rupi.pe')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO curriculo_contexto_cultural (
  id, ruta_id, codigo, ciudad, lugar, titulo, descripcion, mensaje_animo,
  imagen_url, imagen_alt, credito_autor, credito_fuente, credito_licencia, credito_url
) VALUES
  ('cc000000-0000-4000-8000-000000000402', 'd0000000-0000-4000-8000-000000000402', 'BIBLIOTECA_HUAMANGA_COM',
   'Huamanga', 'Biblioteca Municipal de Ayacucho', 'Tesoros de papel e historias vivas',
   'Libros centenarios y periódicos antiguos nos muestran cómo la palabra escrita transforma y educa a los pueblos.',
   '¡Leer con atención te abre las puertas del conocimiento y la libertad!',
   '/images/ayacucho-plaza.jpg', 'Biblioteca Municipal de Ayacucho', 'MINCETUR / MinCul', 'PromPerú', 'CC BY-SA 4.0', 'https://rupi.pe')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO curriculo_contexto_cultural (
  id, ruta_id, codigo, ciudad, lugar, titulo, descripcion, mensaje_animo,
  imagen_url, imagen_alt, credito_autor, credito_fuente, credito_licencia, credito_url
) VALUES
  ('cc000000-0000-4000-8000-000000000403', 'd0000000-0000-4000-8000-000000000403', 'PAMPAS_GALERAS_CYT',
   'Lucanas', 'Reserva Nacional Pampa Galeras', 'Santuario de la vicuña y el ichu',
   'En la inmensa meseta andina, a más de cuatro mil metros de altura, las vicuñas pastan protegidas por la comunidad.',
   '¡Cuidar la biodiversidad es preservar el corazón vivo del Perú!',
   '/images/ayacucho-plaza.jpg', 'Reserva Nacional Pampa Galeras', 'MINCETUR / MinCul', 'PromPerú', 'CC BY-SA 4.0', 'https://rupi.pe')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO curriculo_contexto_cultural (
  id, ruta_id, codigo, ciudad, lugar, titulo, descripcion, mensaje_animo,
  imagen_url, imagen_alt, credito_autor, credito_fuente, credito_licencia, credito_url
) VALUES
  ('cc000000-0000-4000-8000-000000000501', 'd0000000-0000-4000-8000-000000000501', 'VILCASHUAMAN_5P',
   'Vilcashuamán', 'Templo del Sol y la Luna', 'Geometría y proporciones del Imperio',
   'La pirámide del Ushnu domina el valle con terrazas simétricas que desafían al tiempo.',
   '¡La precisión matemática de nuestros antepasados vive en tu mente despierta!',
   '/images/ayacucho-plaza.jpg', 'Templo del Sol y la Luna', 'MINCETUR / MinCul', 'PromPerú', 'CC BY-SA 4.0', 'https://rupi.pe')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO curriculo_contexto_cultural (
  id, ruta_id, codigo, ciudad, lugar, titulo, descripcion, mensaje_animo,
  imagen_url, imagen_alt, credito_autor, credito_fuente, credito_licencia, credito_url
) VALUES
  ('cc000000-0000-4000-8000-000000000502', 'd0000000-0000-4000-8000-000000000502', 'PLAZA_CONGRESO_COM',
   'Huamanga', 'Casona Boza y Solís', 'La palabra como instrumento de libertad',
   'En los salones históricos ayacuchanos se debatieron los ideales de justicia y emancipación de nuestra república.',
   '¡Defender tus ideas con argumentos razonados te convierte en un ciudadano valioso!',
   '/images/ayacucho-plaza.jpg', 'Casona Boza y Solís', 'MINCETUR / MinCul', 'PromPerú', 'CC BY-SA 4.0', 'https://rupi.pe')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO curriculo_contexto_cultural (
  id, ruta_id, codigo, ciudad, lugar, titulo, descripcion, mensaje_animo,
  imagen_url, imagen_alt, credito_autor, credito_fuente, credito_licencia, credito_url
) VALUES
  ('cc000000-0000-4000-8000-000000000503', 'd0000000-0000-4000-8000-000000000503', 'CAMPOS_QUINUA_CYT',
   'Cangallo', 'Valle del Pampas', 'El grano de oro de los Incas',
   'La quinua, la kiwicha y la cañihua nutrieron a generaciones con proteínas completas y minerales insustituibles.',
   '¡Conocer tu cuerpo y alimentarlo con sabiduría andina te da energía infinita!',
   '/images/ayacucho-plaza.jpg', 'Valle del Pampas', 'MINCETUR / MinCul', 'PromPerú', 'CC BY-SA 4.0', 'https://rupi.pe')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO curriculo_contexto_cultural (
  id, ruta_id, codigo, ciudad, lugar, titulo, descripcion, mensaje_animo,
  imagen_url, imagen_alt, credito_autor, credito_fuente, credito_licencia, credito_url
) VALUES
  ('cc000000-0000-4000-8000-000000000601', 'd0000000-0000-4000-8000-000000000601', 'CUMBRE_CONDOR_6P',
   'Cangallo', 'Cañón de Huancaraylla', 'La cima de la travesía escolar',
   'Desde las cumbres más altas donde vuela el cóndor, se contemplan los seis años de esfuerzo y crecimiento.',
   '¡Has llegado a la cima de la primaria! Vuela alto como las aves sagradas del Perú.',
   '/images/ayacucho-plaza.jpg', 'Cañón de Huancaraylla', 'MINCETUR / MinCul', 'PromPerú', 'CC BY-SA 4.0', 'https://rupi.pe')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO curriculo_contexto_cultural (
  id, ruta_id, codigo, ciudad, lugar, titulo, descripcion, mensaje_animo,
  imagen_url, imagen_alt, credito_autor, credito_fuente, credito_licencia, credito_url
) VALUES
  ('cc000000-0000-4000-8000-000000000602', 'd0000000-0000-4000-8000-000000000602', 'TEATRO_MUNICIPAL_COM',
   'Huamanga', 'Teatro Municipal de Ayacucho', 'Escenario de la oratoria y las artes',
   'Poetas, actores y oradores comparten historias que conmueven el corazón y despiertan la conciencia del pueblo.',
   '¡Tu voz y tu pluma son herramientas poderosas para transformar el porvenir!',
   '/images/ayacucho-plaza.jpg', 'Teatro Municipal de Ayacucho', 'MINCETUR / MinCul', 'PromPerú', 'CC BY-SA 4.0', 'https://rupi.pe')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO curriculo_contexto_cultural (
  id, ruta_id, codigo, ciudad, lugar, titulo, descripcion, mensaje_animo,
  imagen_url, imagen_alt, credito_autor, credito_fuente, credito_licencia, credito_url
) VALUES
  ('cc000000-0000-4000-8000-000000000603', 'd0000000-0000-4000-8000-000000000603', 'GLACIAR_RAZHUWILLCA_CYT',
   'Huanta', 'Nevado Razuhuillca', 'El centinela de hielo y las lagunas sagradas',
   'Desde sus cumbres nevadas bajan las aguas que dan vida a Huanta, recordándonos la urgencia de frenar el cambio climático.',
   '¡El futuro del planeta y de nuestras montañas está en tus manos científicas!',
   '/images/ayacucho-plaza.jpg', 'Nevado Razuhuillca', 'MINCETUR / MinCul', 'PromPerú', 'CC BY-SA 4.0', 'https://rupi.pe')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

-- ============================================================================
-- 3. ACTIVIDADES DIDÁCTICAS, VERSIONES Y PARADAS EN ZIGZAG (NODOS)
-- ============================================================================

-- ----------------------------------------------------------------------
-- Paradas y Contenidos: MAT-1P-INICIO (Matemática: Primeros Números y Formas en Ayacucho)
-- ----------------------------------------------------------------------
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000001010001', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000001010002', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000001010003', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000001010004', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000001010005', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000001010006', 'RETO', '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('fa000000-0000-4000-8000-000001010001', 'ac000000-0000-4000-8000-000001010001', 1, 'Contando frutos andinos del 1 al 5', 8, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000001010002', 'ac000000-0000-4000-8000-000001010002', 1, 'Descubriendo círculos y cuadrados', 8, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000001010003', 'ac000000-0000-4000-8000-000001010003', 1, '¿Arriba o abajo del puente?', 8, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000001010004', 'ac000000-0000-4000-8000-000001010004', 1, 'Contamos hasta el 10 con deditos y piedras', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000001010005', 'ac000000-0000-4000-8000-000001010005', 1, 'Patrones con flores rojas y amarillas', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000001010006', 'ac000000-0000-4000-8000-000001010006', 1, 'El gran reto del conteo en la huerta', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ca000000-0000-4000-8000-000001010001', 'b0000000-0000-4000-8000-000000000101', 'fa000000-0000-4000-8000-000001010001', 'd2000000-0000-4000-8000-000000000101', 1, FALSE, 50, 18),
  ('ca000000-0000-4000-8000-000001010002', 'b0000000-0000-4000-8000-000000000101', 'fa000000-0000-4000-8000-000001010002', 'd2000000-0000-4000-8000-000000000101', 2, FALSE, 27, 32),
  ('ca000000-0000-4000-8000-000001010003', 'b0000000-0000-4000-8000-000000000101', 'fa000000-0000-4000-8000-000001010003', 'd2000000-0000-4000-8000-000000000101', 3, FALSE, 73, 46),
  ('ca000000-0000-4000-8000-000001010004', 'b0000000-0000-4000-8000-000000000101', 'fa000000-0000-4000-8000-000001010004', 'd2000000-0000-4000-8000-000000000101', 4, FALSE, 35, 60),
  ('ca000000-0000-4000-8000-000001010005', 'b0000000-0000-4000-8000-000000000101', 'fa000000-0000-4000-8000-000001010005', 'd2000000-0000-4000-8000-000000000101', 5, FALSE, 68, 74),
  ('ca000000-0000-4000-8000-000001010006', 'b0000000-0000-4000-8000-000000000101', 'fa000000-0000-4000-8000-000001010006', 'd2000000-0000-4000-8000-000000000101', 6, FALSE, 50, 88)
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id), mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo) VALUES
  ('bb000000-0000-4000-8000-000001010001', 'fa000000-0000-4000-8000-000001010001', 1, 'TEXTO', 'En una manta tendida hay 1 choclo, 2 papas amarillas y 3 manzanas. Cuenta señalando con tu dedito cada alimento.'),
  ('bb000000-0000-4000-8000-000001010002', 'fa000000-0000-4000-8000-000001010002', 1, 'TEXTO', 'Una moneda de un sol es redonda como una rueda. Una ventana de adobe es cuadrada como una cajita. Busca en tu aula figuras iguales.'),
  ('bb000000-0000-4000-8000-000001010003', 'fa000000-0000-4000-8000-000001010003', 1, 'TEXTO', 'Rupi vuela arriba en el árbol y el perrito duerme abajo sobre el pasto. Levanta las manos arriba y tócalas abajo en tus pies.'),
  ('bb000000-0000-4000-8000-000001010004', 'fa000000-0000-4000-8000-000001010004', 1, 'TEXTO', 'Nuestras dos manos juntas tienen 10 dedos. Cuenta del uno al diez: ¡uno, dos, tres, cuatro, cinco, seis, siete, ocho, nueve y diez!'),
  ('bb000000-0000-4000-8000-000001010005', 'fa000000-0000-4000-8000-000001010005', 1, 'TEXTO', 'Mira el collar de cuentas: roja, amarilla, roja, amarilla... ¿Qué color le sigue ahora? ¡Muy bien, la cuenta roja!'),
  ('bb000000-0000-4000-8000-000001010006', 'fa000000-0000-4000-8000-000001010006', 1, 'TEXTO', 'Has aprendido a contar, comparar y reconocer formas como un gran explorador. ¡Felicitaciones por culminar tu aventura de 1.°!')
ON DUPLICATE KEY UPDATE texto_cuerpo = VALUES(texto_cuerpo);

INSERT INTO evaluacion_pregunta (id, version_actividad_id, numero_secuencia, tipo, enunciado, explicacion, puntos) VALUES
  ('e2000000-0000-4000-8000-000001010001', 'fa000000-0000-4000-8000-000001010001', 1, 'OPCION_MULTIPLE', 'Si tienes 2 papitas y mamita te da 1 más, ¿cuántas papitas tienes en total?', 'Contamos: 2 papitas más 1 papita nos da 3 papitas en total.', 10),
  ('e2000000-0000-4000-8000-000001010002', 'fa000000-0000-4000-8000-000001010002', 1, 'OPCION_MULTIPLE', '¿Qué forma tiene una moneda de un nuevo sol?', 'La moneda tiene borde circular, por eso es redonda como un círculo.', 10),
  ('e2000000-0000-4000-8000-000001010003', 'fa000000-0000-4000-8000-000001010003', 1, 'OPCION_MULTIPLE', '¿Dónde está Rupi cuando se posa en la copa del árbol?', 'Rupi vuela hacia lo alto, ubicándose arriba de los árboles.', 10),
  ('e2000000-0000-4000-8000-000001010004', 'fa000000-0000-4000-8000-000001010004', 1, 'OPCION_MULTIPLE', '¿Cuántos dedos tenemos en nuestras dos manos juntas?', 'Cada mano tiene 5 dedos; 5 + 5 nos da 10 dedos.', 10),
  ('e2000000-0000-4000-8000-000001010005', 'fa000000-0000-4000-8000-000001010005', 1, 'OPCION_MULTIPLE', 'Si el collar va rojo, amarillo, rojo... ¿qué cuenta sigue después?', 'Sigue el color amarillo para continuar el patrón alternado.', 10),
  ('e2000000-0000-4000-8000-000001010006', 'fa000000-0000-4000-8000-000001010006', 1, 'OPCION_MULTIPLE', 'Si en una canasta hay 4 choclos y agregas 2 choclos más, ¿cuántos hay?', 'Sumamos 4 más 2: 4... 5, 6 choclos en total.', 10)
ON DUPLICATE KEY UPDATE enunciado = VALUES(enunciado), explicacion = VALUES(explicacion);

INSERT INTO evaluacion_opcion_respuesta (id, pregunta_id, numero_secuencia, texto, es_correcta) VALUES
  ('f2000000-0000-4000-8000-000000101011', 'e2000000-0000-4000-8000-000001010001', 1, '3 papitas', TRUE),
  ('f2000000-0000-4000-8000-000000101012', 'e2000000-0000-4000-8000-000001010001', 2, '1 papita', FALSE),
  ('f2000000-0000-4000-8000-000000101013', 'e2000000-0000-4000-8000-000001010001', 3, '5 papitas', FALSE),
  ('f2000000-0000-4000-8000-000000101021', 'e2000000-0000-4000-8000-000001010002', 1, 'Círculo redondo', TRUE),
  ('f2000000-0000-4000-8000-000000101022', 'e2000000-0000-4000-8000-000001010002', 2, 'Triángulo con 3 puntas', FALSE),
  ('f2000000-0000-4000-8000-000000101023', 'e2000000-0000-4000-8000-000001010002', 3, 'Cuadrado recto', FALSE),
  ('f2000000-0000-4000-8000-000000101031', 'e2000000-0000-4000-8000-000001010003', 1, 'Arriba en las ramas', TRUE),
  ('f2000000-0000-4000-8000-000000101032', 'e2000000-0000-4000-8000-000001010003', 2, 'Abajo en el suelo', FALSE),
  ('f2000000-0000-4000-8000-000000101033', 'e2000000-0000-4000-8000-000001010003', 3, 'Dentro del agua', FALSE),
  ('f2000000-0000-4000-8000-000000101041', 'e2000000-0000-4000-8000-000001010004', 1, '10 dedos', TRUE),
  ('f2000000-0000-4000-8000-000000101042', 'e2000000-0000-4000-8000-000001010004', 2, '7 dedos', FALSE),
  ('f2000000-0000-4000-8000-000000101043', 'e2000000-0000-4000-8000-000001010004', 3, '12 dedos', FALSE),
  ('f2000000-0000-4000-8000-000000101051', 'e2000000-0000-4000-8000-000001010005', 1, 'Amarillo', TRUE),
  ('f2000000-0000-4000-8000-000000101052', 'e2000000-0000-4000-8000-000001010005', 2, 'Azul oscuro', FALSE),
  ('f2000000-0000-4000-8000-000000101053', 'e2000000-0000-4000-8000-000001010005', 3, 'Verde pasto', FALSE),
  ('f2000000-0000-4000-8000-000000101061', 'e2000000-0000-4000-8000-000001010006', 1, '6 choclos', TRUE),
  ('f2000000-0000-4000-8000-000000101062', 'e2000000-0000-4000-8000-000001010006', 2, '4 choclos', FALSE),
  ('f2000000-0000-4000-8000-000000101063', 'e2000000-0000-4000-8000-000001010006', 3, '8 choclos', FALSE)
ON DUPLICATE KEY UPDATE texto = VALUES(texto), es_correcta = VALUES(es_correcta);

-- ----------------------------------------------------------------------
-- Paradas y Contenidos: COM-1P-LETRAS (Comunicación: Vocales, Sonidos y Rimas con Rupi)
-- ----------------------------------------------------------------------
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000001020001', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000001020002', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000001020003', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000001020004', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000001020005', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000001020006', 'RETO', '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('fa000000-0000-4000-8000-000001020001', 'ac000000-0000-4000-8000-000001020001', 1, 'La vocal A del Árbol y el Águila', 8, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000001020002', 'ac000000-0000-4000-8000-000001020002', 1, 'La vocal E de Estrella y Erizo', 8, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000001020003', 'ac000000-0000-4000-8000-000001020003', 1, 'Descubriendo las vocales I, O, U', 8, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000001020004', 'ac000000-0000-4000-8000-000001020004', 1, 'Rimas divertidas con animales del campo', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000001020005', 'ac000000-0000-4000-8000-000001020005', 1, 'Palabras mágicas: Mamá y Papá', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000001020006', 'ac000000-0000-4000-8000-000001020006', 1, 'Mi primer pregón escolar rimado', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ca000000-0000-4000-8000-000001020001', 'b0000000-0000-4000-8000-000000000102', 'fa000000-0000-4000-8000-000001020001', 'd2000000-0000-4000-8000-000000000102', 1, FALSE, 50, 18),
  ('ca000000-0000-4000-8000-000001020002', 'b0000000-0000-4000-8000-000000000102', 'fa000000-0000-4000-8000-000001020002', 'd2000000-0000-4000-8000-000000000102', 2, FALSE, 27, 32),
  ('ca000000-0000-4000-8000-000001020003', 'b0000000-0000-4000-8000-000000000102', 'fa000000-0000-4000-8000-000001020003', 'd2000000-0000-4000-8000-000000000102', 3, FALSE, 73, 46),
  ('ca000000-0000-4000-8000-000001020004', 'b0000000-0000-4000-8000-000000000102', 'fa000000-0000-4000-8000-000001020004', 'd2000000-0000-4000-8000-000000000102', 4, FALSE, 35, 60),
  ('ca000000-0000-4000-8000-000001020005', 'b0000000-0000-4000-8000-000000000102', 'fa000000-0000-4000-8000-000001020005', 'd2000000-0000-4000-8000-000000000102', 5, FALSE, 68, 74),
  ('ca000000-0000-4000-8000-000001020006', 'b0000000-0000-4000-8000-000000000102', 'fa000000-0000-4000-8000-000001020006', 'd2000000-0000-4000-8000-000000000102', 6, FALSE, 50, 88)
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id), mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo) VALUES
  ('bb000000-0000-4000-8000-000001020001', 'fa000000-0000-4000-8000-000001020001', 1, 'TEXTO', 'Abre bien la boca para pronunciar la A. La palabra Árbol, Alita y Ayacucho empiezan con la hermosa letra A.'),
  ('bb000000-0000-4000-8000-000001020002', 'fa000000-0000-4000-8000-000001020002', 1, 'TEXTO', 'Sonríe suavemente al decir E. Mira la Estrella que brilla en la noche andina y el Escalón del aula.'),
  ('bb000000-0000-4000-8000-000001020003', 'fa000000-0000-4000-8000-000001020003', 1, 'TEXTO', 'La I es delgada como una plumita. La O es redonda como el sol. La U tiene brazos abiertos como una sonrisa.'),
  ('bb000000-0000-4000-8000-000001020004', 'fa000000-0000-4000-8000-000001020004', 1, 'TEXTO', 'El gato zapato baila contento. El ratón Ramón come un melón. Las palabras que riman terminan con el mismo sonido.'),
  ('bb000000-0000-4000-8000-000001020005', 'fa000000-0000-4000-8000-000001020005', 1, 'TEXTO', 'Con la letra M decimos Mamá y con la letra P decimos Papá. Son las primeras palabras llenas de amor y ternura.'),
  ('bb000000-0000-4000-8000-000001020006', 'fa000000-0000-4000-8000-000001020006', 1, 'TEXTO', '¡Lo lograste! Ahora reconoces sonidos y letras con claridad. Comparte una rima con tus amigos de aula.')
ON DUPLICATE KEY UPDATE texto_cuerpo = VALUES(texto_cuerpo);

INSERT INTO evaluacion_pregunta (id, version_actividad_id, numero_secuencia, tipo, enunciado, explicacion, puntos) VALUES
  ('e2000000-0000-4000-8000-000001020001', 'fa000000-0000-4000-8000-000001020001', 1, 'OPCION_MULTIPLE', '¿Qué palabra empieza con el sonido de la letra A?', 'Árbol empieza con el sonido inicial A.', 10),
  ('e2000000-0000-4000-8000-000001020002', 'fa000000-0000-4000-8000-000001020002', 1, 'OPCION_MULTIPLE', '¿Con qué letra empieza la palabra Estrella?', 'Estrella inicia con la vocal E.', 10),
  ('e2000000-0000-4000-8000-000001020003', 'fa000000-0000-4000-8000-000001020003', 1, 'OPCION_MULTIPLE', '¿Qué dibujo representa la vocal O redondita?', 'El sol o el ojo son figuras circulares que se asocian a la letra O.', 10),
  ('e2000000-0000-4000-8000-000001020004', 'fa000000-0000-4000-8000-000001020004', 1, 'OPCION_MULTIPLE', '¿Qué palabra rima con la palabra "cuy"?', '"Muy" termina con el mismo sonido que "cuy".', 10),
  ('e2000000-0000-4000-8000-000001020005', 'fa000000-0000-4000-8000-000001020005', 1, 'OPCION_MULTIPLE', '¿Con qué letra inicia la palabra Mamá?', 'Mamá empieza con la letra M.', 10),
  ('e2000000-0000-4000-8000-000001020006', 'fa000000-0000-4000-8000-000001020006', 1, 'OPCION_MULTIPLE', '¿Qué sonido final comparten "rana" y "campana"?', 'Ambas terminan en el sonido "-ana".', 10)
ON DUPLICATE KEY UPDATE enunciado = VALUES(enunciado), explicacion = VALUES(explicacion);

INSERT INTO evaluacion_opcion_respuesta (id, pregunta_id, numero_secuencia, texto, es_correcta) VALUES
  ('f2000000-0000-4000-8000-000000102011', 'e2000000-0000-4000-8000-000001020001', 1, 'Árbol', TRUE),
  ('f2000000-0000-4000-8000-000000102012', 'e2000000-0000-4000-8000-000001020001', 2, 'Oso', FALSE),
  ('f2000000-0000-4000-8000-000000102013', 'e2000000-0000-4000-8000-000001020001', 3, 'Uva', FALSE),
  ('f2000000-0000-4000-8000-000000102021', 'e2000000-0000-4000-8000-000001020002', 1, 'Con la letra E', TRUE),
  ('f2000000-0000-4000-8000-000000102022', 'e2000000-0000-4000-8000-000001020002', 2, 'Con la letra I', FALSE),
  ('f2000000-0000-4000-8000-000000102023', 'e2000000-0000-4000-8000-000001020002', 3, 'Con la letra O', FALSE),
  ('f2000000-0000-4000-8000-000000102031', 'e2000000-0000-4000-8000-000001020003', 1, 'Ojo / Oso', TRUE),
  ('f2000000-0000-4000-8000-000000102032', 'e2000000-0000-4000-8000-000001020003', 2, 'Iguana', FALSE),
  ('f2000000-0000-4000-8000-000000102033', 'e2000000-0000-4000-8000-000001020003', 3, 'Uva', FALSE),
  ('f2000000-0000-4000-8000-000000102041', 'e2000000-0000-4000-8000-000001020004', 1, 'Muy', TRUE),
  ('f2000000-0000-4000-8000-000000102042', 'e2000000-0000-4000-8000-000001020004', 2, 'Sol', FALSE),
  ('f2000000-0000-4000-8000-000000102043', 'e2000000-0000-4000-8000-000001020004', 3, 'Flor', FALSE),
  ('f2000000-0000-4000-8000-000000102051', 'e2000000-0000-4000-8000-000001020005', 1, 'M', TRUE),
  ('f2000000-0000-4000-8000-000000102052', 'e2000000-0000-4000-8000-000001020005', 2, 'S', FALSE),
  ('f2000000-0000-4000-8000-000000102053', 'e2000000-0000-4000-8000-000001020005', 3, 'L', FALSE),
  ('f2000000-0000-4000-8000-000000102061', 'e2000000-0000-4000-8000-000001020006', 1, 'Terminan en "-ana"', TRUE),
  ('f2000000-0000-4000-8000-000000102062', 'e2000000-0000-4000-8000-000001020006', 2, 'Terminan en "-or"', FALSE),
  ('f2000000-0000-4000-8000-000000102063', 'e2000000-0000-4000-8000-000001020006', 3, 'Terminan en "-uz"', FALSE)
ON DUPLICATE KEY UPDATE texto = VALUES(texto), es_correcta = VALUES(es_correcta);

-- ----------------------------------------------------------------------
-- Paradas y Contenidos: CYT-1P-EXPLOR (Ciencia y Tecnología: Los Sentidos y el Entorno Natural)
-- ----------------------------------------------------------------------
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000001030001', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000001030002', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000001030003', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000001030004', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000001030005', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000001030006', 'RETO', '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('fa000000-0000-4000-8000-000001030001', 'ac000000-0000-4000-8000-000001030001', 1, 'Mis ojos: La vista y los colores', 8, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000001030002', 'ac000000-0000-4000-8000-000001030002', 1, 'Mis oídos: Escuchando el viento y el río', 8, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000001030003', 'ac000000-0000-4000-8000-000001030003', 1, 'Mi nariz y lengua: Aromas y sabores', 8, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000001030004', 'ac000000-0000-4000-8000-000001030004', 1, 'Mis manos: Tocando lo suave y lo áspero', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000001030005', 'ac000000-0000-4000-8000-000001030005', 1, 'Cuidando las plantas de nuestro biohuerto', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000001030006', 'ac000000-0000-4000-8000-000001030006', 1, 'Pequeño detective del ambiente escolar', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ca000000-0000-4000-8000-000001030001', 'b0000000-0000-4000-8000-000000000103', 'fa000000-0000-4000-8000-000001030001', 'd2000000-0000-4000-8000-000000000103', 1, FALSE, 50, 18),
  ('ca000000-0000-4000-8000-000001030002', 'b0000000-0000-4000-8000-000000000103', 'fa000000-0000-4000-8000-000001030002', 'd2000000-0000-4000-8000-000000000103', 2, FALSE, 27, 32),
  ('ca000000-0000-4000-8000-000001030003', 'b0000000-0000-4000-8000-000000000103', 'fa000000-0000-4000-8000-000001030003', 'd2000000-0000-4000-8000-000000000103', 3, FALSE, 73, 46),
  ('ca000000-0000-4000-8000-000001030004', 'b0000000-0000-4000-8000-000000000103', 'fa000000-0000-4000-8000-000001030004', 'd2000000-0000-4000-8000-000000000103', 4, FALSE, 35, 60),
  ('ca000000-0000-4000-8000-000001030005', 'b0000000-0000-4000-8000-000000000103', 'fa000000-0000-4000-8000-000001030005', 'd2000000-0000-4000-8000-000000000103', 5, FALSE, 68, 74),
  ('ca000000-0000-4000-8000-000001030006', 'b0000000-0000-4000-8000-000000000103', 'fa000000-0000-4000-8000-000001030006', 'd2000000-0000-4000-8000-000000000103', 6, FALSE, 50, 88)
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id), mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo) VALUES
  ('bb000000-0000-4000-8000-000001030001', 'fa000000-0000-4000-8000-000001030001', 1, 'TEXTO', 'Con nuestros ojos podemos ver el cielo azul, las plumas rojas de Rupi y el camino hacia la escuela.'),
  ('bb000000-0000-4000-8000-000001030002', 'fa000000-0000-4000-8000-000001030002', 1, 'TEXTO', 'Con los oídos escuchamos la campana del recreo, el trino de las aves y las risas de nuestros compañeros.'),
  ('bb000000-0000-4000-8000-000001030003', 'fa000000-0000-4000-8000-000001030003', 1, 'TEXTO', 'El olfato nos avisa cuando la comida de mamita está lista. El gusto en la lengua siente lo dulce del durazno.'),
  ('bb000000-0000-4000-8000-000001030004', 'fa000000-0000-4000-8000-000001030004', 1, 'TEXTO', 'Acariciar un conejito se siente muy suave. Tocar una piedra de río se siente dura y fría. El tacto nos protege.'),
  ('bb000000-0000-4000-8000-000001030005', 'fa000000-0000-4000-8000-000001030005', 1, 'TEXTO', 'Las plantas necesitan agua, tierra fértil y luz del sol para crecer sanas y regalarnos flores y frutos.'),
  ('bb000000-0000-4000-8000-000001030006', 'fa000000-0000-4000-8000-000001030006', 1, 'TEXTO', '¡Eres un gran explorador de la ciencia! Recuerda lavarte las manos con agua y jabón antes de comer.')
ON DUPLICATE KEY UPDATE texto_cuerpo = VALUES(texto_cuerpo);

INSERT INTO evaluacion_pregunta (id, version_actividad_id, numero_secuencia, tipo, enunciado, explicacion, puntos) VALUES
  ('e2000000-0000-4000-8000-000001030001', 'fa000000-0000-4000-8000-000001030001', 1, 'OPCION_MULTIPLE', '¿Qué sentido usamos para mirar los colores del arcoíris?', 'Usamos la vista mediante nuestros ojos.', 10),
  ('e2000000-0000-4000-8000-000001030002', 'fa000000-0000-4000-8000-000001030002', 1, 'OPCION_MULTIPLE', '¿Con qué parte de nuestro cuerpo escuchamos la música?', 'El sentido del oído reside en las orejas/oídos.', 10),
  ('e2000000-0000-4000-8000-000001030003', 'fa000000-0000-4000-8000-000001030003', 1, 'OPCION_MULTIPLE', '¿Qué sabor sientes cuando pruebas una rodaja de limón?', 'El limón tiene un sabor ácido característico.', 10),
  ('e2000000-0000-4000-8000-000001030004', 'fa000000-0000-4000-8000-000001030004', 1, 'OPCION_MULTIPLE', '¿Cómo se siente la lana de una oveja al tocarla?', 'La lana de oveja es suave y abrigadora.', 10),
  ('e2000000-0000-4000-8000-000001030005', 'fa000000-0000-4000-8000-000001030005', 1, 'OPCION_MULTIPLE', '¿Qué elemento natural le da luz y calor a las plantas para crecer?', 'El sol brinda energía vital para la fotosíntesis.', 10),
  ('e2000000-0000-4000-8000-000001030006', 'fa000000-0000-4000-8000-000001030006', 1, 'OPCION_MULTIPLE', '¿Por qué debemos lavarnos las manos antes de comer?', 'Para eliminar gérmenes y cuidar nuestra salud.', 10)
ON DUPLICATE KEY UPDATE enunciado = VALUES(enunciado), explicacion = VALUES(explicacion);

INSERT INTO evaluacion_opcion_respuesta (id, pregunta_id, numero_secuencia, texto, es_correcta) VALUES
  ('f2000000-0000-4000-8000-000000103011', 'e2000000-0000-4000-8000-000001030001', 1, 'La vista', TRUE),
  ('f2000000-0000-4000-8000-000000103012', 'e2000000-0000-4000-8000-000001030001', 2, 'El oído', FALSE),
  ('f2000000-0000-4000-8000-000000103013', 'e2000000-0000-4000-8000-000001030001', 3, 'El tacto', FALSE),
  ('f2000000-0000-4000-8000-000000103021', 'e2000000-0000-4000-8000-000001030002', 1, 'Con los oídos', TRUE),
  ('f2000000-0000-4000-8000-000000103022', 'e2000000-0000-4000-8000-000001030002', 2, 'Con los pies', FALSE),
  ('f2000000-0000-4000-8000-000000103023', 'e2000000-0000-4000-8000-000001030002', 3, 'Con las manos', FALSE),
  ('f2000000-0000-4000-8000-000000103031', 'e2000000-0000-4000-8000-000001030003', 1, 'Ácido', TRUE),
  ('f2000000-0000-4000-8000-000000103032', 'e2000000-0000-4000-8000-000001030003', 2, 'Dulce como miel', FALSE),
  ('f2000000-0000-4000-8000-000000103033', 'e2000000-0000-4000-8000-000001030003', 3, 'Salado como sopa', FALSE),
  ('f2000000-0000-4000-8000-000000103041', 'e2000000-0000-4000-8000-000001030004', 1, 'Suave y abrigadora', TRUE),
  ('f2000000-0000-4000-8000-000000103042', 'e2000000-0000-4000-8000-000001030004', 2, 'Áspera como lija', FALSE),
  ('f2000000-0000-4000-8000-000000103043', 'e2000000-0000-4000-8000-000001030004', 3, 'Líquida como agua', FALSE),
  ('f2000000-0000-4000-8000-000000103051', 'e2000000-0000-4000-8000-000001030005', 1, 'El sol', TRUE),
  ('f2000000-0000-4000-8000-000000103052', 'e2000000-0000-4000-8000-000001030005', 2, 'La oscuridad', FALSE),
  ('f2000000-0000-4000-8000-000000103053', 'e2000000-0000-4000-8000-000001030005', 3, 'El plástico', FALSE),
  ('f2000000-0000-4000-8000-000000103061', 'e2000000-0000-4000-8000-000001030006', 1, 'Para eliminar microbios y no enfermarnos', TRUE),
  ('f2000000-0000-4000-8000-000000103062', 'e2000000-0000-4000-8000-000001030006', 2, 'Para pintar la mesa', FALSE),
  ('f2000000-0000-4000-8000-000000103063', 'e2000000-0000-4000-8000-000001030006', 3, 'Para enfriar las manos', FALSE)
ON DUPLICATE KEY UPDATE texto = VALUES(texto), es_correcta = VALUES(es_correcta);

-- ----------------------------------------------------------------------
-- Paradas y Contenidos: COM-3P-MITOS (Comunicación: Mitos y Narraciones de los Andes)
-- ----------------------------------------------------------------------
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000003020001', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000003020002', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000003020003', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000003020004', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000003020005', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000003020006', 'RETO', '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('fa000000-0000-4000-8000-000003020001', 'ac000000-0000-4000-8000-000003020001', 1, 'El mito de los Hermanos Ayar', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000003020002', 'ac000000-0000-4000-8000-000003020002', 1, 'Identificando la idea principal en una narración', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000003020003', 'ac000000-0000-4000-8000-000003020003', 1, 'Palabras sinónimas y antónimas en la lectura', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000003020004', 'ac000000-0000-4000-8000-000003020004', 1, 'Estructura narrativa: Inicio, nudo y desenlace', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000003020005', 'ac000000-0000-4000-8000-000003020005', 1, 'Uso del punto seguido, aparte y final', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000003020006', 'ac000000-0000-4000-8000-000003020006', 1, 'Redactando una fábula con moraleja comunitaria', 15, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ca000000-0000-4000-8000-000003020001', 'b0000000-0000-4000-8000-000000000302', 'fa000000-0000-4000-8000-000003020001', 'd2000000-0000-4000-8000-000000000302', 1, FALSE, 50, 18),
  ('ca000000-0000-4000-8000-000003020002', 'b0000000-0000-4000-8000-000000000302', 'fa000000-0000-4000-8000-000003020002', 'd2000000-0000-4000-8000-000000000302', 2, FALSE, 27, 32),
  ('ca000000-0000-4000-8000-000003020003', 'b0000000-0000-4000-8000-000000000302', 'fa000000-0000-4000-8000-000003020003', 'd2000000-0000-4000-8000-000000000302', 3, FALSE, 73, 46),
  ('ca000000-0000-4000-8000-000003020004', 'b0000000-0000-4000-8000-000000000302', 'fa000000-0000-4000-8000-000003020004', 'd2000000-0000-4000-8000-000000000302', 4, FALSE, 35, 60),
  ('ca000000-0000-4000-8000-000003020005', 'b0000000-0000-4000-8000-000000000302', 'fa000000-0000-4000-8000-000003020005', 'd2000000-0000-4000-8000-000000000302', 5, FALSE, 68, 74),
  ('ca000000-0000-4000-8000-000003020006', 'b0000000-0000-4000-8000-000000000302', 'fa000000-0000-4000-8000-000003020006', 'd2000000-0000-4000-8000-000000000302', 6, FALSE, 50, 88)
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id), mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo) VALUES
  ('bb000000-0000-4000-8000-000003020001', 'fa000000-0000-4000-8000-000003020001', 1, 'TEXTO', 'Cuatro hermanos salieron del cerro Tamputoco en busca de tierras fértiles. Cada uno portaba dones especiales para fundar un pueblo próspero.'),
  ('bb000000-0000-4000-8000-000003020002', 'fa000000-0000-4000-8000-000003020002', 1, 'TEXTO', 'La idea principal resume lo más importante que el autor quiere comunicar en el párrafo o texto.'),
  ('bb000000-0000-4000-8000-000003020003', 'fa000000-0000-4000-8000-000003020003', 1, 'TEXTO', 'Los sinónimos tienen significados parecidos (alegre = contento). Los antónimos expresan significados opuestos (valiente ≠ miedoso).'),
  ('bb000000-0000-4000-8000-000003020004', 'fa000000-0000-4000-8000-000003020004', 1, 'TEXTO', 'Toda narración presenta una situación inicial, un problema o nudo emocionante y una solución o desenlace final.'),
  ('bb000000-0000-4000-8000-000003020005', 'fa000000-0000-4000-8000-000003020005', 1, 'TEXTO', 'El punto seguido separa oraciones de una misma idea. El punto aparte cambia de párrafo. El punto final concluye el texto.'),
  ('bb000000-0000-4000-8000-000003020006', 'fa000000-0000-4000-8000-000003020006', 1, 'TEXTO', '¡Excelente redactor! Has combinado la imaginación andina con una escritura clara y ordenada para beneficio de tu aula.')
ON DUPLICATE KEY UPDATE texto_cuerpo = VALUES(texto_cuerpo);

INSERT INTO evaluacion_pregunta (id, version_actividad_id, numero_secuencia, tipo, enunciado, explicacion, puntos) VALUES
  ('e2000000-0000-4000-8000-000003020001', 'fa000000-0000-4000-8000-000003020001', 1, 'OPCION_MULTIPLE', '¿De qué lugar salieron los Hermanos Ayar según la leyenda?', 'El mito narra que surgieron de las ventanas del cerro Tamputoco.', 10),
  ('e2000000-0000-4000-8000-000003020002', 'fa000000-0000-4000-8000-000003020002', 1, 'OPCION_MULTIPLE', '¿Para qué sirve identificar la idea principal de un texto?', 'Nos permite comprender el mensaje esencial sin perdernos en los detalles.', 10),
  ('e2000000-0000-4000-8000-000003020003', 'fa000000-0000-4000-8000-000003020003', 1, 'OPCION_MULTIPLE', '¿Cuál es el sinónimo de la palabra "sabio"?', 'Una persona sabia es una persona culta o prudente.', 10),
  ('e2000000-0000-4000-8000-000003020004', 'fa000000-0000-4000-8000-000003020004', 1, 'OPCION_MULTIPLE', '¿En qué parte de un cuento se presenta el conflicto principal?', 'El conflicto o problema central ocurre en el nudo de la historia.', 10),
  ('e2000000-0000-4000-8000-000003020005', 'fa000000-0000-4000-8000-000003020005', 1, 'OPCION_MULTIPLE', '¿Qué signo ortográfico indica el cierre total de una lectura?', 'El punto final marca la conclusión de la lectura o escrito.', 10),
  ('e2000000-0000-4000-8000-000003020006', 'fa000000-0000-4000-8000-000003020006', 1, 'OPCION_MULTIPLE', '¿Qué elemento distingue a una fábula de otros cuentos?', 'La fábula contiene siempre una enseñanza ética llamada moraleja.', 10)
ON DUPLICATE KEY UPDATE enunciado = VALUES(enunciado), explicacion = VALUES(explicacion);

INSERT INTO evaluacion_opcion_respuesta (id, pregunta_id, numero_secuencia, texto, es_correcta) VALUES
  ('f2000000-0000-4000-8000-000000302011', 'e2000000-0000-4000-8000-000003020001', 1, 'Del cerro Tamputoco', TRUE),
  ('f2000000-0000-4000-8000-000000302012', 'e2000000-0000-4000-8000-000003020001', 2, 'Del fondo del mar', FALSE),
  ('f2000000-0000-4000-8000-000000302013', 'e2000000-0000-4000-8000-000003020001', 3, 'De una nube del cielo', FALSE),
  ('f2000000-0000-4000-8000-000000302021', 'e2000000-0000-4000-8000-000003020002', 1, 'Para entender el mensaje esencial del autor', TRUE),
  ('f2000000-0000-4000-8000-000000302022', 'e2000000-0000-4000-8000-000003020002', 2, 'Para memorizar las fechas', FALSE),
  ('f2000000-0000-4000-8000-000000302023', 'e2000000-0000-4000-8000-000003020002', 3, 'Para contar las palabras', FALSE),
  ('f2000000-0000-4000-8000-000000302031', 'e2000000-0000-4000-8000-000003020003', 1, 'Prudente / Conocedor', TRUE),
  ('f2000000-0000-4000-8000-000000302032', 'e2000000-0000-4000-8000-000003020003', 2, 'Distraído', FALSE),
  ('f2000000-0000-4000-8000-000000302033', 'e2000000-0000-4000-8000-000003020003', 3, 'Temeroso', FALSE),
  ('f2000000-0000-4000-8000-000000302041', 'e2000000-0000-4000-8000-000003020004', 1, 'En el nudo', TRUE),
  ('f2000000-0000-4000-8000-000000302042', 'e2000000-0000-4000-8000-000003020004', 2, 'En el título', FALSE),
  ('f2000000-0000-4000-8000-000000302043', 'e2000000-0000-4000-8000-000003020004', 3, 'En la despedida', FALSE),
  ('f2000000-0000-4000-8000-000000302051', 'e2000000-0000-4000-8000-000003020005', 1, 'Punto final', TRUE),
  ('f2000000-0000-4000-8000-000000302052', 'e2000000-0000-4000-8000-000003020005', 2, 'Coma explicativa', FALSE),
  ('f2000000-0000-4000-8000-000000302053', 'e2000000-0000-4000-8000-000003020005', 3, 'Punto y coma', FALSE),
  ('f2000000-0000-4000-8000-000000302061', 'e2000000-0000-4000-8000-000003020006', 1, 'Tiene una enseñanza o moraleja', TRUE),
  ('f2000000-0000-4000-8000-000000302062', 'e2000000-0000-4000-8000-000003020006', 2, 'Solo habla de robots', FALSE),
  ('f2000000-0000-4000-8000-000000302063', 'e2000000-0000-4000-8000-000003020006', 3, 'No tiene personajes', FALSE)
ON DUPLICATE KEY UPDATE texto = VALUES(texto), es_correcta = VALUES(es_correcta);

-- ----------------------------------------------------------------------
-- Paradas y Contenidos: CYT-3P-ENERGIA (Ciencia y Tecnología: La Fuerza del Sol, el Viento y la Materia)
-- ----------------------------------------------------------------------
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000003030001', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000003030002', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000003030003', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000003030004', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000003030005', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000003030006', 'RETO', '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('fa000000-0000-4000-8000-000003030001', 'ac000000-0000-4000-8000-000003030001', 1, 'Los tres estados del agua en los Andes', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000003030002', 'ac000000-0000-4000-8000-000003030002', 1, 'Fuerza de gravedad: ¿Por qué caen las cosas?', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000003030003', 'ac000000-0000-4000-8000-000003030003', 1, 'Materiales conductores y aislantes del calor', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000003030004', 'ac000000-0000-4000-8000-000003030004', 1, 'La luz solar y las sombras en el reloj andino', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000003030005', 'ac000000-0000-4000-8000-000003030005', 1, 'El ciclo del agua y el cuidado de los manantiales', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000003030006', 'ac000000-0000-4000-8000-000003030006', 1, 'Proyecto escolar: Cocina solar con cartón y espejo', 15, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ca000000-0000-4000-8000-000003030001', 'b0000000-0000-4000-8000-000000000303', 'fa000000-0000-4000-8000-000003030001', 'd2000000-0000-4000-8000-000000000303', 1, FALSE, 50, 18),
  ('ca000000-0000-4000-8000-000003030002', 'b0000000-0000-4000-8000-000000000303', 'fa000000-0000-4000-8000-000003030002', 'd2000000-0000-4000-8000-000000000303', 2, FALSE, 27, 32),
  ('ca000000-0000-4000-8000-000003030003', 'b0000000-0000-4000-8000-000000000303', 'fa000000-0000-4000-8000-000003030003', 'd2000000-0000-4000-8000-000000000303', 3, FALSE, 73, 46),
  ('ca000000-0000-4000-8000-000003030004', 'b0000000-0000-4000-8000-000000000303', 'fa000000-0000-4000-8000-000003030004', 'd2000000-0000-4000-8000-000000000303', 4, FALSE, 35, 60),
  ('ca000000-0000-4000-8000-000003030005', 'b0000000-0000-4000-8000-000000000303', 'fa000000-0000-4000-8000-000003030005', 'd2000000-0000-4000-8000-000000000303', 5, FALSE, 68, 74),
  ('ca000000-0000-4000-8000-000003030006', 'b0000000-0000-4000-8000-000000000303', 'fa000000-0000-4000-8000-000003030006', 'd2000000-0000-4000-8000-000000000303', 6, FALSE, 50, 88)
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id), mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo) VALUES
  ('bb000000-0000-4000-8000-000003030001', 'fa000000-0000-4000-8000-000003030001', 1, 'TEXTO', 'En los glaciares el agua está sólida (hielo). En los arroyos fluye líquida. Con el calor del sol se evapora en gas formando nubes.'),
  ('bb000000-0000-4000-8000-000003030002', 'fa000000-0000-4000-8000-000003030002', 1, 'TEXTO', 'La Tierra ejerce una fuerza que atrae a todos los cuerpos hacia su centro. Por eso, al soltar una manzana, siempre cae hacia el suelo.'),
  ('bb000000-0000-4000-8000-000003030003', 'fa000000-0000-4000-8000-000003030003', 1, 'TEXTO', 'Una cuchara de metal se calienta rápido (conductor). Una cuchara de madera no quema las manos (aislante térmico).'),
  ('bb000000-0000-4000-8000-000003030004', 'fa000000-0000-4000-8000-000003030004', 1, 'TEXTO', 'Cuando la luz viaja en línea recta y choca contra un objeto opaco, se produce una sombra. Los incas medían el tiempo con el Intihuatana.'),
  ('bb000000-0000-4000-8000-000003030005', 'fa000000-0000-4000-8000-000003030005', 1, 'TEXTO', 'Evaporación, condensación, precipitación y filtración forman el ciclo continuo que abastece a nuestros pueblos de agua pura.'),
  ('bb000000-0000-4000-8000-000003030006', 'fa000000-0000-4000-8000-000003030006', 1, 'TEXTO', '¡Felicidades, científico escolar! Has aplicado el método científico para comprender la energía sustentable.')
ON DUPLICATE KEY UPDATE texto_cuerpo = VALUES(texto_cuerpo);

INSERT INTO evaluacion_pregunta (id, version_actividad_id, numero_secuencia, tipo, enunciado, explicacion, puntos) VALUES
  ('e2000000-0000-4000-8000-000003030001', 'fa000000-0000-4000-8000-000003030001', 1, 'OPCION_MULTIPLE', '¿En qué estado físico se encuentra el granizo que cae en la sierra?', 'El granizo es agua en estado sólido.', 10),
  ('e2000000-0000-4000-8000-000003030002', 'fa000000-0000-4000-8000-000003030002', 1, 'OPCION_MULTIPLE', '¿Qué fuerza hace que una piedra caiga al suelo al soltarla?', 'La fuerza de gravedad atrae los objetos hacia la superficie terrestre.', 10),
  ('e2000000-0000-4000-8000-000003030003', 'fa000000-0000-4000-8000-000003030003', 1, 'OPCION_MULTIPLE', '¿Cuál de estos materiales es un buen aislante del calor?', 'La madera conduce mal el calor, protegiendo las manos.', 10),
  ('e2000000-0000-4000-8000-000003030004', 'fa000000-0000-4000-8000-000003030004', 1, 'OPCION_MULTIPLE', '¿Qué se produce cuando un cuerpo opaco bloquea el paso de la luz?', 'Se proyecta una sombra en la superficie opuesta.', 10),
  ('e2000000-0000-4000-8000-000003030005', 'fa000000-0000-4000-8000-000003030005', 1, 'OPCION_MULTIPLE', '¿Cómo se llama la etapa en que el vapor se enfría y forma gotas de lluvia?', 'La condensación forma nubes que luego precipitan en lluvia.', 10),
  ('e2000000-0000-4000-8000-000003030006', 'fa000000-0000-4000-8000-000003030006', 1, 'OPCION_MULTIPLE', '¿Qué energía limpia aprovecha una cocina solar?', 'Aprovecha la radiación y energía térmica del sol.', 10)
ON DUPLICATE KEY UPDATE enunciado = VALUES(enunciado), explicacion = VALUES(explicacion);

INSERT INTO evaluacion_opcion_respuesta (id, pregunta_id, numero_secuencia, texto, es_correcta) VALUES
  ('f2000000-0000-4000-8000-000000303011', 'e2000000-0000-4000-8000-000003030001', 1, 'Sólido', TRUE),
  ('f2000000-0000-4000-8000-000000303012', 'e2000000-0000-4000-8000-000003030001', 2, 'Gaseoso', FALSE),
  ('f2000000-0000-4000-8000-000000303013', 'e2000000-0000-4000-8000-000003030001', 3, 'Líquido tibio', FALSE),
  ('f2000000-0000-4000-8000-000000303021', 'e2000000-0000-4000-8000-000003030002', 1, 'La fuerza de gravedad', TRUE),
  ('f2000000-0000-4000-8000-000000303022', 'e2000000-0000-4000-8000-000003030002', 2, 'La fuerza magnética', FALSE),
  ('f2000000-0000-4000-8000-000000303023', 'e2000000-0000-4000-8000-000003030002', 3, 'La fuerza del viento', FALSE),
  ('f2000000-0000-4000-8000-000000303031', 'e2000000-0000-4000-8000-000003030003', 1, 'La madera', TRUE),
  ('f2000000-0000-4000-8000-000000303032', 'e2000000-0000-4000-8000-000003030003', 2, 'El cobre', FALSE),
  ('f2000000-0000-4000-8000-000000303033', 'e2000000-0000-4000-8000-000003030003', 3, 'El aluminio', FALSE),
  ('f2000000-0000-4000-8000-000000303041', 'e2000000-0000-4000-8000-000003030004', 1, 'Una sombra', TRUE),
  ('f2000000-0000-4000-8000-000000303042', 'e2000000-0000-4000-8000-000003030004', 2, 'Un arcoíris nocturno', FALSE),
  ('f2000000-0000-4000-8000-000000303043', 'e2000000-0000-4000-8000-000003030004', 3, 'Un sonido agudo', FALSE),
  ('f2000000-0000-4000-8000-000000303051', 'e2000000-0000-4000-8000-000003030005', 1, 'Condensación', TRUE),
  ('f2000000-0000-4000-8000-000000303052', 'e2000000-0000-4000-8000-000003030005', 2, 'Congelación total', FALSE),
  ('f2000000-0000-4000-8000-000000303053', 'e2000000-0000-4000-8000-000003030005', 3, 'Combustión', FALSE),
  ('f2000000-0000-4000-8000-000000303061', 'e2000000-0000-4000-8000-000003030006', 1, 'Energía solar', TRUE),
  ('f2000000-0000-4000-8000-000000303062', 'e2000000-0000-4000-8000-000003030006', 2, 'Energía de carbón', FALSE),
  ('f2000000-0000-4000-8000-000000303063', 'e2000000-0000-4000-8000-000003030006', 3, 'Energía nuclear', FALSE)
ON DUPLICATE KEY UPDATE texto = VALUES(texto), es_correcta = VALUES(es_correcta);

-- ----------------------------------------------------------------------
-- Paradas y Contenidos: MAT-4P-FRACC (Matemática: Fracciones y Medidas en las Ferias Andinas)
-- ----------------------------------------------------------------------
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000004010001', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000004010002', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000004010003', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000004010004', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000004010005', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000004010006', 'RETO', '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('fa000000-0000-4000-8000-000004010001', 'ac000000-0000-4000-8000-000004010001', 1, 'Partes iguales: Introducción a las fracciones', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000004010002', 'ac000000-0000-4000-8000-000004010002', 1, 'Numerador y denominador en el reparto de cosechas', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000004010003', 'ac000000-0000-4000-8000-000004010003', 1, 'Fracciones equivalentes con tiras de colores', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000004010004', 'ac000000-0000-4000-8000-000004010004', 1, 'Cálculo de perímetros en canchas y parcelas', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000004010005', 'ac000000-0000-4000-8000-000004010005', 1, 'Área con cuadrículas: Superficie de telares', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000004010006', 'ac000000-0000-4000-8000-000004010006', 1, 'Desafío del comerciante justo en la feria de Quinua', 15, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ca000000-0000-4000-8000-000004010001', 'b0000000-0000-4000-8000-000000000401', 'fa000000-0000-4000-8000-000004010001', 'd2000000-0000-4000-8000-000000000401', 1, FALSE, 50, 18),
  ('ca000000-0000-4000-8000-000004010002', 'b0000000-0000-4000-8000-000000000401', 'fa000000-0000-4000-8000-000004010002', 'd2000000-0000-4000-8000-000000000401', 2, FALSE, 27, 32),
  ('ca000000-0000-4000-8000-000004010003', 'b0000000-0000-4000-8000-000000000401', 'fa000000-0000-4000-8000-000004010003', 'd2000000-0000-4000-8000-000000000401', 3, FALSE, 73, 46),
  ('ca000000-0000-4000-8000-000004010004', 'b0000000-0000-4000-8000-000000000401', 'fa000000-0000-4000-8000-000004010004', 'd2000000-0000-4000-8000-000000000401', 4, FALSE, 35, 60),
  ('ca000000-0000-4000-8000-000004010005', 'b0000000-0000-4000-8000-000000000401', 'fa000000-0000-4000-8000-000004010005', 'd2000000-0000-4000-8000-000000000401', 5, FALSE, 68, 74),
  ('ca000000-0000-4000-8000-000004010006', 'b0000000-0000-4000-8000-000000000401', 'fa000000-0000-4000-8000-000004010006', 'd2000000-0000-4000-8000-000000000401', 6, FALSE, 50, 88)
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id), mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo) VALUES
  ('bb000000-0000-4000-8000-000004010001', 'fa000000-0000-4000-8000-000004010001', 1, 'TEXTO', 'Una fracción expresa una o varias partes iguales en que se divide una unidad entera. Si divides un queso en 4 partes iguales, cada parte es 1/4.'),
  ('bb000000-0000-4000-8000-000004010002', 'fa000000-0000-4000-8000-000004010002', 1, 'TEXTO', 'El denominador indica en cuántas partes iguales se dividió el total. El numerador indica cuántas partes tomamos o comemos.'),
  ('bb000000-0000-4000-8000-000004010003', 'fa000000-0000-4000-8000-000004010003', 1, 'TEXTO', 'Dos fracciones son equivalentes cuando representan la misma cantidad: 1/2 es exactamente igual a 2/4 y a 4/8.'),
  ('bb000000-0000-4000-8000-000004010004', 'fa000000-0000-4000-8000-000004010004', 1, 'TEXTO', 'El perímetro es la suma de las longitudes de todos los lados de una figura geométrica. Un biohuerto de 5m de largo por 3m de ancho tiene: 5+3+5+3 = 16m.'),
  ('bb000000-0000-4000-8000-000004010005', 'fa000000-0000-4000-8000-000004010005', 1, 'TEXTO', 'El área mide el espacio interior de una figura plana. Se calcula contando los cuadraditos de 1 metro cuadrado que cubren la superficie.'),
  ('bb000000-0000-4000-8000-000004010006', 'fa000000-0000-4000-8000-000004010006', 1, 'TEXTO', '¡Misión cumplida! Aplicaste fracciones y geometría para resolver situaciones reales de intercambio solidario.')
ON DUPLICATE KEY UPDATE texto_cuerpo = VALUES(texto_cuerpo);

INSERT INTO evaluacion_pregunta (id, version_actividad_id, numero_secuencia, tipo, enunciado, explicacion, puntos) VALUES
  ('e2000000-0000-4000-8000-000004010001', 'fa000000-0000-4000-8000-000004010001', 1, 'OPCION_MULTIPLE', 'Si repartes un pan andino en 2 partes iguales, ¿cómo se llama cada parte?', 'Cada una de las dos partes iguales corresponde a un medio (1/2).', 10),
  ('e2000000-0000-4000-8000-000004010002', 'fa000000-0000-4000-8000-000004010002', 1, 'OPCION_MULTIPLE', 'En la fracción 3/5, ¿cuál es el denominador?', 'El número inferior (5) es el denominador, indicando 5 partes en total.', 10),
  ('e2000000-0000-4000-8000-000004010003', 'fa000000-0000-4000-8000-000004010003', 1, 'OPCION_MULTIPLE', '¿Cuál de las siguientes fracciones es equivalente a 1/2?', '2/4 simplificado dividiendo entre 2 es igual a 1/2.', 10),
  ('e2000000-0000-4000-8000-000004010004', 'fa000000-0000-4000-8000-000004010004', 1, 'OPCION_MULTIPLE', '¿Cuál es el perímetro de un terreno cuadrado cuyos lados miden 6 metros?', 'Multiplicamos 4 lados por 6 metros: 4 x 6 = 24 metros.', 10),
  ('e2000000-0000-4000-8000-000004010005', 'fa000000-0000-4000-8000-000004010005', 1, 'OPCION_MULTIPLE', 'Si un rectángulo mide 4 unidades de base por 3 de altura, ¿cuál es su área?', 'Área = base x altura = 4 x 3 = 12 unidades cuadradas.', 10),
  ('e2000000-0000-4000-8000-000004010006', 'fa000000-0000-4000-8000-000004010006', 1, 'OPCION_MULTIPLE', 'Si tienes 3/4 de kilo de quinua y compras 1/4 más, ¿cuánto tienes en total?', '3/4 + 1/4 = 4/4, lo que equivale exactamente a 1 kilo completo.', 10)
ON DUPLICATE KEY UPDATE enunciado = VALUES(enunciado), explicacion = VALUES(explicacion);

INSERT INTO evaluacion_opcion_respuesta (id, pregunta_id, numero_secuencia, texto, es_correcta) VALUES
  ('f2000000-0000-4000-8000-000000401011', 'e2000000-0000-4000-8000-000004010001', 1, 'Un medio (1/2)', TRUE),
  ('f2000000-0000-4000-8000-000000401012', 'e2000000-0000-4000-8000-000004010001', 2, 'Un cuarto (1/4)', FALSE),
  ('f2000000-0000-4000-8000-000000401013', 'e2000000-0000-4000-8000-000004010001', 3, 'Tres tercios (3/3)', FALSE),
  ('f2000000-0000-4000-8000-000000401021', 'e2000000-0000-4000-8000-000004010002', 1, 'El número 5', TRUE),
  ('f2000000-0000-4000-8000-000000401022', 'e2000000-0000-4000-8000-000004010002', 2, 'El número 3', FALSE),
  ('f2000000-0000-4000-8000-000000401023', 'e2000000-0000-4000-8000-000004010002', 3, 'El número 8', FALSE),
  ('f2000000-0000-4000-8000-000000401031', 'e2000000-0000-4000-8000-000004010003', 1, '2/4', TRUE),
  ('f2000000-0000-4000-8000-000000401032', 'e2000000-0000-4000-8000-000004010003', 2, '1/3', FALSE),
  ('f2000000-0000-4000-8000-000000401033', 'e2000000-0000-4000-8000-000004010003', 3, '3/4', FALSE),
  ('f2000000-0000-4000-8000-000000401041', 'e2000000-0000-4000-8000-000004010004', 1, '24 metros', TRUE),
  ('f2000000-0000-4000-8000-000000401042', 'e2000000-0000-4000-8000-000004010004', 2, '18 metros', FALSE),
  ('f2000000-0000-4000-8000-000000401043', 'e2000000-0000-4000-8000-000004010004', 3, '36 metros', FALSE),
  ('f2000000-0000-4000-8000-000000401051', 'e2000000-0000-4000-8000-000004010005', 1, '12 unidades cuadradas', TRUE),
  ('f2000000-0000-4000-8000-000000401052', 'e2000000-0000-4000-8000-000004010005', 2, '7 unidades cuadradas', FALSE),
  ('f2000000-0000-4000-8000-000000401053', 'e2000000-0000-4000-8000-000004010005', 3, '14 unidades cuadradas', FALSE),
  ('f2000000-0000-4000-8000-000000401061', 'e2000000-0000-4000-8000-000004010006', 1, '1 kilo completo (4/4)', TRUE),
  ('f2000000-0000-4000-8000-000000401062', 'e2000000-0000-4000-8000-000004010006', 2, '1/2 kilo', FALSE),
  ('f2000000-0000-4000-8000-000000401063', 'e2000000-0000-4000-8000-000004010006', 3, '2 kilos completos', FALSE)
ON DUPLICATE KEY UPDATE texto = VALUES(texto), es_correcta = VALUES(es_correcta);

-- ----------------------------------------------------------------------
-- Paradas y Contenidos: COM-4P-TEXTOS (Comunicación: Textos Informativos y Tradición Oral Peruana)
-- ----------------------------------------------------------------------
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000004020001', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000004020002', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000004020003', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000004020004', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000004020005', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000004020006', 'RETO', '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('fa000000-0000-4000-8000-000004020001', 'ac000000-0000-4000-8000-000004020001', 1, 'El afiche publicitario y escolar: Elementos clave', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000004020002', 'ac000000-0000-4000-8000-000004020002', 1, 'Textos instructivos: Recetas de platos típicos andinos', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000004020003', 'ac000000-0000-4000-8000-000004020003', 1, 'Inferencia de causa y efecto en noticias escolares', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000004020004', 'ac000000-0000-4000-8000-000004020004', 1, 'Uso de conectores lógicos de tiempo y causa', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000004020005', 'ac000000-0000-4000-8000-000004020005', 1, 'Ortografía: Reglas de acentuación de palabras agudas', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000004020006', 'ac000000-0000-4000-8000-000004020006', 1, 'Creación de un boletín escolar sobre nuestra comunidad', 15, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ca000000-0000-4000-8000-000004020001', 'b0000000-0000-4000-8000-000000000402', 'fa000000-0000-4000-8000-000004020001', 'd2000000-0000-4000-8000-000000000402', 1, FALSE, 50, 18),
  ('ca000000-0000-4000-8000-000004020002', 'b0000000-0000-4000-8000-000000000402', 'fa000000-0000-4000-8000-000004020002', 'd2000000-0000-4000-8000-000000000402', 2, FALSE, 27, 32),
  ('ca000000-0000-4000-8000-000004020003', 'b0000000-0000-4000-8000-000000000402', 'fa000000-0000-4000-8000-000004020003', 'd2000000-0000-4000-8000-000000000402', 3, FALSE, 73, 46),
  ('ca000000-0000-4000-8000-000004020004', 'b0000000-0000-4000-8000-000000000402', 'fa000000-0000-4000-8000-000004020004', 'd2000000-0000-4000-8000-000000000402', 4, FALSE, 35, 60),
  ('ca000000-0000-4000-8000-000004020005', 'b0000000-0000-4000-8000-000000000402', 'fa000000-0000-4000-8000-000004020005', 'd2000000-0000-4000-8000-000000000402', 5, FALSE, 68, 74),
  ('ca000000-0000-4000-8000-000004020006', 'b0000000-0000-4000-8000-000000000402', 'fa000000-0000-4000-8000-000004020006', 'd2000000-0000-4000-8000-000000000402', 6, FALSE, 50, 88)
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id), mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo) VALUES
  ('bb000000-0000-4000-8000-000004020001', 'fa000000-0000-4000-8000-000004020001', 1, 'TEXTO', 'Un afiche combina imagen atractiva, eslogan llamativo y texto breve para convencer o informar a la comunidad sobre una causa importante.'),
  ('bb000000-0000-4000-8000-000004020002', 'fa000000-0000-4000-8000-000004020002', 1, 'TEXTO', 'Un texto instructivo detalla materiales o ingredientes y pasos ordenados con verbos de acción para lograr un objetivo preciso.'),
  ('bb000000-0000-4000-8000-000004020003', 'fa000000-0000-4000-8000-000004020003', 1, 'TEXTO', 'La causa es el motivo por el cual ocurre un hecho. El efecto o consecuencia es lo que sucede como resultado de esa acción.'),
  ('bb000000-0000-4000-8000-000004020004', 'fa000000-0000-4000-8000-000004020004', 1, 'TEXTO', 'Conectores como "primero", "luego", "después", "porque" y "por lo tanto" dan fluidez y coherencia a nuestras oraciones.'),
  ('bb000000-0000-4000-8000-000004020005', 'fa000000-0000-4000-8000-000004020005', 1, 'TEXTO', 'Las palabras agudas llevan tilde en la última sílaba cuando terminan en N, S o vocal (ejemplos: perú, volcán, compás).'),
  ('bb000000-0000-4000-8000-000004020006', 'fa000000-0000-4000-8000-000004020006', 1, 'TEXTO', '¡Gran trabajo editorial! Tu afiche y tus artículos muestran la riqueza de tus saberes y tu capacidad comunicativa.')
ON DUPLICATE KEY UPDATE texto_cuerpo = VALUES(texto_cuerpo);

INSERT INTO evaluacion_pregunta (id, version_actividad_id, numero_secuencia, tipo, enunciado, explicacion, puntos) VALUES
  ('e2000000-0000-4000-8000-000004020001', 'fa000000-0000-4000-8000-000004020001', 1, 'OPCION_MULTIPLE', '¿Qué frase resume la intención persuasiva de un afiche?', 'El eslogan o lema sintetiza la idea central de forma memorable.', 10),
  ('e2000000-0000-4000-8000-000004020002', 'fa000000-0000-4000-8000-000004020002', 1, 'OPCION_MULTIPLE', '¿Por qué los pasos de una receta deben seguir un orden secuencial?', 'El orden garantiza que el procedimiento se realice correctamente paso a paso.', 10),
  ('e2000000-0000-4000-8000-000004020003', 'fa000000-0000-4000-8000-000004020003', 1, 'OPCION_MULTIPLE', 'Si no llovió en tres meses, ¿cuál es la consecuencia en los sembríos?', 'La sequía reduce la humedad y marchita las plantas de cultivo.', 10),
  ('e2000000-0000-4000-8000-000004020004', 'fa000000-0000-4000-8000-000004020004', 1, 'OPCION_MULTIPLE', '¿Cuál de estos conectores expresa una causa o motivo?', '"Porque" introduce la justificación o razón de un suceso.', 10),
  ('e2000000-0000-4000-8000-000004020005', 'fa000000-0000-4000-8000-000004020005', 1, 'OPCION_MULTIPLE', '¿Por qué la palabra "Perú" lleva tilde gráfica?', 'Porque es una palabra aguda que termina en vocal.', 10),
  ('e2000000-0000-4000-8000-000004020006', 'fa000000-0000-4000-8000-000004020006', 1, 'OPCION_MULTIPLE', '¿Qué sección de un boletín escolar presenta la opinión de los estudiantes?', 'El artículo de opinión o editorial refleja los puntos de vista del equipo.', 10)
ON DUPLICATE KEY UPDATE enunciado = VALUES(enunciado), explicacion = VALUES(explicacion);

INSERT INTO evaluacion_opcion_respuesta (id, pregunta_id, numero_secuencia, texto, es_correcta) VALUES
  ('f2000000-0000-4000-8000-000000402011', 'e2000000-0000-4000-8000-000004020001', 1, 'El eslogan', TRUE),
  ('f2000000-0000-4000-8000-000000402012', 'e2000000-0000-4000-8000-000004020001', 2, 'El índice de páginas', FALSE),
  ('f2000000-0000-4000-8000-000000402013', 'e2000000-0000-4000-8000-000004020001', 3, 'El pie de imprenta', FALSE),
  ('f2000000-0000-4000-8000-000000402021', 'e2000000-0000-4000-8000-000004020002', 1, 'Para preparar el plato sin omitir ningún paso clave', TRUE),
  ('f2000000-0000-4000-8000-000000402022', 'e2000000-0000-4000-8000-000004020002', 2, 'Para cambiar los nombres de las frutas', FALSE),
  ('f2000000-0000-4000-8000-000000402023', 'e2000000-0000-4000-8000-000004020002', 3, 'Solo por decoración', FALSE),
  ('f2000000-0000-4000-8000-000000402031', 'e2000000-0000-4000-8000-000004020003', 1, 'Los cultivos se secan por falta de agua', TRUE),
  ('f2000000-0000-4000-8000-000000402032', 'e2000000-0000-4000-8000-000004020003', 2, 'Las cosechas crecen el doble', FALSE),
  ('f2000000-0000-4000-8000-000000402033', 'e2000000-0000-4000-8000-000004020003', 3, 'El río se desborda', FALSE),
  ('f2000000-0000-4000-8000-000000402041', 'e2000000-0000-4000-8000-000004020004', 1, 'Porque', TRUE),
  ('f2000000-0000-4000-8000-000000402042', 'e2000000-0000-4000-8000-000004020004', 2, 'Finalmente', FALSE),
  ('f2000000-0000-4000-8000-000000402043', 'e2000000-0000-4000-8000-000004020004', 3, 'Sin embargo', FALSE),
  ('f2000000-0000-4000-8000-000000402051', 'e2000000-0000-4000-8000-000004020005', 1, 'Porque es aguda y termina en vocal', TRUE),
  ('f2000000-0000-4000-8000-000000402052', 'e2000000-0000-4000-8000-000004020005', 2, 'Porque es esdrújula', FALSE),
  ('f2000000-0000-4000-8000-000000402053', 'e2000000-0000-4000-8000-000004020005', 3, 'Porque termina en consonante D', FALSE),
  ('f2000000-0000-4000-8000-000000402061', 'e2000000-0000-4000-8000-000004020006', 1, 'El artículo de opinión', TRUE),
  ('f2000000-0000-4000-8000-000000402062', 'e2000000-0000-4000-8000-000004020006', 2, 'La sopa de letras', FALSE),
  ('f2000000-0000-4000-8000-000000402063', 'e2000000-0000-4000-8000-000004020006', 3, 'La lista de precios', FALSE)
ON DUPLICATE KEY UPDATE texto = VALUES(texto), es_correcta = VALUES(es_correcta);

-- ----------------------------------------------------------------------
-- Paradas y Contenidos: CYT-4P-ECOSIS (Ciencia y Tecnología: Ecosistemas y Cadenas Vivas del Perú)
-- ----------------------------------------------------------------------
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000004030001', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000004030002', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000004030003', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000004030004', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000004030005', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000004030006', 'RETO', '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('fa000000-0000-4000-8000-000004030001', 'ac000000-0000-4000-8000-000004030001', 1, 'Seres bióticos y abióticos en el ecosistema', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000004030002', 'ac000000-0000-4000-8000-000004030002', 1, 'Cadenas tróficas: Productores, consumidores y descomponedores', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000004030003', 'ac000000-0000-4000-8000-000004030003', 1, 'Adaptaciones de animales al frío de la puna', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000004030004', 'ac000000-0000-4000-8000-000004030004', 1, 'La fotosíntesis: Fábrica de oxígeno y glucosa', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000004030005', 'ac000000-0000-4000-8000-000004030005', 1, 'El rol de los bofedales en la reserva de agua', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000004030006', 'ac000000-0000-4000-8000-000004030006', 1, 'Misión Guardián de la Pampa: Plan de conservación', 15, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ca000000-0000-4000-8000-000004030001', 'b0000000-0000-4000-8000-000000000403', 'fa000000-0000-4000-8000-000004030001', 'd2000000-0000-4000-8000-000000000403', 1, FALSE, 50, 18),
  ('ca000000-0000-4000-8000-000004030002', 'b0000000-0000-4000-8000-000000000403', 'fa000000-0000-4000-8000-000004030002', 'd2000000-0000-4000-8000-000000000403', 2, FALSE, 27, 32),
  ('ca000000-0000-4000-8000-000004030003', 'b0000000-0000-4000-8000-000000000403', 'fa000000-0000-4000-8000-000004030003', 'd2000000-0000-4000-8000-000000000403', 3, FALSE, 73, 46),
  ('ca000000-0000-4000-8000-000004030004', 'b0000000-0000-4000-8000-000000000403', 'fa000000-0000-4000-8000-000004030004', 'd2000000-0000-4000-8000-000000000403', 4, FALSE, 35, 60),
  ('ca000000-0000-4000-8000-000004030005', 'b0000000-0000-4000-8000-000000000403', 'fa000000-0000-4000-8000-000004030005', 'd2000000-0000-4000-8000-000000000403', 5, FALSE, 68, 74),
  ('ca000000-0000-4000-8000-000004030006', 'b0000000-0000-4000-8000-000000000403', 'fa000000-0000-4000-8000-000004030006', 'd2000000-0000-4000-8000-000000000403', 6, FALSE, 50, 88)
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id), mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo) VALUES
  ('bb000000-0000-4000-8000-000004030001', 'fa000000-0000-4000-8000-000004030001', 1, 'TEXTO', 'Los factores bióticos tienen vida (plantas, animales, bacterias). Los factores abióticos no tienen vida pero son indispensables (suelo, aire, agua, luz).'),
  ('bb000000-0000-4000-8000-000004030002', 'fa000000-0000-4000-8000-000004030002', 1, 'TEXTO', 'Las plantas son productoras porque elaboran su alimento con el sol. Los herbívoros son consumidores primarios, y los carnívoros secundarios.'),
  ('bb000000-0000-4000-8000-000004030003', 'fa000000-0000-4000-8000-000004030003', 1, 'TEXTO', 'La vicuña tiene sangre con abundante hemoglobina para captar poco oxígeno, y un vellón finísimo que retiene calor ante heladas severas.'),
  ('bb000000-0000-4000-8000-000004030004', 'fa000000-0000-4000-8000-000004030004', 1, 'TEXTO', 'Con clorofila, agua del suelo y dióxido de carbono del aire, las hojas aprovechan la luz solar para producir glucosa y liberar oxígeno puro.'),
  ('bb000000-0000-4000-8000-000004030005', 'fa000000-0000-4000-8000-000004030005', 1, 'TEXTO', 'Los bofedales son humedales de altura que actúan como esponjas gigantes: retienen agua de lluvia y la sueltan lentamente todo el año.'),
  ('bb000000-0000-4000-8000-000004030006', 'fa000000-0000-4000-8000-000004030006', 1, 'TEXTO', '¡Felicitaciones, ecologista escolar! Has comprendido cómo proteger las especies amenazadas de nuestra patria.')
ON DUPLICATE KEY UPDATE texto_cuerpo = VALUES(texto_cuerpo);

INSERT INTO evaluacion_pregunta (id, version_actividad_id, numero_secuencia, tipo, enunciado, explicacion, puntos) VALUES
  ('e2000000-0000-4000-8000-000004030001', 'fa000000-0000-4000-8000-000004030001', 1, 'OPCION_MULTIPLE', '¿Cuál de los siguientes elementos es un factor abiótico del ecosistema?', 'El agua y la luz solar son elementos físicos indispensables sin vida propia.', 10),
  ('e2000000-0000-4000-8000-000004030002', 'fa000000-0000-4000-8000-000004030002', 1, 'OPCION_MULTIPLE', '¿Qué rol cumple el ichu en la cadena trófica de la puna?', 'El ichu es una planta autótrofa y productora primaria de energía.', 10),
  ('e2000000-0000-4000-8000-000004030003', 'fa000000-0000-4000-8000-000004030003', 1, 'OPCION_MULTIPLE', '¿Qué adaptación física protege a la vicuña del frío extremo?', 'Su fina y tupida fibra de lana retiene bolsas de aire cálido.', 10),
  ('e2000000-0000-4000-8000-000004030004', 'fa000000-0000-4000-8000-000004030004', 1, 'OPCION_MULTIPLE', '¿Qué gas indispensable para la vida liberan las plantas durante la fotosíntesis?', 'Liberan oxígeno puro que respiran los seres vivos.', 10),
  ('e2000000-0000-4000-8000-000004030005', 'fa000000-0000-4000-8000-000004030005', 1, 'OPCION_MULTIPLE', '¿Por qué son vitales los bofedales para las comunidades andinas?', 'Almacenan agua dulce y mantienen pastos verdes en épocas secas.', 10),
  ('e2000000-0000-4000-8000-000004030006', 'fa000000-0000-4000-8000-000004030006', 1, 'OPCION_MULTIPLE', '¿Qué práctica comunitaria protege a las vicuñas sin dañarlas?', 'El chaccu es una esquila tradicional prehispánica sin maltrato animal.', 10)
ON DUPLICATE KEY UPDATE enunciado = VALUES(enunciado), explicacion = VALUES(explicacion);

INSERT INTO evaluacion_opcion_respuesta (id, pregunta_id, numero_secuencia, texto, es_correcta) VALUES
  ('f2000000-0000-4000-8000-000000403011', 'e2000000-0000-4000-8000-000004030001', 1, 'El agua del río', TRUE),
  ('f2000000-0000-4000-8000-000000403012', 'e2000000-0000-4000-8000-000004030001', 2, 'La trucha andina', FALSE),
  ('f2000000-0000-4000-8000-000000403013', 'e2000000-0000-4000-8000-000004030001', 3, 'El árbol de quishuar', FALSE),
  ('f2000000-0000-4000-8000-000000403021', 'e2000000-0000-4000-8000-000004030002', 1, 'Productor', TRUE),
  ('f2000000-0000-4000-8000-000000403022', 'e2000000-0000-4000-8000-000004030002', 2, 'Descomponedor de restos', FALSE),
  ('f2000000-0000-4000-8000-000000403023', 'e2000000-0000-4000-8000-000004030002', 3, 'Depredador terciario', FALSE),
  ('f2000000-0000-4000-8000-000000403031', 'e2000000-0000-4000-8000-000004030003', 1, 'Su fibra de lana tupida y densa', TRUE),
  ('f2000000-0000-4000-8000-000000403032', 'e2000000-0000-4000-8000-000004030003', 2, 'Piel escamosa de lagarto', FALSE),
  ('f2000000-0000-4000-8000-000000403033', 'e2000000-0000-4000-8000-000004030003', 3, 'Aletas para nadar', FALSE),
  ('f2000000-0000-4000-8000-000000403041', 'e2000000-0000-4000-8000-000004030004', 1, 'Oxígeno', TRUE),
  ('f2000000-0000-4000-8000-000000403042', 'e2000000-0000-4000-8000-000004030004', 2, 'Monóxido venenoso', FALSE),
  ('f2000000-0000-4000-8000-000000403043', 'e2000000-0000-4000-8000-000004030004', 3, 'Humo negro', FALSE),
  ('f2000000-0000-4000-8000-000000403051', 'e2000000-0000-4000-8000-000004030005', 1, 'Almacenan agua y regulan el caudal de los ríos', TRUE),
  ('f2000000-0000-4000-8000-000000403052', 'e2000000-0000-4000-8000-000004030005', 2, 'Producen hielo artificial', FALSE),
  ('f2000000-0000-4000-8000-000000403053', 'e2000000-0000-4000-8000-000004030005', 3, 'Secan los manantiales', FALSE),
  ('f2000000-0000-4000-8000-000000403061', 'e2000000-0000-4000-8000-000004030006', 1, 'El Chaccu tradicional sostenible', TRUE),
  ('f2000000-0000-4000-8000-000000403062', 'e2000000-0000-4000-8000-000004030006', 2, 'La caza furtiva', FALSE),
  ('f2000000-0000-4000-8000-000000403063', 'e2000000-0000-4000-8000-000004030006', 3, 'La quema de pastizales', FALSE)
ON DUPLICATE KEY UPDATE texto = VALUES(texto), es_correcta = VALUES(es_correcta);

-- ----------------------------------------------------------------------
-- Paradas y Contenidos: MAT-5P-DECIM (Matemática: Números Decimales y Porcentajes en el Comercio)
-- ----------------------------------------------------------------------
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000005010001', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000005010002', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000005010003', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000005010004', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000005010005', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000005010006', 'RETO', '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('fa000000-0000-4000-8000-000005010001', 'ac000000-0000-4000-8000-000005010001', 1, 'Valor posicional: Décimos, centésimos y milésimos', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000005010002', 'ac000000-0000-4000-8000-000005010002', 1, 'Suma y resta de números decimales alineando la coma', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000005010003', 'ac000000-0000-4000-8000-000005010003', 1, 'Multiplicación de decimales por 10, 100 y 1000', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000005010004', 'ac000000-0000-4000-8000-000005010004', 1, '¿Qué es el porcentaje? Descuentos en la tienda escolar', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000005010005', 'ac000000-0000-4000-8000-000005010005', 1, 'Interpretando gráficos de barras y tablas de frecuencias', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000005010006', 'ac000000-0000-4000-8000-000005010006', 1, 'El gran reto del presupuesto para el viaje escolar', 15, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ca000000-0000-4000-8000-000005010001', 'b0000000-0000-4000-8000-000000000501', 'fa000000-0000-4000-8000-000005010001', 'd2000000-0000-4000-8000-000000000501', 1, FALSE, 50, 18),
  ('ca000000-0000-4000-8000-000005010002', 'b0000000-0000-4000-8000-000000000501', 'fa000000-0000-4000-8000-000005010002', 'd2000000-0000-4000-8000-000000000501', 2, FALSE, 27, 32),
  ('ca000000-0000-4000-8000-000005010003', 'b0000000-0000-4000-8000-000000000501', 'fa000000-0000-4000-8000-000005010003', 'd2000000-0000-4000-8000-000000000501', 3, FALSE, 73, 46),
  ('ca000000-0000-4000-8000-000005010004', 'b0000000-0000-4000-8000-000000000501', 'fa000000-0000-4000-8000-000005010004', 'd2000000-0000-4000-8000-000000000501', 4, FALSE, 35, 60),
  ('ca000000-0000-4000-8000-000005010005', 'b0000000-0000-4000-8000-000000000501', 'fa000000-0000-4000-8000-000005010005', 'd2000000-0000-4000-8000-000000000501', 5, FALSE, 68, 74),
  ('ca000000-0000-4000-8000-000005010006', 'b0000000-0000-4000-8000-000000000501', 'fa000000-0000-4000-8000-000005010006', 'd2000000-0000-4000-8000-000000000501', 6, FALSE, 50, 88)
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id), mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo) VALUES
  ('bb000000-0000-4000-8000-000005010001', 'fa000000-0000-4000-8000-000005010001', 1, 'TEXTO', 'A la derecha de la coma decimal encontramos los décimos (0,1), centésimos (0,01) y milésimos (0,001). Un sol con cincuenta céntimos se escribe S/ 1,50.'),
  ('bb000000-0000-4000-8000-000005010002', 'fa000000-0000-4000-8000-000005010002', 1, 'TEXTO', 'Para sumar o restar decimales se alinean las cifras respetando la posición de la coma decimal en una columna vertical recta.'),
  ('bb000000-0000-4000-8000-000005010003', 'fa000000-0000-4000-8000-000005010003', 1, 'TEXTO', 'Al multiplicar por 10, la coma corre un lugar a la derecha; por 100 corre dos lugares; por 1000 tres lugares: 2,45 x 10 = 24,5.'),
  ('bb000000-0000-4000-8000-000005010004', 'fa000000-0000-4000-8000-000005010004', 1, 'TEXTO', 'El porcentaje (%) representa una fracción de 100 partes. El 50% es la mitad (50/100 = 1/2) y el 25% es la cuarta parte (25/100 = 1/4).'),
  ('bb000000-0000-4000-8000-000005010005', 'fa000000-0000-4000-8000-000005010005', 1, 'TEXTO', 'Las tablas organizan datos recolectados. Las barras permiten comparar visualmente qué opción tiene mayor o menor frecuencia.'),
  ('bb000000-0000-4000-8000-000005010006', 'fa000000-0000-4000-8000-000005010006', 1, 'TEXTO', '¡Misión matemática superada! Manejas decimales y finanzas escolares con exactitud y criterio solidario.')
ON DUPLICATE KEY UPDATE texto_cuerpo = VALUES(texto_cuerpo);

INSERT INTO evaluacion_pregunta (id, version_actividad_id, numero_secuencia, tipo, enunciado, explicacion, puntos) VALUES
  ('e2000000-0000-4000-8000-000005010001', 'fa000000-0000-4000-8000-000005010001', 1, 'OPCION_MULTIPLE', 'En el número decimal 4,75, ¿qué posición ocupa el dígito 7?', 'El 7 está inmediatamente a la derecha de la coma, es decir, décimos.', 10),
  ('e2000000-0000-4000-8000-000005010002', 'fa000000-0000-4000-8000-000005010002', 1, 'OPCION_MULTIPLE', 'Si compras papas por S/ 3,50 y maíz por S/ 2,25, ¿cuánto pagas en total?', 'Sumamos 3,50 + 2,25 = 5,75 soles.', 10),
  ('e2000000-0000-4000-8000-000005010003', 'fa000000-0000-4000-8000-000005010003', 1, 'OPCION_MULTIPLE', '¿Cuánto resulta multiplicar 0,85 x 100?', 'Corremos la coma dos lugares a la derecha: 85.', 10),
  ('e2000000-0000-4000-8000-000005010004', 'fa000000-0000-4000-8000-000005010004', 1, 'OPCION_MULTIPLE', '¿Cuánto es el 50% de 80 cuadernos escolares?', 'El 50% es la mitad: 80 ÷ 2 = 40 cuadernos.', 10),
  ('e2000000-0000-4000-8000-000005010005', 'fa000000-0000-4000-8000-000005010005', 1, 'OPCION_MULTIPLE', 'Si la barra de Matemática llega a 25 votos y la de Arte a 15, ¿cuántos votos más tiene Matemática?', 'Restamos 25 - 15 = 10 votos de diferencia.', 10),
  ('e2000000-0000-4000-8000-000005010006', 'fa000000-0000-4000-8000-000005010006', 1, 'OPCION_MULTIPLE', 'Si un pasaje cuesta S/ 12,50 y viajan 4 estudiantes, ¿cuánto cuesta el pasaje total?', 'Multiplicamos 12,50 x 4 = 50 soles exactos.', 10)
ON DUPLICATE KEY UPDATE enunciado = VALUES(enunciado), explicacion = VALUES(explicacion);

INSERT INTO evaluacion_opcion_respuesta (id, pregunta_id, numero_secuencia, texto, es_correcta) VALUES
  ('f2000000-0000-4000-8000-000000501011', 'e2000000-0000-4000-8000-000005010001', 1, 'Décimos', TRUE),
  ('f2000000-0000-4000-8000-000000501012', 'e2000000-0000-4000-8000-000005010001', 2, 'Centésimos', FALSE),
  ('f2000000-0000-4000-8000-000000501013', 'e2000000-0000-4000-8000-000005010001', 3, 'Unidades enteras', FALSE),
  ('f2000000-0000-4000-8000-000000501021', 'e2000000-0000-4000-8000-000005010002', 1, 'S/ 5,75', TRUE),
  ('f2000000-0000-4000-8000-000000501022', 'e2000000-0000-4000-8000-000005010002', 2, 'S/ 5,25', FALSE),
  ('f2000000-0000-4000-8000-000000501023', 'e2000000-0000-4000-8000-000005010002', 3, 'S/ 6,00', FALSE),
  ('f2000000-0000-4000-8000-000000501031', 'e2000000-0000-4000-8000-000005010003', 1, '85', TRUE),
  ('f2000000-0000-4000-8000-000000501032', 'e2000000-0000-4000-8000-000005010003', 2, '8,5', FALSE),
  ('f2000000-0000-4000-8000-000000501033', 'e2000000-0000-4000-8000-000005010003', 3, '850', FALSE),
  ('f2000000-0000-4000-8000-000000501041', 'e2000000-0000-4000-8000-000005010004', 1, '40 cuadernos', TRUE),
  ('f2000000-0000-4000-8000-000000501042', 'e2000000-0000-4000-8000-000005010004', 2, '20 cuadernos', FALSE),
  ('f2000000-0000-4000-8000-000000501043', 'e2000000-0000-4000-8000-000005010004', 3, '60 cuadernos', FALSE),
  ('f2000000-0000-4000-8000-000000501051', 'e2000000-0000-4000-8000-000005010005', 1, '10 votos más', TRUE),
  ('f2000000-0000-4000-8000-000000501052', 'e2000000-0000-4000-8000-000005010005', 2, '5 votos más', FALSE),
  ('f2000000-0000-4000-8000-000000501053', 'e2000000-0000-4000-8000-000005010005', 3, '15 votos más', FALSE),
  ('f2000000-0000-4000-8000-000000501061', 'e2000000-0000-4000-8000-000005010006', 1, 'S/ 50,00', TRUE),
  ('f2000000-0000-4000-8000-000000501062', 'e2000000-0000-4000-8000-000005010006', 2, 'S/ 48,00', FALSE),
  ('f2000000-0000-4000-8000-000000501063', 'e2000000-0000-4000-8000-000005010006', 3, 'S/ 60,00', FALSE)
ON DUPLICATE KEY UPDATE texto = VALUES(texto), es_correcta = VALUES(es_correcta);

-- ----------------------------------------------------------------------
-- Paradas y Contenidos: COM-5P-ARGUM (Comunicación: Textos Argumentativos y Opinión Crítica)
-- ----------------------------------------------------------------------
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000005020001', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000005020002', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000005020003', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000005020004', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000005020005', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000005020006', 'RETO', '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('fa000000-0000-4000-8000-000005020001', 'ac000000-0000-4000-8000-000005020001', 1, 'Tesis y argumentos: La estructura de la opinión razonada', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000005020002', 'ac000000-0000-4000-8000-000005020002', 1, 'Diferenciando hechos de opiniones en las lecturas', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000005020003', 'ac000000-0000-4000-8000-000005020003', 1, 'Participación en debates: Respeto de turnos y réplica', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000005020004', 'ac000000-0000-4000-8000-000005020004', 1, 'El uso de comillas y fuentes bibliográficas en citas', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000005020005', 'ac000000-0000-4000-8000-000005020005', 1, 'Declamación poética: Ritmo, entonación y sentimiento', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000005020006', 'ac000000-0000-4000-8000-000005020006', 1, 'Ensayo escolar: Cuidemos el patrimonio arqueológico', 15, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ca000000-0000-4000-8000-000005020001', 'b0000000-0000-4000-8000-000000000502', 'fa000000-0000-4000-8000-000005020001', 'd2000000-0000-4000-8000-000000000502', 1, FALSE, 50, 18),
  ('ca000000-0000-4000-8000-000005020002', 'b0000000-0000-4000-8000-000000000502', 'fa000000-0000-4000-8000-000005020002', 'd2000000-0000-4000-8000-000000000502', 2, FALSE, 27, 32),
  ('ca000000-0000-4000-8000-000005020003', 'b0000000-0000-4000-8000-000000000502', 'fa000000-0000-4000-8000-000005020003', 'd2000000-0000-4000-8000-000000000502', 3, FALSE, 73, 46),
  ('ca000000-0000-4000-8000-000005020004', 'b0000000-0000-4000-8000-000000000502', 'fa000000-0000-4000-8000-000005020004', 'd2000000-0000-4000-8000-000000000502', 4, FALSE, 35, 60),
  ('ca000000-0000-4000-8000-000005020005', 'b0000000-0000-4000-8000-000000000502', 'fa000000-0000-4000-8000-000005020005', 'd2000000-0000-4000-8000-000000000502', 5, FALSE, 68, 74),
  ('ca000000-0000-4000-8000-000005020006', 'b0000000-0000-4000-8000-000000000502', 'fa000000-0000-4000-8000-000005020006', 'd2000000-0000-4000-8000-000000000502', 6, FALSE, 50, 88)
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id), mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo) VALUES
  ('bb000000-0000-4000-8000-000005020001', 'fa000000-0000-4000-8000-000005020001', 1, 'TEXTO', 'La tesis es la postura o punto de vista que defendemos. Los argumentos son las razones sólidas y pruebas que la justifican.'),
  ('bb000000-0000-4000-8000-000005020002', 'fa000000-0000-4000-8000-000005020002', 1, 'TEXTO', 'Un hecho es una realidad comprobable objetivamente. Una opinión es un juicio personal o valoración subjetiva.'),
  ('bb000000-0000-4000-8000-000005020003', 'fa000000-0000-4000-8000-000005020003', 1, 'TEXTO', 'En un debate escolar se exponen argumentos escuchando al interlocutor con serenidad y rebatiendo ideas sin ofender.'),
  ('bb000000-0000-4000-8000-000005020004', 'fa000000-0000-4000-8000-000005020004', 1, 'TEXTO', 'Cuando copiamos palabras de otro autor, las encerramos entre comillas ("...") e indicamos de quién es la autoría para evitar el plagio.'),
  ('bb000000-0000-4000-8000-000005020005', 'fa000000-0000-4000-8000-000005020005', 1, 'TEXTO', 'Al declamar poemas peruanos modulamos el tono de voz, proyectamos el aire desde el diafragma y transmitimos emoción.'),
  ('bb000000-0000-4000-8000-000005020006', 'fa000000-0000-4000-8000-000005020006', 1, 'TEXTO', '¡Magistral argumentación! Tu ensayo defiende la identidad peruana con fundamentos claros y convincentes.')
ON DUPLICATE KEY UPDATE texto_cuerpo = VALUES(texto_cuerpo);

INSERT INTO evaluacion_pregunta (id, version_actividad_id, numero_secuencia, tipo, enunciado, explicacion, puntos) VALUES
  ('e2000000-0000-4000-8000-000005020001', 'fa000000-0000-4000-8000-000005020001', 1, 'OPCION_MULTIPLE', '¿Qué es la tesis en un texto argumentativo?', 'Es la opinión o postura principal que el autor fundamenta con argumentos.', 10),
  ('e2000000-0000-4000-8000-000005020002', 'fa000000-0000-4000-8000-000005020002', 1, 'OPCION_MULTIPLE', '¿Cuál de las siguientes expresiones es un hecho comprobable?', '"Ayacucho es una región del Perú" es un dato geográfico verificado.', 10),
  ('e2000000-0000-4000-8000-000005020003', 'fa000000-0000-4000-8000-000005020003', 1, 'OPCION_MULTIPLE', '¿Qué actitud es indispensable durante un debate respetuoso?', 'Escuchar los argumentos ajenos y esperar el turno de palabra.', 10),
  ('e2000000-0000-4000-8000-000005020004', 'fa000000-0000-4000-8000-000005020004', 1, 'OPCION_MULTIPLE', '¿Para qué se utilizan las comillas al citar un texto?', 'Para señalar que esas palabras textuales pertenecen a otro autor.', 10),
  ('e2000000-0000-4000-8000-000005020005', 'fa000000-0000-4000-8000-000005020005', 1, 'OPCION_MULTIPLE', '¿Qué recurso vocal ayuda a transmitir la emoción de un verso triste?', 'Un tono pausado, suave y reflexivo acompaña la emoción.', 10),
  ('e2000000-0000-4000-8000-000005020006', 'fa000000-0000-4000-8000-000005020006', 1, 'OPCION_MULTIPLE', '¿Cuál es la conclusión esperada en un ensayo argumentativo?', 'Reafirmar la tesis sintetizando los argumentos presentados.', 10)
ON DUPLICATE KEY UPDATE enunciado = VALUES(enunciado), explicacion = VALUES(explicacion);

INSERT INTO evaluacion_opcion_respuesta (id, pregunta_id, numero_secuencia, texto, es_correcta) VALUES
  ('f2000000-0000-4000-8000-000000502011', 'e2000000-0000-4000-8000-000005020001', 1, 'La postura central que se defiende con razones', TRUE),
  ('f2000000-0000-4000-8000-000000502012', 'e2000000-0000-4000-8000-000005020001', 2, 'La lista de personajes secundarios', FALSE),
  ('f2000000-0000-4000-8000-000000502013', 'e2000000-0000-4000-8000-000005020001', 3, 'El resumen de un diccionario', FALSE),
  ('f2000000-0000-4000-8000-000000502021', 'e2000000-0000-4000-8000-000005020002', 1, 'Ayacucho es un departamento ubicado en la sierra del Perú', TRUE),
  ('f2000000-0000-4000-8000-000000502022', 'e2000000-0000-4000-8000-000005020002', 2, 'El color naranja es el más bonito del mundo', FALSE),
  ('f2000000-0000-4000-8000-000000502023', 'e2000000-0000-4000-8000-000005020002', 3, 'La música clásica es aburrida', FALSE),
  ('f2000000-0000-4000-8000-000000502031', 'e2000000-0000-4000-8000-000005020003', 1, 'Escuchar con atención y esperar el turno de palabra', TRUE),
  ('f2000000-0000-4000-8000-000000502032', 'e2000000-0000-4000-8000-000005020003', 2, 'Interrumpir gritando más fuerte', FALSE),
  ('f2000000-0000-4000-8000-000000502033', 'e2000000-0000-4000-8000-000005020003', 3, 'Burlarse de quien opina distinto', FALSE),
  ('f2000000-0000-4000-8000-000000502041', 'e2000000-0000-4000-8000-000005020004', 1, 'Para indicar las palabras textuales de otra persona', TRUE),
  ('f2000000-0000-4000-8000-000000502042', 'e2000000-0000-4000-8000-000005020004', 2, 'Para indicar que la palabra está mal escrita', FALSE),
  ('f2000000-0000-4000-8000-000000502043', 'e2000000-0000-4000-8000-000005020004', 3, 'Para terminar una oración', FALSE),
  ('f2000000-0000-4000-8000-000000502051', 'e2000000-0000-4000-8000-000005020005', 1, 'Modular un tono pausado y emotivo', TRUE),
  ('f2000000-0000-4000-8000-000000502052', 'e2000000-0000-4000-8000-000005020005', 2, 'Gritar como en un megáfono', FALSE),
  ('f2000000-0000-4000-8000-000000502053', 'e2000000-0000-4000-8000-000005020005', 3, 'Hablar muy rápido sin respirar', FALSE),
  ('f2000000-0000-4000-8000-000000502061', 'e2000000-0000-4000-8000-000005020006', 1, 'Reafirmar la postura central sintetizando los argumentos', TRUE),
  ('f2000000-0000-4000-8000-000000502062', 'e2000000-0000-4000-8000-000005020006', 2, 'Inventar un nuevo problema sin relación', FALSE),
  ('f2000000-0000-4000-8000-000000502063', 'e2000000-0000-4000-8000-000005020006', 3, 'Copiar el texto al revés', FALSE)
ON DUPLICATE KEY UPDATE texto = VALUES(texto), es_correcta = VALUES(es_correcta);

-- ----------------------------------------------------------------------
-- Paradas y Contenidos: CYT-5P-CUERPO (Ciencia y Tecnología: Sistemas del Cuerpo Humano y Nutrición Andina)
-- ----------------------------------------------------------------------
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000005030001', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000005030002', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000005030003', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000005030004', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000005030005', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000005030006', 'RETO', '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('fa000000-0000-4000-8000-000005030001', 'ac000000-0000-4000-8000-000005030001', 1, 'El sistema digestivo: Transformación de nutrientes', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000005030002', 'ac000000-0000-4000-8000-000005030002', 1, 'El sistema circulatorio: El corazón y los vasos sanguíneos', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000005030003', 'ac000000-0000-4000-8000-000005030003', 1, 'El sistema respiratorio: Pulmones y alvéolos a gran altura', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000005030004', 'ac000000-0000-4000-8000-000005030004', 1, 'Valor nutricional de la quinua, kiwicha y cañihua', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000005030005', 'ac000000-0000-4000-8000-000005030005', 1, 'La pirámide alimenticia y el plato del buen comer', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000005030006', 'ac000000-0000-4000-8000-000005030006', 1, 'Feria de la salud: Loncheras nutritivas sin octógonos', 15, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ca000000-0000-4000-8000-000005030001', 'b0000000-0000-4000-8000-000000000503', 'fa000000-0000-4000-8000-000005030001', 'd2000000-0000-4000-8000-000000000503', 1, FALSE, 50, 18),
  ('ca000000-0000-4000-8000-000005030002', 'b0000000-0000-4000-8000-000000000503', 'fa000000-0000-4000-8000-000005030002', 'd2000000-0000-4000-8000-000000000503', 2, FALSE, 27, 32),
  ('ca000000-0000-4000-8000-000005030003', 'b0000000-0000-4000-8000-000000000503', 'fa000000-0000-4000-8000-000005030003', 'd2000000-0000-4000-8000-000000000503', 3, FALSE, 73, 46),
  ('ca000000-0000-4000-8000-000005030004', 'b0000000-0000-4000-8000-000000000503', 'fa000000-0000-4000-8000-000005030004', 'd2000000-0000-4000-8000-000000000503', 4, FALSE, 35, 60),
  ('ca000000-0000-4000-8000-000005030005', 'b0000000-0000-4000-8000-000000000503', 'fa000000-0000-4000-8000-000005030005', 'd2000000-0000-4000-8000-000000000503', 5, FALSE, 68, 74),
  ('ca000000-0000-4000-8000-000005030006', 'b0000000-0000-4000-8000-000000000503', 'fa000000-0000-4000-8000-000005030006', 'd2000000-0000-4000-8000-000000000503', 6, FALSE, 50, 88)
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id), mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo) VALUES
  ('bb000000-0000-4000-8000-000005030001', 'fa000000-0000-4000-8000-000005030001', 1, 'TEXTO', 'La boca tritura, el estómago descompone y el intestino delgado absorbe los nutrientes que pasarán a la sangre para darnos energía.'),
  ('bb000000-0000-4000-8000-000005030002', 'fa000000-0000-4000-8000-000005030002', 1, 'TEXTO', 'El corazón es un músculo infatigable que bombea sangre oxigenada por las arterias a todo el cuerpo y la recoge por las venas.'),
  ('bb000000-0000-4000-8000-000005030003', 'fa000000-0000-4000-8000-000005030003', 1, 'TEXTO', 'Inspiramos aire con oxígeno por los bronquios hasta los alvéolos pulmonares, donde ocurre el intercambio de gases con la sangre.'),
  ('bb000000-0000-4000-8000-000005030004', 'fa000000-0000-4000-8000-000005030004', 1, 'TEXTO', 'Estos granos andinos poseen todos los aminoácidos esenciales, hierro y calcio, combatiendo la anemia y fortaleciendo el desarrollo cerebral.'),
  ('bb000000-0000-4000-8000-000005030005', 'fa000000-0000-4000-8000-000005030005', 1, 'TEXTO', 'Un plato balanceado debe incluir 50% de vegetales y frutas, 25% de proteínas (menestras/huevo/pescado) y 25% de cereales andinos.'),
  ('bb000000-0000-4000-8000-000005030006', 'fa000000-0000-4000-8000-000005030006', 1, 'TEXTO', '¡Excelente promotor de salud escolar! Has creado un plan de alimentación que previene la anemia y potencia la salud.')
ON DUPLICATE KEY UPDATE texto_cuerpo = VALUES(texto_cuerpo);

INSERT INTO evaluacion_pregunta (id, version_actividad_id, numero_secuencia, tipo, enunciado, explicacion, puntos) VALUES
  ('e2000000-0000-4000-8000-000005030001', 'fa000000-0000-4000-8000-000005030001', 1, 'OPCION_MULTIPLE', '¿En qué órgano se absorben los nutrientes principales hacia la sangre?', 'El intestino delgado tiene vellosidades que absorben los nutrientes.', 10),
  ('e2000000-0000-4000-8000-000005030002', 'fa000000-0000-4000-8000-000005030002', 1, 'OPCION_MULTIPLE', '¿Qué vasos sanguíneos llevan sangre con oxígeno desde el corazón al cuerpo?', 'Las arterias transportan la sangre oxigenada hacia los tejidos.', 10),
  ('e2000000-0000-4000-8000-000005030003', 'fa000000-0000-4000-8000-000005030003', 1, 'OPCION_MULTIPLE', '¿Qué gas de desecho expulsamos al exhalar el aire de los pulmones?', 'Expulsamos dióxido de carbono (CO2) resultante del metabolismo.', 10),
  ('e2000000-0000-4000-8000-000005030004', 'fa000000-0000-4000-8000-000005030004', 1, 'OPCION_MULTIPLE', '¿Por qué la quinua es considerada un súper alimento a nivel mundial?', 'Porque contiene proteínas completas con todos los aminoácidos esenciales.', 10),
  ('e2000000-0000-4000-8000-000005030005', 'fa000000-0000-4000-8000-000005030005', 1, 'OPCION_MULTIPLE', '¿Qué porcentaje del plato saludable debe estar compuesto por verduras y hortalizas?', 'Se recomienda la mitad del plato (50%) en verduras variadas.', 10),
  ('e2000000-0000-4000-8000-000005030006', 'fa000000-0000-4000-8000-000005030006', 1, 'OPCION_MULTIPLE', '¿Qué alimento es ideal para una lonchera escolar saludable?', 'Un huevo sancochado, fruta fresca y agua pura brindan energía limpia.', 10)
ON DUPLICATE KEY UPDATE enunciado = VALUES(enunciado), explicacion = VALUES(explicacion);

INSERT INTO evaluacion_opcion_respuesta (id, pregunta_id, numero_secuencia, texto, es_correcta) VALUES
  ('f2000000-0000-4000-8000-000000503011', 'e2000000-0000-4000-8000-000005030001', 1, 'El intestino delgado', TRUE),
  ('f2000000-0000-4000-8000-000000503012', 'e2000000-0000-4000-8000-000005030001', 2, 'El esófago', FALSE),
  ('f2000000-0000-4000-8000-000000503013', 'e2000000-0000-4000-8000-000005030001', 3, 'El apéndice', FALSE),
  ('f2000000-0000-4000-8000-000000503021', 'e2000000-0000-4000-8000-000005030002', 1, 'Las arterias', TRUE),
  ('f2000000-0000-4000-8000-000000503022', 'e2000000-0000-4000-8000-000005030002', 2, 'Las cuerdas vocales', FALSE),
  ('f2000000-0000-4000-8000-000000503023', 'e2000000-0000-4000-8000-000005030002', 3, 'Los alvéolos', FALSE),
  ('f2000000-0000-4000-8000-000000503031', 'e2000000-0000-4000-8000-000005030003', 1, 'Dióxido de carbono', TRUE),
  ('f2000000-0000-4000-8000-000000503032', 'e2000000-0000-4000-8000-000005030003', 2, 'Nitrógeno líquido', FALSE),
  ('f2000000-0000-4000-8000-000000503033', 'e2000000-0000-4000-8000-000005030003', 3, 'Oxígeno puro', FALSE),
  ('f2000000-0000-4000-8000-000000503041', 'e2000000-0000-4000-8000-000005030004', 1, 'Porque aporta proteínas completas y minerales esenciales', TRUE),
  ('f2000000-0000-4000-8000-000000503042', 'e2000000-0000-4000-8000-000005030004', 2, 'Porque solo tiene azúcar artificial', FALSE),
  ('f2000000-0000-4000-8000-000000503043', 'e2000000-0000-4000-8000-000005030004', 3, 'Porque no tiene ningún nutriente', FALSE),
  ('f2000000-0000-4000-8000-000000503051', 'e2000000-0000-4000-8000-000005030005', 1, 'La mitad del plato (50%)', TRUE),
  ('f2000000-0000-4000-8000-000000503052', 'e2000000-0000-4000-8000-000005030005', 2, 'El 5%', FALSE),
  ('f2000000-0000-4000-8000-000000503053', 'e2000000-0000-4000-8000-000005030005', 3, 'Solo una cucharadita', FALSE),
  ('f2000000-0000-4000-8000-000000503061', 'e2000000-0000-4000-8000-000005030006', 1, 'Fruta fresca, huevo duro y agua pura', TRUE),
  ('f2000000-0000-4000-8000-000000503062', 'e2000000-0000-4000-8000-000005030006', 2, 'Gaseosa y golosinas ultraprocesadas', FALSE),
  ('f2000000-0000-4000-8000-000000503063', 'e2000000-0000-4000-8000-000005030006', 3, 'Papas fritas con mayonesa', FALSE)
ON DUPLICATE KEY UPDATE texto = VALUES(texto), es_correcta = VALUES(es_correcta);

-- ----------------------------------------------------------------------
-- Paradas y Contenidos: MAT-6P-PROPOR (Matemática: Proporcionalidad, Álgebra Inicial y Geometría Espacial)
-- ----------------------------------------------------------------------
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000006010001', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000006010002', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000006010003', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000006010004', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000006010005', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000006010006', 'RETO', '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('fa000000-0000-4000-8000-000006010001', 'ac000000-0000-4000-8000-000006010001', 1, 'Razones y proporcionalidad directa en la producción artesanal', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000006010002', 'ac000000-0000-4000-8000-000006010002', 1, 'Ecuaciones de primer grado: Hallando la incógnita X', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000006010003', 'ac000000-0000-4000-8000-000006010003', 1, 'Volumen de cuerpos geométricos: Prismas rectangulares', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000006010004', 'ac000000-0000-4000-8000-000006010004', 1, 'Probabilidad elemental: Sucesos seguros, posibles e imposibles', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000006010005', 'ac000000-0000-4000-8000-000006010005', 1, 'Promedio y mediana en el rendimiento escolar', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000006010006', 'ac000000-0000-4000-8000-000006010006', 1, 'Gran prueba de graduación: Retos matemáticos del Bicentenario', 15, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ca000000-0000-4000-8000-000006010001', 'b0000000-0000-4000-8000-000000000601', 'fa000000-0000-4000-8000-000006010001', 'd2000000-0000-4000-8000-000000000601', 1, FALSE, 50, 18),
  ('ca000000-0000-4000-8000-000006010002', 'b0000000-0000-4000-8000-000000000601', 'fa000000-0000-4000-8000-000006010002', 'd2000000-0000-4000-8000-000000000601', 2, FALSE, 27, 32),
  ('ca000000-0000-4000-8000-000006010003', 'b0000000-0000-4000-8000-000000000601', 'fa000000-0000-4000-8000-000006010003', 'd2000000-0000-4000-8000-000000000601', 3, FALSE, 73, 46),
  ('ca000000-0000-4000-8000-000006010004', 'b0000000-0000-4000-8000-000000000601', 'fa000000-0000-4000-8000-000006010004', 'd2000000-0000-4000-8000-000000000601', 4, FALSE, 35, 60),
  ('ca000000-0000-4000-8000-000006010005', 'b0000000-0000-4000-8000-000000000601', 'fa000000-0000-4000-8000-000006010005', 'd2000000-0000-4000-8000-000000000601', 5, FALSE, 68, 74),
  ('ca000000-0000-4000-8000-000006010006', 'b0000000-0000-4000-8000-000000000601', 'fa000000-0000-4000-8000-000006010006', 'd2000000-0000-4000-8000-000000000601', 6, FALSE, 50, 88)
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id), mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo) VALUES
  ('bb000000-0000-4000-8000-000006010001', 'fa000000-0000-4000-8000-000006010001', 1, 'TEXTO', 'Dos magnitudes son directamente proporcionales si al multiplicar una por un número, la otra queda multiplicada por el mismo número: si 1 telar usa 2 ovillos, 3 telares usan 6 ovillos.'),
  ('bb000000-0000-4000-8000-000006010002', 'fa000000-0000-4000-8000-000006010002', 1, 'TEXTO', 'Una ecuación es una igualdad con una incógnita desconocida. Si x + 15 = 40, restamos 15 a ambos lados para obtener x = 25.'),
  ('bb000000-0000-4000-8000-000006010003', 'fa000000-0000-4000-8000-000006010003', 1, 'TEXTO', 'El volumen mide el espacio tridimensional que ocupa un cuerpo. En un prisma rectangular se calcula multiplicando: largo x ancho x altura.'),
  ('bb000000-0000-4000-8000-000006010004', 'fa000000-0000-4000-8000-000006010004', 1, 'TEXTO', 'La probabilidad mide la posibilidad de que ocurra un evento entre el número de casos totales posibles.'),
  ('bb000000-0000-4000-8000-000006010005', 'fa000000-0000-4000-8000-000006010005', 1, 'TEXTO', 'La media aritmética o promedio es la suma de todos los datos dividida entre el total de datos. Si tus notas son 16, 18 y 20, tu promedio es (16+18+20)/3 = 18.'),
  ('bb000000-0000-4000-8000-000006010006', 'fa000000-0000-4000-8000-000006010006', 1, 'TEXTO', '¡Felicitaciones! Has dominado las matemáticas de educación primaria con solidez, razonamiento y orgullo peruano.')
ON DUPLICATE KEY UPDATE texto_cuerpo = VALUES(texto_cuerpo);

INSERT INTO evaluacion_pregunta (id, version_actividad_id, numero_secuencia, tipo, enunciado, explicacion, puntos) VALUES
  ('e2000000-0000-4000-8000-000006010001', 'fa000000-0000-4000-8000-000006010001', 1, 'OPCION_MULTIPLE', 'Si para tejer 2 mantas se usan 6 ovillos de lana, ¿cuántos ovillos se necesitarán para 4 mantas?', 'Aplicamos proporción directa: 4 mantas es el doble de 2, por ende 6 x 2 = 12 ovillos.', 10),
  ('e2000000-0000-4000-8000-000006010002', 'fa000000-0000-4000-8000-000006010002', 1, 'OPCION_MULTIPLE', 'En la ecuación 2x + 6 = 20, ¿cuál es el valor de x?', 'Restamos 6: 2x = 14; dividimos entre 2: x = 7.', 10),
  ('e2000000-0000-4000-8000-000006010003', 'fa000000-0000-4000-8000-000006010003', 1, 'OPCION_MULTIPLE', '¿Cuál es el volumen de una caja de retablo que mide 4 cm de largo, 3 cm de ancho y 5 cm de alto?', 'Volumen = 4 x 3 x 5 = 60 centímetros cúbicos.', 10),
  ('e2000000-0000-4000-8000-000006010004', 'fa000000-0000-4000-8000-000006010004', 1, 'OPCION_MULTIPLE', 'Al lanzar un dado común de 6 caras, ¿cuál es la probabilidad de obtener el número 4?', 'Hay 1 cara con el número 4 de un total de 6 caras: 1/6.', 10),
  ('e2000000-0000-4000-8000-000006010005', 'fa000000-0000-4000-8000-000006010005', 1, 'OPCION_MULTIPLE', '¿Cuál es el promedio de las calificaciones: 14, 16 y 18?', 'Sumamos 14 + 16 + 18 = 48; dividimos entre 3: 48 ÷ 3 = 16.', 10),
  ('e2000000-0000-4000-8000-000006010006', 'fa000000-0000-4000-8000-000006010006', 1, 'OPCION_MULTIPLE', 'Si una obra comunal emplea 5 trabajadores durante 6 días, ¿cuántas jornadas laborales en total se ejecutaron?', 'Multiplicamos 5 trabajadores x 6 días = 30 jornadas laborales.', 10)
ON DUPLICATE KEY UPDATE enunciado = VALUES(enunciado), explicacion = VALUES(explicacion);

INSERT INTO evaluacion_opcion_respuesta (id, pregunta_id, numero_secuencia, texto, es_correcta) VALUES
  ('f2000000-0000-4000-8000-000000601011', 'e2000000-0000-4000-8000-000006010001', 1, '12 ovillos', TRUE),
  ('f2000000-0000-4000-8000-000000601012', 'e2000000-0000-4000-8000-000006010001', 2, '8 ovillos', FALSE),
  ('f2000000-0000-4000-8000-000000601013', 'e2000000-0000-4000-8000-000006010001', 3, '18 ovillos', FALSE),
  ('f2000000-0000-4000-8000-000000601021', 'e2000000-0000-4000-8000-000006010002', 1, 'x = 7', TRUE),
  ('f2000000-0000-4000-8000-000000601022', 'e2000000-0000-4000-8000-000006010002', 2, 'x = 10', FALSE),
  ('f2000000-0000-4000-8000-000000601023', 'e2000000-0000-4000-8000-000006010002', 3, 'x = 14', FALSE),
  ('f2000000-0000-4000-8000-000000601031', 'e2000000-0000-4000-8000-000006010003', 1, '60 cm³', TRUE),
  ('f2000000-0000-4000-8000-000000601032', 'e2000000-0000-4000-8000-000006010003', 2, '45 cm³', FALSE),
  ('f2000000-0000-4000-8000-000000601033', 'e2000000-0000-4000-8000-000006010003', 3, '24 cm³', FALSE),
  ('f2000000-0000-4000-8000-000000601041', 'e2000000-0000-4000-8000-000006010004', 1, '1 de 6 (1/6)', TRUE),
  ('f2000000-0000-4000-8000-000000601042', 'e2000000-0000-4000-8000-000006010004', 2, '1 de 2 (1/2)', FALSE),
  ('f2000000-0000-4000-8000-000000601043', 'e2000000-0000-4000-8000-000006010004', 3, '4 de 6 (4/6)', FALSE),
  ('f2000000-0000-4000-8000-000000601051', 'e2000000-0000-4000-8000-000006010005', 1, '16', TRUE),
  ('f2000000-0000-4000-8000-000000601052', 'e2000000-0000-4000-8000-000006010005', 2, '15', FALSE),
  ('f2000000-0000-4000-8000-000000601053', 'e2000000-0000-4000-8000-000006010005', 3, '17', FALSE),
  ('f2000000-0000-4000-8000-000000601061', 'e2000000-0000-4000-8000-000006010006', 1, '30 jornadas laborales', TRUE),
  ('f2000000-0000-4000-8000-000000601062', 'e2000000-0000-4000-8000-000006010006', 2, '20 jornadas', FALSE),
  ('f2000000-0000-4000-8000-000000601063', 'e2000000-0000-4000-8000-000006010006', 3, '11 jornadas', FALSE)
ON DUPLICATE KEY UPDATE texto = VALUES(texto), es_correcta = VALUES(es_correcta);

-- ----------------------------------------------------------------------
-- Paradas y Contenidos: COM-6P-CRONIC (Comunicación: Crónicas, Reportajes y Creación Literaria)
-- ----------------------------------------------------------------------
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000006020001', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000006020002', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000006020003', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000006020004', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000006020005', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000006020006', 'RETO', '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('fa000000-0000-4000-8000-000006020001', 'ac000000-0000-4000-8000-000006020001', 1, 'La crónica periodística: Hechos reales contados con arte', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000006020002', 'ac000000-0000-4000-8000-000006020002', 1, 'La entrevista periodística: Preguntas abiertas y repreguntas', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000006020003', 'ac000000-0000-4000-8000-000006020003', 1, 'Figuras literarias: Metáfora, símil y personificación', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000006020004', 'ac000000-0000-4000-8000-000006020004', 1, 'Uso correcto de la tilde diacrítica en monosílabos', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000006020005', 'ac000000-0000-4000-8000-000006020005', 1, 'Cohesión textual: Elipsis y sustitución léxica', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000006020006', 'ac000000-0000-4000-8000-000006020006', 1, 'Discurso de graduación de primaria: Nuestro legado escolar', 15, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ca000000-0000-4000-8000-000006020001', 'b0000000-0000-4000-8000-000000000602', 'fa000000-0000-4000-8000-000006020001', 'd2000000-0000-4000-8000-000000000602', 1, FALSE, 50, 18),
  ('ca000000-0000-4000-8000-000006020002', 'b0000000-0000-4000-8000-000000000602', 'fa000000-0000-4000-8000-000006020002', 'd2000000-0000-4000-8000-000000000602', 2, FALSE, 27, 32),
  ('ca000000-0000-4000-8000-000006020003', 'b0000000-0000-4000-8000-000000000602', 'fa000000-0000-4000-8000-000006020003', 'd2000000-0000-4000-8000-000000000602', 3, FALSE, 73, 46),
  ('ca000000-0000-4000-8000-000006020004', 'b0000000-0000-4000-8000-000000000602', 'fa000000-0000-4000-8000-000006020004', 'd2000000-0000-4000-8000-000000000602', 4, FALSE, 35, 60),
  ('ca000000-0000-4000-8000-000006020005', 'b0000000-0000-4000-8000-000000000602', 'fa000000-0000-4000-8000-000006020005', 'd2000000-0000-4000-8000-000000000602', 5, FALSE, 68, 74),
  ('ca000000-0000-4000-8000-000006020006', 'b0000000-0000-4000-8000-000000000602', 'fa000000-0000-4000-8000-000006020006', 'd2000000-0000-4000-8000-000000000602', 6, FALSE, 50, 88)
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id), mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo) VALUES
  ('bb000000-0000-4000-8000-000006020001', 'fa000000-0000-4000-8000-000006020001', 1, 'TEXTO', 'Una crónica narra acontecimientos de interés público siguiendo un orden cronológico y utilizando descripciones sensoriales vivas.'),
  ('bb000000-0000-4000-8000-000006020002', 'fa000000-0000-4000-8000-000006020002', 1, 'TEXTO', 'Una buena entrevista formula preguntas abiertas ("¿Por qué...", "¿Cómo fue...") que permiten al entrevistado explayarse sobre sus vivencias.'),
  ('bb000000-0000-4000-8000-000006020003', 'fa000000-0000-4000-8000-000006020003', 1, 'TEXTO', 'El símil compara usando "como" ("veloz como el viento"). La metáfora identifica dos conceptos ("las perlas de tu boca"). La personificación da vida a objetos.'),
  ('bb000000-0000-4000-8000-000006020004', 'fa000000-0000-4000-8000-000006020004', 1, 'TEXTO', 'La tilde diacrítica distingue palabras idénticas con distinta función: tú (pronombre) / tu (posesivo); él (pronombre) / el (artículo); té (bebida) / te (pronombre).'),
  ('bb000000-0000-4000-8000-000006020005', 'fa000000-0000-4000-8000-000006020005', 1, 'TEXTO', 'Para no repetir las mismas palabras en un texto usamos sinónimos, pronombres o elipsis (omisión intencional de palabras sobreentendidas).'),
  ('bb000000-0000-4000-8000-000006020006', 'fa000000-0000-4000-8000-000006020006', 1, 'TEXTO', '¡Felicitaciones, graduado de primaria! Has dominado la lectura y redacción con excelencia para ingresar a secundaria.')
ON DUPLICATE KEY UPDATE texto_cuerpo = VALUES(texto_cuerpo);

INSERT INTO evaluacion_pregunta (id, version_actividad_id, numero_secuencia, tipo, enunciado, explicacion, puntos) VALUES
  ('e2000000-0000-4000-8000-000006020001', 'fa000000-0000-4000-8000-000006020001', 1, 'OPCION_MULTIPLE', '¿Qué rasgo principal caracteriza a una crónica periodística?', 'Relata sucesos reales siguiendo una secuencia temporal con estilo narrativo.', 10),
  ('e2000000-0000-4000-8000-000006020002', 'fa000000-0000-4000-8000-000006020002', 1, 'OPCION_MULTIPLE', '¿Cuál de las siguientes preguntas es abierta y fomenta un testimonio amplio?', '"¿Cómo aprendió el arte del retablo...?" invita a una respuesta detallada.', 10),
  ('e2000000-0000-4000-8000-000006020003', 'fa000000-0000-4000-8000-000006020003', 1, 'OPCION_MULTIPLE', 'En la frase "El río cantaba alegre entre las piedras", ¿qué figura literaria se emplea?', 'Atribuir la acción humana de cantar a un río es una personificación.', 10),
  ('e2000000-0000-4000-8000-000006020004', 'fa000000-0000-4000-8000-000006020004', 1, 'OPCION_MULTIPLE', '¿En cuál de las siguientes oraciones "él" debe llevar tilde diacrítica?', 'Cuando sustituye a la persona (pronombre personal): "Él estudió para la prueba".', 10),
  ('e2000000-0000-4000-8000-000006020005', 'fa000000-0000-4000-8000-000006020005', 1, 'OPCION_MULTIPLE', '¿Para qué sirve la sustitución léxica en una redacción?', 'Evita repeticiones monótonas y enriquece la variedad del vocabulario.', 10),
  ('e2000000-0000-4000-8000-000006020006', 'fa000000-0000-4000-8000-000006020006', 1, 'OPCION_MULTIPLE', '¿Cuál es el propósito central de un discurso de graduación escolar?', 'Agradecer a docentes y familias, celebrar logros y motivar hacia nuevos retos.', 10)
ON DUPLICATE KEY UPDATE enunciado = VALUES(enunciado), explicacion = VALUES(explicacion);

INSERT INTO evaluacion_opcion_respuesta (id, pregunta_id, numero_secuencia, texto, es_correcta) VALUES
  ('f2000000-0000-4000-8000-000000602011', 'e2000000-0000-4000-8000-000006020001', 1, 'Narra hechos reales siguiendo el paso del tiempo con estilo vívido', TRUE),
  ('f2000000-0000-4000-8000-000000602012', 'e2000000-0000-4000-8000-000006020001', 2, 'Inventa cuentos de hadas imaginarios', FALSE),
  ('f2000000-0000-4000-8000-000000602013', 'e2000000-0000-4000-8000-000006020001', 3, 'Solo contiene números matemáticos', FALSE),
  ('f2000000-0000-4000-8000-000000602021', 'e2000000-0000-4000-8000-000006020002', 1, '¿Cómo aprendió usted el arte del retablo y qué dificultades superó?', TRUE),
  ('f2000000-0000-4000-8000-000000602022', 'e2000000-0000-4000-8000-000006020002', 2, '¿Es usted de Ayacucho? (Sí/No)', FALSE),
  ('f2000000-0000-4000-8000-000000602023', 'e2000000-0000-4000-8000-000006020002', 3, '¿Tiene más de 20 años?', FALSE),
  ('f2000000-0000-4000-8000-000000602031', 'e2000000-0000-4000-8000-000006020003', 1, 'Personificación', TRUE),
  ('f2000000-0000-4000-8000-000000602032', 'e2000000-0000-4000-8000-000006020003', 2, 'Hipérbole exagerada', FALSE),
  ('f2000000-0000-4000-8000-000000602033', 'e2000000-0000-4000-8000-000006020003', 3, 'Pleonasmo', FALSE),
  ('f2000000-0000-4000-8000-000000602041', 'e2000000-0000-4000-8000-000006020004', 1, 'Él estudió con dedicación para el examen', TRUE),
  ('f2000000-0000-4000-8000-000000602042', 'e2000000-0000-4000-8000-000006020004', 2, 'El libro de lectura está en la mesa', FALSE),
  ('f2000000-0000-4000-8000-000000602043', 'e2000000-0000-4000-8000-000006020004', 3, 'El sol brilla en la mañana', FALSE),
  ('f2000000-0000-4000-8000-000000602051', 'e2000000-0000-4000-8000-000006020005', 1, 'Para evitar repeticiones innecesarias de palabras', TRUE),
  ('f2000000-0000-4000-8000-000000602052', 'e2000000-0000-4000-8000-000006020005', 2, 'Para cambiar el final de la historia', FALSE),
  ('f2000000-0000-4000-8000-000000602053', 'e2000000-0000-4000-8000-000006020005', 3, 'Para alargar las páginas', FALSE),
  ('f2000000-0000-4000-8000-000000602061', 'e2000000-0000-4000-8000-000006020006', 1, 'Agradecer a la comunidad escolar y motivar hacia nuevas metas', TRUE),
  ('f2000000-0000-4000-8000-000000602062', 'e2000000-0000-4000-8000-000006020006', 2, 'Exigir que no haya más clases en el país', FALSE),
  ('f2000000-0000-4000-8000-000000602063', 'e2000000-0000-4000-8000-000006020006', 3, 'Leer una lista de compras', FALSE)
ON DUPLICATE KEY UPDATE texto = VALUES(texto), es_correcta = VALUES(es_correcta);

-- ----------------------------------------------------------------------
-- Paradas y Contenidos: CYT-6P-CLIMA (Ciencia y Tecnología: Energías Renovables y Cambio Climático)
-- ----------------------------------------------------------------------
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000006030001', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000006030002', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000006030003', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000006030004', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000006030005', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000006030006', 'RETO', '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('fa000000-0000-4000-8000-000006030001', 'ac000000-0000-4000-8000-000006030001', 1, 'El efecto invernadero y las causas del calentamiento global', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000006030002', 'ac000000-0000-4000-8000-000006030002', 1, 'Fuentes de energía renovable: Solar, eólica e hidráulica', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000006030003', 'ac000000-0000-4000-8000-000006030003', 1, 'La célula: Unidad fundamental de la vida vegetal y animal', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000006030004', 'ac000000-0000-4000-8000-000006030004', 1, 'Biotecnología tradicional: Fermentación de la chicha de jora', 10, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000006030005', 'ac000000-0000-4000-8000-000006030005', 1, 'Gestión integral de residuos sólidos: Las 5 Erres ecológicas', 12, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.'),
  ('fa000000-0000-4000-8000-000006030006', 'ac000000-0000-4000-8000-000006030006', 1, 'Proyecto de investigación científica escolar: Reforestación con Queñuales', 15, 'PUBLICADO', 'Aprende y resuelve junto a Rupi.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ca000000-0000-4000-8000-000006030001', 'b0000000-0000-4000-8000-000000000603', 'fa000000-0000-4000-8000-000006030001', 'd2000000-0000-4000-8000-000000000603', 1, FALSE, 50, 18),
  ('ca000000-0000-4000-8000-000006030002', 'b0000000-0000-4000-8000-000000000603', 'fa000000-0000-4000-8000-000006030002', 'd2000000-0000-4000-8000-000000000603', 2, FALSE, 27, 32),
  ('ca000000-0000-4000-8000-000006030003', 'b0000000-0000-4000-8000-000000000603', 'fa000000-0000-4000-8000-000006030003', 'd2000000-0000-4000-8000-000000000603', 3, FALSE, 73, 46),
  ('ca000000-0000-4000-8000-000006030004', 'b0000000-0000-4000-8000-000000000603', 'fa000000-0000-4000-8000-000006030004', 'd2000000-0000-4000-8000-000000000603', 4, FALSE, 35, 60),
  ('ca000000-0000-4000-8000-000006030005', 'b0000000-0000-4000-8000-000000000603', 'fa000000-0000-4000-8000-000006030005', 'd2000000-0000-4000-8000-000000000603', 5, FALSE, 68, 74),
  ('ca000000-0000-4000-8000-000006030006', 'b0000000-0000-4000-8000-000000000603', 'fa000000-0000-4000-8000-000006030006', 'd2000000-0000-4000-8000-000000000603', 6, FALSE, 50, 88)
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id), mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo) VALUES
  ('bb000000-0000-4000-8000-000006030001', 'fa000000-0000-4000-8000-000006030001', 1, 'TEXTO', 'Gases como el CO2 y el metano atrapan el calor en la atmósfera. La quema excesiva de combustibles fósiles intensifica este efecto derritiendo los glaciares.'),
  ('bb000000-0000-4000-8000-000006030002', 'fa000000-0000-4000-8000-000006030002', 1, 'TEXTO', 'A diferencia del petróleo o carbón, las energías limpias no emiten gases contaminantes y son inagotables en nuestro territorio.'),
  ('bb000000-0000-4000-8000-000006030003', 'fa000000-0000-4000-8000-000006030003', 1, 'TEXTO', 'Todos los seres vivos están formados por células. La célula vegetal tiene pared celular y cloroplastos para la fotosíntesis; la célula animal no los tiene.'),
  ('bb000000-0000-4000-8000-000006030004', 'fa000000-0000-4000-8000-000006030004', 1, 'TEXTO', 'Nuestros antepasados usaban microorganismos benéficos (levaduras) para fermentar granos de maíz germinado, creando bebidas tradicionales y panes.'),
  ('bb000000-0000-4000-8000-000006030005', 'fa000000-0000-4000-8000-000006030005', 1, 'TEXTO', 'Reducir, Reutilizar, Reciclar, Rechazar y Recuperar son principios indispensables para erradicar los microplásticos de nuestros ríos y campos.'),
  ('bb000000-0000-4000-8000-000006030006', 'fa000000-0000-4000-8000-000006030006', 1, 'TEXTO', '¡Graduado con honores en Ciencia! Tu proyecto de siembra y cosecha de agua con queñuales asegura agua para las próximas generaciones.')
ON DUPLICATE KEY UPDATE texto_cuerpo = VALUES(texto_cuerpo);

INSERT INTO evaluacion_pregunta (id, version_actividad_id, numero_secuencia, tipo, enunciado, explicacion, puntos) VALUES
  ('e2000000-0000-4000-8000-000006030001', 'fa000000-0000-4000-8000-000006030001', 1, 'OPCION_MULTIPLE', '¿Qué consecuencia directa provoca el aumento del calentamiento global en los Andes peruanos?', 'El retroceso acelerado y desglaciación de nuestras cumbres nevadas.', 10),
  ('e2000000-0000-4000-8000-000006030002', 'fa000000-0000-4000-8000-000006030002', 1, 'OPCION_MULTIPLE', '¿Qué fuente renovable aprovecha la fuerza del viento para mover turbinas?', 'La energía eólica transforma el viento en energía eléctrica.', 10),
  ('e2000000-0000-4000-8000-000006030003', 'fa000000-0000-4000-8000-000006030003', 1, 'OPCION_MULTIPLE', '¿Qué organelo celular exclusivo de los vegetales realiza la fotosíntesis?', 'Los cloroplastos contienen clorofila para captar energía solar.', 10),
  ('e2000000-0000-4000-8000-000006030004', 'fa000000-0000-4000-8000-000006030004', 1, 'OPCION_MULTIPLE', '¿Qué microorganismos participan en el proceso biológico de fermentación?', 'Las levaduras y bacterias benéficas transforman los azúcares.', 10),
  ('e2000000-0000-4000-8000-000006030005', 'fa000000-0000-4000-8000-000006030005', 1, 'OPCION_MULTIPLE', '¿Qué acción ejemplifica el principio ecológico de "Reutilizar"?', 'Usar envases de vidrio varias veces para almacenar semillas.', 10),
  ('e2000000-0000-4000-8000-000006030006', 'fa000000-0000-4000-8000-000006030006', 1, 'OPCION_MULTIPLE', '¿Por qué el árbol de queñua es clave para la siembra de agua en las alturas?', 'Su corteza porosa capta la neblina y recarga los acuíferos subterráneos.', 10)
ON DUPLICATE KEY UPDATE enunciado = VALUES(enunciado), explicacion = VALUES(explicacion);

INSERT INTO evaluacion_opcion_respuesta (id, pregunta_id, numero_secuencia, texto, es_correcta) VALUES
  ('f2000000-0000-4000-8000-000000603011', 'e2000000-0000-4000-8000-000006030001', 1, 'El retroceso y derretimiento de los glaciares tropicales', TRUE),
  ('f2000000-0000-4000-8000-000000603012', 'e2000000-0000-4000-8000-000006030001', 2, 'El aumento de nieve perpetua', FALSE),
  ('f2000000-0000-4000-8000-000000603013', 'e2000000-0000-4000-8000-000006030001', 3, 'La disminución de la radiación solar', FALSE),
  ('f2000000-0000-4000-8000-000000603021', 'e2000000-0000-4000-8000-000006030002', 1, 'Energía eólica', TRUE),
  ('f2000000-0000-4000-8000-000000603022', 'e2000000-0000-4000-8000-000006030002', 2, 'Energía fósil', FALSE),
  ('f2000000-0000-4000-8000-000000603023', 'e2000000-0000-4000-8000-000006030002', 3, 'Energía geotérmica de carbón', FALSE),
  ('f2000000-0000-4000-8000-000000603031', 'e2000000-0000-4000-8000-000006030003', 1, 'Los cloroplastos', TRUE),
  ('f2000000-0000-4000-8000-000000603032', 'e2000000-0000-4000-8000-000006030003', 2, 'Los ribosomas animales', FALSE),
  ('f2000000-0000-4000-8000-000000603033', 'e2000000-0000-4000-8000-000006030003', 3, 'El aparato de Golgi', FALSE),
  ('f2000000-0000-4000-8000-000000603041', 'e2000000-0000-4000-8000-000006030004', 1, 'Las levaduras benéficas', TRUE),
  ('f2000000-0000-4000-8000-000000603042', 'e2000000-0000-4000-8000-000006030004', 2, 'Virus destructivos', FALSE),
  ('f2000000-0000-4000-8000-000000603043', 'e2000000-0000-4000-8000-000006030004', 3, 'Algas marinas fósiles', FALSE),
  ('f2000000-0000-4000-8000-000000603051', 'e2000000-0000-4000-8000-000006030005', 1, 'Usar frascos de vidrio vacíos como lapiceros o maceteros', TRUE),
  ('f2000000-0000-4000-8000-000000603052', 'e2000000-0000-4000-8000-000006030005', 2, 'Arrojar botellas al río', FALSE),
  ('f2000000-0000-4000-8000-000000603053', 'e2000000-0000-4000-8000-000006030005', 3, 'Comprar plástico descartable todos los días', FALSE),
  ('f2000000-0000-4000-8000-000000603061', 'e2000000-0000-4000-8000-000006030006', 1, 'Capta la humedad de la neblina y alimenta los manantiales de agua', TRUE),
  ('f2000000-0000-4000-8000-000000603062', 'e2000000-0000-4000-8000-000006030006', 2, 'Porque seca la tierra rápidamente', FALSE),
  ('f2000000-0000-4000-8000-000000603063', 'e2000000-0000-4000-8000-000006030006', 3, 'Porque no resiste el frío', FALSE)
ON DUPLICATE KEY UPDATE texto = VALUES(texto), es_correcta = VALUES(es_correcta);

-- ============================================================================
-- 4. INSCRIPCIONES Y PROGRESO INICIAL PARA EL ESTUDIANTE DEMO
-- ============================================================================

-- Inscripción en MAT-1P-INICIO
INSERT INTO aprendizaje_inscripcion_ruta (
  id, version_ruta_id, estudiante_id, estado, ultimo_nodo_visitado_id, ultima_visita_en, inscrito_en
) VALUES
  ('e0000000-0000-4000-8000-000000000101', 'b0000000-0000-4000-8000-000000000101', '11111111-1111-4111-8111-111111111111', 'ACTIVO', 'ca000000-0000-4000-8000-000001010001', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

-- Progreso en MAT-1P-INICIO (primer nodo DISPONIBLE, siguientes BLOQUEADO)
INSERT INTO aprendizaje_progreso_nodo (
  inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en, primer_acceso_en, completado_en
) VALUES
  ('e0000000-0000-4000-8000-000000000101', 'b0000000-0000-4000-8000-000000000101', 'ca000000-0000-4000-8000-000001010001', 'DISPONIBLE', UTC_TIMESTAMP(6), NULL, NULL),
  ('e0000000-0000-4000-8000-000000000101', 'b0000000-0000-4000-8000-000000000101', 'ca000000-0000-4000-8000-000001010002', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000101', 'b0000000-0000-4000-8000-000000000101', 'ca000000-0000-4000-8000-000001010003', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000101', 'b0000000-0000-4000-8000-000000000101', 'ca000000-0000-4000-8000-000001010004', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000101', 'b0000000-0000-4000-8000-000000000101', 'ca000000-0000-4000-8000-000001010005', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000101', 'b0000000-0000-4000-8000-000000000101', 'ca000000-0000-4000-8000-000001010006', 'BLOQUEADO', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE estado = VALUES(estado);

-- Inscripción en COM-1P-LETRAS
INSERT INTO aprendizaje_inscripcion_ruta (
  id, version_ruta_id, estudiante_id, estado, ultimo_nodo_visitado_id, ultima_visita_en, inscrito_en
) VALUES
  ('e0000000-0000-4000-8000-000000000102', 'b0000000-0000-4000-8000-000000000102', '11111111-1111-4111-8111-111111111111', 'ACTIVO', 'ca000000-0000-4000-8000-000001020001', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

-- Progreso en COM-1P-LETRAS (primer nodo DISPONIBLE, siguientes BLOQUEADO)
INSERT INTO aprendizaje_progreso_nodo (
  inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en, primer_acceso_en, completado_en
) VALUES
  ('e0000000-0000-4000-8000-000000000102', 'b0000000-0000-4000-8000-000000000102', 'ca000000-0000-4000-8000-000001020001', 'DISPONIBLE', UTC_TIMESTAMP(6), NULL, NULL),
  ('e0000000-0000-4000-8000-000000000102', 'b0000000-0000-4000-8000-000000000102', 'ca000000-0000-4000-8000-000001020002', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000102', 'b0000000-0000-4000-8000-000000000102', 'ca000000-0000-4000-8000-000001020003', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000102', 'b0000000-0000-4000-8000-000000000102', 'ca000000-0000-4000-8000-000001020004', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000102', 'b0000000-0000-4000-8000-000000000102', 'ca000000-0000-4000-8000-000001020005', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000102', 'b0000000-0000-4000-8000-000000000102', 'ca000000-0000-4000-8000-000001020006', 'BLOQUEADO', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE estado = VALUES(estado);

-- Inscripción en CYT-1P-EXPLOR
INSERT INTO aprendizaje_inscripcion_ruta (
  id, version_ruta_id, estudiante_id, estado, ultimo_nodo_visitado_id, ultima_visita_en, inscrito_en
) VALUES
  ('e0000000-0000-4000-8000-000000000103', 'b0000000-0000-4000-8000-000000000103', '11111111-1111-4111-8111-111111111111', 'ACTIVO', 'ca000000-0000-4000-8000-000001030001', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

-- Progreso en CYT-1P-EXPLOR (primer nodo DISPONIBLE, siguientes BLOQUEADO)
INSERT INTO aprendizaje_progreso_nodo (
  inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en, primer_acceso_en, completado_en
) VALUES
  ('e0000000-0000-4000-8000-000000000103', 'b0000000-0000-4000-8000-000000000103', 'ca000000-0000-4000-8000-000001030001', 'DISPONIBLE', UTC_TIMESTAMP(6), NULL, NULL),
  ('e0000000-0000-4000-8000-000000000103', 'b0000000-0000-4000-8000-000000000103', 'ca000000-0000-4000-8000-000001030002', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000103', 'b0000000-0000-4000-8000-000000000103', 'ca000000-0000-4000-8000-000001030003', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000103', 'b0000000-0000-4000-8000-000000000103', 'ca000000-0000-4000-8000-000001030004', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000103', 'b0000000-0000-4000-8000-000000000103', 'ca000000-0000-4000-8000-000001030005', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000103', 'b0000000-0000-4000-8000-000000000103', 'ca000000-0000-4000-8000-000001030006', 'BLOQUEADO', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE estado = VALUES(estado);

-- Inscripción en COM-3P-MITOS
INSERT INTO aprendizaje_inscripcion_ruta (
  id, version_ruta_id, estudiante_id, estado, ultimo_nodo_visitado_id, ultima_visita_en, inscrito_en
) VALUES
  ('e0000000-0000-4000-8000-000000000302', 'b0000000-0000-4000-8000-000000000302', '11111111-1111-4111-8111-111111111111', 'ACTIVO', 'ca000000-0000-4000-8000-000003020001', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

-- Progreso en COM-3P-MITOS (primer nodo DISPONIBLE, siguientes BLOQUEADO)
INSERT INTO aprendizaje_progreso_nodo (
  inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en, primer_acceso_en, completado_en
) VALUES
  ('e0000000-0000-4000-8000-000000000302', 'b0000000-0000-4000-8000-000000000302', 'ca000000-0000-4000-8000-000003020001', 'DISPONIBLE', UTC_TIMESTAMP(6), NULL, NULL),
  ('e0000000-0000-4000-8000-000000000302', 'b0000000-0000-4000-8000-000000000302', 'ca000000-0000-4000-8000-000003020002', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000302', 'b0000000-0000-4000-8000-000000000302', 'ca000000-0000-4000-8000-000003020003', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000302', 'b0000000-0000-4000-8000-000000000302', 'ca000000-0000-4000-8000-000003020004', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000302', 'b0000000-0000-4000-8000-000000000302', 'ca000000-0000-4000-8000-000003020005', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000302', 'b0000000-0000-4000-8000-000000000302', 'ca000000-0000-4000-8000-000003020006', 'BLOQUEADO', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE estado = VALUES(estado);

-- Inscripción en CYT-3P-ENERGIA
INSERT INTO aprendizaje_inscripcion_ruta (
  id, version_ruta_id, estudiante_id, estado, ultimo_nodo_visitado_id, ultima_visita_en, inscrito_en
) VALUES
  ('e0000000-0000-4000-8000-000000000303', 'b0000000-0000-4000-8000-000000000303', '11111111-1111-4111-8111-111111111111', 'ACTIVO', 'ca000000-0000-4000-8000-000003030001', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

-- Progreso en CYT-3P-ENERGIA (primer nodo DISPONIBLE, siguientes BLOQUEADO)
INSERT INTO aprendizaje_progreso_nodo (
  inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en, primer_acceso_en, completado_en
) VALUES
  ('e0000000-0000-4000-8000-000000000303', 'b0000000-0000-4000-8000-000000000303', 'ca000000-0000-4000-8000-000003030001', 'DISPONIBLE', UTC_TIMESTAMP(6), NULL, NULL),
  ('e0000000-0000-4000-8000-000000000303', 'b0000000-0000-4000-8000-000000000303', 'ca000000-0000-4000-8000-000003030002', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000303', 'b0000000-0000-4000-8000-000000000303', 'ca000000-0000-4000-8000-000003030003', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000303', 'b0000000-0000-4000-8000-000000000303', 'ca000000-0000-4000-8000-000003030004', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000303', 'b0000000-0000-4000-8000-000000000303', 'ca000000-0000-4000-8000-000003030005', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000303', 'b0000000-0000-4000-8000-000000000303', 'ca000000-0000-4000-8000-000003030006', 'BLOQUEADO', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE estado = VALUES(estado);

-- Inscripción en MAT-4P-FRACC
INSERT INTO aprendizaje_inscripcion_ruta (
  id, version_ruta_id, estudiante_id, estado, ultimo_nodo_visitado_id, ultima_visita_en, inscrito_en
) VALUES
  ('e0000000-0000-4000-8000-000000000401', 'b0000000-0000-4000-8000-000000000401', '11111111-1111-4111-8111-111111111111', 'ACTIVO', 'ca000000-0000-4000-8000-000004010001', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

-- Progreso en MAT-4P-FRACC (primer nodo DISPONIBLE, siguientes BLOQUEADO)
INSERT INTO aprendizaje_progreso_nodo (
  inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en, primer_acceso_en, completado_en
) VALUES
  ('e0000000-0000-4000-8000-000000000401', 'b0000000-0000-4000-8000-000000000401', 'ca000000-0000-4000-8000-000004010001', 'DISPONIBLE', UTC_TIMESTAMP(6), NULL, NULL),
  ('e0000000-0000-4000-8000-000000000401', 'b0000000-0000-4000-8000-000000000401', 'ca000000-0000-4000-8000-000004010002', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000401', 'b0000000-0000-4000-8000-000000000401', 'ca000000-0000-4000-8000-000004010003', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000401', 'b0000000-0000-4000-8000-000000000401', 'ca000000-0000-4000-8000-000004010004', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000401', 'b0000000-0000-4000-8000-000000000401', 'ca000000-0000-4000-8000-000004010005', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000401', 'b0000000-0000-4000-8000-000000000401', 'ca000000-0000-4000-8000-000004010006', 'BLOQUEADO', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE estado = VALUES(estado);

-- Inscripción en COM-4P-TEXTOS
INSERT INTO aprendizaje_inscripcion_ruta (
  id, version_ruta_id, estudiante_id, estado, ultimo_nodo_visitado_id, ultima_visita_en, inscrito_en
) VALUES
  ('e0000000-0000-4000-8000-000000000402', 'b0000000-0000-4000-8000-000000000402', '11111111-1111-4111-8111-111111111111', 'ACTIVO', 'ca000000-0000-4000-8000-000004020001', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

-- Progreso en COM-4P-TEXTOS (primer nodo DISPONIBLE, siguientes BLOQUEADO)
INSERT INTO aprendizaje_progreso_nodo (
  inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en, primer_acceso_en, completado_en
) VALUES
  ('e0000000-0000-4000-8000-000000000402', 'b0000000-0000-4000-8000-000000000402', 'ca000000-0000-4000-8000-000004020001', 'DISPONIBLE', UTC_TIMESTAMP(6), NULL, NULL),
  ('e0000000-0000-4000-8000-000000000402', 'b0000000-0000-4000-8000-000000000402', 'ca000000-0000-4000-8000-000004020002', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000402', 'b0000000-0000-4000-8000-000000000402', 'ca000000-0000-4000-8000-000004020003', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000402', 'b0000000-0000-4000-8000-000000000402', 'ca000000-0000-4000-8000-000004020004', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000402', 'b0000000-0000-4000-8000-000000000402', 'ca000000-0000-4000-8000-000004020005', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000402', 'b0000000-0000-4000-8000-000000000402', 'ca000000-0000-4000-8000-000004020006', 'BLOQUEADO', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE estado = VALUES(estado);

-- Inscripción en CYT-4P-ECOSIS
INSERT INTO aprendizaje_inscripcion_ruta (
  id, version_ruta_id, estudiante_id, estado, ultimo_nodo_visitado_id, ultima_visita_en, inscrito_en
) VALUES
  ('e0000000-0000-4000-8000-000000000403', 'b0000000-0000-4000-8000-000000000403', '11111111-1111-4111-8111-111111111111', 'ACTIVO', 'ca000000-0000-4000-8000-000004030001', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

-- Progreso en CYT-4P-ECOSIS (primer nodo DISPONIBLE, siguientes BLOQUEADO)
INSERT INTO aprendizaje_progreso_nodo (
  inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en, primer_acceso_en, completado_en
) VALUES
  ('e0000000-0000-4000-8000-000000000403', 'b0000000-0000-4000-8000-000000000403', 'ca000000-0000-4000-8000-000004030001', 'DISPONIBLE', UTC_TIMESTAMP(6), NULL, NULL),
  ('e0000000-0000-4000-8000-000000000403', 'b0000000-0000-4000-8000-000000000403', 'ca000000-0000-4000-8000-000004030002', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000403', 'b0000000-0000-4000-8000-000000000403', 'ca000000-0000-4000-8000-000004030003', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000403', 'b0000000-0000-4000-8000-000000000403', 'ca000000-0000-4000-8000-000004030004', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000403', 'b0000000-0000-4000-8000-000000000403', 'ca000000-0000-4000-8000-000004030005', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000403', 'b0000000-0000-4000-8000-000000000403', 'ca000000-0000-4000-8000-000004030006', 'BLOQUEADO', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE estado = VALUES(estado);

-- Inscripción en MAT-5P-DECIM
INSERT INTO aprendizaje_inscripcion_ruta (
  id, version_ruta_id, estudiante_id, estado, ultimo_nodo_visitado_id, ultima_visita_en, inscrito_en
) VALUES
  ('e0000000-0000-4000-8000-000000000501', 'b0000000-0000-4000-8000-000000000501', '11111111-1111-4111-8111-111111111111', 'ACTIVO', 'ca000000-0000-4000-8000-000005010001', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

-- Progreso en MAT-5P-DECIM (primer nodo DISPONIBLE, siguientes BLOQUEADO)
INSERT INTO aprendizaje_progreso_nodo (
  inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en, primer_acceso_en, completado_en
) VALUES
  ('e0000000-0000-4000-8000-000000000501', 'b0000000-0000-4000-8000-000000000501', 'ca000000-0000-4000-8000-000005010001', 'DISPONIBLE', UTC_TIMESTAMP(6), NULL, NULL),
  ('e0000000-0000-4000-8000-000000000501', 'b0000000-0000-4000-8000-000000000501', 'ca000000-0000-4000-8000-000005010002', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000501', 'b0000000-0000-4000-8000-000000000501', 'ca000000-0000-4000-8000-000005010003', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000501', 'b0000000-0000-4000-8000-000000000501', 'ca000000-0000-4000-8000-000005010004', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000501', 'b0000000-0000-4000-8000-000000000501', 'ca000000-0000-4000-8000-000005010005', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000501', 'b0000000-0000-4000-8000-000000000501', 'ca000000-0000-4000-8000-000005010006', 'BLOQUEADO', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE estado = VALUES(estado);

-- Inscripción en COM-5P-ARGUM
INSERT INTO aprendizaje_inscripcion_ruta (
  id, version_ruta_id, estudiante_id, estado, ultimo_nodo_visitado_id, ultima_visita_en, inscrito_en
) VALUES
  ('e0000000-0000-4000-8000-000000000502', 'b0000000-0000-4000-8000-000000000502', '11111111-1111-4111-8111-111111111111', 'ACTIVO', 'ca000000-0000-4000-8000-000005020001', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

-- Progreso en COM-5P-ARGUM (primer nodo DISPONIBLE, siguientes BLOQUEADO)
INSERT INTO aprendizaje_progreso_nodo (
  inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en, primer_acceso_en, completado_en
) VALUES
  ('e0000000-0000-4000-8000-000000000502', 'b0000000-0000-4000-8000-000000000502', 'ca000000-0000-4000-8000-000005020001', 'DISPONIBLE', UTC_TIMESTAMP(6), NULL, NULL),
  ('e0000000-0000-4000-8000-000000000502', 'b0000000-0000-4000-8000-000000000502', 'ca000000-0000-4000-8000-000005020002', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000502', 'b0000000-0000-4000-8000-000000000502', 'ca000000-0000-4000-8000-000005020003', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000502', 'b0000000-0000-4000-8000-000000000502', 'ca000000-0000-4000-8000-000005020004', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000502', 'b0000000-0000-4000-8000-000000000502', 'ca000000-0000-4000-8000-000005020005', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000502', 'b0000000-0000-4000-8000-000000000502', 'ca000000-0000-4000-8000-000005020006', 'BLOQUEADO', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE estado = VALUES(estado);

-- Inscripción en CYT-5P-CUERPO
INSERT INTO aprendizaje_inscripcion_ruta (
  id, version_ruta_id, estudiante_id, estado, ultimo_nodo_visitado_id, ultima_visita_en, inscrito_en
) VALUES
  ('e0000000-0000-4000-8000-000000000503', 'b0000000-0000-4000-8000-000000000503', '11111111-1111-4111-8111-111111111111', 'ACTIVO', 'ca000000-0000-4000-8000-000005030001', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

-- Progreso en CYT-5P-CUERPO (primer nodo DISPONIBLE, siguientes BLOQUEADO)
INSERT INTO aprendizaje_progreso_nodo (
  inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en, primer_acceso_en, completado_en
) VALUES
  ('e0000000-0000-4000-8000-000000000503', 'b0000000-0000-4000-8000-000000000503', 'ca000000-0000-4000-8000-000005030001', 'DISPONIBLE', UTC_TIMESTAMP(6), NULL, NULL),
  ('e0000000-0000-4000-8000-000000000503', 'b0000000-0000-4000-8000-000000000503', 'ca000000-0000-4000-8000-000005030002', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000503', 'b0000000-0000-4000-8000-000000000503', 'ca000000-0000-4000-8000-000005030003', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000503', 'b0000000-0000-4000-8000-000000000503', 'ca000000-0000-4000-8000-000005030004', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000503', 'b0000000-0000-4000-8000-000000000503', 'ca000000-0000-4000-8000-000005030005', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000503', 'b0000000-0000-4000-8000-000000000503', 'ca000000-0000-4000-8000-000005030006', 'BLOQUEADO', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE estado = VALUES(estado);

-- Inscripción en MAT-6P-PROPOR
INSERT INTO aprendizaje_inscripcion_ruta (
  id, version_ruta_id, estudiante_id, estado, ultimo_nodo_visitado_id, ultima_visita_en, inscrito_en
) VALUES
  ('e0000000-0000-4000-8000-000000000601', 'b0000000-0000-4000-8000-000000000601', '11111111-1111-4111-8111-111111111111', 'ACTIVO', 'ca000000-0000-4000-8000-000006010001', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

-- Progreso en MAT-6P-PROPOR (primer nodo DISPONIBLE, siguientes BLOQUEADO)
INSERT INTO aprendizaje_progreso_nodo (
  inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en, primer_acceso_en, completado_en
) VALUES
  ('e0000000-0000-4000-8000-000000000601', 'b0000000-0000-4000-8000-000000000601', 'ca000000-0000-4000-8000-000006010001', 'DISPONIBLE', UTC_TIMESTAMP(6), NULL, NULL),
  ('e0000000-0000-4000-8000-000000000601', 'b0000000-0000-4000-8000-000000000601', 'ca000000-0000-4000-8000-000006010002', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000601', 'b0000000-0000-4000-8000-000000000601', 'ca000000-0000-4000-8000-000006010003', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000601', 'b0000000-0000-4000-8000-000000000601', 'ca000000-0000-4000-8000-000006010004', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000601', 'b0000000-0000-4000-8000-000000000601', 'ca000000-0000-4000-8000-000006010005', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000601', 'b0000000-0000-4000-8000-000000000601', 'ca000000-0000-4000-8000-000006010006', 'BLOQUEADO', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE estado = VALUES(estado);

-- Inscripción en COM-6P-CRONIC
INSERT INTO aprendizaje_inscripcion_ruta (
  id, version_ruta_id, estudiante_id, estado, ultimo_nodo_visitado_id, ultima_visita_en, inscrito_en
) VALUES
  ('e0000000-0000-4000-8000-000000000602', 'b0000000-0000-4000-8000-000000000602', '11111111-1111-4111-8111-111111111111', 'ACTIVO', 'ca000000-0000-4000-8000-000006020001', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

-- Progreso en COM-6P-CRONIC (primer nodo DISPONIBLE, siguientes BLOQUEADO)
INSERT INTO aprendizaje_progreso_nodo (
  inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en, primer_acceso_en, completado_en
) VALUES
  ('e0000000-0000-4000-8000-000000000602', 'b0000000-0000-4000-8000-000000000602', 'ca000000-0000-4000-8000-000006020001', 'DISPONIBLE', UTC_TIMESTAMP(6), NULL, NULL),
  ('e0000000-0000-4000-8000-000000000602', 'b0000000-0000-4000-8000-000000000602', 'ca000000-0000-4000-8000-000006020002', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000602', 'b0000000-0000-4000-8000-000000000602', 'ca000000-0000-4000-8000-000006020003', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000602', 'b0000000-0000-4000-8000-000000000602', 'ca000000-0000-4000-8000-000006020004', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000602', 'b0000000-0000-4000-8000-000000000602', 'ca000000-0000-4000-8000-000006020005', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000602', 'b0000000-0000-4000-8000-000000000602', 'ca000000-0000-4000-8000-000006020006', 'BLOQUEADO', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE estado = VALUES(estado);

-- Inscripción en CYT-6P-CLIMA
INSERT INTO aprendizaje_inscripcion_ruta (
  id, version_ruta_id, estudiante_id, estado, ultimo_nodo_visitado_id, ultima_visita_en, inscrito_en
) VALUES
  ('e0000000-0000-4000-8000-000000000603', 'b0000000-0000-4000-8000-000000000603', '11111111-1111-4111-8111-111111111111', 'ACTIVO', 'ca000000-0000-4000-8000-000006030001', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

-- Progreso en CYT-6P-CLIMA (primer nodo DISPONIBLE, siguientes BLOQUEADO)
INSERT INTO aprendizaje_progreso_nodo (
  inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en, primer_acceso_en, completado_en
) VALUES
  ('e0000000-0000-4000-8000-000000000603', 'b0000000-0000-4000-8000-000000000603', 'ca000000-0000-4000-8000-000006030001', 'DISPONIBLE', UTC_TIMESTAMP(6), NULL, NULL),
  ('e0000000-0000-4000-8000-000000000603', 'b0000000-0000-4000-8000-000000000603', 'ca000000-0000-4000-8000-000006030002', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000603', 'b0000000-0000-4000-8000-000000000603', 'ca000000-0000-4000-8000-000006030003', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000603', 'b0000000-0000-4000-8000-000000000603', 'ca000000-0000-4000-8000-000006030004', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000603', 'b0000000-0000-4000-8000-000000000603', 'ca000000-0000-4000-8000-000006030005', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000603', 'b0000000-0000-4000-8000-000000000603', 'ca000000-0000-4000-8000-000006030006', 'BLOQUEADO', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE estado = VALUES(estado);
