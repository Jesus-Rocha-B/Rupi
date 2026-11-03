import { useState } from 'react'
import type { QuestionDto, AnswerFeedbackResponse } from '../../services/learningRoutesApi'

/** Correct a distractor from the studied lesson using touch-friendly choices. */
export function CorrectionChallenge({ questions, feedback }: { questions: QuestionDto[]; feedback: Record<string, AnswerFeedbackResponse> }) {
  const [index, setIndex] = useState(0)
  const [answer, setAnswer] = useState('')
  const [revealed, setRevealed] = useState(false)
  const challenges = questions.flatMap(question => {
    const result = feedback[question.id]
    const correct = question.options.find(o => o.id === result?.correctOptionId)
    const wrong = question.options.find(o => o.id !== result?.correctOptionId && o.text.trim() !== correct?.text.trim())
    return correct && wrong ? [{ question, correct, wrong, explanation: result.explanation || question.explanation }] : []
  })
  const challenge = challenges[index]
  if (!challenge) return <p role="status">Este tema aún necesita un reto de corrección revisado. Mientras tanto, puedes repasar sus materiales.</p>
  function move(next: number) { setIndex(next); setAnswer(''); setRevealed(false) }
  return <div className="lesson-recall correction-challenge">
    <h3>¡Encuentra la respuesta que corrige el error!</h3>
    <p>Toca una respuesta. Este repaso no cambia tus notas ni suma puntos.</p>
    <p aria-live="polite">Reto {index + 1} de {challenges.length}</p>
    <p><strong>Recuerda esta situación:</strong> {challenge.question.statement}</p>
    <blockquote><strong>Respuesta equivocada para corregir:</strong> {challenge.wrong.text}</blockquote>
    <div className="correction-options" role="group" aria-label="Elige la respuesta que corrige el error">
      {challenge.question.options.map(option => <button key={option.id} type="button" aria-pressed={answer === option.id} disabled={revealed} onClick={() => { setAnswer(option.id); setRevealed(option.id === challenge.correct.id) }}><span aria-hidden="true">{answer === option.id ? (revealed ? '✓' : '↻') : '○'}</span>{option.text}</button>)}
    </div>
    {answer && <div role="status">{revealed ? <><p><strong>¡Lo corregiste! {challenge.correct.text}</strong></p><p>{challenge.explanation}</p></> : <p>Esa respuesta todavía no corrige el error. Lee la situación y prueba otra opción.</p>}</div>}
    <div className="recall-navigation"><button type="button" disabled={index === 0} onClick={() => move(index - 1)}>Reto anterior</button>{index + 1 < challenges.length ? <button type="button" onClick={() => move(index + 1)}>Otro reto</button> : <button type="button" onClick={() => move(0)}>Reiniciar retos</button>}</div>
  </div>
}
