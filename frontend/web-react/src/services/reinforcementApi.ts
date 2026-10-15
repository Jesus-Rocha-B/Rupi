import { ApiError } from './learningRoutesApi'
export interface ReinforcementView {
  status: 'SIN_EVIDENCIA' | 'AVANCE' | 'REFUERZO' | 'SIN_RECURSO'
  nodeTitle: string
  answered: number
  correct: number
  threshold: number
  practice: { question: string; hint: string | null }[]
  resources: { id: string; title: string; url: string; publisher: string; completed: boolean }[]
}
export async function reinforcementRequest<T>(route: string, node: string, suffix: string, method: 'POST' | 'PUT', signal?: AbortSignal): Promise<T> {
  const response = await fetch(`/api/v1/student/learning-routes/${encodeURIComponent(route)}/nodes/${encodeURIComponent(node)}/reinforcements${suffix}`, {
    method, credentials: 'same-origin', headers: { 'X-Requested-With': 'XMLHttpRequest' },
    signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(15000)]) : AbortSignal.timeout(15000),
  })
  if (!response.ok) throw new ApiError(response.status, 'No pudimos actualizar tu refuerzo.')
  return response.status === 204 ? undefined as T : response.json()
}
