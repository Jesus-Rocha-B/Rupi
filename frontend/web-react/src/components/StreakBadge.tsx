import { useEffect, useState } from 'react'
import { Flame } from 'lucide-react'
import { fetchStudyStreak, type StudyStreak } from '../services/streakApi'
import './StreakBadge.css'

type Props = {
  /** Paradas completadas de la ruta abierta: al cambiar, la racha se vuelve a consultar. */
  completedNodes: number
}

function daysLabel(days: number) {
  return days === 1 ? '1 día seguido' : `${days} días seguidos`
}

/** Historia 30: racha de días consecutivos junto a Rupi. Celebra el hábito sin castigar los días sin estudio. */
export function StreakBadge({ completedNodes }: Props) {
  const [streak, setStreak] = useState<StudyStreak | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    fetchStudyStreak(controller.signal)
      .then(setStreak)
      // Si la racha no carga, Rupi sigue acompañando sin mostrar un error que distraiga.
      .catch(() => { if (!controller.signal.aborted) setStreak(null) })
    return () => controller.abort()
  }, [completedNodes])

  if (!streak) return null

  const lit = streak.days > 0
  const title = lit ? daysLabel(streak.days) : 'Tu racha empieza hoy'
  const hint = !lit
    ? 'Completa una actividad para empezarla.'
    : streak.activeToday
      ? '¡Hoy ya sumaste un día!'
      : 'Una actividad hoy y tu racha sigue.'

  return (
    <div className={`streak-badge${lit ? ' is-lit' : ''}${streak.atRisk ? ' is-waiting' : ''}`} role="status" aria-live="polite">
      <span className="streak-badge-flame" aria-hidden="true">
        <Flame size={20} strokeWidth={2.4} />
      </span>
      <span className="streak-badge-text">
        <strong>{title}</strong>
        <span>{hint}</span>
      </span>
    </div>
  )
}
