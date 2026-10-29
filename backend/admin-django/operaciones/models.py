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


class EventoSincronizacion(models.Model):
    """operaciones_evento_sincronizacion: Auditoría de tráfico y sincronización web/móvil (HU-46)."""
    id = models.CharField(max_length=36, primary_key=True)
    usuario_id = models.CharField(max_length=36)
    dispositivo_id = models.CharField(max_length=36, null=True)
    cliente_tipo = models.CharField(max_length=10, default='WEB')
    evento_cliente_id = models.CharField(max_length=36)
    tipo_evento = models.CharField(max_length=80)
    puntaje_xp = models.IntegerField(default=0)
    lecciones_completadas = models.IntegerField(default=0)
    estado = models.CharField(max_length=20)
    codigo_resultado = models.CharField(max_length=80, null=True)
    discrepancia_detectada = models.BooleanField(default=False)
    detalle_payload = models.JSONField(null=True)
    recibido_en = models.DateTimeField()

    class Meta:
        managed = False
        db_table = 'operaciones_evento_sincronizacion'
