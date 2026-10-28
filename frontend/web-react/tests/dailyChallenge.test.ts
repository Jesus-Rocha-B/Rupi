import test from 'node:test'
import assert from 'node:assert/strict'
import { SKILL_LABEL, canFinish, challengeSpeech } from '../src/features/dashboard/dailyChallenge.ts'

test('nombra los dos tipos de reto diario', () => {
  assert.equal(SKILL_LABEL.RAZONAMIENTO_LOGICO, 'Razonamiento lógico')
  assert.equal(SKILL_LABEL.COMPRENSION_LECTORA, 'Comprensión lectora')
})

test('la lectura en voz alta incluye el texto solo en los retos de lectura', () => {
  const lectura = challengeSpeech({ title: 'Mateo y su cometa', description: 'Lee con calma.', readingText: 'Mateo vuela su cometa.' })
  assert.equal(lectura, 'Mateo y su cometa. Lee con calma. Lectura. Mateo vuela su cometa.')
  const logico = challengeSpeech({ title: 'Las alpacas', description: null, readingText: null })
  assert.equal(logico, 'Las alpacas.')
})

test('solo se puede terminar con todas las preguntas respondidas', () => {
  assert.equal(canFinish(['a', 'b'], { a: true }), false)
  assert.equal(canFinish(['a', 'b'], { a: true, b: false }), true)
  assert.equal(canFinish([], {}), false)
})
