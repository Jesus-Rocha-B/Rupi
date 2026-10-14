import { useEffect, useMemo, useRef, useState } from 'react'
import { CircleCheck, Download, LoaderCircle, Printer, TriangleAlert, WifiOff } from 'lucide-react'
import { ApiError, downloadUnitPackage, type RouteDetail } from '../../services/learningRoutesApi'
import { downloadableUnits } from './unitDownloads'
import './UnitDownloadPanel.css'
import { OfficialStudyResources } from './OfficialStudyResources'

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
  const [scope, setScope] = useState<'unit' | 'node'>('node')
  const units = useMemo(() => scope === 'unit' ? downloadableUnits(route) : route.nodes.map(node => ({ key: node.id, title: `Parada ${node.sequence} · ${node.title}`, code: null, stops: 1 })), [route, scope])
  const [status, setStatus] = useState<Record<string, Status>>({})
  const controllers = useRef(new Map<string, AbortController>())

  useEffect(() => {
    const active = controllers.current
    return () => active.forEach(controller => controller.abort())
  }, [route.versionRouteId])

  if (route.nodes.length === 0) return null

  const download = async (key: string, title: string) => {
    const statusKey = `${scope}:${key}`
    controllers.current.get(statusKey)?.abort()
    const controller = new AbortController()
    controllers.current.set(statusKey, controller)
    setStatus(prev => ({ ...prev, [statusKey]: { kind: 'loading' } }))
    try {
      const pack = await downloadUnitPackage(route.versionRouteId, key, `rupi-${route.title}-${title}`, controller.signal, scope === 'node')
      if (controller.signal.aborted) return
      if (pack.verified === false) {
        setStatus(prev => ({ ...prev, [statusKey]: { kind: 'error', message: 'El archivo llegó incompleto. Vuelve a descargarlo.' } }))
        return
      }
      saveFile(pack.blob, pack.fileName)
      setStatus(prev => ({ ...prev, [statusKey]: { kind: 'done', fileName: pack.fileName, verified: pack.verified } }))
    } catch (error) {
      if (controller.signal.aborted) return
      setStatus(prev => ({ ...prev, [statusKey]: { kind: 'error', message: friendlyError(error) } }))
    } finally {
      if (controllers.current.get(statusKey) === controller) controllers.current.delete(statusKey)
    }
  }

  return (
    <section className="unit-downloads" aria-labelledby="unit-downloads-title">
      <header className="unit-downloads-head">
        <span className="unit-downloads-icon" aria-hidden="true">
          <WifiOff size={22} />
        </span>
        <div>
          <h4 id="unit-downloads-title">Llévate tus materiales</h4>
          <p>
            Descarga las lecturas y ejercicios para estudiar sin internet. Trae una guía para imprimir
            <Printer size={15} aria-hidden="true" className="inline-icon" /> y los materiales de tu profesor.
          </p>
        </div>
      </header>

      <p className="offline-instructions">Con internet, descarga tu paquete. Luego extrae todo el ZIP y abre <strong>ABRIR.html</strong> para estudiar sin conexión. Allí verás los materiales disponibles y cualquier archivo que falte. Tu avance en Rupi no cambia al practicar fuera de línea.</p>
      <label className="download-scope">Quiero descargar
        <select value={scope} onChange={event => setScope(event.target.value as 'unit' | 'node')}>
          <option value="node">Una parada del mapa</option>
          <option value="unit">Una unidad completa</option>
        </select>
      </label>
      {units.length === 0 && <p role="status">Tu profesor aún no agrupó las paradas en unidades. Puedes descargar cada parada por separado.</p>}
      <ul className="unit-downloads-list">
        {units.map(unit => {
          const state = status[`${scope}:${unit.key}`] ?? { kind: 'idle' }
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
                      {state.verified ? '¡Archivo verificado! ' : '¡Listo! '}
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
                aria-label={`Descargar ${scope === 'unit' ? 'la unidad ' : 'materiales de '}${unit.title}`}
              >
                {busy ? <LoaderCircle size={20} className="spin" aria-hidden="true" /> : <Download size={20} aria-hidden="true" />}
                <span>{busy ? 'Preparando' : state.kind === 'done' ? 'Otra vez' : 'Descargar'}</span>
              </button>
            </li>
          )
        })}
      </ul>
      <OfficialStudyResources key={route.versionRouteId} routeId={route.versionRouteId} />
    </section>
  )
}
