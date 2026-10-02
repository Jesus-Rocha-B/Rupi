import { useMemo } from 'react'
import { Check, MapPin, Sparkles, Star } from 'lucide-react'
import { RupiCharacter } from '../../components/RupiCharacter'
import { createTrailPath } from './lessons'
import type { RouteDetail, RouteNode } from '../../services/learningRoutesApi'

const STOP_CONTEXTS: Record<number, string> = {
  1: 'Arcos de piedra',
  2: 'Arte en miniatura',
  3: 'Puestos de la plaza',
  4: 'Feria de artesanía',
  5: 'Colores y simetría',
  6: 'Laderas y andenes',
  7: 'Antiguas construcciones',
  8: 'Torres de la plaza',
  9: 'Sendero de altura',
  10: 'Huertas del valle',
}

type Props = {
  route: RouteDetail
  guideMessage: string
  onGuideClick: () => void
  onSelectNode: (node: RouteNode) => void
}

function getNodeDisplayState(state: RouteNode['state']): { cssClass: string; label: string } {
  switch (state) {
    case 'COMPLETADO':
      return { cssClass: 'complete', label: 'completado' }
    case 'EN_CURSO':
      return { cssClass: 'active', label: 'en curso · jugar ahora' }
    case 'DISPONIBLE':
    case 'BLOQUEADO':
    default:
      return { cssClass: 'locked', label: 'bloqueado' }
  }
}

export function Roadmap({ route, guideMessage, onGuideClick, onSelectNode }: Props) {
  const points = useMemo(() => {
    return route.nodes
      .filter((n) => n.position !== null)
      .map((n) => ({ x: n.position!.x, y: n.position!.y }))
  }, [route.nodes])

  const trailPath = useMemo(() => createTrailPath(points), [points])

  return (
    <section className="map-panel" id="explorar" aria-labelledby="roadmap-title">
      <header className="unit-heading">
        <span className="unit-number">01</span>
        <div className="unit-copy">
          <span className="eyebrow">UN PASEO ENTRE PORTALES Y RETABLOS</span>
          <h3 id="roadmap-title">{route.title}</h3>
          <span className="unit-subtitle">
            {route.progress.totalNodes} paradas para descubrir y aprender ({route.area.name} · {route.grade.name})
          </span>
        </div>
        <span className="unit-status">
          {route.enrollment.state === 'ACTIVO' ? 'EN CURSO' : route.enrollment.state}
        </span>
      </header>

      <div className="map-prompt">
        <button className="map-guide" onClick={onGuideClick} aria-label="Pedir un consejo a Rupi">
          <span className="guide-portrait"><RupiCharacter /></span>
          <span className="guide-bubble">
            <b>Rupi <span>TU COMPAÑERO</span></b>
            <span className="guide-copy">{guideMessage}</span>
            <small>Toca para otro consejo</small>
          </span>
        </button>
        <span className="map-progress">
          {route.progress.completedNodes} <span>/</span> {route.progress.totalNodes} <small>niveles</small>
        </span>
      </div>

      <div className="map-area" aria-label={`Recorrido de ${route.progress.totalNodes} actividades`}>
        <svg className="path-line" viewBox="0 0 600 1000" preserveAspectRatio="none" aria-hidden="true">
          <path className="road-shadow" d={trailPath} />
          <path className="road-edge" d={trailPath} />
          <path className="road-surface" d={trailPath} />
          <path className="road-center" d={trailPath} />
        </svg>

        {route.nodes.map((node) => {
          const { cssClass, label: stateLabel } = getNodeDisplayState(node.state)
          const canOpen = cssClass !== 'locked'
          const posX = node.position?.x ?? 50
          const posY = node.position?.y ?? 10
          const labelSide = posX > 50 ? 'left' : 'right'
          const context = STOP_CONTEXTS[node.sequence] ?? 'Parada escolar'
          const activityBadge = `${node.activityType === 'RETO' ? 'Reto' : 'Lección'}${node.estimatedMinutes ? ` · ${node.estimatedMinutes} min` : ''}`

          return (
            <div
              className={`trail-stop ${cssClass}`}
              id={node.sequence === 3 ? 'retos' : undefined}
              key={node.id}
              style={{ left: `${posX}%`, top: `${posY}%` }}
            >
              <button
                className="lesson-node"
                type="button"
                onClick={() => canOpen && onSelectNode(node)}
                disabled={!canOpen}
                aria-label={`Nivel ${node.sequence}: ${node.title}, estado ${stateLabel}`}
              >
                <span className="node-number">
                  {node.state === 'COMPLETADO' ? (
                    <Check aria-hidden="true" />
                  ) : node.state === 'EN_CURSO' ? (
                    <Star aria-hidden="true" />
                  ) : (
                    node.sequence
                  )}
                </span>
              </button>
              <button
                className={`node-label ${labelSide} ${cssClass}`}
                type="button"
                onClick={() => canOpen && onSelectNode(node)}
                disabled={!canOpen}
                aria-label={`Abrir nivel ${node.sequence}: ${node.title} (${stateLabel})`}
              >
                <span className="stop-place">
                  <MapPin aria-hidden="true" />
                  {context}
                </span>
                <b>{node.title}</b>
                <span>{activityBadge}</span>
              </button>
            </div>
          )
        })}

        <Sparkles className="map-decoration sparkle sparkle-one" aria-hidden="true" />
        <Sparkles className="map-decoration sparkle sparkle-two" aria-hidden="true" />
      </div>

      <div className="map-legend" id="logros">
        <span><i className="legend-dot done" />Completado</span>
        <span><i className="legend-dot now" />Siguiente reto</span>
        <span><i className="legend-dot locked" />Por desbloquear</span>
      </div>
      <a className="image-credit" href="https://commons.wikimedia.org/wiki/File:PLAZA_MAYOR_DE_AYACUCHO.jpg" target="_blank" rel="noreferrer">
        Foto: Pollinhhsano · Wikimedia Commons · CC BY-SA 4.0
      </a>
    </section>
  )
}
