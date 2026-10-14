import { useEffect, useState } from 'react'
import { BookOpen, ExternalLink } from 'lucide-react'
import { getStudyResources, type StudyResource } from '../../services/learningRoutesApi'

type State = { kind: 'loading' } | { kind: 'error' } | { kind: 'ready'; resources: StudyResource[] }

export function OfficialStudyResources({ routeId }: { routeId: string }) {
  const [state, setState] = useState<State>({ kind: 'loading' })
  const [retry, setRetry] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    getStudyResources(routeId, controller.signal).then(resources => {
      if (!controller.signal.aborted) setState({ kind: 'ready', resources })
    }).catch(() => {
      if (!controller.signal.aborted) setState({ kind: 'error' })
    })
    return () => controller.abort()
  }, [routeId, retry])

  if (state.kind === 'ready' && state.resources.length === 0) return null
  return (
    <aside className="official-study" aria-label="Materiales oficiales para tu grado">
      <h5><BookOpen size={20} aria-hidden="true" /> Tu biblioteca MINEDU</h5>
      {state.kind === 'loading' && <p role="status">Buscando el material de tu grado…</p>}
      {state.kind === 'error' && <div role="status">
        <p>No pudimos mostrar las fuentes. Puedes seguir descargando tu unidad.</p>
        <button type="button" className="unit-download-btn" onClick={() => {
          setState({ kind: 'loading' }); setRetry(value => value + 1)
        }}>Volver a intentar</button>
      </div>}
      {state.kind === 'ready' && <>
        <p>Abre el libro en el MINEDU y guarda su PDF antes de desconectarte. Se descarga por separado.</p>
        <ul>
          {state.resources.map(resource => <li key={resource.sourceUrl}>
            <strong>{resource.title}</strong>
            <span>{resource.grade}.° de primaria · {resource.year} · MINEDU</span>
            <p>{resource.note}</p>
            <a className="official-study-link" href={resource.sourceUrl} target="_blank" rel="noopener noreferrer">
              Ver y descargar en MINEDU <ExternalLink size={16} aria-hidden="true" />
              <span className="sr-only"> (abre otra pestaña; necesita internet)</span>
            </a>
          </li>)}
        </ul>
        <p className="official-study-note"><strong>Dentro de tu ZIP:</strong> repaso-rupi.pdf trae {state.resources.reduce((sum, item) => sum + item.exercises.length, 0)} ejercicios de repaso del grado y sus respuestas. Son creados por Rupi y funcionan sin internet. El libro del MINEDU no está dentro del ZIP.</p>
      </>}
    </aside>
  )
}
