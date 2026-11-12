from . import credenciales, cuentas, politica_acceso, roles, sesion, sesiones
from .respuestas import ErrorDePeticion, api, json_error, json_ok, leer_json
from .sesion import requiere_permiso


def _administrador(admin):
    return {'id': admin.id, 'usuario': admin.usuario, 'nombre': admin.nombre, 'permisos': sorted(admin.permisos)}


@api({'GET', 'POST', 'DELETE'})
def sesion_actual(request):
    if request.method == 'POST':
        datos = leer_json(request)
        resultado = sesion.iniciar(datos.get('usuario'), datos.get('clave'), request.META.get('REMOTE_ADDR'))
        if isinstance(resultado, sesion.Freno):
            espera = '1 minuto' if resultado.minutos == 1 else f'{resultado.minutos} minutos'
            if resultado.cuenta_bloqueada:
                respuesta = json_error('Por seguridad, esta cuenta se bloqueó tras varios intentos fallidos. Vuelve a '
                                       f'probar en {espera} o pide a otra persona de administración que la desbloquee.',
                                       423)
            else:
                respuesta = json_error(f'Hubo demasiados intentos. Espera {espera} y vuelve a probar.', 429)
            respuesta['Retry-After'] = str(resultado.minutos * 60)
            return respuesta
        if resultado is None:
            return json_error('Usuario o contraseña incorrectos.', 401)
        token, admin = resultado
        respuesta = json_ok(_administrador(admin))
        sesion.poner_cookie(respuesta, request, token)
        return respuesta
    if request.administrador is None:
        return sesion.sin_sesion(request)
    if request.method == 'DELETE':
        sesion.cerrar(request.administrador)
        respuesta = json_ok(None, 204)
        sesion.borrar_cookie(respuesta)
        return respuesta
    return json_ok(_administrador(request.administrador))


def _pagina(valor):
    try:
        return int(valor or 1)
    except ValueError:
        raise ErrorDePeticion('La página debe ser un número.')


@api({'GET', 'POST'})
@requiere_permiso('cuentas.ver')
def lista_cuentas(request):
    if request.method == 'POST':
        if not request.administrador.puede('cuentas.gestionar'):
            return json_error('Tu rol no tiene permiso para crear cuentas.', 403)
        return json_ok(cuentas.crear(leer_json(request), request.administrador), 201)
    g = request.GET
    return json_ok(cuentas.listar(g.get('q'), g.get('rol'), g.get('estado'), _pagina(g.get('pagina')),
                                  cuentas.ve_datos(request.administrador)))


@api({'GET', 'PATCH'})
@requiere_permiso('cuentas.ver')
def cuenta(request, cuenta_id):
    if request.method == 'PATCH':
        if not request.administrador.puede('cuentas.gestionar'):
            return json_error('Tu rol no tiene permiso para modificar cuentas.', 403)
        return json_ok(cuentas.modificar(cuenta_id, leer_json(request), request.administrador))
    return json_ok(cuentas.detalle(cuenta_id, cuentas.ve_datos(request.administrador)))


@api({'POST'})
@requiere_permiso('cuentas.gestionar')
def baja(request, cuenta_id):
    return json_ok(cuentas.dar_de_baja(cuenta_id, request.administrador))


@api({'POST'})
@requiere_permiso('cuentas.gestionar')
def reactivacion(request, cuenta_id):
    return json_ok(cuentas.reactivar(cuenta_id, request.administrador))


@api({'GET'})
@requiere_permiso('cuentas.ver')
def catalogo_roles(request):
    return json_ok(roles.catalogo())


@api({'PUT'})
@requiere_permiso('roles.asignar')
def roles_de_cuenta(request, cuenta_id):
    datos = leer_json(request)
    return json_ok(roles.asignar(cuenta_id, datos.get('roles'), request.administrador,
                                 cuentas.ve_datos(request.administrador)))


@api({'GET'})
@requiere_permiso('sesiones.gestionar')
def sesiones_activas(request):
    return json_ok(sesiones.activas(request.GET.get('cliente')))


@api({'POST'})
@requiere_permiso('sesiones.gestionar')
def cerrar_sesion_de_cuenta(request, cuenta_id):
    return json_ok(sesiones.cerrar(cuenta_id, request.administrador, cuentas.ve_datos(request.administrador)))


@api({'POST'})
@requiere_permiso('credenciales.restablecer')
def clave_de_cuenta(request, cuenta_id):
    return json_ok(credenciales.restablecer_clave(cuenta_id, leer_json(request), request.administrador,
                                                  cuentas.ve_datos(request.administrador)))


@api({'POST'})
@requiere_permiso('credenciales.restablecer')
def bloqueo(request, cuenta_id):
    return json_ok(credenciales.bloquear(cuenta_id, request.administrador, cuentas.ve_datos(request.administrador)))


@api({'POST'})
@requiere_permiso('credenciales.restablecer')
def desbloqueo(request, cuenta_id):
    return json_ok(credenciales.desbloquear(cuenta_id, request.administrador, cuentas.ve_datos(request.administrador)))


@api({'GET', 'PUT'})
@requiere_permiso('acceso.ver')
def politica_de_acceso(request):
    """HU-58: límite de intentos y bloqueo automático para la web, el celular y la consola."""
    if request.method == 'PUT':
        if not request.administrador.puede('acceso.configurar'):
            return json_error('Tu rol solo puede consultar la política de acceso.', 403)
        return json_ok(politica_acceso.actualizar(leer_json(request), request.administrador))
    return json_ok(politica_acceso.detalle())
