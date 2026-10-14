import test from 'node:test'
import assert from 'node:assert/strict'
import { limaInput, limaInstant, monthCells, occursOn, shiftMonth } from '../src/features/calendar/calendarDates.ts'
test('un horario de Lima conserva el día aunque UTC sea el siguiente', () => {
  assert.equal(limaInput('2026-10-16T02:30:00Z'), '2026-10-15T21:30')
  assert.equal(new Date(limaInstant('2026-10-15T21:30')).toISOString(), '2026-10-16T02:30:00.000Z')
})
test('el calendario comienza el lunes y contempla febrero bisiesto', () => {
  const cells = monthCells('2028-02'); assert.equal(cells[0], null); assert.equal(cells[1], '2028-02-01'); assert.equal(cells.at(-1), '2028-02-29')
})
test('un período ocupa los días inicial, intermedio y final en Perú', () => {
  assert.ok(occursOn('2026-10-31T23:00:00-05:00','2026-11-02T10:00:00-05:00','2026-11-01'))
  assert.ok(!occursOn('2026-10-31T23:00:00-05:00','2026-11-02T10:00:00-05:00','2026-11-03'))
})
test('navegar entre meses cambia de año correctamente', () => {
  assert.equal(shiftMonth('2026-12', 1), '2027-01'); assert.equal(shiftMonth('2026-01', -1), '2025-12')
})
