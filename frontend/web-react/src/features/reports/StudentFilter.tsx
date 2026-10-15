import { useState } from 'react'
import type { ReportMember } from '../../services/reportsApi'
import { filterStudents, studentLabel } from './studentFiltering'
export function StudentFilter({ members, selected, onChange }: { members: ReportMember[]; selected: string; onChange: (id: string) => void }) {
  const [search, setSearch] = useState('')
  const options = filterStudents(members, search, selected)
  return <fieldset className="student-filter"><legend>Seguimiento del estudiante</legend>
    <label className="report-filter">Buscar estudiante<input type="search" value={search} onChange={e => setSearch(e.target.value)} placeholder="Nombre o usuario escolar" autoComplete="off" /></label>
    <label className="report-filter">Estudiante<select value={selected} onChange={e => onChange(e.target.value)}>
      <option value="">Todos los estudiantes</option>{options.map(m => <option key={m.id} value={m.id}>{studentLabel(m, members)}</option>)}
    </select></label>
    <p className="report-note">La búsqueda acorta la lista; el reporte cambia al seleccionar un estudiante. Los nombres repetidos se distinguen por su usuario escolar.</p>
    {options.length === 0 && <p role="status">No hay estudiantes que coincidan con la búsqueda.</p>}
    {(selected || search) && <button type="button" onClick={() => { setSearch(''); onChange('') }}>Ver todos los estudiantes</button>}
  </fieldset>
}
