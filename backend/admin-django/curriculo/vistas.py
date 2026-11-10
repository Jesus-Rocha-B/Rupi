from identidad.respuestas import api, json_error, json_ok, leer_json
from identidad.sesion import requiere_permiso

from . import catalogo

PERMISO_GESTIONAR = 'curriculo.gestionar'


def _sin_permiso_de_gestion():
    return json_error('Tu rol puede consultar el catálogo, pero no corregirlo.', 403)


@api({'GET'})
@requiere_permiso('curriculo.ver')
def catalogo_curricular(request):
    return json_ok(catalogo.catalogo(request.GET.get('version') or None))


@api({'POST'})
@requiere_permiso('curriculo.ver')
def areas(request):
    if not request.administrador.puede(PERMISO_GESTIONAR):
        return _sin_permiso_de_gestion()
    return json_ok(catalogo.crear_area(leer_json(request), request.administrador), 201)


@api({'PATCH'})
@requiere_permiso('curriculo.ver')
def area(request, area_id):
    if not request.administrador.puede(PERMISO_GESTIONAR):
        return _sin_permiso_de_gestion()
    return json_ok(catalogo.modificar_area(area_id, leer_json(request), request.administrador))


@api({'POST'})
@requiere_permiso('curriculo.ver')
def competencias(request):
    if not request.administrador.puede(PERMISO_GESTIONAR):
        return _sin_permiso_de_gestion()
    return json_ok(catalogo.crear_competencia(leer_json(request), request.administrador), 201)


@api({'PATCH'})
@requiere_permiso('curriculo.ver')
def competencia(request, competencia_id):
    if not request.administrador.puede(PERMISO_GESTIONAR):
        return _sin_permiso_de_gestion()
    return json_ok(catalogo.modificar_competencia(competencia_id, leer_json(request), request.administrador))
