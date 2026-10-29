import { fileNameFromDisposition, sha256Hex, unitPackagePath } from '../features/roadmap/unitDownloads'

export interface Grade {
  number: number
  name: string
}

export interface Area {
  code: string
  name: string
}

export interface Position {
  x: number
  y: number
}

export type NodeState = 'BLOQUEADO' | 'DISPONIBLE' | 'EN_CURSO' | 'COMPLETADO'

export interface RouteNode {
  id: string
  sequence: number
  title: string
  activityType: string
  estimatedMinutes: number | null
  state: NodeState
  optional: boolean
  position: Position | null
}

export interface RouteSummary {
  lastVisitedAt: string | null
  versionRouteId: string
  title: string
  grade: Grade
  area: Area
  enrollmentState: string
  completedNodes: number
  totalNodes: number
  category?: 'OFICIAL' | 'COMUNIDAD'
  authorName?: string
}

export interface CulturalFact {
  id: string
  nodeSequence: number | null
  order: number
  title: string
  content: string
  icon: string
  source: string
}

export interface CulturalContext {
  id: string
  code: string
  city: string
  place: string
  title: string
  description: string | null
  motivationalMessage: string
  imageUrl: string
  imageAlt: string
  author: string
  source: string
  license: string
  licenseUrl: string
  facts: CulturalFact[]
}

export interface Competency {
  code: string
  name: string
}

export interface RouteUnit {
  id: string | null
  code: string | null
  title: string
  sequence: number | null
  totalNodes?: number
  completedNodes?: number
  competencies: Competency[]
  nodes?: RouteNode[]
  nodeIds?: string[]
}

export type CurriculumStatus = 'COMPLETA' | 'PARCIAL' | 'SIN_UNIDADES' | 'NO_VIGENTE' | 'INCOHERENTE'

export interface Curriculum {
  status: CurriculumStatus
  message: string | null
  units: RouteUnit[]
}

export interface RouteDetail {
  versionRouteId: string
  code?: string
  title: string
  grade: Grade
  area: Area
  enrollment: {
    id: string
    state: string
    lastVisitedNodeId: string | null
  }
  progress: {
    completedNodes: number
    totalNodes: number
  }
  nodes: RouteNode[]
  culturalContext?: CulturalContext | null
  curriculum?: Curriculum | null
}

export interface ActivityBlock {
  id: string
  type: string
  text: string | null
  url: string | null
  accessibleText: string | null
}

export interface OptionDto {
  id: string
  sequence: number
  text: string
}

export interface QuestionDto {
  id: string
  sequence: number
  type: string
  statement: string
  explanation: string
  points: number
  options: OptionDto[]
}

export interface SubmitAnswerRequest {
  questionId: string
  selectedOptionId?: string | null
  textAnswer?: string | null
}

export interface AnswerFeedbackResponse {
  questionId: string
  isCorrect: boolean
  feedbackMessage: string
  correctOptionId: string | null
  explanation: string
}

export interface EvaluationSummaryDto {
  totalQuestions: number
  correctAnswers: number
  totalScore: number
  maxScore: number
  rupiFeedback: string
}

export interface BadgeDto {
  code: string
  name: string
  description: string
  icon: string
}

export interface ActivityContent {
  nodeId: string
  activityId?: string
  title: string
  instructions: string | null
  state: string
  blocks: ActivityBlock[]
  questions?: QuestionDto[]
}

export interface NextUnlockedNode {
  id: string
  sequence: number
  title: string
}

export interface ActivityCompleteResult {
  nodeId: string
  state: string
  experienceEarned: number
  totalExperience: number
  nextUnlockedNode: NextUnlockedNode | null
  rupiMessage: string
  badgeEarned?: BadgeDto | null
  isRouteCompleted?: boolean
  routeBonusExperience?: number
}

export interface RouteDiplomaResponse {
  versionRouteId: string
  routeTitle: string
  studentName: string
  schoolName: string
  gradeNumber: number
  gradeName: string
  areaName: string
  totalCompletedNodes: number
  totalNodes: number
  totalExperienceEarned: number
  completedAt: string
  graduationBadge: BadgeDto
  competencies: Competency[]
  rupiHonorMessage: string
}

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
    this.name = 'ApiError'
  }
}

const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

export interface Student {
  id: string
  name: string
  schoolName?: string
  institutionName?: string
  role?: 'ESTUDIANTE' | 'DOCENTE'
  gradeNumber?: number
  gradeName?: string
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE}/api/v1/${path}`, {
    credentials: 'same-origin',
    ...options,
    signal: options.signal
      ? AbortSignal.any([options.signal, AbortSignal.timeout(15000)])
      : AbortSignal.timeout(15000),
  })
  if (!response.ok) throw await apiErrorFrom(response)
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

async function apiErrorFrom(response: Response): Promise<ApiError> {
  let msg = `No se pudo consultar la información (${response.status})`
  try {
    const errData = await response.json()
    if (errData?.message) msg = errData.message
    else if (errData?.detail) msg = errData.detail
    else if (errData?.error) msg = errData.error
    else if (errData?.title) msg = errData.title
  } catch {
    // Ignora error de parseo si no es JSON
  }
  return new ApiError(response.status, msg)
}

export interface UnitPackage {
  blob: Blob
  fileName: string
  /** true si el SHA-256 del archivo recibido coincide con el que anunció el servidor; null si no se pudo comprobar. */
  verified: boolean | null
}

export async function downloadUnitPackage(
  versionRouteId: string,
  unitKey: string,
  fallbackName: string,
  signal?: AbortSignal,
  nodePackage = false
): Promise<UnitPackage> {
  const path = nodePackage ? `student/learning-routes/${encodeURIComponent(versionRouteId)}/nodes/${encodeURIComponent(unitKey)}/package` : unitPackagePath(versionRouteId, unitKey)
  return downloadStudyPackage(path, fallbackName, signal)
}
export async function downloadStudyPackage(path: string, fallbackName: string, signal?: AbortSignal): Promise<UnitPackage> {
  const timeout = AbortSignal.timeout(60000)
  const response = await fetch(`${API_BASE}/api/v1/${path}`, {
    credentials: 'same-origin',
    signal: signal ? AbortSignal.any([signal, timeout]) : timeout,
  })
  if (!response.ok) throw await apiErrorFrom(response)
  const data = await response.arrayBuffer()
  const expected = response.headers.get('X-Rupi-Paquete-Sha256')
  const actual = expected ? await sha256Hex(data) : null
  return {
    blob: new Blob([data], { type: 'application/zip' }),
    fileName: fileNameFromDisposition(response.headers.get('Content-Disposition'), fallbackName),
    verified: expected && actual ? expected.toLowerCase() === actual : null,
  }
}

export const fetchSession = (signal?: AbortSignal) => request<Student>('student/session', { signal })
export const fetchSessionOptions = (signal?: AbortSignal) => request<{ demoAvailable: boolean }>('auth/options', { signal })
export const login = (username: string, password: string) =>
  request<Student>('auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
export const loginDemo = () => request<Student>('auth/demo', { method: 'POST' })
export const logout = () => request<void>('auth/logout', { method: 'POST' })
export async function fetchStudentRoutes(signal?: AbortSignal): Promise<RouteSummary[]> {
  return (await request<{ items: RouteSummary[] }>('student/learning-routes', { signal })).items
}
export const fetchRouteDetail = (id: string, signal?: AbortSignal) =>
  request<RouteDetail>(`student/learning-routes/${encodeURIComponent(id)}`, { signal })

export const startActivity = (routeId: string, nodeId: string, signal?: AbortSignal) =>
  request<ActivityContent>(
    `student/learning-routes/${encodeURIComponent(routeId)}/nodes/${encodeURIComponent(nodeId)}/start`,
    { method: 'POST', signal }
  )

export const completeActivity = (routeId: string, nodeId: string, signal?: AbortSignal) =>
  request<ActivityCompleteResult>(
    `student/learning-routes/${encodeURIComponent(routeId)}/nodes/${encodeURIComponent(nodeId)}/complete`,
    { method: 'POST', signal }
  )

export const submitQuestionAnswer = (
  activityId: string,
  data: SubmitAnswerRequest,
  signal?: AbortSignal
) =>
  request<AnswerFeedbackResponse>(
    `student/activities/${encodeURIComponent(activityId)}/questions/answer`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
      signal,
    }
  )

export const fetchEvaluationSummary = (activityId: string, signal?: AbortSignal) =>
  request<EvaluationSummaryDto>(
    `student/activities/${encodeURIComponent(activityId)}/summary`,
    { signal }
  )

export const fetchStudentBadges = (signal?: AbortSignal) =>
  request<BadgeDto[]>('student/badges', { signal })

/** Historia 29: XP real del libro de movimientos (incluye retos, bonos y ajustes). */
export const fetchStudentExperience = (signal?: AbortSignal) =>
  request<{ totalXp: number; badges: number }>('student/experience', { signal })

export const fetchRouteDiploma = (routeId: string, signal?: AbortSignal) =>
  request<RouteDiplomaResponse>(
    `student/learning-routes/${encodeURIComponent(routeId)}/diploma`,
    { signal }
  )

export interface RouteSearchResult {
  versionRouteId: string
  routeTitle: string
  description: string | null
  gradeNumber: number
  gradeName: string
  areaCode: string
  areaName: string
  totalNodes: number
  isEnrolled: boolean
  enrollmentState: string
  category?: 'OFICIAL' | 'COMUNIDAD'
  authorName?: string
}

export interface SearchAutocompleteResponse {
  query: string
  suggestedSubjects: string[]
  routes: RouteSearchResult[]
  selectedGrades?: number[]
  selectedAreas?: string[]
  totalResults?: number
}

export interface SearchFilters {
  query?: string
  gradeNumber?: number | null
  grades?: number[]
  area?: string | null
  areas?: string[]
  limit?: number
}

export const searchAlternativeRoutes = (
  filterOrQuery: string | SearchFilters,
  gradeNumber?: number | null,
  limit: number = 20,
  signal?: AbortSignal
) => {
  const params = new URLSearchParams()
  if (typeof filterOrQuery === 'string') {
    if (filterOrQuery) params.set('q', filterOrQuery)
    if (gradeNumber !== undefined && gradeNumber !== null) params.set('grade', gradeNumber.toString())
    if (limit) params.set('limit', limit.toString())
  } else {
    if (filterOrQuery.query) params.set('q', filterOrQuery.query)
    if (filterOrQuery.gradeNumber !== undefined && filterOrQuery.gradeNumber !== null) {
      params.set('grade', filterOrQuery.gradeNumber.toString())
    }
    if (filterOrQuery.grades && filterOrQuery.grades.length > 0) {
      filterOrQuery.grades.forEach((g) => params.append('grades', g.toString()))
    }
    if (filterOrQuery.area) params.set('area', filterOrQuery.area)
    if (filterOrQuery.areas && filterOrQuery.areas.length > 0) {
      filterOrQuery.areas.forEach((a) => params.append('areas', a))
    }
    if (filterOrQuery.limit) params.set('limit', filterOrQuery.limit.toString())
  }
  const qs = params.toString() ? `?${params.toString()}` : ''
  return request<SearchAutocompleteResponse>(`student/learning-routes/search${qs}`, { signal })
}

export const fetchSubjectSuggestions = (
  query: string,
  limit: number = 6,
  signal?: AbortSignal
) => {
  const params = new URLSearchParams()
  if (query) params.set('q', query)
  if (limit) params.set('limit', limit.toString())
  const qs = params.toString() ? `?${params.toString()}` : ''
  return request<string[]>(`student/learning-routes/suggestions${qs}`, { signal })
}

export const enrollInRoute = (versionRouteId: string, signal?: AbortSignal) =>
  request<RouteDetail>(`student/learning-routes/${versionRouteId}/enroll`, {
    method: 'POST',
    signal,
  })

export interface JoinByCodeResult {
  versionRouteId: string
  routeTitle: string
  grade: Grade
  area: Area
  alreadyEnrolled: boolean
  message: string
}

export const joinRouteByCode = (code: string, signal?: AbortSignal) =>
  request<JoinByCodeResult>('student/learning-routes/join-by-code', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code: code.trim().toUpperCase() }),
    signal,
  })


/** Referencias por grado y materia de una ruta autorizada. Los libros se descargan desde el MINEDU. */
export interface StudyResource {
  grade: number
  areaCode: string
  title: string
  publisher: string
  year: number
  sourceUrl: string
  checkedOn: string
  note: string
  exercises: { prompt: string; answer: string }[]
}

export const getStudyResources = (versionRouteId: string, signal?: AbortSignal) =>
  request<StudyResource[]>(`student/learning-routes/${encodeURIComponent(versionRouteId)}/study-resources`, { signal })
