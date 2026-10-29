from identidad.respuestas import api, json_ok
from identidad.sesion import requiere_permiso

from . import salud


@api({'GET'})
@requiere_permiso('plataforma.monitorear')
def salud_de_la_plataforma(request):
    return json_ok(salud.revisar())
