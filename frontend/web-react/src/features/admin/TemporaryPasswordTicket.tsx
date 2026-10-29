import { useState } from 'react'
import { Copy, Check } from 'lucide-react'

interface Props {
  username: string
  password: string
  title?: string
  emailNotified?: string | null
}

/** La clave temporal se muestra una sola vez: la API solo guarda su hash. */
export function TemporaryPasswordTicket({ username, password, title = 'Clave temporal', emailNotified }: Props) {
  const [copied, setCopied] = useState(false)

  async function copy() {
    try {
      await navigator.clipboard.writeText(password)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      setCopied(false)
    }
  }

  return (
    <div className="admin-ticket" role="status">
      <p className="admin-ticket-title">{title}</p>
      <dl>
        <div>
          <dt>Usuario</dt>
          <dd>{username}</dd>
        </div>
        <div>
          <dt>Contraseña</dt>
          <dd className="admin-ticket-password">{password}</dd>
        </div>
      </dl>
      <button type="button" className="admin-btn admin-btn--quiet" onClick={copy}>
        {copied ? <Check size={18} aria-hidden="true" /> : <Copy size={18} aria-hidden="true" />}
        {copied ? 'Copiada' : 'Copiar contraseña'}
      </button>
      {emailNotified ? (
        <p className="admin-ticket-note">
          ✉️ Se envió un correo automático de recuperación con las credenciales a <strong>{emailNotified}</strong>.
        </p>
      ) : (
        <p className="admin-ticket-note">
          Entrégala en persona o a la docente del aula. Cuando cierres este panel no se podrá volver a ver.
        </p>
      )}
    </div>
  )
}
