-- =============================================================================
-- Migración V20: Vinculación de Aula 3.° Primaria y Códigos de Acceso Docente
-- Asegura que el Prof. Carlos Mendoza tenga aula activa asignada para 3.° de Primaria
-- y que sus rutas tengan códigos de acceso directos y legibles para sus estudiantes.
-- =============================================================================

-- 1. Asignar códigos de acceso alfanuméricos legibles en aprendizaje_ruta
UPDATE aprendizaje_ruta SET codigo = 'PROFCARLOS' WHERE id = '0af312d5-952f-40ea-8021-cc8b821702ab';
UPDATE aprendizaje_ruta SET codigo = 'DOCENTE2026' WHERE id = 'f0d015db-0010-4f50-a295-f6d1c7605855';

-- 2. Asegurar aula para 3.° de primaria en la institución Mariscal Sucre con el docente Carlos
INSERT INTO escuela_aula (
  id, institucion_id, anio_escolar_id, grado_id, codigo, nombre, docente_responsable_id, activa
) VALUES (
  'e3000000-0000-4000-8000-000000000003',
  'e1000000-0000-4000-8000-000000000001',
  'e2000000-0000-4000-8000-000000000001',
  '00000000-0000-4000-8000-000000000003',
  'AULA-3A',
  '3.° Primaria A',
  '22222222-2222-4222-8222-222222222222',
  TRUE
)
ON DUPLICATE KEY UPDATE docente_responsable_id = '22222222-2222-4222-8222-222222222222', activa = TRUE;

-- 3. Membresía activa del docente Carlos y del estudiante Mateo en el aula de 3.° grado
INSERT INTO escuela_membresia_aula (
  id, aula_id, usuario_id, rol, estado, ingreso_en
) VALUES (
  'e4000000-0000-4000-8000-000000000003',
  'e3000000-0000-4000-8000-000000000003',
  '22222222-2222-4222-8222-222222222222',
  'DOCENTE',
  'ACTIVO',
  UTC_TIMESTAMP(6)
)
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

INSERT INTO escuela_membresia_aula (
  id, aula_id, usuario_id, rol, estado, ingreso_en
) VALUES (
  'e4000000-0000-4000-8000-000000000004',
  'e3000000-0000-4000-8000-000000000003',
  '11111111-1111-4111-8111-111111111111',
  'ESTUDIANTE',
  'ACTIVO',
  UTC_TIMESTAMP(6)
)
ON DUPLICATE KEY UPDATE estado = 'ACTIVO';

-- 4. Asegurar que Carlos sea docente responsable oficial de 2.° Primaria también
UPDATE escuela_aula SET docente_responsable_id = '22222222-2222-4222-8222-222222222222' WHERE id = 'e3000000-0000-4000-8000-000000000001';
