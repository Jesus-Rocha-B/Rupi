import test from 'node:test'
import assert from 'node:assert/strict'

/**
 * Función pura que emula el tokenizer de formato en línea de Rupi Tutor
 */
function parseInlineTokens(text: string) {
  const regex = /(\*\*[^*]+\*\*|\*[^*\n]+\*|__[^_]+__|`[^`]+`)/g
  const parts = text.split(regex)
  return parts.filter(Boolean).map((part) => {
    if (part.startsWith('**') && part.endsWith('**') && part.length > 4) {
      return { type: 'strong', text: part.slice(2, -2) }
    }
    if (part.startsWith('__') && part.endsWith('__') && part.length > 4) {
      return { type: 'strong', text: part.slice(2, -2) }
    }
    if (part.startsWith('*') && part.endsWith('*') && part.length > 2) {
      return { type: 'strong', text: part.slice(1, -1) }
    }
    if (part.startsWith('`') && part.endsWith('`') && part.length > 2) {
      return { type: 'code', text: part.slice(1, -1) }
    }
    return { type: 'text', text: part }
  })
}

function getChatStorageKey(roadmapId?: string, roadmapTitle?: string) {
  const cleanId = roadmapId || (roadmapTitle ? roadmapTitle.toLowerCase().replace(/[^a-z0-9]+/g, '_') : 'general')
  return `rupi_tutor_history_${cleanId}`
}

test('convierte palabras con asterisco simple o doble a negrilla (strong)', () => {
  const tokens = parseInlineTokens('Aprenderemos sobre las *Bienaventuranzas* y la **paz** en el Perú.')
  
  const strongTokens = tokens.filter((t) => t.type === 'strong')
  assert.equal(strongTokens.length, 2)
  assert.equal(strongTokens[0].text, 'Bienaventuranzas')
  assert.equal(strongTokens[1].text, 'paz')
})

test('identifica fragmentos de código inline', () => {
  const tokens = parseInlineTokens('Usa el comando `suma(5, 3)` para resolver.')
  const codeTokens = tokens.filter((t) => t.type === 'code')
  assert.equal(codeTokens.length, 1)
  assert.equal(codeTokens[0].text, 'suma(5, 3)')
})

test('genera una clave única y estable de historial persistente por cada roadmap (NotebookLM)', () => {
  const key1 = getChatStorageKey('version-uuid-1234')
  assert.equal(key1, 'rupi_tutor_history_version-uuid-1234')

  const key2 = getChatStorageKey(undefined, 'El Sermón de la Montaña')
  assert.equal(key2, 'rupi_tutor_history_el_serm_n_de_la_monta_a')

  const keyGeneral = getChatStorageKey(undefined, undefined)
  assert.equal(keyGeneral, 'rupi_tutor_history_general')
})
