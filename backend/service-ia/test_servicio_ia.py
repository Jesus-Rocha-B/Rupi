"""Pruebas del servicio de IA sin red ni clave real: se reemplaza la llamada a Gemini.

Ejecutar: .venv/bin/python -m unittest test_servicio_ia
"""
import json
import os
import unittest
from unittest import mock

import gemini_client


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


if __name__ == '__main__':
    unittest.main()
