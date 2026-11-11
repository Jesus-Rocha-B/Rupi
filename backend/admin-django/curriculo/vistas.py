"""Vistas HTTP del catálogo curricular MINEDU (HU-49)."""
from functools import wraps

from identidad.respuestas import api, json_error, json_ok, leer_json
from identidad.sesion import requiere_permiso

from . import catalogo

PERMISO_GESTIONAR = 'curriculo.gestionar'


def _requiere_gestion(vista):
    """Restringe las modificaciones del catálogo a roles con permiso explícito de gestión."""
    @wraps(vista)
    def envuelta(request, *args, **kwargs):
        if not request.administrador.puede(PERMISO_GESTIONAR):
            return json_error('Tu rol puede consultar el catálogo, pero no corregirlo.', 403)
        return vista(request, *args, **kwargs)

    return envuelta


@api({'GET'})
@requiere_permiso('curriculo.ver')
def catalogo_curricular(request):
    """Retorna el árbol completo de grados, áreas y competencias del catálogo curricular."""
    return json_ok(catalogo.catalogo(request.GET.get('version') or None))


@api({'POST'})
@requiere_permiso('curriculo.ver')
@_requiere_gestion
def areas(request):
    """Crea una nueva área curricular en la versión activa del catálogo."""
    return json_ok(catalogo.crear_area(leer_json(request), request.administrador), 201)


@api({'PATCH'})
@requiere_permiso('curriculo.ver')
@_requiere_gestion
def area(request, area_id):
    """Actualiza los metadatos de un área curricular existente."""
    return json_ok(catalogo.modificar_area(area_id, leer_json(request), request.administrador))


@api({'POST'})
@requiere_permiso('curriculo.ver')
@_requiere_gestion
def competencias(request):
    """Registra una nueva competencia y sus capacidades asociadas a un área."""
    return json_ok(catalogo.crear_competencia(leer_json(request), request.administrador), 201)


@api({'PATCH'})
@requiere_permiso('curriculo.ver')
@_requiere_gestion
def competencia(request, competencia_id):
    """Actualiza una competencia curricular existente."""
    return json_ok(catalogo.modificar_competencia(competencia_id, leer_json(request), request.administrador))
