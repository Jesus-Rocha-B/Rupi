import { useEffect, useState, type FormEvent } from 'react'
import { Pencil, UserCheck, UserX, X } from 'lucide-react'
import {
  AUDIT_LABELS,
  AdminApiError,
  ROLE_LABELS,
  deactivateAccount,
  displayName,
  fetchAccount,
  formatDateTime,
  reactivateAccount,
  updateAccount,
  type AccountChanges,
  type AccountDetail,
  type Administrator,
} from '../../services/adminApi'
import { SESSION_LOST, stateLabel, stateTone } from './accountStatus'
import { TemporaryPasswordTicket } from './TemporaryPasswordTicket'

interface Props {
  accountId: string
  created?: AccountDetail
  admin: Administrator
  onClose: () => void
  onChanged: () => void
  onSessionLost: (message: string) => void
}

export function AccountPanel({ accountId, created, admin, onClose, onChanged, onSessionLost }: Props) {
  const [account, setAccount] = useState<AccountDetail | null>(created ?? null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [editing, setEditing] = useState(false)
  const [confirmingDeactivation, setConfirmingDeactivation] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const canManage = admin.permisos.includes('cuentas.gestionar')

  useEffect(() => {
    if (created) return
    const controller = new AbortController()
    fetchAccount(accountId, controller.signal)
      .then(setAccount)
      .catch(failure => {
        if (controller.signal.aborted) return
        if (failure instanceof AdminApiError && failure.status === 401) onSessionLost(SESSION_LOST)
        else setError(failure instanceof AdminApiError ? failure.message : 'No pudimos abrir la cuenta.')
      })
    return () => controller.abort()
  }, [accountId, created, onSessionLost])

  async function run(action: () => Promise<AccountDetail>, success: string) {
    setBusy(true)
    setError(null)
    setMessage(null)
    try {
      setAccount(await action())
      setMessage(success)
      onChanged()
      return true
    } catch (failure) {
      if (failure instanceof AdminApiError && failure.status === 401) onSessionLost(SESSION_LOST)
      else setError(failure instanceof AdminApiError ? failure.message : 'No pudimos guardar el cambio.')
      return false
    } finally {
      setBusy(false)
    }
  }

  if (!account) {
    return (
      <section className="admin-card admin-panel" aria-busy={!error}>
        <div className="admin-panel-head">
          <h2>Cuenta</h2>
          <button type="button" className="admin-icon-btn" onClick={onClose} aria-label="Cerrar panel">
            <X size={20} aria-hidden="true" />
          </button>
        </div>
        {error ? <p className="admin-feedback admin-feedback--error" role="alert">{error}</p>
          : <p className="admin-loading">Cargando la cuenta…</p>}
      </section>
    )
  }

  const isSelf = account.id === admin.id
  const disabled = account.estado === 'DESHABILITADO'

  return (
    <section className="admin-card admin-panel" aria-labelledby="admin-account-title">
      <div className="admin-panel-head">
        <div>
          <h2 id="admin-account-title">{displayName(account)}</h2>
          <p className="admin-muted">@{account.usuario}</p>
        </div>
        <button type="button" className="admin-icon-btn" onClick={onClose} aria-label="Cerrar panel">
          <X size={20} aria-hidden="true" />
        </button>
      </div>

      {account.claveTemporal && (
        <TemporaryPasswordTicket username={account.usuario} password={account.claveTemporal} title="Cuenta creada" />
      )}
      {message && <p className="admin-feedback admin-feedback--ok" role="status">{message}</p>}
      {error && <p className="admin-feedback admin-feedback--error" role="alert">{error}</p>}

      <div className="admin-panel-block">
        <div className="admin-block-head">
          <h3>Datos</h3>
          {canManage && !editing && (
            <button type="button" className="admin-btn admin-btn--quiet" onClick={() => setEditing(true)} disabled={busy}>
              <Pencil size={16} aria-hidden="true" />
              Editar
            </button>
          )}
        </div>
        {editing ? (
          <AccountEditForm
            account={account}
            busy={busy}
            onCancel={() => setEditing(false)}
            onSave={async changes => {
              if (await run(() => updateAccount(account.id, changes), 'Datos guardados.')) setEditing(false)
            }}
          />
        ) : (
          <dl className="admin-facts">
            <div><dt>Nombre</dt><dd>{[account.nombre, account.apellido].filter(Boolean).join(' ') || '—'}</dd></div>
            <div><dt>Le dicen</dt><dd>{account.nombrePreferido || '—'}</dd></div>
            <div><dt>Correo</dt><dd>{account.correo || '—'}</dd></div>
            <div><dt>Rol</dt><dd>{account.roles.map(code => ROLE_LABELS[code] ?? code).join(', ') || 'Sin rol'}</dd></div>
            <div>
              <dt>Estado</dt>
              <dd><span className={`admin-chip admin-chip--${stateTone(account)}`}>{stateLabel(account)}</span></dd>
            </div>
            <div><dt>Creada</dt><dd>{formatDateTime(account.creadoEn)}</dd></div>
            <div><dt>Último ingreso</dt><dd>{formatDateTime(account.ultimoIngreso)}</dd></div>
          </dl>
        )}
      </div>

      {canManage && (
        <div className="admin-panel-block">
          <h3>Alta y baja</h3>
          {disabled ? (
            <>
              <p className="admin-muted">Dada de baja el {formatDateTime(account.desactivadoEn)}. No puede ingresar a Rupi.</p>
              <button
                type="button"
                className="admin-btn admin-btn--leaf"
                disabled={busy}
                onClick={() => run(() => reactivateAccount(account.id), 'La cuenta vuelve a estar activa.')}
              >
                <UserCheck size={18} aria-hidden="true" />
                Reactivar cuenta
              </button>
            </>
          ) : confirmingDeactivation ? (
            <div className="admin-confirm" role="group" aria-label="Confirmar baja">
              <p>
                <strong>{displayName(account)}</strong> no podrá ingresar y se cerrará su sesión abierta.
                Su progreso y sus notas se conservan.
              </p>
              <div className="admin-actions">
                <button type="button" className="admin-btn admin-btn--quiet" onClick={() => setConfirmingDeactivation(false)}>
                  Cancelar
                </button>
                <button
                  type="button"
                  className="admin-btn admin-btn--danger"
                  disabled={busy}
                  onClick={async () => {
                    await run(() => deactivateAccount(account.id), 'Cuenta dada de baja.')
                    setConfirmingDeactivation(false)
                  }}
                >
                  Sí, dar de baja
                </button>
              </div>
            </div>
          ) : (
            <>
              <p className="admin-muted">
                {isSelf ? 'No puedes dar de baja tu propia cuenta.' : 'Úsalo cuando alguien deja el colegio o ya no debe ingresar.'}
              </p>
              <button
                type="button"
                className="admin-btn admin-btn--danger-quiet"
                disabled={busy || isSelf}
                onClick={() => setConfirmingDeactivation(true)}
              >
                <UserX size={18} aria-hidden="true" />
                Dar de baja
              </button>
            </>
          )}
        </div>
      )}

      <div className="admin-panel-block">
        <h3>Historial</h3>
        {account.historial.length === 0 ? (
          <p className="admin-muted">Sin cambios registrados desde la consola.</p>
        ) : (
          <ol className="admin-history">
            {account.historial.map((entry, index) => (
              <li key={`${entry.fecha}-${index}`}>
                <span>{AUDIT_LABELS[entry.accion] ?? entry.accion}</span>
                <time dateTime={entry.fecha ?? undefined}>{formatDateTime(entry.fecha)}</time>
              </li>
            ))}
          </ol>
        )}
      </div>
    </section>
  )
}

function AccountEditForm({ account, busy, onCancel, onSave }: {
  account: AccountDetail
  busy: boolean
  onCancel: () => void
  onSave: (changes: AccountChanges) => void
}) {
  const [form, setForm] = useState({
    nombre: account.nombre ?? '',
    apellido: account.apellido ?? '',
    nombrePreferido: account.nombrePreferido ?? '',
    correo: account.correo ?? '',
    usuario: account.usuario,
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    onSave(form)
  }

  return (
    <form className="admin-form" onSubmit={handleSubmit} noValidate>
      <div className="admin-form-row">
        <label className="admin-field">
          <span>Nombre</span>
          <input value={form.nombre} maxLength={100} onChange={event => setForm({ ...form, nombre: event.target.value })} />
        </label>
        <label className="admin-field">
          <span>Apellido</span>
          <input value={form.apellido} maxLength={100} onChange={event => setForm({ ...form, apellido: event.target.value })} />
        </label>
      </div>
      <label className="admin-field">
        <span>Le dicen <em>(opcional)</em></span>
        <input
          value={form.nombrePreferido}
          maxLength={100}
          onChange={event => setForm({ ...form, nombrePreferido: event.target.value })}
        />
      </label>
      <label className="admin-field">
        <span>Usuario</span>
        <input
          value={form.usuario}
          maxLength={80}
          autoCapitalize="none"
          spellCheck={false}
          onChange={event => setForm({ ...form, usuario: event.target.value })}
        />
      </label>
      <label className="admin-field">
        <span>Correo <em>(opcional)</em></span>
        <input
          type="email"
          value={form.correo}
          maxLength={254}
          autoCapitalize="none"
          onChange={event => setForm({ ...form, correo: event.target.value })}
        />
      </label>
      <div className="admin-actions">
        <button type="button" className="admin-btn admin-btn--quiet" onClick={onCancel} disabled={busy}>Cancelar</button>
        <button type="submit" className="admin-btn admin-btn--primary" disabled={busy}>{busy ? 'Guardando…' : 'Guardar'}</button>
      </div>
    </form>
  )
}
