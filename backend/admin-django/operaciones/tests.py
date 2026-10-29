"""Reglas de estado de la salud de la plataforma (HU-45), sin base de datos ni red."""
from django.test import SimpleTestCase

from .salud import CAIDO, LENTO, OK, SIN_CONFIGURAR, estado_general, estado_por_latencia


def servicio(clave, estado):
    return {'clave': clave, 'estado': estado}


class SaludTest(SimpleTestCase):
    def test_latencia_y_p95_marcan_lento(self):
        self.assertEqual(estado_por_latencia(120), OK)
        self.assertEqual(estado_por_latencia(900), LENTO)
        self.assertEqual(estado_por_latencia(50, p95_ms=1600), LENTO)

    def test_errores_del_servidor_marcan_lento(self):
        self.assertEqual(estado_por_latencia(50, errores=6, peticiones=100), LENTO)
        self.assertEqual(estado_por_latencia(50, errores=1, peticiones=100), OK)
        self.assertEqual(estado_por_latencia(50, errores=0, peticiones=0), OK)

    def test_sin_base_o_api_hay_falla(self):
        self.assertEqual(estado_general([servicio('base', CAIDO), servicio('api', OK)]), 'FALLA')
        self.assertEqual(estado_general([servicio('base', OK), servicio('api', CAIDO)]), 'FALLA')

    def test_ia_caida_o_lentitud_piden_atencion(self):
        self.assertEqual(estado_general([servicio('base', OK), servicio('api', OK), servicio('ia', CAIDO)]), 'ATENCION')
        self.assertEqual(estado_general([servicio('base', LENTO), servicio('api', OK)]), 'ATENCION')
        self.assertEqual(estado_general([servicio('base', OK), servicio('api', SIN_CONFIGURAR)]), 'ATENCION')
        self.assertEqual(estado_general([servicio('base', OK), servicio('api', OK), servicio('ia', OK)]), 'OK')
