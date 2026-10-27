import React, { useState, useRef, useEffect } from 'react'
import { X, Send, Sparkles, Volume2, Loader2, RotateCcw, FileText } from 'lucide-react'
import { askRupiTutor, type ChatMessage } from '../services/aiApi'
import { RupiCharacter } from './RupiCharacter'
import { speakText } from '../utils/speech'
import './RupiTutorChat.css'

interface Props {
  roadmapId?: string
  roadmapTitle?: string
  stopTitle?: string
  lessonContent?: string
  sources?: string[]
}

/**
 * Parsea y formatea texto markdown seguro a componentes React:
 * - **palabra** o __palabra__ -> <strong>
 * - *palabra* -> <strong> (requisito explícito del usuario para negrilla)
 * - `código` -> <code>
 */
function renderInlineText(text: string): React.ReactNode[] {
  const regex = /(\*\*[^*]+\*\*|\*[^*\n]+\*|__[^_]+__|`[^`]+`)/g
  const parts = text.split(regex)

  return parts.map((part, idx) => {
    if (!part) return null

    // Negrita doble asterisco **palabra**
    if (part.startsWith('**') && part.endsWith('**') && part.length > 4) {
      return (
        <strong key={idx} className="rupi-chat-strong">
          {part.slice(2, -2)}
        </strong>
      )
    }

    // Negrita guión bajo __palabra__
    if (part.startsWith('__') && part.endsWith('__') && part.length > 4) {
      return (
        <strong key={idx} className="rupi-chat-strong">
          {part.slice(2, -2)}
        </strong>
      )
    }

    // Negrita/énfasis un asterisco *palabra*
    if (part.startsWith('*') && part.endsWith('*') && part.length > 2) {
      return (
        <strong key={idx} className="rupi-chat-strong">
          {part.slice(1, -1)}
        </strong>
      )
    }

    // Código en línea `codigo`
    if (part.startsWith('`') && part.endsWith('`') && part.length > 2) {
      return (
        <code key={idx} className="rupi-chat-code">
          {part.slice(1, -1)}
        </code>
      )
    }

    return <span key={idx}>{part}</span>
  })
}

/**
 * Renderiza el mensaje formateando párrafos, listas con viñetas y palabras resaltadas.
 */
function renderFormattedMessage(content: string): React.ReactNode {
  const lines = content.split('\n')
  const blocks: React.ReactNode[] = []
  let currentList: { type: 'ul' | 'ol'; items: string[] } | null = null

  const flushList = () => {
    if (currentList) {
      if (currentList.type === 'ul') {
        blocks.push(
          <ul key={`ul-${blocks.length}`} className="rupi-chat-ul">
            {currentList.items.map((item, i) => (
              <li key={i}>{renderInlineText(item)}</li>
            ))}
          </ul>
        )
      } else {
        blocks.push(
          <ol key={`ol-${blocks.length}`} className="rupi-chat-ol">
            {currentList.items.map((item, i) => (
              <li key={i}>{renderInlineText(item)}</li>
            ))}
          </ol>
        )
      }
      currentList = null
    }
  }

  for (let i = 0; i < lines.length; i++) {
    const rawLine = lines[i]
    const trimmed = rawLine.trim()

    if (!trimmed) {
      flushList()
      continue
    }

    // Elemento de lista con viñeta: "- item" o "* item"
    const bulletMatch = trimmed.match(/^[-*•]\s+(.+)$/)
    if (bulletMatch) {
      if (!currentList || currentList.type !== 'ul') {
        flushList()
        currentList = { type: 'ul', items: [] }
      }
      currentList.items.push(bulletMatch[1])
      continue
    }

    // Elemento de lista numerada: "1. item"
    const numMatch = trimmed.match(/^\d+[.)]\s+(.+)$/)
    if (numMatch) {
      if (!currentList || currentList.type !== 'ol') {
        flushList()
        currentList = { type: 'ol', items: [] }
      }
      currentList.items.push(numMatch[1])
      continue
    }

    // Párrafo normal
    flushList()
    blocks.push(
      <p key={`p-${blocks.length}`} className="rupi-chat-paragraph">
        {renderInlineText(trimmed)}
      </p>
    )
  }

  flushList()
  return <div className="rupi-formatted-content">{blocks}</div>
}

function loadHistoryFromStorage(key: string, fallbackGreeting: ChatMessage): ChatMessage[] {
  try {
    const saved = localStorage.getItem(key)
    if (saved) {
      const parsed = JSON.parse(saved)
      if (Array.isArray(parsed) && parsed.length > 0) {
        return parsed
      }
    }
  } catch {
    // Si falla JSON.parse, se retorna el saludo inicial
  }
  return [fallbackGreeting]
}

export function RupiTutorChat({ roadmapId, roadmapTitle, stopTitle, lessonContent, sources }: Props) {
  const [isOpen, setIsOpen] = useState(false)
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [hasUnread, setHasUnread] = useState(true)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  // Clave única de almacenamiento persistente por roadmap (Estilo NotebookLM)
  const cleanId = roadmapId || (roadmapTitle ? roadmapTitle.toLowerCase().replace(/[^a-z0-9]+/g, '_') : 'general')
  const storageKey = `rupi_tutor_history_${cleanId}`

  const defaultGreeting: ChatMessage = {
    role: 'assistant',
    content: `¡Hola! Soy Rupi, tu tutor de esta ruta escolar. 🦜✨ ¿Tienes alguna duda sobre "${stopTitle || roadmapTitle || 'esta lección'}"? Pregúntame con confianza y te responderé guiándome en los materiales de tu ruta.`,
  }

  const [prevKey, setPrevKey] = useState(storageKey)
  const [messages, setMessages] = useState<ChatMessage[]>(() =>
    loadHistoryFromStorage(storageKey, defaultGreeting)
  )

  // Actualización limpia cuando el componente cambia de roadmap
  if (prevKey !== storageKey) {
    setPrevKey(storageKey)
    setMessages(loadHistoryFromStorage(storageKey, defaultGreeting))
  }

  // Guardar mensajes automáticamente en localStorage al recibir o enviar
  useEffect(() => {
    if (messages.length > 1) {
      try {
        localStorage.setItem(storageKey, JSON.stringify(messages))
      } catch (err) {
        console.warn('No se pudo guardar el historial del tutor Rupi:', err)
      }
    }
  }, [messages, storageKey])

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

  const handleResetChat = () => {
    try {
      localStorage.removeItem(storageKey)
    } catch {
      // Ignorar error al limpiar
    }
    setMessages([
      {
        role: 'assistant',
        content: `¡Conversación reiniciada! 🦜🌱 Soy Rupi, tu tutor de esta ruta. ¿Qué duda o curiosidad tienes sobre "${stopTitle || roadmapTitle || 'esta lección'}"?`,
      },
    ])
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
          onClick={() => {
            setIsOpen(true)
            setHasUnread(false)
          }}
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
        <div className="rupi-chat-card" role="dialog" aria-label="Tutor Virtual Rupi (Estilo NotebookLM)">
          {/* Cabecera cálida */}
          <div className="rupi-chat-header">
            <div className="rupi-chat-header-info">
              <div className="rupi-header-icon">
                <RupiCharacter mood="cheering" />
              </div>
              <div>
                <div className="rupi-header-title">
                  <strong>Rupi Tutor</strong>
                  <span className="rupi-pill-grounding">NotebookLM Rupi</span>
                </div>
                <p className="rupi-header-sub">
                  {stopTitle ? `Parada: ${stopTitle}` : roadmapTitle || 'Acompañante escolar'}
                </p>
                {sources && sources.length > 0 && (
                  <div className="rupi-header-sources" title={`Fuentes: ${sources.join(', ')}`}>
                    <FileText size={11} aria-hidden="true" />
                    <span>{sources.length} fuente(s) conectada(s)</span>
                  </div>
                )}
              </div>
            </div>

            <div className="rupi-header-actions">
              <button
                type="button"
                className="rupi-reset-chat-btn"
                onClick={handleResetChat}
                title="Reiniciar conversación de este roadmap (Nuevo chat)"
                aria-label="Nuevo chat"
              >
                <RotateCcw size={13} aria-hidden="true" />
                <span>Nuevo chat</span>
              </button>
              <button
                type="button"
                className="rupi-close-btn"
                onClick={() => setIsOpen(false)}
                aria-label="Cerrar chat"
              >
                <X size={20} />
              </button>
            </div>
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
                  {renderFormattedMessage(m.content)}
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
