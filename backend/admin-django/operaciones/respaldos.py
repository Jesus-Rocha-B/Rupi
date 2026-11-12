"""Programación, ejecución, almacenamiento externo y alertas de respaldos automáticos (HU-57).

Django administra la política de respaldos nocturnos (`operaciones_politica_respaldo`) y el historial de copias
(`operaciones_respaldo`). Los volcados de la base principal se generan fuera de horario escolar (ventana de
madrugada 00:00–05:59 America/Lima), se comprimen y verifican con SHA-256 en almacenamiento externo seguro y, ante
cualquier falla, notifican al administrador y registran un incidente en `operaciones_incidente`.
"""
import gzip
import hashlib
import logging
import os
import re
import shlex
import shutil
import subprocess
import tempfile
import urllib.error
import urllib.parse
import urllib.request
import uuid
from dataclasses import dataclass
from datetime import datetime, time, timedelta, timezone as tz
from pathlib import Path
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

from django.conf import settings
from django.db import connection
from django.utils import timezone as dj_timezone

from identidad.respuestas import ErrorDePeticion

from . import auditoria
from .models import Incidente, PoliticaRespaldo, Respaldo
from .notificaciones_alertas import enviar_alerta_fallo_respaldo

logger = logging.getLogger(__name__)

try:
    TZ_LIMA = ZoneInfo('America/Lima')
except ZoneInfoNotFoundError:  # pragma: no cover - respaldo en entornos sin base tzdata
    TZ_LIMA = tz(timedelta(hours=-5))

HORA_MADRUGADA_MIN = 0
HORA_MADRUGADA_MAX = 5
RETENCION_MIN = 7
RETENCION_MAX = 365
FRECUENCIAS_VALIDAS = ('DIARIO', 'SEMANAL')
TIPOS_ALMACENAMIENTO = ('CARPETA_SEGURA', 'S3_COMPATIBLE', 'SFTP_REMOTO')
NOMBRES_DIAS = ('Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo')
PATRON_HORA = re.compile(r'^([01]\d|2[0-3]):([0-5]\d)$')
PATRON_EMAIL = re.compile(r'^[^@\s]+@[^@\s]+\.[^@\s]+$')


@dataclass(frozen=True)
class PoliticaRespaldoConfig:
    habilitado: bool = True
    hora_programada: str = '02:30'
    frecuencia: str = 'DIARIO'
    dia_semana: int | None = None
    retencion_dias: int = 30
    almacenamiento_externo_tipo: str = 'CARPETA_SEGURA'
    almacenamiento_externo_ruta: str = 'backups-externos/rupi'
    cifrado_activo: bool = True
    alertar_si_falla: bool = True
    email_alerta: str | None = None


POR_DEFECTO = PoliticaRespaldoConfig()


def _limites():
    return {
        'horaMadrugada': {'min': '00:00', 'max': '05:59', 'zonaHoraria': 'America/Lima'},
        'retencionDias': {'min': RETENCION_MIN, 'max': RETENCION_MAX},
        'frecuencias': list(FRECUENCIAS_VALIDAS),
        'tiposAlmacenamiento': list(TIPOS_ALMACENAMIENTO),
    }


def _repo_root() -> Path:
    return Path(settings.BASE_DIR).resolve().parent.parent


def _external_root() -> Path:
    base = getattr(settings, 'RUPI_BACKUP_EXTERNAL_DIR', '') or str(_repo_root() / '.local' / 'respaldos-externos')
    return Path(base).resolve()


def _booleano(valor, nombre: str) -> bool:
    if isinstance(valor, bool):
        return valor
    raise ErrorDePeticion(f'{nombre} debe ser verdadero o falso.')


def parsear_hora_madrugada(valor) -> str:
    """Valida formato HH:MM y restringe la ejecución a la madrugada (00:00–05:59 America/Lima)."""
    texto = str(valor or '').strip()
    match = PATRON_HORA.match(texto)
    if not match:
        raise ErrorDePeticion('La hora programada debe tener el formato HH:MM (por ejemplo, 02:30).')
    hora = int(match.group(1))
    if not (HORA_MADRUGADA_MIN <= hora <= HORA_MADRUGADA_MAX):
        raise ErrorDePeticion(
            'El respaldo debe programarse en horario de madrugada (entre 00:00 y 05:59) para evitar horas punta escolares.'
        )
    return texto


def validar_ruta_externa(tipo: str, valor) -> str:
    """Valida destino externo y bloquea rutas inseguras o dentro del directorio de datos MySQL."""
    ruta = str(valor or '').strip()
    if not ruta:
        raise ErrorDePeticion('Indica la ubicación o URI del almacenamiento externo seguro.')
    if len(ruta) > 500:
        raise ErrorDePeticion('La ubicación del almacenamiento externo no puede superar los 500 caracteres.')
    if '..' in Path(ruta).parts or '\\..' in ruta or '/../' in ruta:
        raise ErrorDePeticion('La ruta del almacenamiento externo no puede contener segmentos "..".')

    normalizada_lower = ruta.replace('\\', '/').lower()
    if 'mysql-data' in normalizada_lower or 'var/lib/mysql' in normalizada_lower:
        raise ErrorDePeticion(
            'El respaldo debe guardarse fuera del directorio de datos de MySQL, en un almacenamiento externo.'
        )

    if tipo == 'S3_COMPATIBLE' and not (
        normalizada_lower.startswith('s3://') or normalizada_lower.startswith('https://')
    ):
        raise ErrorDePeticion('Para almacenamiento S3 compatible usa un destino s3://bucket/prefijo o https://.')
    if tipo == 'SFTP_REMOTO' and ':' not in ruta and not normalizada_lower.startswith('sftp://'):
        raise ErrorDePeticion('Para SFTP remoto indica usuario@servidor:/ruta/respaldos o sftp://...')
    return ruta


def validar_datos_politica(datos: dict) -> dict:
    """Valida los parámetros enviados desde la consola de administración (HU-57)."""
    habilitado = _booleano(datos.get('habilitado', True), 'habilitado')
    hora_programada = parsear_hora_madrugada(datos.get('horaProgramada', '02:30'))

    frecuencia = str(datos.get('frecuencia', 'DIARIO')).strip().upper()
    if frecuencia not in FRECUENCIAS_VALIDAS:
        raise ErrorDePeticion('La frecuencia debe ser DIARIO o SEMANAL.')

    dia_semana = datos.get('diaSemana')
    if frecuencia == 'SEMANAL':
        if isinstance(dia_semana, bool) or not isinstance(dia_semana, int) or not (0 <= dia_semana <= 6):
            raise ErrorDePeticion('Para frecuencia semanal elige un día entre lunes (0) y domingo (6).')
    else:
        dia_semana = None

    retencion = datos.get('retencionDias', 30)
    if isinstance(retencion, bool) or not isinstance(retencion, int) or not (RETENCION_MIN <= retencion <= RETENCION_MAX):
        raise ErrorDePeticion(f'La retención debe estar entre {RETENCION_MIN} y {RETENCION_MAX} días.')

    tipo_alm = str(datos.get('almacenamientoExternoTipo', 'CARPETA_SEGURA')).strip().upper()
    if tipo_alm not in TIPOS_ALMACENAMIENTO:
        raise ErrorDePeticion('El tipo de almacenamiento externo no es válido.')

    ruta_alm = validar_ruta_externa(tipo_alm, datos.get('almacenamientoExternoRuta', ''))
    cifrado_activo = _booleano(datos.get('cifradoActivo', True), 'cifradoActivo')
    alertar_si_falla = _booleano(datos.get('alertarSiFalla', True), 'alertarSiFalla')

    email_raw = datos.get('emailAlerta')
    email_alerta = str(email_raw).strip()[:180] if email_raw is not None else ''
    if email_alerta and not PATRON_EMAIL.match(email_alerta):
        raise ErrorDePeticion('El correo para alertas nocturnas no tiene un formato válido.')

    return {
        'habilitado': habilitado,
        'hora_programada': hora_programada,
        'frecuencia': frecuencia,
        'dia_semana': dia_semana,
        'retencion_dias': retencion,
        'almacenamiento_externo_tipo': tipo_alm,
        'almacenamiento_externo_ruta': ruta_alm,
        'cifrado_activo': cifrado_activo,
        'alertar_si_falla': alertar_si_falla,
        'email_alerta': email_alerta or None,
    }


def _utc(dt: datetime | None) -> datetime | None:
    if dt is None:
        return None
    return dt.replace(tzinfo=tz.utc) if dt.tzinfo is None else dt.astimezone(tz.utc)


def calcular_proximo_respaldo(politica, desde_utc: datetime | None = None) -> datetime | None:
    """Calcula la próxima ejecución programada en hora de madrugada de Perú y la devuelve en UTC."""
    habilitado = getattr(politica, 'habilitado', True)
    if not habilitado:
        return None

    hora_txt = getattr(politica, 'hora_programada', '02:30')
    frecuencia = getattr(politica, 'frecuencia', 'DIARIO')
    dia_semana = getattr(politica, 'dia_semana', None)

    ahora_utc = _utc(desde_utc or dj_timezone.now())
    ahora_lima = ahora_utc.astimezone(TZ_LIMA)
    hh, mm = map(int, hora_txt.split(':'))
    objetivo_hoy = datetime.combine(ahora_lima.date(), time(hh, mm), tzinfo=TZ_LIMA)

    if frecuencia == 'SEMANAL':
        dia_obj = 6 if dia_semana is None else int(dia_semana)
        dias_delta = (dia_obj - ahora_lima.weekday()) % 7
        candidato = objetivo_hoy + timedelta(days=dias_delta)
        if candidato <= ahora_lima:
            candidato += timedelta(days=7)
    else:
        candidato = objetivo_hoy if objetivo_hoy > ahora_lima else objetivo_hoy + timedelta(days=1)

    return candidato.astimezone(tz.utc)


def debe_ejecutarse_en_ciclo(politica, ahora_utc: datetime | None = None, ventana_minutos: int = 45) -> tuple[bool, str]:
    """Determina si una tarea periódica del servidor debe lanzar el respaldo en este momento."""
    if not getattr(politica, 'habilitado', True):
        return False, 'Los respaldos automáticos están pausados en la política.'

    ahora = _utc(ahora_utc or dj_timezone.now())
    ahora_lima = ahora.astimezone(TZ_LIMA)

    if not (HORA_MADRUGADA_MIN <= ahora_lima.hour <= HORA_MADRUGADA_MAX):
        return False, (
            f'Fuera de ventana de madrugada ({ahora_lima.strftime("%H:%M")} America/Lima). '
            'Se evita horario escolar.'
        )

    frecuencia = getattr(politica, 'frecuencia', 'DIARIO')
    dia_semana = getattr(politica, 'dia_semana', None)
    if frecuencia == 'SEMANAL' and dia_semana is not None and ahora_lima.weekday() != int(dia_semana):
        return False, f'La frecuencia semanal ejecuta los días {NOMBRES_DIAS[int(dia_semana)]}.'

    ultima = _utc(getattr(politica, 'ultima_ejecucion_en', None))
    if ultima is not None and ultima.astimezone(TZ_LIMA).date() == ahora_lima.date():
        return False, 'Ya se registró una ejecución programada para la fecha de hoy.'

    hh, mm = map(int, getattr(politica, 'hora_programada', '02:30').split(':'))
    programado_hoy = datetime.combine(ahora_lima.date(), time(hh, mm), tzinfo=TZ_LIMA)
    if ahora_lima < programado_hoy:
        return False, f'Aún no alcanza la hora programada ({hh:02d}:{mm:02d} America/Lima).'
    if ahora_lima > programado_hoy + timedelta(minutes=ventana_minutos):
        return False, f'Pasó la ventana de tolerancia de {ventana_minutos} min respecto a {hh:02d}:{mm:02d}.'

    return True, 'Dentro de la ventana nocturna programada.'


def politica_vigente() -> PoliticaRespaldo:
    fila = PoliticaRespaldo.objects.filter(id=1).first()
    if fila is None:
        fila = PoliticaRespaldo(
            id=1,
            habilitado=POR_DEFECTO.habilitado,
            hora_programada=POR_DEFECTO.hora_programada,
            frecuencia=POR_DEFECTO.frecuencia,
            dia_semana=POR_DEFECTO.dia_semana,
            retencion_dias=POR_DEFECTO.retencion_dias,
            almacenamiento_externo_tipo=POR_DEFECTO.almacenamiento_externo_tipo,
            almacenamiento_externo_ruta=POR_DEFECTO.almacenamiento_externo_ruta,
            cifrado_activo=POR_DEFECTO.cifrado_activo,
            alertar_si_falla=POR_DEFECTO.alertar_si_falla,
            email_alerta=POR_DEFECTO.email_alerta,
            actualizado_en=dj_timezone.now(),
        )
    if fila.habilitado and fila.proximo_respaldo_en is None:
        fila.proximo_respaldo_en = calcular_proximo_respaldo(fila)
    return fila


def _serializar_politica(fila) -> dict:
    return {
        'habilitado': bool(fila.habilitado),
        'horaProgramada': fila.hora_programada,
        'frecuencia': fila.frecuencia,
        'diaSemana': fila.dia_semana,
        'retencionDias': int(fila.retencion_dias),
        'almacenamientoExternoTipo': fila.almacenamiento_externo_tipo,
        'almacenamientoExternoRuta': fila.almacenamiento_externo_ruta,
        'cifradoActivo': bool(fila.cifrado_activo),
        'alertarSiFalla': bool(fila.alertar_si_falla),
        'emailAlerta': fila.email_alerta or '',
        'ultimaEjecucionEn': _iso(fila.ultima_ejecucion_en),
        'proximoRespaldoEn': _iso(fila.proximo_respaldo_en or calcular_proximo_respaldo(fila)),
        'actualizadoEn': _iso(getattr(fila, 'actualizado_en', None)),
    }


def _iso(dt: datetime | None) -> str | None:
    utc = _utc(dt)
    return utc.isoformat().replace('+00:00', 'Z') if utc else None


def describir_tareas_servidor(politica) -> dict:
    """Genera las definiciones listas para cron (Linux) y Programador de Tareas (Windows)."""
    hora_txt = getattr(politica, 'hora_programada', '02:30')
    hh, mm = map(int, hora_txt.split(':'))
    frecuencia = getattr(politica, 'frecuencia', 'DIARIO')
    dia_semana = getattr(politica, 'dia_semana', None)

    # cron usa 0=Domingo..6=Sábado mientras Python weekday() usa 0=Lunes..6=Domingo.
    if frecuencia == 'SEMANAL' and dia_semana is not None:
        cron_dow = str((int(dia_semana) + 1) % 7)
        dias_win = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'][int(dia_semana)]
    else:
        cron_dow = '*'
        dias_win = 'DIARIO'

    cron_expr = f'{mm} {hh} * * {cron_dow}'
    comando_django = 'python manage.py ejecutar_respaldo_programado --verificar-ventana'
    script_windows = 'scripts\\Ejecutar-Respaldo-Programado.ps1'
    instalador_windows = 'scripts\\Instalar-Tarea-Respaldo.ps1'

    return {
        'cronExpresion': cron_expr,
        'cronLinea': f'{cron_expr} TZ=America/Lima {comando_django}',
        'comandoDjango': comando_django,
        'windowsScript': script_windows,
        'windowsInstalador': f'powershell -NoProfile -ExecutionPolicy Bypass -File {instalador_windows}',
        'programacionResumen': (
            f'Todos los días a las {hora_txt} (madrugada, America/Lima)'
            if frecuencia == 'DIARIO'
            else f'Cada {NOMBRES_DIAS[dia_semana or 0]} a las {hora_txt} (madrugada, America/Lima)'
        ),
        'frecuenciaEtiqueta': dias_win,
    }


def localizar_mysqldump() -> str:
    """Localiza el binario `mysqldump` configurado, portable en `.local` o del sistema."""
    configurado = getattr(settings, 'RUPI_MYSQLDUMP_BIN', '').strip()
    if configurado:
        candidato = Path(configurado)
        if candidato.is_file():
            return str(candidato)
        raise RuntimeError(f'No se encontró mysqldump en RUPI_MYSQLDUMP_BIN: {configurado}')

    root = _repo_root()
    patrones = ('.local/mysql-*/bin/mysqldump.exe', '.local/mysql-*/bin/mysqldump')
    for patron in patrones:
        encontrados = sorted(root.glob(patron))
        if encontrados:
            return str(encontrados[0])

    sistema = shutil.which('mysqldump')
    if sistema:
        return sistema
    raise RuntimeError(
        'No se encontró el binario mysqldump en el servidor (revisa PATH o RUPI_MYSQLDUMP_BIN).'
    )


def _resolver_carpeta_externa(ruta_configurada: str) -> Path:
    root = _external_root()
    ruta = Path(ruta_configurada)
    if ruta.is_absolute():
        destino = ruta.resolve()
    else:
        destino = (root / ruta).resolve()
        if root not in destino.parents and destino != root:
            raise RuntimeError('La carpeta destino escapa de la raíz de almacenamiento externo.')
    return destino


def _archivo_cnf_temporal(db_cfg: dict) -> Path:
    lineas = ['[client]']
    if db_cfg.get('HOST'):
        lineas.append(f"host={db_cfg['HOST']}")
    if db_cfg.get('PORT'):
        lineas.append(f"port={db_cfg['PORT']}")
    if db_cfg.get('USER'):
        lineas.append(f"user={db_cfg['USER']}")
    if db_cfg.get('PASSWORD'):
        lineas.append(f"password={db_cfg['PASSWORD']}")

    tmp = tempfile.NamedTemporaryFile('w', suffix='.cnf', delete=False, encoding='utf-8')
    try:
        tmp.write('\n'.join(lineas) + '\n')
        tmp.flush()
    finally:
        tmp.close()

    path = Path(tmp.name)
    try:
        os.chmod(path, 0o600)
    except OSError:
        pass
    return path


def _ejecutar_mysqldump_gz(archivo_gz: Path) -> None:
    """Genera volcado consistente InnoDB comprimido con gzip usando archivo temporal de credenciales."""
    mysqldump = localizar_mysqldump()
    db_cfg = settings.DATABASES['default']
    nombre_bd = db_cfg.get('NAME') or 'rupi'
    cnf_path = _archivo_cnf_temporal(db_cfg)

    try:
        cmd = [
            mysqldump,
            f'--defaults-extra-file={cnf_path}',
            '--single-transaction',
            '--routines',
            '--triggers',
            '--quick',
            '--default-character-set=utf8mb4',
            str(nombre_bd),
        ]
        proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        assert proc.stdout is not None and proc.stderr is not None

        with gzip.open(archivo_gz, 'wb', compresslevel=6) as gz_out:
            while True:
                bloque = proc.stdout.read(65_536)
                if not bloque:
                    break
                gz_out.write(bloque)

        stderr_bytes = proc.stderr.read()
        codigo = proc.wait(timeout=300)
        if codigo != 0:
            err = stderr_bytes.decode('utf-8', errors='replace').strip()
            raise RuntimeError(f'mysqldump terminó con código {codigo}: {err or "error desconocido"}')
    finally:
        try:
            cnf_path.unlink(missing_ok=True)
        except OSError:
            pass

    if not archivo_gz.exists() or archivo_gz.stat().st_size == 0:
        raise RuntimeError('El archivo generado por mysqldump quedó vacío.')


def _aplicar_cifrado_si_corresponde(archivo_origen: Path, cifrado_activo: bool) -> tuple[Path, bool]:
    """Cifra el archivo con AES-256-CBC cuando hay clave configurada; retorna (ruta_final, cifrado_real)."""
    clave = getattr(settings, 'RUPI_BACKUP_ENCRYPTION_KEY', '').strip()
    if not (cifrado_activo and clave):
        return archivo_origen, False

    openssl = shutil.which('openssl')
    if not openssl:
        raise RuntimeError(
            'RUPI_BACKUP_ENCRYPTION_KEY está definida pero falta el binario `openssl` para cifrar el respaldo.'
        )

    destino_enc = archivo_origen.with_suffix(archivo_origen.suffix + '.enc')
    env = os.environ.copy()
    env['RUPI_OPENSSL_PASS'] = clave
    proc = subprocess.run(
        [
            openssl, 'enc', '-aes-256-cbc', '-pbkdf2', '-salt',
            '-in', str(archivo_origen),
            '-out', str(destino_enc),
            '-pass', 'env:RUPI_OPENSSL_PASS',
        ],
        capture_output=True,
        env=env,
        timeout=300,
    )
    if proc.returncode != 0:
        err = proc.stderr.decode('utf-8', errors='replace').strip()
        raise RuntimeError(f'No se pudo cifrar el respaldo con OpenSSL: {err}')
    archivo_origen.unlink(missing_ok=True)
    return destino_enc, True


def _sha256_archivo(ruta: Path) -> bytes:
    digest = hashlib.sha256()
    with ruta.open('rb') as f:
        while True:
            bloque = f.read(65_536)
            if not bloque:
                break
            digest.update(bloque)
    return digest.digest()


def _limpiar_vencidos_carpeta(carpeta: Path, retencion_dias: int) -> int:
    limite = dj_timezone.now().timestamp() - (retencion_dias * 86400)
    eliminados = 0
    for item in carpeta.glob('rupi_respaldo_*'):
        if item.is_file() and item.stat().st_mtime < limite:
            item.unlink(missing_ok=True)
            Path(f'{item}.sha256').unlink(missing_ok=True)
            eliminados += 1
    return eliminados


def enviar_a_almacenamiento_externo(
    archivo_local: Path,
    checksum: bytes,
    politica,
) -> tuple[str, bool]:
    """Transfiere y verifica el respaldo en el almacenamiento externo configurado."""
    tipo = politica.almacenamiento_externo_tipo
    ruta_cfg = politica.almacenamiento_externo_ruta
    sha_hex = checksum.hex()

    if tipo == 'CARPETA_SEGURA':
        carpeta = _resolver_carpeta_externa(ruta_cfg)
        carpeta.mkdir(parents=True, exist_ok=True)
        destino = carpeta / archivo_local.name
        shutil.copy2(archivo_local, destino)
        try:
            os.chmod(destino, 0o600)
        except OSError:
            pass

        checksum_destino = _sha256_archivo(destino)
        if checksum_destino != checksum:
            destino.unlink(missing_ok=True)
            raise RuntimeError('El checksum SHA-256 en el almacenamiento externo no coincide con el origen.')

        Path(f'{destino}.sha256').write_text(f'{sha_hex}  {destino.name}\n', encoding='utf-8')
        _limpiar_vencidos_carpeta(carpeta, int(politica.retencion_dias))
        return str(destino), True

    if tipo == 'S3_COMPATIBLE':
        endpoint = getattr(settings, 'RUPI_BACKUP_S3_ENDPOINT', '').strip()
        if not endpoint:
            raise RuntimeError(
                'Almacenamiento S3 compatible sin endpoint configurado (define RUPI_BACKUP_S3_ENDPOINT).'
            )
        parsed = urllib.parse.urlparse(endpoint)
        if parsed.scheme.lower() != 'https':
            raise RuntimeError('El endpoint S3 compatible debe utilizar HTTPS.')

        destino_url = f"{endpoint.rstrip('/')}/{archivo_local.name}"
        headers = {
            'Content-Type': 'application/octet-stream',
            'X-Rupi-Checksum-Sha256': sha_hex,
        }
        token = getattr(settings, 'RUPI_BACKUP_S3_TOKEN', '').strip()
        if token:
            headers['Authorization'] = f'Bearer {token}'

        req = urllib.request.Request(destino_url, data=archivo_local.read_bytes(), headers=headers, method='PUT')
        with urllib.request.urlopen(req, timeout=60) as resp:
            if not (200 <= resp.getcode() < 300):
                raise RuntimeError(f'El almacenamiento S3 respondió estado HTTP {resp.getcode()}.')
        return f'{ruta_cfg.rstrip("/")}/{archivo_local.name}', True

    if tipo == 'SFTP_REMOTO':
        cmd_tpl = getattr(settings, 'RUPI_BACKUP_SFTP_CMD', '').strip()
        if not cmd_tpl:
            raise RuntimeError(
                'Almacenamiento SFTP remoto sin comando configurado (define RUPI_BACKUP_SFTP_CMD).'
            )
        partes = [p.format(archivo=str(archivo_local), destino=ruta_cfg) for p in shlex.split(cmd_tpl)]
        proc = subprocess.run(partes, capture_output=True, timeout=300)
        if proc.returncode != 0:
            err = proc.stderr.decode('utf-8', errors='replace').strip()
            raise RuntimeError(f'Fallo al transferir respaldo por SFTP remoto: {err}')
        return f'{ruta_cfg.rstrip("/")}/{archivo_local.name}', True

    raise RuntimeError(f'Tipo de almacenamiento externo no soportado: {tipo}')


def _registrar_incidente_fallo(respaldo_id: str, origen: str, detalle: str, actor_id: str | None) -> None:
    """Abre un incidente operativo en `operaciones_incidente` si no existe uno activo."""
    existente = Incidente.objects.filter(
        servicio='respaldo_nocturno',
        estado__in=['ABIERTO', 'EN_ATENCION'],
    ).first()
    if existente:
        return
    Incidente.objects.create(
        id=str(uuid.uuid4()),
        servicio='respaldo_nocturno',
        categoria='BASE_DATOS',
        severidad='ALTA',
        estado='ABIERTO',
        resumen=f'Fallo en respaldo {origen.lower()} de base de datos ({respaldo_id[:8]}).',
        referencia_externa=detalle[:255],
        iniciado_en=dj_timezone.now(),
    )
    auditoria.registrar(
        actor_id,
        'INCIDENTE_ABIERTO:RESPALDO_NOCTURNO',
        respaldo_id,
        resultado='ERROR',
        entidad_tipo='RESPALDO',
        dominio='operaciones',
    )


def _resolver_incidente_respaldo() -> None:
    ahora = dj_timezone.now()
    Incidente.objects.filter(
        servicio='respaldo_nocturno',
        estado__in=['ABIERTO', 'EN_ATENCION'],
    ).update(estado='RESUELTO', resuelto_en=ahora)


def _version_esquema_mysql(cifrado_real: bool) -> str:
    sufijo = '+enc' if cifrado_real else '+gz'
    try:
        with connection.cursor() as cur:
            cur.execute('SELECT VERSION()')
            fila = cur.fetchone()
            base = str(fila[0])[:48] if fila else 'mysql-8.4'
    except Exception:
        base = 'mysql-8.4'
    return f'{base}{sufijo}'[:60]


def ejecutar_respaldo(
    origen: str = 'PROGRAMADO',
    actor_id: str | None = None,
    generador_dump=None,
    transferidor=None,
) -> dict:
    """Ejecuta un respaldo completo, lo envía al almacenamiento externo y notifica si falla (HU-57)."""
    origen_norm = 'MANUAL' if str(origen).upper() == 'MANUAL' else 'PROGRAMADO'
    politica = politica_vigente()
    inicio = dj_timezone.now()
    respaldo_id = str(uuid.uuid4())
    marca = inicio.astimezone(TZ_LIMA).strftime('%Y%m%d_%H%M%S')

    registro = Respaldo.objects.create(
        id=respaldo_id,
        tipo='COMPLETO',
        origen=origen_norm,
        iniciado_en=inicio,
        estado='EN_CURSO',
        almacenamiento_externo=politica.almacenamiento_externo_tipo,
        alerta_enviada=False,
    )

    tmp_dir = Path(tempfile.mkdtemp(prefix='rupi_backup_'))
    archivo_dump = tmp_dir / f'rupi_respaldo_{marca}_{respaldo_id[:8]}.sql.gz'

    try:
        dump_fn = generador_dump or _ejecutar_mysqldump_gz
        dump_fn(archivo_dump)

        archivo_final, cifrado_real = _aplicar_cifrado_si_corresponde(
            archivo_dump, bool(politica.cifrado_activo)
        )
        checksum = _sha256_archivo(archivo_final)
        tamano = int(archivo_final.stat().st_size)

        send_fn = transferidor or enviar_a_almacenamiento_externo
        ubicacion, verificado = send_fn(archivo_final, checksum, politica)

        fin = dj_timezone.now()
        estado_final = 'VERIFICADO' if verificado else 'CORRECTO'
        version = _version_esquema_mysql(cifrado_real)

        registro.finalizado_en = fin
        registro.estado = estado_final
        registro.ubicacion_referencia = ubicacion[:500]
        registro.checksum_sha256 = checksum
        registro.tamano_bytes = tamano
        registro.version_esquema = version
        registro.detalle_error = None
        registro.save()

        if PoliticaRespaldo.objects.filter(id=1).exists():
            PoliticaRespaldo.objects.filter(id=1).update(
                ultima_ejecucion_en=fin,
                proximo_respaldo_en=calcular_proximo_respaldo(politica, desde_utc=fin),
            )

        _resolver_incidente_respaldo()
        auditoria.registrar(
            actor_id,
            f'RESPALDO_{origen_norm}_COMPLETADO',
            respaldo_id,
            resultado='CORRECTO',
            entidad_tipo='RESPALDO',
            dominio='operaciones',
        )

        return _serializar_respaldo(registro)
    except Exception as exc:
        fin = dj_timezone.now()
        detalle = str(exc).strip()[:500] or 'Error inesperado al ejecutar respaldo.'
        logger.error('Fallo en respaldo %s (%s): %s', respaldo_id, origen_norm, detalle)

        alerta_enviada = False
        alerta_resultado = None
        if getattr(politica, 'alertar_si_falla', True):
            alerta_resultado = enviar_alerta_fallo_respaldo(
                respaldo_id=respaldo_id,
                origen=origen_norm,
                almacenamiento=f'{politica.almacenamiento_externo_tipo}: {politica.almacenamiento_externo_ruta}',
                detalle_error=detalle,
                email_destino=politica.email_alerta,
            )
            alerta_enviada = bool(alerta_resultado.get('emailEnviado') or alerta_resultado.get('smsEnviado'))

        registro.finalizado_en = fin
        registro.estado = 'FALLIDO'
        registro.detalle_error = detalle
        registro.alerta_enviada = alerta_enviada
        registro.save()

        try:
            _registrar_incidente_fallo(respaldo_id, origen_norm, detalle, actor_id)
        except Exception as inc_exc:  # pragma: no cover - no ocultar el fallo principal de respaldo
            logger.warning('No se pudo registrar incidente de respaldo: %s', inc_exc)

        auditoria.registrar(
            actor_id,
            f'RESPALDO_{origen_norm}_FALLIDO',
            respaldo_id,
            resultado='ERROR',
            entidad_tipo='RESPALDO',
            dominio='operaciones',
        )

        res_dict = _serializar_respaldo(registro)
        if alerta_resultado is not None:
            res_dict['alertaResultado'] = alerta_resultado
        return res_dict
    finally:
        shutil.rmtree(tmp_dir, ignore_errors=True)


def _serializar_respaldo(item: Respaldo) -> dict:
    checksum = bytes(item.checksum_sha256).hex() if item.checksum_sha256 else None
    duracion = None
    if item.iniciado_en and item.finalizado_en:
        duracion = max(0, int((_utc(item.finalizado_en) - _utc(item.iniciado_en)).total_seconds()))
    return {
        'id': item.id,
        'tipo': item.tipo,
        'origen': item.origen,
        'estado': item.estado,
        'iniciadoEn': _iso(item.iniciado_en),
        'finalizadoEn': _iso(item.finalizado_en),
        'duracionSegundos': duracion,
        'ubicacionReferencia': item.ubicacion_referencia,
        'almacenamientoExterno': item.almacenamiento_externo,
        'checksumSha256': checksum,
        'tamanoBytes': item.tamano_bytes,
        'versionEsquema': item.version_esquema,
        'detalleError': item.detalle_error,
        'alertaEnviada': bool(item.alerta_enviada),
    }


def actualizar_politica(datos: dict, actor_id: str | None) -> dict:
    """Valida, persiste la programación de respaldos nocturnos y registra la auditoría."""
    validados = validar_datos_politica(datos)
    ahora = dj_timezone.now()

    fila, _ = PoliticaRespaldo.objects.get_or_create(
        id=1,
        defaults={**validados, 'actualizado_por': actor_id, 'actualizado_en': ahora},
    )
    for campo, valor in validados.items():
        setattr(fila, campo, valor)
    fila.actualizado_por = actor_id
    fila.actualizado_en = ahora
    fila.proximo_respaldo_en = calcular_proximo_respaldo(fila, desde_utc=ahora)
    fila.save()

    auditoria.registrar(
        actor_id,
        'POLITICA_RESPALDO_ACTUALIZADA',
        None,
        resultado='CORRECTO',
        entidad_tipo='POLITICA_RESPALDO',
        dominio='operaciones',
    )
    return estado_respaldos()


def estado_respaldos(limite: int = 20) -> dict:
    """Devuelve política vigente, límites de madrugada, comando de tarea y últimas ejecuciones."""
    politica = politica_vigente()
    historial_qs = list(Respaldo.objects.order_by('-iniciado_en')[:limite])
    historial = [_serializar_respaldo(item) for item in historial_qs]

    hace_30d = dj_timezone.now() - timedelta(days=30)
    recientes = list(Respaldo.objects.filter(iniciado_en__gte=hace_30d))
    total_30d = len(recientes)
    exitosos_30d = sum(1 for r in recientes if r.estado in ('CORRECTO', 'VERIFICADO'))
    fallidos_30d = sum(1 for r in recientes if r.estado == 'FALLIDO')

    ultimo = historial[0] if historial else None
    alerta_email = politica.email_alerta or getattr(
        settings, 'RUPI_ALERTA_EMAIL_RESPONSABLE', 'infraestructura@rupi.edu.pe'
    )

    return {
        'politica': _serializar_politica(politica),
        'limites': _limites(),
        'tareaServidor': describir_tareas_servidor(politica),
        'canalesAlerta': {
            'emailResponsable': alerta_email,
            'smsNumero': getattr(settings, 'RUPI_ALERTA_SMS_NUMERO', '') or None,
            'webhookConfigurado': bool(getattr(settings, 'RUPI_ALERTA_SMS_WEBHOOK', '')),
        },
        'estadisticas': {
            'total30d': total_30d,
            'exitosos30d': exitosos_30d,
            'fallidos30d': fallidos_30d,
            'tasaExitoPercent': round((exitosos_30d / total_30d) * 100.0, 1) if total_30d else 100.0,
            'ultimoRespaldo': ultimo,
        },
        'historial': historial,
    }
