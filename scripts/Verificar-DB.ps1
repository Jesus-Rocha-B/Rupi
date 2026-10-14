$env:MYSQL_PWD = 'rochabobadilla'
$sql = @"
SELECT i.id, i.estudiante_id, i.version_ruta_id, i.estado, vr.estado as vr_estado 
FROM aprendizaje_inscripcion_ruta i 
JOIN aprendizaje_version_ruta vr ON vr.id = i.version_ruta_id;

SELECT nodo_ruta_id, estado, primer_acceso_en, completado_en 
FROM aprendizaje_progreso_nodo 
WHERE inscripcion_id = 'e0000000-0000-4000-8000-000000000001'
ORDER BY nodo_ruta_id;
"@

$sql | & 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe' -u rupi rupi
Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
