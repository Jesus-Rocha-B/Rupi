import { useState, useEffect } from 'react'
import {
  Compass,
  Plus,
  BookOpen,
  Send,
  Sparkles,
  ArrowLeft,
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
} from 'lucide-react'
import {
  fetchTeacherRoutes,
  fetchTeacherRouteDetail,
  createTeacherRoute,
  createTeacherNode,
  attachResourceToNode,
  publishTeacherRoute,
  reorderTeacherNodes,
  type TeacherRouteSummary,
  type TeacherRouteDetail,
  type TeacherNode,
  type AttachResourcePayload,
} from '../../services/teacherApi'
import { MaterialUploadZone } from '../../components/MaterialUploadZone'
import { speakText } from '../../utils/speech'
import './TeacherAuthoring.css'

interface Props {
  teacherName: string
  schoolName: string
  onLogout: () => void
  onSwitchToStudentView: () => void
}

const CNEB_AREAS = [
  { code: 'MAT', name: 'Matemática' },
  { code: 'COM', name: 'Comunicación' },
  { code: 'CYT', name: 'Ciencia y Tecnología' },
  { code: 'SOC', name: 'Personal Social' },
  { code: 'ART', name: 'Arte y Cultura' },
]

export function TeacherAuthoringView({ teacherName, schoolName, onLogout, onSwitchToStudentView }: Props) {
  const [routes, setRoutes] = useState<TeacherRouteSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedRouteId, setSelectedRouteId] = useState<string | null>(null)
  const [activeRouteDetail, setActiveRouteDetail] = useState<TeacherRouteDetail | null>(null)
  const [selectedNode, setSelectedNode] = useState<TeacherNode | null>(null)

  // Modales
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [showAddNodeModal, setShowAddNodeModal] = useState(false)

  // Formulario nueva ruta
  const [newTitle, setNewTitle] = useState('')
  const [newDescription, setNewDescription] = useState('')
  const [newGrade, setNewGrade] = useState<number>(3)
  const [newArea, setNewArea] = useState<string>('COM')
  const [savingRoute, setSavingRoute] = useState(false)

  // Formulario nueva parada
  const [newNodeTitle, setNewNodeTitle] = useState('')
  const [newNodeInstructions, setNewNodeInstructions] = useState('')
  const [newNodeMinutes, setNewNodeMinutes] = useState<number>(15)
  const [newNodeType, setNewNodeType] = useState<string>('LECCION')
  const [savingNode, setSavingNode] = useState(false)

  // Notificaciones
  const [banner, setBanner] = useState<{ type: 'success' | 'error'; message: string } | null>(null)

  // Estados de reorganización y Drag & Drop (HU-06)
  const [orderedNodes, setOrderedNodes] = useState<TeacherNode[]>([])
  const [isReorderDirty, setIsReorderDirty] = useState(false)
  const [savingReorder, setSavingReorder] = useState(false)
  const [draggedIndex, setDraggedIndex] = useState<number | null>(null)
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null)

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

  async function handleCreateRoute(e: React.FormEvent) {
    e.preventDefault()
    if (!newTitle.trim()) return

    setSavingRoute(true)
    setBanner(null)

    try {
      const res = await createTeacherRoute({
        title: newTitle.trim(),
        description: newDescription.trim() || undefined,
        gradeNumber: newGrade,
        areaCode: newArea,
      })

      setShowCreateModal(false)
      setNewTitle('')
      setNewDescription('')
      setBanner({ type: 'success', message: '¡Nuevo roadmap escolar creado! Ahora agrega paradas y materiales.' })
      speakText('Roadmap creado. Ahora puedes estructurar las paradas pedagógicas y adjuntar tus materiales.')

      await loadRoutes()
      await handleOpenRouteDetail(res.routeId)
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error creando el roadmap.'
      setBanner({ type: 'error', message: msg })
    } finally {
      setSavingRoute(false)
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
      speakText('Secuencia de aprendizaje guardada exitosamente.')
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
      speakText('¡Felicitaciones! El roadmap ha sido publicado y ya está disponible para tus estudiantes.')

      const detail = await fetchTeacherRouteDetail(selectedRouteId)
      setActiveRouteDetail(detail)
      loadRoutes()
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'No se pudo publicar la ruta.'
      setBanner({ type: 'error', message: msg })
    }
  }

  return (
    <div className="teacher-authoring-view">
      {/* 1. Barra de cabecera del docente */}
      <header className="teacher-header">
        <div className="teacher-header-lead">
          <div className="teacher-brand-badge">
            <GraduationCap size={24} className="teacher-icon" aria-hidden="true" />
            <div className="teacher-info-text">
              <span className="teacher-role-tag">PANEL DOCENTE · GESTIÓN Y AUTORÍA</span>
              <h1 className="teacher-title">{teacherName}</h1>
              <span className="teacher-school">{schoolName}</span>
            </div>
          </div>
        </div>

        <div className="teacher-header-actions">
          <button
            type="button"
            className="teacher-action-btn secondary"
            onClick={onSwitchToStudentView}
            title="Ver aula virtual de estudiantes"
          >
            <Eye size={18} aria-hidden="true" />
            <span>Vista de Estudiante</span>
          </button>
          <button
            type="button"
            className="teacher-action-btn danger"
            onClick={onLogout}
            title="Cerrar sesión"
          >
            <LogOut size={18} aria-hidden="true" />
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
                <span>+ Diseñar Nuevo Roadmap</span>
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
                          {r.state === 'PUBLICADO' ? '✓ Publicado' : '✎ Borrador'}
                        </span>
                        <span className="grade-badge">{r.gradeName}</span>
                      </div>

                      <h4 className="card-route-title">{r.title}</h4>
                      <p className="card-route-desc">
                        {r.description || 'Sin descripción curricular.'}
                      </p>

                      <div className="card-meta-row">
                        <span className="area-pill">{r.areaName}</span>
                        <span className="stops-count">
                          <MapPin size={14} aria-hidden="true" />
                          <strong>{r.totalNodes}</strong> paradas
                        </span>
                      </div>

                      <div className="card-actions-row">
                        <button
                          type="button"
                          className="open-editor-btn"
                          onClick={() => handleOpenRouteDetail(r.routeId)}
                        >
                          <BookOpen size={16} aria-hidden="true" />
                          <span>Editar y Adjuntar Materiales</span>
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
                  {activeRouteDetail.state === 'PUBLICADO' ? '✓ Publicado para Alumnos' : '✎ En Borrador'}
                </span>
                {activeRouteDetail.state !== 'PUBLICADO' && (
                  <button
                    type="button"
                    className="publish-cta-btn"
                    onClick={handlePublishRoute}
                  >
                    <Send size={16} aria-hidden="true" />
                    <span>Publicar para mis Estudiantes</span>
                  </button>
                )}
              </div>
            </div>

            {/* Cabecera del roadmap activo */}
            <div className="editor-header-card">
              <div className="editor-header-info">
                <div className="editor-tags-row">
                  <span className="area-tag">{activeRouteDetail.area.name}</span>
                  <span className="grade-tag">{activeRouteDetail.grade.name}</span>
                </div>
                <h2 className="editor-title">{activeRouteDetail.title}</h2>
                <p className="editor-desc">{activeRouteDetail.description}</p>
              </div>

              <button
                type="button"
                className="add-node-btn"
                onClick={() => setShowAddNodeModal(true)}
              >
                <Plus size={18} aria-hidden="true" />
                <span>+ Agregar Parada al Mapa</span>
              </button>
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
                  <span className="stops-hint-badge">Arrastra ⋮⋮ para ordenar</span>
                </div>

                <p className="stops-panel-hint">
                  Reorganiza el orden de aprendizaje arrastrando las paradas o usando las flechas de subida y bajada.
                </p>

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
                    <p>Esta ruta no tiene paradas. Toca "+ Agregar Parada al Mapa" para comenzar.</p>
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
                              <span className="stop-attachments">
                                <FileText size={12} aria-hidden="true" />
                                {n.resources.length} recursos
                              </span>
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
                          </div>
                        </div>
                      )
                    })}
                  </div>
                )}
              </div>

              {/* Columna Derecha: Materiales y Recursos de la parada seleccionada */}
              <div className="editor-materials-panel">
                {selectedNode ? (
                  <MaterialUploadZone
                    nodeId={selectedNode.id}
                    nodeTitle={selectedNode.title}
                    resources={selectedNode.resources}
                    onResourceAttached={handleResourceAttached}
                  />
                ) : (
                  <div className="select-stop-prompt">
                    <Sparkles size={32} className="sparkle-icon" aria-hidden="true" />
                    <h4>Selecciona una parada</h4>
                    <p>Elige una parada del camino a la izquierda para subir fichas de lectura, diapositivas y enlaces web.</p>
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
              <h3>Diseñar Nuevo Roadmap Escolar</h3>
              <button
                type="button"
                className="close-modal-btn"
                onClick={() => setShowCreateModal(false)}
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateRoute} className="modal-form">
              <div className="form-group">
                <label htmlFor="route-title">Título del Roadmap *</label>
                <input
                  id="route-title"
                  type="text"
                  className="modal-input"
                  placeholder="Ej. Aventuras Matemáticas en el Valle de Huanta"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="route-desc">Descripción pedagógica</label>
                <textarea
                  id="route-desc"
                  className="modal-textarea"
                  rows={3}
                  placeholder="Explica qué competencias y retos desarrollarán tus estudiantes en esta ruta."
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                />
              </div>

              <div className="form-row">
                <div className="form-group flex-1">
                  <label htmlFor="route-grade">Grado Escolar *</label>
                  <select
                    id="route-grade"
                    className="modal-select"
                    value={newGrade}
                    onChange={(e) => setNewGrade(Number(e.target.value))}
                  >
                    {[1, 2, 3, 4, 5, 6].map((g) => (
                      <option key={g} value={g}>{g}.° de primaria</option>
                    ))}
                  </select>
                </div>

                <div className="form-group flex-1">
                  <label htmlFor="route-area">Área Curricular (CNEB) *</label>
                  <select
                    id="route-area"
                    className="modal-select"
                    value={newArea}
                    onChange={(e) => setNewArea(e.target.value)}
                  >
                    {CNEB_AREAS.map((a) => (
                      <option key={a.code} value={a.code}>{a.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="modal-actions">
                <button
                  type="button"
                  className="modal-cancel-btn"
                  onClick={() => setShowCreateModal(false)}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="modal-save-btn"
                  disabled={savingRoute || !newTitle.trim()}
                >
                  {savingRoute ? 'Creando…' : 'Crear Roadmap'}
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
    </div>
  )
}
