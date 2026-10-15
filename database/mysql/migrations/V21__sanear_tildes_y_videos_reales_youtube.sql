-- =============================================================================
-- Migración V21: Saneamiento de Tildes, Caracteres y Videos Educativos Reales de YouTube
-- Garantiza enlaces pertinentes de YouTube acordes a los temas de los roadmaps
-- y elimina caracteres corruptos (mojibake) en toda la base de datos.
-- =============================================================================

USE rupi;
SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. Saneamiento de tildes y textos en recurso_archivo_multimedia
-- -----------------------------------------------------------------------------
UPDATE recurso_archivo_multimedia
SET texto_accesibilidad = REPLACE(texto_accesibilidad, 'did??ctico', 'didáctico')
WHERE texto_accesibilidad LIKE '%did??ctico%';

UPDATE recurso_archivo_multimedia
SET texto_accesibilidad = REPLACE(texto_accesibilidad, 'Tri???ngulos', 'Triángulos')
WHERE texto_accesibilidad LIKE '%Tri???ngulos%';

UPDATE recurso_archivo_multimedia
SET texto_accesibilidad = REPLACE(texto_accesibilidad, 'cuadril???teros', 'cuadriláteros')
WHERE texto_accesibilidad LIKE '%cuadril???teros%';

UPDATE recurso_archivo_multimedia
SET texto_accesibilidad = REPLACE(texto_accesibilidad, 'Biotecnolog???a', 'Biotecnología')
WHERE texto_accesibilidad LIKE '%Biotecnolog???a%';

UPDATE recurso_archivo_multimedia
SET texto_accesibilidad = REPLACE(texto_accesibilidad, 'Fermentaci???n', 'Fermentación')
WHERE texto_accesibilidad LIKE '%Fermentaci???n%';

UPDATE recurso_archivo_multimedia
SET texto_accesibilidad = REPLACE(texto_accesibilidad, 'cr???nica period???stica', 'crónica periodística')
WHERE texto_accesibilidad LIKE '%cr???nica period???stica%';

UPDATE recurso_archivo_multimedia
SET texto_accesibilidad = REPLACE(texto_accesibilidad, 'entrevista period???stica', 'entrevista periodística')
WHERE texto_accesibilidad LIKE '%entrevista period???stica%';

UPDATE recurso_archivo_multimedia
SET texto_accesibilidad = REPLACE(texto_accesibilidad, 'coraz???n', 'corazón')
WHERE texto_accesibilidad LIKE '%coraz???n%';

UPDATE recurso_archivo_multimedia
SET texto_accesibilidad = REPLACE(texto_accesibilidad, 'sangu???neos', 'sanguíneos')
WHERE texto_accesibilidad LIKE '%sangu???neos%';

UPDATE recurso_archivo_multimedia
SET texto_accesibilidad = REPLACE(texto_accesibilidad, 'Misi???n Guardi???n', 'Misión Guardián')
WHERE texto_accesibilidad LIKE '%Misi???n Guardi???n%';

UPDATE recurso_archivo_multimedia
SET texto_accesibilidad = REPLACE(texto_accesibilidad, 'conservaci???n', 'conservación')
WHERE texto_accesibilidad LIKE '%conservaci???n%';

UPDATE recurso_archivo_multimedia
SET texto_accesibilidad = REPLACE(texto_accesibilidad, 'cart???n', 'cartón')
WHERE texto_accesibilidad LIKE '%cart???n%';

UPDATE recurso_archivo_multimedia
SET texto_accesibilidad = REPLACE(texto_accesibilidad, 'N???meros', 'Números')
WHERE texto_accesibilidad LIKE '%N???meros%';

UPDATE recurso_archivo_multimedia
SET texto_accesibilidad = REPLACE(texto_accesibilidad, '???rbol y el ???guila', 'Árbol y el Águila')
WHERE texto_accesibilidad LIKE '%???rbol y el ???guila%';

UPDATE recurso_archivo_multimedia
SET texto_accesibilidad = REPLACE(texto_accesibilidad, 'vicu???a', 'vicuña')
WHERE texto_accesibilidad LIKE '%vicu???a%';

UPDATE recurso_archivo_multimedia
SET texto_accesibilidad = REPLACE(texto_accesibilidad, '???Arriba', '¿Arriba')
WHERE texto_accesibilidad LIKE '%???Arriba%';

UPDATE recurso_archivo_multimedia
SET nombre_original = 'Las figuras geométricas planas | Vocabulario de Primaria'
WHERE nombre_original LIKE '%geom%tricas%';

UPDATE recurso_archivo_multimedia
SET nombre_original = 'Comprensión lectora y textos narrativos'
WHERE nombre_original LIKE '%Comprensi%n lectora%';

UPDATE recurso_archivo_multimedia
SET nombre_original = 'Los Números del 1 al 100 - Conteo y Matemáticas'
WHERE nombre_original LIKE '%Los N%meros del 1 al 100%';

UPDATE recurso_archivo_multimedia
SET nombre_original = 'Parada 1: Cuentos andinos y tradición oral viva'
WHERE nombre_original LIKE '%Cuentos andinos y tradic% viva%';

UPDATE recurso_archivo_multimedia
SET nombre_original = REPLACE(nombre_original, 'tradici?n', 'tradición'),
    nombre_original = REPLACE(nombre_original, 'Comprensi?n', 'Comprensión'),
    nombre_original = REPLACE(nombre_original, 'Poes?a', 'Poesía'),
    nombre_original = REPLACE(nombre_original, 'ni?os', 'niños'),
    nombre_original = REPLACE(nombre_original, 'Multiplicaci?n', 'Multiplicación'),
    nombre_original = REPLACE(nombre_original, 'num?ricos', 'numéricos'),
    nombre_original = REPLACE(nombre_original, 'geom?tricas', 'geométricas'),
    nombre_original = REPLACE(nombre_original, 'N?meros', 'Números'),
    nombre_original = REPLACE(nombre_original, 'Matem?ticas', 'Matemáticas')
WHERE nombre_original LIKE '%?%';

UPDATE aprendizaje_version_actividad
SET titulo = 'Parada 1: Cuentos andinos y tradición oral viva'
WHERE titulo LIKE '%Cuentos andinos y tradic% viva%';

UPDATE aprendizaje_version_actividad
SET titulo = 'Parada 1: Números mágicos'
WHERE titulo LIKE '%Numeros magicos%';

UPDATE aprendizaje_bloque_contenido
SET texto_accesibilidad = REPLACE(texto_accesibilidad, 'did??ctico', 'didáctico')
WHERE texto_accesibilidad LIKE '%did??ctico%';

-- -----------------------------------------------------------------------------
-- 2. Asignación de videos reales pertinentes según la temática en recurso_archivo_multimedia
-- Reemplaza videos genéricos de suma por videos acordes al tema de cada lección
-- -----------------------------------------------------------------------------

-- Geometría y formas
UPDATE recurso_archivo_multimedia
SET url_externa = 'https://www.youtube.com/watch?v=SXONzObzFk0',
    nombre_original = 'Las figuras geométricas planas | Vocabulario de Primaria'
WHERE url_externa = 'https://www.youtube.com/watch?v=oexd_Dfic_Q'
  AND (texto_accesibilidad LIKE '%figura%' OR texto_accesibilidad LIKE '%geom%' OR texto_accesibilidad LIKE '%Triángulo%' OR texto_accesibilidad LIKE '%cuadrilátero%' OR texto_accesibilidad LIKE '%círculo%' OR texto_accesibilidad LIKE '%arcilla%');

-- Longitud y medición
UPDATE recurso_archivo_multimedia
SET url_externa = 'https://www.youtube.com/watch?v=kzrplJ1jvko',
    nombre_original = 'La longitud y su unidad de medida: El metro | Happy Learning'
WHERE url_externa = 'https://www.youtube.com/watch?v=oexd_Dfic_Q'
  AND (texto_accesibilidad LIKE '%longitud%' OR texto_accesibilidad LIKE '%metro%' OR texto_accesibilidad LIKE '%medid%' OR texto_accesibilidad LIKE '%perímetro%');

-- Fracciones y repartos
UPDATE recurso_archivo_multimedia
SET url_externa = 'https://www.youtube.com/watch?v=c9c_8x6zTdA',
    nombre_original = 'Las Fracciones para niños | Smile and Learn'
WHERE url_externa = 'https://www.youtube.com/watch?v=oexd_Dfic_Q'
  AND (texto_accesibilidad LIKE '%fracci%' OR texto_accesibilidad LIKE '%partes iguales%' OR texto_accesibilidad LIKE '%repart%');

-- Multiplicación
UPDATE recurso_archivo_multimedia
SET url_externa = 'https://www.youtube.com/watch?v=YFtEaVw5k1A',
    nombre_original = 'La Multiplicación | Aprender a multiplicar | Happy Learning'
WHERE url_externa = 'https://www.youtube.com/watch?v=oexd_Dfic_Q'
  AND (texto_accesibilidad LIKE '%multiplic%' OR texto_accesibilidad LIKE '%canasta%');

-- Patrones y secuencias
UPDATE recurso_archivo_multimedia
SET url_externa = 'https://www.youtube.com/watch?v=dzgnqza-2cU',
    nombre_original = 'Secuencias y patrones numéricos para primaria'
WHERE url_externa = 'https://www.youtube.com/watch?v=oexd_Dfic_Q'
  AND (texto_accesibilidad LIKE '%patr%' OR texto_accesibilidad LIKE '%secuencia%' OR texto_accesibilidad LIKE '%quipu%');

-- Ciencias: Cuerpo humano y salud
UPDATE recurso_archivo_multimedia
SET url_externa = 'https://www.youtube.com/watch?v=pNu_pU_G1H8',
    nombre_original = 'El cuerpo humano para niños | Smile and Learn'
WHERE url_externa = 'https://www.youtube.com/watch?v=oexd_Dfic_Q'
  AND (texto_accesibilidad LIKE '%cuerpo%' OR texto_accesibilidad LIKE '%circulatorio%' OR texto_accesibilidad LIKE '%respiratorio%' OR texto_accesibilidad LIKE '%digestivo%' OR texto_accesibilidad LIKE '%corazón%' OR texto_accesibilidad LIKE '%salud%');

-- Ciencias: Ecosistemas, agua y medio ambiente
UPDATE recurso_archivo_multimedia
SET url_externa = 'https://www.youtube.com/watch?v=SsRdLoYOMQA',
    nombre_original = '¡Aprende sobre los Ecosistemas! | Happy Learning'
WHERE url_externa = 'https://www.youtube.com/watch?v=oexd_Dfic_Q'
  AND (texto_accesibilidad LIKE '%ecosistema%' OR texto_accesibilidad LIKE '%agua%' OR texto_accesibilidad LIKE '%solar%' OR texto_accesibilidad LIKE '%chicha%' OR texto_accesibilidad LIKE '%ambiente%' OR texto_accesibilidad LIKE '%Pampa%');

-- Ciencias: Plantas y biohuerto
UPDATE recurso_archivo_multimedia
SET url_externa = 'https://www.youtube.com/watch?v=ThbSPNlRz0o',
    nombre_original = 'El reino de las plantas y su clasificación | Happy Learning'
WHERE url_externa = 'https://www.youtube.com/watch?v=oexd_Dfic_Q'
  AND (texto_accesibilidad LIKE '%planta%' OR texto_accesibilidad LIKE '%biohuerto%' OR texto_accesibilidad LIKE '%flora%');

-- Comunicación: Lectura, crónicas, entrevistas y textos
UPDATE recurso_archivo_multimedia
SET url_externa = 'https://www.youtube.com/watch?v=pCyPzdzrvEI',
    nombre_original = 'Comprensión lectora y textos narrativos'
WHERE url_externa = 'https://www.youtube.com/watch?v=oexd_Dfic_Q'
  AND (texto_accesibilidad LIKE '%crónica%' OR texto_accesibilidad LIKE '%entrevista%' OR texto_accesibilidad LIKE '%afiche%' OR texto_accesibilidad LIKE '%cuento familiar%' OR texto_accesibilidad LIKE '%noticia%' OR texto_accesibilidad LIKE '%lectura%');

-- Comunicación: Cuentos y leyendas andinas
UPDATE recurso_archivo_multimedia
SET url_externa = 'https://www.youtube.com/watch?v=K7Bvi6zMDWk',
    nombre_original = 'Cuento andino: El zorro y el cuy'
WHERE url_externa = 'https://www.youtube.com/watch?v=oexd_Dfic_Q'
  AND (texto_accesibilidad LIKE '%cuento%' OR texto_accesibilidad LIKE '%leyenda%' OR texto_accesibilidad LIKE '%fábula%' OR texto_accesibilidad LIKE '%zorro%');

-- -----------------------------------------------------------------------------
-- 3. Videos reales de YouTube para las rutas comunitarias del Prof. Carlos
-- -----------------------------------------------------------------------------

-- Ruta 1: Taller de Cuentos y Mitos Andinos
-- Paradas 2 a 6
INSERT INTO recurso_archivo_multimedia (id, creado_por, url_externa, nombre_original, tipo_mime, texto_accesibilidad) VALUES
  ('ec000001-0000-4000-8000-000003020002', '22222222-2222-4222-8222-222222222222', 'https://www.youtube.com/watch?v=k5q6G3J3fO8', 'Cuentos andinos y tradición oral viva', 'video/mp4', 'Video didáctico de YouTube para El Zorro y el Cóndor: Personajes y Acciones'),
  ('ec000001-0000-4000-8000-000003020003', '22222222-2222-4222-8222-222222222222', 'https://www.youtube.com/watch?v=pCyPzdzrvEI', 'Comprensión lectora y textos narrativos', 'video/mp4', 'Video didáctico de YouTube para Palabras que Describen: Adjetivos en Nuestras Historias'),
  ('ec000001-0000-4000-8000-000003020004', '22222222-2222-4222-8222-222222222222', 'https://www.youtube.com/watch?v=K7Bvi6zMDWk', 'Cuento andino: El zorro y el cuy', 'video/mp4', 'Video didáctico de YouTube para La Rueda de la Narración: Inicio, Nudo y Desenlace'),
  ('ec000001-0000-4000-8000-000003020005', '22222222-2222-4222-8222-222222222222', 'https://www.youtube.com/watch?v=3R4QxN37z1E', 'Las Rimas y la Poesía para niños | Smile and Learn', 'video/mp4', 'Video didáctico de YouTube para Rimas y Retahílas Quechuas y Castellanas'),
  ('ec000001-0000-4000-8000-000003020006', '22222222-2222-4222-8222-222222222222', 'https://www.youtube.com/watch?v=k5q6G3J3fO8', 'Cuentos andinos y tradición oral viva', 'video/mp4', 'Video didáctico de YouTube para Gran Retablo de Cuentos Escolares')
ON DUPLICATE KEY UPDATE url_externa = VALUES(url_externa), nombre_original = VALUES(nombre_original), texto_accesibilidad = VALUES(texto_accesibilidad);

INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo, archivo_multimedia_id, texto_accesibilidad) VALUES
  ('ed000001-0000-4000-8000-000003020002', 'ea000000-0000-4000-8000-000003020002', 50, 'VIDEO', 'Cuentos andinos y tradición oral viva', 'ec000001-0000-4000-8000-000003020002', 'Video didáctico de YouTube para El Zorro y el Cóndor'),
  ('ed000001-0000-4000-8000-000003020003', 'ea000000-0000-4000-8000-000003020003', 50, 'VIDEO', 'Comprensión lectora y textos narrativos', 'ec000001-0000-4000-8000-000003020003', 'Video didáctico de YouTube para Adjetivos'),
  ('ed000001-0000-4000-8000-000003020004', 'ea000000-0000-4000-8000-000003020004', 50, 'VIDEO', 'Cuento andino: El zorro y el cuy', 'ec000001-0000-4000-8000-000003020004', 'Video didáctico de YouTube para La Rueda de la Narración'),
  ('ed000001-0000-4000-8000-000003020005', 'ea000000-0000-4000-8000-000003020005', 50, 'VIDEO', 'Las Rimas y la Poesía para niños | Smile and Learn', 'ec000001-0000-4000-8000-000003020005', 'Video didáctico de YouTube para Rimas y Retahílas'),
  ('ed000001-0000-4000-8000-000003020006', 'ea000000-0000-4000-8000-000003020006', 50, 'VIDEO', 'Cuentos andinos y tradición oral viva', 'ec000001-0000-4000-8000-000003020006', 'Video didáctico de YouTube para Gran Retablo')
ON DUPLICATE KEY UPDATE texto_cuerpo = VALUES(texto_cuerpo), archivo_multimedia_id = VALUES(archivo_multimedia_id), texto_accesibilidad = VALUES(texto_accesibilidad);

-- Ruta 2: Retos Matemáticos y Geometría en Ayacucho
-- Paradas 2 a 6
INSERT INTO recurso_archivo_multimedia (id, creado_por, url_externa, nombre_original, tipo_mime, texto_accesibilidad) VALUES
  ('ec000002-0000-4000-8000-000003010002', '22222222-2222-4222-8222-222222222222', 'https://www.youtube.com/watch?v=dzgnqza-2cU', 'Secuencias y patrones numéricos para primaria', 'video/mp4', 'Video didáctico de YouTube para Patrones y Simetría en los Tejidos'),
  ('ec000002-0000-4000-8000-000003010003', '22222222-2222-4222-8222-222222222222', 'https://www.youtube.com/watch?v=YFtEaVw5k1A', 'La Multiplicación | Aprender a multiplicar | Happy Learning', 'video/mp4', 'Video didáctico de YouTube para Multiplicamos Canastas de Frutas'),
  ('ec000002-0000-4000-8000-000003010004', '22222222-2222-4222-8222-222222222222', 'https://www.youtube.com/watch?v=kzrplJ1jvko', 'La longitud y su unidad de medida: El metro | Happy Learning', 'video/mp4', 'Video didáctico de YouTube para Medimos Longitudes en el Salón'),
  ('ec000002-0000-4000-8000-000003010005', '22222222-2222-4222-8222-222222222222', 'https://www.youtube.com/watch?v=c9c_8x6zTdA', 'Las Fracciones para niños | Smile and Learn', 'video/mp4', 'Video didáctico de YouTube para Fracciones en la Repartición del Pan de Feria'),
  ('ec000002-0000-4000-8000-000003010006', '22222222-2222-4222-8222-222222222222', 'https://www.youtube.com/watch?v=SXONzObzFk0', 'Las figuras geométricas planas | Vocabulario de Primaria', 'video/mp4', 'Video didáctico de YouTube para Desafío del Retablo Matemático')
ON DUPLICATE KEY UPDATE url_externa = VALUES(url_externa), nombre_original = VALUES(nombre_original), texto_accesibilidad = VALUES(texto_accesibilidad);

INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo, archivo_multimedia_id, texto_accesibilidad) VALUES
  ('ed000002-0000-4000-8000-000003010002', 'fa000000-0000-4000-8000-000003010002', 50, 'VIDEO', 'Secuencias y patrones numéricos para primaria', 'ec000002-0000-4000-8000-000003010002', 'Video didáctico de YouTube para Patrones y Simetría'),
  ('ed000002-0000-4000-8000-000003010003', 'fa000000-0000-4000-8000-000003010003', 50, 'VIDEO', 'La Multiplicación | Aprender a multiplicar | Happy Learning', 'ec000002-0000-4000-8000-000003010003', 'Video didáctico de YouTube para Multiplicación'),
  ('ed000002-0000-4000-8000-000003010004', 'fa000000-0000-4000-8000-000003010004', 50, 'VIDEO', 'La longitud y su unidad de medida: El metro | Happy Learning', 'ec000002-0000-4000-8000-000003010004', 'Video didáctico de YouTube para Medidas y Longitud'),
  ('ed000002-0000-4000-8000-000003010005', 'fa000000-0000-4000-8000-000003010005', 50, 'VIDEO', 'Las Fracciones para niños | Smile and Learn', 'ec000002-0000-4000-8000-000003010005', 'Video didáctico de YouTube para Fracciones'),
  ('ed000002-0000-4000-8000-000003010006', 'fa000000-0000-4000-8000-000003010006', 50, 'VIDEO', 'Las figuras geométricas planas | Vocabulario de Primaria', 'ec000002-0000-4000-8000-000003010006', 'Video didáctico de YouTube para Retablo Matemático y Geometría')
ON DUPLICATE KEY UPDATE texto_cuerpo = VALUES(texto_cuerpo), archivo_multimedia_id = VALUES(archivo_multimedia_id), texto_accesibilidad = VALUES(texto_accesibilidad);
