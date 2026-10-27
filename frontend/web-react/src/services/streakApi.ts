import { ApiError } from './learningRoutesApi'

/** Racha de días seguidos con alguna parada completada (historia 30). Se calcula en el servidor en hora de Perú. */
export interface StudyStreak {
  days: number
  activeToday: boolean
  atRisk: boolean
  localDate: string
}

export async function fetchStudyStreak(signal?: AbortSignal): Promise<StudyStreak> {
  const response = await fetch('/api/v1/student/streak', {
    credentials: 'same-origin',
    signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(15000)]) : AbortSignal.timeout(15000),
    headers: { 'X-Requested-With': 'XMLHttpRequest' },
  })
  if (!response.ok) throw new ApiError(response.status, 'No pudimos consultar tu racha.')
  return response.json()
}
