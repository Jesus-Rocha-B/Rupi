import { useState, useRef, useEffect } from 'react'
import { X, Send, Sparkles, Volume2, Loader2 } from 'lucide-react'
import { askRupiTutor, type ChatMessage } from '../services/aiApi'
import { RupiCharacter } from './RupiCharacter'
import { speakText } from '../utils/speech'
import './RupiTutorChat.css'

interface Props {
  roadmapTitle?: string
  stopTitle?: string
  lessonContent?: string
}

export function RupiTutorChat({ roadmapTitle, stopTitle, lessonContent }: Props) {
  const [isOpen, setIsOpen] = useState(false)
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      role: 'assistant',
      content: `¡Hola! Soy Rupi, tu tutor de esta ruta. 🦜✨ ¿Tienes alguna duda sobre "${stopTitle || roadmapTitle || 'esta lección'}"? Pregúntame con confianza y te daré pistas.`,
    },
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [hasUnread, setHasUnread] = useState(true)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    if (isOpen) {
      scrollToBottom()
    }
  }, [isOpen, messages])

  const handleSend = async (textToSend?: string) => {
    const text = (textToSend || input).trim()
    if (!text || loading) return

    const newMessages: ChatMessage[] = [...messages, { role: 'user', content: text }]
    setMessages(newMessages)
    setInput('')
    setLoading(true)

    try {
      const reply = await askRupiTutor(
        text,
        {
          roadmapTitle: roadmapTitle || 'Ruta Escolar',
          stopTitle: stopTitle || 'Parada Activa',
          lessonContent: lessonContent || '',
        },
        newMessages
      )

      setMessages((prev) => [...prev, { role: 'assistant', content: reply }])
    } catch {
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: '¡Uy! Tuve un pequeño tropiezo de conexión con las montañas. Inténtalo de nuevo en un ratito, ¡no te desanimes! 🧗‍♂️',
        },
      ])
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      handleSend()
    }
  }

  const quickQuestions = [
    '¿De qué trata esta parada?',
    'Dame una pista para resolverla',
    '¿Por qué es importante aprender esto?',
  ]

  return (
    <div className="rupi-tutor-container">
      {/* Botón flotante inferior derecho con la cara de Rupi */}
      {!isOpen && (
        <button
          type="button"
          className="rupi-tutor-bubble-btn"
          onClick={() => { setIsOpen(true); setHasUnread(false); }}
          aria-label="Abrir tutor de ayuda Rupi"
          title="¿Dudas con la lección? ¡Pregúntale a Rupi!"
        >
          <div className="rupi-bubble-avatar">
            <RupiCharacter mood="idle" />
          </div>
          <div className="rupi-bubble-label">
            <span>¿Dudas?</span>
            <strong>Pregúntale a Rupi</strong>
          </div>
          {hasUnread && <span className="rupi-unread-dot" aria-hidden="true" />}
        </button>
      )}

      {/* Ventana de chat desplegable */}
      {isOpen && (
        <div className="rupi-chat-card" role="dialog" aria-label="Tutor Virtual Rupi">
          {/* Cabecera cálida */}
          <div className="rupi-chat-header">
            <div className="rupi-chat-header-info">
              <div className="rupi-header-icon">
                <RupiCharacter mood="cheering" />
              </div>
              <div>
                <div className="rupi-header-title">
                  <strong>Rupi Tutor</strong>
                  <span className="rupi-pill-grounding">Basado en tu lección</span>
                </div>
                <p className="rupi-header-sub">
                  {stopTitle ? `Parada: ${stopTitle}` : roadmapTitle || 'Acompañante escolar'}
                </p>
              </div>
            </div>
            <button
              type="button"
              className="rupi-close-btn"
              onClick={() => setIsOpen(false)}
              aria-label="Cerrar chat"
            >
              <X size={20} />
            </button>
          </div>

          {/* Lista de mensajes */}
          <div className="rupi-chat-messages">
            {messages.map((m, idx) => (
              <div key={idx} className={`rupi-message-row ${m.role}`}>
                {m.role === 'assistant' && (
                  <div className="rupi-msg-avatar" aria-hidden="true">
                    <RupiCharacter mood="idle" />
                  </div>
                )}
                <div className="rupi-message-bubble">
                  <p className="rupi-message-text">{m.content}</p>
                  {m.role === 'assistant' && (
                    <button
                      type="button"
                      className="rupi-speak-btn"
                      onClick={() => speakText(m.content)}
                      title="Escuchar explicación de Rupi"
                      aria-label="Escuchar mensaje en voz alta"
                    >
                      <Volume2 size={15} />
                      <span>Escuchar</span>
                    </button>
                  )}
                </div>
              </div>
            ))}

            {loading && (
              <div className="rupi-message-row assistant">
                <div className="rupi-msg-avatar">
                  <RupiCharacter mood="thinking" />
                </div>
                <div className="rupi-message-bubble loading">
                  <Loader2 size={16} className="rupi-spin" />
                  <span>Rupi está pensando una buena pista...</span>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Sugerencias de preguntas rápidas */}
          <div className="rupi-quick-chips">
            {quickQuestions.map((q, idx) => (
              <button
                key={idx}
                type="button"
                className="rupi-chip-btn"
                onClick={() => handleSend(q)}
                disabled={loading}
              >
                <Sparkles size={13} className="rupi-chip-icon" />
                <span>{q}</span>
              </button>
            ))}
          </div>

          {/* Formulario de entrada */}
          <div className="rupi-chat-input-bar">
            <input
              type="text"
              className="rupi-input"
              placeholder="Hazle una pregunta a Rupi..."
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              disabled={loading}
              aria-label="Escribe tu pregunta para Rupi"
            />
            <button
              type="button"
              className="rupi-send-btn"
              onClick={() => handleSend()}
              disabled={!input.trim() || loading}
              aria-label="Enviar mensaje"
            >
              <Send size={18} />
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
