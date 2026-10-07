import { useMemo, useState } from 'react'
import {
  Award,
  Bell,
  BookOpen,
  ChevronRight,
  Coins,
  Compass,
  ExternalLink,
  Footprints,
  Landmark,
  Layers,
  MapPin,
  Mountain,
  Palette,
  Shapes,
  Sparkles,
  Store,
  Volume2,
} from 'lucide-react'
import { RupiCharacter } from './RupiCharacter'
import { speakText } from '../utils/speech'
import type { CulturalContext, CulturalFact, RouteDetail, RouteNode } from '../services/learningRoutesApi'

type Props = {
  route: RouteDetail
  onSelect?: (node: RouteNode) => void
}

// Catálogo de íconos SVG de Lucide para resolver dinámicamente según la BD (¡Cero emojis!)
const FACT_ICONS: Record<
  string,
  React.ComponentType<{ size?: number; className?: string; 'aria-hidden'?: boolean | 'true' | 'false' }>
> = {
  landmark: Landmark,
  palette: Palette,
  store: Store,
  coins: Coins,
  layers: Layers,
  mountain: Mountain,
  shapes: Shapes,
  bell: Bell,
  footprints: Footprints,
  award: Award,
  sparkles: Sparkles,
  compass: Compass,
  book: BookOpen,
  'book-open': BookOpen,
  mappin: MapPin,
  'map-pin': MapPin,
}

export function RouteCompanion({ route }: Props) {
  // Identificar la parada actual (EN_CURSO o primera DISPONIBLE)
  const currentNode = useMemo(() => {
    return (
      route.nodes.find(node => node.id === route.enrollment.lastVisitedNodeId && node.state !== 'BLOQUEADO') ??
      route.nodes.find((node) => node.state === 'EN_CURSO') ??
      route.nodes.find((node) => node.state === 'DISPONIBLE')
    )
  }, [route.nodes, route.enrollment.lastVisitedNodeId])

  // Contexto cultural: recibido del backend o respaldo de Ayacucho si la aventura está ambientada allí
  const context: CulturalContext | null = useMemo(() => {
    if (route.culturalContext && route.culturalContext.facts?.length) {
      return route.culturalContext
    }
    return route.culturalContext ?? null
  }, [route.culturalContext])

  const facts: CulturalFact[] = useMemo(() => {
    return context?.facts ?? []
  }, [context])

  const factKey = `${route.versionRouteId}:${currentNode?.id ?? ''}`
  const [choice, setChoice] = useState({ key: '', index: 0 })
  const linkedIndex = facts.findIndex(fact => fact.nodeSequence === currentNode?.sequence)
  const activeFactIndex = choice.key === factKey ? choice.index : Math.max(0, linkedIndex)

  // Si no hay contexto cultural, mostrar solo la tarjeta de ánimo pedagógico de Rupi sin romper la pantalla
  if (!context || !facts.length) {
    return (
      <aside className="route-companion sticky-companion" aria-label="Acompañamiento de tu ruta">
        <section className="cultural-companion-card simple-motivational">
          <div className="companion-rupi-quote">
            <RupiCharacter mood="cheering" className="companion-rupi-avatar" />
            <div className="rupi-quote-body">
              <span className="quote-badge">
                <Sparkles size={16} aria-hidden="true" />
                Mensaje de Rupi
              </span>
              <p className="quote-text">
                {context?.motivationalMessage ||
                  'No hay prisa en el camino. ¡Cada paso que das te hace crecer!'}
              </p>
            </div>
          </div>
        </section>
      </aside>
    )
  }

  const currentFact = facts[activeFactIndex] ?? facts[0]
  const FactIcon = FACT_ICONS[currentFact.icon?.toLowerCase().trim()] ?? Sparkles
  const isLinkedToCurrent = currentNode && currentFact.nodeSequence === currentNode.sequence

  const handleNextFact = () => {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel()
    }
    setChoice({ key: factKey, index: (activeFactIndex + 1) % facts.length })
  }

  const handleListenFact = () => {
    speakText(`¿Sabías que? ${currentFact.title}. ${currentFact.content}`)
  }

  return (
    <aside className="route-companion sticky-companion" aria-label="Sobre tu recorrido cultural">
      <section className="cultural-companion-card" role="region" aria-labelledby="cultural-card-title">
        {/* 1. Imagen del lugar con alt descriptivo */}
        <div className="companion-image-wrapper">
          <img
            src={context.imageUrl}
            alt={context.imageAlt}
            className="companion-image"
            loading="lazy"
          />
          <span className="companion-location-chip">
            <MapPin size={15} aria-hidden="true" />
            {context.city}, Perú
          </span>
        </div>

        {/* 2. Cabecera cultural */}
        <div className="companion-card-header">
          <span className="companion-eyebrow">{context.title}</span>
          <h3 id="cultural-card-title">{context.place}</h3>
        </div>

        {/* 3. Zona interactiva: Un dato curioso a la vez */}
        <div className="fact-showcase" aria-live="polite">
          <div className="fact-kicker-row">
            <span className="fact-counter-pill">
              <Sparkles size={15} aria-hidden="true" />
              Dato curioso {activeFactIndex + 1} de {facts.length}
            </span>
            {isLinkedToCurrent && (
              <span className="fact-linked-chip" title="Dato de tu parada actual">
                Tu parada {currentNode?.sequence}
              </span>
            )}
          </div>

          <div className="fact-card-body">
            <div className="fact-icon-badge" aria-hidden="true">
              <FactIcon size={26} className="fact-svg-icon" />
            </div>
            <div className="fact-text-group">
              <h4 className="fact-title">{currentFact.title}</h4>
              <p className="fact-content">{currentFact.content}</p>
              <small className="fact-source">Fuente: {currentFact.source}</small>
            </div>
          </div>

          {/* Botones de acción táctiles (48px accesibles) */}
          <div className="fact-actions-row">
            <button
              type="button"
              className="fact-btn fact-listen-btn"
              onClick={handleListenFact}
              aria-label={`Escuchar el dato curioso: ${currentFact.title}`}
              title="Escuchar dato"
            >
              <Volume2 size={20} aria-hidden="true" />
              <span>Escuchar</span>
            </button>

            <button
              type="button"
              className="fact-btn fact-next-btn"
              onClick={handleNextFact}
              aria-label="Ver otro dato curioso de esta aventura"
              title="Ver otro dato"
            >
              <span>Ver otro dato</span>
              <ChevronRight size={20} aria-hidden="true" />
            </button>
          </div>
        </div>

        {/* 4. Mensaje de aliento de Rupi integrado en la misma tarjeta */}
        <div className="companion-rupi-quote">
          <RupiCharacter mood="cheering" className="companion-rupi-avatar" />
          <div className="rupi-quote-body">
            <span className="quote-badge">
              <Sparkles size={14} aria-hidden="true" />
              Rupi te acompaña
            </span>
            <p className="quote-text">{context.motivationalMessage}</p>
          </div>
        </div>

        {/* 5. Atribución visible de la imagen con enlace a la licencia */}
        <footer className="companion-footer-credit">
          <span className="credit-line">
            Foto: {context.author} · {context.source} ·{' '}
            <a
              href={context.licenseUrl}
              target="_blank"
              rel="noreferrer"
              className="credit-link"
              title="Ver licencia en Wikimedia Commons"
            >
              <span>{context.license}</span>
              <ExternalLink size={12} aria-hidden="true" />
            </a>
          </span>
        </footer>
      </section>
    </aside>
  )
}
