import { ApiError } from './learningRoutesApi'
export interface ReportClassroom { id: string; name: string; grade: number }
export interface ReportRoute { id: string; title: string; version: number; state: string }
export interface StudentResult {
  studentId: string; studentName: string; enrollmentState: string | null
  completedNodes: number; totalNodes: number; answered: number; totalQuestions: number
  correct: number; accuracy: number | null
  status: 'SIN_INSCRIPCION' | 'SIN_RESPUESTAS' | 'EVIDENCIA_INSUFICIENTE' | 'REQUIERE_ATENCION' | 'EN_AVANCE'
}
export interface ResultsReport {
  classroom: ReportClassroom; route: ReportRoute; generatedAt: string; studentCount: number
  units: { id: string; title: string; attentionCount: number
    competencies: { code: string; name: string }[]; students: StudentResult[] }[]
}
export const REPORTS_BASE = '/api/v1/teacher/results-reports'
export async function reportsResponse(path: string, signal?: AbortSignal) {
  const response = await fetch(REPORTS_BASE + path, { credentials: 'same-origin',
    signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(20000)]) : AbortSignal.timeout(20000) })
  if (!response.ok) throw new ApiError(response.status, 'No pudimos consultar los resultados. Verifica tu conexión y el acceso al aula.')
  return response
}
export const reportQuery = (room: string, route: string, student?: string) => {
  const params = new URLSearchParams({ classroomId: room, routeId: route })
  if (student) params.set('studentId', student)
  return `?${params}`
}
export interface ReportMember { id: string; name: string; schoolUsername: string }
export interface StudentOverview {
  student: ReportMember; classroom: ReportClassroom; route: ReportRoute; enrollmentState: string | null
  completedNodes: number; totalNodes: number; answered: number; totalQuestions: number; correct: number
  units: { id: string; title: string; status: StudentResult['status']; average: { value: number; letter: string } | null }[]
  activities: { id: string; title: string; answered: number; correct: number }[]
  reinforcements: { id: string; nodeTitle: string; title: string; url: string | null; linkedAt: string; completedAt: string | null }[]
  roadmap: RoadmapStop[]
  overallAverage: { value: number; letter: string } | null
}
export interface RoadmapStop {
  id: string; sequence: number; title: string; unitId: string | null; unitTitle: string; optional: boolean
  /** null cuando el estudiante no está inscrito en la ruta. */
  state: 'BLOQUEADO' | 'DISPONIBLE' | 'EN_CURSO' | 'COMPLETADO' | null
  completedAt: string | null
  grade: { value: number; letter: string } | null
  gradedAt: string | null
}
