from django.urls import include, path

urlpatterns = [
    path('api/admin/v1/', include('identidad.urls')),
    path('api/admin/v1/', include('operaciones.urls')),
]
