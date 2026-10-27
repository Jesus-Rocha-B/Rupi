import React, { useState, useEffect } from 'react'
import {
  X,
  Check,
  MapPin,
  Sparkles,
  Mountain,
  Sun,
  Trees,
  Compass,
} from 'lucide-react'
import {
  PERUVIAN_BACKGROUNDS,
  type MapBackgroundTheme,
} from '../utils/mapBackgrounds'
import './MapBackgroundModal.css'

interface MapBackgroundModalProps {
  isOpen: boolean
  onClose: () => void
  currentBgId: string
  onSelectBg: (bgId: string) => void
  routeTitle?: string
}

type FilterZone = 'todos' | 'sierra' | 'costa' | 'selva'

export const MapBackgroundModal: React.FC<MapBackgroundModalProps> = ({
  isOpen,
  onClose,
  currentBgId,
  onSelectBg,
  routeTitle,
}) => {
  const [selectedFilter, setSelectedFilter] = useState<FilterZone>('todos')
  const [selectedBgId, setSelectedBgId] = useState<string | null>(null)
  const previewBgId = selectedBgId ?? currentBgId

  const handleClose = () => {
    setSelectedBgId(null)
    onClose()
  }

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  if (!isOpen) return null

  const filteredBgs = PERUVIAN_BACKGROUNDS.filter((bg) => {
    if (selectedFilter === 'todos') return true
    return bg.zone === selectedFilter
  })

  const handleSelect = (bg: MapBackgroundTheme) => {
    setSelectedBgId(bg.id)
    onSelectBg(bg.id)
  }

  return (
    <div
      className="bg-modal-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="bg-modal-title"
      onClick={(e) => {
        if (e.target === e.currentTarget) handleClose()
      }}
    >
      <div className="bg-modal-container">
        {/* Encabezado del modal */}
        <header className="bg-modal-header">
          <div className="bg-modal-title-group">
            <span className="bg-modal-icon-badge" aria-hidden="true">
              <Compass size={24} />
            </span>
            <div>
              <h2 id="bg-modal-title" className="bg-modal-title">
                Paisajes del Perú
              </h2>
              <p className="bg-modal-subtitle">
                {routeTitle
                  ? `Personaliza el fondo para "${routeTitle}" o para todo tu mapa escolar.`
                  : 'Elige el escenario peruano donde recorrerás tus paradas de aprendizaje.'}
              </p>
            </div>
          </div>
          <button
            type="button"
            className="bg-modal-close-btn"
            onClick={handleClose}
            aria-label="Cerrar selector de paisajes"
            title="Cerrar (Esc)"
          >
            <X size={20} />
          </button>
        </header>

        {/* Barra de Filtros por Región Geográfica */}
        <div className="bg-modal-filters" role="tablist" aria-label="Filtrar paisajes por región geográfica">
          <button
            type="button"
            role="tab"
            aria-selected={selectedFilter === 'todos'}
            className={`bg-filter-pill ${selectedFilter === 'todos' ? 'active' : ''}`}
            onClick={() => setSelectedFilter('todos')}
          >
            <Sparkles size={15} aria-hidden="true" />
            <span>Todos los Paisajes ({PERUVIAN_BACKGROUNDS.length})</span>
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={selectedFilter === 'sierra'}
            className={`bg-filter-pill ${selectedFilter === 'sierra' ? 'active' : ''}`}
            onClick={() => setSelectedFilter('sierra')}
          >
            <Mountain size={15} aria-hidden="true" />
            <span>Sierra y Cordillera</span>
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={selectedFilter === 'costa'}
            className={`bg-filter-pill ${selectedFilter === 'costa' ? 'active' : ''}`}
            onClick={() => setSelectedFilter('costa')}
          >
            <Sun size={15} aria-hidden="true" />
            <span>Costa y Desierto</span>
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={selectedFilter === 'selva'}
            className={`bg-filter-pill ${selectedFilter === 'selva' ? 'active' : ''}`}
            onClick={() => setSelectedFilter('selva')}
          >
            <Trees size={15} aria-hidden="true" />
            <span>Selva Amazónica</span>
          </button>
        </div>

        {/* Catálogo de Paisajes en Cuadrícula */}
        <div className="bg-modal-grid" role="region" aria-label="Lista de paisajes disponibles">
          {filteredBgs.map((bg) => {
            const isSelected = bg.id === previewBgId
            return (
              <button
                key={bg.id}
                type="button"
                className={`bg-card ${isSelected ? 'is-selected' : ''}`}
                onClick={() => handleSelect(bg)}
                aria-pressed={isSelected}
              >
                <div
                  className="bg-card-preview"
                  style={{
                    backgroundColor: bg.bgColor,
                    backgroundImage: `${bg.gradient}, url('${bg.imageUrl}')`,
                  }}
                >
                  <span className="bg-card-region-badge">
                    <MapPin size={11} aria-hidden="true" />
                    <span>{bg.region}</span>
                  </span>
                  {isSelected && (
                    <span className="bg-card-active-badge" title="Paisaje actualmente aplicado">
                      <Check size={14} aria-hidden="true" />
                      <span>Activo</span>
                    </span>
                  )}
                </div>

                <div className="bg-card-body">
                  <div className="bg-card-header-row">
                    <h3 className="bg-card-title">{bg.name}</h3>
                    <span className={`bg-zone-pill zone-${bg.zone}`}>
                      {bg.zone === 'sierra' ? 'Sierra' : bg.zone === 'costa' ? 'Costa' : 'Selva'}
                    </span>
                  </div>
                  <p className="bg-card-tagline">{bg.tagline}</p>
                  <p className="bg-card-desc">{bg.description}</p>
                </div>
              </button>
            )
          })}
        </div>

        {/* Pie del modal con confirmación y cierre */}
        <footer className="bg-modal-footer">
          <div className="bg-modal-footer-info">
            <span className="bg-footer-tip">
              💡 <strong>Tip para el aula:</strong> Los fondos han sido diseñados con contraste suave para que el sendero y las paradas se lean con total claridad.
            </span>
          </div>
          <button
            type="button"
            className="bg-modal-done-btn"
            onClick={onClose}
          >
            <Check size={18} aria-hidden="true" />
            <span>Aplicar Paisaje y Salir</span>
          </button>
        </footer>
      </div>
    </div>
  )
}
