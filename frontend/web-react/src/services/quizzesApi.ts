export interface QuizQuestion {
 id: string; text: string; options: { id: string; text: string }[]
 selectedOption: string | null; correct: boolean | null; explanation: string | null; correctOption: string | null
}
export interface QuizView { status: string; practiceId: string; source: string; reviewState: string | null; answered: number; correct: number; questions: QuizQuestion[] }
/** HU-37: pista adaptada al nivel para entender un error; HU-38: con un ejemplo de la vida real. */
export interface QuizHint { questionId: string; grade: number; level: 'INICIAL' | 'INTERMEDIO' | 'AVANZADO'; reinforced: boolean; hint: string; steps: string[]; realLifeExample: string; source: 'IA' | 'EXPLICACION_REVISADA' }
export const HINT_LEVEL_LABEL: Record<QuizHint['level'], string> = {
 INICIAL: 'Explicación para 1.° y 2.° grado', INTERMEDIO: 'Explicación para 3.° y 4.° grado', AVANZADO: 'Explicación para 5.° y 6.° grado',
}
export interface QuizAvailability { configured: boolean; reviewedAvailable: boolean; practice: QuizView | null }
export interface QuizReview { id: string; topic: string; state: string; provider: string; model: string; questions: QuizQuestion[]; reports: string[] }
export async function quizRequest<T>(path: string, method = 'GET', body?: unknown, signal?: AbortSignal): Promise<T> {
 const response = await fetch('/api/v1/' + path, {
  method, credentials: 'same-origin', headers: { 'Content-Type': 'application/json' },
  body: body === undefined ? undefined : JSON.stringify(body),
  signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(45000)]) : AbortSignal.timeout(45000),
 })
 if (!response.ok) {
  const problem = await response.json().catch(() => null) as { detail?: string } | null
  throw new Error(problem?.detail || 'No pudimos completar la práctica. Puedes continuar tu lección.')
 }
 return response.status === 204 ? undefined as T : response.json()
}
