import { useEffect, useState } from 'react'
import { BookOpen, ExternalLink, RefreshCw } from 'lucide-react'
import { getStudyResources, type StudyResource } from '../../services/learningRoutesApi'

type State = { kind: 'loading' } | { kind: 'error' } | { kind: 'ready'; resources: StudyResource[] }

export function OfficialStudyResources({ routeId }: { routeId: string }) {
  const [state, setState] = useState<State>({ kind: 'loading' })
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    getStudyResources(routeId, controller.signal)
      .then((resources) => {
        if (!controller.signal.aborted) setState({ kind: 'ready', resources })
      })
      .catch(() => {
        if (!controller.signal.aborted) setState({ kind: 'error' })
      })
    return () => controller.abort()
  }, [routeId, retry])

  if (state.kind === 'ready' && state.resources.length === 0) return null

  return (
    <aside className="official-study" aria-label="Materiales oficiales para tu grado">
      <h5>
        <BookOpen size={20} aria-hidden="true" /> Biblioteca Escolar MINEDU
      </h5>
      <p>Cuadernos de trabajo oficiales elaborados por el Ministerio de Educación para tu grado escolar:</p>

      {state.kind === 'loading' && <p role="status">Cargando libros oficiales de tu grado...</p>}

      {state.kind === 'error' && (
        <div role="status" style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <p style={{ margin: 0 }}>No pudimos consultar las fuentes oficiales en este momento.</p>
          <button
            type="button"
            className="unit-download-action-btn"
            style={{ minHeight: '38px', padding: '6px 12px', fontSize: '13px' }}
            onClick={() => {
              setState({ kind: 'loading' })
              setRetry((value) => value + 1)
            }}
          >
            <RefreshCw size={14} aria-hidden="true" />
            <span>Reintentar</span>
          </button>
        </div>
      )}

      {state.kind === 'ready' && (
        <ul>
          {state.resources.map((resource) => (
            <li key={resource.sourceUrl}>
              <div>
                <strong>{resource.title}</strong>
                <span>
                  {resource.grade}.° de primaria · {resource.year} · MINEDU
                </span>
                {resource.note && <p style={{ margin: '4px 0 0', fontSize: '13px', color: '#43614c' }}>{resource.note}</p>}
              </div>

              <a
                className="official-study-link"
                href={resource.sourceUrl}
                target="_blank"
                rel="noopener noreferrer"
              >
                <span>Ver libro en MINEDU</span>
                <ExternalLink size={15} aria-hidden="true" />
                <span className="sr-only"> (abre otra pestaña; necesita internet)</span>
              </a>
            </li>
          ))}
        </ul>
      )}
    </aside>
  )
}
