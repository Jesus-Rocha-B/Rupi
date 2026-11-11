from django.urls import path
from . import vistas

urlpatterns = [path('moderacion/interacciones', vistas.historial), path('moderacion/interacciones/<uuid:identifier>', vistas.interaccion), path('moderacion/reglas', vistas.reglas), path('moderacion/evaluar', vistas.evaluar),
    path('moderacion/probar', vistas.probar), path('moderacion/publicar', vistas.publicar)]
