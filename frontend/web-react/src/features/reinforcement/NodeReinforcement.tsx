import { useEffect, useState } from 'react'
import { BookOpen, CheckCircle2 } from 'lucide-react'
import { reinforcementRequest, type ReinforcementView } from '../../services/reinforcementApi'
import './NodeReinforcement.css'

export function NodeReinforcement({ routeId, nodeId, revision }: { routeId: string; nodeId: string; revision: string }) {
  const [view, setView] = useState<ReinforcementView | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [retry, setRetry] = useState(0)
  const [saving, setSaving] = useState<string | null>(null)
  useEffect(() => {
    const controller = new AbortController()
    reinforcementRequest<ReinforcementView>(routeId, nodeId, '/refresh', 'POST', controller.signal)
      .then(data => { if (!controller.signal.aborted) { setView(previous => ({ ...data, resources: data.resources.map(r => ({ ...r, completed: r.completed || Boolean(previous?.resources.some(old => old.id === r.id && old.completed)) })) })); setError(null) } })
      .catch(() => { if (!controller.signal.aborted) setError('No pudimos consultar tu refuerzo. Puedes seguir con la lección y volver a intentarlo.') })
    return () => controller.abort()
  }, [routeId, nodeId, revision, retry])

  async function complete(id: string) {
    setSaving(id)
    try {
      await reinforcementRequest<void>(routeId, nodeId, `/${encodeURIComponent(id)}/complete`, 'PUT')
      setView(previous => previous ? { ...previous, resources: previous.resources.map(r => r.id === id ? { ...r, completed: true } : r) } : previous)
      setError(null)
    } catch { setError('No pudimos guardar tu práctica. Inténtalo de nuevo.') }
    finally { setSaving(null) }
  }
  return <section className="node-reinforcement" aria-label="Refuerzo de esta parada">
    <h2><BookOpen aria-hidden="true" size={24} /> Un ratito para practicar</h2>
    {error && <div role="alert"><p>{error}</p><button type="button" onClick={() => setRetry(n => n + 1)}>Volver a consultar</button></div>}
    {!view && !error && <p role="status">Rupi está revisando tu práctica…</p>}
    {view && <>
      {view.status === 'SIN_EVIDENCIA' && <p>Responde al menos dos preguntas de esta parada. Así podremos acompañarte con un refuerzo.</p>}
      {view.status === 'AVANCE' && <p>¡Tus respuestas muestran avances! Puedes volver a los materiales que ya practicaste.</p>}
      {(view.status === 'REFUERZO' || view.status === 'SIN_RECURSO') && <>
        <p>Vamos paso a paso con «{view.nodeTitle}». Repasemos juntos estas ideas.</p>
        <details><summary>Qué podemos repasar</summary><ul>{view.practice.map((item, index) => <li key={index}><strong>{item.question}</strong>{item.hint && <p>{item.hint}</p>}</li>)}</ul></details>
      </>}
      {view.status === 'SIN_RECURSO' && <p>Todavía no tenemos un material MINEDU verificado para este grado y área. Usa las pistas de la parada y pide orientación a tu profe.</p>}
      {view.resources.map(resource => <article key={resource.id} className="reinforcement-resource">
        <span className="reinforcement-source">Material oficial · MINEDU</span>
        <h3>{resource.title}</h3>
        <p>Abre el cuadernillo de tu grado. Con tu profe, elige una actividad relacionada con esta parada y resuélvela en tu cuaderno.</p>
        <p className="reinforcement-note">El enlace necesita internet y abre el repositorio del Ministerio de Educación. Las pistas de arriba pertenecen a la lección de Rupi.</p>
        <div className="reinforcement-actions">
          <a href={resource.url} target="_blank" rel="noopener noreferrer" aria-label="Abrir material MINEDU (nueva pestaña)">Abrir material MINEDU ↗</a>
          {resource.completed ? <span role="status"><CheckCircle2 aria-hidden="true" size={20} /> Ya lo practiqué</span> :
            <button type="button" disabled={saving !== null} onClick={() => void complete(resource.id)}>{saving === resource.id ? 'Guardando…' : 'Ya lo practiqué en mi cuaderno'}</button>}
        </div>
      </article>)}
      {view.resources.length > 0 && <p className="reinforcement-note">Esta marca guarda tu práctica personal. Tu avance en la ruta se obtiene resolviendo las paradas.</p>}
    </>}
  </section>
}
