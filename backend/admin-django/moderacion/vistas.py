import hmac
from django.conf import settings
from django.db import DatabaseError
from identidad.respuestas import api, leer_json, json_ok, json_error
from identidad.sesion import requiere_permiso
from . import motor, servicio


@api({'GET', 'PUT'})
@requiere_permiso('moderacion.ver')
def reglas(request):
    if request.method == 'GET': return json_ok(servicio.consultar())
    if not request.administrador.puede('moderacion.gestionar'): return json_error('Tu rol solo puede consultar las reglas.', 403)
    return json_ok(servicio.guardar(leer_json(request), request.administrador))


@api({'POST'})
@requiere_permiso('moderacion.ver')
def evaluar(request):
    data = leer_json(request)
    r = servicio.registro(); servicio.comprobar_revision(r, data)
    return json_ok(motor.evaluar(data.get('texto'), servicio.decode(r['borrador'])))


@api({'POST'})
@requiere_permiso('moderacion.gestionar')
def probar(request):
    return json_ok(servicio.probar(leer_json(request)))


@api({'POST'})
@requiere_permiso('moderacion.gestionar')
def publicar(request):
    return json_ok(servicio.publicar(leer_json(request), request.administrador))


@api({'POST'}, exigir_origen=False)
def interna(request):
    expected = settings.RUPI_MONITOR_TOKEN
    received = request.headers.get('X-Rupi-Monitor-Token', '')
    if not expected or not hmac.compare_digest(expected.encode(), received.encode()): return json_error('No autorizado.', 401)
    try: return json_ok(servicio.evaluar_publicada(leer_json(request).get('texto')))
    except DatabaseError: return json_error('La evaluación no está disponible. No mostrar la respuesta.', 503)
