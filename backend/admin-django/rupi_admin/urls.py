from django.urls import include, path
from moderacion.vistas import interna

urlpatterns = [
    path('api/internal/moderacion/evaluar', interna),
    path('api/admin/v1/', include('moderacion.urls')),
    path('api/admin/v1/', include('identidad.urls')),
    path('api/admin/v1/', include('operaciones.urls')),
    path('api/admin/v1/', include('curriculo.urls')),
]
