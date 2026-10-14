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
} from 'lucide-react'
import {
  searchAlternativeRoutes,
  enrollInRoute,
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
  currentGrade,
  currentCourse,
  enrolledRoutes,
  onSelectRoute,
  onOpenJoinModal,
}: ExploreRoadmapsViewProps) {
  const [activeTab, setActiveTab] = useState<CategoryTab>('OFICIAL')
  const [searchQuery, setSearchQuery] = useState('')
  const [filterGrade, setFilterGrade] = useState<number | null>(currentGrade)
  const [filterArea, setFilterArea] = useState<string>(() => {
    if (!currentCourse) return 'TODAS'
    const c = currentCourse.toUpperCase()
    if (c.includes('COM')) return 'COM'
    if (c.includes('MAT')) return 'MAT'
    if (c.includes('CYT')) return 'CYT'
    return 'TODAS'
  })
  const [results, setResults] = useState<RouteSearchResult[]>([])
  const [loading, setLoading] = useState(true)
  const [enrollingId, setEnrollingId] = useState<string | null>(null)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

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
            limit: 40,
          },
          undefined,
          40,
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

  // Filtrar según la pestaña activa (Oficial CNEB vs Comunidad / Docentes)
  const filteredRoutes = useMemo(() => {
    return results.filter((r) => {
      const isComunidad = r.category === 'COMUNIDAD' || (r.authorName && !r.authorName.includes('Rupi Oficial'))
      if (activeTab === 'OFICIAL') {
        return !isComunidad
      } else {
        return isComunidad
      }
    })
  }, [results, activeTab])

  // Manejar click en una tarjeta de ruta
  async function handleActionClick(route: RouteSearchResult) {
    const isEnrolled = route.isEnrolled || enrolledMap.has(route.versionRouteId)
    if (isEnrolled) {
      onSelectRoute(route.versionRouteId, route.gradeNumber, route.areaCode)
      return
    }

    // Si no está inscrita, inscribir primero
    setEnrollingId(route.versionRouteId)
    try {
      await enrollInRoute(route.versionRouteId)
      onSelectRoute(route.versionRouteId, route.gradeNumber, route.areaCode)
    } catch {
      // Si falla la inscripción en red, intentar navegar directamente
      onSelectRoute(route.versionRouteId, route.gradeNumber, route.areaCode)
    } finally {
      setEnrollingId(null)
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
            Descubre nuevos caminos de aprendizaje creados por el CNEB y por docentes de colegios de todo el Perú.
          </p>
        </div>

        <button
          type="button"
          className="btn-join-code-highlight"
          onClick={onOpenJoinModal}
          title="Unirme con el código que me dio mi profesor"
        >
          <KeyRound size={18} aria-hidden="true" />
          <span>Ingresar con código de clase</span>
        </button>
      </header>

      {/* Pestañas de Categoría: Aplicación vs Comunidad / Profesores */}
      <nav className="category-tabs-nav" aria-label="Categorías de roadmaps">
        <button
          type="button"
          className={`category-tab-btn ${activeTab === 'OFICIAL' ? 'active oficial' : ''}`}
          onClick={() => setActiveTab('OFICIAL')}
          aria-pressed={activeTab === 'OFICIAL'}
        >
          <GraduationCap size={20} aria-hidden="true" />
          <div className="tab-text-group">
            <strong className="tab-title">Rutas Oficiales (1.° a 6.° Primaria)</strong>
            <small className="tab-desc">Currículo Nacional CNEB aprobado para primaria</small>
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
            <small className="tab-desc">Creadas por docentes de primaria y proyectos escolares</small>
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
            en <em>{activeTab === 'OFICIAL' ? 'Rutas Oficiales' : 'Comunidad y Profesores'}</em>
          </span>
          {loading && (
            <span className="loading-indicator">
              <RefreshCw size={14} className="spin-icon" aria-hidden="true" /> Buscando rutas...
            </span>
          )}
        </div>

        {/* Estado Vacío */}
        {!loading && filteredRoutes.length === 0 && (
          <div className="explore-empty-state">
            <RupiCharacter mood="thinking" role="default" className="explore-empty-rupi" />
            <h3>No encontramos roadmaps con estos filtros</h3>
            <p>
              {activeTab === 'COMUNIDAD'
                ? 'Aún no hay rutas de la comunidad con estos filtros. Si tu docente te dio un código de clase, puedes ingresarlo arriba.'
                : 'Intenta seleccionando "Todos los grados" o borrando el texto de búsqueda.'}
            </p>
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
