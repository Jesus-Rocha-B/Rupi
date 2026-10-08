import { useState } from 'react'
import {
  AlertCircle,
  ArrowRight,
  Award,
  CheckCircle2,
  HelpCircle,
  Sparkles,
  Volume2,
  X,
} from 'lucide-react'
import {
  completeActivity,
  fetchEvaluationSummary,
  submitQuestionAnswer,
  type ActivityCompleteResult,
  type ActivityContent,
  type AnswerFeedbackResponse,
  type EvaluationSummaryDto,
  type QuestionDto,
} from '../../services/learningRoutesApi'
import { RupiCharacter } from '../../components/RupiCharacter'
import { speakText } from '../../utils/speech'

function safeResource(value: string | null) {
  if (!value) return null
  try {
    const url = new URL(value, window.location.origin)
    return ['http:', 'https:'].includes(url.protocol) ? url.href : null
  } catch {
    return null
  }
}

type Props = {
  activity: ActivityContent
  routeId: string
  onComplete?: (result: ActivityCompleteResult) => void
  onClose: () => void
}

export function ActivityBlocks({ activity, routeId, onComplete, onClose }: Props) {
  const [busy, setBusy] = useState(false)
  const [completionResult, setCompletionResult] = useState<ActivityCompleteResult | null>(null)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const [selectedOptions, setSelectedOptions] = useState<Record<string, string>>({})
  const [feedbackMap, setFeedbackMap] = useState<Record<string, AnswerFeedbackResponse>>({})
  const [submittingQuestionId, setSubmittingQuestionId] = useState<string | null>(null)
  const [summary, setSummary] = useState<EvaluationSummaryDto | null>(null)

  const isCompleted = activity.state === 'COMPLETADO' || completionResult !== null
  const questions: QuestionDto[] = activity.questions || []
  const hasQuestions = questions.length > 0
  const answeredCount = Object.keys(feedbackMap).length

  async function handleAnswerSubmit(question: QuestionDto) {
    const selectedOptionId = selectedOptions[question.id]
    if (!selectedOptionId || !activity.activityId || submittingQuestionId) return

    setSubmittingQuestionId(question.id)
    setErrorMsg(null)
    try {
      const feedback = await submitQuestionAnswer(activity.activityId, {
        questionId: question.id,
        selectedOptionId,
        textAnswer: null,
      })
      setFeedbackMap(prev => ({ ...prev, [question.id]: feedback }))
      const speechMessage = `${feedback.feedbackMessage}. ${feedback.explanation || ''}`
      speakText(speechMessage)

      // Si se respondieron todas las preguntas, cargar resumen formativo
      if (answeredCount + 1 >= questions.length) {
        try {
          const sum = await fetchEvaluationSummary(activity.activityId)
          setSummary(sum)
        } catch {
          // Si falla la red para el resumen, no bloquea el flujo
        }
      }
    } catch {
      setErrorMsg('No pudimos verificar tu respuesta. Comprueba tu conexión e intenta de nuevo.')
    } finally {
      setSubmittingQuestionId(null)
    }
  }

  async function handleFinish() {
    if (busy) return
    setBusy(true)
    setErrorMsg(null)
    try {
      const res = await completeActivity(routeId, activity.nodeId)
      setCompletionResult(res)
      const speech = res.badgeEarned
        ? `¡Felicitaciones! Has obtenido la insignia ${res.badgeEarned.name}. ${res.rupiMessage}`
        : res.rupiMessage
      speakText(speech)
      if (onComplete) {
        onComplete(res)
      }
    } catch {
      setErrorMsg('No pudimos guardar tu avance. Revisa tu conexión y vuelve a intentar.')
    } finally {
      setBusy(false)
    }
  }

  // Vista de celebración cuando se completa la actividad con insignia escolar (HU-07)
  if (completionResult) {
    return (
      <div className="activity-celebration-card" role="region" aria-label="Celebración de parada completada">
        <button
          className="modal-close"
          type="button"
          onClick={onClose}
          aria-label="Volver al mapa"
        >
          <X aria-hidden="true" />
        </button>

        <div className="celebration-mascot" aria-hidden="true">
          <RupiCharacter mood="cheering" className="celebration-rupi-svg" />
        </div>

        <div className="celebration-xp-pill">
          <Sparkles size={18} aria-hidden="true" />
          <span>+{completionResult.experienceEarned} XP</span>
        </div>

        <h3 className="celebration-title">¡Misión Cumplida!</h3>
        <p className="celebration-message">{completionResult.rupiMessage}</p>

        {/* Insignia ganada (HU-07) */}
        {completionResult.badgeEarned && (
          <div className="celebration-badge-card" role="region" aria-label="Insignia ganada">
            <div className="badge-icon-disc">
              <Award size={34} className="badge-gold-icon" aria-hidden="true" />
            </div>
            <div className="badge-text-group">
              <span className="badge-tag">¡NUEVA INSIGNIA ESCOLAR!</span>
              <h4 className="badge-name">{completionResult.badgeEarned.name}</h4>
              <p className="badge-desc">{completionResult.badgeEarned.description}</p>
            </div>
          </div>
        )}

        {completionResult.nextUnlockedNode && (
          <div className="next-unlocked-card">
            <span className="next-unlocked-tag">SIGUIENTE PARADA DESBLOQUEADA</span>
            <b className="next-unlocked-name">
              Parada #{completionResult.nextUnlockedNode.sequence}: {completionResult.nextUnlockedNode.title}
            </b>
          </div>
        )}

        <div className="celebration-actions">
          <button
            type="button"
            className="celebration-continue-btn"
            onClick={onClose}
            autoFocus
          >
            <span>Continuar mi aventura</span>
            <ArrowRight size={18} aria-hidden="true" />
          </button>

          <button
            type="button"
            className="celebration-speech-btn"
            onClick={() =>
              speakText(
                completionResult.badgeEarned
                  ? `¡Felicitaciones! Has obtenido la insignia ${completionResult.badgeEarned.name}. ${completionResult.rupiMessage}`
                  : completionResult.rupiMessage
              )
            }
            aria-label="Escuchar felicitación"
            title="Escuchar felicitación"
          >
            <Volume2 size={18} aria-hidden="true" />
            <span>Escuchar</span>
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="activity-modal-wrapper">
      <header className="activity-modal-header">
        <div className="activity-modal-title-col">
          <span className="activity-badge-pill">
            <Award size={14} aria-hidden="true" />
            <span>ACTIVIDAD ESCOLAR</span>
          </span>
          <h2 id="modal-title">{activity.title}</h2>
        </div>
        <div className="activity-header-actions">
          <button
            type="button"
            className="modal-listen-btn"
            onClick={() => speakText(`${activity.title}. ${activity.instructions || ''}`)}
            aria-label="Escuchar actividad"
            title="Escuchar instrucciones"
          >
            <Volume2 size={18} aria-hidden="true" />
          </button>
          <button
            className="modal-close"
            type="button"
            onClick={onClose}
            aria-label="Cerrar lección"
          >
            <X aria-hidden="true" />
          </button>
        </div>
      </header>

      {activity.instructions && (
        <div className="activity-instructions-box">
          <p>{activity.instructions}</p>
        </div>
      )}

      {/* Bloques de contenido didáctico */}
      <div className="activity-blocks-list" aria-label="Contenido didáctico">
        {activity.blocks.length > 0 ? (
          activity.blocks.map(block => {
            const url = safeResource(block.url)
            return (
              <div
                key={block.id}
                className={`activity-block-item ${block.type === 'DESTACADO' ? 'highlight-box' : 'standard-box'}`}
              >
                {block.text && <p className="block-text">{block.text}</p>}
                {url && (
                  <div className="block-media-container">
                    <img
                      src={url}
                      alt={block.accessibleText || 'Material didáctico'}
                      className="block-media-img"
                      loading="lazy"
                    />
                  </div>
                )}
              </div>
            )
          })
        ) : (
          <div className="activity-block-item highlight-box">
            <p className="block-text">
              ¡Hola, explorador! En esta parada escolar descubriremos conceptos clave de tu aventura. Lee con atención cada paso, practica con tu cuaderno y completa la misión para sumar tu experiencia con Rupi.
            </p>
          </div>
        )}
      </div>

      {/* Sección interactiva de preguntas y evaluación formativa (HU-06) */}
      {hasQuestions && (
        <section className="activity-questions-section" aria-label="Preguntas interactivas de la lección">
          <div className="questions-section-header">
            <div className="questions-header-title">
              <HelpCircle size={20} className="questions-title-icon" aria-hidden="true" />
              <h3>Desafíos y Preguntas con Rupi</h3>
            </div>
            <span className="questions-progress-pill">
              {answeredCount} de {questions.length} respondidas
            </span>
          </div>

          <div className="questions-cards-stack">
            {questions.map((question, qIdx) => {
              const selectedOptionId = selectedOptions[question.id]
              const feedback = feedbackMap[question.id]
              const isSubmitting = submittingQuestionId === question.id

              return (
                <article key={question.id} className="question-card" aria-label={`Pregunta ${qIdx + 1}`}>
                  <div className="question-header">
                    <span className="question-number-pill">Pregunta #{question.sequence || qIdx + 1}</span>
                    {question.points && (
                      <span className="question-points-pill">{question.points} puntos</span>
                    )}
                    <button
                      type="button"
                      className="question-listen-btn"
                      onClick={() => speakText(question.statement)}
                      aria-label={`Escuchar pregunta ${qIdx + 1}`}
                      title="Escuchar pregunta"
                    >
                      <Volume2 size={16} aria-hidden="true" />
                    </button>
                  </div>

                  <p className="question-statement">{question.statement}</p>

                  <div
                    className="question-options-list"
                    role="radiogroup"
                    aria-label={`Opciones de la pregunta ${qIdx + 1}`}
                  >
                    {question.options.map(option => {
                      const isSelected = selectedOptionId === option.id
                      const isCorrectOpt = feedback?.correctOptionId === option.id
                      const isChosenAndWrong = feedback && !feedback.isCorrect && isSelected

                      let optionClass = 'question-option-btn'
                      if (isSelected) optionClass += ' selected'
                      if (feedback) {
                        if (isCorrectOpt) optionClass += ' correct'
                        else if (isChosenAndWrong) optionClass += ' wrong'
                        else optionClass += ' disabled'
                      }

                      return (
                        <button
                          key={option.id}
                          type="button"
                          role="radio"
                          aria-checked={isSelected}
                          disabled={Boolean(feedback) || isSubmitting}
                          className={optionClass}
                          onClick={() => {
                            if (!feedback) {
                              setSelectedOptions(prev => ({ ...prev, [question.id]: option.id }))
                            }
                          }}
                        >
                          <span className="option-indicator" aria-hidden="true">
                            {String.fromCharCode(64 + (option.sequence || 1))}
                          </span>
                          <span className="option-text">{option.text}</span>
                          {feedback && isCorrectOpt && (
                            <CheckCircle2 size={18} className="option-feedback-icon correct" aria-hidden="true" />
                          )}
                          {feedback && isChosenAndWrong && (
                            <AlertCircle size={18} className="option-feedback-icon wrong" aria-hidden="true" />
                          )}
                        </button>
                      )
                    })}
                  </div>

                  {!feedback ? (
                    <div className="question-action-bar">
                      <button
                        type="button"
                        className="question-check-btn"
                        disabled={!selectedOptionId || isSubmitting}
                        onClick={() => handleAnswerSubmit(question)}
                      >
                        <span>{isSubmitting ? 'Verificando con Rupi…' : 'Comprobar mi respuesta'}</span>
                        <ArrowRight size={16} aria-hidden="true" />
                      </button>
                    </div>
                  ) : (
                    <div
                      className={`question-feedback-box ${feedback.isCorrect ? 'correct' : 'retry'}`}
                      role="region"
                      aria-label="Retroalimentación formativa"
                    >
                      <div className="feedback-mascot-col" aria-hidden="true">
                        <RupiCharacter
                          mood={feedback.isCorrect ? 'cheering' : 'thinking'}
                          className="feedback-rupi-svg"
                        />
                      </div>
                      <div className="feedback-content-col">
                        <div className="feedback-heading-row">
                          <strong className="feedback-status-title">
                            {feedback.isCorrect ? '¡Excelente deducción!' : '¡Buen intento! Sigamos aprendiendo'}
                          </strong>
                          <button
                            type="button"
                            className="feedback-listen-btn"
                            onClick={() =>
                              speakText(`${feedback.feedbackMessage}. ${feedback.explanation || ''}`)
                            }
                            aria-label="Escuchar retroalimentación"
                            title="Escuchar retroalimentación"
                          >
                            <Volume2 size={16} aria-hidden="true" />
                          </button>
                        </div>
                        <p className="feedback-message-text">{feedback.feedbackMessage}</p>
                        {feedback.explanation && (
                          <p className="feedback-explanation-text">{feedback.explanation}</p>
                        )}
                      </div>
                    </div>
                  )}
                </article>
              )
            })}
          </div>

          {/* Resumen formativo de la evaluación (HU-06 y HU-07) */}
          {summary && (
            <div className="evaluation-summary-card" role="region" aria-label="Resumen de evaluación formativa">
              <div className="summary-top-row">
                <div className="summary-score-group">
                  <span className="summary-score-label">Respuestas correctas:</span>
                  <b className="summary-score-val">
                    {summary.correctAnswers} / {summary.totalQuestions}
                  </b>
                </div>
                <button
                  type="button"
                  className="summary-listen-btn"
                  onClick={() => speakText(summary.rupiFeedback)}
                  aria-label="Escuchar mensaje formativo de Rupi"
                  title="Escuchar mensaje formativo"
                >
                  <Volume2 size={16} aria-hidden="true" />
                  <span>Escuchar a Rupi</span>
                </button>
              </div>
              <p className="summary-rupi-text">{summary.rupiFeedback}</p>
            </div>
          )}
        </section>
      )}

      {errorMsg && (
        <div className="activity-error-alert" role="alert">
          <p>{errorMsg}</p>
        </div>
      )}

      <footer className="activity-modal-footer">
        {isCompleted ? (
          <button
            type="button"
            className="activity-finish-btn already-done"
            onClick={onClose}
          >
            <CheckCircle2 size={18} aria-hidden="true" />
            <span>Parada completada · Volver al mapa</span>
          </button>
        ) : (
          <button
            type="button"
            className="activity-finish-btn"
            disabled={busy}
            onClick={handleFinish}
          >
            <Sparkles size={18} aria-hidden="true" />
            <span>{busy ? 'Guardando tu avance…' : '¡Terminé esta lección! (+50 XP)'}</span>
            <ArrowRight size={18} aria-hidden="true" />
          </button>
        )}
      </footer>
    </div>
  )
}
