import type { RouteSummary } from '../../services/learningRoutesApi'

export function areaKey(code: string) {
  const key = code.toUpperCase()
  if (['MAT', 'MATEMATICA'].includes(key)) return 'MATEMATICA'
  if (['COM', 'COMUNICACION'].includes(key)) return 'COMUNICACION'
  if (['CYT', 'CT', 'CIENCIA_TECNOLOGIA'].includes(key)) return 'CYT'
  return key
}
export function matchingRoutes(routes: RouteSummary[], grade: number | null, course: string | null) {
  return routes.filter(route => route.grade.number === grade && areaKey(route.area.code) === areaKey(course ?? ''))
}
