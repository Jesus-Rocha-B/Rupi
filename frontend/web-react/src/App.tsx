import { useEffect, useState } from 'react'
import {
  AlertTriangle,
  ArrowRight,
  Bird,
  BookOpen,
  BrainCircuit,
  Check,
  ChevronDown,
  Compass,
  Heart,
  House,
  Inbox,
  Lightbulb,
  Lock,
  RefreshCw,
  Sparkles,
  Star,
  Target,
  Trophy,
  X,
} from 'lucide-react'
import { RupiCharacter } from './components/RupiCharacter'
import { Roadmap } from './features/roadmap/Roadmap'
import {
  fetchRouteDetail,
  fetchStudentRoutes,
  type RouteDetail,
  type RouteNode,
  type RouteSummary,
} from './services/learningRoutesApi'
import './App.css'

const DEMO_STUDENT_ID = '11111111-1111-4111-8111-111111111111'

const guideMessages = [
  '¡Ese reto de restas te espera! Puedes ganar 20 XP.',
  'Cada parada que completas te acerca al siguiente nivel.',
  '¿Listo para superar tu propio récord? ¡Tú marcas el ritmo!',
]

function App() {
  const [studentId, setStudentId] = useState<string>(DEMO_STUDENT_ID)
  const [, setRoutes] = useState<RouteSummary[]>([])
  const [selectedRoute, setSelectedRoute] = useState<RouteDetail | null>(null)
  const [selectedNode, setSelectedNode] = useState<RouteNode | null>(null)
  const [loading, setLoading] = useState(true)
  const [errorType, setErrorType] = useState<'NETWORK' | 'UNAUTHORIZED' | 'EMPTY' | null>(null)
  const [guideIndex, setGuideIndex] = useState(0)
  const [tutorHintOpen, setTutorHintOpen] = useState(false)

  const [reloadKey, setReloadKey] = useState(0)

  const handleRetry = () => {
    setLoading(true)
    setErrorType(null)
    setReloadKey((prev) => prev + 1)
  }

  const handleToggleStudent = () => {
    setLoading(true)
    setErrorType(null)
    setStudentId((prev) => (prev ? '' : DEMO_STUDENT_ID))
  }

  const handleLoginDemo = () => {
    setLoading(true)
    setErrorType(null)
    setStudentId(DEMO_STUDENT_ID)
  }

  useEffect(() => {
    let ignore = false

    fetchStudentRoutes(studentId)
      .then(async (routeList) => {
        if (ignore) return
        setRoutes(routeList)

        if (routeList.length === 0) {
          setErrorType('EMPTY')
          setSelectedRoute(null)
          setLoading(false)
          return
        }

        const targetVersionId = routeList[0].versionRouteId
        const detail = await fetchRouteDetail(targetVersionId, studentId)
        if (ignore) return
        setSelectedRoute(detail)
        setLoading(false)
      })
      .catch((error: unknown) => {
        if (ignore) return
        if (
          typeof error === 'object' &&
          error !== null &&
          'status' in error &&
          (error as { status: number }).status === 401
        ) {
          setErrorType('UNAUTHORIZED')
        } else {
          setErrorType('NETWORK')
        }
        setSelectedRoute(null)
        setLoading(false)
      })

    return () => {
      ignore = true
    }
  }, [studentId, reloadKey])

  // Cálculo derivado del progreso en la presentación (sin duplicar porcentaje)
  const completedNodes = selectedRoute?.progress.completedNodes ?? 0
  const totalNodes = selectedRoute?.progress.totalNodes ?? 0
  const progressPct = totalNodes > 0 ? Math.round((completedNodes / totalNodes) * 100) : 0

  // Nodo sugerido para continuar (último visitado o primero disponible/en curso)
  const resumeNode =
    selectedRoute?.nodes.find((n) => n.id === selectedRoute.enrollment.lastVisitedNodeId) ??
    selectedRoute?.nodes.find((n) => n.state === 'EN_CURSO') ??
    selectedRoute?.nodes.find((n) => n.state === 'DISPONIBLE') ??
    selectedRoute?.nodes[0]

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#inicio" aria-label="Rupi, inicio">
          <img className="brand-logo" src="/rupi-logo.jpg" alt="" />
          <span>rupi<span className="brand-dot">.</span></span>
        </a>
        <div className="school-label">MI ESPACIO</div>
        <nav className="main-nav" aria-label="Navegación principal">
          <a className="nav-item selected" href="#ruta"><House aria-hidden="true" />Mi ruta</a>
          <a className="nav-item" href="#explorar"><Compass aria-hidden="true" />Explorar rutas</a>
          <a className="nav-item" href="#repaso"><BookOpen aria-hidden="true" />Repaso IA</a>
          <a className="nav-item" href="#tutor"><BrainCircuit aria-hidden="true" />Tutor IA</a>
          <a className="nav-item" href="#retos"><Target aria-hidden="true" />Retos</a>
          <a className="nav-item" href="#logros"><Trophy aria-hidden="true" />Logros</a>
        </nav>
        <div className="sidebar-bottom">
          <div className="help-bubble">
            ¿Necesitas ayuda?<br />
            <span>Rupi está aquí para ti.</span>
            <div className="help-face"><Bird aria-hidden="true" /></div>
          </div>
          <button
            className="profile-button"
            type="button"
            title={studentId ? 'Clic para simular cierre de sesión' : 'Clic para iniciar sesión'}
            onClick={handleToggleStudent}
          >
            <span className="avatar small">{studentId ? 'M' : '?'}</span>
            <span>
              <b>{studentId ? 'Mateo Quispe' : 'Sin sesión'}</b>
              <small>{studentId ? '2.º de primaria' : 'Toca para acceder'}</small>
            </span>
          </button>
        </div>
      </aside>

      <main className="main-content" id="ruta">
        <section className="course-heading" aria-labelledby="course-title">
          <div>
            <div className="eyebrow">TU RUTA DE APRENDIZAJE</div>
            <h1 id="course-title">
              {selectedRoute ? selectedRoute.area.name : 'Matemática'}{' '}
              <span className="grade-pill">{selectedRoute ? selectedRoute.grade.name : '2.º grado'}</span>
            </h1>
            <p>Avanza a tu ritmo con Rupi. ¡Tú puedes!</p>
          </div>
          <button className="term-button" type="button">Periodo 1 <ChevronDown aria-hidden="true" /></button>
        </section>

        {/* Resumen de avance calculado desde nodos completados */}
        {selectedRoute && (
          <section className="progress-card" aria-label="Progreso del curso">
            <div className="progress-top">
              <div><Sparkles className="progress-star" aria-hidden="true" /><b>Tu aventura va tomando forma</b></div>
              <span className="progress-numbers">{completedNodes} de {totalNodes} niveles <b>·</b> {progressPct}%</span>
            </div>
            <div className="progress-track">
              <div className="progress-fill" style={{ width: `${progressPct}%` }} />
            </div>
          </section>
        )}

        {/* Estado 1: Cargando */}
        {loading && (
          <section className="status-panel" aria-busy="true">
            <div className="status-panel-icon loading"><Sparkles aria-hidden="true" /></div>
            <h2>Cargando tu ruta de aprendizaje...</h2>
            <p>Rupi está trayendo tus niveles y el mapa de Ayacucho desde el servidor.</p>
          </section>
        )}

        {/* Estado 2: Error de conexión o 503 */}
        {!loading && errorType === 'NETWORK' && (
          <section className="status-panel" role="alert">
            <div className="status-panel-icon error"><AlertTriangle aria-hidden="true" /></div>
            <h2>No se pudo conectar con el servidor</h2>
            <p>Hubo un inconveniente al consultar tu progreso. Comprueba tu conexión o que la API esté activa.</p>
            <button className="status-action-btn" type="button" onClick={handleRetry}>
              <RefreshCw aria-hidden="true" /> Reintentar
            </button>
          </section>
        )}

        {/* Estado 3: No autenticado (401) */}
        {!loading && errorType === 'UNAUTHORIZED' && (
          <section className="status-panel" role="alert">
            <div className="status-panel-icon auth"><Lock aria-hidden="true" /></div>
            <h2>Sesión no iniciada</h2>
            <p>No se encontró una sesión activa de estudiante. Inicia sesión para visualizar tu avance.</p>
            <button
              className="status-action-btn"
              type="button"
              onClick={handleLoginDemo}
            >
              Entrar como Mateo Quispe (Demo) <ArrowRight aria-hidden="true" />
            </button>
          </section>
        )}

        {/* Estado 4: Sin rutas inscritas (Vacío) */}
        {!loading && errorType === 'EMPTY' && (
          <section className="status-panel">
            <div className="status-panel-icon empty"><Inbox aria-hidden="true" /></div>
            <h2>Aún no tienes rutas asignadas</h2>
            <p>No tienes inscripciones activas por el momento. Puedes unirte con un código o consultar con tu docente.</p>
            <a className="status-action-btn" href="#explorar">
              Explorar rutas disponibles <Compass aria-hidden="true" />
            </a>
          </section>
        )}

        {/* Estado 5: Mapa interactivo con datos reales */}
        {!loading && selectedRoute && (
          <div className="learning-layout">
            <Roadmap
              route={selectedRoute}
              guideMessage={guideMessages[guideIndex]}
              onGuideClick={() => setGuideIndex((index) => (index + 1) % guideMessages.length)}
              onSelectNode={setSelectedNode}
            />

            <aside className="right-column">
              {resumeNode && (
                <section className="continue-card" id="repaso" aria-labelledby="continue-title">
                  <div className="continue-top"><Sparkles className="mini-spark" aria-hidden="true" /><span>CONTINÚA TU AVENTURA</span></div>
                  <div className="continue-art">
                    <span className="place-art-label">PORTALES Y CAMPANAS</span>
                  </div>
                  <div className="continue-body">
                    <span className="lesson-tag">
                      {resumeNode.activityType}
                      {resumeNode.estimatedMinutes ? ` · ${resumeNode.estimatedMinutes} MIN` : ''}
                    </span>
                    <h2 id="continue-title">{resumeNode.title}</h2>
                    <p>Parada #{resumeNode.sequence} en la plaza. ¡Sigue avanzando con tu ruta!</p>
                    <button
                      className="start-button"
                      type="button"
                      onClick={() => setSelectedNode(resumeNode)}
                    >
                      Continuar aprendiendo <ArrowRight aria-hidden="true" />
                    </button>
                  </div>
                </section>
              )}

              <section className="mascot-card" id="tutor" aria-labelledby="tutor-title">
                <div className="mascot-copy">
                  <span className="eyebrow" id="tutor-title">RUPI, TU TUTOR</span>
                  <p>¿Te atascaste? Pide una pista y sigue intentando.</p>
                  <button
                    className="hint-button"
                    type="button"
                    aria-expanded={tutorHintOpen}
                    onClick={() => setTutorHintOpen((open) => !open)}
                  >
                    <Lightbulb aria-hidden="true" />
                    {tutorHintOpen ? 'Ocultar pista' : 'Pedir una pista'}
                  </button>
                  {tutorHintOpen && (
                    <div className="hint-panel" role="status">
                      Prueba separar las cantidades en decenas y unidades. ¿Qué monedas podrías juntar?
                    </div>
                  )}
                </div>
                <RupiCharacter className="mascot" />
              </section>
              <div className="curriculum-note"><Sparkles aria-hidden="true" />Ruta alineada al currículo escolar</div>
            </aside>
          </div>
        )}

        <footer className="page-footer">
          Hecho para aprender, crecer y descubrir <Heart aria-label="con cariño" />
        </footer>
      </main>

      {/* Modal accesible de nodo */}
      {selectedNode && (
        <div className="modal-backdrop" role="presentation" onClick={() => setSelectedNode(null)}>
          <section
            className="lesson-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="modal-title"
            onClick={(event) => event.stopPropagation()}
          >
            <button className="modal-close" type="button" onClick={() => setSelectedNode(null)} aria-label="Cerrar">
              <X aria-hidden="true" />
            </button>
            <div className="modal-icon">
              {selectedNode.state === 'COMPLETADO' ? <Check aria-hidden="true" /> : <Star aria-hidden="true" />}
            </div>
            <span className="eyebrow">
              PARADA #{selectedNode.sequence} · {selectedNode.activityType}
            </span>
            <h2 id="modal-title">{selectedNode.title}</h2>
            <p>
              {selectedNode.state === 'COMPLETADO'
                ? '¡Ya completaste esta actividad! Puedes volver a explorarla cuando quieras.'
                : '¡Es hora de aprender! En esta actividad practicarás con pistas y pequeños desafíos.'}
            </p>
            <button className="start-button" type="button" onClick={() => setSelectedNode(null)}>
              {selectedNode.state === 'COMPLETADO' ? '¡Entendido!' : '¡Vamos allá!'} <ArrowRight aria-hidden="true" />
            </button>
          </section>
        </div>
      )}
    </div>
  )
}

export default App
