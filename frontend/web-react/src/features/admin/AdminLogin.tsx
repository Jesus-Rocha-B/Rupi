import { useState, type FormEvent } from 'react'
import { KeyRound } from 'lucide-react'
import { AdminApiError, adminLogin, type Administrator } from '../../services/adminApi'

interface Props {
  notice: string | null
  serviceDown: boolean
  onLogin: (admin: Administrator) => void
}

export function AdminLogin({ notice, serviceDown, onLogin }: Props) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!username.trim() || !password) {
      setError('Escribe tu usuario y tu contraseña.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      onLogin(await adminLogin(username.trim(), password))
    } catch (failure) {
      setPassword('')
      setError(failure instanceof AdminApiError && failure.status !== 500
        ? failure.message
        : 'No pudimos conectar con la API de administración. Revisa que esté encendida.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="admin-shell admin-shell--center">
      <form className="admin-card admin-login" onSubmit={handleSubmit} noValidate>
        <span className="admin-brand-mark admin-brand-mark--large" aria-hidden="true">R</span>
        <h1>Administración de Rupi</h1>
        <p className="admin-muted">Solo para el equipo que gestiona cuentas, roles y la salud de la plataforma.</p>
        {notice && <p className="admin-feedback admin-feedback--info" role="status">{notice}</p>}
        {serviceDown && !notice && (
          <p className="admin-feedback admin-feedback--error" role="alert">
            La API de administración no responde. Enciéndela con iniciar-rupi y vuelve a cargar.
          </p>
        )}
        <label className="admin-field">
          <span>Usuario</span>
          <input
            autoComplete="username"
            autoCapitalize="none"
            spellCheck={false}
            value={username}
            onChange={event => setUsername(event.target.value)}
            disabled={busy}
          />
        </label>
        <label className="admin-field">
          <span>Contraseña</span>
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={event => setPassword(event.target.value)}
            disabled={busy}
          />
        </label>
        {error && <p className="admin-feedback admin-feedback--error" role="alert">{error}</p>}
        <button type="submit" className="admin-btn admin-btn--primary admin-btn--wide" disabled={busy}>
          <KeyRound size={18} aria-hidden="true" />
          {busy ? 'Ingresando…' : 'Ingresar'}
        </button>
      </form>
    </main>
  )
}
