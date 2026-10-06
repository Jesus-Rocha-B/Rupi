export interface Grade {
  number: number
  name: string
}

export interface Area {
  code: string
  name: string
}

export interface Position {
  x: number
  y: number
}

export type NodeState = 'BLOQUEADO' | 'DISPONIBLE' | 'EN_CURSO' | 'COMPLETADO'

export interface RouteNode {
  id: string
  sequence: number
  title: string
  activityType: string
  estimatedMinutes: number | null
  state: NodeState
  optional: boolean
  position: Position | null
}

export interface RouteSummary {
  versionRouteId: string
  title: string
  grade: Grade
  area: Area
  enrollmentState: string
  completedNodes: number
  totalNodes: number
}

export interface RouteDetail {
  versionRouteId: string
  title: string
  grade: Grade
  area: Area
  enrollment: {
    id: string
    state: string
    lastVisitedNodeId: string | null
  }
  progress: {
    completedNodes: number
    totalNodes: number
  }
  nodes: RouteNode[]
}

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
    this.name = 'ApiError'
  }
}


const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

export interface Student { id: string; name: string }

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE}/api/v1/${path}`, {
    credentials: 'same-origin',
    ...options,
    signal: options.signal
      ? AbortSignal.any([options.signal, AbortSignal.timeout(15000)])
      : AbortSignal.timeout(15000),
  })
  if (!response.ok) throw new ApiError(response.status, `No se pudo consultar la información (${response.status})`)
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const fetchSession = (signal?: AbortSignal) => request<Student>('student/session', { signal })
export const fetchSessionOptions = (signal?: AbortSignal) => request<{ demoAvailable: boolean }>('auth/options', { signal })
export const loginDemo = () => request<Student>('auth/demo', { method: 'POST' })
export const logout = () => request<void>('auth/logout', { method: 'POST' })
export async function fetchStudentRoutes(signal?: AbortSignal): Promise<RouteSummary[]> {
  return (await request<{ items: RouteSummary[] }>('student/learning-routes', { signal })).items
}
export const fetchRouteDetail = (id: string, signal?: AbortSignal) =>
  request<RouteDetail>(`student/learning-routes/${encodeURIComponent(id)}`, { signal })
