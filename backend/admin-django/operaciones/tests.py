"""Reglas de estado de la salud y sincronización de la plataforma (HU-45, HU-46 y HU-47), sin base de datos ni red."""
from datetime import datetime, timedelta, timezone
from unittest.mock import MagicMock, patch

from django.test import SimpleTestCase

from .incidentes import (
    registrar_caida,
    registrar_recuperacion,
    resolver_incidente_manual,
)
from .monitoreo import ejecutar_ciclo_monitoreo
from .notificaciones_alertas import (
    enviar_alerta_caida_servicio,
    enviar_notificacion_recuperacion,
    probar_canales_alerta,
)
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


class NotificacionesAlertasTest(SimpleTestCase):
    @patch('operaciones.notificaciones_alertas.send_mail')
    def test_envio_alerta_caida_servicio(self, mock_send_mail):
        mock_send_mail.return_value = 1
        res = enviar_alerta_caida_servicio(
            servicio='ia',
            categoria='IA',
            severidad='CRITICA',
            resumen='Servicio de tutor IA no responde en puerto 8000',
            detalle_error='ConnectionRefusedError: [WinError 10061]',
            autoreinicio_info='Autoreinicio programado en curso',
        )
        self.assertTrue(res['emailEnviado'])
        self.assertTrue(mock_send_mail.called)
        call_args = mock_send_mail.call_args
        asunto = call_args[0][0]
        cuerpo = call_args[0][1]
        self.assertIn('CRITICA', asunto)
        self.assertIn('Inteligencia Artificial', asunto)
        self.assertIn('ConnectionRefusedError', cuerpo)
        self.assertIn('Autoreinicio programado en curso', cuerpo)

    @patch('operaciones.notificaciones_alertas.send_mail')
    def test_envio_notificacion_recuperacion(self, mock_send_mail):
        mock_send_mail.return_value = 1
        res = enviar_notificacion_recuperacion(
            servicio='base',
            duracion_segundos=125,
        )
        self.assertTrue(res['emailEnviado'])
        self.assertTrue(mock_send_mail.called)
        asunto = mock_send_mail.call_args[0][0]
        cuerpo = mock_send_mail.call_args[0][1]
        self.assertIn('RECUPERADO', asunto)
        self.assertIn('MySQL', asunto)
        self.assertIn('125 segundos', cuerpo)

    @patch('operaciones.notificaciones_alertas.send_mail')
    def test_probar_canales_alerta(self, mock_send_mail):
        mock_send_mail.return_value = 1
        res = probar_canales_alerta(actor_nombre='admin_test')
        self.assertTrue(res['emailEnviado'])
        self.assertIn('destinatarioEmail', res)


class IncidentesTest(SimpleTestCase):
    @patch('operaciones.incidentes.enviar_alerta_caida_servicio')
    @patch('operaciones.incidentes.ejecutar_reinicio_servicio')
    @patch('operaciones.incidentes.auditoria.registrar')
    @patch('operaciones.incidentes.Incidente')
    def test_registrar_caida_nuevo_incidente(
        self,
        mock_incidente_cls,
        mock_auditoria,
        mock_reinicio,
        mock_enviar_alerta,
    ):
        mock_incidente_cls.objects.filter.return_value.order_by.return_value.first.return_value = None
        instancia = MagicMock()
        instancia.id = 'inc-123'
        instancia.servicio = 'ia'
        instancia.estado = 'ABIERTO'
        instancia.severidad = 'CRITICA'
        instancia.resumen = 'Fallo en IA'
        instancia.iniciado_en = datetime.now(timezone.utc)
        instancia.resuelto_en = None
        mock_incidente_cls.return_value = instancia
        mock_reinicio.return_value = {'exito': False, 'mensaje': 'Sin reinicio'}
        mock_enviar_alerta.return_value = {'email': {'enviado': True}}

        resultado = registrar_caida(
            servicio='ia',
            severidad='CRITICA',
            resumen='Fallo en IA',
            intentar_autoreinicio=False,
        )

        self.assertTrue(resultado['esNuevo'])
        self.assertEqual(resultado['servicio'], 'ia')
        self.assertEqual(resultado['severidad'], 'CRITICA')
        self.assertTrue(mock_enviar_alerta.called)
        self.assertTrue(instancia.save.called)

    @patch('operaciones.incidentes.enviar_alerta_caida_servicio')
    @patch('operaciones.incidentes.Incidente')
    def test_registrar_caida_evita_duplicados_si_ya_esta_abierto(
        self,
        mock_incidente_cls,
        mock_enviar_alerta,
    ):
        existente = MagicMock()
        existente.id = 'inc-previo'
        existente.servicio = 'base'
        existente.estado = 'ABIERTO'
        existente.severidad = 'CRITICA'
        existente.resumen = 'Caida previa de base'
        existente.iniciado_en = datetime.now(timezone.utc)
        mock_incidente_cls.objects.filter.return_value.order_by.return_value.first.return_value = existente

        resultado = registrar_caida(servicio='base')

        self.assertFalse(resultado['esNuevo'])
        self.assertEqual(resultado['id'], 'inc-previo')
        self.assertFalse(mock_enviar_alerta.called)

    @patch('operaciones.incidentes.enviar_notificacion_recuperacion')
    @patch('operaciones.incidentes.auditoria.registrar')
    @patch('operaciones.incidentes.Incidente')
    def test_registrar_recuperacion_cierra_incidente(
        self,
        mock_incidente_cls,
        mock_auditoria,
        mock_enviar_recuperacion,
    ):
        abierto = MagicMock()
        abierto.id = 'inc-cerrar'
        abierto.servicio = 'ia'
        abierto.estado = 'ABIERTO'
        abierto.iniciado_en = datetime.now(timezone.utc) - timedelta(seconds=60)
        mock_incidente_cls.objects.filter.return_value.order_by.return_value.first.return_value = abierto
        mock_enviar_recuperacion.return_value = {'email': {'enviado': True}}

        resultado = registrar_recuperacion('ia')

        self.assertIsNotNone(resultado)
        self.assertEqual(resultado['id'], 'inc-cerrar')
        self.assertEqual(abierto.estado, 'RESUELTO')
        self.assertTrue(abierto.save.called)
        self.assertTrue(mock_enviar_recuperacion.called)

    @patch('operaciones.incidentes.auditoria.registrar')
    @patch('operaciones.incidentes.Incidente')
    def test_resolver_incidente_manual(self, mock_incidente_cls, mock_auditoria):
        incidente = MagicMock()
        incidente.id = 'inc-manual'
        incidente.servicio = 'api'
        incidente.estado = 'ABIERTO'
        incidente.iniciado_en = datetime.now(timezone.utc) - timedelta(minutes=5)
        incidente.referencia_externa = 'Error 500'
        mock_incidente_cls.objects.filter.return_value.first.return_value = incidente

        res = resolver_incidente_manual('inc-manual', actor_id='admin-1', comentario='Reiniciado por admin')

        self.assertEqual(res['estado'], 'RESUELTO')
        self.assertIn('Reiniciado por admin', incidente.referencia_externa)
        self.assertTrue(incidente.save.called)


class MonitoreoTest(SimpleTestCase):
    @patch('operaciones.monitoreo.incidentes.listar_incidentes')
    @patch('operaciones.monitoreo.incidentes.registrar_recuperacion')
    @patch('operaciones.monitoreo.incidentes.registrar_caida')
    @patch('operaciones.monitoreo.salud.revisar')
    def test_ejecutar_ciclo_monitoreo_detecta_caida(
        self,
        mock_revisar,
        mock_registrar_caida,
        mock_registrar_recuperacion,
        mock_listar_incidentes,
    ):
        mock_listar_incidentes.return_value = []
        mock_registrar_recuperacion.return_value = None
        mock_revisar.return_value = {
            'estado': 'FALLA',
            'servicios': [
                {'clave': 'base', 'estado': 'OK', 'nombre': 'Base de Datos'},
                {'clave': 'ia', 'estado': 'CAIDO', 'nombre': 'IA Tutor', 'detalle': {'error': 'Puerto 8000 cerrado'}},
                {'clave': 'api', 'estado': 'OK', 'nombre': 'API Spring Boot'},
            ],
            'alertas': [],
        }
        mock_registrar_caida.return_value = {
            'id': 'inc-ia',
            'esNuevo': True,
            'autoreinicio': 'Reinicio simulado',
            'autoreinicioExitoso': True,
        }

        res = ejecutar_ciclo_monitoreo(intentar_autoreinicio=False)

        self.assertEqual(res['estadoGeneral'], 'FALLA')
        self.assertEqual(len(res['novedades']), 1)
        self.assertEqual(res['novedades'][0]['servicio'], 'ia')
        self.assertTrue(mock_registrar_caida.called)

    @patch('operaciones.monitoreo.incidentes.listar_incidentes')
    @patch('operaciones.monitoreo.incidentes.registrar_recuperacion')
    @patch('operaciones.monitoreo.incidentes.registrar_caida')
    @patch('operaciones.monitoreo.salud.revisar')
    def test_ejecutar_ciclo_monitoreo_detecta_recuperacion(
        self,
        mock_revisar,
        mock_registrar_caida,
        mock_registrar_recuperacion,
        mock_listar_incidentes,
    ):
        mock_listar_incidentes.return_value = []
        mock_revisar.return_value = {
            'estado': 'OK',
            'servicios': [
                {'clave': 'base', 'estado': 'OK', 'nombre': 'Base de Datos'},
            ],
            'alertas': [],
        }
        mock_registrar_recuperacion.return_value = {
            'id': 'inc-base-recuperado',
            'servicio': 'base',
            'duracionSegundos': 45,
        }

        res = ejecutar_ciclo_monitoreo()

        self.assertEqual(res['estadoGeneral'], 'OK')
        self.assertEqual(len(res['novedades']), 1)
        self.assertEqual(res['novedades'][0]['evento'], 'RECUPERACION_REGISTRADA')
        self.assertEqual(res['novedades'][0]['duracionSegundos'], 45)
        self.assertFalse(mock_registrar_caida.called)
