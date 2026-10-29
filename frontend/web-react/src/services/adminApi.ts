// Consola de administración: habla solo con la API Django (/api/admin/v1), nunca con Spring Boot.
const ADMIN_BASE = `${import.meta.env.VITE_ADMIN_API_BASE_URL || ''}/api/admin/v1`

export class AdminApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
    this.name = 'AdminApiError'
  }
}

export type AccountState = 'INVITADO' | 'ACTIVO' | 'BLOQUEADO' | 'DESHABILITADO'
export type SessionClient = 'WEB_ADMIN' | 'WEB_USUARIO' | 'MOVIL'

export interface Administrator {
  id: string
  usuario: string
  nombre: string
  permisos: string[]
}

export interface AccountSummary {
  id: string
  usuario: string
  nombre: string | null
  apellido: string | null
  nombrePreferido: string | null
  correo: string | null
  /** false si el rol no tiene `cuentas.datos_personales`: el correo llega enmascarado. */
  datosCompletos: boolean
  estado: AccountState
  bloqueada: boolean
  bloqueadoHasta: string | null
  roles: string[]
  creadoEn: string | null
  desactivadoEn: string | null
  sesionActiva: { cliente: SessionClient; desde: string | null; actividad: string | null } | null
}

export interface AuditEntry {
  accion: string
  resultado: string
  fecha: string | null
  actor: string | null
}

export interface AccountDetail extends AccountSummary {
  ultimoIngreso: string | null
  claveCambiadaEn: string | null
  /** Fallos de ingreso en los últimos 15 minutos (Spring frena 5 minutos tras 5 seguidos). */
  intentosFallidos: number
  historial: AuditEntry[]
  claveTemporal?: string
}

export interface AccountPage {
  items: AccountSummary[]
  total: number
  pagina: number
  porPagina: number
}

export interface AccountFilters {
  q?: string
  rol?: string
  estado?: string
  pagina?: number
}

export interface NewAccount {
  usuario: string
  nombre: string
  apellido: string
  nombrePreferido?: string
  correo?: string
  rol: 'ESTUDIANTE' | 'DOCENTE'
}

export type AccountChanges = Partial<Pick<NewAccount, 'usuario' | 'nombre' | 'apellido' | 'nombrePreferido' | 'correo'>>

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${ADMIN_BASE}/${path}`, {
    credentials: 'same-origin',
    ...options,
    headers: options.body ? { 'Content-Type': 'application/json', ...options.headers } : options.headers,
    signal: options.signal
      ? AbortSignal.any([options.signal, AbortSignal.timeout(15000)])
      : AbortSignal.timeout(15000),
  })
  if (!response.ok) {
    let message = `No se pudo completar la acción (${response.status}).`
    try {
      const data = await response.json()
      if (typeof data?.detail === 'string') message = data.detail
    } catch {
      // La respuesta no era JSON: se queda el mensaje general.
    }
    throw new AdminApiError(response.status, message)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

const post = <T>(path: string, body?: unknown) =>
  request<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) })

export const fetchAdminSession = (signal?: AbortSignal) => request<Administrator>('sesion', { signal })
export const adminLogin = (usuario: string, clave: string) => post<Administrator>('sesion', { usuario, clave })
export const adminLogout = () => request<void>('sesion', { method: 'DELETE' })

export function fetchAccounts(filters: AccountFilters, signal?: AbortSignal) {
  const params = new URLSearchParams()
  if (filters.q?.trim()) params.set('q', filters.q.trim())
  if (filters.rol) params.set('rol', filters.rol)
  if (filters.estado) params.set('estado', filters.estado)
  if (filters.pagina && filters.pagina > 1) params.set('pagina', String(filters.pagina))
  const query = params.toString()
  return request<AccountPage>(`cuentas${query ? `?${query}` : ''}`, { signal })
}

export const fetchAccount = (id: string, signal?: AbortSignal) =>
  request<AccountDetail>(`cuentas/${encodeURIComponent(id)}`, { signal })
export const createAccount = (account: NewAccount) => post<AccountDetail>('cuentas', account)
export const updateAccount = (id: string, changes: AccountChanges) =>
  request<AccountDetail>(`cuentas/${encodeURIComponent(id)}`, { method: 'PATCH', body: JSON.stringify(changes) })
export const deactivateAccount = (id: string) => post<AccountDetail>(`cuentas/${encodeURIComponent(id)}/baja`)
export const reactivateAccount = (id: string) => post<AccountDetail>(`cuentas/${encodeURIComponent(id)}/reactivar`)

export interface RoleInfo {
  codigo: string
  nombre: string
  descripcion: string | null
  permisos: string[]
  consola: boolean
  cuentas: number
}

export interface RoleCatalog {
  roles: RoleInfo[]
  permisos: { codigo: string; descripcion: string | null }[]
}

export const fetchRoleCatalog = (signal?: AbortSignal) => request<RoleCatalog>('roles', { signal })
export const assignRoles = (id: string, roles: string[]) =>
  request<AccountDetail>(`cuentas/${encodeURIComponent(id)}/roles`, { method: 'PUT', body: JSON.stringify({ roles }) })

export interface ActiveSession {
  cuentaId: string
  usuario: string
  nombre: string
  roles: string[]
  cliente: SessionClient
  desde: string
  actividad: string
  expira: string
}

export interface ActiveSessions {
  totales: Record<SessionClient, number>
  items: ActiveSession[]
}

export const fetchActiveSessions = (client: SessionClient | '', signal?: AbortSignal) =>
  request<ActiveSessions>(`sesiones${client ? `?cliente=${client}` : ''}`, { signal })
export const closeAccountSession = (id: string) => post<AccountDetail>(`cuentas/${encodeURIComponent(id)}/cerrar-sesion`)

/** Sin `clave`, la API genera una temporal y la devuelve en `claveTemporal`. */
export const resetPassword = (id: string, clave?: string) =>
  post<AccountDetail>(`cuentas/${encodeURIComponent(id)}/clave`, clave ? { clave } : {})
export const lockAccount = (id: string) => post<AccountDetail>(`cuentas/${encodeURIComponent(id)}/bloquear`)
export const unlockAccount = (id: string) => post<AccountDetail>(`cuentas/${encodeURIComponent(id)}/desbloquear`)

export const CLIENT_LABELS: Record<SessionClient, string> = {
  WEB_USUARIO: 'Web',
  MOVIL: 'Celular',
  WEB_ADMIN: 'Consola',
}

export const ROLE_LABELS: Record<string, string> = {
  ESTUDIANTE: 'Estudiante',
  DOCENTE: 'Docente',
  SOPORTE: 'Mesa de ayuda',
  ADMINISTRADOR: 'Administración',
}

export const PERMISSION_LABELS: Record<string, string> = {
  'consola.ingresar': 'Entrar a la consola',
  'cuentas.ver': 'Ver cuentas',
  'cuentas.gestionar': 'Crear, editar y dar de baja cuentas',
  'cuentas.datos_personales': 'Ver correos y datos personales completos',
  'roles.asignar': 'Asignar y retirar roles',
  'sesiones.gestionar': 'Ver y cerrar sesiones abiertas',
  'credenciales.restablecer': 'Restablecer contraseñas y desbloquear cuentas',
}

export const STATE_LABELS: Record<AccountState, string> = {
  INVITADO: 'Invitada',
  ACTIVO: 'Activa',
  BLOQUEADO: 'Bloqueada',
  DESHABILITADO: 'De baja',
}

export const AUDIT_LABELS: Record<string, string> = {
  CUENTA_CREADA: 'Cuenta creada',
  CUENTA_MODIFICADA: 'Datos modificados',
  CUENTA_DADA_DE_BAJA: 'Dada de baja',
  CUENTA_REACTIVADA: 'Reactivada',
  SESION_CERRADA: 'Sesión cerrada desde la consola',
  CLAVE_RESTABLECIDA: 'Contraseña restablecida',
  CUENTA_BLOQUEADA: 'Cuenta bloqueada',
  CUENTA_DESBLOQUEADA: 'Cuenta desbloqueada',
}

export function auditLabel(action: string) {
  const [base, role] = action.split(':')
  if (role && base === 'ROL_ASIGNADO') return `Rol asignado: ${ROLE_LABELS[role] ?? role}`
  if (role && base === 'ROL_RETIRADO') return `Rol retirado: ${ROLE_LABELS[role] ?? role}`
  return AUDIT_LABELS[action] ?? action
}

export function displayName(account: Pick<AccountSummary, 'nombre' | 'apellido' | 'nombrePreferido' | 'usuario'>) {
  const full = [account.nombre, account.apellido].filter(Boolean).join(' ')
  return full || account.nombrePreferido || account.usuario
}

export function formatDateTime(value: string | null) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Lima' })
    .format(new Date(value))
}
