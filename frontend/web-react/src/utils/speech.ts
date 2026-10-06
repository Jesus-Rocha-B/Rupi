export function speakText(text: string): void {
  if (typeof window === 'undefined' || !('speechSynthesis' in window)) {
    return
  }

  try {
    window.speechSynthesis.cancel()
    const cleanText = text.replace(/[✦★·#]/g, '').trim()
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

    window.speechSynthesis.speak(utterance)
  } catch (error) {
    console.warn('No se pudo reproducir audio con Web Speech API:', error)
  }
}

export function isSpeechSupported(): boolean {
  return typeof window !== 'undefined' && 'speechSynthesis' in window
}
