import { useEffect, useRef, useState } from 'react'
import {
  AlertTriangle,
  ArrowRight,
  Award,
  BookOpen,
  Calculator,
  Check,
  Compass,
  FlaskConical,
  GraduationCap,
  Heart,
  House,
  Inbox,
  KeyRound,
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
  enrollInRoute,
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
  type JoinByCodeResult,
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
import { StudentBadgesModal } from './components/StudentBadgesModal'
import { JoinClassModal } from './components/JoinClassModal'
import { TeacherAuthoringView } from './features/teacher/TeacherAuthoringView'
import { StudentAvatar } from './components/StudentAvatar'
import { ExploreRoadmapsView } from './features/roadmap/ExploreRoadmapsView'
import { StudentProfileView } from './features/profile/StudentProfileView'
import { speakText } from './utils/speech'
import './App.css'

export type AppView = 'camino' | 'explorar' | 'perfil'


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
  const [badgesModalOpen, setBadgesModalOpen] = useState(false)
  const [highlightBadge, setHighlightBadge] = useState<BadgeDto | null>(null)
  const [joinModalOpen, setJoinModalOpen] = useState(false)
  const [loading, setLoading] = useState(true)
  const [currentView, setCurrentView] = useState<AppView>('camino')
  const [teacherViewMode, setTeacherViewMode] = useState<'authoring' | 'student_preview'>('authoring')

  const [errorType, setErrorType] = useState<Failure>(null)
  const [guideIndex, setGuideIndex] = useState(0)
  const [reloadKey, setReloadKey] = useState(0)
  const [sessionBusy, setSessionBusy] = useState(false)
  const [studentEnrolledGrade, setStudentEnrolledGrade] = useState<number>(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('rupi_student_enrolled_grade')
      if (saved) {
        const parsed = parseInt(saved, 10)
        if (!isNaN(parsed) && parsed >= 1 && parsed <= 6) return parsed
      }
    }
    return 2
  })
  const [selectedGrade, setSelectedGrade] = useState<number | null>(() => {
    if (typeof window !== 'undefined') {
      const params = new URLSearchParams(window.location.search)
      const urlGrade = params.get('grade') || params.get('grades')
      if (urlGrade) {
        const parsed = parseInt(urlGrade.split(',')[0], 10)
        if (!isNaN(parsed) && parsed >= 1 && parsed <= 6) return parsed
      }
    }
    const saved = localStorage.getItem('rupi_selected_grade')
    return saved ? parseInt(saved, 10) : 2
  })
  const [selectedCourse, setSelectedCourse] = useState<string | null>(() => {
    if (typeof window !== 'undefined') {
      const params = new URLSearchParams(window.location.search)
      const urlArea = params.get('area') || params.get('areas')
      if (urlArea) {
        const cleaned = urlArea.split(',')[0].toUpperCase()
        if (cleaned.includes('COM')) return 'COMUNICACION'
        if (cleaned.includes('CYT')) return 'CYT'
        if (cleaned.includes('MAT')) return 'MATEMATICA'
        return cleaned
      }
    }
    return localStorage.getItem('rupi_selected_course') || 'MATEMATICA'
  })
  const dialog = useRef<HTMLDialogElement>(null)

  function handleOpenBadges(badge?: BadgeDto) {
    if (badge) {
      setHighlightBadge(badge)
    }
    setBadgesModalOpen(true)
  }

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
    setActivityError(null)

    if (node.state === 'BLOQUEADO') {
      setActivityLoading(false)
      return
    }

    setActivityLoading(true)
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
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 403) {
        setActivityError('Esta parada aún está bloqueada. Avanza en las paradas previas para abrirla.')
      } else {
        setActivityError('No se pudo abrir la lección. Revisa tu conexión a internet e intenta de nuevo.')
      }
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
            completedNodes: (unit.nodes || []).filter(un =>
              un.id === result.nodeId ? true : updatedNodes.find(n => n.id === un.id)?.state === 'COMPLETADO'
            ).length,
            nodes: (unit.nodes || []).map(un => {
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
    if (result.isRouteCompleted) {
      void fetchStudentBadges().then(setBadges).catch(() => {})
    }
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
        // Buscar ruta que coincida con routeId, o con el curso y grado seleccionados
        let targetRoute = list.find(r => r.versionRouteId === routeId)
        if (!targetRoute && selectedCourse) {
          const courseCode = selectedCourse.toUpperCase()
          targetRoute = list.find(r => {
            const matchesGrade = selectedGrade == null || r.grade.number === selectedGrade
            const matchesArea = r.area.code.toUpperCase().includes(courseCode) ||
              (courseCode === 'COMUNICACION' && r.area.code === 'COM') ||
              (courseCode === 'MATEMATICA' && r.area.code === 'MAT') ||
              (courseCode === 'CYT' && r.area.code === 'CYT')
            return matchesGrade && matchesArea
          })
        }
        if (!targetRoute && selectedGrade) {
          targetRoute = list.find(r => r.grade.number === selectedGrade)
        }
        const id = routeId || (targetRoute ? targetRoute.versionRouteId : (list.length > 0 ? list[0].versionRouteId : ''))
        if (!id) {
          setErrorType('EMPTY')
          setLoading(false)
          return
        }
        let detail: RouteDetail
        try {
          detail = await fetchRouteDetail(id, signal)
        } catch (error) {
          // Ruta publicada del catálogo en la que aún no está inscrito: la inscripción es una acción explícita.
          const notEnrolled = error instanceof ApiError && error.status === 404 && !list.some(r => r.versionRouteId === id)
          if (!notEnrolled) throw error
          detail = await enrollInRoute(id, signal)
        }
        if (signal.aborted) return
        setSelectedRoute(detail)
        // Si la ruta obtenida fue auto-inscrita y no estaba en la lista previa, agregarla al estado
        if (!list.some(r => r.versionRouteId === detail.versionRouteId)) {
          setRoutes(prev => [
            ...prev,
            {
              versionRouteId: detail.versionRouteId,
              title: detail.title,
              grade: detail.grade,
              area: detail.area,
              enrollmentState: detail.enrollment.state,
              completedNodes: detail.progress.completedNodes,
              totalNodes: detail.progress.totalNodes,
              lastVisitedAt: new Date().toISOString(),
            }
          ])
        }
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

  // Roadmap disponible si la ruta seleccionada tiene paradas cargadas para interactuar
  const isRoadmapAvailable = Boolean(selectedRoute && selectedRoute.nodes && selectedRoute.nodes.length > 0)

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
    setRouteId('')
    resetView()
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

    if (typeof window !== 'undefined') {
      const url = new URL(window.location.href)
      if (gradeNumber != null) {
        url.searchParams.set('grade', String(gradeNumber))
      } else {
        url.searchParams.delete('grade')
      }
      if (courseCode != null) {
        url.searchParams.set('area', courseCode)
      } else {
        url.searchParams.delete('area')
      }
      window.history.replaceState(null, '', `${url.pathname}${url.search}${url.hash}`)
    }
  }

  function handleJoinedClass(result: JoinByCodeResult) {
    const areaUpper = result.area.code.toUpperCase()
    let courseCode = 'MATEMATICA'
    if (areaUpper.includes('COM')) {
      courseCode = 'COMUNICACION'
    } else if (areaUpper.includes('CYT')) {
      courseCode = 'CYT'
    }
    handleSelectGradeAndCourse(result.grade.number, courseCode)
    setRouteId(result.versionRouteId)
    setReloadKey(value => value + 1)
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
      setTeacherViewMode('authoring')

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
      setTeacherViewMode('authoring')
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
      setTeacherViewMode('authoring')
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

  // 1.5 Panel de Autoría para Docentes (HU-05)
  if (student.role === 'DOCENTE' && teacherViewMode === 'authoring') {
    return (
      <TeacherAuthoringView
        teacherName={student.name}
        schoolName={student.schoolName || student.institutionName || 'I.E. 38001 Mariscal Sucre'}
        onLogout={handleLogout}
        onSwitchToStudentView={() => {
          setTeacherViewMode('student_preview')
          if (selectedGrade === null) setSelectedGrade(2)
          if (selectedCourse === null) setSelectedCourse('MATEMATICA')
        }}
      />
    )
  }

  // 2. Pantalla de selección de grado y materias de Primaria: aislada y enfocada
  if (selectedGrade === null || selectedCourse === null) {
    return (
      <>
        <GradeSelectView
          student={student}
          routes={routes}
          initialGrade={selectedGrade}
          onSelectGradeAndCourse={(grade, course) => {
            setStudentEnrolledGrade(grade)
            localStorage.setItem('rupi_student_enrolled_grade', String(grade))
            handleSelectGradeAndCourse(grade, course)
          }}
          onSelectEnrolledRoute={(versionRouteId, gradeNumber, courseCode) => {
            setSelectedGrade(gradeNumber)
            setSelectedCourse(courseCode)
            setRouteId(versionRouteId)
            resetView()
          }}
          onLogout={handleLogout}
          onOpenJoinModal={() => setJoinModalOpen(true)}
          onBackToTeacherAuthoring={student.role === 'DOCENTE' ? () => setTeacherViewMode('authoring') : undefined}
        />
        <JoinClassModal
          isOpen={joinModalOpen}
          onClose={() => setJoinModalOpen(false)}
          onJoined={handleJoinedClass}
        />
      </>
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
          <button
            type="button"
            className={`nav-item ${currentView === 'camino' ? 'selected' : ''}`}
            onClick={() => setCurrentView('camino')}
            aria-current={currentView === 'camino' ? 'page' : undefined}
          >
            <House aria-hidden="true" />
            <span>Mi Camino</span>
          </button>
          <button
            type="button"
            className={`nav-item ${currentView === 'explorar' ? 'selected' : ''}`}
            onClick={() => setCurrentView('explorar')}
            aria-current={currentView === 'explorar' ? 'page' : undefined}
          >
            <Compass aria-hidden="true" />
            <span>Explorar Roadmaps</span>
            <span className="nav-badge-pill">Oficial y Comunidad</span>
          </button>
          <button
            type="button"
            className={`nav-item ${currentView === 'perfil' ? 'selected' : ''}`}
            onClick={() => setCurrentView('perfil')}
            aria-current={currentView === 'perfil' ? 'page' : undefined}
            title="Ver mi perfil e insignias escolares"
          >
            <Award aria-hidden="true" />
            <span>Mi Perfil</span>
            {badges.length > 0 && (
              <span className="nav-badge-count">{badges.length}</span>
            )}
          </button>
        </nav>
        <div className="sidebar-bottom">
          <div className="profile-top-group">
            <button
              type="button"
              className={`profile-student-card ${currentView === 'perfil' ? 'active-profile' : ''}`}
              onClick={() => setCurrentView('perfil')}
              title={`Ver perfil e insignias de ${student.name}`}
            >
              <StudentAvatar
                name={student.name}
                role={student.role}
                size="sm"
                showBadge
              />
              <span className="profile-text-col">
                <b className="profile-student-name">{student.name}</b>
                <small className="profile-school-name">
                  {student.role === 'DOCENTE' ? 'Docente' : `${studentEnrolledGrade}.° de Primaria`}
                </small>
              </span>
            </button>
            <button
              className="profile-logout-btn"
              type="button"
              disabled={loading || sessionBusy}
              onClick={() => void handleLogout()}
              aria-label={`Cerrar sesión de ${student.name}`}
              title="Cerrar sesión"
            >
              <LogOut size={16} aria-hidden="true" />
              <span>Salir</span>
            </button>
          </div>
        </div>
      </aside>

      <main className="main-content" id="main-content">
        {student.role === 'DOCENTE' && (
          <div className="teacher-preview-topbar" role="status">
            <div className="teacher-preview-info">
              <GraduationCap size={18} aria-hidden="true" />
              <span>
                <strong>Modo Previsualización Docente:</strong> Estás probando la experiencia interactiva como un estudiante.
              </span>
            </div>
            <button
              type="button"
              className="back-to-authoring-btn"
              onClick={() => setTeacherViewMode('authoring')}
              title="Regresar a diseñar y editar tus roadmaps"
            >
              <Compass size={16} aria-hidden="true" />
              <span>Volver a Panel de Autoría</span>
            </button>
          </div>
        )}

        {/* Pantalla 1: Explorar Roadmaps (Oficial CNEB 1.°-6.° vs Comunidad/Profesores) */}
        {currentView === 'explorar' && (
          <ExploreRoadmapsView
            currentGrade={selectedGrade}
            currentCourse={selectedCourse}
            enrolledRoutes={routes}
            onSelectRoute={(versionRouteId, gradeNumber, areaCode) => {
              if (gradeNumber) setSelectedGrade(gradeNumber)
              if (areaCode) {
                const code = areaCode.toUpperCase()
                if (code.includes('COM')) setSelectedCourse('COMUNICACION')
                else if (code.includes('CYT')) setSelectedCourse('CYT')
                else if (code.includes('MAT')) setSelectedCourse('MATEMATICA')
              }
              resetView()
              setRouteId(versionRouteId)
              setCurrentView('camino')
            }}
            onOpenJoinModal={() => setJoinModalOpen(true)}
          />
        )}

        {/* Pantalla 2: Mi Perfil Escolar e Insignias Integradas */}
        {currentView === 'perfil' && (
          <StudentProfileView
            student={student}
            enrolledGrade={studentEnrolledGrade}
            earnedBadges={badges}
            enrolledRoutes={routes}
            onSelectRoute={(versionRouteId, gradeNumber, areaCode) => {
              if (gradeNumber) setSelectedGrade(gradeNumber)
              if (areaCode) {
                const code = areaCode.toUpperCase()
                if (code.includes('COM')) setSelectedCourse('COMUNICACION')
                else if (code.includes('CYT')) setSelectedCourse('CYT')
                else if (code.includes('MAT')) setSelectedCourse('MATEMATICA')
              }
              resetView()
              setRouteId(versionRouteId)
              setCurrentView('camino')
            }}
            onChangeGrade={() => {
              setSelectedGrade(null)
              setSelectedCourse(null)
              localStorage.removeItem('rupi_selected_grade')
              localStorage.removeItem('rupi_selected_course')
            }}
            onOpenJoinModal={() => setJoinModalOpen(true)}
            onLogout={() => void handleLogout()}
          />
        )}

        {/* Pantalla 3: Mi Camino (Mapa interactivo despejado y enfocado en la lección actual) */}
        {currentView === 'camino' && (
          <>
            <section className="course-heading" aria-labelledby="course-title">
              <div className="course-heading-layout">
                <div className="course-heading-main">
                  <span className="eyebrow">Tu espacio para descubrir</span>
                  <h1 id="course-title">
                    {selectedRoute ? selectedRoute.title : 'Mi ruta de aprendizaje'}
                  </h1>
                  <p className="course-subtitle">
                    {selectedRoute?.culturalContext?.motivationalMessage || 'Un mundo por aprender. A tu propio ritmo.'}
                  </p>
                </div>
                <div className="course-heading-meta">
                  <span className="current-grade-badge">
                    <GraduationCap size={16} aria-hidden="true" />
                    <span>
                      {selectedRoute
                        ? `${selectedRoute.grade.number}.° de primaria · ${selectedRoute.area.name}`
                        : `${selectedGrade || studentEnrolledGrade}.° de primaria · ${getCourseLabel(selectedCourse)}`}
                    </span>
                  </span>

                  {selectedRoute && selectedRoute.grade.number !== studentEnrolledGrade && (
                    <span className="exploring-grade-pill" title="Explorando contenido de otro grado escolar">
                      <Compass size={14} aria-hidden="true" />
                      <span>Explorando {selectedRoute.grade.number}.° de Primaria</span>
                    </span>
                  )}

                  <div className="course-heading-actions">
                    <button
                      type="button"
                      className="change-grade-btn explore-more-btn"
                      onClick={() => setCurrentView('explorar')}
                      title="Explorar más rutas oficiales y de profesores"
                      aria-label="Explorar otros roadmaps"
                    >
                      <Compass size={16} aria-hidden="true" />
                      <span>Explorar otros roadmaps</span>
                    </button>
                  </div>
                </div>
              </div>
            </section>

            {/* Barra interactiva de cambio ágil de materia escolar */}
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
                {routes.some(r => (r.area.code === 'COM' || r.area.code.includes('COM')) && (selectedGrade == null || r.grade.number === selectedGrade)) ? (
                  <span className="tab-status-pill active" title="Roadmap activo">
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
                className={`course-switch-tab ${selectedCourse === 'MATEMATICA' ? 'active matematica' : ''}`}
                onClick={() => handleSelectGradeAndCourse(selectedGrade, 'MATEMATICA')}
                aria-pressed={selectedCourse === 'MATEMATICA'}
                title="Cambiar a Matemática"
              >
                <Calculator size={16} aria-hidden="true" />
                <span>Matemática</span>
                {routes.some(r => (r.area.code === 'MAT' || r.area.code.includes('MAT')) && (selectedGrade == null || r.grade.number === selectedGrade)) ? (
                  <span className="tab-status-pill active" title="Roadmap activo">
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
                {routes.some(r => (r.area.code === 'CYT' || r.area.code.includes('CYT')) && (selectedGrade == null || r.grade.number === selectedGrade)) ? (
                  <span className="tab-status-pill active" title="Roadmap activo">
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
            </nav>

            {/* CASO A: Roadmap Habilitado para la materia y grado */}
            {isRoadmapAvailable && (
              <>
                {selectedRoute && (
                  <RouteOverview
                    route={selectedRoute}
                    name={student.name}
                    badges={badges}
                    onSelect={handleOpenNode}
                    onOpenBadges={() => setCurrentView('perfil')}
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
                      <p>Consulta con tu docente o selecciona otra ruta en el catálogo.</p>
                      <button
                        type="button"
                        className="status-action-btn"
                        onClick={() => setCurrentView('explorar')}
                      >
                        <Compass size={16} aria-hidden="true" />
                        <span>Ver catálogo de roadmaps</span>
                      </button>
                    </section>
                  )
                )}
              </>
            )}

            {/* CASO B: Sin roadmap directo para la materia y grado */}
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
                          : 'mathematician'
                      }
                      mood="thinking"
                      className="locked-rupi-svg"
                    />
                  </div>

                  <div className="course-locked-content">
                    <div className="course-locked-tag">
                      <Compass size={15} aria-hidden="true" />
                      <span>EXPLORACIÓN ESCOLAR</span>
                    </div>

                    <h2 id="locked-course-title">
                      Ruta en preparación para {selectedGrade ? `${selectedGrade}.° de primaria` : 'tu grado'} · {getCourseLabel(selectedCourse)}
                    </h2>

                    <p className="locked-course-desc">
                      ¡Hola, {student.name}! Tu docente o tu escuela aún están preparando el camino para esta materia.
                      Puedes explorar el catálogo completo de roadmaps oficiales y de la comunidad, o unirte con el código que te dé tu profesor.
                    </p>

                    <div className="course-locked-actions">
                      <button
                        type="button"
                        className="go-to-math-btn"
                        onClick={() => setCurrentView('explorar')}
                        title="Explorar roadmaps oficiales y de la comunidad"
                      >
                        <Compass size={18} aria-hidden="true" />
                        <span>Explorar catálogo de roadmaps</span>
                        <ArrowRight size={18} aria-hidden="true" />
                      </button>

                      {routes.length > 0 && (
                        <button
                          type="button"
                          className="change-course-alt-btn"
                          onClick={() => {
                            const target = routes[0]
                            const courseCode = target.area.code.includes('COM')
                              ? 'COMUNICACION'
                              : target.area.code.includes('CYT')
                              ? 'CYT'
                              : 'MATEMATICA'
                            handleSelectGradeAndCourse(target.grade.number, courseCode)
                          }}
                          title="Ir a mi ruta activa"
                        >
                          <BookOpen size={16} aria-hidden="true" />
                          <span>Volver a: {routes[0].title}</span>
                        </button>
                      )}

                      <button
                        type="button"
                        className="join-code-cta-btn"
                        onClick={() => setJoinModalOpen(true)}
                        title="Ingresar código de clase docente"
                      >
                        <KeyRound size={16} aria-hidden="true" />
                        <span>Tengo un código de clase</span>
                      </button>

                      <button
                        type="button"
                        className="locked-speech-btn"
                        onClick={() => speakText(
                          `Ruta en preparación para ${selectedGrade} grado de primaria en ${getCourseLabel(selectedCourse)}. Puedes explorar el catálogo de roadmaps o ingresar un código de clase.`
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
          </>
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
                <RupiCharacter mood="thinking" className="loading-rupi-svg" />
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
              onOpenBadges={() => handleOpenBadges()}
            />
          ) : selectedNode.state === 'BLOQUEADO' ? (
            <div className="modal-locked-view" role="region" aria-label="Parada bloqueada">
              <button
                className="modal-close"
                autoFocus
                onClick={() => setSelectedNode(null)}
                aria-label="Cerrar detalle"
              >
                <X aria-hidden="true" />
              </button>
              <div className="modal-icon locked">
                <Lock size={28} aria-hidden="true" />
              </div>
              <span className="eyebrow locked-eyebrow">
                PARADA #{selectedNode.sequence} · BLOQUEADA
              </span>
              <div className="modal-title-row">
                <h2 id="modal-title">{selectedNode.title}</h2>
                <button
                  type="button"
                  className="modal-listen-btn"
                  onClick={() =>
                    speakText(
                      `Parada ${selectedNode.sequence}: ${selectedNode.title}. Esta parada aún está descansando. Completa la parada anterior para abrir este camino.`
                    )
                  }
                  aria-label="Escuchar explicación de parada bloqueada"
                  title="Escuchar"
                >
                  <Volume2 size={18} aria-hidden="true" />
                </button>
              </div>
              <div className="locked-rupi-card">
                <div className="locked-rupi-avatar" aria-hidden="true">
                  <RupiCharacter mood="thinking" />
                </div>
                <div className="locked-rupi-bubble">
                  <p>
                    ¡Hola, explorador! Esta parada aún está en descanso 🌿.
                    Para desbloquearla y ganar nuevas insignias, primero completa la parada #{Math.max(1, selectedNode.sequence - 1)} en tu mapa.
                  </p>
                </div>
              </div>
              <button className="start-button locked-back-btn" onClick={() => setSelectedNode(null)}>
                Volver a mi mapa <ArrowRight aria-hidden="true" />
              </button>
            </div>
          ) : (
            <div className="modal-summary-view">
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
              <div className="modal-summary-actions">
                <button
                  type="button"
                  className="start-button primary"
                  onClick={() => handleOpenNode(selectedNode)}
                >
                  <span>{selectedNode.state === 'COMPLETADO' ? 'Repasar lección' : 'Comenzar lección'}</span>
                  <ArrowRight aria-hidden="true" />
                </button>
                <button
                  type="button"
                  className="start-button secondary"
                  onClick={() => setSelectedNode(null)}
                >
                  Volver al mapa
                </button>
              </div>
            </div>
          )}
        </dialog>
      )}

      {badgesModalOpen && (
        <StudentBadgesModal
          studentName={student?.name || 'Estudiante'}
          schoolName={student?.schoolName || 'I.E. 38001 Mariscal Sucre'}
          gradeName={`${selectedGrade || 2}.° de primaria`}
          earnedBadges={badges}
          totalXp={routes.reduce((acc, r) => acc + (r.completedNodes * 50), 0) + (badges.some(b => b.code === 'GRAN_TUNQUI_ORO') ? 100 : 0)}
          highlightBadge={highlightBadge}
          onClose={() => {
            setBadgesModalOpen(false)
            setHighlightBadge(null)
          }}
        />
      )}

      <JoinClassModal
        isOpen={joinModalOpen}
        onClose={() => setJoinModalOpen(false)}
        onJoined={handleJoinedClass}
      />
    </div>

  )
}

export default App
