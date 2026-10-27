import { useEffect, useState } from 'react'
import { quizRequest } from '../../services/quizzesApi'
import '../roadmap/StudyTouch.css'
import './StudyNotifications.css'

type Preferences = { dailyGoal: number; remindAt: string; goals: boolean; evaluations: boolean; system: boolean; quietFrom: string; quietUntil: string; revision: number }
type Status = { preferences: Preferences; completedToday: number; localDate: string }
type Notice = { id: string; title: string; body: string; read: boolean }
type Inbox = { items: Notice[]; quiet: boolean }
const path = 'student/notifications'
export function StudyNotifications() {
 const [status, setStatus] = useState<Status>()
 const [form, setForm] = useState<Preferences>()
 const [inbox, setInbox] = useState<Inbox>({ items: [], quiet: false })
 const [error, setError] = useState('')
 const [message, setMessage] = useState('')
 const [busy, setBusy] = useState(false)
 async function refreshForm(signal?: AbortSignal) {
  const result = await quizRequest<Status>(path + '/preferences', 'GET', undefined, signal)
  setStatus(result); setForm(result.preferences)
 }
 useEffect(() => {
  const controller = new AbortController()
  void refreshForm(controller.signal).catch(e => { if (!controller.signal.aborted) setError(e.message) })
  const refresh = () => { void quizRequest<Inbox>(path, 'GET', undefined, controller.signal).then(setInbox).catch(e => { if (!controller.signal.aborted) setError(e.message) }) }
  refresh()
  const timer = window.setInterval(refresh, 60000)
  window.addEventListener('focus', refresh)
  return () => { controller.abort(); clearInterval(timer); window.removeEventListener('focus', refresh) }
 }, [])
 async function save(event: React.FormEvent) {
  event.preventDefault(); if (!form || busy) return
  setBusy(true); setError(''); setMessage('')
  try {
   const result = await quizRequest<Status>(path + '/preferences', 'PUT', form)
   setStatus(result); setForm(result.preferences)
   setInbox(await quizRequest<Inbox>(path))
   setMessage('Preferencias guardadas para tu cuenta en todos tus dispositivos.')
  } catch (e) { setError(e instanceof Error ? e.message : 'No pudimos guardar. Inténtalo nuevamente.') }
  finally { setBusy(false) }
 }
 return <section className="study-touch study-notifications" aria-labelledby="study-notifications-title">
  <h2 id="study-notifications-title">Mi ritmo de estudio</h2>
  <p>Elige cuándo recordar tus metas. Las horas corresponden a Perú.</p>
  {error && <p role="alert">{error}</p>}
  {message && <p role="status">{message}</p>}
  {!form ? <button type="button" onClick={() => { void refreshForm().catch(e => setError(e.message)) }}>Cargar preferencias</button> : <form onSubmit={save}>
   <fieldset disabled={busy}><legend>Metas y recordatorios</legend>
    <label>Mi meta diaria de actividades<select value={form.dailyGoal} onChange={e => setForm({ ...form, dailyGoal: Number(e.target.value) })}>
     {[1, 2, 3, 4, 5].map(n => <option key={n} value={n}>{n} actividad{n > 1 ? 'es' : ''}</option>)}
    </select></label>
    <p>Completaste {status?.completedToday ?? 0} actividades hoy. La meta no cambia tus calificaciones.</p>
    <label><input type="checkbox" checked={form.goals} onChange={e => setForm({ ...form, goals: e.target.checked })} />Recordar mi meta pendiente y mi racha</label>
    <label>Hora del recordatorio<input type="time" required value={form.remindAt} onChange={e => setForm({ ...form, remindAt: e.target.value })} /></label>
    <label><input type="checkbox" checked={form.evaluations} onChange={e => setForm({ ...form, evaluations: e.target.checked })} />Avisos de mi docente: nuevas fechas de evaluación, materiales nuevos y evaluaciones de las próximas 24 horas</label>
    <label><input type="checkbox" checked={form.system} onChange={e => setForm({ ...form, system: e.target.checked })} />Permitir avisos en Android</label>
    <p>Activa también el permiso desde la aplicación Android. En esta web puedes consultar tus avisos.</p>
    <label>Descansar desde<input type="time" required value={form.quietFrom} onChange={e => setForm({ ...form, quietFrom: e.target.value })} /></label>
    <label>Volver a recibir avisos a las<input type="time" required value={form.quietUntil} onChange={e => setForm({ ...form, quietUntil: e.target.value })} /></label>
    <button type="submit">{busy ? 'Guardando…' : 'Guardar mis preferencias'}</button>
    <button type="button" onClick={() => { void refreshForm().then(() => setError('')).catch(e => setError(e.message)) }}>Recargar preferencias guardadas</button>
   </fieldset>
  </form>}
  <h3>Mis avisos</h3>
  {inbox.quiet && <p>Estás en horario de descanso. Los avisos del dispositivo están pausados.</p>}
  {!inbox.items.length && <p>No tienes recordatorios pendientes. Puedes continuar a tu ritmo.</p>}
  <ul>{inbox.items.map(item => <li key={item.id}><strong>{item.title}</strong><p>{item.body}</p>
   {item.read ? <span>Leído</span> : <button type="button" disabled={busy} onClick={async () => {
    setBusy(true)
    try { await quizRequest(path + '/' + item.id + '/read', 'PUT'); setInbox(await quizRequest<Inbox>(path)) }
    catch (e) { setError(e instanceof Error ? e.message : 'No pudimos actualizar el aviso.') }
    finally { setBusy(false) }
   }}>Marcar como leído</button>}
  </li>)}</ul>
 </section>
}
