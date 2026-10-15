import type { GradeRow } from '../../services/gradesTypes'
export function sortGrades(rows: GradeRow[], key: string, ascending: boolean): GradeRow[] {
  const direction = ascending ? 1 : -1
  return [...rows].sort((a, b) => {
    const names = a.studentName.localeCompare(b.studentName, 'es', { sensitivity: 'base', numeric: true })
      || a.studentId.localeCompare(b.studentId)
    if (key === 'name') return direction * names
    const x = a.cells.find(cell => cell.unitId === key)?.average?.sortValue
    const y = b.cells.find(cell => cell.unitId === key)?.average?.sortValue
    if (x === undefined && y === undefined) return names
    if (x === undefined) return 1
    if (y === undefined) return -1
    return direction * (x - y) || names
  })
}
