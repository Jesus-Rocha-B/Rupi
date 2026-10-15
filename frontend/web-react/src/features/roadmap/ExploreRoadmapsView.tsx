import { useState, useEffect, useMemo } from 'react'
import {
  BookOpen,
  Calculator,
  Compass,
  GraduationCap,
  KeyRound,
  Search,
  Users,
  FlaskConical,
  Filter,
  RefreshCw,
  CheckCircle2,
  AlertCircle,
  Sparkles,
} from 'lucide-react'
import {
  searchAlternativeRoutes,
  enrollInRoute,
  joinRouteByCode,
  type RouteSearchResult,
  type RouteSummary,
} from '../../services/learningRoutesApi'
import { RupiCharacter } from '../../components/RupiCharacter'
import { RoadmapCard } from '../../components/RoadmapCard'
import './ExploreRoadmapsView.css'

interface ExploreRoadmapsViewProps {
  currentGrade: number | null
  currentCourse: string | null
  enrolledRoutes: RouteSummary[]
  onSelectRoute: (versionRouteId: string, gradeNumber?: number, areaCode?: string) => void
  onOpenJoinModal: () => void
}

type CategoryTab = 'OFICIAL' | 'COMUNIDAD'

export function ExploreRoadmapsView({
  currentGrade: _currentGrade,
  currentCourse: _currentCourse,
  enrolledRoutes,
  onSelectRoute,
  onOpenJoinModal,
}: ExploreRoadmapsViewProps) {
  const [activeTab, setActiveTab] = useState<CategoryTab>('OFICIAL')
  const [searchQuery, setSearchQuery] = useState('')
  const [filterGrade, setFilterGrade] = useState<number | null>(null)
  const [filterArea, setFilterArea] = useState<string>('TODAS')
  const [results, setResults] = useState<RouteSearchResult[]>([])
  const [loading, setLoading] = useState(true)
  const [enrollingId, setEnrollingId] = useState<string | null>(null)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  // Estado para unirse a rutas con código de profesor directamente desde Comunidad
  const [inlineCode, setInlineCode] = useState('')
  const [inlineJoining, setInlineJoining] = useState(false)
  const [inlineError, setInlineError] = useState<string | null>(null)
  const [inlineSuccess, setInlineSuccess] = useState<string | null>(null)

  // Mapa de rutas inscritas para búsqueda O(1) de progreso
  const enrolledMap = useMemo(() => {
    const map = new Map<string, RouteSummary>()
    for (const r of enrolledRoutes) {
      map.set(r.versionRouteId, r)
    }
    return map
  }, [enrolledRoutes])

  // Cargar catálogo de rutas según filtros
  useEffect(() => {
    let isCancelled = false
    const controller = new AbortController()

    async function fetchCatalog() {
      setLoading(true)
      setErrorMsg(null)
      try {
        const areaParam = filterArea === 'TODAS' ? undefined : filterArea
        const res = await searchAlternativeRoutes(
          {
            query: searchQuery.trim(),
            gradeNumber: filterGrade,
            area: areaParam,
            limit: 50,
          },
          undefined,
          50,
          controller.signal
        )
        if (!isCancelled) {
          setResults(res.routes || [])
        }
      } catch {
        if (!isCancelled) {
          setErrorMsg('No pudimos cargar el catálogo de roadmaps. Revisa tu conexión e intenta de nuevo.')
        }
      } finally {
        if (!isCancelled) {
          setLoading(false)
        }
      }
    }

    const timer = setTimeout(() => {
      void fetchCatalog()
    }, 200)

    return () => {
      isCancelled = true
      controller.abort()
      clearTimeout(timer)
    }
  }, [searchQuery, filterGrade, filterArea])

  // Filtrar según la pestaña activa (Roadmaps RUPI oficiales vs Comunidad / Profesores)
  const filteredRoutes = useMemo(() => {
    if (activeTab === 'OFICIAL') {
      // Rutas oficiales creadas por RUPI (1.° a 6.° de primaria)
      return results.filter((r) => r.category !== 'COMUNIDAD')
    } else {
      // En Comunidad: todos los roadmaps comunitarios creados por docentes y comunidad educativa
      return results.filter((r) => r.category === 'COMUNIDAD' || (r.authorName && !r.authorName.includes('RUPI')))
    }
  }, [results, activeTab])

  // Manejar click en una tarjeta de ruta (si no está inscrita, inscribir primero y luego abrir)
  async function handleActionClick(route: RouteSearchResult) {
    const isEnrolled = route.isEnrolled || enrolledMap.has(route.versionRouteId)
    if (isEnrolled) {
      onSelectRoute(route.versionRouteId, route.gradeNumber, route.areaCode)
      return
    }

    setEnrollingId(route.versionRouteId)
    try {
      const detail = await enrollInRoute(route.versionRouteId)
      // Actualizar estado local para que la tarjeta se marque como inscrita de inmediato
      setResults((prev) =>
        prev.map((r) =>
          r.versionRouteId === route.versionRouteId ? { ...r, isEnrolled: true } : r
        )
      )
      onSelectRoute(detail.versionRouteId, detail.grade?.number || route.gradeNumber, detail.area?.code || route.areaCode)
    } catch {
      // Si falla la inscripción en red, intentar navegar directamente
      onSelectRoute(route.versionRouteId, route.gradeNumber, route.areaCode)
    } finally {
      setEnrollingId(null)
    }
  }

  // Manejar ingreso de código de profesor directamente en la vista
  async function handleInlineCodeSubmit(e: React.FormEvent) {
    e.preventDefault()
    const trimmed = inlineCode.trim().toUpperCase()
    if (!trimmed || trimmed.length < 3) {
      setInlineError('Ingresa un código de clase válido (mínimo 3 letras o números).')
      return
    }
    setInlineJoining(true)
    setInlineError(null)
    setInlineSuccess(null)
    try {
      const res = await joinRouteByCode(trimmed)
      setInlineSuccess(`¡Bienvenido a tu clase! Te has unido a: ${res.routeTitle}`)
      // Actualizar resultados locales si la ruta está presente
      setResults((prev) =>
        prev.map((r) =>
          r.versionRouteId === res.versionRouteId ? { ...r, isEnrolled: true } : r
        )
      )
      setTimeout(() => {
        onSelectRoute(res.versionRouteId, res.grade?.number, res.area?.code)
      }, 1000)
    } catch (err: unknown) {
      const msg =
        err instanceof Error
          ? err.message
          : 'No encontramos un roadmap con este código. Revisa las letras o consulta con tu profesor.'
      setInlineError(msg)
    } finally {
      setInlineJoining(false)
    }
  }

  return (
    <div className="explore-view-container" id="explorar-roadmaps">
      {/* Cabecera de la sección de exploración */}
      <header className="explore-header">
        <div className="explore-header-text">
          <div className="explore-eyebrow">
            <Compass size={18} aria-hidden="true" />
            <span>Catálogo Escolar de Primaria</span>
          </div>
          <h1 className="explore-title">Explorar Roadmaps</h1>
          <p className="explore-subtitle">
            Descubre los caminos de aprendizaje oficiales creados por RUPI o accede a las rutas creadas por tus profesores para tu aula.
          </p>
        </div>

        <button
          type="button"
          className="btn-join-code-highlight"
          onClick={onOpenJoinModal}
          title="Unirme con el código que me dio mi profesor"
        >
          <KeyRound size={18} aria-hidden="true" />
          <span>Ingresar código de clase</span>
        </button>
      </header>

      {/* Pestañas de Categoría: Por Defecto (RUPI) vs Comunidad / Profesores */}
      <nav className="category-tabs-nav" aria-label="Categorías de roadmaps">
        <button
          type="button"
          className={`category-tab-btn ${activeTab === 'OFICIAL' ? 'active oficial' : ''}`}
          onClick={() => setActiveTab('OFICIAL')}
          aria-pressed={activeTab === 'OFICIAL'}
        >
          <GraduationCap size={20} aria-hidden="true" />
          <div className="tab-text-group">
            <strong className="tab-title">Roadmaps RUPI</strong>
            <small className="tab-desc">Rutas oficiales de 1.° a 6.° de primaria según el Currículo Nacional</small>
          </div>
        </button>

        <button
          type="button"
          className={`category-tab-btn ${activeTab === 'COMUNIDAD' ? 'active comunidad' : ''}`}
          onClick={() => setActiveTab('COMUNIDAD')}
          aria-pressed={activeTab === 'COMUNIDAD'}
        >
          <Users size={20} aria-hidden="true" />
          <div className="tab-text-group">
            <strong className="tab-title">Comunidad y Profesores</strong>
            <small className="tab-desc">Rutas de tus docentes a las que te has unido con código</small>
          </div>
        </button>
      </nav>

      {/* Barra de Filtros: Buscador + Grados + Materias */}
      <section className="explore-filters-bar" aria-label="Filtros de búsqueda">
        {/* Buscador de texto */}
        <div className="explore-search-input-wrap">
          <Search size={18} className="search-icon" aria-hidden="true" />
          <input
            type="search"
            className="explore-search-input"
            placeholder="Buscar por tema (ej. suma, cuentos, animales, ecosistema)..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            aria-label="Buscar roadmaps por tema o palabra clave"
          />
          {searchQuery && (
            <button
              type="button"
              className="clear-search-btn"
              onClick={() => setSearchQuery('')}
              aria-label="Borrar búsqueda"
            >
              ×
            </button>
          )}
        </div>

        {/* Filtro por Grado de Primaria (1.° a 6.°) */}
        <div className="explore-grade-chips" role="group" aria-label="Filtrar por grado de primaria">
          <span className="chips-label">
            <Filter size={14} aria-hidden="true" /> Grado:
          </span>
          <button
            type="button"
            className={`grade-chip-btn ${filterGrade === null ? 'active' : ''}`}
            onClick={() => setFilterGrade(null)}
          >
            Todos
          </button>
          {[1, 2, 3, 4, 5, 6].map((num) => (
            <button
              key={num}
              type="button"
              className={`grade-chip-btn ${filterGrade === num ? 'active' : ''}`}
              onClick={() => setFilterGrade(num)}
            >
              {num}.° Primaria
            </button>
          ))}
        </div>

        {/* Filtro por Materia Escolar */}
        <div className="explore-area-chips" role="group" aria-label="Filtrar por materia">
          <button
            type="button"
            className={`area-chip-btn ${filterArea === 'TODAS' ? 'active' : ''}`}
            onClick={() => setFilterArea('TODAS')}
          >
            Todas las materias
          </button>
          <button
            type="button"
            className={`area-chip-btn ${filterArea === 'COM' ? 'active' : ''}`}
            onClick={() => setFilterArea('COM')}
          >
            <BookOpen size={14} aria-hidden="true" /> Comunicación
          </button>
          <button
            type="button"
            className={`area-chip-btn ${filterArea === 'MAT' ? 'active' : ''}`}
            onClick={() => setFilterArea('MAT')}
          >
            <Calculator size={14} aria-hidden="true" /> Matemática
          </button>
          <button
            type="button"
            className={`area-chip-btn ${filterArea === 'CYT' ? 'active' : ''}`}
            onClick={() => setFilterArea('CYT')}
          >
            <FlaskConical size={14} aria-hidden="true" /> Ciencia y Tecnología
          </button>
        </div>
      </section>

      {/* Mensaje de error si la conexión falla */}
      {errorMsg && (
        <div className="explore-error-banner" role="alert">
          <p>{errorMsg}</p>
        </div>
      )}

      {/* Resultados de la búsqueda */}
      <section className="explore-results-section" aria-live="polite">
        <div className="results-count-bar">
          <span>
            Mostrando <strong>{filteredRoutes.length}</strong> {filteredRoutes.length === 1 ? 'roadmap' : 'roadmaps'}{' '}
            en <em>{activeTab === 'OFICIAL' ? 'Por Defecto (RUPI)' : 'Comunidad y Profesores'}</em>
          </span>
          {loading && (
            <span className="loading-indicator">
              <RefreshCw size={14} className="spin-icon" aria-hidden="true" /> Buscando rutas...
            </span>
          )}
        </div>

        {/* Sección interactiva destacada para ingresar código de profesor en la pestaña Comunidad */}
        {activeTab === 'COMUNIDAD' && (
          <div className="teacher-code-join-card" role="region" aria-label="Ingresar código de profesor">
            <div className="teacher-code-header">
              <div className="teacher-code-icon-badge" aria-hidden="true">
                <KeyRound size={26} />
              </div>
              <div className="teacher-code-titles">
                <h2 className="teacher-code-title">¿Tienes un código de tu profesor?</h2>
                <p className="teacher-code-desc">
                  Ingresa el código que te dio tu maestro o maestra para desbloquear y entrar a su roadmap escolar (ejemplos: <strong>PROFCARLOS</strong>, <strong>DOCENTE2026</strong> o <strong>WARI2026</strong>).
                </p>
              </div>
            </div>

            <form className="teacher-code-form" onSubmit={handleInlineCodeSubmit}>
              <div className="teacher-code-input-wrap">
                <KeyRound size={20} className="code-input-icon" aria-hidden="true" />
                <input
                  type="text"
                  className="teacher-code-input"
                  placeholder="Escribe el código (ej. PROFCARLOS)..."
                  value={inlineCode}
                  onChange={(e) => {
                    setInlineCode(e.target.value.toUpperCase().replace(/[^A-Z0-9_-]/g, ''))
                    setInlineError(null)
                  }}
                  maxLength={30}
                  aria-label="Código de clase del profesor"
                  autoCapitalize="characters"
                  autoComplete="off"
                  spellCheck="false"
                />
                {inlineCode && (
                  <button
                    type="button"
                    className="clear-code-btn"
                    onClick={() => {
                      setInlineCode('')
                      setInlineError(null)
                    }}
                    aria-label="Borrar código escrito"
                  >
                    ×
                  </button>
                )}
              </div>

              <button
                type="submit"
                className="btn-join-code-action"
                disabled={inlineJoining || !inlineCode.trim()}
              >
                {inlineJoining ? (
                  <>
                    <RefreshCw size={18} className="spin-icon" aria-hidden="true" />
                    <span>Verificando...</span>
                  </>
                ) : (
                  <>
                    <Sparkles size={18} aria-hidden="true" />
                    <span>Unirme al Roadmap</span>
                  </>
                )}
              </button>
            </form>

            {inlineSuccess && (
              <div className="code-feedback-alert success" role="alert">
                <CheckCircle2 size={18} aria-hidden="true" />
                <span>{inlineSuccess}</span>
              </div>
            )}
            {inlineError && (
              <div className="code-feedback-alert error" role="alert">
                <AlertCircle size={18} aria-hidden="true" />
                <span>{inlineError}</span>
              </div>
            )}
          </div>
        )}

        {/* Estado Vacío */}
        {!loading && filteredRoutes.length === 0 && (
          <div className="explore-empty-state">
            <RupiCharacter mood={activeTab === 'COMUNIDAD' ? 'cheering' : 'thinking'} role="default" className="explore-empty-rupi" />
            {activeTab === 'COMUNIDAD' ? (
              <>
                <h3>¡Explora las rutas de tu comunidad escolar!</h3>
                <p>
                  No encontramos roadmaps publicados con los filtros actuales. Prueba seleccionando "Todos los grados" o ingresa el código de tu profesor en la casilla superior para acceder a su aula.
                </p>
                <div className="empty-actions">
                  {filterGrade !== null && (
                    <button
                      type="button"
                      className="btn-reset-filters"
                      onClick={() => setFilterGrade(null)}
                    >
                      Ver todos los grados
                    </button>
                  )}
                  {filterArea !== 'TODAS' && (
                    <button
                      type="button"
                      className="btn-reset-filters"
                      onClick={() => setFilterArea('TODAS')}
                    >
                      Ver todas las materias
                    </button>
                  )}
                </div>
              </>
            ) : (
              <>
                <h3>No encontramos roadmaps con estos filtros</h3>
                <p>Intenta seleccionando "Todos los grados" o borrando el texto de búsqueda.</p>
                <div className="empty-actions">
                  {searchQuery && (
                    <button
                      type="button"
                      className="btn-reset-filters"
                      onClick={() => setSearchQuery('')}
                    >
                      Borrar búsqueda
                    </button>
                  )}
                  {filterGrade !== null && (
                    <button
                      type="button"
                      className="btn-reset-filters"
                      onClick={() => setFilterGrade(null)}
                    >
                      Ver todos los grados
                    </button>
                  )}
                </div>
              </>
            )}
          </div>
        )}

        {/* Grilla de Tarjetas de Roadmaps Ilustradas */}
        <div className="roadmaps-grid">
          {filteredRoutes.map((route) => {
            const enrolled = enrolledMap.get(route.versionRouteId)
            const isEnrolled = Boolean(route.isEnrolled || enrolled)
            const completedCount = enrolled ? enrolled.completedNodes : 0
            const totalCount = enrolled ? enrolled.totalNodes : route.totalNodes
            const isBusy = enrollingId === route.versionRouteId

            return (
              <RoadmapCard
                key={route.versionRouteId}
                route={route}
                isEnrolled={isEnrolled}
                completedNodes={completedCount}
                totalNodes={totalCount}
                isBusy={isBusy}
                onAction={() => void handleActionClick(route)}
              />
            )
          })}
        </div>
      </section>
    </div>
  )
}
