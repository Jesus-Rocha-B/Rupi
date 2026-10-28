import { RouteCalendar } from '../calendar/RouteCalendar'

import { useState, useEffect, useRef } from 'react'

import {

  Compass,

  Plus,

  BookOpen,

  Send,

  Sparkles,

  ArrowLeft,
  ArrowRight,
  ExternalLink,

  GraduationCap,

  FileText,

  MapPin,

  Clock,

  CheckCircle,

  Eye,

  LogOut,

  RefreshCw,

  GripVertical,

  ChevronUp,

  ChevronDown,

  Check,

  RotateCcw,

  ArrowUpDown,

  PauseCircle,

  Video,

  HelpCircle,

  Trash2,

  Upload,

  Calendar,

  AlertCircle,

  CheckCircle2,

  Play,

  Paperclip,

  KeyRound,

  Copy,

  Palette,

} from 'lucide-react'

import { useMapBackground } from '../../utils/mapBackgrounds'

import { MapBackgroundModal } from '../../components/MapBackgroundModal'

import {

  fetchTeacherRoutes,

  fetchTeacherRouteDetail,

  createTeacherRoute,

  createTeacherNode,

  attachResourceToNode,

  publishTeacherRoute,

  pauseTeacherRoute,

  reorderTeacherNodes,

  uploadTeacherMaterial,

  fetchTeacherNodeDetail,

  updateTeacherNode,

  deleteTeacherNode,

  deleteTeacherRoute,

  type TeacherRouteSummary,

  type TeacherRouteDetail,

  type TeacherNode,

  type AttachResourcePayload,
  type UploadMaterialResult,

  type TeacherQuestion,

  type UpdateNodePayload,

} from '../../services/teacherApi'

import { StudentAvatar } from '../../components/StudentAvatar'

import { MaterialUploadZone } from '../../components/MaterialUploadZone'

import { generateRoadmapFromFile } from '../../services/aiApi'

import { resolveEducationalVideo } from '../../utils/educationalVideos'

import './TeacherAuthoring.css'



interface Props {

  teacherName: string

  schoolName: string

  onLogout: () => void

  onSwitchToStudentView: (targetRouteId?: string, gradeNum?: number, areaCode?: string) => void

  /** Abre los reportes de una versión de ruta; el grado permite elegir el aula que corresponde. */

  onOpenReports: (versionRouteId?: string, gradeNum?: number) => void

}



const CNEB_AREAS = [
  { code: 'MAT', name: 'Matemática' },
  { code: 'COM', name: 'Comunicación' },
  { code: 'CYT', name: 'Ciencia y Tecnología' },
  { code: 'SOC', name: 'Personal Social' },
  { code: 'ART', name: 'Arte y Cultura' },
  { code: 'REL', name: 'Educación Religiosa' },
  { code: 'EFI', name: 'Educación Física' },
  { code: 'ING', name: 'Inglés como Lengua Extranjera' },
  { code: 'TOE', name: 'Tutoría y Orientación Educativa' },
  { code: 'TIC', name: 'Tecnologías Digitales y Computación' },
]

const CNEB_GRADES = [
  { value: 0, label: 'Libre / Multigrado' },
  { value: 1, label: '1.° de primaria' },
  { value: 2, label: '2.° de primaria' },
  { value: 3, label: '3.° de primaria' },
  { value: 4, label: '4.° de primaria' },
  { value: 5, label: '5.° de primaria' },
  { value: 6, label: '6.° de primaria' },
]



function stateLabel(state: string) {

  if (state === 'PUBLICADO') return '✓ Publicado'

  if (state === 'PAUSADO') return '❚❚ En pausa'

  return '✎ Borrador'

}



function sanitizeActivityType(raw?: string): string {
  if (!raw) return 'LECCION'
  const t = raw.trim().toUpperCase()
  if (t === 'RETO' || t.includes('RETO') || t.includes('FORMATIVO')) return 'RETO'
  if (t === 'CUESTIONARIO' || t.includes('CUEST') || t.includes('PRACTICO') || t.includes('QUIZ')) return 'CUESTIONARIO'
  if (t.includes('TARJETA') || t.includes('MEMORIA')) return 'TARJETAS_MEMORIA'
  if (t.includes('INFO') || t.includes('GRAFIA')) return 'INFOGRAFIA'
  return 'LECCION'
}

function toDatetimeLocal(iso: string | null | undefined): string {

  if (!iso) return ''

  try {

    const d = new Date(iso)

    if (isNaN(d.getTime())) return ''

    const pad = (n: number) => n.toString().padStart(2, '0')

    const yyyy = d.getFullYear()

    const mm = pad(d.getMonth() + 1)

    const dd = pad(d.getDate())

    const hh = pad(d.getHours())

    const mi = pad(d.getMinutes())

    return `${yyyy}-${mm}-${dd}T${hh}:${mi}`

  } catch {

    return ''

  }

}



export function TeacherAuthoringView({ teacherName, schoolName, onLogout, onSwitchToStudentView, onOpenReports }: Props) {

  const [routes, setRoutes] = useState<TeacherRouteSummary[]>([])

  const [loading, setLoading] = useState(true)

  const [selectedRouteId, setSelectedRouteId] = useState<string | null>(null)

  const [activeRouteDetail, setActiveRouteDetail] = useState<TeacherRouteDetail | null>(null)

  const [selectedNode, setSelectedNode] = useState<TeacherNode | null>(null)



  // Modales

  const [showCreateModal, setShowCreateModal] = useState(false)

  const [showAddNodeModal, setShowAddNodeModal] = useState(false)

  const [showBgModal, setShowBgModal] = useState(false)

  const [teacherMapBg, setTeacherMapBg] = useMapBackground(activeRouteDetail?.versionRouteId || activeRouteDetail?.routeId)



  // Formulario nueva ruta
  const [aiFiles, setAiFiles] = useState<File[]>([])
  const [aiGenerating, setAiGenerating] = useState(false)
  const [aiError, setAiError] = useState<string | null>(null)

  const [newTitle, setNewTitle] = useState('')
  const [newDescription, setNewDescription] = useState('')
  const [newGrade, setNewGrade] = useState<number>(0)
  const [newArea, setNewArea] = useState<string>('COM')
  const [savingRoute, setSavingRoute] = useState(false)

  // Formulario nueva parada
  const [newNodeTitle, setNewNodeTitle] = useState('')
  const [newNodeInstructions, setNewNodeInstructions] = useState('')
  const [newNodeMinutes, setNewNodeMinutes] = useState<number>(15)
  const [newNodeType, setNewNodeType] = useState<string>('LECCION')
  const [savingNode, setSavingNode] = useState(false)



  // Notificaciones principales

  const [banner, setBanner] = useState<{ type: 'success' | 'error'; message: string } | null>(null)



  // Estados de reorganización y Drag & Drop (HU-06)

  const [orderedNodes, setOrderedNodes] = useState<TeacherNode[]>([])

  const [isReorderDirty, setIsReorderDirty] = useState(false)

  const [savingReorder, setSavingReorder] = useState(false)

  const [draggedIndex, setDraggedIndex] = useState<number | null>(null)

  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null)



  // Estados de edición del nodo seleccionado

  type NodeTab = 'detalles' | 'video' | 'preguntas' | 'materiales'

  const [activeNodeTab, setActiveNodeTab] = useState<NodeTab>('detalles')

  const [loadingNodeDetail, setLoadingNodeDetail] = useState(false)

  const [editTitle, setEditTitle] = useState('')

  const [editInstructions, setEditInstructions] = useState('')

  const [editMinutes, setEditMinutes] = useState<number>(15)

  const [editActivityType, setEditActivityType] = useState<string>('LECCION')

  const [editNoDateLimit, setEditNoDateLimit] = useState(true)

  const [editAvailableFrom, setEditAvailableFrom] = useState('')

  const [editLockedFrom, setEditLockedFrom] = useState('')

  const [editVideoUrl, setEditVideoUrl] = useState('')

  const [editQuestions, setEditQuestions] = useState<TeacherQuestion[]>([])

  const [savingNodeChanges, setSavingNodeChanges] = useState(false)

  const [uploadingVideo, setUploadingVideo] = useState(false)

  const [nodeSaveBanner, setNodeSaveBanner] = useState<{ type: 'success' | 'error'; message: string } | null>(null)

  const videoInputRef = useRef<HTMLInputElement | null>(null)



  // Estados para confirmación de borrado (paradas y roadmaps)

  const [nodeToDelete, setNodeToDelete] = useState<TeacherNode | null>(null)

  const [deletingNode, setDeletingNode] = useState(false)

  const [routeToDelete, setRouteToDelete] = useState<{ routeId: string; title: string } | null>(null)

  const [deletingRoute, setDeletingRoute] = useState(false)



  // Copiado de código de clase al portapapeles

  const [copiedCode, setCopiedCode] = useState<string | null>(null)



  function handleCopyCode(code: string) {

    if (!code) return

    navigator.clipboard.writeText(code).then(() => {

      setCopiedCode(code)

      setTimeout(() => setCopiedCode(null), 2500)

    }).catch(() => {

      setCopiedCode(code)

      setTimeout(() => setCopiedCode(null), 2500)

    })

  }



  async function handleConfirmDeleteNode() {

    if (!selectedRouteId || !nodeToDelete) return

    setDeletingNode(true)

    try {

      const res = await deleteTeacherNode(selectedRouteId, nodeToDelete.id)

      setBanner({ type: 'success', message: res.message || 'Parada eliminada exitosamente del roadmap.' })

      setNodeToDelete(null)



      const detail = await fetchTeacherRouteDetail(selectedRouteId)

      setActiveRouteDetail(detail)

      setOrderedNodes(detail.nodes)

      setIsReorderDirty(false)

      if (selectedNode?.id === nodeToDelete.id) {

        setSelectedNode(detail.nodes[0] || null)

      }

      loadRoutes()

    } catch (err: unknown) {

      const msg = err instanceof Error ? err.message : 'Error al eliminar la parada didáctica.'

      setBanner({ type: 'error', message: msg })

    } finally {

      setDeletingNode(false)

    }

  }



  async function handleConfirmDeleteRoute() {

    if (!routeToDelete) return

    setDeletingRoute(true)

    try {

      const res = await deleteTeacherRoute(routeToDelete.routeId)

      setBanner({ type: 'success', message: res.message || 'Roadmap escolar eliminado exitosamente.' })

      setRouteToDelete(null)

      if (activeRouteDetail?.routeId === routeToDelete.routeId) {

        setActiveRouteDetail(null)

        setSelectedRouteId(null)

        setSelectedNode(null)

      }

      await loadRoutes()

    } catch (err: unknown) {

      const msg = err instanceof Error ? err.message : 'Error al eliminar el roadmap escolar.'

      setBanner({ type: 'error', message: msg })

    } finally {

      setDeletingRoute(false)

    }

  }



  useEffect(() => {

    loadRoutes()

  }, [])



  async function loadRoutes() {

    setLoading(true)

    try {

      const data = await fetchTeacherRoutes()

      setRoutes(data)

    } catch {

      setBanner({ type: 'error', message: 'No se pudieron cargar los roadmaps del docente.' })

    } finally {

      setLoading(false)

    }

  }



  async function handleOpenRouteDetail(routeId: string) {

    setSelectedRouteId(routeId)

    setSelectedNode(null)

    setBanner(null)

    try {

      const detail = await fetchTeacherRouteDetail(routeId)

      setActiveRouteDetail(detail)

      setOrderedNodes(detail.nodes)

      setIsReorderDirty(false)

      if (detail.nodes.length > 0) {

        setSelectedNode(detail.nodes[0])

      }

    } catch {

      setBanner({ type: 'error', message: 'No se pudo cargar el detalle del roadmap.' })

    }

  }



  useEffect(() => {

    if (selectedRouteId && selectedNode?.id) {

      loadSelectedNodeDetail(selectedRouteId, selectedNode.id)

    }

  }, [selectedRouteId, selectedNode?.id])



  async function loadSelectedNodeDetail(routeId: string, nodeId: string) {

    setLoadingNodeDetail(true)

    setNodeSaveBanner(null)

    try {

      const detail = await fetchTeacherNodeDetail(routeId, nodeId)

      setEditTitle(detail.title || '')

      setEditInstructions(detail.instructions || '')

      setEditActivityType(detail.activityType || 'LECCION')

      setEditMinutes(detail.estimatedMinutes || 15)

      setEditVideoUrl(detail.videoUrl || '')



      const hasLimit = Boolean(detail.availableFrom || detail.lockedFrom)

      setEditNoDateLimit(!hasLimit)

      setEditAvailableFrom(toDatetimeLocal(detail.availableFrom))

      setEditLockedFrom(toDatetimeLocal(detail.lockedFrom))



      const sanitizedQuestions = (detail.questions || []).map((q) => {
        if (!q.options || q.options.length === 0) return q
        const correctIndices = q.options
          .map((o, idx) => (o.isCorrect ? idx : -1))
          .filter((idx) => idx !== -1)

        if (correctIndices.length > 1) {
          const chosenCorrectIdx = correctIndices[correctIndices.length - 1]
          return {
            ...q,
            options: q.options.map((o, idx) => ({
              ...o,
              isCorrect: idx === chosenCorrectIdx,
            })),
          }
        }
        if (correctIndices.length === 0 && q.options.length > 0) {
          return {
            ...q,
            options: q.options.map((o, idx) => ({
              ...o,
              isCorrect: idx === 0,
            })),
          }
        }
        return q
      })

      setEditQuestions(sanitizedQuestions)

    } catch (err: unknown) {

      console.error('Error cargando detalle del nodo:', err)

      setNodeSaveBanner({ type: 'error', message: 'No se pudo cargar el detalle completo de la parada.' })

    } finally {

      setLoadingNodeDetail(false)

    }

  }



  function handleAddQuestion() {

    const newQ: TeacherQuestion = {

      sequence: editQuestions.length + 1,

      type: 'OPCION_UNICA',

      statement: '',

      explanation: '',

      points: 10,

      options: [

        { sequence: 1, text: '', isCorrect: true },

        { sequence: 2, text: '', isCorrect: false },

        { sequence: 3, text: '', isCorrect: false },

      ],

    }

    setEditQuestions((prev) => [...prev, newQ])

  }



  function handleRemoveQuestion(index: number) {

    setEditQuestions((prev) => prev.filter((_, i) => i !== index))

  }



  function handleQuestionStatementChange(index: number, statement: string) {

    setEditQuestions((prev) => {

      const copy = [...prev]

      copy[index] = { ...copy[index], statement }

      return copy

    })

  }



  function handleQuestionExplanationChange(index: number, explanation: string) {

    setEditQuestions((prev) => {

      const copy = [...prev]

      copy[index] = { ...copy[index], explanation }

      return copy

    })

  }



  function handleQuestionPointsChange(index: number, points: number) {

    setEditQuestions((prev) => {

      const copy = [...prev]

      copy[index] = { ...copy[index], points }

      return copy

    })

  }



  function handleSetCorrectOption(qIndex: number, optIndex: number) {

    setEditQuestions((prev) => {

      const copy = [...prev]

      const q = { ...copy[qIndex] }

      q.options = q.options.map((opt, i) => ({

        ...opt,

        isCorrect: i === optIndex,

      }))

      copy[qIndex] = q

      return copy

    })

  }



  function handleOptionTextChange(qIndex: number, optIndex: number, text: string) {

    setEditQuestions((prev) => {

      const copy = [...prev]

      const q = { ...copy[qIndex] }

      const opts = [...q.options]

      opts[optIndex] = { ...opts[optIndex], text }

      q.options = opts

      copy[qIndex] = q

      return copy

    })

  }



  function handleAddOption(qIndex: number) {

    setEditQuestions((prev) => {

      const copy = [...prev]

      const q = { ...copy[qIndex] }

      q.options = [

        ...q.options,

        { sequence: q.options.length + 1, text: '', isCorrect: false },

      ]

      copy[qIndex] = q

      return copy

    })

  }



  function handleRemoveOption(qIndex: number, optIndex: number) {

    setEditQuestions((prev) => {

      const copy = [...prev]

      const q = { ...copy[qIndex] }

      if (q.options.length <= 2) return prev

      const wasCorrect = q.options[optIndex].isCorrect

      const newOpts = q.options.filter((_, i) => i !== optIndex)

      if (wasCorrect && newOpts.length > 0) {

        newOpts[0].isCorrect = true

      }

      q.options = newOpts

      copy[qIndex] = q

      return copy

    })

  }



  function setQuickDateRange(days: number) {

    const now = new Date()

    const end = new Date(now.getTime() + days * 24 * 60 * 60 * 1000)

    setEditAvailableFrom(toDatetimeLocal(now.toISOString()))

    setEditLockedFrom(toDatetimeLocal(end.toISOString()))

    setEditNoDateLimit(false)

  }



  async function handleVideoFileUpload(e: React.ChangeEvent<HTMLInputElement>) {

    const file = e.target.files?.[0]

    if (!file) return

    if (!file.type.startsWith('video/')) {

      setNodeSaveBanner({

        type: 'error',

        message: 'Por favor selecciona un archivo de video válido (.mp4, .webm).',

      })

      return

    }



    setUploadingVideo(true)

    setNodeSaveBanner(null)

    try {

      const uploaded = await uploadTeacherMaterial(file)

      setEditVideoUrl(uploaded.downloadUrl)

      setNodeSaveBanner({

        type: 'success',

        message: `¡Video "${file.name}" cargado exitosamente! Recuerda guardar la parada para aplicar los cambios.`,

      })

    } catch (err: unknown) {

      const msg = err instanceof Error ? err.message : 'Error al subir el archivo de video.'

      setNodeSaveBanner({ type: 'error', message: msg })

    } finally {

      setUploadingVideo(false)

      if (e.target) e.target.value = ''

    }

  }



  async function handleSaveNodeChanges() {

    if (!selectedRouteId || !selectedNode) return

    if (!editTitle.trim()) {

      setNodeSaveBanner({ type: 'error', message: 'El título de la parada no puede estar vacío.' })

      setActiveNodeTab('detalles')

      return

    }



    for (let i = 0; i < editQuestions.length; i++) {

      const q = editQuestions[i]

      if (!q.statement.trim()) {

        setNodeSaveBanner({ type: 'error', message: `La pregunta #${i + 1} no tiene enunciado escrito.` })

        setActiveNodeTab('preguntas')

        return

      }

      const hasCorrect = q.options.some((o) => o.isCorrect)

      if (!hasCorrect) {

        setNodeSaveBanner({ type: 'error', message: `Marca cuál es la respuesta correcta en la pregunta #${i + 1}.` })

        setActiveNodeTab('preguntas')

        return

      }

      const emptyOpt = q.options.some((o) => !o.text.trim())

      if (emptyOpt) {

        setNodeSaveBanner({ type: 'error', message: `Hay opciones vacías en la pregunta #${i + 1}. Completa el texto de cada opción.` })

        setActiveNodeTab('preguntas')

        return

      }

    }



    setSavingNodeChanges(true)

    setNodeSaveBanner(null)



    try {

      const payload: UpdateNodePayload = {

        title: editTitle.trim(),

        instructions: editInstructions.trim() || undefined,

        activityType: editActivityType,

        estimatedMinutes: editMinutes,

        videoUrl: editVideoUrl.trim() || null,

        availableFrom: editNoDateLimit ? null : (editAvailableFrom ? new Date(editAvailableFrom).toISOString() : null),

        lockedFrom: editNoDateLimit ? null : (editLockedFrom ? new Date(editLockedFrom).toISOString() : null),

        questions: editQuestions.map((q) => {
          if (q.type === 'OPCION_UNICA' && q.options) {
            const correctIndices = q.options
              .map((o, idx) => (o.isCorrect ? idx : -1))
              .filter((idx) => idx !== -1)
            const chosenIdx = correctIndices.length > 0 ? correctIndices[correctIndices.length - 1] : 0
            return {
              ...q,
              options: q.options.map((opt, idx) => ({
                ...opt,
                isCorrect: idx === chosenIdx,
              })),
            }
          }
          return q
        }),

      }



      const res = await updateTeacherNode(selectedRouteId, selectedNode.id, payload)

      setNodeSaveBanner({ type: 'success', message: res.message || '¡Parada didáctica actualizada exitosamente!' })



      const detail = await fetchTeacherRouteDetail(selectedRouteId)

      setActiveRouteDetail(detail)

      setOrderedNodes(detail.nodes)

      const updated = detail.nodes.find((n) => n.id === selectedNode.id)

      if (updated) setSelectedNode(updated)

    } catch (err: unknown) {

      const msg = err instanceof Error ? err.message : 'Error al guardar los cambios de la parada.'

      setNodeSaveBanner({ type: 'error', message: msg })

    } finally {

      setSavingNodeChanges(false)

    }

  }



  async function handleCreateRoute(e: React.FormEvent) {
    e.preventDefault()
    setSavingRoute(true)
    setBanner(null)
    setAiError(null)

    const hasAiFiles = aiFiles.length > 0
    let finalTitle = newTitle.trim()
    let finalDescription = newDescription.trim() || undefined
    let finalGrade = newGrade
    let finalArea = newArea
    let stopsToCreate: {
      title: string
      description?: string
      activityType?: string
      sourceDocuments?: string[]
      challengeQuestion?: string
      options?: string[]
      correctAnswer?: string
      explanation?: string
    }[] = []

    try {
      if (hasAiFiles) {
        setAiGenerating(true)
        const generated = await generateRoadmapFromFile(aiFiles, newGrade, newArea)
        setAiGenerating(false)

        if (!finalTitle) {
          finalTitle = generated.title || 'Ruta Escolar Diseñada con Rupi IA'
        }
        if (!finalDescription && generated.description) {
          finalDescription = generated.description
        }
        if (generated.gradeNumber !== undefined && generated.gradeNumber !== null && newGrade === 0) {
          finalGrade = newGrade
        }
        if (generated.subjectCode && !newArea) {
          finalArea = generated.subjectCode
        }
        if (generated.stops && generated.stops.length > 0) {
          stopsToCreate = generated.stops.map((s) => ({
            title: s.title,
            description: s.description || s.challengeQuestion,
            activityType: s.activityType,
            sourceDocuments: s.sourceDocuments,
            challengeQuestion: s.challengeQuestion,
            options: s.options,
            correctAnswer: s.correctAnswer,
            explanation: s.explanation,
          }))
        }
      }

      if (!finalTitle) {
        setAiError('Por favor ingresa un título para el roadmap o sube documentos para que Rupi IA lo titule.')
        setSavingRoute(false)
        return
      }

      const res = await createTeacherRoute({
        title: finalTitle,
        description: finalDescription,
        gradeNumber: finalGrade,
        areaCode: finalArea,
      })

      // Subir los archivos base a Spring Boot para asociarlos como fuentes a las paradas
      const uploadedMaterialsMap = new Map<string, UploadMaterialResult>()
      if (hasAiFiles) {
        for (const file of aiFiles) {
          try {
            const uploaded = await uploadTeacherMaterial(file)
            uploadedMaterialsMap.set(file.name.toLowerCase().trim(), uploaded)
          } catch (uploadErr) {
            console.error('Error subiendo documento base de la ruta a Spring Boot:', uploadErr)
          }
        }
      }

      if (stopsToCreate.length > 0) {
        for (const stop of stopsToCreate) {
          try {
            const validActivityType = sanitizeActivityType(stop.activityType)
            const nodeRes = await createTeacherNode(res.routeId, {
              title: stop.title.trim() || 'Parada Didáctica',
              instructions: stop.description?.trim() || undefined,
              activityType: validActivityType,
              estimatedMinutes: 15,
            })

            // Asociar documentos fuente (fuentes de las que se deriva este nodo)
            if (nodeRes?.nodeId && uploadedMaterialsMap.size > 0) {
              let matchingMaterials: UploadMaterialResult[] = []
              if (stop.sourceDocuments && stop.sourceDocuments.length > 0) {
                for (const srcDoc of stop.sourceDocuments) {
                  const cleanSrc = srcDoc.toLowerCase().trim()
                  const found = Array.from(uploadedMaterialsMap.entries()).find(([name]) =>
                    name === cleanSrc || name.includes(cleanSrc) || cleanSrc.includes(name)
                  )
                  if (found && !matchingMaterials.some((m) => m.id === found[1].id)) {
                    matchingMaterials.push(found[1])
                  }
                }
              }
              if (matchingMaterials.length === 0) {
                // Si la parada sintetiza todas las fuentes o no hubo nombre específico, asociar todos los documentos cargados
                matchingMaterials = Array.from(uploadedMaterialsMap.values())
              }

              for (const mat of matchingMaterials) {
                try {
                  let resType: 'DOCUMENTO' | 'PRESENTACION' | 'IMAGEN' = 'DOCUMENTO'
                  const lower = mat.originalName.toLowerCase()
                  if (lower.endsWith('.ppt') || lower.endsWith('.pptx')) resType = 'PRESENTACION'
                  else if (lower.endsWith('.jpg') || lower.endsWith('.jpeg') || lower.endsWith('.png')) resType = 'IMAGEN'

                  await attachResourceToNode(res.routeId, nodeRes.nodeId, {
                    type: resType,
                    title: mat.originalName,
                    url: mat.downloadUrl,
                    multimediaId: mat.id,
                  })
                } catch (attachErr) {
                  console.error('Error asociando documento fuente a la parada:', attachErr)
                }
              }
            }

            // Crear y asociar las preguntas interactivas generadas por IA
            if (nodeRes?.nodeId && stop.challengeQuestion) {
              let matchedIdx = -1
              const normTarget = (stop.correctAnswer || '').trim().toLowerCase()
              if (stop.options && stop.options.length > 0) {
                matchedIdx = stop.options.findIndex((opt) => opt.trim().toLowerCase() === normTarget)
                if (matchedIdx === -1 && normTarget) {
                  if (/^[a-d]$/i.test(normTarget)) {
                    matchedIdx = normTarget.charCodeAt(0) - 97
                  } else if (/^[1-9]$/.test(normTarget)) {
                    matchedIdx = parseInt(normTarget, 10) - 1
                  }
                }
                if (matchedIdx < 0 || matchedIdx >= stop.options.length) {
                  matchedIdx = 0
                }
              }

              const opts = (stop.options && stop.options.length > 0)
                ? stop.options.map((opt, oIdx) => ({
                    sequence: oIdx + 1,
                    text: opt,
                    isCorrect: oIdx === matchedIdx,
                  }))
                : [
                    { sequence: 1, text: 'Verdadero', isCorrect: true },
                    { sequence: 2, text: 'Falso', isCorrect: false },
                  ]

              const questionsPayload: TeacherQuestion[] = [
                {
                  sequence: 1,
                  type: 'OPCION_UNICA',
                  statement: stop.challengeQuestion,
                  explanation: stop.explanation || '¡Excelente trabajo repasando los conceptos aprendidos!',
                  points: 10,
                  options: opts,
                },
              ]

              try {
                await updateTeacherNode(res.routeId, nodeRes.nodeId, {
                  title: stop.title.trim() || 'Parada Didáctica',
                  instructions: stop.description?.trim() || undefined,
                  activityType: validActivityType,
                  estimatedMinutes: 15,
                  questions: questionsPayload,
                })
              } catch (qErr) {
                console.error('Error guardando preguntas asociadas a la parada:', qErr)
              }
            }
          } catch (nodeErr) {
            console.error('Error creando nodo generado por IA:', nodeErr)
          }
        }
      }

      setShowCreateModal(false)
      setNewTitle('')
      setNewDescription('')
      setAiFiles([])
      setBanner({
        type: 'success',
        message: stopsToCreate.length
          ? `¡Roadmap escolar creado con ${stopsToCreate.length} paradas y preguntas generadas por Rupi IA!`
          : '¡Nuevo roadmap escolar creado! Ahora puedes agregar paradas y materiales didácticos.',
      })

      await loadRoutes()
      await handleOpenRouteDetail(res.routeId)
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error creando el roadmap escolar.'
      setAiError(msg)
    } finally {
      setSavingRoute(false)
      setAiGenerating(false)
    }
  }

  async function handleAddNode(e: React.FormEvent) {

    e.preventDefault()

    if (!selectedRouteId || !newNodeTitle.trim()) return



    setSavingNode(true)

    setBanner(null)



    try {

      await createTeacherNode(selectedRouteId, {

        title: newNodeTitle.trim(),

        instructions: newNodeInstructions.trim() || undefined,

        activityType: newNodeType,

        estimatedMinutes: newNodeMinutes,

      })



      setShowAddNodeModal(false)

      setNewNodeTitle('')

      setNewNodeInstructions('')

      setBanner({ type: 'success', message: '¡Parada didáctica agregada exitosamente al camino!' })



      // Recargar detalle

      const detail = await fetchTeacherRouteDetail(selectedRouteId)

      setActiveRouteDetail(detail)

      setOrderedNodes(detail.nodes)

      setIsReorderDirty(false)

      if (detail.nodes.length > 0) {

        setSelectedNode(detail.nodes[detail.nodes.length - 1])

      }

      loadRoutes()

    } catch (err: unknown) {

      const msg = err instanceof Error ? err.message : 'Error agregando la parada.'

      setBanner({ type: 'error', message: msg })

    } finally {

      setSavingNode(false)

    }

  }



  async function handleResourceAttached(payload: AttachResourcePayload) {

    if (!selectedRouteId || !selectedNode) return



    await attachResourceToNode(selectedRouteId, selectedNode.id, payload)



    // Recargar detalle para ver el recurso reflejado

    const detail = await fetchTeacherRouteDetail(selectedRouteId)

    setActiveRouteDetail(detail)

    setOrderedNodes(detail.nodes)

    const updatedNode = detail.nodes.find((n) => n.id === selectedNode.id)

    if (updatedNode) setSelectedNode(updatedNode)

    setBanner({ type: 'success', message: `Material enlazado exitosamente a "${selectedNode.title}".` })

  }



  // Funciones de Drag and Drop y Reorganización de Secuencia (HU-06)

  function handleDragStart(index: number, e: React.DragEvent) {

    setDraggedIndex(index)

    e.dataTransfer.effectAllowed = 'move'

    e.dataTransfer.setData('text/plain', String(index))

  }



  function handleDragOver(index: number, e: React.DragEvent) {

    e.preventDefault()

    e.dataTransfer.dropEffect = 'move'

    if (dragOverIndex !== index) {

      setDragOverIndex(index)

    }

  }



  function handleDragLeave(index: number) {

    if (dragOverIndex === index) {

      setDragOverIndex(null)

    }

  }



  function handleDrop(targetIndex: number, e: React.DragEvent) {

    e.preventDefault()

    setDragOverIndex(null)

    if (draggedIndex === null || draggedIndex === targetIndex) return



    moveNode(draggedIndex, targetIndex)

    setDraggedIndex(null)

  }



  function handleDragEnd() {

    setDraggedIndex(null)

    setDragOverIndex(null)

  }



  function moveNode(fromIndex: number, toIndex: number) {

    if (fromIndex === toIndex || toIndex < 0 || toIndex >= orderedNodes.length) return

    const updated = [...orderedNodes]

    const [moved] = updated.splice(fromIndex, 1)

    updated.splice(toIndex, 0, moved)



    // Actualizar temporalmente el orden visual de la secuencia (Tarea 2)

    const renumbered = updated.map((item, idx) => ({ ...item, sequence: idx + 1 }))

    setOrderedNodes(renumbered)

    setIsReorderDirty(true)

  }



  function handleCancelReorder() {

    if (!activeRouteDetail) return

    setOrderedNodes(activeRouteDetail.nodes)

    setIsReorderDirty(false)

    setBanner({ type: 'success', message: 'Secuencia restablecida a la última versión guardada.' })

  }



  async function handleSaveReorder() {

    if (!selectedRouteId || !isReorderDirty) return

    setSavingReorder(true)

    setBanner(null)

    try {

      const nodeIds = orderedNodes.map((n) => n.id)

      const updatedDetail = await reorderTeacherNodes(selectedRouteId, nodeIds)

      setActiveRouteDetail(updatedDetail)

      setOrderedNodes(updatedDetail.nodes)

      setIsReorderDirty(false)

      setBanner({ type: 'success', message: '¡Secuencia pedagógica reorganizada y guardada exitosamente!' })

    } catch (err: unknown) {

      const msg = err instanceof Error ? err.message : 'Error al guardar la nueva secuencia.'

      setBanner({ type: 'error', message: msg })

    } finally {

      setSavingReorder(false)

    }

  }



  async function handlePublishRoute() {

    if (!selectedRouteId || !activeRouteDetail) return

    if (activeRouteDetail.nodes.length === 0) {

      setBanner({ type: 'error', message: 'Debes agregar al menos una parada antes de publicar el roadmap.' })

      return

    }



    try {

      const res = await publishTeacherRoute(selectedRouteId)

      setBanner({ type: 'success', message: res.message })



      const detail = await fetchTeacherRouteDetail(selectedRouteId)

      setActiveRouteDetail(detail)

      loadRoutes()

    } catch (err: unknown) {

      const msg = err instanceof Error ? err.message : 'No se pudo publicar la ruta.'

      setBanner({ type: 'error', message: msg })

    }

  }



  async function handlePauseRoute() {

    if (!selectedRouteId) return

    try {

      const res = await pauseTeacherRoute(selectedRouteId)

      setBanner({ type: 'success', message: res.message })

      const detail = await fetchTeacherRouteDetail(selectedRouteId)

      setActiveRouteDetail(detail)

      loadRoutes()

    } catch (err: unknown) {

      const msg = err instanceof Error ? err.message : 'No se pudo pausar la ruta.'

      setBanner({ type: 'error', message: msg })

    }

  }



  return (

    <div className="teacher-authoring-view">

      {/* 1. Barra de cabecera del docente con perfil e institución */}

      <header className="teacher-header">

        <div className="teacher-header-left">

          <div className="teacher-brand-badge">

            <GraduationCap size={24} className="teacher-icon" aria-hidden="true" />

            <h1 className="teacher-brand-title">Panel de Docente</h1>

          </div>

        </div>



        <div className="teacher-header-right">

          <div className="profile-student-card teacher-profile-card">

            <StudentAvatar

              name={teacherName}

              role="DOCENTE"

              size="sm"

              showBadge

            />

            <span className="profile-text-col">

              <b className="profile-student-name">{teacherName}</b>

              <small className="profile-school-name">{schoolName}</small>

            </span>

          </div>

          <button

            type="button"

            className="profile-logout-btn teacher-logout-btn"

            onClick={onLogout}

            title="Cerrar sesión"

          >

            <LogOut size={16} aria-hidden="true" />

            <span>Salir</span>

          </button>

        </div>

      </header>



      {/* Banner de retroalimentación */}

      {banner && (

        <div className={`teacher-banner ${banner.type}`} role="alert">

          {banner.type === 'success' ? <CheckCircle size={20} /> : <BookOpen size={20} />}

          <p>{banner.message}</p>

        </div>

      )}



      {/* 2. Contenido principal */}

      <main className="teacher-main-content">

        {!activeRouteDetail ? (

          /* Vista: Lista de Roadmaps del Profesor */

          <section className="teacher-routes-dashboard" aria-label="Tus roadmaps educativos">

            <div className="dashboard-hero-card">

              <div className="hero-text-block">

                <h2 className="hero-heading">Herramienta de Autoría y Planificación Curricular</h2>

                <p className="hero-subtext">

                  Diseña rutas de aprendizaje personalizadas para tus estudiantes de primaria, organiza los tramos

                  didácticos y adjunta fichas de trabajo, presentaciones y enlaces a cada parada del mapa.

                </p>

              </div>

              <button

                type="button"

                className="hero-create-btn"

                onClick={() => setShowCreateModal(true)}

              >

                <Plus size={20} aria-hidden="true" />

                <span>Diseñar Nuevo Roadmap</span>

              </button>

            </div>



            <div className="routes-list-section">

              <div className="section-title-row">

                <h3 className="section-title">

                  <Compass size={20} className="icon-leaf" aria-hidden="true" />

                  <span>Mis Roadmaps Creados ({routes.length})</span>

                </h3>

                <button

                  type="button"

                  className="refresh-btn"

                  onClick={loadRoutes}

                  disabled={loading}

                  title="Actualizar lista"

                >

                  <RefreshCw size={16} className={loading ? 'spinning' : ''} aria-hidden="true" />

                  <span>Actualizar</span>

                </button>

              </div>



              {loading ? (

                <div className="teacher-loading-card">Cargando tus roadmaps escolares…</div>

              ) : routes.length === 0 ? (

                <div className="teacher-empty-state">

                  <BookOpen size={48} className="empty-icon" aria-hidden="true" />

                  <h4>Aún no has creado ningún roadmap</h4>

                  <p>Comienza diseñando tu primer mapa curricular adaptado a la programación de tu aula.</p>

                  <button

                    type="button"

                    className="empty-create-btn"

                    onClick={() => setShowCreateModal(true)}

                  >

                    <Plus size={18} aria-hidden="true" />

                    <span>Crear mi primer roadmap</span>

                  </button>

                </div>

              ) : (

                <div className="teacher-routes-grid">

                  {routes.map((r) => (

                    <article key={r.routeId} className="teacher-route-card">

                      <div className="card-top-row">

                        <span className={`state-pill ${r.state.toLowerCase()}`}>

                          {stateLabel(r.state)}

                        </span>

                        <span className="grade-badge">{r.gradeNumber === 0 ? "Libre / Multigrado" : r.gradeName}</span>

                      </div>



                      <h4 className="card-route-title">{r.title}</h4>

                      <p className="card-route-desc">

                        {r.description || 'Sin descripción curricular.'}

                      </p>



                      {/* Código de acceso e inscripción escolar */}

                      {r.code && (

                        <div className="card-code-strip">

                          <KeyRound size={14} className="code-key-icon" aria-hidden="true" />

                          <span className="card-code-label">Código:</span>

                          <code className="card-code-value">{r.code}</code>

                          <button

                            type="button"

                            className="card-code-copy-btn"

                            onClick={(e) => {

                              e.stopPropagation()

                              handleCopyCode(r.code!)

                            }}

                            title="Copiar código de clase para tus estudiantes"

                          >

                            {copiedCode === r.code ? (

                              <>

                                <Check size={12} className="copied-check" />

                                <span>Copiado</span>

                              </>

                            ) : (

                              <>

                                <Copy size={12} />

                                <span>Copiar</span>

                              </>

                            )}

                          </button>

                        </div>

                      )}



                      <div className="card-meta-row">

                        <span className="area-pill">{r.areaName}</span>

                        <span className="stops-count">

                          <MapPin size={14} aria-hidden="true" />

                          <strong>{r.totalNodes}</strong> paradas

                        </span>

                      </div>



                      <div className="card-actions-row" style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', gap: '8px', width: '100%', marginTop: 'auto' }}>
                        <button
                          type="button"
                          className="open-editor-btn"
                          style={{ flex: 1, minWidth: 0, width: 'auto' }}
                          onClick={() => handleOpenRouteDetail(r.routeId)}
                        >
                          <BookOpen size={16} aria-hidden="true" />
                          <span>Editar Roadmap y Paradas</span>
                        </button>
                        <button
                          type="button"
                          className="card-delete-btn"
                          style={{ flexShrink: 0, width: '44px', height: '44px' }}
                          onClick={(e) => {
                            e.stopPropagation()
                            setRouteToDelete({ routeId: r.routeId, title: r.title })
                          }}
                          title="Eliminar este roadmap"
                          aria-label={`Eliminar roadmap ${r.title}`}
                        >
                          <Trash2 size={16} aria-hidden="true" />
                        </button>
                      </div>

                    </article>

                  ))}

                </div>

              )}

            </div>

          </section>

        ) : (

          /* Vista: Editor de Roadmap y Adjuntos */

          <section className="teacher-editor-layout" aria-label={`Editando ${activeRouteDetail.title}`}>

            <div className="editor-top-nav">

              <button

                type="button"

                className="back-to-list-btn"

                onClick={() => { setActiveRouteDetail(null); setSelectedRouteId(null); }}

              >

                <ArrowLeft size={18} aria-hidden="true" />

                <span>Volver a Mis Roadmaps</span>

              </button>



              <div className="editor-status-box">

                <span className={`state-pill ${activeRouteDetail.state.toLowerCase()}`}>

                  {activeRouteDetail.state === 'PUBLICADO' ? '✓ Publicado para Alumnos' : stateLabel(activeRouteDetail.state)}

                </span>

                {activeRouteDetail.state === 'PUBLICADO' ? (

                  <button

                    type="button"

                    className="pause-cta-btn"

                    onClick={handlePauseRoute}

                    title="Tus estudiantes dejan de ver la ruta hasta que la vuelvas a publicar. Su avance se conserva."

                  >

                    <PauseCircle size={16} aria-hidden="true" />

                    <span>Pausar para mis Estudiantes</span>

                  </button>

                ) : (

                  <button

                    type="button"

                    className="publish-cta-btn"

                    onClick={handlePublishRoute}

                  >

                    <Send size={16} aria-hidden="true" />

                    <span>{activeRouteDetail.state === 'PAUSADO' ? 'Volver a publicar' : 'Publicar para mis Estudiantes'}</span>

                  </button>

                )}

                <button
                  type="button"
                  className="delete-roadmap-nav-btn"
                  onClick={() => setRouteToDelete({ routeId: activeRouteDetail.routeId, title: activeRouteDetail.title })}
                  title="Eliminar este roadmap"
                >
                  <Trash2 size={16} aria-hidden="true" />
                  <span>Eliminar Roadmap</span>
                </button>


              </div>

            </div>



            {/* SECCIÓN PRINCIPAL: CALENDARIO COMPACTO (IZQUIERDA) Y ACCIONES VINCULADAS (DERECHA) */}

            <div className="roadmap-hero-panel">

              {/* Columna Izquierda: Calendario del aula en formato compacto */}

              <div className="roadmap-calendar-col">

                <RouteCalendar

                  key={activeRouteDetail.versionRouteId}

                  routeId={activeRouteDetail.versionRouteId}

                  teacher

                  compact

                />

              </div>



              {/* Columna Derecha: Acciones principales al costado del calendario */}

              <div className="roadmap-actions-col">

                <div className="roadmap-identity-box">

                  <div className="roadmap-tags-row">

                    <span className="area-tag">{activeRouteDetail.area.name}</span>

                    <span className="grade-tag">{activeRouteDetail.grade.number === 0 ? "Libre / Multigrado" : activeRouteDetail.grade.name}</span>

                    <button
                      type="button"
                      className="roadmap-bg-selector-pill"
                      onClick={() => setShowBgModal(true)}
                      title={`Cambiar paisaje peruano para este roadmap (actual: ${teacherMapBg.name})`}
                    >
                      <Palette size={13} aria-hidden="true" />
                      <span>Paisaje: <strong>{teacherMapBg.region}</strong></span>
                    </button>

                  </div>

                  <h2 className="roadmap-hero-title">{activeRouteDetail.title}</h2>

                  <p className="roadmap-hero-desc">{activeRouteDetail.description || 'Sin descripción curricular.'}</p>

                </div>



                {/* Código de clase e inscripción escolar */}

                {activeRouteDetail.code && (

                  <div className="roadmap-code-banner">

                    <div className="code-text-group">

                      <KeyRound size={16} className="code-key-icon" aria-hidden="true" />

                      <span className="code-title">Código de inscripción:</span>

                      <code className="code-highlight">{activeRouteDetail.code}</code>

                    </div>

                    <button

                      type="button"

                      className="copy-code-badge-btn"

                      onClick={() => handleCopyCode(activeRouteDetail.code!)}

                      title="Copiar código de clase"

                    >

                      {copiedCode === activeRouteDetail.code ? (

                        <>

                          <Check size={14} className="copied-check" />

                          <span>¡Copiado!</span>

                        </>

                      ) : (

                        <>

                          <Copy size={14} />

                          <span>Copiar Código</span>

                        </>

                      )}

                    </button>

                  </div>

                )}



                {/* Botones principales solicitados: Reportes de resultados y Vista de Estudiantes */}

                <div className="roadmap-primary-buttons">

                  <button

                    type="button"

                    className="roadmap-big-action-btn reports"

                    onClick={() => onOpenReports(activeRouteDetail.versionRouteId, activeRouteDetail.grade.number)}

                    title="Ver reportes de resultados de este roadmap escolar"

                  >

                    <FileText size={20} aria-hidden="true" />

                    <div className="action-btn-copy">

                      <span className="action-btn-title">Reportes de resultados</span>

                      <span className="action-btn-subtitle">Progreso y calificaciones del aula</span>

                    </div>

                  </button>



                  <button

                    type="button"

                    className="roadmap-big-action-btn student-view"

                    onClick={() => onSwitchToStudentView(activeRouteDetail.versionRouteId, activeRouteDetail.grade.number, activeRouteDetail.area.code)}

                    title="Ver y explorar el mapa interactivo como tus estudiantes"

                  >

                    <Eye size={20} aria-hidden="true" />

                    <div className="action-btn-copy">

                      <span className="action-btn-title">Vista de Estudiantes</span>

                      <span className="action-btn-subtitle">Probar aventura didáctica</span>

                    </div>

                  </button>

                </div>




              </div>

            </div>



            {/* Cuerpo del editor: Paradas y Materiales */}

            <div className="editor-columns">

              {/* Columna Izquierda: Paradas del camino y Reorganización Drag & Drop (HU-06) */}

              <div className="editor-stops-panel">

                <div className="stops-panel-header">
                  <h3 className="stops-panel-title">
                    <Compass size={18} aria-hidden="true" />
                    <span>Secuencia de Paradas ({orderedNodes.length})</span>
                  </h3>
                  <button
                    type="button"
                    className="add-stop-panel-btn"
                    onClick={() => setShowAddNodeModal(true)}
                  >
                    <Plus size={16} aria-hidden="true" />
                    <span>Agregar Parada</span>
                  </button>
                </div>

                <div className="stops-panel-subheader">
                  <p className="stops-panel-hint">
                    Reorganiza el orden de aprendizaje arrastrando las paradas o usando las flechas de subida y bajada.
                  </p>
                  {orderedNodes.length > 1 && (
                    <span className="stops-hint-badge">Arrastra ⋮⋮ para ordenar</span>
                  )}
                </div>




                {/* Banner de confirmación de nuevo orden (Tarea 2) */}

                {isReorderDirty && (

                  <div className="reorder-action-banner" role="status" aria-live="polite">

                    <div className="reorder-banner-info">

                      <ArrowUpDown size={18} className="reorder-icon" aria-hidden="true" />

                      <div>

                        <strong className="reorder-alert-title">Nuevo orden sin guardar</strong>

                        <span className="reorder-alert-desc">Se actualizó el orden visualmente. Confirma para guardar.</span>

                      </div>

                    </div>

                    <div className="reorder-banner-actions">

                      <button

                        type="button"

                        className="reorder-save-btn"

                        onClick={handleSaveReorder}

                        disabled={savingReorder}

                        title="Confirmar y guardar nuevo orden en la base de datos"

                      >

                        <Check size={16} aria-hidden="true" />

                        <span>{savingReorder ? 'Guardando…' : 'Guardar Orden'}</span>

                      </button>

                      <button

                        type="button"

                        className="reorder-cancel-btn"

                        onClick={handleCancelReorder}

                        disabled={savingReorder}

                        title="Descartar cambios y volver al orden original"

                      >

                        <RotateCcw size={15} aria-hidden="true" />

                        <span>Restablecer</span>

                      </button>

                    </div>

                  </div>

                )}



                {orderedNodes.length === 0 ? (

                  <div className="no-stops-card">
                    <div className="no-stops-icon">
                      <Compass size={32} aria-hidden="true" />
                    </div>
                    <p className="no-stops-title">Esta ruta aún no tiene paradas</p>
                    <p className="no-stops-desc">
                      Usa el botón <strong>"+ Agregar Parada"</strong> de arriba para comenzar a estructurar tu camino de aprendizaje.
                    </p>
                  </div>

                ) : (

                  <div className="stops-timeline" role="list" aria-label="Lista de paradas reorganizables">

                    {orderedNodes.map((n, index) => {

                      const isSelected = selectedNode?.id === n.id

                      const isDragging = draggedIndex === index

                      const isOver = dragOverIndex === index



                      return (

                        <div

                          key={n.id}

                          className={`stop-card-item ${isSelected ? 'is-selected' : ''} ${isDragging ? 'is-dragging' : ''} ${isOver ? 'drag-over' : ''}`}

                          draggable={true}

                          onDragStart={(e) => handleDragStart(index, e)}

                          onDragOver={(e) => handleDragOver(index, e)}

                          onDragLeave={() => handleDragLeave(index)}

                          onDrop={(e) => handleDrop(index, e)}

                          onDragEnd={handleDragEnd}

                          onClick={() => setSelectedNode(n)}

                          role="listitem"

                          aria-grabbed={isDragging}

                        >

                          <div

                            className="stop-drag-handle"

                            title="Arrastra para mover en la secuencia"

                            aria-label="Arrastrar parada"

                          >

                            <GripVertical size={16} aria-hidden="true" />

                          </div>



                          <div className="stop-badge-seq">{n.sequence}</div>



                          <div className="stop-card-content">

                            <h4 className="stop-title">{n.title}</h4>

                            <div className="stop-meta">

                              <span className="stop-type">{n.activityType}</span>

                              <span className="stop-time">

                                <Clock size={12} aria-hidden="true" />

                                {n.estimatedMinutes || 10} min

                              </span>

                              {n.resources.length > 0 ? (
                                <span className="stop-meta-pill doc-source" title={`Fuentes base: ${n.resources.map((r) => r.title).join(", ")}`}>
                                  <FileText size={11} aria-hidden="true" /> {n.resources.length === 1 ? "1 doc" : `${n.resources.length} docs`}
                                </span>
                              ) : (
                                <span className="stop-attachments" title="Sin documentos adjuntos">
                                  <FileText size={12} aria-hidden="true" /> 0 docs
                                </span>
                              )}

                              {n.videoUrl && (

                                <span className="stop-meta-pill video" title="Tiene video configurado">

                                  <Video size={11} aria-hidden="true" /> Video

                                </span>

                              )}

                              {(n.availableFrom || n.lockedFrom) && (

                                <span className="stop-meta-pill date" title="Acceso con fechas programadas">

                                  <Calendar size={11} aria-hidden="true" /> Fechas

                                </span>

                              )}

                            </div>

                          </div>



                          <div className="stop-move-actions" onClick={(e) => e.stopPropagation()}>

                            <button

                              type="button"

                              className="move-node-btn"

                              onClick={() => moveNode(index, index - 1)}

                              disabled={index === 0}

                              title="Mover hacia arriba"

                              aria-label={`Mover hacia arriba ${n.title}`}

                            >

                              <ChevronUp size={16} aria-hidden="true" />

                            </button>

                            <button

                              type="button"

                              className="move-node-btn"

                              onClick={() => moveNode(index, index + 1)}

                              disabled={index === orderedNodes.length - 1}

                              title="Mover hacia abajo"

                              aria-label={`Mover hacia abajo ${n.title}`}

                            >

                              <ChevronDown size={16} aria-hidden="true" />

                            </button>

                            <button

                              type="button"

                              className="delete-node-btn"

                              onClick={() => setNodeToDelete(n)}

                              title={`Eliminar parada "${n.title}"`}

                              aria-label={`Eliminar parada ${n.title}`}

                            >

                              <Trash2 size={15} aria-hidden="true" />

                            </button>

                          </div>

                        </div>

                      )

                    })}

                  </div>

                )}



              </div>



              {/* Columna Derecha: Taller de Edición Integral de la Parada Seleccionada */}

              <div className="editor-stop-workspace">

                {selectedNode ? (

                  <div className="stop-editor-card">

                    {/* Cabecera de la parada */}

                    <div className="stop-editor-header">

                      <div className="stop-header-identity">

                        <span className="stop-seq-pill">Parada didáctica #{selectedNode.sequence}</span>

                        <h3 className="stop-editor-title">{editTitle || selectedNode.title}</h3>

                      </div>

                      <div className="stop-header-actions-group">

                        <div className="stop-header-status">

                          <span className="stop-type-pill">{editActivityType === 'LECCION' ? 'Lección Interactiva' : 'Reto Pedagógico'}</span>

                          <span className="stop-time-pill">{editMinutes} min estimados</span>

                        </div>

                        <button

                          type="button"

                          className="delete-current-node-btn"

                          onClick={() => setNodeToDelete(selectedNode)}

                          title="Eliminar esta parada didáctica del roadmap"

                        >

                          <Trash2 size={15} aria-hidden="true" />

                          <span>Eliminar parada</span>

                        </button>

                      </div>

                    </div>



                    {/* Barra de pestañas segmentadas */}

                    <nav className="stop-nav-tabs" role="tablist" aria-label="Secciones de edición de parada">

                      <button

                        type="button"

                        role="tab"

                        aria-selected={activeNodeTab === 'detalles'}

                        className={`stop-nav-tab ${activeNodeTab === 'detalles' ? 'is-active' : ''}`}

                        onClick={() => setActiveNodeTab('detalles')}

                      >

                        <FileText size={16} aria-hidden="true" />

                        <span>1. Detalles y Acceso</span>

                        {!editNoDateLimit && <span className="tab-dot-badge" title="Fechas programadas">•</span>}

                      </button>



                      <button

                        type="button"

                        role="tab"

                        aria-selected={activeNodeTab === 'video'}

                        className={`stop-nav-tab ${activeNodeTab === 'video' ? 'is-active' : ''}`}

                        onClick={() => setActiveNodeTab('video')}

                      >

                        <Video size={16} aria-hidden="true" />

                        <span>2. Video de la Lección</span>

                        {editVideoUrl.trim() && <span className="tab-pill-green">✓ Video</span>}

                      </button>



                      <button

                        type="button"

                        role="tab"

                        aria-selected={activeNodeTab === 'preguntas'}

                        className={`stop-nav-tab ${activeNodeTab === 'preguntas' ? 'is-active' : ''}`}

                        onClick={() => setActiveNodeTab('preguntas')}

                      >

                        <HelpCircle size={16} aria-hidden="true" />

                        <span>3. Preguntas del Reto ({editQuestions.length})</span>

                      </button>



                      <button

                        type="button"

                        role="tab"

                        aria-selected={activeNodeTab === 'materiales'}

                        className={`stop-nav-tab ${activeNodeTab === 'materiales' ? 'is-active' : ''}`}

                        onClick={() => setActiveNodeTab('materiales')}

                      >

                        <Paperclip size={16} aria-hidden="true" />

                        <span>4. Fichas y Recursos ({selectedNode.resources.length})</span>

                      </button>

                    </nav>



                    {/* Cuerpo del editor */}

                    <div className="stop-editor-body">

                      {loadingNodeDetail ? (

                        <div className="stop-loading-box">

                          <RefreshCw size={24} className="spinning" aria-hidden="true" />

                          <p>Cargando información y preguntas de la parada…</p>

                        </div>

                      ) : (

                        <>

                          {/* PESTAÑA 1: DETALLES Y ACCESO TEMPORAL */}

                          {activeNodeTab === 'detalles' && (

                            <div className="tab-content-panel" role="tabpanel">

                              {/* Documentos y Fuentes de Aprendizaje del Nodo */}
                              <div className="form-card-section node-sources-section">
                                <div className="node-sources-header">
                                  <div className="sources-title-row">
                                    <FileText size={18} className="sources-icon" aria-hidden="true" />
                                    <h4 className="section-subtitle">
                                      Documentos y Fuentes Base ({selectedNode.resources.length})
                                    </h4>
                                  </div>
                                  <button
                                    type="button"
                                    className="sources-manage-link"
                                    onClick={() => setActiveNodeTab('materiales')}
                                    title="Gestionar en Fichas y Recursos"
                                  >
                                    <span>Gestionar en Fichas y Recursos</span>
                                    <ArrowRight size={14} aria-hidden="true" />
                                  </button>
                                </div>

                                {selectedNode.resources.length > 0 ? (
                                  <div className="node-sources-chips-grid">
                                    {selectedNode.resources.map((res) => (
                                      <div key={res.id} className="node-source-card">
                                        <div className="source-card-lead">
                                          <span className="source-badge">Fuente Base</span>
                                          <strong className="source-title" title={res.title}>
                                            {res.title}
                                          </strong>
                                        </div>
                                        <a
                                          href={res.url}
                                          target="_blank"
                                          rel="noopener noreferrer"
                                          className="source-view-btn"
                                          title="Abrir o descargar documento"
                                        >
                                          <ExternalLink size={13} aria-hidden="true" />
                                          <span>Ver</span>
                                        </a>
                                      </div>
                                    ))}
                                  </div>
                                ) : (
                                  <div className="no-sources-callout">
                                    <p>Esta parada no tiene documentos base adjuntos.</p>
                                    <button
                                      type="button"
                                      className="add-source-inline-btn"
                                      onClick={() => setActiveNodeTab('materiales')}
                                    >
                                      <Paperclip size={14} aria-hidden="true" />
                                      <span>Adjuntar PDF o Documento a esta parada</span>
                                    </button>
                                  </div>
                                )}
                              </div>

                              <div className="form-card-section">

                                <h4 className="section-subtitle">Datos Pedagógicos de la Parada</h4>

                                <div className="form-group">

                                  <label htmlFor="edit-node-title">Título de la parada didáctica *</label>

                                  <input

                                    id="edit-node-title"

                                    type="text"

                                    className="editor-form-input"

                                    value={editTitle}

                                    onChange={(e) => setEditTitle(e.target.value)}

                                    placeholder="Ej. Parada 1: Cuentos andinos y tradición oral"

                                    required

                                  />

                                </div>



                                <div className="form-row-two">

                                  <div className="form-group">

                                    <label htmlFor="edit-node-type">Modalidad didáctica</label>

                                    <select

                                      id="edit-node-type"

                                      className="editor-form-select"

                                      value={editActivityType}

                                      onChange={(e) => setEditActivityType(e.target.value)}

                                    >

                                      <option value="LECCION">Lección explicativa y formativa</option>

                                      <option value="RETO">Reto evaluativo con preguntas</option>

                                    </select>

                                  </div>



                                  <div className="form-group">

                                    <label htmlFor="edit-node-minutes">Tiempo estimado (minutos)</label>

                                    <input

                                      id="edit-node-minutes"

                                      type="number"

                                      min={5}

                                      max={120}

                                      className="editor-form-input"

                                      value={editMinutes}

                                      onChange={(e) => setEditMinutes(Number(e.target.value))}

                                    />

                                  </div>

                                </div>



                                <div className="form-group">

                                  <label htmlFor="edit-node-instructions">Instrucciones para los estudiantes</label>

                                  <textarea

                                    id="edit-node-instructions"

                                    rows={3}

                                    className="editor-form-textarea"

                                    value={editInstructions}

                                    onChange={(e) => setEditInstructions(e.target.value)}

                                    placeholder="Explica a los estudiantes qué aprenderán, qué video observar o qué reto resolverán en esta parada."

                                  />

                                </div>

                              </div>



                              {/* Bloque de Acceso Temporal (Fechas) */}

                              <div className="form-card-section dates-config-section">

                                <div className="dates-header-row">

                                  <div className="dates-title-box">

                                    <Calendar size={20} className="icon-calendar" aria-hidden="true" />

                                    <div>

                                      <h4 className="section-subtitle">Disponibilidad y Límite de Acceso</h4>

                                      <p className="section-subdesc">

                                        Controla si esta parada está abierta permanentemente o si requiere una ventana de calendario escolar.

                                      </p>

                                    </div>

                                  </div>

                                </div>



                                <label className="date-limit-toggle-card">

                                  <input

                                    type="checkbox"

                                    checked={editNoDateLimit}

                                    onChange={(e) => setEditNoDateLimit(e.target.checked)}

                                  />

                                  <div className="toggle-text-block">

                                    <strong className="toggle-label-main">

                                      Acceso continuo e ilimitado (Sin límite de fechas)

                                    </strong>

                                    <span className="toggle-label-sub">

                                      Recomendado para el inicio del curso: los alumnos pueden acceder a esta parada en cualquier momento según su propio avance.

                                    </span>

                                  </div>

                                </label>



                                {!editNoDateLimit && (

                                  <div className="date-pickers-container">

                                    <div className="date-picker-row">

                                      <div className="form-group">

                                        <label htmlFor="edit-node-available-from">Disponible a partir de:</label>

                                        <input

                                          id="edit-node-available-from"

                                          type="datetime-local"

                                          className="editor-form-input"

                                          value={editAvailableFrom}

                                          onChange={(e) => setEditAvailableFrom(e.target.value)}

                                        />

                                        <small className="field-hint">Antes de esta fecha la parada estará en espera.</small>

                                      </div>



                                      <div className="form-group">

                                        <label htmlFor="edit-node-locked-from">Bloquear acceso a partir de:</label>

                                        <input

                                          id="edit-node-locked-from"

                                          type="datetime-local"

                                          className="editor-form-input"

                                          value={editLockedFrom}

                                          onChange={(e) => setEditLockedFrom(e.target.value)}

                                        />

                                        <small className="field-hint">Después de esta fecha el nodo ya no permitirá nuevos intentos.</small>

                                      </div>

                                    </div>



                                    <div className="quick-dates-row">

                                      <span className="quick-dates-label">Programación rápida:</span>

                                      <button

                                        type="button"

                                        className="btn-quick-date"

                                        onClick={() => setQuickDateRange(7)}

                                      >

                                        Esta semana (+7 días)

                                      </button>

                                      <button

                                        type="button"

                                        className="btn-quick-date"

                                        onClick={() => setQuickDateRange(30)}

                                      >

                                        Este mes (+30 días)

                                      </button>

                                      <button

                                        type="button"

                                        className="btn-quick-date reset"

                                        onClick={() => setEditNoDateLimit(true)}

                                      >

                                        Restablecer a Sin Límite

                                      </button>

                                    </div>

                                  </div>

                                )}

                              </div>

                            </div>

                          )}



                          {/* PESTAÑA 2: VIDEO DE LA LECCIÓN */}

                          {activeNodeTab === 'video' && (

                            <div className="tab-content-panel" role="tabpanel">

                              <div className="form-card-section">

                                <h4 className="section-subtitle">Video Educativo para la Parada</h4>

                                <p className="section-subdesc">

                                  Ingresa un enlace de YouTube o sube un archivo de video local (.mp4 o .webm) que se reproducirá directamente para los estudiantes.

                                </p>



                                <div className="video-input-controls">

                                  <div className="form-group flex-1">

                                    <label htmlFor="edit-node-video-url">Enlace o URL del video</label>

                                    <input

                                      id="edit-node-video-url"

                                      type="text"

                                      className="editor-form-input"

                                      value={editVideoUrl}

                                      onChange={(e) => setEditVideoUrl(e.target.value)}

                                      placeholder="https://www.youtube.com/watch?v=... o URL de video"

                                    />

                                  </div>



                                  <div className="video-action-buttons">

                                    <input

                                      type="file"

                                      ref={videoInputRef}

                                      accept="video/mp4,video/webm"

                                      style={{ display: 'none' }}

                                      onChange={handleVideoFileUpload}

                                    />

                                    <button

                                      type="button"

                                      className="btn-upload-file-video"

                                      onClick={() => videoInputRef.current?.click()}

                                      disabled={uploadingVideo}

                                      title="Subir video desde mi computadora"

                                    >

                                      <Upload size={16} aria-hidden="true" />

                                      <span>{uploadingVideo ? 'Subiendo video…' : 'Subir video (.mp4/.webm)'}</span>

                                    </button>



                                    {editVideoUrl.trim() && (

                                      <button

                                        type="button"

                                        className="btn-remove-video"

                                        onClick={() => setEditVideoUrl('')}

                                        title="Quitar video de esta parada"

                                      >

                                        <Trash2 size={16} aria-hidden="true" />

                                        <span>Quitar</span>

                                      </button>

                                    )}

                                  </div>

                                </div>

                              </div>



                              {/* Vista previa en vivo del video */}

                              <div className="video-preview-box">

                                {editVideoUrl.trim() ? (

                                  (() => {

                                    const videoInfo = resolveEducationalVideo(

                                      [{ url: editVideoUrl.trim(), type: 'VIDEO', text: editTitle || selectedNode.title }],

                                      editTitle || selectedNode.title,

                                      selectedNode.sequence,

                                      activeRouteDetail?.area?.code

                                    )



                                    return (

                                      <div className="video-preview-player-container">

                                        <div className="preview-top-bar">

                                          <div className="preview-title-box">

                                            <Play size={16} className="play-icon" aria-hidden="true" />

                                            <strong>{videoInfo.title}</strong>

                                          </div>

                                          {videoInfo.isDirectVideo ? (

                                            <span className="direct-badge">Video Subido / Local</span>

                                          ) : (

                                            <span className="youtube-badge">YouTube Verificado</span>

                                          )}

                                        </div>



                                        <div className="video-player-frame">

                                          {videoInfo.isDirectVideo ? (

                                            <video

                                              controls

                                              className="authoring-video-player"

                                              src={videoInfo.watchUrl}

                                            />

                                          ) : (

                                            <iframe

                                              className="authoring-video-iframe"

                                              src={videoInfo.embedUrl}

                                              title={videoInfo.title}

                                              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"

                                              allowFullScreen

                                            />

                                          )}

                                        </div>

                                      </div>

                                    )

                                  })()

                                ) : (

                                  <div className="video-empty-state">

                                    <Video size={40} className="empty-video-icon" aria-hidden="true" />

                                    <h5>Sin video asignado</h5>

                                    <p>Pega un enlace de YouTube o sube un video para enriquecer el aprendizaje de tus estudiantes en esta parada.</p>

                                  </div>

                                )}

                              </div>

                            </div>

                          )}



                          {/* PESTAÑA 3: PREGUNTAS DEL RETO */}

                          {activeNodeTab === 'preguntas' && (

                            <div className="tab-content-panel" role="tabpanel">

                              <div className="questions-intro-card">

                                <div>

                                  <h4 className="section-subtitle">Preguntas de Evaluación Didáctica ({editQuestions.length})</h4>

                                  <p className="section-subdesc">

                                    Configura las preguntas interactivas. Marca cuál es la respuesta correcta usando el selector verde de cada opción.

                                  </p>

                                </div>

                                <button

                                  type="button"

                                  className="btn-add-question-cta"

                                  onClick={handleAddQuestion}

                                >

                                  <Plus size={16} aria-hidden="true" />

                                  <span>Agregar Pregunta</span>

                                </button>

                              </div>



                              {editQuestions.length === 0 ? (

                                <div className="questions-empty-card">

                                  <HelpCircle size={40} className="empty-q-icon" aria-hidden="true" />

                                  <h5>Esta parada didáctica no tiene preguntas configuradas</h5>

                                  <p>Agrega retos de opción múltiple para que los estudiantes comprueben su aprendizaje y ganen estrellas.</p>

                                  <button

                                    type="button"

                                    className="btn-create-first-q"

                                    onClick={handleAddQuestion}

                                  >

                                    <Plus size={18} aria-hidden="true" />

                                    <span>Crear Primera Pregunta Interactiva</span>

                                  </button>

                                </div>

                              ) : (

                                <div className="questions-list-stack">

                                  {editQuestions.map((q, qIndex) => (

                                    <article key={q.id || `q-${qIndex}`} className="question-authoring-card">

                                      <div className="q-card-header">

                                        <div className="q-header-left">

                                          <span className="q-seq-badge">Pregunta #{qIndex + 1}</span>

                                          <div className="q-points-box">

                                            <label htmlFor={`q-points-${qIndex}`}>Puntos:</label>

                                            <input

                                              id={`q-points-${qIndex}`}

                                              type="number"

                                              min={1}

                                              max={100}

                                              className="q-points-input"

                                              value={q.points || 10}

                                              onChange={(e) => handleQuestionPointsChange(qIndex, Number(e.target.value))}

                                            />

                                            <span>XP</span>

                                          </div>

                                        </div>



                                        <button

                                          type="button"

                                          className="btn-delete-question"

                                          onClick={() => handleRemoveQuestion(qIndex)}

                                          title="Eliminar esta pregunta"

                                        >

                                          <Trash2 size={16} aria-hidden="true" />

                                          <span>Eliminar</span>

                                        </button>

                                      </div>



                                      <div className="q-card-fields">

                                        <div className="form-group">

                                          <label htmlFor={`q-stmt-${qIndex}`}>Enunciado de la pregunta *</label>

                                          <input

                                            id={`q-stmt-${qIndex}`}

                                            type="text"

                                            className="editor-form-input q-statement-input"

                                            value={q.statement}

                                            onChange={(e) => handleQuestionStatementChange(qIndex, e.target.value)}

                                            placeholder="Ej. ¿Qué descubrieron los niños en el cuento tradicional andino?"

                                            required

                                          />

                                        </div>



                                        <div className="form-group">

                                          <label htmlFor={`q-exp-${qIndex}`}>Explicación didáctica (retroalimentación pedagógica)</label>

                                          <textarea

                                            id={`q-exp-${qIndex}`}

                                            rows={2}

                                            className="editor-form-textarea q-explanation-input"

                                            value={q.explanation || ''}

                                            onChange={(e) => handleQuestionExplanationChange(qIndex, e.target.value)}

                                            placeholder="Pista o explicación que se mostrará al niño tras responder para reforzar el aprendizaje."

                                          />

                                        </div>



                                        {/* Opciones de respuesta */}

                                        <div className="q-options-container">

                                          <div className="options-title-row">

                                            <span className="options-title">Opciones de respuesta (marca la correcta):</span>

                                            <span className="options-hint">Los niños verán estas opciones al resolver la parada.</span>

                                          </div>



                                          <div className="options-list-grid">

                                            {q.options.map((opt, optIndex) => (

                                              <div

                                                key={opt.id || `opt-${optIndex}`}

                                                className={`option-row-card ${opt.isCorrect ? 'is-correct-option' : ''}`}

                                              >

                                                <label className="option-radio-selector">

                                                  <input

                                                    type="radio"

                                                    name={`correct-answer-for-q-${qIndex}`}

                                                    checked={opt.isCorrect}

                                                    onChange={() => handleSetCorrectOption(qIndex, optIndex)}

                                                  />

                                                  <span className={`option-status-tag ${opt.isCorrect ? 'correct' : 'incorrect'}`}>

                                                    {opt.isCorrect ? '✓ Correcta' : 'Incorrecta'}

                                                  </span>

                                                </label>



                                                <input

                                                  type="text"

                                                  className="option-text-input"

                                                  value={opt.text}

                                                  onChange={(e) => handleOptionTextChange(qIndex, optIndex, e.target.value)}

                                                  placeholder={`Escribe el texto de la opción ${optIndex + 1}`}

                                                  required

                                                />



                                                <button

                                                  type="button"

                                                  className="btn-remove-opt-item"

                                                  onClick={() => handleRemoveOption(qIndex, optIndex)}

                                                  disabled={q.options.length <= 2}

                                                  title={q.options.length <= 2 ? 'Mínimo 2 opciones requeridas' : 'Eliminar opción'}

                                                >

                                                  ✕

                                                </button>

                                              </div>

                                            ))}

                                          </div>



                                          <button

                                            type="button"

                                            className="btn-add-opt-item"

                                            onClick={() => handleAddOption(qIndex)}

                                          >

                                            <Plus size={14} aria-hidden="true" />

                                            <span>Agregar opción de respuesta</span>

                                          </button>

                                        </div>

                                      </div>

                                    </article>

                                  ))}




                                </div>

                              )}

                            </div>

                          )}



                          {/* PESTAÑA 4: FICHAS Y MATERIALES ADJUNTOS */}

                          {activeNodeTab === 'materiales' && (

                            <div className="tab-content-panel" role="tabpanel">

                              <MaterialUploadZone

                                nodeId={selectedNode.id}

                                nodeTitle={selectedNode.title}

                                resources={selectedNode.resources}

                                onResourceAttached={handleResourceAttached}

                              />

                            </div>

                          )}

                        </>

                      )}

                    </div>



                    {/* Barra de Guardado Permanente al Pie de la Parada */}

                    <div className="stop-editor-footer-bar">

                      <div className="footer-status-box">

                        {nodeSaveBanner && (

                          <div className={`node-save-alert ${nodeSaveBanner.type}`} role="alert">

                            {nodeSaveBanner.type === 'success' ? (

                              <CheckCircle2 size={18} aria-hidden="true" />

                            ) : (

                              <AlertCircle size={18} aria-hidden="true" />

                            )}

                            <span>{nodeSaveBanner.message}</span>

                          </div>

                        )}

                        {!nodeSaveBanner && (

                          <span className="footer-hint-text">

                            Edición en vivo de la Parada #{selectedNode.sequence}: <strong>"{editTitle || selectedNode.title}"</strong>

                          </span>

                        )}

                      </div>



                      <div className="footer-actions-group">

                        <button

                          type="button"

                          className="btn-save-node-changes"

                          onClick={handleSaveNodeChanges}

                          disabled={savingNodeChanges || loadingNodeDetail}

                          title="Guardar todos los cambios (detalles, fechas, video y preguntas) en la base de datos"

                        >

                          <Check size={18} aria-hidden="true" />

                          <span>{savingNodeChanges ? 'Guardando parada…' : 'Guardar Cambios en la Parada'}</span>

                        </button>

                      </div>

                    </div>

                  </div>

                ) : (

                  <div className="select-stop-prompt">

                    <Sparkles size={36} className="sparkle-icon" aria-hidden="true" />

                    <h4>Selecciona una parada del camino</h4>

                    <p>Elige una parada en la columna de la izquierda para editar su título, configurar fechas de acceso, asignar video y diseñar sus preguntas interactivas.</p>

                  </div>

                )}

              </div>

            </div>

          </section>

        )}

      </main>



      {/* Modal: Crear Nuevo Roadmap */}
      {showCreateModal && (
        <div className="teacher-modal-backdrop" role="dialog" aria-modal="true">
          <div className="teacher-modal-card">
            <div className="modal-header">
              <div className="modal-header-brand">
                <Compass size={22} className="modal-header-icon" />
                <h3>Diseñar Nuevo Roadmap Escolar</h3>
              </div>
              <button
                type="button"
                className="close-modal-btn"
                onClick={() => setShowCreateModal(false)}
                disabled={savingRoute || aiGenerating}
                aria-label="Cerrar modal"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateRoute} className="modal-form">
              {/* 1. Título */}
              <div className="form-group">
                <label htmlFor="route-title">
                  Título del Roadmap {aiFiles.length === 0 && <span className="required-star">*</span>}
                </label>
                <input
                  id="route-title"
                  type="text"
                  className="modal-input"
                  placeholder={
                    aiFiles.length > 0
                      ? 'Opcional: Déjalo en blanco para que Rupi IA lo titule según el material'
                      : 'Ej. Aventuras Matemáticas en el Valle de Huanta'
                  }
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  disabled={savingRoute || aiGenerating}
                />
              </div>

              {/* 2. Descripción pedagógica */}
              <div className="form-group">
                <label htmlFor="route-desc">Descripción pedagógica (opcional)</label>
                <textarea
                  id="route-desc"
                  className="modal-textarea"
                  rows={2}
                  placeholder="Explica qué competencias, retos o aprendizajes desarrollarán tus estudiantes en esta ruta."
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                  disabled={savingRoute || aiGenerating}
                />
              </div>

              {/* 3. Grado y Área Curricular en cuadrícula simétrica */}
              <div
                className="form-row"
                style={{
                  display: 'grid',
                  gridTemplateColumns: '1fr 1fr',
                  gap: '14px',
                  width: '100%',
                }}
              >
                <div className="form-group" style={{ minWidth: 0, width: '100%' }}>
                  <label htmlFor="route-grade">Grado Escolar</label>
                  <select
                    id="route-grade"
                    className="modal-select"
                    style={{ width: '100%', minWidth: 0 }}
                    value={newGrade}
                    onChange={(e) => setNewGrade(Number(e.target.value))}
                    disabled={savingRoute || aiGenerating}
                  >
                    {CNEB_GRADES.map((g) => (
                      <option key={g.value} value={g.value}>
                        {g.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-group" style={{ minWidth: 0, width: '100%' }}>
                  <label htmlFor="route-area">Área Curricular (CNEB)</label>
                  <select
                    id="route-area"
                    className="modal-select"
                    style={{ width: '100%', minWidth: 0 }}
                    value={newArea}
                    onChange={(e) => setNewArea(e.target.value)}
                    disabled={savingRoute || aiGenerating}
                  >
                    {CNEB_AREAS.map((a) => (
                      <option key={a.code} value={a.code}>
                        {a.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* 4. Apartado de subir documentos (Rupi IA) */}
              <div className="ai-authoring-card">
                <div className="ai-authoring-card-header">
                  <div className="ai-tag">
                    <Sparkles size={14} className="sparkle-icon" />
                    <span>Rupi IA Pedagógica</span>
                  </div>
                  <span className="ai-optional-pill">Opcional</span>
                </div>

                <p className="ai-card-subtitle">
                  Sube tus sesiones de clase, fichas o lecturas y Rupi estructurará las paradas y preguntas interactivas automáticamente.
                </p>

                {aiFiles.length === 0 ? (
                  /* Estado inicial: Caja para subir documentos */
                  <div className="ai-upload-box">
                    <label htmlFor="ai-doc-upload" className="ai-upload-label">
                      <div className="ai-upload-icon">
                        <Upload size={18} />
                      </div>
                      <span className="ai-upload-title">Subir sesiones de clase o lecturas</span>
                      <span className="ai-upload-hint">Soporta PDF, Word (.docx) o Texto (.txt)</span>
                    </label>
                  </div>
                ) : (
                  /* Estado con archivos: A la izquierda el titulo del documento, a la derecha el boton de subir mas */
                  <div className="ai-files-compact-row">
                    <div className="ai-files-chips-left">
                      {aiFiles.map((file, idx) => (
                        <div key={`${file.name}-${idx}`} className="ai-file-chip">
                          <Paperclip size={13} className="file-chip-icon" />
                          <span className="file-chip-name" title={file.name}>
                            {file.name}
                          </span>
                          <span className="file-chip-size">
                            ({(file.size / 1024).toFixed(0)} KB)
                          </span>
                          <button
                            type="button"
                            className="remove-file-chip-btn"
                            onClick={() =>
                              setAiFiles((prev) => prev.filter((_, i) => i !== idx))
                            }
                            disabled={savingRoute || aiGenerating}
                            title="Quitar archivo"
                          >
                            ✕
                          </button>
                        </div>
                      ))}
                    </div>

                    <label
                      htmlFor="ai-doc-upload"
                      className="ai-add-more-inline-btn"
                      title="Subir más documentos"
                    >
                      <Plus size={14} />
                      <span>Subir más</span>
                    </label>
                  </div>
                )}

                <input
                  id="ai-doc-upload"
                  type="file"
                  multiple
                  className="ai-upload-input"
                  accept=".pdf,.docx,.doc,.txt,.md"
                  onChange={(e) => {
                    if (e.target.files && e.target.files.length > 0) {
                      const incoming = Array.from(e.target.files)
                      setAiFiles((prev) => {
                        const seen = new Set(prev.map((f) => `${f.name}_${f.size}`))
                        const fresh = incoming.filter((f) => !seen.has(`${f.name}_${f.size}`))
                        return [...prev, ...fresh]
                      })
                      setAiError(null)
                      e.target.value = ''
                    }
                  }}
                  disabled={savingRoute || aiGenerating}
                />
              </div>

              {/* Mensaje de error si falla */}
              {aiError && (
                <div className="form-error-banner" role="alert">
                  <AlertCircle size={16} />
                  <span>{aiError}</span>
                </div>
              )}

              {/* Indicador de carga cuando la IA genera */}
              {aiGenerating && (
                <div className="ai-generating-indicator" role="status">
                  <Sparkles size={18} className="sparkle-icon-spin" />
                  <span>
                    Rupi IA está analizando tus {aiFiles.length} documentos y construyendo las paradas didácticas…
                  </span>
                </div>
              )}

              {/* 5. Acciones del Modal */}
              <div
                className="modal-actions"
                style={{
                  display: 'flex',
                  justifyContent: 'flex-end',
                  alignItems: 'center',
                  gap: '10px',
                  marginTop: '10px',
                }}
              >
                <button
                  type="button"
                  className="modal-cancel-btn"
                  onClick={() => setShowCreateModal(false)}
                  disabled={savingRoute || aiGenerating}
                >
                  Cancelar
                </button>

                <button
                  type="submit"
                  className={`modal-save-btn ${aiFiles.length > 0 ? 'ai-action-btn' : ''}`}
                  disabled={
                    savingRoute ||
                    aiGenerating ||
                    (aiFiles.length === 0 && !newTitle.trim())
                  }
                >
                  {aiFiles.length > 0 ? (
                    <>
                      <Sparkles size={16} />
                      <span>
                        {savingRoute || aiGenerating
                          ? 'Diseñando Roadmap con IA…'
                          : 'Crear Roadmap con Rupi IA'}
                      </span>
                    </>
                  ) : (
                    <span>{savingRoute ? 'Creando…' : 'Crear Roadmap'}</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Agregar Parada Didáctica */}

      {showAddNodeModal && (

        <div className="teacher-modal-backdrop" role="dialog" aria-modal="true">

          <div className="teacher-modal-card">

            <div className="modal-header">

              <h3>Agregar Parada al Roadmap</h3>

              <button

                type="button"

                className="close-modal-btn"

                onClick={() => setShowAddNodeModal(false)}

              >

                ✕

              </button>

            </div>



            <form onSubmit={handleAddNode} className="modal-form">

              <div className="form-group">

                <label htmlFor="node-title">Título de la parada didáctica *</label>

                <input

                  id="node-title"

                  type="text"

                  className="modal-input"

                  placeholder="Ej. Parada 1: Cuentos andinos y tradición oral"

                  value={newNodeTitle}

                  onChange={(e) => setNewNodeTitle(e.target.value)}

                  required

                />

              </div>



              <div className="form-group">

                <label htmlFor="node-instructions">Instrucciones para los estudiantes</label>

                <textarea

                  id="node-instructions"

                  className="modal-textarea"

                  rows={3}

                  placeholder="Instrucciones didácticas que leerán los niños al ingresar a esta lección."

                  value={newNodeInstructions}

                  onChange={(e) => setNewNodeInstructions(e.target.value)}

                />

              </div>



              <div className="form-row">

                <div className="form-group flex-1">

                  <label htmlFor="node-type">Tipo de parada</label>

                  <select

                    id="node-type"

                    className="modal-select"

                    value={newNodeType}

                    onChange={(e) => setNewNodeType(e.target.value)}

                  >

                    <option value="LECCION">Lección explicativa</option>

                    <option value="RETO">Reto pedagógico</option>

                  </select>

                </div>



                <div className="form-group flex-1">

                  <label htmlFor="node-minutes">Tiempo estimado (minutos)</label>

                  <input

                    id="node-minutes"

                    type="number"

                    min={5}

                    max={60}

                    className="modal-input"

                    value={newNodeMinutes}

                    onChange={(e) => setNewNodeMinutes(Number(e.target.value))}

                  />

                </div>

              </div>



              <div className="modal-actions">

                <button

                  type="button"

                  className="modal-cancel-btn"

                  onClick={() => setShowAddNodeModal(false)}

                >

                  Cancelar

                </button>

                <button

                  type="submit"

                  className="modal-save-btn"

                  disabled={savingNode || !newNodeTitle.trim()}

                >

                  {savingNode ? 'Guardando…' : 'Guardar Parada'}

                </button>

              </div>

            </form>

          </div>

        </div>

      )}



      {/* Modal: Confirmar Eliminar Parada Didáctica */}
      {nodeToDelete && (
        <div className="teacher-modal-backdrop" role="dialog" aria-modal="true">
          <div className="confirm-delete-modal">
            <button
              type="button"
              className="confirm-delete-close-btn"
              onClick={() => setNodeToDelete(null)}
              disabled={deletingNode}
              aria-label="Cerrar ventana de confirmación"
            >
              ✕
            </button>
            <div className="confirm-delete-content">
              <div className="confirm-delete-icon-wrapper">
                <Trash2 size={26} aria-hidden="true" />
              </div>
              <h3 className="confirm-delete-title">¿Eliminar parada didáctica?</h3>
              <p className="confirm-delete-target">
                ¿Estás seguro de que deseas eliminar la parada <strong>"{nodeToDelete.title}"</strong>?
              </p>
              <div className="confirm-delete-warning-box">
                <AlertCircle size={18} className="warning-box-icon" aria-hidden="true" />
                <p>
                  Se eliminarán sus recursos, preguntas y actividades asociadas. Las paradas restantes se renumerarán automáticamente. Esta acción no se puede deshacer.
                </p>
              </div>
              <div className="confirm-delete-actions">
                <button
                  type="button"
                  className="confirm-btn-cancel"
                  onClick={() => setNodeToDelete(null)}
                  disabled={deletingNode}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  className="confirm-btn-danger"
                  onClick={handleConfirmDeleteNode}
                  disabled={deletingNode}
                >
                  {deletingNode ? 'Eliminando…' : 'Sí, eliminar parada'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Confirmar Eliminar Roadmap */}
      {routeToDelete && (
        <div className="teacher-modal-backdrop" role="dialog" aria-modal="true">
          <div className="confirm-delete-modal">
            <button
              type="button"
              className="confirm-delete-close-btn"
              onClick={() => setRouteToDelete(null)}
              disabled={deletingRoute}
              aria-label="Cerrar ventana de confirmación"
            >
              ✕
            </button>
            <div className="confirm-delete-content">
              <div className="confirm-delete-icon-wrapper">
                <Trash2 size={26} aria-hidden="true" />
              </div>
              <h3 className="confirm-delete-title">¿Eliminar este roadmap?</h3>
              <p className="confirm-delete-target">
                ¿Estás seguro de que deseas eliminar el roadmap <strong>"{routeToDelete.title}"</strong>?
              </p>
              <div className="confirm-delete-warning-box">
                <AlertCircle size={18} className="warning-box-icon" aria-hidden="true" />
                <p>
                  El mapa se archivará, el código de acceso será revocado y tus alumnos ya no podrán ingresar a esta ruta. Esta acción no se puede deshacer.
                </p>
              </div>
              <div className="confirm-delete-actions">
                <button
                  type="button"
                  className="confirm-btn-cancel"
                  onClick={() => setRouteToDelete(null)}
                  disabled={deletingRoute}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  className="confirm-btn-danger"
                  onClick={handleConfirmDeleteRoute}
                  disabled={deletingRoute}
                >
                  {deletingRoute ? 'Eliminando…' : 'Sí, eliminar roadmap'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
      {activeRouteDetail && (
        <MapBackgroundModal
          isOpen={showBgModal}
          onClose={() => setShowBgModal(false)}
          currentBgId={teacherMapBg.id}
          onSelectBg={(bgId) => setTeacherMapBg(bgId)}
          routeTitle={activeRouteDetail.title}
        />
      )}


    </div>

  )

}

