export interface TeacherGrade {
  number: number
  name: string
}

export interface TeacherArea {
  code: string
  name: string
}

export interface AttachedResource {
  id: string
  type: 'DOCUMENTO' | 'ENLACE' | 'PRESENTACION' | 'IMAGEN'
  title: string
  url: string
  multimediaId: string | null
  createdAt: string
}

export interface TeacherNode {
  id: string
  sequence: number
  title: string
  instructions: string | null
  activityType: string
  estimatedMinutes: number | null
  mapX: number | null
  mapY: number | null
  resources: AttachedResource[]
}

export interface TeacherRouteSummary {
  routeId: string
  versionRouteId: string
  title: string
  description: string | null
  gradeNumber: number
  gradeName: string
  areaCode: string
  areaName: string
  state: 'BORRADOR' | 'PUBLICADO' | 'PAUSADO' | 'RETIRADO'
  totalNodes: number
  createdAt: string
  publishedAt: string | null
}

export interface TeacherRouteDetail {
  routeId: string
  versionRouteId: string
  title: string
  description: string | null
  grade: TeacherGrade
  area: TeacherArea
  state: 'BORRADOR' | 'PUBLICADO' | 'PAUSADO' | 'RETIRADO'
  nodes: TeacherNode[]
  createdAt: string
  publishedAt: string | null
}

export interface CreateRoutePayload {
  title: string
  description?: string
  gradeNumber: number
  areaCode: string
}

export interface CreateNodePayload {
  title: string
  instructions?: string
  activityType?: string
  estimatedMinutes?: number
  sequence?: number
  mapX?: number
  mapY?: number
}

export interface AttachResourcePayload {
  type: 'DOCUMENTO' | 'ENLACE' | 'PRESENTACION' | 'IMAGEN'
  title: string
  url: string
  multimediaId?: string | null
}

export interface UploadMaterialResult {
  id: string
  originalName: string
  mimeType: string
  sizeBytes: number
  downloadUrl: string
}

import { ApiError } from './learningRoutesApi'

async function teacherRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const url = `/api/v1/${path}`
  const headers = new Headers(options.headers || {})
  headers.set('X-Requested-With', 'XMLHttpRequest')

  const res = await fetch(url, {
    ...options,
    headers,
    credentials: 'include',
  })

  if (!res.ok) {
    let msg = `Error ${res.status}`
    try {
      const errJson = await res.json()
      if (errJson.detail) msg = errJson.detail
      else if (errJson.message) msg = errJson.message
    } catch {
      // Ignorar error al parsear JSON
    }
    throw new ApiError(res.status, msg)
  }

  const contentType = res.headers.get('content-type')
  if (contentType && contentType.includes('application/json')) {
    return (await res.json()) as T
  }
  return null as unknown as T
}

export const fetchTeacherRoutes = (signal?: AbortSignal) =>
  teacherRequest<TeacherRouteSummary[]>('teacher/learning-routes', { signal })

export const fetchTeacherRouteDetail = (routeId: string, signal?: AbortSignal) =>
  teacherRequest<TeacherRouteDetail>(`teacher/learning-routes/${encodeURIComponent(routeId)}`, { signal })

export const createTeacherRoute = (payload: CreateRoutePayload) =>
  teacherRequest<{ routeId: string; versionRouteId: string; title: string; state: string; message: string }>(
    'teacher/learning-routes',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }
  )

export const createTeacherNode = (routeId: string, payload: CreateNodePayload) =>
  teacherRequest<{ nodeId: string; message: string }>(
    `teacher/learning-routes/${encodeURIComponent(routeId)}/nodes`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }
  )

export const attachResourceToNode = (routeId: string, nodeId: string, payload: AttachResourcePayload) =>
  teacherRequest<{ resourceId: string; message: string }>(
    `teacher/learning-routes/${encodeURIComponent(routeId)}/nodes/${encodeURIComponent(nodeId)}/attachments`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }
  )

export const publishTeacherRoute = (routeId: string) =>
  teacherRequest<{ routeId: string; versionRouteId: string; state: string; publishedAt: string; message: string }>(
    `teacher/learning-routes/${encodeURIComponent(routeId)}/publish`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    }
  )

export const uploadTeacherMaterial = async (file: File): Promise<UploadMaterialResult> => {
  const formData = new FormData()
  formData.append('file', file)

  const res = await fetch('/api/v1/teacher/materials/upload', {
    method: 'POST',
    body: formData,
    credentials: 'include',
    headers: {
      'X-Requested-With': 'XMLHttpRequest',
    },
  })

  if (!res.ok) {
    let msg = `Error al subir archivo (${res.status})`
    try {
      const errJson = await res.json()
      if (errJson.detail) msg = errJson.detail
      else if (errJson.message) msg = errJson.message
    } catch {
      // Ignorar
    }
    throw new ApiError(res.status, msg)
  }

  return (await res.json()) as UploadMaterialResult
}
