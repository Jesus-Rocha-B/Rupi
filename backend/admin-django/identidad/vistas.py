from . import cuentas, sesion
from .respuestas import ErrorDePeticion, api, json_error, json_ok, leer_json
from .sesion import requiere_permiso


def _administrador(admin):
    return {'id': admin.id, 'usuario': admin.usuario, 'nombre': admin.nombre, 'permisos': sorted(admin.permisos)}


@api({'GET', 'POST', 'DELETE'})
def sesion_actual(request):
    if request.method == 'POST':
        datos = leer_json(request)
        resultado = sesion.iniciar(datos.get('usuario'), datos.get('clave'), request.META.get('REMOTE_ADDR'))
        if resultado == 'LIMITE':
            return json_error('Hubo demasiados intentos. Espera cinco minutos y vuelve a probar.', 429)
        if resultado is None:
            return json_error('Usuario o contraseña incorrectos.', 401)
        token, admin = resultado
        respuesta = json_ok(_administrador(admin))
        sesion.poner_cookie(respuesta, request, token)
        return respuesta
    if request.administrador is None:
        return json_error('No hay una sesión de administración abierta.', 401)
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
    return json_ok(cuentas.listar(g.get('q'), g.get('rol'), g.get('estado'), _pagina(g.get('pagina'))))


@api({'GET', 'PATCH'})
@requiere_permiso('cuentas.ver')
def cuenta(request, cuenta_id):
    if request.method == 'PATCH':
        if not request.administrador.puede('cuentas.gestionar'):
            return json_error('Tu rol no tiene permiso para modificar cuentas.', 403)
        return json_ok(cuentas.modificar(cuenta_id, leer_json(request), request.administrador))
    return json_ok(cuentas.detalle(cuenta_id))


@api({'POST'})
@requiere_permiso('cuentas.gestionar')
def baja(request, cuenta_id):
    return json_ok(cuentas.dar_de_baja(cuenta_id, request.administrador))


@api({'POST'})
@requiere_permiso('cuentas.gestionar')
def reactivacion(request, cuenta_id):
    return json_ok(cuentas.reactivar(cuenta_id, request.administrador))
