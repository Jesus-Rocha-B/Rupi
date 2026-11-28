import { useEffect, useState } from 'react'
import { Check, ChevronDown, GraduationCap, Headphones, History, Landmark, LockKeyhole, Presentation, RefreshCw, Search, ShieldCheck, Users, UserCircle } from 'lucide-react'
import { AdminApiError, PERMISSION_LABELS, fetchRoleCatalog, type RoleCatalog } from '../../services/adminApi'
import { SESSION_LOST } from './accountStatus'

import './RolesView.css'

const roleIcons = { ESTUDIANTE: GraduationCap, DOCENTE: Presentation, SOPORTE: Headphones, ADMINISTRADOR: Landmark }
const roleNames: Record<string, string> = { ESTUDIANTE: 'Estudiante de primaria', DOCENTE: 'Profesor de primaria', SOPORTE: 'Mesa de ayuda', ADMINISTRADOR: 'Administración' }
const examples: Record<string, string> = {
  ESTUDIANTE: 'Un estudiante ingresa a su ruta escolar para continuar una lección.',
  DOCENTE: 'Un docente prepara una ruta de aprendizaje para su aula.',
  ADMINISTRADOR: 'Consulta los permisos activos para saber qué operaciones puede realizar esta cuenta.',
}
const permissionDetails: Record<string, [string, string, typeof Users]> = {
  'consola.ingresar': ['Entrar a la consola', 'Acceso al panel de administración.', GraduationCap],
  'cuentas.ver': ['Consultar cuentas', 'Visualización de perfiles de docentes y alumnos.', Users],
  'respaldos.ver': ['Consultar el historial de respaldos', 'Visualización del historial y programación de copias.', History],
  'sincronizacion.auditar': ['Auditar la sincronización web y móvil', 'Consulta del estado y registros de sincronización entre plataformas.', RefreshCw],
}
function normalize(value: string) {
  return value.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')
}
interface Props {
  onSessionLost: (message: string) => void
}

/** Matriz de solo lectura: los permisos de cada rol se versionan en las migraciones, no se editan aquí. */
export function RolesView({ onSessionLost }: Props) {
  const [selected, setSelected] = useState('SOPORTE')
  const [query, setQuery] = useState('')
  const [catalog, setCatalog] = useState<RoleCatalog | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    fetchRoleCatalog(controller.signal)
      .then(setCatalog)
      .catch(failure => {
        if (controller.signal.aborted) return
        if (failure instanceof AdminApiError && failure.status === 401) onSessionLost(failure.message || SESSION_LOST)
        else setError(failure instanceof AdminApiError ? failure.message : 'No pudimos cargar los roles.')
      })
    return () => controller.abort()
  }, [onSessionLost])

  const needle = normalize(query.trim())
  const permissions = catalog?.permisos ?? []
  function title(code: string) {
    return permissionDetails[code]?.[0] ?? PERMISSION_LABELS[code] ?? permissions.find(p => p.codigo === code)?.descripcion ?? code
  }
  const roles = (catalog?.roles ?? []).filter(role =>
    normalize(roleNames[role.codigo] ?? role.nombre).includes(needle)
    || role.permisos.some(code => normalize(code + ' ' + title(code)).includes(needle)))
  const role = roles.find(item => item.codigo === selected) ?? roles[0]
  const Icon = roleIcons[role?.codigo as keyof typeof roleIcons] ?? ShieldCheck
  const allowed = permissions.filter(p => role?.permisos.includes(p.codigo))
  const restricted = permissions.filter(p => !role?.permisos.includes(p.codigo))
  const example = role?.codigo === 'SOPORTE' && role.permisos.includes('cuentas.ver') && !role.permisos.includes('cuentas.gestionar')
    ? 'Puede localizar la cuenta de un docente. Modificar sus datos requiere otro permiso.'
    : examples[role?.codigo] ?? role?.descripcion

  return <section className="role-browser" aria-labelledby="admin-roles-title">
    <header><div><h1 id="admin-roles-title">Roles y permisos</h1><p>Selecciona un rol para consultar sus accesos.</p></div>
      <label className="role-search"><Search size={19}/><input type="search" aria-label="Buscar rol o permiso" placeholder="Buscar rol o permiso..." value={query} onChange={e => setQuery(e.target.value)}/></label>
    </header>
    {error && <p role="alert">{error}</p>}
    {!catalog && !error && <p role="status">Cargando roles…</p>}
    {catalog && !role && <p role="status">No se encontraron roles para esta búsqueda.</p>}
    <div className="role-choices">{roles.map(item => {
      const TileIcon = roleIcons[item.codigo as keyof typeof roleIcons] ?? ShieldCheck
      const active = item.codigo === role.codigo
      return <button key={item.codigo} type="button" aria-pressed={active} onClick={() => setSelected(item.codigo)}>
        <TileIcon size={29}/>{active && <Check className="role-selected" size={23}/>}
        <strong>{roleNames[item.codigo] ?? item.nombre}</strong><span>{item.cuentas} {item.cuentas === 1 ? 'cuenta' : 'cuentas'}</span>
      </button>
    })}</div>
    {role && <div className="role-columns">
      <section className="role-panel"><div className="role-heading"><h2>Permisos activos</h2><p>Accesos de este rol para consultar.</p></div>
        {allowed.map(p => {
          const PermissionIcon = permissionDetails[p.codigo]?.[2] ?? ShieldCheck
          return <div className="role-permission" key={p.codigo}><PermissionIcon/><div><strong>{title(p.codigo)}</strong><small>{permissionDetails[p.codigo]?.[1] ?? p.descripcion}</small></div><Check className="role-granted" aria-label="Permitido"/></div>
        })}
        {!allowed.length && <p className="role-heading">Este rol no tiene permisos de administración activos.</p>}
      </section>
      <section className="role-panel role-summary"><h2>Resumen del rol</h2>
        <div className="role-identity"><Icon/><div><h3>{roleNames[role.codigo] ?? role.nombre}</h3><p>{role.codigo === 'SOPORTE' ? 'Atiende consultas de docentes y familias.' : role.descripcion}</p></div></div>
        {example && <div className="role-example"><UserCircle/><div><strong>Ejemplo</strong><p>{example}</p></div></div>}
        <details key={role.codigo}><summary><LockKeyhole/>Ver los {restricted.length} permisos restringidos<ChevronDown/></summary>
          <ul>{restricted.map(p => <li key={p.codigo}>{title(p.codigo)}</li>)}</ul>
        </details>
      </section>
    </div>}
  </section>
}
