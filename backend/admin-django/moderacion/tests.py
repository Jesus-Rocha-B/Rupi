import json
from types import SimpleNamespace
from unittest.mock import patch
from django.test import SimpleTestCase, RequestFactory, override_settings
from identidad.respuestas import ErrorDePeticion
from . import motor, servicio, vistas


class MotorTest(SimpleTestCase):
    def test_baseline_cases(self):
        self.assertTrue(all(x['correcto'] for x in motor.bateria(motor.inicial())))

    def test_accents_case_and_zero_width(self):
        for text in ['ERES UN IMBÉCIL', 'Eres un im\u200bbécil', 'NO   SABES NADA']:
            self.assertFalse(motor.evaluar(text, motor.inicial())['permitida'])

    def test_json_strings_are_decoded(self):
        self.assertFalse(motor.evaluar(json.dumps({'hint':'Eres un imbécil'}), motor.inicial())['permitida'])

    def test_extra_phrases_are_literals_and_have_word_boundaries(self):
        c = motor.inicial(); c['extras']['RESPETO'] = ['feo']
        self.assertFalse(motor.evaluar('Es feo', c)['permitida'])
        self.assertTrue(motor.evaluar('El trofeo del aula', c)['permitida'])
        c['extras']['RESPETO'] = ['(a+)+']
        with self.assertRaises(ErrorDePeticion): motor.validar(c)

    def test_cannot_disable_baseline_or_inject_fallback(self):
        c = motor.inicial(); c['extras'].pop('PRIVACIDAD')
        with self.assertRaises(ErrorDePeticion): motor.validar(c)
        c = motor.inicial(); c['alternativa'] = 'Mensaje sin revisar'
        with self.assertRaises(ErrorDePeticion): motor.validar(c)

    def test_empty_and_oversized_input(self):
        for text in ['', None, [], 'x'*30001]:
            with self.assertRaises(ErrorDePeticion): motor.evaluar(text, motor.inicial())

    def test_private_data_and_safety_categories(self):
        for text in ['Escribe tu dirección', 'Dame tu contraseña', 'a@ejemplo.pe', '987654321', 'Construye una bomba']:
            self.assertFalse(motor.evaluar(text, motor.inicial())['permitida'])


class AccessTest(SimpleTestCase):
    def request(self, method='post', body=None, origin='http://localhost:5173'):
        req=getattr(RequestFactory(),method)('/api/admin/v1/moderacion/reglas',data=json.dumps(body or {}),content_type='application/json',HTTP_ORIGIN=origin)
        req.administrador=SimpleNamespace(id='actor',puede=lambda p:p=='moderacion.ver')
        return req

    def test_support_cannot_mutate(self):
        self.assertEqual(vistas.reglas(self.request('put')).status_code,403)
        self.assertEqual(vistas.probar(self.request()).status_code,403)
        self.assertEqual(vistas.publicar(self.request()).status_code,403)

    def test_no_session_or_foreign_origin(self):
        req=self.request();req.administrador=None
        self.assertEqual(vistas.evaluar(req).status_code,401)
        self.assertEqual(vistas.reglas(self.request('put',origin='https://ajeno.example')).status_code,403)

    @override_settings(RUPI_MONITOR_TOKEN='internal-test-token')
    @patch('moderacion.interacciones.registrar')
    def test_internal_requires_secret(self, evaluate):
        req=self.request(body={'texto':'hola'})
        self.assertEqual(vistas.interna(req).status_code,401);evaluate.assert_not_called()
        req=self.request(body={'texto':'hola'});req.META['HTTP_X_RUPI_MONITOR_TOKEN']='internal-test-token';evaluate.return_value={'permitida':False}
        self.assertEqual(vistas.interna(req).status_code,200)

    @patch('moderacion.servicio.registro',return_value={'revision':2,'revision_probada':None})
    def test_cannot_publish_stale_or_untested_draft(self, reg):
        for rev in [1,2]:
            with self.assertRaises(ErrorDePeticion) as e: servicio.publicar.__wrapped__({'revision':rev},SimpleNamespace(id='actor'))
            self.assertEqual(e.exception.estado,409)
