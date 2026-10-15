-- =============================================================================
-- Migración V19: Publicación y Enriquecimiento de Roadmaps de la Comunidad / Docente
-- Autor: Prof. Carlos Mendoza (profesor.demo)
-- Provee roadmaps comunitarios completos y códigos de acceso PROFCARLOS y DOCENTE2026.
-- =============================================================================

USE rupi;
SET NAMES utf8mb4;

-- 1. Actualizar títulos y descripciones de las rutas comunitarias del docente
UPDATE aprendizaje_ruta
SET titulo = 'Taller de Cuentos y Mitos Andinos',
    descripcion = 'Aventura literaria creada por el Prof. Carlos para descubrir personajes mágicos, fábulas y leyendas de Ayacucho.'
WHERE id = '0af312d5-952f-40ea-8021-cc8b821702ab';

UPDATE aprendizaje_ruta
SET titulo = 'Retos Matemáticos y Geometría en Ayacucho',
    descripcion = 'Exploración de números, patrones y medidas en ferias y mercados tradicionales creada por el Prof. Carlos.'
WHERE id = 'f0d015db-0010-4f50-a295-f6d1c7605855';

-- 2. Publicar ambas versiones de ruta
UPDATE aprendizaje_version_ruta
SET estado = 'PUBLICADO',
    publicado_en = COALESCE(publicado_en, UTC_TIMESTAMP(6))
WHERE id IN ('41581fe2-4b60-4a7d-8d3e-d2224780fbae', 'c51711e8-4ed9-49ca-8efd-f9bb88846b97');

-- 2.1 Vincular parada inicial a la unidad curricular para conformar una unidad única y unificada
UPDATE aprendizaje_nodo_ruta
SET unidad_id = 'd2000000-0000-4000-8000-000000000302'
WHERE version_ruta_id = '41581fe2-4b60-4a7d-8d3e-d2224780fbae' AND numero_secuencia = 1;

UPDATE aprendizaje_nodo_ruta
SET unidad_id = 'd2000000-0000-4000-8000-000000000062'
WHERE version_ruta_id = 'c51711e8-4ed9-49ca-8efd-f9bb88846b97' AND numero_secuencia = 1;

-- 3. Paradas pedagógicas para "Taller de Cuentos y Mitos Andinos" (41581fe2-4b60-4a7d-8d3e-d2224780fbae)
-- Actividades
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ec000000-0000-4000-8000-000003020002', 'LECCION', '22222222-2222-4222-8222-222222222222'),
  ('ec000000-0000-4000-8000-000003020003', 'RETO', '22222222-2222-4222-8222-222222222222'),
  ('ec000000-0000-4000-8000-000003020004', 'LECCION', '22222222-2222-4222-8222-222222222222'),
  ('ec000000-0000-4000-8000-000003020005', 'RETO', '22222222-2222-4222-8222-222222222222'),
  ('ec000000-0000-4000-8000-000003020006', 'RETO', '22222222-2222-4222-8222-222222222222')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

-- Versiones de Actividad
INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('ea000000-0000-4000-8000-000003020002', 'ec000000-0000-4000-8000-000003020002', 1, 'El Zorro y el Cóndor: Personajes y Acciones', 10, 'PUBLICADO', 'Lee con atención e identifica qué hace cada personaje.'),
  ('ea000000-0000-4000-8000-000003020003', 'ec000000-0000-4000-8000-000003020003', 1, 'Palabras que Describen: Adjetivos en Nuestras Historias', 10, 'PUBLICADO', 'Reconoce las características de los lugares y seres andinos.'),
  ('ea000000-0000-4000-8000-000003020004', 'ec000000-0000-4000-8000-000003020004', 1, 'La Rueda de la Narración: Inicio, Nudo y Desenlace', 12, 'PUBLICADO', 'Ordena los momentos clave de una historia.'),
  ('ea000000-0000-4000-8000-000003020005', 'ec000000-0000-4000-8000-000003020005', 1, 'Rimas y Retahílas Quechuas y Castellanas', 10, 'PUBLICADO', 'Descubre el ritmo y la musicalidad de nuestras palabras.'),
  ('ea000000-0000-4000-8000-000003020006', 'ec000000-0000-4000-8000-000003020006', 1, 'Gran Retablo de Cuentos Escolares', 15, 'PUBLICADO', 'Demuestra todo lo que aprendiste creando tu relato final.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), instrucciones = VALUES(instrucciones), estado = 'PUBLICADO';

-- Nodos de la ruta
INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ee000000-0000-4000-8000-000003020002', '41581fe2-4b60-4a7d-8d3e-d2224780fbae', 'ea000000-0000-4000-8000-000003020002', 'd2000000-0000-4000-8000-000000000302', 2, FALSE, 28, 34),
  ('ee000000-0000-4000-8000-000003020003', '41581fe2-4b60-4a7d-8d3e-d2224780fbae', 'ea000000-0000-4000-8000-000003020003', 'd2000000-0000-4000-8000-000000000302', 3, FALSE, 72, 48),
  ('ee000000-0000-4000-8000-000003020004', '41581fe2-4b60-4a7d-8d3e-d2224780fbae', 'ea000000-0000-4000-8000-000003020004', 'd2000000-0000-4000-8000-000000000302', 4, FALSE, 34, 62),
  ('ee000000-0000-4000-8000-000003020005', '41581fe2-4b60-4a7d-8d3e-d2224780fbae', 'ea000000-0000-4000-8000-000003020005', 'd2000000-0000-4000-8000-000000000302', 5, FALSE, 66, 75),
  ('ee000000-0000-4000-8000-000003020006', '41581fe2-4b60-4a7d-8d3e-d2224780fbae', 'ea000000-0000-4000-8000-000003020006', 'd2000000-0000-4000-8000-000000000302', 6, FALSE, 50, 90)
ON DUPLICATE KEY UPDATE mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

-- Bloques de contenido y preguntas
INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo) VALUES
  ('eb000000-0000-4000-8000-000003020002', 'ea000000-0000-4000-8000-000003020002', 1, 'TEXTO', 'En los cuentos andinos, el cóndor vuela alto y el zorro busca astucia en la tierra. Identifica qué virtudes nos enseña cada uno.'),
  ('eb000000-0000-4000-8000-000003020003', 'ea000000-0000-4000-8000-000003020003', 1, 'TEXTO', 'Los adjetivos nos dicen cómo son las cosas: el nevado es brillante, la vicuña es veloz y el río es cristalino.'),
  ('eb000000-0000-4000-8000-000003020004', 'ea000000-0000-4000-8000-000003020004', 1, 'TEXTO', 'Toda narración tiene un inicio donde conocemos a los personajes, un nudo con el problema y un desenlace con la solución.'),
  ('eb000000-0000-4000-8000-000003020005', 'ea000000-0000-4000-8000-000003020005', 1, 'TEXTO', 'Las retahílas son juegos de palabras con ritmo alegre que se transmiten de abuelos a nietos en nuestros pueblos.'),
  ('eb000000-0000-4000-8000-000003020006', 'ea000000-0000-4000-8000-000003020006', 1, 'TEXTO', '¡Felicidades! Has completado el taller literario del Prof. Carlos. ¡Eres un narrador de los Andes!')
ON DUPLICATE KEY UPDATE texto_cuerpo = VALUES(texto_cuerpo);

INSERT INTO evaluacion_pregunta (id, version_actividad_id, numero_secuencia, tipo, enunciado, explicacion, puntos) VALUES
  ('ef000000-0000-4000-8000-000003020002', 'ea000000-0000-4000-8000-000003020002', 1, 'OPCION_MULTIPLE', '¿Cuál es el hogar tradicional del majestuoso cóndor andino?', 'El cóndor habita en las cumbres andinas y los grandes cañones.', 10),
  ('ef000000-0000-4000-8000-000003020003', 'ea000000-0000-4000-8000-000003020003', 1, 'OPCION_MULTIPLE', 'En la frase "La vicuña veloz corre en la pampa", ¿cuál es el adjetivo?', '"Veloz" nos dice cómo es la vicuña, por eso es el adjetivo.', 10),
  ('ef000000-0000-4000-8000-000003020004', 'ea000000-0000-4000-8000-000003020004', 1, 'OPCION_MULTIPLE', '¿En qué parte de un cuento se resuelve el problema de los personajes?', 'El desenlace es el final donde todo se soluciona.', 10),
  ('ef000000-0000-4000-8000-000003020005', 'ea000000-0000-4000-8000-000003020005', 1, 'OPCION_MULTIPLE', '¿Para qué usamos las rimas y canciones tradicionales?', 'Para disfrutar del ritmo y recordar historias con alegría.', 10),
  ('ef000000-0000-4000-8000-000003020006', 'ea000000-0000-4000-8000-000003020006', 1, 'OPCION_MULTIPLE', '¿Qué cualidad demostró tener un buen narrador escolar?', 'Saber escuchar, ordenar sus ideas y compartir con entusiasmo.', 10)
ON DUPLICATE KEY UPDATE enunciado = VALUES(enunciado), explicacion = VALUES(explicacion);

INSERT INTO evaluacion_opcion_respuesta (id, pregunta_id, numero_secuencia, texto, es_correcta) VALUES
  ('ed000000-0000-4000-8000-000003020211', 'ef000000-0000-4000-8000-000003020002', 1, 'Las altas cumbres de los Andes', TRUE),
  ('ed000000-0000-4000-8000-000003020212', 'ef000000-0000-4000-8000-000003020002', 2, 'El fondo del mar', FALSE),
  ('ed000000-0000-4000-8000-000003020311', 'ef000000-0000-4000-8000-000003020003', 1, 'Veloz', TRUE),
  ('ed000000-0000-4000-8000-000003020312', 'ef000000-0000-4000-8000-000003020003', 2, 'Pampa', FALSE),
  ('ed000000-0000-4000-8000-000003020411', 'ef000000-0000-4000-8000-000003020004', 1, 'En el desenlace', TRUE),
  ('ed000000-0000-4000-8000-000003020412', 'ef000000-0000-4000-8000-000003020004', 2, 'En el título', FALSE),
  ('ed000000-0000-4000-8000-000003020511', 'ef000000-0000-4000-8000-000003020005', 1, 'Para sentir el ritmo y gozar las palabras', TRUE),
  ('ed000000-0000-4000-8000-000003020512', 'ef000000-0000-4000-8000-000003020005', 2, 'Para dormir en silencio', FALSE),
  ('ed000000-0000-4000-8000-000003020611', 'ef000000-0000-4000-8000-000003020006', 1, 'Escuchar y ordenar sus ideas con claridad', TRUE),
  ('ed000000-0000-4000-8000-000003020612', 'ef000000-0000-4000-8000-000003020006', 2, 'Gritar muy fuerte sin pensar', FALSE)
ON DUPLICATE KEY UPDATE texto = VALUES(texto);

-- 4. Códigos de acceso de clase para ingresar directamente a las rutas del profesor
INSERT INTO aprendizaje_codigo_acceso_ruta (
  id, version_ruta_id, hash_codigo, creado_por, valido_desde, valido_hasta, maximo_usos, revocado_en
) VALUES
  ('c0000001-0000-4000-8000-000000000010', '41581fe2-4b60-4a7d-8d3e-d2224780fbae',
   UNHEX(SHA2('PROFCARLOS', 256)), '22222222-2222-4222-8222-222222222222',
   '2026-01-01 00:00:00.000000', '2027-12-31 23:59:59.000000', 100, NULL),
  ('c0000001-0000-4000-8000-000000000011', 'c51711e8-4ed9-49ca-8efd-f9bb88846b97',
   UNHEX(SHA2('DOCENTE2026', 256)), '22222222-2222-4222-8222-222222222222',
   '2026-01-01 00:00:00.000000', '2027-12-31 23:59:59.000000', 100, NULL)
ON DUPLICATE KEY UPDATE revocado_en = NULL, maximo_usos = 100;
