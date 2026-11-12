import { useCallback, useEffect, useMemo, useState } from 'react'
import { CheckCircle2, Clock, RefreshCw, RotateCcw, Search, ShieldCheck } from 'lucide-react'
import {
  AdminApiError,
  ROLE_LABELS,
  confirmMyTwoFactor,
  fetchTwoFactorOverview,
  formatDateTime,
  resetAccountTwoFactor,
  rotateMyTwoFactor,
  type Administrator,
  type TwoFactorOverview,
  type TwoFactorResetResult,
} from '../../services/adminApi'
import { TwoFactorSetupCard } from '../../components/TwoFactorSetupCard'
import { SESSION_LOST } from './accountStatus'
import './TwoFactorView.css'

interface Props {
  admin: Administrator
  onSessionLost: (message: string) => void
}

export function TwoFactorView({ admin, onSessionLost }: Props) {
  const [overview, setOverview] = useState<TwoFactorOverview | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<'TODOS' | 'ACTIVO' | 'PENDIENTE'>('TODOS')
  const [resetPreview, setResetPreview] = useState<TwoFactorResetResult | null>(null)

  const load = useCallback((signal?: AbortSignal) => {
    fetchTwoFactorOverview(signal)
      .then(data => {
        setOverview(data)
        setError(null)
      })
      .catch(failure => {
        if (signal?.aborted) return
        if (failure instanceof AdminApiError && failure.status === 401) {
          onSessionLost(failure.message || SESSION_LOST)
        } else {
          setError(failure instanceof AdminApiError ? failure.message : 'No pudimos cargar el estado de doble factor.')
        }
      })
  }, [onSessionLost])

  useEffect(() => {
    const controller = new AbortController()
    load(controller.signal)
    return () => controller.abort()
  }, [load])

  const filteredAccounts = useMemo(() => {
    if (!overview) return []
    const needle = search.trim().toLowerCase()
    return overview.cuentas.filter(item => {
      if (statusFilter === 'ACTIVO' && !item.verificado) return false
      if (statusFilter === 'PENDIENTE' && item.verificado) return false
      if (!needle) return true
      return (
        item.usuario.toLowerCase().includes(needle)
        || item.nombre.toLowerCase().includes(needle)
        || item.roles.some(r => r.toLowerCase().includes(needle))
      )
    })
  }, [overview, search, statusFilter])

  const summary = useMemo(() => {
    const total = overview?.cuentas.length ?? 0
    const active = overview?.cuentas.filter(c => c.verificado).length ?? 0
    return { total, active, pending: total - active }
  }, [overview])

  async function handleRotateOwn() {
    setBusy(true)
    setError(null)
    setNotice(null)
    try {
      const updated = await rotateMyTwoFactor()
      setOverview(prev => (prev ? { ...prev, propio: updated } : prev))
      setNotice('Generamos una nueva clave secreta. Escanea el código QR y confirma con el código de 6 dígitos.')
    } catch (failure) {
      if (failure instanceof AdminApiError && failure.status === 401) onSessionLost(failure.message || SESSION_LOST)
      else setError(failure instanceof AdminApiError ? failure.message : 'No pudimos rotar tu clave 2FA.')
    } finally {
      setBusy(false)
    }
  }

  async function handleConfirmOwn(code: string) {
    setBusy(true)
    setError(null)
    setNotice(null)
    try {
      const updated = await confirmMyTwoFactor(code)
      setOverview(prev => (prev ? { ...prev, propio: updated } : prev))
      setNotice('Tu doble factor (2FA) quedó verificado y activo.')
      load()
    } catch (failure) {
      if (failure instanceof AdminApiError && failure.status === 401) onSessionLost(failure.message || SESSION_LOST)
      else setError(failure instanceof AdminApiError ? failure.message : 'Código temporal inválido o vencido.')
    } finally {
      setBusy(false)
    }
  }

  async function handleResetAccount(accountId: string) {
    setBusy(true)
    setError(null)
    setNotice(null)
    try {
      const result = await resetAccountTwoFactor(accountId)
      setResetPreview(result)
      setNotice(`Se reinició el doble factor de @${result.usuario}. Deberá escanear el nuevo QR en su próximo acceso.`)
      load()
    } catch (failure) {
      if (failure instanceof AdminApiError && failure.status === 401) onSessionLost(failure.message || SESSION_LOST)
      else setError(failure instanceof AdminApiError ? failure.message : 'No pudimos reiniciar el doble factor.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="tfa-grid">
      <section className="admin-card tfa-panel">
        <div className="admin-head">
          <div>
            <h1>Autenticación de doble factor (2FA)</h1>
            <p className="admin-muted">
              Obligatoria para todas las cuentas con privilegios de administración, soporte y docentes (RFC 6238 TOTP).
            </p>
          </div>
          <button type="button" className="admin-btn admin-btn--quiet" onClick={() => load()} disabled={busy}>
            <RefreshCw size={16} aria-hidden="true" />
            Actualizar
          </button>
        </div>

        {notice && <p className="admin-feedback admin-feedback--ok" role="status">{notice}</p>}
        {error && <p className="admin-feedback admin-feedback--error" role="alert">{error}</p>}

        {overview && (
          <div className="tfa-own-box">
            <div className="tfa-own-status">
              <div>
                <span className="tfa-eyebrow">Tu cuenta (@{admin.usuario})</span>
                <h2>
                  {overview.propio.verificado
                    ? 'Doble factor activado y protegido'
                    : 'Pendiente de escanear código QR'}
                </h2>
                <p className="admin-muted">
                  {overview.propio.activadoEn
                    ? `Activado el ${formatDateTime(overview.propio.activadoEn)}`
                    : 'Escanea el QR con tu aplicación autenticadora para vincular tu dispositivo.'}
                </p>
              </div>
              <button
                type="button"
                className="admin-btn admin-btn--sky"
                onClick={handleRotateOwn}
                disabled={busy}
              >
                <RotateCcw size={16} aria-hidden="true" />
                {overview.propio.verificado ? 'Generar nuevo QR' : 'Rotar clave QR'}
              </button>
            </div>

            {overview.propio.otpauthUri && overview.propio.secretoBase32 && (
              <div className="tfa-setup-wrap">
                <TwoFactorSetupCard
                  username={admin.usuario}
                  otpauthUri={overview.propio.otpauthUri}
                  secretBase32={overview.propio.secretoBase32}
                  busy={busy}
                  error={null}
                  submitLabel="Confirmar código QR"
                  onVerify={handleConfirmOwn}
                />
              </div>
            )}
          </div>
        )}

        {resetPreview && (
          <div className="tfa-reset-preview" role="region" aria-label="Nuevo QR tras reinicio">
            <div className="tfa-reset-head">
              <div>
                <h3>QR reiniciado para @{resetPreview.usuario}</h3>
                <p className="admin-muted">
                  La persona también verá este mismo QR automáticamente la próxima vez que inicie sesión.
                </p>
              </div>
              <button type="button" className="admin-btn admin-btn--quiet" onClick={() => setResetPreview(null)}>
                Ocultar
              </button>
            </div>
            <div className="tfa-reset-body">
              <code>{resetPreview.otpauthUri}</code>
              <span>Clave manual: <strong>{resetPreview.secretoBase32}</strong></span>
            </div>
          </div>
        )}
      </section>

      {overview && overview.cuentas.length > 0 && (
        <section className="admin-card tfa-panel">
          <div className="tfa-summary-bar">
            <div className="tfa-stat">
              <ShieldCheck size={18} aria-hidden="true" />
              <span><strong>{summary.total}</strong> cuentas obligatorias</span>
            </div>
            <div className="tfa-stat tfa-stat--ok">
              <CheckCircle2 size={18} aria-hidden="true" />
              <span><strong>{summary.active}</strong> con 2FA activo</span>
            </div>
            <div className="tfa-stat tfa-stat--warn">
              <Clock size={18} aria-hidden="true" />
              <span><strong>{summary.pending}</strong> pendientes de QR</span>
            </div>
          </div>

          <div className="admin-filters">
            <label className="admin-search">
              <Search size={17} aria-hidden="true" />
              <input
                type="search"
                placeholder="Buscar usuario, nombre o rol…"
                value={search}
                onChange={event => setSearch(event.target.value)}
              />
            </label>
            <select
              value={statusFilter}
              onChange={event => setStatusFilter(event.target.value as 'TODOS' | 'ACTIVO' | 'PENDIENTE')}
              aria-label="Filtrar por estado de doble factor"
            >
              <option value="TODOS">Todos los estados</option>
              <option value="ACTIVO">2FA activo</option>
              <option value="PENDIENTE">Pendiente de QR</option>
            </select>
          </div>

          {filteredAccounts.length === 0 ? (
            <p className="admin-empty-line">No hay cuentas obligatorias con ese filtro.</p>
          ) : (
            <ul className="tfa-account-list">
              {filteredAccounts.map(row => (
                <li key={row.id} className="tfa-account-row">
                  <div className="tfa-account-info">
                    <div className="tfa-account-title">
                      <strong>{row.nombre}</strong>
                      <span>@{row.usuario}</span>
                    </div>
                    <div className="admin-chips">
                      {row.roles.map(code => (
                        <span key={code} className={`admin-chip admin-chip--role-${code.toLowerCase()}`}>
                          {ROLE_LABELS[code] ?? code}
                        </span>
                      ))}
                      <span className={`admin-chip ${row.verificado ? 'admin-chip--ok' : 'admin-chip--warn'}`}>
                        {row.verificado ? '2FA Activo' : 'Pendiente QR'}
                      </span>
                    </div>
                    {row.activadoEn && (
                      <small className="admin-muted">Activado: {formatDateTime(row.activadoEn)}</small>
                    )}
                  </div>
                  {overview.puedeGestionar && (
                    <button
                      type="button"
                      className="admin-btn admin-btn--quiet"
                      disabled={busy}
                      onClick={() => handleResetAccount(row.id)}
                    >
                      <RotateCcw size={15} aria-hidden="true" />
                      Reiniciar QR
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </div>
  )
}
