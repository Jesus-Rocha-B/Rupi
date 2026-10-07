import { useState, useEffect, type FormEvent, type KeyboardEvent } from 'react'
import {
  AlertCircle,
  ArrowRight,
  BookOpen,
  Calculator,
  Eye,
  EyeOff,
  FlaskConical,
  HelpCircle,
  KeyRound,
  Lock,
  Sparkles,
  User,
  Volume2,
} from 'lucide-react'
import { RupiCharacter, type RupiRole } from '../../components/RupiCharacter'
import { speakText } from '../../utils/speech'
import { ApiError } from '../../services/learningRoutesApi'

type Props = {
  onLogin: (username: string, password: string) => Promise<void>
  onLoginDemo: () => Promise<void>
  sessionBusy: boolean
  demoAvailable: boolean
  serviceError?: string | null
}

interface RupiCourseCharacter {
  role: RupiRole
  title: string
  area: string
  phrase: string
  badgeText: string
}

const RUPI_COURSES: RupiCourseCharacter[] = [
  {
    role: 'reader',
    title: 'Rupi Lector',
    area: 'Comunicación',
    phrase: '¡Soy Rupi Lector! En Comunicación descubrimos cuentos mágicos, leyendas del Perú y el poder de las palabras.',
    badgeText: 'Letras y relatos',
  },
  {
    role: 'mathematician',
    title: 'Rupi Matemático',
    area: 'Matemática',
    phrase: '¡Soy Rupi Matemático! Resuelve acertijos numéricos, calcula rutas y conquista grandes desafíos.',
    badgeText: 'Desafíos numéricos',
  },
  {
    role: 'scientist',
    title: 'Rupi Científico',
    area: 'Ciencia y Tecnología',
    phrase: '¡Soy Rupi Científico! En Ciencia y Tecnología experimentamos, observamos la naturaleza y creamos inventos geniales.',
    badgeText: 'Naturaleza y ciencia',
  },
]

export function LoginView({ onLogin, onLoginDemo, sessionBusy, demoAvailable, serviceError }: Props) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [capsLockActive, setCapsLockActive] = useState(false)
  const [feedback, setFeedback] = useState<string | null>(null)
  const [feedbackType, setFeedbackType] = useState<'error' | 'info'>('error')
  const [rupiJumping, setRupiJumping] = useState(false)
  const [courseIndex, setCourseIndex] = useState(0)

  const currentCourse = RUPI_COURSES[courseIndex]

  const welcomeMessage =
    '¡Hola, explorador! Ingresa a tu aula virtual en Rupi para descubrir tus rutas de aprendizaje por el Perú.'

  // Las animaciones de Rupi se van intercalando cada 10 segundos
  useEffect(() => {
    const timer = setInterval(() => {
      setCourseIndex(prev => (prev + 1) % RUPI_COURSES.length)
    }, 10000)
    return () => clearInterval(timer)
  }, [])

  function handleKeyActivity(e: KeyboardEvent<HTMLInputElement>) {
    if (typeof e.getModifierState === 'function') {
      setCapsLockActive(e.getModifierState('CapsLock'))
    }
  }

  function handleSelectCourseCharacter(index: number) {
    setCourseIndex(index)
    speakText(RUPI_COURSES[index].phrase)
  }

  function handleRupiCheer() {
    setRupiJumping(true)
    speakText(currentCourse.phrase)
    setTimeout(() => setRupiJumping(false), 1200)
  }

  function handleListenCurrentSpeech() {
    setRupiJumping(true)
    speakText(currentCourse.phrase)
    setTimeout(() => setRupiJumping(false), 1200)
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFeedback(null)

    const trimmedUser = username.trim()
    const trimmedPass = password.trim()

    if (!trimmedUser || !trimmedPass) {
      setFeedback('Escribe tu usuario y tu contraseña para ingresar a clase.')
      setFeedbackType('error')
      return
    }

    try {
      await onLogin(trimmedUser, password)
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 429) {
          setFeedback('Muchos intentos seguidos. Espera unos minutitos con tu profe para volver a probar.')
        } else if (err.status === 401 || err.status === 400) {
          setFeedback('Revisa tu usuario o contraseña con ayuda de tu profe.')
        } else {
          setFeedback('No pudimos conectar con tu aula. Espera un momento y vuelve a intentar.')
        }
      } else {
        setFeedback('No pudimos conectar con tu aula. Espera un momento y vuelve a intentar.')
      }
      setFeedbackType('error')
    }
  }

  async function handleDirectDemoAccess() {
    setFeedback(null)
    try {
      await onLoginDemo()
    } catch {
      setFeedback('No pudimos conectar con el servidor local de prueba.')
      setFeedbackType('error')
    }
  }

  return (
    <div className="login-split-page" role="region" aria-label="Pantalla de inicio de sesión de Rupi">
      {/* 1. Panel de marca y narrativa visual con animaciones de Rupi */}
      <section className="login-brand-panel" aria-label="Bienvenida a Rupi">
        <div className="brand-panel-decorations" aria-hidden="true">
          {/* Siluetas de cerros andinos, sol brillante, nevado, nubes y camino sinuoso animado */}
          <svg className="andean-hills-svg" viewBox="0 0 600 360" preserveAspectRatio="none">
            {/* Sol Andino radiante en las cumbres */}
            <g className="andean-sun">
              <circle cx="85" cy="65" r="34" className="sun-core" />
              <circle cx="85" cy="65" r="44" className="sun-ray-ring" />
              <circle cx="85" cy="65" r="22" className="sun-inner-glow" />
            </g>

            {/* Nubes andinas flotando suavemente */}
            <g className="andean-clouds">
              <path
                className="andean-cloud cloud-1"
                d="M140 75 Q160 55 185 75 Q210 65 225 87 Q240 102 220 109 Q200 114 165 109 Q135 109 140 75 Z"
              />
              <path
                className="andean-cloud cloud-2"
                d="M390 55 Q405 39 425 55 Q445 47 455 65 Q465 77 450 83 Q435 87 408 83 Q386 83 390 55 Z"
              />
            </g>

            {/* Capas de montañas andinas con pico nevado */}
            <path
              className="hill-back"
              d="M0 240 Q140 130 290 190 T600 160 L600 360 L0 360 Z"
            />
            <path
              className="mountain-snowcap"
              d="M260 178 L290 155 L320 178 Q305 183 290 174 Q275 183 260 178 Z"
            />
            <path
              className="hill-mid"
              d="M0 260 Q180 180 340 230 T600 210 L600 360 L0 360 Z"
            />
            <path
              className="hill-front"
              d="M0 280 Q220 230 420 270 T600 250 L600 360 L0 360 Z"
            />

            {/* Flora andina silvestre en los cerros (retamas) */}
            <g className="andean-flora">
              <path className="flora-stem stem-1" d="M95 285 Q92 272 90 268" />
              <circle cx="90" cy="266" r="3.5" className="flora-bloom bloom-yellow" />
              <circle cx="94" cy="270" r="3" className="flora-bloom bloom-orange" />

              <path className="flora-stem stem-2" d="M490 270 Q495 258 497 252" />
              <circle cx="497" cy="250" r="4" className="flora-bloom bloom-yellow" />
              <circle cx="493" cy="255" r="3" className="flora-bloom bloom-orange" />
            </g>

            {/* Camino sinuoso animado con base y trazo dinámico */}
            <path
              className="trail-path-base"
              d="M50 320 C140 270 170 210 280 220 C380 230 430 180 530 190"
            />
            <path
              className="trail-path"
              d="M50 320 C140 270 170 210 280 220 C380 230 430 180 530 190"
            />

            {/* Hitos / paradas interactivas a lo largo de la ruta andina */}
            <g className="trail-stops-nodes">
              <circle cx="140" cy="270" r="7" className="map-stop-node stop-completed" />
              <circle cx="280" cy="220" r="9" className="map-stop-node stop-active" />
              <circle cx="280" cy="220" r="14" className="map-stop-pulse" />
              <circle cx="430" cy="180" r="7" className="map-stop-node stop-target" />
            </g>
          </svg>
        </div>

        <div className="brand-panel-content">
          <header className="brand-panel-header">
            <div className="brand-logo-badge">
              <img className="brand-logo-img" src="/rupi-logo.jpg" alt="" />
              <span className="brand-title">rupi<span className="brand-dot">.</span></span>
            </div>
            <span className="brand-region-tag">APRENDIZAJE ESCOLAR · PERÚ</span>
          </header>

          {/* Escenario central de Rupi con animación viva e insignias flotantes */}
          <div className="brand-character-stage">
            {/* Selector interactivo de cursos: Lector, Matemático, Científico (intercalado cada 10s) */}
            <div className="rupi-course-switcher" role="tablist" aria-label="Cursos y personajes de Rupi">
              {RUPI_COURSES.map((course, idx) => (
                <button
                  key={course.role}
                  type="button"
                  role="tab"
                  aria-selected={idx === courseIndex}
                  className={`course-role-chip ${idx === courseIndex ? `active role-${course.role}` : ''}`}
                  onClick={() => handleSelectCourseCharacter(idx)}
                  title={`Ver a ${course.title} (${course.area})`}
                >
                  {course.role === 'reader' && <BookOpen size={14} aria-hidden="true" />}
                  {course.role === 'mathematician' && <Calculator size={14} aria-hidden="true" />}
                  {course.role === 'scientist' && <FlaskConical size={14} aria-hidden="true" />}
                  <span>{course.area}</span>
                  {idx === courseIndex && <div className="chip-timer-bar" aria-hidden="true" />}
                </button>
              ))}
            </div>

            {/* Burbuja de diálogo cálida de Rupi con la frase del curso actual */}
            <div className="rupi-speech-balloon" role="region" aria-label={`Mensaje de ${currentCourse.title}`}>
              <div className="speech-balloon-header">
                <span className="speech-balloon-pill">{currentCourse.title} · {currentCourse.area}</span>
                <button
                  type="button"
                  className="speech-audio-btn"
                  onClick={handleListenCurrentSpeech}
                  aria-label={`Escuchar mensaje de ${currentCourse.title}`}
                  title="Escuchar a Rupi"
                >
                  <Volume2 size={16} aria-hidden="true" />
                  <span>Escuchar a Rupi</span>
                </button>
              </div>
              <p className="speech-balloon-text">
                {currentCourse.phrase}
              </p>
              <div className="speech-balloon-tail" aria-hidden="true" />
            </div>

            {/* Botón interactivo de Rupi en su pose actual con salto alegre y destellos */}
            <div className="brand-rupi-container">
              <div className="character-halo" aria-hidden="true" />
              <div className="character-sun-rays" aria-hidden="true" />

              {/* Chispitas mágicas decorativas */}
              <div className="rupi-sparkle sparkle-tl" aria-hidden="true">
                <Sparkles size={20} />
              </div>
              <div className="rupi-sparkle sparkle-tr" aria-hidden="true">
                <Sparkles size={16} />
              </div>
              <div className="rupi-sparkle sparkle-bl" aria-hidden="true">
                <Sparkles size={14} />
              </div>
              <div className="rupi-sparkle sparkle-br" aria-hidden="true">
                <Sparkles size={18} />
              </div>

              {/* Rupi animado en su rol correspondiente */}
              <button
                type="button"
                className={`brand-rupi-button ${rupiJumping ? 'jumping' : ''}`}
                onClick={handleRupiCheer}
                aria-label={`Toca a ${currentCourse.title} para que salte de alegría`}
                title="¡Tócame para saltar!"
              >
                <RupiCharacter
                  role={currentCourse.role}
                  mood={rupiJumping ? 'jumping' : 'cheering'}
                  className="brand-rupi-mascot"
                />
                <span className="rupi-tap-hint">
                  <Sparkles size={12} aria-hidden="true" />
                  ¡Toca a {currentCourse.title}!
                </span>
              </button>

              {/* Insignias de los tres cursos con microanimación 3D */}
              <div className="floating-badge badge-math" aria-hidden="true">
                <div className="badge-icon-box">
                  <Calculator size={15} />
                </div>
                <span>Matemática activa</span>
              </div>

              <div className="floating-badge badge-routes" aria-hidden="true">
                <div className="badge-icon-box">
                  <BookOpen size={15} />
                </div>
                <span>Comunicación lectora</span>
              </div>

              <div className="floating-badge badge-cheer" aria-hidden="true">
                <div className="badge-icon-box">
                  <FlaskConical size={15} />
                </div>
                <span>Ciencia y Tecnología</span>
              </div>
            </div>
          </div>

          <div className="brand-slogan-box">
            <h2 className="brand-slogan-title">
              Tu espacio para descubrir
            </h2>
            <p className="brand-slogan-text">
              Un mundo por aprender, a tu propio ritmo.
            </p>
          </div>
        </div>
      </section>

      {/* 2. Panel del formulario escolar (Derecha en escritorio, principal en móvil) */}
      <section className="login-form-panel" aria-labelledby="form-greeting-title">
        <div className="form-panel-inner">
          <header className="form-panel-header">
            <div className="greeting-row">
              <h1 id="form-greeting-title" className="greeting-heading">
                ¡Hola, explorador!
              </h1>
              <button
                type="button"
                className="greeting-audio-btn"
                onClick={() => speakText(welcomeMessage)}
                aria-label="Escuchar mensaje de bienvenida"
                title="Escuchar bienvenida"
              >
                <Volume2 size={20} aria-hidden="true" />
              </button>
            </div>
            <p className="greeting-instruction">
              Ingresa tus datos para entrar a tu aula de aprendizaje.
            </p>
          </header>

          <form className="student-login-form" onSubmit={handleSubmit} noValidate>
              {serviceError && <p role="alert" className="auth-feedback error">{serviceError}</p>}
            {/* Campo Usuario */}
            <div className="form-group">
              <label htmlFor="student-username" className="field-label">
                <User size={18} aria-hidden="true" />
                <span>Usuario escolar</span>
              </label>
              <div className="input-shell">
                <input
                  id="student-username"
                  name="username"
                  type="text"
                  className="field-input"
                  placeholder="Tu usuario"
                  autoComplete="username"
                  inputMode="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  disabled={sessionBusy}
                  required
                />
              </div>
            </div>

            {/* Campo Contraseña con toggle de visibilidad */}
            <div className="form-group">
              <label htmlFor="student-password" className="field-label">
                <KeyRound size={18} aria-hidden="true" />
                <span>Contraseña secreta</span>
              </label>
              <div className="input-shell with-toggle">
                <input
                  id="student-password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  className="field-input"
                  placeholder="Tu contraseña"
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  onKeyDown={handleKeyActivity}
                  onKeyUp={handleKeyActivity}
                  disabled={sessionBusy}
                  aria-describedby={capsLockActive ? 'caps-lock-warning' : undefined}
                  required
                />
                <button
                  type="button"
                  className="password-toggle-btn"
                  onClick={() => setShowPassword((prev) => !prev)}
                  aria-label={showPassword ? 'Ocultar contraseña' : 'Ver contraseña'}
                  title={showPassword ? 'Ocultar' : 'Ver'}
                >
                  {showPassword ? (
                    <EyeOff size={20} aria-hidden="true" />
                  ) : (
                    <Eye size={20} aria-hidden="true" />
                  )}
                </button>
              </div>

              {/* Advertencia de Bloq Mayús */}
              {capsLockActive && (
                <div id="caps-lock-warning" className="caps-warning" role="status">
                  <AlertCircle size={15} aria-hidden="true" />
                  <span>Tienes las mayúsculas activadas</span>
                </div>
              )}
            </div>

            {/* Mensajes de feedback accesibles */}
            {feedback && (
              <div
                className={`login-alert-banner ${feedbackType}`}
                role="alert"
                aria-live="polite"
              >
                <AlertCircle size={18} aria-hidden="true" />
                <p>{feedback}</p>
              </div>
            )}

            {/* Botón principal único con estado de carga */}
            <button
              type="submit"
              className="primary-login-btn"
              disabled={sessionBusy}
              aria-label="Entrar a mi aula"
            >
              <span>{sessionBusy ? 'Entrando a tu aula…' : 'Entrar a mi aula'}</span>
              <ArrowRight size={20} aria-hidden="true" />
            </button>

            {/* Ayuda escolar en línea */}
            <div className="school-help-line">
              <HelpCircle size={16} aria-hidden="true" />
              <span>¿Olvidaste tu contraseña? Pide ayuda a tu profe.</span>
            </div>
          </form>

          {/* Acceso técnico de prueba local: solo en desarrollo y sin datos personales */}
          {import.meta.env.DEV && demoAvailable && (
            <div className="dev-demo-wrapper">
              <button
                type="button"
                className="dev-demo-trigger-btn"
                onClick={handleDirectDemoAccess}
                disabled={sessionBusy}
                aria-label="Acceder con sesión de prueba local para desarrollo"
              >
                <Sparkles size={14} aria-hidden="true" />
                <span>Acceso de prueba local (Desarrollo)</span>
              </button>
            </div>
          )}

          <footer className="form-panel-footer">
            <Lock size={15} aria-hidden="true" />
            <span>Acceso seguro para colegios de primaria · Perú</span>
          </footer>
        </div>
      </section>
    </div>
  )
}
