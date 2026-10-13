import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  downloadableUnits,
  fileNameFromDisposition,
  LOOSE_UNIT_KEY,
  sha256Hex,
  unitPackagePath,
} from '../src/features/roadmap/unitDownloads.ts'
import type { RouteDetail, RouteUnit } from '../src/services/learningRoutesApi.ts'

const unit = (id: string | null, code: string | null, title: string, nodeIds: string[]): RouteUnit => ({
  id, code, title, sequence: null, competencies: [], nodeIds,
})
const route = (units: RouteUnit[] | null): RouteDetail => ({
  versionRouteId: 'ruta-1', title: 'Ruta', grade: { number: 3, name: '3.°' }, area: { code: 'MAT', name: 'Matemática' },
  enrollment: { id: 'e', state: 'ACTIVO', lastVisitedNodeId: null },
  progress: { completedNodes: 0, totalNodes: 3 }, nodes: [],
  curriculum: units ? { status: 'PARCIAL', message: null, units } : null,
})

test('lista las unidades descargables en orden y usa «otras» para las paradas sueltas', () => {
  const units = downloadableUnits(route([
    unit('u1', 'MAT-3-U1', 'Números en la feria', ['a', 'b']),
    unit(null, 'OTRAS', 'Otras paradas', ['c']),
  ]))
  assert.deepEqual(units, [
    { key: 'u1', title: 'Números en la feria', code: 'MAT-3-U1', stops: 2 },
    { key: LOOSE_UNIT_KEY, title: 'Otras paradas', code: null, stops: 1 },
  ])
})

test('sin unidades o con unidades vacías no muestra nada para descargar', () => {
  assert.deepEqual(downloadableUnits(route(null)), [])
  assert.deepEqual(downloadableUnits(route([unit('u1', 'U1', 'Vacía', [])])), [])
})

test('arma la ruta de la API escapando los identificadores', () => {
  assert.equal(unitPackagePath('ruta 1', 'u/1'), 'student/learning-routes/ruta%201/units/u%2F1/package')
})

test('toma el nombre del archivo de Content-Disposition o arma uno seguro', () => {
  assert.equal(fileNameFromDisposition('attachment; filename="rupi-mat-u1.zip"', 'x'), 'rupi-mat-u1.zip')
  assert.equal(fileNameFromDisposition("attachment; filename*=UTF-8''rupi-ni%C3%B1os.zip", 'x'), 'rupi-niños.zip')
  assert.equal(fileNameFromDisposition(null, 'rupi-Matemática en Ayacucho-Números'), 'rupi-matematica-en-ayacucho-numeros.zip')
})

test('calcula el SHA-256 del archivo recibido para comprobar que llegó completo', async () => {
  const data = new TextEncoder().encode('abc').buffer as ArrayBuffer
  assert.equal(await sha256Hex(data), 'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad')
})
