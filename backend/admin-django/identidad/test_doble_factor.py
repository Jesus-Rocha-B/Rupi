"""Pruebas sin base de datos de doble factor TOTP y restricción de acceso (HU-60)."""
from types import SimpleNamespace
from unittest.mock import patch

from django.test import SimpleTestCase

from . import doble_factor, sesion
from .respuestas import ErrorDePeticion

# Secreto ASCII "12345678901234567890" del Apéndice B de RFC 6238 en Base32.
SECRETO_RFC = 'GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ'


class TotpRfc6238Test(SimpleTestCase):
    def test_vector_oficial_rfc_6238(self):
        self.assertEqual(doble_factor.codigo_totp(SECRETO_RFC, instante=59), '287082')
        self.assertEqual(doble_factor.codigo_totp(SECRETO_RFC, instante=1111111109), '081804')
        self.assertEqual(doble_factor.codigo_totp(SECRETO_RFC, instante=1234567890), '005924')

    def test_secreto_aleatorio_y_uri_otpauth(self):
        secreto = doble_factor.generar_secreto()
        self.assertEqual(len(secreto), 32)
        uri = doble_factor.construir_uri_otpauth('admin.rupi', secreto)
        self.assertTrue(uri.startswith('otpauth://totp/Rupi:admin.rupi?'))
        self.assertIn(f'secret={secreto}', uri)
        self.assertIn('issuer=Rupi', uri)

    def test_acepta_ventana_y_bloquea_reuso_del_mismo_paso(self):
        paso = doble_factor.paso_actual(1700000000)
        codigo = doble_factor.codigo_totp(SECRETO_RFC, paso=paso)
        ok, paso_usado = doble_factor.verificar_codigo(SECRETO_RFC, codigo, None, instante=1700000030)
        self.assertTrue(ok)
        self.assertEqual(paso_usado, paso)
        # Reusar el mismo código cuando ya se registró ese paso queda bloqueado.
        ok_reuso, _ = doble_factor.verificar_codigo(SECRETO_RFC, codigo, paso_usado, instante=1700000030)
        self.assertFalse(ok_reuso)

    def test_rechaza_codigo_mal_formado_o_errado(self):
        self.assertEqual(doble_factor.verificar_codigo(SECRETO_RFC, '12ab', None, instante=59), (False, None))
        self.assertEqual(doble_factor.verificar_codigo(SECRETO_RFC, '000000', None, instante=59), (False, None))


class DesafioYRestriccionSesionTest(SimpleTestCase):
    def test_desafio_firmado_valida_proposito(self):
        token = doble_factor.crear_desafio('u-1', 'admin_login')
        self.assertEqual(doble_factor.leer_desafio(token, 'admin_login'), 'u-1')
        with self.assertRaises(ErrorDePeticion):
            doble_factor.leer_desafio(token, 'docente_enrolar')
        with self.assertRaises(ErrorDePeticion):
            doble_factor.leer_desafio(token + 'x', 'admin_login')

    @patch('identidad.sesion._intento')
    @patch('identidad.sesion.doble_factor.preparar_enrolamiento')
    @patch('identidad.sesion.DobleFactorUsuario')
    @patch('identidad.sesion.permisos_de', return_value=frozenset({'consola.ingresar'}))
    @patch('identidad.sesion.claves.verificar', return_value=True)
    @patch('identidad.sesion.CuentaUsuario')
    @patch('identidad.sesion._verificar_freno', return_value=(b'id', None, None))
    def test_admin_sin_2fa_recibe_qr_y_no_abre_sesion(
        self, _freno, mock_cuenta, _claves, _permisos, mock_df, mock_prep, _intento
    ):
        cuenta = SimpleNamespace(id='u-1', nombre_usuario='admin.rupi', hash_clave='h', estado='ACTIVO',
                                 desactivado_en=None, bloqueado_hasta=None, perfil=None)
        mock_cuenta.objects.select_related.return_value.filter.return_value.first.return_value = cuenta
        mock_df.objects.filter.return_value.first.return_value = None
        mock_prep.return_value = SimpleNamespace(secreto_base32=SECRETO_RFC, verificado=False)

        res = sesion.iniciar('admin.rupi', '123456', '127.0.0.1')
        self.assertIsInstance(res, sesion.Desafio2FA)
        self.assertFalse(res.configurado)
        self.assertEqual(res.secreto_base32, SECRETO_RFC)
        self.assertIn('otpauth://totp/Rupi:admin.rupi', res.otpauth_uri)

    @patch('identidad.sesion._intento')
    @patch('identidad.sesion.doble_factor.validar_inicio_sesion', return_value=False)
    @patch('identidad.sesion.DobleFactorUsuario')
    @patch('identidad.sesion.permisos_de', return_value=frozenset({'consola.ingresar'}))
    @patch('identidad.sesion.claves.verificar', return_value=True)
    @patch('identidad.sesion.CuentaUsuario')
    @patch('identidad.sesion._verificar_freno', return_value=(b'id', None, None))
    def test_admin_con_2fa_fallido_recibe_error(
        self, _freno, mock_cuenta, _claves, _permisos, mock_df, _val, _intento
    ):
        cuenta = SimpleNamespace(id='u-1', nombre_usuario='admin.rupi', hash_clave='h', estado='ACTIVO',
                                 desactivado_en=None, bloqueado_hasta=None, perfil=None)
        mock_cuenta.objects.select_related.return_value.filter.return_value.first.return_value = cuenta
        mock_df.objects.filter.return_value.first.return_value = SimpleNamespace(verificado=True)

        res = sesion.iniciar('admin.rupi', '123456', '127.0.0.1', codigo_totp='999999')
        self.assertIsInstance(res, sesion.Error2FA)
