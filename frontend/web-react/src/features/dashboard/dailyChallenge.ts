import type { ChallengeSkill, DailyChallenge } from '../../services/dailyChallengeApi.ts'

/** Etiquetas visibles del tipo de reto (HU-40). */
export const SKILL_LABEL: Record<ChallengeSkill, string> = {
  RAZONAMIENTO_LOGICO: 'Razonamiento lógico',
  COMPRENSION_LECTORA: 'Comprensión lectora',
}

/** Lo que se lee al pulsar Escuchar: título, consigna y, si es de lectura, el texto completo. */
export function challengeSpeech(challenge: Pick<DailyChallenge, 'title' | 'description' | 'readingText'>): string {
  return [challenge.title + '.', challenge.description ?? '', challenge.readingText ? 'Lectura. ' + challenge.readingText : '']
    .filter(Boolean)
    .join(' ')
}

/** El reto se puede terminar cuando todas sus preguntas tienen una respuesta guardada. */
export function canFinish(questionIds: string[], answered: Record<string, unknown>): boolean {
  return questionIds.length > 0 && questionIds.every(id => answered[id] !== undefined)
}
