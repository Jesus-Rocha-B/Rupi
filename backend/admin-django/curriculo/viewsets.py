"""Consulta curricular con DRF y ORM; mantiene el JSON que ya consume React."""
from rest_framework import mixins, viewsets
from rest_framework.renderers import JSONRenderer
from core.autenticacion import SesionRupiAuthentication, PuedeConsultarCurriculo
from .models import Unidad
from .serializers import FiltroUnidadesSerializer, UnidadSerializer

class UnidadViewSet(mixins.ListModelMixin, viewsets.GenericViewSet):
    authentication_classes = [SesionRupiAuthentication]
    permission_classes = [PuedeConsultarCurriculo]
    renderer_classes = [JSONRenderer]
    serializer_class = UnidadSerializer
    pagination_class = None  # Conserva el contrato: arreglo, no envoltorio paginado.
    http_method_names = ['get', 'head', 'options']

    def get_queryset(self):
        # Valida antes del ORM. select_related evita una consulta extra por cada relación.
        filtro = FiltroUnidadesSerializer(data=self.request.query_params)
        filtro.is_valid(raise_exception=True)
        return Unidad.objects.select_related('grado', 'area', 'version_catalogo').filter(
            version_catalogo_id=str(filtro.validated_data['version'])
        ).order_by('grado__numero_grado', 'area__nombre', 'numero_secuencia', 'titulo')
