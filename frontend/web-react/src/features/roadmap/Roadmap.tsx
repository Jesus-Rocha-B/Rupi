import { Fragment, useEffect, useMemo, useState } from 'react'
import {
  Check,
  Compass,
  Sparkles,
  MapPin,
  Volume2,
  Trophy,
  Flag,
  Lock,
  Award,
  Eye,
  Ear,
  Flower2,
  Heart,
  Apple,
  Droplets,
  Sun,
  Microscope,
  FlaskConical,
  Binary,
  Shapes,
  Ruler,
  Lightbulb,
  Calculator,
  PenTool,
  MessageSquare,
  BookOpen,
} from 'lucide-react'
import { nodeStateLabels } from './nodeStates'
import { RupiCharacter } from '../../components/RupiCharacter'
import { unitBandsByNode } from './unitGroups'
import { UnitDownloadPanel } from './UnitDownloadPanel'
import { speakText } from '../../utils/speech'
import type { RouteDetail, RouteNode } from '../../services/learningRoutesApi'
import './UnitBands.css'

function createTrailPath(points: { x: number; y: number }[]): string {
  const coords = points.map(({ x, y }) => ({ x: x * 6, y: y * 10 }))
  if (coords.length < 2) return ''

  return coords.reduce((path, point, index) => {
    if (index === 0) return `M ${point.x} ${point.y}`
    const previous = coords[index - 1]
    const beforePrevious = coords[index - 2] ?? previous
    const next = coords[index + 1] ?? point
    const controlOne = {
      x: previous.x + (point.x - beforePrevious.x) / 6,
      y: previous.y + (point.y - beforePrevious.y) / 6,
    }
    const controlTwo = {
      x: point.x - (next.x - previous.x) / 6,
      y: point.y - (next.y - previous.y) / 6,
    }
    return `${path} C ${controlOne.x} ${controlOne.y}, ${controlTwo.x} ${controlTwo.y}, ${point.x} ${point.y}`
  }, '')
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
      return { cssClass: 'complete', label: '¡Lo lograste!' }
    case 'EN_CURSO':
      return { cssClass: 'active', label: '¡Sigue por aquí!' }
    case 'DISPONIBLE':
      return { cssClass: 'available', label: '¡Puedes entrar!' }
    case 'BLOQUEADO':
    default:
      return { cssClass: 'locked', label: 'Aún no' }
  }
}

function getStopThemeIcon(node: RouteNode, areaCode?: string) {
  const t = (node.title || '').toLowerCase()
  const area = (areaCode || '').toUpperCase()

  // 1. Ciencia y Tecnología: sentidos, naturaleza, cuerpo, energía
  if (
    area.includes('CYT') ||
    t.includes('sentido') ||
    t.includes('vista') ||
    t.includes('ojo') ||
    t.includes('oído') ||
    t.includes('oido') ||
    t.includes('tacto') ||
    t.includes('gusto') ||
    t.includes('olfato') ||
    t.includes('cuerpo') ||
    t.includes('planta') ||
    t.includes('flora') ||
    t.includes('animal') ||
    t.includes('ave') ||
    t.includes('fauna') ||
    t.includes('ambiente') ||
    t.includes('energ')
  ) {
    if (t.includes('vista') || t.includes('ojo') || t.includes('sentido') || t.includes('observa')) return Eye
    if (t.includes('oído') || t.includes('oido') || t.includes('sonido') || t.includes('escucha') || t.includes('cantar')) return Ear
    if (t.includes('cuerpo') || t.includes('salud') || t.includes('piel') || t.includes('tacto') || t.includes('vida')) return Heart
    if (t.includes('gusto') || t.includes('fruto') || t.includes('alimento') || t.includes('nutri') || t.includes('papa')) return Apple
    if (t.includes('planta') || t.includes('flor') || t.includes('semilla') || t.includes('biohuerto') || t.includes('olfato') || t.includes('chacra') || t.includes('suelo')) return Flower2
    if (t.includes('agua') || t.includes('río') || t.includes('lluvia') || t.includes('nube')) return Droplets
    if (t.includes('sol') || t.includes('luz') || t.includes('energía') || t.includes('energia')) return Sun
    if (t.includes('experimento') || t.includes('laboratorio') || t.includes('materia')) return Microscope
    return FlaskConical
  }

  // 2. Matemática: números, figuras, medidas, problemas
  if (
    area.includes('MAT') ||
    t.includes('número') ||
    t.includes('numero') ||
    t.includes('conteo') ||
    t.includes('suma') ||
    t.includes('resta') ||
    t.includes('geomet') ||
    t.includes('figura') ||
    t.includes('fracci') ||
    t.includes('patr')
  ) {
    if (t.includes('número') || t.includes('numero') || t.includes('conteo') || t.includes('secuencia') || t.includes('patr') || t.includes('quipu')) return Binary
    if (t.includes('geomet') || t.includes('figura') || t.includes('forma') || t.includes('triángulo') || t.includes('cuadrilátero') || t.includes('cuerpo')) return Shapes
    if (t.includes('medida') || t.includes('metro') || t.includes('longitud') || t.includes('regla') || t.includes('medimos')) return Ruler
    if (t.includes('solución') || t.includes('problema') || t.includes('reto')) return Lightbulb
    return Calculator
  }

  // 3. Comunicación: lectura, escritura, relatos, diálogos
  if (
    area.includes('COM') ||
    t.includes('lectura') ||
    t.includes('leer') ||
    t.includes('cuento') ||
    t.includes('fábula') ||
    t.includes('fabula') ||
    t.includes('leyenda') ||
    t.includes('texto') ||
    t.includes('escrib')
  ) {
    if (t.includes('escritura') || t.includes('escrib') || t.includes('redact') || t.includes('carta')) return PenTool
    if (t.includes('oral') || t.includes('diálogo') || t.includes('habla') || t.includes('convers') || t.includes('adivinanza')) return MessageSquare
    return BookOpen
  }

  // Fallback armónico
  switch (node.sequence % 3) {
    case 1: return BookOpen
    case 2: return Calculator
    default: return FlaskConical
  }
}

export function Roadmap({ route, guideMessage, onGuideClick, onSelectNode }: Props) {
  const [rupiMood, setRupiMood] = useState<'idle' | 'jumping' | 'cheering' | 'thinking'>('cheering')

  // Bandas curriculares por unidad del MINEDU (HU-04)
  const unitBands = useMemo(() => unitBandsByNode(route), [route])

  // Separación vertical fija de 145px entre paradas a lo largo del sendero
  const STOP_VERTICAL_GAP = 145
  const TOP_OFFSET = 75

  const totalMapHeight = useMemo(() => {
    return Math.max(760, TOP_OFFSET + (route.nodes.length - 1) * STOP_VERTICAL_GAP + 120)
  }, [route.nodes.length])

  // Coordenadas con etiquetas dirigidas al exterior del camino
  const points = useMemo(() => {
    return route.nodes.map((_, index) => {
      const isGoal = index === route.nodes.length - 1
      const xPercent = isGoal ? 50 : index % 2 ? 34 : 66
      const yPixel = TOP_OFFSET + index * STOP_VERTICAL_GAP
      const yPercent = (yPixel / totalMapHeight) * 100
      return {
        x: xPercent,
        y: yPercent,
        pixelY: yPixel,
      }
    })
  }, [route.nodes, totalMapHeight])

  const trailPath = useMemo(() => createTrailPath(points), [points])

  // Identificar el nodo activo (el nivel actual del jugador para que Rupi avance)
  const lastVisitedNodeId = route.enrollment?.lastVisitedNodeId
  const activeIndex = useMemo(() => {
    // 1. Si hay un nodo 'EN_CURSO', el jugador está jugando ese nivel
    const inProgressIdx = route.nodes.findIndex((n) => n.state === 'EN_CURSO')
    if (inProgressIdx !== -1) return inProgressIdx

    // 2. Si el último visitado no está completado ni bloqueado, usarlo
    if (lastVisitedNodeId) {
      const lastIdx = route.nodes.findIndex((n) => n.id === lastVisitedNodeId)
      if (lastIdx !== -1 && route.nodes[lastIdx].state !== 'COMPLETADO' && route.nodes[lastIdx].state !== 'BLOQUEADO') {
        return lastIdx
      }
    }

    // 3. Primer nodo DISPONIBLE para jugar (el siguiente nivel a superar)
    const firstAvailableIdx = route.nodes.findIndex((n) => n.state === 'DISPONIBLE')
    if (firstAvailableIdx !== -1) return firstAvailableIdx

    // 4. Si toda la ruta está completada, Rupi se coloca en la meta final
    const allCompleted = route.nodes.length > 0 && route.nodes.every((n) => n.state === 'COMPLETADO')
    if (allCompleted) {
      return route.nodes.length - 1
    }

    // 5. Respaldo: primer nodo
    return 0
  }, [route.nodes, lastVisitedNodeId])

  // Desplazar suavemente el mapa al nodo activo al cargar la ruta (HU-03)
  useEffect(() => {
    const timer = setTimeout(() => {
      const activeEl = document.querySelector('.trail-stop.is-current-active')
      if (activeEl) {
        activeEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }
    }, 300)
    return () => clearTimeout(timer)
  }, [route.versionRouteId, route.enrollment?.lastVisitedNodeId])

  // Unidad activa según el nivel actual del estudiante en el sendero
  const activeNode = route.nodes[activeIndex]
  const currentUnit = useMemo(() => {
    const units = route.curriculum?.units ?? []
    if (!units.length) return null
    if (activeNode) {
      for (const u of units) {
        if (u.nodeIds?.includes(activeNode.id) || u.nodes?.some(n => n.id === activeNode.id)) {
          return u
        }
      }
    }
    return units[0]
  }, [route.curriculum, activeNode])

  const handleGuideAction = () => {
    setRupiMood((prev) => (prev === 'jumping' ? 'cheering' : 'jumping'))
    if (guideMessage) {
      speakText(guideMessage)
    }
    onGuideClick()
  }

  const handleNodeInteraction = (node: RouteNode) => {
    onSelectNode(node)
  }

  return (
    <section className="map-panel" id="explorar" aria-labelledby="roadmap-title">
      <header className="unit-heading" role="region" aria-label="Unidad escolar y avance de paradas">
        <span className="unit-number">
          {currentUnit ? <BookOpen size={18} aria-hidden="true" /> : <MapPin aria-hidden="true" />}
        </span>
        <div className="unit-copy">
          <div className="unit-eyebrow-row">
            <span className="unit-badge-tag">
              {currentUnit?.code ? currentUnit.code : 'Unidad de Aprendizaje'}
            </span>
            <span className="unit-progress-pill">
              <Award size={12} aria-hidden="true" />
              <span>
                {currentUnit
                  ? `${currentUnit.completedNodes ?? route.progress.completedNodes} de ${currentUnit.totalNodes ?? route.progress.totalNodes} paradas`
                  : `${route.progress.completedNodes} de ${route.progress.totalNodes} paradas`}
              </span>
            </span>
          </div>
          <h3 id="roadmap-title">
            {currentUnit ? currentUnit.title : `${route.progress.completedNodes} de ${route.progress.totalNodes} paradas completadas`}
          </h3>
          {currentUnit?.competencies && currentUnit.competencies.length > 0 && (
            <span className="unit-competencies-text">
              {currentUnit.competencies.map(c => c.name).join(' · ')}
            </span>
          )}
        </div>
        {guideMessage && (
          <button
            type="button"
            className="listen-guide-btn"
            onClick={handleGuideAction}
            title={`Escuchar a Rupi: "${guideMessage}"`}
            aria-label="Escuchar consejo de Rupi"
          >
            <Volume2 size={16} aria-hidden="true" />
            <span>Escuchar a Rupi</span>
          </button>
        )}
        <span className="unit-status">
          {route.progress.completedNodes === route.progress.totalNodes ? '¡Ruta completada!' : 'En marcha'}
        </span>
      </header>

      <div
        className="map-area"
        style={{ height: `${totalMapHeight}px`, minHeight: `${totalMapHeight}px` }}
        aria-label={`Mapa con ${route.progress.totalNodes} paradas de aprendizaje`}
      >
        {/* Decoraciones laterales sutiles que no compiten con los nodos */}
        <div className="lateral-decorations" aria-hidden="true">
          <span className="side-deco side-llama"><Compass size={28} /></span>
          <span className="side-deco side-flower"><Sparkles size={26} /></span>
          <span className="side-deco side-sun"><Sun size={30} /></span>
        </div>

        {/* Sendero principal en zigzag */}
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
          const posY = points[index].pixelY
          // Etiquetas orientadas siempre al EXTERIOR del sendero
          const labelSide = posX >= 50 ? 'right' : 'left'
          const isActive = index === activeIndex
          const isGoal = index === route.nodes.length - 1
          const ThemeIcon = getStopThemeIcon(node, route.area?.code)

          return (
            <Fragment key={node.id}>
              <div
                className={`trail-stop ${cssClass} ${isActive ? 'is-current-active' : ''} ${isGoal ? 'is-final-goal' : ''}`}
                id={node.sequence === 3 ? 'retos' : undefined}
                style={{ left: `${posX}%`, top: `${posY}px` }}
              >
                {/* Destellos de celebración en paradas completadas */}
                {node.state === 'COMPLETADO' && (
                  <div className="completion-burst" aria-hidden="true">
                    <Sparkles size={12} className="burst-star star-1" />
                    <Sparkles size={10} className="burst-star star-2" />
                    <Sparkles size={14} className="burst-star star-3" />
                  </div>
                )}

                {/* Nodo circular con Rupi adentro si es el nivel actual del jugador */}
                <button
                  className={`lesson-node ${cssClass} ${isGoal ? 'is-goal-node' : ''} ${isActive ? 'is-current-player-node' : ''}`}
                  type="button"
                  onClick={() => handleNodeInteraction(node)}
                  aria-label={`Parada ${node.sequence}: ${node.title}. Estado: ${stateLabel}. ${canOpen ? 'Toca para abrir' : 'Aún no disponible'}`}
                >
                  {isActive ? (
                    <div className="node-rupi-inside" aria-hidden="true">
                      <RupiCharacter mood={rupiMood} className="node-rupi-svg" />
                    </div>
                  ) : (
                    <ThemeIcon className="node-theme-icon" aria-hidden="true" />
                  )}
                  <span className="node-seq-badge" aria-hidden="true">
                    {node.state === 'COMPLETADO' ? (
                      <Check size={14} strokeWidth={3.5} />
                    ) : (
                      node.sequence
                    )}
                  </span>
                </button>

                {/* Etiqueta compacta hacia el exterior con conector al nodo */}
                <div
                  className={`node-label-pill ${labelSide} ${cssClass} ${isGoal ? 'is-goal-pill' : ''} ${isActive ? 'active-player-pill' : ''}`}
                  onClick={() => handleNodeInteraction(node)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault()
                      handleNodeInteraction(node)
                    }
                  }}
                >
                  {isActive && (
                    <span className="player-here-badge">
                      <span className="player-dot-pulse" aria-hidden="true" />
                      ¡Nivel actual con Rupi!
                    </span>
                  )}
                  {isGoal && (
                    <span className="goal-kicker-tag">
                      <Trophy size={14} aria-hidden="true" />
                      ¡Meta!
                    </span>
                  )}
                  <b className="stop-title-text">{node.title}</b>
                  <span className={`pill-state ${cssClass}`}>{nodeStateLabels[node.state]}</span>
                </div>
              </div>
            </Fragment>
          )
        })}

        <Sparkles className="map-decoration sparkle sparkle-one" aria-hidden="true" />
        <Sparkles className="map-decoration sparkle sparkle-two" aria-hidden="true" />
      </div>

      {/* Leyenda única y uniforme con mini-nodos idénticos al mapa */}
      <div className="map-legend" id="logros" aria-label="Significado de los símbolos del mapa">
        <div className="legend-chip">
          <div className="legend-node-mini complete" aria-hidden="true">
            <Check size={14} strokeWidth={3.5} />
          </div>
          <span>Completada</span>
        </div>
        <div className="legend-chip">
          <div className="legend-node-mini active" aria-hidden="true">
            <Sparkles size={14} />
          </div>
          <span>Tu nivel actual (con Rupi)</span>
        </div>
        <div className="legend-chip">
          <div className="legend-node-mini available" aria-hidden="true">
            <Flag size={14} />
          </div>
          <span>Disponible para jugar</span>
        </div>
        <div className="legend-chip">
          <div className="legend-node-mini locked" aria-hidden="true">
            <Lock size={13} />
          </div>
          <span>Bloqueada</span>
        </div>
      </div>

      <UnitDownloadPanel key={route.versionRouteId} route={route} />
    </section>
  )
}
