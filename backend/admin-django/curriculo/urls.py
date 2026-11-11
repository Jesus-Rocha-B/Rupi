from django.urls import path

from identidad import urls as _convertidores  # noqa: F401  registra el convertidor uuid36

from . import vistas

# Catálogo general de grados, áreas y competencias del MINEDU (HU-49)
urlpatterns = [
    path('curriculo/unidades', vistas.unidades_metas),
    path('curriculo/unidades/<uuid36:unidad_id>/registros', vistas.registros_unidad),
    path('curriculo/unidades/<uuid36:unidad_id>/registros/<str:tipo>/<uuid36:registro_id>', vistas.corregir_registro),
    path('curriculo/catalogo', vistas.catalogo_curricular),
    path('curriculo/areas', vistas.areas),
    path('curriculo/areas/<uuid36:area_id>', vistas.area),
    path('curriculo/competencias', vistas.competencias),
    path('curriculo/competencias/<uuid36:competencia_id>', vistas.competencia),
]
