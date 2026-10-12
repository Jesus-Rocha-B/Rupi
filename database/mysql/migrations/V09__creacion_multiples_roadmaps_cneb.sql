-- V09: Creación integral de múltiples roadmaps escolares CNEB vinculados a la base de datos (HU-09).
-- Define rutas completas para Comunicación, Ciencia y Tecnología, Personal Social, Arte y Cultura, y Matemática 3.°.
-- Incluye: unidades curriculares, competencias, nodos en zigzag, actividades didácticas, bloques pedagógicos,
-- preguntas de evaluación formativa con opciones, contextos culturales peruanos e inscripciones con progreso inicial.
-- Idempotente / MySQL 8.4 InnoDB / UTF-8 mb4

USE rupi;
SET NAMES utf8mb4;

-- ============================================================================
-- 1. COMPETENCIAS CURRICULARES CNEB-2017 ADICIONALES
-- ============================================================================
INSERT INTO curriculo_competencia (id, area_id, codigo, nombre) VALUES
  -- Comunicación
  ('c2000000-0000-4000-8000-000000000010', 'a0000000-0000-4000-8000-000000000002', 'COM-ORAL', 'Se comunica oralmente en su lengua materna'),
  ('c2000000-0000-4000-8000-000000000011', 'a0000000-0000-4000-8000-000000000002', 'COM-LEE', 'Lee diversos tipos de textos escritos en su lengua materna'),
  ('c2000000-0000-4000-8000-000000000012', 'a0000000-0000-4000-8000-000000000002', 'COM-ESC', 'Escribe diversos tipos de textos en su lengua materna'),
  -- Ciencia y Tecnología
  ('c2000000-0000-4000-8000-000000000020', 'a0000000-0000-4000-8000-000000000003', 'CYT-INDAGA', 'Indaga mediante métodos científicos para construir sus conocimientos'),
  ('c2000000-0000-4000-8000-000000000021', 'a0000000-0000-4000-8000-000000000003', 'CYT-EXPLICA', 'Explica el mundo físico basándose en conocimientos sobre seres vivos, materia y energía'),
  -- Personal Social
  ('c2000000-0000-4000-8000-000000000030', 'a0000000-0000-4000-8000-000000000004', 'SOC-IDENT', 'Construye su identidad'),
  ('c2000000-0000-4000-8000-000000000031', 'a0000000-0000-4000-8000-000000000004', 'SOC-CONVIVE', 'Convive y participa democráticamente en la búsqueda del bien común'),
  -- Arte y Cultura
  ('c2000000-0000-4000-8000-000000000040', 'a0000000-0000-4000-8000-000000000005', 'ART-APRECIA', 'Aprecia de manera crítica manifestaciones artístico-culturales'),
  ('c2000000-0000-4000-8000-000000000041', 'a0000000-0000-4000-8000-000000000005', 'ART-CREA', 'Crea proyectos desde los lenguajes artísticos')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- ============================================================================
-- 2. UNIDADES CURRICULARES CNEB PARA LAS NUEVAS RUTAS
-- ============================================================================

-- Unidades Comunicación 2.° Primaria
INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, numero_secuencia, titulo) VALUES
  ('d2000000-0000-4000-8000-000000000021', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000002', 'U1-COM2', 1, 'Escuchamos y contamos fábulas del valle'),
  ('d2000000-0000-4000-8000-000000000022', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000002', 'U2-COM2', 2, 'Lectores exploradores de leyendas andinas'),
  ('d2000000-0000-4000-8000-000000000023', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000002', 'U3-COM2', 3, 'Escribimos nuestro libro de cuentos escolares')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- Unidades Ciencia y Tecnología 2.° Primaria
INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, numero_secuencia, titulo) VALUES
  ('d2000000-0000-4000-8000-000000000031', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000003', 'U1-CYT2', 1, 'Los seres vivos de nuestro entorno andino'),
  ('d2000000-0000-4000-8000-000000000032', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000003', 'U2-CYT2', 2, 'Plantas que curan y suelos que alimentan'),
  ('d2000000-0000-4000-8000-000000000033', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000003', 'U3-CYT2', 3, 'Guardianes del agua y la naturaleza')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- Unidades Personal Social 2.° Primaria
INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, numero_secuencia, titulo) VALUES
  ('d2000000-0000-4000-8000-000000000041', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000004', 'U1-SOC2', 1, 'Quién soy y el tesoro de mi familia'),
  ('d2000000-0000-4000-8000-000000000042', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000004', 'U2-SOC2', 2, 'Convivimos en armonía y cuidamos nuestro espacio'),
  ('d2000000-0000-4000-8000-000000000043', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000004', 'U3-SOC2', 3, 'Viajeros en el tiempo: Costumbres de nuestra comunidad')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- Unidades Arte y Cultura 2.° Primaria
INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, numero_secuencia, titulo) VALUES
  ('d2000000-0000-4000-8000-000000000051', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000005', 'U1-ART2', 1, 'Modelado y colores de la tierra'),
  ('d2000000-0000-4000-8000-000000000052', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000005', 'U2-ART2', 2, 'El retablo: contando historias en una caja mágica'),
  ('d2000000-0000-4000-8000-000000000053', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000005', 'U3-ART2', 3, 'Música, danzas y fiesta tradicional')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- Unidades Matemática 3.° Primaria
INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, numero_secuencia, titulo) VALUES
  ('d2000000-0000-4000-8000-000000000061', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000001', 'U1-MAT3', 1, 'Multiplicando en los andenes de cultivo'),
  ('d2000000-0000-4000-8000-000000000062', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000001', 'U2-MAT3', 2, 'Formas, ángulos y figuras en la piedra Wari'),
  ('d2000000-0000-4000-8000-000000000063', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000001', 'U3-MAT3', 3, 'La balanza mágica y medidas en la feria')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- Vínculos Unidad-Competencia
INSERT INTO curriculo_unidad_competencia (unidad_id, competencia_id) VALUES
  ('d2000000-0000-4000-8000-000000000021', 'c2000000-0000-4000-8000-000000000010'),
  ('d2000000-0000-4000-8000-000000000022', 'c2000000-0000-4000-8000-000000000011'),
  ('d2000000-0000-4000-8000-000000000023', 'c2000000-0000-4000-8000-000000000012'),
  ('d2000000-0000-4000-8000-000000000031', 'c2000000-0000-4000-8000-000000000021'),
  ('d2000000-0000-4000-8000-000000000032', 'c2000000-0000-4000-8000-000000000020'),
  ('d2000000-0000-4000-8000-000000000033', 'c2000000-0000-4000-8000-000000000021'),
  ('d2000000-0000-4000-8000-000000000041', 'c2000000-0000-4000-8000-000000000030'),
  ('d2000000-0000-4000-8000-000000000042', 'c2000000-0000-4000-8000-000000000031'),
  ('d2000000-0000-4000-8000-000000000043', 'c2000000-0000-4000-8000-000000000030'),
  ('d2000000-0000-4000-8000-000000000051', 'c2000000-0000-4000-8000-000000000041'),
  ('d2000000-0000-4000-8000-000000000052', 'c2000000-0000-4000-8000-000000000040'),
  ('d2000000-0000-4000-8000-000000000053', 'c2000000-0000-4000-8000-000000000041'),
  ('d2000000-0000-4000-8000-000000000061', 'c1000000-0000-4000-8000-000000000001'),
  ('d2000000-0000-4000-8000-000000000062', 'c1000000-0000-4000-8000-000000000003'),
  ('d2000000-0000-4000-8000-000000000063', 'c1000000-0000-4000-8000-000000000002')
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id);

-- ============================================================================
-- 3. RUTA Y VERSIÓN MATEMÁTICA 3.° DE PRIMARIA (MAT-3P-WARI)
-- ============================================================================
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000007', '11111111-1111-4111-8111-111111111111', 'MAT-3P-WARI',
   'Matemática: Desafíos y Geometría en la Ciudadela Wari',
   'Aventura matemática de 3.° de primaria resolviendo retos de multiplicación, geometría en muros andinos y medidas ancestrales.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000007', 'd0000000-0000-4000-8000-000000000007', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000001', 1, 'PUBLICADO', UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

-- ============================================================================
-- 4. CONTEXTOS CULTURALES ANDINOS PARA CADA RUTA
-- ============================================================================
INSERT INTO curriculo_contexto_cultural (
  id, ruta_id, codigo, ciudad, lugar, titulo, descripcion, mensaje_animo,
  imagen_url, imagen_alt, credito_autor, credito_fuente, credito_licencia, credito_url
) VALUES
  ('cc000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000002', 'VALLE_SAGRADO_CUENTOS',
   'Huanta', 'Valle de las Esmeraldas', 'Palabras y leyendas del valle',
   'El valle andino guarda los cantos del Tunqui y las historias que los abuelos narran bajo las estrellas.',
   '¡Abre tus alas y deja que tu imaginación vuele como el gallito de las rocas!',
   '/images/ayacucho-plaza.jpg', 'Valle fértil andino', 'Tradición oral', 'CNEB Perú', 'CC BY-SA 4.0', 'https://rupi.pe'),
  ('cc000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000003', 'BOSQUE_NEBLINA_CYT',
   'La Mar', 'Bosques de Neblina de Ayacucho', 'El hogar del Gallito de las Rocas',
   'Entre neblina y árboles frondosos viven aves de plumaje encendido y plantas milenarias llenas de vida.',
   '¡Observa como un científico y descubre las maravillas que nos rodean!',
   '/images/ayacucho-plaza.jpg', 'Bosque de neblina andino', 'SERFOR', 'Sernanp Perú', 'CC BY-SA 4.0', 'https://rupi.pe'),
  ('cc000000-0000-4000-8000-000000000004', 'd0000000-0000-4000-8000-000000000004', 'COMUNIDAD_AYACUCHO_SOC',
   'Huamanga', 'Comunidades Tradicionales', 'Vivir en armonía: el Ayni',
   'En nuestras comunidades aprendemos que cuando nos ayudamos entre todos, el pueblo florece con alegría.',
   '¡Eres parte importante de tu comunidad y de la gran familia del Perú!',
   '/images/ayacucho-plaza.jpg', 'Comunidad andina', 'Ministerio de Cultura', 'MinCul Perú', 'CC BY-SA 4.0', 'https://rupi.pe'),
  ('cc000000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-000000000005', 'SANTA_ANA_ARTE',
   'Ayacucho', 'Barrio Artesanal de Santa Ana', 'Taller de retablos y colores',
   'Cajas de cedro, figuras de harina de papa y tintes naturales dan vida al arte popular más querido de nuestra patria.',
   '¡Tus manos y tu corazón crean belleza única en cada actividad!',
   '/images/ayacucho-plaza.jpg', 'Taller de retablos', 'Artesanía de Ayacucho', 'MINCETUR', 'CC BY-SA 4.0', 'https://rupi.pe'),
  ('cc000000-0000-4000-8000-000000000007', 'd0000000-0000-4000-8000-000000000007', 'WARI_GEOMETRIA_MAT',
   'Quinua', 'Complejo Arqueológico Wari', 'Piedras talladas y grandes murallas',
   'Hace más de mil años, los antiguos constructores Wari usaban proporciones exactas para edificar templos eternos.',
   '¡Piensa en grande y resuelve cada reto con la fuerza de nuestros antepasados!',
   '/images/ayacucho-plaza.jpg', 'Murallas de Wari', 'Arqueología Peruana', 'MINCETUR', 'CC BY-SA 4.0', 'https://rupi.pe')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

-- ============================================================================
-- 5. ACTIVIDADES Y VERSIONES DIDÁCTICAS PARA LAS 6 PARADAS DE CADA RUTA
-- ============================================================================

-- COMUNICACIÓN 2.° (Ruta b0000000-0000-4000-8000-000000000002)
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000000000201', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000202', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000203', 'RETO',    '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000204', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000205', 'RETO',    '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000206', 'RETO',    '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('fa000000-0000-4000-8000-000000000201', 'ac000000-0000-4000-8000-000000000201', 1, 'Fábula: El zorro astuto y el cuy andino', 8, 'PUBLICADO', 'Lee con atención la historia y descubre la enseñanza que nos deja el cuy.'),
  ('fa000000-0000-4000-8000-000000000202', 'ac000000-0000-4000-8000-000000000202', 1, 'El cantar del Tunqui en el bosque', 8, 'PUBLICADO', 'Escucha los sonidos de la naturaleza e identifica los personajes del relato.'),
  ('fa000000-0000-4000-8000-000000000203', 'ac000000-0000-4000-8000-000000000203', 1, 'Rimas alegres en el mercado de Huamanga', 10, 'PUBLICADO', 'Busca las palabras que terminan con el mismo sonido en los pregones del mercado.'),
  ('fa000000-0000-4000-8000-000000000204', 'ac000000-0000-4000-8000-000000000204', 1, 'La leyenda de la flor sagrada de Cantuta', 10, 'PUBLICADO', 'Conoce por qué la cantuta es la flor nacional del Perú y símbolo de unión.'),
  ('fa000000-0000-4000-8000-000000000205', 'ac000000-0000-4000-8000-000000000205', 1, 'Adivinanzas en quechua y castellano', 10, 'PUBLICADO', 'Descifra las adivinanzas tradicionales usando las pistas de Rupi.'),
  ('fa000000-0000-4000-8000-000000000206', 'ac000000-0000-4000-8000-000000000206', 1, 'Mi primer cuento familiar ilustrado', 12, 'PUBLICADO', 'Escribe el inicio, nudo y desenlace de una linda historia de tu familia.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- CIENCIA Y TECNOLOGÍA 2.° (Ruta b0000000-0000-4000-8000-000000000003)
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000000000301', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000302', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000303', 'RETO',    '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000304', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000305', 'RETO',    '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000306', 'RETO',    '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('fa000000-0000-4000-8000-000000000301', 'ac000000-0000-4000-8000-000000000301', 1, 'El vuelo y plumaje del Gallito de las Rocas', 8, 'PUBLICADO', 'Descubre cómo vive nuestra ave nacional y por qué cuida las semillas del bosque.'),
  ('fa000000-0000-4000-8000-000000000302', 'ac000000-0000-4000-8000-000000000302', 1, 'Plantas medicinales: Muña y eucalipto', 8, 'PUBLICADO', 'Observa las hojas, el aroma y las propiedades que alivian el frío andino.'),
  ('fa000000-0000-4000-8000-000000000303', 'ac000000-0000-4000-8000-000000000303', 1, 'El viaje del agua: De la nube a la chacra', 10, 'PUBLICADO', 'Sigue el camino del agua desde los nevados hasta los canales de riego.'),
  ('fa000000-0000-4000-8000-000000000304', 'ac000000-0000-4000-8000-000000000304', 1, 'El suelo fértil y la siembra de la papa', 10, 'PUBLICADO', 'Compara la tierra de cultivo con la arena y descubre qué necesita una semilla para crecer.'),
  ('fa000000-0000-4000-8000-000000000305', 'ac000000-0000-4000-8000-000000000305', 1, 'La vicuña y la fauna de la puna', 10, 'PUBLICADO', 'Conoce cómo se abriga la vicuña en el frío extremo con su fina lana dorada.'),
  ('fa000000-0000-4000-8000-000000000306', 'ac000000-0000-4000-8000-000000000306', 1, 'Misión Guardián Verde: Protegemos nuestro río', 12, 'PUBLICADO', 'Diseña un plan escolar para no arrojar plásticos y cuidar el agua limpia.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- PERSONAL SOCIAL 2.° (Ruta b0000000-0000-4000-8000-000000000004)
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000000000401', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000402', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000403', 'RETO',    '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000404', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000405', 'RETO',    '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000406', 'RETO',    '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('fa000000-0000-4000-8000-000000000401', 'ac000000-0000-4000-8000-000000000401', 1, 'Mi historia familiar y mis raíces', 8, 'PUBLICADO', 'Reconoce las costumbres y valores que tu familia te enseña con cariño.'),
  ('fa000000-0000-4000-8000-000000000402', 'ac000000-0000-4000-8000-000000000402', 1, 'El Ayni: Trabajo en equipo y solidaridad', 8, 'PUBLICADO', 'Aprende cómo las familias andinas se ayudan en la siembra y la cosecha.'),
  ('fa000000-0000-4000-8000-000000000403', 'ac000000-0000-4000-8000-000000000403', 1, 'Los maestros artesanos de Huamanga', 10, 'PUBLICADO', 'Valora el trabajo de los abuelos talladores y ceramistas del barrio.'),
  ('fa000000-0000-4000-8000-000000000404', 'ac000000-0000-4000-8000-000000000404', 1, 'Normas de amistad y respeto en el aula', 10, 'PUBLICADO', 'Construyamos acuerdos para jugar y aprender en paz y compañerismo.'),
  ('fa000000-0000-4000-8000-000000000405', 'ac000000-0000-4000-8000-000000000405', 1, 'Cuidamos la plaza y los espacios de todos', 10, 'PUBLICADO', 'Identifica acciones para mantener limpios los parques y banquetas.'),
  ('fa000000-0000-4000-8000-000000000406', 'ac000000-0000-4000-8000-000000000406', 1, 'El gran mural de nuestra memoria comunitaria', 12, 'PUBLICADO', 'Dibuja lo que más amas de tu pueblo para compartirlo con tu escuela.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- ARTE Y CULTURA 2.° (Ruta b0000000-0000-4000-8000-000000000005)
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000000000501', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000502', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000503', 'RETO',    '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000504', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000505', 'RETO',    '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000506', 'RETO',    '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('fa000000-0000-4000-8000-000000000501', 'ac000000-0000-4000-8000-000000000501', 1, 'Modelando arcilla con nuestras manos', 8, 'PUBLICADO', 'Siente la textura de la arcilla y moldea una pequeña ave o vasija.'),
  ('fa000000-0000-4000-8000-000000000502', 'ac000000-0000-4000-8000-000000000502', 1, 'Tintes naturales: Rojo cochinilla y maíz morado', 8, 'PUBLICADO', 'Observa cómo los frutos de la tierra nos dan colores vivos y alegres.'),
  ('fa000000-0000-4000-8000-000000000503', 'ac000000-0000-4000-8000-000000000503', 1, 'El retablo: Una caja mágica de madera', 10, 'PUBLICADO', 'Descubre los dos pisos del retablo y las figuras que celebran la vida.'),
  ('fa000000-0000-4000-8000-000000000504', 'ac000000-0000-4000-8000-000000000504', 1, 'Sonidos del charango, zampoña y quena', 10, 'PUBLICADO', 'Reconoce las melodías dulces del viento y las cuerdas andinas.'),
  ('fa000000-0000-4000-8000-000000000505', 'ac000000-0000-4000-8000-000000000505', 1, 'Danza de las Tijeras: Agilidad y destreza', 10, 'PUBLICADO', 'Imita los pasos firmes y saltos que rinden tributo a las montañas.'),
  ('fa000000-0000-4000-8000-000000000506', 'ac000000-0000-4000-8000-000000000506', 1, 'Nuestra galería escolar de arte tradicional', 12, 'PUBLICADO', 'Presenta tu obra terminada y comparte qué sentiste al crearla.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- MATEMÁTICA 3.° (Ruta b0000000-0000-4000-8000-000000000007)
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000000000701', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000702', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000703', 'RETO',    '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000704', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000705', 'RETO',    '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000706', 'RETO',    '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado, instrucciones) VALUES
  ('fa000000-0000-4000-8000-000000000701', 'ac000000-0000-4000-8000-000000000701', 1, 'Multiplicamos hileras de maíz en los andenes', 10, 'PUBLICADO', 'Usa sumas repetidas para descubrir cuántas mazorcas hay en 3 hileras de 4 plantas.'),
  ('fa000000-0000-4000-8000-000000000702', 'ac000000-0000-4000-8000-000000000702', 1, 'El enigma de los ángulos en las murallas de Wari', 10, 'PUBLICADO', 'Observa las esquinas rectas y figuras geométricas talladas en la roca.'),
  ('fa000000-0000-4000-8000-000000000703', 'ac000000-0000-4000-8000-000000000703', 1, 'Medimos perímetros con cuerdas y pasos firmes', 12, 'PUBLICADO', 'Calcula el contorno de una plaza rectangular sumando todos sus lados.'),
  ('fa000000-0000-4000-8000-000000000704', 'ac000000-0000-4000-8000-000000000704', 1, 'La balanza mágica: Pesando papas y quinua en kilos', 10, 'PUBLICADO', 'Equilibra la balanza usando pesas de 1 kilo y medio kilo en el mercado.'),
  ('fa000000-0000-4000-8000-000000000705', 'ac000000-0000-4000-8000-000000000705', 1, 'Patrones numéricos en los nudos del quipu', 10, 'PUBLICADO', 'Descubre el número que falta en una secuencia de números de 5 en 5 y 10 en 10.'),
  ('fa000000-0000-4000-8000-000000000706', 'ac000000-0000-4000-8000-000000000706', 1, 'Gran desafío del arquitecto Wari', 15, 'PUBLICADO', 'Resuelve el gran problema de construcción combinando multiplicación y geometría.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- ============================================================================
-- 6. PARADAS / NODOS EN ZIGZAG PARA CADA RUTA (X, Y)
-- ============================================================================

-- Retira los nodos provisionales de V08 (una parada por ruta) que esta migración reemplaza por las 6 paradas definitivas.
-- No toca progreso: V08 no crea inscripciones y los nodos definitivos conservan su propio id.
DELETE FROM aprendizaje_nodo_ruta
 WHERE id IN ('ca000000-0000-4000-8000-000000000021', 'ca000000-0000-4000-8000-000000000031',
              'ca000000-0000-4000-8000-000000000041', 'ca000000-0000-4000-8000-000000000051')
   AND numero_secuencia = 1;

-- COMUNICACIÓN 2.° (b0000000-0000-4000-8000-000000000002)
INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ca000000-0000-4000-8000-000000000211', 'b0000000-0000-4000-8000-000000000002', 'fa000000-0000-4000-8000-000000000201', 'd2000000-0000-4000-8000-000000000021', 1, FALSE, 50, 18),
  ('ca000000-0000-4000-8000-000000000212', 'b0000000-0000-4000-8000-000000000002', 'fa000000-0000-4000-8000-000000000202', 'd2000000-0000-4000-8000-000000000021', 2, FALSE, 27, 32),
  ('ca000000-0000-4000-8000-000000000213', 'b0000000-0000-4000-8000-000000000002', 'fa000000-0000-4000-8000-000000000203', 'd2000000-0000-4000-8000-000000000022', 3, FALSE, 73, 46),
  ('ca000000-0000-4000-8000-000000000214', 'b0000000-0000-4000-8000-000000000002', 'fa000000-0000-4000-8000-000000000204', 'd2000000-0000-4000-8000-000000000022', 4, FALSE, 35, 60),
  ('ca000000-0000-4000-8000-000000000215', 'b0000000-0000-4000-8000-000000000002', 'fa000000-0000-4000-8000-000000000205', 'd2000000-0000-4000-8000-000000000023', 5, FALSE, 68, 74),
  ('ca000000-0000-4000-8000-000000000216', 'b0000000-0000-4000-8000-000000000002', 'fa000000-0000-4000-8000-000000000206', 'd2000000-0000-4000-8000-000000000023', 6, FALSE, 50, 88)
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id), mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

-- CIENCIA Y TECNOLOGÍA 2.° (b0000000-0000-4000-8000-000000000003)
INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ca000000-0000-4000-8000-000000000311', 'b0000000-0000-4000-8000-000000000003', 'fa000000-0000-4000-8000-000000000301', 'd2000000-0000-4000-8000-000000000031', 1, FALSE, 50, 18),
  ('ca000000-0000-4000-8000-000000000312', 'b0000000-0000-4000-8000-000000000003', 'fa000000-0000-4000-8000-000000000302', 'd2000000-0000-4000-8000-000000000031', 2, FALSE, 27, 32),
  ('ca000000-0000-4000-8000-000000000313', 'b0000000-0000-4000-8000-000000000003', 'fa000000-0000-4000-8000-000000000303', 'd2000000-0000-4000-8000-000000000032', 3, FALSE, 73, 46),
  ('ca000000-0000-4000-8000-000000000314', 'b0000000-0000-4000-8000-000000000003', 'fa000000-0000-4000-8000-000000000304', 'd2000000-0000-4000-8000-000000000032', 4, FALSE, 35, 60),
  ('ca000000-0000-4000-8000-000000000315', 'b0000000-0000-4000-8000-000000000003', 'fa000000-0000-4000-8000-000000000305', 'd2000000-0000-4000-8000-000000000033', 5, FALSE, 68, 74),
  ('ca000000-0000-4000-8000-000000000316', 'b0000000-0000-4000-8000-000000000003', 'fa000000-0000-4000-8000-000000000306', 'd2000000-0000-4000-8000-000000000033', 6, FALSE, 50, 88)
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id), mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

-- PERSONAL SOCIAL 2.° (b0000000-0000-4000-8000-000000000004)
INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ca000000-0000-4000-8000-000000000411', 'b0000000-0000-4000-8000-000000000004', 'fa000000-0000-4000-8000-000000000401', 'd2000000-0000-4000-8000-000000000041', 1, FALSE, 50, 18),
  ('ca000000-0000-4000-8000-000000000412', 'b0000000-0000-4000-8000-000000000004', 'fa000000-0000-4000-8000-000000000402', 'd2000000-0000-4000-8000-000000000041', 2, FALSE, 27, 32),
  ('ca000000-0000-4000-8000-000000000413', 'b0000000-0000-4000-8000-000000000004', 'fa000000-0000-4000-8000-000000000403', 'd2000000-0000-4000-8000-000000000042', 3, FALSE, 73, 46),
  ('ca000000-0000-4000-8000-000000000414', 'b0000000-0000-4000-8000-000000000004', 'fa000000-0000-4000-8000-000000000404', 'd2000000-0000-4000-8000-000000000042', 4, FALSE, 35, 60),
  ('ca000000-0000-4000-8000-000000000415', 'b0000000-0000-4000-8000-000000000004', 'fa000000-0000-4000-8000-000000000004', 'd2000000-0000-4000-8000-000000000043', 5, FALSE, 68, 74),
  ('ca000000-0000-4000-8000-000000000416', 'b0000000-0000-4000-8000-000000000004', 'fa000000-0000-4000-8000-000000000406', 'd2000000-0000-4000-8000-000000000043', 6, FALSE, 50, 88)
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id), mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

-- ARTE Y CULTURA 2.° (b0000000-0000-4000-8000-000000000005)
INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ca000000-0000-4000-8000-000000000511', 'b0000000-0000-4000-8000-000000000005', 'fa000000-0000-4000-8000-000000000501', 'd2000000-0000-4000-8000-000000000051', 1, FALSE, 50, 18),
  ('ca000000-0000-4000-8000-000000000512', 'b0000000-0000-4000-8000-000000000005', 'fa000000-0000-4000-8000-000000000502', 'd2000000-0000-4000-8000-000000000051', 2, FALSE, 27, 32),
  ('ca000000-0000-4000-8000-000000000513', 'b0000000-0000-4000-8000-000000000005', 'fa000000-0000-4000-8000-000000000503', 'd2000000-0000-4000-8000-000000000052', 3, FALSE, 73, 46),
  ('ca000000-0000-4000-8000-000000000514', 'b0000000-0000-4000-8000-000000000005', 'fa000000-0000-4000-8000-000000000504', 'd2000000-0000-4000-8000-000000000052', 4, FALSE, 35, 60),
  ('ca000000-0000-4000-8000-000000000515', 'b0000000-0000-4000-8000-000000000005', 'fa000000-0000-4000-8000-000000000505', 'd2000000-0000-4000-8000-000000000053', 5, FALSE, 68, 74),
  ('ca000000-0000-4000-8000-000000000516', 'b0000000-0000-4000-8000-000000000005', 'fa000000-0000-4000-8000-000000000506', 'd2000000-0000-4000-8000-000000000053', 6, FALSE, 50, 88)
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id), mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

-- MATEMÁTICA 3.° (b0000000-0000-4000-8000-000000000007)
INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, unidad_id, numero_secuencia, es_opcional, mapa_x, mapa_y) VALUES
  ('ca000000-0000-4000-8000-000000000711', 'b0000000-0000-4000-8000-000000000007', 'fa000000-0000-4000-8000-000000000701', 'd2000000-0000-4000-8000-000000000061', 1, FALSE, 50, 18),
  ('ca000000-0000-4000-8000-000000000712', 'b0000000-0000-4000-8000-000000000007', 'fa000000-0000-4000-8000-000000000702', 'd2000000-0000-4000-8000-000000000061', 2, FALSE, 27, 32),
  ('ca000000-0000-4000-8000-000000000713', 'b0000000-0000-4000-8000-000000000007', 'fa000000-0000-4000-8000-000000000703', 'd2000000-0000-4000-8000-000000000062', 3, FALSE, 73, 46),
  ('ca000000-0000-4000-8000-000000000714', 'b0000000-0000-4000-8000-000000000007', 'fa000000-0000-4000-8000-000000000704', 'd2000000-0000-4000-8000-000000000062', 4, FALSE, 35, 60),
  ('ca000000-0000-4000-8000-000000000715', 'b0000000-0000-4000-8000-000000000007', 'fa000000-0000-4000-8000-000000000705', 'd2000000-0000-4000-8000-000000000063', 5, FALSE, 68, 74),
  ('ca000000-0000-4000-8000-000000000716', 'b0000000-0000-4000-8000-000000000007', 'fa000000-0000-4000-8000-000000000706', 'd2000000-0000-4000-8000-000000000063', 6, FALSE, 50, 88)
ON DUPLICATE KEY UPDATE unidad_id = VALUES(unidad_id), mapa_x = VALUES(mapa_x), mapa_y = VALUES(mapa_y);

-- ============================================================================
-- 7. BLOQUES PEDAGÓGICOS ESTRUCTURADOS (TEXTO Y DESTACADOS)
-- ============================================================================
INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo) VALUES
  -- Comunicación 2.°
  ('bb000000-0000-4000-8000-000000000201', 'fa000000-0000-4000-8000-000000000201', 1, 'TEXTO', 'En las faldas de Huanta, un zorro juguetón retó al pequeño cuy a una carrera hasta la cima del cerro. El cuy pensó: "No soy el más rápido, pero sí el más atento". Conoce cómo la perseverancia vence a la soberbia.'),
  ('bb000000-0000-4000-8000-000000000202', 'fa000000-0000-4000-8000-000000000202', 1, 'TEXTO', 'Al amanecer, entre los árboles de la ceja de selva, el gallito de las rocas salta y canta alegremente. Escucha cómo imita los sonidos del viento y aprende a describir sus colores.'),
  ('bb000000-0000-4000-8000-000000000203', 'fa000000-0000-4000-8000-000000000203', 1, 'TEXTO', 'En el mercado de Huamanga los puesteros pregonan con rima: "¡Palta madurita para su mesita!", "¡Manzana colorada, dulce y perfumada!". Encuentra palabras que riman.'),
  ('bb000000-0000-4000-8000-000000000204', 'fa000000-0000-4000-8000-000000000204', 1, 'TEXTO', 'Cuenta la leyenda que dos pueblos hermanos se reconciliaron en las alturas cuando floreció la flor de cantuta con sus campanas rojas y amarillas.'),
  ('bb000000-0000-4000-8000-000000000205', 'fa000000-0000-4000-8000-000000000205', 1, 'TEXTO', 'Imaynallataq: ¿Qué animalito tiene orejas largas, come pasto tierno y vive en madrigueras? Resuelve las adivinanzas de Rupi.'),
  ('bb000000-0000-4000-8000-000000000206', 'fa000000-0000-4000-8000-000000000206', 1, 'TEXTO', 'Escribe en tu cuaderno: "Había una vez...", agrega el personaje que más te gusta de tu familia y cuéntanos una anécdota alegre que recuerdes juntos.'),

  -- Ciencia y Tecnología 2.°
  ('bb000000-0000-4000-8000-000000000301', 'fa000000-0000-4000-8000-000000000301', 1, 'TEXTO', 'El Tunqui tiene plumas anaranjadas en la cabeza y pecho, y alas negras con destellos grises. Se alimenta de frutos silvestres y dispersa semillas para que crezcan nuevos árboles.'),
  ('bb000000-0000-4000-8000-000000000302', 'fa000000-0000-4000-8000-000000000302', 1, 'TEXTO', 'La muña es una plantita aromática que crece entre piedras. Al frotar sus hojitas despide un perfume fresco que ayuda a calmar el dolor de barriguita y el frío.'),
  ('bb000000-0000-4000-8000-000000000303', 'fa000000-0000-4000-8000-000000000303', 1, 'TEXTO', 'El agua del nevado baja por riachuelos, llena las lagunas y riega los campos. Si cuidamos los pastos andinos, ellos absorben la lluvia como esponjas naturales.'),
  ('bb000000-0000-4000-8000-000000000304', 'fa000000-0000-4000-8000-000000000304', 1, 'TEXTO', 'La tierra negra tiene humus: restos de hojitas secas que alimentan a la planta. La arena sola se seca muy rápido. Para sembrar papas necesitamos tierra con nutrientes.'),
  ('bb000000-0000-4000-8000-000000000305', 'fa000000-0000-4000-8000-000000000305', 1, 'TEXTO', 'La vicuña tiene pestañas largas para protegerse del viento helado y pezuñas con almohadillas que no dañan el suelo de la puna. Es un tesoro silvestre del Perú.'),
  ('bb000000-0000-4000-8000-000000000306', 'fa000000-0000-4000-8000-000000000306', 1, 'TEXTO', 'Un río limpio tiene pececitos y agua cristalina. Cuando dejamos basura en las orillas, el agua se enferma. ¡Tú eres un científico guardián del agua!'),

  -- Personal Social 2.°
  ('bb000000-0000-4000-8000-000000000401', 'fa000000-0000-4000-8000-000000000401', 1, 'TEXTO', 'Todos tenemos una familia con historias únicas. Algunos abuelos hablan quechua, otros viven en el campo o en la ciudad. Nuestras diferencias nos enriquecen.'),
  ('bb000000-0000-4000-8000-000000000402', 'fa000000-0000-4000-8000-000000000402', 1, 'TEXTO', 'El Ayni es una práctica milenaria: si mi vecino necesita techar su casita, todos vamos a ayudarlo. Luego, cuando nosotros necesitemos ayuda, él vendrá con alegría.'),
  ('bb000000-0000-4000-8000-000000000403', 'fa000000-0000-4000-8000-000000000403', 1, 'TEXTO', 'En el barrio de Santa Ana, Don Jesús talla la piedra de Huamanga con cincel fino. Su abuelo le enseñó el oficio y él se lo enseña a sus hijos para que el arte nunca muera.'),
  ('bb000000-0000-4000-8000-000000000404', 'fa000000-0000-4000-8000-000000000404', 1, 'TEXTO', 'En el recreo compartimos la pelota, respetamos los turnos en el columpio y decimos "por favor" y "gracias". Una escuela con respeto es un hogar donde da gusto aprender.'),
  ('bb000000-0000-4000-8000-000000000405', 'fa000000-0000-4000-8000-000000000405', 1, 'TEXTO', 'Los parques, bancos y monumentos son de todos. Cuando los cuidamos sin rayarlos ni tirar papeles, mostramos cariño y orgullo por nuestro pueblo.'),
  ('bb000000-0000-4000-8000-000000000406', 'fa000000-0000-4000-8000-000000000406', 1, 'TEXTO', 'Imagina que pintas un mural en la pared de tu colegio: dibuja a tus amigos jugando, a Rupi volando y las montañas protectoras al fondo.'),

  -- Arte y Cultura 2.°
  ('bb000000-0000-4000-8000-000000000501', 'fa000000-0000-4000-8000-000000000501', 1, 'TEXTO', 'La arcilla se amasa con agua hasta que queda suave como plastilina. Con tus pulgares dale forma redonda para crear un platito o la figura de un cuy.'),
  ('bb000000-0000-4000-8000-000000000502', 'fa000000-0000-4000-8000-000000000502', 1, 'TEXTO', 'La cochinilla vive en las pencas de tuna. Al machacarla suavemente con gotitas de limón, aparece un color carmín brillante usado en tejidos y retablos.'),
  ('bb000000-0000-4000-8000-000000000503', 'fa000000-0000-4000-8000-000000000503', 1, 'TEXTO', 'El retablo ayacuchano tiene dos puertas decoradas con flores de retama. Al abrirlo, muestra dos pisos: arriba el cielo con estrellas y abajo la fiesta del pueblo.'),
  ('bb000000-0000-4000-8000-000000000504', 'fa000000-0000-4000-8000-000000000504', 1, 'TEXTO', 'El charango se toca con rasgueo alegre y la quena suena como el viento que baja de la cordillera. Sigue el ritmo con las palmas de tus manos: ¡pam, pam, pam!'),
  ('bb000000-0000-4000-8000-000000000505', 'fa000000-0000-4000-8000-000000000505', 1, 'TEXTO', 'Los danzantes de tijeras llevan sombreros con plumas de pavo real y espejitos brillantes. Hacen sonar sus tijeras de acero mientras bailan sin perder el equilibrio.'),
  ('bb000000-0000-4000-8000-000000000506', 'fa000000-0000-4000-8000-000000000506', 1, 'TEXTO', 'Coloca tu dibujo o figura modelada en la mesa del aula. Cuenta a tus compañeros por qué elegiste esos colores y qué historia quisiste expresar.'),

  -- Matemática 3.°
  ('bb000000-0000-4000-8000-000000000701', 'fa000000-0000-4000-8000-000000000701', 1, 'TEXTO', 'En un andén de Wari hay 3 hileras de maíz y cada hilera tiene 5 mazorcas. En vez de sumar 5 + 5 + 5, multiplicamos: 3 veces 5 es igual a 15 mazorcas.'),
  ('bb000000-0000-4000-8000-000000000702', 'fa000000-0000-4000-8000-000000000702', 1, 'TEXTO', 'Mira las paredes de piedra: los bloques forman rectángulos y esquinas rectas de 90 grados. Cuenta cuántos vértices tiene una roca rectangular.'),
  ('bb000000-0000-4000-8000-000000000703', 'fa000000-0000-4000-8000-000000000703', 1, 'TEXTO', 'El patio ceremonial mide 6 metros de largo y 4 metros de ancho. Para cercarlo con sogas sumamos: 6 + 4 + 6 + 4 = 20 metros de perímetro total.'),
  ('bb000000-0000-4000-8000-000000000704', 'fa000000-0000-4000-8000-000000000704', 1, 'TEXTO', 'En la balanza, 1 kilo de quinua equivale a 2 bolsas de medio kilo. Si llevas 3 kilos de quinua, ¿cuántas bolsas de medio kilo necesitas en total?'),
  ('bb000000-0000-4000-8000-000000000705', 'fa000000-0000-4000-8000-000000000705', 1, 'TEXTO', 'Un quipu tiene nudos que siguen una regla: 10, 20, 30, __, 50. El número que falta es 40. Descubre el patrón que suma diez en cada tramo.'),
  ('bb000000-0000-4000-8000-000000000706', 'fa000000-0000-4000-8000-000000000706', 1, 'TEXTO', 'El maestro constructor tiene 24 bloques de piedra para armar 4 columnas iguales. ¿Cuántos bloques pondrá en cada columna? 24 entre 4 es igual a 6.')
ON DUPLICATE KEY UPDATE texto_cuerpo = VALUES(texto_cuerpo);

-- ============================================================================
-- 8. PREGUNTAS Y OPCIONES FORMATIVAS PARA LAS ACTIVIDADES
-- ============================================================================
INSERT INTO evaluacion_pregunta (id, version_actividad_id, numero_secuencia, tipo, enunciado, explicacion, puntos) VALUES
  -- Pregunta Comunicación 1
  ('e2000000-0000-4000-8000-000000000201', 'fa000000-0000-4000-8000-000000000201', 1, 'OPCION_MULTIPLE',
   '¿Qué cualidad demostró el pequeño cuy para superar el reto del zorro?',
   'El cuy fue muy atento y perseverante, demostrando que pensar con calma vale más que la soberbia.', 10),
  -- Pregunta Ciencia y Tecnología 1
  ('e2000000-0000-4000-8000-000000000301', 'fa000000-0000-4000-8000-000000000301', 1, 'OPCION_MULTIPLE',
   '¿Por qué el Gallito de las Rocas ayuda a que crezcan nuevos árboles en el bosque?',
   'Al alimentarse de frutos silvestres, el gallito dispersa las semillas por todo el bosque al volar.', 10),
  -- Pregunta Personal Social 1
  ('e2000000-0000-4000-8000-000000000401', 'fa000000-0000-4000-8000-000000000401', 1, 'OPCION_MULTIPLE',
   '¿Qué valor representa la tradición andina del Ayni en nuestras comunidades?',
   'El Ayni es la ayuda mutua y la solidaridad entre vecinos: hoy por ti, mañana por mí.', 10),
  -- Pregunta Arte y Cultura 1
  ('e2000000-0000-4000-8000-000000000501', 'fa000000-0000-4000-8000-000000000501', 1, 'OPCION_MULTIPLE',
   '¿Qué tinte natural tradicional nos da el color rojo vivo en la artesanía ayacuchana?',
   'La cochinilla, un pequeño insecto que vive en la tuna, produce el tinte rojo carmín tradicional.', 10),
  -- Pregunta Matemática 3.° 1
  ('e2000000-0000-4000-8000-000000000701', 'fa000000-0000-4000-8000-000000000701', 1, 'OPCION_MULTIPLE',
   'Si en un andén hay 3 hileras con 5 mazorcas cada una, ¿cuántas mazorcas hay en total?',
   'Multiplicamos 3 hileras por 5 mazorcas: 3 x 5 = 15 mazorcas en total.', 10)
ON DUPLICATE KEY UPDATE enunciado = VALUES(enunciado), explicacion = VALUES(explicacion);

-- Opciones de respuesta
INSERT INTO evaluacion_opcion_respuesta (id, pregunta_id, numero_secuencia, texto, es_correcta) VALUES
  -- Opciones Com 1
  ('f2000000-0000-4000-8000-000000000211', 'e2000000-0000-4000-8000-000000000201', 1, 'Fue atento, perseverante y pensó con calma', TRUE),
  ('f2000000-0000-4000-8000-000000000212', 'e2000000-0000-4000-8000-000000000201', 2, 'Corrió sin mirar el camino', FALSE),
  ('f2000000-0000-4000-8000-000000000213', 'e2000000-0000-4000-8000-000000000201', 3, 'Se quedó dormido bajo una piedra', FALSE),

  -- Opciones CyT 1
  ('f2000000-0000-4000-8000-000000000311', 'e2000000-0000-4000-8000-000000000301', 1, 'Porque come frutos y dispersa sus semillas al volar', TRUE),
  ('f2000000-0000-4000-8000-000000000312', 'e2000000-0000-4000-8000-000000000301', 2, 'Porque corta las ramas secas con su pico', FALSE),
  ('f2000000-0000-4000-8000-000000000313', 'e2000000-0000-4000-8000-000000000301', 3, 'Porque duerme en el suelo de la chacra', FALSE),

  -- Opciones Soc 1
  ('f2000000-0000-4000-8000-000000000411', 'e2000000-0000-4000-8000-000000000401', 1, 'La solidaridad y ayuda mutua entre familias', TRUE),
  ('f2000000-0000-4000-8000-000000000412', 'e2000000-0000-4000-8000-000000000401', 2, 'Trabajar en silencio sin hablar con nadie', FALSE),
  ('f2000000-0000-4000-8000-000000000413', 'e2000000-0000-4000-8000-000000000401', 3, 'Guardar todas las herramientas para uno solo', FALSE),

  -- Opciones Arte 1
  ('f2000000-0000-4000-8000-000000000511', 'e2000000-0000-4000-8000-000000000501', 1, 'La cochinilla que vive en las pencas de tuna', TRUE),
  ('f2000000-0000-4000-8000-000000000512', 'e2000000-0000-4000-8000-000000000501', 2, 'Témpera de plástico de la librería', FALSE),
  ('f2000000-0000-4000-8000-000000000513', 'e2000000-0000-4000-8000-000000000501', 3, 'Ceniza de carbón apagado', FALSE),

  -- Opciones Mat 3 1
  ('f2000000-0000-4000-8000-000000000711', 'e2000000-0000-4000-8000-000000000701', 1, '15 mazorcas (3 x 5 = 15)', TRUE),
  ('f2000000-0000-4000-8000-000000000712', 'e2000000-0000-4000-8000-000000000701', 2, '8 mazorcas (3 + 5 = 8)', FALSE),
  ('f2000000-0000-4000-8000-000000000713', 'e2000000-0000-4000-8000-000000000701', 3, '12 mazorcas', FALSE)
ON DUPLICATE KEY UPDATE texto = VALUES(texto);

-- ============================================================================
-- 9. INSCRIPCIONES Y PROGRESO INICIAL PARA EL ESTUDIANTE DEMO
-- ============================================================================
-- Estudiante demo: 11111111-1111-4111-8111-111111111111 (Mateo Quispe)
-- Permite que pueda navegar y abrir inmediatamente cualquiera de estas rutas.

INSERT INTO aprendizaje_inscripcion_ruta (
  id, version_ruta_id, estudiante_id, estado, ultimo_nodo_visitado_id, ultima_visita_en, inscrito_en
) VALUES
  -- Comunicación 2.°
  ('e0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000002', '11111111-1111-4111-8111-111111111111',
   'ACTIVO', 'ca000000-0000-4000-8000-000000000211', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  -- Ciencia y Tecnología 2.°
  ('e0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000003', '11111111-1111-4111-8111-111111111111',
   'ACTIVO', 'ca000000-0000-4000-8000-000000000311', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  -- Personal Social 2.°
  ('e0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000004', '11111111-1111-4111-8111-111111111111',
   'ACTIVO', 'ca000000-0000-4000-8000-000000000411', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  -- Arte y Cultura 2.°
  ('e0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000005', '11111111-1111-4111-8111-111111111111',
   'ACTIVO', 'ca000000-0000-4000-8000-000000000511', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  -- Matemática 3.°
  ('e0000000-0000-4000-8000-000000000007', 'b0000000-0000-4000-8000-000000000007', '11111111-1111-4111-8111-111111111111',
   'ACTIVO', 'ca000000-0000-4000-8000-000000000711', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

-- Progreso por nodo para cada ruta (Parada 1 DISPONIBLE, Paradas 2..6 BLOQUEADAS)
INSERT INTO aprendizaje_progreso_nodo (
  inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en, primer_acceso_en, completado_en
) VALUES
  -- Comunicación 2.°
  ('e0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000002', 'ca000000-0000-4000-8000-000000000211', 'DISPONIBLE', UTC_TIMESTAMP(6), NULL, NULL),
  ('e0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000002', 'ca000000-0000-4000-8000-000000000212', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000002', 'ca000000-0000-4000-8000-000000000213', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000002', 'ca000000-0000-4000-8000-000000000214', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000002', 'ca000000-0000-4000-8000-000000000215', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000002', 'ca000000-0000-4000-8000-000000000216', 'BLOQUEADO', NULL, NULL, NULL),

  -- Ciencia y Tecnología 2.°
  ('e0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000003', 'ca000000-0000-4000-8000-000000000311', 'DISPONIBLE', UTC_TIMESTAMP(6), NULL, NULL),
  ('e0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000003', 'ca000000-0000-4000-8000-000000000312', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000003', 'ca000000-0000-4000-8000-000000000313', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000003', 'ca000000-0000-4000-8000-000000000314', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000003', 'ca000000-0000-4000-8000-000000000315', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000003', 'ca000000-0000-4000-8000-000000000316', 'BLOQUEADO', NULL, NULL, NULL),

  -- Personal Social 2.°
  ('e0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000004', 'ca000000-0000-4000-8000-000000000411', 'DISPONIBLE', UTC_TIMESTAMP(6), NULL, NULL),
  ('e0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000004', 'ca000000-0000-4000-8000-000000000412', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000004', 'ca000000-0000-4000-8000-000000000413', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000004', 'ca000000-0000-4000-8000-000000000414', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000004', 'ca000000-0000-4000-8000-000000000415', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000004', 'ca000000-0000-4000-8000-000000000416', 'BLOQUEADO', NULL, NULL, NULL),

  -- Arte y Cultura 2.°
  ('e0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000005', 'ca000000-0000-4000-8000-000000000511', 'DISPONIBLE', UTC_TIMESTAMP(6), NULL, NULL),
  ('e0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000005', 'ca000000-0000-4000-8000-000000000512', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000005', 'ca000000-0000-4000-8000-000000000513', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000005', 'ca000000-0000-4000-8000-000000000514', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000005', 'ca000000-0000-4000-8000-000000000515', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000005', 'ca000000-0000-4000-8000-000000000516', 'BLOQUEADO', NULL, NULL, NULL),

  -- Matemática 3.°
  ('e0000000-0000-4000-8000-000000000007', 'b0000000-0000-4000-8000-000000000007', 'ca000000-0000-4000-8000-000000000711', 'DISPONIBLE', UTC_TIMESTAMP(6), NULL, NULL),
  ('e0000000-0000-4000-8000-000000000007', 'b0000000-0000-4000-8000-000000000007', 'ca000000-0000-4000-8000-000000000712', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000007', 'b0000000-0000-4000-8000-000000000007', 'ca000000-0000-4000-8000-000000000713', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000007', 'b0000000-0000-4000-8000-000000000007', 'ca000000-0000-4000-8000-000000000714', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000007', 'b0000000-0000-4000-8000-000000000007', 'ca000000-0000-4000-8000-000000000715', 'BLOQUEADO', NULL, NULL, NULL),
  ('e0000000-0000-4000-8000-000000000007', 'b0000000-0000-4000-8000-000000000007', 'ca000000-0000-4000-8000-000000000716', 'BLOQUEADO', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE estado = VALUES(estado);
