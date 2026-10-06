import { ArrowRight, Play, Star, Volume2 } from 'lucide-react'
import { RupiCharacter } from './RupiCharacter'
import { speakText } from '../utils/speech'
import type { RouteDetail, RouteNode } from '../services/learningRoutesApi'

type Props = {
  route: RouteDetail
  name: string
  onSelect?: (node: RouteNode) => void
}

export function RouteOverview({ route, name, onSelect }: Props) {
  const completed = route.progress.completedNodes
  const total = route.progress.totalNodes || 10
  const nextNode =
    route.nodes.find((node) => node.state === 'EN_CURSO') ??
    route.nodes.find((node) => node.state === 'DISPONIBLE')

  const progressPhrase =
    completed === 0
      ? `¡Todo listo para empezar tu aventura, ${name}!`
      : completed === total
      ? `¡Increíble, ${name}! ¡Completaste todas las paradas!`
      : `¡Ya llevas ${completed} de ${total} paradas logradas, ${name}! ¡Qué gran esfuerzo!`

  const nextActionTitle = nextNode
    ? nextNode.state === 'EN_CURSO'
      ? `¡Seguir con: ${nextNode.title}!`
      : `¡Empezar: ${nextNode.title}!`
    : '¡Ver mi mapa de paradas!'

  return (
    <section className="adventure-welcome" aria-label="Resumen de tu aventura">
      <div className="welcome-compact-grid">
        {/* Columna izquierda: Saludo, Rupi y CTA principal del siguiente paso */}
        <div className="welcome-main-col">
          <div className="welcome-header-mini">
            <span className="welcome-greeting">¡Hola, {name}!</span>
            <button
              type="button"
              className="listen-greeting-btn"
              onClick={() => speakText(`${progressPhrase}. Tu siguiente paso es ${nextActionTitle}`)}
              aria-label="Escuchar tu avance y siguiente paso"
              title="Escuchar"
            >
              <Volume2 size={16} aria-hidden="true" />
            </button>
          </div>

          <h2 className="welcome-title">
            Un paso a la vez, <span className="title-highlight">descubriendo el Perú</span>.
          </h2>

          <p className="welcome-phrase">{progressPhrase}</p>

          {/* Botón táctil principal: Tu siguiente paso visible de inmediato */}
          {nextNode && (
            <div className="hero-cta-wrapper">
              <button
                type="button"
                className="hero-next-button"
                onClick={() => onSelect?.(nextNode)}
                aria-label={`Continuar con: ${nextNode.title}`}
              >
                <span className="cta-icon-box">
                  <Play size={18} fill="currentColor" aria-hidden="true" />
                </span>
                <span className="cta-label-group">
                  <small className="cta-kicker">Tu siguiente paso</small>
                  <strong className="cta-title">{nextActionTitle}</strong>
                </span>
                <ArrowRight size={20} className="cta-arrow" aria-hidden="true" />
              </button>
            </div>
          )}
        </div>

        {/* Columna derecha: Progreso visual con estrellas ganadas y Rupi */}
        <div className="welcome-progress-col">
          <div className="stars-progress-card" role="region" aria-label="Estrellas ganadas">
            <div className="stars-card-top">
              <span className="stars-card-label">
                <Star size={16} fill="#ffd26a" stroke="#dca331" aria-hidden="true" />
                <b>Estrellas ganadas</b>
              </span>
              <span className="stars-count-badge" aria-label={`${completed} de ${total} estrellas`}>
                <b>{completed}</b>
                <span className="stars-count-sep">/</span>
                <span>{total}</span>
              </span>
            </div>

            {/* Fila de estrellas: solo estrellas ganadas en dorado y vacías para las pendientes */}
            <div className="stars-track" aria-hidden="true">
              {Array.from({ length: total }).map((_, index) => {
                const isEarned = index < completed
                return (
                  <span
                    key={index}
                    className={`star-seed ${isEarned ? 'earned' : 'pending'}`}
                    title={`Parada ${index + 1}`}
                  >
                    <Star
                      size={20}
                      fill={isEarned ? '#ffd26a' : 'none'}
                      stroke={isEarned ? '#dca331' : '#b6c7b2'}
                      strokeWidth={2}
                    />
                  </span>
                )
              })}
            </div>
          </div>

          <div className="mascot-badge" aria-hidden="true">
            <RupiCharacter mood="cheering" className="mascot-img-sm" />
            <span className="mascot-cheer">¡Vamos juntos!</span>
          </div>
        </div>
      </div>
    </section>
  )
}
