import { useState, useEffect } from 'react'
import {
  ArrowLeft,
  ArrowRight,
  BookOpen,
  Calculator,
  Compass,
  FlaskConical,
  GraduationCap,
  Info,
  KeyRound,
  LogOut,
  Sparkles,
  Users,
  Volume2,
} from 'lucide-react'
import { RupiCharacter } from '../../components/RupiCharacter'
import { RoadmapCard } from '../../components/RoadmapCard'
import { speakText } from '../../utils/speech'
import {
  searchAlternativeRoutes,
  enrollInRoute,
  type RouteSearchResult,
  type RouteSummary,
  type Student,
} from '../../services/learningRoutesApi'

type Props = {
  student: Student
  routes?: RouteSummary[]
  initialGrade?: number | null
  initialSection?: 'RUPI' | 'COMUNIDAD' | null
  onSelectGradeAndCourse: (gradeNumber: number, courseCode: string) => void
  onSelectEnrolledRoute?: (versionRouteId: string, gradeNumber: number, courseCode: string) => void
  onLogout: () => void
  onOpenJoinModal?: () => void
  onBackToTeacherAuthoring?: () => void
}

interface PrimaryGradeOption {
  number: number
  title: string
  subtitle: string
  coursesCount: number
  hasActiveRoadmap: boolean
}

const PRIMARY_GRADES: PrimaryGradeOption[] = [
  {
    number: 1,
    title: '1.° de primaria',
    subtitle: 'Lectura inicial, primeros números y descubrimientos con Rupi.',
    coursesCount: 3,
    hasActiveRoadmap: true,
  },
  {
    number: 2,
    title: '2.° de primaria',
    subtitle: 'Retos matemáticos, fábulas andinas y seres vivos de Ayacucho.',
    coursesCount: 5,
    hasActiveRoadmap: true,
  },
  {
    number: 3,
    title: '3.° de primaria',
    subtitle: 'Geometría en Wari, mitos andinos y fuerzas de la naturaleza.',
    coursesCount: 3,
    hasActiveRoadmap: true,
  },
  {
    number: 4,
    title: '4.° de primaria',
    subtitle: 'Fracciones en ferias, lecturas informativas y ecosistemas del Perú.',
    coursesCount: 3,
    hasActiveRoadmap: true,
  },
  {
    number: 5,
    title: '5.° de primaria',
    subtitle: 'Operaciones decimales, textos de opinión y nutrición andina.',
    coursesCount: 3,
    hasActiveRoadmap: true,
  },
  {
    number: 6,
    title: '6.° de primaria',
    subtitle: 'Proporciones, crónicas periodísticas y energías del Perú.',
    coursesCount: 3,
    hasActiveRoadmap: true,
  },
]

interface CourseOption {
  code: string
  name: string
  role: 'reader' | 'mathematician' | 'scientist'
  description: string
  badgeText: string
}

const BASE_COURSES: CourseOption[] = [
  {
    code: 'COMUNICACION',
    name: 'Comunicación',
    role: 'reader',
    description: 'Comprensión lectora, redacción de textos, cuentos y relatos peruanos.',
    badgeText: 'Letras y relatos',
  },
  {
    code: 'MATEMATICA',
    name: 'Matemática',
    role: 'mathematician',
    description: 'Geometría andina, cálculo, operaciones y ruta activa de desafíos numéricos.',
    badgeText: 'Ruta activa en Ayacucho',
  },
  {
    code: 'CYT',
    name: 'Ciencia y Tecnología (CYT)',
    role: 'scientist',
    description: 'Método científico, seres vivos, ecosistemas del Perú y proyectos de indagación.',
    badgeText: 'Indagación y experimentos',
  },
]

export function GradeSelectView({
  student,
  routes = [],
  initialGrade = null,
  initialSection = null,
  onSelectGradeAndCourse,
  onSelectEnrolledRoute,
  onLogout,
  onOpenJoinModal,
  onBackToTeacherAuthoring,
}: Props) {
  // Estado principal de la sección: null = pantalla inicial de 2 botones (RUPI vs COMUNIDAD)
  const [activeSection, setActiveSection] = useState<'RUPI' | 'COMUNIDAD' | null>(initialSection)
  const [selectedGradeNumber, setSelectedGradeNumber] = useState<number | null>(initialGrade)
  const [notice, setNotice] = useState<string | null>(null)

  // Rutas de comunidad y profesores
  const [communityRoutes, setCommunityRoutes] = useState<RouteSearchResult[]>([])
  const [loadingCommunity, setLoadingCommunity] = useState(false)

  const selectedGradeObj = PRIMARY_GRADES.find((g) => g.number === selectedGradeNumber)

  // Cargar catálogo de rutas comunitarias y de docentes cuando se entra a esa sección
  useEffect(() => {
    if (activeSection !== 'COMUNIDAD') return
    let isCancelled = false
    async function loadCommunity() {
      setLoadingCommunity(true)
      try {
        const res = await searchAlternativeRoutes({ limit: 40 })
        if (!isCancelled) {
          const list = (res.routes || []).filter(
            (r) => r.category === 'COMUNIDAD' || (r.authorName && !r.authorName.toUpperCase().includes('RUPI'))
          )
          setCommunityRoutes(list)
        }
      } catch {
        // En caso de fallo de red, se mantiene la lista local
      } finally {
        if (!isCancelled) setLoadingCommunity(false)
      }
    }
    void loadCommunity()
    return () => {
      isCancelled = true
    }
  }, [activeSection])

  function handleGradeClick(grade: PrimaryGradeOption) {
    setNotice(null)
    setSelectedGradeNumber(grade.number)
  }

  function handleCourseClick(course: CourseOption) {
    if (!selectedGradeNumber) return
    onSelectGradeAndCourse(selectedGradeNumber, course.code)
  }

  async function handleSelectCommunityRoute(r: RouteSearchResult) {
    if (r.isEnrolled || routes.some((x) => x.versionRouteId === r.versionRouteId)) {
      if (onSelectEnrolledRoute) {
        onSelectEnrolledRoute(r.versionRouteId, r.gradeNumber, r.areaCode)
      } else {
        onSelectGradeAndCourse(r.gradeNumber, r.areaCode)
      }
      return
    }

    try {
      await enrollInRoute(r.versionRouteId)
    } catch {
      // Continuar incluso si ya estaba inscrita
    } finally {
      if (onSelectEnrolledRoute) {
        onSelectEnrolledRoute(r.versionRouteId, r.gradeNumber, r.areaCode)
      } else {
        onSelectGradeAndCourse(r.gradeNumber, r.areaCode)
      }
    }
  }

  // Filtrar rutas inscritas según la sección actual
  const enrolledInSelectedGrade = routes.filter(
    (r) => r.category !== 'COMUNIDAD' && selectedGradeNumber !== null && r.grade.number === selectedGradeNumber
  )
  const enrolledCommunityRoutes = routes.filter((r) => r.category === 'COMUNIDAD')

  // Mensaje para voz interactiva
  const welcomeInstruction =
    activeSection === null
      ? `¡Hola, ${student.name}! Bienvenido a RUPI. Elige si deseas entrar a los Roadmaps oficiales de RUPI o a Comunidad y Profesores.`
      : activeSection === 'RUPI'
      ? selectedGradeNumber
        ? `Materias de ${selectedGradeObj?.title}. Selecciona tu curso para comenzar.`
        : `Roadmaps RUPI. Elige tu grado escolar para explorar las materias.`
      : `Comunidad y Profesores. Aquí encuentras las rutas de tu aula o puedes ingresar con tu código de clase.`

  return (
    <section className="grade-screen-wrapper" aria-labelledby="grade-main-title">
      <div className="grade-container">
        {/* ============================================================== */}
        {/* CABECERA PRINCIPAL CON RUPI Y ACCIONES                         */}
        {/* ============================================================== */}
        <header className="grade-header">
          <div className="grade-header-top">
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <span className="grade-badge-tag">
                <GraduationCap size={16} aria-hidden="true" />
                <span>
                  {activeSection === null
                    ? 'AULA RUPI'
                    : activeSection === 'RUPI'
                    ? selectedGradeNumber
                      ? `${selectedGradeObj?.title.toUpperCase()}`
                      : 'ROADMAPS OFICIALES · CNEB'
                    : 'COMUNIDAD Y PROFESORES'}
                </span>
              </span>

              {/* Botón para regresar a los 2 botones de sección */}
              {activeSection !== null && (
                <button
                  type="button"
                  className="back-to-sections-btn"
                  onClick={() => {
                    setActiveSection(null)
                    setSelectedGradeNumber(null)
                    setNotice(null)
                  }}
                  title="Volver a elegir entre Roadmaps RUPI o Comunidad y Profesores"
                >
                  <ArrowLeft size={16} aria-hidden="true" />
                  <span>Volver a secciones</span>
                </button>
              )}

              {/* Botón para cambiar grado dentro de Roadmaps RUPI */}
              {activeSection === 'RUPI' && selectedGradeNumber && (
                <button
                  type="button"
                  className="back-to-grades-btn"
                  onClick={() => {
                    setSelectedGradeNumber(null)
                    setNotice(null)
                  }}
                  title="Elegir otro grado de primaria"
                >
                  <ArrowLeft size={15} aria-hidden="true" />
                  <span>Cambiar grado</span>
                </button>
              )}
            </div>

            <div className="grade-top-actions">
              {student.role === 'DOCENTE' && onBackToTeacherAuthoring && (
                <button
                  type="button"
                  className="grade-join-code-btn"
                  onClick={onBackToTeacherAuthoring}
                  title="Volver al panel de autoría docente"
                  aria-label="Volver a autoría docente"
                  style={{ background: '#31724b', color: '#ffffff', borderColor: '#173e30' }}
                >
                  <Compass size={16} aria-hidden="true" />
                  <span>Panel Docente</span>
                </button>
              )}

              {onOpenJoinModal && (
                <button
                  type="button"
                  className="grade-join-code-btn"
                  onClick={onOpenJoinModal}
                  title="Unirme con código alfanumérico proporcionado por mi profesor"
                  aria-label="Unirme con código de clase docente"
                >
                  <KeyRound size={16} aria-hidden="true" />
                  <span>Código de clase</span>
                </button>
              )}

              <button
                type="button"
                className="grade-logout-btn"
                onClick={onLogout}
                aria-label="Cerrar sesión y cambiar de estudiante"
                title="Cerrar sesión"
              >
                <LogOut size={16} aria-hidden="true" />
                <span>Salir</span>
              </button>
            </div>
          </div>

          <div className="grade-hero-row">
            <div className="grade-rupi-holder" aria-hidden="true">
              <RupiCharacter
                role={
                  activeSection === 'RUPI' && selectedGradeNumber === 2
                    ? 'mathematician'
                    : 'default'
                }
                mood={activeSection === null ? 'jumping' : 'cheering'}
                className="grade-rupi-svg"
              />
            </div>

            <div className="grade-hero-text">
              <div className="grade-title-group">
                <h1 id="grade-main-title" className="grade-title">
                  {activeSection === null ? (
                    <>
                      ¡Hola, <span className="grade-student-name">{student.name}</span>! ¿Qué vamos a aprender hoy?
                    </>
                  ) : activeSection === 'RUPI' ? (
                    selectedGradeNumber ? (
                      <>
                        Materias de <span className="grade-student-name">{selectedGradeObj?.title}</span>
                      </>
                    ) : (
                      <>
                        Roadmaps RUPI · <span className="grade-student-name">Elige tu grado</span>
                      </>
                    )
                  ) : (
                    <>
                      Comunidad y Profesores · <span className="grade-student-name">Rutas escolares</span>
                    </>
                  )}
                </h1>
                <button
                  type="button"
                  className="grade-listen-btn"
                  onClick={() => speakText(welcomeInstruction)}
                  aria-label="Escuchar instrucciones con Rupi"
                  title="Escuchar con Rupi"
                >
                  <Volume2 size={18} aria-hidden="true" />
                </button>
              </div>
              <p className="grade-subtitle">
                {activeSection === null
                  ? 'Elige una de las dos secciones para comenzar tu camino:'
                  : activeSection === 'RUPI'
                  ? selectedGradeNumber
                    ? `Selecciona la materia que deseas practicar hoy en ${selectedGradeObj?.title}:`
                    : 'Selecciona tu grado de primaria para ver tus cursos oficiales alineados al CNEB:'
                  : 'Caminos didácticos preparados por tus profesores o ingresa con tu código de aula:'}
              </p>
            </div>
          </div>
        </header>

        {notice && (
          <div className="grade-notice-banner" role="status">
            <Info size={18} aria-hidden="true" />
            <p>{notice}</p>
          </div>
        )}

        {/* ============================================================== */}
        {/* PASO 1: PANTALLA DE ENTRADA CON 2 BOTONES PRINCIPALES           */}
        {/* (Estudiante ingresa -> Dos secciones: Roadmaps RUPI / Comunidad)*/}
        {/* ============================================================== */}
        {activeSection === null && (
          <div className="section-two-cards-grid" role="group" aria-label="Secciones de aprendizaje disponibles">
            {/* BOTÓN 1: ROADMAPS RUPI */}
            <div
              className="section-card card-rupi"
              role="button"
              tabIndex={0}
              onClick={() => setActiveSection('RUPI')}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault()
                  setActiveSection('RUPI')
                }
              }}
              aria-label="Entrar a Roadmaps RUPI oficiales"
            >
              <div className="section-card-top">
                <div className="section-card-icon-disc" aria-hidden="true">
                  <GraduationCap size={32} />
                </div>
                <span className="section-card-badge">
                  <Sparkles size={12} aria-hidden="true" />
                  OFICIALES · CNEB
                </span>
              </div>

              <div className="section-card-body">
                <h2 className="section-card-title">Roadmaps RUPI</h2>
                <p className="section-card-desc">
                  Rutas escolares oficiales para 1.° a 6.° de primaria con retos interactivos de Matemática, Comunicación y Ciencia.
                </p>
              </div>

              <button
                type="button"
                className="section-enter-btn btn-rupi"
                onClick={(e) => {
                  e.stopPropagation()
                  setActiveSection('RUPI')
                }}
              >
                <span>Ver grados de primaria</span>
                <ArrowRight size={18} aria-hidden="true" />
              </button>
            </div>

            {/* BOTÓN 2: COMUNIDAD Y PROFESORES */}
            <div
              className="section-card card-comunidad"
              role="button"
              tabIndex={0}
              onClick={() => setActiveSection('COMUNIDAD')}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault()
                  setActiveSection('COMUNIDAD')
                }
              }}
              aria-label="Entrar a Comunidad y Profesores"
            >
              <div className="section-card-top">
                <div className="section-card-icon-disc" aria-hidden="true">
                  <Users size={32} />
                </div>
                <span className="section-card-badge">
                  <KeyRound size={12} aria-hidden="true" />
                  DOCENTES Y AULAS
                </span>
              </div>

              <div className="section-card-body">
                <h2 className="section-card-title">Comunidad y Profesores</h2>
                <p className="section-card-desc">
                  Rutas personalizadas por tus profesores para tu salón de clases y acceso directo mediante código de clase.
                </p>
              </div>

              <button
                type="button"
                className="section-enter-btn btn-comunidad"
                onClick={(e) => {
                  e.stopPropagation()
                  setActiveSection('COMUNIDAD')
                }}
              >
                <span>Ver rutas de clase</span>
                <ArrowRight size={18} aria-hidden="true" />
              </button>
            </div>
          </div>
        )}

        {/* ============================================================== */}
        {/* SECCIÓN 1: ROADMAPS RUPI (AHÍ RECIÉN APARECEN LOS ROADMAPS)     */}
        {/* ============================================================== */}
        {activeSection === 'RUPI' && (
          <>
            {/* Grados de Primaria (1.° a 6.°): Se muestran primero; los roadmaps aparecen solo al hacer clic en un grado */}

            {/* Grados de Primaria (1.° a 6.°) */}
            {!selectedGradeNumber && (
              <div className="grade-cards-grid" role="list" aria-label="Grados de primaria disponibles en RUPI">
                {PRIMARY_GRADES.map((grade) => (
                  <div
                    key={grade.number}
                    role="listitem"
                    className={`grade-card available ${grade.hasActiveRoadmap ? 'has-active-route' : ''}`}
                    onClick={() => handleGradeClick(grade)}
                  >
                    <div className="grade-card-top">
                      <div className="grade-card-icon" aria-hidden="true">
                        <span style={{ fontWeight: 900, fontSize: '18px', color: '#173e30' }}>{grade.number}.°</span>
                      </div>

                      <span className="grade-status-pill ready">
                        <span>{grade.coursesCount} cursos CNEB</span>
                      </span>
                    </div>

                    <div className="grade-card-content">
                      <h2 className="grade-card-title">{grade.title}</h2>
                      <p className="grade-card-desc">{grade.subtitle}</p>
                    </div>

                    <div className="grade-card-footer">
                      <button
                        type="button"
                        className="grade-select-btn"
                        disabled={!grade.coursesCount}
                        onClick={(e) => {
                          e.stopPropagation()
                          handleGradeClick(grade)
                        }}
                        aria-label={`Ver materias de ${grade.title}`}
                      >
                        <span>Ver materias</span>
                        <ArrowRight size={18} aria-hidden="true" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Materias del Grado Elegido (aparecen tras hacer clic en un grado) */}
            {selectedGradeNumber && (
              <>
                {enrolledInSelectedGrade.length > 0 && (
                  <section className="enrolled-welcome-routes-section" aria-label="Ruta en marcha en este grado">
                    <div className="enrolled-welcome-header">
                      <span className="enrolled-welcome-tag">
                        <Sparkles size={15} aria-hidden="true" />
                        <span>TU AVANCE EN ESTE GRADO</span>
                      </span>
                      <h2 className="enrolled-welcome-title">Continuar en {selectedGradeObj?.title}</h2>
                    </div>

                    <div className="enrolled-welcome-cards-grid">
                      {enrolledInSelectedGrade.map((route) => {
                        const areaCode = (route.area.code || '').toUpperCase()
                        const courseCode = areaCode.includes('COM')
                          ? 'COMUNICACION'
                          : areaCode.includes('CYT')
                          ? 'CYT'
                          : 'MATEMATICA'

                        return (
                          <RoadmapCard
                            key={route.versionRouteId}
                            route={route}
                            isEnrolled={true}
                            completedNodes={route.completedNodes}
                            totalNodes={route.totalNodes}
                            onAction={() => {
                              if (onSelectEnrolledRoute) {
                                onSelectEnrolledRoute(route.versionRouteId, route.grade.number, courseCode)
                              } else {
                                onSelectGradeAndCourse(route.grade.number, courseCode)
                              }
                            }}
                          />
                        )
                      })}
                    </div>

                    <div className="enrolled-welcome-divider">
                      <span>O ELIGE OTRA MATERIA DE {selectedGradeObj?.title.toUpperCase()}</span>
                    </div>
                  </section>
                )}

                <div className="courses-cards-grid" role="list" aria-label={`Materias de ${selectedGradeObj?.title}`}>
                {BASE_COURSES.map((course) => {
                  const cardClass =
                    course.code === 'COMUNICACION'
                      ? 'comunicacion'
                      : course.code === 'MATEMATICA'
                      ? 'matematica'
                      : 'cyt'

                  return (
                    <div
                      key={course.code}
                      role="listitem"
                      className={`course-card ${cardClass} roadmap-enabled`}
                      onClick={() => handleCourseClick(course)}
                    >
                      <div className="course-card-header">
                        <div className="course-mascot-avatar" aria-hidden="true">
                          <RupiCharacter role={course.role} mood="cheering" />
                        </div>
                        <span className="course-area-pill">CNEB · Con videos</span>
                      </div>

                      <div className="course-card-body">
                        <h2 className="course-card-title">{course.name}</h2>
                        <p className="course-card-desc">{course.description}</p>
                      </div>

                      <div className="course-card-footer">
                        <button
                          type="button"
                          className="course-card-btn active-roadmap-btn"
                          onClick={(e) => {
                            e.stopPropagation()
                            handleCourseClick(course)
                          }}
                          aria-label={`Empezar curso de ${course.name}`}
                        >
                          {course.code === 'COMUNICACION' && <BookOpen size={18} aria-hidden="true" />}
                          {course.code === 'MATEMATICA' && <Calculator size={18} aria-hidden="true" />}
                          {course.code === 'CYT' && <FlaskConical size={18} aria-hidden="true" />}
                          <span>Empezar {course.name}</span>
                          <ArrowRight size={18} aria-hidden="true" />
                        </button>
                      </div>
                    </div>
                  )
                })}
              </div>
            </>
          )}
        </>
      )}

        {/* ============================================================== */}
        {/* SECCIÓN 2: COMUNIDAD Y PROFESORES (AHÍ RECIÉN APARECEN)        */}
        {/* ============================================================== */}
        {activeSection === 'COMUNIDAD' && (
          <div className="community-section-content" aria-label="Rutas de Comunidad y Profesores">
            {/* Banner para ingresar código de clase del profesor */}
            <div className="community-join-card">
              <div className="community-join-info">
                <div className="community-join-icon-wrap" aria-hidden="true">
                  <KeyRound size={24} />
                </div>
                <div className="community-join-text">
                  <h3>¿Tu profesor te dio un código de clase?</h3>
                  <p>Ingresa el código alfanumérico para inscribirte al roadmap creado para tu aula.</p>
                </div>
              </div>
              {onOpenJoinModal && (
                <button
                  type="button"
                  className="community-join-action-btn"
                  onClick={onOpenJoinModal}
                  aria-label="Abrir ventana para ingresar código de clase"
                >
                  <KeyRound size={16} aria-hidden="true" />
                  <span>Ingresar código de clase</span>
                </button>
              )}
            </div>

            {/* Rutas creadas por profesores y comunidad */}
            <div className="enrolled-welcome-header">
              <span className="enrolled-welcome-tag">
                <Users size={15} aria-hidden="true" />
                <span>RUTAS DE TUS DOCENTES</span>
              </span>
              <h2 className="enrolled-welcome-title">Roadmaps de Profesores y Comunidad</h2>
              <p className="enrolled-welcome-desc">
                Caminos didácticos preparados por maestros para complementar tus aprendizajes escolares:
              </p>
            </div>

            {loadingCommunity ? (
              <p style={{ textAlign: 'center', padding: '24px', color: '#556c60' }}>
                Cargando roadmaps de la comunidad escolar...
              </p>
            ) : communityRoutes.length > 0 ? (
              <div className="enrolled-welcome-cards-grid">
                {communityRoutes.map((route) => {
                  const isEnrolled = route.isEnrolled || routes.some((r) => r.versionRouteId === route.versionRouteId)
                  return (
                    <RoadmapCard
                      key={route.versionRouteId}
                      route={{
                        versionRouteId: route.versionRouteId,
                        title: route.routeTitle,
                        grade: { number: route.gradeNumber, name: route.gradeName || `${route.gradeNumber}.° de Primaria` },
                        area: { name: route.areaName, code: route.areaCode },
                        enrollmentState: isEnrolled ? 'ACTIVO' : 'DISPONIBLE',
                        completedNodes: 0,
                        totalNodes: route.totalNodes,
                        lastVisitedAt: null,
                      }}
                      isEnrolled={isEnrolled}
                      completedNodes={0}
                      totalNodes={route.totalNodes}
                      onAction={() => void handleSelectCommunityRoute(route)}
                    />
                  )
                })}
              </div>
            ) : enrolledCommunityRoutes.length > 0 ? (
              <div className="enrolled-welcome-cards-grid">
                {enrolledCommunityRoutes.map((route) => {
                  const areaCode = (route.area.code || '').toUpperCase()
                  const courseCode = areaCode.includes('COM')
                    ? 'COMUNICACION'
                    : areaCode.includes('CYT')
                    ? 'CYT'
                    : 'MATEMATICA'

                  return (
                    <RoadmapCard
                      key={route.versionRouteId}
                      route={route}
                      isEnrolled={true}
                      completedNodes={route.completedNodes}
                      totalNodes={route.totalNodes}
                      onAction={() => {
                        if (onSelectEnrolledRoute) {
                          onSelectEnrolledRoute(route.versionRouteId, route.grade.number, courseCode)
                        } else {
                          onSelectGradeAndCourse(route.grade.number, courseCode)
                        }
                      }}
                    />
                  )
                })}
              </div>
            ) : (
              <div className="community-empty-card">
                <RupiCharacter mood="thinking" />
                <h3>Aún no tienes rutas de aula asignadas</h3>
                <p>
                  Pídele el código de clase a tu docente o ingresa el código alfanumérico arriba para sumarte a la ruta de tu salón.
                </p>
                {onOpenJoinModal && (
                  <button
                    type="button"
                    className="community-join-action-btn"
                    onClick={onOpenJoinModal}
                  >
                    <KeyRound size={16} aria-hidden="true" />
                    <span>Ingresar mi código de clase</span>
                  </button>
                )}
              </div>
            )}
          </div>
        )}

        {/* Pie de página con recordatorio de Rupi */}
        <footer className="grade-footer-note">
          <Sparkles size={16} aria-hidden="true" />
          <span>
            {activeSection === null
              ? 'RUPI · Plataforma educativa para estudiantes de primaria de escuelas del Perú.'
              : activeSection === 'RUPI'
              ? 'Cursos y paradas alineados al Currículo Nacional de Educación Básica (CNEB).'
              : 'Espacio de colaboración pedagógica entre docentes, aulas y estudiantes.'}
          </span>
        </footer>
      </div>
    </section>
  )
}
