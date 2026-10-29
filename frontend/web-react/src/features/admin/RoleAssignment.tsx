import { useState } from 'react'
import { ROLE_LABELS, type AccountDetail, type Administrator } from '../../services/adminApi'
import { roleProblem } from './accountStatus'

interface Props {
  account: AccountDetail
  admin: Administrator
  busy: boolean
  onSave: (roles: string[]) => void
}

const ROLE_ORDER = ['ESTUDIANTE', 'DOCENTE', 'SOPORTE', 'ADMINISTRADOR']

const ROLE_HINTS: Record<string, string> = {
  ESTUDIANTE: 'Aprende en la web y el celular. No se combina con otros roles.',
  DOCENTE: 'Arma rutas, ve reportes y su aula.',
  SOPORTE: 'Entra a la consola para atender pedidos, sin datos personales completos.',
  ADMINISTRADOR: 'Gestiona cuentas, roles y la plataforma.',
}

export function RoleAssignment({ account, admin, busy, onSave }: Props) {
  const [selected, setSelected] = useState<string[]>(account.roles)
  const problem = roleProblem(selected, account, admin)
  const changed = [...selected].sort().join() !== [...account.roles].sort().join()

  function toggle(code: string) {
    setSelected(previous => previous.includes(code) ? previous.filter(item => item !== code) : [...previous, code])
  }

  return (
    <div className="admin-form">
      <fieldset className="admin-role-checks">
        <legend className="admin-sr-only">Roles de la cuenta</legend>
        {ROLE_ORDER.map(code => (
          <label key={code} className={`admin-role-check${selected.includes(code) ? ' is-checked' : ''}`}>
            <input type="checkbox" checked={selected.includes(code)} onChange={() => toggle(code)} disabled={busy} />
            <span>
              <strong>{ROLE_LABELS[code]}</strong>
              <small>{ROLE_HINTS[code]}</small>
            </span>
          </label>
        ))}
      </fieldset>
      {problem && changed && <p className="admin-feedback admin-feedback--error" role="alert">{problem}</p>}
      {changed && !problem && (
        <p className="admin-muted">Al guardar se cierra la sesión abierta de esta cuenta para que entre con sus permisos nuevos.</p>
      )}
      <div className="admin-actions">
        <button type="button" className="admin-btn admin-btn--quiet" disabled={!changed || busy} onClick={() => setSelected(account.roles)}>
          Deshacer
        </button>
        <button type="button" className="admin-btn admin-btn--sky" disabled={!changed || Boolean(problem) || busy} onClick={() => onSave(selected)}>
          Guardar roles
        </button>
      </div>
    </div>
  )
}
