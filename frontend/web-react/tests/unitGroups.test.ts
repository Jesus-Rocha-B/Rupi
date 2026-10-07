import { test } from 'node:test'
import assert from 'node:assert/strict'
import { unitBandsByNode } from '../src/features/roadmap/unitGroups.ts'
import type { RouteDetail, RouteNode, RouteUnit } from '../src/services/learningRoutesApi.ts'

const node = (id: string, sequence: number, state: RouteNode['state']): RouteNode => ({
  id, sequence, title: id, activityType: 'LECCION', estimatedMinutes: 8, state, optional: false, position: null,
})
const unit = (id: string | null, title: string, nodeIds: string[]): RouteUnit => ({
  id, code: id, title, sequence: null, competencies: [], nodeIds,
})
const route = (nodes: RouteNode[], units: RouteUnit[] | null): RouteDetail => ({
  versionRouteId: 'r', title: 'Ruta', grade: { number: 2, name: '2.°' }, area: { code: 'MAT', name: 'Matemática' },
  enrollment: { id: 'e', state: 'ACTIVO', lastVisitedNodeId: null },
  progress: { completedNodes: 0, totalNodes: nodes.length }, nodes,
  curriculum: units ? { status: 'COMPLETA', message: null, units } : null,
})
const nodes = [node('a', 1, 'COMPLETADO'), node('b', 2, 'EN_CURSO'), node('c', 3, 'BLOQUEADO')]

test('una banda por unidad, en la primera parada de cada tramo', () => {
  const bands = unitBandsByNode(route(nodes, [unit('u1', 'Uno', ['a', 'b']), unit('u2', 'Dos', ['c'])]))
  assert.deepEqual([...bands.keys()], ['a', 'c'])
  assert.equal(bands.get('a')?.unit.title, 'Uno')
})
test('el avance de la banda se deriva del estado de sus paradas', () => {
  const band = unitBandsByNode(route(nodes, [unit('u1', 'Uno', ['a', 'b']), unit('u2', 'Dos', ['c'])])).get('a')
  assert.deepEqual([band?.completed, band?.total], [1, 2])
})
test('sin agrupación válida el mapa no recibe bandas', () => {
  assert.equal(unitBandsByNode(route(nodes, null)).size, 0)
  assert.equal(unitBandsByNode(route(nodes, [])).size, 0)
})
