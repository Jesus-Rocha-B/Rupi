-- V17: completa las filas de progreso que faltan en inscripciones existentes.
-- El progreso por parada se creaba solo al inscribirse; una parada agregada después por el docente quedaba sin fila
-- y bloqueada para siempre para los alumnos ya inscritos. Desde esta versión la API crea esas filas sola
-- (al agregar la parada y al abrir o completar una actividad); V17 repara los datos anteriores.
-- Una parada nace DISPONIBLE si es la primera de la ruta o si la anterior ya está completada; si no, BLOQUEADA.
-- INSERT IGNORE: no modifica ninguna fila existente. Idempotente.

USE rupi;
SET NAMES utf8mb4;

INSERT IGNORE INTO aprendizaje_progreso_nodo
    (inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en)
SELECT i.id, i.version_ruta_id, n.id,
       CASE WHEN prev.id IS NULL OR pp.estado = 'COMPLETADO' THEN 'DISPONIBLE' ELSE 'BLOQUEADO' END,
       CASE WHEN prev.id IS NULL OR pp.estado = 'COMPLETADO' THEN UTC_TIMESTAMP(6) ELSE NULL END
FROM aprendizaje_inscripcion_ruta i
JOIN aprendizaje_nodo_ruta n ON n.version_ruta_id = i.version_ruta_id
LEFT JOIN aprendizaje_nodo_ruta prev
       ON prev.version_ruta_id = i.version_ruta_id
      AND prev.numero_secuencia = (
          SELECT MAX(x.numero_secuencia) FROM aprendizaje_nodo_ruta x
          WHERE x.version_ruta_id = i.version_ruta_id AND x.numero_secuencia < n.numero_secuencia)
LEFT JOIN aprendizaje_progreso_nodo pp
       ON pp.inscripcion_id = i.id AND pp.nodo_ruta_id = prev.id
WHERE i.estado IN ('ACTIVO', 'COMPLETADO');
