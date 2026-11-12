"""Correspondencia curricular ante una resolución ministerial (HU-51). Solo entorno local.

Recorre el flujo completo contra Django (8082), Spring (8081) y MySQL reales: registra una resolución, retira una
unidad, corrige el título de otra, publica la nueva versión y actualiza una ruta del docente. En finally deja la
base como estaba con el root local: CNEB-2017 vigente, la ruta en su versión y sus unidades, y sin la versión
de prueba.

RUPI_ADMIN_CLAVE_LOCAL=... python3 -I scripts/verify_hu51_correspondencia.py \\
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
p.add_argument('--api', default='http://127.0.0.1:8081')
p.add_argument('--usuario', default='admin.rupi')
args = p.parse_args()
if not all(u.startswith(('http://127.0.0.1:', 'http://localhost:')) for u in (args.admin, args.api)):
    raise SystemExit('Esta prueba solo se ejecuta contra servicios locales.')
clave_admin = os.environ.get('RUPI_ADMIN_CLAVE_LOCAL')
if not clave_admin:
    raise SystemExit('Define RUPI_ADMIN_CLAVE_LOCAL con la clave de la cuenta de administración local.')

ORIGEN = 'http://127.0.0.1:5173'
CNEB = 'c0000000-0000-4000-8000-000000000001'
RUTA = 'd0000000-0000-4000-8000-000000000001'
cli = [str(Path(args.mysql).resolve()), '--defaults-extra-file=' + str(Path(args.root_config).resolve()),
       '--batch', '--skip-column-names', 'rupi']


def sql(sentencia):
    resultado = subprocess.run(cli, input=sentencia.encode(), capture_output=True)
    if resultado.returncode:
        raise RuntimeError(resultado.stderr.decode(errors='replace'))
    return resultado.stdout.decode().strip()


def pedir(c, url, metodo='GET', cuerpo=None, origen=ORIGEN):
    h = {'Content-Type': 'application/json', **({'Origin': origen} if origen else {})}
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    req = urllib.request.Request(url, method=metodo, headers=h, data=datos)
    try:
        with c.open(req, timeout=30) as r:
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


def sesion():
    return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))


if sql("SELECT COUNT(*) FROM curriculo_resolucion WHERE publicado_en IS NULL") != '0':
    raise SystemExit('Ya hay una resolución en borrador en esta base: la prueba no la toca.')

version_ruta, area_ruta, dueno = sql(f"""SELECT vr.id, vr.area_id, r.propietario_id FROM aprendizaje_version_ruta vr
    JOIN aprendizaje_ruta r ON r.id=vr.ruta_id WHERE r.id='{RUTA}' ORDER BY vr.numero_version DESC LIMIT 1;""").split('\t')
nodos_antes = sql(f"SELECT id, IFNULL(unidad_id,'') FROM aprendizaje_nodo_ruta WHERE version_ruta_id='{version_ruta}';")
planes_antes = sql(f"SELECT id, referencia_id FROM escuela_planificacion_ruta WHERE version_ruta_id='{version_ruta}';")
unidades = sql(f"""SELECT u.id, u.titulo FROM aprendizaje_nodo_ruta n JOIN curriculo_unidad u ON u.id=n.unidad_id
    WHERE n.version_ruta_id='{version_ruta}' GROUP BY u.id, u.titulo, u.numero_secuencia ORDER BY u.numero_secuencia;""").splitlines()
(retirar_id, retirar_titulo), (renombrar_id, renombrar_titulo) = [f.split('\t') for f in unidades[:2]]
paradas_retiradas = int(sql(f"SELECT COUNT(*) FROM aprendizaje_nodo_ruta WHERE version_ruta_id='{version_ruta}' AND unidad_id='{retirar_id}';"))
profesor = sql("SELECT id FROM identidad_cuenta_usuario WHERE nombre_usuario='profesor.demo';")
sufijo = uuid.uuid4().hex[:4].upper()
numero = f'RM 9{sufijo[:2]}-2026-MINEDU'
nuevo_titulo = f'{renombrar_titulo} (actualizada {sufijo})'
etiqueta = f'CNEB-PRUEBA-{sufijo}'
destino = None
try:
    sql(f"UPDATE aprendizaje_ruta SET propietario_id='{profesor}' WHERE id='{RUTA}';")
    a = sesion()
    print('HU-51 · consola de administración')
    estado, yo = pedir(a, f'{args.admin}/api/admin/v1/sesion', 'POST', {'usuario': args.usuario, 'clave': clave_admin})
    comprobar(estado == 200 and 'curriculo.gestionar' in yo['permisos'], 'la administración ingresa con curriculo.gestionar')
    estado, lista = pedir(a, f'{args.admin}/api/admin/v1/curriculo/resoluciones')
    comprobar(estado == 200 and lista['puedeRegistrar'] and lista['vigente']['id'] == CNEB, 'CNEB-2017 vigente y se puede registrar')
    datos = {'numero': numero, 'fechaEmision': '2026-10-30', 'resumen': 'Prueba local: retira y renombra unidades.',
             'etiqueta': etiqueta, 'vigenteDesde': '2027-03-01'}
    comprobar(pedir(a, f'{args.admin}/api/admin/v1/curriculo/resoluciones', 'POST', datos, origen=None)[0] == 403,
              'sin Origin no se registra')
    estado, det = pedir(a, f'{args.admin}/api/admin/v1/curriculo/resoluciones', 'POST', datos)
    comprobar(estado == 201 and det['resolucion']['destino']['estado'] == 'BORRADOR', 'se crea el borrador copiado')
    destino, resolucion = det['resolucion']['destino']['id'], det['resolucion']['id']
    origen_unidades = int(sql(f"SELECT COUNT(*) FROM curriculo_unidad WHERE version_catalogo_id='{CNEB}';"))
    comprobar(int(sql(f"SELECT COUNT(*) FROM curriculo_unidad WHERE version_catalogo_id='{destino}';")) == origen_unidades
              and det['resumen']['UNIDAD']['SE_MANTIENE'] == origen_unidades, f'las {origen_unidades} unidades se copian y se mantienen')
    comprobar(int(sql(f"SELECT COUNT(*) FROM curriculo_capacidad k JOIN curriculo_competencia c ON c.id=k.competencia_id "
                      f"JOIN curriculo_area x ON x.id=c.area_id WHERE x.version_catalogo_id='{destino}';")) == 99,
              'las 99 capacidades se copian con sus competencias')
    comprobar(pedir(a, f'{args.admin}/api/admin/v1/curriculo/resoluciones', 'POST', {**datos, 'numero': 'RM 1-2026-MINEDU'})[0] == 409,
              'no se abre una segunda resolución en borrador')

    corr = {f['origen']: f for f in det['elementos']['UNIDAD']}
    estado, det = pedir(a, f"{args.admin}/api/admin/v1/curriculo/resoluciones/{resolucion}/correspondencias/{corr[retirar_titulo]['id']}",
                        'PATCH', {'accion': 'RETIRAR'})
    comprobar(estado == 400, 'retirar sin sustento se rechaza')
    estado, det = pedir(a, f"{args.admin}/api/admin/v1/curriculo/resoluciones/{resolucion}/correspondencias/{corr[retirar_titulo]['id']}",
                        'PATCH', {'accion': 'RETIRAR', 'nota': 'Anexo 1 de la prueba'})
    comprobar(estado == 200 and det['impacto']['paradasSinUnidad'] >= paradas_retiradas, 'retirar muestra las paradas afectadas')
    estado, det = pedir(a, f"{args.admin}/api/admin/v1/curriculo/resoluciones/{resolucion}/correspondencias/{corr[renombrar_titulo]['id']}",
                        'PATCH', {'accion': 'CORREGIR', 'titulo': nuevo_titulo, 'nota': 'Anexo 2 de la prueba'})
    cambios = {f['origen']: f['cambio'] for f in det['elementos']['UNIDAD']}
    comprobar(cambios[retirar_titulo] == 'RETIRADA' and cambios[renombrar_titulo] == 'MODIFICADA', 'la correspondencia marca retirada y modificada')

    comprobar(pedir(a, f'{args.admin}/api/admin/v1/curriculo/resoluciones/{resolucion}/publicar', 'POST', {'confirmacion': 'RM'})[0] == 400,
              'publicar exige escribir el número exacto')
    estado, det = pedir(a, f'{args.admin}/api/admin/v1/curriculo/resoluciones/{resolucion}/publicar', 'POST', {'confirmacion': numero})
    comprobar(estado == 200 and det['resolucion']['estado'] == 'PUBLICADA', 'la resolución se publica')
    comprobar(sql(f"SELECT estado FROM curriculo_version_catalogo WHERE id='{CNEB}';") == 'RETIRADO'
              and sql(f"SELECT estado FROM curriculo_version_catalogo WHERE id='{destino}';") == 'ACTIVO',
              'CNEB-2017 queda retirada y la nueva versión vigente')
    comprobar(int(sql(f"SELECT COUNT(*) FROM curriculo_unidad WHERE version_catalogo_id='{destino}';")) == origen_unidades - 1,
              'la unidad retirada ya no está en la nueva versión')
    comprobar(sql(f"SELECT COUNT(*) FROM curriculo_correspondencia WHERE resolucion_id='{resolucion}' AND cambio='RETIRADA' AND destino_id IS NOT NULL;") == '0',
              'lo retirado queda sin destino en la correspondencia')
    estado, _ = pedir(a, f"{args.admin}/api/admin/v1/curriculo/resoluciones/{resolucion}/correspondencias/{corr[renombrar_titulo]['id']}",
                      'PATCH', {'accion': 'MANTENER'})
    comprobar(estado == 409, 'una resolución publicada ya no se corrige')

    print('HU-51 · ruta del docente')
    d = sesion()
    pedir(d, f'{args.api}/api/v1/auth/login', 'POST', {'username': 'profesor.demo', 'password': '123456'})
    url = f'{args.api}/api/v1/teacher/learning-routes/{RUTA}/curriculum-update'
    estado, plan = pedir(d, url)
    comprobar(estado == 200 and plan['available'] and plan['toVersion'] == etiqueta and plan['resolutions'][0]['number'] == numero,
              'el docente ve la resolución y la versión vigente')
    por_titulo = {u['fromTitle']: u for u in plan['units']}
    comprobar(por_titulo[retirar_titulo]['change'] == 'RETIRADA' and por_titulo[renombrar_titulo]['toTitle'] == nuevo_titulo,
              'el plan muestra la unidad retirada y la renombrada')
    comprobar(plan['stopsWithoutUnit'] == paradas_retiradas, f'{paradas_retiradas} paradas quedarán en «Otras paradas»')
    comprobar(pedir(d, url, 'POST', origen=None)[0] == 403, 'actualizar sin Origin se rechaza')
    estado, plan = pedir(d, url, 'POST')
    comprobar(estado == 200, 'el docente actualiza su ruta')
    comprobar(sql(f"SELECT version_catalogo_id FROM aprendizaje_version_ruta WHERE id='{version_ruta}';") == destino,
              'la ruta pasa a la nueva versión')
    comprobar(int(sql(f"SELECT COUNT(*) FROM aprendizaje_nodo_ruta WHERE version_ruta_id='{version_ruta}' AND unidad_id IS NULL;")) == paradas_retiradas,
              'las paradas de la unidad retirada quedan sin unidad')
    comprobar(sql(f"""SELECT DISTINCT u.titulo FROM aprendizaje_nodo_ruta n JOIN curriculo_unidad u ON u.id=n.unidad_id
        WHERE n.version_ruta_id='{version_ruta}' AND u.titulo LIKE '%{sufijo}%';""") == nuevo_titulo,
              'las paradas de la unidad renombrada siguen a su equivalente')
    comprobar(sql(f"""SELECT COUNT(*) FROM aprendizaje_nodo_ruta n JOIN curriculo_unidad u ON u.id=n.unidad_id
        WHERE n.version_ruta_id='{version_ruta}' AND u.version_catalogo_id<>'{destino}';""") == '0',
              'ninguna parada queda en una unidad de la versión retirada')
    estado, plan = pedir(d, url)
    comprobar(estado == 200 and not plan['available'] and plan['fromVersion'] == plan['toVersion'], 'ya no se ofrece actualizar')
    e = sesion()
    pedir(e, f'{args.api}/api/v1/auth/login', 'POST', {'username': 'diego.ccori', 'password': '123456'})
    comprobar(pedir(e, url)[0] == 403, 'un estudiante no puede consultar la actualización')
    print('Todo en orden.')
finally:
    restaurar_nodos = '\n'.join(
        f"UPDATE aprendizaje_nodo_ruta SET unidad_id={repr(u) if u else 'NULL'} WHERE id='{n}';"
        for n, u in (f.split('\t') for f in nodos_antes.splitlines() if f))
    restaurar_planes = '\n'.join(
        f"UPDATE escuela_planificacion_ruta SET referencia_id='{r}' WHERE id='{i}';"
        for i, r in (f.split('\t') for f in planes_antes.splitlines() if f))
    sql(f"""
        UPDATE aprendizaje_version_ruta SET version_catalogo_id='{CNEB}', area_id='{area_ruta}' WHERE id='{version_ruta}';
        {restaurar_nodos}
        {restaurar_planes}
        UPDATE aprendizaje_ruta SET propietario_id='{dueno}' WHERE id='{RUTA}';
        UPDATE curriculo_version_catalogo SET estado='ACTIVO' WHERE id='{CNEB}';
    """)
    if destino:
        sql(f"""
            DELETE FROM curriculo_correspondencia WHERE resolucion_id IN (SELECT id FROM curriculo_resolucion WHERE version_destino_id='{destino}');
            DELETE FROM curriculo_resolucion WHERE version_destino_id='{destino}';
            DELETE l FROM curriculo_unidad_meta l JOIN curriculo_unidad u ON u.id=l.unidad_id WHERE u.version_catalogo_id='{destino}';
            DELETE l FROM curriculo_unidad_estandar l JOIN curriculo_unidad u ON u.id=l.unidad_id WHERE u.version_catalogo_id='{destino}';
            DELETE l FROM curriculo_unidad_competencia l JOIN curriculo_unidad u ON u.id=l.unidad_id WHERE u.version_catalogo_id='{destino}';
            DELETE m FROM curriculo_meta_aprendizaje m JOIN curriculo_competencia c ON c.id=m.competencia_id JOIN curriculo_area x ON x.id=c.area_id WHERE x.version_catalogo_id='{destino}';
            DELETE s FROM curriculo_estandar s JOIN curriculo_competencia c ON c.id=s.competencia_id JOIN curriculo_area x ON x.id=c.area_id WHERE x.version_catalogo_id='{destino}';
            DELETE k FROM curriculo_capacidad k JOIN curriculo_competencia c ON c.id=k.competencia_id JOIN curriculo_area x ON x.id=c.area_id WHERE x.version_catalogo_id='{destino}';
            DELETE FROM curriculo_unidad WHERE version_catalogo_id='{destino}';
            DELETE c FROM curriculo_competencia c JOIN curriculo_area x ON x.id=c.area_id WHERE x.version_catalogo_id='{destino}';
            DELETE FROM curriculo_area WHERE version_catalogo_id='{destino}';
            DELETE FROM curriculo_version_catalogo WHERE id='{destino}';
        """)
    print('  · base restaurada: CNEB-2017 vigente y la ruta en sus unidades originales')
