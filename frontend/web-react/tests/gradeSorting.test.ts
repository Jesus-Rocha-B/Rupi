import { test } from 'node:test'
import assert from 'node:assert/strict'
import { sortGrades } from '../src/features/reports/gradeSorting.ts'
import type { GradeRow } from '../src/services/gradesTypes.ts'
const row = (id: string, name: string, value: number | null): GradeRow => ({ studentId: id, studentName: name,
  cells: [{ unitId: 'u', totalActivities: 1, details: [], average: value === null ? null : { value, sortValue: value, letter: 'C' } }] })
test('sorts Spanish names in either direction without changing source', () => {
  const rows = [row('1', 'Zoe', 0), row('2', 'Ángela', null)]
  assert.deepEqual(sortGrades(rows, 'name', true).map(r => r.studentName), ['Ángela', 'Zoe'])
  assert.deepEqual(sortGrades(rows, 'name', false).map(r => r.studentName), ['Zoe', 'Ángela'])
  assert.equal(rows[0].studentName, 'Zoe')
})
test('missing grades remain last in both directions and zero is a real grade', () => {
  const rows = [row('n', 'Sin nota', null), row('a', 'Ana', 0), row('b', 'Bea', 14)]
  assert.deepEqual(sortGrades(rows, 'u', true).map(r => r.studentId), ['a', 'b', 'n'])
  assert.deepEqual(sortGrades(rows, 'u', false).map(r => r.studentId), ['b', 'a', 'n'])
})
test('orders by unrounded values and ties by name', () => {
  const rows = [row('z', 'Zoe', 13.99), row('a', 'Ana', 14), row('b', 'Bea', 14)]
  rows.forEach(r => { if (r.cells[0].average) r.cells[0].average.value = 14 })
  assert.deepEqual(sortGrades(rows, 'u', true).map(r => r.studentId), ['z', 'a', 'b'])
})
