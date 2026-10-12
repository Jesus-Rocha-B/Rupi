-- V11: Códigos de Acceso Alfanuméricos para Salones de Clase Docente (HU-11)
-- Permite que los estudiantes ingresen un código de clase proporcionado por su profesor
-- para unirse a un roadmap específico y acceder a la ruta escolar de su salón.
-- Esquema: rupi (MySQL 8.4 InnoDB / UTF-8 mb4). Idempotente.

USE rupi;
SET NAMES utf8mb4;

-- 1. Tabla de códigos de acceso (asegurada con idempotencia)
CREATE TABLE IF NOT EXISTS aprendizaje_codigo_acceso_ruta (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  version_ruta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  hash_codigo BINARY(32) NOT NULL UNIQUE,
  creado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  valido_desde DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  valido_hasta DATETIME(6) NULL,
  maximo_usos INT UNSIGNED NULL,
  revocado_en DATETIME(6) NULL,
  FOREIGN KEY (version_ruta_id) REFERENCES aprendizaje_version_ruta(id),
  FOREIGN KEY (creado_por) REFERENCES identidad_cuenta_usuario(id),
  CHECK (valido_hasta IS NULL OR valido_hasta > valido_desde),
  CHECK (maximo_usos IS NULL OR maximo_usos > 0)
) ENGINE=InnoDB;

-- 2. Semilla de códigos de clase de demostración y producción escolar
-- Códigos activos para docentes de aula:
-- 'AYACUCHO2026' -> 2.° Matemática (Retablo Matemático)
-- 'WARI2026'     -> 3.° Matemática (Ciudadela Wari)
-- 'PALABRAS2026' -> 2.° Comunicación (Cuentos y Leyendas del Valle)
-- 'CIENCIA2026'  -> 2.° Ciencia y Tecnología (Biodiversidad Andina)
-- 'PRIMERO2026'  -> 1.° Matemática (Aventuras Numéricas)
-- 'CUARTO2026'   -> 4.° Matemática (Multiplicación y Geometría)
-- 'QUINTO2026'   -> 5.° Matemática (Fracciones y Andenes)
-- 'SEXTO2026'    -> 6.° Matemática (Razonamiento y Datos)
--
-- Códigos de prueba para validaciones pedagógicas y de seguridad:
-- 'EXPIRADO2025' -> Código vencido para probar rechazo comprensible
-- 'REVOCADO2026' -> Código revocado por docente
-- 'FUTURO2099'   -> Código aún no habilitado

INSERT INTO aprendizaje_codigo_acceso_ruta (
  id, version_ruta_id, hash_codigo, creado_por, valido_desde, valido_hasta, maximo_usos, revocado_en
) VALUES
  -- 2.° Primaria Matemática (Ruta principal de Ayacucho)
  ('c0000001-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001',
   UNHEX(SHA2('AYACUCHO2026', 256)), '11111111-1111-4111-8111-111111111111',
   '2026-01-01 00:00:00.000000', '2027-12-31 23:59:59.000000', 100, NULL),

  -- 3.° Primaria Matemática (Ciudadela Wari)
  ('c0000001-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000007',
   UNHEX(SHA2('WARI2026', 256)), '11111111-1111-4111-8111-111111111111',
   '2026-01-01 00:00:00.000000', '2027-12-31 23:59:59.000000', 100, NULL),

  -- 2.° Primaria Comunicación (Valle de Cuentos)
  ('c0000001-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000002',
   UNHEX(SHA2('PALABRAS2026', 256)), '11111111-1111-4111-8111-111111111111',
   '2026-01-01 00:00:00.000000', '2027-12-31 23:59:59.000000', 100, NULL),

  -- 2.° Primaria Ciencia y Tecnología (Biodiversidad)
  ('c0000001-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000003',
   UNHEX(SHA2('CIENCIA2026', 256)), '11111111-1111-4111-8111-111111111111',
   '2026-01-01 00:00:00.000000', '2027-12-31 23:59:59.000000', 100, NULL),

  -- 1.° Primaria Matemática
  ('c0000001-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000101',
   UNHEX(SHA2('PRIMERO2026', 256)), '11111111-1111-4111-8111-111111111111',
   '2026-01-01 00:00:00.000000', '2027-12-31 23:59:59.000000', 100, NULL),

  -- 4.° Primaria Matemática
  ('c0000001-0000-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000401',
   UNHEX(SHA2('CUARTO2026', 256)), '11111111-1111-4111-8111-111111111111',
   '2026-01-01 00:00:00.000000', '2027-12-31 23:59:59.000000', 100, NULL),

  -- 5.° Primaria Matemática
  ('c0000001-0000-4000-8000-000000000007', 'b0000000-0000-4000-8000-000000000501',
   UNHEX(SHA2('QUINTO2026', 256)), '11111111-1111-4111-8111-111111111111',
   '2026-01-01 00:00:00.000000', '2027-12-31 23:59:59.000000', 100, NULL),

  -- 6.° Primaria Matemática
  ('c0000001-0000-4000-8000-000000000008', 'b0000000-0000-4000-8000-000000000601',
   UNHEX(SHA2('SEXTO2026', 256)), '11111111-1111-4111-8111-111111111111',
   '2026-01-01 00:00:00.000000', '2027-12-31 23:59:59.000000', 100, NULL),

  -- Casos de prueba para validación formativa:
  -- Código expirado
  ('c0000001-0000-4000-8000-000000000091', 'b0000000-0000-4000-8000-000000000001',
   UNHEX(SHA2('EXPIRADO2025', 256)), '11111111-1111-4111-8111-111111111111',
   '2024-01-01 00:00:00.000000', '2025-01-01 00:00:00.000000', 100, NULL),

  -- Código revocado por docente
  ('c0000001-0000-4000-8000-000000000092', 'b0000000-0000-4000-8000-000000000001',
   UNHEX(SHA2('REVOCADO2026', 256)), '11111111-1111-4111-8111-111111111111',
   '2026-01-01 00:00:00.000000', '2027-12-31 23:59:59.000000', 100, '2026-02-01 12:00:00.000000'),

  -- Código con fecha futura (aún no activo)
  ('c0000001-0000-4000-8000-000000000093', 'b0000000-0000-4000-8000-000000000001',
   UNHEX(SHA2('FUTURO2099', 256)), '11111111-1111-4111-8111-111111111111',
   '2099-01-01 00:00:00.000000', '2099-12-31 23:59:59.000000', 100, NULL)
ON DUPLICATE KEY UPDATE
  hash_codigo = VALUES(hash_codigo),
  valido_desde = VALUES(valido_desde),
  valido_hasta = VALUES(valido_hasta),
  maximo_usos = VALUES(maximo_usos),
  revocado_en = VALUES(revocado_en);
