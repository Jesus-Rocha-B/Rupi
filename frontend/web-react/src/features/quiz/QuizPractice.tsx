import { useEffect, useRef, useState } from 'react'
import { quizRequest, type QuizAvailability, type QuizQuestion, type QuizView } from '../../services/quizzesApi'
import '../reinforcement/AssignedTasks.css'
import './QuizPractice.css'
function Question({ question, busy, answer, report }: { question: QuizQuestion; busy: boolean; answer: (id: string, option: string) => void; report: (id: string, reason: string) => void }) {
 const [selected, setSelected] = useState('')
 const [reason, setReason] = useState('')
 return <article className="quiz-question">
  <fieldset disabled={busy || question.selectedOption !== null}><legend>{question.text}</legend>
   {question.options.map(option => <label className="assigned-check" key={option.id}><input type="radio" name={question.id}
    value={option.id} checked={(question.selectedOption ?? selected) === option.id} onChange={() => setSelected(option.id)} />{option.text}</label>)}
   {!question.selectedOption && <button disabled={!selected || busy} onClick={() => answer(question.id, selected)}>Comprobar mi respuesta</button>}
  </fieldset>
  {question.selectedOption && <div role="status"><strong>{question.correct ? '¡Buen trabajo!' : 'Sigamos aprendiendo.'}</strong><p>{question.explanation}</p></div>}
  <details><summary>Creo que esta pregunta necesita revisión</summary><label>Cuéntanos qué debemos revisar
   <textarea maxLength={500} value={reason} onChange={e => setReason(e.target.value)} /></label>
   <button disabled={busy || !reason.trim()} onClick={() => report(question.id, reason)}>Enviar a revisión</button>
  </details>
 </article>
}
export function QuizPractice({ routeId, nodeId }: { routeId: string; nodeId: string }) {
 const path = 'student/learning-routes/' + encodeURIComponent(routeId) + '/nodes/' + encodeURIComponent(nodeId) + '/ai-quiz'
 const [availability, setAvailability] = useState<QuizAvailability>()
 const [view, setView] = useState<QuizView | null>(null)
 const [error, setError] = useState('')
 const [busy, setBusy] = useState(false)
 const [revision, setRevision] = useState(0)
 const pending = useRef(false)
 const lifetime = useRef<AbortController | null>(null)
 const requestId = useRef<string | null>(null)
 useEffect(() => { lifetime.current = new AbortController(); return () => lifetime.current?.abort() }, [])
 useEffect(() => {
  const abort = new AbortController()
  quizRequest<QuizAvailability>(path, 'GET', undefined, abort.signal).then(data => {
   if (!abort.signal.aborted) { setAvailability(data); setView(data.practice); setError('') }
  }).catch((e: Error) => { if (!abort.signal.aborted) setError(e.message) })
  return () => abort.abort()
 }, [path, revision])
 useEffect(() => {
  if (view?.status !== 'GENERANDO') return
  const timer = setTimeout(() => setRevision(n => n + 1), 3000)
  return () => clearTimeout(timer)
 }, [view, revision])
 async function perform(operation: () => Promise<QuizView>) {
  if (pending.current) return
  pending.current = true; setBusy(true); setError('')
  try {
   const data = await operation()
   if (!lifetime.current?.signal.aborted) { setView(data); if (data.status !== 'GENERANDO') requestId.current = null }
  } catch (e) { if (!lifetime.current?.signal.aborted) setError((e as Error).message) }
  finally { pending.current = false; if (!lifetime.current?.signal.aborted) setBusy(false) }
 }
 function generate() {
  requestId.current ??= crypto.randomUUID()
  void perform(() => quizRequest<QuizView>(path, 'POST', { requestId: requestId.current }, lifetime.current?.signal))
 }
 function answer(id: string, option: string) {
  if (view) void perform(() => quizRequest<QuizView>('student/ai-quizzes/' + view.practiceId + '/answers', 'POST', { questionId: id, optionId: option }, lifetime.current?.signal))
 }
 function report(id: string, reason: string) {
  if (view) void perform(async () => {
   await quizRequest('student/ai-quizzes/' + view.practiceId + '/reports', 'POST', { questionId: id, reason }, lifetime.current?.signal)
   return quizRequest<QuizView>('student/ai-quizzes/' + view.practiceId, 'GET', undefined, lifetime.current?.signal)
  })
 }
 const generating = view?.status === 'GENERANDO'
 return <section className="assigned-tasks quiz-practice" aria-labelledby="quiz-practice-title">
  <h2 id="quiz-practice-title">Practicar con Rupi</h2>
  <p>Tres a cinco preguntas sobre esta lección para comprobar lo aprendido. Es una práctica formativa y no cambia tu nota.</p>
  {error && <div role="alert"><p>{error}</p><button disabled={busy} onClick={() => setRevision(n => n + 1)}>Volver a consultar</button></div>}
  {!availability && !error && <p role="status">Buscando tu práctica…</p>}
  {availability && !availability.configured && <p>La generación con IA todavía no está habilitada.{availability.reviewedAvailable ? ' Puedes usar un cuestionario revisado por tu docente.' : ' Puedes continuar con las preguntas y materiales de tu lección.'}</p>}
  {(busy || generating) && <p role="status" aria-live="polite">{generating ? 'Preparando y revisando las preguntas. Puedes seguir con tu lección.' : 'Guardando o preparando tu práctica…'}</p>}
  {view?.status === 'FALLIDO' && <p>No pudimos preparar preguntas revisadas automáticamente. Tu avance anterior está guardado; puedes continuar tu lección.</p>}
  {view?.status === 'RETIRADO' && <p role="status">Este cuestionario se retiró para revisión. Gracias por ayudarnos a mejorar las preguntas.</p>}
  {view && view.questions.length > 0 && <>
   <p className="quiz-origin">{view.source === 'REUTILIZADO' ? 'Cuestionario existente reutilizado · revisión docente' : 'Cuestionario asistido por IA'} · {view.reviewState === 'REVISADO_DOCENTE' ? 'Revisado por docente' : 'Validación automática; puede contener errores'}</p>
   <p>{view.answered} de {view.questions.length} respuestas guardadas.</p>
   {view.questions.map(q => <Question key={view.practiceId + q.id} question={q} busy={busy} answer={answer} report={report} />)}
   {view.status === 'COMPLETADO' && <p role="status"><strong>¡Terminaste tu práctica!</strong> Acertaste {view.correct} de {view.questions.length}. Revisa las explicaciones con calma.</p>}
  </>}
  {availability && (!view || !['LISTO', 'GENERANDO'].includes(view.status)) && <button disabled={busy || (!availability.configured && !availability.reviewedAvailable)} onClick={generate}>
   {availability.configured ? 'Preparar cuestionario breve' : 'Usar cuestionario revisado'}</button>}
 </section>
}
