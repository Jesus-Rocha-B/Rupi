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


class Incidente(models.Model):
    """operaciones_incidente: Registro de caídas y alertas de infraestructura (HU-47)."""
    ESTADOS = (
        ('ABIERTO', 'Abierto'),
        ('EN_ATENCION', 'En atención'),
        ('RESUELTO', 'Resuelto'),
        ('CERRADO', 'Cerrado'),
    )
    SEVERIDADES = (
        ('BAJA', 'Baja'),
        ('MEDIA', 'Media'),
        ('ALTA', 'Alta'),
        ('CRITICA', 'Crítica'),
    )
    CATEGORIAS = (
        ('DISPONIBILIDAD', 'Disponibilidad'),
        ('RENDIMIENTO', 'Rendimiento'),
        ('SINCRONIZACION', 'Sincronización'),
        ('IA', 'Inteligencia Artificial'),
        ('BASE_DATOS', 'Base de Datos'),
        ('OTRO', 'Otro'),
    )

    id = models.CharField(max_length=36, primary_key=True)
    servicio = models.CharField(max_length=80)
    categoria = models.CharField(max_length=30, choices=CATEGORIAS)
    severidad = models.CharField(max_length=20, choices=SEVERIDADES)
    estado = models.CharField(max_length=20, choices=ESTADOS, default='ABIERTO')
    resumen = models.CharField(max_length=500)
    referencia_externa = models.CharField(max_length=255, null=True, blank=True)
    iniciado_en = models.DateTimeField()
    resuelto_en = models.DateTimeField(null=True, blank=True)

    class Meta:
        managed = False
        db_table = 'operaciones_incidente'


class HistorialCaida(models.Model):
    """operaciones_historial_caida: Registro histórico de interrupciones y caídas del servidor principal (HU-48)."""
    TIPOS = (
        ('CAIDA', 'Caída'),
        ('DEGRADACION', 'Degradación'),
        ('RECUPERACION', 'Recuperación'),
    )

    id = models.CharField(max_length=36, primary_key=True)
    servidor = models.CharField(max_length=80, default='servidor_principal')
    tipo_evento = models.CharField(max_length=20, choices=TIPOS, default='CAIDA')
    iniciado_en = models.DateTimeField()
    restablecido_en = models.DateTimeField(null=True, blank=True)
    duracion_segundos = models.PositiveIntegerField(null=True, blank=True)
    motivo = models.CharField(max_length=500)
    codigo_error = models.CharField(max_length=80, null=True, blank=True)
    impacto_colegios = models.CharField(max_length=255, null=True, blank=True)
    registrado_en = models.DateTimeField()

    class Meta:
        managed = False
        db_table = 'operaciones_historial_caida'
