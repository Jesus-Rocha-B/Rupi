import test from 'node:test'
import assert from 'node:assert/strict'
import { questionSpeech, speechText, speakText } from '../src/utils/speech.ts'

test('quita la negrilla del tutor y los símbolos decorativos antes de leer', () => {
  assert.equal(speechText('**En la vida real:** cuentas   ★ mandarinas · en el mercado'), 'En la vida real: cuentas mandarinas en el mercado')
})

test('lee la consigna con sus alternativas en orden', () => {
  assert.equal(
    questionSpeech('¿Cuánto es 7 + 7?', ['12', '14', '16']),
    '¿Cuánto es 7 + 7? Alternativa a: 12. Alternativa b: 14. Alternativa c: 16.',
  )
  assert.equal(questionSpeech('  Lee el texto. '), 'Lee el texto.')
})

test('sin síntesis de voz disponible no falla', () => {
  assert.doesNotThrow(() => speakText('Hola Rupi'))
})
