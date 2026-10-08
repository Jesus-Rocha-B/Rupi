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
  lastVisitedAt?: string | null
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
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE}/api/v1/${path}`, {
    credentials: 'same-origin',
    ...options,
    signal: options.signal
      ? AbortSignal.any([options.signal, AbortSignal.timeout(15000)])
      : AbortSignal.timeout(15000),
  })
  if (!response.ok) throw new ApiError(response.status, `No se pudo consultar la información (${response.status})`)
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
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
