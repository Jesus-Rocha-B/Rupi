"""Prueba HTTP/MySQL con datos ficticios propios y limpieza en finally. Solo entorno local.
python scripts/verify_learning_flow.py --root-config .local/root.cnf --mysql .local/mysql-8.4.11-winx64/bin/mysql.exe
"""
import argparse
import base64
import hashlib
import http.cookiejar
import json
import subprocess
import uuid
import urllib.error
import urllib.request
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('--root-config', required=True)
p.add_argument('--mysql', required=True)
p.add_argument('--base', default='http://127.0.0.1:8081')
args = p.parse_args()
if not args.base.startswith(('http://127.0.0.1:', 'http://localhost:')):
    raise SystemExit('Esta prueba solo se ejecuta contra la API local.')
cli = [str(Path(args.mysql).resolve()), '--defaults-extra-file='+str(Path(args.root_config).resolve()), '--batch', '--skip-column-names', 'rupi']
def sql(statement):
    result = subprocess.run(cli, input=statement.encode(), capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stderr.decode(errors='replace'))
    return result.stdout.decode().strip()
def client():
    return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
def request(c, path, method='GET', body=None, origin=None):
    headers = {'Origin': origin or args.base, 'Content-Type': 'application/json'}
    req = urllib.request.Request(args.base+'/api/v1/'+path, method=method, headers=headers,
        data=json.dumps(body).encode() if body is not None else (b'' if method=='POST' else None))
    try:
        with c.open(req, timeout=20) as response:
            raw = response.read()
            return response.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as error:
        return error.code, None
def uid(): return str(uuid.uuid4())
users=[uid() for _ in range(3)]
routes=[uid() for _ in range(2)]
versions=[uid() for _ in range(2)]
enrollments=[uid() for _ in range(3)]
nodes=[uid() for _ in range(4)]
activities=[uid() for _ in range(4)]
activity_versions=[uid() for _ in range(4)]
blocks=[uid() for _ in range(4)]
usernames=['qa_'+u.replace('-','')[:16] for u in users]
password='Rupi-test-'+uuid.uuid4().hex
salt=uuid.uuid4().hex
hashed='pbkdf2_sha256$600000$'+salt+'$'+base64.b64encode(hashlib.pbkdf2_hmac('sha256',password.encode(),salt.encode(),600000)).decode()
def ids(values): return ','.join("'"+value+"'" for value in values)
passed=[]
def check(name, condition):
    if not condition: raise AssertionError(name)
    passed.append(name); print('OK:',name)
try:
    catalog, area = sql("SELECT a.version_catalogo_id,a.id FROM curriculo_area a LIMIT 1").split('\t')
    grade=sql('SELECT id FROM curriculo_grado WHERE numero_grado=2')
    for i,u in enumerate(users):
        sql(f"INSERT INTO identidad_cuenta_usuario(id,nombre_usuario,hash_clave,estado) VALUES ('{u}','{usernames[i]}','{hashed}','{'BLOQUEADO' if i==2 else 'ACTIVO'}')")
    for i,r in enumerate(routes):
        sql(f"INSERT INTO aprendizaje_ruta(id,codigo,titulo) VALUES ('{r}','QA-{r}','Prueba {i}'); INSERT INTO aprendizaje_version_ruta(id,ruta_id,version_catalogo_id,grado_id,area_id,numero_version,estado) VALUES ('{versions[i]}','{r}','{catalog}','{grade}','{area}',1,'PUBLICADO')")
    for i,n in enumerate(nodes):
        route=versions[0 if i<3 else 1]
        seq=i+1 if i<3 else 1
        sql(f"INSERT INTO aprendizaje_actividad(id,tipo) VALUES ('{activities[i]}','LECCION'); INSERT INTO aprendizaje_version_actividad(id,actividad_id,numero_version,titulo,estado) VALUES ('{activity_versions[i]}','{activities[i]}',1,'Actividad {i}','PUBLICADO'); INSERT INTO aprendizaje_bloque_contenido(id,version_actividad_id,numero_secuencia,tipo,texto_cuerpo) VALUES ('{blocks[i]}','{activity_versions[i]}',1,'TEXTO','Contenido {i}'); INSERT INTO aprendizaje_nodo_ruta(id,version_ruta_id,version_actividad_id,numero_secuencia,es_opcional) VALUES ('{n}','{route}','{activity_versions[i]}',{seq},FALSE)")
    for idx,(user,version) in enumerate([(users[0],versions[0]),(users[0],versions[1]),(users[1],versions[0])]):
        e=enrollments[idx]
        sql(f"INSERT INTO aprendizaje_inscripcion_ruta(id,version_ruta_id,estudiante_id,estado) VALUES ('{e}','{version}','{user}','ACTIVO')")
        for ni in ([0,1,2] if version==versions[0] else [3]):
            state='BLOQUEADO' if ni==2 else 'DISPONIBLE'
            unlocked='NULL' if ni==2 else 'UTC_TIMESTAMP(6)'
            sql(f"INSERT INTO aprendizaje_progreso_nodo(inscripcion_id,version_ruta_id,nodo_ruta_id,estado,desbloqueado_en) VALUES ('{e}','{version}','{nodes[ni]}','{state}',{unlocked})")
    c1,c2=client(),client()
    check('sin sesión: 401', request(c1,'student/learning-routes')[0]==401)
    check('contraseña incorrecta: 401',request(c1,'auth/login','POST',{'username':usernames[0],'password':'incorrecta'})[0]==401)
    check('cuenta bloqueada: 401',request(c1,'auth/login','POST',{'username':usernames[2],'password':password})[0]==401)
    for i,c in enumerate([c1,c2]):
        check(f'login estudiante {i+1}',request(c,'auth/login','POST',{'username':usernames[i],'password':password})[0]==200)
    check('cada cuenta recibe sus propias matrículas',len(request(c1,'student/learning-routes')[1]['items'])==2 and len(request(c2,'student/learning-routes')[1]['items'])==1)
    def start(c,route,node,origin=None):return request(c,f'student/learning-routes/{route}/nodes/{node}/start','POST',origin=origin)
    check('ruta ajena: 404', start(c2,versions[1],nodes[3])[0]==404)
    check('origen externo: 403',start(c1,versions[0],nodes[0],'https://other.example')[0]==403)
    check('nodo bloqueado: 409',start(c1,versions[0],nodes[2])[0]==409)
    check('rechazo no modifica último nodo',sql(f"SELECT COALESCE(ultimo_nodo_visitado_id,'NULL') FROM aprendizaje_inscripcion_ruta WHERE id='{enrollments[0]}'")=='NULL')
    status,body=start(c1,versions[0],nodes[0])
    check('iniciar devuelve contenido y EN_CURSO',status==200 and body['state']=='EN_CURSO' and body['blocks'][0]['text']=='Contenido 0')
    first=sql(f"SELECT primer_acceso_en FROM aprendizaje_progreso_nodo WHERE inscripcion_id='{enrollments[0]}' AND nodo_ruta_id='{nodes[0]}'")
    check('inicio repetido conserva estado',start(c1,versions[0],nodes[0])[1]['state']=='EN_CURSO')
    check('primer acceso no se reinicia',first==sql(f"SELECT primer_acceso_en FROM aprendizaje_progreso_nodo WHERE inscripcion_id='{enrollments[0]}' AND nodo_ruta_id='{nodes[0]}'"))
    check('progreso del otro estudiante sigue intacto',sql(f"SELECT estado FROM aprendizaje_progreso_nodo WHERE inscripcion_id='{enrollments[2]}' AND nodo_ruta_id='{nodes[0]}'")=='DISPONIBLE')
    check('segunda ruta se inicia',start(c1,versions[1],nodes[3])[0]==200)
    check('ruta más reciente se ofrece primero',request(c1,'student/learning-routes')[1]['items'][0]['versionRouteId']==versions[1])
    check('logout revoca sesión',request(c1,'auth/logout','POST')[0]==204 and request(c1,'student/session')[0]==401)
    check('reingreso correcto',request(c1,'auth/login','POST',{'username':usernames[0],'password':password})[0]==200)
    detail=request(c1,'student/learning-routes/'+versions[1])[1]
    check('recupera el nodo tras cerrar sesión',detail['enrollment']['lastVisitedNodeId']==nodes[3])
    sql(f"UPDATE aprendizaje_version_actividad SET estado='RETIRADO' WHERE id='{activity_versions[0]}'")
    check('actividad retirada: 404',start(c1,versions[0],nodes[0])[0]==404)
    check('reanudar nodo retirado ofrece alternativa válida',request(c1,'student/learning-routes/'+versions[0])[1]['enrollment']['lastVisitedNodeId']==nodes[1])
    sql(f"UPDATE aprendizaje_progreso_nodo SET estado='COMPLETADO',completado_en=UTC_TIMESTAMP(6) WHERE inscripcion_id='{enrollments[0]}' AND nodo_ruta_id='{nodes[1]}'")
    check('repasar no borra finalización',start(c1,versions[0],nodes[1])[1]['state']=='COMPLETADO')
    sql(f"UPDATE aprendizaje_version_ruta SET estado='PAUSADO' WHERE id='{versions[0]}'")
    check('ruta pausada rechaza inicio',start(c1,versions[0],nodes[1])[0]==404)
    print(f'{len(passed)} comprobaciones HTTP/MySQL correctas.')
finally:
    sql(f"DELETE FROM identidad_sesion_usuario WHERE usuario_id IN ({ids(users)}); DELETE FROM identidad_intento_acceso WHERE usuario_id IN ({ids(users)}); DELETE FROM aprendizaje_progreso_nodo WHERE inscripcion_id IN ({ids(enrollments)}); DELETE FROM aprendizaje_inscripcion_ruta WHERE id IN ({ids(enrollments)}); DELETE FROM aprendizaje_nodo_ruta WHERE id IN ({ids(nodes)}); DELETE FROM aprendizaje_bloque_contenido WHERE id IN ({ids(blocks)}); DELETE FROM aprendizaje_version_actividad WHERE id IN ({ids(activity_versions)}); DELETE FROM aprendizaje_actividad WHERE id IN ({ids(activities)}); DELETE FROM aprendizaje_version_ruta WHERE id IN ({ids(versions)}); DELETE FROM aprendizaje_ruta WHERE id IN ({ids(routes)}); DELETE FROM identidad_cuenta_usuario WHERE id IN ({ids(users)})")
    print('Datos ficticios de esta ejecución eliminados; datos existentes conservados.')
