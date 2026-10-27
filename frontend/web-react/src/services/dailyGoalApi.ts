/**
 * dailyGoalApi.ts
 * Cliente API para el indicador gráfico y meta de progreso diario del estudiante (HU-34).
 */

const BASE_URL = '/api/v1'

export interface DayProgress {
  date: string
  targetLessons: number
  completedLessons: number
  pendingLessons: number
  percentage: number
  isGoalMet: boolean
  metAt: string | null
  motivationalMessage: string
}

export interface DayHistory {
  date: string
  dayOfWeek: string
  targetLessons: number
  completedLessons: number
  percentage: number
  isGoalMet: boolean
  isToday: boolean
}

export interface DailyGoalOverview {
  today: DayProgress
  weeklyHistory: DayHistory[]
  currentDailyTargetSetting: number
  availableTargetOptions: number[]
  currentStreakDays: number
}

export async function fetchDailyGoalOverview(signal?: AbortSignal): Promise<DailyGoalOverview> {
  const res = await fetch(`${BASE_URL}/student/daily-goal`, {
    method: 'GET',
    headers: {
      Accept: 'application/json',
    },
    credentials: 'same-origin',
    signal,
  })

  if (!res.ok) {
    throw new Error(`Error ${res.status}: No se pudo cargar el progreso diario`)
  }

  return (await res.json()) as DailyGoalOverview
}

export async function updateDailyGoalTarget(
  target: number,
  signal?: AbortSignal
): Promise<DailyGoalOverview> {
  const res = await fetch(`${BASE_URL}/student/daily-goal/target?target=${target}`, {
    method: 'PUT',
    headers: {
      Accept: 'application/json',
    },
    credentials: 'same-origin',
    signal,
  })

  if (!res.ok) {
    throw new Error(`Error ${res.status}: No se pudo actualizar la meta diaria`)
  }

  return (await res.json()) as DailyGoalOverview
}
