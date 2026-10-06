import type { NodeState } from '../../services/learningRoutesApi'

export const nodeStateLabels: Record<NodeState, string> = {
  COMPLETADO: '¡Lo lograste!',
  EN_CURSO: '¡Sigue por aquí!',
  DISPONIBLE: '¡Puedes entrar!',
  BLOQUEADO: 'Aún no',
}
