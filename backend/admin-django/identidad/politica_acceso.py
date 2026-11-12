"""Política de bloqueo automático y límite de intentos de inicio de sesión (HU-58).

La administración fija cuántos fallos se toleran y cuánto dura el bloqueo. La política vale para la web, el
celular (los dos en Spring) y la consola (aquí). El bloqueo automático no se escribe en la cuenta: se calcula con
identidad_intento_acceso, así Spring no escribe identidad_cuenta_usuario. Desbloquear desde la consola guarda
intentos_reiniciados_en y los fallos anteriores dejan de contar. Spring hace el mismo cálculo en LoginPolicy.java.
"""
import hashlib
from dataclasses import asdict, dataclass
from datetime import timedelta

from django.db import transaction
from django.db.models import Count
from django.utils import timezone

from operaciones import auditoria

from .models import CuentaUsuario, IntentoAcceso, PoliticaAcceso, UsuarioRol
from .respuestas import ErrorDePeticion

# Nombre en la API → (columna, mínimo, máximo). Los rangos repiten los CHECK de V46.
CAMPOS = {
    'ventanaMinutos': ('ventana_minutos', 1, 60),
    'maxFallosEquipoCuenta': ('max_fallos_equipo_cuenta', 3, 20),
    'maxFallosEquipo': ('max_fallos_equipo', 10, 200),
    'maxFallosCuenta': ('max_fallos_cuenta', 5, 50),
    'minutosBloqueo': ('minutos_bloqueo', 5, 1440),
}
LIMITE_FALLOS_LEIDOS = 500


@dataclass(frozen=True)
class Politica:
    ventana_minutos: int = 5
    max_fallos_equipo_cuenta: int = 5
    max_fallos_equipo: int = 20
    bloqueo_automatico: bool = True
    max_fallos_cuenta: int = 10
    minutos_bloqueo: int = 15

    @property
    def ventana(self):
        return timedelta(minutes=self.ventana_minutos)

    @property
    def bloqueo(self):
        return timedelta(minutes=self.minutos_bloqueo)

    def desde(self, ahora):
        """Desde cuándo hay que leer fallos para saber si una cuenta sigue bloqueada."""
        return ahora - self.ventana - self.bloqueo


POR_DEFECTO = Politica()


def vigente():
    fila = PoliticaAcceso.objects.filter(id=1).first()
    if fila is None:
        return POR_DEFECTO
    return Politica(fila.ventana_minutos, fila.max_fallos_equipo_cuenta, fila.max_fallos_equipo,
                    bool(fila.bloqueo_automatico), fila.max_fallos_cuenta, fila.minutos_bloqueo)


def bloqueado_hasta(fallos, politica, ahora):
    """Fin del bloqueo automático o None. `fallos` son las fechas de clave incorrecta en orden ascendente.

    Cada vez que `max_fallos_cuenta` fallos caben en la ventana empieza un bloqueo; durante el bloqueo la clave no
    se prueba (el intento queda como BLOQUEADO) y los fallos que ya causaron un bloqueo no vuelven a contar.
    """
    n = politica.max_fallos_cuenta
    if not politica.bloqueo_automatico or len(fallos) < n:
        return None
    hasta, desde = None, 0
    for i, momento in enumerate(fallos):
        if hasta is not None and momento <= hasta:
            desde = i + 1
            continue
        primero = i - n + 1
        if primero >= desde and momento <= fallos[primero] + politica.ventana:
            hasta, desde = momento + politica.bloqueo, i + 1
    return hasta if hasta is not None and hasta > ahora else None


def minutos_restantes(hasta, ahora):
    segundos = max(1, int((hasta - ahora).total_seconds()))
    return max(1, -(-segundos // 60))


def identificador(nombre_usuario):
    texto = nombre_usuario.strip().lower() if isinstance(nombre_usuario, str) else ''
    return hashlib.sha256(texto.encode('utf-8')).digest()


def _fallos(filtro, reinicio, politica, ahora):
    desde = politica.desde(ahora)
    if reinicio is not None and reinicio > desde:
        desde = reinicio
    fechas = (IntentoAcceso.objects.filter(resultado='INCORRECTO', ocurrido_en__gt=desde, **filtro)
              .order_by('-ocurrido_en').values_list('ocurrido_en', flat=True)[:LIMITE_FALLOS_LEIDOS])
    return list(reversed(list(fechas)))


def bloqueo_de(nombre_usuario, politica, ahora):
    """Fin del bloqueo automático para ese nombre de usuario, exista o no la cuenta (no revela cuáles existen)."""
    if not politica.bloqueo_automatico:
        return None
    reinicio = (CuentaUsuario.objects.filter(nombre_usuario=(nombre_usuario or '').strip())
                .values_list('intentos_reiniciados_en', flat=True).first())
    return bloqueado_hasta(_fallos({'identificador_hash': identificador(nombre_usuario)}, reinicio, politica, ahora),
                           politica, ahora)


def bloqueos_de_cuentas(cuentas, politica=None, ahora=None):
    """{cuenta_id: fin del bloqueo} de las cuentas dadas que están bloqueadas automáticamente ahora."""
    politica = politica or vigente()
    ahora = ahora or timezone.now()
    if not politica.bloqueo_automatico or not cuentas:
        return {}
    ids = [c.id for c in cuentas]
    # Primero se descartan, con una sola consulta, las que no llegan al número de fallos.
    candidatas = set(IntentoAcceso.objects.filter(usuario_id__in=ids, resultado='INCORRECTO',
                                                  ocurrido_en__gt=politica.desde(ahora))
                     .values('usuario_id').annotate(n=Count('id')).filter(n__gte=politica.max_fallos_cuenta)
                     .values_list('usuario_id', flat=True))
    resultado = {}
    for cuenta in cuentas:
        if cuenta.id in candidatas:
            hasta = bloqueado_hasta(_fallos({'identificador_hash': identificador(cuenta.nombre_usuario)},
                                            cuenta.intentos_reiniciados_en, politica, ahora), politica, ahora)
            if hasta is not None:
                resultado[cuenta.id] = hasta
    return resultado


def bloqueadas_ahora(politica=None, ahora=None):
    """(cuentas con fallos recientes, {cuenta_id: fin del bloqueo}) de las bloqueadas automáticamente ahora."""
    politica = politica or vigente()
    ahora = ahora or timezone.now()
    if not politica.bloqueo_automatico:
        return [], {}
    recientes = (IntentoAcceso.objects.filter(usuario_id__isnull=False, resultado='INCORRECTO',
                                              ocurrido_en__gt=politica.desde(ahora))
                 .values_list('usuario_id', flat=True).distinct())
    cuentas = list(CuentaUsuario.objects.select_related('perfil').filter(id__in=list(recientes)))
    return cuentas, bloqueos_de_cuentas(cuentas, politica, ahora)


def _iso(valor):
    return valor.isoformat().replace('+00:00', 'Z') if valor else None


def _como_api(politica):
    datos = asdict(politica)
    respuesta = {nombre: datos[columna] for nombre, (columna, _, _) in CAMPOS.items()}
    respuesta['bloqueoAutomatico'] = politica.bloqueo_automatico
    return respuesta


def detalle():
    ahora = timezone.now()
    politica = vigente()
    fila = PoliticaAcceso.objects.select_related('actualizado_por').filter(id=1).first()
    cuentas, bloqueos = bloqueadas_ahora(politica, ahora)
    roles = {}
    for usuario_id, rol in UsuarioRol.objects.filter(usuario_id__in=list(bloqueos)).values_list(
            'usuario_id', 'rol__codigo'):
        roles.setdefault(usuario_id, []).append(rol)
    bloqueadas = []
    for cuenta in cuentas:
        if cuenta.id not in bloqueos:
            continue
        perfil = getattr(cuenta, 'perfil', None)
        nombre = ' '.join(x for x in (perfil.nombre, perfil.apellido) if x) if perfil else ''
        bloqueadas.append({'id': cuenta.id, 'usuario': cuenta.nombre_usuario, 'nombre': nombre or cuenta.nombre_usuario,
                           'roles': sorted(roles.get(cuenta.id, [])), 'bloqueadoHasta': _iso(bloqueos[cuenta.id])})
    bloqueadas.sort(key=lambda c: c['bloqueadoHasta'], reverse=True)
    dia = ahora - timedelta(hours=24)
    actividad = dict(IntentoAcceso.objects.filter(ocurrido_en__gt=dia).values('resultado')
                     .annotate(n=Count('id')).values_list('resultado', 'n'))
    return {
        'politica': _como_api(politica),
        'limites': {nombre: {'min': minimo, 'max': maximo} for nombre, (_, minimo, maximo) in CAMPOS.items()},
        'actualizadoEn': _iso(fila.actualizado_en) if fila else None,
        'actualizadoPor': fila.actualizado_por.nombre_usuario if fila and fila.actualizado_por else None,
        'cuentasBloqueadas': bloqueadas,
        'ultimas24h': {'fallidos': actividad.get('INCORRECTO', 0), 'frenados': actividad.get('BLOQUEADO', 0),
                       'correctos': actividad.get('CORRECTO', 0)},
    }


def validar(datos):
    """Devuelve la política nueva o lanza ErrorDePeticion con un mensaje para la pantalla."""
    if not isinstance(datos, dict):
        raise ErrorDePeticion('Envía la política como un objeto.')
    permitidos = set(CAMPOS) | {'bloqueoAutomatico'}
    extra = sorted(set(datos) - permitidos)
    if extra:
        raise ErrorDePeticion(f'Campos no reconocidos: {", ".join(extra)}.')
    faltan = sorted(permitidos - set(datos))
    if faltan:
        raise ErrorDePeticion(f'Faltan campos: {", ".join(faltan)}.')
    valores = {}
    etiquetas = {
        'ventanaMinutos': 'La ventana de tiempo', 'maxFallosEquipoCuenta': 'El límite por cuenta en un equipo',
        'maxFallosEquipo': 'El límite por equipo', 'maxFallosCuenta': 'Los fallos que bloquean la cuenta',
        'minutosBloqueo': 'La duración del bloqueo',
    }
    for nombre, (columna, minimo, maximo) in CAMPOS.items():
        valor = datos[nombre]
        if isinstance(valor, bool) or not isinstance(valor, int):
            raise ErrorDePeticion(f'{etiquetas[nombre]} debe ser un número entero.')
        if not minimo <= valor <= maximo:
            raise ErrorDePeticion(f'{etiquetas[nombre]} debe estar entre {minimo} y {maximo}.')
        valores[columna] = valor
    if not isinstance(datos['bloqueoAutomatico'], bool):
        raise ErrorDePeticion('Indica si el bloqueo automático está activo (sí o no).')
    if valores['max_fallos_equipo'] < valores['max_fallos_equipo_cuenta']:
        raise ErrorDePeticion('El límite por equipo no puede ser menor que el límite por cuenta en un equipo: '
                              'en un aula varias niñas y niños comparten la misma computadora.')
    return Politica(bloqueo_automatico=datos['bloqueoAutomatico'], **valores)


@transaction.atomic
def actualizar(datos, actor):
    politica = validar(datos)
    ahora = timezone.now()
    campos = {**asdict(politica), 'actualizado_por_id': actor.id, 'actualizado_en': ahora}
    if not PoliticaAcceso.objects.filter(id=1).update(**campos):
        PoliticaAcceso.objects.create(id=1, **campos)
    auditoria.registrar(actor.id, 'POLITICA_ACCESO_ACTUALIZADA', None, entidad_tipo='POLITICA_ACCESO')
    return detalle()
