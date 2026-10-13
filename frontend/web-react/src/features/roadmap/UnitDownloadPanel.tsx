import { useEffect, useMemo, useRef, useState } from 'react'
import { CircleCheck, Download, LoaderCircle, Printer, TriangleAlert, WifiOff } from 'lucide-react'
import { ApiError, downloadUnitPackage, type RouteDetail } from '../../services/learningRoutesApi'
import { downloadableUnits } from './unitDownloads'
import './UnitDownloadPanel.css'

type Status =
  | { kind: 'idle' }
  | { kind: 'loading' }
  | { kind: 'done'; fileName: string; verified: boolean | null }
  | { kind: 'error'; message: string }

function friendlyError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401) return 'Tu sesión terminó. Vuelve a entrar para descargar.'
    if (error.status === 404 || error.status === 409) return error.message
  }
  if (error instanceof DOMException && error.name === 'TimeoutError') {
    return 'La descarga tardó mucho. Revisa tu conexión e inténtalo otra vez.'
  }
  return 'No pudimos preparar tu paquete. Inténtalo otra vez en un ratito.'
}

function saveFile(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export function UnitDownloadPanel({ route }: { route: RouteDetail }) {
  const units = useMemo(() => downloadableUnits(route), [route])
  const [status, setStatus] = useState<Record<string, Status>>({})
  const controllers = useRef(new Map<string, AbortController>())

  useEffect(() => {
    const active = controllers.current
    return () => active.forEach(controller => controller.abort())
  }, [route.versionRouteId])

  if (units.length === 0) return null

  const download = async (key: string, title: string) => {
    controllers.current.get(key)?.abort()
    const controller = new AbortController()
    controllers.current.set(key, controller)
    setStatus(prev => ({ ...prev, [key]: { kind: 'loading' } }))
    try {
      const pack = await downloadUnitPackage(route.versionRouteId, key, `rupi-${route.title}-${title}`, controller.signal)
      if (pack.verified === false) {
        setStatus(prev => ({ ...prev, [key]: { kind: 'error', message: 'El archivo llegó incompleto. Vuelve a descargarlo.' } }))
        return
      }
      saveFile(pack.blob, pack.fileName)
      setStatus(prev => ({ ...prev, [key]: { kind: 'done', fileName: pack.fileName, verified: pack.verified } }))
    } catch (error) {
      if (controller.signal.aborted) return
      setStatus(prev => ({ ...prev, [key]: { kind: 'error', message: friendlyError(error) } }))
    } finally {
      if (controllers.current.get(key) === controller) controllers.current.delete(key)
    }
  }

  return (
    <section className="unit-downloads" aria-labelledby="unit-downloads-title">
      <header className="unit-downloads-head">
        <span className="unit-downloads-icon" aria-hidden="true">
          <WifiOff size={22} />
        </span>
        <div>
          <h4 id="unit-downloads-title">Llévate tu unidad</h4>
          <p>
            Descarga las lecturas y ejercicios para estudiar sin internet. Trae una guía para imprimir
            <Printer size={15} aria-hidden="true" className="inline-icon" /> y los materiales de tu profesor.
          </p>
        </div>
      </header>

      <ul className="unit-downloads-list">
        {units.map(unit => {
          const state = status[unit.key] ?? { kind: 'idle' }
          const busy = state.kind === 'loading'
          return (
            <li key={unit.key} className={`unit-download-card is-${state.kind}`}>
              <div className="unit-download-text">
                <span className="unit-download-title">{unit.title}</span>
                <span className="unit-download-meta">
                  {unit.code ? `${unit.code} · ` : ''}
                  {unit.stops} {unit.stops === 1 ? 'parada' : 'paradas'}
                </span>
                <span className="unit-download-status" role="status" aria-live="polite">
                  {state.kind === 'loading' && 'Preparando tu paquete…'}
                  {state.kind === 'done' && (
                    <>
                      <CircleCheck size={15} aria-hidden="true" />
                      {state.verified ? '¡Listo y completo! ' : '¡Listo! '}
                      Búscalo en tus descargas como {state.fileName}
                    </>
                  )}
                  {state.kind === 'error' && (
                    <>
                      <TriangleAlert size={15} aria-hidden="true" />
                      {state.message}
                    </>
                  )}
                </span>
              </div>
              <button
                type="button"
                className="unit-download-btn"
                onClick={() => download(unit.key, unit.title)}
                disabled={busy}
                aria-busy={busy}
                aria-label={`Descargar la unidad ${unit.title}`}
              >
                {busy ? <LoaderCircle size={20} className="spin" aria-hidden="true" /> : <Download size={20} aria-hidden="true" />}
                <span>{busy ? 'Preparando' : state.kind === 'done' ? 'Otra vez' : 'Descargar'}</span>
              </button>
            </li>
          )
        })}
      </ul>
    </section>
  )
}
