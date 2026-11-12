"""Verificación end-to-end de HU-60 (autenticación de doble factor obligatoria).

Prueba contra los servicios locales vivos (Django 8082, Spring 8081) y MySQL:
1. Crea tres cuentas temporales aisladas: admin (ADMINISTRADOR), docente (DOCENTE) y
   alumna (ESTUDIANTE), para no alterar cuentas reales como admin.rupi.
2. Verifica en Django (`POST /api/admin/v1/sesion`):
   - Admin sin 2FA recibe `202` con `requiere2fa=True`, `configurado=False` y `otpauthUri`.
   - `POST /api/admin/v1/sesion/2fa` rechaza un código incorrecto (`401`).
   - `POST /api/admin/v1/sesion/2fa` con token TOTP válido activa `identidad_doble_factor`,
     abre la sesión `WEB_ADMIN` (`200`) y permite consultar `GET /api/admin/v1/seguridad/2fa`.
   - Un segundo ingreso con el mismo token queda rechazado (antirrepetición).
3. Verifica en Spring (`POST /api/v1/auth/login`):
   - ESTUDIANTE entra sin 2FA (`200`).
   - DOCENTE sin 2FA recibe `428` (`TOTP_SETUP_REQUIRED`).
   - DOCENTE completa enrolamiento QR en Django (`/api/admin/v1/2fa/docente/iniciar` y `confirmar`).
   - DOCENTE sin `totpCode` recibe `428` (`TOTP_REQUIRED`); con código incorrecto recibe `401` (`TOTP_INVALID`);
     con TOTP válido entra correctamente (`200`).
4. Verifica reinicio de 2FA desde consola (`POST /api/admin/v1/seguridad/2fa/cuentas/<id>/reiniciar`).
5. Limpia en `finally` todas las cuentas temporales y sus filas relacionadas.
"""
import argparse
import http.cookiejar
import json
import os
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DJANGO_DIR = ROOT / 'backend' / 'admin-django'
sys.path.insert(0, str(DJANGO_DIR))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'rupi_admin.settings')

import django  # noqa: E402

django.setup()

from django.db import connection  # noqa: E402
from django.utils import timezone  # noqa: E402

from identidad import claves, doble_factor  # noqa: E402
from identidad.models import CuentaUsuario, PerfilUsuario, Rol, UsuarioRol  # noqa: E402


def comprobar(condicion: bool, mensaje: str) -> None:
    if not condicion:
        raise AssertionError(mensaje)
    print(f'  ok · {mensaje}')


def pedir(opener, url: str, metodo: str = 'GET', cuerpo: dict | None = None, origen: str | None = 'http://127.0.0.1:5173'):
    headers = {'Content-Type': 'application/json'}
    if origen:
        headers['Origin'] = origen
    datos = json.dumps(cuerpo).encode('utf-8') if cuerpo is not None else None
    req = urllib.request.Request(url, method=metodo, headers=headers, data=datos)
    try:
        with opener.open(req, timeout=20) as resp:
            crudo = resp.read()
            return resp.status, (json.loads(crudo) if crudo else None)
    except urllib.error.HTTPError as err:
        crudo = err.read()
        try:
            return err.code, (json.loads(crudo) if crudo else None)
        except ValueError:
            return err.code, None


def crear_cuenta(codigo_rol: str, nombre_usuario: str, clave_plana: str) -> CuentaUsuario:
    uid = str(uuid.uuid4())
    ahora = timezone.now()
    cuenta = CuentaUsuario.objects.create(
        id=uid,
        nombre_usuario=nombre_usuario,
        correo=f'{nombre_usuario}@rupi.edu.pe',
        hash_clave=claves.cifrar(clave_plana),
        estado='ACTIVO',
        clave_cambiada_en=ahora,
        creado_en=ahora,
        actualizado_en=ahora,
    )
    PerfilUsuario.objects.create(usuario=cuenta, nombre='Prueba HU60', apellido=codigo_rol)
    rol = Rol.objects.get(codigo=codigo_rol)
    UsuarioRol.objects.create(usuario_id=uid, rol_id=rol.id, asignado_en=ahora)
    return cuenta


def limpiar_cuentas(ids: list[str]) -> None:
    if not ids:
        return
    marcadores = ','.join(['%s'] * len(ids))
    with connection.cursor() as cursor:
        cursor.execute(f'DELETE FROM identidad_doble_factor WHERE usuario_id IN ({marcadores})', ids)
        cursor.execute(f'DELETE FROM identidad_intento_acceso WHERE usuario_id IN ({marcadores})', ids)
        cursor.execute(f'DELETE FROM identidad_sesion_usuario WHERE usuario_id IN ({marcadores})', ids)
        cursor.execute(f'DELETE FROM identidad_usuario_rol WHERE usuario_id IN ({marcadores})', ids)
        cursor.execute(f'DELETE FROM operaciones_evento_auditoria WHERE actor_usuario_id IN ({marcadores})', ids)
        cursor.execute(f'DELETE FROM identidad_perfil_usuario WHERE usuario_id IN ({marcadores})', ids)
        cursor.execute(f'DELETE FROM identidad_cuenta_usuario WHERE id IN ({marcadores})', ids)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--django', default='http://127.0.0.1:8082')
    parser.add_argument('--spring', default='http://127.0.0.1:8081')
    args = parser.parse_args()

    sufijo = uuid.uuid4().hex[:8]
    u_admin = f'hu60.admin.{sufijo}'
    u_docente = f'hu60.docente.{sufijo}'
    u_alumna = f'hu60.alumna.{sufijo}'
    clave = f'Quinua-{sufijo}-60!'

    admin_url = args.django.rstrip('/') + '/api/admin/v1'
    spring_login = args.spring.rstrip('/') + '/api/v1/auth/login'

    creadas: list[str] = []
    try:
        c_admin = crear_cuenta('ADMINISTRADOR', u_admin, clave)
        c_docente = crear_cuenta('DOCENTE', u_docente, clave)
        c_alumna = crear_cuenta('ESTUDIANTE', u_alumna, clave)
        creadas = [c_admin.id, c_docente.id, c_alumna.id]
        comprobar(True, f'cuentas temporales creadas ({sufijo})')

        cj_admin = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        cj_docente = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        cj_alumna = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

        # 1) Consola Django: exige QR inicial
        st, body = pedir(cj_admin, f'{admin_url}/sesion', 'POST', {'usuario': u_admin, 'clave': clave})
        comprobar(st == 202 and body and body.get('requiere2fa') and not body.get('configurado'),
                  'Django exige configuración QR inicial (202)')
        secreto_admin = body['secretoBase32']
        desafio_admin = body['desafio']
        comprobar(body['otpauthUri'].startswith('otpauth://totp/'), 'Django entrega URI otpauth válido')

        # Código incorrecto
        st_bad, _ = pedir(cj_admin, f'{admin_url}/sesion/2fa', 'POST', {'desafio': desafio_admin, 'codigo': '000000'})
        comprobar(st_bad == 401, 'Django rechaza código TOTP incorrecto (401)')

        # Código válido
        tok_admin = doble_factor.codigo_totp(secreto_admin)
        st_ok, body_ok = pedir(cj_admin, f'{admin_url}/sesion/2fa', 'POST', {'desafio': desafio_admin, 'codigo': tok_admin})
        comprobar(st_ok == 200 and body_ok and body_ok.get('usuario') == u_admin,
                  'Django activa 2FA y abre sesión WEB_ADMIN (200)')

        # Vista de gestión 2FA en consola
        st_ov, body_ov = pedir(cj_admin, f'{admin_url}/seguridad/2fa', 'GET')
        comprobar(st_ov == 200 and body_ov and body_ov['propio']['verificado'] is True,
                  'GET seguridad/2fa confirma estado verificado del admin')

        # Antirrepetición del mismo código
        st_rep, body_rep = pedir(cj_admin, f'{admin_url}/sesion', 'POST', {'usuario': u_admin, 'clave': clave, 'codigo': tok_admin})
        comprobar(st_rep == 401 and body_rep is not None,
                  'Django bloquea reutilización inmediata del mismo token TOTP')

        # 2) Spring Boot: ESTUDIANTE entra sin 2FA
        st_alu, _ = pedir(cj_alumna, spring_login, 'POST', {'username': u_alumna, 'password': clave})
        comprobar(st_alu == 200, 'ESTUDIANTE inicia sesión en Spring sin 2FA (200)')

        # 3) Spring Boot: DOCENTE sin 2FA recibe TOTP_SETUP_REQUIRED (428)
        st_doc0, body_doc0 = pedir(cj_docente, spring_login, 'POST', {'username': u_docente, 'password': clave})
        comprobar(st_doc0 == 428 and body_doc0 and body_doc0.get('code') == 'TOTP_SETUP_REQUIRED',
                  'Spring exige enrolamiento QR previo al docente sin 2FA (428 TOTP_SETUP_REQUIRED)')

        # Enrolamiento QR de DOCENTE en Django
        st_en1, body_en1 = pedir(cj_docente, f'{admin_url}/2fa/docente/iniciar', 'POST', {'usuario': u_docente, 'clave': clave})
        comprobar(st_en1 == 200 and body_en1 and not body_en1.get('yaConfigurado'),
                  'Django entrega QR inicial para docente (200)')
        secreto_doc = body_en1['secretoBase32']
        tok_doc = doble_factor.codigo_totp(secreto_doc)
        st_en2, body_en2 = pedir(cj_docente, f'{admin_url}/2fa/docente/confirmar', 'POST', {
            'desafio': body_en1['desafio'],
            'codigo': tok_doc,
        })
        comprobar(st_en2 == 200 and body_en2 and body_en2.get('verificado') is True,
                  'Django confirma primer token del docente y activa 2FA (200)')

        # DOCENTE ya configurado en Spring sin código -> 428 TOTP_REQUIRED
        st_doc1, body_doc1 = pedir(cj_docente, spring_login, 'POST', {'username': u_docente, 'password': clave})
        comprobar(st_doc1 == 428 and body_doc1 and body_doc1.get('code') == 'TOTP_REQUIRED',
                  'Spring solicita token de 6 dígitos al docente ya enrolado (428 TOTP_REQUIRED)')

        # DOCENTE con código incorrecto -> 401 TOTP_INVALID
        st_doc_bad, body_doc_bad = pedir(cj_docente, spring_login, 'POST', {
            'username': u_docente,
            'password': clave,
            'totpCode': '999999',
        })
        comprobar(st_doc_bad == 401 and body_doc_bad and body_doc_bad.get('code') == 'TOTP_INVALID',
                  'Spring rechaza token inválido de docente (401 TOTP_INVALID)')

        # DOCENTE con código válido en otro paso de 30s (usamos +30s por ventana ±1 permitido)
        paso_siguiente = doble_factor.paso_actual() + 1
        tok_doc_sig = doble_factor.codigo_totp(secreto_doc, paso=paso_siguiente)
        st_doc_ok, _ = pedir(cj_docente, spring_login, 'POST', {
            'username': u_docente,
            'password': clave,
            'totpCode': tok_doc_sig,
        })
        comprobar(st_doc_ok == 200, 'Spring autentica al docente con token TOTP válido (200)')

        # 4) Reinicio 2FA del docente desde consola admin
        st_rst, body_rst = pedir(cj_admin, f'{admin_url}/seguridad/2fa/cuentas/{c_docente.id}/reiniciar', 'POST', {})
        comprobar(st_rst == 200 and body_rst and body_rst.get('pendienteQr') is True and not body_rst.get('verificado'),
                  'Admin reinicia el 2FA del docente desde seguridad/2fa')

        print('HU-60 verificada de extremo a extremo (Django + Spring + MySQL).')
        return 0
    finally:
        limpiar_cuentas(creadas)


if __name__ == '__main__':
    raise SystemExit(main())
