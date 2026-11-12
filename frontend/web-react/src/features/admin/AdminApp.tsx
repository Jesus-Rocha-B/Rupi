import { useCallback, useEffect, useState } from 'react'
import { Activity, ArrowLeftRight, BarChart3, BookOpen, ChevronDown, DatabaseBackup, LockKeyhole, LogOut, MonitorSmartphone, ShieldCheck, UserRound, Users } from 'lucide-react'
import { AdminApiError, adminLogout, fetchAdminSession, type Administrator } from '../../services/adminApi'
import { AccessPolicyView } from './AccessPolicyView'
import { AccountsView } from './AccountsView'
import { AdminLogin } from './AdminLogin'
import { ArchitectureDashboardView } from './ArchitectureDashboardView'
import { BackupsView } from './BackupsView'
import { CurriculumView } from './CurriculumView'
import { HealthView } from './HealthView'
import { RolesView } from './RolesView'
import { SessionsView } from './SessionsView'
import { SyncAuditView } from './SyncAuditView'
import { LanguageModerationView } from './LanguageModerationView'
import './AdminConsole.css'
import './AdminNavigation.css'

type SectionId = 'cuentas' | 'roles' | 'sesiones' | 'acceso' | 'catalogo' | 'moderacion' | 'salud' | 'tablero' | 'sincronizacion' | 'respaldos'

interface Section {
  id: SectionId
  label: string
  permission: string
  icon: typeof Users
}

const SECTIONS: Section[] = [
  { id: 'moderacion', label: 'Moderación IA', permission: 'moderacion.ver', icon: ShieldCheck },
  { id: 'cuentas', label: 'Cuentas', permission: 'cuentas.ver', icon: Users },
  { id: 'roles', label: 'Roles y permisos', permission: 'cuentas.ver', icon: ShieldCheck },
  { id: 'sesiones', label: 'Sesiones', permission: 'sesiones.gestionar', icon: MonitorSmartphone },
  { id: 'acceso', label: 'Bloqueo por intentos', permission: 'acceso.ver', icon: LockKeyhole },
  { id: 'catalogo', label: 'Catálogo curricular', permission: 'curriculo.ver', icon: BookOpen },
  { id: 'salud', label: 'Salud', permission: 'plataforma.monitorear', icon: Activity },
  { id: 'tablero', label: 'Tablero de Uptime', permission: 'plataforma.monitorear', icon: BarChart3 },
  { id: 'sincronizacion', label: 'Sincronización Web/Móvil', permission: 'sincronizacion.auditar', icon: ArrowLeftRight },
  { id: 'respaldos', label: 'Respaldos automáticos', permission: 'respaldos.ver', icon: DatabaseBackup },
]

const GROUPS: { label: string; icon: typeof Users; sections: SectionId[] }[] = [
  { label: 'Usuarios', icon: Users, sections: ['cuentas', 'roles', 'sesiones', 'acceso'] },
  { label: 'Catálogo MINEDU', icon: BookOpen, sections: ['catalogo'] },
  { label: 'Moderación IA', icon: ShieldCheck, sections: ['moderacion'] },
  { label: 'Monitoreo', icon: Activity, sections: ['salud', 'tablero', 'sincronizacion', 'respaldos'] },
]

export function AdminApp() {
  const [admin, setAdmin] = useState<Administrator | null>(null)
  const [checking, setChecking] = useState(true)
  const [serviceDown, setServiceDown] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [section, setSection] = useState<SectionId>('cuentas')

  useEffect(() => {
    document.title = 'Rupi · Consola de Administración'
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
      <main className="admin-shell admin-shell--center" aria-busy="true">
        <div className="admin-card admin-empty">
          <span className="admin-brand-mark admin-brand-mark--large" aria-hidden="true">R</span>
          <h1>Consola de Administración</h1>
          <p className="admin-muted">Entrando a la administración de Rupi…</p>
        </div>
      </main>
    )
  }

  if (serviceDown) {
    return (
      <main className="admin-shell admin-shell--center">
        <section className="admin-card" role="alert">
          <span className="admin-brand-mark admin-brand-mark--large" aria-hidden="true">!</span>
          <h1>No pudimos conectar con la administración</h1>
          <p className="admin-muted">
            Revisa que la consola de Django esté encendida (puerto 8082) y que MySQL responda.
          </p>
          <button type="button" className="admin-btn admin-btn--sky" onClick={() => window.location.reload()}>
            Reintentar conexión
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
  const groups = GROUPS.filter(group => availableSections.some(s => group.sections.includes(s.id)))
  const currentGroup = groups.find(group => current && group.sections.includes(current.id))

  return (
    <div className="admin-shell">
      <a href="#admin-main" className="admin-skip">Saltar al contenido principal</a>
      <header className="admin-topbar admin-topbar--grouped">
        <div className="admin-brand">
          <img className="admin-original-logo" src="/rupi-logo.jpg" alt="Rupi" />
          <div>
            <strong>Rupi</strong>
            <span>Administración</span>
          </div>
        </div>
        <nav className="admin-nav" aria-label="Secciones de la consola">
          {groups.map(item => {
            const Icon = item.icon
            const active = currentGroup === item
            return (
              <button
                key={item.label}
                type="button"
                className={`admin-nav-btn ${active ? 'is-current' : ''}`}
                aria-current={active ? 'location' : undefined}
                onClick={() => { if (!active) setSection(availableSections.find(s => item.sections.includes(s.id))!.id) }}
              >
                <Icon size={18} aria-hidden="true" />
                <span>{item.label}</span>
              </button>
            )
          })}
        </nav>
        <details className="admin-account-menu" onKeyDown={event => { if (event.key === 'Escape') { event.currentTarget.open = false; event.currentTarget.querySelector('summary')?.focus() } }}>
          <summary><UserRound size={19} aria-hidden="true" /><span>{admin.nombre}</span><ChevronDown size={15} aria-hidden="true" /></summary>
          <div className="admin-account-popover"><span>Sesión de administración</span><button type="button" className="admin-btn" onClick={handleLogout}>
            <LogOut size={16} aria-hidden="true" />
            Cerrar sesión
          </button></div>
        </details>
      </header>
      <main id="admin-main" className="admin-main">
        {currentGroup && current?.id !== 'catalogo' && current?.id !== 'moderacion' && <nav className="admin-section-nav" aria-label={`Opciones de ${currentGroup.label}`}><span className="admin-section-context">{currentGroup.label}</span>{availableSections.filter(s => currentGroup.sections.includes(s.id)).map(item => { const Icon = item.icon; return <button key={item.id} type="button" className={`admin-section-link ${current.id === item.id ? 'is-active' : ''}`} aria-current={current.id === item.id ? 'page' : undefined} onClick={() => setSection(item.id)}><Icon size={18} aria-hidden="true" />{item.label}</button> })}</nav>}
        {!current && (
          <section className="admin-card admin-empty">
            <h1>Tu rol todavía no tiene secciones asignadas</h1>
            <p className="admin-muted">Pide a otra persona de administración que revise los permisos de tu rol.</p>
          </section>
        )}
        {current?.id === 'cuentas' && <AccountsView admin={admin} onSessionLost={handleSessionLost} />}
        {current?.id === 'roles' && <RolesView onSessionLost={handleSessionLost} />}
        {current?.id === 'sesiones' && <SessionsView admin={admin} onSessionLost={handleSessionLost} />}
        {current?.id === 'acceso' && <AccessPolicyView admin={admin} onSessionLost={handleSessionLost} />}
        {current?.id === 'moderacion' && <LanguageModerationView admin={admin} onSessionLost={handleSessionLost} />}
        {current?.id === 'catalogo' && <CurriculumView admin={admin} onSessionLost={handleSessionLost} />}
        {current?.id === 'salud' && <HealthView onSessionLost={handleSessionLost} />}
        {current?.id === 'tablero' && <ArchitectureDashboardView onSessionLost={handleSessionLost} />}
        {current?.id === 'sincronizacion' && <SyncAuditView onSessionLost={handleSessionLost} />}
        {current?.id === 'respaldos' && <BackupsView admin={admin} onSessionLost={handleSessionLost} />}
      </main>
    </div>
  )
}
