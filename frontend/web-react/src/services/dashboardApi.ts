/**
 * dashboardApi.ts
 * Cliente API para el panel principal y resumen métrico del estudiante (HU-33).
 *
 * Incluye sincronización rápida de datos con soporte para caché local
 * y revalidación de cabeceras ETag (HTTP 304).
 */

const BASE_URL = '/api/v1'
const CACHE_KEY = 'rupi_student_dashboard_summary'

export interface StudentOverview {
  studentId: string
  studentName: string
  schoolName: string
  role: string
  gradeNumber: number
  gradeName: string
  equippedAccessories: string[]
}

export interface MetricSummary {
  totalCompletedLessons: number
  totalPendingLessons: number
  totalEnrolledLessons: number
  overallCompletionPercentage: number
  lessonsCompletedToday: number
  currentStreakDays: number
  totalXp: number
  activeRoutesCount: number
}

export interface SubjectProgress {
  versionRouteId: string
  routeTitle: string
  areaCode: string
  areaName: string
  gradeNumber: number
  completedLessons: number
  pendingLessons: number
  totalLessons: number
  completionPercentage: number
  lastVisitedAt: string | null
}

export interface NextLessonRecommendation {
  versionRouteId: string
  routeTitle: string
  areaCode: string
  areaName: string
  nodeId: string
  nodeTitle: string
  sequence: number
  activityType: string
  estimatedMinutes: number | null
  nodeState: string
}

export interface SyncMeta {
  syncedAt: string
  dataHash: string
}

export interface DashboardSummaryResponse {
  student: StudentOverview
  metrics: MetricSummary
  subjects: SubjectProgress[]
  nextLesson: NextLessonRecommendation | null
  sync: SyncMeta
}

/**
 * Obtiene el resumen almacenado en la caché local del navegador para carga instantánea.
 */
export function getCachedDashboardSummary(): DashboardSummaryResponse | null {
  try {
    const raw = localStorage.getItem(CACHE_KEY)
    if (!raw) return null
    return JSON.parse(raw) as DashboardSummaryResponse
  } catch {
    return null
  }
}

/**
 * Guarda el resumen en la caché local para acelerar la sincronización en dispositivos móviles.
 */
function cacheDashboardSummary(data: DashboardSummaryResponse) {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify(data))
  } catch {
    // Si el almacenamiento está restringido, se ignora silenciosamente.
  }
}

/**
 * Consulta el endpoint de resumen del panel principal con soporte para ETag (HTTP 304).
 * Si los datos no han cambiado en el servidor, devuelve la versión en caché instantáneamente.
 */
export async function fetchDashboardSummary(
  options: { forceRefresh?: boolean; signal?: AbortSignal } = {}
): Promise<{ data: DashboardSummaryResponse; fromCache: boolean }> {
  const cached = getCachedDashboardSummary()
  const headers: Record<string, string> = {
    Accept: 'application/json',
  }

  if (cached?.sync?.dataHash && !options.forceRefresh) {
    headers['If-None-Match'] = `"${cached.sync.dataHash}"`
  }

  try {
    const res = await fetch(`${BASE_URL}/student/dashboard/summary`, {
      method: 'GET',
      headers,
      credentials: 'same-origin',
      signal: options.signal,
    })

    if (res.status === 304 && cached) {
      // Datos no modificados en servidor: actualizamos timestamp y devolvemos la caché
      const refreshed: DashboardSummaryResponse = {
        ...cached,
        sync: {
          ...cached.sync,
          syncedAt: new Date().toISOString(),
        },
      }
      cacheDashboardSummary(refreshed)
      return { data: refreshed, fromCache: true }
    }

    if (!res.ok) {
      if (cached) {
        return { data: cached, fromCache: true }
      }
      throw new Error(`Error ${res.status}: No se pudo cargar el resumen del panel`)
    }

    const fresh = (await res.json()) as DashboardSummaryResponse
    cacheDashboardSummary(fresh)
    return { data: fresh, fromCache: false }
  } catch (err: unknown) {
    if (cached) {
      return { data: cached, fromCache: true }
    }
    throw err
  }
}
