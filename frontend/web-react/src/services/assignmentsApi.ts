export interface Assignment {
  id: string; studentId: string; studentName: string; room: string; route: string; node: string
  title: string; instructions: string; reason: string; resourceTitle: string; url: string; nodeTitle: string
  answered: number; correct: number; assignedAt: string; startedAt: string | null; completedAt: string | null; reflection: string | null
}
export interface Candidate {
  studentId: string; studentName: string; nodeId: string; nodeTitle: string; unitTitle: string; status: string
  answered: number; correct: number; resources: { id: string; title: string; url: string }[]
}
export interface AssignmentBoard { available: boolean; candidates: Candidate[]; assignments: Assignment[] }
export interface StudentAssignmentBoard { available: boolean; assignments: Assignment[] }
export async function assignmentsRequest<T>(role: 'teacher' | 'student', suffix = '', method = 'GET', body?: unknown, signal?: AbortSignal): Promise<T> {
  const response = await fetch('/api/v1/' + role + '/reinforcement-assignments' + suffix, {
    method, credentials: 'same-origin', headers: { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(20000)]) : AbortSignal.timeout(20000),
  })
  if (!response.ok) {
    const problem = await response.json().catch(() => null) as { detail?: string } | null
    throw new Error(problem?.detail || 'No pudimos consultar los refuerzos. Comprueba tu conexión y acceso.')
  }
  return response.status === 204 ? undefined as T : response.json()
}
export const assignmentState = (task: Pick<Assignment, 'completedAt' | 'startedAt'>) =>
  task.completedAt ? 'Completado según el estudiante' : task.startedAt ? 'En curso' : 'Pendiente'
