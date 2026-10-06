import type { NodeState } from '../../services/learningRoutesApi'

export const nodeStateLabels: Record<NodeState, string> = {
  COMPLETADO: 'Completado',
  EN_CURSO: 'En curso',
  DISPONIBLE: 'Disponible',
  BLOQUEADO: 'Bloqueado',
}
