"""Pruebas del servicio de IA sin red ni clave real: se reemplaza la llamada a Gemini.

Ejecutar: .venv/bin/python -m unittest test_servicio_ia
"""
import json
import os
import unittest
from unittest import mock

from fastapi.testclient import TestClient

import gemini_client
import main

cliente = TestClient(main.app)


class _Respuesta:
    def __init__(self, cuerpo):
        self._cuerpo = cuerpo

    def read(self):
        return self._cuerpo

    def __enter__(self):
        return self

    def __exit__(self, *args):
        return False


class ClaveDeGeminiTest(unittest.TestCase):
    def test_la_clave_viaja_en_cabecera_y_no_en_la_url(self):
        respuesta = json.dumps({'candidates': [{'content': {'parts': [{'text': 'hola'}]}}]}).encode()
        with mock.patch.dict(os.environ, {'GEMINI_API_KEY': 'clave-de-prueba'}), \
                mock.patch('urllib.request.urlopen', return_value=_Respuesta(respuesta)) as abrir:
            self.assertEqual(gemini_client.call_gemini('sistema', 'pregunta'), 'hola')
        peticion = abrir.call_args.args[0]
        self.assertNotIn('clave-de-prueba', peticion.full_url)
        self.assertEqual(peticion.get_header('X-goog-api-key'), 'clave-de-prueba')



class ErroresSinFiltrarTest(unittest.TestCase):
    def test_sin_clave_responde_503_claro(self):
        with mock.patch.object(main, 'call_gemini', side_effect=gemini_client.IANoConfigurada('falta GEMINI_API_KEY en .local')):
            r = cliente.post('/api/v1/ia/tutor/chat', json={'message': 'hola'})
        self.assertEqual(r.status_code, 503)
        self.assertNotIn('.local', r.text)

    def test_un_fallo_de_gemini_no_llega_al_cliente(self):
        with mock.patch.object(main, 'call_gemini', side_effect=gemini_client.IAError('Error de Gemini (400): API key secreta-123 inválida')), \
                self.assertLogs('rupi.ia', level='ERROR'):
            r = cliente.post('/api/v1/ia/roadmap/generate-from-text', json={'text': 'fracciones'})
        self.assertEqual(r.status_code, 502)
        self.assertNotIn('secreta', r.text)
        self.assertNotIn('Gemini (400)', r.text)



class CorsTest(unittest.TestCase):
    def test_solo_la_web_de_rupi_puede_llamar_desde_el_navegador(self):
        preflight = {'Access-Control-Request-Method': 'POST', 'Access-Control-Request-Headers': 'content-type'}
        ajena = cliente.options('/api/v1/ia/tutor/chat', headers={'Origin': 'https://ajena.example', **preflight})
        self.assertNotIn('access-control-allow-origin', ajena.headers)
        propia = cliente.options('/api/v1/ia/tutor/chat', headers={'Origin': 'http://127.0.0.1:5173', **preflight})
        self.assertEqual(propia.headers.get('access-control-allow-origin'), 'http://127.0.0.1:5173')
        self.assertNotIn('access-control-allow-credentials', propia.headers)


if __name__ == '__main__':
    unittest.main()
