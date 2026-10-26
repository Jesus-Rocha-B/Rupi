import { TeacherAssignments } from '../reinforcement/AssignedTasks'
import { StudentFilter } from './StudentFilter'
import { StudentOverviewView } from './StudentOverviewView'
import { ReportDownload } from './ReportDownload'
import { useReportData } from './useReportData'
import { GradesReportView } from './GradesReportView'
import { useState } from 'react'
import { ArrowLeft, BookOpen, LogOut } from 'lucide-react'
import { reportQuery, type ReportMember, type ReportClassroom, type ReportRoute, type ResultsReport, type StudentResult } from '../../services/reportsApi'
import './TeacherReportsView.css'

const labels: Record<StudentResult['status'], string> = {
  SIN_INSCRIPCION: 'Sin inscripción', SIN_RESPUESTAS: 'Sin respuestas',
  EVIDENCIA_INSUFICIENTE: 'Falta evidencia', REQUIERE_ATENCION: 'Requiere atención', EN_AVANCE: 'En avance',
}
function ReportError({ message, retry }: { message: string; retry: () => void }) {
  return <div className="report-error" role="alert"><p>{message}</p><button type="button" onClick={retry}>Volver a consultar</button></div>
}
function ReportResults({ room, route, student }: { room: string; route: string; student: string }) {
  const { data, error, retry } = useReportData<ResultsReport>(reportQuery(room, route, student))
  if (error) return <ReportError message={error} retry={retry} />
  if (!data) return <p role="status">Preparando los resultados del aula…</p>
  const attention = new Set(data.units.flatMap(unit => unit.students.filter(row => row.status === 'REQUIERE_ATENCION').map(row => row.studentId))).size
  return <div className="results-report" aria-label="Resultados del aula">
    <div className="report-summary">
      <div><h2>{data.route.title}</h2><p>{data.classroom.name} · {data.studentCount} {data.studentCount === 1 ? 'estudiante' : 'estudiantes'} · {attention} con unidades por revisar</p>
        <p className="report-note">Consultado: {new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Lima' }).format(new Date(data.generatedAt))} (Perú)</p></div>
      <div className="report-actions"><button type="button" onClick={retry}>Actualizar reporte</button>
        <ReportDownload path={'/export' + reportQuery(room, route, student)} filename={student ? 'rupi-resultados-estudiante.csv' : 'rupi-resultados.csv'} available={data.units.some(u => u.students.some(s => s.answered > 0 || s.completedNodes > 0))} /></div>
    </div>
    <div className="report-criteria" id="report-criteria">
      <strong>Cómo leer los resultados</strong>
      <p>Se señala atención cuando hay al menos dos preguntas respondidas y menos del 70 % de aciertos. Una unidad sin respuestas no se considera de bajo rendimiento. El avance de paradas y los aciertos se muestran por separado.</p>
      <p>Las competencias señaladas pertenecen a la unidad: la alerta es indirecta y orienta tu revisión. Las preguntas todavía no califican competencias individuales. Se incluyen preguntas de opción única y verdadero/falso que pueden atribuirse a esta ruta.</p>
    </div>
    {data.units.length === 0 && <p>Esta ruta todavía no tiene paradas para resumir.</p>}
    {data.units.map((unit, index) => <details className="report-unit" key={unit.id} open={index === 0}>
      <summary><span>{unit.title}</span>{' '}<span className={unit.attentionCount > 0 ? 'report-attention' : 'report-note'}>{unit.attentionCount > 0 ? `${unit.attentionCount} ${unit.attentionCount === 1 ? 'estudiante' : 'estudiantes'} por acompañar` : 'Sin alertas de rendimiento'}</span></summary>
      <div className="report-unit-content">
        {unit.attentionCount > 0 && <div className="report-competencies">
          <h3>Competencias que conviene revisar</h3>
          {unit.competencies.length > 0 ? <ul>{unit.competencies.map(c => <li key={c.code}><strong>{c.code}</strong> · {c.name}</li>)}</ul> : <p>No hay competencias vinculadas válidas para esta unidad. Revisa su configuración curricular.</p>}
          <p className="report-note">Evidencia indirecta: resultados bajos en actividades de esta unidad. Confirma la dificultad con el estudiante antes de planificar el apoyo.</p>
        </div>}
        <p className="report-mobile-hint">Desliza la tabla hacia los lados para ver los aciertos y el seguimiento.</p>
        <div className="report-table-scroll" role="region" aria-label={`Resultados por estudiante: ${unit.title}`} tabIndex={0}>
          <table aria-describedby="report-criteria"><caption>Resultados por estudiante · {unit.title}</caption>
            <thead><tr><th scope="col">Estudiante</th><th scope="col">Paradas completas</th><th scope="col">Preguntas respondidas</th><th scope="col">Aciertos</th><th scope="col">Seguimiento</th></tr></thead>
            <tbody>{unit.students.map(row => <tr key={row.studentId}>
              <th scope="row">{row.studentName}{row.enrollmentState && row.enrollmentState !== 'ACTIVO' && <small>Inscripción: {row.enrollmentState.toLowerCase()}</small>}</th>
              <td>{row.completedNodes} de {row.totalNodes}</td><td>{row.totalQuestions === 0 ? 'Sin preguntas evaluables' : `${row.answered} de ${row.totalQuestions}`}</td>
              <td>{row.accuracy === null ? 'Sin datos' : `${row.correct}/${row.answered} (${row.accuracy.toLocaleString('es-PE')} %)`}</td>
              <td><span className={row.status === 'REQUIERE_ATENCION' ? 'report-attention' : ''}>{labels[row.status]}</span></td>
            </tr>)}</tbody>
          </table>
        </div>
        {unit.students.length === 0 && <p>No hay estudiantes activos en esta aula.</p>}
      </div>
    </details>)}
  </div>
}
function ClassroomReports({ room, view, onViewChange, initialRouteId }: { room: string; view: string; onViewChange: (view: string) => void; initialRouteId?: string }) {
  const { data: routes, error, retry } = useReportData<ReportRoute[]>(`/classrooms/${encodeURIComponent(room)}/routes`)
  const [selected, setSelected] = useState(initialRouteId || '')
  const [selectedStudent, setSelectedStudent] = useState('')
  const { data: members, error: membersError, retry: retryMembers } = useReportData<ReportMember[]>(`/classrooms/${encodeURIComponent(room)}/students`)
  if (membersError) return <ReportError message={membersError} retry={retryMembers} />
  if (!members) return <p role="status">Buscando los estudiantes del aula…</p>
  const student = members.some(m => m.id === selectedStudent) ? selectedStudent : ''
  if (error) return <ReportError message={error} retry={retry} />
  if (!routes) return <p role="status">Buscando las rutas de tus estudiantes…</p>
  if (!routes.length) return <p className="report-empty">Todavía no hay inscripciones en rutas del grado de esta aula. El reporte aparecerá cuando los estudiantes se inscriban.</p>
  const route = routes.some(r => r.id === selected) ? selected : (initialRouteId && routes.some(r => r.id === initialRouteId) ? initialRouteId : routes[0].id)
  return <><label className="report-filter">Ruta y versión<select value={route} onChange={event => setSelected(event.target.value)}>
    {routes.map(r => <option key={r.id} value={r.id}>{r.title} · v{r.version} · {r.state === 'PUBLICADO' ? 'Publicada' : 'No publicada'}</option>)}
  </select></label><StudentFilter members={members} selected={student} onChange={setSelectedStudent} />
    {student && <StudentOverviewView key={`${route}:${student}`} room={room} route={route} student={student} onViewChange={onViewChange} />}
    <TeacherAssignments key={`refuerzo:${room}:${route}:${student}`} room={room} route={route} student={student} />
    <div id="report-detail" tabIndex={-1}>{view === 'grades' ? <GradesReportView key={`${route}:${student}`} room={room} route={route} student={student} /> : <ReportResults key={`${route}:${student}`} room={room} route={route} student={student} />}</div></>
}

export function TeacherReportsView({ onBack, onLogout, initialRouteId, initialGrade }: { onBack: () => void; onLogout: () => void; initialRouteId?: string; initialGrade?: number }) {
  const [view, setView] = useState('results')
  const { data: rooms, error, retry } = useReportData<ReportClassroom[]>('/classrooms')
  const [selected, setSelected] = useState('')
  // Al llegar desde un roadmap se abre el aula de su grado: la primera aula podía ser de otro grado y no tener esa ruta.
  const room = rooms?.some(r => r.id === selected) ? selected : (rooms?.find(r => r.grade === initialGrade) ?? rooms?.[0])?.id
  return <main className="teacher-reports">
    <header className="report-header"><div><span className="report-eyebrow"><BookOpen size={20} aria-hidden="true" /> Cuaderno docente · Rupi</span><h1>Reportes de resultados</h1><p>Observa el avance de tus estudiantes y encuentra dónde acompañarlos.</p></div>
      <nav aria-label="Navegación docente"><button type="button" onClick={onBack}><ArrowLeft size={20} aria-hidden="true" /> Volver a mis rutas</button><button type="button" onClick={onLogout}><LogOut size={20} aria-hidden="true" /> Salir</button></nav></header>
    <div className="report-actions" aria-label="Tipo de reporte"><button type="button" aria-pressed={view === 'results'} onClick={() => setView('results')}>Resultados y atención</button><button type="button" aria-pressed={view === 'grades'} onClick={() => setView('grades')}>Calificaciones promedio</button></div>
    {error && <ReportError message={error} retry={retry} />}
    {!rooms && !error && <p role="status">Buscando tus aulas…</p>}
    {rooms && !rooms.length && <p className="report-empty">Aún no tienes aulas activas asignadas. Solicita que te registren como docente o responsable de tu aula para consultar sus resultados.</p>}
    {rooms && room && <><label className="report-filter">Aula<select value={room} onChange={event => setSelected(event.target.value)}>
      {rooms.map(r => <option key={r.id} value={r.id}>{r.name} · {r.grade}.º de primaria</option>)}
    </select></label><ClassroomReports key={room} room={room} view={view} onViewChange={setView} initialRouteId={initialRouteId} /></>}
  </main>
}
