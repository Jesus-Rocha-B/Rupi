import { ApiError } from './learningRoutesApi'
export interface EvaluationReminder {
  id: string
  classroomId: string
  classroomName: string
  title: string
  startsAt: string
  read: boolean
}
export async function remindersRequest<T>(routeId: string, suffix = '', options: RequestInit = {}): Promise<T> {
  const response = await fetch(`/api/v1/student/learning-routes/${encodeURIComponent(routeId)}/reminders${suffix}`, {
    ...options, credentials: 'same-origin',
    signal: options.signal ? AbortSignal.any([options.signal, AbortSignal.timeout(15000)]) : AbortSignal.timeout(15000),
    headers: { 'X-Requested-With': 'XMLHttpRequest' },
  })
  if (!response.ok) throw new ApiError(response.status, 'No pudimos actualizar tus recordatorios. Vuelve a intentarlo.')
  return response.status === 204 ? undefined as T : response.json()
}
