import React, { useState, useEffect, useTransition } from 'react'
import {
  Check,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Flame,
  Sparkles,
  Target,
  Trophy,
} from 'lucide-react'
import {
  fetchDailyGoalOverview,
  updateDailyGoalTarget,
  type DailyGoalOverview,
} from '../services/dailyGoalApi'
import { speakText } from '../utils/speech'
import './DailyGoalRing.css'

interface Props {
  initialData?: DailyGoalOverview | null
  compact?: boolean
  refreshTrigger?: number
  onGoalUpdated?: (data: DailyGoalOverview) => void
}

const RADIUS = 54
const CIRCUMFERENCE = 2 * Math.PI * RADIUS // ~339.29

export const DailyGoalRing: React.FC<Props> = ({
  initialData,
  compact = false,
  refreshTrigger = 0,
  onGoalUpdated,
}) => {
  const [data, setData] = useState<DailyGoalOverview | null>(initialData ?? null)
  const [loading, setLoading] = useState(!initialData)
  const [updating, setUpdating] = useState(false)
  const [showConfig, setShowConfig] = useState(false)
  const [, startTransition] = useTransition()

  useEffect(() => {
    let ignore = false
    fetchDailyGoalOverview()
      .then((res) => {
        if (ignore) return
        startTransition(() => {
          setData(res)
          setLoading(false)
          onGoalUpdated?.(res)
        })
      })
      .catch(() => {
        if (ignore) return
        startTransition(() => {
          setLoading(false)
        })
      })

    return () => {
      ignore = true
    }
  }, [refreshTrigger, onGoalUpdated])

  const handleSelectTarget = (target: number) => {
    if (updating || data?.currentDailyTargetSetting === target) return
    setUpdating(true)
    updateDailyGoalTarget(target)
      .then((res) => {
        startTransition(() => {
          setData(res)
          setUpdating(false)
          onGoalUpdated?.(res)
        })
      })
      .catch(() => {
        startTransition(() => {
          setUpdating(false)
        })
      })
  }

  const handleSpeakMotivation = () => {
    if (!data) return
    speakText(data.today.motivationalMessage)
  }

  if (loading && !data) {
    return (
      <div className={`daily-goal-card ${compact ? 'is-compact' : ''} is-loading`} role="status">
        <div className="daily-goal-spinner" aria-hidden="true" />
        <p>Cargando meta del día...</p>
      </div>
    )
  }

  const today = data?.today ?? {
    date: '',
    targetLessons: 3,
    completedLessons: 0,
    pendingLessons: 3,
    percentage: 0,
    isGoalMet: false,
    metAt: null,
    motivationalMessage: '¡Inicia tu primera lección de hoy!',
  }

  const clampedFraction = Math.min(1, Math.max(0, today.completedLessons / (today.targetLessons || 1)))
  const strokeDashoffset = CIRCUMFERENCE * (1 - clampedFraction)

  if (compact) {
    return (
      <div className="daily-goal-compact" role="region" aria-label="Progreso de meta diaria">
        <div className="compact-ring-wrap" title={`Meta de hoy: ${today.completedLessons} de ${today.targetLessons} lecciones`}>
          <svg className="compact-svg-ring" viewBox="0 0 72 72" aria-hidden="true">
            <circle
              className="ring-bg"
              cx="36"
              cy="36"
              r="28"
            />
            <circle
              className={`ring-progress ${today.isGoalMet ? 'goal-met' : ''}`}
              cx="36"
              cy="36"
              r="28"
              strokeDasharray={2 * Math.PI * 28}
              strokeDashoffset={2 * Math.PI * 28 * (1 - clampedFraction)}
            />
          </svg>
          <div className="compact-inner-icon">
            {today.isGoalMet ? (
              <Trophy size={16} className="trophy-gold" />
            ) : (
              <span className="compact-ratio">{today.completedLessons}/{today.targetLessons}</span>
            )}
          </div>
        </div>

        <div className="compact-text-col">
          <div className="compact-header-row">
            <span className="compact-label">Meta de Hoy</span>
            {today.isGoalMet && <span className="compact-done-badge">¡Lograda!</span>}
          </div>
          <span className="compact-subtext">
            {today.isGoalMet
              ? '¡Excelente constancia!'
              : `${today.pendingLessons} parada${today.pendingLessons === 1 ? '' : 's'} restante${today.pendingLessons === 1 ? '' : 's'}`}
          </span>
        </div>
      </div>
    )
  }

  return (
    <div
      className={`daily-goal-card ${today.isGoalMet ? 'goal-completed-card' : ''}`}
      role="region"
      aria-label="Indicador gráfico de progreso y meta diaria"
    >
      <div className="goal-card-top">
        <div className="goal-header-title">
          <span className="goal-icon-badge" aria-hidden="true">
            {today.isGoalMet ? <Trophy size={20} /> : <Target size={20} />}
          </span>
          <div>
            <h3 className="goal-title">Meta de Aprendizaje del Día</h3>
            <p className="goal-subtitle">Avanza lecciones hoy para encender tu constancia</p>
          </div>
        </div>

        <button
          type="button"
          className="goal-config-toggle"
          onClick={() => setShowConfig((prev) => !prev)}
          title="Ajustar número de paradas de mi meta"
          aria-expanded={showConfig}
        >
          <span>Meta: {today.targetLessons}</span>
          {showConfig ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
        </button>
      </div>

      {/* Selector desplegable de objetivo diario */}
      {showConfig && (
        <div className="goal-target-selector" role="group" aria-label="Selecciona tu meta diaria">
          <span className="selector-prompt">¿Cuántas paradas deseas completar por día?</span>
          <div className="selector-buttons">
            {(data?.availableTargetOptions ?? [1, 2, 3, 5]).map((option) => (
              <button
                key={option}
                type="button"
                className={`target-pill-btn ${today.targetLessons === option ? 'is-selected' : ''}`}
                disabled={updating}
                onClick={() => handleSelectTarget(option)}
              >
                <span>{option} {option === 1 ? 'parada' : 'paradas'}</span>
                {option === 3 && <small className="recommended-tag">Recomendado</small>}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Cuerpo principal con Anillo Circular de Progreso */}
      <div className="goal-body-row">
        <div className="goal-ring-wrapper">
          <svg className="goal-svg-ring" viewBox="0 0 136 136" aria-hidden="true">
            <circle
              className="ring-bg"
              cx="68"
              cy="68"
              r={RADIUS}
            />
            <circle
              className={`ring-progress ${today.isGoalMet ? 'goal-met' : ''}`}
              cx="68"
              cy="68"
              r={RADIUS}
              strokeDasharray={CIRCUMFERENCE}
              strokeDashoffset={strokeDashoffset}
            />
          </svg>

          <div className="goal-ring-center">
            {today.isGoalMet ? (
              <div className="center-completed-badge">
                <Sparkles size={20} className="sparkle-icon" />
                <span className="goal-center-percent">100%</span>
                <span className="goal-center-caption">¡Logrado!</span>
              </div>
            ) : (
              <div className="center-progress-text">
                <span className="goal-center-count">
                  {today.completedLessons}<small>/{today.targetLessons}</small>
                </span>
                <span className="goal-center-percent">{today.percentage}%</span>
              </div>
            )}
          </div>
        </div>

        <div className="goal-details-col">
          {/* Barra de progreso táctil segmentada */}
          <div className="segmented-track-container">
            <div className="segmented-track" aria-hidden="true">
              {Array.from({ length: today.targetLessons }).map((_, idx) => {
                const isFilled = idx < today.completedLessons
                return (
                  <div
                    key={idx}
                    className={`track-segment ${isFilled ? 'segment-filled' : ''} ${today.isGoalMet ? 'segment-gold' : ''}`}
                  >
                    {isFilled && <Check size={12} strokeWidth={3} />}
                  </div>
                )
              })}
            </div>
            <span className="segmented-counter-text">
              {today.isGoalMet
                ? '¡Meta del día superada con éxito!'
                : `${today.completedLessons} de ${today.targetLessons} lecciones completadas`}
            </span>
          </div>

          {/* Mensaje motivador pedagógico */}
          <button
            type="button"
            className="goal-motivation-bubble"
            onClick={handleSpeakMotivation}
            title="Escuchar mensaje motivacional"
          >
            <Flame size={16} className="motivation-flame" aria-hidden="true" />
            <p>{today.motivationalMessage}</p>
          </button>
        </div>
      </div>

      {/* Historial semanal de 7 días */}
      {data?.weeklyHistory && data.weeklyHistory.length > 0 && (
        <div className="weekly-history-strip" role="group" aria-label="Historial de metas de la semana">
          <span className="strip-title">Tu semana de estudio:</span>
          <div className="strip-days-row">
            {data.weeklyHistory.map((day) => (
              <div
                key={day.date}
                className={`history-day-chip ${day.isToday ? 'is-current-day' : ''} ${day.isGoalMet ? 'day-met' : ''}`}
                title={`${day.date}: ${day.completedLessons} de ${day.targetLessons} lecciones ${day.isGoalMet ? '(Meta cumplida)' : ''}`}
              >
                <span className="day-name">{day.dayOfWeek}</span>
                <div className="day-indicator">
                  {day.isGoalMet ? (
                    <CheckCircle2 size={16} className="check-icon" />
                  ) : day.completedLessons > 0 ? (
                    <span className="partial-badge">{day.completedLessons}</span>
                  ) : (
                    <span className="empty-dot" />
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
