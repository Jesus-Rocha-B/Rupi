import test from 'node:test'
import assert from 'node:assert/strict'

test('normaliza el código alfanumérico a mayúsculas y sin caracteres no permitidos', () => {
  function normalizeCode(raw: string): string {
    return raw.trim().toUpperCase().replace(/[^A-Z0-9_-]/g, '')
  }

  assert.equal(normalizeCode('  ayacucho2026  '), 'AYACUCHO2026')
  assert.equal(normalizeCode('wari-2026!'), 'WARI-2026')
  assert.equal(normalizeCode('palabras_2026$#'), 'PALABRAS_2026')
})

test('valida la longitud permitida del código (entre 3 y 30 caracteres)', () => {
  function isValidCodeLength(code: string): boolean {
    const trimmed = code.trim()
    return trimmed.length >= 3 && trimmed.length <= 30
  }

  assert.equal(isValidCodeLength(''), false)
  assert.equal(isValidCodeLength('ab'), false)
  assert.equal(isValidCodeLength('ABC'), true)
  assert.equal(isValidCodeLength('AYACUCHO2026'), true)
  assert.equal(isValidCodeLength('A'.repeat(30)), true)
  assert.equal(isValidCodeLength('A'.repeat(31)), false)
})

test('asigna correctamente el curso escolar a partir del código de área curricular devuelto', () => {
  function resolveCourseCode(areaCode: string): string {
    const upper = areaCode.toUpperCase()
    if (upper.includes('COM')) return 'COMUNICACION'
    if (upper.includes('CYT')) return 'CYT'
    if (upper.includes('MAT')) return 'MATEMATICA'
    return 'MATEMATICA'
  }

  assert.equal(resolveCourseCode('MAT'), 'MATEMATICA')
  assert.equal(resolveCourseCode('MAT-3P'), 'MATEMATICA')
  assert.equal(resolveCourseCode('COM'), 'COMUNICACION')
  assert.equal(resolveCourseCode('COM-2P'), 'COMUNICACION')
  assert.equal(resolveCourseCode('CYT'), 'CYT')
  assert.equal(resolveCourseCode('CYT-2P'), 'CYT')
})

test('extrae mensajes pedagógicos formativos de respuestas RFC-7807 del servidor', () => {
  function extractServerErrorMessage(data: Record<string, unknown>, fallbackStatus: number): string {
    if (data.message && typeof data.message === 'string') return data.message
    if (data.detail && typeof data.detail === 'string') return data.detail
    if (data.error && typeof data.error === 'string') return data.error
    if (data.title && typeof data.title === 'string') return data.title
    return `No se pudo consultar la información (${fallbackStatus})`
  }

  assert.equal(
    extractServerErrorMessage({ message: 'El código de clase no existe.' }, 404),
    'El código de clase no existe.'
  )
  assert.equal(
    extractServerErrorMessage({ detail: 'Este código de clase ya venció.' }, 400),
    'Este código de clase ya venció.'
  )
  assert.equal(
    extractServerErrorMessage({ title: 'Servicio no disponible' }, 503),
    'Servicio no disponible'
  )
  assert.equal(
    extractServerErrorMessage({}, 500),
    'No se pudo consultar la información (500)'
  )
})
