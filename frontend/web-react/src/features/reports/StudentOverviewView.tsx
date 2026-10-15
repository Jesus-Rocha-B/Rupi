import { reportQuery, type StudentOverview } from '../../services/reportsApi'
import { useReportData } from './useReportData'
import { StudentRoadmapProgress } from './StudentRoadmapProgress'
export function StudentOverviewView({ room, route, student, onViewChange }: { room: string; route: string; student: string; onViewChange: (view: string) => void }) {
  const { data, error, retry } = useReportData<StudentOverview>('/student-overview' + reportQuery(room, route, student))
  if (error) return <div className="report-error" role="alert"><p>{error}</p><button type="button" onClick={retry}>Reintentar resumen</button></div>
  if (!data) return <p role="status">Preparando el resumen del estudiante…</p>
  const attention = data.units.filter(u => u.status === 'REQUIERE_ATENCION')
  const averages = data.units.filter(u => u.average !== null)
  return <section className="student-overview" aria-labelledby="student-overview-title">
    <h2 id="student-overview-title">Acompañamiento de {data.student.name}</h2><p>{data.classroom.name} · {data.route.title} · Usuario escolar: {data.student.schoolUsername}</p>
    {data.enrollmentState === null ? <p className="report-attention">Sin inscripción en esta ruta. No hay resultados atribuibles al estudiante.</p> : <p className="report-note">Inscripción: {data.enrollmentState.toLowerCase()}. {data.answered === 0 && 'Todavía no tiene respuestas evaluables en esta ruta.'}</p>}
    <dl className="student-indicators"><div><dt>Paradas completadas</dt><dd>{data.completedNodes} / {data.totalNodes}</dd></div>
      <div><dt>Preguntas respondidas</dt><dd>{data.answered} / {data.totalQuestions}</dd></div>
      <div><dt>Aciertos</dt><dd>{data.answered === 0 ? 'Sin respuestas' : `${data.correct} / ${data.answered}`}</dd></div>
      <div><dt>Unidades por revisar</dt><dd>{attention.length}</dd></div></dl>
    <StudentRoadmapProgress data={data} />
    <h3>Unidades que requieren acompañamiento</h3>
    {attention.length ? <ul>{attention.map(u => <li key={u.id}>{u.title}</li>)}</ul> : <p>{data.answered === 0 ? 'Aún no hay evidencia para identificar dificultades.' : 'Sin alertas con la evidencia disponible.'}</p>}
    <p className="report-note">Las alertas orientan tu revisión; no equivalen a una evaluación oficial de competencias.</p>
    <h3>Promedios disponibles</h3>{averages.length ? <ul>{averages.map(u => <li key={u.id}>{u.title}: <strong>{u.average!.value.toLocaleString('es-PE', { minimumFractionDigits: 1 })} / 20 · {u.average!.letter}</strong> (orientativo)</li>)}</ul> : <p>Sin calificaciones: todavía no hay intentos completos válidos para calcular promedios.</p>}
    <div className="report-actions"><a href="#report-detail" onClick={() => onViewChange('results')}>Ver resultados por unidad</a><a href="#report-detail" onClick={() => onViewChange('grades')}>Ver detalle de calificaciones</a><button type="button" onClick={retry}>Actualizar resumen</button></div>
    <details><summary>Actividades con respuestas ({data.activities.length})</summary>{data.activities.length ? <ul>{data.activities.map(a => <li key={a.id}><strong>{a.title}</strong> · {a.answered} respuestas · {a.correct} aciertos</li>)}</ul> : <p>Sin actividades con respuestas evaluables.</p>}</details>
    <details><summary>Refuerzos vinculados ({data.reinforcements.length})</summary>{data.reinforcements.length ? <ul>{data.reinforcements.map(r => <li key={r.id}><strong>{r.nodeTitle}</strong><p>{r.url ? <a href={r.url} target="_blank" rel="noopener noreferrer">{r.title} · MINEDU (abre otra pestaña)</a> : r.title}</p><p>{r.completedAt ? 'Realizado según registro del estudiante' : 'Pendiente de realizar'}</p></li>)}</ul> : <p>No tiene refuerzos registrados para esta ruta.</p>}<p className="report-note">Marcar un refuerzo como realizado no acredita dominio de la competencia.</p></details>
  </section>
}
