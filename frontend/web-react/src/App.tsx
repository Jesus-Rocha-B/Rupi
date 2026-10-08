import { useEffect, useRef, useState } from 'react'
import {
  AlertTriangle,
  ArrowRight,
  Bird,
  BookOpen,
  Calculator,
  Check,
  Compass,
  FlaskConical,
  GraduationCap,
  Heart,
  House,
  Inbox,
  Lock,
  LogOut,
  RefreshCw,
  Sparkles,
  Star,
  Volume2,
  X,
} from 'lucide-react'
import { Roadmap } from './features/roadmap/Roadmap'
import { RoadmapSkeleton } from './features/roadmap/RoadmapSkeleton'
import {
  ApiError,
  fetchRouteDetail,
  fetchStudentRoutes,
  fetchStudentBadges,
  fetchSession,
  fetchSessionOptions,
  login,
  loginDemo,
  logout,
  startActivity,
  type ActivityCompleteResult,
  type ActivityContent,
  type BadgeDto,
  type RouteDetail,
  type RouteNode,
  type RouteSummary,
  type Student,
} from './services/learningRoutesApi'
import { nodeStateLabels } from './features/roadmap/nodeStates'
import { RouteOverview } from './components/RouteOverview'
import { RouteCompanion } from './components/RouteCompanion'
import { LoginView } from './features/auth/LoginView'
import { GradeSelectView } from './features/auth/GradeSelectView'
import { RupiCharacter } from './components/RupiCharacter'
import { ActivityBlocks } from './features/roadmap/ActivityBlocks'
import { speakText } from './utils/speech'
import './App.css'

const guideMessages = [
  'Cada parada muestra el avance que has guardado en tu ruta.',
  'Consulta los estados de tus actividades: disponible, en curso, completada o bloqueada.',
  '¡Tú marcas el ritmo! Cada paso cuenta.',
]
type Failure = 'NETWORK' | 'UNAUTHORIZED' | 'EMPTY' | 'NOT_FOUND' | null

function getCourseLabel(courseCode: string | null) {
  if (courseCode === 'COMUNICACION') return 'Comunicación'
  if (courseCode === 'MATEMATICA') return 'Matemática'
  if (courseCode === 'CYT') return 'Ciencia y Tecnología'
  return 'Aventura escolar'
}

function App() {
  const [student, setStudent] = useState<Student | null>(null)
  const [demoAvailable, setDemoAvailable] = useState(false)
  const [routes, setRoutes] = useState<RouteSummary[]>([])
  const [badges, setBadges] = useState<BadgeDto[]>([])
  const [routeId, setRouteId] = useState('')
  const [selectedRoute, setSelectedRoute] = useState<RouteDetail | null>(null)
  const [selectedNode, setSelectedNode] = useState<RouteNode | null>(null)
  const [activity, setActivity] = useState<ActivityContent | null>(null)
  const [activityLoading, setActivityLoading] = useState(false)
  const [activityError, setActivityError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [errorType, setErrorType] = useState<Failure>(null)
  const [guideIndex, setGuideIndex] = useState(0)
  const [reloadKey, setReloadKey] = useState(0)
  const [sessionBusy, setSessionBusy] = useState(false)
  const [selectedGrade, setSelectedGrade] = useState<number | null>(() => {
    const saved = localStorage.getItem('rupi_selected_grade')
    return saved ? parseInt(saved, 10) : 2
  })
  const [selectedCourse, setSelectedCourse] = useState<string | null>(() => {
    return localStorage.getItem('rupi_selected_course') || 'MATEMATICA'
  })
  const dialog = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    if (!selectedNode || !dialog.current) return
    const previous = document.activeElement as HTMLElement | null
    dialog.current.showModal()
    return () => previous?.focus()
  }, [selectedNode])

  async function handleOpenNode(node: RouteNode) {
    if (!selectedRoute) return
    setSelectedNode(node)
    setActivity(null)
    setActivityLoading(true)
    setActivityError(null)
    try {
      const act = await startActivity(selectedRoute.versionRouteId, node.id)
      setActivity(act)
      if (node.state === 'DISPONIBLE') {
        setSelectedRoute(prev => {
          if (!prev) return prev
          return {
            ...prev,
            nodes: prev.nodes.map(n => n.id === node.id ? { ...n, state: 'EN_CURSO' } : n)
          }
        })
      }
    } catch {
      setActivityError('No se pudo abrir la lección. Revisa tu conexión a internet.')
    } finally {
      setActivityLoading(false)
    }
  }

  function handleActivityCompleted(result: ActivityCompleteResult) {
    if (!selectedRoute) return
    if (result.badgeEarned) {
      setBadges(prev => {
        if (prev.some(b => b.code === result.badgeEarned!.code)) return prev
        return [...prev, result.badgeEarned!]
      })
    }
    setSelectedRoute(prev => {
      if (!prev) return prev
      const updatedNodes = prev.nodes.map(n => {
        if (n.id === result.nodeId) {
          return { ...n, state: 'COMPLETADO' as const }
        }
        if (result.nextUnlockedNode && n.id === result.nextUnlockedNode.id && n.state === 'BLOQUEADO') {
          return { ...n, state: 'DISPONIBLE' as const }
        }
        return n
      })
      const completedCount = updatedNodes.filter(n => n.state === 'COMPLETADO').length
      return {
        ...prev,
        nodes: updatedNodes,
        progress: {
          ...prev.progress,
          completedNodes: completedCount,
        },
        curriculum: prev.curriculum ? {
          ...prev.curriculum,
          units: prev.curriculum.units.map(unit => ({
            ...unit,
            completedNodes: unit.nodes.filter(un =>
              un.id === result.nodeId ? true : updatedNodes.find(n => n.id === un.id)?.state === 'COMPLETADO'
            ).length,
            nodes: unit.nodes.map(un => {
              const matching = updatedNodes.find(n => n.id === un.id)
              return matching ? { ...un, state: matching.state } : un
            })
          }))
        } : null
      }
    })
    setRoutes(prevList =>
      prevList.map(r =>
        r.versionRouteId === selectedRoute.versionRouteId
          ? { ...r, completedNodes: r.completedNodes + (result.experienceEarned > 0 ? 1 : 0) }
          : r
      )
    )
  }

  useEffect(() => {
    const controller = new AbortController()
    const signal = controller.signal
    async function load() {
      try {
        const options = await fetchSessionOptions(signal)
        if (signal.aborted) return
        setDemoAvailable(options.demoAvailable)
        const current = await fetchSession(signal)
        const list = await fetchStudentRoutes(signal)
        if (signal.aborted) return
        setStudent(current)
        setRoutes(list)
        if (!list.length) {
          setErrorType('EMPTY')
          setLoading(false)
          return
        }
        const id = list.some(route => route.versionRouteId === routeId)
          ? routeId
          : list[0].versionRouteId
        const detail = await fetchRouteDetail(id, signal)
        if (signal.aborted) return
        setSelectedRoute(detail)
        setErrorType(null)
        try {
          const earnedBadges = await fetchStudentBadges(signal)
          if (!signal.aborted) setBadges(earnedBadges)
        } catch {
          // Si no hay insignias o falla la consulta, no interrumpe el aula
        }
        setLoading(false)
      } catch (error) {
        if (signal.aborted) return
        const status = error instanceof ApiError ? error.status : 0
        setErrorType(status === 401 ? 'UNAUTHORIZED' : status === 404 ? 'NOT_FOUND' : 'NETWORK')
        if (status === 401) {
          setStudent(null)
          setRoutes([])
          setSelectedRoute(null)
          setSelectedGrade(null)
          setSelectedCourse(null)

        }
        setSelectedRoute(null)
        setLoading(false)
      }
    }
    void load()
    return () => controller.abort()
  }, [routeId, reloadKey, selectedGrade, selectedCourse])

  // Restricción estricta de acceso al roadmap:
  // Solo 2.° de primaria Matemática tiene acceso a su roadmap
  const isRoadmapAvailable = selectedGrade === 2 && selectedCourse === 'MATEMATICA'

  function resetView() {
    setLoading(true)
    setErrorType(null)
    setSelectedRoute(null)
    setSelectedNode(null)
  }
  function retry() {
    resetView()
    setReloadKey(value => value + 1)
  }

  function handleSelectGradeAndCourse(gradeNumber: number | null, courseCode: string | null) {
    setSelectedGrade(gradeNumber)
    setSelectedCourse(courseCode)
    if (gradeNumber != null) {
      localStorage.setItem('rupi_selected_grade', String(gradeNumber))
    } else {
      localStorage.removeItem('rupi_selected_grade')
    }
    if (courseCode != null) {
      localStorage.setItem('rupi_selected_course', courseCode)
    } else {
      localStorage.removeItem('rupi_selected_course')
    }
  }

  async function handleLogout() {
    setSessionBusy(true)
    resetView()
    try {
      await logout()
      setStudent(null)
      setRoutes([])
      setBadges([])
      setRouteId('')
      setSelectedGrade(null)
      setSelectedCourse(null)

      setErrorType('UNAUTHORIZED')
      setReloadKey(value => value + 1)
    } catch {
      setErrorType('NETWORK')
      setLoading(false)
    } finally {
      setSessionBusy(false)
    }
  }

  async function handleLogin(username: string, password: string) {
    setSessionBusy(true)
    setErrorType(null)
    try {
      const current = await login(username, password)
      resetView()
      setStudent(current)
      setRoutes([]); setRouteId(''); setSelectedGrade(null); setSelectedCourse(null)
      setReloadKey(value => value + 1)
    } catch (error) {
      setLoading(false)
      throw error
    } finally {
      setSessionBusy(false)
    }
  }

  async function handleLoginDemo() {
    setSessionBusy(true)
    try {
      const current = await loginDemo()
      resetView()
      setStudent(current)
      setRoutes([]); setRouteId(''); setSelectedGrade(null); setSelectedCourse(null)
      setReloadKey(value => value + 1)
    } catch {
      setErrorType('NETWORK')
      setLoading(false)
    } finally {
      setSessionBusy(false)
    }
  }

  // Estado de carga inicial
  if (loading) {
    return (
      <div className="app-loading-screen" aria-busy="true">
        <p role="status">Cargando tu aula y tu progreso…</p>
        <RoadmapSkeleton />
      </div>
    )
  }

  // 1. Pantalla de inicio de sesión: sin barra de navegación escolar
  if (!student || errorType === 'UNAUTHORIZED') {
    return (
      <LoginView
        onLogin={handleLogin}
        onLoginDemo={handleLoginDemo}
        sessionBusy={sessionBusy}
        demoAvailable={demoAvailable}
        serviceError={errorType === 'NETWORK' ? 'No pudimos conectar con tu aula. Comprueba que el servidor esté disponible y vuelve a intentar.' : null}
      />
    )
  }

  // 2. Pantalla de selección de grado y materias de Primaria: aislada y enfocada
  if (selectedGrade === null || selectedCourse === null) {
    return (
      <GradeSelectView
        student={student}
        routes={routes}
        initialGrade={selectedGrade}
        onSelectGradeAndCourse={(grade, course) => handleSelectGradeAndCourse(grade, course)}
        onLogout={handleLogout}
      />
    )
  }

  // 3. Aula escolar con barra de navegación y control de acceso a roadmaps
  return (
    <div className="app-shell">
      <a href="#main-content" className="skip-link">
        Saltar al contenido principal
      </a>
      <aside className="sidebar">
        <a className="brand" href="#main-content" aria-label="Rupi, inicio">
          <img className="brand-logo" src="/rupi-logo.jpg" alt="" />
          <span>
            rupi<span className="brand-dot">.</span>
          </span>
        </a>
        <div className="school-label">MI ESPACIO</div>
        <nav className="main-nav" aria-label="Navegación principal">
          <a className="nav-item selected" href="#main-content" aria-current="page">
            <House aria-hidden="true" />Mi aula
          </a>
          <a className="nav-item" href="#mis-rutas">
            <Compass aria-hidden="true" />Mis aventuras
          </a>
        </nav>
        <div className="sidebar-bottom">
          <div className="help-bubble">
            ¿Necesitas otra ruta?
            <br />
            <span>Consulta con tu docente.</span>
            <div className="help-face">
              <Bird aria-hidden="true" />
            </div>
          </div>
          <div className="profile-top-group">
            <div
              className="profile-student-card"
              title={`Estudiante: ${student.name} · ${student.schoolName || student.institutionName || 'I.E. 38001 Mariscal Sucre'}`}
            >
              <span className="avatar small" aria-hidden="true">
                {student.name.charAt(0)}
              </span>
              <span className="profile-text-col">
                <b className="profile-student-name">{student.name}</b>
                <small className="profile-school-name">
                  {student.schoolName || student.institutionName || 'I.E. 38001 Mariscal Sucre'}
                </small>
              </span>
            </div>
            <button
              className="profile-logout-btn"
              type="button"
              disabled={loading || sessionBusy}
              onClick={() => void handleLogout()}
              aria-label={`Cerrar sesión de ${student.name}`}
              title="Cerrar sesión"
            >
              <LogOut size={15} aria-hidden="true" />
              <span>Salir</span>
            </button>
          </div>
        </div>
      </aside>

      <main className="main-content" id="main-content">
        <section className="course-heading" aria-labelledby="course-title">
          <div>
            <div className="eyebrow">Tu espacio para descubrir</div>
            <h1 id="course-title">
              Mi ruta de aprendizaje
              <span className="current-grade-badge">
                <GraduationCap size={16} aria-hidden="true" />
                <span>{`${selectedGrade}.° de primaria · ${getCourseLabel(selectedCourse)}`}</span>
                <button
                  type="button"
                  className="change-grade-btn"
                  onClick={() => {
                    setSelectedCourse(null)
                    localStorage.removeItem('rupi_selected_course')
                  }}
                  title="Cambiar de materia"
                  aria-label="Cambiar de materia escolar"
                >
                  Cambiar materia
                </button>
                <button
                  type="button"
                  className="change-grade-btn"
                  onClick={() => {
                    setSelectedGrade(null)
                    setSelectedCourse(null)
                    localStorage.removeItem('rupi_selected_grade')
                    localStorage.removeItem('rupi_selected_course')
                  }}
                  title="Cambiar de grado de primaria"
                  aria-label="Cambiar de grado de primaria"
                >
                  Cambiar grado
                </button>
              </span>
            </h1>
            <p>Un mundo por aprender. A tu propio ritmo.</p>
          </div>
        </section>

        {/* Barra interactiva de cambio de materia escolar:
            Permite alternar entre Comunicación, Matemática y CYT en todo momento */}
        <nav className="course-switch-bar" aria-label="Cambiar de materia">
          <button
            type="button"
            className={`course-switch-tab ${selectedCourse === 'COMUNICACION' ? 'active comunicacion' : ''}`}
            onClick={() => handleSelectGradeAndCourse(selectedGrade, 'COMUNICACION')}
            aria-pressed={selectedCourse === 'COMUNICACION'}
            title="Cambiar a Comunicación"
          >
            <BookOpen size={16} aria-hidden="true" />
            <span>Comunicación</span>
            <span className="tab-status-pill locked" title="Sin roadmap activo">
              <Lock size={11} aria-hidden="true" />
              <span>Sin mapa</span>
            </span>
          </button>

          <button
            type="button"
            className={`course-switch-tab ${selectedCourse === 'MATEMATICA' ? 'active matematica' : ''}`}
            onClick={() => handleSelectGradeAndCourse(selectedGrade, 'MATEMATICA')}
            aria-pressed={selectedCourse === 'MATEMATICA'}
            title="Cambiar a Matemática"
          >
            <Calculator size={16} aria-hidden="true" />
            <span>Matemática</span>
            {selectedGrade === 2 ? (
              <span className="tab-status-pill active" title="Roadmap activo en Ayacucho">
                <Sparkles size={11} aria-hidden="true" />
                <span>Mapa activo</span>
              </span>
            ) : (
              <span className="tab-status-pill locked" title="Sin roadmap activo">
                <Lock size={11} aria-hidden="true" />
                <span>Sin mapa</span>
              </span>
            )}
          </button>

          <button
            type="button"
            className={`course-switch-tab ${selectedCourse === 'CYT' ? 'active cyt' : ''}`}
            onClick={() => handleSelectGradeAndCourse(selectedGrade, 'CYT')}
            aria-pressed={selectedCourse === 'CYT'}
            title="Cambiar a Ciencia y Tecnología"
          >
            <FlaskConical size={16} aria-hidden="true" />
            <span>Ciencia y Tecnología</span>
            <span className="tab-status-pill locked" title="Sin roadmap activo">
              <Lock size={11} aria-hidden="true" />
              <span>Sin mapa</span>
            </span>
          </button>
        </nav>

        {/* CASO A: 2.° de Primaria Matemática -> Roadmap Habilitado */}
        {isRoadmapAvailable && (
          <>
            {routes.length > 1 && (
              <section id="mis-rutas" className="route-picker" aria-label="Rutas inscritas">
                <label htmlFor="route-select">Mis aventuras inscritas</label>
                <select
                  id="route-select"
                  value={selectedRoute?.versionRouteId ?? routeId}
                  disabled={loading || sessionBusy}
                  onChange={event => {
                    resetView()
                    setRouteId(event.target.value)
                  }}
                >
                  {!selectedRoute && !routeId && <option value="" disabled>Cargando ruta…</option>}
                  {routes.map(route => (
                    <option key={route.versionRouteId} value={route.versionRouteId}>
                      {route.grade.name} · {route.area.name} · {route.title} ({route.completedNodes}/{route.totalNodes})
                    </option>
                  ))}
                </select>
              </section>
            )}

            {selectedRoute && (
              <RouteOverview
                route={selectedRoute}
                name={student.name}
                badges={badges}
                onSelect={handleOpenNode}
              />
            )}

            {errorType === 'NETWORK' && (
              <section className="status-panel" role="alert">
                <div className="status-panel-icon error">
                  <AlertTriangle aria-hidden="true" />
                </div>
                <h2>No pudimos consultar tu progreso</h2>
                <p>Tu avance sigue guardado. Espera un momento y vuelve a intentarlo.</p>
                <button className="status-action-btn" onClick={retry}>
                  <RefreshCw aria-hidden="true" />Reintentar
                </button>
              </section>
            )}

            {errorType === 'NOT_FOUND' && (
              <section className="status-panel" role="alert">
                <h2>Esta ruta ya no está disponible</h2>
                <p>Puede haber cambiado tu inscripción. Consulta tus rutas de nuevo.</p>
                <button
                  className="status-action-btn"
                  onClick={() => {
                    resetView()
                    setRouteId('')
                    setReloadKey(value => value + 1)
                  }}
                >
                  Volver a mis rutas
                </button>
              </section>
            )}

            {errorType === 'EMPTY' && (
              <section className="status-panel" role="status">
                <div className="status-panel-icon empty">
                  <Inbox aria-hidden="true" />
                </div>
                <h2>Aún no tienes rutas asignadas</h2>
                <p>Pide a tu docente que te inscriba en una ruta para comenzar tu aventura.</p>
                <button className="status-action-btn" onClick={retry}>
                  <RefreshCw aria-hidden="true" />Consultar mis inscripciones
                </button>
              </section>
            )}

            {selectedRoute && (
              selectedRoute.nodes.length ? (
                <div className="adventure-layout">
                  <Roadmap
                    route={selectedRoute}
                    guideMessage={guideMessages[guideIndex]}
                    onGuideClick={() => setGuideIndex(index => (index + 1) % guideMessages.length)}
                    onSelectNode={handleOpenNode}
                  />
                  <RouteCompanion route={selectedRoute} onSelect={handleOpenNode} />
                </div>
              ) : (
                <section className="status-panel" role="status">
                  <h2>Esta ruta aún no tiene actividades</h2>
                  <p>Consulta con tu docente o selecciona otra ruta.</p>
                </section>
              )
            )}
          </>
        )}

        {/* CASO B: Otra materia u otro grado -> Roadmap NO accesible (Permite ver curso, no entrar al roadmap) */}
        {!isRoadmapAvailable && (
          <section className="course-locked-panel" role="region" aria-labelledby="locked-course-title">
            <div className="course-locked-card">
              <div className="course-locked-mascot" aria-hidden="true">
                <RupiCharacter
                  role={
                    selectedCourse === 'COMUNICACION'
                      ? 'reader'
                      : selectedCourse === 'CYT'
                      ? 'scientist'
                      : 'default'
                  }
                  mood="thinking"
                  className="locked-rupi-svg"
                />
              </div>

              <div className="course-locked-content">
                <div className="course-locked-tag">
                  <Lock size={15} aria-hidden="true" />
                  <span>ACCESO A ROADMAP RESTRINGIDO</span>
                </div>

                <h2 id="locked-course-title">
                  {selectedGrade === 2
                    ? `El roadmap de ${getCourseLabel(selectedCourse)} aún no está disponible`
                    : `Los roadmaps para ${selectedGrade}.° de primaria están en preparación`
                  }
                </h2>

                <p className="locked-course-desc">
                  {selectedGrade === 2
                    ? `¡Hola, ${student.name}! Puedes cambiar de materia libremente para revisar tus áreas de clase, pero actualmente el mapa de ruta interactivo con paradas y retos solo está habilitado para 2.° de primaria · Matemática.`
                    : `¡Hola, ${student.name}! Puedes explorar las áreas escolares de ${selectedGrade}.° de primaria, pero las rutas interactivas con el gallito Rupi se encuentran en preparación con tu docente. La única aventura activa es 2.° de primaria · Matemática.`
                  }
                </p>

                <div className="course-locked-actions">
                  <button
                    type="button"
                    className="go-to-math-btn"
                    onClick={() => handleSelectGradeAndCourse(2, 'MATEMATICA')}
                    title="Ingresar al roadmap activo de 2.° de primaria Matemática"
                  >
                    <Compass size={18} aria-hidden="true" />
                    <span>Ingresar al roadmap de 2.° de primaria · Matemática</span>
                    <ArrowRight size={18} aria-hidden="true" />
                  </button>

                  <button
                    type="button"
                    className="change-course-alt-btn"
                    onClick={() => {
                      setSelectedCourse(null)
                      localStorage.removeItem('rupi_selected_course')
                    }}
                    title="Elegir otra materia"
                  >
                    <span>Elegir otra materia</span>
                  </button>

                  <button
                    type="button"
                    className="locked-speech-btn"
                    onClick={() => speakText(
                      selectedGrade === 2
                        ? `El roadmap de ${getCourseLabel(selectedCourse)} para 2.° de primaria aún no está disponible. Tu ruta activa te espera en 2.° de primaria de Matemática.`
                        : `Los roadmaps para ${selectedGrade} grado de primaria aún no están disponibles. Solo puedes ingresar al mapa de 2.° de primaria de Matemática.`
                    )}
                    aria-label="Escuchar explicación de Rupi"
                    title="Escuchar explicación"
                  >
                    <Volume2 size={18} aria-hidden="true" />
                    <span>Escuchar</span>
                  </button>
                </div>
              </div>
            </div>
          </section>
        )}

        <footer className="page-footer">
          Hecho para aprender, crecer y descubrir <Heart aria-label="con cariño" />
        </footer>
      </main>

      {selectedNode && (
        <dialog
          ref={dialog}
          className="lesson-modal"
          aria-labelledby="modal-title"
          aria-describedby="modal-desc"
          onCancel={() => setSelectedNode(null)}
          onClick={event => {
            if (event.target === event.currentTarget) setSelectedNode(null)
          }}
        >
          {activityLoading ? (
            <div className="activity-loading-state" role="status" aria-live="polite">
              <div className="activity-loading-mascot" aria-hidden="true">
                <RupiCharacter mood="talking" className="loading-rupi-svg" />
              </div>
              <p className="activity-loading-text">Preparando tu lección... ¡Ya casi estamos listos!</p>
            </div>
          ) : activityError ? (
            <div className="activity-error-state" role="alert">
              <button
                className="modal-close"
                type="button"
                autoFocus
                onClick={() => setSelectedNode(null)}
                aria-label="Cerrar ventana"
              >
                <X aria-hidden="true" />
              </button>
              <AlertTriangle className="activity-error-icon" size={40} aria-hidden="true" />
              <h3>¡Uy! Ocurrió un inconveniente</h3>
              <p>{activityError}</p>
              <div className="activity-error-actions">
                <button
                  type="button"
                  className="activity-retry-btn"
                  onClick={() => handleOpenNode(selectedNode)}
                >
                  <RefreshCw size={16} aria-hidden="true" />
                  <span>Volver a intentar</span>
                </button>
                <button
                  type="button"
                  className="activity-cancel-btn"
                  onClick={() => setSelectedNode(null)}
                >
                  <span>Cerrar</span>
                </button>
              </div>
            </div>
          ) : activity ? (
            <ActivityBlocks
              activity={activity}
              routeId={selectedRoute?.versionRouteId ?? ''}
              onComplete={handleActivityCompleted}
              onClose={() => setSelectedNode(null)}
            />
          ) : (
            <>
              <button
                className="modal-close"
                autoFocus
                onClick={() => setSelectedNode(null)}
                aria-label="Cerrar detalle"
              >
                <X aria-hidden="true" />
              </button>
              <div className="modal-icon">
                {selectedNode.state === 'COMPLETADO' ? (
                  <Check aria-hidden="true" />
                ) : (
                  <Star aria-hidden="true" />
                )}
              </div>
              <span className="eyebrow">
                PARADA #{selectedNode.sequence} · {nodeStateLabels[selectedNode.state]}
              </span>
              <div className="modal-title-row">
                <h2 id="modal-title">{selectedNode.title}</h2>
                <button
                  type="button"
                  className="modal-listen-btn"
                  onClick={() =>
                    speakText(
                      `Parada ${selectedNode.sequence}: ${selectedNode.title}. ${nodeStateLabels[selectedNode.state]}`
                    )
                  }
                  aria-label="Escuchar título"
                  title="Escuchar"
                >
                  <Volume2 size={18} aria-hidden="true" />
                </button>
              </div>
              <p id="modal-desc">
                {selectedNode.activityType}
                {selectedNode.estimatedMinutes != null
                  ? ` · ${selectedNode.estimatedMinutes} minutos aprox.`
                  : ''}
                . Aquí puedes consultar el progreso guardado de esta actividad.
              </p>
              <button className="start-button" onClick={() => setSelectedNode(null)}>
                Volver al mapa <ArrowRight aria-hidden="true" />
              </button>
            </>
          )}
        </dialog>
      )}
    </div>
  )
}

export default App
