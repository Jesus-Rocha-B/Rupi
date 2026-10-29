import { useEffect, useState } from 'react'
import { AlertTriangle, CheckCircle2, CircleOff, Pause, Play, RefreshCw, Settings2, Timer } from 'lucide-react'
import {
  AdminApiError,
  fetchPlatformHealth,
  formatDateTime,
  type PlatformHealth,
  type ServiceHealth,
  type ServiceState,
} from '../../services/adminApi'
import { SESSION_LOST } from './accountStatus'
import { healthHeadline, ms, serviceFacts, uptime } from './healthText'

interface Props {
  onSessionLost: (message: string) => void
}

const REFRESH_MS = 30_000

const STATE_TEXT: Record<ServiceState, string> = {
  OK: 'Responde bien',
  LENTO: 'Lento',
  CAIDO: 'No responde',
  SIN_CONFIGURAR: 'Sin configurar',
}

const STATE_ICON = { OK: CheckCircle2, LENTO: Timer, CAIDO: CircleOff, SIN_CONFIGURAR: Settings2 }

export function HealthView({ onSessionLost }: Props) {
  const [health, setHealth] = useState<PlatformHealth | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [paused, setPaused] = useState(false)
  const [tick, setTick] = useState(0)
  // «Revisando» mientras la revisión pedida (tick) todavía no respondió.
  const [answeredTick, setAnsweredTick] = useState(-1)
  const checking = answeredTick !== tick

  // Se revisa al abrir, cada 30 s mientras la pestaña está visible y al pulsar «Revisar ahora».
  useEffect(() => {
    if (paused) return
    const timer = setInterval(() => {
      if (document.visibilityState === 'visible') setTick(value => value + 1)
    }, REFRESH_MS)
    return () => clearInterval(timer)
  }, [paused])

  useEffect(() => {
    const controller = new AbortController()
    fetchPlatformHealth(controller.signal)
      .then(result => {
        setHealth(result)
        setError(null)
        setAnsweredTick(tick)
      })
      .catch(failure => {
        if (controller.signal.aborted) return
        setAnsweredTick(tick)
        if (failure instanceof AdminApiError && failure.status === 401) onSessionLost(failure.message || SESSION_LOST)
        else setError(failure instanceof AdminApiError ? failure.message : 'No pudimos revisar la plataforma.')
      })
    return () => controller.abort()
  }, [tick, onSessionLost])

  const api = health?.servicios.find(item => item.clave === 'api')
  const headline = health ? healthHeadline(health) : null

  return (
    <div className="admin-health">
      <section className="admin-card" aria-labelledby="admin-health-title">
        <div className="admin-section-head">
          <div>
            <h1 id="admin-health-title">Salud de la plataforma</h1>
            <p className="admin-muted">
              {health ? `Revisado ${formatDateTime(health.revisadoEn)}` : 'Revisando…'}
              {paused ? ' · revisión automática en pausa' : ' · se revisa sola cada 30 segundos'}
            </p>
          </div>
          <div className="admin-actions">
            <button type="button" className="admin-btn admin-btn--quiet" onClick={() => setPaused(value => !value)}>
              {paused ? <Play size={18} aria-hidden="true" /> : <Pause size={18} aria-hidden="true" />}
              {paused ? 'Reanudar' : 'Pausar'}
            </button>
            <button type="button" className="admin-btn admin-btn--sky" onClick={() => setTick(value => value + 1)} disabled={checking}>
              <RefreshCw size={18} aria-hidden="true" />
              {checking ? 'Revisando…' : 'Revisar ahora'}
            </button>
          </div>
        </div>

        {error && <p className="admin-feedback admin-feedback--error" role="alert">{error}</p>}

        {health && headline && (
          <div className={`admin-health-banner admin-health-banner--${health.estado.toLowerCase()}`} role="status" aria-live="polite">
            {health.estado === 'OK' ? <CheckCircle2 size={28} aria-hidden="true" /> : <AlertTriangle size={28} aria-hidden="true" />}
            <div>
              <strong>{headline.title}</strong>
              <p>{headline.body}</p>
            </div>
          </div>
        )}

        {health && (
          <ul className="admin-service-grid">
            {health.servicios.map(service => <ServiceCard key={service.clave} service={service} />)}
          </ul>
        )}
      </section>

      {health && (
        <section className="admin-card" aria-labelledby="admin-alerts-title">
          <div className="admin-section-head">
            <div>
              <h2 id="admin-alerts-title">Alertas automáticas del sistema</h2>
              <p className="admin-muted">
                Detección temprana de saturación de CPU/RAM, demoras de red y fallos inminentes para proteger las clases escolares.
              </p>
            </div>
            <span
              className={`admin-badge admin-badge--${
                health.alertas && health.alertas.length > 0
                  ? health.alertas.some(a => a.nivel === 'CRITICA')
                    ? 'error'
                    : 'warn'
                  : 'success'
              }`}
            >
              {!health.alertas || health.alertas.length === 0
                ? 'Sin alertas'
                : `${health.alertas.length} alerta(s)`}
            </span>
          </div>

          {!health.alertas || health.alertas.length === 0 ? (
            <div className="admin-alert-empty" role="status">
              <CheckCircle2 size={24} aria-hidden="true" />
              <div>
                <strong>Todos los servicios operan dentro de los umbrales normales</strong>
                <p>El uso de CPU, memoria RAM y tiempos de respuesta se mantienen estables para las clases escolares.</p>
              </div>
            </div>
          ) : (
            <ul className="admin-alert-list" role="list">
              {health.alertas.map(alerta => (
                <li key={alerta.id} className={`admin-alert-item admin-alert-item--${alerta.nivel.toLowerCase()}`}>
                  <div className="admin-alert-head">
                    <span className="admin-alert-title">{alerta.titulo}</span>
                    <span className={`admin-alert-badge admin-alert-badge--${alerta.nivel.toLowerCase()}`}>
                      {alerta.nivel} · {alerta.servicio}
                    </span>
                  </div>
                  <p className="admin-alert-desc">{alerta.mensaje}</p>
                  {(alerta.impacto || alerta.sugerencia) && (
                    <div className="admin-alert-extra">
                      {alerta.impacto && <p><strong>Impacto:</strong> {alerta.impacto}</p>}
                      {alerta.sugerencia && <p><strong>Acción sugerida:</strong> {alerta.sugerencia}</p>}
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {api?.peticiones && (
        <section className="admin-card" aria-labelledby="admin-timing-title">
          <h2 id="admin-timing-title">Tiempos de respuesta de la API · últimos 15 minutos</h2>
          <p className="admin-muted">
            El p95 es el tiempo que no supera el 95 % de las peticiones: si sube, alguien en el aula ya está esperando.
          </p>
          <dl className="admin-timing-tiles">
            <div><dt>Peticiones</dt><dd>{api.peticiones.ventana.requests}</dd></div>
            <div><dt>Por minuto</dt><dd>{api.peticiones.ventana.perMinute}</dd></div>
            <div><dt>Mediana</dt><dd>{ms(api.peticiones.ventana.p50Ms)}</dd></div>
            <div className={api.peticiones.ventana.p95Ms >= (health?.umbrales.p95LentoMs ?? Infinity) ? 'is-warn' : undefined}>
              <dt>p95</dt><dd>{ms(api.peticiones.ventana.p95Ms)}</dd>
            </div>
            <div><dt>Máximo</dt><dd>{ms(api.peticiones.ventana.maxMs)}</dd></div>
            <div className={api.peticiones.ventana.serverErrors > 0 ? 'is-warn' : undefined}>
              <dt>Errores 5xx</dt><dd>{api.peticiones.ventana.serverErrors}</dd>
            </div>
          </dl>
          <h3 className="admin-subtitle">Rutas más lentas desde que arrancó la API</h3>
          {api.peticiones.rutasLentas.length === 0 ? (
            <p className="admin-empty-line">Todavía nadie ha usado la API desde que arrancó.</p>
          ) : (
            <div className="admin-table-wrap" role="region" aria-label="Rutas más lentas" tabIndex={0}>
              <table className="admin-matrix admin-timing-table">
                <caption className="admin-sr-only">Rutas ordenadas por p95, de la más lenta a la más rápida</caption>
                <thead>
                  <tr>
                    <th scope="col">Ruta</th>
                    <th scope="col">Peticiones</th>
                    <th scope="col">Mediana</th>
                    <th scope="col">p95</th>
                    <th scope="col">Máximo</th>
                    <th scope="col">Errores 5xx</th>
                  </tr>
                </thead>
                <tbody>
                  {api.peticiones.rutasLentas.map(route => (
                    <tr key={route.route}>
                      <th scope="row"><code>{route.route}</code></th>
                      <td>{route.requests}</td>
                      <td>{ms(route.p50Ms)}</td>
                      <td className={route.p95Ms >= (health?.umbrales.p95LentoMs ?? Infinity) ? 'is-warn' : undefined}>{ms(route.p95Ms)}</td>
                      <td>{ms(route.maxMs)}</td>
                      <td className={route.serverErrors > 0 ? 'is-warn' : undefined}>{route.serverErrors}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {health && (
            <p className="admin-muted admin-thresholds">
              Umbrales: lento si una revisión tarda {health.umbrales.latenciaLentaMs} ms o más, si el p95 llega a{' '}
              {health.umbrales.p95LentoMs} ms, si la CPU supera el {health.umbrales.cpuAlertaPercent ?? 70} % o si más del{' '}
              {Math.round(health.umbrales.erroresAlerta * 100)} % de las peticiones falla.
            </p>
          )}
        </section>
      )}
    </div>
  )
}

function ServiceCard({ service }: { service: ServiceHealth }) {
  const Icon = STATE_ICON[service.estado]
  const facts = serviceFacts(service)
  return (
    <li className={`admin-service admin-service--${service.estado.toLowerCase()}`}>
      <div className="admin-service-head">
        <h3>{service.nombre}</h3>
        <span className="admin-service-state">
          <Icon size={18} aria-hidden="true" />
          {STATE_TEXT[service.estado]}
        </span>
      </div>
      {service.latenciaMs !== null && service.estado !== 'CAIDO' && <p className="admin-service-latency">{ms(service.latenciaMs)}<span> en responder</span></p>}
      {typeof service.detalle.error === 'string' && <p className="admin-service-error">{service.detalle.error}</p>}
      {facts.length > 0 && (
        <dl className="admin-facts">
          {facts.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}
        </dl>
      )}
      {typeof service.detalle.segundosActiva === 'number' && service.clave === 'consola' && (
        <p className="admin-muted">Encendida hace {uptime(service.detalle.segundosActiva)}</p>
      )}
    </li>
  )
}
