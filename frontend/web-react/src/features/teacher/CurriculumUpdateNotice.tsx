import { useEffect, useState } from 'react'
import { ArrowRight, BookMarked, CheckCircle2, ExternalLink } from 'lucide-react'
import './CurriculumUpdateNotice.css'

interface Plan {
  available: boolean; message: string | null; fromVersion: string; toVersion: string | null
  fromArea: string; toArea: string | null; stopsWithoutUnit: number
  resolutions: { number: string; issuedOn: string; summary: string; officialLink: string | null }[]
  units: { fromTitle: string; toTitle: string | null; change: 'SE_MANTIENE' | 'MODIFICADA' | 'RETIRADA'; stops: number }[]
}

const base = (routeId: string) => `/api/v1/teacher/learning-routes/${encodeURIComponent(routeId)}/curriculum-update`
const CHANGE: Record<Plan['units'][number]['change'], string> = { SE_MANTIENE: 'Se mantiene', MODIFICADA: 'Cambia de nombre', RETIRADA: 'Se retira' }
const day = (value: string) => new Intl.DateTimeFormat('es-PE', { dateStyle: 'long', timeZone: 'UTC' }).format(new Date(`${value}T00:00:00Z`))

/**
 * HU-51: si el catálogo de la ruta quedó retirado por una resolución ministerial, el docente revisa la
 * correspondencia y actualiza su ruta a la versión vigente sin perder paradas ni el avance de sus estudiantes.
 */
export function CurriculumUpdateNotice({ routeId, onUpdated }: { routeId: string; onUpdated: () => void }) {
  const [plan, setPlan] = useState<Plan | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [done, setDone] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    fetch(base(routeId), { credentials: 'same-origin', signal: AbortSignal.any([controller.signal, AbortSignal.timeout(15000)]) })
      .then(r => (r.ok ? r.json() : null)).then((data: Plan | null) => { if (!controller.signal.aborted) setPlan(data) })
      .catch(() => { /* Sin aviso si no se pudo consultar: el editor sigue funcionando. */ })
    return () => controller.abort()
  }, [routeId])

  async function apply() {
    setBusy(true); setError('')
    try {
      const response = await fetch(base(routeId), { method: 'POST', credentials: 'same-origin', signal: AbortSignal.timeout(20000) })
      const data = await response.json().catch(() => null)
      if (!response.ok) throw new Error(data?.detail || 'No pudimos actualizar la ruta. Inténtalo otra vez.')
      setDone(`Listo: tu ruta ya sigue ${data.toVersion}. Tus paradas y el avance de tus estudiantes se conservaron.`)
      setPlan(null)
      onUpdated()
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : 'No pudimos actualizar la ruta.')
    } finally {
      setBusy(false)
    }
  }

  if (done) return <p className="curriculum-update-done" role="status"><CheckCircle2 size={20} aria-hidden="true" />{done}</p>
  if (!plan || plan.fromVersion === plan.toVersion) return null

  return <section className="curriculum-update" aria-labelledby="curriculum-update-title">
    <div className="curriculum-update-head">
      <BookMarked size={26} aria-hidden="true" />
      <div>
        <h3 id="curriculum-update-title">El MINEDU actualizó el currículo de esta ruta</h3>
        <p>Tu ruta sigue {plan.fromVersion}, que ya no está vigente. {plan.available
          ? <>Revisa los cambios y actualízala a <strong>{plan.toVersion}</strong>.</>
          : plan.message}</p>
      </div>
    </div>
    {plan.resolutions.map(r => <div className="curriculum-update-norm" key={r.number}>
      <strong>{r.number}</strong> · {day(r.issuedOn)}
      <p>{r.summary}</p>
      {r.officialLink && <a href={r.officialLink} target="_blank" rel="noreferrer noopener"><ExternalLink size={15} aria-hidden="true" />Leer la norma</a>}
    </div>)}
    {plan.available && <>
      <details className="curriculum-update-units">
        <summary>Ver qué pasa con tus {plan.units.length} {plan.units.length === 1 ? 'unidad' : 'unidades'}</summary>
        {plan.fromArea !== plan.toArea && <p>Área: {plan.fromArea} <ArrowRight size={14} aria-label="pasa a" /> {plan.toArea}</p>}
        <ul>{plan.units.map(u => <li key={u.fromTitle} className={`is-${u.change.toLowerCase()}`}>
          <span>{u.fromTitle}{u.change === 'MODIFICADA' && u.toTitle && <> <ArrowRight size={14} aria-label="ahora" /> {u.toTitle}</>}</span>
          <small>{CHANGE[u.change]} · {u.stops} {u.stops === 1 ? 'parada' : 'paradas'}</small>
        </li>)}</ul>
      </details>
      {plan.message && <p className="curriculum-update-warning">{plan.message}</p>}
      {error && <p className="curriculum-update-error" role="alert">{error}</p>}
      <button type="button" className="curriculum-update-btn" disabled={busy} onClick={() => void apply()}>
        {busy ? 'Actualizando…' : `Actualizar mi ruta a ${plan.toVersion}`}
      </button>
    </>}
  </section>
}
