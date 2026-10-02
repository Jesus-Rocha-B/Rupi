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

export async function fetchStudentRoutes(studentId?: string): Promise<RouteSummary[]> {
  const headers: HeadersInit = {}
  if (studentId) {
    headers['Authorization'] = `Bearer ${studentId}`
    headers['X-Student-Id'] = studentId
  }

  const response = await fetch(`${API_BASE}/api/v1/student/learning-routes`, {
    headers,
  })

  if (!response.ok) {
    throw new ApiError(response.status, `Error al obtener rutas: ${response.status}`)
  }

  const data = await response.json()
  return data.items ?? []
}

export async function fetchRouteDetail(versionRouteId: string, studentId?: string): Promise<RouteDetail> {
  const headers: HeadersInit = {}
  if (studentId) {
    headers['Authorization'] = `Bearer ${studentId}`
    headers['X-Student-Id'] = studentId
  }

  const response = await fetch(`${API_BASE}/api/v1/student/learning-routes/${versionRouteId}`, {
    headers,
  })

  if (!response.ok) {
    throw new ApiError(response.status, `Error al obtener la ruta: ${response.status}`)
  }

  return response.json()
}
