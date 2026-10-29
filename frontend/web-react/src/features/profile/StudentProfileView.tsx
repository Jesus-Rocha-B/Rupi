import { useState, useEffect, useTransition } from 'react'
import {
  Award,
  CheckCircle2,
  Flame,
  GraduationCap,
  MapPin,
  School,
  Shirt,
  Sparkles,
  Star,
  Trophy,
  Volume2,
} from 'lucide-react'
import type { BadgeDto, RouteSummary, Student } from '../../services/learningRoutesApi'
import {
  fetchDashboardSummary,
  type DashboardSummaryResponse,
} from '../../services/dashboardApi'
import { fetchEquippedAccessories } from '../../services/accessoriesApi'
import { StudentAvatar } from '../../components/StudentAvatar'
import { RupiCharacter, type EquippedAccessories } from '../../components/RupiCharacter'
import { DailyGoalRing } from '../../components/DailyGoalRing'
import { ScoreComparison } from '../../components/ScoreComparison'
import { DailyChallengeCard } from '../dashboard/DailyChallengeCard'
import { RupiCustomizerModal } from '../../components/RupiCustomizerModal'
import { ClassroomLeaderboardModal } from '../../components/ClassroomLeaderboardModal'
import { StudentBadgesModal } from '../../components/StudentBadgesModal'
import { speakText } from '../../utils/speech'
import '../dashboard/StudentDashboardView.css'
import './StudentProfileView.css'

export interface StudentProfileViewProps {
  student: Student
  enrolledGrade: number
  earnedBadges?: BadgeDto[]
  enrolledRoutes?: RouteSummary[]
  refreshTrigger?: number
  onChangeGrade: () => void
  onOpenJoinModal?: () => void
  onOpenLeaderboard?: () => void
  onOpenBadges?: () => void
  onLogout?: () => void
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

function parseEquippedAccessories(codes: string[]): EquippedAccessories {
  const acc: EquippedAccessories = {
    sombrero: null,
    vestimenta: null,
    fondo: null,
  }
  for (const c of codes) {
    if (c.includes('CHULLO') || c.includes('BIRRETE') || c.includes('GORRO')) {
      acc.sombrero = c
    } else if (c.includes('CHALECO') || c.includes('PONCHO') || c.includes('CAPA')) {
      acc.vestimenta = c
    } else if (c.includes('FONDO')) {
      acc.fondo = c
    }
  }
  return acc
}

export function StudentProfileView({
  student,
  enrolledGrade,
  earnedBadges = [],
  refreshTrigger = 0,
  onChangeGrade,
  onOpenLeaderboard,
  onOpenBadges,
}: StudentProfileViewProps) {
  const [data, setData] = useState<DashboardSummaryResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showBadgesModal, setShowBadgesModal] = useState(false)
  const [showCustomizer, setShowCustomizer] = useState(false)
  const [showLeaderboard, setShowLeaderboard] = useState(false)
  const [equippedFallback, setEquippedFallback] = useState<EquippedAccessories>({})
  const [scoreRefresh, setScoreRefresh] = useState(0)
  const [, startTransition] = useTransition()

  // Sincronización inicial y ante cambios de refreshTrigger
  useEffect(() => {
    let ignore = false
    fetchDashboardSummary({ forceRefresh: false })
      .then((res) => {
        if (ignore) return
        startTransition(() => {
          setData(res.data)
          setLoading(false)
          setError(null)
        })
      })
      .catch((err: unknown) => {
        if (ignore) return
        startTransition(() => {
          setLoading(false)
          setError(err instanceof Error ? err.message : 'No se pudo cargar el panel de aprendizaje')
        })
      })

    return () => {
      ignore = true
    }
  }, [refreshTrigger])

  // Carga complementaria de accesorios equipados
  useEffect(() => {
    let active = true
    fetchEquippedAccessories()
      .then((dataAcc) => {
        if (!active) return
        setEquippedFallback({
          sombrero: dataAcc.sombrero,
          vestimenta: dataAcc.vestimenta,
          fondo: dataAcc.fondo,
        })
      })
      .catch(() => {
        // Fallback silencioso si no hay conexión
      })
    return () => {
      active = false
    }
  }, [])

  const handleRefreshClick = () => {
    fetchDashboardSummary({ forceRefresh: true })
      .then((res) => {
        startTransition(() => {
          setData(res.data)
          setError(null)
        })
      })
      .catch((err: unknown) => {
        startTransition(() => {
          setError(err instanceof Error ? err.message : 'No se pudo sincronizar el panel')
        })
      })
  }

  const handleChallengeCompleted = () => {
    setScoreRefresh((n) => n + 1)
    handleRefreshClick()
  }

  const metrics = data?.metrics ?? {
    totalCompletedLessons: 0,
    totalPendingLessons: 0,
    totalEnrolledLessons: 0,
    overallCompletionPercentage: 0,
    lessonsCompletedToday: 0,
    currentStreakDays: 0,
    totalXp: 0,
    activeRoutesCount: 0,
  }

  const equipped = parseEquippedAccessories(
    data?.student?.equippedAccessories && data.student.equippedAccessories.length > 0
      ? data.student.equippedAccessories
      : [equippedFallback.sombrero, equippedFallback.vestimenta, equippedFallback.fondo].filter(Boolean) as string[]
  )

  // Catálogo de insignias
  const earnedCodes = new Set(earnedBadges.map((b) => b.code))
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

  function handleSpeakSummary() {
    if (!data) return
    const msg = `¡Hola ${student.name}! Has completado ${metrics.totalCompletedLessons} lecciones escolares. Tienes ${metrics.totalPendingLessons} lecciones pendientes y tu avance general es de ${metrics.overallCompletionPercentage} por ciento. Tienes ${unlockedCount} de ${displayCatalog.length} insignias ganadas. ¡Sigue adelante!`
    speakText(msg)
  }

  function handleOpenBadges() {
    if (onOpenBadges) {
      onOpenBadges()
    } else {
      setShowBadgesModal(true)
    }
  }

  if (loading && !data) {
    return (
      <div className="dashboard-loading-state" role="status">
        <div className="dashboard-spinner" aria-hidden="true" />
        <p>Cargando tu perfil y panel de aprendizaje...</p>
      </div>
    )
  }

  return (
    <div
      className="profile-view-container student-dashboard"
      id="mi-perfil"
      role="region"
      aria-label="Perfil y panel de estudio"
    >
      {/* 1. Hero de Bienvenida, Identidad Escolar y Guía Rupi */}
      <section className="dashboard-hero" aria-labelledby="profile-heading">
        <div className="dashboard-hero-content">
          <div className="dashboard-hero-text">


            <div className="dashboard-greeting-wrap">
              <StudentAvatar
                name={student.name}
                role={student.role}
                size="md"
                showBadge
              />
              <h1 id="profile-heading" className="dashboard-greeting">
                ¡Hola, <span className="greeting-name">{student.name}</span>!
              </h1>
            </div>

            <div className="dashboard-school-info-row">
              <span className="school-name">
                <School size={16} aria-hidden="true" />
                <span>{data?.student?.schoolName || student.schoolName || student.institutionName || 'I.E. 38001 Mariscal Sucre'}</span>
              </span>
              <span className="grade-badge-tag">
                <GraduationCap size={15} aria-hidden="true" />
                <span>{data?.student?.gradeName || (enrolledGrade ? `${enrolledGrade}.° de Primaria` : 'Primaria')}</span>
              </span>
              <button
                type="button"
                className="btn-change-grade-pill"
                onClick={onChangeGrade}
                title="Cambiar mi grado escolar oficial"
                aria-label="Cambiar mi grado escolar oficial"
              >
                Cambiar de grado
              </button>
            </div>

            <div className="dashboard-speech-bubble" role="note">
              <Sparkles size={18} className="bubble-sparkle" aria-hidden="true" />
              <p>
                {metrics.totalCompletedLessons > 0
                  ? `¡Gran trabajo, ${student.name}! Llevas ${metrics.totalCompletedLessons} lecciones superadas. Te faltan ${metrics.totalPendingLessons} para completar tus metas escolares.`
                  : `¡Bienvenido a tu perfil y panel escolar, ${student.name}! Empieza tu primera lección para encender tu racha de estudio.`}
              </p>
            </div>

            <div className="dashboard-hero-actions">
              <button
                type="button"
                className="action-btn action-audio"
                onClick={handleSpeakSummary}
                aria-label="Escuchar resumen de logros"
                title="Escuchar a Rupi leer mi resumen escolar"
              >
                <Volume2 size={18} aria-hidden="true" />
                <span>Escuchar</span>
              </button>

              <button
                type="button"
                className="action-btn action-badges"
                onClick={handleOpenBadges}
                aria-label="Ver mis insignias escolares en ventana flotante"
                title="Abrir vitrina de insignias escolares"
              >
                <Award size={18} aria-hidden="true" />
                <span>Mis Insignias</span>
                <span className="badges-btn-pill" aria-hidden="true">
                  {unlockedCount}
                </span>
              </button>
            </div>
          </div>

          <div className="dashboard-hero-avatar-col">
            <div
              className="dashboard-rupi-podium"
              role="region"
              aria-label="Armario de Rupi"
            >
              <button
                type="button"
                className="rupi-avatar-click-btn"
                onClick={() => setShowCustomizer(true)}
                title="Abrir Armario de Rupi para cambiar vestuario"
                aria-label="Cambiar vestuario de Rupi"
              >
                <RupiCharacter
                  className="dashboard-rupi-avatar"
                  mood={metrics.totalCompletedLessons > 0 ? 'cheering' : 'idle'}
                  accessories={equipped}
                />
              </button>
              <button
                type="button"
                className="customize-avatar-link"
                onClick={() => setShowCustomizer(true)}
                title="Abrir Armario de Rupi para cambiar vestuario y accesorios"
                aria-label="Abrir Armario de Rupi para cambiar vestuario"
              >
                <Shirt size={14} aria-hidden="true" />
                <span>Armario Rupi</span>
              </button>
            </div>
          </div>
        </div>
      </section>

      {error && (
        <div className="dashboard-error-banner" role="alert">
          <p>⚠️ {error}</p>
          <button type="button" onClick={handleRefreshClick}>
            Reintentar
          </button>
        </div>
      )}

      {/* 2. Indicador Gráfico de Progreso Diario y Meta de Hoy (HU-34) */}
      <section className="dashboard-daily-goal-section" aria-label="Meta de aprendizaje del día">
        <DailyGoalRing refreshTrigger={refreshTrigger} />
        <ScoreComparison refreshTrigger={refreshTrigger + scoreRefresh} />
      </section>

      {/* Reto del día: razonamiento lógico o comprensión lectora (HU-40) */}
      <DailyChallengeCard refreshTrigger={refreshTrigger} onCompleted={handleChallengeCompleted} />

      {/* 3. Cuadrícula de Tarjetas Métricas de Aprendizaje (HU-33) */}
      <section className="dashboard-metrics-grid" aria-label="Tarjetas métricas de aprendizaje">
        {/* Tarjeta 1: Lecciones Completadas */}
        <article className="metric-card metric-completed">
          <div className="metric-card-top">
            <span className="metric-icon-box box-green" aria-hidden="true">
              <CheckCircle2 size={24} />
            </span>
            <span className="metric-badge-tag green">Completadas</span>
          </div>
          <div className="metric-value-row">
            <span className="metric-large-number">{metrics.totalCompletedLessons}</span>
            <span className="metric-unit-label">lecciones</span>
          </div>
          <p className="metric-subtext">
            de {metrics.totalEnrolledLessons} inscritas en total
          </p>
          <div className="metric-progress-track" aria-hidden="true">
            <div
              className="metric-progress-fill fill-green"
              style={{ width: `${Math.min(100, Math.max(0, metrics.overallCompletionPercentage))}%` }}
            />
          </div>
          <footer className="metric-card-footer">
            <span>{metrics.overallCompletionPercentage}% de avance</span>
            {metrics.lessonsCompletedToday > 0 && (
              <span className="today-chip">+{metrics.lessonsCompletedToday} hoy</span>
            )}
          </footer>
        </article>

        {/* Tarjeta 2: Lecciones Pendientes */}
        <article className="metric-card metric-pending">
          <div className="metric-card-top">
            <span className="metric-icon-box box-orange" aria-hidden="true">
              <MapPin size={24} />
            </span>
            <span className="metric-badge-tag orange">Por Realizar</span>
          </div>
          <div className="metric-value-row">
            <span className="metric-large-number">{metrics.totalPendingLessons}</span>
            <span className="metric-unit-label">pendientes</span>
          </div>
          <p className="metric-subtext">
            para completar todas tus materias
          </p>
          <div className="metric-progress-track" aria-hidden="true">
            <div
              className="metric-progress-fill fill-orange"
              style={{ width: `${Math.min(100, Math.max(0, 100 - metrics.overallCompletionPercentage))}%` }}
            />
          </div>
          <footer className="metric-card-footer">
            <span>¡Cada paso cuenta!</span>
          </footer>
        </article>

        {/* Tarjeta 3: Racha de Estudio */}
        <article className="metric-card metric-streak">
          <div className="metric-card-top">
            <span className="metric-icon-box box-flame" aria-hidden="true">
              <Flame size={24} />
            </span>
            <span className="metric-badge-tag flame">Racha Activa</span>
          </div>
          <div className="metric-value-row">
            <span className="metric-large-number">{metrics.currentStreakDays}</span>
            <span className="metric-unit-label">días</span>
          </div>
          <p className="metric-subtext">
            {metrics.currentStreakDays > 0
              ? '¡Excelente constancia escolar!'
              : '¡Haz una lección hoy para encenderla!'}
          </p>
          <footer className="metric-card-footer">
            <button
              type="button"
              className="metric-link-btn"
              onClick={() => {
                if (onOpenLeaderboard) onOpenLeaderboard()
                else setShowLeaderboard(true)
              }}
            >
              <Trophy size={14} aria-hidden="true" />
              <span>Ver Tabla del Aula</span>
            </button>
          </footer>
        </article>

        {/* Tarjeta 4: Puntos XP */}
        <article className="metric-card metric-xp">
          <div className="metric-card-top">
            <span className="metric-icon-box box-yellow" aria-hidden="true">
              <Star size={24} />
            </span>
            <span className="metric-badge-tag yellow">Experiencia</span>
          </div>
          <div className="metric-value-row">
            <span className="metric-large-number">{metrics.totalXp}</span>
            <span className="metric-unit-label">XP</span>
          </div>
          <p className="metric-subtext">
            ganados con tus aciertos y paradas
          </p>
          <footer className="metric-card-footer">
            <span>¡Puntos de experiencia para tu aventura escolar!</span>
          </footer>
        </article>
      </section>





      <RupiCustomizerModal
        isOpen={showCustomizer}
        onClose={() => setShowCustomizer(false)}
        onEquippedChange={(acc) => {
          setEquippedFallback(acc)
          handleRefreshClick()
        }}
      />

      <ClassroomLeaderboardModal
        isOpen={showLeaderboard}
        onClose={() => setShowLeaderboard(false)}
      />

      {showBadgesModal && (
        <StudentBadgesModal
          studentName={student.name}
          schoolName={data?.student?.schoolName || student.schoolName || student.institutionName || 'I.E. 38001 Mariscal Sucre'}
          gradeName={data?.student?.gradeName || (enrolledGrade ? `${enrolledGrade}.° de primaria` : 'Primaria')}
          earnedBadges={earnedBadges}
          totalXp={metrics.totalXp}
          onClose={() => setShowBadgesModal(false)}
        />
      )}
    </div>
  )
}
