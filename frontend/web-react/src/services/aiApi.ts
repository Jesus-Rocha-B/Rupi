/**
 * Servicio de integración con la IA de RUPI (FastAPI / Gemini Flash)
 */

export interface GeneratedStop {
  sequenceNumber: number
  title: string
  description: string
  activityType: 'LECTURA' | 'EJERCICIO_PRACTICO' | 'RETO_FORMATIVO'
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
  stopTitle?: string
  lessonContent?: string
}

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export async function generateRoadmapFromFile(
  file: File,
  gradeNumber?: number,
  subjectCode?: string
): Promise<GeneratedRoadmap> {
  const formData = new FormData()
  formData.append('file', file)
  if (gradeNumber) formData.append('gradeNumber', String(gradeNumber))
  if (subjectCode) formData.append('subjectCode', subjectCode)

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
