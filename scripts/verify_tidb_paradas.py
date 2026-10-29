"""Regresión SQL de apertura de paradas: solo EXPLAIN y SELECT con datos sintéticos.
Ejecutar con el Python de admin-django y DJANGO_SETTINGS_MODULE/PYTHONPATH configurados para la BD de destino.
No crea cuentas, sesiones ni progreso; no ejecuta el INSERT sobre las tablas compartidas.
"""
import pathlib,re,django
django.setup()
from django.db import connection
root=pathlib.Path(__file__).resolve().parent.parent
source=(root/"backend/api-spring-boot/src/main/java/pe/rupi/api/learningroute/ActivityService.java").read_text(encoding="utf-8")
sql=re.search(r'private void ensureProgressRows.*?jdbc.update\("""(.*?)""", params\);',source,re.S).group(1)
select=sql[sql.index("SELECT i.id"):]
fixture="""WITH aprendizaje_inscripcion_ruta AS (
 SELECT 'enrollment' id,'route' version_ruta_id,'student' estudiante_id,'ACTIVO' estado
), aprendizaje_nodo_ruta AS (
 SELECT 'first' id,'route' version_ruta_id,2 numero_secuencia UNION ALL
 SELECT 'second','route',5 UNION ALL SELECT 'third','route',9 UNION ALL
 SELECT 'fourth','route',12 UNION ALL SELECT 'other','different-route',1
), aprendizaje_progreso_nodo AS (
 SELECT 'enrollment' inscripcion_id,'first' nodo_ruta_id,'COMPLETADO' estado UNION ALL
 SELECT 'enrollment','second','EN_CURSO' UNION ALL SELECT 'enrollment','third','COMPLETADO'
)
"""
with connection.cursor() as c:
 real=sql.replace(":student","'00000000-0000-0000-0000-000000000000'").replace(":route","'00000000-0000-0000-0000-000000000000'")
 c.execute("EXPLAIN "+real);c.fetchall()
 print("PASS TiDB acepta el INSERT mediante EXPLAIN (sin ejecutar escrituras).")
 c.execute(fixture+select.replace(":student","'student'").replace(":route","'route'"))
 actual={r[2]:r[3] for r in c.fetchall()}
 assert actual=={"first":"DISPONIBLE","second":"DISPONIBLE","third":"BLOQUEADO","fourth":"DISPONIBLE"},actual
 print("PASS primera parada, secuencias con huecos, predecesor completado o pendiente y separacion de rutas.")
 c.execute(fixture+select.replace(":student","'another-student'").replace(":route","'route'"))
 assert not c.fetchall()
 print("PASS estudiante sin inscripcion no obtiene filas. No se modificaron datos compartidos.")
