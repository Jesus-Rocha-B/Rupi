"""Pruebas sin base de datos (SimpleTestCase) del catálogo curricular (HU-49)."""
from types import SimpleNamespace
from unittest.mock import MagicMock, patch

from django.test import SimpleTestCase

from identidad.respuestas import ErrorDePeticion

from . import catalogo
from .catalogo import normalizar_capacidades, normalizar_codigo_area, normalizar_codigo_competencia

ACTOR = SimpleNamespace(id='a0000000-0000-4000-8000-0000000000aa')
VERSION_ID = 'c0000000-0000-4000-8000-000000000001'


class CodigosTest(SimpleTestCase):
    def test_codigo_de_area_en_mayusculas(self):
        self.assertEqual(normalizar_codigo_area(' mat '), 'MAT')
        self.assertEqual(normalizar_codigo_area('CAS2'), 'CAS2')

    def test_codigo_de_area_rechaza_espacios_tildes_y_largos(self):
        for valor in ('M', 'MA T', 'ÁREA', 'MATEMATICAS1', '2MAT', '', None, 7):
            with self.assertRaises(ErrorDePeticion, msg=valor):
                normalizar_codigo_area(valor)

    def test_codigo_de_competencia_empieza_con_el_area(self):
        self.assertEqual(normalizar_codigo_competencia('mat-c5', 'MAT'), 'MAT-C5')
        self.assertEqual(normalizar_codigo_competencia('CAS2-ORAL', 'CAS2'), 'CAS2-ORAL')
        with self.assertRaises(ErrorDePeticion):
            normalizar_codigo_competencia('COM-ORAL', 'MAT')

    def test_codigo_de_competencia_necesita_guion(self):
        for valor in ('MAT', 'MAT-', 'MAT--C1', 'MAT C1', 'MAT-Ñ'):
            with self.assertRaises(ErrorDePeticion, msg=valor):
                normalizar_codigo_competencia(valor, 'MAT')


class CapacidadesTest(SimpleTestCase):
    def test_limpia_espacios_y_conserva_el_orden(self):
        self.assertEqual(
            normalizar_capacidades(['  Traduce cantidades  a expresiones numéricas', 'Usa estrategias']),
            ['Traduce cantidades a expresiones numéricas', 'Usa estrategias'])

    def test_exige_al_menos_una_y_como_mucho_ocho(self):
        for valor in ([], None, 'Se valora a sí mismo', ['Capacidad'] * 9):
            with self.assertRaises(ErrorDePeticion, msg=valor):
                normalizar_capacidades(valor)

    def test_rechaza_repetidas_sin_importar_mayusculas(self):
        with self.assertRaises(ErrorDePeticion):
            normalizar_capacidades(['Comprende su cuerpo', 'comprende su cuerpo'])

    def test_rechaza_textos_vacios_o_cortos(self):
        for valor in (['  '], ['ab'], [12]):
            with self.assertRaises(ErrorDePeticion, msg=valor):
                normalizar_capacidades(valor)


class NumeroCnebTest(SimpleTestCase):
    def test_va_del_uno_al_treinta_y_uno(self):
        self.assertEqual(catalogo._entero(23, 1, 31, 'el número'), 23)
        self.assertIsNone(catalogo._entero(None, 1, 31, 'el número'))
        for valor in (0, 32, '23', 2.5, True):
            with self.assertRaises(ErrorDePeticion, msg=valor):
                catalogo._entero(valor, 1, 31, 'el número')


def _version(estado='ACTIVO'):
    return SimpleNamespace(id=VERSION_ID, etiqueta_version='CNEB-2017', estado=estado)


class EscriturasTest(SimpleTestCase):
    def test_version_retirada_no_se_corrige(self):
        with self.assertRaises(ErrorDePeticion) as error:
            catalogo._editable(_version('RETIRADO'))
        self.assertEqual(error.exception.estado, 409)
        catalogo._editable(_version('BORRADOR'))

    @patch('curriculo.catalogo._area')
    def test_codigo_de_area_no_se_cambia(self, mock_area):
        mock_area.return_value = SimpleNamespace(id='x', codigo='MAT', version_catalogo=_version())
        with self.assertRaises(ErrorDePeticion):
            catalogo.modificar_area.__wrapped__('x', {'codigo': 'MATE'}, ACTOR)

    @patch('curriculo.catalogo.Competencia')
    def test_numero_cneb_no_se_repite_en_la_version(self, mock_competencia):
        consulta = mock_competencia.objects.filter.return_value
        consulta.exclude.return_value.first.return_value = SimpleNamespace(codigo='MAT-C1')
        with self.assertRaises(ErrorDePeticion) as error:
            catalogo._numero_libre(23, VERSION_ID, excepto='otra')
        self.assertEqual(error.exception.estado, 409)
        self.assertIn('MAT-C1', error.exception.mensaje)
        mock_competencia.objects.filter.assert_called_with(area__version_catalogo_id=VERSION_ID, numero_cneb=23)

    @patch('curriculo.catalogo.Capacidad')
    def test_guardar_capacidades_actualiza_agrega_y_quita(self, mock_capacidad):
        primera = SimpleNamespace(orden=1, nombre='Comprende su cuerpo', save=lambda **_: None)
        segunda = SimpleNamespace(orden=2, nombre='Se expresa', save=lambda **_: None)
        sobrantes = MagicMock()
        mock_capacidad.objects.filter.side_effect = [[primera, segunda], sobrantes]
        competencia = SimpleNamespace(id='c1')

        cambio = catalogo._guardar_capacidades(competencia, ['Comprende su cuerpo', 'Se expresa corporalmente', 'Nueva'])

        self.assertTrue(cambio)
        self.assertEqual(segunda.nombre, 'Se expresa corporalmente')
        mock_capacidad.objects.filter.assert_any_call(competencia_id='c1', orden__gt=3)
        sobrantes.delete.assert_called_once()
        creada = mock_capacidad.objects.create.call_args.kwargs
        self.assertEqual((creada['orden'], creada['nombre']), (3, 'Nueva'))

    @patch('curriculo.catalogo.Capacidad')
    def test_guardar_capacidades_iguales_no_escribe(self, mock_capacidad):
        mock_capacidad.objects.filter.return_value = [SimpleNamespace(orden=1, nombre='Comprende su cuerpo')]
        self.assertFalse(catalogo._guardar_capacidades(SimpleNamespace(id='c1'), ['Comprende su cuerpo']))
        mock_capacidad.objects.create.assert_not_called()
