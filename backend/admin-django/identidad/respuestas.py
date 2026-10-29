import json
from functools import wraps
from urllib.parse import urlsplit

from django.conf import settings
from django.http import JsonResponse

METODOS_QUE_ESCRIBEN = {'POST', 'PUT', 'PATCH', 'DELETE'}


class ErrorDePeticion(Exception):
    def __init__(self, mensaje, estado=400):
        super().__init__(mensaje)
        self.mensaje = mensaje
        self.estado = estado


def json_ok(datos, estado=200):
    return JsonResponse(datos, status=estado, safe=False, json_dumps_params={'ensure_ascii': False})


def json_error(mensaje, estado):
    return JsonResponse({'detail': mensaje}, status=estado, json_dumps_params={'ensure_ascii': False})


def leer_json(request):
    if not request.body:
        return {}
    if len(request.body) > 32_768:
        raise ErrorDePeticion('La solicitud es demasiado grande.', 413)
    try:
        datos = json.loads(request.body)
    except (ValueError, UnicodeDecodeError):
        raise ErrorDePeticion('La solicitud no tiene un JSON válido.')
    if not isinstance(datos, dict):
        raise ErrorDePeticion('La solicitud debe ser un objeto JSON.')
    return datos


def origen_permitido(request):
    """Misma regla que SessionController.checkOrigin en Spring: los cambios exigen Origin propio o de la web."""
    origen = request.headers.get('Origin')
    if not origen:
        return False
    partes = urlsplit(origen)
    if not partes.scheme or not partes.netloc:
        return False
    if f'{partes.scheme}://{partes.netloc}'.lower() in {o.lower() for o in settings.RUPI_ORIGENES_PERMITIDOS}:
        return True
    try:
        return partes.netloc.lower() == request.get_host().lower() and partes.scheme == request.scheme
    except Exception:
        return False


def api(metodos):
    """Convierte las vistas en endpoints JSON: método permitido, Origin en escrituras y errores con «detail»."""
    def decorador(vista):
        @wraps(vista)
        def envoltura(request, *args, **kwargs):
            if request.method not in metodos:
                return json_error('Método no permitido.', 405)
            if request.method in METODOS_QUE_ESCRIBEN and not origen_permitido(request):
                return json_error('Origen no permitido.', 403)
            try:
                return vista(request, *args, **kwargs)
            except ErrorDePeticion as error:
                return json_error(error.mensaje, error.estado)
        return envoltura
    return decorador
