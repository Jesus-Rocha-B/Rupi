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
    # Usuario inventado: así cada corrida no suma fallos a la cuenta de administración (límite de 5 en 5 minutos).
    comprobar(admin(a, 'sesion', 'POST', {'usuario': f'nadie.{sufijo}', 'clave': 'mal'})[0] == 401, 'credenciales erradas dan 401')
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
    comprobar(estado == 200 and 'cuentas.ver' in soporte['permisos']
              and not {'cuentas.gestionar', 'roles.asignar', 'cuentas.datos_personales'} & set(soporte['permisos']),
              'mesa de ayuda entra con permisos acotados')
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

    print('HU-43 · sesión única entre la web y el celular')
    clave = nueva['claveTemporal']
    web, movil = cliente(), cliente()
    comprobar(api(web, 'auth/login', 'POST', {'username': f'prueba.{sufijo}', 'password': clave})[0] == 200, 'ingresa en la web')
    comprobar(api(web, 'student/session')[0] == 200, 'la web tiene su sesión')
    estado, _, _ = api(movil, 'auth/login', 'POST', {'username': f'prueba.{sufijo}', 'password': clave},
                       {'X-Rupi-Cliente': 'MOVIL'})
    comprobar(estado == 200, 'ingresa en el celular con la misma cuenta')
    estado, _, cabeceras = api(web, 'student/learning-routes')
    comprobar(estado == 401 and cabeceras.get('X-Rupi-Sesion-Cerrada') == 'OTRO_DISPOSITIVO'
              and cabeceras.get('X-Rupi-Sesion-Nueva') == 'MOVIL', 'la web recibe 401 explicando que se abrió en un celular')
    estado, _, cabeceras = api(web, 'auth/session')
    comprobar(estado == 401 and cabeceras.get('X-Rupi-Sesion-Cerrada') == 'OTRO_DISPOSITIVO', 'la comprobación periódica también lo explica')
    comprobar(api(movil, 'student/session')[0] == 200, 'el celular sigue con su sesión')
    activas = int(sql(f"SELECT COUNT(*) FROM identidad_sesion_usuario WHERE usuario_id = '{nueva['id']}' AND revocada_en IS NULL"))
    comprobar(activas == 1, 'en la base queda una sola sesión activa')
    estado, lista, _ = admin(a, 'sesiones?cliente=MOVIL')
    comprobar(estado == 200 and any(i['cuentaId'] == nueva['id'] and i['cliente'] == 'MOVIL' for i in lista['items']),
              'la consola la muestra abierta en el celular')
    comprobar(admin(a, f'cuentas/{nueva["id"]}')[1]['sesionActiva']['cliente'] == 'MOVIL', 'el detalle de la cuenta dice dónde está abierta')
    estado, cerrada, _ = admin(a, f'cuentas/{nueva["id"]}/cerrar-sesion', 'POST')
    comprobar(estado == 200 and cerrada['sesionActiva'] is None, 'la administración cierra la sesión del celular')
    estado, _, cabeceras = api(movil, 'student/session')
    comprobar(estado == 401 and cabeceras.get('X-Rupi-Sesion-Cerrada') == 'CERRADA', 'el celular recibe 401 de sesión cerrada')
    comprobar(admin(a, f'cuentas/{yo["id"]}/cerrar-sesion', 'POST')[0] == 409, 'la consola no cierra su propia sesión por esta vía')
    comprobar('SESION_CERRADA' in [h['accion'] for h in admin(a, f'cuentas/{nueva["id"]}')[1]['historial']],
              'el cierre queda auditado')
    otra_consola = cliente()
    comprobar(admin(otra_consola, 'sesion', 'POST', {'usuario': args.usuario, 'clave': clave_admin})[0] == 200,
              'la misma administración abre otra consola')
    estado, cuerpo, _ = admin(a, 'cuentas')
    comprobar(estado == 401 and 'otra consola' in cuerpo['detail'], 'la primera consola explica por qué se cerró')
    a = otra_consola

    print('HU-44 · contraseñas y desbloqueo a pedido')
    usuario = f'prueba.{sufijo}'
    estado, bloqueada, _ = admin(a, f'cuentas/{nueva["id"]}/bloquear', 'POST')
    comprobar(estado == 200 and bloqueada['estado'] == 'BLOQUEADO' and bloqueada['bloqueada'], 'bloquea la cuenta a pedido')
    estado, cuerpo, _ = api(cliente(), 'auth/login', 'POST', {'username': usuario, 'password': clave})
    comprobar(estado == 423 and 'desbloqueo' in (cuerpo or {}).get('detail', ''), 'con la clave correcta Spring avisa 423 «bloqueada»')
    comprobar(api(cliente(), 'auth/login', 'POST', {'username': usuario, 'password': 'otra-clave-9'})[0] == 401,
              'con una clave equivocada no revela que está bloqueada')
    estado, libre, _ = admin(a, f'cuentas/{nueva["id"]}/desbloquear', 'POST')
    comprobar(estado == 200 and libre['estado'] == 'ACTIVO' and libre['intentosFallidos'] >= 2,
              'la desbloquea y muestra los intentos fallidos recientes')
    alumno = cliente()
    comprobar(api(alumno, 'auth/login', 'POST', {'username': usuario, 'password': clave})[0] == 200, 'vuelve a ingresar')

    estado, nueva_clave, _ = admin(a, f'cuentas/{nueva["id"]}/clave', 'POST', {})
    temporal = (nueva_clave or {}).get('claveTemporal')
    comprobar(estado == 200 and temporal and nueva_clave['sesionActiva'] is None, 'genera una clave temporal y cierra su sesión')
    comprobar(api(alumno, 'student/session')[0] == 401, 'la sesión abierta con la clave anterior deja de valer')
    comprobar(api(cliente(), 'auth/login', 'POST', {'username': usuario, 'password': clave})[0] == 401, 'la clave anterior ya no sirve')
    comprobar(api(cliente(), 'auth/login', 'POST', {'username': usuario, 'password': temporal})[0] == 200, 'la clave temporal sí')
    comprobar(admin(a, f'cuentas/{nueva["id"]}/clave', 'POST', {'clave': 'abc'})[0] == 400, 'rechaza una clave corta')
    comprobar(admin(a, f'cuentas/{nueva["id"]}/clave', 'POST', {'clave': f'{usuario}-2026'})[0] == 400,
              'rechaza una clave con el nombre de usuario')
    estado, escrita, _ = admin(a, f'cuentas/{nueva["id"]}/clave', 'POST', {'clave': 'quinua-andina-77'})
    comprobar(estado == 200 and 'claveTemporal' not in escrita, 'acepta una clave escrita y no la devuelve')
    comprobar(api(cliente(), 'auth/login', 'POST', {'username': usuario, 'password': 'quinua-andina-77'})[0] == 200,
              'la clave escrita funciona en la web')
    comprobar(admin(a, f'cuentas/{yo["id"]}/clave', 'POST', {})[0] == 409, 'nadie cambia su propio acceso desde la consola')

    admin(a, f'cuentas/{mesa["id"]}/roles', 'PUT', {'roles': ['SOPORTE']})
    s = cliente()
    comprobar(admin(s, 'sesion', 'POST', {'usuario': f'mesa.{sufijo}', 'clave': mesa['claveTemporal']})[0] == 200,
              'mesa de ayuda vuelve a la consola')
    estado, por_mesa, _ = admin(s, f'cuentas/{nueva["id"]}/clave', 'POST', {})
    comprobar(estado == 200 and por_mesa.get('claveTemporal'), 'mesa de ayuda restablece la clave de una estudiante')
    comprobar(admin(s, f'cuentas/{yo["id"]}/clave', 'POST', {})[0] == 403, 'mesa de ayuda no toca cuentas de la consola')
    comprobar(admin(s, f'cuentas/{yo["id"]}/bloquear', 'POST')[0] == 403, 'mesa de ayuda no bloquea a la administración')
    admin(a, f'cuentas/{nueva["id"]}/baja', 'POST')
    comprobar(admin(a, f'cuentas/{nueva["id"]}/clave', 'POST', {})[0] == 409, 'una cuenta dada de baja no recibe clave')
    acciones = [h['accion'] for h in admin(a, f'cuentas/{nueva["id"]}')[1]['historial']]
    comprobar({'CLAVE_RESTABLECIDA', 'CUENTA_BLOQUEADA', 'CUENTA_DESBLOQUEADA'} <= set(acciones), 'todo queda auditado')

    print('HU-45 · salud de los servidores y tiempos de respuesta')
    comprobar(api(cliente(), 'monitor/health')[0] == 401, 'Spring no entrega sus tiempos sin token')
    comprobar(api(cliente(), 'monitor/health', cabeceras={'X-Rupi-Monitor-Token': 'adivinado'})[0] == 401,
              'ni con un token equivocado')
    for _ in range(3):
        api(cliente(), 'auth/options')
    estado, salud, _ = admin(a, 'salud')
    servicios = {srv['clave']: srv for srv in (salud or {}).get('servicios', [])}
    comprobar(estado == 200 and {'base', 'api', 'ia', 'consola'} <= servicios.keys(), 'la consola revisa base, API, IA y consola')
    comprobar(servicios['base']['estado'] in ('OK', 'LENTO') and servicios['base']['latenciaMs'] is not None,
              'mide cuánto tarda MySQL')
    comprobar(servicios['api']['estado'] in ('OK', 'LENTO'), 'la API responde al monitor con el token compartido')
    rutas = [r['route'] for r in servicios['api']['peticiones']['rutasLentas']]
    comprobar('GET /api/v1/auth/options' in rutas and servicios['api']['peticiones']['ventana']['requests'] >= 3,
              'agrupa los tiempos por ruta sin ids')
    comprobar(not any('/monitor' in r for r in rutas), 'el propio monitor no se cuenta')
    comprobar(salud['estado'] in ('OK', 'ATENCION') and (servicios['ia']['estado'] == 'OK') == (salud['estado'] == 'OK'),
              f'estado general coherente (IA: {servicios["ia"]["estado"]})')
    comprobar(admin(s, 'salud')[0] == 403, 'mesa de ayuda no ve la salud de la plataforma')
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
