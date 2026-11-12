from . import credenciales, cuentas, doble_factor, politica_acceso, roles, sesion, sesiones
from .models import CuentaUsuario
from .respuestas import ErrorDePeticion, api, json_error, json_ok, leer_json
from .sesion import PERMISO_CONSOLA, requiere_permiso


def _administrador(admin):
    return {'id': admin.id, 'usuario': admin.usuario, 'nombre': admin.nombre, 'permisos': sorted(admin.permisos)}


def _respuesta_freno(freno):
    espera = '1 minuto' if freno.minutos == 1 else f'{freno.minutos} minutos'
    if freno.cuenta_bloqueada:
        respuesta = json_error('Por seguridad, esta cuenta se bloqueó tras varios intentos fallidos. Vuelve a '
                               f'probar en {espera} o pide a otra persona de administración que la desbloquee.',
                               423)
    else:
        respuesta = json_error(f'Hubo demasiados intentos. Espera {espera} y vuelve a probar.', 429)
    respuesta['Retry-After'] = str(freno.minutos * 60)
    return respuesta


def _respuesta_login(request, resultado):
    if isinstance(resultado, sesion.Freno):
        return _respuesta_freno(resultado)
    if isinstance(resultado, sesion.Error2FA):
        return json_error(resultado.mensaje, 401)
    if isinstance(resultado, sesion.Desafio2FA):
        carga = {
            'requiere2fa': True,
            'configurado': resultado.configurado,
            'desafio': resultado.desafio,
            'usuario': resultado.usuario,
        }
        if resultado.otpauth_uri:
            carga['otpauthUri'] = resultado.otpauth_uri
            carga['secretoBase32'] = resultado.secreto_base32
        return json_ok(carga, 202)
    if resultado is None:
        return json_error('Usuario o contraseña incorrectos.', 401)
    token, admin = resultado
    respuesta = json_ok(_administrador(admin))
    sesion.poner_cookie(respuesta, request, token)
    return respuesta


@api({'GET', 'POST', 'DELETE'})
def sesion_actual(request):
    if request.method == 'POST':
        datos = leer_json(request)
        codigo = datos.get('codigo') if datos.get('codigo') is not None else datos.get('totp')
        resultado = sesion.iniciar(datos.get('usuario'), datos.get('clave'), request.META.get('REMOTE_ADDR'), codigo)
        return _respuesta_login(request, resultado)
    if request.administrador is None:
        return sesion.sin_sesion(request)
    if request.method == 'DELETE':
        sesion.cerrar(request.administrador)
        respuesta = json_ok(None, 204)
        sesion.borrar_cookie(respuesta)
        return respuesta
    return json_ok(_administrador(request.administrador))


@api({'POST'})
def sesion_2fa(request):
    """HU-60: completa el segundo factor (activación de QR o código TOTP) para abrir la sesión WEB_ADMIN."""
    datos = leer_json(request)
    resultado = sesion.completar_2fa(datos.get('desafio'), datos.get('codigo'), request.META.get('REMOTE_ADDR'))
    return _respuesta_login(request, resultado)


@api({'POST'})
def docente_2fa_iniciar(request):
    """HU-60: entrega el QR inicial a un docente tras validar su contraseña (sin escribir desde Spring)."""
    datos = leer_json(request)
    resultado = sesion.iniciar_enrolamiento_docente(
        datos.get('usuario'), datos.get('clave'), request.META.get('REMOTE_ADDR')
    )
    if isinstance(resultado, sesion.Freno):
        return _respuesta_freno(resultado)
    if resultado is None:
        return json_error('Usuario o contraseña incorrectos.', 401)
    return json_ok(resultado)


@api({'POST'})
def docente_2fa_confirmar(request):
    """HU-60: valida el primer token TOTP del docente y deja activo su doble factor."""
    datos = leer_json(request)
    cuenta_id = doble_factor.leer_desafio(datos.get('desafio'), 'docente_enrolar')
    cuenta_obj = CuentaUsuario.objects.filter(id=cuenta_id).first()
    if cuenta_obj is None:
        return json_error('No encontramos la cuenta.', 404)
    ok, _ = doble_factor.confirmar_enrolamiento(cuenta_obj, datos.get('codigo'), actor=cuenta_obj)
    if not ok:
        return json_error('El código de 6 dígitos es incorrecto o ya venció.', 401)
    return json_ok({'verificado': True, 'usuario': cuenta_obj.nombre_usuario})


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


@api({'GET'})
@requiere_permiso(PERMISO_CONSOLA)
def seguridad_2fa(request):
    """HU-60: estado del doble factor propio y de las cuentas obligatorias (admin/soporte/docente)."""
    admin = request.administrador
    puede_gestionar = admin.puede('seguridad.2fa')
    puede_ver_lista = puede_gestionar or admin.puede('cuentas.ver')
    return json_ok({
        'propio': doble_factor.resumen_propio(admin),
        'puedeGestionar': puede_gestionar,
        'cuentas': doble_factor.listar_cuentas_obligatorias() if puede_ver_lista else [],
    })


@api({'POST'})
@requiere_permiso(PERMISO_CONSOLA)
def seguridad_2fa_rotar(request):
    return json_ok(doble_factor.rotar_propio(request.administrador))


@api({'POST'})
@requiere_permiso(PERMISO_CONSOLA)
def seguridad_2fa_confirmar(request):
    datos = leer_json(request)
    return json_ok(doble_factor.confirmar_propio(request.administrador, datos.get('codigo')))


@api({'POST'})
@requiere_permiso('seguridad.2fa')
def seguridad_2fa_reiniciar(request, cuenta_id):
    return json_ok(doble_factor.reiniciar_cuenta(cuenta_id, request.administrador))
