import { useState } from 'react'
import { ArrowRight } from 'lucide-react'
import { RupiCharacter } from '../../components/RupiCharacter'
import { Flashcards } from '../flashcards/Flashcards'
import { QuizPractice } from '../quiz/QuizPractice'
import type { QuestionDto, AnswerFeedbackResponse } from '../../services/learningRoutesApi'
import './LessonReview.css'
import { ExplorerBackpack, HappyReviewBook } from './ReviewIllustrations'
import { CorrectionChallenge } from './CorrectionChallenge'

type Props = { routeId: string; nodeId: string; questions: QuestionDto[]; feedback: Record<string, AnswerFeedbackResponse>; mode?: "cards" | "practice"; correctionQuestions?: QuestionDto[] }
function LessonRecall({ questions, feedback, cards = false }: Pick<Props, 'questions' | 'feedback'> & { cards?: boolean }) {
  const [index, setIndex] = useState(0)
  const [revealed, setRevealed] = useState(false)
  const [selected, setSelected] = useState('')
  const [checked, setChecked] = useState(false)
  const usable = questions.filter(q => cards ? Boolean(feedback[q.id]?.explanation || q.explanation || feedback[q.id]?.correctOptionId) : Boolean(feedback[q.id]?.correctOptionId))
  const question = usable[index]
  if (!question) return <p role="status">Esta lección todavía no tiene preguntas de repaso disponibles. Puedes volver a leer sus materiales.</p>
  const result = feedback[question.id]
  const answer = question.options.find(o => o.id === result?.correctOptionId)?.text
  function move(delta: number) { setIndex(n => n + delta); setRevealed(false); setSelected(''); setChecked(false) }
  return <div className="lesson-recall">
    <p>Repaso de tu lección · No cambia tus notas ni suma puntos. Se reinicia al cerrar.</p>
    <p aria-live="polite">{cards ? 'Tarjeta' : 'Pregunta'} {index + 1} de {usable.length}</p>
    {cards ? <button type="button" className="recall-face" onClick={() => setRevealed(v => !v)} aria-label={revealed ? 'Volver a la pregunta' : 'Voltear para ver la respuesta'}>
      <small>{revealed ? 'Respuesta · Toca para volver' : 'Pregunta · Toca para voltear'}</small>
      <strong>{revealed ? answer || result?.explanation || question.explanation : question.statement}</strong>
      {revealed && answer && <span>{result?.explanation || question.explanation}</span>}
    </button> : <fieldset><legend>{question.statement}</legend>
      {question.options.map(o => <label key={o.id}><input type="radio" name={`review-${question.id}`} checked={selected === o.id} disabled={checked} onChange={() => setSelected(o.id)} />{o.text}</label>)}
      <button type="button" disabled={!selected || checked} onClick={() => setChecked(true)}>Comprobar respuesta</button>
      {checked && <p role="status">{selected === result.correctOptionId ? '¡Muy bien! ' : `Sigamos aprendiendo. La respuesta es: ${answer}. `}{result.explanation || question.explanation}</p>}
    </fieldset>}
    <div className="recall-navigation"><button type="button" disabled={index === 0} onClick={() => move(-1)}>Anterior</button>
      {index < usable.length - 1 ? <button type="button" onClick={() => move(1)}>Siguiente</button> : <button type="button" onClick={() => move(-index)}>Volver a repasar</button>}</div>
  </div>
}

export function LessonReview({ routeId, nodeId, questions, feedback, mode = "cards", correctionQuestions }: Props) {
  const [active, setActive] = useState<string | null>(null)
  const entries = [
    { id: 'cards', title: '¡Repasar con tarjetas!', text: 'Voltea la tarjeta y descubre la respuesta.', action: '¡Jugar!' },
    { id: 'quiz', title: '¡Practica con Rupi!', text: 'Responde preguntas rápidas sobre tu lección.', action: '¡Empezar!' },
    { id: 'reinforce', title: '¡Un ratito para practicar!', text: 'Encuentra el error y toca la respuesta correcta.', action: '¡Vamos!' },
  ]
  return <div className="lesson-review study-touch" aria-label="Recursos de repaso">
    {entries.filter(entry => mode === "cards" ? entry.id === "cards" : entry.id !== "cards").map(entry => <section key={entry.id} className={`review-tile review-${entry.id}`}>
      <div className="review-launch">
        <div className="review-art" aria-hidden="true">{entry.id === 'reinforce' ? <HappyReviewBook /> : entry.id === 'quiz' ? <ExplorerBackpack /> : <RupiCharacter role="reader" mood="idle" accessories={{ sombrero: 'SOMBRERO_EXPLORADOR' }} />}</div>
        <div className="review-copy"><h2>{entry.title}</h2><p>{entry.text}</p></div>
        <button className="review-launch-button" type="button" aria-expanded={active === entry.id} aria-controls={`review-${nodeId}-${entry.id}`} onClick={() => setActive(v => v === entry.id ? null : entry.id)}>{active === entry.id ? 'Cerrar' : entry.action}<ArrowRight size={18} aria-hidden="true" /></button>
      </div>
      <div id={`review-${nodeId}-${entry.id}`} hidden={active !== entry.id} className="review-content">
        {active === entry.id && (entry.id === 'cards' ? <Flashcards routeId={routeId} nodeId={nodeId} fallback={<LessonRecall questions={questions} feedback={feedback} cards />} /> : entry.id === 'quiz' ? correctionQuestions ? <LessonRecall questions={questions} feedback={feedback} /> : <QuizPractice routeId={routeId} nodeId={nodeId} fallback={<LessonRecall questions={questions} feedback={feedback} />} /> : <CorrectionChallenge questions={correctionQuestions ?? questions} feedback={feedback} />)}
      </div>
    </section>)}
  </div>
}
