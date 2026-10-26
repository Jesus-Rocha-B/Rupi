import { useEffect, useRef, useState } from 'react'
import { quizRequest } from '../../services/quizzesApi'
import type { Card } from './Flashcards'
import '../reinforcement/AssignedTasks.css'
export function FlashcardEditor({ room, route }: { room: string; route: string }) {
 const query = '?' + new URLSearchParams({ classroomId: room, routeId: route })
 const [data, setData] = useState<{ nodes: { id: string; title: string }[]; cards: Card[] }>()
 const [node, setNode] = useState(''), [question, setQuestion] = useState(''), [answer, setAnswer] = useState(''), [source, setSource] = useState('')
 const [reviewed, setReviewed] = useState(false), [busy, setBusy] = useState(false), [error, setError] = useState(''), [version, setVersion] = useState(0)
 const pending = useRef<{ requestId: string; nodeId: string; question: string; answer: string; source: string; reviewed: boolean } | null>(null)
 const [hasPending, setHasPending] = useState(false)
 const lock = useRef(false)
 const life = useRef<AbortController | null>(null)
 useEffect(() => { const c = new AbortController(); life.current = c; return () => c.abort() }, [])
 useEffect(() => {
  const c = new AbortController()
  quizRequest<typeof data>('teacher/flashcards' + query, 'GET', undefined, c.signal).then(d => { if (!c.signal.aborted) setData(d) }).catch((e: Error) => { if (!c.signal.aborted) setError(e.message) })
  return () => c.abort()
 }, [query, version])
 async function save(id?: string) {
  if (lock.current) return
  lock.current = true; setBusy(true); setError('')
  try {
   if (id) await quizRequest('teacher/flashcards/' + id + query, 'DELETE', undefined, life.current?.signal)
   else {
    pending.current ??= { requestId: crypto.randomUUID(), nodeId: node, question, answer, source, reviewed }
    setHasPending(true)
    await quizRequest('teacher/flashcards' + query, 'POST', pending.current, life.current?.signal)
    if (!life.current?.signal.aborted) { pending.current = null; setHasPending(false); setQuestion(''); setAnswer(''); setReviewed(false) }
   }
   if (!life.current?.signal.aborted) setVersion(n => n + 1)
  } catch (e) { if (!life.current?.signal.aborted) setError((e as Error).message) }
  finally { lock.current = false; if (!life.current?.signal.aborted) setBusy(false) }
 }
 return <details className="assigned-tasks"><summary>Crear tarjetas de memoria revisadas</summary>
  <p>Publica conceptos breves adecuados al grado y al tema. Para corregir una tarjeta, retírala y crea una nueva versión; su autoevaluación anterior se conserva.</p>
  {error && <p role="alert">{error}</p>}
  <fieldset disabled={busy || hasPending}><legend>Contenido de la tarjeta</legend>
   <label>Actividad<select value={node} onChange={e => setNode(e.target.value)}><option value="">Elige una actividad</option>{data?.nodes.map(n => <option key={n.id} value={n.id}>{n.title}</option>)}</select></label>
   <label>Pregunta<textarea maxLength={500} value={question} onChange={e => setQuestion(e.target.value)} /></label>
   <label>Respuesta breve<textarea maxLength={1500} value={answer} onChange={e => setAnswer(e.target.value)} /></label>
   <label>Fuente o referencia educativa<input maxLength={1000} value={source} onChange={e => setSource(e.target.value)} /></label>
   <label className="assigned-check"><input type="checkbox" checked={reviewed} onChange={e => setReviewed(e.target.checked)} />Revisé la exactitud, el lenguaje y la relación con el tema.</label>
  </fieldset>
  <button disabled={busy || !node || !reviewed || !question.trim() || !answer.trim() || !source.trim()} onClick={() => void save()}>{hasPending ? 'Reintentar publicación' : 'Publicar tarjeta revisada'}</button>
  {data?.cards.length === 0 && <p>Aún no has publicado tarjetas en esta aula y ruta.</p>}
  {data?.cards.map(c => <article key={c.id}><strong>{c.question}</strong><p>{c.answer}</p><p>{c.source}</p><button disabled={busy} onClick={() => void save(c.id)}>Retirar tarjeta</button></article>)}
 </details>
}
