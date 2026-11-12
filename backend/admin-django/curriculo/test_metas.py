import json
from types import SimpleNamespace
from unittest.mock import patch
from django.test import SimpleTestCase, RequestFactory
from identidad.respuestas import ErrorDePeticion
from . import metas, vistas


class MetasTest(SimpleTestCase):
    def test_rechaza_cambio_de_relaciones_en_correccion(self):
        for field in ['unidadId', 'competenciaId', 'gradoId', 'tipo', 'codigo']:
            with self.assertRaises(ErrorDePeticion):
                metas.validar({'descripcion': 'Comparar cantidades hasta cien.', 'motivo': 'Precisar alcance', field: 'otro'}, True)

    def test_validar_rechaza_payload_no_objeto(self):
        for payload in [None, [], 'texto', 123]:
            with self.assertRaises(ErrorDePeticion):
                metas.validar(payload)

    def test_valida_textos_tipo_y_codigo(self):
        for tipo in [None, [], {}, 'OTRO']:
            with self.assertRaises(ErrorDePeticion):
                metas.tipo_valido(tipo)
        for descripcion in ['', 'corta', 'x' * 4001, 45]:
            with self.assertRaises(ErrorDePeticion):
                metas.validar({'descripcion': descripcion, 'codigo': 'MAT-01'})
        self.assertEqual(metas.validar({'descripcion': ' Comparar cantidades hasta cien. ', 'codigo': 'mat-01'})[1], 'MAT-01')

    @patch('curriculo.metas.unidad')
    @patch('curriculo.metas.filas')
    def test_conflicto_concurrente_no_escribe(self, rows, unit):
        rows.return_value = [{'descripcion': 'Texto que cambió en otro equipo.'}]
        with self.assertRaises(ErrorDePeticion) as error:
            metas.modificar.__wrapped__('unidad', 'META', 'registro', {'descripcion': 'Una descripción suficientemente larga.', 'motivo': 'Precisar contenido', 'revision': 'anterior'}, SimpleNamespace(id='actor'))
        self.assertEqual(error.exception.estado, 409)

    @patch('curriculo.metas.unidad')
    @patch('curriculo.metas.filas', return_value=[])
    def test_registro_ajeno_a_unidad_no_se_modifica(self, rows, unit):
        with self.assertRaises(ErrorDePeticion) as error:
            metas.modificar.__wrapped__('unidad', 'META', 'registro', {'descripcion': 'Una descripción suficientemente larga.', 'motivo': 'Precisar contenido', 'revision': 'anterior'}, SimpleNamespace(id='actor'))
        self.assertEqual(error.exception.estado, 404)

    @patch('curriculo.metas.crear')
    @patch('curriculo.metas.modificar')
    def test_soporte_no_puede_crear_ni_corregir(self, modificar, crear):
        factory = RequestFactory()
        actor = SimpleNamespace(puede=lambda p: p == 'curriculo.ver')
        for method, view, args in [('post', vistas.registros_unidad, ['unidad']), ('patch', vistas.corregir_registro, ['unidad','META','registro'])]:
            request = getattr(factory, method)('/api/admin/v1/curriculo/unidades', data=json.dumps({}), content_type='application/json', HTTP_ORIGIN='http://localhost:5173')
            request.administrador = actor
            self.assertEqual(view(request,*args).status_code,403)
        crear.assert_not_called(); modificar.assert_not_called()
