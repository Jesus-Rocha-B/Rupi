-- V16: el docente demo pasa a ser responsable de las aulas activas del alumno demo.
-- Sin aula a su cargo, el calendario (HU-13) le devuelve una lista vacía y no se generan recordatorios (HU-14).
-- El aula se busca por la membresía del alumno, no por id: según cómo se armó la base, el aula demo
-- tiene id e3000000-… (seed actual) o au000000-… (seeds anteriores), y V12 solo cubría el primero.
-- Solo datos demo; no reemplaza a un docente ya asignado. Idempotente.

USE rupi;
SET NAMES utf8mb4;

UPDATE escuela_aula a
JOIN escuela_membresia_aula m
  ON m.aula_id = a.id
 AND m.usuario_id = '11111111-1111-4111-8111-111111111111'
 AND m.rol = 'ESTUDIANTE'
 AND m.estado = 'ACTIVO'
JOIN identidad_cuenta_usuario docente
  ON docente.id = '22222222-2222-4222-8222-222222222222'
SET a.docente_responsable_id = docente.id
WHERE a.activa = TRUE
  AND a.docente_responsable_id IS NULL;
