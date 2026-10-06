import { useMemo } from 'react'
import { Check, Lock, MapPin, Sparkles, Star } from 'lucide-react'
import { nodeStateLabels } from './nodeStates'
import { RupiCharacter } from '../../components/RupiCharacter'
import { createTrailPath } from './lessons'
import type { RouteDetail, RouteNode } from '../../services/learningRoutesApi'

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
      return { cssClass: 'active', label: 'en curso' }
    case 'DISPONIBLE':
      return { cssClass: 'available', label: 'disponible' }
    case 'BLOQUEADO':
    default:
      return { cssClass: 'locked', label: 'bloqueado' }
  }
}

export function Roadmap({ route, guideMessage, onGuideClick, onSelectNode }: Props) {
  const points = useMemo(() => {
    return route.nodes.map((n, index) => n.position ?? {
      x: index % 2 ? 30 : 65,
      y: 10 + index * 80 / Math.max(1, route.nodes.length - 1),
    })
  }, [route.nodes])

  const trailPath = useMemo(() => createTrailPath(points), [points])

  return (
    <section className="map-panel" id="explorar" aria-labelledby="roadmap-title">
      <header className="unit-heading">
        <span className="unit-number"><MapPin aria-hidden="true" /></span>
        <div className="unit-copy">
          <span className="eyebrow">El camino se hace aprendiendo</span>
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

      <div className="map-area" style={{ minHeight: `${Math.max(750, route.nodes.length * 125)}px` }} aria-label={`Recorrido de ${route.progress.totalNodes} actividades`}>
        <svg className="path-line" viewBox="0 0 600 1000" preserveAspectRatio="none" aria-hidden="true">
          <path className="road-shadow" d={trailPath} />
          <path className="road-edge" d={trailPath} />
          <path className="road-surface" d={trailPath} />
          <path className="road-center" d={trailPath} />
        </svg>

        {route.nodes.map((node, index) => {
          const { cssClass, label: stateLabel } = getNodeDisplayState(node.state)
          const canOpen = cssClass !== 'locked'
          const posX = points[index].x
          const posY = points[index].y
          const labelSide = posX >= 50 ? 'left' : 'right'
          const context = `Parada ${node.sequence}`
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
                  ) : node.state === 'BLOQUEADO' ? <Lock aria-hidden="true" /> : (
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
                <span className="node-state">{nodeStateLabels[node.state]}{node.optional ? ' · Opcional' : ''}</span>
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
        <span><i className="legend-dot now" />En curso</span>
        <span><i className="legend-dot available" />Disponible</span>
        <span><i className="legend-dot locked" />Bloqueado</span>
      </div>
      <a className="image-credit" href="https://commons.wikimedia.org/wiki/File:PLAZA_MAYOR_DE_AYACUCHO.jpg" target="_blank" rel="noreferrer">
        Foto: Pollinhhsano · Wikimedia Commons · CC BY-SA 4.0
      </a>
    </section>
  )
}



