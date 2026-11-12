"""HU-57: pruebas unitarias de programación nocturna, almacenamiento externo y alertas de respaldo."""
from datetime import datetime, timezone as tz
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import MagicMock, patch
import tempfile

from django.core.management import call_command
from django.test import RequestFactory, SimpleTestCase

from identidad.respuestas import ErrorDePeticion

from . import notificaciones_alertas, respaldos, vistas


class PoliticaRespaldoValidacionTest(SimpleTestCase):
    def _datos(self, **cambios):
        base = {
            'habilitado': True,
            'horaProgramada': '02:30',
            'frecuencia': 'DIARIO',
            'diaSemana': None,
            'retencionDias': 30,
            'almacenamientoExternoTipo': 'CARPETA_SEGURA',
            'almacenamientoExternoRuta': 'backups-externos/rupi',
            'cifradoActivo': True,
            'alertarSiFalla': True,
            'emailAlerta': 'admin@rupi.edu.pe',
        }
        base.update(cambios)
        return base

    def test_acepta_horario_de_madrugada_y_valores_validos(self):
        limpio = respaldos.validar_datos_politica(self._datos(horaProgramada='03:15'))
        self.assertEqual(limpio['hora_programada'], '03:15')
        self.assertEqual(limpio['almacenamiento_externo_tipo'], 'CARPETA_SEGURA')

    def test_rechaza_horarios_escolares_fuera_de_madrugada(self):
        for hora_invalida in ('06:00', '08:30', '14:00', '22:45', '25:00', '2:30'):
            with self.subTest(hora=hora_invalida):
                with self.assertRaises(ErrorDePeticion):
                    respaldos.validar_datos_politica(self._datos(horaProgramada=hora_invalida))

    def test_rechaza_ruta_dentro_de_mysql_data_o_traversal(self):
        for ruta in ('../fuera', 'C:\\Rupi\\.local\\mysql-data\\backups', '/var/lib/mysql/respaldo', ''):
            with self.subTest(ruta=ruta):
                with self.assertRaises(ErrorDePeticion):
                    respaldos.validar_datos_politica(self._datos(almacenamientoExternoRuta=ruta))

    def test_valida_esquema_s3_y_sftp_y_frecuencia_semanal(self):
        with self.assertRaises(ErrorDePeticion):
            respaldos.validar_datos_politica(
                self._datos(almacenamientoExternoTipo='S3_COMPATIBLE', almacenamientoExternoRuta='carpeta-local')
            )
        semanal = respaldos.validar_datos_politica(
            self._datos(
                frecuencia='SEMANAL',
                diaSemana=6,
                almacenamientoExternoTipo='S3_COMPATIBLE',
                almacenamientoExternoRuta='s3://rupi-backups-escuela/noche',
            )
        )
        self.assertEqual(semanal['dia_semana'], 6)


class VentanaYCalculoNocturnoTest(SimpleTestCase):
    def test_calcular_proximo_respaldo_diario_en_lima_utc(self):
        # 12:00 UTC = 07:00 America/Lima -> siguiente 02:30 Lima es mañana (07:30 UTC)
        politica = SimpleNamespace(habilitado=True, hora_programada='02:30', frecuencia='DIARIO', dia_semana=None)
        ahora = datetime(2026, 11, 12, 12, 0, tzinfo=tz.utc)
        proximo = respaldos.calcular_proximo_respaldo(politica, desde_utc=ahora)
        self.assertEqual(proximo, datetime(2026, 11, 13, 7, 30, tzinfo=tz.utc))

    def test_debe_ejecutarse_solo_en_madrugada_y_una_vez_al_dia(self):
        politica = SimpleNamespace(
            habilitado=True,
            hora_programada='02:30',
            frecuencia='DIARIO',
            dia_semana=None,
            ultima_ejecucion_en=None,
        )
        # 07:35 UTC = 02:35 America/Lima -> debe ejecutar
        ok, _ = respaldos.debe_ejecutarse_en_ciclo(politica, ahora_utc=datetime(2026, 11, 12, 7, 35, tzinfo=tz.utc))
        self.assertTrue(ok)

        # 15:00 UTC = 10:00 America/Lima (horario escolar) -> bloqueado
        ok_dia, motivo = respaldos.debe_ejecutarse_en_ciclo(politica, ahora_utc=datetime(2026, 11, 12, 15, 0, tzinfo=tz.utc))
        self.assertFalse(ok_dia)
        self.assertIn('madrugada', motivo.lower())

        # Si ya ejecutó hoy en Lima -> no duplica
        politica.ultima_ejecucion_en = datetime(2026, 11, 12, 7, 31, tzinfo=tz.utc)
        ok_dup, _ = respaldos.debe_ejecutarse_en_ciclo(politica, ahora_utc=datetime(2026, 11, 12, 7, 45, tzinfo=tz.utc))
        self.assertFalse(ok_dup)


class AlmacenamientoExternoYAlertasTest(SimpleTestCase):
    def test_carpeta_externa_guarda_verifica_sha256_y_limpia_vencidos(self):
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            origen = tmp_path / 'rupi_respaldo_20261112_023000_abcd1234.sql.gz'
            origen.write_bytes(b'contenido-sql-comprimido')
            checksum = respaldos._sha256_archivo(origen)
            politica = SimpleNamespace(
                almacenamiento_externo_tipo='CARPETA_SEGURA',
                almacenamiento_externo_ruta='subcarpeta',
                retencion_dias=30,
            )
            with self.settings(RUPI_BACKUP_EXTERNAL_DIR=str(tmp_path / 'externo')):
                ubicacion, verificado = respaldos.enviar_a_almacenamiento_externo(origen, checksum, politica)

            self.assertTrue(verificado)
            destino = Path(ubicacion)
            self.assertTrue(destino.is_file())
            self.assertTrue(Path(f'{destino}.sha256').is_file())
            self.assertEqual(destino.read_bytes(), b'contenido-sql-comprimido')

    def test_fallo_de_respaldo_dispara_alerta_e_incidente(self):
        politica = SimpleNamespace(
            habilitado=True,
            hora_programada='02:30',
            frecuencia='DIARIO',
            dia_semana=None,
            retencion_dias=30,
            almacenamiento_externo_tipo='CARPETA_SEGURA',
            almacenamiento_externo_ruta='backups-externos/rupi',
            cifrado_activo=False,
            alertar_si_falla=True,
            email_alerta='responsable@rupi.edu.pe',
            ultima_ejecucion_en=None,
            proximo_respaldo_en=None,
            actualizado_en=datetime.now(tz.utc),
        )
        registro_mock = MagicMock()
        registro_mock.id = 'uuid-respaldo-test'
        registro_mock.tipo = 'COMPLETO'
        registro_mock.origen = 'PROGRAMADO'
        registro_mock.iniciado_en = datetime.now(tz.utc)
        registro_mock.checksum_sha256 = None
        registro_mock.tamano_bytes = None
        registro_mock.version_esquema = None
        registro_mock.ubicacion_referencia = None
        registro_mock.almacenamiento_externo = 'CARPETA_SEGURA'

        def dump_roto(_path):
            raise RuntimeError('Disco externo desconectado durante el respaldo nocturno')

        with patch('operaciones.respaldos.politica_vigente', return_value=politica), \
             patch('operaciones.respaldos.Respaldo.objects.create', return_value=registro_mock), \
             patch('operaciones.respaldos.enviar_alerta_fallo_respaldo', return_value={'emailEnviado': True, 'smsEnviado': False}) as mock_alerta, \
             patch('operaciones.respaldos._registrar_incidente_fallo') as mock_incidente, \
             patch('operaciones.respaldos.auditoria.registrar') as mock_auditoria:
            res = respaldos.ejecutar_respaldo(origen='PROGRAMADO', generador_dump=dump_roto)

        self.assertEqual(res['estado'], 'FALLIDO')
        self.assertTrue(res['alertaEnviada'])
        self.assertIn('Disco externo desconectado', res['detalleError'])
        mock_alerta.assert_called_once()
        mock_incidente.assert_called_once()
        mock_auditoria.assert_called_once()

    def test_alerta_fallo_respaldo_usa_despachador_estrategia(self):
        canal_fake = MagicMock(spec=notificaciones_alertas.CanalCorreoAlerta)
        canal_fake.despachar.return_value = notificaciones_alertas.ResultadoCanal(
            exito=True, destinatario='admin@rupi.edu.pe'
        )
        desp = notificaciones_alertas.DespachadorAlertas(canales=[canal_fake])
        out = notificaciones_alertas.enviar_alerta_fallo_respaldo(
            respaldo_id='r-1',
            origen='PROGRAMADO',
            almacenamiento='CARPETA_SEGURA: backups-externos/rupi',
            detalle_error='Error de red S3',
            email_destino='admin@rupi.edu.pe',
            despachador=desp,
        )
        self.assertTrue(out['emailEnviado'])
        self.assertEqual(out['destinatarioEmail'], 'admin@rupi.edu.pe')

    def test_vista_respaldos_exige_permiso(self):
        rf = RequestFactory()
        req = rf.get('/api/admin/v1/respaldos')
        req.administrador = SimpleNamespace(
            id='u-1',
            nombre='Soporte',
            puede=lambda permiso: permiso in {'plataforma.monitorear'},
        )
        resp = vistas.estado_respaldos(req)
        self.assertEqual(resp.status_code, 403)

        req_put = rf.put(
            '/api/admin/v1/respaldos/politica',
            data='{}',
            content_type='application/json',
            HTTP_ORIGIN='http://localhost:5173',
        )
        req_put.administrador = SimpleNamespace(
            id='u-1',
            nombre='Soporte',
            puede=lambda permiso: permiso in {'respaldos.ver'},
        )
        resp_put = vistas.actualizar_politica_respaldos(req_put)
        self.assertEqual(resp_put.status_code, 403)

    def test_comando_programado_respeta_ventana_madrugada(self):
        politica = SimpleNamespace(
            habilitado=True,
            hora_programada='02:30',
            frecuencia='DIARIO',
            dia_semana=None,
            ultima_ejecucion_en=None,
        )
        with patch('operaciones.respaldos.politica_vigente', return_value=politica), \
             patch('operaciones.respaldos.debe_ejecutarse_en_ciclo', return_value=(False, 'Fuera de madrugada')), \
             patch('operaciones.respaldos.ejecutar_respaldo') as mock_exec:
            call_command('ejecutar_respaldo_programado', verificar_ventana=True)
            mock_exec.assert_not_called()

        with patch('operaciones.respaldos.politica_vigente', return_value=politica), \
             patch('operaciones.respaldos.ejecutar_respaldo', return_value={'id': 'r-1', 'estado': 'VERIFICADO', 'ubicacionReferencia': '/ext/a.sql.gz', 'tamanoBytes': 512}) as mock_exec:
            call_command('ejecutar_respaldo_programado', forzar=True)
            mock_exec.assert_called_once_with(origen='PROGRAMADO')
