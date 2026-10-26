import { useEffect, useRef, useState } from 'react'
import { quizRequest } from '../../services/quizzesApi'
import { SummaryContent, type Summary } from './VisualSummary'
function Review({ item, query, reload }: { item: Summary; query: string; reload: () => void }) {
 const [note, setNote] = useState(''), [busy, setBusy] = useState(false), [error, setError] = useState('')
 const lock = useRef(false)
 async function save(approve: boolean) {
  if (lock.current) return
  lock.current = true; setBusy(true)
  try { await quizRequest('teacher/visual-summaries/' + item.id + '/review' + query, 'POST', { approve, note }); reload() }
  catch (e) { setError((e as Error).message) } finally { lock.current = false; setBusy(false) }
 }
 return <article><p>Estado: {item.state}</p><SummaryContent item={item} />
  <label>Resultado de la revisión<textarea maxLength={1000} value={note} onChange={e => setNote(e.target.value)} /></label>
  {error && <p role="alert">{error}</p>}
  <button disabled={busy || !note.trim()} onClick={() => void save(true)}>Aprobar resumen revisado</button>{' '}
  <button disabled={busy || !note.trim()} onClick={() => void save(false)}>Retirar resumen</button>
 </article>
}
export function SummaryReviews({ room, route }: { room: string; route: string }) {
 const [items, setItems] = useState<Summary[]>([]), [error, setError] = useState(''), [revision, setRevision] = useState(0)
 const query = '?' + new URLSearchParams({ classroomId: room, routeId: route })
 useEffect(() => { const c = new AbortController(); quizRequest<Summary[]>('teacher/visual-summaries' + query, 'GET', undefined, c.signal).then(d => { if (!c.signal.aborted) setItems(d) }).catch((e: Error) => { if (!c.signal.aborted) setError(e.message) }); return () => c.abort() }, [query, revision])
 return <details className="assigned-tasks"><summary>Revisión docente de resúmenes visuales</summary><p>Comprueba exactitud, ejemplos y lenguaje. Los resúmenes se comparten entre estudiantes con acceso al mismo tema.</p>
  {error && <p role="alert">{error}</p>}{items.length === 0 && <p>No hay resúmenes generados en esta ruta.</p>}
  {items.map(item => <Review key={item.id + item.state} item={item} query={query} reload={() => setRevision(n => n + 1)} />)}
 </details>
}
