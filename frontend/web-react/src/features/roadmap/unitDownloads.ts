import type { RouteDetail } from '../../services/learningRoutesApi'

export const LOOSE_UNIT_KEY = 'otras'

export type DownloadableUnit = {
  key: string
  title: string
  code: string | null
  stops: number
}

/** Unidades del mapa que se pueden descargar, en el mismo orden que las bandas. */
export function downloadableUnits(route: RouteDetail): DownloadableUnit[] {
  const units = route.curriculum?.units ?? []
  return units
    .map(unit => ({
      key: unit.id ?? LOOSE_UNIT_KEY,
      title: unit.title,
      code: unit.id && unit.code !== 'OTRAS' ? unit.code : null,
      stops: unit.totalNodes ?? unit.nodeIds?.length ?? unit.nodes?.length ?? 0,
    }))
    .filter(unit => unit.stops > 0)
}

export function unitPackagePath(versionRouteId: string, unitKey: string): string {
  return `student/learning-routes/${encodeURIComponent(versionRouteId)}/units/${encodeURIComponent(unitKey)}/package`
}

/** Nombre del archivo según Content-Disposition; si no viene, uno seguro a partir del respaldo. */
export function fileNameFromDisposition(header: string | null, fallback: string): string {
  const encoded = header?.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
  if (encoded) {
    try {
      return decodeURIComponent(encoded.trim())
    } catch {
      // Cae al nombre simple
    }
  }
  const plain = header?.match(/filename="?([^";]+)"?/i)?.[1]
  if (plain) return plain.trim()
  const safe = fallback.normalize('NFD').replace(/\p{M}/gu, '').replace(/[^A-Za-z0-9._-]+/g, '-').replace(/-+/g, '-')
  return `${safe.replace(/^-|-$/g, '').toLowerCase() || 'rupi-unidad'}.zip`
}

export async function sha256Hex(data: ArrayBuffer): Promise<string | null> {
  if (!globalThis.crypto?.subtle) return null
  const digest = await globalThis.crypto.subtle.digest('SHA-256', data)
  return Array.from(new Uint8Array(digest), byte => byte.toString(16).padStart(2, '0')).join('')
}
