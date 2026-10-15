import type { ReportMember } from '../../services/reportsApi'
const normalized = (text: string) => text.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('es').trim()
export function studentLabel(student: ReportMember, members: ReportMember[]): string {
  return members.filter(m => normalized(m.name) === normalized(student.name)).length > 1
    ? `${student.name} · ${student.schoolUsername}` : student.name
}
export function filterStudents(members: ReportMember[], search: string, selected: string): ReportMember[] {
  const query = normalized(search)
  return members.filter(m => m.id === selected || normalized(m.name).includes(query) || normalized(m.schoolUsername).includes(query))
}
