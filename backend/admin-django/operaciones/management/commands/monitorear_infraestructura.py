import time
from django.core.management.base import BaseCommand
from operaciones.monitoreo import ejecutar_ciclo_monitoreo


class Command(BaseCommand):
    help = 'Monitoreo continuo de infraestructura con alertas tempranas y autoreinicio (HU-47)'

    def add_arguments(self, parser):
        parser.add_argument(
            '--intervalo',
            type=int,
            default=15,
            help='Intervalo en segundos entre cada sondeo continuo (por defecto: 15s)',
        )
        parser.add_argument(
            '--un-ciclo',
            action='store_true',
            help='Ejecuta un solo ciclo de diagnóstico y finaliza',
        )
        parser.add_argument(
            '--sin-autoreinicio',
            action='store_true',
            help='Desactiva la ejecución automática del script de recuperación de emergencia',
        )

    def handle(self, *args, **options):
        intervalo = options['intervalo']
        un_ciclo = options['un_ciclo']
        auto_recuperar = not options['sin_autoreinicio']

        self.stdout.write(self.style.SUCCESS(
            f'Iniciando monitoreo de infraestructura (intervalo: {intervalo}s, autoreinicio: {auto_recuperar})...'
        ))

        while True:
            try:
                res = ejecutar_ciclo_monitoreo(intentar_autoreinicio=auto_recuperar)
                estado = res.get('estadoGeneral', 'DESCONOCIDO')
                color = self.style.SUCCESS if estado == 'OK' else self.style.WARNING if estado == 'ATENCION' else self.style.ERROR
                self.stdout.write(color(
                    f"[{res.get('revisadoEn')}] Estado General: {estado} | Incidentes abiertos: {res.get('totalIncidentesAbiertos')}"
                ))

                for nov in res.get('novedades', []):
                    self.stdout.write(self.style.NOTICE(f"  -> {nov.get('servicio')}: {nov.get('evento')}"))

                for acc in res.get('accionesTomadas', []):
                    self.stdout.write(self.style.WARNING(f"  [MEDIDA CORRECTIVA] {acc.get('servicio')}: {acc.get('detalle')}"))

            except Exception as e:
                self.stdout.write(self.style.ERROR(f'Error en ciclo de monitoreo: {e}'))

            if un_ciclo:
                break
            time.sleep(intervalo)
