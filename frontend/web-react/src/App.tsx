import { useEffect, useRef, useState } from 'react'
import { AlertTriangle, ArrowRight, Bird, Check, Compass, GraduationCap, Heart, House, Inbox, Lock, RefreshCw, Star, Volume2, X } from 'lucide-react'
import { Roadmap } from './features/roadmap/Roadmap'
import { RoadmapSkeleton } from './features/roadmap/RoadmapSkeleton'
import { ApiError, fetchRouteDetail, fetchStudentRoutes, fetchSession, fetchSessionOptions, login, loginDemo, logout, type RouteDetail, type RouteNode, type RouteSummary, type Student } from './services/learningRoutesApi'
import { nodeStateLabels } from './features/roadmap/nodeStates'
import { RouteOverview } from './components/RouteOverview'
import { RouteCompanion } from './components/RouteCompanion'
import { LoginView } from './features/auth/LoginView'
import { GradeSelectView } from './features/auth/GradeSelectView'
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
  const [routeId, setRouteId] = useState('')
  const [selectedRoute, setSelectedRoute] = useState<RouteDetail | null>(null)
  const [selectedNode, setSelectedNode] = useState<RouteNode | null>(null)
  const [loading, setLoading] = useState(true)
  const [errorType, setErrorType] = useState<Failure>(null)
  const [guideIndex, setGuideIndex] = useState(0)
  const [reloadKey, setReloadKey] = useState(0)
  const [sessionBusy, setSessionBusy] = useState(false)
  const dialog = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    if (!selectedNode || !dialog.current) return
    const previous = document.activeElement as HTMLElement | null
    dialog.current.showModal()
    return () => previous?.focus()
  }, [selectedNode])

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
        if (!list.length) { setErrorType('EMPTY'); setLoading(false); return }
        const id = list.some(route => route.versionRouteId === routeId) ? routeId : list[0].versionRouteId
        const detail = await fetchRouteDetail(id, signal)
        if (signal.aborted) return
        setSelectedRoute(detail)
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
          localStorage.removeItem('rupi_selected_grade')
          localStorage.removeItem('rupi_selected_course')
        }
        setSelectedRoute(null)
        setLoading(false)
      }
    }
    void load()
    return () => controller.abort()
  }, [routeId, reloadKey])

  const [selectedGrade, setSelectedGrade] = useState<number | null>(() => {
    const saved = localStorage.getItem('rupi_selected_grade')
    return saved ? Number(saved) : null
  })

  const [selectedCourse, setSelectedCourse] = useState<string | null>(() => {
    return localStorage.getItem('rupi_selected_course')
  })

  function resetView() {
    setLoading(true)
    setErrorType(null)
    setSelectedRoute(null)
    setSelectedNode(null)
  }
  function retry() { resetView(); setReloadKey(value => value + 1) }

  function handleSelectGradeAndCourse(gradeNumber: number, courseCode: string) {
    setSelectedGrade(gradeNumber)
    setSelectedCourse(courseCode)
    localStorage.setItem('rupi_selected_grade', String(gradeNumber))
    localStorage.setItem('rupi_selected_course', courseCode)
  }

  async function handleLogout() {
    setSessionBusy(true)
    resetView()
    try {
      await logout()
      setStudent(null)
      setRoutes([])
      setRouteId('')
      setSelectedGrade(null)
      setSelectedCourse(null)
      localStorage.removeItem('rupi_selected_grade')
      localStorage.removeItem('rupi_selected_course')
      setErrorType('UNAUTHORIZED')
      setReloadKey(value => value + 1)
    } catch {
      setErrorType('NETWORK')
      setLoading(false)
    } finally { setSessionBusy(false) }
  }

  async function handleLogin(username: string, password: string) {
    setSessionBusy(true)
    resetView()
    try {
      await login(username, password)
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
    resetView()
    try {
      await loginDemo()
      setReloadKey(value => value + 1)
    } catch {
      setErrorType('NETWORK')
      setLoading(false)
    } finally { setSessionBusy(false) }
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
      />
    )
  }

  // 2. Pantalla de selección de grado y cursos: aislada y enfocada
  if (selectedGrade === null || selectedCourse === null) {
    return (
      <GradeSelectView
        student={student}
        initialGrade={selectedGrade}
        onSelectGradeAndCourse={handleSelectGradeAndCourse}
        onLogout={handleLogout}
      />
    )
  }

  // 3. Aula y ruta desbloqueadas: con barra de navegación escolar completa
  return (
    <div className="app-shell">
      <a href="#main-content" className="skip-link">Saltar al contenido principal</a>
      <aside className="sidebar">
        <a className="brand" href="#main-content" aria-label="Rupi, inicio"><img className="brand-logo" src="/rupi-logo.jpg" alt="" /><span>rupi<span className="brand-dot">.</span></span></a>
        <div className="school-label">MI ESPACIO</div>
        <nav className="main-nav" aria-label="Navegación principal">
          <a className="nav-item selected" href="#main-content" aria-current="page"><House aria-hidden="true" />Mi ruta</a>
          <a className="nav-item" href="#mis-rutas"><Compass aria-hidden="true" />Mis aventuras</a>
        </nav>
        <div className="sidebar-bottom">
          <div className="help-bubble">¿Necesitas otra ruta?<br /><span>Consulta con tu docente.</span><div className="help-face"><Bird aria-hidden="true" /></div></div>
          <button className="profile-button" type="button" disabled={loading || sessionBusy} onClick={() => void handleLogout()} aria-label={`Cerrar sesión de ${student.name}`}>
            <span className="avatar small" aria-hidden="true">{student.name.charAt(0)}</span>
            <span><b>{student.name}</b><small>{`${selectedGrade}.° sec · ${getCourseLabel(selectedCourse)} · Salir`}</small></span>
          </button>
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
                    <span>{`${selectedGrade}.° de secundaria · ${getCourseLabel(selectedCourse)}`}</span>
                    <button
                      type="button"
                      className="change-grade-btn"
                      onClick={() => {
                        setSelectedCourse(null)
                        localStorage.removeItem('rupi_selected_course')
                      }}
                      title="Cambiar de curso"
                      aria-label="Cambiar de curso escolar"
                    >
                      Cambiar curso
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
                      title="Cambiar de grado"
                      aria-label="Cambiar de grado escolar"
                    >
                      Cambiar grado
                    </button>
                  </span>
                </h1>
                <p>Un mundo por aprender. A tu propio ritmo.</p>
              </div>
            </section>
            {routes.length > 1 && <section id="mis-rutas" className="route-picker" aria-label="Rutas inscritas">
              <label htmlFor="route-select">Mis aventuras inscritas</label><select id="route-select" value={selectedRoute?.versionRouteId ?? routeId} disabled={loading || sessionBusy} onChange={event => { resetView(); setRouteId(event.target.value) }}>
                {!selectedRoute && !routeId && <option value="" disabled>Cargando ruta…</option>}
                {routes.map(route => <option key={route.versionRouteId} value={route.versionRouteId}>{route.grade.name} · {route.area.name} · {route.title} ({route.completedNodes}/{route.totalNodes})</option>)}
              </select>
            </section>}
            {selectedRoute && <RouteOverview route={selectedRoute} name={student.name} onSelect={setSelectedNode} />}
            {errorType === 'NETWORK' && <section className="status-panel" role="alert"><div className="status-panel-icon error"><AlertTriangle aria-hidden="true" /></div><h2>No pudimos consultar tu progreso</h2><p>Tu avance sigue guardado. Espera un momento y vuelve a intentarlo.</p><button className="status-action-btn" onClick={retry}><RefreshCw aria-hidden="true" />Reintentar</button></section>}
            {errorType === 'NOT_FOUND' && <section className="status-panel" role="alert"><h2>Esta ruta ya no está disponible</h2><p>Puede haber cambiado tu inscripción. Consulta tus rutas de nuevo.</p><button className="status-action-btn" onClick={() => { resetView(); setRouteId(''); setReloadKey(value => value + 1) }}>Volver a mis rutas</button></section>}
            {errorType === 'EMPTY' && <section className="status-panel" role="status"><div className="status-panel-icon empty"><Inbox aria-hidden="true" /></div><h2>Aún no tienes rutas asignadas</h2><p>Pide a tu docente que te inscriba en una ruta para comenzar tu aventura.</p><button className="status-action-btn" onClick={retry}><RefreshCw aria-hidden="true" />Consultar mis inscripciones</button></section>}
            {selectedRoute && (selectedRoute.nodes.length ? <div className="adventure-layout"><Roadmap route={selectedRoute} guideMessage={guideMessages[guideIndex]} onGuideClick={() => setGuideIndex(index => (index + 1) % guideMessages.length)} onSelectNode={setSelectedNode} /><RouteCompanion route={selectedRoute} onSelect={setSelectedNode} /></div> : <section className="status-panel" role="status"><h2>Esta ruta aún no tiene actividades</h2><p>Consulta con tu docente o selecciona otra ruta.</p></section>)}
        <footer className="page-footer">Hecho para aprender, crecer y descubrir <Heart aria-label="con cariño" /></footer>
      </main>
      {selectedNode && <dialog ref={dialog} className="lesson-modal" aria-labelledby="modal-title" aria-describedby="modal-desc" onCancel={() => setSelectedNode(null)} onClick={event => { if (event.target === event.currentTarget) setSelectedNode(null) }}>
        <button className="modal-close" autoFocus onClick={() => setSelectedNode(null)} aria-label="Cerrar detalle"><X aria-hidden="true" /></button>
        <div className="modal-icon">{selectedNode.state === 'COMPLETADO' ? <Check aria-hidden="true" /> : <Star aria-hidden="true" />}</div>
        <span className="eyebrow">PARADA #{selectedNode.sequence} · {nodeStateLabels[selectedNode.state]}</span>
        <div className="modal-title-row">
          <h2 id="modal-title">{selectedNode.title}</h2>
          <button
            type="button"
            className="modal-listen-btn"
            onClick={() => speakText(`Parada ${selectedNode.sequence}: ${selectedNode.title}. ${nodeStateLabels[selectedNode.state]}`)}
            aria-label="Escuchar título"
            title="Escuchar"
          >
            <Volume2 size={18} aria-hidden="true" />
          </button>
        </div>
        <p id="modal-desc">{selectedNode.activityType}{selectedNode.estimatedMinutes != null ? ` · ${selectedNode.estimatedMinutes} minutos aprox.` : ''}. Aquí puedes consultar el progreso guardado de esta actividad.</p>
        <button className="start-button" onClick={() => setSelectedNode(null)}>Volver al mapa <ArrowRight aria-hidden="true" /></button>
      </dialog>}
    </div>
  )
}
export default App


