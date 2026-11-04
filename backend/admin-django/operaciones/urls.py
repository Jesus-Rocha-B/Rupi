from django.urls import path

from . import vistas

urlpatterns = [
    path('salud', vistas.salud_de_la_plataforma),
    path('sincronizacion', vistas.auditoria_sincronizacion),
    # Monitoreo continuo e incidentes (HU-47)
    path('incidentes', vistas.lista_incidentes),
    path('incidentes/<str:incidente_id>/resolver', vistas.resolver_incidente),
    path('monitoreo/ciclo', vistas.ejecutar_ciclo),
    path('monitoreo/evento-externo', vistas.hook_evento_externo),
    path('alertas/probar', vistas.probar_alertas),
    path('servicios/<str:servicio>/reiniciar', vistas.reiniciar_servicio_manual),
]
