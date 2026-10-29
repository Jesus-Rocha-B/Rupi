"""Consola de administración contra Django (8082), Spring (8081) y MySQL reales. Solo entorno local.
Crea cuentas ficticias propias y las borra en finally con el root local.

RUPI_ADMIN_CLAVE_LOCAL=... python3 -I scripts/verify_admin_cuentas.py \\
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
for base in (args.admin, args.api):
    if not base.startswith(('http://127.0.0.1:', 'http://localhost:')):
        raise SystemExit('Esta prueba solo se ejecuta contra servicios locales.')
clave_admin = os.environ.get('RUPI_ADMIN_CLAVE_LOCAL')
if not clave_admin:
    raise SystemExit('Define RUPI_ADMIN_CLAVE_LOCAL con la clave de la cuenta de administración local.')

ORIGEN = 'http://127.0.0.1:5173'
cli = [str(Path(args.mysql).resolve()), '--defaults-extra-file=' + str(Path(args.root_config).resolve()),
       '--batch', '--skip-column-names', 'rupi']


def sql(sentencia):
    resultado = subprocess.run(cli, input=sentencia.encode(), capture_output=True)
    if resultado.returncode:
        raise RuntimeError(resultado.stderr.decode(errors='replace'))
    return resultado.stdout.decode().strip()


def cliente():
    return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))


def pedir(c, url, metodo='GET', cuerpo=None, cabeceras=None):
    h = {'Origin': ORIGEN, 'Content-Type': 'application/json', **(cabeceras or {})}
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else (b'' if metodo in ('POST', 'PATCH') else None)
    req = urllib.request.Request(url, method=metodo, headers=h, data=datos)
    try:
        with c.open(req, timeout=20) as r:
            crudo = r.read()
            return r.status, (json.loads(crudo) if crudo else None), dict(r.headers)
    except urllib.error.HTTPError as error:
        crudo = error.read()
        try:
            cuerpo_error = json.loads(crudo) if crudo else None
        except ValueError:
            cuerpo_error = None
        return error.code, cuerpo_error, dict(error.headers)


def admin(c, ruta, metodo='GET', cuerpo=None):
    return pedir(c, f'{args.admin}/api/admin/v1/{ruta}', metodo, cuerpo)


def api(c, ruta, metodo='GET', cuerpo=None, cabeceras=None):
    return pedir(c, f'{args.api}/api/v1/{ruta}', metodo, cuerpo, cabeceras)


def comprobar(condicion, mensaje):
    if not condicion:
        raise AssertionError(mensaje)
    print('  ok ·', mensaje)


creadas = []
sufijo = uuid.uuid4().hex[:6]
try:
    print('HU-41 · altas, bajas y modificaciones')
    a = cliente()
    comprobar(admin(a, 'cuentas')[0] == 401, 'sin sesión la consola responde 401')
    comprobar(admin(a, 'sesion', 'POST', {'usuario': args.usuario, 'clave': 'mal'})[0] == 401, 'clave errada da 401')
    estado, yo, _ = admin(a, 'sesion', 'POST', {'usuario': args.usuario, 'clave': clave_admin})
    comprobar(estado == 200 and 'cuentas.gestionar' in yo['permisos'], 'la administración ingresa con sus permisos')

    estado, nueva, _ = admin(a, 'cuentas', 'POST', {'usuario': f'prueba.{sufijo}', 'nombre': 'Nayeli', 'apellido': 'Quispe',
                                                    'rol': 'ESTUDIANTE'})
    comprobar(estado == 201 and nueva['claveTemporal'] and nueva['roles'] == ['ESTUDIANTE'], 'crea una cuenta de estudiante')
    creadas.append(nueva['id'])
    comprobar(admin(a, 'cuentas', 'POST', {'usuario': f'prueba.{sufijo}', 'nombre': 'X', 'apellido': 'Y',
                                           'rol': 'ESTUDIANTE'})[0] == 409, 'usuario repetido da 409')

    alumno = cliente()
    estado, sesion, _ = api(alumno, 'auth/login', 'POST', {'username': f'prueba.{sufijo}', 'password': nueva['claveTemporal']})
    comprobar(estado == 200 and sesion['role'] == 'ESTUDIANTE', 'Spring acepta la clave temporal creada por Django')

    estado, editada, _ = admin(a, f'cuentas/{nueva["id"]}', 'PATCH', {'nombrePreferido': 'Nayi'})
    comprobar(estado == 200 and editada['nombrePreferido'] == 'Nayi', 'modifica el nombre preferido')
    comprobar(api(alumno, 'student/session')[1]['name'] == 'Nayi', 'la web muestra el nombre nuevo sin volver a ingresar')

    comprobar(admin(a, f'cuentas/{yo["id"]}/baja', 'POST')[0] == 409, 'no puede darse de baja a sí misma')
    estado, baja, _ = admin(a, f'cuentas/{nueva["id"]}/baja', 'POST')
    comprobar(estado == 200 and baja['estado'] == 'DESHABILITADO' and baja['sesionActiva'] is None, 'da de baja y cierra la sesión')
    comprobar(api(alumno, 'student/session')[0] == 401, 'la sesión abierta en la web deja de valer')
    comprobar(api(cliente(), 'auth/login', 'POST', {'username': f'prueba.{sufijo}',
                                                    'password': nueva['claveTemporal']})[0] == 401, 'una cuenta de baja no ingresa')
    estado, activa, _ = admin(a, f'cuentas/{nueva["id"]}/reactivar', 'POST')
    comprobar(estado == 200 and activa['estado'] == 'ACTIVO', 'reactiva la cuenta')
    comprobar(api(cliente(), 'auth/login', 'POST', {'username': f'prueba.{sufijo}',
                                                    'password': nueva['claveTemporal']})[0] == 200, 'vuelve a ingresar')
    acciones = [h['accion'] for h in admin(a, f'cuentas/{nueva["id"]}')[1]['historial']]
    comprobar(acciones == ['CUENTA_REACTIVADA', 'CUENTA_DADA_DE_BAJA', 'CUENTA_MODIFICADA', 'CUENTA_CREADA'],
              'cada cambio queda en la auditoría')
    estado, pagina, _ = admin(a, f'cuentas?q=prueba.{sufijo}&rol=ESTUDIANTE')
    comprobar(estado == 200 and pagina['total'] == 1, 'la búsqueda filtra por usuario y rol')

    print('HU-42 · roles y permisos estrictos')
    estado, catalogo, _ = admin(a, 'roles')
    roles = {r['codigo']: r for r in catalogo['roles']}
    comprobar(estado == 200 and {'ESTUDIANTE', 'DOCENTE', 'SOPORTE', 'ADMINISTRADOR'} <= roles.keys(), 'lista los cuatro roles')
    comprobar('consola.ingresar' in roles['SOPORTE']['permisos'] and 'cuentas.gestionar' not in roles['SOPORTE']['permisos'],
              'mesa de ayuda entra a la consola pero no gestiona cuentas')
    comprobar(not roles['ESTUDIANTE']['consola'] and not roles['DOCENTE']['consola'], 'estudiantes y docentes no usan la consola')

    estado, mesa, _ = admin(a, 'cuentas', 'POST', {'usuario': f'mesa.{sufijo}', 'nombre': 'Edith', 'apellido': 'Condori',
                                                   'correo': f'edith.{sufijo}@rupi.pe', 'rol': 'DOCENTE'})
    creadas.append(mesa['id'])
    comprobar(admin(a, f'cuentas/{nueva["id"]}/roles', 'PUT', {'roles': ['ESTUDIANTE', 'DOCENTE']})[0] == 409,
              'una cuenta de estudiante no recibe otros roles')
    comprobar(admin(a, f'cuentas/{nueva["id"]}/roles', 'PUT', {'roles': []})[0] == 409, 'toda cuenta conserva un rol')
    comprobar(admin(a, f'cuentas/{nueva["id"]}/roles', 'PUT', {'roles': ['RECTOR']})[0] == 400, 'rechaza roles inexistentes')
    comprobar(admin(a, f'cuentas/{yo["id"]}/roles', 'PUT', {'roles': ['DOCENTE']})[0] == 409,
              'nadie se quita su propio rol de administración')

    docente = cliente()
    comprobar(api(docente, 'auth/login', 'POST', {'username': f'mesa.{sufijo}', 'password': mesa['claveTemporal']})[0] == 200,
              'la docente ingresa a la web')
    estado, cambiada, _ = admin(a, f'cuentas/{mesa["id"]}/roles', 'PUT', {'roles': ['SOPORTE']})
    comprobar(estado == 200 and cambiada['roles'] == ['SOPORTE'] and cambiada['sesionActiva'] is None,
              'pasa a mesa de ayuda y se cierra su sesión de la web')
    comprobar(api(docente, 'teacher/session')[0] == 401, 'su sesión docente anterior ya no abre el panel')
    estado, cuerpo, _ = api(cliente(), 'auth/login', 'POST', {'username': f'mesa.{sufijo}', 'password': mesa['claveTemporal']})
    comprobar(estado == 403 and '/admin' in (cuerpo or {}).get('detail', ''), 'Spring la manda a la consola con 403')

    s = cliente()
    estado, soporte, _ = admin(s, 'sesion', 'POST', {'usuario': f'mesa.{sufijo}', 'clave': mesa['claveTemporal']})
    comprobar(estado == 200 and soporte['permisos'] == ['consola.ingresar', 'cuentas.ver'], 'mesa de ayuda entra con permisos acotados')
    estado, vista, _ = admin(s, f'cuentas/{mesa["id"]}')
    comprobar(estado == 200 and vista['correo'].startswith('e•••@') and not vista['datosCompletos'],
              'mesa de ayuda ve el correo enmascarado')
    comprobar(admin(s, f'cuentas?q=edith.{sufijo}')[1]['total'] == 0, 'no puede buscar por el correo oculto')
    comprobar(admin(s, 'cuentas', 'POST', {'usuario': 'x.y', 'nombre': 'X', 'apellido': 'Y', 'rol': 'DOCENTE'})[0] == 403,
              'mesa de ayuda no crea cuentas')
    comprobar(admin(s, f'cuentas/{nueva["id"]}/baja', 'POST')[0] == 403, 'mesa de ayuda no da de baja')
    comprobar(admin(s, f'cuentas/{mesa["id"]}/roles', 'PUT', {'roles': ['ADMINISTRADOR']})[0] == 403,
              'mesa de ayuda no se asciende a sí misma')
    admin(a, f'cuentas/{mesa["id"]}/roles', 'PUT', {'roles': ['DOCENTE']})
    comprobar(admin(s, 'cuentas')[0] == 401, 'al retirarle el rol pierde la consola en la siguiente petición')
    acciones = [h['accion'] for h in admin(a, f'cuentas/{mesa["id"]}')[1]['historial']]
    comprobar('ROL_RETIRADO:SOPORTE' in acciones and 'ROL_ASIGNADO:SOPORTE' in acciones, 'los cambios de rol quedan auditados')
    print('Todo correcto.')
finally:
    for cuenta_id in creadas:
        sql(f"""
            DELETE FROM operaciones_evento_auditoria WHERE entidad_id = '{cuenta_id}';
            DELETE FROM identidad_intento_acceso WHERE usuario_id = '{cuenta_id}';
            DELETE FROM identidad_sesion_usuario WHERE usuario_id = '{cuenta_id}';
            DELETE FROM identidad_usuario_rol WHERE usuario_id = '{cuenta_id}';
            DELETE FROM identidad_perfil_usuario WHERE usuario_id = '{cuenta_id}';
            DELETE FROM identidad_cuenta_usuario WHERE id = '{cuenta_id}';
        """)
