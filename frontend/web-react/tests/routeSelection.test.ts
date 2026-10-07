import { test } from 'node:test'
import assert from 'node:assert/strict'
import { matchingRoutes } from '../src/features/roadmap/routeSelection.ts'
import type { RouteSummary } from '../src/services/learningRoutesApi.ts'
const route = (id: string, grade: number, area: string): RouteSummary => ({
  versionRouteId: id, grade: { number: grade, name: `${grade} primaria` }, area: { code: area, name: area },
  title: id, enrollmentState: 'ACTIVO', completedNodes: 0, totalNodes: 3, lastVisitedAt: null,
})
const routes = [route('math2', 2, 'MAT'), route('com2', 2, 'COM'), route('math3', 3, 'MAT')]
test('cada curso y grado conduce a su propia ruta', () => {
  assert.deepEqual(matchingRoutes(routes, 2, 'MATEMATICA').map(r => r.versionRouteId), ['math2'])
  assert.deepEqual(matchingRoutes(routes, 2, 'COMUNICACION').map(r => r.versionRouteId), ['com2'])
  assert.deepEqual(matchingRoutes(routes, 3, 'MATEMATICA').map(r => r.versionRouteId), ['math3'])
})
test('no sustituye cursos ausentes por el primer roadmap', () => {
  assert.deepEqual(matchingRoutes(routes, 2, 'CYT'), [])
  assert.deepEqual(matchingRoutes(routes, 6, 'MAT'), [])
  assert.deepEqual(matchingRoutes([], 2, 'MAT'), [])
})
