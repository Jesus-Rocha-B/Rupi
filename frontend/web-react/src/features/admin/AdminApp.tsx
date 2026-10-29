import { useCallback, useEffect, useState } from 'react'
import { LogOut, MonitorSmartphone, ShieldCheck, Users } from 'lucide-react'
import { AdminApiError, adminLogout, fetchAdminSession, type Administrator } from '../../services/adminApi'
import { AdminLogin } from './AdminLogin'
import { AccountsView } from './AccountsView'
import { RolesView } from './RolesView'
import { SessionsView } from './SessionsView'
import './AdminConsole.css'

type SectionId = 'cuentas' | 'roles' | 'sesiones'

interface Section {
  id: SectionId
  label: string
  permission: string
  icon: typeof Users
}

const SECTIONS: Section[] = [
  { id: 'cuentas', label: 'Cuentas', permission: 'cuentas.ver', icon: Users },
  { id: 'roles', label: 'Roles y permisos', permission: 'cuentas.ver', icon: ShieldCheck },
  { id: 'sesiones', label: 'Sesiones', permission: 'sesiones.gestionar', icon: MonitorSmartphone },
]

export function AdminApp() {
  const [admin, setAdmin] = useState<Administrator | null>(null)
  const [checking, setChecking] = useState(true)
  const [serviceDown, setServiceDown] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [section, setSection] = useState<SectionId>('cuentas')

  useEffect(() => {
    document.title = 'Rupi · Administración'
    const controller = new AbortController()
    fetchAdminSession(controller.signal)
      .then(setAdmin)
      .catch(error => {
        if (controller.signal.aborted) return
        setServiceDown(!(error instanceof AdminApiError && error.status === 401))
      })
      .finally(() => {
        if (!controller.signal.aborted) setChecking(false)
      })
    return () => controller.abort()
  }, [])

  // Cualquier 401 de una sección significa que la sesión terminó (expiró, se cerró o se abrió en otro equipo).
  const handleSessionLost = useCallback((message: string) => {
    setAdmin(null)
    setNotice(message)
  }, [])

  async function handleLogout() {
    try {
      await adminLogout()
    } catch {
      // Aunque falle la red, en este equipo la consola se cierra igual.
    }
    setAdmin(null)
    setNotice('Cerraste la sesión de administración.')
  }

  if (checking) {
    return <main className="admin-shell admin-shell--center"><p className="admin-loading">Abriendo la consola…</p></main>
  }

  if (!admin) {
    return (
      <AdminLogin
        notice={notice}
        serviceDown={serviceDown}
        onLogin={value => {
          setAdmin(value)
          setNotice(null)
          setServiceDown(false)
        }}
      />
    )
  }

  const visibleSections = SECTIONS.filter(item => admin.permisos.includes(item.permission))
  const current = visibleSections.find(item => item.id === section) ?? visibleSections[0]

  return (
    <div className="admin-shell">
      <a className="admin-skip" href="#admin-main">Saltar al contenido</a>
      <header className="admin-topbar">
        <div className="admin-brand">
          <span className="admin-brand-mark" aria-hidden="true">R</span>
          <div>
            <strong>Rupi</strong>
            <span>Administración de la plataforma</span>
          </div>
        </div>
        <nav className="admin-nav" aria-label="Secciones de administración">
          {visibleSections.map(item => {
            const Icon = item.icon
            return (
              <button
                key={item.id}
                type="button"
                className={`admin-nav-btn${current?.id === item.id ? ' is-current' : ''}`}
                aria-current={current?.id === item.id ? 'page' : undefined}
                onClick={() => setSection(item.id)}
              >
                <Icon size={18} aria-hidden="true" />
                {item.label}
              </button>
            )
          })}
        </nav>
        <div className="admin-user">
          <span>{admin.nombre}</span>
          <button type="button" className="admin-btn admin-btn--quiet" onClick={handleLogout}>
            <LogOut size={18} aria-hidden="true" />
            Salir
          </button>
        </div>
      </header>
      <main id="admin-main" className="admin-main">
        {!current && (
          <section className="admin-card admin-empty">
            <h1>Tu rol todavía no tiene secciones asignadas</h1>
            <p>Pide a otra persona de administración que revise los permisos de tu rol.</p>
          </section>
        )}
        {current?.id === 'cuentas' && <AccountsView admin={admin} onSessionLost={handleSessionLost} />}
        {current?.id === 'roles' && <RolesView onSessionLost={handleSessionLost} />}
        {current?.id === 'sesiones' && <SessionsView admin={admin} onSessionLost={handleSessionLost} />}
      </main>
    </div>
  )
}
