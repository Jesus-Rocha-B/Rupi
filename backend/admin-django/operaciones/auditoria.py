from django.utils import timezone

from .models import EventoAuditoria


def registrar(actor_id, accion, entidad_id, resultado='CORRECTO', entidad_tipo='CUENTA_USUARIO'):
    EventoAuditoria.objects.create(
        actor_usuario_id=actor_id,
        dominio='identidad',
        accion=accion,
        entidad_tipo=entidad_tipo,
        entidad_id=entidad_id,
        resultado=resultado,
        ocurrido_en=timezone.now(),
    )


def historial(entidad_id, limite=12):
    return list(EventoAuditoria.objects.filter(entidad_id=entidad_id).order_by('-ocurrido_en', '-id')[:limite])
