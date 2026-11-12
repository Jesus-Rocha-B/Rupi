"""Límite de intentos y bloqueo automático configurables (HU-58). Solo entorno local.

Recorre el flujo contra Django (8082), Spring (8081) y MySQL reales con dos cuentas temporales (una estudiante y
una de mesa de ayuda) creadas con el root local: la consola cambia la política, la web bloquea la cuenta tras los
fallos configurados aunque luego llegue la clave correcta, la consola la lista y la desbloquea, el freno por equipo
responde 429 con Retry-After y la consola aplica la misma política a su propio ingreso. En finally restaura la
política y borra las cuentas y sus intentos.

Necesita bcrypt (está en backend/admin-django/.venv):
RUPI_ADMIN_CLAVE_LOCAL=... backend/admin-django/.venv/bin/python -I scripts/verify_hu58_bloqueo_intentos.py \\
    --root-config .local/root.cnf --mysql .local/mysql-8.4.11-linux-glibc2.28-x86_64/bin/mysql
"""
import argparse
import hashlib
import http.cookiejar
import json
import os
import subprocess
import urllib.error
import urllib.request
import uuid
from pathlib import Path

import bcrypt

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
ADMIN = args.admin + '/api/admin/v1'
cli = [str(Path(args.mysql).resolve()), '--defaults-extra-file=' + str(Path(args.root_config).resolve()),
       '--batch', '--skip-column-names', 'rupi']
sufijo = uuid.uuid4().hex[:8]
ALUMNA, SOPORTE, FANTASMA = f'hu58.alumna.{sufijo}', f'hu58.soporte.{sufijo}', f'hu58.noexiste.{sufijo}'
CLAVE = 'quinua-cometa-58'
ids = {ALUMNA: str(uuid.uuid4()), SOPORTE: str(uuid.uuid4())}


def sql(sentencia):
    resultado = subprocess.run(cli, input=sentencia.encode(), capture_output=True)
    if resultado.returncode:
        raise RuntimeError(resultado.stderr.decode(errors='replace'))
    return resultado.stdout.decode().strip()


def pedir(c, url, metodo='GET', cuerpo=None, origen=ORIGEN):
    """Devuelve (estado, cuerpo, cabeceras)."""
    h = {'Content-Type': 'application/json', **({'Origin': origen} if origen else {})}
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    req = urllib.request.Request(url, method=metodo, headers=h, data=datos)
    try:
        with c.open(req, timeout=30) as r:
            crudo = r.read()
            return r.status, (json.loads(crudo) if crudo else None), r.headers
    except urllib.error.HTTPError as error:
        crudo = error.read()
        try:
            return error.code, (json.loads(crudo) if crudo else None), error.headers
        except ValueError:
            return error.code, None, error.headers


def comprobar(condicion, mensaje):
    if not condicion:
        raise AssertionError(mensaje)
    print('  ok ·', mensaje)


def sesion():
    return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))


def web(usuario, clave):
    return pedir(sesion(), args.api + '/api/v1/auth/login', 'POST', {'username': usuario, 'password': clave})


def consola(usuario, clave, c=None):
    return pedir(c or sesion(), ADMIN + '/sesion', 'POST', {'usuario': usuario, 'clave': clave})


def politica(**cambios):
    base = {'ventanaMinutos': 5, 'maxFallosEquipoCuenta': 6, 'maxFallosEquipo': 40, 'bloqueoAutomatico': True,
            'maxFallosCuenta': 5, 'minutosBloqueo': 5}
    base.update(cambios)
    return base


def hash_de(usuario):
    return hashlib.sha256(usuario.lower().encode()).hexdigest()


antes = sql('SELECT ventana_minutos, max_fallos_equipo_cuenta, max_fallos_equipo, bloqueo_automatico, '
            'max_fallos_cuenta, minutos_bloqueo, IFNULL(actualizado_por, \'NULL\'), actualizado_en '
            'FROM identidad_politica_acceso WHERE id=1')
if not antes:
    raise SystemExit('Falta la fila de identidad_politica_acceso: aplica V46 primero.')
auditoria_antes = sql("SELECT IFNULL(MAX(id), 0) FROM operaciones_evento_auditoria")
hash_clave = bcrypt.hashpw(CLAVE.encode(), bcrypt.gensalt(rounds=10)).decode()

try:
    for usuario, rol in ((ALUMNA, 'ESTUDIANTE'), (SOPORTE, 'SOPORTE')):
        sql(f"""INSERT INTO identidad_cuenta_usuario (id, nombre_usuario, hash_clave, estado)
                VALUES ('{ids[usuario]}', '{usuario}', '{hash_clave}', 'ACTIVO');
                INSERT INTO identidad_perfil_usuario (usuario_id, nombre, apellido)
                VALUES ('{ids[usuario]}', 'Prueba', 'HU-58');
                INSERT INTO identidad_usuario_rol (usuario_id, rol_id)
                SELECT '{ids[usuario]}', id FROM identidad_rol WHERE codigo='{rol}';""")

    admin = sesion()
    estado, _, _ = consola(args.usuario, clave_admin, admin)
    comprobar(estado == 200, 'la administración ingresa a la consola')

    print('1. La consola consulta y cambia la política')
    estado, datos, _ = pedir(admin, ADMIN + '/acceso/politica')
    comprobar(estado == 200 and set(datos['politica']) == set(politica()), 'GET devuelve la política completa')
    comprobar(datos['limites']['maxFallosCuenta'] == {'min': 5, 'max': 50}, 'GET informa los rangos permitidos')
    estado, _, _ = pedir(admin, ADMIN + '/acceso/politica', 'PUT', politica(), origen=None)
    comprobar(estado == 403, 'PUT sin Origin se rechaza (403)')
    estado, datos, _ = pedir(admin, ADMIN + '/acceso/politica', 'PUT', politica(maxFallosEquipoCuenta=15,
                                                                                 maxFallosEquipo=12))
    comprobar(estado == 400, 'PUT con el límite del equipo menor que el de la cuenta se rechaza (400)')
    estado, datos, _ = pedir(admin, ADMIN + '/acceso/politica', 'PUT', politica(minutosBloqueo=2))
    comprobar(estado == 400 and 'entre 5 y 1440' in datos['detail'], 'PUT fuera de rango explica el rango')
    estado, datos, _ = pedir(admin, ADMIN + '/acceso/politica', 'PUT', politica())
    comprobar(estado == 200 and datos['politica'] == politica() and datos['actualizadoPor'] == args.usuario,
              'PUT válido guarda la política y quién la cambió')
    comprobar(sql(f"SELECT COUNT(*) FROM operaciones_evento_auditoria WHERE id > {auditoria_antes} "
                  "AND accion='POLITICA_ACCESO_ACTUALIZADA'") == '1', 'el cambio queda en la auditoría')

    print('2. Mesa de ayuda solo consulta')
    soporte = sesion()
    estado, _, _ = consola(SOPORTE, CLAVE, soporte)
    comprobar(estado == 200, 'la cuenta de mesa de ayuda ingresa a la consola')
    estado, _, _ = pedir(soporte, ADMIN + '/acceso/politica')
    comprobar(estado == 200, 'mesa de ayuda puede ver la política')
    estado, _, _ = pedir(soporte, ADMIN + '/acceso/politica', 'PUT', politica())
    comprobar(estado == 403, 'mesa de ayuda no puede cambiarla (403)')

    print('3. La web bloquea la cuenta tras 5 fallos (el freno por equipo está en 6)')
    for _ in range(5):
        estado, _, _ = web(ALUMNA, 'clave-equivocada')
    comprobar(estado == 401, 'los 5 fallos responden 401')
    estado, datos, cabeceras = web(ALUMNA, CLAVE)
    comprobar(estado == 423, 'con la cuenta bloqueada ni la clave correcta entra (423)')
    comprobar('5 minutos' in datos['detail'] and 'desbloqueo' in datos['detail'], 'el mensaje dice cuánto esperar')
    comprobar(cabeceras.get('Retry-After') == '300', 'Retry-After indica 300 segundos')
    comprobar(sql(f"SELECT estado FROM identidad_cuenta_usuario WHERE id='{ids[ALUMNA]}'") == 'ACTIVO',
              'Spring no escribió la cuenta: el bloqueo se calcula con los intentos')
    for _ in range(5):
        web(FANTASMA, 'clave-equivocada')
    estado, _, _ = web(FANTASMA, CLAVE)
    comprobar(estado == 423, 'un usuario que no existe recibe la misma respuesta (no se revela qué cuentas existen)')

    print('4. La consola ve el bloqueo y lo levanta')
    estado, datos, _ = pedir(admin, ADMIN + '/acceso/politica')
    bloqueadas = {c['usuario']: c for c in datos['cuentasBloqueadas']}
    comprobar(ALUMNA in bloqueadas and bloqueadas[ALUMNA]['roles'] == ['ESTUDIANTE'],
              'la alumna aparece en «Cuentas bloqueadas ahora»')
    comprobar(datos['ultimas24h']['fallidos'] >= 10 and datos['ultimas24h']['frenados'] >= 2,
              'el resumen de 24 h cuenta fallos y frenados')
    estado, datos, _ = pedir(admin, ADMIN + f'/cuentas/{ids[ALUMNA]}')
    comprobar(datos['bloqueada'] and datos['bloqueoAutomatico'] and datos['bloqueadoHasta'],
              'el detalle de la cuenta la muestra bloqueada automáticamente y hasta cuándo')
    estado, datos, _ = pedir(admin, ADMIN + f'/cuentas?estado=BLOQUEADO&q={ALUMNA}')
    comprobar(any(c['id'] == ids[ALUMNA] for c in datos['items']), 'el filtro «Bloqueadas» la incluye')
    estado, datos, _ = pedir(admin, ADMIN + f'/cuentas/{ids[ALUMNA]}/desbloquear', 'POST')
    comprobar(estado == 200 and not datos['bloqueada'] and datos['estado'] == 'ACTIVO', 'desbloquear la libera')
    comprobar(sql(f"SELECT intentos_reiniciados_en IS NOT NULL FROM identidad_cuenta_usuario "
                  f"WHERE id='{ids[ALUMNA]}'") == '1', 'desbloquear guarda el reinicio de intentos')
    estado, datos, _ = web(ALUMNA, CLAVE)
    comprobar(estado == 200 and datos['role'] == 'ESTUDIANTE', 'tras desbloquear, la alumna entra con su clave')

    print('5. Bloqueo apagado y freno por equipo')
    estado, _, _ = pedir(admin, ADMIN + '/acceso/politica', 'PUT', politica(bloqueoAutomatico=False))
    comprobar(estado == 200, 'se apaga el bloqueo automático')
    for _ in range(5):
        web(ALUMNA, 'clave-equivocada')
    estado, _, _ = web(ALUMNA, CLAVE)
    comprobar(estado == 200, 'sin bloqueo automático, 5 fallos no bloquean la cuenta')
    web(ALUMNA, 'clave-equivocada')
    estado, datos, cabeceras = web(ALUMNA, CLAVE)
    comprobar(estado == 429 and '5 minutos' in datos['detail'] and cabeceras.get('Retry-After') == '300',
              'al 6.º fallo en el equipo responde 429 con el tiempo de espera')

    print('6. La consola aplica la misma política a su ingreso')
    estado, _, _ = pedir(admin, ADMIN + '/acceso/politica', 'PUT', politica())
    for _ in range(5):
        estado, _, _ = consola(SOPORTE, 'clave-equivocada')
    comprobar(estado == 401, 'los fallos en la consola responden 401')
    estado, datos, cabeceras = consola(SOPORTE, CLAVE)
    comprobar(estado == 423 and 'administración' in datos['detail'] and cabeceras.get('Retry-After') == '300',
              'la cuenta de consola queda bloqueada (423) con Retry-After')
    print('\nHU-58 verificada.')
finally:
    lista = ', '.join(f"'{v}'" for v in ids.values())
    hashes = ', '.join(f"UNHEX('{hash_de(u)}')" for u in (ALUMNA, SOPORTE, FANTASMA))
    v, ec, e, b, c, m, por, en = antes.split('\t')
    por = 'NULL' if por == 'NULL' else f"'{por}'"
    sql(f"""UPDATE identidad_politica_acceso SET ventana_minutos={v}, max_fallos_equipo_cuenta={ec},
              max_fallos_equipo={e}, bloqueo_automatico={b}, max_fallos_cuenta={c}, minutos_bloqueo={m},
              actualizado_por={por}, actualizado_en='{en}' WHERE id=1;
            DELETE FROM operaciones_evento_auditoria WHERE id > {auditoria_antes}
              AND (accion='POLITICA_ACCESO_ACTUALIZADA' OR entidad_id IN ({lista}) OR actor_usuario_id IN ({lista}));
            DELETE FROM identidad_intento_acceso WHERE identificador_hash IN ({hashes}) OR usuario_id IN ({lista});
            DELETE FROM identidad_sesion_usuario WHERE usuario_id IN ({lista});
            DELETE FROM identidad_usuario_rol WHERE usuario_id IN ({lista});
            DELETE FROM identidad_perfil_usuario WHERE usuario_id IN ({lista});
            DELETE FROM identidad_cuenta_usuario WHERE id IN ({lista});""")
    print('Base restaurada: política anterior y sin cuentas de prueba.')
