import { useState, type FormEvent } from 'react'
import { X } from 'lucide-react'
import { AdminApiError, createAccount, type AccountDetail, type NewAccount } from '../../services/adminApi'
import { SESSION_LOST } from './accountStatus'

interface Props {
  onCancel: () => void
  onCreated: (account: AccountDetail) => void
  onSessionLost: (message: string) => void
}

const EMPTY: NewAccount = { usuario: '', nombre: '', apellido: '', nombrePreferido: '', correo: '', rol: 'ESTUDIANTE' }

/** Propone un usuario «nombre.apellido» sin tildes ni espacios; la persona puede cambiarlo. */
function suggestUsername(nombre: string, apellido: string) {
  const clean = (value: string) => value.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
    .replace(/[^a-z0-9]+/g, ' ').trim().split(' ')[0] ?? ''
  return [clean(nombre), clean(apellido)].filter(Boolean).join('.')
}

export function NewAccountForm({ onCancel, onCreated, onSessionLost }: Props) {
  const [form, setForm] = useState<NewAccount>(EMPTY)
  const [usernameTouched, setUsernameTouched] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function update<K extends keyof NewAccount>(key: K, value: NewAccount[K]) {
    setForm(previous => {
      const next = { ...previous, [key]: value }
      if (!usernameTouched && (key === 'nombre' || key === 'apellido')) {
        next.usuario = suggestUsername(next.nombre, next.apellido)
      }
      return next
    })
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!form.nombre.trim() || !form.apellido.trim() || !form.usuario.trim()) {
      setError('Completa nombre, apellido y usuario.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      onCreated(await createAccount(form))
    } catch (failure) {
      if (failure instanceof AdminApiError && failure.status === 401) onSessionLost(failure.message || SESSION_LOST)
      else setError(failure instanceof AdminApiError ? failure.message : 'No pudimos crear la cuenta.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="admin-card admin-panel" aria-labelledby="admin-new-title">
      <div className="admin-panel-head">
        <h2 id="admin-new-title">Nueva cuenta</h2>
        <button type="button" className="admin-icon-btn" onClick={onCancel} aria-label="Cerrar formulario">
          <X size={20} aria-hidden="true" />
        </button>
      </div>
      <form className="admin-form" onSubmit={handleSubmit} noValidate>
        <fieldset className="admin-role-pick">
          <legend>¿Para quién es?</legend>
          {(['ESTUDIANTE', 'DOCENTE'] as const).map(code => (
            <label key={code} className={`admin-role-option${form.rol === code ? ' is-checked' : ''}`}>
              <input
                type="radio"
                name="rol"
                value={code}
                checked={form.rol === code}
                onChange={() => update('rol', code)}
              />
              {code === 'ESTUDIANTE' ? 'Estudiante' : 'Docente'}
            </label>
          ))}
        </fieldset>
        <div className="admin-form-row">
          <label className="admin-field">
            <span>Nombre</span>
            <input value={form.nombre} onChange={event => update('nombre', event.target.value)} maxLength={100} />
          </label>
          <label className="admin-field">
            <span>Apellido</span>
            <input value={form.apellido} onChange={event => update('apellido', event.target.value)} maxLength={100} />
          </label>
        </div>
        <label className="admin-field">
          <span>Usuario para ingresar</span>
          <input
            value={form.usuario}
            autoCapitalize="none"
            spellCheck={false}
            maxLength={80}
            onChange={event => {
              setUsernameTouched(true)
              update('usuario', event.target.value)
            }}
          />
          <small>Letras sin tildes, números, punto o guion.</small>
        </label>
        <label className="admin-field">
          <span>Cómo le gusta que la llamen <em>(opcional)</em></span>
          <input
            value={form.nombrePreferido}
            onChange={event => update('nombrePreferido', event.target.value)}
            maxLength={100}
          />
        </label>
        <label className="admin-field">
          <span>Correo <em>(opcional)</em></span>
          <input
            type="email"
            value={form.correo}
            autoCapitalize="none"
            onChange={event => update('correo', event.target.value)}
            maxLength={254}
          />
        </label>
        <p className="admin-muted">Rupi genera una clave temporal fácil de dictar. La verás una sola vez al crear la cuenta.</p>
        {error && <p className="admin-feedback admin-feedback--error" role="alert">{error}</p>}
        <div className="admin-actions">
          <button type="button" className="admin-btn admin-btn--quiet" onClick={onCancel} disabled={busy}>Cancelar</button>
          <button type="submit" className="admin-btn admin-btn--primary" disabled={busy}>
            {busy ? 'Creando…' : 'Crear cuenta'}
          </button>
        </div>
      </form>
    </section>
  )
}
