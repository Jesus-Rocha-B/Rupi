import { useEffect, useState } from 'react'
import {
  AlertTriangle,
  ArrowRight,
  Award,
  BookOpen,
  Calculator,
  Compass,
  FlaskConical,
  GraduationCap,
  Heart,
  House,
  KeyRound,
  LogOut,
  RefreshCw,
  Sparkles,
  Star,
  Volume2,
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
import { RouteCompanion } from './components/RouteCompanion'
import { LoginView } from './features/auth/LoginView'
import { GradeSelectView } from './features/auth/GradeSelectView'
import { RupiCharacter } from './components/RupiCharacter'
import { LessonScreenView } from './features/roadmap/LessonScreenView'
import { StudentBadgesModal } from './components/StudentBadgesModal'
import { JoinClassModal } from './components/JoinClassModal'
import { TeacherReportsView } from './features/reports/TeacherReportsView'
import { TeacherAuthoringView } from './features/teacher/TeacherAuthoringView'
import { StudentAvatar } from './components/StudentAvatar'
import { ExploreRoadmapsView } from './features/roadmap/ExploreRoadmapsView'
import { StudentProfileView } from './features/profile/StudentProfileView'
import { speakText } from './utils/speech'
import './App.css'

export type AppView = 'camino' | 'explorar' | 'perfil' | 'leccion'


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

// Preferencias de navegación de esta pestaña. El grado escolar del alumno lo decide siempre el servidor.
const NAVIGATION_KEYS = ['rupi_selected_grade', 'rupi_selected_course']
// Claves antiguas que guardaban el grado en la tableta y lo heredaba el siguiente alumno.
const LEGACY_GRADE_KEYS = ['rupi_student_manual_grade', 'rupi_student_enrolled_grade']
const SLOW_LOADING_MS = 8000

function readStorage(key: string): string | null {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

function writeStorage(key: string, value: string | null) {
  try {
    if (value == null) localStorage.removeItem(key)
    else localStorage.setItem(key, value)
  } catch {
    // Sin almacenamiento local (modo privado o bloqueado): la navegación sigue funcionando.
  }
}

function clearStoredNavigation() {
  ;[...NAVIGATION_KEYS, ...LEGACY_GRADE_KEYS].forEach(key => writeStorage(key, null))
}

function parseGrade(value: string | number | null | undefined): number | null {
  const parsed = typeof value === 'number' ? value : Number.parseInt(value ?? '', 10)
  return Number.isInteger(parsed) && parsed >= 1 && parsed <= 6 ? parsed : null
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
  const [slowLoading, setSlowLoading] = useState(false)
  const [currentView, setCurrentView] = useState<AppView>('camino')
  const [teacherViewMode, setTeacherViewMode] = useState<'authoring' | 'student_preview' | 'reports'>('authoring')

  const [errorType, setErrorType] = useState<Failure>(null)
  const [guideIndex, setGuideIndex] = useState(0)
  const [reloadKey, setReloadKey] = useState(0)
  const [sessionBusy, setSessionBusy] = useState(false)
  // Grado escolar real del alumno (aula activa); solo se actualiza con la respuesta de la sesión.
  const [studentEnrolledGrade, setStudentEnrolledGrade] = useState<number>(2)
  const [selectedGrade, setSelectedGrade] = useState<number | null>(() => {
    if (typeof window !== 'undefined') {
      const params = new URLSearchParams(window.location.search)
      const urlGrade = params.get('grade') || params.get('grades')
      const fromUrl = parseGrade(urlGrade?.split(',')[0])
      if (fromUrl != null) return fromUrl
    }
    return parseGrade(readStorage('rupi_selected_grade'))
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
    return readStorage('rupi_selected_course')
  })
  function handleOpenBadges(badge?: BadgeDto) {
    if (badge) {
      setHighlightBadge(badge)
    }
    setBadgesModalOpen(true)
  }

  async function handleOpenNode(node: RouteNode) {
    if (!selectedRoute) return
    if (node.state === 'BLOQUEADO') {
      speakText('Esta parada aún está bloqueada. Completa las paradas anteriores en tu camino para abrirla.')
      return
    }

    setSelectedNode(node)
    setCurrentView('leccion')
    setActivity(null)
    setActivityError(null)
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
      const nextNodeId = result.nextUnlockedNode?.id || (prev.enrollment ? prev.enrollment.lastVisitedNodeId : result.nodeId)
      return {
        ...prev,
        nodes: updatedNodes,
        enrollment: prev.enrollment ? {
          ...prev.enrollment,
          lastVisitedNodeId: nextNodeId,
        } : prev.enrollment,
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
        setStudent(current)
        const currentList = await fetchStudentRoutes(signal)
        setStudentEnrolledGrade(parseGrade(current.gradeNumber) ?? 2)

        // Sin inscripciones no se inscribe a nadie al cargar: el alumno elige su ruta en «Explorar» o con un código de clase.
        setRoutes(currentList)
        if (!currentList.length) {
          setErrorType('EMPTY')
          setLoading(false)
          return
        }
        // Si el estudiante aún no ha elegido su grado y curso (y no hay ruta específica por id en la URL),
        // no forzamos una ruta por defecto para permitir que el estudiante elija en GradeSelectView
        if (current.role === 'ESTUDIANTE' && !routeId && (selectedGrade == null || selectedCourse == null)) {
          setSelectedRoute(null)
          setLoading(false)
          return
        }

        // Buscar ruta que coincida con routeId, o con el curso y grado seleccionados
        let targetRoute = currentList.find(r => r.versionRouteId === routeId)
        if (!targetRoute && selectedCourse) {
          const courseCode = selectedCourse.toUpperCase()
          targetRoute = currentList.find(r => {
            const matchesGrade = selectedGrade == null || r.grade.number === selectedGrade
            const matchesArea = r.area.code.toUpperCase().includes(courseCode) ||
              (courseCode === 'COMUNICACION' && r.area.code === 'COM') ||
              (courseCode === 'MATEMATICA' && r.area.code === 'MAT') ||
              (courseCode === 'CYT' && r.area.code === 'CYT')
            return matchesGrade && matchesArea
          })
        }
        if (!targetRoute && selectedGrade) {
          targetRoute = currentList.find(r => r.grade.number === selectedGrade)
        }
        const id = routeId || (targetRoute ? targetRoute.versionRouteId : (currentList.length > 0 ? currentList[0].versionRouteId : ''))
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
          const notEnrolled = error instanceof ApiError && error.status === 404 && !currentList.some(r => r.versionRouteId === id)
          if (!notEnrolled) throw error
          detail = await enrollInRoute(id, signal)
        }
        if (signal.aborted) return
        setSelectedRoute(detail)
        if (detail.grade?.number) {
          setSelectedGrade(detail.grade.number)
          writeStorage('rupi_selected_grade', String(detail.grade.number))
        }
        if (detail.area?.code) {
          const code = detail.area.code.toUpperCase()
          let normalized: string
          if (code.includes('COM')) normalized = 'COMUNICACION'
          else if (code.includes('CYT')) normalized = 'CYT'
          else if (code.includes('MAT')) normalized = 'MATEMATICA'
          else normalized = code
          setSelectedCourse(normalized)
          writeStorage('rupi_selected_course', normalized)
        }
        // Si la ruta obtenida fue auto-inscrita y no estaba en la lista previa, agregarla al estado
        if (!currentList.some(r => r.versionRouteId === detail.versionRouteId)) {
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
      } catch (error) {
        // Una carga cancelada (cambio de ruta, grado o curso, o el doble montaje de StrictMode) no es un fallo:
        // marcarla como error de red mostraba el inicio de sesión a un alumno con sesión abierta.
        if (signal.aborted) return
        const status = error instanceof ApiError ? error.status : 0
        setErrorType(status === 401 ? 'UNAUTHORIZED' : status === 404 ? 'NOT_FOUND' : 'NETWORK')
        if (status === 401) {
          setStudent(null)
          setRoutes([])
          setSelectedRoute(null)
        }
        setSelectedRoute(null)
      } finally {
        // La carga que reemplazó a esta es la que decide cuándo termina.
        if (!signal.aborted) setLoading(false)
      }
    }
    void load()
    return () => controller.abort()
  }, [routeId, reloadKey, selectedGrade, selectedCourse])

  // Con conexión lenta se ofrece reintentar sin salir de la pantalla de carga: ocultarla antes de tiempo
  // mostraba el inicio de sesión a un alumno que ya tenía sesión abierta. Cada petición expira a los 15 s.
  useEffect(() => {
    if (!loading) return
    const timer = setTimeout(() => setSlowLoading(true), SLOW_LOADING_MS)
    return () => clearTimeout(timer)
  }, [loading, reloadKey])

  // Roadmap disponible si la ruta seleccionada tiene paradas cargadas para interactuar
  const isRoadmapAvailable = Boolean(selectedRoute && selectedRoute.nodes && selectedRoute.nodes.length > 0)

  // Toda pantalla de carga debe ir acompañada de una carga nueva. Si solo se activaba `loading` y la ruta,
  // el grado y el curso no cambiaban (p. ej., volver a elegir la misma ruta), el efecto no se repetía y la app
  // quedaba en el esqueleto hasta pulsar F5.
  function resetView() {
    setLoading(true)
    setSlowLoading(false)
    setReloadKey(value => value + 1)
    setErrorType(null)
    setSelectedRoute(null)
    setSelectedNode(null)
  }
  function retry() {
    resetView()
  }

  function handleSelectGradeAndCourse(gradeNumber: number | null, courseCode: string | null) {
    setSelectedGrade(gradeNumber)
    setSelectedCourse(courseCode)
    setRouteId('')
    resetView()
    writeStorage('rupi_selected_grade', gradeNumber != null ? String(gradeNumber) : null)
    writeStorage('rupi_selected_course', courseCode)

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
      // En tabletas compartidas, el siguiente alumno no debe heredar grado ni curso.
      clearStoredNavigation()
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
      clearStoredNavigation()
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
      clearStoredNavigation()
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
        {slowLoading && (
          <div className="app-loading-slow" role="alert">
            <p>La conexión está lenta. Tu avance sigue guardado.</p>
            <button type="button" className="status-action-btn" onClick={retry}>
              <RefreshCw aria-hidden="true" />Reintentar
            </button>
          </div>
        )}
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

  if (student.role === 'DOCENTE' && teacherViewMode === 'reports') {
    return <TeacherReportsView onBack={() => setTeacherViewMode('authoring')} onLogout={handleLogout} />
  }

  // 1.5 Panel de Autoría para Docentes (HU-05)
  if (student.role === 'DOCENTE' && teacherViewMode === 'authoring') {
    return (
      <TeacherAuthoringView
        onOpenReports={() => setTeacherViewMode('reports')}
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
          onSelectGradeAndCourse={handleSelectGradeAndCourse}
          onSelectEnrolledRoute={(versionRouteId, gradeNumber, courseCode) => {
            // También fija el grado: con `selectedGrade` en null la app volvía a esta pantalla tras cargar la ruta.
            handleSelectGradeAndCourse(gradeNumber, courseCode)
            setRouteId(versionRouteId)
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
            aria-label="Explorar Roadmaps"
          >
            <Compass aria-hidden="true" />
            <span>Explorar<span className="nav-label-extra"> Roadmaps</span></span>
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
            currentGrade={studentEnrolledGrade}
            currentCourse={selectedCourse}
            enrolledRoutes={routes}
            onSelectRoute={(versionRouteId, gradeNumber, areaCode) => {
              if (gradeNumber != null) {
                setSelectedGrade(gradeNumber)
                writeStorage('rupi_selected_grade', String(gradeNumber))
              }
              if (areaCode) {
                const code = areaCode.toUpperCase()
                let normalized: string
                if (code.includes('COM')) normalized = 'COMUNICACION'
                else if (code.includes('CYT')) normalized = 'CYT'
                else if (code.includes('MAT')) normalized = 'MATEMATICA'
                else normalized = code
                setSelectedCourse(normalized)
                writeStorage('rupi_selected_course', normalized)
              }
              setRouteId(versionRouteId)
              resetView()
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
              if (gradeNumber != null) {
                setSelectedGrade(gradeNumber)
                writeStorage('rupi_selected_grade', String(gradeNumber))
              }
              if (areaCode) {
                const code = areaCode.toUpperCase()
                let normalized: string
                if (code.includes('COM')) normalized = 'COMUNICACION'
                else if (code.includes('CYT')) normalized = 'CYT'
                else if (code.includes('MAT')) normalized = 'MATEMATICA'
                else normalized = code
                setSelectedCourse(normalized)
                writeStorage('rupi_selected_course', normalized)
              }
              setRouteId(versionRouteId)
              resetView()
              setCurrentView('camino')
            }}
            onChangeGrade={() => {
              setSelectedGrade(null)
              setSelectedCourse(null)
              writeStorage('rupi_selected_grade', null)
              writeStorage('rupi_selected_course', null)
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
                  <div className="course-grade-strip">
                    <span className="current-grade-badge">
                      <GraduationCap size={16} aria-hidden="true" />
                      <span>{selectedGrade || studentEnrolledGrade}.° de Primaria</span>
                    </span>
                    {selectedRoute && selectedRoute.grade.number !== studentEnrolledGrade && (
                      <span className="exploring-grade-pill" title="Explorando contenido de otro grado escolar">
                        <Compass size={13} aria-hidden="true" />
                        <span>Explorando</span>
                      </span>
                    )}
                  </div>
                  <h1 id="course-title">
                    {selectedRoute ? selectedRoute.title : 'Mi ruta de aprendizaje'}
                  </h1>
                </div>

                <div className="course-heading-meta">
                  {selectedRoute && (
                    <div className="course-stats-chip" aria-label="Progreso de estrellas y experiencia">
                      <span className="stat-pill stars" title="Estrellas ganadas en esta ruta">
                        <Star size={15} fill="#ffd26a" stroke="#dca331" aria-hidden="true" />
                        <strong>{selectedRoute.progress.completedNodes}</strong>
                        <span className="stat-sep">/</span>
                        <span>{selectedRoute.progress.totalNodes}</span>
                      </span>
                      <span className="stat-pill xp" title="Puntos de experiencia acumulados">
                        <Sparkles size={14} aria-hidden="true" />
                        <strong>{selectedRoute.progress.completedNodes * 50} XP</strong>
                      </span>
                    </div>
                  )}

                  <button
                    type="button"
                    className="change-grade-btn"
                    onClick={() => {
                      setSelectedGrade(null)
                      setSelectedCourse(null)
                      setRouteId('')
                      clearStoredNavigation()
                      resetView()
                    }}
                    title="Cambiar de grado o sección escolar"
                    aria-label="Cambiar de grado o sección escolar"
                  >
                    <Compass size={16} aria-hidden="true" />
                    <span>Cambiar grado</span>
                  </button>
                </div>
              </div>
            </section>

            {/* Barra interactiva de cambio ágil de materia escolar solo para rutas oficiales */}
            {!(
              selectedRoute && (
                routes.find(r => r.versionRouteId === selectedRoute.versionRouteId)?.category === 'COMUNIDAD' ||
                (selectedRoute as { category?: string }).category === 'COMUNIDAD' ||
                routes.find(r => r.versionRouteId === selectedRoute.versionRouteId)?.authorName?.toUpperCase().includes('PROFESOR') ||
                routes.find(r => r.versionRouteId === selectedRoute.versionRouteId)?.authorName?.toUpperCase().includes('CARLOS') ||
                !['COM', 'MAT', 'CYT'].includes(selectedRoute.area?.code || '')
              )
            ) && (
              <nav className="course-switch-bar" aria-label="Cambiar de materia">
                <button
                  type="button"
                  className={`course-switch-tab ${selectedCourse === 'COMUNICACION' ? 'active comunicacion' : ''}`}
                  onClick={() => handleSelectGradeAndCourse(selectedGrade, 'COMUNICACION')}
                  aria-pressed={selectedCourse === 'COMUNICACION'}
                  title="Ver ruta de Comunicación"
                >
                  <BookOpen size={18} aria-hidden="true" />
                  <span>Comunicación</span>
                </button>

                <button
                  type="button"
                  className={`course-switch-tab ${selectedCourse === 'MATEMATICA' ? 'active matematica' : ''}`}
                  onClick={() => handleSelectGradeAndCourse(selectedGrade, 'MATEMATICA')}
                  aria-pressed={selectedCourse === 'MATEMATICA'}
                  title="Ver ruta de Matemática"
                >
                  <Calculator size={18} aria-hidden="true" />
                  <span>Matemática</span>
                </button>

                <button
                  type="button"
                  className={`course-switch-tab ${selectedCourse === 'CYT' ? 'active cyt' : ''}`}
                  onClick={() => handleSelectGradeAndCourse(selectedGrade, 'CYT')}
                  aria-pressed={selectedCourse === 'CYT'}
                  title="Ver ruta de Ciencia y Tecnología"
                >
                  <FlaskConical size={18} aria-hidden="true" />
                  <span>Ciencia y Tecnología</span>
                </button>
              </nav>
            )}

            {/* Los errores se muestran fuera de los casos A y B: cuando fallan, no hay ruta cargada
                y antes quedaban ocultos tras «Ruta en preparación», sin opción de reintentar. */}
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
                  }}
                >
                  Volver a mis rutas
                </button>
              </section>
            )}

            {/* CASO A: Roadmap Habilitado para la materia y grado */}
            {isRoadmapAvailable && (
              <>

                {selectedRoute && (
                  selectedRoute.nodes.length ? (
                    <div className="adventure-layout">
                      <Roadmap
                        route={selectedRoute}
                        guideMessage={guideMessages[guideIndex]}
                        onGuideClick={() => setGuideIndex(index => (index + 1) % guideMessages.length)}
                        onSelectNode={handleOpenNode}
                      />
                      <RouteCompanion route={selectedRoute} onSelect={handleOpenNode} teacher={student.role === 'DOCENTE'} />
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

            {/* CASO B: Sin roadmap directo para la materia y grado (también sin inscripciones: ofrece explorar o usar un código) */}
            {!isRoadmapAvailable && errorType !== 'NETWORK' && errorType !== 'NOT_FOUND' && (
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

        {/* Pantalla 4: Lección Escolar Dedicada (Video explicativo previo + Retos y Preguntas didácticas) */}
        {currentView === 'leccion' && selectedNode && selectedRoute && (
          <LessonScreenView
            node={selectedNode}
            route={selectedRoute}
            activity={activity}
            loading={activityLoading}
            error={activityError}
            onBackToRoadmap={() => {
              setCurrentView('camino')
              setSelectedNode(null)
            }}
            onComplete={(result) => {
              handleActivityCompleted(result)
            }}
            onOpenBadges={() => handleOpenBadges()}
          />
        )}

        <footer className="page-footer">
          Hecho para aprender, crecer y descubrir <Heart aria-label="con cariño" />
        </footer>
      </main>

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
