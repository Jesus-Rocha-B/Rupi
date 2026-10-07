-- Semilla de desarrollo para HU-01: Mapa interactivo
-- Esquema: rupi (MySQL 8 / MariaDB)

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

USE rupi;

-- 1. Asegurar nombres con tildes y caracteres correctos en curriculo_grado
UPDATE curriculo_grado SET nombre = '1.° de primaria' WHERE numero_grado = 1;
UPDATE curriculo_grado SET nombre = '2.° de primaria' WHERE numero_grado = 2;
UPDATE curriculo_grado SET nombre = '3.° de primaria' WHERE numero_grado = 3;
UPDATE curriculo_grado SET nombre = '4.° de primaria' WHERE numero_grado = 4;
UPDATE curriculo_grado SET nombre = '5.° de primaria' WHERE numero_grado = 5;
UPDATE curriculo_grado SET nombre = '6.° de primaria' WHERE numero_grado = 6;

-- 2. Estudiante de prueba (Credenciales de desarrollo: usuario 'estudiante.demo', contraseña '123456')
INSERT INTO identidad_cuenta_usuario (
  id, nombre_usuario, correo, hash_clave, estado, desactivado_en
) VALUES (
  '11111111-1111-4111-8111-111111111111',
  'estudiante.demo',
  'estudiante.demo@rupi.pe',
  'pbkdf2_sha256$600000$rupi-dev-hu01$8LkMHYbhvu/QFEd17e3Ilkb37nI2vSgtJUg4jaCaNjE=',
  'ACTIVO',
  NULL
) ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

INSERT INTO identidad_perfil_usuario (
  usuario_id, nombre, apellido, nombre_preferido, alias_publico, mostrar_en_clasificacion, idioma, zona_horaria
) VALUES (
  '11111111-1111-4111-8111-111111111111',
  'Mateo',
  'Quispe',
  'Mateo',
  'mateo2026',
  FALSE,
  'es-PE',
  'America/Lima'
) ON DUPLICATE KEY UPDATE nombre = 'Mateo';

-- 3. Catálogo curricular oficial y Área Matemática
INSERT INTO curriculo_version_catalogo (
  id, etiqueta_version, referencia_origen, vigente_desde, estado
) VALUES (
  'c0000000-0000-4000-8000-000000000001',
  'CNEB-2024',
  'Currículo Nacional de la Educación Básica - MINEDU',
  '2024-01-01',
  'ACTIVO'
) ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

INSERT INTO curriculo_area (
  id, version_catalogo_id, codigo, nombre
) VALUES (
  'a0000000-0000-4000-8000-000000000001',
  'c0000000-0000-4000-8000-000000000001',
  'MAT',
  'Matemática'
) ON DUPLICATE KEY UPDATE nombre = 'Matemática';

-- 4. Ruta y Versión de Ruta publicada para 2.º grado de primaria
INSERT INTO aprendizaje_ruta (
  id, propietario_id, codigo, titulo, descripcion
) VALUES (
  'd0000000-0000-4000-8000-000000000001',
  '11111111-1111-4111-8111-111111111111',
  'MAT-2P-PLAZA',
  'Matemática: aventura en la plaza',
  'Recorrido de diez paradas por la Plaza Mayor de Ayacucho aprendiendo números, operaciones y patrones.'
) ON DUPLICATE KEY UPDATE titulo = 'Matemática: aventura en la plaza';

INSERT INTO aprendizaje_version_ruta (
  id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en
) VALUES (
  'b0000000-0000-4000-8000-000000000001',
  'd0000000-0000-4000-8000-000000000001',
  'c0000000-0000-4000-8000-000000000001',
  '00000000-0000-4000-8000-000000000002',
  'a0000000-0000-4000-8000-000000000001',
  1,
  'PUBLICADO',
  CURRENT_TIMESTAMP(6)
) ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

-- 5. Diez actividades y sus versiones (usando UUIDs hexadecimales válidos)
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000000000001', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000002', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000003', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000004', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000005', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000006', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000007', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000008', 'RETO', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000009', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000010', 'RETO', '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (
  id, actividad_id, numero_version, titulo, dificultad, minutos_estimados, estado, publicado_en
) VALUES
  ('fa000000-0000-4000-8000-000000000001', 'ac000000-0000-4000-8000-000000000001', 1, 'Números en la plaza', 1, 8, 'PUBLICADO', CURRENT_TIMESTAMP(6)),
  ('fa000000-0000-4000-8000-000000000002', 'ac000000-0000-4000-8000-000000000002', 1, 'Sumas con retablos', 1, 10, 'PUBLICADO', CURRENT_TIMESTAMP(6)),
  ('fa000000-0000-4000-8000-000000000003', 'ac000000-0000-4000-8000-000000000003', 1, 'La aventura de restar', 2, 8, 'PUBLICADO', CURRENT_TIMESTAMP(6)),
  ('fa000000-0000-4000-8000-000000000004', 'ac000000-0000-4000-8000-000000000004', 1, 'Compras en el mercado', 2, 12, 'PUBLICADO', CURRENT_TIMESTAMP(6)),
  ('fa000000-0000-4000-8000-000000000005', 'ac000000-0000-4000-8000-000000000005', 1, 'Patrones en los tejidos', 2, 9, 'PUBLICADO', CURRENT_TIMESTAMP(6)),
  ('fa000000-0000-4000-8000-000000000006', 'ac000000-0000-4000-8000-000000000006', 1, 'Medimos el camino', 2, 10, 'PUBLICADO', CURRENT_TIMESTAMP(6)),
  ('fa000000-0000-4000-8000-000000000007', 'ac000000-0000-4000-8000-000000000007', 1, 'Formas en la piedra', 2, 8, 'PUBLICADO', CURRENT_TIMESTAMP(6)),
  ('fa000000-0000-4000-8000-000000000008', 'ac000000-0000-4000-8000-000000000008', 1, 'La hora de las campanas', 3, 9, 'PUBLICADO', CURRENT_TIMESTAMP(6)),
  ('fa000000-0000-4000-8000-000000000009', 'ac000000-0000-4000-8000-000000000009', 1, 'Contamos los pasos', 3, 10, 'PUBLICADO', CURRENT_TIMESTAMP(6)),
  ('fa000000-0000-4000-8000-000000000010', 'ac000000-0000-4000-8000-000000000010', 1, 'Reto de la cosecha', 3, 15, 'PUBLICADO', CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- 6. Diez paradas / nodos de la ruta con coordenadas de la plaza
INSERT INTO aprendizaje_nodo_ruta (
  id, version_ruta_id, version_actividad_id, unidad_id, nodo_prerrequisito_id, numero_secuencia, mapa_x, mapa_y, es_opcional
) VALUES
  ('ca000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001', 'fa000000-0000-4000-8000-000000000001', NULL, NULL, 1, 50.00, 10.00, FALSE),
  ('ca000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000001', 'fa000000-0000-4000-8000-000000000002', NULL, 'ca000000-0000-4000-8000-000000000001', 2, 27.00, 19.00, FALSE),
  ('ca000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000001', 'fa000000-0000-4000-8000-000000000003', NULL, 'ca000000-0000-4000-8000-000000000002', 3, 73.00, 28.50, FALSE),
  ('ca000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000001', 'fa000000-0000-4000-8000-000000000004', NULL, 'ca000000-0000-4000-8000-000000000003', 4, 35.00, 37.00, FALSE),
  ('ca000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000001', 'fa000000-0000-4000-8000-000000000005', NULL, 'ca000000-0000-4000-8000-000000000004', 5, 68.00, 46.00, FALSE),
  ('ca000000-0000-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000001', 'fa000000-0000-4000-8000-000000000006', NULL, 'ca000000-0000-4000-8000-000000000005', 6, 25.00, 55.00, FALSE),
  ('ca000000-0000-4000-8000-000000000007', 'b0000000-0000-4000-8000-000000000001', 'fa000000-0000-4000-8000-000000000007', NULL, 'ca000000-0000-4000-8000-000000000006', 7, 75.00, 64.00, FALSE),
  ('ca000000-0000-4000-8000-000000000008', 'b0000000-0000-4000-8000-000000000001', 'fa000000-0000-4000-8000-000000000008', NULL, 'ca000000-0000-4000-8000-000000000007', 8, 36.00, 73.00, FALSE),
  ('ca000000-0000-4000-8000-000000000009', 'b0000000-0000-4000-8000-000000000001', 'fa000000-0000-4000-8000-000000000009', NULL, 'ca000000-0000-4000-8000-000000000008', 9, 67.00, 82.00, FALSE),
  ('ca000000-0000-4000-8000-000000000010', 'b0000000-0000-4000-8000-000000000001', 'fa000000-0000-4000-8000-000000000010', NULL, 'ca000000-0000-4000-8000-000000000009', 10, 50.00, 90.00, FALSE)
ON DUPLICATE KEY UPDATE
  mapa_x = VALUES(mapa_x),
  mapa_y = VALUES(mapa_y);

-- 7. Inscripción activa del estudiante en la ruta
INSERT INTO aprendizaje_inscripcion_ruta (
  id, version_ruta_id, estudiante_id, estado, ultimo_nodo_visitado_id, inscrito_en
) VALUES (
  'e0000000-0000-4000-8000-000000000001',
  'b0000000-0000-4000-8000-000000000001',
  '11111111-1111-4111-8111-111111111111',
  'ACTIVO',
  'ca000000-0000-4000-8000-000000000003',
  CURRENT_TIMESTAMP(6)
) ON DUPLICATE KEY UPDATE
  estado = 'ACTIVO',
  ultimo_nodo_visitado_id = 'ca000000-0000-4000-8000-000000000003';

-- 8. Progreso inicial del estudiante en los diez nodos
-- 2 completados, 1 en curso (último visitado), 1 disponible, 6 bloqueados.
INSERT INTO aprendizaje_progreso_nodo (
  inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en, primer_acceso_en, completado_en
) VALUES
  ('e0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001', 'ca000000-0000-4000-8000-000000000001', 'COMPLETADO', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
  ('e0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001', 'ca000000-0000-4000-8000-000000000002', 'COMPLETADO', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
  ('e0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001', 'ca000000-0000-4000-8000-000000000003', 'EN_CURSO',   CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL),
  ('e0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001', 'ca000000-0000-4000-8000-000000000004', 'DISPONIBLE', CURRENT_TIMESTAMP(6), NULL,                NULL),
  ('e0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001', 'ca000000-0000-4000-8000-000000000005', 'BLOQUEADO',  NULL,                NULL,                NULL),
  ('e0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001', 'ca000000-0000-4000-8000-000000000006', 'BLOQUEADO',  NULL,                NULL,                NULL),
  ('e0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001', 'ca000000-0000-4000-8000-000000000007', 'BLOQUEADO',  NULL,                NULL,                NULL),
  ('e0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001', 'ca000000-0000-4000-8000-000000000008', 'BLOQUEADO',  NULL,                NULL,                NULL),
  ('e0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001', 'ca000000-0000-4000-8000-000000000009', 'BLOQUEADO',  NULL,                NULL,                NULL),
  ('e0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001', 'ca000000-0000-4000-8000-000000000010', 'BLOQUEADO',  NULL,                NULL,                NULL)
ON DUPLICATE KEY UPDATE
  estado = VALUES(estado),
  desbloqueado_en = VALUES(desbloqueado_en),
  primer_acceso_en = VALUES(primer_acceso_en),
  completado_en = VALUES(completado_en);

-- 9. Contexto cultural y datos curiosos verificados de Ayacucho (Dominio Curricular)
INSERT INTO curriculo_contexto_cultural (
  id, ruta_id, codigo, ciudad, lugar, titulo, descripcion, mensaje_animo,
  imagen_url, imagen_alt, credito_autor, credito_fuente, credito_licencia, credito_url
) VALUES (
  'cc000000-0000-4000-8000-000000000001',
  'd0000000-0000-4000-8000-000000000001',
  'AYACUCHO_PLAZA',
  'Ayacucho',
  'Plaza Mayor de Ayacucho',
  'Hecho con raíces peruanas',
  'La hermosa plaza de Ayacucho acompaña nuestra ruta. Cada rincón del Perú guarda historias y aprendizajes por descubrir.',
  'No hay prisa en el camino. ¡Cada paso que das te hace crecer!',
  '/images/ayacucho-plaza.jpg',
  'Plaza Mayor de Ayacucho con sus tradicionales arquerías blancas y cielo andino',
  'Pollinhhsano',
  'Wikimedia Commons',
  'CC BY-SA 4.0',
  'https://commons.wikimedia.org/wiki/File:PLAZA_MAYOR_DE_AYACUCHO.jpg'
) ON DUPLICATE KEY UPDATE
  ciudad = VALUES(ciudad),
  lugar = VALUES(lugar),
  titulo = VALUES(titulo),
  descripcion = VALUES(descripcion),
  mensaje_animo = VALUES(mensaje_animo);

INSERT INTO curriculo_contexto_cultural_dato (
  id, contexto_cultural_id, parada_secuencia, orden, titulo, contenido, icono, fuente, estado
) VALUES
  ('fd000000-0000-4000-8000-000000000001', 'cc000000-0000-4000-8000-000000000001', 1, 1, 'Arquerías de piedra blanca', 'La Plaza de Ayacucho es una de las más grandes del Perú y está rodeada por hermosos arcos de piedra blanca.', 'landmark', 'MINCETUR - Guía de Turismo de Ayacucho', 'PUBLICADO'),
  ('fd000000-0000-4000-8000-000000000002', 'cc000000-0000-4000-8000-000000000001', 2, 2, 'Cajas mágicas: retablos', 'Los retablos ayacuchanos son coloridas cajas de madera que guardan pequeñas figuras modeladas y pintadas con amor.', 'palette', 'Ministerio de Cultura del Perú - Patrimonio Cultural de la Nación', 'PUBLICADO'),
  ('fd000000-0000-4000-8000-000000000003', 'cc000000-0000-4000-8000-000000000001', 3, 3, 'El rico pan chapla', 'En los mercados de Ayacucho se hornea el pan chapla, un pancito esponjoso y calentito que se come con queso andino.', 'store', 'PromPerú - Cocina Tradicional Ayacuchana', 'PUBLICADO'),
  ('fd000000-0000-4000-8000-000000000004', 'cc000000-0000-4000-8000-000000000001', 4, 4, 'El trueque en la plaza', 'Antes de usar monedas, las familias intercambiaban maíz, papitas y frutas como muestra de amistad y apoyo mutuo.', 'coins', 'Banco Central de Reserva del Perú - Museo Central', 'PUBLICADO'),
  ('fd000000-0000-4000-8000-000000000005', 'cc000000-0000-4000-8000-000000000001', 5, 5, 'Tejidos de Santa Ana', 'Los artesanos tejen mantas con lanas teñidas con plantas naturales, creando formas geométricas llenas de historia.', 'layers', 'Ministerio de Cultura - Arte Tradicional de Ayacucho', 'PUBLICADO'),
  ('fd000000-0000-4000-8000-000000000006', 'cc000000-0000-4000-8000-000000000001', 6, 6, 'Ciudad de las iglesias', 'Ayacucho tiene más de treinta templos históricos de piedra con torres altas que miran hacia las montañas.', 'mountain', 'Municipalidad Provincial de Huamanga / MINCETUR', 'PUBLICADO'),
  ('fd000000-0000-4000-8000-000000000007', 'cc000000-0000-4000-8000-000000000001', 7, 7, 'Piedra de Huamanga', 'Es una piedra blanca y suave como la cera. Con ella se tallan figuras brillantes y nacimientos muy delicados.', 'shapes', 'Ministerio de Cultura - Declaratoria Patrimonio Cultural de la Nación', 'PUBLICADO'),
  ('fd000000-0000-4000-8000-000000000008', 'cc000000-0000-4000-8000-000000000001', 8, 8, 'Campanas cantarinas', 'La Catedral de Ayacucho tiene campanas de bronce que tocan alegres melodías para avisar las fiestas del pueblo.', 'bell', 'Arzobispado de Ayacucho', 'PUBLICADO'),
  ('fd000000-0000-4000-8000-000000000009', 'cc000000-0000-4000-8000-000000000001', 9, 9, 'El santuario de Quinua', 'A pocos kilómetros de la plaza está la Pampa de Ayacucho, un campo verde donde se selló la libertad del Perú.', 'footprints', 'Sernanp - Santuario Histórico de la Pampa de Ayacucho', 'PUBLICADO'),
  ('fd000000-0000-4000-8000-000000000010', 'cc000000-0000-4000-8000-000000000001', 10, 10, 'La fiesta de la cosecha', 'En los campos andinos se cosechan cientos de tipos de papas nativas con colores divertidos como amarillo y morado.', 'award', 'Centro Internacional de la Papa (CIP) / INIA', 'PUBLICADO'),
  ('fd000000-0000-4000-8000-000000000011', 'cc000000-0000-4000-8000-000000000001', NULL, 11, 'Rupi, el gallito de las rocas', 'El gallito de las rocas es el ave nacional del Perú. Sus plumas son de un rojo brillante y le gusta saltar entre los árboles.', 'sparkles', 'SERFOR - Fauna Silvestre del Perú', 'PUBLICADO')
ON DUPLICATE KEY UPDATE
  titulo = VALUES(titulo),
  contenido = VALUES(contenido),
  icono = VALUES(icono),
  fuente = VALUES(fuente),
  estado = VALUES(estado);

