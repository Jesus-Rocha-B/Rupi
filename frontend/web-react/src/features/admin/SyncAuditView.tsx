import { useEffect, useState } from 'react'
import {
  ArrowLeftRight,
  CheckCircle2,
  Monitor,
  Pause,
  Play,
  RefreshCw,
  Smartphone,
  XCircle,
} from 'lucide-react'
import {
  AdminApiError,
  fetchSyncAudit,
  formatDateTime,
  type SyncAuditData,
  type SyncDeviceComparison,
  type SyncTrafficEvent,
} from '../../services/adminApi'
import { SESSION_LOST } from './accountStatus'

interface Props {
  onSessionLost: (message: string) => void
}

const REFRESH_MS = 30_000

export function SyncAuditView({ onSessionLost }: Props) {
  const [data, setData] = useState<SyncAuditData | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [paused, setPaused] = useState(false)
  const [tick, setTick] = useState(0)
  const [answeredTick, setAnsweredTick] = useState(-1)
  const [activeTab, setActiveTab] = useState<'comparativa' | 'bitacora'>('comparativa')

  const checking = answeredTick !== tick

  useEffect(() => {
    if (paused) return
    const timer = setInterval(() => {
      if (document.visibilityState === 'visible') setTick(value => value + 1)
    }, REFRESH_MS)
    return () => clearInterval(timer)
  }, [paused])

  useEffect(() => {
    const controller = new AbortController()
    fetchSyncAudit(controller.signal)
      .then(result => {
        setData(result)
        setError(null)
        setAnsweredTick(tick)
      })
      .catch(failure => {
        if (controller.signal.aborted) return
        setAnsweredTick(tick)
        if (failure instanceof AdminApiError && failure.status === 401) onSessionLost(failure.message || SESSION_LOST)
        else setError(failure instanceof AdminApiError ? failure.message : 'No pudimos consultar la auditoría de sincronización.')
      })
    return () => controller.abort()
  }, [tick, onSessionLost])

  return (
    <div className="admin-health">
      <section className="admin-card" aria-labelledby="admin-sync-title">
        <div className="admin-section-head">
          <div>
            <h1 id="admin-sync-title">Auditoría de sincronización · Web y Móvil</h1>
            <p className="admin-muted">
              {data ? `Auditado ${formatDateTime(data.revisadoEn)}` : 'Auditando…'}
              {paused ? ' · sondeo en pausa' : ' · se actualiza cada 30 segundos'}
            </p>
          </div>
          <div className="admin-actions">
            <button type="button" className="admin-btn admin-btn--quiet" onClick={() => setPaused(value => !value)}>
              {paused ? <Play size={18} aria-hidden="true" /> : <Pause size={18} aria-hidden="true" />}
              {paused ? 'Reanudar' : 'Pausar'}
            </button>
            <button
              type="button"
              className="admin-btn admin-btn--sky"
              onClick={() => setTick(value => value + 1)}
              disabled={checking}
            >
              <RefreshCw size={18} aria-hidden="true" />
              {checking ? 'Auditando…' : 'Auditar ahora'}
            </button>
          </div>
        </div>

        {error && <p className="admin-feedback admin-feedback--error" role="alert">{error}</p>}

        {data && (
          <dl className="admin-timing-tiles" aria-label="Resumen de sincronización">
            <div>
              <dt>Tráfico total (24 h)</dt>
              <dd>{data.resumen.totalTrafico}</dd>
            </div>
            <div>
              <dt>Tráfico Web</dt>
              <dd>{data.resumen.traficoWeb}</dd>
            </div>
            <div>
              <dt>Tráfico Celular</dt>
              <dd>{data.resumen.traficoMovil}</dd>
            </div>
            <div className={data.resumen.tasaExitoPercent < 95.0 ? 'is-warn' : undefined}>
              <dt>Tasa de éxito</dt>
              <dd>{data.resumen.tasaExitoPercent} %</dd>
            </div>
            <div className={data.resumen.discrepanciasDetectadas > 0 ? 'is-warn' : undefined}>
              <dt>Discrepancias</dt>
              <dd>{data.resumen.discrepanciasDetectadas}</dd>
            </div>
            <div className={data.resumen.eventosError > 0 ? 'is-warn' : undefined}>
              <dt>Errores</dt>
              <dd>{data.resumen.eventosError}</dd>
            </div>
          </dl>
        )}
      </section>

      {data && (
        <section className="admin-card" aria-labelledby="admin-sync-alerts-title">
          <div className="admin-section-head">
            <div>
              <h2 id="admin-sync-alerts-title">Alertas automáticas de sincronización</h2>
              <p className="admin-muted">
                Detección inmediata de desfases en puntajes XP, lecciones no reflejadas o fallos de entrega entre dispositivos.
              </p>
            </div>
            <span
              className={`admin-badge admin-badge--${
                data.alertas.length > 0
                  ? data.alertas.some(a => a.nivel === 'CRITICA')
                    ? 'error'
                    : 'warn'
                  : 'success'
              }`}
            >
              {data.alertas.length === 0 ? 'Sin alertas activas' : `${data.alertas.length} alerta(s)`}
            </span>
          </div>

          {data.alertas.length === 0 ? (
            <div className="admin-alert-empty" role="status">
              <CheckCircle2 size={24} aria-hidden="true" />
              <div>
                <strong>Puntajes y lecciones sincronizados en ambos lados</strong>
                <p>Las computadoras de aula y las tabletas escolares reportan el mismo avance para todos los estudiantes.</p>
              </div>
            </div>
          ) : (
            <ul className="admin-alert-list" role="list">
              {data.alertas.map(alerta => (
                <li key={alerta.id} className={`admin-alert-item admin-alert-item--${alerta.nivel.toLowerCase()}`}>
                  <div className="admin-alert-head">
                    <span className="admin-alert-title">{alerta.titulo}</span>
                    <span className={`admin-alert-badge admin-alert-badge--${alerta.nivel.toLowerCase()}`}>
                      {alerta.nivel}
                    </span>
                  </div>
                  <p className="admin-alert-desc">{alerta.mensaje}</p>
                  {(alerta.impacto || alerta.sugerencia) && (
                    <div className="admin-alert-extra">
                      {alerta.impacto && <p><strong>Impacto:</strong> {alerta.impacto}</p>}
                      {alerta.sugerencia && <p><strong>Acción correctiva:</strong> {alerta.sugerencia}</p>}
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {data && (
        <section className="admin-card" aria-labelledby="admin-sync-details-title">
          <div className="admin-section-head">
            <h2 id="admin-sync-details-title">Detalle de sincronización</h2>
            <div className="admin-actions">
              <button
                type="button"
                className={`admin-btn ${activeTab === 'comparativa' ? 'admin-btn--sky' : 'admin-btn--quiet'}`}
                onClick={() => setActiveTab('comparativa')}
              >
                <ArrowLeftRight size={16} aria-hidden="true" />
                Comparativa por estudiante ({data.comparaciones.length})
              </button>
              <button
                type="button"
                className={`admin-btn ${activeTab === 'bitacora' ? 'admin-btn--sky' : 'admin-btn--quiet'}`}
                onClick={() => setActiveTab('bitacora')}
              >
                Tráfico en tiempo real ({data.eventosRecientes.length})
              </button>
            </div>
          </div>

          {activeTab === 'comparativa' ? (
            <ComparativaTable comparaciones={data.comparaciones} />
          ) : (
            <BitacoraTable eventos={data.eventosRecientes} />
          )}
        </section>
      )}
    </div>
  )
}

function ComparativaTable({ comparaciones }: { comparaciones: SyncDeviceComparison[] }) {
  if (comparaciones.length === 0) {
    return <p className="admin-empty-line">No hay registros de estudiantes para auditar sincronización.</p>
  }

  return (
    <div className="admin-table-wrap" role="region" aria-label="Comparativa de sincronización" tabIndex={0}>
      <table className="admin-matrix">
        <caption className="admin-sr-only">Comparativa de puntajes y avance entre Web y Celular</caption>
        <thead>
          <tr>
            <th scope="col">Estudiante</th>
            <th scope="col">Base de Datos (XP / Lecciones)</th>
            <th scope="col">Reporte Web</th>
            <th scope="col">Reporte Celular</th>
            <th scope="col">Estado</th>
            <th scope="col">Desfase</th>
          </tr>
        </thead>
        <tbody>
          {comparaciones.map(item => {
            const hasIssue = item.estado === 'DISCREPANCIA'
            return (
              <tr key={item.usuarioId} className={hasIssue ? 'is-warn' : undefined}>
                <th scope="row">
                  <strong>{item.nombreCompleto}</strong>
                  <div className="admin-muted">@{item.usuario}</div>
                </th>
                <td>
                  <strong>{item.puntajeBd} XP</strong>
                  <div className="admin-muted">{item.leccionesBd} lecciones</div>
                </td>
                <td>
                  <span className="admin-device-tag">
                    <Monitor size={14} aria-hidden="true" />
                    Web: {item.reporteWeb.puntajeXp} XP ({item.reporteWeb.lecciones} lec.)
                  </span>
                </td>
                <td>
                  <span className="admin-device-tag">
                    <Smartphone size={14} aria-hidden="true" />
                    Celular: {item.reporteMovil.puntajeXp} XP ({item.reporteMovil.lecciones} lec.)
                  </span>
                </td>
                <td>
                  <span
                    className={`admin-badge admin-badge--${
                      item.estado === 'DISCREPANCIA'
                        ? 'error'
                        : item.estado === 'SINCRONIZADO'
                        ? 'success'
                        : 'neutral'
                    }`}
                  >
                    {item.estado === 'DISCREPANCIA' ? 'Desfase' : item.estado === 'SINCRONIZADO' ? 'Sincronizado' : 'Alineado'}
                  </span>
                </td>
                <td>
                  {item.diferenciaXp > 0 || item.diferenciaLecciones > 0 ? (
                    <strong className="admin-text-danger">
                      {item.diferenciaXp > 0 ? `Δ ${item.diferenciaXp} XP` : ''}
                      {item.diferenciaLecciones > 0 ? ` · Δ ${item.diferenciaLecciones} lec.` : ''}
                    </strong>
                  ) : (
                    <span className="admin-muted">0 (exacto)</span>
                  )}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

function BitacoraTable({ eventos }: { eventos: SyncTrafficEvent[] }) {
  if (eventos.length === 0) {
    return <p className="admin-empty-line">Aún no se ha registrado tráfico de sincronización en la plataforma.</p>
  }

  return (
    <div className="admin-table-wrap" role="region" aria-label="Bitácora de eventos de tráfico" tabIndex={0}>
      <table className="admin-matrix">
        <caption className="admin-sr-only">Eventos de tráfico bidireccional entre web y celular</caption>
        <thead>
          <tr>
            <th scope="col">Hora</th>
            <th scope="col">Dispositivo</th>
            <th scope="col">Estudiante</th>
            <th scope="col">Tipo de Evento</th>
            <th scope="col">Puntaje / Avance</th>
            <th scope="col">Resultado</th>
          </tr>
        </thead>
        <tbody>
          {eventos.map(evento => (
            <tr key={evento.id}>
              <td>{formatDateTime(evento.recibidoEn)}</td>
              <td>
                <span className="admin-device-tag">
                  {evento.clienteTipo === 'MOVIL' ? (
                    <>
                      <Smartphone size={14} aria-hidden="true" /> Celular
                    </>
                  ) : (
                    <>
                      <Monitor size={14} aria-hidden="true" /> Web
                    </>
                  )}
                </span>
              </td>
              <td>
                <strong>{evento.nombre}</strong>
                <div className="admin-muted">@{evento.usuario}</div>
              </td>
              <td>
                <code>{evento.tipoEvento}</code>
              </td>
              <td>
                <strong>{evento.puntajeXp} XP</strong>
                <span className="admin-muted"> · {evento.leccionesCompletadas} lecciones</span>
              </td>
              <td>
                <span
                  className={`admin-badge admin-badge--${
                    evento.estado === 'APLICADO'
                      ? 'success'
                      : evento.estado === 'DUPLICADO'
                      ? 'neutral'
                      : 'error'
                  }`}
                >
                  {evento.estado === 'APLICADO' ? (
                    <CheckCircle2 size={12} aria-hidden="true" />
                  ) : evento.estado === 'ERROR' ? (
                    <XCircle size={12} aria-hidden="true" />
                  ) : null}
                  {evento.estado}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
