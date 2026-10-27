import { useState, useEffect } from 'react'
import {
  Award,
  BookOpen,
  Calculator,
  CheckCircle2,
  Feather,
  GraduationCap,
  KeyRound,
  Lock,
  LogOut,
  School,
  Shirt,
  Sparkles,
  Star,
  Volume2,
} from 'lucide-react'
import type { BadgeDto, RouteSummary, Student } from '../../services/learningRoutesApi'
import { StudentAvatar } from '../../components/StudentAvatar'
import { RupiCharacter, type EquippedAccessories } from '../../components/RupiCharacter'
import { RupiCustomizerModal } from '../../components/RupiCustomizerModal'
import { fetchEquippedAccessories } from '../../services/accessoriesApi'
import { RoadmapCard } from '../../components/RoadmapCard'
import { speakText } from '../../utils/speech'
import './StudentProfileView.css'
import { StudyNotifications } from './StudyNotifications'

interface StudentProfileViewProps {
  student: Student
  enrolledGrade: number
  earnedBadges: BadgeDto[]
  enrolledRoutes: RouteSummary[]
  onSelectRoute?: (versionRouteId: string, gradeNumber?: number, areaCode?: string) => void
  onChangeGrade: () => void
  onOpenJoinModal: () => void
  onLogout: () => void
}

interface CatalogBadge {
  code: string
  name: string
  description: string
  icon: 'feather' | 'medal' | 'sparkles' | 'math' | 'book'
  xpBonus: number
}

const PRIMARY_BADGES_CATALOG: CatalogBadge[] = [
  {
    code: 'GRAN_TUNQUI_ORO',
    name: 'Gran Gallito de Oro',
    description: '¡Alcanzaste la cima de la ruta escolar! Volaste como el gallito de las rocas por toda la aventura.',
    icon: 'feather',
    xpBonus: 100,
  },
  {
    code: 'EXPLORADOR_AYACUCHO',
    name: 'Explorador de Ayacucho',
    description: 'Completaste tu primera parada escolar y superaste los desafíos de la lección.',
    icon: 'medal',
    xpBonus: 50,
  },
  {
    code: 'RETO_DIARIO',
    name: 'Guardián del Reto Diario',
    description: 'Resolviste el desafío del día y mantuviste tu práctica constante.',
    icon: 'sparkles',
    xpBonus: 30,
  },
  {
    code: 'RETABLO_MATEMATICO',
    name: 'Maestro del Retablo',
    description: 'Completaste ejercicios de sumas, conteo y patrones con figuras tradicionales.',
    icon: 'math',
    xpBonus: 40,
  },
  {
    code: 'PALABRAS_DEL_VALLE',
    name: 'Lector del Valle',
    description: 'Exploraste historias andinas, cuentos y palabras mágicas del Perú.',
    icon: 'book',
    xpBonus: 40,
  },
]

export function StudentProfileView({
  student,
  enrolledGrade,
  earnedBadges = [],
  enrolledRoutes = [],
  onSelectRoute,
  onChangeGrade,
  onOpenJoinModal,
  onLogout,
}: StudentProfileViewProps) {
  const [selectedBadge, setSelectedBadge] = useState<CatalogBadge | null>(null)
  const [showCustomizer, setShowCustomizer] = useState(false)
  const [equippedAccessories, setEquippedAccessories] = useState<EquippedAccessories>({})

  useEffect(() => {
    let active = true
    fetchEquippedAccessories()
      .then((data) => {
        if (!active) return
        setEquippedAccessories({
          sombrero: data.sombrero,
          vestimenta: data.vestimenta,
          fondo: data.fondo,
        })
      })
      .catch(() => {
        // Fallback silencioso si no hay conexión
      })
    return () => {
      active = false
    }
  }, [])

  const earnedCodes = new Set(earnedBadges.map((b) => b.code))

  // Combinar catálogo predefinido con cualquier insignia ganada desde el backend
  const displayCatalog = [...PRIMARY_BADGES_CATALOG]
  for (const b of earnedBadges) {
    if (!displayCatalog.some((c) => c.code === b.code)) {
      displayCatalog.push({
        code: b.code,
        name: b.name,
        description: b.description,
        icon: b.icon === 'feather' ? 'feather' : 'medal',
        xpBonus: 50,
      })
    }
  }

  const unlockedCount = displayCatalog.filter((b) => earnedCodes.has(b.code)).length
  const totalCompletedRoutes = enrolledRoutes.filter(
    (r) => r.totalNodes > 0 && r.completedNodes >= r.totalNodes
  ).length

  // Cálculo de XP total aproximado
  const totalBadgesXp = displayCatalog
    .filter((b) => earnedCodes.has(b.code))
    .reduce((sum, b) => sum + b.xpBonus, 0)
  const totalNodesXp = enrolledRoutes.reduce((sum, r) => sum + r.completedNodes * 10, 0)
  const totalXp = totalBadgesXp + totalNodesXp

  function handleSpeakBadge(badge: CatalogBadge, isEarned: boolean) {
    const text = isEarned
      ? `Insignia ganada: ${badge.name}. ${badge.description}. Ganaste ${badge.xpBonus} puntos de experiencia.`
      : `Insignia por desbloquear: ${badge.name}. ${badge.description}. Sigue avanzando en tus lecciones escolares para ganarla.`
    speakText(text)
  }

  function handleSpeakSummary() {
    const speech = `Perfil de ${student.name}. Tienes ${totalXp} puntos de experiencia, ${unlockedCount} de ${displayCatalog.length} insignias ganadas y ${enrolledRoutes.length} rutas activas. ¡Buen trabajo!`
    speakText(speech)
  }

  function renderBadgeIcon(icon: CatalogBadge['icon']) {
    switch (icon) {
      case 'feather':
        return <Feather size={28} aria-hidden="true" />
      case 'sparkles':
        return <Sparkles size={28} aria-hidden="true" />
      case 'math':
        return <Calculator size={28} aria-hidden="true" />
      case 'book':
        return <BookOpen size={28} aria-hidden="true" />
      default:
        return <Award size={28} aria-hidden="true" />
    }
  }

  return (
    <div className="profile-view-container" id="mi-perfil">
      <StudyNotifications key={student.id} />
      {/* Tarjeta Principal del Perfil */}
      <section className="profile-hero-card" aria-labelledby="profile-student-title">
        <div className="profile-hero-avatar-col">
          <StudentAvatar
            name={student.name}
            role={student.role}
            size="lg"
            showBadge
          />
        </div>

        <div className="profile-hero-info-col">
          <div className="profile-role-pill">
            {student.role === 'DOCENTE' ? (
              <>
                <GraduationCap size={15} aria-hidden="true" />
                <span>Docente de Primaria</span>
              </>
            ) : (
              <>
                <Sparkles size={15} aria-hidden="true" />
                <span>Estudiante de Primaria</span>
              </>
            )}
          </div>

          <h1 id="profile-student-title" className="profile-name">
            {student.name}
          </h1>

          <div className="profile-school-row">
            <School size={16} aria-hidden="true" />
            <span>{student.schoolName || student.institutionName || 'I.E. 38001 Mariscal Sucre'}</span>
          </div>

          <div className="profile-grade-row">
            <span className="profile-grade-badge">
              <GraduationCap size={15} aria-hidden="true" />
              <span>{enrolledGrade ? `${enrolledGrade}.° de Primaria (Matriculado)` : 'Primaria'}</span>
            </span>

            <button
              type="button"
              className="btn-change-grade-pill"
              onClick={onChangeGrade}
              title="Cambiar mi grado escolar oficial"
            >
              Cambiar de grado
            </button>
          </div>
        </div>

        {/* Resumen Métrico de Logros */}
        <div className="profile-stats-grid">
          <div className="stat-card xp-stat">
            <div className="stat-icon-wrap xp">
              <Star size={20} aria-hidden="true" />
            </div>
            <div className="stat-content">
              <strong className="stat-number">{totalXp}</strong>
              <span className="stat-label">Puntos XP</span>
            </div>
          </div>

          <div className="stat-card badges-stat">
            <div className="stat-icon-wrap badge">
              <Award size={20} aria-hidden="true" />
            </div>
            <div className="stat-content">
              <strong className="stat-number">{unlockedCount} / {displayCatalog.length}</strong>
              <span className="stat-label">Insignias</span>
            </div>
          </div>

          <div className="stat-card routes-stat">
            <div className="stat-icon-wrap routes">
              <BookOpen size={20} aria-hidden="true" />
            </div>
            <div className="stat-content">
              <strong className="stat-number">{enrolledRoutes.length}</strong>
              <span className="stat-label">
                {totalCompletedRoutes > 0 ? `${totalCompletedRoutes} completadas` : 'Rutas activas'}
              </span>
            </div>
          </div>
        </div>
      </section>

      {/* Mi Compañero Rupi y Armario (HU-31) */}
      <section className="profile-rupi-section" aria-labelledby="profile-rupi-title">
        <div className="profile-rupi-content">
          <h2 id="profile-rupi-title" className="profile-rupi-title">
            <Shirt size={22} aria-hidden="true" />
            <span>Mi Compañero Rupi</span>
          </h2>
          <p className="profile-rupi-subtitle">
            Personaliza a Rupi con sombreros tradicionales, vestimentas andinas y auras mágicas desbloqueados con tus puntos XP.
          </p>
          <button
            type="button"
            className="btn-open-rupi-store"
            onClick={() => setShowCustomizer(true)}
            title="Abrir la tienda y vestir a Rupi"
            aria-label="Abrir la tienda y vestir a Rupi"
          >
            <Sparkles size={18} aria-hidden="true" />
            <span>Abrir Armario y Tienda de Rupi</span>
          </button>
        </div>

        <div className="profile-rupi-avatar-wrap">
          <RupiCharacter
            mood="cheering"
            accessories={equippedAccessories}
            className="profile-rupi-character"
          />
        </div>
      </section>

      {/* Mis Rutas de Aprendizaje en Marcha */}
      <section className="profile-routes-section" aria-labelledby="profile-routes-title">
        <div className="routes-header-row">
          <div>
            <h2 id="profile-routes-title" className="profile-routes-title">
              <BookOpen size={22} aria-hidden="true" />
              <span>Mis Rutas de Aprendizaje</span>
            </h2>
            <p className="profile-routes-subtitle">
              Tus aventuras activas: tanto las de tu grado como las de tus profesores y proyectos escolares a los que te has unido.
            </p>
          </div>
        </div>

        {enrolledRoutes.length > 0 ? (
          <div className="profile-routes-grid">
            {enrolledRoutes.map((route) => {
              const areaUpper = (route.area.code || '').toUpperCase()
              const courseCode = areaUpper.includes('COM')
                ? 'COMUNICACION'
                : areaUpper.includes('CYT')
                ? 'CYT'
                : 'MATEMATICA'

              return (
                <RoadmapCard
                  key={route.versionRouteId}
                  route={route}
                  isEnrolled={true}
                  completedNodes={route.completedNodes}
                  totalNodes={route.totalNodes}
                  onAction={() => {
                    if (onSelectRoute) {
                      onSelectRoute(route.versionRouteId, route.grade.number, courseCode)
                    }
                  }}
                />
              )
            })}
          </div>
        ) : (
          <div className="profile-empty-routes">
            <p>Aún no te has inscrito en ninguna ruta. Explora el catálogo escolar para comenzar.</p>
          </div>
        )}
      </section>

      {/* Vitrina de Insignias Integrada en la Pantalla */}
      <section className="profile-badges-section" aria-labelledby="badges-section-title">
        <div className="badges-header-row">
          <div>
            <h2 id="badges-section-title" className="badges-title">
              <Award size={22} aria-hidden="true" />
              <span>Mis Insignias Escolares</span>
            </h2>
            <p className="badges-subtitle">
              Celebramos tu constancia, tus descubrimientos y cada lección aprendida.
            </p>
          </div>

          <button
            type="button"
            className="btn-speak-summary"
            onClick={handleSpeakSummary}
            title="Escuchar resumen de logros"
            aria-label="Escuchar resumen de logros escolares"
          >
            <Volume2 size={18} aria-hidden="true" />
            <span>Escuchar logros</span>
          </button>
        </div>

        {/* Grilla de Insignias */}
        <div className="badges-grid-layout" role="list">
          {displayCatalog.map((badge) => {
            const isEarned = earnedCodes.has(badge.code)
            const isSelected = selectedBadge?.code === badge.code

            return (
              <div
                key={badge.code}
                role="listitem"
                className={`profile-badge-card ${isEarned ? 'earned' : 'locked'} ${
                  isSelected ? 'selected' : ''
                }`}
                onClick={() => {
                  setSelectedBadge(badge)
                  handleSpeakBadge(badge, isEarned)
                }}
                tabIndex={0}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault()
                    setSelectedBadge(badge)
                    handleSpeakBadge(badge, isEarned)
                  }
                }}
                aria-label={`${badge.name}: ${isEarned ? 'Insignia ganada' : 'Por desbloquear'}`}
              >
                <div className="badge-icon-disc">
                  {isEarned ? (
                    renderBadgeIcon(badge.icon)
                  ) : (
                    <Lock size={24} aria-hidden="true" />
                  )}
                </div>

                <div className="badge-info-col">
                  <div className="badge-name-row">
                    <strong className="badge-name">{badge.name}</strong>
                    {isEarned && (
                      <span className="badge-check-tag" title="Ganada">
                        <CheckCircle2 size={14} aria-hidden="true" />
                      </span>
                    )}
                  </div>

                  <p className="badge-description">{badge.description}</p>

                  <div className="badge-footer-row">
                    <span className="badge-xp-bonus">
                      <Star size={13} aria-hidden="true" /> +{badge.xpBonus} XP
                    </span>
                    <span className={`badge-status-pill ${isEarned ? 'unlocked' : 'pending'}`}>
                      {isEarned ? 'Desbloqueada' : 'Por descubrir'}
                    </span>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      </section>

      {/* Opciones y Acciones Rápidas */}
      <section className="profile-actions-section" aria-label="Acciones del perfil">
        <h2 className="section-small-title">Opciones Escolares</h2>
        <div className="profile-actions-grid">
          <button
            type="button"
            className="profile-action-box"
            onClick={onOpenJoinModal}
          >
            <div className="action-box-icon code">
              <KeyRound size={22} aria-hidden="true" />
            </div>
            <div className="action-box-text">
              <strong>Unirse con código de clase</strong>
              <span>Ingresa el código que te compartió tu docente</span>
            </div>
          </button>

          <button
            type="button"
            className="profile-action-box"
            onClick={onChangeGrade}
          >
            <div className="action-box-icon grade">
              <GraduationCap size={22} aria-hidden="true" />
            </div>
            <div className="action-box-text">
              <strong>Cambiar de grado de primaria</strong>
              <span>Modifica tu grado escolar predeterminado (1.° a 6.°)</span>
            </div>
          </button>

          <button
            type="button"
            className="profile-action-box logout-box"
            onClick={onLogout}
          >
            <div className="action-box-icon logout">
              <LogOut size={22} aria-hidden="true" />
            </div>
            <div className="action-box-text">
              <strong>Cerrar sesión</strong>
              <span>Salir de forma segura de tu cuenta escolar</span>
            </div>
          </button>
        </div>
      </section>

      <RupiCustomizerModal
        isOpen={showCustomizer}
        onClose={() => setShowCustomizer(false)}
        onEquippedChange={setEquippedAccessories}
      />
    </div>
  )
}
