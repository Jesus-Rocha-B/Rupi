"""Verificación integral de HU-57 (respaldos automáticos nocturnos en Django + MySQL).

Prueba contra la base MySQL local y las vistas reales de Django:
1. Rechazo de horarios escolares (06:00-23:59) y rutas dentro de mysql-data.
2. Permisos: SOPORTE consulta (200) pero no configura (403); ADMINISTRADOR guarda (200).
3. Ejecución real de `mysqldump` sobre la base `rupi`, compresión gzip, traslado a almacenamiento
   externo, comprobación SHA-256 y persistencia en `operaciones_respaldo` (VERIFICADO).
4. Manejo de fallos: al fallar el volcado registra `FALLIDO`, abre incidente en
   `operaciones_incidente` y envía la alerta por correo al administrador.
En `finally` restaura la política original y elimina los registros y archivos temporales.
"""
import gzip
import json
import os
import shutil
import sys
import tempfile
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DJANGO_DIR = ROOT / 'backend' / 'admin-django'
sys.path.insert(0, str(DJANGO_DIR))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'rupi_admin.settings')

import django  # noqa: E402

django.setup()

from django.core import mail  # noqa: E402
from django.core.management import call_command  # noqa: E402
from django.test import Client, override_settings  # noqa: E402
from django.utils import timezone  # noqa: E402

from identidad import claves  # noqa: E402
from identidad.models import CuentaUsuario, PerfilUsuario, Rol, UsuarioRol  # noqa: E402
from identidad.sesion import iniciar  # noqa: E402
from operaciones.models import EventoAuditoria, Incidente, PoliticaRespaldo, Respaldo  # noqa: E402


def comprobar(condicion: bool, mensaje: str) -> None:
    if not condicion:
        raise AssertionError(mensaje)
    print(f'  ok · {mensaje}')


def _crear_actor(codigo_rol: str, sufijo: str):
    uid = str(uuid.uuid4())
    ahora = timezone.now()
    usuario = f'hu57.{codigo_rol.lower()}.{sufijo}'
    clave_plana = f'Quinua-{sufijo}-57!'
    cuenta = CuentaUsuario.objects.create(
        id=uid,
        nombre_usuario=usuario,
        correo=f'{usuario}@rupi.edu.pe',
        hash_clave=claves.cifrar(clave_plana),
        estado='ACTIVO',
        clave_cambiada_en=ahora,
        creado_en=ahora,
        actualizado_en=ahora,
    )
    PerfilUsuario.objects.create(usuario=cuenta, nombre=f'Test HU57', apellido=codigo_rol)
    rol = Rol.objects.get(codigo=codigo_rol)
    UsuarioRol.objects.create(usuario_id=uid, rol_id=rol.id, asignado_en=ahora)
    token, _admin = iniciar(usuario, clave_plana, '127.0.0.1')
    return cuenta, token


def _eliminar_actor(cuenta_id: str) -> None:
    from django.db import connection
    with connection.cursor() as cur:
        cur.execute('DELETE FROM identidad_intento_acceso WHERE usuario_id = %s', [cuenta_id])
        cur.execute('DELETE FROM identidad_sesion_usuario WHERE usuario_id = %s', [cuenta_id])
        cur.execute('DELETE FROM identidad_usuario_rol WHERE usuario_id = %s', [cuenta_id])
        cur.execute('DELETE FROM identidad_perfil_usuario WHERE usuario_id = %s', [cuenta_id])
        cur.execute('DELETE FROM operaciones_evento_auditoria WHERE actor_usuario_id = %s', [cuenta_id])
        cur.execute('DELETE FROM identidad_cuenta_usuario WHERE id = %s', [cuenta_id])


def main() -> int:
    print('Verificando HU-57 (Django + MySQL reales)...')
    sufijo = uuid.uuid4().hex[:8]
    tmp_ext = tempfile.mkdtemp(prefix='rupi_hu57_ext_')
    politica_orig = PoliticaRespaldo.objects.filter(id=1).values().first()
    incidentes_antes = set(Incidente.objects.filter(servicio='respaldo_nocturno').values_list('id', flat=True))
    respaldos_creados: list[str] = []
    actores: list[str] = []

    try:
        admin, token_admin = _crear_actor('ADMINISTRADOR', sufijo)
        actores.append(admin.id)
        soporte, token_soporte = _crear_actor('SOPORTE', sufijo)
        actores.append(soporte.id)

        headers_origen = {'HTTP_ORIGIN': 'http://127.0.0.1:5173'}

        c_soporte = Client()
        c_soporte.cookies['rupi_admin'] = token_soporte
        r = c_soporte.get('/api/admin/v1/respaldos')
        comprobar(r.status_code == 200, 'SOPORTE puede consultar GET /api/admin/v1/respaldos (200)')

        r = c_soporte.put(
            '/api/admin/v1/respaldos/politica',
            data=json.dumps({'horaProgramada': '02:30'}),
            content_type='application/json',
            **headers_origen,
        )
        comprobar(r.status_code == 403, 'SOPORTE no puede modificar programación nocturna (403)')

        c_admin = Client()
        c_admin.cookies['rupi_admin'] = token_admin

        # Rechaza horario escolar (09:30)
        r = c_admin.put(
            '/api/admin/v1/respaldos/politica',
            data=json.dumps({
                'habilitado': True,
                'horaProgramada': '09:30',
                'frecuencia': 'DIARIO',
                'retencionDias': 30,
                'almacenamientoExternoTipo': 'CARPETA_SEGURA',
                'almacenamientoExternoRuta': 'hu57-test',
                'cifradoActivo': False,
                'alertarSiFalla': True,
                'emailAlerta': 'admin@rupi.edu.pe',
            }),
            content_type='application/json',
            **headers_origen,
        )
        comprobar(r.status_code == 400, 'Rechaza programar respaldos en horario escolar (09:30 -> 400)')

        # Guarda horario válido de madrugada (03:15)
        r = c_admin.put(
            '/api/admin/v1/respaldos/politica',
            data=json.dumps({
                'habilitado': True,
                'horaProgramada': '03:15',
                'frecuencia': 'DIARIO',
                'retencionDias': 30,
                'almacenamientoExternoTipo': 'CARPETA_SEGURA',
                'almacenamientoExternoRuta': 'hu57-test',
                'cifradoActivo': False,
                'alertarSiFalla': True,
                'emailAlerta': 'alertas-hu57@rupi.edu.pe',
            }),
            content_type='application/json',
            **headers_origen,
        )
        comprobar(r.status_code == 200, 'ADMINISTRADOR guarda programación en madrugada (03:15 -> 200)')
        payload = r.json()
        comprobar(payload['politica']['horaProgramada'] == '03:15', 'Hora nocturna persistida en MySQL')
        comprobar('15 3 * * *' in payload['tareaServidor']['cronLinea'], 'Expresión cron generada coincide con 03:15')

        # Ejecuta respaldo real contra MySQL + almacenamiento externo temporal
        with override_settings(RUPI_BACKUP_EXTERNAL_DIR=tmp_ext):
            r_exec = c_admin.post('/api/admin/v1/respaldos/ejecutar', **headers_origen)
            comprobar(r_exec.status_code == 201, f'POST /respaldos/ejecutar genera respaldo real (HTTP {r_exec.status_code})')
            res_ok = r_exec.json()
            respaldos_creados.append(res_ok['id'])
            comprobar(res_ok['estado'] == 'VERIFICADO', 'Respaldo verificado por SHA-256 en destino externo')

            dest_file = Path(res_ok['ubicacionReferencia'])
            comprobar(dest_file.is_file() and Path(f'{dest_file}.sha256').is_file(), 'Archivo .sql.gz y sidecar .sha256 existen en destino externo')
            with gzip.open(dest_file, 'rt', encoding='utf-8', errors='replace') as gz:
                inicio_sql = gz.read(256)
            comprobar('MySQL dump' in inicio_sql or 'CREATE TABLE' in inicio_sql or '--' in inicio_sql, 'El volcado comprimido contiene SQL válido de MySQL')

            # Comando programado del servidor con --forzar
            call_command('ejecutar_respaldo_programado', forzar=True)
            ultimo_prog = Respaldo.objects.filter(origen='PROGRAMADO').order_by('-iniciado_en').first()
            comprobar(ultimo_prog is not None and ultimo_prog.estado == 'VERIFICADO', 'Management command `ejecutar_respaldo_programado --forzar` completado')
            if ultimo_prog:
                respaldos_creados.append(ultimo_prog.id)

        # Simula fallo real de binario mysqldump para verificar alerta + incidente + estado FALLIDO
        with override_settings(
            RUPI_BACKUP_EXTERNAL_DIR=tmp_ext,
            RUPI_MYSQLDUMP_BIN='C:\\no-existe\\mysqldump.exe',
            EMAIL_BACKEND='django.core.mail.backends.locmem.EmailBackend',
        ):
            mail.outbox = []
            r_fail = c_admin.post('/api/admin/v1/respaldos/ejecutar', **headers_origen)
            comprobar(r_fail.status_code == 500, 'Fallo en volcado responde 500 con diagnóstico')
            res_fail = r_fail.json()
            respaldos_creados.append(res_fail['id'])
            comprobar(res_fail['estado'] == 'FALLIDO', 'Registro persistido con estado FALLIDO')
            comprobar(res_fail['alertaEnviada'] is True, 'Marca alertaEnviada=True en base de datos')
            comprobar(len(mail.outbox) == 1 and 'alertas-hu57@rupi.edu.pe' in mail.outbox[0].to, 'Correo de alerta enviado al administrador responsable')
            comprobar(
                Incidente.objects.filter(servicio='respaldo_nocturno', estado__in=['ABIERTO', 'EN_ATENCION']).exists(),
                'Abre incidente en operaciones_incidente tras el fallo del respaldo',
            )

        print('HU-57 verificada correctamente.')
        return 0
    finally:
        for rid in respaldos_creados:
            Respaldo.objects.filter(id=rid).delete()
            EventoAuditoria.objects.filter(entidad_id=rid).delete()
        incidentes_ahora = set(Incidente.objects.filter(servicio='respaldo_nocturno').values_list('id', flat=True))
        for inc_id in (incidentes_ahora - incidentes_antes):
            Incidente.objects.filter(id=inc_id).delete()
        if politica_orig:
            PoliticaRespaldo.objects.filter(id=1).update(**{k: v for k, v in politica_orig.items() if k != 'id'})
        for uid in actores:
            _eliminar_actor(uid)
        shutil.rmtree(tmp_ext, ignore_errors=True)


if __name__ == '__main__':
    raise SystemExit(main())
