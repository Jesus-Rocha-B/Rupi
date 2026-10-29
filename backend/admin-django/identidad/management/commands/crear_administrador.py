"""Crea la primera cuenta de administración (o le da el rol a una cuenta existente).

La consola no puede crear administradores si todavía no hay ninguno; este comando se ejecuta una vez por
entorno desde el servidor. La clave se pide sin mostrarla o se lee de la entrada estándar con --clave-stdin.
"""
import getpass
import sys

from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from django.utils import timezone

from identidad import claves
from identidad.cuentas import normalizar_usuario
from identidad.models import CuentaUsuario, PerfilUsuario, Rol, UsuarioRol
from identidad.respuestas import ErrorDePeticion
from identidad.sesion import ROL_ADMINISTRADOR
from operaciones import auditoria


class Command(BaseCommand):
    help = 'Crea una cuenta de administración de Rupi o da ese rol a una cuenta existente.'

    def add_arguments(self, parser):
        parser.add_argument('--usuario', required=True)
        parser.add_argument('--nombre', default='Administración')
        parser.add_argument('--apellido', default='Rupi')
        parser.add_argument('--clave-stdin', action='store_true', help='Lee la contraseña de la entrada estándar.')

    def handle(self, *args, **opciones):
        try:
            usuario = normalizar_usuario(opciones['usuario'])
        except ErrorDePeticion as error:
            raise CommandError(error.mensaje)
        rol = Rol.objects.filter(codigo=ROL_ADMINISTRADOR).first()
        if rol is None:
            raise CommandError('Falta el rol ADMINISTRADOR: aplica database/mysql/migrations/V31 y V32.')

        cuenta = CuentaUsuario.objects.filter(nombre_usuario=usuario).first()
        ahora = timezone.now()
        with transaction.atomic():
            if cuenta is None:
                clave = self._leer_clave(opciones['clave_stdin'], usuario)
                cuenta = CuentaUsuario.objects.create(nombre_usuario=usuario, hash_clave=claves.cifrar(clave),
                                                      estado='ACTIVO', clave_cambiada_en=ahora, creado_en=ahora,
                                                      actualizado_en=ahora)
                PerfilUsuario.objects.create(usuario=cuenta, nombre=opciones['nombre'], apellido=opciones['apellido'])
                auditoria.registrar(None, 'CUENTA_CREADA', cuenta.id)
                self.stdout.write(f'Cuenta {usuario} creada.')
            if UsuarioRol.objects.filter(usuario_id=cuenta.id, rol=rol).exists():
                self.stdout.write(f'{usuario} ya tiene el rol de administración.')
                return
            if UsuarioRol.objects.filter(usuario_id=cuenta.id, rol__codigo='ESTUDIANTE').exists():
                raise CommandError('Una cuenta de estudiante no puede administrar la plataforma.')
            UsuarioRol.objects.create(usuario=cuenta, rol=rol, asignado_por=None, asignado_en=ahora)
            auditoria.registrar(None, f'ROL_ASIGNADO:{ROL_ADMINISTRADOR}', cuenta.id)
        self.stdout.write(self.style.SUCCESS(f'{usuario} ahora puede ingresar a la consola de administración.'))

    def _leer_clave(self, desde_stdin, usuario):
        if desde_stdin:
            clave = sys.stdin.readline().rstrip('\n')
        else:
            clave = getpass.getpass('Contraseña: ')
            if clave != getpass.getpass('Repite la contraseña: '):
                raise CommandError('Las contraseñas no coinciden.')
        problema = claves.problema_clave_nueva(clave, usuario)
        if problema:
            raise CommandError(problema)
        return clave
