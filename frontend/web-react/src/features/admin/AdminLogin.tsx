import { useState, type FormEvent } from 'react'
import { ArrowLeft, KeyRound, ShieldCheck } from 'lucide-react'
import {
  AdminApiError,
  adminLogin,
  verifyAdminTwoFactor,
  type Administrator,
} from '../../services/adminApi'
import { TwoFactorSetupCard } from '../../components/TwoFactorSetupCard'

interface Props {
  notice: string | null
  serviceDown: boolean
  onLogin: (admin: Administrator) => void
}

type ChallengeState = {
  configured: boolean
  challenge: string
  username: string
  otpauthUri?: string
  secretBase32?: string
}

export function AdminLogin({ notice, serviceDown, onLogin }: Props) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [totpCode, setTotpCode] = useState('')
  const [challenge, setChallenge] = useState<ChallengeState | null>(null)
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
      const outcome = await adminLogin(username.trim(), password)
      if (outcome.kind === 'authenticated') {
        onLogin(outcome.admin)
      } else {
        setTotpCode('')
        setChallenge({
          configured: outcome.configured,
          challenge: outcome.challenge,
          username: outcome.username,
          otpauthUri: outcome.otpauthUri,
          secretBase32: outcome.secretBase32,
        })
      }
    } catch (failure) {
      setPassword('')
      setError(failure instanceof AdminApiError && failure.status !== 500
        ? failure.message
        : 'No pudimos conectar con la API de administración. Revisa que esté encendida.')
    } finally {
      setBusy(false)
    }
  }

  async function handleVerifyChallenge(codeToVerify: string) {
    if (!challenge) return
    setBusy(true)
    setError(null)
    try {
      const admin = await verifyAdminTwoFactor(challenge.challenge, codeToVerify)
      onLogin(admin)
    } catch (failure) {
      setError(failure instanceof AdminApiError && failure.status !== 500
        ? failure.message
        : 'No pudimos validar el código de doble factor.')
    } finally {
      setBusy(false)
    }
  }

  async function handleVerifyCodeSubmit(event: FormEvent) {
    event.preventDefault()
    const clean = totpCode.replace(/\D/g, '')
    if (clean.length !== 6) {
      setError('Ingresa el código temporal de 6 dígitos de tu aplicación autenticadora.')
      return
    }
    await handleVerifyChallenge(clean)
  }

  if (challenge && !challenge.configured && challenge.otpauthUri && challenge.secretBase32) {
    return (
      <main className="admin-shell admin-shell--center">
        <div className="admin-card" style={{ maxWidth: 640, width: '100%' }}>
          <TwoFactorSetupCard
            username={challenge.username}
            otpauthUri={challenge.otpauthUri}
            secretBase32={challenge.secretBase32}
            busy={busy}
            error={error}
            submitLabel="Activar 2FA y entrar"
            onVerify={handleVerifyChallenge}
            onCancel={() => {
              setChallenge(null)
              setError(null)
            }}
          />
        </div>
      </main>
    )
  }

  if (challenge && challenge.configured) {
    return (
      <main className="admin-shell admin-shell--center">
        <form className="admin-card admin-login" onSubmit={handleVerifyCodeSubmit} noValidate>
          <span className="admin-brand-mark admin-brand-mark--large" aria-hidden="true">
            <ShieldCheck size={28} />
          </span>
          <h1>Verificación en dos pasos</h1>
          <p className="admin-muted">
            Cuenta <strong>@{challenge.username}</strong>. Abre tu aplicación autenticadora e ingresa el código de 6 dígitos.
          </p>
          <label className="admin-field">
            <span>Código temporal (6 dígitos)</span>
            <input
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              placeholder="000000"
              value={totpCode}
              onChange={event => setTotpCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
              disabled={busy}
              style={{ textAlign: 'center', fontSize: '1.35rem', letterSpacing: '0.32em', fontFamily: 'monospace' }}
            />
          </label>
          {error && <p className="admin-feedback admin-feedback--error" role="alert">{error}</p>}
          <button type="submit" className="admin-btn admin-btn--primary admin-btn--wide" disabled={busy}>
            <KeyRound size={18} aria-hidden="true" />
            {busy ? 'Validando código…' : 'Confirmar y entrar'}
          </button>
          <button
            type="button"
            className="admin-btn admin-btn--quiet admin-btn--wide"
            disabled={busy}
            onClick={() => {
              setChallenge(null)
              setTotpCode('')
              setError(null)
            }}
          >
            <ArrowLeft size={16} aria-hidden="true" />
            Cambiar usuario
          </button>
        </form>
      </main>
    )
  }

  return (
    <main className="admin-shell admin-shell--center">
      <form className="admin-card admin-login" onSubmit={handleSubmit} noValidate>
        <span className="admin-brand-mark admin-brand-mark--large" aria-hidden="true">R</span>
        <h1>Administración de Rupi</h1>
        <p className="admin-muted">Acceso protegido con doble factor obligatorio (2FA) para el equipo de gestión.</p>
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
          {busy ? 'Verificando…' : 'Continuar'}
        </button>
      </form>
    </main>
  )
}
