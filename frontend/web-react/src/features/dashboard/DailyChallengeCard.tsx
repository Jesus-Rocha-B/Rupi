import React, { useEffect, useState } from 'react'
import { BookOpen, Clock, Puzzle, Star, Trophy } from 'lucide-react'
import {
  completeDailyChallenge,
  fetchDailyChallenge,
  type DailyChallenge,
  type DailyChallengeResult,
} from '../../services/dailyChallengeApi'
import { submitQuestionAnswer, type AnswerFeedbackResponse, type QuestionDto } from '../../services/learningRoutesApi'
import { ListenButton } from '../../components/ListenButton'
import { questionSpeech } from '../../utils/speech'
import { SKILL_LABEL, canFinish, challengeSpeech } from './dailyChallenge'
import './DailyChallengeCard.css'

interface Props {
  refreshTrigger?: number
  onCompleted?: () => void
}

function ChallengeQuestion({
  activityId,
  question,
  feedback,
  onAnswered,
}: {
  activityId: string
  question: QuestionDto
  feedback: AnswerFeedbackResponse | undefined
  onAnswered: (feedback: AnswerFeedbackResponse) => void
}) {
  const [selected, setSelected] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')

  const check = async () => {
    setSending(true)
    setError('')
    try {
      onAnswered(await submitQuestionAnswer(activityId, { questionId: question.id, selectedOptionId: selected }))
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setSending(false)
    }
  }

  const correctText = question.options.find(o => o.id === feedback?.correctOptionId)?.text

  return (
    <article className="challenge-question">
      <div className="challenge-question-head">
        <h4>{question.statement}</h4>
        <ListenButton compact subject="la pregunta y sus alternativas" text={questionSpeech(question.statement, question.options.map(o => o.text))} />
      </div>
      <fieldset disabled={sending || !!feedback}>
        <legend className="sr-only">Alternativas</legend>
        {question.options.map(option => (
          <label key={option.id} className={`challenge-option ${feedback?.correctOptionId === option.id ? 'is-correct' : ''}`}>
            <input
              type="radio"
              name={`reto-${question.id}`}
              value={option.id}
              checked={selected === option.id}
              onChange={() => setSelected(option.id)}
            />
            <span>{option.text}</span>
          </label>
        ))}
      </fieldset>
      {!feedback && (
        <button type="button" className="challenge-btn" disabled={!selected || sending} onClick={() => void check()}>
          {sending ? 'Revisando…' : 'Comprobar mi respuesta'}
        </button>
      )}
      {error && <p role="alert" className="challenge-error">{error}</p>}
      {feedback && (
        <div role="status" className={`challenge-feedback ${feedback.isCorrect ? 'is-correct' : 'is-retry'}`}>
          <div className="challenge-question-head">
            <strong>{feedback.isCorrect ? '¡Muy bien!' : `La respuesta era: ${correctText ?? 'revisa la explicación'}`}</strong>
            <ListenButton compact subject="la explicación" text={feedback.explanation || question.explanation} />
          </div>
          <p>{feedback.explanation || question.explanation}</p>
        </div>
      )}
    </article>
  )
}

/** Reto diario de razonamiento lógico o comprensión lectora con contexto peruano (HU-40). */
export const DailyChallengeCard: React.FC<Props> = ({ refreshTrigger = 0, onCompleted }) => {
  const [challenge, setChallenge] = useState<DailyChallenge | null>(null)
  const [message, setMessage] = useState('')
  const [open, setOpen] = useState(false)
  const [answers, setAnswers] = useState<Record<string, AnswerFeedbackResponse>>({})
  const [result, setResult] = useState<DailyChallengeResult | null>(null)
  const [finishing, setFinishing] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    fetchDailyChallenge(controller.signal)
      .then(data => {
        setChallenge(data)
        // Lo respondido hoy viene del servidor: lo de esta pantalla manda si ya respondió aquí mismo.
        setAnswers(previous => ({
          ...Object.fromEntries((data.answeredToday ?? []).map(answer => [answer.questionId, answer])),
          ...previous,
        }))
        setMessage('')
      })
      .catch((e: Error) => {
        if (!controller.signal.aborted) setMessage(e.message)
      })
    return () => controller.abort()
  }, [refreshTrigger])

  if (message) {
    return (
      <section className="challenge-card" aria-label="Reto del día">
        <p className="challenge-empty">{message}</p>
      </section>
    )
  }
  if (!challenge) return null

  const Icon = challenge.skill === 'COMPRENSION_LECTORA' ? BookOpen : Puzzle
  const done = challenge.isCompleted || !!result
  const ready = canFinish(challenge.questions.map(q => q.id), answers)

  const finish = async () => {
    setFinishing(true)
    setError('')
    try {
      const data = await completeDailyChallenge(challenge.id)
      setResult(data)
      onCompleted?.()
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setFinishing(false)
    }
  }

  return (
    <section className={`challenge-card skill-${challenge.skill.toLowerCase()}`} aria-labelledby="challenge-title">
      <header className="challenge-head">
        <span className="challenge-icon" aria-hidden="true"><Icon size={22} /></span>
        <div className="challenge-head-text">
          <p className="challenge-kicker">Reto del día · {SKILL_LABEL[challenge.skill]}</p>
          <h3 id="challenge-title">{challenge.title}</h3>
          <p className="challenge-meta">
            <span><Clock size={14} aria-hidden="true" /> {challenge.estimatedMinutes} min</span>
            <span><Star size={14} aria-hidden="true" /> +{challenge.experienceReward} XP</span>
          </p>
        </div>
        <ListenButton subject="el reto del día" text={challengeSpeech(challenge)} />
      </header>

      {done ? (
        <div className="challenge-done" role="status">
          <Trophy size={20} aria-hidden="true" />
          <p>{result?.rupiMessage ?? '¡Ya completaste el reto de hoy! Mañana Rupi te trae uno nuevo.'}</p>
        </div>
      ) : !open ? (
        <>
          {challenge.description && <p className="challenge-desc">{challenge.description}</p>}
          <button type="button" className="challenge-btn is-primary" onClick={() => setOpen(true)}>
            Empezar el reto
          </button>
        </>
      ) : (
        <div className="challenge-body">
          {challenge.description && <p className="challenge-desc">{challenge.description}</p>}
          {challenge.readingText && (
            <article className="challenge-reading" aria-label="Lectura del reto">
              <div className="challenge-question-head">
                <h4>Lectura</h4>
                <ListenButton subject="la lectura" text={challenge.readingText} />
              </div>
              {challenge.readingText.split('\n\n').map((paragraph, i) => <p key={i}>{paragraph}</p>)}
            </article>
          )}
          {challenge.questions.map(question => (
            <ChallengeQuestion
              key={question.id}
              activityId={challenge.activityVersionId}
              question={question}
              feedback={answers[question.id]}
              onAnswered={feedback => setAnswers(prev => ({ ...prev, [question.id]: feedback }))}
            />
          ))}
          {error && <p role="alert" className="challenge-error">{error}</p>}
          <button type="button" className="challenge-btn is-primary" disabled={!ready || finishing} onClick={() => void finish()}>
            {finishing ? 'Guardando…' : ready ? 'Terminar el reto' : `Responde las ${challenge.questions.length} preguntas para terminar`}
          </button>
        </div>
      )}
    </section>
  )
}
