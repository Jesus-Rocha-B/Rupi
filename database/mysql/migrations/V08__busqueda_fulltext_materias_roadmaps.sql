-- V08: Índices de búsqueda de texto completo (Full-Text Search) en áreas curriculares y catálogo de materias/roadmaps alternativos (HU-09).
-- Permite búsqueda rápida por nombre de materia, título y descripción de roadmaps.
-- Idempotente: se puede aplicar múltiples veces sin alterar el progreso de los estudiantes.
-- UTF-8 / MySQL 8.4 InnoDB

USE rupi;
SET NAMES utf8mb4;

-- 1. Crear índice Full-Text Search en curriculo_area(nombre) de forma condicional/idempotente
SET @exist_ft_area := (SELECT COUNT(*) FROM information_schema.statistics
                       WHERE table_schema = DATABASE() AND table_name = 'curriculo_area' AND index_name = 'ft_area_busqueda');
SET @sql_ft_area := IF(@exist_ft_area = 0,
                       'ALTER TABLE curriculo_area ADD FULLTEXT INDEX ft_area_busqueda (nombre)',
                       'SELECT "Index ft_area_busqueda already exists"');
PREPARE stmt_area FROM @sql_ft_area;
EXECUTE stmt_area;
DEALLOCATE PREPARE stmt_area;

-- 2. Asegurar índice Full-Text Search en aprendizaje_ruta(titulo, descripcion)
SET @exist_ft_ruta := (SELECT COUNT(*) FROM information_schema.statistics
                       WHERE table_schema = DATABASE() AND table_name = 'aprendizaje_ruta' AND index_name = 'ft_ruta_busqueda');
SET @sql_ft_ruta := IF(@exist_ft_ruta = 0,
                       'ALTER TABLE aprendizaje_ruta ADD FULLTEXT INDEX ft_ruta_busqueda (titulo, descripcion)',
                       'SELECT "Index ft_ruta_busqueda already exists"');
PREPARE stmt_ruta FROM @sql_ft_ruta;
EXECUTE stmt_ruta;
DEALLOCATE PREPARE stmt_ruta;

-- 3. Catálogo oficial de Áreas Curriculares del CNEB-2017 para Primaria Peruana
INSERT INTO curriculo_area (id, version_catalogo_id, codigo, nombre) VALUES
  ('a0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001', 'MAT', 'Matemática'),
  ('a0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000001', 'COM', 'Comunicación'),
  ('a0000000-0000-4000-8000-000000000003', 'c0000000-0000-4000-8000-000000000001', 'CYT', 'Ciencia y Tecnología'),
  ('a0000000-0000-4000-8000-000000000004', 'c0000000-0000-4000-8000-000000000001', 'SOC', 'Personal Social'),
  ('a0000000-0000-4000-8000-000000000005', 'c0000000-0000-4000-8000-000000000001', 'ART', 'Arte y Cultura'),
  ('a0000000-0000-4000-8000-000000000006', 'c0000000-0000-4000-8000-000000000001', 'ING', 'Inglés como Lengua Extranjera')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- 4. Catálogo de Roadmaps Alternativos Publicados para Exploración Escolar

-- 4.1. Comunicación (Cuentos y Leyendas del Valle Sagrado)
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000002', '11111111-1111-4111-8111-111111111111', 'COM-2P-VALLE',
   'Comunicación: Cuentos y Leyendas del Valle Sagrado',
   'Recorrido de lectura y narración oral descubriendo las tradiciones, fábulas y leyendas andinas de Ayacucho.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000002', 1, 'PUBLICADO', CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

-- 4.2. Ciencia y Tecnología (Exploradores de la Biodiversidad Andina)
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000003', '11111111-1111-4111-8111-111111111111', 'CYT-2P-BIODIV',
   'Ciencia y Tecnología: Exploradores de la Biodiversidad Andina',
   'Aventura científica para conocer los ecosistemas, la flora nativa, el suelo y el hábitat del Gallito de las Rocas.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000003', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000003', 1, 'PUBLICADO', CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

-- 4.3. Personal Social (Historia y Costumbres de Nuestra Comunidad)
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000004', '11111111-1111-4111-8111-111111111111', 'SOC-2P-COMUN',
   'Personal Social: Historia y Costumbres de Nuestra Comunidad',
   'Exploración de la convivencia familiar, los saberes comunitarios y el patrimonio cultural de los pueblos peruanos.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000004', 'd0000000-0000-4000-8000-000000000004', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000004', 1, 'PUBLICADO', CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

-- 4.4. Arte y Cultura (Colores, Retablos y Tejidos Tradicionales)
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000005', '11111111-1111-4111-8111-111111111111', 'ART-2P-RETABLO',
   'Arte y Cultura: Colores, Retablos y Tejidos Tradicionales',
   'Taller interactivo de expresión plástica inspirado en la artesanía popular ayacuchana y la música andina.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-000000000005', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000005', 1, 'PUBLICADO', CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

-- 4.5. Matemática Alternativa (Geometría Andina y Patrones en Wari)
INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion) VALUES
  ('d0000000-0000-4000-8000-000000000006', '11111111-1111-4111-8111-111111111111', 'MAT-2P-GEOM',
   'Matemática: Formas Geométricas en la Arquitectura Andina',
   'Descubre patrones, figuras bidimensionales y medidas usando como modelo las murallas y templos de Wari.')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), descripcion = VALUES(descripcion);

INSERT INTO aprendizaje_version_ruta (id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, publicado_en) VALUES
  ('b0000000-0000-4000-8000-000000000006', 'd0000000-0000-4000-8000-000000000006', 'c0000000-0000-4000-8000-000000000001',
   '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001', 1, 'PUBLICADO', CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE estado = 'PUBLICADO';

-- 5. Actividades y Paradas Muestra para las Rutas Alternativas
-- Permite que los roadmaps alternativos tengan recuento real de paradas (total_nodes > 0)
INSERT INTO aprendizaje_actividad (id, tipo, creado_por) VALUES
  ('ac000000-0000-4000-8000-000000000020', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000030', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000040', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000050', 'LECCION', '11111111-1111-4111-8111-111111111111'),
  ('ac000000-0000-4000-8000-000000000060', 'LECCION', '11111111-1111-4111-8111-111111111111')
ON DUPLICATE KEY UPDATE tipo = VALUES(tipo);

INSERT INTO aprendizaje_version_actividad (id, actividad_id, numero_version, titulo, minutos_estimados, estado) VALUES
  ('fa000000-0000-4000-8000-000000000020', 'ac000000-0000-4000-8000-000000000020', 1, 'Lectura: El zorro y el cuy andino', 10, 'PUBLICADO'),
  ('fa000000-0000-4000-8000-000000000030', 'ac000000-0000-4000-8000-000000000030', 1, 'Descubriendo las plantas medicinales', 10, 'PUBLICADO'),
  ('fa000000-0000-4000-8000-000000000040', 'ac000000-0000-4000-8000-000000000040', 1, 'Mi familia y nuestras raíces', 10, 'PUBLICADO'),
  ('fa000000-0000-4000-8000-000000000050', 'ac000000-0000-4000-8000-000000000050', 1, 'Modelando arcilla ayacuchana', 10, 'PUBLICADO'),
  ('fa000000-0000-4000-8000-000000000060', 'ac000000-0000-4000-8000-000000000060', 1, 'Triángulos y cuadriláteros en la piedra', 10, 'PUBLICADO')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

-- Nodos vinculados para cada versión de ruta
INSERT INTO aprendizaje_nodo_ruta (id, version_ruta_id, version_actividad_id, numero_secuencia, es_opcional) VALUES
  ('ca000000-0000-4000-8000-000000000021', 'b0000000-0000-4000-8000-000000000002', 'fa000000-0000-4000-8000-000000000020', 1, FALSE),
  ('ca000000-0000-4000-8000-000000000031', 'b0000000-0000-4000-8000-000000000003', 'fa000000-0000-4000-8000-000000000030', 1, FALSE),
  ('ca000000-0000-4000-8000-000000000041', 'b0000000-0000-4000-8000-000000000004', 'fa000000-0000-4000-8000-000000000040', 1, FALSE),
  ('ca000000-0000-4000-8000-000000000051', 'b0000000-0000-4000-8000-000000000005', 'fa000000-0000-4000-8000-000000000050', 1, FALSE),
  ('ca000000-0000-4000-8000-000000000061', 'b0000000-0000-4000-8000-000000000006', 'fa000000-0000-4000-8000-000000000060', 1, FALSE)
ON DUPLICATE KEY UPDATE version_actividad_id = VALUES(version_actividad_id);
