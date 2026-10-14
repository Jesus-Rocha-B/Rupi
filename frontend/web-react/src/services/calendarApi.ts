import { ApiError } from './learningRoutesApi'
export interface CalendarEvent { id: string; classroomId: string; classroomName: string; type: 'UNIDAD' | 'EVALUACION'; targetId: string; title: string; start: string; end: string }
export interface Schedule { classrooms: {id: string; name: string}[]; targets: {id: string; title: string; type: 'UNIDAD' | 'EVALUACION'}[]; events: CalendarEvent[] }
export type CalendarDates = Pick<CalendarEvent, 'classroomId' | 'type' | 'targetId' | 'start' | 'end'>
export async function calendarRequest<T>(role: 'teacher' | 'student', route: string, suffix = '', options: RequestInit = {}): Promise<T> {
  const response = await fetch(`/api/v1/${role}/learning-routes/${encodeURIComponent(route)}/calendar${suffix}`, {
    ...options, credentials: 'same-origin', headers: { 'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
    signal: options.signal ? AbortSignal.any([options.signal, AbortSignal.timeout(15000)]) : AbortSignal.timeout(15000),
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new ApiError(response.status, error.detail || 'No pudimos guardar o consultar las fechas. Inténtalo nuevamente.')
  }
  return response.status === 204 ? undefined as T : response.json()
}
