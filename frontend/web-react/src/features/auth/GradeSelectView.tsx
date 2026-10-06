import { useState } from 'react'
import {
  ArrowLeft,
  BookOpen,
  Calculator,
  Check,
  Compass,
  FlaskConical,
  GraduationCap,
  Info,
  LogOut,
  Sparkles,
  Volume2,
} from 'lucide-react'
import { RupiCharacter } from '../../components/RupiCharacter'
import { speakText } from '../../utils/speech'
import type { Student } from '../../services/learningRoutesApi'

type Props = {
  student: Student
  initialGrade?: number | null
  onSelectGradeAndCourse: (gradeNumber: number, courseCode: string) => void
  onLogout: () => void
}

interface SecondaryGradeOption {
  number: number
  title: string
  subtitle: string
  coursesCount: number
}

const SECONDARY_GRADES: SecondaryGradeOption[] = [
  {
    number: 1,
    title: '1.° de secundaria',
    subtitle: 'Inicio de secundaria: Comunicación, Matemática y CYT listos',
    coursesCount: 3,
  },
  {
    number: 2,
    title: '2.° de secundaria',
    subtitle: 'Consolidación de competencias y retos escolares',
    coursesCount: 3,
  },
  {
    number: 3,
    title: '3.° de secundaria',
    subtitle: 'Razonamiento científico y literatura analítica',
    coursesCount: 3,
  },
  {
    number: 4,
    title: '4.° de secundaria',
    subtitle: 'Pensamiento crítico y proyectos de indagación',
    coursesCount: 3,
  },
  {
    number: 5,
    title: '5.° de secundaria',
    subtitle: 'Liderazgo escolar y preparación académica',
    coursesCount: 3,
  },
  {
    number: 6,
    title: '6.° de secundaria',
    subtitle: 'Graduación y proyectos de impacto en el Perú',
    coursesCount: 3,
  },
]

interface CourseOption {
  code: string
  name: string
  role: 'reader' | 'mathematician' | 'scientist'
  description: string
  badgeText: string
  available: boolean
}

const BASE_COURSES: CourseOption[] = [
  {
    code: 'COMUNICACION',
    name: 'Comunicación',
    role: 'reader',
    description: 'Comprensión lectora, redacción de textos, cuentos y relatos peruanos.',
    badgeText: 'Letras y literatura',
    available: true,
  },
  {
    code: 'MATEMATICA',
    name: 'Matemática',
    role: 'mathematician',
    description: 'Álgebra, geometría andina, cálculo y ruta activa de desafíos numéricos.',
    badgeText: 'Ruta activa en Ayacucho',
    available: true,
  },
  {
    code: 'CYT',
    name: 'Ciencia y Tecnología (CYT)',
    role: 'scientist',
    description: 'Método científico, seres vivos, ecosistemas del Perú y proyectos de indagación.',
    badgeText: 'Indagación y experimentos',
    available: true,
  },
]

export function GradeSelectView({
  student,
  initialGrade = null,
  onSelectGradeAndCourse,
  onLogout,
}: Props) {
  const [selectedGradeNumber, setSelectedGradeNumber] = useState<number | null>(initialGrade)
  const [notice, setNotice] = useState<string | null>(null)

  const selectedGradeObj = SECONDARY_GRADES.find((g) => g.number === selectedGradeNumber)

  function handleGradeClick(grade: SecondaryGradeOption) {
    setNotice(null)
    setSelectedGradeNumber(grade.number)
    speakText(`Has elegido ${grade.title}. Ahora selecciona tu curso: Comunicación, Matemática o Ciencia y Tecnología.`)
  }

  function handleCourseClick(course: CourseOption) {
    if (!selectedGradeNumber) return
    onSelectGradeAndCourse(selectedGradeNumber, course.code)
  }

  const welcomeInstruction = selectedGradeNumber
    ? `En ${selectedGradeObj?.title} tienes disponibles Comunicación, Matemática y Ciencia y Tecnología. Elige tu curso para ingresar.`
    : `¡Hola, ${student.name}! ¿En qué grado estás? Selecciona tu grado de 1.° a 6.° de secundaria para ver tus cursos.`

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
                    ? `${selectedGradeObj?.title.toUpperCase()} · CURSOS`
                    : 'SECUNDARIA · SELECCIÓN DE GRADO'}
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
                  title="Elegir otro grado de secundaria"
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
                  selectedGradeNumber
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
                      Cursos de <span className="grade-student-name">{selectedGradeObj?.title}</span>
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
                  ? 'Para este grado tienes Comunicación, Matemática y CYT. Selecciona tu materia para entrar a tu aula.'
                  : 'Elige tu año de secundaria (1.° a 6.°) para ver los cursos que ofrecemos.'}
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

        {/* PASO 1: Selección de Grado (1.° a 6.° de Secundaria) */}
        {!selectedGradeNumber && (
          <div className="grade-cards-grid" role="list" aria-label="Grados de secundaria disponibles">
            {SECONDARY_GRADES.map((grade) => (
              <div
                key={grade.number}
                role="listitem"
                className="grade-card available"
                onClick={() => handleGradeClick(grade)}
              >
                <div className="grade-card-top">
                  <div className="grade-card-icon" aria-hidden="true">
                    <Compass size={24} className="icon-available" />
                  </div>

                  <span className="grade-status-pill ready">
                    <Sparkles size={12} aria-hidden="true" />
                    <span>3 cursos listos</span>
                  </span>
                </div>

                <div className="grade-card-content">
                  <h2 className="grade-card-title">{grade.title}</h2>
                  <p className="grade-card-desc">{grade.subtitle}</p>

                  <div className="grade-route-preview">
                    <BookOpen size={14} aria-hidden="true" />
                    <span>Comunicación, Matemática y CYT</span>
                  </div>
                </div>

                <div className="grade-card-footer">
                  <button
                    type="button"
                    className="grade-select-btn"
                    onClick={(e) => {
                      e.stopPropagation()
                      handleGradeClick(grade)
                    }}
                    aria-label={`Ver cursos de ${grade.title}`}
                  >
                    <span>Ver cursos de {grade.title}</span>
                    <Check size={18} aria-hidden="true" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* PASO 2: Selección de Cursos para el Grado Elegido (Comunicación, Matemática, CYT) */}
        {selectedGradeNumber && (
          <div className="courses-cards-grid" role="list" aria-label={`Cursos de ${selectedGradeObj?.title}`}>
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
                  className={`course-card ${cardClass}`}
                  onClick={() => handleCourseClick(course)}
                >
                  <div className="course-card-header">
                    <div className="course-mascot-avatar" aria-hidden="true">
                      <RupiCharacter role={course.role} mood="idle" />
                    </div>
                    <span className="course-area-pill">{course.badgeText}</span>
                  </div>

                  <div className="course-card-body">
                    <h2 className="course-card-title">{course.name}</h2>
                    <p className="course-card-desc">{course.description}</p>
                    <div className="course-card-badge-row">
                      <Sparkles size={14} aria-hidden="true" />
                      <span>Ruta guiada por {course.name}</span>
                    </div>
                  </div>

                  <div className="course-card-footer">
                    <button
                      type="button"
                      className="course-card-btn"
                      onClick={(e) => {
                        e.stopPropagation()
                        handleCourseClick(course)
                      }}
                      aria-label={`Entrar al curso de ${course.name}`}
                    >
                      {course.code === 'COMUNICACION' && <BookOpen size={18} aria-hidden="true" />}
                      {course.code === 'MATEMATICA' && <Calculator size={18} aria-hidden="true" />}
                      {course.code === 'CYT' && <FlaskConical size={18} aria-hidden="true" />}
                      <span>Entrar a {course.name}</span>
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
              ? `Cursos alineados al Currículo Nacional de Educación Básica para ${selectedGradeObj?.title}.`
              : 'Selecciona tu grado de secundaria para ver los cursos que ofrecemos con Rupi.'}
          </span>
        </footer>
      </div>
    </section>
  )
}
