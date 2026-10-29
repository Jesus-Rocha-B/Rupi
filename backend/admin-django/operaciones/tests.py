"""Reglas de estado de la salud y sincronización de la plataforma (HU-45 y HU-46), sin base de datos ni red."""
from django.test import SimpleTestCase

from .salud import (
    CAIDO,
    LENTO,
    OK,
    SIN_CONFIGURAR,
    estado_general,
    estado_por_latencia,
    evaluar_alertas,
)
from .sincronizacion import evaluar_alertas_sincronizacion


def servicio(clave, estado, detalle=None, peticiones=None, latencia_ms=None):
    s = {'clave': clave, 'estado': estado, 'detalle': detalle or {}}
    if peticiones is not None:
        s['peticiones'] = peticiones
    if latencia_ms is not None:
        s['latenciaMs'] = latencia_ms
    return s


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

    def test_evaluar_alertas_detecta_caidas_y_p95(self):
        servicios = [
            servicio('base', CAIDO),
            servicio('api', OK, peticiones={'ventana': {'p95Ms': 1800, 'serverErrors': 0, 'requests': 50}}),
        ]
        alertas = evaluar_alertas(servicios)
        ids = [a['id'] for a in alertas]
        self.assertIn('base_caida', ids)
        self.assertIn('api_p95_alto', ids)
        self.assertTrue(any(a['nivel'] == 'CRITICA' for a in alertas))

    def test_evaluar_alertas_detecta_cpu_y_memoria_critica(self):
        servicios = [
            servicio('base', OK),
            servicio('api', OK, detalle={
                'sistema': {'cpuPercent': 89.5, 'memoryPercent': 92.0, 'usedMemoryMb': 7000, 'totalMemoryMb': 8000}
            }),
        ]
        alertas = evaluar_alertas(servicios)
        ids = [a['id'] for a in alertas]
        self.assertIn('api_cpu_critica', ids)
        self.assertIn('api_memoria_critica', ids)
        self.assertEqual(estado_general(servicios, alertas), 'FALLA')


class SincronizacionTest(SimpleTestCase):
    def test_detecta_errores_de_transmision_como_criticos(self):
        resumen = {'eventosError': 3, 'totalTrafico': 50, 'tasaExitoPercent': 94.0}
        alertas = evaluar_alertas_sincronizacion(resumen, [], [])
        ids = [a['id'] for a in alertas]
        self.assertIn('sync_errores_transmision', ids)
        critica = next(a for a in alertas if a['id'] == 'sync_errores_transmision')
        self.assertEqual(critica['nivel'], 'CRITICA')

    def test_detecta_discrepancias_entre_web_y_movil(self):
        resumen = {'eventosError': 0, 'totalTrafico': 20, 'tasaExitoPercent': 100.0}
        comparaciones = [
            {
                'usuario': 'mateo.quispe',
                'estado': 'DISCREPANCIA',
                'puntajeBd': 120,
                'reporteWeb': {'puntajeXp': 120, 'lecciones': 5},
                'reporteMovil': {'puntajeXp': 90, 'lecciones': 4},
            }
        ]
        alertas = evaluar_alertas_sincronizacion(resumen, comparaciones, [])
        ids = [a['id'] for a in alertas]
        self.assertIn('sync_discrepancia_puntajes', ids)
        alerta = next(a for a in alertas if a['id'] == 'sync_discrepancia_puntajes')
        self.assertEqual(alerta['nivel'], 'ADVERTENCIA')
        self.assertIn('mateo.quispe', alerta['mensaje'])

    def test_sin_errores_ni_discrepancias_lista_vacia(self):
        resumen = {'eventosError': 0, 'totalTrafico': 25, 'tasaExitoPercent': 100.0}
        comparaciones = [
            {
                'usuario': 'lucia.huaman',
                'estado': 'SINCRONIZADO',
                'puntajeBd': 150,
                'reporteWeb': {'puntajeXp': 150, 'lecciones': 6},
                'reporteMovil': {'puntajeXp': 150, 'lecciones': 6},
            }
        ]
        alertas = evaluar_alertas_sincronizacion(resumen, comparaciones, [])
        self.assertEqual(len(alertas), 0)
