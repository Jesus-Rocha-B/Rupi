import { STATE_LABELS, type AccountSummary, type Administrator } from '../../services/adminApi'

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

/** Misma regla que identidad/roles.py: se avisa antes de enviar para no depender del error del servidor. */
export function roleProblem(selected: string[], account: Pick<AccountSummary, 'id' | 'roles'>, admin: Administrator) {
  if (selected.length === 0) return 'Toda cuenta necesita al menos un rol.'
  if (selected.includes('ESTUDIANTE') && selected.length > 1) {
    return 'Una cuenta de estudiante no puede tener otros roles: es de una niña o un niño.'
  }
  if (account.id === admin.id && account.roles.includes('ADMINISTRADOR') && !selected.includes('ADMINISTRADOR')) {
    return 'No puedes quitarte tu propio rol de administración.'
  }
  return null
}
