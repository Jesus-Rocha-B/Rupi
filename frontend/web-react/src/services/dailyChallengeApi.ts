/**
 * dailyChallengeApi.ts
 * Cliente del reto diario de razonamiento lógico y comprensión lectora (HU-40).
 * Las respuestas se guardan con submitQuestionAnswer (mismo endpoint de las lecciones).
 */
import { quizRequest } from './quizzesApi'
import type { QuestionDto } from './learningRoutesApi'

export type ChallengeSkill = 'RAZONAMIENTO_LOGICO' | 'COMPRENSION_LECTORA'

export interface DailyChallenge {
  id: string
  date: string
  activityVersionId: string
  title: string
  description: string | null
  skill: ChallengeSkill
  readingText: string | null
  estimatedMinutes: number
  isCompleted: boolean
  experienceReward: number
  questions: QuestionDto[]
}

export interface DailyChallengeResult {
  challengeId: string
  isCompleted: boolean
  experienceEarned: number
  totalExperience: number
  correctAnswers: number
  totalQuestions: number
  rupiMessage: string
}

export const fetchDailyChallenge = (signal?: AbortSignal) =>
  quizRequest<DailyChallenge>('student/daily-challenge', 'GET', undefined, signal)

export const completeDailyChallenge = (challengeId: string, signal?: AbortSignal) =>
  quizRequest<DailyChallengeResult>(`student/daily-challenge/${encodeURIComponent(challengeId)}/complete`, 'POST', undefined, signal)
