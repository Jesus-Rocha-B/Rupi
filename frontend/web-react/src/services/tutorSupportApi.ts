import type { TutorAssistanceMode } from './aiApi'

/** HU-15: ayuda del tutor Rupi configurada por el docente para un aula. */
export interface ClassroomTutorSupport {
  classroomId: string; classroomName: string; grade: number; mode: TutorAssistanceMode
  configured: boolean; updatedAt: string | null; updatedBy: string | null
}

const base = (room: string) => `/api/v1/teacher/tutor-support/classrooms/${encodeURIComponent(room)}`

async function request(room: string, init: RequestInit = {}, signal?: AbortSignal): Promise<ClassroomTutorSupport> {
  const response = await fetch(base(room), {
    ...init, credentials: 'same-origin', headers: { 'Content-Type': 'application/json' },
    signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(15000)]) : AbortSignal.timeout(15000),
  })
  if (!response.ok) {
    const problem = await response.json().catch(() => null) as { detail?: string } | null
    throw new Error(problem?.detail || 'No pudimos guardar la ayuda del tutor. Comprueba tu conexión y el acceso al aula.')
  }
  return response.json()
}

export const fetchClassroomTutorSupport = (room: string, signal?: AbortSignal) => request(room, {}, signal)
export const saveClassroomTutorSupport = (room: string, mode: TutorAssistanceMode) =>
  request(room, { method: 'PUT', body: JSON.stringify({ mode }) })
