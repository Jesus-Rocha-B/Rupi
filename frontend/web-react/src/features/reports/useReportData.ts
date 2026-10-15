import { useEffect, useState } from 'react'
import { reportsResponse } from '../../services/reportsApi'

export function useReportData<T>(path: string) {
  const [attempt, setAttempt] = useState(0)
  const key = `${path}:${attempt}`
  const [result, setResult] = useState<{ key: string; data?: T; error?: string } | null>(null)
  useEffect(() => {
    const controller = new AbortController()
    reportsResponse(path, controller.signal).then(response => response.json()).then((data: T) => {
      if (!controller.signal.aborted) setResult({ key, data })
    }).catch(() => {
      if (!controller.signal.aborted) setResult({ key, error: 'No pudimos consultar el reporte. Comprueba tu conexión y el acceso al aula.' })
    })
    return () => controller.abort()
  }, [path, key])
  return { data: result?.key === key ? result.data : undefined, error: result?.key === key ? result.error : undefined,
    retry: () => setAttempt(value => value + 1) }
}
