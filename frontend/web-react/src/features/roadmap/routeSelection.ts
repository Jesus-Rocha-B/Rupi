import type { RouteSummary } from '../../services/learningRoutesApi'

<<<<<<< HEAD
export function areaKey(code: string) {
  const key = code.toUpperCase()
  if (['MAT', 'MATEMATICA'].includes(key)) return 'MATEMATICA'
  if (['COM', 'COMUNICACION'].includes(key)) return 'COMUNICACION'
  if (['CYT', 'CT', 'CIENCIA_TECNOLOGIA'].includes(key)) return 'CYT'
  return key
}
export function matchingRoutes(routes: RouteSummary[], grade: number | null, course: string | null) {
  return routes.filter(route => route.grade.number === grade && areaKey(route.area.code) === areaKey(course ?? ''))
=======
export function areaKey(code: string | null | undefined): string {
  if (!code) return ''
  const upper = code.toUpperCase()
  if (upper === 'MAT' || upper === 'MATEMATICA') return 'MATEMATICA'
  if (upper === 'COM' || upper === 'COMUNICACION') return 'COMUNICACION'
  if (upper === 'CYT' || upper === 'CIENCIA') return 'CYT'
  return upper
}

export function matchingRoutes(
  routes: RouteSummary[],
  gradeNumber: number,
  courseCode: string
): RouteSummary[] {
  const targetArea = areaKey(courseCode)
  return routes.filter(
    route => route.grade.number === gradeNumber && areaKey(route.area.code) === targetArea
  )
>>>>>>> 656170f (Se implementó el flujo completo de inicio y finalización de lecciones escolares con otorgamiento idempotente de 50 puntos de experiencia y desbloqueo automático de la siguiente parada para cumplir con HU-02 y HU-05, además se configuró el desplazamiento suave y centrado accesible del mapa en el último nodo visitado junto con el acceso directo desde el botón de bienvenida para cumplir con HU-03, también se corrigieron las discrepancias de columnas de catálogo en el repositorio de Spring Boot para enlazar las unidades y competencias curriculares oficiales del MINEDU con sus respectivas bandas visuales en el roadmap para cumplir con HU-04, asimismo se diseñó el modal didáctico de actividades con estados de carga interactivos, contenido explicativo de respaldo y pantalla de celebración con Rupi festejando y audio de felicitación, y finalmente se agregaron las pruebas unitarias en JUnit, el script de integración para PowerShell y la documentación técnica detallada de las cuatro historias en la carpeta de implementación)
}
