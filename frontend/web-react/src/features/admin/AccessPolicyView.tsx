import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { LockKeyhole, LockOpen, Monitor, RefreshCw, RotateCcw, UserRoundX } from 'lucide-react'
import {
  AdminApiError,
  RECOMMENDED_ACCESS_POLICY,
  ROLE_LABELS,
  fetchAccessPolicy,
  formatDateTime,
  saveAccessPolicy,
  unlockAccount,
  type AccessPolicy,
  type AccessPolicyNumber,
  type AccessPolicyState,
  type Administrator,
} from '../../services/adminApi'
import { SESSION_LOST } from './accountStatus'
import './AccessPolicy.css'

interface Props {
  admin: Administrator
  onSessionLost: (message: string) => void
}

type Draft = Record<AccessPolicyNumber, string> & { bloqueoAutomatico: boolean }

const FIELDS: Record<AccessPolicyNumber, { label: string; unit: string; help: string }> = {
  maxFallosEquipoCuenta: { label: 'Fallos de una cuenta en el mismo equipo', unit: 'intentos', help: 'Luego Rupi pide esperar antes de dejarla probar otra vez.' },
  maxFallosEquipo: { label: 'Fallos de un equipo con cualquier cuenta', unit: 'intentos', help: 'En un aula varias niñas y niños comparten la computadora: deja margen.' },
  ventanaMinutos: { label: 'Ventana de tiempo', unit: 'minutos', help: 'Los fallos más antiguos dejan de contar.' },
  maxFallosCuenta: { label: 'Fallos que bloquean la cuenta', unit: 'intentos', help: 'Desde cualquier equipo, dentro de la ventana de tiempo.' },
  minutosBloqueo: { label: 'Duración del bloqueo', unit: 'minutos', help: 'Termina sola, o antes si la desbloqueas aquí.' },
}

const toDraft = (policy: AccessPolicy): Draft => ({
  ventanaMinutos: String(policy.ventanaMinutos),
  maxFallosEquipoCuenta: String(policy.maxFallosEquipoCuenta),
  maxFallosEquipo: String(policy.maxFallosEquipo),
  maxFallosCuenta: String(policy.maxFallosCuenta),
  minutosBloqueo: String(policy.minutosBloqueo),
  bloqueoAutomatico: policy.bloqueoAutomatico,
})

const time = (value: string) =>
  new Intl.DateTimeFormat('es-PE', { timeStyle: 'short', timeZone: 'America/Lima' }).format(new Date(value))

const plural = (value: number, one: string, many: string) => `${value} ${value === 1 ? one : many}`

/** HU-58: límite de intentos fallidos y bloqueo automático para la web, el celular y la consola. */
export function AccessPolicyView({ admin, onSessionLost }: Props) {
  const [data, setData] = useState<AccessPolicyState | null>(null)
  const [draft, setDraft] = useState<Draft | null>(null)
  const [reload, setReload] = useState(0)
  const [busy, setBusy] = useState(false)
  const [unlocking, setUnlocking] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const writable = admin.permisos.includes('acceso.configurar')
  const canUnlock = admin.permisos.includes('credenciales.restablecer')

  const fail = useCallback((failure: unknown, fallback: string) => {
    if (failure instanceof AdminApiError && failure.status === 401) onSessionLost(failure.message || SESSION_LOST)
    else setError(failure instanceof AdminApiError ? failure.message : fallback)
  }, [onSessionLost])

  useEffect(() => {
    const controller = new AbortController()
    fetchAccessPolicy(controller.signal)
      .then(value => { setData(value); setDraft(toDraft(value.politica)); setError(null) })
      .catch(failure => { if (!controller.signal.aborted) fail(failure, 'No pudimos cargar la política de acceso.') })
    return () => controller.abort()
  }, [reload, fail])

  const saved = data ? toDraft(data.politica) : null
  const dirty = !!draft && !!saved && (Object.keys(saved) as (keyof Draft)[]).some(key => saved[key] !== draft[key])

  function problem(current: Draft): string | null {
    if (!data) return null
    for (const key of Object.keys(FIELDS) as AccessPolicyNumber[]) {
      const value = Number(current[key])
      const { min, max } = data.limites[key]
      if (!Number.isInteger(value) || value < min || value > max) return `${FIELDS[key].label}: escribe un número entre ${min} y ${max}.`
    }
    if (Number(current.maxFallosEquipo) < Number(current.maxFallosEquipoCuenta)) {
      return 'El límite del equipo no puede ser menor que el de una cuenta en ese equipo.'
    }
    return null
  }

  const formProblem = draft ? problem(draft) : null

  async function save(event: FormEvent) {
    event.preventDefault()
    if (!draft || formProblem || busy) return
    setBusy(true); setError(null); setMessage(null)
    try {
      const value = await saveAccessPolicy({
        ventanaMinutos: Number(draft.ventanaMinutos),
        maxFallosEquipoCuenta: Number(draft.maxFallosEquipoCuenta),
        maxFallosEquipo: Number(draft.maxFallosEquipo),
        bloqueoAutomatico: draft.bloqueoAutomatico,
        maxFallosCuenta: Number(draft.maxFallosCuenta),
        minutosBloqueo: Number(draft.minutosBloqueo),
      })
      setData(value); setDraft(toDraft(value.politica))
      setMessage('Política guardada. Se aplica desde el próximo inicio de sesión en la web, el celular y la consola.')
    } catch (failure) {
      fail(failure, 'No pudimos guardar la política.')
    } finally {
      setBusy(false)
    }
  }

  async function unlock(id: string, name: string) {
    setUnlocking(id); setError(null); setMessage(null)
    try {
      await unlockAccount(id)
      setMessage(`${name} ya puede volver a ingresar. Sus intentos fallidos anteriores dejaron de contar.`)
      setReload(value => value + 1)
    } catch (failure) {
      fail(failure, 'No pudimos desbloquear la cuenta.')
    } finally {
      setUnlocking(null)
    }
  }

  const field = (key: AccessPolicyNumber, disabled = false) => {
    if (!draft || !data) return null
    const { min, max } = data.limites[key]
    return (
      <label className="admin-field access-field">
        <span>{FIELDS[key].label}</span>
        <span className="access-input">
          <input type="number" inputMode="numeric" min={min} max={max} step={1} value={draft[key]}
            disabled={!writable || busy || disabled}
            onChange={event => { setDraft({ ...draft, [key]: event.target.value }); setMessage(null) }} />
          <em>{FIELDS[key].unit}</em>
        </span>
        <small>{FIELDS[key].help} Entre {min} y {max}.</small>
      </label>
    )
  }

  const preview = draft && !formProblem ? draft : saved

  return (
    <section className="admin-card access-policy" aria-labelledby="access-policy-title">
      <div className="admin-section-head">
        <div>
          <h1 id="access-policy-title">Bloqueo por intentos</h1>
          <p className="admin-muted">Cuántas contraseñas equivocadas tolera Rupi antes de frenar un equipo o bloquear una cuenta. Vale para la web, el celular y esta consola.</p>
        </div>
        <button type="button" className="admin-btn admin-btn--quiet" disabled={busy}
          onClick={() => { if (!dirty || window.confirm('Hay cambios sin guardar. ¿Quieres recargar?')) { setMessage(null); setReload(value => value + 1) } }}>
          <RefreshCw size={18} aria-hidden="true" />
          Actualizar
        </button>
      </div>

      {error && <p className="admin-feedback admin-feedback--error" role="alert">{error}</p>}
      {message && <p className="admin-feedback admin-feedback--ok" role="status">{message}</p>}
      {!data && !error && <p className="admin-loading">Cargando la política de acceso…</p>}

      {data && draft && preview && <>
        <dl className="admin-session-totals access-totals" aria-label="Inicios de sesión de las últimas 24 horas">
          <div><dt>Ingresos correctos · 24 h</dt><dd>{data.ultimas24h.correctos}</dd></div>
          <div><dt>Contraseñas equivocadas · 24 h</dt><dd>{data.ultimas24h.fallidos}</dd></div>
          <div><dt>Intentos frenados · 24 h</dt><dd>{data.ultimas24h.frenados}</dd></div>
        </dl>

        <form className="access-form" onSubmit={save} noValidate>
          <fieldset className="access-group">
            <legend><Monitor size={20} aria-hidden="true" />Freno por equipo</legend>
            <p className="admin-muted">Detiene a quien prueba contraseñas desde una misma computadora o celular.</p>
            <div className="access-grid">
              {field('maxFallosEquipoCuenta')}
              {field('maxFallosEquipo')}
              {field('ventanaMinutos')}
            </div>
          </fieldset>

          <fieldset className="access-group">
            <legend><UserRoundX size={20} aria-hidden="true" />Bloqueo automático de la cuenta</legend>
            <p className="admin-muted">Protege la cuenta aunque los intentos lleguen desde varios equipos. Mientras dura, ni la contraseña correcta entra.</p>
            <label className="access-switch">
              <input type="checkbox" checked={draft.bloqueoAutomatico} disabled={!writable || busy}
                onChange={event => { setDraft({ ...draft, bloqueoAutomatico: event.target.checked }); setMessage(null) }} />
              <span>{draft.bloqueoAutomatico ? 'Activo: bloquear la cuenta tras muchos fallos' : 'Apagado: solo se frena por equipo'}</span>
            </label>
            <div className="access-grid">
              {field('maxFallosCuenta', !draft.bloqueoAutomatico)}
              {field('minutosBloqueo', !draft.bloqueoAutomatico)}
            </div>
          </fieldset>

          <p className="access-summary" aria-live="polite">
            <strong>Así funcionará:</strong> tras {plural(Number(preview.maxFallosEquipoCuenta), 'contraseña equivocada', 'contraseñas equivocadas')} de
            una cuenta en el mismo equipo, o {Number(preview.maxFallosEquipo)} de ese equipo con cualquier cuenta, Rupi pide
            esperar {plural(Number(preview.ventanaMinutos), 'minuto', 'minutos')}.{' '}
            {preview.bloqueoAutomatico
              ? <>Si la cuenta suma {Number(preview.maxFallosCuenta)} fallos en {plural(Number(preview.ventanaMinutos), 'minuto', 'minutos')} desde cualquier lugar, se bloquea {plural(Number(preview.minutosBloqueo), 'minuto', 'minutos')} o hasta que la desbloquees.</>
              : <>Las cuentas no se bloquean solas.</>}
          </p>

          {formProblem && <p className="admin-feedback admin-feedback--error" role="alert">{formProblem}</p>}

          <div className="access-footer">
            <small className="admin-muted">
              {data.actualizadoPor ? `Último cambio: ${formatDateTime(data.actualizadoEn)} · @${data.actualizadoPor}` : 'Valores iniciales: todavía nadie los cambió.'}
            </small>
            {writable ? (
              <div className="admin-actions">
                <button type="button" className="admin-btn admin-btn--quiet" disabled={busy}
                  onClick={() => { setDraft(toDraft(RECOMMENDED_ACCESS_POLICY)); setMessage(null) }}>
                  <RotateCcw size={18} aria-hidden="true" />
                  Valores recomendados
                </button>
                <button type="submit" className="admin-btn admin-btn--leaf" disabled={busy || !dirty || !!formProblem}>
                  <LockKeyhole size={18} aria-hidden="true" />
                  {busy ? 'Guardando…' : 'Guardar política'}
                </button>
              </div>
            ) : <p className="admin-muted">Tu rol puede consultar la política, pero no cambiarla.</p>}
          </div>
        </form>

        <section className="access-locked" aria-labelledby="access-locked-title">
          <h2 id="access-locked-title">Cuentas bloqueadas ahora</h2>
          {data.cuentasBloqueadas.length === 0 ? (
            <p className="admin-empty-line">Ninguna cuenta está bloqueada por intentos fallidos.</p>
          ) : (
            <ul className="access-locked-list">
              {data.cuentasBloqueadas.map(account => (
                <li key={account.id} className="access-locked-row">
                  <span className="admin-account-name">
                    <strong>{account.nombre}</strong>
                    <span>@{account.usuario} · {account.roles.map(code => ROLE_LABELS[code] ?? code).join(', ') || 'Sin rol'}</span>
                  </span>
                  <span className="admin-chip admin-chip--warn">Hasta las {time(account.bloqueadoHasta)}</span>
                  {canUnlock && account.id !== admin.id && (
                    <button type="button" className="admin-btn admin-btn--leaf" disabled={unlocking !== null}
                      onClick={() => unlock(account.id, account.nombre)}>
                      <LockOpen size={18} aria-hidden="true" />
                      {unlocking === account.id ? 'Desbloqueando…' : 'Desbloquear'}
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      </>}
    </section>
  )
}
