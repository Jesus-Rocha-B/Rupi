import { useEffect, useMemo, useRef, useState } from 'react'
import {
  BookOpen,
  CheckCircle2,
  Download,
  FolderDown,
  Layers,
  LoaderCircle,
  Printer,
  Sparkles,
  TriangleAlert,
  WifiOff,
} from 'lucide-react'
import { ApiError, downloadUnitPackage, type RouteDetail } from '../../services/learningRoutesApi'
import { downloadableUnits, LOOSE_UNIT_KEY } from './unitDownloads'
import { RupiCharacter } from '../../components/RupiCharacter'
import { OfficialStudyResources } from './OfficialStudyResources'
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
  // Predeterminado en 'unit' para mostrar un único paquete claro y no saturar con 6 tarjetas repetidas
  const [scope, setScope] = useState<'unit' | 'node'>('unit')

  const availableUnits = useMemo(() => {
    const list = downloadableUnits(route)
    if (list.length > 0) return list
    // Respaldo para rutas sin unidades explícitas: paquete completo de la ruta
    return [
      {
        key: LOOSE_UNIT_KEY,
        title: `Cuaderno completo de ${route.area?.name || 'la ruta'}`,
        code: null,
        stops: route.nodes.length,
      },
    ]
  }, [route])

  const nodesList = useMemo(() => {
    return route.nodes.map((node) => ({
      key: node.id,
      title: `Parada ${node.sequence} · ${node.title}`,
      code: null,
      stops: 1,
    }))
  }, [route.nodes])

  const items = scope === 'unit' ? availableUnits : nodesList
  const [status, setStatus] = useState<Record<string, Status>>({})
  const controllers = useRef(new Map<string, AbortController>())

  useEffect(() => {
    const active = controllers.current
    return () => active.forEach((controller) => controller.abort())
  }, [route.versionRouteId])

  if (route.nodes.length === 0) return null

  const download = async (key: string, title: string) => {
    const statusKey = `${scope}:${key}`
    controllers.current.get(statusKey)?.abort()
    const controller = new AbortController()
    controllers.current.set(statusKey, controller)
    setStatus((prev) => ({ ...prev, [statusKey]: { kind: 'loading' } }))
    try {
      const pack = await downloadUnitPackage(
        route.versionRouteId,
        key,
        `rupi-${route.title}-${title}`,
        controller.signal,
        scope === 'node'
      )
      if (controller.signal.aborted) return
      if (pack.verified === false) {
        setStatus((prev) => ({
          ...prev,
          [statusKey]: { kind: 'error', message: 'El archivo llegó incompleto. Vuelve a descargarlo.' },
        }))
        return
      }
      saveFile(pack.blob, pack.fileName)
      setStatus((prev) => ({
        ...prev,
        [statusKey]: { kind: 'done', fileName: pack.fileName, verified: pack.verified },
      }))
    } catch (error) {
      if (controller.signal.aborted) return
      setStatus((prev) => ({ ...prev, [statusKey]: { kind: 'error', message: friendlyError(error) } }))
    } finally {
      if (controllers.current.get(statusKey) === controller) controllers.current.delete(statusKey)
    }
  }

  return (
    <section className="unit-downloads" aria-labelledby="unit-downloads-title">
      {/* Cabecera amigable tipo cuaderno escolar */}
      <header className="unit-downloads-head">
        <div className="unit-downloads-icon-disc" aria-hidden="true">
          <BookOpen size={24} />
        </div>
        <div className="unit-downloads-titles">
          <span className="unit-downloads-kicker">
            <WifiOff size={14} aria-hidden="true" />
            <span>ESTUDIA EN CASA SIN INTERNET</span>
          </span>
          <h4 id="unit-downloads-title">Mochila Escolar para Llevar</h4>
          <p className="unit-downloads-desc">
            Descarga las fichas y actividades de esta ruta para seguir practicando en tu tableta o computadora cuando no tengas conexión.
          </p>
        </div>
      </header>

      {/* Selector limpio de modo: Todo el cuaderno (1 clic) vs Por paradas sueltas */}
      <div className="download-scope-switcher" role="tablist" aria-label="Modo de descarga">
        <button
          type="button"
          role="tab"
          aria-selected={scope === 'unit'}
          className={`scope-switch-tab ${scope === 'unit' ? 'active' : ''}`}
          onClick={() => setScope('unit')}
        >
          <FolderDown size={16} aria-hidden="true" />
          <span>Cuaderno completo de la ruta</span>
          <span className="scope-tab-pill">Recomendado</span>
        </button>

        <button
          type="button"
          role="tab"
          aria-selected={scope === 'node'}
          className={`scope-switch-tab ${scope === 'node' ? 'active' : ''}`}
          onClick={() => setScope('node')}
        >
          <Layers size={16} aria-hidden="true" />
          <span>Elegir una parada específica</span>
        </button>
      </div>

      {/* Lista de descarga: si es 'unit', es 1 tarjeta destacada; si es 'node', grilla ordenada */}
      <div className={`unit-downloads-container ${scope === 'unit' ? 'single-package-mode' : 'nodes-grid-mode'}`}>
        {items.map((item) => {
          const state = status[`${scope}:${item.key}`] ?? { kind: 'idle' }
          const busy = state.kind === 'loading'
          return (
            <article key={item.key} className={`unit-download-card is-${state.kind}`}>
              <div className="unit-download-main-info">
                <div className="download-item-icon" aria-hidden="true">
                  {scope === 'unit' ? <FolderDown size={24} /> : <BookOpen size={20} />}
                </div>
                <div className="unit-download-text">
                  <strong className="unit-download-title">{item.title}</strong>
                  <span className="unit-download-meta">
                    {item.code ? `${item.code} · ` : ''}
                    {item.stops} {item.stops === 1 ? 'parada de aprendizaje' : 'paradas en este paquete'}
                  </span>

                  {/* Estado de la descarga */}
                  {state.kind === 'loading' && (
                    <span className="unit-download-status loading" role="status" aria-live="polite">
                      <LoaderCircle size={15} className="spin" aria-hidden="true" />
                      <span>Preparando tu paquete escolar...</span>
                    </span>
                  )}
                  {state.kind === 'done' && (
                    <span className="unit-download-status done" role="status" aria-live="polite">
                      <CheckCircle2 size={16} aria-hidden="true" />
                      <span>¡Listo! Guardado como {state.fileName}</span>
                    </span>
                  )}
                  {state.kind === 'error' && (
                    <span className="unit-download-status error" role="status" aria-live="polite">
                      <TriangleAlert size={15} aria-hidden="true" />
                      <span>{state.message}</span>
                    </span>
                  )}
                </div>
              </div>

              <button
                type="button"
                className={`unit-download-action-btn ${busy ? 'busy' : state.kind === 'done' ? 'done' : ''}`}
                onClick={() => download(item.key, item.title)}
                disabled={busy}
                aria-busy={busy}
                aria-label={`Descargar ${item.title}`}
              >
                {busy ? (
                  <LoaderCircle size={18} className="spin" aria-hidden="true" />
                ) : (
                  <Download size={18} aria-hidden="true" />
                )}
                <span>
                  {busy
                    ? 'Preparando...'
                    : state.kind === 'done'
                    ? 'Descargar otra vez'
                    : scope === 'unit'
                    ? 'Descargar cuaderno (ZIP)'
                    : 'Descargar'}
                </span>
              </button>
            </article>
          )
        })}
      </div>

      {/* Mensaje motivador y cercano de Rupi para niños y familias */}
      <aside className="offline-friendly-tip" aria-label="Consejo para estudiar sin internet">
        <div className="tip-rupi-avatar" aria-hidden="true">
          <RupiCharacter mood="cheering" className="tip-rupi-svg" />
        </div>
        <div className="tip-text-content">
          <strong>
            <Sparkles size={14} aria-hidden="true" />
            Consejo de Rupi para tu hogar
          </strong>
          <p>
            Al descargar estos materiales, podrás ver tus lecturas y fichas en tu tableta o computadora sin gastar datos.
            Si tienes impresora en casa o en tu colegio, puedes pedir ayuda a un adulto para imprimirlas con el icono{' '}
            <Printer size={13} aria-hidden="true" className="inline-print-icon" /> y resolverlas con lápiz.
          </p>
        </div>
      </aside>

      {/* Biblioteca oficial del MINEDU integrada limpiamente */}
      <OfficialStudyResources key={route.versionRouteId} routeId={route.versionRouteId} />
    </section>
  )
}
