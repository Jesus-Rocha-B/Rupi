import { useEffect, useState } from 'react'
import { Check, Minus } from 'lucide-react'
import { AdminApiError, PERMISSION_LABELS, fetchRoleCatalog, type RoleCatalog } from '../../services/adminApi'
import { SESSION_LOST } from './accountStatus'

interface Props {
  onSessionLost: (message: string) => void
}

/** Matriz de solo lectura: los permisos de cada rol se versionan en las migraciones, no se editan aquí. */
export function RolesView({ onSessionLost }: Props) {
  const [catalog, setCatalog] = useState<RoleCatalog | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    fetchRoleCatalog(controller.signal)
      .then(setCatalog)
      .catch(failure => {
        if (controller.signal.aborted) return
        if (failure instanceof AdminApiError && failure.status === 401) onSessionLost(SESSION_LOST)
        else setError(failure instanceof AdminApiError ? failure.message : 'No pudimos cargar los roles.')
      })
    return () => controller.abort()
  }, [onSessionLost])

  return (
    <section className="admin-card" aria-labelledby="admin-roles-title">
      <h1 id="admin-roles-title">Roles y permisos</h1>
      <p className="admin-muted">
        Cada cuenta recibe roles; cada rol trae permisos fijos. Para cambiar lo que puede un rol se prepara una
        migración revisada por el equipo, así nadie amplía permisos sin dejar rastro.
      </p>
      {error && <p className="admin-feedback admin-feedback--error" role="alert">{error}</p>}
      {!catalog && !error && <p className="admin-loading">Cargando roles…</p>}
      {catalog && (
        <>
          <ul className="admin-role-cards">
            {catalog.roles.map(role => (
              <li key={role.codigo} className={`admin-role-card admin-role-card--${role.codigo.toLowerCase()}`}>
                <h2>{role.nombre}</h2>
                <p className="admin-muted">{role.descripcion}</p>
                <p className="admin-role-meta">
                  <span>{role.cuentas} {role.cuentas === 1 ? 'cuenta' : 'cuentas'}</span>
                  <span>{role.consola ? 'Usa la consola' : 'Usa la web y el celular de Rupi'}</span>
                </p>
              </li>
            ))}
          </ul>
          <div className="admin-table-wrap" role="region" aria-label="Permisos por rol" tabIndex={0}>
            <table className="admin-matrix">
              <caption className="admin-sr-only">Qué permisos tiene cada rol</caption>
              <thead>
                <tr>
                  <th scope="col">Permiso</th>
                  {catalog.roles.map(role => <th key={role.codigo} scope="col">{role.nombre}</th>)}
                </tr>
              </thead>
              <tbody>
                {catalog.permisos.map(permission => (
                  <tr key={permission.codigo}>
                    <th scope="row">
                      {PERMISSION_LABELS[permission.codigo] ?? permission.descripcion ?? permission.codigo}
                      <code>{permission.codigo}</code>
                    </th>
                    {catalog.roles.map(role => {
                      const granted = role.permisos.includes(permission.codigo)
                      return (
                        <td key={role.codigo} className={granted ? 'is-granted' : undefined}>
                          {granted
                            ? <><Check size={20} aria-hidden="true" /><span className="admin-sr-only">Sí</span></>
                            : <><Minus size={20} aria-hidden="true" /><span className="admin-sr-only">No</span></>}
                        </td>
                      )
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </section>
  )
}
