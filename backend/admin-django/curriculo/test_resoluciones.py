"""HU-51: validaciones de la resolución, la correspondencia y los permisos (sin base de datos)."""
import json
from datetime import date
from types import SimpleNamespace
from unittest.mock import patch

from django.test import RequestFactory, SimpleTestCase

from identidad.respuestas import ErrorDePeticion

from . import resoluciones, vistas

HOY = date(2026, 11, 11)


def datos(**cambios):
    base = {'numero': 'RM 587-2026-MINEDU', 'fechaEmision': '2026-10-30',
            'resumen': 'Actualiza los desempeños de Matemática para 2027.',
            'enlace': 'https://www.gob.pe/institucion/minedu/normas-legales/587',
            'etiqueta': 'cneb-2027', 'vigenteDesde': '2027-03-01'}
    base.update(cambios)
    return base


class ResolucionTest(SimpleTestCase):
    def test_normaliza_numero_y_etiqueta(self):
        limpio = resoluciones.validar_resolucion(datos(numero='rm 587-2026-minedu'), HOY)
        self.assertEqual(limpio['numero'], 'RM 587-2026-MINEDU')
        self.assertEqual(limpio['etiqueta'], 'CNEB-2027')
        self.assertEqual(limpio['vigente_desde'], date(2027, 3, 1))

    def test_rechaza_fechas_imposibles(self):
        for cambio in [{'fechaEmision': '2026-12-01'}, {'fechaEmision': '2026-02-30'},
                       {'vigenteDesde': '2026-10-01'}, {'fechaEmision': None}]:
            with self.assertRaises(ErrorDePeticion):
                resoluciones.validar_resolucion(datos(**cambio), HOY)

    def test_rechaza_enlace_inseguro_y_campos_extra(self):
        for cambio in [{'enlace': 'http://gob.pe/norma'}, {'enlace': 'javascript:alert(1)'},
                       {'estado': 'ACTIVO'}, {'numero': '<b>RM</b>'}, {'resumen': 'corto'}]:
            with self.assertRaises(ErrorDePeticion):
                resoluciones.validar_resolucion(datos(**cambio), HOY)

    def test_al_corregir_no_cambia_version_ni_vigencia(self):
        with self.assertRaises(ErrorDePeticion):
            resoluciones.validar_resolucion(datos(), HOY, creando=False)
        d = datos()
        del d['etiqueta'], d['vigenteDesde']
        self.assertNotIn('etiqueta', resoluciones.validar_resolucion(d, HOY, creando=False))


class CorrespondenciaTest(SimpleTestCase):
    def test_cambio_efectivo(self):
        self.assertEqual(resoluciones.cambio_efectivo({'cambio': 'SE_MANTIENE', 'origen': 'A', 'destino': 'A'}), 'SE_MANTIENE')
        self.assertEqual(resoluciones.cambio_efectivo({'cambio': 'SE_MANTIENE', 'origen': 'A', 'destino': 'B'}), 'MODIFICADA')
        self.assertEqual(resoluciones.cambio_efectivo({'cambio': 'RETIRADA', 'origen': 'A', 'destino': 'A'}), 'RETIRADA')
        self.assertEqual(resoluciones.cambio_efectivo({'cambio': 'RETIRADA', 'origen': 'A', 'destino': None}), 'RETIRADA')

    def test_retirar_y_corregir_piden_sustento(self):
        with self.assertRaises(ErrorDePeticion):
            resoluciones.validar_correspondencia({'accion': 'RETIRAR'}, 'UNIDAD')
        with self.assertRaises(ErrorDePeticion):
            resoluciones.validar_correspondencia({'accion': 'CORREGIR', 'titulo': 'Nuevo título'}, 'UNIDAD')
        self.assertEqual(resoluciones.validar_correspondencia({'accion': 'MANTENER'}, 'UNIDAD'), ('MANTENER', None, None))

    def test_competencias_no_se_renombran_aqui(self):
        with self.assertRaises(ErrorDePeticion):
            resoluciones.validar_correspondencia({'accion': 'CORREGIR', 'titulo': 'Otro nombre', 'nota': 'Según el anexo'},
                                                 'COMPETENCIA')
        with self.assertRaises(ErrorDePeticion):
            resoluciones.validar_correspondencia({'accion': 'BORRAR', 'nota': 'Según el anexo'}, 'UNIDAD')

    @patch('curriculo.resoluciones._resolucion', return_value={'id': 'r', 'publicado_en': '2026-11-11', 'destino_estado': 'ACTIVO'})
    def test_una_resolucion_publicada_no_se_corrige(self, _):
        with self.assertRaises(ErrorDePeticion) as error:
            resoluciones.corregir_correspondencia.__wrapped__('r', 'c', {'accion': 'MANTENER'}, SimpleNamespace(id='a'))
        self.assertEqual(error.exception.estado, 409)

    @patch('curriculo.resoluciones._resolucion', return_value={'id': 'r', 'numero': 'RM 587-2026-MINEDU', 'publicado_en': None})
    def test_publicar_exige_escribir_el_numero(self, _):
        with self.assertRaises(ErrorDePeticion):
            resoluciones.publicar.__wrapped__('r', {'confirmacion': 'RM 587'}, SimpleNamespace(id='a'))


class PermisosTest(SimpleTestCase):
    @patch('curriculo.resoluciones.registrar')
    @patch('curriculo.resoluciones.corregir_resolucion')
    @patch('curriculo.resoluciones.corregir_correspondencia')
    @patch('curriculo.resoluciones.publicar')
    def test_soporte_solo_consulta(self, publicar, correspondencia, corregir, registrar):
        factory = RequestFactory()
        actor = SimpleNamespace(puede=lambda p: p == 'curriculo.ver')
        casos = [('post', vistas.resoluciones, []), ('patch', vistas.resolucion, ['r']),
                 ('patch', vistas.correspondencia, ['r', 'c']), ('post', vistas.publicar_resolucion, ['r'])]
        for metodo, vista, args in casos:
            request = getattr(factory, metodo)('/api/admin/v1/curriculo/resoluciones', data=json.dumps({}),
                                               content_type='application/json', HTTP_ORIGIN='http://localhost:5173')
            request.administrador = actor
            self.assertEqual(vista(request, *args).status_code, 403)
        for simulado in (publicar, correspondencia, corregir, registrar):
            simulado.assert_not_called()
