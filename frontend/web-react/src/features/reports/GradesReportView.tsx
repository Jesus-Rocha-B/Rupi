import { useEffect, useRef, useState } from 'react'
import { reportQuery } from '../../services/reportsApi'
import type { GradeCell, GradesReport } from '../../services/gradesTypes'
import { useReportData } from './useReportData'
import { sortGrades } from './gradeSorting'
import './GradesReportView.css'

const number = (value: number) => value.toLocaleString('es-PE', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
const date = (value: string) => new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Lima' }).format(new Date(value))
const labels = { AD: 'Logro destacado', A: 'Logro esperado', B: 'En proceso', C: 'En inicio' }
type Selection = { student: string; unit: string; cell: GradeCell }
function GradeValue({ cell }: { cell: GradeCell }) {
  return cell.average ? <><span className={`grade-letter grade-${cell.average.letter}`}>{cell.average.letter}</span><span>{number(cell.average.value)} / 20</span></> : <span>— <small>Sin calificaciones</small></span>
}
function GradeDetailDialog({ selection, onClose }: { selection: Selection; onClose: () => void }) {
  const ref = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const previous = document.activeElement
    const dialog = ref.current
    dialog?.showModal()
    return () => { dialog?.close(); if (previous instanceof HTMLElement) previous.focus() }
  }, [])
  return <dialog ref={ref} className="grade-dialog" aria-labelledby="grade-detail-title" onCancel={onClose}>
    <div className="grade-dialog-heading"><h2 id="grade-detail-title">Detalle del promedio</h2><button type="button" onClick={onClose} autoFocus>Cerrar</button></div>
    <p><strong>{selection.student}</strong> · {selection.unit}</p>
    <div className="grade-value"><GradeValue cell={selection.cell} /></div>
    <p>{selection.cell.details.length} de {selection.cell.totalActivities} actividades con puntaje completo válido.</p>
    <ol className="grade-detail-list">{selection.cell.details.map(detail => <li key={detail.activityId}>
      <h3>{detail.title}</h3><p>{detail.earned.toLocaleString('es-PE')} de {detail.possible.toLocaleString('es-PE')} puntos → <strong>{number(detail.grade)} / 20</strong></p>
      <p>Última respuesta: {date(detail.date)} (Perú).</p>
    </li>)}</ol>
    <p>Se usa el último intento completo válido por actividad. Cada actividad pesa lo mismo; sus preguntas pesan según sus puntos. El promedio se calcula antes de redondear.</p>
    <p>La letra es orientativa de Rupi. Los valores mostrados se redondean a un decimal; la letra utiliza el valor sin redondear.</p>
  </dialog>
}
function GradesTable({ data }: { data: GradesReport }) {
  const [sortKey, setSortKey] = useState('name')
  const [ascending, setAscending] = useState(true)
  const [selection, setSelection] = useState<Selection | null>(null)
  const rows = sortGrades(data.students, sortKey, ascending)
  function sort(key: string) { if (key === sortKey) setAscending(!ascending); else { setSortKey(key); setAscending(true) } }
  const ariaSort = (key: string) => key === sortKey ? ascending ? 'ascending' as const : 'descending' as const : 'none' as const
  const content = (cell: GradeCell, student: string, unit: string) => cell.average
    ? <button className="grade-cell-button" type="button" onClick={() => setSelection({ cell, student, unit })} aria-label={`${student}, ${unit}: ${cell.average.letter}, ${number(cell.average.value)} sobre 20. Ver detalle`}>
        <GradeValue cell={cell} /><small>{cell.details.length}/{cell.totalActivities} actividades · Ver detalle</small></button>
    : <GradeValue cell={cell} />
  if (!data.units.length) return <p className="report-empty">Esta ruta todavía no tiene unidades curriculares válidas vinculadas a sus paradas.</p>
  if (!data.students.length) return <p className="report-empty">No hay estudiantes activos en esta aula.</p>
  return <>
    <div className="grade-sort"><label className="report-filter">Ordenar por<select value={sortKey} onChange={e => setSortKey(e.target.value)}><option value="name">Nombre del estudiante</option>{data.units.map(unit => <option key={unit.id} value={unit.id}>{unit.title}</option>)}</select></label>
      <button type="button" onClick={() => setAscending(!ascending)}>{ascending ? 'Ascendente ↑' : 'Descendente ↓'}</button></div>
    <p role="status" className="report-note">{rows.length} {rows.length === 1 ? 'estudiante' : 'estudiantes'} · Los registros sin calificaciones aparecen al final al ordenar por promedio.</p>
    <div className="report-table-scroll grade-table" role="region" aria-label="Calificaciones por estudiante y unidad" tabIndex={0}>
      <table aria-describedby="grade-criteria"><caption>Promedios por unidad · {data.classroom.name} · {data.route.title}</caption>
        <thead><tr><th scope="col" aria-sort={ariaSort('name')}><button type="button" onClick={() => sort('name')}>Estudiante {sortKey === 'name' ? ascending ? '↑' : '↓' : '↕'}</button></th>
          {data.units.map(unit => <th key={unit.id} scope="col" aria-sort={ariaSort(unit.id)}><button type="button" onClick={() => sort(unit.id)}>{unit.title} {sortKey === unit.id ? ascending ? '↑' : '↓' : '↕'}</button></th>)}</tr></thead>
        <tbody>{rows.map(row => <tr key={row.studentId}><th scope="row">{row.studentName}</th>{row.cells.map(cell => <td key={cell.unitId}>{content(cell, row.studentName, data.units.find(unit => unit.id === cell.unitId)!.title)}</td>)}</tr>)}</tbody>
      </table>
    </div>
    <div className="grade-cards">{rows.map(row => <article key={row.studentId} className="report-unit"><h3>{row.studentName}</h3><details><summary>Ver unidades y calificaciones</summary><dl>{row.cells.map(cell => <div key={cell.unitId}><dt>{data.units.find(unit => unit.id === cell.unitId)!.title}</dt><dd>{content(cell, row.studentName, data.units.find(unit => unit.id === cell.unitId)!.title)}</dd></div>)}</dl></details></article>)}</div>
    {selection && <GradeDetailDialog selection={selection} onClose={() => setSelection(null)} />}
  </>
}
export function GradesReportView({ room, route }: { room: string; route: string }) {
  const { data, error, retry } = useReportData<GradesReport>('/grades' + reportQuery(room, route))
  if (error) return <div className="report-error" role="alert"><p>{error}</p><button type="button" onClick={retry}>Volver a consultar</button></div>
  if (!data) return <p role="status">Preparando las calificaciones del aula…</p>
  return <section className="grades-report" aria-label="Calificaciones promedio">
    <div className="report-summary"><h2>Calificaciones promedio</h2><p>{data.classroom.name} · {data.route.title} · v{data.route.version}</p><p className="report-note">Consultado: {date(data.generatedAt)} (Perú)</p><button type="button" onClick={retry}>Actualizar calificaciones</button></div>
    <div className="report-criteria" id="grade-criteria"><strong>Puntaje sobre 20 y letra orientativa de Rupi</strong>
      <p>AD: 18–20 · A: desde 14 hasta menos de 18 · B: desde 11 hasta menos de 14 · C: desde 0 hasta menos de 11.</p>
      <p>{Object.entries(labels).map(([letter, label]) => `${letter}: ${label}`).join(' · ')}.</p>
      <p>Esta equivalencia interna ayuda al seguimiento de actividades; no sustituye la evaluación docente de competencias ni una calificación oficial del MINEDU.</p>
      <details><summary>Cómo calculamos el promedio</summary><p>Nota de actividad = puntos obtenidos ÷ puntos posibles × 20. Cada actividad pesa lo mismo en la unidad y se cuenta una sola vez, aunque aparezca en varias paradas.</p>
        <p>Se toma el último intento con todas sus preguntas puntuables corregidas y puntajes válidos. Un intento se considera completo cuando todas esas preguntas tienen una respuesta calificada. Solo se admiten actividades con preguntas puntuables de opción única o verdadero/falso.</p>
        <p>Se excluyen intentos abandonados, parciales, sin calificar, con puntajes inválidos o que no puedan atribuirse a la ruta. Las actividades sin evidencia no se convierten en cero. El resultado es parcial mientras falten actividades por calificar.</p>
        <p>Se redondea solo la presentación a un decimal. La letra y el orden usan el promedio sin redondear; cerca de un límite, el número mostrado puede parecer del siguiente nivel.</p></details>
    </div>
    {data.unassignedNodes > 0 && <p className="report-attention">{data.unassignedNodes} paradas sin unidad curricular válida quedan fuera del promedio. Revisa su vinculación.</p>}
    <GradesTable data={data} />
  </section>
}
