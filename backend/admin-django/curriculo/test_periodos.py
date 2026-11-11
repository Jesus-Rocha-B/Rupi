import json
from datetime import date, datetime
from types import SimpleNamespace
from unittest.mock import patch
from django.test import SimpleTestCase, RequestFactory
from identidad.respuestas import ErrorDePeticion
from . import periodos, vistas


class PeriodosTest(SimpleTestCase):
    year = {'anio': 2027, 'comienza': date(2027, 3, 1), 'termina': date(2027, 12, 20)}

    def data(self):
        return {'tipo': 'TRIMESTRE', 'periodos': [
            {'comienza': '2027-03-01', 'termina': '2027-05-31'},
            {'comienza': '2027-06-01', 'termina': '2027-08-31'},
            {'comienza': '2027-09-01', 'termina': '2027-12-20'}]}

    def test_three_trimesters_and_four_bimesters(self):
        self.assertEqual(periodos.validar(self.data(), self.year)[2]['nombre'], 'III trimestre')
        d = self.data(); d['tipo'] = 'BIMESTRE'
        with self.assertRaises(ErrorDePeticion): periodos.validar(d, self.year)

    def test_rejects_overlap_even_on_same_day(self):
        d = self.data(); d['periodos'][1]['comienza'] = '2027-05-31'
        with self.assertRaises(ErrorDePeticion): periodos.validar(d, self.year)

    def test_rejects_missing_reversed_invalid_and_out_of_calendar_dates(self):
        for start, end in [('', ''), ('2027-02-30','2027-05-31'), ('2027-02-28','2027-05-31'), ('2028-03-01','2028-05-31'), ('2027-05-31','2027-03-01')]:
            d = self.data(); d['periodos'][0] = {'comienza': start, 'termina': end}
            with self.assertRaises(ErrorDePeticion): periodos.validar(d, self.year)

    def test_peru_timezone_and_cross_period_event(self):
        ps = periodos.validar(self.data(), self.year)
        self.assertEqual(periodos.periodo_para(datetime(2027,6,1,2), datetime(2027,6,1,4), ps)['nombre'], 'I trimestre')
        self.assertIsNone(periodos.periodo_para(datetime(2027,6,1,2), datetime(2027,6,1,6), ps))

    @patch('curriculo.periodos.anio')
    @patch('curriculo.periodos.consultar', return_value={'revision': 3})
    def test_concurrent_save_is_rejected(self, query, year):
        with self.assertRaises(ErrorDePeticion) as e:
            periodos.guardar.__wrapped__('id', {'revision': 2}, SimpleNamespace(id='actor'))
        self.assertEqual(e.exception.estado, 409)

    @patch('curriculo.periodos.anio')
    @patch('curriculo.periodos.vista_previa', return_value={'token': 'nuevo'})
    def test_stale_preview_is_rejected(self, preview, year):
        with self.assertRaises(ErrorDePeticion) as e:
            periodos.aplicar.__wrapped__('id', {'token': 'viejo'}, SimpleNamespace(id='actor'))
        self.assertEqual(e.exception.estado, 409)

    @patch('curriculo.periodos.guardar')
    @patch('curriculo.periodos.aplicar')
    def test_support_cannot_save_or_apply(self, apply, save):
        factory = RequestFactory()
        for method, view in [('put', vistas.calendario_periodos), ('post', vistas.aplicar_periodos)]:
            req = getattr(factory, method)('/api/admin/v1/curriculo/periodos/id', data=json.dumps({}), content_type='application/json', HTTP_ORIGIN='http://localhost:5173')
            req.administrador = SimpleNamespace(puede=lambda p: p == 'curriculo.ver')
            self.assertEqual(view(req, 'id').status_code, 403)
        save.assert_not_called(); apply.assert_not_called()

    def test_invalid_filters(self):
        for d in [{}, {'areas': []}, {'areas': ['no-es-uuid']}, {'areas': 'MAT'}]:
            with self.assertRaises(ErrorDePeticion): periodos.filtros(d)
