import { STATE_LABELS, type AccountSummary } from '../../services/adminApi'

export const SESSION_LOST = 'Tu sesión de administración terminó. Vuelve a ingresar.'

export function stateTone(account: Pick<AccountSummary, 'estado' | 'bloqueada'>) {
  if (account.estado === 'DESHABILITADO') return 'off'
  if (account.bloqueada) return 'warn'
  return account.estado === 'ACTIVO' ? 'ok' : 'neutral'
}

export function stateLabel(account: Pick<AccountSummary, 'estado' | 'bloqueada'>) {
  if (account.estado !== 'DESHABILITADO' && account.bloqueada) return STATE_LABELS.BLOQUEADO
  return STATE_LABELS[account.estado]
}
