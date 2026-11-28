import { useEffect, useState } from 'react'
import { AdminApiError, fetchAccounts, fetchRoleCatalog, fetchTwoFactorOverview, ROLE_LABELS, type Administrator, type RoleInfo } from '../../services/adminApi'
import { SESSION_LOST } from './accountStatus'

interface Props {
  revision: number
  admin: Administrator
  onNavigate: (section: 'roles' | '2fa') => void
  onSessionLost: (message: string) => void
}

export function AccountOverview({ revision, admin, onNavigate, onSessionLost }: Props) {
  const [counts, setCounts] = useState<number[] | null>(null)
  const [roles, setRoles] = useState<RoleInfo[]>([])
  const [factor, setFactor] = useState<{ active: number; total: number } | null>(null)
  const [failed, setFailed] = useState(false)
  const canViewFactor = admin.permisos.includes('consola.ingresar')
  useEffect(() => {
    const controller = new AbortController()
    const { signal } = controller
    function report(error: unknown) {
      if (signal.aborted) return
      if (error instanceof AdminApiError && error.status === 401) onSessionLost(SESSION_LOST)
      else setFailed(true)
    }
    setFailed(false)
    Promise.all([fetchAccounts({}, signal), fetchAccounts({ estado: 'ACTIVO' }, signal)])
      .then(pages => setCounts(pages.map(page => page.total))).catch(report)
    fetchRoleCatalog(signal).then(data => setRoles(data.roles)).catch(report)
    if (canViewFactor) fetchTwoFactorOverview(signal).then(data => {
      setFactor({ active: data.cuentas.filter(account => account.verificado).length, total: data.cuentas.length })
    }).catch(report)
    return () => controller.abort()
  }, [revision, canViewFactor, onSessionLost])
  return <>
    <div className="console-summary">
      <section><h2>Resumen de cuentas</h2>
        <p>Cuentas registradas <b>{counts?.[0] ?? '—'}</b></p>
        <p>Cuentas activas <b>{counts?.[1] ?? '—'}</b></p>
      </section>
      <section><h2>Distribución de cuentas por rol</h2>
        {roles.map(role => <div className="console-role" key={role.codigo}>
          <span>{ROLE_LABELS[role.codigo] ?? role.nombre}</span>
          <meter min={0} max={Math.max(1, ...roles.map(item => item.cuentas))} value={role.cuentas} aria-label={role.nombre} />
          <strong>{role.cuentas}</strong>
        </div>)}
        <button className="admin-btn admin-btn--quiet" onClick={() => onNavigate('roles')}>Roles y permisos →</button>
      </section>
      {canViewFactor && <section><h2>Doble factor (2FA)</h2>
        <div className="console-factor"><strong style={{backgroundImage: `conic-gradient(#ff923c ${factor?.total ? 100 * factor.active / factor.total : 0}%, var(--adm-line) 0)`}}><span>{factor ? `${factor.active}/${factor.total}` : '—'}</span></strong>
          <div>Cuentas protegidas<br />{factor && <small>{factor.total - factor.active} pendientes de QR</small>}</div>
        </div>
        <button className="admin-btn admin-btn--quiet" onClick={() => onNavigate('2fa')}>Gestionar doble factor →</button>
      </section>}
    </div>
    {failed && <p role="status">Algunos indicadores no están disponibles.</p>}
  </>
}
