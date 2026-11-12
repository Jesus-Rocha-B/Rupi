"""Tarea programada del servidor para respaldar la base de datos en horario de madrugada (HU-57)."""
import sys

from django.core.management.base import BaseCommand

from operaciones import respaldos


class Command(BaseCommand):
    help = 'Ejecuta el respaldo automático nocturno de la base de datos principal hacia almacenamiento externo (HU-57)'

    def add_arguments(self, parser):
        parser.add_argument(
            '--forzar',
            action='store_true',
            help='Ejecuta el respaldo de inmediato sin esperar la ventana de madrugada configurada.',
        )
        parser.add_argument(
            '--verificar-ventana',
            action='store_true',
            help='Verifica que la hora actual (America/Lima) corresponda al horario nocturno programado.',
        )

    def handle(self, *args, **options):
        forzar = options['forzar']
        verificar = options['verificar_ventana']
        politica = respaldos.politica_vigente()

        if not forzar and (verificar or not politica.habilitado):
            ejecutar, motivo = respaldos.debe_ejecutarse_en_ciclo(politica)
            if not ejecutar:
                self.stdout.write(self.style.WARNING(f'Respaldo omitido: {motivo}'))
                return

        resultado = respaldos.ejecutar_respaldo(origen='PROGRAMADO')
        estado = resultado.get('estado')
        if estado in ('CORRECTO', 'VERIFICADO'):
            self.stdout.write(
                self.style.SUCCESS(
                    f"Respaldo {resultado['id']} ({estado}) guardado en {resultado.get('ubicacionReferencia')} "
                    f"({resultado.get('tamanoBytes')} bytes)."
                )
            )
            return

        self.stderr.write(
            self.style.ERROR(
                f"Fallo en respaldo {resultado.get('id')}: {resultado.get('detalleError')} "
                f"(alertaEnviada={resultado.get('alertaEnviada')})"
            )
        )
        sys.exit(1)
