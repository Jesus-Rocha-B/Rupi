import { useEffect, useState } from 'react'
import { Bell, CheckCheck } from 'lucide-react'
import { quizRequest } from '../../services/quizzesApi'
import './StudyInbox.css'

type Notice = { id: string; title: string; body: string; read: boolean }
type Inbox = { items: Notice[]; quiet: boolean }
const path = 'student/notifications'

/** HU-28: bandeja de avisos del docente (fechas de evaluación y materiales nuevos) y de la racha. */
export function StudyInbox() {
 const [inbox, setInbox] = useState<Inbox>({ items: [], quiet: false })
 const [loaded, setLoaded] = useState(false)
 const [error, setError] = useState('')
 const [busy, setBusy] = useState(false)
 useEffect(() => {
  const controller = new AbortController()
  const refresh = () => {
   void quizRequest<Inbox>(path, 'GET', undefined, controller.signal)
    .then(result => { setInbox(result); setLoaded(true); setError('') })
    .catch(e => { if (!controller.signal.aborted) setError(e.message) })
  }
  refresh()
  const timer = window.setInterval(refresh, 60000)
  window.addEventListener('focus', refresh)
  return () => { controller.abort(); clearInterval(timer); window.removeEventListener('focus', refresh) }
 }, [])
 async function markRead(id: string) {
  setBusy(true)
  try { await quizRequest(path + '/' + id + '/read', 'PUT'); setInbox(await quizRequest<Inbox>(path)) }
  catch (e) { setError(e instanceof Error ? e.message : 'No pudimos actualizar el aviso.') }
  finally { setBusy(false) }
 }
 const pending = inbox.items.filter(item => !item.read).length
 return <section className="study-inbox" aria-labelledby="study-inbox-title">
  <div className="study-inbox-head">
   <span className="study-inbox-icon" aria-hidden="true"><Bell size={20} /></span>
   <div>
    <h3 id="study-inbox-title">Mis avisos</h3>
    <p>{pending ? `${pending} sin leer` : 'Fechas de evaluación y materiales nuevos de tu profe'}</p>
   </div>
  </div>
  {error && <p role="alert" className="study-inbox-error">{error}</p>}
  {inbox.quiet && <p className="study-inbox-quiet">Estás en horario de descanso. Los avisos del celular están en pausa.</p>}
  {loaded && !inbox.items.length && <p className="study-inbox-empty">No tienes avisos nuevos. Puedes continuar a tu ritmo.</p>}
  {inbox.items.length > 0 && <ul>{inbox.items.map(item => <li key={item.id} className={item.read ? 'is-read' : undefined}>
   <div>
    <strong>{item.title}</strong>
    <p>{item.body}</p>
   </div>
   {item.read
    ? <span className="study-inbox-read"><CheckCheck size={16} aria-hidden="true" />Leído</span>
    : <button type="button" disabled={busy} onClick={() => void markRead(item.id)}>Marcar como leído</button>}
  </li>)}</ul>}
 </section>
}
