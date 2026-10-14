-- V15: Diversificación geográfica cultural en Costa, Sierra y Selva para Roadmaps CNEB (1.° a 6.°)
-- Ambientación auténtica con pertinencia geográfica, mensajes de ánimo y datos formativos.
-- Esquema: rupi (MySQL 8.4 InnoDB / UTF-8 mb4). Idempotente.

USE rupi;
SET NAMES utf8mb4;

-- ----------------------------------------------------------------------------
-- 1. ACTUALIZACIÓN DE CONTEXTOS CULTURALES COSTA, SIERRA Y SELVA
-- ----------------------------------------------------------------------------

-- 1.° PRIMARIA
-- MAT-1P-INICIO: COSTA (Paracas e Ica)
UPDATE curriculo_contexto_cultural SET
  codigo = 'PARACAS_COSTA_MAT1',
  ciudad = 'Paracas, Ica',
  lugar = 'Reserva Nacional de Paracas y Bahía',
  titulo = 'Primeros Números y Formas en la Costa de Paracas',
  descripcion = 'Descubre las figuras y los números en las orillas del mar peruano contando conchitas, olas y flamencos.',
  mensaje_animo = '¡Avanza por la costa! Cada número que descubres brilla como el sol en la arena de Paracas.',
  imagen_url = '/images/costa-paracas.jpg',
  imagen_alt = 'Paisaje costero de la Reserva Nacional de Paracas con el mar azul y aves guaneras',
  credito_autor = 'Sernanp Perú',
  credito_fuente = 'Archivo Patrimonial MINCETUR',
  credito_licencia = 'CC-BY-4.0',
  credito_url = 'https://www.sernanp.gob.pe'
WHERE ruta_id = 'd0000000-0000-4000-8000-000000000101';

-- COM-1P-LETRAS: SIERRA (Valle del Mantaro y Ayacucho)
UPDATE curriculo_contexto_cultural SET
  codigo = 'VALLE_SIERRA_COM1',
  ciudad = 'Huanta, Ayacucho',
  lugar = 'Valle Verde y Riberas del Río Cachi',
  titulo = 'Vocales, Sonidos y Rimas con el Viento Andino',
  descripcion = 'Iniciación a la lectura y escritura con rimas tradicionales y los alegres sonidos de la naturaleza andina.',
  mensaje_animo = '¡Escucha con atención! Las palabras vuelan como avecillas en el cielo de los Andes.',
  imagen_url = '/images/ayacucho-plaza.jpg',
  imagen_alt = 'Valle andino con campos de cultivo y montañas bajo el cielo azul',
  credito_autor = 'Archivo Regional de Ayacucho',
  credito_fuente = 'DRE Ayacucho',
  credito_licencia = 'CC-BY-4.0',
  credito_url = 'https://www.gob.pe/regionayacucho'
WHERE ruta_id = 'd0000000-0000-4000-8000-000000000102';

-- CYT-1P-EXPLOR: SELVA (Iquitos y la Selva Amazónica)
UPDATE curriculo_contexto_cultural SET
  codigo = 'IQUITOS_SELVA_CYT1',
  ciudad = 'Iquitos, Loreto',
  lugar = 'Bosque Tropical y Jardín Etnobotánico Amazónico',
  titulo = 'Los Cinco Sentidos en la Selva Amazónica',
  descripcion = 'Siente el aroma de las flores silvestres, escucha los cantos de aves y descubre las texturas de la gran selva.',
  mensaje_animo = '¡Abre bien tus ojos y oídos! La selva amazónica tiene mil secretos para aprender.',
  imagen_url = '/images/selva-amazonas.jpg',
  imagen_alt = 'Frondoso bosque tropical amazónico con río serpenteante y vegetación brillante',
  credito_autor = 'IIAP Instituto de Investigaciones de la Amazonía Peruana',
  credito_fuente = 'IIAP Loreto',
  credito_licencia = 'CC-BY-4.0',
  credito_url = 'http://www.iiap.gob.pe'
WHERE ruta_id = 'd0000000-0000-4000-8000-000000000103';


-- 2.° PRIMARIA
-- MAT-2P-PLAZA: SIERRA (Plaza Mayor de Ayacucho y Retablos)
UPDATE curriculo_contexto_cultural SET
  codigo = 'AYACUCHO_PLAZA_MAT2',
  ciudad = 'Huamanga, Ayacucho',
  lugar = 'Plaza Mayor y Taller de Retablos de Santa Ana',
  titulo = 'Aventura Matemática en la Plaza de Ayacucho',
  descripcion = 'Retos matemáticos, geometría en arcos coloniales y conteo con retablos tradicionales guiados por Rupi.',
  mensaje_animo = '¡Cada desafío resuelto te acerca a la gran cima del retablo andino! Sigue adelante con Rupi.',
  imagen_url = '/images/ayacucho-plaza.jpg',
  imagen_alt = 'Plaza Mayor de Huamanga con sus arquerías blancas y flores andinas'
WHERE ruta_id = 'd0000000-0000-4000-8000-000000000001';

-- COM-2P-VALLE: COSTA (Costa Verde y la Civilización de Caral)
UPDATE curriculo_contexto_cultural SET
  codigo = 'CARAL_COSTA_COM2',
  ciudad = 'Barranca, Lima',
  lugar = 'Valle Sagrado de Caral y Litoral Marino',
  titulo = 'Cuentos y Relatos del Mar y Caral',
  descripcion = 'Comprensión lectora de narraciones ancestrales sobre los primeros pescadores y constructores de América.',
  mensaje_animo = '¡Las historias del mar te enseñan a imaginar mundos nuevos y escribir con emoción!',
  imagen_url = '/images/costa-paracas.jpg',
  imagen_alt = 'Vista aérea de las pirámides ceremoniales de Caral junto al valle costeño'
WHERE ruta_id = 'd0000000-0000-4000-8000-000000000002';

-- CYT-2P-BIODIV: SELVA (Tambopata: Hogar del Gallito de las Rocas)
UPDATE curriculo_contexto_cultural SET
  codigo = 'TAMBOPATA_SELVA_CYT2',
  ciudad = 'Puerto Maldonado, Madre de Dios',
  lugar = 'Reserva Nacional Tambopata y Bosque Nuboso',
  titulo = 'Exploradores de la Biodiversidad en Tambopata',
  descripcion = 'Investigación escolar sobre plantas medicinales, animales en peligro y el hábitat del Gallito de las Rocas.',
  mensaje_animo = '¡Rupi te acompaña volando por los árboles más altos! Cuidar la naturaleza es nuestra misión escolar.',
  imagen_url = '/images/selva-amazonas.jpg',
  imagen_alt = 'Laguna Sandoval en Tambopata rodeada de palmeras de aguaje y fauna silvestre'
WHERE ruta_id = 'd0000000-0000-4000-8000-000000000003';


-- 3.° PRIMARIA
-- MAT-3P-WARI: SIERRA (Complejo Arqueológico Wari)
UPDATE curriculo_contexto_cultural SET
  codigo = 'WARI_SIERRA_MAT3',
  ciudad = 'Quinua, Ayacucho',
  lugar = 'Complejo Arqueológico Wari y Pampa de Ayacucho',
  titulo = 'Geometría y Desafíos en la Gran Ciudad Wari',
  descripcion = 'Descubrimiento de patrones, ángulos y simetrías en los imponentes muros y cerámicas de la cultura Wari.',
  mensaje_animo = '¡Los grandes constructores Wari usaban la matemática para levantar murallas eternas!',
  imagen_url = '/images/ayacucho-plaza.jpg',
  imagen_alt = 'Muros de piedra tallada en el complejo arqueológico Wari'
WHERE ruta_id = 'd0000000-0000-4000-8000-000000000007';

-- COM-3P-MITOS: SELVA (Mitos del Río Amazonas)
UPDATE curriculo_contexto_cultural SET
  codigo = 'AMAZONAS_SELVA_COM3',
  ciudad = 'Nauta, Loreto',
  lugar = 'Nacimiento del Río Amazonas y Selva Baja',
  titulo = 'Mitos y Leyendas del Gran Río Amazonas',
  descripcion = 'Lectura comprensiva de leyendas sobre el Bufeo Colorado, la Yacumama y la tradición oral de los pueblos ribereños.',
  mensaje_animo = '¡Cada relato de la selva guarda una enseñanza sobre el respeto a la vida y al río!',
  imagen_url = '/images/selva-amazonas.jpg',
  imagen_alt = 'El majestuoso río Amazonas serpenteando a través de la densa selva verde'
WHERE ruta_id = 'd0000000-0000-4000-8000-000000000302';

-- CYT-3P-ENERGIA: COSTA (Desierto de Ica y Energía del Sol y del Viento)
UPDATE curriculo_contexto_cultural SET
  codigo = 'PARACAS_COSTA_CYT3',
  ciudad = 'Ica / Marcona',
  lugar = 'Parque Eólico Costero y Desierto de Paracas',
  titulo = 'La Fuerza del Viento y el Sol Costero',
  descripcion = 'Indagación experimental sobre la transformación de la energía del viento Paraca y los rayos de sol en electricidad limpia.',
  mensaje_animo = '¡El sol y el viento del desierto peruano generan la energía limpia del futuro!',
  imagen_url = '/images/costa-paracas.jpg',
  imagen_alt = 'Aerogeneradores modernos girando frente al océano Pacífico bajo cielo despejado'
WHERE ruta_id = 'd0000000-0000-4000-8000-000000000303';


-- 4.° PRIMARIA
-- MAT-4P-FRACC: COSTA (Caletas Pesqueras y Ferias de Lima)
UPDATE curriculo_contexto_cultural SET
  codigo = 'CALETA_COSTA_MAT4',
  ciudad = 'Chorrillos, Lima',
  lugar = 'Caleta de Pescadores y Mercado Tradicional',
  titulo = 'Fracciones y Medidas en la Caleta de Pescadores',
  descripcion = 'Aplicación práctica de fracciones en la repartición justa de la pesca artesanal y medidas de longitud.',
  mensaje_animo = '¡Las fracciones son el lenguaje para compartir en equipo con justicia y precisión!',
  imagen_url = '/images/costa-paracas.jpg',
  imagen_alt = 'Botes de pesca artesanal meciéndose en el mar de la caleta de Chorrillos'
WHERE ruta_id = 'd0000000-0000-4000-8000-000000000401';

-- COM-4P-TEXTOS: SIERRA (Lago Titicaca y los Uros)
UPDATE curriculo_contexto_cultural SET
  codigo = 'TITICACA_SIERRA_COM4',
  ciudad = 'Puno',
  lugar = 'Lago Titicaca e Islas Flotantes de los Uros',
  titulo = 'Textos Informativos sobre el Lago Sagrado de los Incas',
  descripcion = 'Análisis de artículos informativos, infografías y crónicas sobre la biodiversidad y cultura del lago navegable más alto del mundo.',
  mensaje_animo = '¡Navega por las palabras como una balsa de totora en las aguas azules del Titicaca!',
  imagen_url = '/images/ayacucho-plaza.jpg',
  imagen_alt = 'Balsa tradicional de totora en el lago Titicaca con fondo de montañas nevadas'
WHERE ruta_id = 'd0000000-0000-4000-8000-000000000402';

-- CYT-4P-ECOSIS: SELVA (Reserva Nacional Pacaya Samiria)
UPDATE curriculo_contexto_cultural SET
  codigo = 'PACAYA_SELVA_CYT4',
  ciudad = 'Requena, Loreto',
  lugar = 'Reserva Nacional Pacaya Samiria (La Selva de los Espejos)',
  titulo = 'Ecosistemas Acuáticos de la Selva de los Espejos',
  descripcion = 'Estudio de cadenas tróficas, el ciclo de inundación del bosque y la preservación del paiche amazónico.',
  mensaje_animo = '¡En la selva de los espejos, cada ser vivo cumple un rol vital para el equilibrio del planeta!',
  imagen_url = '/images/selva-amazonas.jpg',
  imagen_alt = 'Reflejo perfecto del cielo y los árboles en las aguas oscuras de Pacaya Samiria'
WHERE ruta_id = 'd0000000-0000-4000-8000-000000000403';


-- 5.° PRIMARIA
-- MAT-5P-DECIM: SIERRA (Terrazas Agrícolas de Moray y Vilcashuamán)
UPDATE curriculo_contexto_cultural SET
  codigo = 'MORAY_SIERRA_MAT5',
  ciudad = 'Cusco / Vilcashuamán',
  lugar = 'Andenes Circulares de Moray y Templo del Sol',
  titulo = 'Números Decimales y Medidas en las Terrazas del Sol',
  descripcion = 'Cálculos de precisión con decimales, áreas de andenes circulares y proporciones en la ingeniería incaica.',
  mensaje_animo = '¡Los ingenieros andinos dominaban las medidas con exactitud matemática asombrosa!',
  imagen_url = '/images/ayacucho-plaza.jpg',
  imagen_alt = 'Terrazas agrícolas concéntricas de Moray excavadas en la tierra del Valle Sagrado'
WHERE ruta_id = 'd0000000-0000-4000-8000-000000000501';

-- COM-5P-ARGUM: COSTA (Ciudadela de Barro de Chan Chan)
UPDATE curriculo_contexto_cultural SET
  codigo = 'CHANCHAN_COSTA_COM5',
  ciudad = 'Trujillo, La Libertad',
  lugar = 'Ciudadela de Chan Chan y Huaca de la Luna',
  titulo = 'Textos Argumentativos: Protegiendo Nuestro Patrimonio Costero',
  descripcion = 'Redacción y fundamentación de opiniones para proteger las murallas de barro más grandes de América precolombina.',
  mensaje_animo = '¡Tu voz y tus argumentos son la fuerza para defender la historia de nuestro pueblo!',
  imagen_url = '/images/costa-paracas.jpg',
  imagen_alt = 'Relieves geométricos y de peces en los muros de barro de Chan Chan'
WHERE ruta_id = 'd0000000-0000-4000-8000-000000000502';

-- CYT-5P-CUERPO: SELVA (Cataratas de Ahuashiyacu y Saberes Botánicos)
UPDATE curriculo_contexto_cultural SET
  codigo = 'AHUASHIYACU_SELVA_CYT5',
  ciudad = 'Tarapoto, San Martín',
  lugar = 'Cordillera Escalera y Cataratas de Ahuashiyacu',
  titulo = 'Plantas Medicinales y Sistemas del Cuerpo Humano',
  descripcion = 'Indagación científica sobre el sistema circulatorio, la respiración y los principios activos de la botánica amazónica.',
  mensaje_animo = '¡La ciencia escolar se nutre de los saberes milenarios de las comunidades de la selva!',
  imagen_url = '/images/selva-amazonas.jpg',
  imagen_alt = 'Catarata de Ahuashiyacu cayendo en una poza cristalina en medio de la vegetación'
WHERE ruta_id = 'd0000000-0000-4000-8000-000000000503';


-- 6.° PRIMARIA
-- MAT-6P-PROPOR: COSTA (Líneas y Geoglifos de Nazca)
UPDATE curriculo_contexto_cultural SET
  codigo = 'NAZCA_COSTA_MAT6',
  ciudad = 'Nazca, Ica',
  lugar = 'Pampas de Jumana y Geoglifos de Nazca',
  titulo = 'Proporcionalidad y Escalas en las Líneas de Nazca',
  descripcion = 'Razones, proporciones, planos y escalas geométricas aplicadas a los geoglifos gigantes trazados en el desierto.',
  mensaje_animo = '¡Desde el cielo, las matemáticas de Nazca nos recuerdan la grandeza del ingenio peruano!',
  imagen_url = '/images/costa-paracas.jpg',
  imagen_alt = 'Geoglifo del colibrí gigante trazado en la pampa desértica de Nazca'
WHERE ruta_id = 'd0000000-0000-4000-8000-000000000601';

-- COM-6P-CRONIC: SELVA (Parque Nacional del Manu)
UPDATE curriculo_contexto_cultural SET
  codigo = 'MANU_SELVA_COM6',
  ciudad = 'Manu, Madre de Dios',
  lugar = 'Reserva de Biósfera del Manu',
  titulo = 'Crónicas y Reportajes desde el Corazón del Manu',
  descripcion = 'Elaboración de crónicas periodísticas y reportajes de divulgación científica sobre la protección de la selva virgen.',
  mensaje_animo = '¡Escribe con la pasión de un cronista explorador y lleva la voz del Manu al mundo entero!',
  imagen_url = '/images/selva-amazonas.jpg',
  imagen_alt = 'Río Manu serpenteando en la reserva de biósfera con bancos de arena y bosque primario'
WHERE ruta_id = 'd0000000-0000-4000-8000-000000000602';

-- CYT-6P-CLIMA: SIERRA (Parque Nacional Huascarán)
UPDATE curriculo_contexto_cultural SET
  codigo = 'HUASCARAN_SIERRA_CYT6',
  ciudad = 'Huaraz, Ancash',
  lugar = 'Parque Nacional Huascarán y Cordillera Blanca',
  titulo = 'Cambio Climático, Glaciares y Energías Renovables',
  descripcion = 'Investigación sobre el retroceso glaciar en la Cordillera Blanca, el ciclo del agua dulce y la transición ecológica.',
  mensaje_animo = '¡En la cumbre más alta del Perú, tu conocimiento científico es la esperanza para el futuro!',
  imagen_url = '/images/ayacucho-plaza.jpg',
  imagen_alt = 'Pico nevado del Huascarán reflejado en una laguna turquesa de la Cordillera Blanca'
WHERE ruta_id = 'd0000000-0000-4000-8000-000000000603';
