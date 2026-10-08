<<<<<<< HEAD
import type { ActivityContent } from '../../services/learningRoutesApi'
=======
import { useState } from 'react'
import {
  ArrowRight,
  Award,
  CheckCircle2,
  Sparkles,
  Volume2,
  X,
} from 'lucide-react'
import {
  completeActivity,
  type ActivityCompleteResult,
  type ActivityContent,
} from '../../services/learningRoutesApi'
import { RupiCharacter } from '../../components/RupiCharacter'
import { speakText } from '../../utils/speech'
>>>>>>> 656170f (Se implementó el flujo completo de inicio y finalización de lecciones escolares con otorgamiento idempotente de 50 puntos de experiencia y desbloqueo automático de la siguiente parada para cumplir con HU-02 y HU-05, además se configuró el desplazamiento suave y centrado accesible del mapa en el último nodo visitado junto con el acceso directo desde el botón de bienvenida para cumplir con HU-03, también se corrigieron las discrepancias de columnas de catálogo en el repositorio de Spring Boot para enlazar las unidades y competencias curriculares oficiales del MINEDU con sus respectivas bandas visuales en el roadmap para cumplir con HU-04, asimismo se diseñó el modal didáctico de actividades con estados de carga interactivos, contenido explicativo de respaldo y pantalla de celebración con Rupi festejando y audio de felicitación, y finalmente se agregaron las pruebas unitarias en JUnit, el script de integración para PowerShell y la documentación técnica detallada de las cuatro historias en la carpeta de implementación)

function safeResource(value: string | null) {
  if (!value) return null
  try {
    const url = new URL(value, window.location.origin)
    return ['http:', 'https:'].includes(url.protocol) ? url.href : null
<<<<<<< HEAD
  } catch { return null }
}
export function ActivityBlocks({ activity }: { activity: ActivityContent }) {
  return <section className="activity-content" aria-label="Contenido de la actividad">
    {activity.instructions && <p>{activity.instructions}</p>}
    {activity.blocks.map(block => {
      const url = safeResource(block.url)
      return <div key={block.id} className={block.type === 'DESTACADO' ? 'activity-highlight' : 'activity-block'}>
        {block.text && <p>{block.text}</p>}
        {url && block.type === 'IMAGEN' && <img src={url} alt={block.accessibleText ?? ''} />}
        {url && block.type === 'AUDIO' && <audio controls src={url} aria-label={block.accessibleText ?? 'Audio de la actividad'} />}
        {url && block.type === 'VIDEO' && <video controls src={url} aria-label={block.accessibleText ?? 'Video de la actividad'} />}
        {url && block.type === 'ADJUNTO' && <a href={url} target="_blank" rel="noreferrer">{block.accessibleText ?? 'Abrir material'}</a>}
        {!url && !block.text && <p>Este material aún no está disponible. Consulta con tu docente.</p>}
      </div>
    })}
    <p className="activity-saved">Tu visita quedó guardada. Puedes volver al mapa y continuar después.</p>
  </section>
=======
  } catch {
    return null
  }
}

type Props = {
  activity: ActivityContent
  routeId: string
  onComplete?: (result: ActivityCompleteResult) => void
  onClose: () => void
}

export function ActivityBlocks({ activity, routeId, onComplete, onClose }: Props) {
  const [busy, setBusy] = useState(false)
  const [completionResult, setCompletionResult] = useState<ActivityCompleteResult | null>(null)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  const isCompleted = activity.state === 'COMPLETADO' || completionResult !== null

  async function handleFinish() {
    if (busy) return
    setBusy(true)
    setErrorMsg(null)
    try {
      const res = await completeActivity(routeId, activity.nodeId)
      setCompletionResult(res)
      speakText(res.rupiMessage)
      if (onComplete) {
        onComplete(res)
      }
    } catch {
      setErrorMsg('No pudimos guardar tu avance. Revisa tu conexión y vuelve a intentar.')
    } finally {
      setBusy(false)
    }
  }

  // Vista de celebración cuando se completa la actividad
  if (completionResult) {
    return (
      <div className="activity-celebration-card" role="region" aria-label="Celebración de parada completada">
        <button
          className="modal-close"
          type="button"
          onClick={onClose}
          aria-label="Volver al mapa"
        >
          <X aria-hidden="true" />
        </button>

        <div className="celebration-mascot" aria-hidden="true">
          <RupiCharacter mood="cheering" className="celebration-rupi-svg" />
        </div>

        <div className="celebration-xp-pill">
          <Sparkles size={18} aria-hidden="true" />
          <span>+{completionResult.experienceEarned} XP</span>
        </div>

        <h3 className="celebration-title">¡Misión Cumplida!</h3>
        <p className="celebration-message">{completionResult.rupiMessage}</p>

        {completionResult.nextUnlockedNode && (
          <div className="next-unlocked-card">
            <span className="next-unlocked-tag">SIGUIENTE PARADA DESBLOQUEADA</span>
            <b className="next-unlocked-name">
              Parada #{completionResult.nextUnlockedNode.sequence}: {completionResult.nextUnlockedNode.title}
            </b>
          </div>
        )}

        <div className="celebration-actions">
          <button
            type="button"
            className="celebration-continue-btn"
            onClick={onClose}
            autoFocus
          >
            <span>Continuar mi aventura</span>
            <ArrowRight size={18} aria-hidden="true" />
          </button>

          <button
            type="button"
            className="celebration-speech-btn"
            onClick={() => speakText(completionResult.rupiMessage)}
            aria-label="Escuchar felicitación"
            title="Escuchar felicitación"
          >
            <Volume2 size={18} aria-hidden="true" />
            <span>Escuchar</span>
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="activity-modal-wrapper">
      <header className="activity-modal-header">
        <div className="activity-modal-title-col">
          <span className="activity-badge-pill">
            <Award size={14} aria-hidden="true" />
            <span>ACTIVIDAD ESCOLAR</span>
          </span>
          <h2 id="modal-title">{activity.title}</h2>
        </div>
        <div className="activity-header-actions">
          <button
            type="button"
            className="modal-listen-btn"
            onClick={() => speakText(`${activity.title}. ${activity.instructions || ''}`)}
            aria-label="Escuchar actividad"
            title="Escuchar instrucciones"
          >
            <Volume2 size={18} aria-hidden="true" />
          </button>
          <button
            className="modal-close"
            type="button"
            onClick={onClose}
            aria-label="Cerrar lección"
          >
            <X aria-hidden="true" />
          </button>
        </div>
      </header>

      {activity.instructions && (
        <div className="activity-instructions-box">
          <p>{activity.instructions}</p>
        </div>
      )}

      <div className="activity-blocks-list" aria-label="Contenido didáctico">
        {activity.blocks.length > 0 ? (
          activity.blocks.map(block => {
            const url = safeResource(block.url)
            return (
              <div
                key={block.id}
                className={`activity-block-item ${block.type === 'DESTACADO' ? 'highlight-box' : 'standard-box'}`}
              >
                {block.text && <p className="block-text">{block.text}</p>}
                {url && (
                  <div className="block-media-container">
                    <img
                      src={url}
                      alt={block.accessibleText || 'Material didáctico'}
                      className="block-media-img"
                      loading="lazy"
                    />
                  </div>
                )}
              </div>
            )
          })
        ) : (
          <div className="activity-block-item highlight-box">
            <p className="block-text">
              ¡Hola, explorador! En esta parada escolar descubriremos conceptos clave de tu aventura. Lee con atención cada paso, practica con tu cuaderno y completa la misión para sumar tu experiencia con Rupi.
            </p>
          </div>
        )}
      </div>

      {errorMsg && (
        <div className="activity-error-alert" role="alert">
          <p>{errorMsg}</p>
        </div>
      )}

      <footer className="activity-modal-footer">
        {isCompleted ? (
          <button
            type="button"
            className="activity-finish-btn already-done"
            onClick={onClose}
          >
            <CheckCircle2 size={18} aria-hidden="true" />
            <span>Parada completada · Volver al mapa</span>
          </button>
        ) : (
          <button
            type="button"
            className="activity-finish-btn"
            disabled={busy}
            onClick={handleFinish}
          >
            <Sparkles size={18} aria-hidden="true" />
            <span>{busy ? 'Guardando tu avance…' : '¡Terminé esta lección! (+50 XP)'}</span>
            <ArrowRight size={18} aria-hidden="true" />
          </button>
        )}
      </footer>
    </div>
  )
>>>>>>> 656170f (Se implementó el flujo completo de inicio y finalización de lecciones escolares con otorgamiento idempotente de 50 puntos de experiencia y desbloqueo automático de la siguiente parada para cumplir con HU-02 y HU-05, además se configuró el desplazamiento suave y centrado accesible del mapa en el último nodo visitado junto con el acceso directo desde el botón de bienvenida para cumplir con HU-03, también se corrigieron las discrepancias de columnas de catálogo en el repositorio de Spring Boot para enlazar las unidades y competencias curriculares oficiales del MINEDU con sus respectivas bandas visuales en el roadmap para cumplir con HU-04, asimismo se diseñó el modal didáctico de actividades con estados de carga interactivos, contenido explicativo de respaldo y pantalla de celebración con Rupi festejando y audio de felicitación, y finalmente se agregaron las pruebas unitarias en JUnit, el script de integración para PowerShell y la documentación técnica detallada de las cuatro historias en la carpeta de implementación)
}
