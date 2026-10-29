/** Lo que trae un ApiError de learningRoutesApi; se lee por forma para probarlo sin Vite. */
interface SessionEndInfo {
  status?: number
  sessionEnd?: string | null
  sessionNewClient?: string | null
}

/**
 * HU-43: Rupi permite una sola sesión por cuenta. Si la cuenta se abre en otro lugar, esta pantalla lo explica
 * con palabras de aula en vez de mostrar el ingreso sin más. Devuelve null si el 401 no trae motivo.
 */
export function sessionEndMessage(error: unknown): string | null {
  if (!error || typeof error !== 'object') return null
  const info = error as SessionEndInfo
  if (info.status !== 401 || !info.sessionEnd) return null
  if (info.sessionEnd === 'OTRO_DISPOSITIVO') {
    const place = info.sessionNewClient === 'MOVIL' ? 'en un celular' : 'en otra computadora o tableta'
    return `Tu cuenta se abrió ${place}, así que aquí cerramos la sesión para cuidar tu avance. Si fuiste tú, todo está bien; si no, avísale a tu profe.`
  }
  if (info.sessionEnd === 'EXPIRADA') return 'Pasó mucho tiempo y tu sesión terminó. Vuelve a ingresar para seguir.'
  return 'Tu sesión se cerró. Vuelve a ingresar con tu usuario y contraseña.'
}
