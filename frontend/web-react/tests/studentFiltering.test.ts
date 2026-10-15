import { test } from 'node:test'
import assert from 'node:assert/strict'
import { filterStudents, studentLabel } from '../src/features/reports/studentFiltering.ts'
const members = [{ id: '1', name: 'José Quispe', schoolUsername: 'jose01' }, { id: '2', name: 'José Quispe', schoolUsername: 'jose02' }, { id: '3', name: 'Ana', schoolUsername: 'ana01' }]
test('search ignores accents/case and can locate the school username', () => {
  assert.equal(filterStudents(members, ' JOSE ', '').length, 2)
  assert.deepEqual(filterStudents(members, 'jose02', '').map(s => s.id), ['2'])
})
test('duplicate names are distinguished without emails or identifiers', () => {
  assert.equal(studentLabel(members[0], members), 'José Quispe · jose01')
  assert.equal(studentLabel(members[1], members), 'José Quispe · jose02')
  assert.equal(studentLabel(members[2], members), 'Ana')
})
test('search preserves current selection and never modifies the roster', () => {
  assert.deepEqual(filterStudents(members, 'nothing', '1').map(s => s.id), ['1'])
  assert.equal(filterStudents(members, 'nothing', '').length, 0)
  assert.equal(members.length, 3)
})
