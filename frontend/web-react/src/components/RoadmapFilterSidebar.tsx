import { useEffect, useState } from 'react'
import {
  ArrowRight,
  BookOpen,
  Calculator,
  Check,
  ChevronDown,
  ChevronUp,
  Compass,
  Filter,
  FlaskConical,
  GraduationCap,
  Palette,
  RotateCcw,
  Sparkles,
  Volume2,
  X,
} from 'lucide-react'
import {
  searchAlternativeRoutes,
  type RouteSearchResult,
} from '../services/learningRoutesApi'
import { RupiCharacter } from './RupiCharacter'
import { speakText } from '../utils/speech'
import './RoadmapFilterSidebar.css'

export interface FilterState {
  grades: number[]
  areas: string[]
  query: string
}

interface Props {
  currentGrade?: number | null
  currentCourse?: string | null
  onSelectRoute: (versionRouteId: string, gradeNumber: number, areaCode: string) => void
  isOpen?: boolean
  onToggleOpen?: () => void
}

const AVAILABLE_GRADES = [
  { number: 1, label: '1.° de primaria', shortLabel: '1.º' },
  { number: 2, label: '2.° de primaria', shortLabel: '2.º' },
  { number: 3, label: '3.° de primaria', shortLabel: '3.º' },
  { number: 4, label: '4.° de primaria', shortLabel: '4.º' },
  { number: 5, label: '5.° de primaria', shortLabel: '5.º' },
  { number: 6, label: '6.° de primaria', shortLabel: '6.º' },
]

const AVAILABLE_AREAS = [
  { code: 'MATEMATICA', alias: 'MAT', name: 'Matemática', icon: Calculator, colorClass: 'mat' },
  { code: 'COMUNICACION', alias: 'COM', name: 'Comunicación', icon: BookOpen, colorClass: 'com' },
  { code: 'CYT', alias: 'CYT', name: 'Ciencia y Tecnología', icon: FlaskConical, colorClass: 'cyt' },
  { code: 'SOC', alias: 'SOC', name: 'Personal Social', icon: Compass, colorClass: 'soc' },
  { code: 'ART', alias: 'ART', name: 'Arte y Cultura', icon: Palette, colorClass: 'art' },
]

/**
 * Lee los filtros iniciales desde la URL (query parameters)
 */
function readFiltersFromUrl(defaultGrade?: number | null, defaultCourse?: string | null): FilterState {
  if (typeof window === 'undefined') {
    return {
      grades: defaultGrade ? [defaultGrade] : [],
      areas: defaultCourse ? [defaultCourse] : [],
      query: '',
    }
  }

  const params = new URLSearchParams(window.location.search)

  // Grados: soporta ?grades=3,4 o múltiples ?grades=3&grades=4 o ?grade=3
  const gradesSet = new Set<number>()
  const gradesParam = params.getAll('grades')
  const gradeParam = params.getAll('grade')

  const allGradeValues = [...gradesParam, ...gradeParam]
  allGradeValues.forEach((val) => {
    val.split(',').forEach((numStr) => {
      const parsed = parseInt(numStr.trim(), 10)
      if (!isNaN(parsed) && parsed >= 1 && parsed <= 6) {
        gradesSet.add(parsed)
      }
    })
  })

  // Materias/Áreas: soporta ?areas=MAT,COM o ?area=MATEMATICA
  const areasSet = new Set<string>()
  const areasParam = params.getAll('areas')
  const areaParam = params.getAll('area')
  const allAreaValues = [...areasParam, ...areaParam]
  allAreaValues.forEach((val) => {
    val.split(',').forEach((areaStr) => {
      const cleaned = areaStr.trim().toUpperCase()
      if (cleaned) {
        areasSet.add(cleaned)
      }
    })
  })

  const q = params.get('q') || ''

  // Si no hay filtros en URL, usar los defaults
  if (gradesSet.size === 0 && defaultGrade) {
    gradesSet.add(defaultGrade)
  }
  if (areasSet.size === 0 && defaultCourse) {
    areasSet.add(defaultCourse.toUpperCase())
  }

  return {
    grades: Array.from(gradesSet).sort((a, b) => a - b),
    areas: Array.from(areasSet),
    query: q,
  }
}

/**
 * Sincroniza el estado de los filtros con la URL sin recargar la página
 */
function writeFiltersToUrl(filters: FilterState) {
  if (typeof window === 'undefined') return
  const url = new URL(window.location.href)
  const params = url.searchParams

  // Limpiar llaves previas
  params.delete('grades')
  params.delete('grade')
  params.delete('areas')
  params.delete('area')
  params.delete('q')

  if (filters.grades.length > 0) {
    params.set('grades', filters.grades.join(','))
  }
  if (filters.areas.length > 0) {
    params.set('areas', filters.areas.join(','))
  }
  if (filters.query.trim()) {
    params.set('q', filters.query.trim())
  }

  const newSearch = params.toString() ? `?${params.toString()}` : ''
  const newUrl = `${url.pathname}${newSearch}${url.hash}`
  window.history.replaceState(null, '', newUrl)
}

export function RoadmapFilterSidebar({
  currentGrade,
  currentCourse,
  onSelectRoute,
  isOpen: controlledIsOpen,
  onToggleOpen,
}: Props) {
  const [internalIsOpen, setInternalIsOpen] = useState(false)
  const isPanelOpen = controlledIsOpen !== undefined ? controlledIsOpen : internalIsOpen

  const [filters, setFilters] = useState<FilterState>(() =>
    readFiltersFromUrl(currentGrade, currentCourse)
  )
  const [results, setResults] = useState<RouteSearchResult[]>([])
  const [loading, setLoading] = useState(false)
  const [totalFound, setTotalFound] = useState(0)

  const [gradesExpanded, setGradesExpanded] = useState(true)
  const [areasExpanded, setAreasExpanded] = useState(true)

  // Disparar búsqueda y sincronizar URL al actualizar filtros
  useEffect(() => {
    writeFiltersToUrl(filters)
    let isCancelled = false
    const controller = new AbortController()

    searchAlternativeRoutes(
      {
        query: filters.query,
        grades: filters.grades,
        areas: filters.areas,
        limit: 30,
      },
      null,
      30,
      controller.signal
    )
      .then((response) => {
        if (!isCancelled) {
          setResults(response.routes || [])
          setTotalFound(response.totalResults ?? response.routes?.length ?? 0)
          setLoading(false)
        }
      })
      .catch(() => {
        if (!isCancelled) {
          setResults([])
          setTotalFound(0)
          setLoading(false)
        }
      })

    return () => {
      isCancelled = true
      controller.abort()
    }
  }, [filters])

  // Manejador para toggle de grado
  function toggleGrade(gradeNum: number) {
    setFilters((prev) => {
      const exists = prev.grades.includes(gradeNum)
      const newGrades = exists
        ? prev.grades.filter((g) => g !== gradeNum)
        : [...prev.grades, gradeNum].sort((a, b) => a - b)
      return { ...prev, grades: newGrades }
    })
  }

  // Manejador para seleccionar todos los grados
  function selectAllGrades() {
    setFilters((prev) => ({
      ...prev,
      grades: AVAILABLE_GRADES.map((g) => g.number),
    }))
  }

  // Manejador para toggle de área / materia
  function toggleArea(areaCode: string) {
    setFilters((prev) => {
      const exists = prev.areas.includes(areaCode)
      const newAreas = exists
        ? prev.areas.filter((a) => a !== areaCode)
        : [...prev.areas, areaCode]
      return { ...prev, areas: newAreas }
    })
  }

  // Limpiar todos los filtros
  function resetAllFilters() {
    const defaultState: FilterState = {
      grades: currentGrade ? [currentGrade] : [],
      areas: currentCourse ? [currentCourse.toUpperCase()] : [],
      query: '',
    }
    setFilters(defaultState)
  }

  const activeFiltersCount =
    filters.grades.length +
    filters.areas.length +
    (filters.query.trim() ? 1 : 0)

  function speakFilterSummary() {
    const gradesStr = filters.grades.length
      ? `grados ${filters.grades.join(', ')} de primaria`
      : 'todos los grados'
    const areasStr = filters.areas.length
      ? filters.areas.join(' y ')
      : 'todas las materias'
    speakText(
      `Filtros activos: ${gradesStr}, en ${areasStr}. Hay ${totalFound} rutas disponibles.`
    )
  }

  function handlePanelToggle() {
    if (onToggleOpen) {
      onToggleOpen()
    } else {
      setInternalIsOpen((prev) => !prev)
    }
  }

  return (
    <aside
      className={`roadmap-filter-sidebar ${isPanelOpen ? 'expanded' : 'collapsed'}`}
      aria-label="Filtros escolares por grado y materia"
    >
      {/* Botón flotante / trigger para abrir/cerrar filtros */}
      <div className="filter-sidebar-header-bar">
        <button
          type="button"
          className="filter-toggle-btn"
          onClick={handlePanelToggle}
          aria-expanded={isPanelOpen}
          aria-controls="filter-panel-content"
          title={isPanelOpen ? 'Ocultar panel de filtros' : 'Abrir filtros por grado y materia'}
        >
          <Filter size={18} aria-hidden="true" />
          <span className="filter-btn-title">Filtros por Grado y Materia</span>
          {activeFiltersCount > 0 && (
            <span className="filter-count-badge" aria-label={`${activeFiltersCount} filtros activos`}>
              {activeFiltersCount}
            </span>
          )}
          {isPanelOpen ? (
            <ChevronUp size={16} aria-hidden="true" />
          ) : (
            <ChevronDown size={16} aria-hidden="true" />
          )}
        </button>

        {activeFiltersCount > 0 && (
          <button
            type="button"
            className="filter-reset-quick-btn"
            onClick={resetAllFilters}
            title="Restablecer filtros"
            aria-label="Restablecer todos los filtros"
          >
            <RotateCcw size={14} aria-hidden="true" />
            <span>Restablecer</span>
          </button>
        )}
      </div>

      {/* Contenido desplegable del panel */}
      {isPanelOpen && (
        <div id="filter-panel-content" className="filter-panel-body" role="region" aria-label="Opciones de filtrado">
          {/* Cabecera del panel con Rupi */}
          <div className="filter-panel-intro">
            <div className="filter-rupi-mini" aria-hidden="true">
              <RupiCharacter role="mathematician" mood="thinking" />
            </div>
            <div className="filter-intro-text">
              <h2 className="filter-section-main-title">Ajusta tu ruta de aprendizaje</h2>
              <p className="filter-intro-desc">
                Filtra por tu grado escolar para encontrar desafíos a tu nivel.
              </p>
            </div>
            <button
              type="button"
              className="filter-audio-btn"
              onClick={speakFilterSummary}
              aria-label="Escuchar resumen de filtros"
              title="Escuchar resumen de filtros"
            >
              <Volume2 size={16} aria-hidden="true" />
            </button>
          </div>

          {/* Chips de filtros activos */}
          {activeFiltersCount > 0 && (
            <div className="active-chips-container" aria-label="Filtros aplicados actualmente">
              <span className="active-chips-label">Aplicados:</span>
              <div className="active-chips-wrap">
                {filters.grades.map((gradeNum) => (
                  <span key={`chip-grade-${gradeNum}`} className="filter-chip grade-chip">
                    <GraduationCap size={13} aria-hidden="true" />
                    <span>{gradeNum}.° primaria</span>
                    <button
                      type="button"
                      onClick={() => toggleGrade(gradeNum)}
                      aria-label={`Quitar filtro ${gradeNum}.° de primaria`}
                    >
                      <X size={12} aria-hidden="true" />
                    </button>
                  </span>
                ))}

                {filters.areas.map((areaCode) => {
                  const areaInfo = AVAILABLE_AREAS.find(
                    (a) => a.code === areaCode || a.alias === areaCode
                  )
                  return (
                    <span key={`chip-area-${areaCode}`} className="filter-chip area-chip">
                      <Sparkles size={13} aria-hidden="true" />
                      <span>{areaInfo ? areaInfo.name : areaCode}</span>
                      <button
                        type="button"
                        onClick={() => toggleArea(areaCode)}
                        aria-label={`Quitar filtro ${areaInfo ? areaInfo.name : areaCode}`}
                      >
                        <X size={12} aria-hidden="true" />
                      </button>
                    </span>
                  )
                })}

                {filters.query.trim() && (
                  <span className="filter-chip query-chip">
                    <span>"{filters.query.trim()}"</span>
                    <button
                      type="button"
                      onClick={() => setFilters((prev) => ({ ...prev, query: '' }))}
                      aria-label="Quitar filtro de texto de búsqueda"
                    >
                      <X size={12} aria-hidden="true" />
                    </button>
                  </span>
                )}
              </div>
            </div>
          )}

          {/* SECCIÓN 1: Filtros de Grado Escolar (1.° a 6.° de primaria) */}
          <fieldset className="filter-fieldset">
            <legend className="filter-legend">
              <button
                type="button"
                className="filter-section-toggle"
                onClick={() => setGradesExpanded((prev) => !prev)}
                aria-expanded={gradesExpanded}
              >
                <div className="legend-left">
                  <GraduationCap size={18} aria-hidden="true" />
                  <span className="legend-title">Grado escolar</span>
                  {filters.grades.length > 0 && (
                    <span className="legend-count-pill">{filters.grades.length}</span>
                  )}
                </div>
                {gradesExpanded ? <ChevronUp size={16} aria-hidden="true" /> : <ChevronDown size={16} aria-hidden="true" />}
              </button>
            </legend>

            {gradesExpanded && (
              <div className="filter-group-content">
                <div className="grades-quick-actions">
                  <div className="grade-dropdown-box">
                    <label htmlFor="grade-select-dropdown" className="grade-dropdown-label">
                      Filtro rápido (desplegable):
                    </label>
                    <select
                      id="grade-select-dropdown"
                      className="grade-quick-select"
                      value={filters.grades.length === 1 ? filters.grades[0] : filters.grades.length === 0 ? '' : 'MULTIPLE'}
                      onChange={(e) => {
                        const val = e.target.value
                        if (val === '') {
                          setFilters((p) => ({ ...p, grades: [] }))
                        } else if (val === 'ALL') {
                          selectAllGrades()
                        } else if (val !== 'MULTIPLE') {
                          const num = parseInt(val, 10)
                          if (!isNaN(num)) {
                            setFilters((p) => ({ ...p, grades: [num] }))
                          }
                        }
                      }}
                    >
                      <option value="">Todos los grados (sin filtro)</option>
                      <option value="ALL">Marcar todos (1.° a 6.°)</option>
                      {filters.grades.length > 1 && (
                        <option value="MULTIPLE" disabled>
                          Varios grados elegidos ({filters.grades.map(n => `${n}.°`).join(', ')})
                        </option>
                      )}
                      {AVAILABLE_GRADES.map((g) => (
                        <option key={g.number} value={g.number}>
                          {g.label}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="grades-buttons-row">
                    <button
                      type="button"
                      className="select-all-btn"
                      onClick={selectAllGrades}
                      title="Marcar de 1.° a 6.° de primaria"
                    >
                      <span>Seleccionar todos (1.° a 6.°)</span>
                    </button>
                    {filters.grades.length > 0 && (
                      <button
                        type="button"
                        className="clear-group-btn"
                        onClick={() => setFilters((p) => ({ ...p, grades: [] }))}
                        title="Desmarcar grados"
                      >
                        <span>Limpiar</span>
                      </button>
                    )}
                  </div>
                </div>

                <div className="grades-checkboxes-grid" role="group" aria-label="Lista de grados de primaria">
                  {AVAILABLE_GRADES.map((g) => {
                    const isChecked = filters.grades.includes(g.number)
                    const checkboxId = `filter-grade-${g.number}`
                    return (
                      <label
                        key={g.number}
                        htmlFor={checkboxId}
                        className={`grade-checkbox-card ${isChecked ? 'selected' : ''}`}
                      >
                        <input
                          type="checkbox"
                          id={checkboxId}
                          checked={isChecked}
                          onChange={() => toggleGrade(g.number)}
                          className="sr-checkbox"
                          aria-label={g.label}
                        />
                        <div className="grade-box-visual" aria-hidden="true">
                          {isChecked && <Check size={14} className="check-icon" />}
                        </div>
                        <div className="grade-label-info">
                          <span className="grade-number-badge">{g.shortLabel}</span>
                          <span className="grade-full-name">{g.label}</span>
                        </div>
                      </label>
                    )
                  })}
                </div>
              </div>
            )}
          </fieldset>

          {/* SECCIÓN 2: Filtros de Materias / Áreas Curriculares */}
          <fieldset className="filter-fieldset">
            <legend className="filter-legend">
              <button
                type="button"
                className="filter-section-toggle"
                onClick={() => setAreasExpanded((prev) => !prev)}
                aria-expanded={areasExpanded}
              >
                <div className="legend-left">
                  <BookOpen size={18} aria-hidden="true" />
                  <span className="legend-title">Materia o curso</span>
                  {filters.areas.length > 0 && (
                    <span className="legend-count-pill">{filters.areas.length}</span>
                  )}
                </div>
                {areasExpanded ? <ChevronUp size={16} aria-hidden="true" /> : <ChevronDown size={16} aria-hidden="true" />}
              </button>
            </legend>

            {areasExpanded && (
              <div className="filter-group-content">
                <div className="areas-checkboxes-list" role="group" aria-label="Lista de materias escolares">
                  {AVAILABLE_AREAS.map((area) => {
                    const isChecked =
                      filters.areas.includes(area.code) || filters.areas.includes(area.alias)
                    const checkboxId = `filter-area-${area.code}`
                    const AreaIcon = area.icon

                    return (
                      <label
                        key={area.code}
                        htmlFor={checkboxId}
                        className={`area-checkbox-card ${area.colorClass} ${isChecked ? 'selected' : ''}`}
                      >
                        <input
                          type="checkbox"
                          id={checkboxId}
                          checked={isChecked}
                          onChange={() => toggleArea(area.code)}
                          className="sr-checkbox"
                          aria-label={`Materia de ${area.name}`}
                        />
                        <div className="area-box-visual" aria-hidden="true">
                          {isChecked && <Check size={14} className="check-icon" />}
                        </div>
                        <div className="area-icon-holder" aria-hidden="true">
                          <AreaIcon size={16} />
                        </div>
                        <span className="area-title-text">{area.name}</span>
                      </label>
                    )
                  })}
                </div>
              </div>
            )}
          </fieldset>

          {/* SECCIÓN 3: Resultados filtrados en tiempo real */}
          <section className="filter-results-section" aria-labelledby="filter-results-heading">
            <div className="filter-results-header">
              <h3 id="filter-results-heading" className="filter-results-title">
                Rutas adaptadas a tu nivel ({totalFound})
              </h3>
              {loading && <span className="results-loading-indicator">Buscando...</span>}
            </div>

            {results.length === 0 && !loading && (
              <div className="filter-empty-state">
                <p>No se encontraron rutas con los filtros seleccionados.</p>
                <button
                  type="button"
                  className="filter-reset-action-btn"
                  onClick={resetAllFilters}
                >
                  Restablecer filtros
                </button>
              </div>
            )}

            <div className="filtered-routes-list" role="list">
              {results.map((route) => (
                <div
                  key={route.versionRouteId}
                  role="listitem"
                  className="filtered-route-card"
                  onClick={() => onSelectRoute(route.versionRouteId, route.gradeNumber, route.areaCode)}
                >
                  <div className="route-card-meta-row">
                    <span className="route-grade-pill">{route.gradeName}</span>
                    <span className={`route-area-pill ${route.areaCode.toLowerCase()}`}>
                      {route.areaName}
                    </span>
                    {route.isEnrolled && (
                      <span className="route-enrolled-pill" title="Ya estás inscrito en esta ruta">
                        Inscrito
                      </span>
                    )}
                  </div>

                  <h4 className="filtered-route-title">{route.routeTitle}</h4>
                  {route.description && (
                    <p className="filtered-route-desc">{route.description}</p>
                  )}

                  <div className="filtered-route-footer">
                    <span className="filtered-route-nodes">
                      <Sparkles size={13} aria-hidden="true" />
                      <span>{route.totalNodes} paradas</span>
                    </span>

                    <button
                      type="button"
                      className="filtered-route-select-btn"
                      onClick={(e) => {
                        e.stopPropagation()
                        onSelectRoute(route.versionRouteId, route.gradeNumber, route.areaCode)
                      }}
                      aria-label={`Explorar ${route.routeTitle}`}
                    >
                      <span>Abrir ruta</span>
                      <ArrowRight size={14} aria-hidden="true" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>
      )}
    </aside>
  )
}
