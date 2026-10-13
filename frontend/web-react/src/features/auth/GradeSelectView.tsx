import { useState } from 'react'
import {
  ArrowLeft,
  ArrowRight,
  BookOpen,
  Calculator,
  Check,
  Compass,
  FlaskConical,
  GraduationCap,
  Info,
  KeyRound,
  LogOut,
  Sparkles,
  Volume2,
} from 'lucide-react'
import { RupiCharacter } from '../../components/RupiCharacter'
import { speakText } from '../../utils/speech'
import type { RouteSummary, Student } from '../../services/learningRoutesApi'

type Props = {
  student: Student
  routes?: RouteSummary[]
  initialGrade?: number | null
  onSelectGradeAndCourse: (gradeNumber: number, courseCode: string) => void
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
    subtitle: 'Primeros pasos en primaria: lectura inicial, números y descubrimientos con Rupi',
    coursesCount: 3,
    hasActiveRoadmap: true,
  },
  {
    number: 2,
    title: '2.° de primaria',
    subtitle: 'Aventuras en Ayacucho: retos matemáticos, fábulas y biodiversidad andina',
    coursesCount: 5,
    hasActiveRoadmap: true,
  },
  {
    number: 3,
    title: '3.° de primaria',
    subtitle: 'Geometría en Wari, mitos ancestrales andinos y fuerzas de la naturaleza',
    coursesCount: 3,
    hasActiveRoadmap: true,
  },
  {
    number: 4,
    title: '4.° de primaria',
    subtitle: 'Fracciones en las ferias, artículos informativos y ecosistemas del Perú',
    coursesCount: 3,
    hasActiveRoadmap: true,
  },
  {
    number: 5,
    title: '5.° de primaria',
    subtitle: 'Decimales comerciales, textos argumentativos y nutrición andina',
    coursesCount: 3,
    hasActiveRoadmap: true,
  },
  {
    number: 6,
    title: '6.° de primaria',
    subtitle: 'Proporcionalidad, crónicas periodísticas y energías renovables',
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
  initialGrade = null,
  onSelectGradeAndCourse,
  onLogout,
  onOpenJoinModal,
  onBackToTeacherAuthoring,
}: Props) {

  const [selectedGradeNumber, setSelectedGradeNumber] = useState<number | null>(initialGrade)
  const [notice, setNotice] = useState<string | null>(null)

  const selectedGradeObj = PRIMARY_GRADES.find((g) => g.number === selectedGradeNumber)

  function handleGradeClick(grade: PrimaryGradeOption) {
    setNotice(null)
    setSelectedGradeNumber(grade.number)
    speakText(`Has elegido ${grade.title}. Tienes rutas escolares activas en tus materias. Elige tu curso para continuar con Rupi.`)
  }

  function handleCourseClick(course: CourseOption) {
    if (!selectedGradeNumber) return
    speakText(`Ingresando a ${course.name} de ${selectedGradeObj?.title}. ¡Vamos a aprender!`)
    onSelectGradeAndCourse(selectedGradeNumber, course.code)
  }

  const welcomeInstruction = selectedGradeNumber
    ? `En ${selectedGradeObj?.title} tienes tus materias listas con rutas interactivas. Elige tu curso para ingresar.`
    : `¡Hola, ${student.name}! ¿En qué grado estás? Selecciona tu grado de 1.° a 6.° de primaria para ver tus materias y roadmaps.`

  return (
    <section className="grade-screen-wrapper" aria-labelledby="grade-main-title">
      <div className="grade-container">
        {/* Cabecera con Rupi y saludo */}
        <header className="grade-header">
          <div className="grade-header-top">
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <span className="grade-badge-tag">
                <GraduationCap size={16} aria-hidden="true" />
                <span>
                  {selectedGradeNumber
                    ? `${selectedGradeObj?.title.toUpperCase()} · MATERIAS`
                    : 'PRIMARIA · SELECCIÓN DE GRADO'}
                </span>
              </span>

              {selectedGradeNumber && (
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
                  selectedGradeNumber === 2
                    ? 'mathematician'
                    : 'default'
                }
                mood="jumping"
                className="grade-rupi-svg"
              />
            </div>

            <div className="grade-hero-text">
              <div className="grade-title-group">
                <h1 id="grade-main-title" className="grade-title">
                  {selectedGradeNumber ? (
                    <>
                      Materias de <span className="grade-student-name">{selectedGradeObj?.title}</span>
                    </>
                  ) : (
                    <>
                      ¿En qué grado estás, <span className="grade-student-name">{student.name}</span>?
                    </>
                  )}
                </h1>
                <button
                  type="button"
                  className="grade-listen-btn"
                  onClick={() => speakText(welcomeInstruction)}
                  aria-label="Escuchar instrucciones"
                  title="Escuchar"
                >
                  <Volume2 size={18} aria-hidden="true" />
                </button>
              </div>
              <p className="grade-subtitle">
                {selectedGradeNumber
                  ? `Tienes disponibles rutas interactivas en ${selectedGradeObj?.title}. Elige tu materia para comenzar tu aventura con Rupi.`
                  : 'Elige tu grado de primaria (1.° a 6.°) para ver los cursos que ofrecemos con Rupi.'}
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

        {/* PASO 1: Selección de Grado (1.° a 6.° de Primaria) */}
        {!selectedGradeNumber && (
          <div className="grade-cards-grid" role="list" aria-label="Grados de primaria disponibles">
            {PRIMARY_GRADES.map((grade) => (
              <div
                key={grade.number}
                role="listitem"
                className={`grade-card available ${grade.hasActiveRoadmap ? 'has-active-route' : ''}`}
                onClick={() => handleGradeClick(grade)}
              >
                <div className="grade-card-top">
                  <div className="grade-card-icon" aria-hidden="true">
                    <Compass size={24} className="icon-available" />
                  </div>

                  <span className={`grade-status-pill ${grade.hasActiveRoadmap ? 'ready active-glow' : 'ready'}`}>
                    <Sparkles size={12} aria-hidden="true" />
                    <span>Rutas escolares activas</span>
                  </span>
                </div>

                <div className="grade-card-content">
                  <h2 className="grade-card-title">{grade.title}</h2>
                  <p className="grade-card-desc">{grade.subtitle}</p>

                  <div className="grade-route-preview">
                    <BookOpen size={14} aria-hidden="true" />
                    <span>Comunicación, Matemática y Ciencia y Tecnología</span>
                  </div>
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
                    <span>Ver materias de {grade.title}</span>
                    <Check size={18} aria-hidden="true" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* PASO 2: Selección de Cursos para el Grado Elegido (Comunicación, Matemática, CYT) */}
        {selectedGradeNumber && (
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
                    <span className="course-area-pill">
                      Ruta activa CNEB
                    </span>
                  </div>

                  <div className="course-card-body">
                    <h2 className="course-card-title">{course.name}</h2>
                    <p className="course-card-desc">{course.description}</p>
                    <div className="course-card-badge-row">
                      <Sparkles size={14} aria-hidden="true" />
                      <span>Roadmap habilitado · Paradas interactivas</span>
                    </div>
                  </div>

                  <div className="course-card-footer">
                    <button
                      type="button"
                      className="course-card-btn active-roadmap-btn"
                      onClick={(e) => {
                        e.stopPropagation()
                        handleCourseClick(course)
                      }}
                      aria-label={`Ingresar al roadmap de ${course.name}`}
                    >
                      {course.code === 'COMUNICACION' && <BookOpen size={18} aria-hidden="true" />}
                      {course.code === 'MATEMATICA' && <Calculator size={18} aria-hidden="true" />}
                      {course.code === 'CYT' && <FlaskConical size={18} aria-hidden="true" />}
                      <span>Ingresar al roadmap de {course.name}</span>
                      <ArrowRight size={18} aria-hidden="true" />
                    </button>
                  </div>
                </div>
              )
            })}
          </div>
        )}

        {/* Mensaje de apoyo de Rupi */}
        <footer className="grade-footer-note">
          <Sparkles size={16} aria-hidden="true" />
          <span>
            {selectedGradeNumber
              ? `Cursos alineados al Currículo Nacional de Educación Básica para ${selectedGradeObj?.title}. ¡Avanza a tu propio ritmo con Rupi!`
              : 'Selecciona tu grado de primaria para ver los cursos que ofrecemos con Rupi.'}
          </span>
        </footer>
      </div>
    </section>
  )
}
