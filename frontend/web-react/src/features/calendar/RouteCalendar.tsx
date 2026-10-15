import { EvaluationReminders } from '../reminders/EvaluationReminders'
import { useEffect, useState } from 'react'
import { CalendarDays, ChevronLeft, ChevronRight } from 'lucide-react'
import { calendarRequest, type Schedule, type CalendarEvent } from '../../services/calendarApi'
import { limaInput, limaInstant, monthCells, occursOn, shiftMonth } from './calendarDates'
import './RouteCalendar.css'

const dateLabel = (iso: string) => new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Lima' }).format(new Date(iso))
export function RouteCalendar({ routeId, teacher = false }: { routeId: string; teacher?: boolean }) {
  const role = teacher ? 'teacher' : 'student'
  const [data, setData] = useState<Schedule | null>(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [retry, setRetry] = useState(0)
  const [busy, setBusy] = useState(false)
  const [month, setMonth] = useState(() => limaInput(new Date().toISOString()).slice(0, 7))
  const [room, setRoom] = useState('')
  const [target, setTarget] = useState('')
  const [start, setStart] = useState('')
  const [end, setEnd] = useState('')
  const [editing, setEditing] = useState(false)
  useEffect(() => {
    const abort = new AbortController()
    calendarRequest<Schedule>(role, routeId, '', {signal: abort.signal}).then(value => {
      if (!abort.signal.aborted) { setData(value); setError('') }
    }).catch(() => { if (!abort.signal.aborted) setError('No pudimos cargar el calendario. Vuelve a intentarlo.') })
    return () => abort.abort()
  }, [role, routeId, retry])
  const selectedRoom = room || data?.classrooms[0]?.id || ''
  const events = data?.events.filter(event => event.classroomId === selectedRoom) ?? []
  const visible = events.filter(event => limaInput(event.start).slice(0, 7) <= month && limaInput(event.end).slice(0, 7) >= month)
  function reset() { setEditing(false); setTarget(''); setStart(''); setEnd('') }
  function edit(event: CalendarEvent) {
    setRoom(event.classroomId); setTarget(`${event.type}:${event.targetId}`); setStart(limaInput(event.start)); setEnd(limaInput(event.end)); setEditing(true); setMessage('Modifica las fechas y pulsa Guardar fechas.')
  }
  async function save(event: React.FormEvent) {
    event.preventDefault(); setError(''); setMessage('')
    if (!start || !end || end < start) { setError('El final debe ser igual o posterior al inicio.'); return }
    const [type, targetId] = target.split(':')
    setBusy(true)
    try {
      const result = await calendarRequest<Schedule>('teacher', routeId, '', {method: 'PUT', body: JSON.stringify({classroomId: selectedRoom, type, targetId, start: limaInstant(start), end: limaInstant(end)})})
      setData(result); setMonth(start.slice(0, 7)); reset(); setMessage('Fechas guardadas. Tu aula ya puede consultarlas junto a la ruta.')
    } catch (err) { setError(err instanceof Error ? err.message : 'No se pudieron guardar las fechas.') }
    finally { setBusy(false) }
  }
  async function remove(event: CalendarEvent) {
    setBusy(true); setError(''); setMessage('')
    try {
      await calendarRequest('teacher', routeId, `/${encodeURIComponent(event.id)}?classroomId=${encodeURIComponent(event.classroomId)}`, {method: 'DELETE'})
      setData(prev => prev ? {...prev, events: prev.events.filter(e => e.id !== event.id)} : prev); reset(); setMessage('Fecha retirada del calendario.')
    } catch (err) { setError(err instanceof Error ? err.message : 'No se pudo retirar la fecha. Inténtalo otra vez.') }
    finally { setBusy(false) }
  }
  if (!teacher && data && data.classrooms.length === 0) {
    return null
  }

  return <section className="route-calendar" aria-label="Calendario del aula">
    <h3><CalendarDays aria-hidden="true" /> Calendario del aula</h3>
    <p>Unidades y evaluaciones de nuestra ruta. Horarios de Perú.</p>
    {data && <button type="button" disabled={busy} onClick={() => setRetry(r => r + 1)}>Actualizar fechas</button>}
    {error && <p role="alert">{error} {!data && <button type="button" onClick={() => setRetry(r => r + 1)}>Volver a intentar</button>}</p>}
    {message && <p role="status">{message}</p>}
    {!data && !error && <p role="status">Cargando las fechas…</p>}
    {data && <>
      {data.classrooms.length === 0 ? (
        <div className="calendar-no-classrooms-box">
          <p className="calendar-no-classrooms-text">
            Esta ruta curricular aún no tiene un aula activa asociada para este grado escolar. Puedes continuar planificando y editando tus paradas, preguntas y materiales didácticos con normalidad.
          </p>
        </div>
      ) : <>
        <label>Aula<select aria-label="Aula" value={selectedRoom} disabled={busy} onChange={e => {setRoom(e.target.value); reset()}}>{data.classrooms.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}</select></label>
        {!teacher && <EvaluationReminders key={routeId} routeId={routeId} classroomId={selectedRoom} />}
        <div className="calendar-month"><button type="button" aria-label="Mes anterior" onClick={() => setMonth(m => shiftMonth(m, -1))}><ChevronLeft /></button><strong aria-live="polite">{new Intl.DateTimeFormat('es-PE', {month: 'long', year: 'numeric', timeZone: 'UTC'}).format(new Date(`${month}-01T12:00:00Z`))}</strong><button type="button" aria-label="Mes siguiente" onClick={() => setMonth(m => shiftMonth(m, 1))}><ChevronRight /></button></div>
        <div className="calendar-grid" aria-label="Fechas del mes">
          {['Lu','Ma','Mi','Ju','Vi','Sá','Do'].map(day => <b key={day}>{day}</b>)}
          {monthCells(month).map((day, index) => { const scheduled = day ? events.filter(e => occursOn(e.start,e.end,day)) : []; return <span key={day ?? index} className={scheduled.length ? 'has-events' : ''} title={scheduled.map(e => e.title).join(', ')} aria-label={day ? `${day}${scheduled.length ? ': ' + scheduled.map(e => e.title).join(', ') : ''}` : undefined}>{day ? Number(day.slice(-2)) : ''}{scheduled.length > 0 && <small aria-hidden="true">●</small>}</span> })}
        </div>
        <p className="calendar-key">● Hay una unidad o evaluación programada.</p>
        {visible.length === 0 && <p>No hay fechas programadas para este mes.</p>}
        <ol className="calendar-events">{visible.map(event => <li key={event.id}>
          <span className="calendar-kind">{event.type === 'UNIDAD' ? 'Unidad' : 'Evaluación'}</span><strong>{event.title}</strong>
          <time dateTime={event.start}>{dateLabel(event.start)}</time><span>hasta {dateLabel(event.end)}</span>
          {teacher && <div className="calendar-actions"><button type="button" disabled={busy} onClick={() => edit(event)}>Editar fechas</button><button type="button" disabled={busy} onClick={() => remove(event)}>Retirar fecha</button></div>}
        </li>)}</ol>
        {teacher && <form onSubmit={save}>
          <p>Rupi enviará un recordatorio dentro de la aplicación durante las 24 horas anteriores a cada evaluación publicada, sin repetirlo para la misma fecha.</p>
          <h4>{editing ? 'Modificar fechas' : 'Programar unidad o evaluación'}</h4>
          {data.targets.length === 0 ? <p>Agrega unidades o paradas de tipo Cuestionario o Reto a esta ruta para programarlas.</p> : <>
            <label>Unidad o evaluación<select required value={target} disabled={busy || editing} onChange={e => setTarget(e.target.value)}><option value="">Selecciona una actividad</option>{data.targets.map(t => <option key={`${t.type}:${t.id}`} value={`${t.type}:${t.id}`}>{t.type === 'UNIDAD' ? 'Unidad' : 'Evaluación'} · {t.title}</option>)}</select></label>
            <label>Inicio (hora de Perú)<input required type="datetime-local" min="2000-01-01T00:00" max="2100-12-31T23:59" value={start} disabled={busy} onChange={e => setStart(e.target.value)} /></label>
            <label>Fin (hora de Perú)<input required type="datetime-local" min={start || '2000-01-01T00:00'} max="2100-12-31T23:59" value={end} disabled={busy} onChange={e => setEnd(e.target.value)} /></label>
            <p>Guardar otra vez la misma unidad o evaluación actualiza sus fechas para esta aula.</p>
            <button className="calendar-save" disabled={busy}>{busy ? 'Guardando…' : 'Guardar fechas'}</button>{editing && <button type="button" disabled={busy} onClick={reset}>Cancelar edición</button>}
          </>}
        </form>}
      </>}
    </>}
  </section>
}
