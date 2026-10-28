import React, { useEffect, useRef, useState } from 'react'
import { Square, Volume2 } from 'lucide-react'
import { isSpeechSupported, speakText, stopSpeaking } from '../utils/speech'
import './ListenButton.css'

interface Props {
  /** Texto que se lee en voz alta. */
  text: string
  /** Qué se va a escuchar, para el lector de pantalla: "la pregunta", "la explicación". */
  subject: string
  compact?: boolean
}

/** Botón para escuchar consignas y explicaciones con la voz del navegador (HU-39). */
export const ListenButton: React.FC<Props> = ({ text, subject, compact = false }) => {
  const [speaking, setSpeaking] = useState(false)
  const speakingRef = useRef(false)
  const update = (value: boolean) => {
    speakingRef.current = value
    setSpeaking(value)
  }

  // Solo al desmontar: si este botón seguía leyendo, se detiene. Otro botón que empieza a leer
  // ya cancela la lectura anterior, que termina por su cuenta con onEnd.
  useEffect(() => () => { if (speakingRef.current) stopSpeaking() }, [])

  if (!isSpeechSupported() || !text.trim()) return null

  const toggle = () => {
    if (speaking) {
      stopSpeaking()
      update(false)
      return
    }
    update(true)
    speakText(text, { onEnd: () => update(false) })
  }

  return (
    <button
      type="button"
      className={`listen-btn ${compact ? 'is-compact' : ''} ${speaking ? 'is-speaking' : ''}`}
      onClick={toggle}
      aria-pressed={speaking}
      aria-label={speaking ? `Detener la lectura de ${subject}` : `Escuchar ${subject}`}
    >
      {speaking ? <Square size={16} aria-hidden="true" /> : <Volume2 size={18} aria-hidden="true" />}
      {!compact && <span>{speaking ? 'Detener' : 'Escuchar'}</span>}
    </button>
  )
}
