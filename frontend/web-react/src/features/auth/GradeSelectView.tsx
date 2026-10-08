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
  Lock,
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
    subtitle: 'Primeros pasos en primaria: lectura inicial, números y descubrimientos',
    coursesCount: 3,
    hasActiveRoadmap: false,
  },
  {
    number: 2,
    title: '2.° de primaria',
    subtitle: 'Ruta activa en Ayacucho: retos matemáticos y aventura con Rupi',
    coursesCount: 3,
    hasActiveRoadmap: true,
  },
  {
    number: 3,
    title: '3.° de primaria',
    subtitle: 'Consolidación de operaciones, comprensión lectora y naturaleza',
    coursesCount: 3,
    hasActiveRoadmap: false,
  },
  {
    number: 4,
    title: '4.° de primaria',
    subtitle: 'Resolución de problemas, proyectos de indagación y relatos peruanos',
    coursesCount: 3,
    hasActiveRoadmap: false,
  },
  {
    number: 5,
    title: '5.° de primaria',
    subtitle: 'Pensamiento lógico, redacción y ecosistemas del Perú',
    coursesCount: 3,
    hasActiveRoadmap: false,
  },
  {
    number: 6,
    title: '6.° de primaria',
    subtitle: 'Consolidación de primaria y preparación para nuevos retos escolares',
    coursesCount: 3,
    hasActiveRoadmap: false,
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
  routes: _routes,
  initialGrade = null,
  onSelectGradeAndCourse,
  onLogout,
}: Props) {
  const [selectedGradeNumber, setSelectedGradeNumber] = useState<number | null>(initialGrade)
  const [notice, setNotice] = useState<string | null>(null)

  const selectedGradeObj = PRIMARY_GRADES.find((g) => g.number === selectedGradeNumber)

  function handleGradeClick(grade: PrimaryGradeOption) {
    setNotice(null)
    setSelectedGradeNumber(grade.number)
    if (grade.number === 2) {
      speakText(`Has elegido ${grade.title}. En este grado tienes la ruta activa de Matemática en Ayacucho. Elige tu materia para continuar.`)
    } else {
      speakText(`Has elegido ${grade.title}. Las rutas interactivas de este grado están en preparación. Puedes elegir una materia o cambiar a 2.° de primaria para acceder al roadmap activo.`)
    }
  }

  function handleCourseClick(course: CourseOption) {
    if (!selectedGradeNumber) return
    const isRoadmapReady = selectedGradeNumber === 2 && course.code === 'MATEMATICA'

    if (!isRoadmapReady) {
      setNotice(
        selectedGradeNumber === 2
          ? `La materia de ${course.name} (2.° de primaria) aún no cuenta con un mapa interactivo disponible. Puedes cambiar de materia para explorarla, pero el roadmap activo está en Matemática.`
          : `Las materias de ${selectedGradeNumber}.° de primaria están en preparación. Puedes ver la materia, pero actualmente el único roadmap activo es 2.° de primaria · Matemática.`
      )
      speakText(
        `Esta materia aún no tiene un mapa de ruta disponible. Solo puedes acceder al roadmap interactivo en 2.° de primaria de Matemática.`
      )
    }

    // Permite cambiar de curso, ingresando a la vista del curso
    onSelectGradeAndCourse(selectedGradeNumber, course.code)
  }

  const welcomeInstruction = selectedGradeNumber
    ? (selectedGradeNumber === 2
      ? `En 2.° de primaria tienes disponible la ruta activa de Matemática en Ayacucho. Elige tu curso para ingresar.`
      : `En ${selectedGradeObj?.title} los mapas están en preparación. Elige tu curso para revisarlo o cambia a 2.° de primaria para ver la ruta activa.`)
    : `¡Hola, ${student.name}! ¿En qué grado estás? Selecciona tu grado de 1.° a 6.° de primaria para ver tus materias.`

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
                  ? (selectedGradeNumber === 2
                    ? 'Tienes disponible la ruta activa de Matemática en Ayacucho. Puedes cambiar de materia para revisar tus cursos escolares.'
                    : 'Las rutas interactivas de este grado están en preparación con tu docente. Recuerda que el roadmap activo está en 2.° de primaria · Matemática.')
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
                    <span>{grade.hasActiveRoadmap ? 'Ruta activa disponible' : '3 materias'}</span>
                  </span>
                </div>

                <div className="grade-card-content">
                  <h2 className="grade-card-title">{grade.title}</h2>
                  <p className="grade-card-desc">{grade.subtitle}</p>

                  <div className="grade-route-preview">
                    <BookOpen size={14} aria-hidden="true" />
                    <span>{grade.hasActiveRoadmap ? 'Matemática con roadmap en Ayacucho' : 'Comunicación, Matemática y CYT'}</span>
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
              const isRoadmapReady = selectedGradeNumber === 2 && course.code === 'MATEMATICA'
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
                  className={`course-card ${cardClass} ${isRoadmapReady ? 'roadmap-enabled' : 'roadmap-restricted'}`}
                  onClick={() => handleCourseClick(course)}
                >
                  <div className="course-card-header">
                    <div className="course-mascot-avatar" aria-hidden="true">
                      <RupiCharacter role={course.role} mood={isRoadmapReady ? 'cheering' : 'idle'} />
                    </div>
                    <span className="course-area-pill">
                      {isRoadmapReady ? 'Ruta activa en Ayacucho' : 'Sin roadmap activo'}
                    </span>
                  </div>

                  <div className="course-card-body">
                    <h2 className="course-card-title">{course.name}</h2>
                    <p className="course-card-desc">{course.description}</p>
                    <div className="course-card-badge-row">
                      {isRoadmapReady ? (
                        <>
                          <Sparkles size={14} aria-hidden="true" />
                          <span>Roadmap habilitado · 10 paradas</span>
                        </>
                      ) : (
                        <>
                          <Lock size={14} aria-hidden="true" />
                          <span>Roadmap en preparación · Solo cambio de materia</span>
                        </>
                      )}
                    </div>
                  </div>

                  <div className="course-card-footer">
                    <button
                      type="button"
                      className={`course-card-btn ${isRoadmapReady ? 'active-roadmap-btn' : 'preview-course-btn'}`}
                      onClick={(e) => {
                        e.stopPropagation()
                        handleCourseClick(course)
                      }}
                      aria-label={
                        isRoadmapReady
                          ? `Ingresar al roadmap de ${course.name}`
                          : `Cambiar a la materia de ${course.name} (sin roadmap)`
                      }
                    >
                      {course.code === 'COMUNICACION' && <BookOpen size={18} aria-hidden="true" />}
                      {course.code === 'MATEMATICA' && <Calculator size={18} aria-hidden="true" />}
                      {course.code === 'CYT' && <FlaskConical size={18} aria-hidden="true" />}
                      <span>
                        {isRoadmapReady ? `Ingresar al roadmap de ${course.name}` : `Cambiar a ${course.name}`}
                      </span>
                      {isRoadmapReady ? <ArrowRight size={18} aria-hidden="true" /> : <Lock size={16} aria-hidden="true" />}
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
              ? `Cursos alineados al Currículo Nacional de Educación Básica para ${selectedGradeObj?.title}. Solo 2.° de primaria Matemática tiene acceso a su mapa interactivo.`
              : 'Selecciona tu grado de primaria para ver los cursos que ofrecemos con Rupi.'}
          </span>
        </footer>
      </div>
    </section>
  )
}
