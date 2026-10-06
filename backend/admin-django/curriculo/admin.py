from django.contrib import admin
from .models import (
    VersionCatalogo,
    Grado,
    Area,
    Competencia,
    Unidad,
    ContextoCultural,
    ContextoCulturalDato,
)

@admin.register(VersionCatalogo)
class VersionCatalogoAdmin(admin.ModelAdmin):
    list_display = ('etiqueta_version', 'estado', 'vigente_desde')
    list_filter = ('estado',)
    search_fields = ('etiqueta_version', 'referencia_origen')


@admin.register(Grado)
class GradoAdmin(admin.ModelAdmin):
    list_display = ('numero_grado', 'nombre')
    ordering = ('numero_grado',)


@admin.register(Area)
class AreaAdmin(admin.ModelAdmin):
    list_display = ('codigo', 'nombre', 'version_catalogo')
    list_filter = ('version_catalogo',)
    search_fields = ('codigo', 'nombre')


@admin.register(Competencia)
class CompetenciaAdmin(admin.ModelAdmin):
    list_display = ('codigo', 'nombre', 'area')
    list_filter = ('area',)
    search_fields = ('codigo', 'nombre')


@admin.register(Unidad)
class UnidadAdmin(admin.ModelAdmin):
    list_display = ('codigo', 'titulo', 'grado', 'area', 'numero_secuencia')
    list_filter = ('grado', 'area', 'version_catalogo')
    search_fields = ('codigo', 'titulo')


class ContextoCulturalDatoInline(admin.TabularInline):
    model = ContextoCulturalDato
    extra = 1
    fields = ('orden', 'parada_secuencia', 'titulo', 'icono', 'estado', 'fuente', 'contenido')


@admin.register(ContextoCultural)
class ContextoCulturalAdmin(admin.ModelAdmin):
    list_display = ('codigo', 'ciudad', 'lugar', 'titulo', 'credito_licencia')
    search_fields = ('codigo', 'ciudad', 'lugar', 'titulo')
    inlines = [ContextoCulturalDatoInline]


@admin.register(ContextoCulturalDato)
class ContextoCulturalDatoAdmin(admin.ModelAdmin):
    list_display = ('titulo', 'contexto_cultural', 'parada_secuencia', 'icono', 'estado', 'orden')
    list_filter = ('estado', 'contexto_cultural', 'parada_secuencia')
    search_fields = ('titulo', 'contenido', 'fuente')
