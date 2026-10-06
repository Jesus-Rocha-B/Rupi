import { useEffect, useMemo, useState } from 'react'
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

function resolveFactIcon(iconName: string) {
  const key = iconName ? iconName.toLowerCase().trim() : ''
  return FACT_ICONS[key] ?? Sparkles
}

// Datos culturales verificados de Ayacucho como fallback de respaldo para el cliente
const AYACUCHO_BACKUP_CONTEXT: CulturalContext = {
  id: 'cc000000-0000-4000-8000-000000000001',
  code: 'AYACUCHO_PLAZA',
  city: 'Ayacucho',
  place: 'Plaza Mayor de Ayacucho',
  title: 'Hecho con raíces peruanas',
  description:
    'La hermosa plaza de Ayacucho acompaña nuestra ruta. Cada rincón del Perú guarda historias y aprendizajes por descubrir.',
  motivationalMessage: 'No hay prisa en el camino. ¡Cada paso que das te hace crecer!',
  imageUrl: '/images/ayacucho-plaza.jpg',
  imageAlt: 'Plaza Mayor de Ayacucho con sus tradicionales arquerías blancas y cielo andino',
  author: 'Pollinhhsano',
  source: 'Wikimedia Commons',
  license: 'CC BY-SA 4.0',
  licenseUrl: 'https://commons.wikimedia.org/wiki/File:PLAZA_MAYOR_DE_AYACUCHO.jpg',
  facts: [
    {
      id: 'fd000000-0000-4000-8000-000000000001',
      nodeSequence: 1,
      order: 1,
      title: 'Arquerías de piedra blanca',
      content:
        'La Plaza de Ayacucho es una de las más grandes del Perú y está rodeada por hermosos arcos de piedra blanca.',
      icon: 'landmark',
      source: 'MINCETUR - Guía de Turismo de Ayacucho',
    },
    {
      id: 'fd000000-0000-4000-8000-000000000002',
      nodeSequence: 2,
      order: 2,
      title: 'Cajas mágicas: retablos',
      content:
        'Los retablos ayacuchanos son coloridas cajas de madera que guardan pequeñas figuras modeladas y pintadas con amor.',
      icon: 'palette',
      source: 'Ministerio de Cultura del Perú - Patrimonio Cultural de la Nación',
    },
    {
      id: 'fd000000-0000-4000-8000-000000000003',
      nodeSequence: 3,
      order: 3,
      title: 'El rico pan chapla',
      content:
        'En los mercados de Ayacucho se hornea el pan chapla, un pancito esponjoso y calentito que se come con queso andino.',
      icon: 'store',
      source: 'PromPerú - Cocina Tradicional Ayacuchana',
    },
    {
      id: 'fd000000-0000-4000-8000-000000000004',
      nodeSequence: 4,
      order: 4,
      title: 'El trueque en la plaza',
      content:
        'Antes de usar monedas, las familias intercambiaban maíz, papitas y frutas como muestra de amistad y apoyo mutuo.',
      icon: 'coins',
      source: 'Banco Central de Reserva del Perú - Museo Central',
    },
    {
      id: 'fd000000-0000-4000-8000-000000000005',
      nodeSequence: 5,
      order: 5,
      title: 'Tejidos de Santa Ana',
      content:
        'Los artesanos tejen mantas con lanas teñidas con plantas naturales, creando formas geométricas llenas de historia.',
      icon: 'layers',
      source: 'Ministerio de Cultura - Arte Tradicional de Ayacucho',
    },
    {
      id: 'fd000000-0000-4000-8000-000000000006',
      nodeSequence: 6,
      order: 6,
      title: 'Ciudad de las iglesias',
      content:
        'Ayacucho tiene más de treinta templos históricos de piedra con torres altas que miran hacia las montañas.',
      icon: 'mountain',
      source: 'Municipalidad Provincial de Huamanga / MINCETUR',
    },
    {
      id: 'fd000000-0000-4000-8000-000000000007',
      nodeSequence: 7,
      order: 7,
      title: 'Piedra de Huamanga',
      content:
        'Es una piedra blanca y suave como la cera. Con ella se tallan figuras brillantes y nacimientos muy delicados.',
      icon: 'shapes',
      source: 'Ministerio de Cultura - Declaratoria Patrimonio Cultural de la Nación',
    },
    {
      id: 'fd000000-0000-4000-8000-000000000008',
      nodeSequence: 8,
      order: 8,
      title: 'Campanas cantarinas',
      content:
        'La Catedral de Ayacucho tiene campanas de bronce que tocan alegres melodías para avisar las fiestas del pueblo.',
      icon: 'bell',
      source: 'Arzobispado de Ayacucho',
    },
    {
      id: 'fd000000-0000-4000-8000-000000000009',
      nodeSequence: 9,
      order: 9,
      title: 'El santuario de Quinua',
      content:
        'A pocos kilómetros de la plaza está la Pampa de Ayacucho, un campo verde donde se selló la libertad del Perú.',
      icon: 'footprints',
      source: 'Sernanp - Santuario Histórico de la Pampa de Ayacucho',
    },
    {
      id: 'fd000000-0000-4000-8000-000000000010',
      nodeSequence: 10,
      order: 10,
      title: 'La fiesta de la cosecha',
      content:
        'En los campos andinos se cosechan cientos de tipos de papas nativas con colores divertidos como amarillo y morado.',
      icon: 'award',
      source: 'Centro Internacional de la Papa (CIP) / INIA',
    },
    {
      id: 'fd000000-0000-4000-8000-000000000011',
      nodeSequence: null,
      order: 11,
      title: 'Rupi, el gallito de las rocas',
      content:
        'El gallito de las rocas es el ave nacional del Perú. Sus plumas son de un rojo brillante y le gusta saltar entre los árboles.',
      icon: 'sparkles',
      source: 'SERFOR - Fauna Silvestre del Perú',
    },
  ],
}

export function RouteCompanion({ route }: Props) {
  // Identificar la parada actual (EN_CURSO o primera DISPONIBLE)
  const currentNode = useMemo(() => {
    return (
      route.nodes.find((node) => node.state === 'EN_CURSO') ??
      route.nodes.find((node) => node.state === 'DISPONIBLE')
    )
  }, [route.nodes])

  // Contexto cultural: recibido del backend o respaldo de Ayacucho si la aventura está ambientada allí
  const context: CulturalContext | null = useMemo(() => {
    if (route.culturalContext && route.culturalContext.facts?.length) {
      return route.culturalContext
    }
    const titleLower = route.title.toLowerCase()
    if (titleLower.includes('ayacucho') || titleLower.includes('plaza') || titleLower.includes('mat')) {
      return AYACUCHO_BACKUP_CONTEXT
    }
    return route.culturalContext ?? null
  }, [route.culturalContext, route.title])

  const facts: CulturalFact[] = useMemo(() => {
    return context?.facts ?? []
  }, [context])

  // Índice del dato curioso activo
  const [activeFactIndex, setActiveFactIndex] = useState(0)

  // Al cambiar de parada actual, priorizar automáticamente el dato vinculado si existe
  useEffect(() => {
    if (!facts.length || !currentNode) return
    const matchIdx = facts.findIndex((f) => f.nodeSequence === currentNode.sequence)
    if (matchIdx !== -1) {
      setActiveFactIndex(matchIdx)
    }
  }, [currentNode, facts])

  // Si no hay contexto cultural, mostrar solo la tarjeta de ánimo pedagógico de Rupi sin romper la pantalla
  if (!context || !facts.length) {
    return (
      <aside className="route-companion sticky-companion" aria-label="Acompañamiento de tu ruta">
        <section className="cultural-companion-card simple-motivational">
          <div className="companion-rupi-quote">
            <RupiCharacter mood="happy" className="companion-rupi-avatar" />
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
  const FactIcon = resolveFactIcon(currentFact.icon)
  const isLinkedToCurrent = currentNode && currentFact.nodeSequence === currentNode.sequence

  const handleNextFact = () => {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel()
    }
    setActiveFactIndex((prev) => (prev + 1) % facts.length)
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
          <RupiCharacter mood="happy" className="companion-rupi-avatar" />
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
