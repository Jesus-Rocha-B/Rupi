-- V27: Accesorios desbloqueables para la mascota Rupi con puntos XP (HU-31)

-- 1. Permitir que archivo_multimedia_id sea opcional
ALTER TABLE gamificacion_accesorio MODIFY archivo_multimedia_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL;

-- 2. Asegurar campos descriptivos
ALTER TABLE gamificacion_accesorio ADD COLUMN descripcion VARCHAR(255) NULL AFTER nombre;

-- 3. Insertar catalogo inicial de accesorios de Rupi con identidad peruana y escolar
INSERT IGNORE INTO gamificacion_accesorio (id, codigo, nombre, descripcion, tipo, costo_experiencia, activo) VALUES
-- SOMBREROS
('a1000000-0000-0000-0000-000000000001', 'CHULLO_ANDINO', 'Chullo de Alpaca', 'Tradicional gorro andino tejido con orejeras y motivos incas.', 'SOMBRERO', 0, 1),
('a1000000-0000-0000-0000-000000000002', 'BIRRETE_ACADEMICO', 'Birrete de Graduación', 'Elegante birrete con borla dorada de honor escolar.', 'SOMBRERO', 150, 1),
('a1000000-0000-0000-0000-000000000003', 'CORONA_INTI', 'Mascapaicha Imperial', 'Corona dorada de los emperadores incas con plumas reales.', 'SOMBRERO', 350, 1),
('a1000000-0000-0000-0000-000000000004', 'SOMBRERO_CHALAN', 'Sombrero de Paja Chalán', 'Típico sombrero de paja toquilla norteño de ala ancha.', 'SOMBRERO', 200, 1),

-- VESTIMENTAS
('b1000000-0000-0000-0000-000000000001', 'PONCHO_CUSQUENO', 'Poncho Cusqueño de Fiesta', 'Cálido poncho de lana roja con grecas de colores vivos.', 'VESTIMENTA', 0, 1),
('b1000000-0000-0000-0000-000000000002', 'CHALECO_ESCOLAR', 'Chaleco Escolar de Punto', 'Chaleco azul marino con cuello en V ribeteado y corbatita.', 'VESTIMENTA', 100, 1),
('b1000000-0000-0000-0000-000000000003', 'BATA_CIENTIFICA', 'Bata Blanca de Explorador', 'Bata de laboratorio con bolsillo para lapiceros y lupa.', 'VESTIMENTA', 250, 1),
('b1000000-0000-0000-0000-000000000004', 'CAPA_SUPER_RUPI', 'Capa de Héroe Andino', 'Capa verde hoja con broche dorado que ondea con el viento.', 'VESTIMENTA', 400, 1),

-- FONDOS / EFECTOS
('c1000000-0000-0000-0000-000000000001', 'AURA_SOLAR', 'Resplandor Solar Inti', 'Un halo brillante y dorado que ilumina el camino de Rupi.', 'FONDO', 200, 1),
('c1000000-0000-0000-0000-000000000002', 'ESTRELLAS_ANDINAS', 'Estrellas de la Cruz del Sur', 'Destellos estelares titilando en torno a la mascota.', 'FONDO', 300, 1);
