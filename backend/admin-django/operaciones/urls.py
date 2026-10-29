from django.urls import path

from . import vistas

urlpatterns = [
    path('salud', vistas.salud_de_la_plataforma),
    path('sincronizacion', vistas.auditoria_sincronizacion),
]
