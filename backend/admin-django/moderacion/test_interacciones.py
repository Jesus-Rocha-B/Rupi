import json
from types import SimpleNamespace
from unittest.mock import patch
from django.test import SimpleTestCase, RequestFactory
from identidad.respuestas import ErrorDePeticion
from . import interacciones, vistas


class HistoryTests(SimpleTestCase):
    def test_redaction_and_non_personal_content(self):
        result=interacciones.redactar('Correo: nina@example.pe; teléfono 987654321; clave: privada; 2 + 3 = 5')
        self.assertNotIn('nina@example.pe',result)
        self.assertNotIn('987654321',result)
        self.assertNotIn('privada',result)
        self.assertIn('2 + 3 = 5',result)

    def test_filters_are_bound_and_dates_use_peru(self):
        sql,values,page=interacciones.filtros({'q':"abc' OR 1=1",'desde':'2026-11-01','hasta':'2026-11-01'})
        self.assertNotIn('OR 1=1',sql)
        self.assertEqual(values[1].hour,5)
        self.assertEqual(values[2].day,2)
        self.assertEqual(page,1)
        for params in [{'pagina':'0'},{'pagina':'abc'},{'desde':'2026-15-01'},{'desde':'2026-11-02','hasta':'2026-11-01'},{'resultado':'anything'},{'hasta':'9999-12-31'}]:
            with self.assertRaises(ErrorDePeticion):interacciones.filtros(params)

    @patch('moderacion.interacciones.listar')
    def test_history_is_not_available_to_support_or_anonymous(self, listing):
        req=RequestFactory().get('/api/admin/v1/moderacion/interacciones')
        req.administrador=None
        self.assertEqual(vistas.historial(req).status_code,401)
        req.administrador=SimpleNamespace(puede=lambda p:p=='moderacion.ver')
        self.assertEqual(vistas.historial(req).status_code,403)
        listing.assert_not_called()

    @patch('moderacion.interacciones.revisar')
    def test_read_access_does_not_allow_review(self, review):
        req=RequestFactory().post('/api/admin/v1/moderacion/interacciones/id',HTTP_ORIGIN='http://localhost:5173')
        req.administrador=SimpleNamespace(puede=lambda p:p=='interacciones.ver')
        self.assertEqual(vistas.interaccion(req,'id').status_code,403)
        review.assert_not_called()

    @patch('moderacion.interacciones.servicio.evaluar_publicada',return_value={'permitida':True,'reglas':[],'version':0})
    def test_invalid_metadata_rejected_before_insert(self, policy):
        for audit in [{'duracionMs':-1},{'duracionMs':True},{'origen':'BROWSER'},{'origen':[]},{'modelo':'x'*121}]:
            with self.assertRaises(ErrorDePeticion):
                # Bypass transaction wrapper for a DB-free validation test.
                interacciones.registrar.__wrapped__({'texto':'Texto', 'auditoria':audit})

    @patch('moderacion.interacciones.listar',return_value={'items':[]})
    def test_authorized_history_is_not_cacheable(self, listing):
        req=RequestFactory().get('/api/admin/v1/moderacion/interacciones')
        req.administrador=SimpleNamespace(puede=lambda p:p=='interacciones.ver')
        result=vistas.historial(req)
        self.assertEqual(result.status_code,200)
        self.assertEqual(result['Cache-Control'],'private, no-store')
