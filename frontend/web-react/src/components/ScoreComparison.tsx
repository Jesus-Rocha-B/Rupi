import React, { useEffect, useState } from 'react'
import { ArrowDownRight, ArrowRight, ArrowUpRight, Volume2 } from 'lucide-react'
import { fetchScoreComparison, type ScoreComparison as Comparison } from '../services/dailyGoalApi'
import { comparisonBars } from '../features/dashboard/scoreComparison'
import { speakText } from '../utils/speech'
import './ScoreComparison.css'

interface Props {
  refreshTrigger?: number
}

/** Comparación breve entre el puntaje de hoy y el de ayer (HU-36). */
export const ScoreComparison: React.FC<Props> = ({ refreshTrigger = 0 }) => {
  const [data, setData] = useState<Comparison | null>(null)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    fetchScoreComparison(controller.signal)
      .then((res) => {
        setData(res)
        setFailed(false)
      })
      .catch(() => {
        if (!controller.signal.aborted) setFailed(true)
      })
    return () => controller.abort()
  }, [refreshTrigger])

  if (failed) {
    return (
      <div className="score-compare-card" role="status">
        <p className="score-compare-empty">No pudimos comparar tu puntaje con el de ayer. Inténtalo al actualizar.</p>
      </div>
    )
  }
  if (!data) return null

  const bars = comparisonBars(data)
  const TrendIcon = data.trend === 'SUBE' ? ArrowUpRight : data.trend === 'BAJA' ? ArrowDownRight : ArrowRight

  return (
    <div className={`score-compare-card trend-${data.trend.toLowerCase()}`} role="region" aria-label="Comparación de mi puntaje de hoy con el de ayer">
      <div className="score-compare-head">
        <span className="score-compare-icon" aria-hidden="true"><TrendIcon size={20} /></span>
        <div>
          <h3>Hoy vs. ayer</h3>
          <p>{bars.label}</p>
        </div>
        <button type="button" className="score-compare-listen" onClick={() => speakText(bars.spoken)} aria-label="Escuchar la comparación de mi puntaje">
          <Volume2 size={18} aria-hidden="true" />
        </button>
      </div>

      <dl className="score-compare-bars">
        <div className="score-compare-row">
          <dt>Hoy</dt>
          <dd>
            <span className="score-bar"><span className="score-bar-fill is-today" style={{ width: `${bars.todayWidth}%` }} /></span>
            <strong>{data.todayExperience} XP</strong>
            <small>{data.todayLessons} {data.todayLessons === 1 ? 'parada' : 'paradas'}</small>
          </dd>
        </div>
        <div className="score-compare-row">
          <dt>Ayer</dt>
          <dd>
            <span className="score-bar"><span className="score-bar-fill is-yesterday" style={{ width: `${bars.yesterdayWidth}%` }} /></span>
            <strong>{data.yesterdayExperience} XP</strong>
            <small>{data.yesterdayLessons} {data.yesterdayLessons === 1 ? 'parada' : 'paradas'}</small>
          </dd>
        </div>
      </dl>

      <p className="score-compare-message">{data.message}</p>
    </div>
  )
}
