from django.db import models


class EventoAuditoria(models.Model):
    """operaciones_evento_auditoria: Django registra aquí solo las acciones de la consola de administración."""
    id = models.BigAutoField(primary_key=True)
    actor_usuario_id = models.CharField(max_length=36, null=True)
    dominio = models.CharField(max_length=60)
    accion = models.CharField(max_length=80)
    entidad_tipo = models.CharField(max_length=80)
    entidad_id = models.CharField(max_length=36, null=True)
    resultado = models.CharField(max_length=20)
    ocurrido_en = models.DateTimeField()
    correlacion_id = models.CharField(max_length=36, null=True)

    class Meta:
        managed = False
        db_table = 'operaciones_evento_auditoria'
