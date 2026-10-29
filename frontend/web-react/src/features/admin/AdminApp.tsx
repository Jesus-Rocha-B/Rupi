import { useCallback, useEffect, useState } from 'react'
import { Activity, ArrowLeftRight, LogOut, MonitorSmartphone, ShieldCheck, Users } from 'lucide-react'
import { AdminApiError, adminLogout, fetchAdminSession, type Administrator } from '../../services/adminApi'
import { AdminLogin } from './AdminLogin'
import { AccountsView } from './AccountsView'
import { HealthView } from './HealthView'
import { RolesView } from './RolesView'
import { SessionsView } from './SessionsView'
import { SyncAuditView } from './SyncAuditView'
import './AdminConsole.css'

type SectionId = 'cuentas' | 'roles' | 'sesiones' | 'salud' | 'sincronizacion'

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
  { id: 'salud', label: 'Salud', permission: 'plataforma.monitorear', icon: Activity },
  { id: 'sincronizacion', label: 'Sincronización Web/Móvil', permission: 'sincronizacion.auditar', icon: ArrowLeftRight },
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
  }

  if (checking) {
    return (
      <main className="admin-loading-screen" aria-busy="true">
        <p>Entrando a la administración de Rupi…</p>
      </main>
    )
  }

  if (serviceDown) {
    return (
      <main className="admin-login-wrap">
        <section className="admin-card" role="alert">
          <h1>No pudimos conectar con la administración</h1>
          <p className="admin-muted">
            Revisa que la consola de Django esté encendida (puerto 8082) y que MySQL responda.
          </p>
          <button type="button" className="admin-btn admin-btn--sky" onClick={() => window.location.reload()}>
            Reintentar
          </button>
        </section>
      </main>
    )
  }

  if (!admin) {
    return <AdminLogin onLogin={setAdmin} notice={notice} serviceDown={serviceDown} />
  }

  const availableSections = SECTIONS.filter(s => admin.permisos.includes(s.permission))
  const current = availableSections.find(s => s.id === section) || availableSections[0]

  return (
    <div className="admin-layout">
      <header className="admin-topbar">
        <div className="admin-brand">
          <span className="admin-brand-dot" aria-hidden="true" />
          <span>RUPI · Consola</span>
        </div>
        <nav className="admin-tabs" aria-label="Secciones de la consola">
          {availableSections.map(item => {
            const Icon = item.icon
            const active = current?.id === item.id
            return (
              <button
                key={item.id}
                type="button"
                className={`admin-tab ${active ? 'is-active' : ''}`}
                aria-current={active ? 'page' : undefined}
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
            Cerrar sesión
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
        {current?.id === 'salud' && <HealthView onSessionLost={handleSessionLost} />}
        {current?.id === 'sincronizacion' && <SyncAuditView onSessionLost={handleSessionLost} />}
      </main>
    </div>
  )
}
