import test from 'node:test'
import assert from 'node:assert/strict'

test('calcula el strokeDashoffset del anillo circular sin desbordar el perímetro', () => {
  const RADIUS = 54
  const CIRCUMFERENCE = 2 * Math.PI * RADIUS // ~339.292

  function computeOffset(completed: number, target: number) {
    const fraction = target > 0 ? completed / target : 0
    const clamped = Math.min(1, Math.max(0, fraction))
    return CIRCUMFERENCE * (1 - clamped)
  }

  // 0% completado -> offset total (anillo vacío)
  assert.equal(computeOffset(0, 3), CIRCUMFERENCE)

  // 50% completado -> offset medio
  const halfOffset = computeOffset(1, 2)
  assert.equal(Math.round(halfOffset), Math.round(CIRCUMFERENCE / 2))

  // 100% completado -> offset 0 (anillo completo)
  assert.equal(computeOffset(3, 3), 0)

  // >100% (superó la meta) -> fijado en 0 sin desbordar
  assert.equal(computeOffset(5, 3), 0)
})

test('genera segmentos de la barra táctil según la meta seleccionada', () => {
  function getSegments(completed: number, target: number) {
    return Array.from({ length: target }).map((_, idx) => ({
      index: idx,
      isFilled: idx < completed,
    }))
  }

  const segments = getSegments(2, 3)
  assert.equal(segments.length, 3)
  assert.equal(segments[0].isFilled, true)
  assert.equal(segments[1].isFilled, true)
  assert.equal(segments[2].isFilled, false)

  const segmentsFull = getSegments(4, 3)
  assert.equal(segmentsFull.every(s => s.isFilled), true)
})

test('selecciona el mensaje motivador adecuado según el avance diario', () => {
  function getMotivationalMessage(completed: number, target: number): string {
    const isGoalMet = completed >= target
    const pending = Math.max(0, target - completed)
    if (isGoalMet) {
      return '¡Felicitaciones! Has alcanzado tu meta diaria escolar. ¡Eres imparable! 🎉'
    }
    if (completed === 0) {
      return '¡Inicia el día con tu primera parada escolar para avanzar en tu meta!'
    }
    if (pending === 1) {
      return '¡Solo te falta 1 lección para alcanzar tu meta de hoy! ¡Tú puedes!'
    }
    return `Llevas ${completed} de ${target} lecciones hoy. ¡Sigue avanzando paso a paso!`
  }

  assert.match(getMotivationalMessage(0, 3), /primera parada escolar/)
  assert.match(getMotivationalMessage(2, 3), /Solo te falta 1 lección/)
  assert.match(getMotivationalMessage(1, 3), /Llevas 1 de 3/)
  assert.match(getMotivationalMessage(3, 3), /Felicitaciones.*imparable/)
  assert.match(getMotivationalMessage(4, 3), /Felicitaciones.*imparable/)
})

test('valida la estructura de la tira de 7 días del historial semanal', () => {
  const daysOfWeek = ['LUN', 'MAR', 'MIÉ', 'JUE', 'VIE', 'SÁB', 'DOM']
  const mockWeek = [
    { day: 'MIÉ', completed: 0, target: 3, met: false, isToday: false },
    { day: 'JUE', completed: 0, target: 3, met: false, isToday: false },
    { day: 'VIE', completed: 0, target: 3, met: false, isToday: false },
    { day: 'SÁB', completed: 3, target: 3, met: true, isToday: false },
    { day: 'DOM', completed: 4, target: 3, met: true, isToday: false },
    { day: 'LUN', completed: 3, target: 3, met: true, isToday: false },
    { day: 'MAR', completed: 1, target: 3, met: false, isToday: true },
  ]

  assert.equal(mockWeek.length, 7)
  assert.equal(mockWeek[6].isToday, true)
  assert.equal(mockWeek.filter(d => d.met).length, 3)
  for (const d of mockWeek) {
    assert.equal(daysOfWeek.includes(d.day), true)
  }
})
