import { useEffect, useState } from 'react'
import { quizRequest, type QuizReview } from '../../services/quizzesApi'
import './QuizPractice.css'
function ReviewCard({ item, query, refresh }: { item: QuizReview; query: string; refresh: () => void }) {
 const [note, setNote] = useState('')
 const [busy, setBusy] = useState(false)
 const [error, setError] = useState('')
 const context = JSON.parse(item.topic) as { grade: number; area: string; topic: string; content: string }
 async function review(approve: boolean) {
  if (busy) return
  setBusy(true); setError('')
  try { await quizRequest('teacher/ai-quizzes/' + item.id + '/review' + query, 'POST', { approve, note }); refresh() }
  catch (e) { setError((e as Error).message) } finally { setBusy(false) }
 }
 return <article>
  <h3>{context.topic} · {context.grade}.º</h3><p>Estado: {item.state} · {item.provider} / {item.model}</p>
  <details><summary>Contenido usado para generar</summary><p className="assigned-instructions">{context.content}</p></details>
  {item.reports.map((r, i) => <p key={i}>Reporte: {r}</p>)}
  {item.questions.map(q => <div key={q.id}><strong>{q.text}</strong><ul>{q.options.map(o => <li key={o.id}>{o.text}{q.correctOption === o.id ? ' · Respuesta propuesta' : ''}</li>)}</ul><p>{q.explanation}</p></div>)}
  <label>Resultado de la revisión<textarea maxLength={1000} value={note} onChange={e => setNote(e.target.value)} /></label>
  {error && <p role="alert">{error}</p>}
  <div className="quiz-actions"><button disabled={busy || !note.trim()} onClick={() => void review(true)}>Aprobar contenido revisado</button>
   <button disabled={busy || !note.trim()} onClick={() => void review(false)}>Retirar cuestionario</button></div>
 </article>
}
export function QuizReviews({ room, route }: { room: string; route: string }) {
 const [items, setItems] = useState<QuizReview[]>()
 const [error, setError] = useState('')
 const [revision, setRevision] = useState(0)
 const query = '?' + new URLSearchParams({ classroomId: room, routeId: route })
 useEffect(() => {
  const controller = new AbortController()
  quizRequest<QuizReview[]>('teacher/ai-quizzes' + query, 'GET', undefined, controller.signal)
   .then(data => { if (!controller.signal.aborted) { setItems(data); setError('') } })
   .catch((e: Error) => { if (!controller.signal.aborted) setError(e.message) })
  return () => controller.abort()
 }, [query, revision])
 return <details className="assigned-tasks"><summary>Revisión docente de cuestionarios IA</summary>
  <p>Comprueba contenido, respuestas y lenguaje antes de aprobar. La validación automática no sustituye tu revisión. Se muestran cuestionarios del aula y ruta seleccionadas.</p>
  {error && <p role="alert">{error}</p>}{!items && !error && <p role="status">Consultando cuestionarios…</p>}
  {items?.length === 0 && <p>Todavía no hay cuestionarios IA generados en esta selección.</p>}
  {items?.map(item => <ReviewCard key={item.id + item.state + revision} item={item} query={query} refresh={() => setRevision(n => n + 1)} />)}
  <button onClick={() => setRevision(n => n + 1)}>Actualizar revisión</button>
 </details>
}
