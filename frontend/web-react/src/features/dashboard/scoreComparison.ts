import type { ScoreComparison } from '../../services/dailyGoalApi.ts'

/** Datos de presentación de la comparación hoy vs. ayer (HU-36). */
export interface ComparisonBars {
  todayWidth: number
  yesterdayWidth: number
  label: string
  spoken: string
}

const TREND_LABEL: Record<ScoreComparison['trend'], string> = {
  SUBE: 'Vas mejor que ayer',
  IGUAL: 'Vas igual que ayer',
  BAJA: 'Aún puedes alcanzar tu ritmo de ayer',
}

/** Ancho de cada barra en porcentaje del mayor puntaje; una barra con XP nunca baja de 6 % para que se vea. */
export function comparisonBars(comparison: ScoreComparison): ComparisonBars {
  const max = Math.max(comparison.todayExperience, comparison.yesterdayExperience)
  const width = (value: number) => (max === 0 || value === 0 ? 0 : Math.max(6, Math.round((value * 100) / max)))
  return {
    todayWidth: width(comparison.todayExperience),
    yesterdayWidth: width(comparison.yesterdayExperience),
    label: TREND_LABEL[comparison.trend],
    spoken: `Hoy sumaste ${comparison.todayExperience} XP y ayer ${comparison.yesterdayExperience} XP. ${comparison.message}`,
  }
}
