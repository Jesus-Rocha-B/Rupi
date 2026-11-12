/**
 * Servicio de integración con la IA de RUPI (FastAPI / Gemini Flash)
 */

export interface GeneratedStop {
  sequenceNumber: number
  title: string
  description: string
  activityType: 'LECCION' | 'RETO' | 'CUESTIONARIO' | 'TARJETAS_MEMORIA' | 'INFOGRAFIA' | string
  sourceDocuments?: string[]
  challengeQuestion: string
  options?: string[]
  correctAnswer?: string
  explanation?: string
}

export interface GeneratedRoadmap {
  title: string
  description: string
  gradeNumber: number
  gradeName: string
  subjectCode: string
  subjectName: string
  summarySource?: string
  stops: GeneratedStop[]
}

export interface ChatContext {
  roadmapTitle?: string
  roadmapDescription?: string
  stopsSummary?: string
  currentGrade?: string
  currentSubject?: string
  stopTitle?: string
  lessonContent?: string
  /** HU-15: modo que el docente eligió para el aula. */
  assistanceMode?: TutorAssistanceMode
}

export type TutorAssistanceMode = 'PISTAS_GUIADAS' | 'EXPLICACION_DIRECTA'
export interface TutorSupport { mode: TutorAssistanceMode; classroomName: string | null; source: 'AULA' | 'PREDETERMINADO' }

/** HU-15: modo de ayuda del tutor para el aula de esta ruta. Ante cualquier fallo se guía con pistas. */
export async function fetchTutorSupport(routeId: string, signal?: AbortSignal): Promise<TutorSupport> {
  const fallback: TutorSupport = { mode: 'PISTAS_GUIADAS', classroomName: null, source: 'PREDETERMINADO' }
  try {
    const res = await fetch(`/api/v1/student/tutor-support?routeId=${encodeURIComponent(routeId)}`, {
      credentials: 'same-origin',
      signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(10000)]) : AbortSignal.timeout(10000),
    })
    if (!res.ok) return fallback
    const data = (await res.json()) as TutorSupport
    return data.mode === 'EXPLICACION_DIRECTA' || data.mode === 'PISTAS_GUIADAS' ? data : fallback
  } catch (error) {
    if (signal?.aborted) throw error
    return fallback
  }
}

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export async function generateRoadmapFromFile(
  fileOrFiles: File | File[],
  gradeNumber?: number,
  subjectCode?: string
): Promise<GeneratedRoadmap> {
  const formData = new FormData()
  const fileList = Array.isArray(fileOrFiles) ? fileOrFiles : [fileOrFiles]
  
  fileList.forEach((f) => {
    formData.append('files', f)
  })
  if (fileList.length > 0) {
    formData.append('file', fileList[0])
  }
  
  if (gradeNumber !== undefined && gradeNumber !== null) {
    formData.append('gradeNumber', String(gradeNumber))
  }
  if (subjectCode) {
    formData.append('subjectCode', subjectCode)
  }

  const res = await fetch('/api/v1/ia/roadmap/generate-from-file', {
    method: 'POST',
    body: formData,
  })

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({ detail: 'Error en el servidor de IA' }))
    throw new Error(errorData.detail || 'Error al procesar el archivo')
  }

  const data = await res.json()
  return data.roadmap
}

export async function generateRoadmapFromText(
  text: string,
  gradeNumber?: number,
  subjectCode?: string
): Promise<GeneratedRoadmap> {
  const res = await fetch('/api/v1/ia/roadmap/generate-from-text', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text, gradeNumber, subjectCode }),
  })

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({ detail: 'Error en el servidor de IA' }))
    throw new Error(errorData.detail || 'Error al generar la ruta')
  }

  const data = await res.json()
  return data.roadmap
}

export async function askRupiTutor(
  message: string,
  context?: ChatContext,
  history?: ChatMessage[]
): Promise<string> {
  const res = await fetch('/api/v1/ia/tutor/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, context, history }),
  })

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({ detail: 'Error al consultar a Rupi' }))
    throw new Error(errorData.detail || 'Error de comunicación con Rupi')
  }

  const data = await res.json()
  return data.reply
}
