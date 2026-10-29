import {
  BookOpen,
  Calculator,
  FlaskConical,
  GraduationCap,
  Sparkles,
  Users,
  CheckCircle2,
  ArrowRight,
  Video,
  FileCheck,
} from 'lucide-react'
import type { RouteSearchResult, RouteSummary } from '../services/learningRoutesApi'
import './RoadmapCard.css'

interface RoadmapCardProps {
  route: RouteSearchResult | RouteSummary
  isEnrolled?: boolean
  completedNodes?: number
  totalNodes?: number
  isBusy?: boolean
  onAction: () => void
}

export function RoadmapCard({
  route,
  isEnrolled = false,
  completedNodes = 0,
  totalNodes = 0,
  isBusy = false,
  onAction,
}: RoadmapCardProps) {
  // Normalizar datos entre RouteSearchResult y RouteSummary
  const title = 'title' in route ? route.title : route.routeTitle
  const gradeNumber = 'grade' in route ? route.grade.number : route.gradeNumber
  const areaCode = ('area' in route ? route.area.code : route.areaCode || '').toUpperCase()
  const areaName = 'area' in route ? route.area.name : route.areaName
  const description = 'description' in route ? route.description : null
  const category = route.category || (route.authorName?.includes('RUPI') ? 'OFICIAL' : 'COMUNIDAD')
  const authorName = route.authorName || (category === 'OFICIAL' ? 'Creado por RUPI' : 'Docente de Primaria')
  const total = totalNodes || ('totalNodes' in route ? route.totalNodes : 0)
  const completed = completedNodes || ('completedNodes' in route ? route.completedNodes : 0)
  const percent = total > 0 ? Math.min(100, Math.round((completed / total) * 100)) : 0

  const isMat = areaCode.includes('MAT')
  const isCyt = areaCode.includes('CYT')
  const isTeacherRoute = category === 'COMUNIDAD'

  return (
    <article
      className={`roadmap-card-toy ${isTeacherRoute ? 'theme-community' : isMat ? 'theme-math' : isCyt ? 'theme-cyt' : 'theme-com'}`}
      aria-label={`Roadmap de ${title}, ${gradeNumber === 0 ? 'Sin grado' : `${gradeNumber}.° de Primaria`}`}
      role="button"
      tabIndex={0}
      onClick={onAction}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault()
          onAction()
        }
      }}
    >
      {/* Ilustración de portada temática según la materia */}
      <div className="card-illustration-header" aria-hidden="true">
        {isTeacherRoute ? (
          /* Portada: Aula escolar docente y pizarra con tiza */
          <svg viewBox="0 0 400 160" className="card-header-svg" preserveAspectRatio="xMidYMid slice">
            <rect width="400" height="160" fill="#2d6a4f" />
            {/* Pizarra verde escolar */}
            <rect x="25" y="20" width="350" height="120" rx="10" fill="#1b4332" stroke="#d8f3dc" strokeWidth="4" />
            {/* Trazos de tiza amigables */}
            <line x1="45" y1="45" x2="160" y2="45" stroke="#ffffff" strokeWidth="3" strokeDasharray="6,4" opacity="0.8" />
            <line x1="45" y1="65" x2="220" y2="65" stroke="#ffd26a" strokeWidth="3" strokeDasharray="8,4" opacity="0.9" />
            <line x1="45" y1="85" x2="140" y2="85" stroke="#ffffff" strokeWidth="3" strokeDasharray="6,4" opacity="0.8" />
            {/* Medalla docente o sol sonriente */}
            <circle cx="310" cy="75" r="28" fill="#ffd26a" />
            <path d="M300 70 Q310 82 320 70" stroke="#1b4332" strokeWidth="3" fill="none" strokeLinecap="round" />
            <circle cx="304" cy="65" r="3" fill="#1b4332" />
            <circle cx="316" cy="65" r="3" fill="#1b4332" />
            {/* Libros apilados */}
            <rect x="260" y="115" width="45" height="10" rx="2" fill="#f95738" />
            <rect x="264" y="105" width="40" height="10" rx="2" fill="#246a91" />
          </svg>
        ) : isMat ? (
          /* Portada: Retablo matemático andino con quipus, figuras geométricas y estrellas */
          <svg viewBox="0 0 400 160" className="card-header-svg" preserveAspectRatio="xMidYMid slice">
            <rect width="400" height="160" fill="#ffb703" />
            {/* Montañas geométricas de Ayacucho */}
            <polygon points="0,160 120,40 240,160" fill="#fb8500" opacity="0.85" />
            <polygon points="160,160 280,25 400,160" fill="#d48b00" opacity="0.9" />
            {/* Figuras geométricas flotantes con relieve */}
            <rect x="50" y="35" width="34" height="34" rx="6" fill="#ffffff" stroke="#173e30" strokeWidth="3" transform="rotate(15 67 52)" />
            <polygon points="200,30 225,75 175,75" fill="#f95738" stroke="#ffffff" strokeWidth="3" />
            <circle cx="330" cy="50" r="22" fill="#246a91" stroke="#ffffff" strokeWidth="3" />
            {/* Números y símbolos amigables */}
            <text x="63" y="58" fontFamily="Nunito, sans-serif" fontSize="20" fontWeight="900" fill="#173e30">+2</text>
            <text x="193" y="66" fontFamily="Nunito, sans-serif" fontSize="18" fontWeight="900" fill="#ffffff">★</text>
            <text x="323" y="57" fontFamily="Nunito, sans-serif" fontSize="20" fontWeight="900" fill="#ffd26a">×</text>
          </svg>
        ) : isCyt ? (
          /* Portada: Selva alta peruana, Gallito de las Rocas y lupa de exploración */
          <svg viewBox="0 0 400 160" className="card-header-svg" preserveAspectRatio="xMidYMid slice">
            <rect width="400" height="160" fill="#2a9d8f" />
            {/* Montañas y follaje tropical */}
            <path d="M0 160 Q100 60 200 120 T400 160 Z" fill="#264653" />
            <path d="M220 160 Q300 40 400 110 L400 160 Z" fill="#1e3d34" />
            {/* Hoja andina brillante */}
            <path d="M80 120 C60 60, 140 40, 150 110 C130 115, 100 120, 80 120 Z" fill="#a7c957" />
            {/* Lupa exploradora */}
            <circle cx="280" cy="65" r="28" fill="#e9f5ed" stroke="#173e30" strokeWidth="4" />
            <circle cx="280" cy="65" r="20" fill="#c7ebd1" />
            <line x1="300" y1="85" x2="330" y2="115" stroke="#173e30" strokeWidth="7" strokeLinecap="round" />
            {/* Planta dentro de la lupa */}
            <path d="M280 75 Q275 60 280 52 Q288 60 280 75" fill="#31724b" />
          </svg>
        ) : (
          /* Portada: Comunicación - Valle andino de leyendas, libro abierto y cóndor */
          <svg viewBox="0 0 400 160" className="card-header-svg" preserveAspectRatio="xMidYMid slice">
            <rect width="400" height="160" fill="#457b9d" />
            {/* Colinas andinas */}
            <polygon points="0,160 140,50 300,160" fill="#1d3557" />
            <polygon points="180,160 310,35 400,160" fill="#2b4c6f" />
            {/* Sol andino naciente */}
            <circle cx="70" cy="55" r="26" fill="#ffd26a" />
            {/* Libro mágico abierto */}
            <g transform="translate(230, 40)">
              <rect x="0" y="0" width="55" height="42" rx="4" fill="#ffffff" stroke="#1d3557" strokeWidth="2.5" />
              <rect x="55" y="0" width="55" height="42" rx="4" fill="#f8f9fa" stroke="#1d3557" strokeWidth="2.5" />
              <line x1="10" y1="12" x2="45" y2="12" stroke="#f95738" strokeWidth="2.5" strokeLinecap="round" />
              <line x1="10" y1="20" x2="40" y2="20" stroke="#457b9d" strokeWidth="2" strokeLinecap="round" />
              <line x1="10" y1="28" x2="45" y2="28" stroke="#457b9d" strokeWidth="2" strokeLinecap="round" />
              <line x1="65" y1="12" x2="100" y2="12" stroke="#457b9d" strokeWidth="2" strokeLinecap="round" />
              <line x1="65" y1="20" x2="95" y2="20" stroke="#457b9d" strokeWidth="2" strokeLinecap="round" />
            </g>
          </svg>
        )}

        {/* Badge de Grado Escolar superpuesto en la portada */}
        <span className="card-floating-grade-badge">
          <GraduationCap size={15} aria-hidden="true" />
          <span>{gradeNumber === 0 ? 'Sin grado' : `${gradeNumber}.° de Primaria`}</span>
        </span>

        {/* Píldora de Materia */}
        <span className="card-floating-area-badge">
          {isMat ? <Calculator size={15} /> : isCyt ? <FlaskConical size={15} /> : <BookOpen size={15} />}
          <span>{areaName}</span>
        </span>
      </div>

      {/* Cuerpo de la tarjeta */}
      <div className="card-toy-body">
        {/* Sello de Autoría: CNEB Oficial vs Comunidad / Docente */}
        <div className="card-author-pill-row">
          {isTeacherRoute ? (
            <span className="author-toy-pill teacher" title={`Creado por: ${authorName}`}>
              <Users size={14} aria-hidden="true" />
              <span>{authorName}</span>
            </span>
          ) : (
            <span className="author-toy-pill official" title="Creado por el equipo RUPI">
              <Sparkles size={14} aria-hidden="true" />
              <span>Creado por RUPI</span>
            </span>
          )}

          <span className="card-stops-count-pill">
            <FileCheck size={14} aria-hidden="true" />
            <span>{total} paradas</span>
          </span>
        </div>

        {/* Título de la Ruta */}
        <h3 className="card-toy-title">{title}</h3>

        {/* Descripción didáctica */}
        {description && <p className="card-toy-desc">{description}</p>}

        {/* Etiquetas de recursos didácticos incluidos */}
        <div className="card-features-row" aria-label="Recursos didácticos incluidos">
          <span className="feature-chip">
            <Video size={13} aria-hidden="true" /> Video explicativo
          </span>
          <span className="feature-chip">
            <Sparkles size={13} aria-hidden="true" /> Retos interactivos
          </span>
        </div>

        {/* Si ya está inscrita: barra de avance */}
        {isEnrolled && (
          <div className="card-enrolled-progress">
            <div className="enrolled-progress-header">
              <span className="enrolled-status-text">
                <CheckCircle2 size={14} aria-hidden="true" />
                <span>Progreso: {completed} de {total} completadas</span>
              </span>
              <strong className="enrolled-percent">{percent}%</strong>
            </div>
            <div className="enrolled-track">
              <div className="enrolled-fill" style={{ width: `${percent}%` }} />
            </div>
          </div>
        )}

        {/* Botón de Acción Táctil (Mínimo 48px de alto) */}
        <div className="card-toy-footer">
          <button
            type="button"
            className={`card-toy-btn ${isEnrolled ? 'btn-toy-continue' : isTeacherRoute ? 'btn-toy-join' : 'btn-toy-start'}`}
            disabled={isBusy}
            onClick={(e) => {
              e.stopPropagation()
              onAction()
            }}
            aria-label={`${isEnrolled ? 'Continuar ruta' : isTeacherRoute ? 'Unirme a este roadmap' : 'Empezar aventura'}: ${title}`}
          >
            {isBusy ? (
              <span>Preparando lección...</span>
            ) : isEnrolled ? (
              <>
                <span>Continuar mi aventura</span>
                <ArrowRight size={18} aria-hidden="true" />
              </>
            ) : isTeacherRoute ? (
              <>
                <span>Unirme a este roadmap</span>
                <Users size={18} aria-hidden="true" />
              </>
            ) : (
              <>
                <span>Empezar aventura</span>
                <Sparkles size={18} aria-hidden="true" />
              </>
            )}
          </button>
        </div>
      </div>
    </article>
  )
}
