export interface AccessoryItem {
  id: string
  codigo: string
  nombre: string
  descripcion: string
  tipo: 'SOMBRERO' | 'VESTIMENTA' | 'FONDO' | 'MARCO' | 'ANIMACION'
  costoExperiencia: number
  desbloqueado: boolean
  equipado: boolean
}

export interface StudentWardrobe {
  totalXp: number
  items: AccessoryItem[]
}

const DEFAULT_TIMEOUT_MS = 15000

function buildOptions(options: RequestInit = {}, signal?: AbortSignal): RequestInit {
  return {
    credentials: 'same-origin',
    redirect: 'error',
    ...options,
    headers: {
      'X-Requested-With': 'XMLHttpRequest',
      ...options.headers,
    },
    signal: signal
      ? AbortSignal.any([signal, AbortSignal.timeout(DEFAULT_TIMEOUT_MS)])
      : AbortSignal.timeout(DEFAULT_TIMEOUT_MS),
  }
}

export async function fetchWardrobe(signal?: AbortSignal): Promise<StudentWardrobe> {
  const res = await fetch('/api/v1/student/accessories', buildOptions({}, signal))
  if (!res.ok) {
    throw new Error('No se pudo cargar el armario de accesorios de Rupi.')
  }
  return res.json()
}

export async function unlockAccessory(codigo: string): Promise<AccessoryItem> {
  const res = await fetch(
    `/api/v1/student/accessories/${encodeURIComponent(codigo)}/unlock`,
    buildOptions({ method: 'POST' })
  )
  if (!res.ok) {
    const errorData = await res.json().catch(() => null)
    throw new Error(errorData?.message || 'No se pudo desbloquear el accesorio.')
  }
  return res.json()
}

export async function equipAccessory(codigo: string, equip = true): Promise<AccessoryItem[]> {
  const flag = encodeURIComponent(String(Boolean(equip)))
  const res = await fetch(
    `/api/v1/student/accessories/${encodeURIComponent(codigo)}/equip?equip=${flag}`,
    buildOptions({ method: 'PUT' })
  )
  if (!res.ok) {
    const errorData = await res.json().catch(() => null)
    throw new Error(errorData?.message || 'No se pudo equipar el accesorio.')
  }
  return res.json()
}

export async function fetchEquippedAccessories(signal?: AbortSignal): Promise<Record<string, string>> {
  const res = await fetch('/api/v1/student/accessories/equipped', buildOptions({}, signal))
  if (!res.ok) {
    return {}
  }
  return res.json()
}
