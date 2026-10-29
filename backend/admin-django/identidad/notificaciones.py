"""Envío de correos automáticos para recuperación de cuentas (HU-44 Subtarea 4).

Notifica a docentes, tutores o estudiantes cuando se restablece su contraseña o se
desbloquea su cuenta institucional, asegurando la continuidad del acceso escolar.
"""
import logging
from django.conf import settings
from django.core.mail import send_mail

logger = logging.getLogger(__name__)


def notificar_recuperacion_clave(destinatario: str | None, usuario: str, nombre: str | None, clave_temporal: str | None = None) -> bool:
    """Envía correo automático tras el restablecimiento de contraseña de la cuenta."""
    if not destinatario or '@' not in destinatario:
        return False
    asunto = 'Tu cuenta en RUPI ha sido actualizada'
    nombre_mostrar = nombre or usuario

    lineas = [
        f'Hola, {nombre_mostrar}:',
        '',
        'Te informamos que la contraseña de tu cuenta institucional en RUPI ha sido restablecida por la administración escolar.',
        f'Usuario: {usuario}',
    ]
    if clave_temporal:
        lineas.extend([
            f'Tu contraseña temporal de acceso es: {clave_temporal}',
            '',
            'Por seguridad, recuerda cambiarla al ingresar y no compartirla con nadie.',
        ])
    else:
        lineas.extend([
            'Se ha asignado una nueva contraseña institucional por el equipo de soporte.',
        ])
    lineas.extend([
        '',
        'Puedes ingresar a la plataforma en https://rupi.edu.pe o desde la app móvil.',
        '',
        'Atentamente,',
        'Equipo de Plataforma Educativa RUPI',
    ])
    cuerpo = '\n'.join(lineas)

    try:
        send_mail(
            asunto,
            cuerpo,
            settings.DEFAULT_FROM_EMAIL,
            [destinatario],
            fail_silently=False,
        )
        return True
    except Exception as e:
        logger.warning('No se pudo enviar el correo de recuperación a %s: %s', destinatario, e)
        return False


def notificar_cuenta_desbloqueada(destinatario: str | None, usuario: str, nombre: str | None) -> bool:
    """Envía correo automático tras el desbloqueo de la cuenta institucional."""
    if not destinatario or '@' not in destinatario:
        return False
    asunto = 'Tu cuenta en RUPI ha sido desbloqueada'
    nombre_mostrar = nombre or usuario

    lineas = [
        f'Hola, {nombre_mostrar}:',
        '',
        'Te informamos que tu cuenta institucional en RUPI ha sido desbloqueada con éxito.',
        f'Usuario: {usuario}',
        '',
        'Ya puedes volver a iniciar sesión con tu contraseña habitual en la plataforma y continuar con tus clases.',
        '',
        'Atentamente,',
        'Equipo de Plataforma Educativa RUPI',
    ]
    cuerpo = '\n'.join(lineas)

    try:
        send_mail(
            asunto,
            cuerpo,
            settings.DEFAULT_FROM_EMAIL,
            [destinatario],
            fail_silently=False,
        )
        return True
    except Exception as e:
        logger.warning('No se pudo enviar el correo de desbloqueo a %s: %s', destinatario, e)
        return False
