from django.urls import path
from . import vistas

urlpatterns = [path('moderacion/reglas', vistas.reglas), path('moderacion/evaluar', vistas.evaluar),
    path('moderacion/probar', vistas.probar), path('moderacion/publicar', vistas.publicar)]
