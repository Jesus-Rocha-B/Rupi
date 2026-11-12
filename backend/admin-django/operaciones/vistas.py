import secrets
from django.conf import settings
from identidad.respuestas import ErrorDePeticion, api, json_ok, leer_json
from identidad.sesion import requiere_permiso

from . import (
    autoreinicio,
    incidentes,
    monitoreo,
    notificaciones_alertas,
    respaldos,
    salud,
    sincronizacion,
    tablero_uptime,
)


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
        cliente_tipo = str(datos.get('clienteTipo', 'WEB')).upper()
        if cliente_tipo not in ('WEB', 'MOVIL', 'SISTEMA'):
            cliente_tipo = 'WEB'
        tipo_evento = str(datos.get('tipoEvento', 'AVANCE_LECCION')).upper()
        try:
            puntaje_xp = int(datos.get('puntajeXp', 0))
            lecciones = int(datos.get('leccionesCompletadas', 0))
        except (TypeError, ValueError):
            raise ErrorDePeticion('puntajeXp y leccionesCompletadas deben ser valores numéricos enteros.', 400)
        estado = str(datos.get('estado', 'APLICADO')).upper()
        if estado not in ('APLICADO', 'DESCARTADO', 'CONFLICTO'):
            estado = 'APLICADO'
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
    try:
        limite = max(1, min(200, int(request.GET.get('limite', 50))))
    except (TypeError, ValueError):
        limite = 50

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


@api({'POST'}, exigir_origen=False)
def hook_evento_externo(request):
    """Hook ligero para que scripts watchdog locales (ej. Monitoreo-Continuo.ps1) reporten caídas."""
    remoto = request.META.get('REMOTE_ADDR')
    ips_locales = {'127.0.0.1', 'localhost', '::1', '::ffff:127.0.0.1'}
    token_recibido = request.headers.get('X-Rupi-Monitor-Token')
    token_esperado = getattr(settings, 'RUPI_MONITOR_TOKEN', '')

    es_loopback = remoto in ips_locales
    token_valido = bool(token_esperado and token_recibido and secrets.compare_digest(token_recibido, token_esperado))

    if not (es_loopback or token_valido):
        raise ErrorDePeticion('Acceso no permitido.', 403)

    datos = leer_json(request)
    servicio = str(datos.get('servicio', 'desconocido')).strip().lower()[:80]
    estado = str(datos.get('estado', 'CAIDO')).strip().upper()
    detalle = str(datos.get('detalle', ''))[:500]

    servicios_permitidos = {'mysql', 'base', 'ia', 'api', 'servidor_principal'}
    if servicio not in servicios_permitidos:
        raise ErrorDePeticion(f'Servicio no válido. Permitidos: {", ".join(sorted(servicios_permitidos))}', 400)

    if estado not in ('CAIDO', 'OK'):
        raise ErrorDePeticion('El estado debe ser CAIDO u OK.', 400)

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


# =====================================================================
# Tablero Centralizado de Arquitectura, Uptime y Errores (HU-48)
# =====================================================================

@api({'GET'})
@requiere_permiso('plataforma.monitorear')
def tablero_arquitectura(request):
    """Retorna las métricas de uptime (24h, 7d, 30d), conteo de errores, historial de caídas y series temporales."""
    datos = tablero_uptime.recopilar_metricas_uptime()
    return json_ok(datos)


@api({'POST'})
@requiere_permiso('plataforma.monitorear')
def registrar_caida_servidor_manual(request):
    """Permite al administrador registrar manualmente una interrupción del servidor principal."""
    datos = leer_json(request) if request.body else {}
    servidor = str(datos.get('servidor', 'servidor_principal')).strip()[:80] or 'servidor_principal'
    tipo_evento = str(datos.get('tipoEvento', 'CAIDA')).strip().upper()
    if tipo_evento not in ('CAIDA', 'DEGRADACION', 'RECUPERACION'):
        tipo_evento = 'CAIDA'

    motivo = str(datos.get('motivo', 'Interrupción reportada manualmente')).strip()[:500]
    codigo_error = str(datos.get('codigoError', '')).strip()[:80] or None
    impacto = str(datos.get('impactoColegios', '')).strip()[:255] or None

    duracion = None
    if datos.get('duracionSegundos') is not None:
        try:
            duracion = max(0, int(datos.get('duracionSegundos')))
        except (TypeError, ValueError):
            duracion = None

    actor_id = getattr(request.administrador, 'id', None)

    res = tablero_uptime.registrar_evento_caida(
        servidor=servidor,
        tipo_evento=tipo_evento,
        motivo=motivo,
        codigo_error=codigo_error,
        impacto_colegios=impacto,
        duracion_segundos=duracion,
        actor_id=actor_id,
    )
    return json_ok(res, estado=201)


# =====================================================================
# Respaldos Automáticos Nocturnos y Almacenamiento Externo (HU-57)
# =====================================================================

@api({'GET'})
@requiere_permiso('respaldos.ver')
def estado_respaldos(request):
    """Retorna la programación nocturna, guía de tarea del servidor, canales de alerta e historial."""
    try:
        limite = max(1, min(100, int(request.GET.get('limite', 20))))
    except (TypeError, ValueError):
        limite = 20
    return json_ok(respaldos.estado_respaldos(limite=limite))


@api({'PUT'})
@requiere_permiso('respaldos.gestionar')
def actualizar_politica_respaldos(request):
    """Guarda horario de madrugada, retención, almacenamiento externo y alertas por fallo (HU-57)."""
    datos = leer_json(request)
    actor_id = getattr(request.administrador, 'id', None)
    return json_ok(respaldos.actualizar_politica(datos, actor_id=actor_id))


@api({'POST'})
@requiere_permiso('respaldos.gestionar')
def ejecutar_respaldo_manual(request):
    """Dispara un respaldo completo inmediato hacia el almacenamiento externo configurado."""
    actor_id = getattr(request.administrador, 'id', None)
    resultado = respaldos.ejecutar_respaldo(origen='MANUAL', actor_id=actor_id)
    estado_http = 201 if resultado.get('estado') in ('CORRECTO', 'VERIFICADO') else 500
    return json_ok(resultado, estado=estado_http)


@api({'POST'})
@requiere_permiso('respaldos.gestionar')
def probar_alerta_respaldo(request):
    """Envía una notificación simulada de fallo nocturno al administrador para verificar alertas."""
    nombre_actor = getattr(request.administrador, 'nombre', 'Administrador')
    politica = respaldos.politica_vigente()
    resultado = notificaciones_alertas.probar_alerta_respaldo(
        actor_nombre=nombre_actor,
        email_destino=politica.email_alerta,
    )
    return json_ok(resultado)
