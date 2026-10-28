import test from 'node:test'
import assert from 'node:assert/strict'
import { comparisonBars } from '../src/features/dashboard/scoreComparison.ts'
import type { ScoreComparison } from '../src/services/dailyGoalApi.ts'

function comparison(todayExperience: number, yesterdayExperience: number): ScoreComparison {
  const difference = todayExperience - yesterdayExperience
  return {
    today: '2026-10-28',
    yesterday: '2026-10-27',
    todayExperience,
    yesterdayExperience,
    difference,
    trend: difference > 0 ? 'SUBE' : difference < 0 ? 'BAJA' : 'IGUAL',
    todayLessons: 2,
    yesterdayLessons: 1,
    message: 'Mensaje de Rupi',
  }
}

test('la barra del mayor puntaje ocupa todo el ancho y la otra es proporcional', () => {
  const bars = comparisonBars(comparison(80, 40))
  assert.equal(bars.todayWidth, 100)
  assert.equal(bars.yesterdayWidth, 50)
  assert.equal(bars.label, 'Vas mejor que ayer')
})

test('un puntaje pequeño sigue siendo visible y un cero queda vacío', () => {
  const bars = comparisonBars(comparison(1, 100))
  assert.equal(bars.todayWidth, 6)
  assert.equal(bars.yesterdayWidth, 100)
  assert.equal(comparisonBars(comparison(0, 30)).todayWidth, 0)
})

test('sin actividad en ambos días no divide entre cero', () => {
  const bars = comparisonBars(comparison(0, 0))
  assert.equal(bars.todayWidth, 0)
  assert.equal(bars.yesterdayWidth, 0)
  assert.equal(bars.label, 'Vas igual que ayer')
})

test('el texto para escuchar incluye ambos puntajes y el mensaje', () => {
  const bars = comparisonBars(comparison(30, 50))
  assert.match(bars.spoken, /Hoy sumaste 30 XP y ayer 50 XP/)
  assert.match(bars.spoken, /Mensaje de Rupi/)
})
