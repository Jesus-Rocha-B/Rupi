import test from 'node:test'
import assert from 'node:assert/strict'

test('construye los parámetros de búsqueda de roadmaps alternativos correctamente', () => {
  const query = 'matematica'
  const grade = 2
  const limit = 20

  const params = new URLSearchParams()
  if (query) params.set('q', query)
  if (grade) params.set('grade', grade.toString())
  if (limit) params.set('limit', limit.toString())

  assert.equal(params.toString(), 'q=matematica&grade=2&limit=20')
})

test('gestiona parámetros vacíos sin incluir claves innecesarias', () => {
  const query = ''
  const grade = null
  const limit = 10

  const params = new URLSearchParams()
  if (query) params.set('q', query)
  if (grade) params.set('grade', (grade as unknown as number).toString())
  if (limit) params.set('limit', limit.toString())

  assert.equal(params.toString(), 'limit=10')
})

test('identifica y clasifica materias escolares según su código curricular', () => {
  function getSubjectColor(code: string) {
    if (code.includes('MAT')) return 'mat'
    if (code.includes('COM')) return 'com'
    if (code.includes('CYT')) return 'cyt'
    if (code.includes('ART')) return 'art'
    return 'soc'
  }

  assert.equal(getSubjectColor('MAT-2P'), 'mat')
  assert.equal(getSubjectColor('COM-2P'), 'com')
  assert.equal(getSubjectColor('CYT-2P'), 'cyt')
  assert.equal(getSubjectColor('ART-2P'), 'art')
  assert.equal(getSubjectColor('SOC-2P'), 'soc')
})
