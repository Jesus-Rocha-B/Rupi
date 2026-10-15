import { useState } from 'react'
import {
  ArrowLeft,
  ArrowRight,
  Award,
  CheckCircle2,
  Download,
  Feather,
  FileText,
  Paperclip,
  Presentation,
  Sparkles,
  Video,
  Volume2,
} from 'lucide-react'
import type {
  ActivityCompleteResult,
  ActivityContent,
  AnswerFeedbackResponse,
  QuestionDto,
  RouteDetail,
  RouteNode,
} from '../../services/learningRoutesApi'
import {
  completeActivity,
  submitQuestionAnswer,
} from '../../services/learningRoutesApi'
import { RupiCharacter } from '../../components/RupiCharacter'
import { speakText } from '../../utils/speech'
import './LessonScreenView.css'
import { NodeReinforcement } from '../reinforcement/NodeReinforcement'

function safeResource(value: string | null) {
  if (!value) return null
  try {
    const url = new URL(value, window.location.origin)
    return ['http:', 'https:'].includes(url.protocol) ? url.href : null
  } catch {
    return null
  }
}

function getYouTubeEmbedUrl(urlStr: string | null): string | null {
  if (!urlStr) return null
  try {
    const url = new URL(urlStr)
    if (url.hostname.includes('youtube.com')) {
      if (url.pathname.startsWith('/embed/')) {
        return `https://www.youtube-nocookie.com/embed/${url.pathname.replace('/embed/', '')}`
      }
      const v = url.searchParams.get('v')
      if (v) return `https://www.youtube-nocookie.com/embed/${v}`
    }
    if (url.hostname.includes('youtu.be')) {
      const id = url.pathname.replace('/', '')
      if (id) return `https://www.youtube-nocookie.com/embed/${id}`
    }
    return null
  } catch {
    return null
  }
}

interface LessonScreenViewProps {
  node: RouteNode
  route: RouteDetail
  activity: ActivityContent | null
  loading: boolean
  error: string | null
  onBackToRoadmap: () => void
  onComplete: (result: ActivityCompleteResult) => void
  onOpenBadges?: () => void
}

type LessonStep = 'video_preparacion' | 'preguntas_practica'

export function LessonScreenView({
  node,
  route,
  activity,
  loading,
  error,
  onBackToRoadmap,
  onComplete,
  onOpenBadges,
}: LessonScreenViewProps) {
  const [step, setStep] = useState<LessonStep>('video_preparacion')
  const [busy, setBusy] = useState(false)
  const [completionResult, setCompletionResult] = useState<ActivityCompleteResult | null>(null)
  const [selectedOptions, setSelectedOptions] = useState<Record<string, string>>({})
  const [feedbackMap, setFeedbackMap] = useState<Record<string, AnswerFeedbackResponse>>({})
  const [submittingQuestionId, setSubmittingQuestionId] = useState<string | null>(null)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  const questions: QuestionDto[] = activity?.questions || []
  const hasQuestions = questions.length > 0
  const answeredCount = Object.keys(feedbackMap).length

  // Extraer el primer video de YouTube si existe en los bloques
  const firstVideoBlock = activity?.blocks.find(b => getYouTubeEmbedUrl(b.url) !== null)
  const youtubeUrl = firstVideoBlock ? getYouTubeEmbedUrl(firstVideoBlock.url) : null

  // Manejar envío de respuesta a una pregunta
  async function handleAnswerSubmit(question: QuestionDto) {
    const selectedOptionId = selectedOptions[question.id]
    if (!selectedOptionId || !activity?.activityId || submittingQuestionId) return

    setSubmittingQuestionId(question.id)
    setErrorMsg(null)
    try {
      const feedback = await submitQuestionAnswer(activity.activityId, {
        questionId: question.id,
        selectedOptionId,
        textAnswer: null,
      })
      setFeedbackMap(prev => ({ ...prev, [question.id]: feedback }))
    } catch {
      setErrorMsg('No pudimos verificar tu respuesta. Revisa tu conexión e intenta de nuevo.')
    } finally {
      setSubmittingQuestionId(null)
    }
  }

  // Culminar la actividad y registrar avance
  async function handleFinish() {
    if (busy || !activity) return
    setBusy(true)
    setErrorMsg(null)
    try {
      const res = await completeActivity(route.versionRouteId, activity.nodeId)
      setCompletionResult(res)
      onComplete(res)
    } catch {
      setErrorMsg('No pudimos guardar tu avance. Revisa tu conexión y vuelve a intentar.')
    } finally {
      setBusy(false)
    }
  }

  const reinforcementPanel = <NodeReinforcement key={`${route.versionRouteId}:${node.id}`}
    routeId={route.versionRouteId} nodeId={node.id}
    revision={Object.entries(feedbackMap).map(([id, feedback]) => `${id}:${feedback.isCorrect}`).join(',')} />

  // 1. ESTADO DE CARGA
  if (loading) {
    return (
      <div className="lesson-screen-container" role="main">
        <div className="lesson-loading-card" role="status" aria-live="polite">
          <RupiCharacter mood="thinking" className="loading-rupi-svg" />
          <h2>Preparando tu lección...</h2>
          <p>Rupi está organizando tus materiales y videos de clase. ¡Ya casi estamos listos!</p>
        </div>
      </div>
    )
  }

  // 2. ESTADO DE ERROR DE CARGA
  if (error || !activity) {
    return (
      <div className="lesson-screen-container" role="main">
        <div className="lesson-error-card" role="alert">
          <RupiCharacter mood="thinking" className="loading-rupi-svg" />
          <h2>No pudimos abrir esta parada</h2>
          <p>{error || 'Esta parada aún no tiene actividades cargadas o se encuentra temporalmente inaccesible.'}</p>
          <button type="button" className="btn-back-to-map" onClick={onBackToRoadmap}>
            <ArrowLeft size={18} aria-hidden="true" />
            <span>Volver a mi mapa de ruta</span>
          </button>
        </div>
      </div>
    )
  }

  // 3. PANTALLA DE CELEBRACIÓN / PARADA COMPLETADA
  if (completionResult) {
    return (
      <div className="lesson-screen-container celebration-mode" role="main">
        <div className="lesson-celebration-hero" role="region" aria-label="Celebración de parada culminada">
          <div className="celebration-mascot" aria-hidden="true">
            <RupiCharacter mood="cheering" className="celebration-rupi-svg" />
          </div>

          <div className="celebration-xp-pill">
            <Sparkles size={20} aria-hidden="true" />
            <span>+{completionResult.experienceEarned} XP Ganados</span>
          </div>

          {completionResult.routeBonusExperience ? (
            <div className="celebration-xp-pill bonus">
              <Sparkles size={18} aria-hidden="true" />
              <span>+{completionResult.routeBonusExperience} XP Bono de Graduación</span>
            </div>
          ) : null}

          <h1 className="celebration-title">
            {completionResult.isRouteCompleted ? '¡Ruta Escolar Culminada!' : '¡Misión Cumplida!'}
          </h1>
          <p className="celebration-message">{completionResult.rupiMessage}</p>

          {/* Insignia ganada */}
          {completionResult.badgeEarned && (
            <div className="celebration-badge-card" role="region" aria-label="Insignia ganada">
              <div className="badge-icon-disc">
                {completionResult.badgeEarned.icon === 'feather' ? (
                  <Feather size={36} className="badge-gold-icon" aria-hidden="true" />
                ) : (
                  <Award size={36} className="badge-gold-icon" aria-hidden="true" />
                )}
              </div>
              <div className="badge-text-group">
                <span className="badge-tag">
                  {completionResult.isRouteCompleted ? '¡INSIGNIA DE GRADUACIÓN!' : '¡NUEVA INSIGNIA ESCOLAR!'}
                </span>
                <h3 className="badge-name">{completionResult.badgeEarned.name}</h3>
                <p className="badge-desc">{completionResult.badgeEarned.description}</p>
              </div>
            </div>
          )}

          {reinforcementPanel}

          {/* Siguiente parada desbloqueada */}
          {completionResult.nextUnlockedNode && (
            <div className="next-unlocked-card">
              <span className="next-unlocked-tag">SIGUIENTE NIVEL DESBLOQUEADO</span>
              <strong className="next-unlocked-name">
                Parada #{completionResult.nextUnlockedNode.sequence}: {completionResult.nextUnlockedNode.title}
              </strong>
            </div>
          )}

          <div className="celebration-actions">
            <button
              type="button"
              className="celebration-continue-btn"
              onClick={onBackToRoadmap}
              autoFocus
            >
              <span>Continuar en mi camino</span>
              <ArrowRight size={20} aria-hidden="true" />
            </button>

            {completionResult.isRouteCompleted && onOpenBadges && (
              <button
                type="button"
                className="celebration-diploma-btn"
                onClick={() => {
                  onBackToRoadmap()
                  onOpenBadges()
                }}
              >
                <Award size={18} aria-hidden="true" />
                <span>Ver mis insignias y diploma</span>
              </button>
            )}

            <button
              type="button"
              className="celebration-speech-btn"
              onClick={() =>
                speakText(
                  completionResult.isRouteCompleted
                    ? `¡Misión Cumplida! Has completado todas las paradas de la ruta. Obtuviste la insignia de graduación ${completionResult.badgeEarned?.name || 'Gran Gallito de Oro'}, cien puntos de experiencia de bono y tu diploma escolar está listo.`
                    : completionResult.badgeEarned
                    ? `¡Felicitaciones! Has obtenido la insignia ${completionResult.badgeEarned.name}. ${completionResult.rupiMessage}`
                    : completionResult.rupiMessage
                )
              }
              aria-label="Escuchar felicitación de Rupi"
            >
              <Volume2 size={18} aria-hidden="true" />
              <span>Escuchar a Rupi</span>
            </button>
          </div>
        </div>
      </div>
    )
  }

  // 4. PANTALLA PRINCIPAL DE LA LECCIÓN (2 ETAPAS: VIDEO Y PREPARACIÓN -> PREGUNTAS Y PRÁCTICA)
  return (
    <div className="lesson-screen-container" role="main" id="pantalla-leccion">
      {/* Barra superior de navegación para volver al mapa */}
      <nav className="lesson-top-nav" aria-label="Navegación de lección">
        <button
          type="button"
          className="btn-back-to-map"
          onClick={onBackToRoadmap}
          title="Regresar a mi camino"
        >
          <ArrowLeft size={18} aria-hidden="true" />
          <span>Volver a mi camino</span>
        </button>

        <div className="lesson-nav-meta">
          <span className="lesson-nav-grade">
            {route.grade.number}.° Primaria · {route.area.name}
          </span>
          <span className="lesson-nav-stop-pill">
            Parada #{node.sequence}
          </span>
        </div>
      </nav>

      {/* Cabecera Principal de la Lección */}
      <header className="lesson-screen-header">
        <div className="lesson-header-left">
          <div className="lesson-eyebrow">
            <Award size={16} aria-hidden="true" />
            <span>NIVEL #{node.sequence} ESCOLAR</span>
          </div>
          <h1 className="lesson-screen-title">{activity.title}</h1>
          {activity.instructions && (
            <p className="lesson-screen-desc">{activity.instructions}</p>
          )}
        </div>

        <div className="lesson-header-actions">
          <button
            type="button"
            className="btn-listen-lesson"
            onClick={() => speakText(`${activity.title}. ${activity.instructions || ''}`)}
            title="Escuchar título e instrucciones"
          >
            <Volume2 size={18} aria-hidden="true" />
            <span>Escuchar lección</span>
          </button>
        </div>
      </header>

      {/* Indicador de pasos pedagógicos (Video/Explicación vs Práctica/Preguntas) */}
      <div className="lesson-stepper-bar" role="tablist" aria-label="Etapas de la lección">
        <button
          type="button"
          role="tab"
          aria-selected={step === 'video_preparacion'}
          className={`stepper-tab ${step === 'video_preparacion' ? 'active' : 'completed'}`}
          onClick={() => setStep('video_preparacion')}
        >
          <span className="step-num">1</span>
          <div className="step-text-col">
            <strong className="step-title">Video y Explicación</strong>
            <small className="step-sub">Aprende antes de responder</small>
          </div>
        </button>

        <button
          type="button"
          role="tab"
          aria-selected={step === 'preguntas_practica'}
          className={`stepper-tab ${step === 'preguntas_practica' ? 'active' : ''}`}
          onClick={() => setStep('preguntas_practica')}
        >
          <span className="step-num">2</span>
          <div className="step-text-col">
            <strong className="step-title">Reto y Preguntas</strong>
            <small className="step-sub">
              {hasQuestions ? `${answeredCount} de ${questions.length} respondidas` : 'Práctica didáctica'}
            </small>
          </div>
        </button>
      </div>

      {/* ============================================================== */}
      {/* ETAPA 1: VIDEO Y EXPLICACIÓN DIDÁCTICA PREVIA                   */}
      {/* ============================================================== */}
      {step === 'video_preparacion' && (
        <section className="lesson-step-content video-stage" aria-label="Etapa de video y preparación">
          {/* Tarjeta de Video Destacado */}
          {youtubeUrl ? (
            <div className="lesson-video-card">
              <div className="video-card-header">
                <Video size={20} className="video-header-icon" aria-hidden="true" />
                <div className="video-header-text">
                  <h3>Video didáctico explicativo</h3>
                  <p>Observa el video con atención antes de comenzar tu reto.</p>
                </div>
              </div>
              <div className="video-embed-wrapper">
                <iframe
                  src={youtubeUrl}
                  title={activity.title || 'Video didáctico de la lección'}
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                  allowFullScreen
                  className="lesson-iframe-video"
                />
              </div>
            </div>
          ) : (
            <div className="lesson-illustration-card">
              <div className="illustration-character-col">
                <RupiCharacter mood="cheering" className="lesson-guide-rupi" />
              </div>
              <div className="illustration-text-col">
                <span className="card-badge-tag">GUÍA DE RUPI</span>
                <h3>¡Aprende este tema con Rupi!</h3>
                <p>
                  Lee las ideas principales que tienes a continuación. Cada reto te ayudará a comprender mejor el mundo y avanzar por el mapa del Perú.
                </p>
                <button
                  type="button"
                  className="btn-listen-speech-subtle"
                  onClick={() =>
                    speakText(
                      `¡Hola explorador! En esta lección aprenderemos sobre ${activity.title}. Lee con atención los textos y cuando estés listo, presiona el botón para responder las preguntas.`
                    )
                  }
                >
                  <Volume2 size={16} aria-hidden="true" />
                  <span>Escuchar a Rupi</span>
                </button>
              </div>
            </div>
          )}

          {/* Bloques de Explicación y Materiales del Profesor */}
          <div className="lesson-explanation-blocks">
            {activity.blocks.map(block => {
              // Si ya se mostró como video principal, no duplicar el iframe
              if (block === firstVideoBlock) return null

              const url = safeResource(block.url)
              const isPdf = block.url?.toLowerCase().includes('.pdf')
              const isPpt = block.url?.toLowerCase().includes('.ppt') || block.url?.toLowerCase().includes('.pptx')
              const isImage = block.url && /\.(png|jpe?g|svg|webp|gif)($|\?)/i.test(block.url)
              const isAttachment = block.type === 'ADJUNTO' || isPdf || isPpt || (!isImage && Boolean(url) && block.type !== 'DESTACADO')

              if (isAttachment) {
                return (
                  <div key={block.id} className="attachment-material-card">
                    <div className="attachment-left">
                      <div className="attachment-disc-icon">
                        {isPdf ? <FileText size={24} /> : isPpt ? <Presentation size={24} /> : <Paperclip size={24} />}
                      </div>
                      <div className="attachment-meta">
                        <span className="attachment-pill">MATERIAL DEL PROFESOR</span>
                        <h4>{block.text || (isPdf ? 'Documento PDF' : 'Presentación de Clase')}</h4>
                        {block.accessibleText && <p>{block.accessibleText}</p>}
                      </div>
                    </div>
                    {url && (
                      <a href={url} target="_blank" rel="noopener noreferrer" className="btn-open-attachment">
                        <Download size={16} aria-hidden="true" />
                        <span>Abrir recurso</span>
                      </a>
                    )}
                  </div>
                )
              }

              return (
                <article key={block.id} className="explanation-text-card">
                  {block.text && <p className="explanation-p">{block.text}</p>}
                  {url && (
                    <div className="explanation-media-wrap">
                      <img src={url} alt={block.accessibleText || 'Imagen explicativa'} className="explanation-img" />
                    </div>
                  )}
                </article>
              )
            })}
          </div>

          {/* Banner de Salto a la Práctica / Preguntas */}
          <div className="lesson-action-cta-card">
            <div className="cta-mascot-group">
              <RupiCharacter mood="cheering" role="mathematician" className="cta-rupi" />
              <div className="cta-text-group">
                <h4>¿Ya revisaste el video y la explicación?</h4>
                <p>¡Es momento de poner a prueba lo aprendido y ganar tus puntos de experiencia!</p>
              </div>
            </div>

            <button
              type="button"
              className="btn-start-practice"
              onClick={() => setStep('preguntas_practica')}
              autoFocus
            >
              <span>¡Listo, a practicar! (Comenzar preguntas)</span>
              <ArrowRight size={22} aria-hidden="true" />
            </button>
          </div>
        </section>
      )}

      {/* ============================================================== */}
      {/* ETAPA 2: RETO Y PREGUNTAS INTERACTIVAS                         */}
      {/* ============================================================== */}
      {step === 'preguntas_practica' && (
        <section className="lesson-step-content questions-stage" aria-label="Etapa de preguntas interactivas">
          {/* Barra de ayuda con botón para repasar el video */}
          <div className="practice-helper-bar">
            <button
              type="button"
              className="btn-review-video"
              onClick={() => setStep('video_preparacion')}
            >
              <Video size={16} aria-hidden="true" />
              <span>← Repasar el video o explicación</span>
            </button>

            <span className="practice-progress-pill">
              {hasQuestions ? `${answeredCount} de ${questions.length} respondidas` : 'Práctica final'}
            </span>
          </div>

          {errorMsg && (
            <div className="practice-error-banner" role="alert">
              <p>{errorMsg}</p>
            </div>
          )}

          {/* Preguntas didácticas */}
          {hasQuestions ? (
            <div className="practice-questions-list">
              {questions.map((question, qIdx) => {
                const selectedOptionId = selectedOptions[question.id]
                const feedback = feedbackMap[question.id]
                const isSubmitting = submittingQuestionId === question.id

                return (
                  <article key={question.id} className="practice-question-card" aria-label={`Pregunta ${qIdx + 1}`}>
                    <div className="question-top-row">
                      <span className="question-seq-pill">Pregunta #{question.sequence || qIdx + 1}</span>
                      {question.points && (
                        <span className="question-points-pill">+{question.points} puntos</span>
                      )}
                      <button
                        type="button"
                        className="btn-listen-question"
                        onClick={() => speakText(question.statement)}
                        title="Escuchar pregunta"
                      >
                        <Volume2 size={16} aria-hidden="true" />
                        <span>Escuchar</span>
                      </button>
                    </div>

                    <h3 className="question-statement-text">{question.statement}</h3>

                    <div className="practice-options-grid" role="radiogroup" aria-label={`Opciones de pregunta ${qIdx + 1}`}>
                      {question.options.map(option => {
                        const isSelected = selectedOptionId === option.id
                        const isCorrectOpt = feedback?.correctOptionId === option.id
                        const isChosenAndWrong = feedback && !feedback.isCorrect && isSelected

                        let optClass = 'practice-option-btn'
                        if (isSelected) optClass += ' selected'
                        if (feedback) {
                          if (isCorrectOpt) optClass += ' correct'
                          else if (isChosenAndWrong) optClass += ' wrong'
                          else optClass += ' muted'
                        }

                        return (
                          <button
                            key={option.id}
                            type="button"
                            role="radio"
                            aria-checked={isSelected}
                            disabled={Boolean(feedback) || isSubmitting}
                            className={optClass}
                            onClick={() => {
                              if (!feedback) {
                                setSelectedOptions(prev => ({ ...prev, [question.id]: option.id }))
                              }
                            }}
                          >
                            <span className="option-letter" aria-hidden="true">
                              {String.fromCharCode(64 + (option.sequence || 1))}
                            </span>
                            <span className="option-label">{option.text}</span>
                            {feedback && isCorrectOpt && (
                              <CheckCircle2 size={20} className="option-check-icon" aria-hidden="true" />
                            )}
                          </button>
                        )
                      })}
                    </div>

                    {!feedback ? (
                      <div className="question-submit-row">
                        <button
                          type="button"
                          className="btn-check-answer"
                          disabled={!selectedOptionId || isSubmitting}
                          onClick={() => handleAnswerSubmit(question)}
                        >
                          <span>{isSubmitting ? 'Verificando con Rupi…' : 'Comprobar mi respuesta'}</span>
                          <ArrowRight size={18} aria-hidden="true" />
                        </button>
                      </div>
                    ) : (
                      <div className={`feedback-result-card ${feedback.isCorrect ? 'correct' : 'retry'}`}>
                        <div className="feedback-avatar">
                          <RupiCharacter mood={feedback.isCorrect ? 'cheering' : 'thinking'} className="rupi-feedback-svg" />
                        </div>
                        <div className="feedback-text-col">
                          <strong>{feedback.isCorrect ? '¡Excelente trabajo!' : '¡Buen intento! Sigamos aprendiendo'}</strong>
                          <p>{feedback.feedbackMessage || feedback.explanation || (feedback.isCorrect ? '¡Has acertado la respuesta correcta!' : 'Revisa la explicación e inténtalo en la siguiente parada.')}</p>
                        </div>
                      </div>
                    )}
                  </article>
                )
              })}
            </div>
          ) : (
            <div className="no-questions-card">
              <RupiCharacter mood="cheering" className="no-q-rupi" />
              <h3>¡Excelente lectura y exploración!</h3>
              <p>Esta parada de descubrimiento no tiene preguntas de opción múltiple. Guarda tu avance para sumar experiencia y avanzar en el mapa.</p>
            </div>
          )}

          {/* Botón final para culminar la parada escolar */}
          <div className="lesson-finish-bar">
            <button
              type="button"
              className="btn-finish-lesson"
              disabled={busy || (hasQuestions && answeredCount < questions.length)}
              onClick={handleFinish}
            >
              <span>{busy ? 'Guardando tu avance escolar…' : '¡Completar parada y ganar XP!'}</span>
              <Sparkles size={20} aria-hidden="true" />
            </button>
          </div>
        </section>
      )}
      {reinforcementPanel}
    </div>
  )
}
