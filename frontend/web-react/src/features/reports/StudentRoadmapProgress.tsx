import { Check, Lock, MapPin, Play } from 'lucide-react'
import type { RoadmapStop, StudentOverview } from '../../services/reportsApi'

const stateLabels: Record<NonNullable<RoadmapStop['state']>, string> = {
  COMPLETADO: 'Completada', EN_CURSO: 'En curso', DISPONIBLE: 'Disponible', BLOQUEADO: 'Bloqueada',
}
const stateIcons = { COMPLETADO: Check, EN_CURSO: Play, DISPONIBLE: MapPin, BLOQUEADO: Lock }
const formatGrade = (value: number) => value.toLocaleString('es-PE', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
const formatDate = (iso: string) => new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeZone: 'America/Lima' }).format(new Date(iso))

/** Estado del roadmap del estudiante, parada por parada, con la nota de cada avance. */
export function StudentRoadmapProgress({ data }: { data: StudentOverview }) {
  const stops = data.roadmap ?? []
  if (!stops.length) return null
  const enrolled = data.enrollmentState !== null
  const done = stops.filter(s => s.state === 'COMPLETADO').length
  const percent = Math.round((done / stops.length) * 100)
  const graded = stops.filter(s => s.grade)
  return <section className="student-roadmap" aria-labelledby="student-roadmap-title">
    <h3 id="student-roadmap-title">Estado de su roadmap</h3>
    <div className="student-roadmap-summary">
      <div className="student-roadmap-bar" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={percent}
        aria-label={`Avance del roadmap: ${done} de ${stops.length} paradas`}>
        <span style={{ width: `${percent}%` }} />
      </div>
      <p><strong>{done} de {stops.length} paradas</strong> ({percent} %)
        {' · '}{data.overallAverage
          ? <>Promedio del avance: <strong>{formatGrade(data.overallAverage.value)} / 20 · {data.overallAverage.letter}</strong> en {graded.length} {graded.length === 1 ? 'parada calificada' : 'paradas calificadas'}</>
          : 'Sin paradas calificadas todavía'}</p>
    </div>
    {!enrolled && <p className="report-attention">Todavía no se inscribe en esta ruta: el mapa muestra las paradas que le esperan.</p>}
    <ol className="student-roadmap-stops">
      {stops.map(stop => {
        const state = stop.state ?? 'BLOQUEADO'
        const Icon = stateIcons[state]
        return <li key={stop.id} className={`roadmap-stop is-${state.toLowerCase()}`}>
          <span className="roadmap-stop-marker" aria-hidden="true"><Icon size={18} /></span>
          <div className="roadmap-stop-body">
            <strong>{stop.sequence}. {stop.title}</strong>
            <small>{stop.unitTitle}{stop.optional ? ' · Opcional' : ''}</small>
            <span className="roadmap-stop-state">{enrolled ? stateLabels[state] : 'Sin inscripción'}
              {stop.completedAt && ` · ${formatDate(stop.completedAt)}`}</span>
          </div>
          <span className={`roadmap-stop-grade${stop.grade ? '' : ' is-empty'}`}>
            {stop.grade ? <><b>{formatGrade(stop.grade.value)}</b>/20 · {stop.grade.letter}</> : 'Sin nota'}
          </span>
        </li>
      })}
    </ol>
    <p className="report-note">La nota de cada parada usa su último intento completo con preguntas calificables (escala 0 a 20). Es orientativa y no reemplaza la evaluación oficial.</p>
  </section>
}
