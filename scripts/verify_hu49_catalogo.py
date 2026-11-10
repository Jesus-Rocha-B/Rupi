"""Catálogo curricular del MINEDU (HU-49) contra Django (8082) y MySQL reales. Solo entorno local.
Revisa el CNEB 2017 que deja V39 y prueba las escrituras en una versión BORRADOR propia, que se borra en
finally con el root local, para no tocar el catálogo que usan las rutas.

RUPI_ADMIN_CLAVE_LOCAL=... python3 -I scripts/verify_hu49_catalogo.py \\
    --root-config .local/root.cnf --mysql .local/mysql-8.4.11-linux-glibc2.28-x86_64/bin/mysql
"""
import argparse
import http.cookiejar
import json
import os
import subprocess
import urllib.error
import urllib.request
import uuid
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('--root-config', required=True)
p.add_argument('--mysql', required=True)
p.add_argument('--admin', default='http://127.0.0.1:8082')
p.add_argument('--usuario', default='admin.rupi')
args = p.parse_args()
if not args.admin.startswith(('http://127.0.0.1:', 'http://localhost:')):
    raise SystemExit('Esta prueba solo se ejecuta contra servicios locales.')
clave_admin = os.environ.get('RUPI_ADMIN_CLAVE_LOCAL')
if not clave_admin:
    raise SystemExit('Define RUPI_ADMIN_CLAVE_LOCAL con la clave de la cuenta de administración local.')

ORIGEN = 'http://127.0.0.1:5173'
CNEB = 'c0000000-0000-4000-8000-000000000001'
cli = [str(Path(args.mysql).resolve()), '--defaults-extra-file=' + str(Path(args.root_config).resolve()),
       '--batch', '--skip-column-names', 'rupi']


def sql(sentencia):
    resultado = subprocess.run(cli, input=sentencia.encode(), capture_output=True)
    if resultado.returncode:
        raise RuntimeError(resultado.stderr.decode(errors='replace'))
    return resultado.stdout.decode().strip()


def pedir(c, ruta, metodo='GET', cuerpo=None, origen=ORIGEN):
    h = {'Content-Type': 'application/json', **({'Origin': origen} if origen else {})}
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    req = urllib.request.Request(f'{args.admin}/api/admin/v1/{ruta}', method=metodo, headers=h, data=datos)
    try:
        with c.open(req, timeout=20) as r:
            crudo = r.read()
            return r.status, (json.loads(crudo) if crudo else None)
    except urllib.error.HTTPError as error:
        crudo = error.read()
        try:
            return error.code, (json.loads(crudo) if crudo else None)
        except ValueError:
            return error.code, None


def comprobar(condicion, mensaje):
    if not condicion:
        raise AssertionError(mensaje)
    print('  ok ·', mensaje)


def area_de(catalogo, codigo):
    return next(a for a in catalogo['areas'] if a['codigo'] == codigo)


version_prueba = str(uuid.uuid4())
sufijo = uuid.uuid4().hex[:6].upper()
try:
    c = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    print('HU-49 · catálogo de grados, áreas y competencias')
    comprobar(pedir(c, 'curriculo/catalogo')[0] == 401, 'sin sesión el catálogo responde 401')
    estado, yo = pedir(c, 'sesion', 'POST', {'usuario': args.usuario, 'clave': clave_admin})
    comprobar(estado == 200 and {'curriculo.ver', 'curriculo.gestionar'} <= set(yo['permisos']),
              'la administración ingresa con curriculo.ver y curriculo.gestionar')

    estado, cat = pedir(c, 'curriculo/catalogo')
    comprobar(estado == 200 and cat['version']['id'] == CNEB, 'por defecto se abre la versión ACTIVA CNEB-2017')
    comprobar([g['numero'] for g in cat['grados']] == [1, 2, 3, 4, 5, 6], 'los seis grados de primaria')
    comprobar([a['codigo'] for a in cat['areas']] == ['SOC', 'EF', 'COM', 'ART', 'CAS2', 'ING', 'MAT', 'CYT', 'REL'],
              'las nueve áreas en el orden del programa curricular')
    numeros = [c_['numeroCneb'] for a in cat['areas'] for c_ in a['competencias']]
    comprobar(sorted(numeros) == [*range(1, 27), 28, 29], 'las 28 competencias de área con su número CNEB')
    comprobar(cat['resumen'] == {'grados': 6, 'areas': 9, 'competencias': 28, 'capacidades': 99},
              'el resumen cuenta 6 grados, 9 áreas, 28 competencias y 99 capacidades')
    mat_c1 = area_de(cat, 'MAT')['competencias'][0]
    comprobar(mat_c1['codigo'] == 'MAT-C1' and len(mat_c1['capacidades']) == 4 and mat_c1['unidades'] > 0,
              'MAT-C1 trae sus cuatro capacidades y las unidades que la desarrollan')

    sql(f"INSERT INTO curriculo_version_catalogo (id, etiqueta_version, estado) "
        f"VALUES ('{version_prueba}', 'PRUEBA-HU49-{sufijo}', 'BORRADOR');")
    comprobar(pedir(c, 'curriculo/areas', 'POST', {'version': version_prueba, 'codigo': 'MAT', 'nombre': 'Matemática'},
                    origen='http://evil.example')[0] == 403, 'una escritura desde otro origen se rechaza')
    estado, cat = pedir(c, 'curriculo/areas', 'POST', {'version': version_prueba, 'codigo': 'mat', 'nombre': ' Matemática ',
                                                       'orden': 7})
    comprobar(estado == 201 and area_de(cat, 'MAT')['nombre'] == 'Matemática', 'se crea un área en la versión borrador')
    area_id = area_de(cat, 'MAT')['id']
    comprobar(pedir(c, 'curriculo/areas', 'POST', {'version': version_prueba, 'codigo': 'MAT', 'nombre': 'Otra'})[0] == 409,
              'no se repite el código del área en la misma versión')
    comprobar(pedir(c, f'curriculo/areas/{area_id}', 'PATCH', {'codigo': 'MATE'})[0] == 400,
              'el código del área no se cambia')

    cuerpo = {'area': area_id, 'codigo': 'MAT-C1', 'numeroCneb': 23, 'nombre': 'Resuelve problemas de cantidad',
              'capacidades': ['Traduce cantidades a expresiones numéricas', 'Usa estrategias de cálculo']}
    estado, cat = pedir(c, 'curriculo/competencias', 'POST', cuerpo)
    competencia = area_de(cat, 'MAT')['competencias'][0]
    comprobar(estado == 201 and [k['orden'] for k in competencia['capacidades']] == [1, 2],
              'se crea una competencia con sus capacidades en orden')
    comprobar(pedir(c, 'curriculo/competencias', 'POST', {**cuerpo, 'codigo': 'MAT-C9'})[0] == 409,
              'el número CNEB no se repite en la misma versión')
    comprobar(pedir(c, 'curriculo/competencias', 'POST', {**cuerpo, 'codigo': 'COM-C1', 'numeroCneb': None})[0] == 400,
              'el código de la competencia empieza con el del área')

    estado, cat = pedir(c, f"curriculo/competencias/{competencia['id']}", 'PATCH',
                        {'capacidades': ['Traduce cantidades a expresiones numéricas']})
    comprobar(estado == 200 and len(area_de(cat, 'MAT')['competencias'][0]['capacidades']) == 1,
              'corregir la competencia quita la capacidad sobrante')
    auditados = sql(f"SELECT GROUP_CONCAT(accion ORDER BY id) FROM operaciones_evento_auditoria "
                    f"WHERE dominio = 'curriculo' AND entidad_id IN ('{area_id}', '{competencia['id']}');")
    comprobar(auditados == 'AREA_CREADA,COMPETENCIA_CREADA,COMPETENCIA_MODIFICADA',
              'la auditoría registra cada cambio del catálogo')

    sql(f"UPDATE curriculo_version_catalogo SET estado = 'RETIRADO' WHERE id = '{version_prueba}';")
    comprobar(pedir(c, f'curriculo/areas/{area_id}', 'PATCH', {'nombre': 'Matemáticas'})[0] == 409,
              'una versión retirada ya no se corrige')
    estado, cat = pedir(c, f'curriculo/catalogo?version={version_prueba}')
    comprobar(estado == 200 and cat['editable'] is False, 'la versión retirada se consulta como registro')
    pedir(c, 'sesion', 'DELETE')
    print('Todo en orden.')
finally:
    sql(f"""
        DELETE FROM operaciones_evento_auditoria WHERE entidad_id IN (
          SELECT id FROM curriculo_area WHERE version_catalogo_id = '{version_prueba}'
          UNION SELECT c.id FROM curriculo_competencia c JOIN curriculo_area a ON a.id = c.area_id
                WHERE a.version_catalogo_id = '{version_prueba}');
        DELETE k FROM curriculo_capacidad k JOIN curriculo_competencia c ON c.id = k.competencia_id
          JOIN curriculo_area a ON a.id = c.area_id WHERE a.version_catalogo_id = '{version_prueba}';
        DELETE c FROM curriculo_competencia c JOIN curriculo_area a ON a.id = c.area_id
          WHERE a.version_catalogo_id = '{version_prueba}';
        DELETE FROM curriculo_area WHERE version_catalogo_id = '{version_prueba}';
        DELETE FROM curriculo_version_catalogo WHERE id = '{version_prueba}';
    """)
