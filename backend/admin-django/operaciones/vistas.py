from django.conf import settings
from identidad.respuestas import ErrorDePeticion, api, json_ok, leer_json
from identidad.sesion import requiere_permiso

from . import autoreinicio, incidentes, monitoreo, notificaciones_alertas, salud, sincronizacion


@api({'GET'})
@requiere_permiso('plataforma.monitorear')
def salud_de_la_plataforma(request):
    return json_ok(salud.revisar())


@api({'GET', 'POST'})
@requiere_permiso('sincronizacion.auditar')
def auditoria_sincronizacion(request):
    if request.method == 'POST':
        datos = leer_json(request)
        usuario_id = datos.get('usuarioId')
        if not usuario_id:
            raise ErrorDePeticion('Falta usuarioId para registrar el evento de sincronización.')
        cliente_tipo = datos.get('clienteTipo', 'WEB')
        tipo_evento = datos.get('tipoEvento', 'AVANCE_LECCION')
        puntaje_xp = int(datos.get('puntajeXp', 0))
        lecciones = int(datos.get('leccionesCompletadas', 0))
        estado = datos.get('estado', 'APLICADO')
        codigo = datos.get('codigoResultado')
        detalle = datos.get('detalle')
        evento_id = sincronizacion.registrar_evento(
            usuario_id, cliente_tipo, tipo_evento, puntaje_xp, lecciones, estado, codigo, detalle
        )
        return json_ok({'id': evento_id, 'mensaje': 'Evento de sincronización registrado correctamente.'}, estado=201)

    return json_ok(sincronizacion.auditar_sincronizacion())


# =====================================================================
# Endpoints de Monitoreo Continuo, Incidentes y Alertas (HU-47)
# =====================================================================

@api({'GET'})
@requiere_permiso('plataforma.monitorear')
def lista_incidentes(request):
    """Retorna la lista de incidentes de infraestructura y la configuración de canales de alerta."""
    estado = request.GET.get('estado')
    limite = int(request.GET.get('limite', '50'))
    items = incidentes.listar_incidentes(estado=estado, limite=limite)
    abiertos = [i for i in items if i['estado'] in ('ABIERTO', 'EN_ATENCION')]

    destinatario_email = getattr(settings, 'RUPI_ALERTA_EMAIL_RESPONSABLE', 'infraestructura@rupi.edu.pe')
    destinatario_sms = getattr(settings, 'RUPI_ALERTA_SMS_NUMERO', '')
    webhook_sms = getattr(settings, 'RUPI_ALERTA_SMS_WEBHOOK', '')

    return json_ok({
        'incidentes': items,
        'total': len(items),
        'abiertos': len(abiertos),
        'canalesAlerta': {
            'emailResponsable': destinatario_email,
            'smsNumero': destinatario_sms or None,
            'webhookConfigurado': bool(webhook_sms),
            'autoreinicioHabilitado': getattr(settings, 'RUPI_AUTOREINICIO_HABILITADO', True),
        },
    })


@api({'POST'})
@requiere_permiso('plataforma.monitorear')
def resolver_incidente(request, incidente_id):
    """Marca un incidente de infraestructura como resuelto manualmente por el responsable."""
    datos = leer_json(request) if request.body else {}
    comentario = datos.get('comentario')
    actor_id = getattr(request.administrador, 'id', None)
    try:
        resultado = incidentes.resolver_incidente_manual(incidente_id, actor_id=actor_id, comentario=comentario)
        return json_ok(resultado)
    except ValueError as e:
        raise ErrorDePeticion(str(e), 404)


@api({'POST'})
@requiere_permiso('plataforma.monitorear')
def ejecutar_ciclo(request):
    """Ejecuta un ciclo de monitoreo continuo manual y aplica medidas correctivas si se detectan caídas."""
    actor_id = getattr(request.administrador, 'id', None)
    resultado = monitoreo.ejecutar_ciclo_monitoreo(intentar_autoreinicio=True, actor_id=actor_id)
    return json_ok(resultado)


@api({'POST'})
@requiere_permiso('plataforma.monitorear')
def probar_alertas(request):
    """Envía mensajes de prueba a los canales de alerta (Correo y SMS/Webhook) para verificación."""
    nombre_actor = getattr(request.administrador, 'nombre', 'Responsable de Infraestructura')
    resultado = notificaciones_alertas.probar_canales_alerta(actor_nombre=nombre_actor)
    return json_ok(resultado)


@api({'POST'})
@requiere_permiso('plataforma.monitorear')
def reiniciar_servicio_manual(request, servicio):
    """Permite al responsable forzar el reinicio inmediato de un servicio crítico caído."""
    actor_id = getattr(request.administrador, 'id', None)
    resultado = autoreinicio.ejecutar_reinicio_servicio(servicio, actor_id=actor_id, forzar=True)
    if not resultado.get('exito'):
        return json_ok(resultado, estado=500)
    return json_ok(resultado)


@api({'POST'})
def hook_evento_externo(request):
    """Hook ligero para que scripts watchdog locales (ej. Monitoreo-Continuo.ps1) reporten caídas."""
    remoto = request.META.get('REMOTE_ADDR')
    if remoto not in ('127.0.0.1', 'localhost', '::1'):
        raise ErrorDePeticion('Acceso no permitido.', 403)

    datos = leer_json(request)
    servicio = datos.get('servicio', 'desconocido')
    estado = datos.get('estado', 'CAIDO')
    detalle = datos.get('detalle', '')

    if estado == 'CAIDO':
        res = incidentes.registrar_caida(
            servicio=servicio,
            severidad='CRITICA',
            resumen=f"Alerta externa watchdog: {servicio} no responde.",
            detalle_error=detalle,
            intentar_autoreinicio=False,
        )
        return json_ok({'mensaje': 'Incidente registrado', 'incidente': res}, estado=201)
    elif estado == 'OK':
        res = incidentes.registrar_recuperacion(servicio=servicio)
        return json_ok({'mensaje': 'Recuperación registrada', 'incidente': res})

    return json_ok({'mensaje': 'Evento recibido sin acción requerida.'})
