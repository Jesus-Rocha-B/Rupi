"""S13: contrato de consulta usado realmente por la pantalla de HU-50."""
from rest_framework import serializers
from .models import Unidad

class FiltroUnidadesSerializer(serializers.Serializer):
    version = serializers.UUIDField(required=True)

class UnidadSerializer(serializers.ModelSerializer):
    gradoId = serializers.CharField(source='grado_id')
    areaId = serializers.CharField(source='area_id')
    grado = serializers.CharField(source='grado.nombre')
    area = serializers.CharField(source='area.nombre')
    estado = serializers.CharField(source='version_catalogo.estado')

    class Meta:
        model = Unidad
        fields = ('id', 'codigo', 'titulo', 'gradoId', 'areaId', 'grado', 'area', 'estado')
        read_only_fields = fields
