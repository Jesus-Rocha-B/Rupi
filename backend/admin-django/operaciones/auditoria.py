from django.utils import timezone

from .models import EventoAuditoria


# EJEMPLO ORM: objects.create convierte los campos del modelo en una inserción SQL.
# Las HU curriculares y de moderación usan esta función para dejar constancia de las acciones.
def registrar(actor_id, accion, entidad_id, resultado='CORRECTO', entidad_tipo='CUENTA_USUARIO', dominio='identidad'):
    EventoAuditoria.objects.create(
        actor_usuario_id=actor_id,
        dominio=dominio,
        accion=accion,
        entidad_tipo=entidad_tipo,
        entidad_id=entidad_id,
        resultado=resultado,
        ocurrido_en=timezone.now(),
    )


def historial(entidad_id, limite=12):
    return list(EventoAuditoria.objects.filter(entidad_id=entidad_id).order_by('-ocurrido_en', '-id')[:limite])
