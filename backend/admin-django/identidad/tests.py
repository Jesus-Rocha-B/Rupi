"""Pruebas sin base de datos (SimpleTestCase). El flujo contra MySQL está en scripts/verify_admin_cuentas.py."""
import base64
import hashlib

import bcrypt
from django.test import RequestFactory, SimpleTestCase, override_settings

from . import claves
from .cuentas import enmascarar_correo, normalizar_correo, normalizar_usuario
from .respuestas import ErrorDePeticion, origen_permitido
from .roles import problema_combinacion


class ClavesTest(SimpleTestCase):
    def test_cifra_con_bcrypt_que_spring_puede_verificar(self):
        guardado = claves.cifrar('condor-maiz-48')
        self.assertTrue(guardado.startswith('$2b$12$'))
        self.assertTrue(claves.verificar('condor-maiz-48', guardado))
        self.assertFalse(claves.verificar('condor-maiz-49', guardado))

    def test_verifica_hashes_2a_de_spring(self):
        guardado = bcrypt.hashpw(b'123456', bcrypt.gensalt(rounds=4, prefix=b'2a')).decode()
        self.assertTrue(claves.verificar('123456', guardado))

    def test_verifica_pbkdf2_de_django(self):
        derivado = hashlib.pbkdf2_hmac('sha256', b'quinua-2026', b'salRupi', 1000)
        guardado = f'pbkdf2_sha256$1000$salRupi${base64.b64encode(derivado).decode()}'
        self.assertTrue(claves.verificar('quinua-2026', guardado))
        self.assertFalse(claves.verificar('quinua-2027', guardado))

    def test_rechaza_hashes_vacios_o_desconocidos(self):
        self.assertFalse(claves.verificar('x', ''))
        self.assertFalse(claves.verificar('', '$2b$12$abc'))
        self.assertFalse(claves.verificar('x', 'md5$abc'))
        self.assertFalse(claves.verificar('x', '$2b$12$roto'))

    def test_clave_temporal_es_dictable(self):
        clave = claves.generar_clave_temporal()
        partes = clave.split('-')
        self.assertEqual(len(partes), 4)
        self.assertTrue(partes[3].isdigit() and len(partes[3]) == 2)
        self.assertTrue(all(p.isascii() and p.islower() for p in partes[:3]))
        self.assertIsNone(claves.problema_clave_nueva(clave, 'lucia.huaman'))


class DatosDeCuentaTest(SimpleTestCase):
    def test_usuario_se_normaliza_a_minusculas(self):
        self.assertEqual(normalizar_usuario('  Lucia.Huaman '), 'lucia.huaman')

    def test_usuario_rechaza_tildes_espacios_y_extremos(self):
        for valor in ('lú', 'ana maria', '.ana', 'ana-', 'ab', None, 12):
            with self.assertRaises(ErrorDePeticion, msg=valor):
                normalizar_usuario(valor)

    def test_correo_opcional_y_validado(self):
        self.assertIsNone(normalizar_correo(''))
        self.assertIsNone(normalizar_correo(None))
        self.assertEqual(normalizar_correo(' Ana@Rupi.PE '), 'ana@rupi.pe')
        with self.assertRaises(ErrorDePeticion):
            normalizar_correo('ana@')


@override_settings(RUPI_ORIGENES_PERMITIDOS=['http://127.0.0.1:5173'], ALLOWED_HOSTS=['127.0.0.1'])
class OrigenTest(SimpleTestCase):
    def setUp(self):
        self.fabrica = RequestFactory()

    def test_acepta_la_web_y_el_mismo_servidor(self):
        self.assertTrue(origen_permitido(self.fabrica.post('/', HTTP_ORIGIN='http://127.0.0.1:5173')))
        self.assertTrue(origen_permitido(self.fabrica.post('/', HTTP_ORIGIN='http://127.0.0.1:8082',
                                                           HTTP_HOST='127.0.0.1:8082')))

    def test_rechaza_sin_origin_o_de_otro_sitio(self):
        self.assertFalse(origen_permitido(self.fabrica.post('/')))
        self.assertFalse(origen_permitido(self.fabrica.post('/', HTTP_ORIGIN='https://otro.example')))
        self.assertFalse(origen_permitido(self.fabrica.post('/', HTTP_ORIGIN='null')))


class RolesEstrictosTest(SimpleTestCase):
    def test_estudiante_no_se_combina(self):
        self.assertIsNotNone(problema_combinacion({'ESTUDIANTE', 'DOCENTE'}))
        self.assertIsNotNone(problema_combinacion({'ESTUDIANTE', 'ADMINISTRADOR'}))
        self.assertIsNone(problema_combinacion({'ESTUDIANTE'}))

    def test_toda_cuenta_tiene_un_rol(self):
        self.assertIsNotNone(problema_combinacion(set()))

    def test_docente_puede_administrar(self):
        self.assertIsNone(problema_combinacion({'DOCENTE', 'ADMINISTRADOR'}))

    def test_correo_enmascarado_conserva_el_dominio(self):
        self.assertEqual(enmascarar_correo('lucia.huaman@rupi.pe'), 'l•••@rupi.pe')
        self.assertIsNone(enmascarar_correo(None))
