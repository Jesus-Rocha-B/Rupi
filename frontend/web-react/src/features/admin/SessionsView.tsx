import { useEffect, useState } from 'react'
import { Laptop, LogOut, MonitorCog, RefreshCw, Smartphone } from 'lucide-react'
import {
  AdminApiError,
  CLIENT_LABELS,
  ROLE_LABELS,
  closeAccountSession,
  fetchActiveSessions,
  formatDateTime,
  type ActiveSessions,
  type Administrator,
  type SessionClient,
} from '../../services/adminApi'
import { SESSION_LOST } from './accountStatus'

interface Props {
  admin: Administrator
  onSessionLost: (message: string) => void
}

const CLIENT_ICONS = { WEB_USUARIO: Laptop, MOVIL: Smartphone, WEB_ADMIN: MonitorCog }
const FILTERS: (SessionClient | '')[] = ['', 'WEB_USUARIO', 'MOVIL', 'WEB_ADMIN']

export function SessionsView({ admin, onSessionLost }: Props) {
  const [client, setClient] = useState<SessionClient | ''>('')
  const [reloadKey, setReloadKey] = useState(0)
  const [result, setResult] = useState<{ key: string; data: ActiveSessions | null; error: string | null } | null>(null)
  const [closing, setClosing] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const requestKey = `${client}-${reloadKey}`

  useEffect(() => {
    const controller = new AbortController()
    fetchActiveSessions(client, controller.signal)
      .then(data => setResult({ key: requestKey, data, error: null }))
      .catch(failure => {
        if (controller.signal.aborted) return
        if (failure instanceof AdminApiError && failure.status === 401) onSessionLost(failure.message || SESSION_LOST)
        setResult(previous => ({
          key: requestKey,
          data: previous?.data ?? null,
          error: failure instanceof AdminApiError ? failure.message : 'No pudimos cargar las sesiones.',
        }))
      })
    return () => controller.abort()
  }, [client, requestKey, onSessionLost])

  const data = result?.data ?? null
  const loading = result?.key !== requestKey
  const error = result?.key === requestKey ? result.error : null

  async function close(accountId: string, name: string) {
    setClosing(accountId)
    setMessage(null)
    try {
      await closeAccountSession(accountId)
      setMessage(`Se cerró la sesión de ${name}. Tendrá que volver a ingresar.`)
      setReloadKey(value => value + 1)
    } catch (failure) {
      if (failure instanceof AdminApiError && failure.status === 401) onSessionLost(failure.message || SESSION_LOST)
      else setMessage(failure instanceof AdminApiError ? failure.message : 'No pudimos cerrar la sesión.')
    } finally {
      setClosing(null)
    }
  }

  return (
    <section className="admin-card" aria-labelledby="admin-sessions-title">
      <div className="admin-section-head">
        <div>
          <h1 id="admin-sessions-title">Sesiones</h1>
          <p className="admin-muted">Dónde está abierta cada cuenta en este momento.</p>
        </div>
        <button type="button" className="admin-btn admin-btn--quiet" onClick={() => setReloadKey(value => value + 1)} disabled={loading}>
          <RefreshCw size={18} aria-hidden="true" />
          Actualizar
        </button>
      </div>

      <div className="admin-policy">
        <span className="admin-policy-seal" aria-hidden="true">1</span>
        <div>
          <h2>Una sola sesión por cuenta</h2>
          <p>
            Si una estudiante entra desde el celular, se cierra su sesión en la computadora del aula, y al revés. La otra
            pantalla le explica por qué. Así nadie usa la misma cuenta en dos lugares a la vez ni se mezclan respuestas.
          </p>
          <p className="admin-policy-state">Activa para todas las cuentas · la garantiza la base de datos</p>
        </div>
      </div>

      {data && (
        <dl className="admin-session-totals">
          {(['WEB_USUARIO', 'MOVIL', 'WEB_ADMIN'] as const).map(code => {
            const Icon = CLIENT_ICONS[code]
            return (
              <div key={code}>
                <dt><Icon size={18} aria-hidden="true" />{CLIENT_LABELS[code]}</dt>
                <dd>{data.totales[code] ?? 0}</dd>
              </div>
            )
          })}
        </dl>
      )}

      <div className="admin-segmented" role="group" aria-label="Filtrar por lugar">
        {FILTERS.map(code => (
          <button
            key={code || 'todas'}
            type="button"
            className={client === code ? 'is-current' : undefined}
            aria-pressed={client === code}
            onClick={() => setClient(code)}
          >
            {code ? CLIENT_LABELS[code] : 'Todas'}
          </button>
        ))}
      </div>

      {message && <p className="admin-feedback admin-feedback--info" role="status">{message}</p>}
      {error && <p className="admin-feedback admin-feedback--error" role="alert">{error}</p>}
      {loading && !data && <p className="admin-loading">Buscando sesiones abiertas…</p>}

      {data && data.items.length === 0 && <p className="admin-empty-line">No hay sesiones abiertas con este filtro.</p>}
      <ul className="admin-session-list">
        {data?.items.map(item => {
          const Icon = CLIENT_ICONS[item.cliente]
          const isSelf = item.cuentaId === admin.id
          return (
            <li key={item.cuentaId} className="admin-session-row">
              <span className={`admin-session-place admin-session-place--${item.cliente.toLowerCase()}`}>
                <Icon size={20} aria-hidden="true" />
                {CLIENT_LABELS[item.cliente]}
              </span>
              <span className="admin-account-name">
                <strong>{item.nombre}</strong>
                <span>@{item.usuario} · {item.roles.map(code => ROLE_LABELS[code] ?? code).join(', ')}</span>
              </span>
              <span className="admin-session-times">
                <span>Desde {formatDateTime(item.desde)}</span>
                <span>Última actividad {formatDateTime(item.actividad)}</span>
              </span>
              {isSelf ? (
                <span className="admin-chip admin-chip--neutral">Eres tú</span>
              ) : (
                <button
                  type="button"
                  className="admin-btn admin-btn--danger-quiet"
                  disabled={closing !== null}
                  onClick={() => close(item.cuentaId, item.nombre)}
                >
                  <LogOut size={18} aria-hidden="true" />
                  {closing === item.cuentaId ? 'Cerrando…' : 'Cerrar sesión'}
                </button>
              )}
            </li>
          )
        })}
      </ul>
    </section>
  )
}
