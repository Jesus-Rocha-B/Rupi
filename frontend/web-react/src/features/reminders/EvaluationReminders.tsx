import { useEffect, useState } from 'react'
import { Bell, Check } from 'lucide-react'
import { remindersRequest, type EvaluationReminder } from '../../services/remindersApi'
import './EvaluationReminders.css'

const dateLabel = (value: string) => new Intl.DateTimeFormat('es-PE', {
  dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Lima',
}).format(new Date(value))

export function EvaluationReminders({ routeId, classroomId }: { routeId: string; classroomId: string }) {
  const [items, setItems] = useState<EvaluationReminder[] | null>(null)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)
  const [reading, setReading] = useState<string | null>(null)
  useEffect(() => {
    let disposed = false
    let loading = false
    const abort = new AbortController()
    async function refresh() {
      if (disposed || loading || document.hidden) return
      loading = true
      try {
        const next = await remindersRequest<EvaluationReminder[]>(routeId, '', { signal: abort.signal })
        if (!disposed) {
          // Un sondeo que comenzó antes de marcar «leído» no debe deshacer esa confirmación local.
          setItems(previous => next.map(item => ({ ...item, read: item.read || !!previous?.some(old => old.id === item.id && old.read) })))
          setError('')
        }
      } catch { if (!disposed) setError('No pudimos actualizar los recordatorios. Revisa tu conexión y vuelve a intentarlo.') }
      finally { loading = false }
    }
    void refresh()
    const timer = window.setInterval(() => { void refresh() }, 60000)
    const visible = () => { void refresh() }
    document.addEventListener('visibilitychange', visible)
    return () => { disposed = true; abort.abort(); window.clearInterval(timer); document.removeEventListener('visibilitychange', visible) }
  }, [routeId, retry])

  async function markRead(id: string) {
    setReading(id)
    try {
      await remindersRequest<void>(routeId, `/${encodeURIComponent(id)}/read`, { method: 'PUT' })
      setItems(previous => previous?.map(item => item.id === id ? { ...item, read: true } : item) ?? null)
      setError('')
    } catch { setError('No pudimos marcar el aviso como leído. Inténtalo nuevamente.'); setRetry(value => value + 1) }
    finally { setReading(null) }
  }

  const visible = items?.filter(item => item.classroomId === classroomId) ?? []
  const unread = visible.filter(item => !item.read).length
  return <section className="evaluation-reminders" aria-label="Recordatorios de evaluaciones">
    <h4><Bell size={20} aria-hidden="true" /> Tus próximas evaluaciones</h4>
    <p>Rupi te avisa durante las 24 horas anteriores. Horarios de Perú.</p>
    <p role="status">{items === null ? (error ? 'Los recordatorios no están disponibles por el momento.' : 'Buscando recordatorios…') : unread ? `${unread} ${unread === 1 ? 'aviso nuevo' : 'avisos nuevos'}` : 'Estás al día con tus avisos.'}</p>
    {error && <p role="alert">{error}</p>}
    <button type="button" onClick={() => setRetry(value => value + 1)}>Actualizar avisos</button>
    {items !== null && visible.length === 0 && <p>No tienes evaluaciones próximas con recordatorios.</p>}
    <ul>{visible.map(item => <li key={item.id} className={item.read ? 'is-read' : 'is-new'}>
      <strong>{item.title}</strong>
      <time dateTime={item.startsAt}>{dateLabel(item.startsAt)}</time>
      <span>{item.classroomName} · Prepara tus materiales y repasa con calma.</span>
      {item.read ? <span className="reminder-read"><Check size={16} aria-hidden="true" /> Ya lo leíste</span> :
        <button type="button" disabled={reading !== null} aria-label={`Marcar como leído: ${item.title}`} onClick={() => { void markRead(item.id) }}>
          {reading === item.id ? 'Guardando…' : 'Ya lo leí'}
        </button>}
    </li>)}</ul>
  </section>
}
