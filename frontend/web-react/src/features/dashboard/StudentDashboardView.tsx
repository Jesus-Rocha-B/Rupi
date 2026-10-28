import React, { useState, useEffect, useTransition } from 'react'
import {
  CheckCircle2,
  Clock,
  Compass,
  Flame,
  LayoutDashboard,
  MapPin,
  RefreshCw,
  Shirt,
  Sparkles,
  Star,
  Trophy,
  Volume2,
} from 'lucide-react'
import {
  fetchDashboardSummary,
  type DashboardSummaryResponse,
} from '../../services/dashboardApi'
import { RupiCharacter, type EquippedAccessories } from '../../components/RupiCharacter'
import { DailyGoalRing } from '../../components/DailyGoalRing'
import { ScoreComparison } from '../../components/ScoreComparison'
import { speakText } from '../../utils/speech'
import './StudentDashboardView.css'

interface Props {
  studentName: string
  refreshTrigger?: number
  onContinueLesson: (routeId: string, nodeId: string) => void
  onNavigateToRoute: (routeId: string) => void
  onOpenStore: () => void
  onOpenLeaderboard: () => void
  onNavigateToCamino: () => void
  onNavigateToExplorar: () => void
}

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

export const StudentDashboardView: React.FC<Props> = ({
  studentName,
  refreshTrigger = 0,
  onContinueLesson,
  onNavigateToRoute,
  onOpenStore,
  onOpenLeaderboard,
  onNavigateToCamino,
  onNavigateToExplorar,
}) => {
  const [data, setData] = useState<DashboardSummaryResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [isFromCache, setIsFromCache] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [, startTransition] = useTransition()

  useEffect(() => {
    let ignore = false
    fetchDashboardSummary({ forceRefresh: false })
      .then((res) => {
        if (ignore) return
        startTransition(() => {
          setData(res.data)
          setIsFromCache(res.fromCache)
          setLoading(false)
          setError(null)
        })
      })
      .catch((err: unknown) => {
        if (ignore) return
        startTransition(() => {
          setLoading(false)
          setError(err instanceof Error ? err.message : 'No se pudo cargar el panel')
        })
      })

    return () => {
      ignore = true
    }
  }, [refreshTrigger])

  const handleSpeakSummary = () => {
    if (!data) return
    const msg = `¡Hola ${studentName}! Has completado ${data.metrics.totalCompletedLessons} lecciones escolares. Tienes ${data.metrics.totalPendingLessons} lecciones pendientes y tu avance general es de ${data.metrics.overallCompletionPercentage} por ciento. ¡Sigue adelante!`
    speakText(msg)
  }

  const handleRefreshClick = () => {
    setRefreshing(true)
    fetchDashboardSummary({ forceRefresh: true })
      .then((res) => {
        startTransition(() => {
          setData(res.data)
          setIsFromCache(res.fromCache)
          setRefreshing(false)
          setError(null)
        })
      })
      .catch((err: unknown) => {
        startTransition(() => {
          setRefreshing(false)
          setError(err instanceof Error ? err.message : 'No se pudo sincronizar el panel')
        })
      })
  }

  if (loading && !data) {
    return (
      <div className="dashboard-loading-state" role="status">
        <div className="dashboard-spinner" aria-hidden="true" />
        <p>Cargando tu panel de aprendizaje...</p>
      </div>
    )
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

  const equipped = parseEquippedAccessories(data?.student?.equippedAccessories ?? [])
  const nextLesson = data?.nextLesson

  return (
    <div className="student-dashboard" role="region" aria-label="Panel principal de estudio">
      {/* 1. Hero de Bienvenida y Guía Rupi */}
      <section className="dashboard-hero">
        <div className="dashboard-hero-content">
          <div className="dashboard-hero-text">
            <div className="dashboard-tagline">
              <span className="dashboard-pill">
                <LayoutDashboard size={14} aria-hidden="true" />
                <span>Panel Principal</span>
              </span>
              {isFromCache ? (
                <span className="sync-chip sync-cached" title="Cargado al instante desde memoria local">
                  ⚡ Datos rápidos
                </span>
              ) : (
                <span className="sync-chip sync-live" title="Sincronizado con el servidor">
                  ☁️ Al día
                </span>
              )}
            </div>

            <h1 className="dashboard-greeting">
              ¡Hola, <span className="greeting-name">{studentName}</span>! 👋
            </h1>
            <p className="dashboard-school-info">
              {data?.student?.gradeName || 'Primaria'} · {data?.student?.schoolName || 'I.E. Escolar'}
            </p>

            <div className="dashboard-speech-bubble" role="note">
              <Sparkles size={18} className="bubble-sparkle" aria-hidden="true" />
              <p>
                {metrics.totalCompletedLessons > 0
                  ? `¡Gran trabajo! Llevas ${metrics.totalCompletedLessons} lecciones superadas. Te faltan ${metrics.totalPendingLessons} para completar tus metas.`
                  : '¡Bienvenido a tu camino escolar! Empieza tu primera lección para encender tu racha.'}
              </p>
            </div>

            <div className="dashboard-hero-actions">
              <button
                type="button"
                className="action-btn action-audio"
                onClick={handleSpeakSummary}
                aria-label="Escuchar resumen de lecciones"
                title="Escuchar a Rupi leer mi resumen"
              >
                <Volume2 size={18} aria-hidden="true" />
                <span>Escuchar</span>
              </button>

              <button
                type="button"
                className={`action-btn action-sync ${refreshing ? 'is-spinning' : ''}`}
                onClick={handleRefreshClick}
                disabled={refreshing}
                aria-label="Sincronizar lecciones ahora"
                title="Actualizar datos con el servidor"
              >
                <RefreshCw size={18} aria-hidden="true" />
                <span>{refreshing ? 'Sincronizando...' : 'Actualizar'}</span>
              </button>
            </div>
          </div>

          <div className="dashboard-hero-avatar-col">
            <div className="dashboard-rupi-podium">
              <RupiCharacter
                className="dashboard-rupi-avatar"
                mood={metrics.totalCompletedLessons > 0 ? 'cheering' : 'idle'}
                accessories={equipped}
              />
              <button
                type="button"
                className="customize-avatar-link"
                onClick={onOpenStore}
                title="Personalizar mi Rupi con ropa y accesorios"
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
        <ScoreComparison refreshTrigger={refreshTrigger} />
      </section>

      {/* 3. Cuadrícula de Tarjetas Métricas (Recuento Exacto) */}
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
              onClick={onOpenLeaderboard}
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
            <button
              type="button"
              className="metric-link-btn"
              onClick={onOpenStore}
            >
              <Shirt size={14} aria-hidden="true" />
              <span>Tienda de Rupi</span>
            </button>
          </footer>
        </article>
      </section>

      {/* 3. Tarjeta de Acción: Siguiente Lección Recomendada */}
      <section className="dashboard-action-section" aria-label="Siguiente parada recomendada">
        {nextLesson ? (
          <div className="next-lesson-card">
            <div className="next-lesson-badge">
              <Sparkles size={16} aria-hidden="true" />
              <span>RECOMENDADO PARA CONTINUAR</span>
            </div>
            <div className="next-lesson-body">
              <div className="next-lesson-info">
                <span className="next-lesson-area-tag">
                  {nextLesson.areaName || 'Materia Escolar'}
                </span>
                <h2 className="next-lesson-title">
                  {nextLesson.nodeTitle}
                </h2>
                <p className="next-lesson-route-name">
                  Ruta: <strong>{nextLesson.routeTitle}</strong> (Parada #{nextLesson.sequence})
                </p>
                <div className="next-lesson-meta">
                  <span className="meta-pill">
                    <Clock size={14} aria-hidden="true" />
                    <span>{nextLesson.estimatedMinutes || 8} min aprox.</span>
                  </span>
                  <span className={`meta-pill state-${nextLesson.nodeState.toLowerCase()}`}>
                    {nextLesson.nodeState === 'EN_CURSO' ? 'En curso' : 'Lista para empezar'}
                  </span>
                </div>
              </div>

              <div className="next-lesson-action-col">
                <button
                  type="button"
                  className="continue-lesson-btn"
                  onClick={() => onContinueLesson(nextLesson.versionRouteId, nextLesson.nodeId)}
                >
                  <span>Continuar Lección</span>
                  <span className="btn-arrow" aria-hidden="true">→</span>
                </button>
              </div>
            </div>
          </div>
        ) : (
          <div className="next-lesson-empty">
            <Trophy size={36} className="empty-trophy" aria-hidden="true" />
            <div>
              <h3>¡Todas tus lecciones están al día! 🎉</h3>
              <p>Has completado las actividades de tus rutas actuales. Explora nuevas rutas en el catálogo.</p>
            </div>
            <button
              type="button"
              className="explore-more-btn"
              onClick={onNavigateToExplorar}
            >
              <Compass size={18} aria-hidden="true" />
              <span>Explorar Más Rutas</span>
            </button>
          </div>
        )}
      </section>

      {/* 4. Desglose de Avance por Materia Escolar */}
      <section className="dashboard-subjects-section" aria-label="Progreso por materias escolares">
        <div className="section-header">
          <div>
            <h2 className="section-title">Mis Materias y Rutas Escolares</h2>
            <p className="section-subtitle">
              Recuento detallado de lecciones por cada área curricular inscrita
            </p>
          </div>
          <button
            type="button"
            className="view-map-btn"
            onClick={onNavigateToCamino}
          >
            <Compass size={16} aria-hidden="true" />
            <span>Ver Mi Camino</span>
          </button>
        </div>

        {data?.subjects && data.subjects.length > 0 ? (
          <div className="subjects-grid">
            {data.subjects.map((sub) => (
              <div key={sub.versionRouteId} className="subject-progress-card">
                <div className="subject-card-header">
                  <span className={`subject-area-pill area-${sub.areaCode.toLowerCase()}`}>
                    {sub.areaName}
                  </span>
                  <span className="subject-grade-label">
                    {sub.gradeNumber}.° grado
                  </span>
                </div>

                <h3 className="subject-route-title">{sub.routeTitle}</h3>

                <div className="subject-stats-row">
                  <div className="stat-col">
                    <span className="stat-num completed">{sub.completedLessons}</span>
                    <span className="stat-desc">completadas</span>
                  </div>
                  <div className="stat-col">
                    <span className="stat-num pending">{sub.pendingLessons}</span>
                    <span className="stat-desc">pendientes</span>
                  </div>
                  <div className="stat-col">
                    <span className="stat-num total">{sub.totalLessons}</span>
                    <span className="stat-desc">total</span>
                  </div>
                </div>

                <div className="subject-bar-track" aria-hidden="true">
                  <div
                    className={`subject-bar-fill fill-${sub.areaCode.toLowerCase()}`}
                    style={{ width: `${Math.min(100, Math.max(0, sub.completionPercentage))}%` }}
                  />
                </div>

                <div className="subject-card-actions">
                  <span className="subject-percentage-label">
                    {sub.completionPercentage}% completado
                  </span>
                  <button
                    type="button"
                    className="open-route-btn"
                    onClick={() => onNavigateToRoute(sub.versionRouteId)}
                  >
                    <span>Abrir Camino</span>
                    <span aria-hidden="true">→</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-subjects-box">
            <p>No tienes rutas activas inscritas actualmente.</p>
            <button
              type="button"
              className="explore-more-btn"
              onClick={onNavigateToExplorar}
            >
              <Compass size={18} aria-hidden="true" />
              <span>Explorar Catálogo</span>
            </button>
          </div>
        )}
      </section>
    </div>
  )
}
