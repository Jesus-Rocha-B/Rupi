"""HU-58: validación de la política y cálculo del bloqueo automático, sin base de datos.

El flujo contra MySQL, Spring y la consola está en scripts/verify_hu58_bloqueo_intentos.py.
"""
import hashlib
from datetime import datetime, timedelta, timezone as tz

from django.test import SimpleTestCase

from .politica_acceso import POR_DEFECTO, Politica, bloqueado_hasta, identificador, minutos_restantes, validar
from .respuestas import ErrorDePeticion

AHORA = datetime(2026, 11, 11, 15, 0, tzinfo=tz.utc)


def fallos(primero, cantidad, separacion):
    return [primero + timedelta(seconds=i * separacion) for i in range(cantidad)]


def datos(**cambios):
    base = {'ventanaMinutos': 5, 'maxFallosEquipoCuenta': 5, 'maxFallosEquipo': 20, 'bloqueoAutomatico': True,
            'maxFallosCuenta': 10, 'minutosBloqueo': 15}
    base.update(cambios)
    return base


class BloqueoAutomaticoTest(SimpleTestCase):
    def test_valores_iniciales_son_los_de_antes(self):
        self.assertEqual((POR_DEFECTO.ventana_minutos, POR_DEFECTO.max_fallos_equipo_cuenta,
                          POR_DEFECTO.max_fallos_equipo), (5, 5, 20))

    def test_menos_fallos_que_el_limite_no_bloquea(self):
        self.assertIsNone(bloqueado_hasta(fallos(AHORA - timedelta(minutes=2), 9, 10), POR_DEFECTO, AHORA))

    def test_llegar_al_limite_bloquea_desde_el_ultimo_fallo(self):
        lista = fallos(AHORA - timedelta(minutes=3), 10, 10)
        self.assertEqual(bloqueado_hasta(lista, POR_DEFECTO, AHORA), lista[-1] + timedelta(minutes=15))

    def test_fallos_espaciados_fuera_de_la_ventana_no_bloquean(self):
        self.assertIsNone(bloqueado_hasta(fallos(AHORA - timedelta(minutes=7), 10, 40), POR_DEFECTO, AHORA))

    def test_el_bloqueo_termina_solo(self):
        self.assertIsNone(bloqueado_hasta(fallos(AHORA - timedelta(minutes=20), 10, 5), POR_DEFECTO, AHORA))

    def test_fallos_que_ya_bloquearon_no_vuelven_a_contar(self):
        politica = Politica(60, 5, 20, True, 5, 5)
        lista = fallos(AHORA - timedelta(minutes=20), 5, 10) + [AHORA - timedelta(minutes=1)]
        self.assertIsNone(bloqueado_hasta(lista, politica, AHORA))

    def test_bloqueo_apagado_nunca_bloquea(self):
        politica = Politica(5, 5, 20, False, 10, 15)
        self.assertIsNone(bloqueado_hasta(fallos(AHORA - timedelta(minutes=1), 30, 1), politica, AHORA))

    def test_minutos_restantes_redondea_hacia_arriba(self):
        self.assertEqual(minutos_restantes(AHORA + timedelta(minutes=14, seconds=1), AHORA), 15)
        self.assertEqual(minutos_restantes(AHORA + timedelta(seconds=3), AHORA), 1)

    def test_identificador_igual_que_spring(self):
        # Spring guarda SHA-256 del usuario en minúsculas; el mismo valor permite contar los fallos de ambos lados.
        self.assertEqual(identificador('  Lucia.Huaman '), hashlib.sha256(b'lucia.huaman').digest())

    def test_identificador_tolera_valores_no_cadena(self):
        vacio = hashlib.sha256(b'').digest()
        self.assertEqual(identificador(None), vacio)
        self.assertEqual(identificador(123), vacio)


class ValidarPoliticaTest(SimpleTestCase):
    def test_acepta_una_politica_valida(self):
        politica = validar(datos(maxFallosCuenta=8, minutosBloqueo=30, bloqueoAutomatico=False))
        self.assertEqual((politica.max_fallos_cuenta, politica.minutos_bloqueo, politica.bloqueo_automatico),
                         (8, 30, False))

    def test_rechaza_valores_fuera_de_rango(self):
        for campo, valor in (('ventanaMinutos', 0), ('maxFallosEquipoCuenta', 2), ('maxFallosEquipo', 201),
                             ('maxFallosCuenta', 4), ('minutosBloqueo', 1441)):
            with self.assertRaises(ErrorDePeticion, msg=campo):
                validar(datos(**{campo: valor}))

    def test_rechaza_tipos_equivocados(self):
        for cambio in ({'ventanaMinutos': '5'}, {'maxFallosCuenta': 7.5}, {'minutosBloqueo': True},
                       {'bloqueoAutomatico': 'si'}):
            with self.assertRaises(ErrorDePeticion, msg=cambio):
                validar(datos(**cambio))

    def test_rechaza_campos_extra_o_faltantes(self):
        with self.assertRaises(ErrorDePeticion):
            validar(datos(otro=1))
        incompleto = datos()
        del incompleto['minutosBloqueo']
        with self.assertRaises(ErrorDePeticion):
            validar(incompleto)
        with self.assertRaises(ErrorDePeticion):
            validar([])

    def test_limite_por_equipo_no_menor_que_por_cuenta(self):
        with self.assertRaisesMessage(ErrorDePeticion, 'comparten la misma computadora'):
            validar(datos(maxFallosEquipoCuenta=15, maxFallosEquipo=12))
