import uuid
from django.db import models

class VersionCatalogo(models.Model):
    id = models.CharField(primary_key=True, max_length=36, default=uuid.uuid4)
    etiqueta_version = models.CharField(max_length=60, unique=True)
    referencia_origen = models.CharField(max_length=255, null=True, blank=True)
    vigente_desde = models.DateField(null=True, blank=True)
    estado = models.CharField(max_length=20, default='BORRADOR', choices=[
        ('BORRADOR', 'Borrador'),
        ('ACTIVO', 'Activo'),
        ('RETIRADO', 'Retirado'),
    ])
    creado_en = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'curriculo_version_catalogo'
        verbose_name = 'Versión de catálogo'
        verbose_name_plural = 'Versiones de catálogo'

    def __str__(self):
        return self.etiqueta_version


class Grado(models.Model):
    id = models.CharField(primary_key=True, max_length=36, default=uuid.uuid4)
    numero_grado = models.PositiveSmallIntegerField(unique=True)
    nombre = models.CharField(max_length=80)

    class Meta:
        db_table = 'curriculo_grado'
        ordering = ['numero_grado']

    def __str__(self):
        return self.nombre


class Area(models.Model):
    id = models.CharField(primary_key=True, max_length=36, default=uuid.uuid4)
    version_catalogo = models.ForeignKey(VersionCatalogo, on_delete=models.CASCADE, db_column='version_catalogo_id')
    codigo = models.CharField(max_length=40)
    nombre = models.CharField(max_length=120)

    class Meta:
        db_table = 'curriculo_area'
        unique_together = (('version_catalogo', 'codigo'),)

    def __str__(self):
        return f"{self.codigo} - {self.nombre}"


class Competencia(models.Model):
    id = models.CharField(primary_key=True, max_length=36, default=uuid.uuid4)
    area = models.ForeignKey(Area, on_delete=models.CASCADE, db_column='area_id')
    codigo = models.CharField(max_length=60)
    nombre = models.CharField(max_length=180)
    descripcion = models.TextField(null=True, blank=True)

    class Meta:
        db_table = 'curriculo_competencia'
        unique_together = (('area', 'codigo'),)

    def __str__(self):
        return f"{self.codigo}: {self.nombre}"


class Unidad(models.Model):
    id = models.CharField(primary_key=True, max_length=36, default=uuid.uuid4)
    version_catalogo = models.ForeignKey(VersionCatalogo, on_delete=models.CASCADE, db_column='version_catalogo_id')
    grado = models.ForeignKey(Grado, on_delete=models.CASCADE, db_column='grado_id')
    area = models.ForeignKey(Area, on_delete=models.CASCADE, db_column='area_id')
    codigo = models.CharField(max_length=60)
    titulo = models.CharField(max_length=180)
    numero_secuencia = models.PositiveSmallIntegerField(null=True, blank=True)

    class Meta:
        db_table = 'curriculo_unidad'
        unique_together = (('version_catalogo', 'grado', 'area', 'codigo'),)

    def __str__(self):
        return f"U{self.numero_secuencia or 0}: {self.titulo}"


class ContextoCultural(models.Model):
    id = models.CharField(primary_key=True, max_length=36, default=uuid.uuid4)
    ruta_id = models.CharField(max_length=36, db_index=True, help_text="ID de aprendizaje_ruta asociada")
    codigo = models.CharField(max_length=60, unique=True)
    ciudad = models.CharField(max_length=100)
    lugar = models.CharField(max_length=150)
    titulo = models.CharField(max_length=180)
    descripcion = models.TextField(null=True, blank=True)
    mensaje_animo = models.CharField(max_length=255)
    imagen_url = models.CharField(max_length=1000)
    imagen_alt = models.CharField(max_length=500)
    credito_autor = models.CharField(max_length=150)
    credito_fuente = models.CharField(max_length=150)
    credito_licencia = models.CharField(max_length=80)
    credito_url = models.CharField(max_length=1000)
    creado_en = models.DateTimeField(auto_now_add=True)
    actualizado_en = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'curriculo_contexto_cultural'
        verbose_name = 'Contexto cultural'
        verbose_name_plural = 'Contextos culturales'

    def __str__(self):
        return f"{self.ciudad} - {self.lugar} ({self.codigo})"


class ContextoCulturalDato(models.Model):
    id = models.CharField(primary_key=True, max_length=36, default=uuid.uuid4)
    contexto_cultural = models.ForeignKey(
        ContextoCultural,
        on_delete=models.CASCADE,
        related_name='datos',
        db_column='contexto_cultural_id'
    )
    parada_secuencia = models.PositiveSmallIntegerField(
        null=True,
        blank=True,
        help_text="Número de secuencia de la parada a la que se vincula (opcional)"
    )
    orden = models.PositiveSmallIntegerField(default=1)
    titulo = models.CharField(max_length=150)
    contenido = models.TextField(help_text="Dato curioso en lenguaje para primaria (6-8 años)")
    icono = models.CharField(
        max_length=60,
        help_text="Nombre del ícono SVG en Lucide (ej. landmark, palette, bell). ¡Cero emojis!"
    )
    fuente = models.CharField(max_length=255, help_text="Fuente verificada del dato")
    estado = models.CharField(max_length=20, default='BORRADOR', choices=[
        ('BORRADOR', 'Borrador'),
        ('PUBLICADO', 'Publicado'),
        ('RETIRADO', 'Retirado'),
    ])
    creado_en = models.DateTimeField(auto_now_add=True)
    actualizado_en = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'curriculo_contexto_cultural_dato'
        ordering = ['orden', 'parada_secuencia']
        verbose_name = 'Dato curioso cultural'
        verbose_name_plural = 'Datos curiosos culturales'

    def __str__(self):
        return f"[{self.estado}] {self.titulo} (Parada {self.parada_secuencia or 'General'})"
