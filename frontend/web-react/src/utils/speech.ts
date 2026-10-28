/** Limpia el texto antes de leerlo: símbolos decorativos y la negrilla Markdown del tutor (**palabra**). */
export function speechText(text: string): string {
  return text
    .replace(/\*\*/g, '')
    .replace(/[✦★·#•]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

/** Une consigna y alternativas en una sola lectura: "Pregunta. Alternativa a: ... Alternativa b: ..." (HU-39). */
export function questionSpeech(statement: string, options: string[] = []): string {
  const letters = 'abcdefghij'
  const choices = options.map((option, index) => `Alternativa ${letters[index] ?? index + 1}: ${option}.`)
  return [statement.trim(), ...choices].join(' ')
}

interface SpeakOptions {
  onEnd?: () => void
}

export function speakText(text: string, options: SpeakOptions = {}): void {
  if (typeof window === 'undefined' || !('speechSynthesis' in window)) {
    return
  }

  try {
    window.speechSynthesis.cancel()
    const cleanText = speechText(text)
    if (!cleanText) {
      options.onEnd?.()
      return
    }
    const utterance = new SpeechSynthesisUtterance(cleanText)

    // Configurar voz en español (preferencia es-PE o fallback es)
    utterance.lang = 'es-PE'
    utterance.rate = 0.92 // Ritmo pausado y claro para niños de primaria
    utterance.pitch = 1.08 // Tono cálido y amigable

    const voices = window.speechSynthesis.getVoices()
    const spanishVoice = voices.find(v => v.lang.startsWith('es-PE')) ||
                         voices.find(v => v.lang.startsWith('es-419')) ||
                         voices.find(v => v.lang.startsWith('es'))
    if (spanishVoice) {
      utterance.voice = spanishVoice
    }
    if (options.onEnd) {
      utterance.onend = options.onEnd
      utterance.onerror = options.onEnd
    }

    window.speechSynthesis.speak(utterance)
  } catch (error) {
    console.warn('No se pudo reproducir audio con Web Speech API:', error)
    options.onEnd?.()
  }
}

export function stopSpeaking(): void {
  if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
    window.speechSynthesis.cancel()
  }
}

export function isSpeechSupported(): boolean {
  return typeof window !== 'undefined' && 'speechSynthesis' in window
}
