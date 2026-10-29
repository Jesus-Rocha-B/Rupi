import test from 'node:test'
import assert from 'node:assert/strict'
import { sessionEndMessage } from '../src/utils/sessionEnd.ts'

test('explica que la cuenta se abrió en un celular', () => {
  const message = sessionEndMessage({ status: 401, sessionEnd: 'OTRO_DISPOSITIVO', sessionNewClient: 'MOVIL' })
  assert.match(message ?? '', /en un celular/)
})

test('explica que la cuenta se abrió en otra computadora', () => {
  const message = sessionEndMessage({ status: 401, sessionEnd: 'OTRO_DISPOSITIVO', sessionNewClient: 'WEB_USUARIO' })
  assert.match(message ?? '', /otra computadora o tableta/)
})

test('distingue una sesión cerrada de una vencida', () => {
  assert.match(sessionEndMessage({ status: 401, sessionEnd: 'CERRADA' }) ?? '', /se cerró/)
  assert.match(sessionEndMessage({ status: 401, sessionEnd: 'EXPIRADA' }) ?? '', /Pasó mucho tiempo/)
})

test('sin motivo o con otro estado no inventa un aviso', () => {
  assert.equal(sessionEndMessage({ status: 401 }), null)
  assert.equal(sessionEndMessage({ status: 500, sessionEnd: 'CERRADA' }), null)
  assert.equal(sessionEndMessage(new Error('red')), null)
  assert.equal(sessionEndMessage(null), null)
})
