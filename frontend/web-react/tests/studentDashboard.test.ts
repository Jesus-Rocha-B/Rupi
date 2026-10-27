import test from 'node:test'
import assert from 'node:assert/strict'
import type { DashboardSummaryResponse } from '../src/services/dashboardApi'

test('clasifica accesorios equipados en sus capas visuales correspondientes', () => {
  function parseEquippedAccessories(codes: string[]) {
    const acc: { sombrero: string | null; vestimenta: string | null; fondo: string | null } = {
      sombrero: null,
      vestimenta: null,
      fondo: null,
    }
    for (const c of codes) {
      if (c.includes('CHULLO') || c.includes('BIRRETE') || c.includes('GORRO')) {
        acc.sombrero = c
      } else if (c.includes('CHALECO') || c.includes('PONCHO') || c.includes('CAPA')) {
        acc.vestimenta = c
      } else if (c.includes('FONDO')) {
        acc.fondo = c
      }
    }
    return acc
  }

  const equipped = parseEquippedAccessories(['CHULLO_ANDINO', 'CHALECO_ESCOLAR', 'FONDO_ESTRELLAS'])
  assert.equal(equipped.sombrero, 'CHULLO_ANDINO')
  assert.equal(equipped.vestimenta, 'CHALECO_ESCOLAR')
  assert.equal(equipped.fondo, 'FONDO_ESTRELLAS')
})

test('calcula métricas agregadas exactas a partir de las materias inscritas', () => {
  const subjects = [
    { completed: 6, pending: 0, total: 6 },
    { completed: 4, pending: 6, total: 10 },
    { completed: 2, pending: 4, total: 6 },
  ]

  let totalCompleted = 0
  let totalPending = 0
  for (const s of subjects) {
    totalCompleted += s.completed
    totalPending += s.pending
  }
  const totalEnrolled = totalCompleted + totalPending
  const percentage = totalEnrolled > 0 ? Math.round((totalCompleted * 100) / totalEnrolled) : 0

  assert.equal(totalCompleted, 12)
  assert.equal(totalPending, 10)
  assert.equal(totalEnrolled, 22)
  assert.equal(percentage, 55)
})

test('reconoce coincidencia de ETag con o sin prefijo débil W/', () => {
  function isETagMatch(ifNoneMatch: string | null, dataHash: string): boolean {
    if (!ifNoneMatch) return false
    const clean = ifNoneMatch.trim()
    const quoted = `"${dataHash}"`
    const weakQuoted = `W/"${dataHash}"`
    return clean === quoted || clean === weakQuoted || clean === dataHash
  }

  const hash = '14e98eb01e3ebed0'
  assert.equal(isETagMatch('"14e98eb01e3ebed0"', hash), true)
  assert.equal(isETagMatch('W/"14e98eb01e3ebed0"', hash), true)
  assert.equal(isETagMatch('14e98eb01e3ebed0', hash), true)
  assert.equal(isETagMatch('"otherhash123"', hash), false)
  assert.equal(isETagMatch(null, hash), false)
})

test('valida la estructura completa de un resumen del dashboard escolar', () => {
  const mockDashboard: DashboardSummaryResponse = {
    student: {
      studentId: '11111111-1111-4111-8111-111111111111',
      studentName: 'Mateo Quispe',
      schoolName: 'I.E. 38001 Mariscal Sucre',
      role: 'ESTUDIANTE',
      gradeNumber: 3,
      gradeName: '3.° de primaria',
      equippedAccessories: ['CHULLO_ANDINO'],
    },
    metrics: {
      totalCompletedLessons: 27,
      totalPendingLessons: 109,
      totalEnrolledLessons: 136,
      overallCompletionPercentage: 20,
      lessonsCompletedToday: 0,
      currentStreakDays: 4,
      totalXp: 1550,
      activeRoutesCount: 22,
    },
    subjects: [
      {
        versionRouteId: 'b0000000-0000-4000-8000-000000000101',
        routeTitle: 'Matemática en Ayacucho',
        areaCode: 'MAT',
        areaName: 'Matemática',
        gradeNumber: 3,
        completedLessons: 4,
        pendingLessons: 6,
        totalLessons: 10,
        completionPercentage: 40,
        lastVisitedAt: '2026-10-27T00:00:00Z',
      },
    ],
    nextLesson: {
      versionRouteId: 'b0000000-0000-4000-8000-000000000101',
      routeTitle: 'Matemática en Ayacucho',
      areaCode: 'MAT',
      areaName: 'Matemática',
      nodeId: 'ca000000-0000-4000-8000-000001010003',
      nodeTitle: '¿Arriba o abajo del puente?',
      sequence: 3,
      activityType: 'RETO',
      estimatedMinutes: 8,
      nodeState: 'EN_CURSO',
    },
    sync: {
      syncedAt: '2026-10-27T07:31:53Z',
      dataHash: '14e98eb01e3ebed0',
    },
  }

  assert.equal(mockDashboard.student.studentName, 'Mateo Quispe')
  assert.equal(mockDashboard.metrics.totalCompletedLessons, 27)
  assert.equal(mockDashboard.metrics.totalPendingLessons, 109)
  assert.equal(mockDashboard.subjects.length, 1)
  assert.equal(mockDashboard.nextLesson?.nodeTitle, '¿Arriba o abajo del puente?')
  assert.equal(mockDashboard.nextLesson?.nodeState, 'EN_CURSO')
})
