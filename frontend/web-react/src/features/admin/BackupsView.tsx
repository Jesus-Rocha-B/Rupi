import { useCallback, useEffect, useState, type FormEvent } from 'react'
import {
  Bell,
  CheckCircle2,
  Clock,
  Copy,
  Database,
  HardDrive,
  Play,
  RefreshCw,
  Save,
  ShieldAlert,
  Terminal,
  XCircle,
} from 'lucide-react'
import {
  AdminApiError,
  fetchBackups,
  formatDateTime,
  runBackupNow,
  saveBackupPolicy,
  testBackupAlert,
  type Administrator,
  type BackupFrequency,
  type BackupPolicy,
  type BackupRecord,
  type BackupsState,
  type BackupStorageType,
} from '../../services/adminApi'
import { SESSION_LOST } from './accountStatus'
import './BackupsView.css'

interface Props {
  admin: Administrator
  onSessionLost: (message: string) => void
}

interface Draft {
  habilitado: boolean
  horaProgramada: string
  frecuencia: BackupFrequency
  diaSemana: string
  retencionDias: string
  almacenamientoExternoTipo: BackupStorageType
  almacenamientoExternoRuta: string
  cifradoActivo: boolean
  alertarSiFalla: boolean
  emailAlerta: string
}

const WEEKDAYS = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo']

const STORAGE_LABELS: Record<BackupStorageType, string> = {
  CARPETA_SEGURA: 'Carpeta externa segura (fuera de MySQL)',
  S3_COMPATIBLE: 'Almacenamiento S3 compatible (HTTPS)',
  SFTP_REMOTO: 'Servidor SFTP remoto',
}

const STATUS_LABELS: Record<BackupRecord['estado'], string> = {
  VERIFICADO: 'Verificado (SHA-256)',
  CORRECTO: 'Completado',
  FALLIDO: 'Fallido',
  EN_CURSO: 'En curso',
}

function toDraft(policy: BackupPolicy): Draft {
  return {
    habilitado: policy.habilitado,
    horaProgramada: policy.horaProgramada,
    frecuencia: policy.frecuencia,
    diaSemana: String(policy.diaSemana ?? 6),
    retencionDias: String(policy.retencionDias),
    almacenamientoExternoTipo: policy.almacenamientoExternoTipo,
    almacenamientoExternoRuta: policy.almacenamientoExternoRuta,
    cifradoActivo: policy.cifradoActivo,
    alertarSiFalla: policy.alertarSiFalla,
    emailAlerta: policy.emailAlerta || '',
  }
}

function formatBytes(bytes: number | null): string {
  if (bytes == null || bytes < 0) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

/** HU-57: Programación de respaldos automáticos nocturnos, almacenamiento externo y alertas por fallo. */
export function BackupsView({ admin, onSessionLost }: Props) {
  const [data, setData] = useState<BackupsState | null>(null)
  const [draft, setDraft] = useState<Draft | null>(null)
  const [reload, setReload] = useState(0)
  const [busy, setBusy] = useState(false)
  const [runningNow, setRunningNow] = useState(false)
  const [testingAlert, setTestingAlert] = useState(false)
  const [copiedKey, setCopiedKey] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  const writable = admin.permisos.includes('respaldos.gestionar')

  const fail = useCallback((failure: unknown, fallback: string) => {
    if (failure instanceof AdminApiError && failure.status === 401) onSessionLost(failure.message || SESSION_LOST)
    else setError(failure instanceof AdminApiError ? failure.message : fallback)
  }, [onSessionLost])

  useEffect(() => {
    const controller = new AbortController()
    fetchBackups(controller.signal)
      .then(value => {
        setData(value)
        setDraft(toDraft(value.politica))
        setError(null)
      })
      .catch(failure => {
        if (!controller.signal.aborted) fail(failure, 'No pudimos cargar la programación de respaldos.')
      })
    return () => controller.abort()
  }, [reload, fail])

  const saved = data ? toDraft(data.politica) : null
  const dirty = !!draft && !!saved && (Object.keys(saved) as (keyof Draft)[]).some(key => saved[key] !== draft[key])

  function validate(current: Draft): string | null {
    const match = /^([01]\d|2[0-3]):([0-5]\d)$/.exec(current.horaProgramada.trim())
    if (!match) return 'Escribe la hora en formato HH:MM (por ejemplo, 02:30).'
    const hour = Number(match[1])
    if (hour < 0 || hour > 5) {
      return 'Elige una hora de madrugada entre 00:00 y 05:59 para no afectar el tráfico escolar.'
    }
    const ret = Number(current.retencionDias.trim())
    const minRet = data?.limites.retencionDias.min ?? 7
    const maxRet = data?.limites.retencionDias.max ?? 365
    if (!Number.isInteger(ret) || ret < minRet || ret > maxRet) {
      return `Los días de retención deben estar entre ${minRet} y ${maxRet}.`
    }
    const ruta = current.almacenamientoExternoRuta.trim()
    if (!ruta) return 'Indica la ruta o URI del almacenamiento externo seguro.'
    if (ruta.includes('..') || ruta.toLowerCase().includes('mysql-data')) {
      return 'El almacenamiento externo debe quedar fuera del directorio de datos de MySQL.'
    }
    if (current.emailAlerta.trim() && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(current.emailAlerta.trim())) {
      return 'El correo para alertas nocturnas no tiene un formato válido.'
    }
    return null
  }

  const formProblem = draft ? validate(draft) : null

  async function handleSave(event: FormEvent) {
    event.preventDefault()
    if (!draft || formProblem || busy) return
    setBusy(true)
    setError(null)
    setMessage(null)
    try {
      const updated = await saveBackupPolicy({
        habilitado: draft.habilitado,
        horaProgramada: draft.horaProgramada.trim(),
        frecuencia: draft.frecuencia,
        diaSemana: draft.frecuencia === 'SEMANAL' ? Number(draft.diaSemana) : null,
        retencionDias: Number(draft.retencionDias),
        almacenamientoExternoTipo: draft.almacenamientoExternoTipo,
        almacenamientoExternoRuta: draft.almacenamientoExternoRuta.trim(),
        cifradoActivo: draft.cifradoActivo,
        alertarSiFalla: draft.alertarSiFalla,
        emailAlerta: draft.emailAlerta.trim(),
      })
      setData(updated)
      setDraft(toDraft(updated.politica))
      setMessage('Programación de respaldos guardada correctamente en Django.')
    } catch (failure) {
      fail(failure, 'No pudimos guardar la programación de respaldos.')
    } finally {
      setBusy(false)
    }
  }

  async function handleRunNow() {
    if (runningNow || busy) return
    setRunningNow(true)
    setError(null)
    setMessage(null)
    try {
      const result = await runBackupNow()
      setMessage(`Respaldo completado y verificado en almacenamiento externo: ${result.ubicacionReferencia}`)
      setReload(v => v + 1)
    } catch (failure) {
      fail(failure, 'El respaldo manual falló y disparó la alerta de notificación.')
      setReload(v => v + 1)
    } finally {
      setRunningNow(false)
    }
  }

  async function handleTestAlert() {
    if (testingAlert || busy) return
    setTestingAlert(true)
    setError(null)
    setMessage(null)
    try {
      const res = await testBackupAlert()
      const emailPart = res.emailEnviado
        ? `Correo enviado a ${res.destinatarioEmail}`
        : `Correo no entregado (${res.errorEmail || 'revisar SMTP'})`
      setMessage(`Prueba de alerta nocturna ejecutada: ${emailPart}.`)
    } catch (failure) {
      fail(failure, 'No pudimos probar la alerta de fallo de respaldo.')
    } finally {
      setTestingAlert(false)
    }
  }

  async function copyCommand(key: string, text: string) {
    try {
      await navigator.clipboard.writeText(text)
      setCopiedKey(key)
      setTimeout(() => setCopiedKey(current => (current === key ? null : current)), 2000)
    } catch {
      // Silencio si el navegador bloquea el portapapeles
    }
  }

  const ultimo = data?.estadisticas.ultimoRespaldo ?? null

  return (
    <section className="admin-card backups-view" aria-labelledby="backups-title">
      <div className="admin-section-head">
        <div>
          <h1 id="backups-title">Respaldos automáticos</h1>
          <p className="admin-muted">
            Programa copias automáticas de la base principal durante la madrugada para evitar las horas punta escolares,
            guárdalas en almacenamiento externo seguro y recibe alertas si el respaldo nocturno falla.
          </p>
        </div>
        <div className="admin-actions">
          <button
            type="button"
            className="admin-btn admin-btn--quiet"
            disabled={busy || runningNow}
            onClick={() => {
              if (!dirty || window.confirm('Hay cambios sin guardar. ¿Quieres recargar?')) {
                setMessage(null)
                setReload(v => v + 1)
              }
            }}
          >
            <RefreshCw size={18} aria-hidden="true" />
            Actualizar
          </button>
          {writable && (
            <button
              type="button"
              className="admin-btn admin-btn--sky"
              disabled={busy || runningNow}
              onClick={handleRunNow}
            >
              <Play size={18} aria-hidden="true" />
              {runningNow ? 'Respaldando…' : 'Respaldar ahora'}
            </button>
          )}
        </div>
      </div>

      {error && <p className="admin-feedback admin-feedback--error" role="alert">{error}</p>}
      {message && <p className="admin-feedback admin-feedback--ok" role="status">{message}</p>}
      {!data && !error && <p className="admin-loading">Cargando programación de respaldos…</p>}

      {data && draft && <>
        <dl className="admin-session-totals" aria-label="Resumen de respaldos automáticos">
          <div>
            <dt>Próximo respaldo nocturno</dt>
            <dd>{data.politica.habilitado ? formatDateTime(data.politica.proximoRespaldoEn ?? null) : 'Pausado'}</dd>
          </div>
          <div>
            <dt>Éxito últimos 30 días</dt>
            <dd>{data.estadisticas.tasaExitoPercent}% ({data.estadisticas.exitosos30d}/{data.estadisticas.total30d})</dd>
          </div>
          <div>
            <dt>Último estado</dt>
            <dd>{ultimo ? STATUS_LABELS[ultimo.estado] : 'Sin registros'}</dd>
          </div>
        </dl>

        <form className="backups-form" onSubmit={handleSave} noValidate>
          <fieldset className="backups-group">
            <legend><Clock size={20} aria-hidden="true" />Horario en madrugada (sin tráfico escolar)</legend>
            <p className="admin-muted">
              Ventana permitida: de <strong>00:00</strong> a <strong>05:59</strong> (hora de Perú, America/Lima) para no
              competir con clases ni sincronizaciones en colegios.
            </p>
            <label className="backups-switch">
              <input
                type="checkbox"
                checked={draft.habilitado}
                disabled={!writable || busy}
                onChange={e => { setDraft({ ...draft, habilitado: e.target.checked }); setMessage(null) }}
              />
              <span>{draft.habilitado ? 'Activo: ejecutar respaldos automáticos nocturnos' : 'Pausado: solo respaldo manual'}</span>
            </label>
            <div className="backups-grid">
              <label className="admin-field">
                <span>Hora programada (madrugada)</span>
                <input
                  type="time"
                  min="00:00"
                  max="05:59"
                  value={draft.horaProgramada}
                  disabled={!writable || busy}
                  onChange={e => { setDraft({ ...draft, horaProgramada: e.target.value }); setMessage(null) }}
                />
                <small>Rango exclusivo de madrugada: 00:00 a 05:59.</small>
              </label>
              <label className="admin-field">
                <span>Frecuencia</span>
                <select
                  value={draft.frecuencia}
                  disabled={!writable || busy}
                  onChange={e => { setDraft({ ...draft, frecuencia: e.target.value as BackupFrequency }); setMessage(null) }}
                >
                  <option value="DIARIO">Diario (cada madrugada)</option>
                  <option value="SEMANAL">Semanal</option>
                </select>
              </label>
              {draft.frecuencia === 'SEMANAL' && (
                <label className="admin-field">
                  <span>Día de la semana</span>
                  <select
                    value={draft.diaSemana}
                    disabled={!writable || busy}
                    onChange={e => { setDraft({ ...draft, diaSemana: e.target.value }); setMessage(null) }}
                  >
                    {WEEKDAYS.map((day, idx) => <option key={day} value={String(idx)}>{day}</option>)}
                  </select>
                </label>
              )}
              <label className="admin-field">
                <span>Retención histórica (días)</span>
                <input
                  type="number"
                  min={data.limites.retencionDias.min}
                  max={data.limites.retencionDias.max}
                  value={draft.retencionDias}
                  disabled={!writable || busy}
                  onChange={e => { setDraft({ ...draft, retencionDias: e.target.value }); setMessage(null) }}
                />
                <small>Entre {data.limites.retencionDias.min} y {data.limites.retencionDias.max} días.</small>
              </label>
            </div>
          </fieldset>

          <fieldset className="backups-group">
            <legend><HardDrive size={20} aria-hidden="true" />Almacenamiento externo y seguridad</legend>
            <p className="admin-muted">
              Las copias comprimidas y verificadas con SHA-256 se envían fuera de la base de datos para resistir fallas de disco.
            </p>
            <div className="backups-grid">
              <label className="admin-field">
                <span>Destino externo</span>
                <select
                  value={draft.almacenamientoExternoTipo}
                  disabled={!writable || busy}
                  onChange={e => {
                    setDraft({ ...draft, almacenamientoExternoTipo: e.target.value as BackupStorageType })
                    setMessage(null)
                  }}
                >
                  {(Object.keys(STORAGE_LABELS) as BackupStorageType[]).map(key => (
                    <option key={key} value={key}>{STORAGE_LABELS[key]}</option>
                  ))}
                </select>
              </label>
              <label className="admin-field">
                <span>Ruta o URI de destino</span>
                <input
                  type="text"
                  value={draft.almacenamientoExternoRuta}
                  disabled={!writable || busy}
                  placeholder="backups-externos/rupi"
                  onChange={e => { setDraft({ ...draft, almacenamientoExternoRuta: e.target.value }); setMessage(null) }}
                />
                <small>Carpeta externa montada, URI `s3://bucket/prefijo` o `usuario@host:/ruta`.</small>
              </label>
            </div>
            <label className="backups-switch">
              <input
                type="checkbox"
                checked={draft.cifradoActivo}
                disabled={!writable || busy}
                onChange={e => { setDraft({ ...draft, cifradoActivo: e.target.checked }); setMessage(null) }}
              />
              <span>Cifrar copias con AES-256 cuando el servidor tenga `RUPI_BACKUP_ENCRYPTION_KEY`</span>
            </label>
          </fieldset>

          <fieldset className="backups-group">
            <legend><Bell size={20} aria-hidden="true" />Alertas ante fallos nocturnos</legend>
            <p className="admin-muted">
              Si un respaldo nocturno falla o no logra enviarse al almacenamiento externo, Django abre un incidente y notifica al administrador.
            </p>
            <label className="backups-switch">
              <input
                type="checkbox"
                checked={draft.alertarSiFalla}
                disabled={!writable || busy}
                onChange={e => { setDraft({ ...draft, alertarSiFalla: e.target.checked }); setMessage(null) }}
              />
              <span>Enviar alerta inmediata si el respaldo nocturno falla</span>
            </label>
            <div className="backups-grid">
              <label className="admin-field">
                <span>Correo de alertas</span>
                <input
                  type="email"
                  value={draft.emailAlerta}
                  disabled={!writable || busy || !draft.alertarSiFalla}
                  placeholder={data.canalesAlerta.emailResponsable}
                  onChange={e => { setDraft({ ...draft, emailAlerta: e.target.value }); setMessage(null) }}
                />
                <small>Si queda vacío, usa el correo institucional ({data.canalesAlerta.emailResponsable}).</small>
              </label>
            </div>
          </fieldset>

          <p className="backups-summary" aria-live="polite">
            <strong>Resumen de tarea:</strong> {data.tareaServidor.programacionResumen}. Destino:{' '}
            {STORAGE_LABELS[draft.almacenamientoExternoTipo]} ({draft.almacenamientoExternoRuta || 'sin ruta'}).{' '}
            {draft.alertarSiFalla ? 'Con alerta automática por correo/SMS si falla.' : 'Sin alertas automáticas.'}
          </p>

          {formProblem && <p className="admin-feedback admin-feedback--error" role="alert">{formProblem}</p>}

          <div className="backups-footer">
            <small className="admin-muted">
              {data.politica.actualizadoEn ? `Último ajuste: ${formatDateTime(data.politica.actualizadoEn)}` : 'Configuración inicial.'}
            </small>
            {writable ? (
              <div className="admin-actions">
                <button
                  type="button"
                  className="admin-btn admin-btn--quiet"
                  disabled={busy || testingAlert}
                  onClick={handleTestAlert}
                >
                  <ShieldAlert size={18} aria-hidden="true" />
                  {testingAlert ? 'Probando alerta…' : 'Probar alerta de fallo'}
                </button>
                <button
                  type="submit"
                  className="admin-btn admin-btn--leaf"
                  disabled={busy || !dirty || !!formProblem}
                >
                  <Save size={18} aria-hidden="true" />
                  {busy ? 'Guardando…' : 'Guardar programación'}
                </button>
              </div>
            ) : (
              <p className="admin-muted">Tu rol puede consultar los respaldos, pero solo Administración los configura.</p>
            )}
          </div>
        </form>

        <section className="backups-section" aria-labelledby="backups-server-title">
          <h2 id="backups-server-title"><Terminal size={18} aria-hidden="true" /> Tarea programada del servidor</h2>
          <p className="admin-muted">
            Instala esta tarea en el servidor para que Django ejecute el volcado automáticamente cada madrugada.
          </p>
          <div className="backups-tasks-grid">
            <article className="backups-task-card">
              <header>
                <span>Windows · Programador de Tareas</span>
                <button
                  type="button"
                  className="admin-btn admin-btn--quiet backups-copy-btn"
                  onClick={() => copyCommand('win', data.tareaServidor.windowsInstalador)}
                >
                  <Copy size={16} aria-hidden="true" />
                  {copiedKey === 'win' ? 'Copiado' : 'Copiar'}
                </button>
              </header>
              <pre className="backups-code">{data.tareaServidor.windowsInstalador}</pre>
            </article>
            <article className="backups-task-card">
              <header>
                <span>Linux · Cron (`TZ=America/Lima`)</span>
                <button
                  type="button"
                  className="admin-btn admin-btn--quiet backups-copy-btn"
                  onClick={() => copyCommand('cron', data.tareaServidor.cronLinea)}
                >
                  <Copy size={16} aria-hidden="true" />
                  {copiedKey === 'cron' ? 'Copiado' : 'Copiar'}
                </button>
              </header>
              <pre className="backups-code">{data.tareaServidor.cronLinea}</pre>
            </article>
          </div>
        </section>

        <section className="backups-section" aria-labelledby="backups-history-title">
          <h2 id="backups-history-title"><Database size={18} aria-hidden="true" /> Historial de respaldos</h2>
          {data.historial.length === 0 ? (
            <p className="admin-empty-line">Todavía no se han ejecutado respaldos. Usa «Respaldar ahora» para generar la primera copia.</p>
          ) : (
            <ul className="backups-history-list">
              {data.historial.map(item => {
                const ok = item.estado === 'VERIFICADO' || item.estado === 'CORRECTO'
                return (
                  <li key={item.id} className={`backups-history-row ${item.estado === 'FALLIDO' ? 'is-failed' : ''}`}>
                    <div className="backups-meta">
                      <strong>
                        {item.origen === 'PROGRAMADO' ? 'Respaldo nocturno programado' : 'Respaldo manual'} · {formatDateTime(item.iniciadoEn)}
                      </strong>
                      <small>
                        Destino: {item.ubicacionReferencia || item.almacenamientoExterno || 'Pendiente'} · Tamaño: {formatBytes(item.tamanoBytes)}
                        {item.duracionSegundos != null ? ` · Duración: ${item.duracionSegundos} s` : ''}
                      </small>
                      {item.checksumSha256 && <small>SHA-256: {item.checksumSha256.slice(0, 16)}…</small>}
                      {item.detalleError && <small>Error: {item.detalleError}</small>}
                    </div>
                    <div className="backups-badges">
                      <span className={`admin-chip ${ok ? 'admin-chip--ok' : item.estado === 'FALLIDO' ? 'admin-chip--danger' : 'admin-chip--warn'}`}>
                        {ok ? <CheckCircle2 size={14} aria-hidden="true" /> : <XCircle size={14} aria-hidden="true" />}
                        {STATUS_LABELS[item.estado]}
                      </span>
                      {item.alertaEnviada && <span className="admin-chip admin-chip--warn">Alerta enviada</span>}
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
        </section>
      </>}
    </section>
  )
}
