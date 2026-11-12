import { useState, type FormEvent } from 'react'
import { Check, Copy, KeyRound, ShieldCheck, Smartphone } from 'lucide-react'
import { QrCodeSvg } from './QrCodeSvg'
import './TwoFactorSetupCard.css'

interface TwoFactorSetupCardProps {
  username: string
  otpauthUri: string
  secretBase32: string
  busy: boolean
  error: string | null
  submitLabel?: string
  onVerify: (code: string) => Promise<void>
  onCancel?: () => void
}

export function TwoFactorSetupCard({
  username,
  otpauthUri,
  secretBase32,
  busy,
  error,
  submitLabel = 'Activar y entrar',
  onVerify,
  onCancel,
}: TwoFactorSetupCardProps) {
  const [code, setCode] = useState('')
  const [copied, setCopied] = useState(false)
  const [localError, setLocalError] = useState<string | null>(null)

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(secretBase32)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      setLocalError('No pudimos copiar automáticamente; selecciona la clave manualmente.')
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const clean = code.replace(/\D/g, '')
    if (clean.length !== 6) {
      setLocalError('Ingresa los 6 dígitos temporales de tu aplicación autenticadora.')
      return
    }
    setLocalError(null)
    await onVerify(clean)
  }

  const shownError = localError || error

  return (
    <form className="rupi-2fa-card" onSubmit={handleSubmit} noValidate>
      <div className="rupi-2fa-head">
        <span className="rupi-2fa-icon" aria-hidden="true">
          <ShieldCheck size={22} />
        </span>
        <div>
          <h2>Configurar doble factor (2FA)</h2>
          <p>
            Cuenta <strong>@{username}</strong> · Obligatorio para administración y docentes
          </p>
        </div>
      </div>

      <div className="rupi-2fa-steps">
        <div className="rupi-2fa-qr-box">
          <QrCodeSvg value={otpauthUri} label={`Código QR 2FA para ${username}`} />
        </div>
        <div className="rupi-2fa-guide">
          <p className="rupi-2fa-step-title">
            <Smartphone size={16} aria-hidden="true" />
            <span>1. Escanea este QR en tu celular</span>
          </p>
          <p className="rupi-2fa-muted">
            Usa Google Authenticator, Microsoft Authenticator o Authy. Si prefieres, copia la clave manual:
          </p>
          <div className="rupi-2fa-secret-row">
            <code>{secretBase32}</code>
            <button
              type="button"
              className="rupi-2fa-copy-btn"
              onClick={handleCopy}
              aria-label="Copiar clave secreta 2FA"
            >
              {copied ? <Check size={15} aria-hidden="true" /> : <Copy size={15} aria-hidden="true" />}
              <span>{copied ? 'Copiado' : 'Copiar'}</span>
            </button>
          </div>

          <label className="rupi-2fa-code-field">
            <span>2. Escribe el código temporal de 6 dígitos</span>
            <input
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              placeholder="000000"
              value={code}
              onChange={e => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              disabled={busy}
            />
          </label>
        </div>
      </div>

      {shownError && (
        <p className="rupi-2fa-error" role="alert">
          {shownError}
        </p>
      )}

      <div className="rupi-2fa-actions">
        {onCancel && (
          <button type="button" className="rupi-2fa-btn rupi-2fa-btn--ghost" onClick={onCancel} disabled={busy}>
            Volver
          </button>
        )}
        <button type="submit" className="rupi-2fa-btn rupi-2fa-btn--primary" disabled={busy}>
          <KeyRound size={17} aria-hidden="true" />
          <span>{busy ? 'Verificando…' : submitLabel}</span>
        </button>
      </div>
    </form>
  )
}
