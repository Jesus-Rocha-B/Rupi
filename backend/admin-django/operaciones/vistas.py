from identidad.respuestas import ErrorDePeticion, api, json_ok
from identidad.sesion import requiere_permiso

from . import salud, sincronizacion


@api({'GET'})
@requiere_permiso('plataforma.monitorear')
def salud_de_la_plataforma(request):
    return json_ok(salud.revisar())


@api({'GET', 'POST'})
@requiere_permiso('sincronizacion.auditar')
def auditoria_sincronizacion(request):
    if request.method == 'POST':
        datos = request.datos
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
