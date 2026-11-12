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

export interface LanguageConfig { extras: Record<string, string[]>; alternativa: string }
export interface LanguageState {
  configuracion: LanguageConfig; revision: number; versionPublicada: number; probada: boolean
  reglas: { codigo: string; nombre: string; descripcion: string }[]
  alternativas: string[]; casos: { nombre: string; texto: string }[]
  historial: { version: number; fecha: string; autor: string }[]
}
export interface LanguageDecision { permitida: boolean; reglas: string[]; alternativa: string | null; metodo: string; aviso: string }
export const fetchLanguageRules = (signal?: AbortSignal) => request<LanguageState>('moderacion/reglas', { signal })
export const saveLanguageRules = (revision: number, configuracion: LanguageConfig) => request<LanguageState>('moderacion/reglas', { method: 'PUT', body: JSON.stringify({ revision, configuracion }) })
export const evaluateLanguage = (revision: number, texto: string) => post<LanguageDecision>('moderacion/evaluar', { revision, texto })
export const testLanguageRules = (revision: number) => post<{ estado: LanguageState; resultados: { caso: string; correcto: boolean }[] }>('moderacion/probar', { revision })
export const publishLanguageRules = (revision: number) => post<LanguageState>('moderacion/publicar', { revision })

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
  correoNotificado?: boolean
  correoDestino?: string | null
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

export type ServiceState = 'OK' | 'LENTO' | 'CAIDO' | 'SIN_CONFIGURAR'

export interface RouteTiming {
  route: string
  requests: number
  p50Ms: number
  p95Ms: number
  maxMs: number
  serverErrors: number
  lastAt: string
}

export interface RequestWindow {
  requests: number
  p50Ms: number
  p95Ms: number
  maxMs: number
  serverErrors: number
  perMinute: number
}

export interface ServiceHealth {
  clave: 'base' | 'api' | 'ia' | 'consola'
  nombre: string
  estado: ServiceState
  latenciaMs: number | null
  detalle: Record<string, unknown>
  peticiones?: { ventana: RequestWindow; total: number; rutasLentas: RouteTiming[] }
}

export interface HealthAlert {
  id: string
  nivel: 'CRITICA' | 'ADVERTENCIA'
  servicio: 'base' | 'api' | 'ia' | 'consola'
  titulo: string
  mensaje: string
  impacto?: string
  sugerencia?: string
}

export interface PlatformHealth {
  estado: 'OK' | 'ATENCION' | 'FALLA'
  revisadoEn: string
  umbrales: {
    latenciaLentaMs: number
    p95LentoMs: number
    erroresAlerta: number
    cpuAlertaPercent?: number
    cpuCriticaPercent?: number
    memoriaAlertaPercent?: number
    memoriaCriticaPercent?: number
  }
  alertas: HealthAlert[]
  servicios: ServiceHealth[]
}

export const fetchPlatformHealth = (signal?: AbortSignal) => request<PlatformHealth>('salud', { signal })

export type IncidentSeverity = 'CRITICA' | 'ALTA' | 'MEDIA' | 'BAJA'
export type IncidentState = 'ABIERTO' | 'EN_ATENCION' | 'RESUELTO' | 'CERRADO'

export interface IncidentDto {
  id: string
  servicio: string
  categoria: string
  severidad: IncidentSeverity
  estado: IncidentState
  resumen: string
  detalleError?: string | null
  iniciadoEn: string
  resueltoEn?: string | null
  duracionSegundos?: number | null
  duracionTexto?: string | null
}

export interface AlertChannelsConfig {
  emailResponsable: string
  smsNumero: string | null
  webhookConfigurado: boolean
  autoreinicioHabilitado: boolean
}

export interface IncidentsResponse {
  incidentes: IncidentDto[]
  total: number
  abiertos: number
  canalesAlerta: AlertChannelsConfig
}

export interface TestAlertResponse {
  emailEnviado: boolean
  destinatarioEmail: string
  errorEmail?: string | null
  smsEnviado: boolean
  destinatarioSms?: string | null
  mensaje: string
}

export interface RestartServiceResponse {
  servicio: string
  accion: string
  exito: boolean
  tiempoMs: number
  mensaje: string
  puertos?: { puerto: number; abierto: boolean }[]
}

export const fetchIncidents = (params?: { estado?: string; limite?: number }, signal?: AbortSignal) => {
  const query = new URLSearchParams()
  if (params?.estado) query.set('estado', params.estado)
  if (params?.limite) query.set('limite', String(params.limite))
  const qs = query.toString()
  return request<IncidentsResponse>(`incidentes${qs ? `?${qs}` : ''}`, { signal })
}

export const resolveIncident = (incidentId: string, comentario?: string) =>
  request<{ id: string; servicio: string; estado: string; resueltoEn: string; mensaje: string }>(
    `incidentes/${incidentId}/resolver`,
    {
      method: 'POST',
      body: JSON.stringify({ comentario: comentario ?? '' }),
    }
  )

export const triggerMonitoringCycle = () =>
  request<{
    revisadoEn: string
    estadoGeneral: string
    servicios: ServiceHealth[]
    alertas: HealthAlert[]
    novedades: { servicio: string; evento: string; incidenteId?: string; autoreinicio?: string }[]
    accionesTomadas: { servicio: string; accion: string; exito: boolean; detalle?: string }[]
    incidentesAbiertos: IncidentDto[]
    totalIncidentesAbiertos: number
  }>('monitoreo/ciclo', {
    method: 'POST',
    body: JSON.stringify({}),
  })

export const testAlertChannels = () =>
  request<TestAlertResponse>('alertas/probar', {
    method: 'POST',
    body: JSON.stringify({}),
  })

export const restartService = (servicio: string, forzar: boolean = true) =>
  request<RestartServiceResponse>(`servicios/${servicio}/reiniciar`, {
    method: 'POST',
    body: JSON.stringify({ forzar }),
  })

// =====================================================================
// Tablero Centralizado de Arquitectura, Uptime y Errores (HU-48)
// =====================================================================

export interface ArchitectureSummary {
  uptime24h: number
  uptime7d: number
  uptime30d: number
  segundosCaida24h: number
  tiempoCaidaTexto24h: string
  totalErroresReportados: number
  errores24h: number
  incidentesAbiertos: number
  estadoServidor: 'OPERATIVO' | 'DEGRADADO' | 'CAIDO'
  slaCalidad: 'EXCELENTE' | 'BUENO' | 'EN_RIESGO'
  latenciaPromedioMs: number
}

export interface TimeSeriesPoint {
  etiqueta: string
  timestamp: string
  uptimePercent: number
  errores: number
  latenciaMs: number
}

export interface ErrorCategoryBreakdown {
  categoria: string
  cantidad: number
  porcentaje: number
  descripcion: string
  color: string
}

export interface ServerOutageRecord {
  id: string
  servidor: string
  tipoEvento: 'CAIDA' | 'DEGRADACION' | 'RECUPERACION'
  iniciadoEn: string
  restablecidoEn: string | null
  duracionSegundos: number | null
  duracionTexto: string
  motivo: string
  codigoError?: string | null
  impactoColegios: string
  registradoEn: string
}

export interface ArchitectureDashboardData {
  revisadoEn: string
  resumen: ArchitectureSummary
  serieTemporal24h: TimeSeriesPoint[]
  serieTemporal7d: TimeSeriesPoint[]
  desgloseErrores: ErrorCategoryBreakdown[]
  historialCaidas: ServerOutageRecord[]
  totalHistorialCaidas: number
}

export const fetchArchitectureDashboard = (signal?: AbortSignal) =>
  request<ArchitectureDashboardData>('tablero-arquitectura', { signal })

export const recordServerOutage = (data: {
  servidor?: string
  tipoEvento?: string
  motivo: string
  codigoError?: string
  impactoColegios?: string
  duracionSegundos?: number
}) =>
  request<ServerOutageRecord>('tablero-arquitectura/caidas', {
    method: 'POST',
    body: JSON.stringify(data),
  })

export interface SyncTrafficSummary {
  totalTrafico: number
  traficoWeb: number
  traficoMovil: number
  eventosAplicados: number
  eventosDuplicados: number
  eventosError: number
  discrepanciasDetectadas: number
  tasaExitoPercent: number
}

export interface SyncAlert {
  id: string
  nivel: 'CRITICA' | 'ADVERTENCIA'
  titulo: string
  mensaje: string
  impacto?: string
  sugerencia?: string
}

export interface SyncTrafficEvent {
  id: string
  usuarioId: string
  usuario: string
  nombre: string
  clienteTipo: 'WEB' | 'MOVIL'
  tipoEvento: string
  puntajeXp: number
  leccionesCompletadas: number
  estado: 'RECIBIDO' | 'APLICADO' | 'DUPLICADO' | 'RECHAZADO' | 'ERROR'
  codigoResultado: string | null
  discrepanciaDetectada: boolean
  recibidoEn: string
}

export interface SyncDeviceComparison {
  usuarioId: string
  usuario: string
  nombreCompleto: string
  puntajeBd: number
  leccionesBd: number
  reporteWeb: {
    puntajeXp: number
    lecciones: number
    fecha: string | null
  }
  reporteMovil: {
    puntajeXp: number
    lecciones: number
    fecha: string | null
  }
  estado: 'SINCRONIZADO' | 'DISCREPANCIA' | 'ALINEADO'
  diferenciaXp: number
  diferenciaLecciones: number
}

export interface SyncAuditData {
  revisadoEn: string
  resumen: SyncTrafficSummary
  alertas: SyncAlert[]
  comparaciones: SyncDeviceComparison[]
  eventosRecientes: SyncTrafficEvent[]
}

export const fetchSyncAudit = (signal?: AbortSignal) => request<SyncAuditData>('sincronizacion', { signal })

// Catálogo general de grados, áreas y competencias del MINEDU (HU-49)
export type CatalogState = 'BORRADOR' | 'ACTIVO' | 'RETIRADO'

export interface CatalogVersion {
  id: string
  etiqueta: string
  referencia: string | null
  vigenteDesde: string | null
  estado: CatalogState
}

export interface CurriculumCapacity {
  id: string
  orden: number
  nombre: string
}

export interface CurriculumCompetency {
  id: string
  codigo: string
  numeroCneb: number | null
  nombre: string
  descripcion: string | null
  /** Unidades del catálogo que desarrollan esta competencia. */
  unidades: number
  capacidades: CurriculumCapacity[]
}

export interface CurriculumArea {
  id: string
  codigo: string
  nombre: string
  orden: number | null
  unidades: number
  competencias: CurriculumCompetency[]
}

export interface CurriculumCatalog {
  versiones: CatalogVersion[]
  version: CatalogVersion | null
  /** false si la versión está RETIRADA: queda como registro. */
  editable: boolean
  grados: { id: string; numero: number; nombre: string }[]
  areas: CurriculumArea[]
  resumen: { grados: number; areas: number; competencias: number; capacidades: number }
}

export interface AreaInput {
  version: string
  codigo: string
  nombre: string
  orden: number | null
}

export interface CurriculumUnit { id: string; codigo: string; titulo: string; gradoId: string; areaId: string; grado: string; area: string; estado: string }
export interface CurriculumGoal { id: string; codigo: string; descripcion: string; competenciaId: string; competencia: string; tipo: 'META' | 'ESTANDAR'; revision: string; unidadesVinculadas: number; historial: { anterior: string | null; nueva: string; motivo: string; fecha: string; autor: string }[] }
export interface UnitGoals { competencias: { id: string; codigo: string; nombre: string }[]; registros: CurriculumGoal[]; editable: boolean }
export interface GoalInput { tipo: 'META' | 'ESTANDAR'; codigo: string; descripcion: string; competenciaId: string }
export const fetchCurriculumUnits = (version: string, signal?: AbortSignal) => request<CurriculumUnit[]>(`curriculo/unidades?version=${encodeURIComponent(version)}`, { signal })
export const fetchUnitGoals = (unit: string, signal?: AbortSignal) => request<UnitGoals>(`curriculo/unidades/${unit}/registros`, { signal })
export const createUnitGoal = (unit: string, data: GoalInput) => post<UnitGoals>(`curriculo/unidades/${unit}/registros`, data)
export const updateUnitGoal = (unit: string, record: CurriculumGoal, descripcion: string, motivo: string) => request<UnitGoals>(`curriculo/unidades/${unit}/registros/${record.tipo}/${record.id}`, { method: 'PATCH', body: JSON.stringify({ descripcion, motivo, revision: record.revision }) })

export interface CompetencyInput {
  area: string
  codigo: string
  numeroCneb: number | null
  nombre: string
  descripcion: string
  capacidades: string[]
}

export interface SchoolPeriod { nombre: string; comienza: string; termina: string }
export interface PeriodCalendar { id: string | null; tipo: 'BIMESTRE' | 'TRIMESTRE'; periodos: SchoolPeriod[]; revision: number; editable: boolean }
export interface PeriodOptions {
  calendarios: { id: string; anio: number; comienza: string | null; termina: string | null; estado: string; institucion: string }[]
  grados: { id: string; nombre: string }[]
  areas: { id: string; nombre: string }[]
}
export interface PeriodPreview { token: string; revision: number; aplicables: number; planificaciones: { id: string; titulo: string; aula: string; ruta: string; periodo: SchoolPeriod | null }[] }
export const fetchPeriodOptions = (signal?: AbortSignal) => request<PeriodOptions>('curriculo/periodos/opciones', { signal })
export const fetchPeriods = (year: string, signal?: AbortSignal) => request<PeriodCalendar>(`curriculo/periodos/${year}`, { signal })
export const savePeriods = (year: string, data: Pick<PeriodCalendar, 'tipo' | 'periodos' | 'revision'>) => request<PeriodCalendar>(`curriculo/periodos/${year}`, { method: 'PUT', body: JSON.stringify(data) })
export const previewPeriods = (year: string, grado: string, areas: string[]) => post<PeriodPreview>(`curriculo/periodos/${year}/revisar`, { grado, areas })
export const applyPeriods = (year: string, grado: string, areas: string[], token: string) => post<{ aplicadas: number; omitidas: number }>(`curriculo/periodos/${year}/aplicar`, { grado, areas, token })

export const fetchCurriculumCatalog = (version: string | null, signal?: AbortSignal) =>
  request<CurriculumCatalog>(`curriculo/catalogo${version ? `?version=${encodeURIComponent(version)}` : ''}`, { signal })
export const createCurriculumArea = (area: AreaInput) => post<CurriculumCatalog>('curriculo/areas', area)
export const updateCurriculumArea = (id: string, changes: Pick<AreaInput, 'nombre' | 'orden'>) =>
  request<CurriculumCatalog>(`curriculo/areas/${encodeURIComponent(id)}`, { method: 'PATCH', body: JSON.stringify(changes) })
export const createCompetency = (competency: CompetencyInput) => post<CurriculumCatalog>('curriculo/competencias', competency)
export const updateCompetency = (id: string, changes: Omit<CompetencyInput, 'area' | 'codigo'>) =>
  request<CurriculumCatalog>(`curriculo/competencias/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    body: JSON.stringify(changes),
  })


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
  'interacciones.ver': 'Consultar historial restringido de IA',
  'interacciones.revisar': 'Revisar interacciones de IA',
  'moderacion.ver': 'Consultar reglas y probar respuestas de IA',
  'moderacion.gestionar': 'Configurar y publicar reglas de lenguaje',
  'consola.ingresar': 'Entrar a la consola',
  'cuentas.ver': 'Ver cuentas',
  'cuentas.gestionar': 'Crear, editar y dar de baja cuentas',
  'cuentas.datos_personales': 'Ver correos y datos personales completos',
  'roles.asignar': 'Asignar y retirar roles',
  'sesiones.gestionar': 'Ver y cerrar sesiones abiertas',
  'credenciales.restablecer': 'Restablecer contraseñas y desbloquear cuentas',
  'plataforma.monitorear': 'Ver la salud de los servidores',
  'sincronizacion.auditar': 'Auditar sincronización web y celular',
  'curriculo.ver': 'Consultar el catálogo curricular',
  'curriculo.gestionar': 'Registrar y corregir el catálogo curricular',
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


export interface InteractionRow {
  id: string; recurso: string; origen: string; permitida: boolean; creada_en: string
  revisada_en: string | null; duracion_ms: number | null
}
export interface InteractionDetail extends InteractionRow {
  proveedor: string | null; modelo: string | null; pregunta: string | null; respuesta: string
  alternativa: string | null; reglas: string[]; version_reglas: number; revisor_id: string | null
  traza: { accion: string; fecha: string }[]
}
export interface InteractionPage { items: InteractionRow[]; total: number; pendientes: number; promedio: number | null; pagina: number; porPagina: number }
export const fetchInteractions = (params: URLSearchParams, signal?: AbortSignal) => request<InteractionPage>(`moderacion/interacciones?${params}`, { signal })
export const fetchInteraction = (id: string, signal?: AbortSignal) => request<InteractionDetail>(`moderacion/interacciones/${encodeURIComponent(id)}`, { signal })
export const reviewInteraction = (id: string) => post<InteractionDetail>(`moderacion/interacciones/${encodeURIComponent(id)}`)

// HU-51 · Resoluciones ministeriales y correspondencia curricular
export type CorrespondenceChange = 'SE_MANTIENE' | 'MODIFICADA' | 'RETIRADA' | 'NUEVA'
export type CorrespondenceType = 'AREA' | 'COMPETENCIA' | 'UNIDAD'
export interface Resolution {
  id: string; numero: string; fechaEmision: string; resumen: string; enlace: string | null
  origen: { id: string; etiqueta: string; estado: CatalogState }
  destino: { id: string; etiqueta: string; estado: CatalogState; vigenteDesde: string | null }
  estado: 'BORRADOR' | 'PUBLICADA'; creadoEn: string; publicadoEn: string | null
}
export interface ResolutionList {
  resoluciones: Resolution[]; vigente: { id: string; etiqueta: string; vigenteDesde: string | null } | null; puedeRegistrar: boolean
}
export interface CorrespondenceItem {
  id: string | null; codigo: string; grado: string | null; origen: string | null; destino: string | null
  cambio: CorrespondenceChange; nota: string | null; editable: boolean
}
export interface ResolutionDetail {
  resolucion: Resolution; editable: boolean
  elementos: Record<CorrespondenceType, CorrespondenceItem[]>
  resumen: Record<CorrespondenceType, Record<CorrespondenceChange, number>>
  impacto: { rutas: number; rutasPublicadas: number; paradasSinUnidad: number }
}
export interface ResolutionInput { numero: string; fechaEmision: string; resumen: string; enlace?: string; etiqueta: string; vigenteDesde: string }
export interface CorrespondenceInput { accion: 'MANTENER' | 'CORREGIR' | 'RETIRAR'; titulo?: string; nota?: string }
export const fetchResolutions = (signal?: AbortSignal) => request<ResolutionList>('curriculo/resoluciones', { signal })
export const fetchResolution = (id: string, signal?: AbortSignal) => request<ResolutionDetail>(`curriculo/resoluciones/${encodeURIComponent(id)}`, { signal })
export const registerResolution = (data: ResolutionInput) => post<ResolutionDetail>('curriculo/resoluciones', data)
export const updateCorrespondence = (resolution: string, item: string, data: CorrespondenceInput) =>
  request<ResolutionDetail>(`curriculo/resoluciones/${encodeURIComponent(resolution)}/correspondencias/${encodeURIComponent(item)}`, { method: 'PATCH', body: JSON.stringify(data) })
export const publishResolution = (resolution: string, confirmacion: string) =>
  post<ResolutionDetail>(`curriculo/resoluciones/${encodeURIComponent(resolution)}/publicar`, { confirmacion })
