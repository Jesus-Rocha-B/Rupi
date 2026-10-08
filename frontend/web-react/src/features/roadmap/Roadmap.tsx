<<<<<<< HEAD
import { Fragment, useEffect, useRef, useMemo, useState } from 'react'
=======
import { useEffect, useMemo, useState } from 'react'
>>>>>>> 656170f (Se implementó el flujo completo de inicio y finalización de lecciones escolares con otorgamiento idempotente de 50 puntos de experiencia y desbloqueo automático de la siguiente parada para cumplir con HU-02 y HU-05, además se configuró el desplazamiento suave y centrado accesible del mapa en el último nodo visitado junto con el acceso directo desde el botón de bienvenida para cumplir con HU-03, también se corrigieron las discrepancias de columnas de catálogo en el repositorio de Spring Boot para enlazar las unidades y competencias curriculares oficiales del MINEDU con sus respectivas bandas visuales en el roadmap para cumplir con HU-04, asimismo se diseñó el modal didáctico de actividades con estados de carga interactivos, contenido explicativo de respaldo y pantalla de celebración con Rupi festejando y audio de felicitación, y finalmente se agregaron las pruebas unitarias en JUnit, el script de integración para PowerShell y la documentación técnica detallada de las cuatro historias en la carpeta de implementación)
import {
  Check,
  Star,
  Sparkles,
  MapPin,
  Volume2,
  Trophy,
  Flag,
  HelpCircle,
  Landmark,
  Palette,
  Store,
  Coins,
  Layers,
  Mountain,
  Boxes,
  Bell,
  Footprints,
  Sun,
  Compass,
  Award,
} from 'lucide-react'
import { nodeStateLabels } from './nodeStates'
import { RupiCharacter } from '../../components/RupiCharacter'
import { createTrailPath } from './lessons'
import { unitBandsByNode } from './unitGroups'
import './UnitBands.css'
import { speakText } from '../../utils/speech'
import type { RouteDetail, RouteNode } from '../../services/learningRoutesApi'
import { unitBandsByNode } from './unitGroups'
import './UnitBands.css'

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

function getStopThemeIcon(sequence: number) {
  switch (sequence) {
    case 1:
      return Landmark
    case 2:
      return Palette
    case 3:
      return Store
    case 4:
      return Coins
    case 5:
      return Layers
    case 6:
      return Mountain
    case 7:
      return Boxes
    case 8:
      return Bell
    case 9:
      return Footprints
    case 10:
      return Trophy
    default:
      return Sparkles
  }
}

export function Roadmap({ route, guideMessage, onGuideClick, onSelectNode }: Props) {
  const [rupiMood, setRupiMood] = useState<'idle' | 'jumping' | 'cheering' | 'thinking'>('cheering')

<<<<<<< HEAD
  const restored = useRef('')
  useEffect(() => {
    if (restored.current === route.versionRouteId) return
    const node = route.nodes.find(item => item.id === route.enrollment.lastVisitedNodeId && item.state !== 'BLOQUEADO')
    if (!node) return
    const button = document.getElementById(`node-${node.id}`)
    if (button) {
      restored.current = route.versionRouteId
      button.scrollIntoView({ block: 'center', behavior: 'instant' })
      button.focus({ preventScroll: true })
    }
  }, [route])
=======
  // Bandas curriculares por unidad del MINEDU (HU-04)
  const unitBands = useMemo(() => unitBandsByNode(route), [route])
>>>>>>> 656170f (Se implementó el flujo completo de inicio y finalización de lecciones escolares con otorgamiento idempotente de 50 puntos de experiencia y desbloqueo automático de la siguiente parada para cumplir con HU-02 y HU-05, además se configuró el desplazamiento suave y centrado accesible del mapa en el último nodo visitado junto con el acceso directo desde el botón de bienvenida para cumplir con HU-03, también se corrigieron las discrepancias de columnas de catálogo en el repositorio de Spring Boot para enlazar las unidades y competencias curriculares oficiales del MINEDU con sus respectivas bandas visuales en el roadmap para cumplir con HU-04, asimismo se diseñó el modal didáctico de actividades con estados de carga interactivos, contenido explicativo de respaldo y pantalla de celebración con Rupi festejando y audio de felicitación, y finalmente se agregaron las pruebas unitarias en JUnit, el script de integración para PowerShell y la documentación técnica detallada de las cuatro historias en la carpeta de implementación)

  // Separación vertical fija de 145px entre paradas
  const STOP_VERTICAL_GAP = 145
  const TOP_OFFSET = 75
  const UNIT_BAND_GAP = 124
  const unitBands = useMemo(() => unitBandsByNode(route), [route])
  // Cada banda de unidad empuja hacia abajo las paradas que le siguen
  const bandsBefore = useMemo(() => {
    let count = 0
    return route.nodes.map(node => (unitBands.has(node.id) ? ++count : count))
  }, [route.nodes, unitBands])
  const totalMapHeight = useMemo(() => {
    const bands = bandsBefore.length ? bandsBefore[bandsBefore.length - 1] : 0
    return Math.max(800, TOP_OFFSET + (route.nodes.length - 1) * STOP_VERTICAL_GAP + bands * UNIT_BAND_GAP + 100)
  }, [route.nodes.length, bandsBefore])

  // Coordenadas con etiquetas dirigidas al exterior del camino
  const points = useMemo(() => {
    return route.nodes.map((_, index) => {
      const isGoal = index === route.nodes.length - 1
      const xPercent = isGoal ? 50 : index % 2 ? 34 : 66
      const yPixel = TOP_OFFSET + index * STOP_VERTICAL_GAP + bandsBefore[index] * UNIT_BAND_GAP
      const yPercent = (yPixel / totalMapHeight) * 100
      return {
        x: xPercent,
        y: yPercent,
        pixelY: yPixel,
      }
    })
  }, [route.nodes, totalMapHeight, bandsBefore])

  const trailPath = useMemo(() => createTrailPath(points), [points])

  // Identificar el nodo activo (priorizando el último visitado guardado en BD, HU-03)
  const activeIndex = useMemo(() => {
<<<<<<< HEAD
    const remembered = route.nodes.findIndex(n => n.id === route.enrollment.lastVisitedNodeId && n.state !== 'BLOQUEADO')
    if (remembered !== -1) return remembered
=======
    if (route.enrollment?.lastVisitedNodeId) {
      const lastIdx = route.nodes.findIndex((n) => n.id === route.enrollment.lastVisitedNodeId)
      if (lastIdx !== -1) return lastIdx
    }
>>>>>>> 656170f (Se implementó el flujo completo de inicio y finalización de lecciones escolares con otorgamiento idempotente de 50 puntos de experiencia y desbloqueo automático de la siguiente parada para cumplir con HU-02 y HU-05, además se configuró el desplazamiento suave y centrado accesible del mapa en el último nodo visitado junto con el acceso directo desde el botón de bienvenida para cumplir con HU-03, también se corrigieron las discrepancias de columnas de catálogo en el repositorio de Spring Boot para enlazar las unidades y competencias curriculares oficiales del MINEDU con sus respectivas bandas visuales en el roadmap para cumplir con HU-04, asimismo se diseñó el modal didáctico de actividades con estados de carga interactivos, contenido explicativo de respaldo y pantalla de celebración con Rupi festejando y audio de felicitación, y finalmente se agregaron las pruebas unitarias en JUnit, el script de integración para PowerShell y la documentación técnica detallada de las cuatro historias en la carpeta de implementación)
    const idx = route.nodes.findIndex((n) => n.state === 'EN_CURSO')
    if (idx !== -1) return idx
    const availIdx = route.nodes.findIndex((n) => n.state === 'DISPONIBLE')
    return availIdx !== -1 ? availIdx : 0
<<<<<<< HEAD
  }, [route.nodes, route.enrollment.lastVisitedNodeId])
=======
  }, [route.nodes, route.enrollment?.lastVisitedNodeId])

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
>>>>>>> 656170f (Se implementó el flujo completo de inicio y finalización de lecciones escolares con otorgamiento idempotente de 50 puntos de experiencia y desbloqueo automático de la siguiente parada para cumplir con HU-02 y HU-05, además se configuró el desplazamiento suave y centrado accesible del mapa en el último nodo visitado junto con el acceso directo desde el botón de bienvenida para cumplir con HU-03, también se corrigieron las discrepancias de columnas de catálogo en el repositorio de Spring Boot para enlazar las unidades y competencias curriculares oficiales del MINEDU con sus respectivas bandas visuales en el roadmap para cumplir con HU-04, asimismo se diseñó el modal didáctico de actividades con estados de carga interactivos, contenido explicativo de respaldo y pantalla de celebración con Rupi festejando y audio de felicitación, y finalmente se agregaron las pruebas unitarias en JUnit, el script de integración para PowerShell y la documentación técnica detallada de las cuatro historias en la carpeta de implementación)

  const handleGuideAction = () => {
    setRupiMood((prev) => (prev === 'jumping' ? 'cheering' : 'jumping'))
    onGuideClick()
  }

  const handleNodeInteraction = (node: RouteNode) => {
    speakText(`Parada ${node.sequence}: ${node.title}. ${nodeStateLabels[node.state]}`)
    if (node.state !== 'BLOQUEADO') {
      onSelectNode(node)
    }
  }

  return (
    <section className="map-panel" id="explorar" aria-labelledby="roadmap-title">
      <header className="unit-heading">
        <span className="unit-number"><MapPin aria-hidden="true" /></span>
        <div className="unit-copy">
          <span className="eyebrow">Camino de aventuras</span>
          <h3 id="roadmap-title">{route.title}</h3>
          <span className="unit-subtitle">
            {route.progress.totalNodes} paradas para descubrir y aprender ({route.area.name} · {route.grade.name})
          </span>
        </div>
        <span className="unit-status">
          {route.enrollment.state === 'ACTIVO' ? 'EN AVENTURA' : route.enrollment.state}
        </span>
      </header>

      {route.curriculum && route.curriculum.status !== 'COMPLETA' && route.curriculum.message && (
        <p className={`curriculum-note ${route.curriculum.status.toLowerCase()}`} role="status">
          {route.curriculum.message}
        </p>
      )}

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
          const ThemeIcon = getStopThemeIcon(node.sequence)
<<<<<<< HEAD
          const band = unitBands.get(node.id)

          return (
            <Fragment key={node.id}>
            {band && (
              <div className="unit-band" style={{ top: `${posY - UNIT_BAND_GAP - 20}px` }} role="group" aria-label={`${band.unit.title}. ${band.completed} de ${band.total} paradas completadas`}>
                <span className="unit-band-text">
                  <span className="unit-band-title">{band.unit.title}</span>
                  {band.unit.competencies.length > 0 && (
                    <span className="unit-band-competencies">
                      Currículo MINEDU: {band.unit.competencies.map(item => item.name).join(' · ')}
                    </span>
                  )}
                </span>
                <span className="unit-band-progress">{band.completed} de {band.total}</span>
              </div>
            )}
            <div
              className={`trail-stop ${cssClass} ${isActive ? 'is-current-active' : ''} ${isGoal ? 'is-final-goal' : ''}`}
              id={node.sequence === 3 ? 'retos' : undefined}
              style={{ left: `${posX}%`, top: `${posY}px` }}
            >
              {/* Destellos de celebración en paradas completadas */}
              {node.state === 'COMPLETADO' && (
                <div className="completion-burst" aria-hidden="true">
                  <span className="burst-star star-1">✦</span>
                  <span className="burst-star star-2">✦</span>
                  <span className="burst-star star-3">✦</span>
                </div>
              )}

              {/* Nodo circular con ícono temático y número legible en insignia */}
              <button
                id={`node-${node.id}`}
                disabled={!canOpen}
                className={`lesson-node ${cssClass} ${isGoal ? 'is-goal-node' : ''}`}
                type="button"
                onClick={() => handleNodeInteraction(node)}
                aria-label={`Parada ${node.sequence}: ${node.title}. Estado: ${stateLabel}. ${canOpen ? 'Toca para abrir' : 'Aún no disponible'}`}
              >
                <ThemeIcon className="node-theme-icon" aria-hidden="true" />
                <span className="node-seq-badge" aria-hidden="true">
                  {node.state === 'COMPLETADO' ? (
                    <Check size={14} strokeWidth={3.5} />
                  ) : (
                    node.sequence
                  )}
                </span>
              </button>

              {/* Etiqueta compacta hacia el exterior con conector al nodo */}
=======
          const unitBand = unitBands.get(node.id)

          return (
            <div key={node.id}>
              {/* Banda de encabezado de unidad curricular MINEDU (HU-04) */}
              {unitBand && (
                <div
                  className="unit-band"
                  style={{ top: `${Math.max(10, posY - 68)}px` }}
                  role="region"
                  aria-label={`Unidad curricular: ${unitBand.unit.title}`}
                >
                  <div className="unit-band-text">
                    <span className="unit-band-title">
                      {unitBand.unit.title}
                    </span>
                    {unitBand.unit.competencies.length > 0 && (
                      <span className="unit-band-competencies">
                        Currículo MINEDU: {unitBand.unit.competencies.map((c) => c.name).join(' · ')}
                      </span>
                    )}
                  </div>
                  <span className="unit-band-progress">
                    <Award size={13} aria-hidden="true" />
                    <span>{unitBand.completed} de {unitBand.total}</span>
                  </span>
                </div>
              )}

>>>>>>> 656170f (Se implementó el flujo completo de inicio y finalización de lecciones escolares con otorgamiento idempotente de 50 puntos de experiencia y desbloqueo automático de la siguiente parada para cumplir con HU-02 y HU-05, además se configuró el desplazamiento suave y centrado accesible del mapa en el último nodo visitado junto con el acceso directo desde el botón de bienvenida para cumplir con HU-03, también se corrigieron las discrepancias de columnas de catálogo en el repositorio de Spring Boot para enlazar las unidades y competencias curriculares oficiales del MINEDU con sus respectivas bandas visuales en el roadmap para cumplir con HU-04, asimismo se diseñó el modal didáctico de actividades con estados de carga interactivos, contenido explicativo de respaldo y pantalla de celebración con Rupi festejando y audio de felicitación, y finalmente se agregaron las pruebas unitarias en JUnit, el script de integración para PowerShell y la documentación técnica detallada de las cuatro historias en la carpeta de implementación)
              <div
                className={`trail-stop ${cssClass} ${isActive ? 'is-current-active' : ''} ${isGoal ? 'is-final-goal' : ''}`}
                id={node.sequence === 3 ? 'retos' : undefined}
                style={{ left: `${posX}%`, top: `${posY}px` }}
              >
                {/* Destellos de celebración en paradas completadas */}
                {node.state === 'COMPLETADO' && (
                  <div className="completion-burst" aria-hidden="true">
                    <span className="burst-star star-1">✦</span>
                    <span className="burst-star star-2">✦</span>
                    <span className="burst-star star-3">✦</span>
                  </div>
                )}

                {/* Nodo circular con ícono temático y número legible en insignia */}
                <button
                  className={`lesson-node ${cssClass} ${isGoal ? 'is-goal-node' : ''}`}
                  type="button"
                  onClick={() => handleNodeInteraction(node)}
                  aria-label={`Parada ${node.sequence}: ${node.title}. Estado: ${stateLabel}. ${canOpen ? 'Toca para abrir' : 'Aún no disponible'}`}
                >
                  <ThemeIcon className="node-theme-icon" aria-hidden="true" />
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
                  className={`node-label-pill ${labelSide} ${cssClass} ${isGoal ? 'is-goal-pill' : ''}`}
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
                  {isGoal && (
                    <span className="goal-kicker-tag">
                      <Trophy size={14} aria-hidden="true" />
                      ¡Meta!
                    </span>
                  )}
                  <b className="stop-title-text">{node.title}</b>
                  <span className={`pill-state ${cssClass}`}>{nodeStateLabels[node.state]}</span>
                </div>

                {/* Rupi y su bocadillo situados al lado opuesto de la etiqueta */}
                {isActive && (
                  <div
                    className={`rupi-road-anchor ${labelSide === 'right' ? 'anchor-left' : 'anchor-right'}`}
                    aria-live="polite"
                  >
                    <div
                      className="rupi-guide-box"
                      onClick={handleGuideAction}
                      role="button"
                      tabIndex={0}
                      aria-label="Rupi te acompaña en esta parada. Toca para pedir otro consejo."
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault()
                          handleGuideAction()
                        }
                      }}
                    >
                      <RupiCharacter mood={rupiMood} className="avatar-on-road" />
                      <div className="road-bubble">
                        <div className="road-bubble-header">
                          <span className="rupi-badge-name">Rupi contigo</span>
                          <button
                            type="button"
                            className="rupi-listen-btn"
                            onClick={(e) => {
                              e.stopPropagation()
                              speakText(guideMessage)
                            }}
                            aria-label="Escuchar consejo de Rupi"
                            title="Escuchar consejo"
                          >
                            <Volume2 size={16} aria-hidden="true" />
                            <span>Escuchar</span>
                          </button>
                        </div>
                        <p className="road-bubble-text">{guideMessage}</p>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
            </Fragment>
          )
        })}

        <Sparkles className="map-decoration sparkle sparkle-one" aria-hidden="true" />
        <Sparkles className="map-decoration sparkle sparkle-two" aria-hidden="true" />
      </div>

      {/* Leyenda única y uniforme */}
      <div className="map-legend" id="logros" aria-label="Significado de los símbolos del mapa">
        <span className="legend-chip">
          <span className="legend-icon-badge done">
            <Check size={16} strokeWidth={3} aria-hidden="true" />
          </span>
          <b>¡Lo lograste!</b>
        </span>
        <span className="legend-chip">
          <span className="legend-icon-badge now">
            <Star size={16} fill="currentColor" aria-hidden="true" />
          </span>
          <b>¡Sigue por aquí!</b>
        </span>
        <span className="legend-chip">
          <span className="legend-icon-badge available">
            <Flag size={16} fill="currentColor" aria-hidden="true" />
          </span>
          <b>¡Puedes entrar!</b>
        </span>
        <span className="legend-chip">
          <span className="legend-icon-badge locked">
            <HelpCircle size={16} strokeWidth={2.5} aria-hidden="true" />
          </span>
          <b>Aún no</b>
        </span>
      </div>
    </section>
  )
}
