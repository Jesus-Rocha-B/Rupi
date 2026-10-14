import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import {
  AlertCircle,
  ArrowRight,
  CheckCircle2,
  GraduationCap,
  KeyRound,
  Loader2,
  Sparkles,
  Volume2,
  X,
} from 'lucide-react'
import { joinRouteByCode, type JoinByCodeResult } from '../services/learningRoutesApi'
import { speakText } from '../utils/speech'
import './JoinClassModal.css'

interface JoinClassModalProps {
  isOpen: boolean
  onClose: () => void
  onJoined: (result: JoinByCodeResult) => void
}

export function JoinClassModal({ isOpen, onClose, onJoined }: JoinClassModalProps) {
  const [code, setCode] = useState('')
  const [status, setStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle')
  const [errorMessage, setErrorMessage] = useState('')
  const [successResult, setSuccessResult] = useState<JoinByCodeResult | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const redirectTimerRef = useRef<number | null>(null)

  const handleClose = useCallback(() => {
    setCode('')
    setStatus('idle')
    setErrorMessage('')
    setSuccessResult(null)
    if (redirectTimerRef.current) {
      window.clearTimeout(redirectTimerRef.current)
      redirectTimerRef.current = null
    }
    onClose()
  }, [onClose])

  // Enfocar el input al abrir
  useEffect(() => {
    if (isOpen) {
      const timer = window.setTimeout(() => {
        inputRef.current?.focus()
      }, 100)
      return () => window.clearTimeout(timer)
    }
  }, [isOpen])

  // Limpiar timers al desmontar
  useEffect(() => {
    return () => {
      if (redirectTimerRef.current) {
        window.clearTimeout(redirectTimerRef.current)
        redirectTimerRef.current = null
      }
    }
  }, [])

  // Cerrar con Escape
  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape' && isOpen && status !== 'loading') {
        handleClose()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, status, handleClose])



  if (!isOpen) return null

  function handleVoiceInstructions() {
    speakText(
      'Escribe el código alfanumérico que tu profesor o profesora compartió en la pizarra para entrar exactamente a la ruta de tu salón de clases.'
    )
  }

  function handleVoiceError() {
    if (errorMessage) {
      speakText(errorMessage)
    }
  }

  function handleInputChange(val: string) {
    // Sanitizar en tiempo real: mayúsculas y caracteres alfanuméricos o guiones
    const clean = val.toUpperCase().replace(/[^A-Z0-9_-]/g, '')
    setCode(clean)
    if (status === 'error') {
      setStatus('idle')
      setErrorMessage('')
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const trimmed = code.trim()
    if (!trimmed || trimmed.length < 3) {
      const msg = 'El código de clase debe tener al menos 3 letras o números.'
      setStatus('error')
      setErrorMessage(msg)
      inputRef.current?.focus()
      return
    }

    setStatus('loading')
    setErrorMessage('')

    try {
      const result = await joinRouteByCode(trimmed)
      setSuccessResult(result)
      setStatus('success')

      // Redirección suave al mapa interactivo tras breve confirmación
      redirectTimerRef.current = window.setTimeout(() => {
        onJoined(result)
        handleClose()
      }, 1800)
    } catch (err: unknown) {
      setStatus('error')
      const msg =
        err instanceof Error
          ? err.message
          : 'No se pudo verificar el código de clase. Revisa tu conexión a internet.'
      setErrorMessage(msg)
      inputRef.current?.focus()
    }
  }

  function handleDirectGo() {
    if (redirectTimerRef.current) {
      window.clearTimeout(redirectTimerRef.current)
      redirectTimerRef.current = null
    }
    if (successResult) {
      onJoined(successResult)
    }
    handleClose()
  }

  return (
    <div
      className="join-class-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="join-class-title"
      aria-describedby="join-class-desc"
      onClick={(e) => {
        if (e.target === e.currentTarget && status !== 'loading') {
          handleClose()
        }
      }}
    >

      <div className="join-class-modal">
        {/* Cabecera del modal */}
        <div className="join-class-header">
          <div className="join-class-header-brand">
            <span className="join-class-icon-wrapper" aria-hidden="true">
              <KeyRound size={24} />
            </span>
            <div>
              <h2 id="join-class-title" className="join-class-title">
                Unirme con código de clase
              </h2>
              <p id="join-class-desc" className="join-class-subtitle">
                Ingresa a la ruta creada por tu docente para tu salón
              </p>
            </div>
          </div>

          <div className="join-class-header-actions">
            <button
              type="button"
              className="join-class-voice-btn"
              onClick={handleVoiceInstructions}
              title="Escuchar instrucciones con Rupi"
              aria-label="Escuchar instrucciones por voz con Rupi"
            >
              <Volume2 size={20} aria-hidden="true" />
            </button>
            <button
              type="button"
              className="join-class-close-btn"
              onClick={handleClose}
              disabled={status === 'loading'}
              title="Cerrar ventana"
              aria-label="Cerrar ventana de unirse a clase"
            >
              <X size={22} aria-hidden="true" />
            </button>

          </div>
        </div>

        {/* Contenido según estado */}
        {status === 'success' && successResult ? (
          <div className="join-class-success-view">
            <div className="join-class-success-badge" aria-hidden="true">
              <CheckCircle2 size={44} />
            </div>

            <h3 className="join-class-success-title">
              {successResult.alreadyEnrolled ? '¡Ya estás en esta clase!' : '¡Te uniste con éxito!'}
            </h3>

            <div className="join-class-route-card">
              <div className="join-class-route-meta">
                <span className="join-class-grade-pill">
                  <GraduationCap size={16} aria-hidden="true" />
                  {successResult.grade.name}
                </span>
                <span className="join-class-area-pill">{successResult.area.name}</span>
              </div>
              <p className="join-class-route-title">{successResult.routeTitle}</p>
              <p className="join-class-route-msg">{successResult.message}</p>
            </div>

            <p className="join-class-redirect-notice">
              <Sparkles size={16} aria-hidden="true" />
              Llevándote al mapa interactivo de tu salón...
            </p>

            <button
              type="button"
              className="join-class-action-btn primary full"
              onClick={handleDirectGo}
            >
              <span>¡Ir al mapa ahora!</span>
              <ArrowRight size={20} aria-hidden="true" />
            </button>
          </div>
        ) : (
          <form className="join-class-form" onSubmit={handleSubmit} noValidate>
            <div className="join-class-field">
              <label htmlFor="classroom-code-input" className="join-class-label">
                Código alfanumérico del salón:
              </label>

              <div className="join-class-input-wrapper">
                <input
                  ref={inputRef}
                  id="classroom-code-input"
                  type="text"
                  className={`join-class-input ${status === 'error' ? 'has-error' : ''}`}
                  placeholder="EJ. AYACUCHO2026"
                  maxLength={30}
                  autoComplete="off"
                  spellCheck="false"
                  value={code}
                  disabled={status === 'loading'}
                  onChange={(e) => handleInputChange(e.target.value)}
                  aria-invalid={status === 'error'}
                  aria-describedby={status === 'error' ? 'join-class-error-alert' : undefined}
                />
                {code && status !== 'loading' && (
                  <button
                    type="button"
                    className="join-class-clear-btn"
                    onClick={() => {
                      setCode('')
                      inputRef.current?.focus()
                    }}
                    aria-label="Borrar código escrito"
                  >
                    <X size={16} aria-hidden="true" />
                  </button>
                )}
              </div>

              <div className="join-class-hint">
                <span>Pide este código a tu docente en el aula o revisa la pizarra escolar.</span>
              </div>
            </div>

            {/* Notificación de Error Accesible */}
            {status === 'error' && errorMessage && (
              <div
                id="join-class-error-alert"
                className="join-class-alert error"
                role="alert"
                aria-live="assertive"
              >
                <AlertCircle size={22} className="join-class-alert-icon" aria-hidden="true" />
                <div className="join-class-alert-text">
                  <p className="join-class-alert-msg">{errorMessage}</p>
                </div>
                <button
                  type="button"
                  className="join-class-alert-voice-btn"
                  onClick={handleVoiceError}
                  title="Escuchar explicación del error"
                  aria-label="Escuchar explicación del error"
                >
                  <Volume2 size={18} aria-hidden="true" />
                </button>
              </div>
            )}

            {/* Códigos de ejemplo para orientación del alumno */}
            <div className="join-class-examples">
              <span className="join-class-examples-label">Códigos de prueba activos:</span>
              <div className="join-class-example-chips">
                <button
                  type="button"
                  className="join-class-chip"
                  onClick={() => handleInputChange('AYACUCHO2026')}
                  disabled={status === 'loading'}
                >
                  AYACUCHO2026
                </button>
                <button
                  type="button"
                  className="join-class-chip"
                  onClick={() => handleInputChange('WARI2026')}
                  disabled={status === 'loading'}
                >
                  WARI2026
                </button>
                <button
                  type="button"
                  className="join-class-chip"
                  onClick={() => handleInputChange('PALABRAS2026')}
                  disabled={status === 'loading'}
                >
                  PALABRAS2026
                </button>
              </div>
            </div>

            {/* Botones de acción */}
            <div className="join-class-actions">
              <button
                type="button"
                className="join-class-action-btn secondary"
                onClick={handleClose}
                disabled={status === 'loading'}
              >
                Cancelar
              </button>


              <button
                type="submit"
                className="join-class-action-btn primary"
                disabled={status === 'loading' || !code.trim()}
              >
                {status === 'loading' ? (
                  <>
                    <Loader2 size={20} className="join-class-spinner" aria-hidden="true" />
                    <span>Verificando...</span>
                  </>
                ) : (
                  <>
                    <span>Unirme a mi clase</span>
                    <ArrowRight size={20} aria-hidden="true" />
                  </>
                )}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
