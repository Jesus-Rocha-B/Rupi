-- V38__tablero_uptime_historial_caidas_hu48.sql
-- Historial de caídas del servidor principal y métricas de disponibilidad (HU-48)
--
-- Almacena las interrupciones, caídas y restablecimientos del servidor principal
-- para calcular el porcentaje de disponibilidad (uptime) y evaluar la calidad técnica brindada a los colegios.
-- Escritor: Django (operaciones_*). Idempotente con IF NOT EXISTS.

CREATE TABLE IF NOT EXISTS operaciones_historial_caida (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  servidor VARCHAR(80) NOT NULL DEFAULT 'servidor_principal',
  tipo_evento ENUM('CAIDA', 'DEGRADACION', 'RECUPERACION') NOT NULL DEFAULT 'CAIDA',
  iniciado_en DATETIME(6) NOT NULL,
  restablecido_en DATETIME(6) NULL,
  duracion_segundos INT UNSIGNED NULL,
  motivo VARCHAR(500) NOT NULL,
  codigo_error VARCHAR(80) NULL,
  impacto_colegios VARCHAR(255) NULL,
  registrado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY ix_caida_servidor_fecha (servidor, iniciado_en),
  KEY ix_caida_tipo (tipo_evento)
) ENGINE=InnoDB;

-- Datos de historial base para cálculos representativos de uptime en los colegios
INSERT IGNORE INTO operaciones_historial_caida 
(id, servidor, tipo_evento, iniciado_en, restablecido_en, duracion_segundos, motivo, codigo_error, impacto_colegios, registrado_en)
VALUES
('d0480000-0000-4000-8000-000000000001', 'servidor_principal', 'CAIDA', DATE_SUB(NOW(6), INTERVAL 12 DAY), DATE_ADD(DATE_SUB(NOW(6), INTERVAL 12 DAY), INTERVAL 180 SECOND), 180, 'Mantenimiento preventivo programado de base de datos MySQL', 'MAINT_01', 'Sin impacto en horario escolar (madrugada)', DATE_SUB(NOW(6), INTERVAL 12 DAY)),
('d0480000-0000-4000-8000-000000000002', 'servidor_principal', 'DEGRADACION', DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_ADD(DATE_SUB(NOW(6), INTERVAL 5 DAY), INTERVAL 95 SECOND), 95, 'Pico de latencia por concurrencia en subida de notas bimestrales', 'LATENCY_PEAK', 'Demoras leves de 1.5s en sincronización de 2 aulas', DATE_SUB(NOW(6), INTERVAL 5 DAY)),
('d0480000-0000-4000-8000-000000000003', 'servidor_principal', 'CAIDA', DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_ADD(DATE_SUB(NOW(6), INTERVAL 2 DAY), INTERVAL 45 SECOND), 45, 'Reinicio automático por saturación de memoria en microservicio IA', 'AUTO_RESTART_IA', 'Reanudación automática en menos de 1 minuto sin pérdida de datos', DATE_SUB(NOW(6), INTERVAL 2 DAY));
