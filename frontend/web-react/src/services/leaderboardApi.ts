/**
 * leaderboardApi.ts
 * Cliente API para la tabla de logros y Leaderboard del aula (HU-32).
 */

const BASE_URL = '/api/v1'

export interface EquippedSummary {
  sombrero: string | null
  vestimenta: string | null
  fondo: string | null
}

export interface LeaderboardEntry {
  rank: number
  studentId: string
  displayName: string
  aliasPublico: string | null
  esUsuarioActual: boolean
  totalXp: number
  streakDays: number
  streakActiveToday: boolean
  equipped: EquippedSummary
}

export interface PrivacySettings {
  mostrarEnClasificacion: boolean
  aliasPublico: string | null
  mostrarComo: 'NOMBRE' | 'ALIAS'
}

export interface ClassroomLeaderboard {
  classroomId: string
  classroomName: string
  gradeNumber: number
  sortBy: 'XP' | 'RACHA'
  myRank: number
  totalStudents: number
  entries: LeaderboardEntry[]
  myPrivacy: PrivacySettings
}

export async function fetchClassroomLeaderboard(
  sortBy: 'XP' | 'RACHA' = 'XP',
  signal?: AbortSignal
): Promise<ClassroomLeaderboard> {
  const res = await fetch(`${BASE_URL}/student/leaderboard?sort=${sortBy}`, {
    method: 'GET',
    headers: {
      Accept: 'application/json',
    },
    signal,
  })

  if (!res.ok) {
    let msg = 'No se pudo cargar la tabla de posiciones del aula.'
    try {
      const err = await res.json()
      if (err.message) msg = err.message
    } catch {
      // Ignorar fallback
    }
    throw new Error(msg)
  }

  return res.json()
}

export async function fetchLeaderboardPrivacy(
  signal?: AbortSignal
): Promise<PrivacySettings> {
  const res = await fetch(`${BASE_URL}/student/leaderboard/privacy`, {
    method: 'GET',
    headers: {
      Accept: 'application/json',
    },
    signal,
  })

  if (!res.ok) {
    throw new Error('No se pudo consultar tus opciones de privacidad.')
  }

  return res.json()
}

export async function updateLeaderboardPrivacy(
  settings: PrivacySettings
): Promise<PrivacySettings> {
  const res = await fetch(`${BASE_URL}/student/leaderboard/privacy`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    body: JSON.stringify(settings),
  })

  if (!res.ok) {
    let msg = 'No se pudo actualizar la privacidad.'
    try {
      const err = await res.json()
      if (err.message) msg = err.message
    } catch {
      // Ignorar
    }
    throw new Error(msg)
  }

  return res.json()
}
