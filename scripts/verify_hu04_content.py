"""HU-04: valida el contenido curricular de la ruta demo contra el orden esperado (scripts/fixtures).
Revisa MySQL (reglas de coherencia) y la API (orden entregado). Solo lectura. Entorno local.
python3 -I scripts/verify_hu04_content.py --root-config .local/root.cnf --mysql .local/mysql-8.4.11-linux-glibc2.28-x86_64/bin/mysql
"""
import argparse, http.cookiejar, json, subprocess, sys, urllib.error, urllib.request
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('--root-config', required=True)
p.add_argument('--mysql', required=True)
p.add_argument('--base', default='http://127.0.0.1:8081')
p.add_argument('--user', default='estudiante.demo')
p.add_argument('--password', default='123456')
args = p.parse_args()
if not args.base.startswith(('http://127.0.0.1:', 'http://localhost:')):
    raise SystemExit('Solo contra la API local.')
fx = json.loads((Path(__file__).parent / 'fixtures' / 'hu04-orden-esperado.json').read_text(encoding='utf-8'))
RUTA = fx['versionRutaId']
cli = [str(Path(args.mysql).resolve()), '--defaults-extra-file=' + str(Path(args.root_config).resolve()),
       '--batch', '--skip-column-names', '--default-character-set=utf8mb4', 'rupi']
def sql(q):
    r = subprocess.run(cli, input=q.encode(), capture_output=True)
    if r.returncode: raise RuntimeError(r.stderr.decode(errors='replace'))
    return [line.split('\t') for line in r.stdout.decode().splitlines() if line]
def one(q): return sql(q)[0][0]
fails = []
def check(name, ok):
    print(('OK:' if ok else 'FALLA:'), name)
    if not ok: fails.append(name)

# 1. Catálogo y datos oficiales
check('catálogo vigente ' + fx['catalogo'],
      sql(f"SELECT c.etiqueta_version,c.estado FROM curriculo_version_catalogo c JOIN aprendizaje_version_ruta r ON r.version_catalogo_id=c.id WHERE r.id='{RUTA}'") == [[fx['catalogo'], 'ACTIVO']])
check('cada competencia esperada tiene su estándar y los desempeños de 2.° esperados',
      all(int(one(f"SELECT COUNT(*) FROM curriculo_competencia c JOIN curriculo_estandar e ON e.competencia_id=c.id WHERE c.codigo='{k}'")) == 1
          and int(one(f"SELECT COUNT(*) FROM curriculo_meta_aprendizaje m JOIN curriculo_competencia c ON c.id=m.competencia_id JOIN curriculo_grado g ON g.id=m.grado_id WHERE c.codigo='{k}' AND g.numero_grado=2")) == n
          for k, n in fx['desempenosPorCompetencia'].items()))
check('ningún texto oficial está vacío',
      one("SELECT COUNT(*) FROM curriculo_estandar WHERE TRIM(descripcion)=''") == '0'
      and one("SELECT COUNT(*) FROM curriculo_meta_aprendizaje WHERE TRIM(descripcion)=''") == '0')

# 2. Coherencia de la ruta
check('todos los nodos de la ruta tienen unidad', one(f"SELECT COUNT(*) FROM aprendizaje_nodo_ruta WHERE version_ruta_id='{RUTA}' AND unidad_id IS NULL") == '0')
check('unidades del mismo catálogo, grado y área que la ruta',
      one(f"SELECT COUNT(*) FROM aprendizaje_nodo_ruta n JOIN aprendizaje_version_ruta r ON r.id=n.version_ruta_id JOIN curriculo_unidad u ON u.id=n.unidad_id WHERE r.id='{RUTA}' AND (u.version_catalogo_id<>r.version_catalogo_id OR u.grado_id<>r.grado_id OR u.area_id<>r.area_id)") == '0')
check('competencias de las unidades del mismo catálogo y área que la ruta',
      one(f"SELECT COUNT(*) FROM curriculo_unidad_competencia uc JOIN curriculo_competencia c ON c.id=uc.competencia_id JOIN curriculo_area a ON a.id=c.area_id JOIN aprendizaje_version_ruta r ON r.id='{RUTA}' WHERE uc.unidad_id IN (SELECT unidad_id FROM aprendizaje_nodo_ruta WHERE version_ruta_id='{RUTA}') AND (a.version_catalogo_id<>r.version_catalogo_id OR a.id<>r.area_id)") == '0')
check('cada actividad de la ruta está vinculada a la unidad de su nodo',
      one(f"SELECT COUNT(*) FROM aprendizaje_nodo_ruta n LEFT JOIN aprendizaje_actividad_unidad au ON au.version_actividad_id=n.version_actividad_id AND au.unidad_id=n.unidad_id WHERE n.version_ruta_id='{RUTA}' AND au.unidad_id IS NULL") == '0')

# 3. Orden en la base (unidad por secuencia curricular, paradas por secuencia) frente al orden esperado
rows = sql(f"SELECT u.titulo, va.titulo FROM aprendizaje_nodo_ruta n JOIN curriculo_unidad u ON u.id=n.unidad_id JOIN aprendizaje_version_actividad va ON va.id=n.version_actividad_id WHERE n.version_ruta_id='{RUTA}' ORDER BY u.numero_secuencia, n.numero_secuencia")
def agrupar(pares):
    out = []
    for unidad, parada in pares:
        if not out or out[-1][0] != unidad: out.append((unidad, []))
        out[-1][1].append(parada)
    return out
esperado = [(u['titulo'], u['paradas']) for u in fx['unidades']]
check('orden en la base = orden esperado', agrupar(rows) == esperado)

# 4. Orden que entrega la API
c = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
def api(path, body=None):
    req = urllib.request.Request(args.base + '/api/v1/' + path, method='POST' if body is not None else 'GET',
        headers={'Origin': args.base, 'Content-Type': 'application/json'}, data=json.dumps(body).encode() if body is not None else None)
    with c.open(req, timeout=20) as r: return json.loads(r.read() or 'null')
try:
    api('auth/login', {'username': args.user, 'password': args.password})
    ruta = api('student/learning-routes/' + RUTA)
    titulo = {n['id']: n['title'] for n in ruta['nodes']}
    cur = ruta['curriculum']
    check('la API responde COMPLETA', cur['status'] == 'COMPLETA')
    check('orden de unidades y paradas en la API = orden esperado',
          [(u['title'], [titulo[i] for i in u['nodeIds']]) for u in cur['units']] == esperado)
    check('competencias por unidad en la API = esperadas',
          [[x['code'] for x in u['competencies']] for u in cur['units']] == [u['competencias'] for u in fx['unidades']])
except (urllib.error.URLError, KeyError) as e:
    check(f'la API respondió con la ruta y su currículo ({e})', False)

print(f"\n{'TODO CORRECTO' if not fails else str(len(fails)) + ' comprobaciones fallaron'}")
sys.exit(1 if fails else 0)
