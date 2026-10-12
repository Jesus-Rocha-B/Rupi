import { useEffect, useRef, useState } from 'react'
import {
  BookOpen,
  Calculator,
  Compass,
  FlaskConical,
  GraduationCap,
  Layers,
  Palette,
  Search,
  Sparkles,
  Volume2,
  X,
} from 'lucide-react'
import {
  fetchSubjectSuggestions,
  searchAlternativeRoutes,
  type RouteSearchResult,
} from '../services/learningRoutesApi'
import { RupiCharacter } from './RupiCharacter'
import { speakText } from '../utils/speech'
import './RoadmapSearchBar.css'

interface Props {
  currentGrade?: number | null
  onSelectRoute?: (versionRouteId: string, areaCode?: string) => void
  onSelectSubject?: (subjectCode: string) => void
}

function getSubjectIcon(areaCode: string) {
  const code = areaCode.toUpperCase()
  if (code.includes('MAT')) return <Calculator size={16} aria-hidden="true" />
  if (code.includes('COM')) return <BookOpen size={16} aria-hidden="true" />
  if (code.includes('CYT')) return <FlaskConical size={16} aria-hidden="true" />
  if (code.includes('ART')) return <Palette size={16} aria-hidden="true" />
  return <Compass size={16} aria-hidden="true" />
}

function getSubjectBadgeClass(areaCode: string) {
  const code = areaCode.toUpperCase()
  if (code.includes('MAT')) return 'subject-pill mat'
  if (code.includes('COM')) return 'subject-pill com'
  if (code.includes('CYT')) return 'subject-pill cyt'
  if (code.includes('ART')) return 'subject-pill art'
  return 'subject-pill soc'
}

export function RoadmapSearchBar({ currentGrade, onSelectRoute }: Props) {
  const [query, setQuery] = useState('')
  const [suggestions, setSuggestions] = useState<string[]>([])
  const [results, setResults] = useState<RouteSearchResult[]>([])
  const [isOpen, setIsOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [hasSearched, setHasSearched] = useState(false)
  const [selectedSuggestionIndex, setSelectedSuggestionIndex] = useState(-1)
  const containerRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  // Cerrar al hacer clic fuera
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  // Atajo de teclado: presionar '/' para enfocar el buscador
  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (
        (event.key === '/' || (event.key === 'k' && (event.ctrlKey || event.metaKey))) &&
        document.activeElement?.tagName !== 'INPUT' &&
        document.activeElement?.tagName !== 'TEXTAREA'
      ) {
        event.preventDefault()
        inputRef.current?.focus()
        setIsOpen(true)
      }
      if (event.key === 'Escape' && isOpen) {
        setIsOpen(false)
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen])

  // Obtener sugerencias de autocompletado en tiempo real con debounce
  useEffect(() => {
    const trimmed = query.trim()
    if (!trimmed) {
      return
    }

    const timer = setTimeout(async () => {
      try {
        const fetched = await fetchSubjectSuggestions(trimmed, 6)
        setSuggestions(fetched)
      } catch {
        // En caso de fallo de red, se mantiene silencioso
      }
    }, 200)

    return () => clearTimeout(timer)
  }, [query])

  // Ejecutar búsqueda completa de roadmaps alternativos
  async function performSearch(searchTerm: string) {
    const trimmed = searchTerm.trim()
    setLoading(true)
    setHasSearched(true)
    setIsOpen(true)
    try {
      const response = await searchAlternativeRoutes(trimmed, currentGrade ?? undefined, 20)
      setResults(response.routes)
      if (response.suggestedSubjects && response.suggestedSubjects.length > 0) {
        setSuggestions(response.suggestedSubjects)
      }
    } catch {
      setResults([])
    } finally {
      setLoading(false)
    }
  }

  function handleInputChange(value: string) {
    setQuery(value)
    setSelectedSuggestionIndex(-1)
    if (!isOpen) setIsOpen(true)
    if (value.trim().length === 0) {
      setSuggestions([])
      setResults([])
      setHasSearched(false)
    } else if (value.trim().length >= 2) {
      void performSearch(value)
    }
  }

  function handleSelectSuggestion(subject: string) {
    setQuery(subject)
    setSelectedSuggestionIndex(-1)
    void performSearch(subject)
  }

  function handleClear() {
    setQuery('')
    setSuggestions([])
    setResults([])
    setHasSearched(false)
    setSelectedSuggestionIndex(-1)
    inputRef.current?.focus()
  }

  function handleKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      if (suggestions.length > 0) {
        setSelectedSuggestionIndex((prev) => (prev + 1) % suggestions.length)
      }
    } else if (event.key === 'ArrowUp') {
      event.preventDefault()
      if (suggestions.length > 0) {
        setSelectedSuggestionIndex((prev) => (prev - 1 + suggestions.length) % suggestions.length)
      }
    } else if (event.key === 'Enter') {
      event.preventDefault()
      if (selectedSuggestionIndex >= 0 && selectedSuggestionIndex < suggestions.length) {
        handleSelectSuggestion(suggestions[selectedSuggestionIndex])
      } else {
        void performSearch(query)
      }
    } else if (event.key === 'Escape') {
      setIsOpen(false)
    }
  }

  const voiceAnnouncement = results.length > 0
    ? `Encontré ${results.length} roadmaps escolares para ${query || 'tu búsqueda'}.`
    : `No se encontraron roadmaps para esa materia. Puedes buscar Matemática, Comunicación o Ciencia.`

  return (
    <div className="roadmap-search-container" ref={containerRef} role="search">
      <div className="roadmap-search-bar-wrapper">
        <label htmlFor="roadmap-search-input" className="sr-only">
          Buscar roadmaps alternativos por materia escolar
        </label>
        <div className="roadmap-search-input-box">
          <Search size={18} className="search-box-icon" aria-hidden="true" />
          <input
            id="roadmap-search-input"
            ref={inputRef}
            type="text"
            className="roadmap-search-input"
            value={query}
            onChange={(e) => handleInputChange(e.target.value)}
            onFocus={() => setIsOpen(true)}
            onKeyDown={handleKeyDown}
            placeholder="Buscar roadmaps por materia (ej. Comunicación, Ciencia, Matemática)..."
            autoComplete="off"
            aria-autocomplete="list"
            aria-expanded={isOpen}
            aria-controls="search-dropdown-panel"
          />
          {query && (
            <button
              type="button"
              className="search-clear-btn"
              onClick={handleClear}
              aria-label="Borrar búsqueda"
              title="Borrar texto"
            >
              <X size={16} aria-hidden="true" />
            </button>
          )}
          <kbd className="search-shortcut-badge" title="Presiona la tecla / para buscar">
            /
          </kbd>
        </div>

        <button
          type="button"
          className="search-submit-btn"
          onClick={() => void performSearch(query)}
          aria-label="Buscar roadmaps alternativos"
        >
          <Search size={16} aria-hidden="true" />
          <span>Buscar</span>
        </button>
      </div>

      {/* Menú desplegable con autocompletado y resultados de roadmaps alternativos */}
      {isOpen && (
        <div
          id="search-dropdown-panel"
          className="search-dropdown-panel"
          role="region"
          aria-label="Resultados de búsqueda y materias sugeridas"
        >
          {/* Fila de sugerencias rápidas de materias (Autocompletado) */}
          <div className="suggestions-bar">
            <span className="suggestions-label">
              <Sparkles size={13} aria-hidden="true" />
              <b>Materias rápidas:</b>
            </span>
            <div className="suggestions-chip-list" role="listbox" aria-label="Materias sugeridas">
              {['Matemática', 'Comunicación', 'Ciencia y Tecnología', 'Personal Social', 'Arte y Cultura'].map(
                (subject, index) => {
                  const isSelected = selectedSuggestionIndex === index || query.toLowerCase() === subject.toLowerCase()
                  return (
                    <button
                      key={subject}
                      type="button"
                      className={`suggestion-chip ${isSelected ? 'selected' : ''}`}
                      onClick={() => handleSelectSuggestion(subject)}
                    >
                      {getSubjectIcon(subject)}
                      <span>{subject}</span>
                    </button>
                  )
                }
              )}
            </div>
          </div>

          {/* Estado de carga */}
          {loading && (
            <div className="search-loading-state" role="status">
              <div className="search-spinner" aria-hidden="true" />
              <span>Buscando roadmaps de materias con Rupi...</span>
            </div>
          )}

          {/* Resultados de Roadmaps Alternativos */}
          {!loading && hasSearched && results.length > 0 && (
            <div className="search-results-section">
              <div className="results-header-row">
                <span className="results-count-title">
                  <Layers size={15} aria-hidden="true" />
                  <span>Roadmaps alternativos encontrados ({results.length}):</span>
                </span>
                <button
                  type="button"
                  className="results-voice-btn"
                  onClick={() => speakText(voiceAnnouncement)}
                  title="Escuchar resumen con Rupi"
                  aria-label="Escuchar resumen de resultados"
                >
                  <Volume2 size={15} aria-hidden="true" />
                  <span>Escuchar</span>
                </button>
              </div>

              <div className="roadmap-results-grid">
                {results.map((route) => (
                  <article
                    key={route.versionRouteId}
                    className={`roadmap-result-card ${route.isEnrolled ? 'enrolled' : ''}`}
                  >
                    <div className="result-card-header">
                      <span className={getSubjectBadgeClass(route.areaCode)}>
                        {getSubjectIcon(route.areaCode)}
                        <span>{route.areaName}</span>
                      </span>
                      <span className="result-grade-tag">
                        <GraduationCap size={13} aria-hidden="true" />
                        <span>{route.gradeName}</span>
                      </span>
                    </div>

                    <h4 className="result-card-title">{route.routeTitle}</h4>
                    {route.description && (
                      <p className="result-card-description">{route.description}</p>
                    )}

                    <div className="result-card-footer">
                      <span className="result-nodes-pill">
                        <Compass size={13} aria-hidden="true" />
                        <span>{route.totalNodes} paradas</span>
                      </span>

                      {route.isEnrolled ? (
                        <div className="enrolled-status-box">
                          <span className="enrolled-indicator">✓ Tu ruta activa</span>
                          {onSelectRoute && (
                            <button
                              type="button"
                              className="explore-route-btn active"
                              onClick={() => {
                                onSelectRoute(route.versionRouteId, route.areaCode)
                                setIsOpen(false)
                              }}
                            >
                              Ver mi mapa
                            </button>
                          )}
                        </div>
                      ) : (
                        <div className="alternative-status-box">
                          <span className="alternative-indicator">Ruta alternativa</span>
                          {onSelectRoute && (
                            <button
                              type="button"
                              className="explore-route-btn alternative"
                              onClick={() => {
                                onSelectRoute(route.versionRouteId, route.areaCode)
                                setIsOpen(false)
                              }}
                              title="Explorar el contenido de este roadmap"
                            >
                              Explorar
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  </article>
                ))}
              </div>
            </div>
          )}

          {/* Estado sin resultados */}
          {!loading && hasSearched && results.length === 0 && (
            <div className="search-empty-state" role="status">
              <div className="empty-mascot-col" aria-hidden="true">
                <RupiCharacter role="mathematician" mood="thinking" className="empty-rupi-svg" />
              </div>
              <div className="empty-text-col">
                <h4>No encontramos roadmaps para «{query}»</h4>
                <p>
                  Prueba buscando por materias principales como <b>Matemática</b>, <b>Comunicación</b> o{' '}
                  <b>Ciencia y Tecnología</b>.
                </p>
                <div className="empty-quick-buttons">
                  <button
                    type="button"
                    className="empty-suggestion-btn"
                    onClick={() => handleSelectSuggestion('Comunicación')}
                  >
                    Ver Comunicación
                  </button>
                  <button
                    type="button"
                    className="empty-suggestion-btn"
                    onClick={() => handleSelectSuggestion('Ciencia')}
                  >
                    Ver Ciencia
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Pie del buscador */}
          <div className="search-panel-footer">
            <small>
              Presiona <kbd>Esc</kbd> para cerrar o haz clic en cualquier resultado para explorar.
            </small>
            <button type="button" className="close-panel-btn" onClick={() => setIsOpen(false)}>
              Cerrar
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
