"""Tablas identidad_* del esquema compartido (database/mysql/schema.sql).

Son modelos no gestionados: Django las lee y escribe, pero nunca las crea ni las altera. Las fechas se
guardan siempre desde Python en UTC; los DEFAULT CURRENT_TIMESTAMP de MySQL usan la hora del servidor.
"""
import uuid

from django.db import models


def nuevo_id():
    return str(uuid.uuid4())


class CuentaUsuario(models.Model):
    ESTADOS = [
        ('INVITADO', 'Invitado'),
        ('ACTIVO', 'Activo'),
        ('BLOQUEADO', 'Bloqueado'),
        ('DESHABILITADO', 'Dado de baja'),
    ]

    id = models.CharField(primary_key=True, max_length=36, default=nuevo_id)
    nombre_usuario = models.CharField(max_length=80, unique=True)
    correo = models.CharField(max_length=254, null=True, blank=True, unique=True)
    hash_clave = models.CharField(max_length=255)
    estado = models.CharField(max_length=20, choices=ESTADOS, default='INVITADO')
    bloqueado_hasta = models.DateTimeField(null=True, blank=True)
    clave_cambiada_en = models.DateTimeField()
    creado_en = models.DateTimeField()
    actualizado_en = models.DateTimeField()
    desactivado_en = models.DateTimeField(null=True, blank=True)

    class Meta:
        managed = False
        db_table = 'identidad_cuenta_usuario'

    def __str__(self):
        return self.nombre_usuario


class PerfilUsuario(models.Model):
    usuario = models.OneToOneField(CuentaUsuario, primary_key=True, on_delete=models.DO_NOTHING,
                                   db_column='usuario_id', related_name='perfil')
    nombre = models.CharField(max_length=100, null=True, blank=True)
    apellido = models.CharField(max_length=100, null=True, blank=True)
    nombre_preferido = models.CharField(max_length=100, null=True, blank=True)
    alias_publico = models.CharField(max_length=40, null=True, blank=True)
    mostrar_en_clasificacion = models.BooleanField(default=False)
    idioma = models.CharField(max_length=12, default='es-PE')
    zona_horaria = models.CharField(max_length=64, default='America/Lima')

    class Meta:
        managed = False
        db_table = 'identidad_perfil_usuario'


class Rol(models.Model):
    id = models.CharField(primary_key=True, max_length=36, default=nuevo_id)
    codigo = models.CharField(max_length=40, unique=True)
    nombre = models.CharField(max_length=100)
    descripcion = models.CharField(max_length=255, null=True, blank=True)

    class Meta:
        managed = False
        db_table = 'identidad_rol'

    def __str__(self):
        return self.codigo


class Permiso(models.Model):
    id = models.CharField(primary_key=True, max_length=36, default=nuevo_id)
    codigo = models.CharField(max_length=80, unique=True)
    descripcion = models.CharField(max_length=255, null=True, blank=True)

    class Meta:
        managed = False
        db_table = 'identidad_permiso'


class UsuarioRol(models.Model):
    pk = models.CompositePrimaryKey('usuario_id', 'rol_id')
    usuario = models.ForeignKey(CuentaUsuario, on_delete=models.DO_NOTHING, db_column='usuario_id',
                                related_name='roles_asignados')
    rol = models.ForeignKey(Rol, on_delete=models.DO_NOTHING, db_column='rol_id')
    asignado_por = models.ForeignKey(CuentaUsuario, on_delete=models.DO_NOTHING, db_column='asignado_por',
                                     null=True, related_name='+')
    asignado_en = models.DateTimeField()

    class Meta:
        managed = False
        db_table = 'identidad_usuario_rol'


class RolPermiso(models.Model):
    pk = models.CompositePrimaryKey('rol_id', 'permiso_id')
    rol = models.ForeignKey(Rol, on_delete=models.DO_NOTHING, db_column='rol_id', related_name='permisos')
    permiso = models.ForeignKey(Permiso, on_delete=models.DO_NOTHING, db_column='permiso_id')

    class Meta:
        managed = False
        db_table = 'identidad_rol_permiso'


class SesionUsuario(models.Model):
    """Una sola sesión activa por cuenta: la columna generada usuario_activo_id es UNIQUE en MySQL."""
    CLIENTES = [('WEB_ADMIN', 'Consola de administración'), ('WEB_USUARIO', 'Web'), ('MOVIL', 'Celular')]

    id = models.CharField(primary_key=True, max_length=36, default=nuevo_id)
    usuario = models.ForeignKey(CuentaUsuario, on_delete=models.DO_NOTHING, db_column='usuario_id',
                                related_name='sesiones')
    token_hash = models.BinaryField(max_length=32)
    revocada_en = models.DateTimeField(null=True, blank=True)
    cliente = models.CharField(max_length=20, choices=CLIENTES)
    creada_en = models.DateTimeField()
    actividad_en = models.DateTimeField()
    expira_en = models.DateTimeField()

    class Meta:
        managed = False
        db_table = 'identidad_sesion_usuario'


class IntentoAcceso(models.Model):
    id = models.BigAutoField(primary_key=True)
    usuario = models.ForeignKey(CuentaUsuario, on_delete=models.DO_NOTHING, db_column='usuario_id',
                                null=True, related_name='+')
    identificador_hash = models.BinaryField(max_length=32)
    resultado = models.CharField(max_length=20)
    origen_hash = models.BinaryField(max_length=32, null=True)
    ocurrido_en = models.DateTimeField()

    class Meta:
        managed = False
        db_table = 'identidad_intento_acceso'
