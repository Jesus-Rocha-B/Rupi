import { useEffect, useState, useCallback } from 'react'
import {
  AdminApiError,
  fetchArchitectureDashboard,
  type ArchitectureDashboardData,
  type TimeSeriesPoint,
} from '../../services/adminApi'
import { SESSION_LOST } from './accountStatus'

export type RefreshInterval = 5000 | 15000 | 30000 | 0
export type TimeWindow = '24h' | '7d'

export interface UseArchitectureDashboardOptions {
  onSessionLost: (message: string) => void
  initialInterval?: RefreshInterval
  initialTimeWindow?: TimeWindow
}

export interface UseArchitectureDashboardReturn {
  data: ArchitectureDashboardData | null
  error: string | null
  refreshInterval: RefreshInterval
  setRefreshInterval: React.Dispatch<React.SetStateAction<RefreshInterval>>
  timeWindow: TimeWindow
  setTimeWindow: React.Dispatch<React.SetStateAction<TimeWindow>>
  checking: boolean
  activePointIndex: number | null
  setActivePointIndex: React.Dispatch<React.SetStateAction<number | null>>
  resumen: ArchitectureDashboardData['resumen'] | undefined
  serie: TimeSeriesPoint[]
  refreshNow: () => void
}

/**
 * Hook personalizado (Custom Hook Pattern) para el Tablero de Arquitectura y Uptime (HU-48).
 * Desacopla el ciclo de vida de sondeo en tiempo real, control de intervalos,
 * manejo de cancelación de peticiones (AbortController) y gestión de errores de la vista presentacional.
 */
export function useArchitectureDashboard({
  onSessionLost,
  initialInterval = 15000,
  initialTimeWindow = '24h',
}: UseArchitectureDashboardOptions): UseArchitectureDashboardReturn {
  const [data, setData] = useState<ArchitectureDashboardData | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [refreshInterval, setRefreshInterval] = useState<RefreshInterval>(initialInterval)
  const [timeWindow, setTimeWindow] = useState<TimeWindow>(initialTimeWindow)
  const [tick, setTick] = useState(0)
  const [answeredTick, setAnsweredTick] = useState(-1)
  const [activePointIndex, setActivePointIndex] = useState<number | null>(null)

  const checking = answeredTick !== tick

  // Sondeo en tiempo real respetando la visibilidad del documento
  useEffect(() => {
    if (refreshInterval === 0) return
    const timer = setInterval(() => {
      if (document.visibilityState === 'visible') {
        setTick(v => v + 1)
      }
    }, refreshInterval)
    return () => clearInterval(timer)
  }, [refreshInterval])

  // Carga asíncrona cancelable de métricas de arquitectura
  useEffect(() => {
    const controller = new AbortController()
    fetchArchitectureDashboard(controller.signal)
      .then(res => {
        setData(res)
        setError(null)
        setAnsweredTick(tick)
      })
      .catch(err => {
        if (controller.signal.aborted) return
        setAnsweredTick(tick)
        if (err instanceof AdminApiError && err.status === 401) {
          onSessionLost(err.message || SESSION_LOST)
        } else {
          setError(
            err instanceof AdminApiError
              ? err.message
              : 'No se pudo cargar el tablero de arquitectura.'
          )
        }
      })
    return () => controller.abort()
  }, [tick, onSessionLost])

  const refreshNow = useCallback(() => {
    setTick(v => v + 1)
  }, [])

  const resumen = data?.resumen
  const serie = timeWindow === '24h' ? (data?.serieTemporal24h ?? []) : (data?.serieTemporal7d ?? [])

  return {
    data,
    error,
    refreshInterval,
    setRefreshInterval,
    timeWindow,
    setTimeWindow,
    checking,
    activePointIndex,
    setActivePointIndex,
    resumen,
    serie,
    refreshNow,
  }
}
