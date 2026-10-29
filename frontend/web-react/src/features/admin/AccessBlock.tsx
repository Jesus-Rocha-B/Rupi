import { useState, type FormEvent } from 'react'
import { KeyRound, LockOpen } from 'lucide-react'
import { formatDateTime, lockAccount, resetPassword, unlockAccount, type AccountDetail } from '../../services/adminApi'

interface Props {
  account: AccountDetail
  busy: boolean
  /** Ejecuta la acción con el manejo de errores del panel; devuelve si salió bien. */
  run: (action: () => Promise<AccountDetail>, success: string | ((account: AccountDetail) => string)) => Promise<boolean>
}

type Mode = 'idle' | 'typing' | 'confirmLock'

/** HU-44: contraseñas y desbloqueo a pedido de la docente o la familia. */
export function AccessBlock({ account, busy, run }: Props) {
  const [mode, setMode] = useState<Mode>('idle')
  const [password, setPassword] = useState('')
  const [repeat, setRepeat] = useState('')
  const [formError, setFormError] = useState<string | null>(null)

  async function handleTyped(event: FormEvent) {
    event.preventDefault()
    if (password.length < 8) {
      setFormError('La contraseña debe tener al menos 8 caracteres.')
      return
    }
    if (password !== repeat) {
      setFormError('Las dos contraseñas no coinciden.')
      return
    }
    setFormError(null)
    if (
      await run(
        () => resetPassword(account.id, password),
        acc =>
          acc.correoNotificado && acc.correoDestino
            ? `Contraseña cambiada. Se envió correo de notificación a ${acc.correoDestino}.`
            : 'Contraseña cambiada. Entrégala en persona.'
      )
    ) {
      setPassword('')
      setRepeat('')
      setMode('idle')
    }
  }

  return (
    <div className="admin-form">
      <dl className="admin-facts">
        <div><dt>Contraseña</dt><dd>Cambiada el {formatDateTime(account.claveCambiadaEn)}</dd></div>
        <div>
          <dt>Intentos fallidos</dt>
          <dd>
            {account.intentosFallidos} en los últimos 15 minutos
            {account.intentosFallidos >= 5 && (
              <small className="admin-masked-note">Tras 5 seguidos, Rupi espera 5 minutos antes de dejarla intentar otra vez.</small>
            )}
          </dd>
        </div>
      </dl>

      {mode === 'typing' ? (
        <form className="admin-form admin-subform" onSubmit={handleTyped} noValidate>
          <label className="admin-field">
            <span>Nueva contraseña</span>
            <input type="password" autoComplete="new-password" value={password} maxLength={64}
              onChange={event => setPassword(event.target.value)} />
            <small>Al menos 8 caracteres y sin el nombre de usuario.</small>
          </label>
          <label className="admin-field">
            <span>Repítela</span>
            <input type="password" autoComplete="new-password" value={repeat} maxLength={64}
              onChange={event => setRepeat(event.target.value)} />
          </label>
          {formError && <p className="admin-feedback admin-feedback--error" role="alert">{formError}</p>}
          <div className="admin-actions">
            <button type="button" className="admin-btn admin-btn--quiet" onClick={() => setMode('idle')} disabled={busy}>Cancelar</button>
            <button type="submit" className="admin-btn admin-btn--sky" disabled={busy}>Guardar contraseña</button>
          </div>
        </form>
      ) : (
        <div className="admin-actions admin-actions--start">
          <button
            type="button"
            className="admin-btn admin-btn--sky"
            disabled={busy}
            onClick={() =>
              run(
                () => resetPassword(account.id),
                acc =>
                  acc.correoNotificado && acc.correoDestino
                    ? `Se generó una clave temporal y se envió a ${acc.correoDestino}.`
                    : 'Se generó una clave temporal y se cerró su sesión.'
              )
            }
          >
            <KeyRound size={18} aria-hidden="true" />
            Generar clave temporal
          </button>
          <button type="button" className="admin-btn admin-btn--quiet" disabled={busy} onClick={() => setMode('typing')}>
            Escribir una contraseña
          </button>
        </div>
      )}

      {account.bloqueada ? (
        <div className="admin-confirm admin-confirm--calm">
          <p>Está bloqueada: no puede ingresar a Rupi hasta que la desbloquees.</p>
          <button
            type="button"
            className="admin-btn admin-btn--leaf"
            disabled={busy}
            onClick={() =>
              run(
                () => unlockAccount(account.id),
                acc =>
                  acc.correoNotificado && acc.correoDestino
                    ? `Cuenta desbloqueada. Se notificó por correo a ${acc.correoDestino}.`
                    : 'Cuenta desbloqueada. Ya puede ingresar.'
              )
            }
          >
            <LockOpen size={18} aria-hidden="true" />
            Desbloquear
          </button>
        </div>
      ) : mode === 'confirmLock' ? (
        <div className="admin-confirm" role="group" aria-label="Confirmar bloqueo">
          <p>No podrá ingresar y se cerrará su sesión abierta. Úsalo si la clave se compartió o alguien la usa sin permiso.</p>
          <div className="admin-actions">
            <button type="button" className="admin-btn admin-btn--quiet" onClick={() => setMode('idle')}>Cancelar</button>
            <button
              type="button"
              className="admin-btn admin-btn--danger"
              disabled={busy}
              onClick={async () => {
                await run(() => lockAccount(account.id), 'Cuenta bloqueada.')
                setMode('idle')
              }}
            >
              Sí, bloquear
            </button>
          </div>
        </div>
      ) : (
        <div className="admin-actions admin-actions--start">
          <button type="button" className="admin-btn admin-btn--quiet" disabled={busy} onClick={() => setMode('confirmLock')}>
            Bloquear cuenta
          </button>
        </div>
      )}
    </div>
  )
}
