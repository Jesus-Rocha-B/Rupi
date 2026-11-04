import { useEffect, useState } from 'react'
import {
  AlertTriangle,
  Check,
  CheckCircle2,
  CircleOff,
  Mail,
  MessageSquare,
  Pause,
  Play,
  RefreshCw,
  RotateCcw,
  Settings2,
  Timer,
  Zap,
} from 'lucide-react'
import {
  AdminApiError,
  fetchIncidents,
  fetchPlatformHealth,
  formatDateTime,
  resolveIncident,
  restartService,
  testAlertChannels,
  triggerMonitoringCycle,
  type IncidentsResponse,
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
  const [incidentsData, setIncidentsData] = useState<IncidentsResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [paused, setPaused] = useState(false)
  const [tick, setTick] = useState(0)
  const [answeredTick, setAnsweredTick] = useState(-1)
  const checking = answeredTick !== tick

  // Estados de acciones operativas de emergencia (HU-47)
  const [testingAlerts, setTestingAlerts] = useState(false)
  const [alertFeedback, setAlertFeedback] = useState<string | null>(null)
  const [restartingKey, setRestartingKey] = useState<string | null>(null)
  const [actionFeedback, setActionFeedback] = useState<{ tipo: 'ok' | 'error'; mensaje: string } | null>(null)
  const [cycleLoading, setCycleLoading] = useState(false)
  const [incidentFilter, setIncidentFilter] = useState<'TODOS' | 'ABIERTOS'>('TODOS')
  const [resolvingId, setResolvingId] = useState<string | null>(null)

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
    Promise.all([
      fetchPlatformHealth(controller.signal),
      fetchIncidents(undefined, controller.signal),
    ])
      .then(([healthRes, incRes]) => {
        setHealth(healthRes)
        setIncidentsData(incRes)
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

  // 1. Probar canales de alerta (Correo y SMS/Webhook)
  const handleTestAlerts = async () => {
    setTestingAlerts(true)
    setAlertFeedback(null)
    setActionFeedback(null)
    try {
      const res = await testAlertChannels()
      const smsEstado = res.smsEnviado ? 'SMS/Webhook emitido' : 'SMS registrado en log'
      const emailEstado = res.emailEnviado ? `Correo enviado a ${res.destinatarioEmail}` : 'Fallo en envío de correo'
      setAlertFeedback(`✓ Prueba exitosa: ${emailEstado} · ${smsEstado}.`)
    } catch (err) {
      setActionFeedback({
        tipo: 'error',
        mensaje: err instanceof Error ? err.message : 'Error al probar los canales de alerta.',
      })
    } finally {
      setTestingAlerts(false)
    }
  }

  // 2. Disparar ciclo manual de monitoreo
  const handleTriggerCycle = async () => {
    setCycleLoading(true)
    setActionFeedback(null)
    try {
      const ciclo = await triggerMonitoringCycle()
      setTick(v => v + 1)
      const novedadesTxt = ciclo.novedades?.length
        ? `Novedades detectadas: ${ciclo.novedades.length} evento(s).`
        : 'Todos los servicios evaluados sin novedades críticas.'
      setActionFeedback({
        tipo: 'ok',
        mensaje: `Ciclo de monitoreo completado. Estado: ${ciclo.estadoGeneral}. ${novedadesTxt}`,
      })
    } catch (err) {
      setActionFeedback({
        tipo: 'error',
        mensaje: err instanceof Error ? err.message : 'Error al ejecutar el ciclo de monitoreo.',
      })
    } finally {
      setCycleLoading(false)
    }
  }

  // 3. Reiniciar servicio de emergencia
  const handleRestart = async (claveServicio: string) => {
    const claveParaScript = claveServicio === 'base' ? 'mysql' : claveServicio
    setRestartingKey(claveServicio)
    setActionFeedback(null)
    try {
      const res = await restartService(claveParaScript, true)
      setTick(v => v + 1)
      if (res.exito) {
        setActionFeedback({
          tipo: 'ok',
          mensaje: `✓ Servicio ${claveServicio.toUpperCase()} reiniciado correctamente en ${res.tiempoMs} ms.`,
        })
      } else {
        setActionFeedback({
          tipo: 'error',
          mensaje: `No se pudo reiniciar el servicio ${claveServicio}: ${res.mensaje}`,
        })
      }
    } catch (err) {
      setActionFeedback({
        tipo: 'error',
        mensaje: err instanceof Error ? err.message : `Error al reiniciar el servicio ${claveServicio}.`,
      })
    } finally {
      setRestartingKey(null)
    }
  }

  // 4. Resolver incidente manualmente
  const handleResolve = async (incidentId: string) => {
    setResolvingId(incidentId)
    try {
      await resolveIncident(incidentId, 'Resuelto manualmente por el administrador desde la consola.')
      setTick(v => v + 1)
      setActionFeedback({
        tipo: 'ok',
        mensaje: 'Incidente marcado como resuelto.',
      })
    } catch (err) {
      setActionFeedback({
        tipo: 'error',
        mensaje: err instanceof Error ? err.message : 'Error al resolver el incidente.',
      })
    } finally {
      setResolvingId(null)
    }
  }

  const canales = incidentsData?.canalesAlerta
  const incidentesLista = incidentsData?.incidentes ?? []
  const incidentesFiltrados = incidentFilter === 'ABIERTOS'
    ? incidentesLista.filter(i => i.estado === 'ABIERTO' || i.estado === 'EN_ATENCION')
    : incidentesLista

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
              <RefreshCw size={18} className={checking ? 'admin-spin' : undefined} aria-hidden="true" />
              {checking ? 'Revisando…' : 'Revisar ahora'}
            </button>
          </div>
        </div>

        {error && <p className="admin-feedback admin-feedback--error" role="alert">{error}</p>}
        {actionFeedback && (
          <p
            className={`admin-feedback admin-feedback--${actionFeedback.tipo === 'ok' ? 'success' : 'error'}`}
            role="status"
          >
            {actionFeedback.mensaje}
          </p>
        )}
        {alertFeedback && (
          <p className="admin-feedback admin-feedback--success" role="status">
            {alertFeedback}
          </p>
        )}

        {/* Panel de Canales de Alerta Temprana y Monitoreo Continuo (HU-47) */}
        {canales && (
          <div className="admin-infra-banner">
            <div>
              <h2 style={{ fontSize: '1.1rem', margin: 0, color: 'var(--adm-forest)' }}>
                Monitoreo continuo y canales de alerta temprana (HU-47)
              </h2>
              <p className="admin-muted" style={{ margin: '4px 0 0', fontSize: '0.9rem' }}>
                Vigilancia automática sobre la base de datos MySQL, el tutor de inteligencia artificial y la API central con medidas correctivas inmediatas.
              </p>
            </div>

            <div className="admin-channel-grid">
              <div className="admin-channel-card">
                <div className="admin-channel-icon" aria-hidden="true">
                  <Mail size={20} />
                </div>
                <div className="admin-channel-body">
                  <strong>Notificaciones por Correo</strong>
                  <p>{canales.emailResponsable}</p>
                  <span className="admin-channel-status">✓ Canal Activo</span>
                </div>
              </div>

              <div className="admin-channel-card">
                <div className="admin-channel-icon admin-channel-icon--sky" aria-hidden="true">
                  <MessageSquare size={20} />
                </div>
                <div className="admin-channel-body">
                  <strong>Alertas SMS / Webhook</strong>
                  <p>{canales.smsNumero ? canales.smsNumero : 'Simulado en consola'}</p>
                  <span className={`admin-channel-status ${canales.webhookConfigurado ? '' : 'admin-channel-status--simulated'}`}>
                    {canales.webhookConfigurado ? '✓ Webhook HTTP' : '✓ Registro Activo'}
                  </span>
                </div>
              </div>

              <div className="admin-channel-card">
                <div className="admin-channel-icon admin-channel-icon--sun" aria-hidden="true">
                  <Zap size={20} />
                </div>
                <div className="admin-channel-body">
                  <strong>Medidas de Autorreinicio</strong>
                  <p>Restablecimiento inmediato</p>
                  <span className="admin-channel-status">
                    {canales.autoreinicioHabilitado ? '✓ Scripts Activos' : '⚠ Deshabilitado'}
                  </span>
                </div>
              </div>
            </div>

            <div className="admin-infra-actions">
              <button
                type="button"
                className="admin-btn admin-btn--leaf"
                onClick={handleTestAlerts}
                disabled={testingAlerts}
              >
                <Mail size={16} aria-hidden="true" />
                {testingAlerts ? 'Enviando prueba…' : 'Probar canales de alerta (Email/SMS)'}
              </button>
              <button
                type="button"
                className="admin-btn admin-btn--quiet"
                onClick={handleTriggerCycle}
                disabled={cycleLoading}
              >
                <RotateCcw size={16} aria-hidden="true" />
                {cycleLoading ? 'Verificando…' : 'Ejecutar ciclo de monitoreo ahora'}
              </button>
            </div>
          </div>
        )}

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
            {health.servicios.map(service => (
              <ServiceCard
                key={service.clave}
                service={service}
                restarting={restartingKey === service.clave}
                onRestart={() => handleRestart(service.clave)}
              />
            ))}
          </ul>
        )}
      </section>

      {/* Registro de Incidentes de Infraestructura (HU-47) */}
      <section className="admin-card" aria-labelledby="admin-incidents-title">
        <div className="admin-section-head">
          <div>
            <h2 id="admin-incidents-title">Incidentes de infraestructura y medidas de emergencia</h2>
            <p className="admin-muted">
              Registro histórico de caídas, tiempos de indisponibilidad y resoluciones automáticas o manuales.
            </p>
          </div>
          <div className="admin-actions">
            <button
              type="button"
              className={`admin-btn ${incidentFilter === 'TODOS' ? 'admin-btn--sky' : 'admin-btn--quiet'}`}
              onClick={() => setIncidentFilter('TODOS')}
            >
              Todos ({incidentsData?.total ?? 0})
            </button>
            <button
              type="button"
              className={`admin-btn ${incidentFilter === 'ABIERTOS' ? 'admin-btn--primary' : 'admin-btn--quiet'}`}
              onClick={() => setIncidentFilter('ABIERTOS')}
            >
              Abiertos ({incidentsData?.abiertos ?? 0})
            </button>
          </div>
        </div>

        {incidentesFiltrados.length === 0 ? (
          <div className="admin-alert-empty" role="status">
            <CheckCircle2 size={24} aria-hidden="true" />
            <div>
              <strong>No hay incidentes {incidentFilter === 'ABIERTOS' ? 'abiertos en este momento' : 'registrados'}</strong>
              <p>Los servicios críticos no registran interrupciones pendientes de atención.</p>
            </div>
          </div>
        ) : (
          <div className="admin-incident-table-wrap">
            <table className="admin-matrix">
              <thead>
                <tr>
                  <th scope="col">Servicio</th>
                  <th scope="col">Severidad</th>
                  <th scope="col">Estado</th>
                  <th scope="col">Resumen</th>
                  <th scope="col">Iniciado</th>
                  <th scope="col">Duración</th>
                  <th scope="col">Acción</th>
                </tr>
              </thead>
              <tbody>
                {incidentesFiltrados.map(inc => (
                  <tr key={inc.id}>
                    <td>
                      <span className="admin-service-code">{inc.servicio.toUpperCase()}</span>
                    </td>
                    <td>
                      <span className={`admin-incident-badge admin-incident-badge--${inc.severidad.toLowerCase()}`}>
                        {inc.severidad}
                      </span>
                    </td>
                    <td>
                      <span className={`admin-incident-status admin-incident-status--${inc.estado.toLowerCase()}`}>
                        <span className={`admin-status-dot ${inc.estado === 'ABIERTO' ? 'admin-status-dot--pulse' : ''}`} />
                        {inc.estado}
                      </span>
                    </td>
                    <td>
                      <div style={{ maxWidth: '380px' }}>
                        <strong style={{ color: 'var(--adm-forest)', fontSize: '0.9rem' }}>{inc.resumen}</strong>
                        {inc.detalleError && (
                          <p className="admin-muted" style={{ fontSize: '0.82rem', margin: '2px 0 0' }}>
                            {inc.detalleError}
                          </p>
                        )}
                      </div>
                    </td>
                    <td>{formatDateTime(inc.iniciadoEn)}</td>
                    <td>{inc.duracionTexto || (inc.estado === 'ABIERTO' ? 'En curso' : '—')}</td>
                    <td>
                      {inc.estado === 'ABIERTO' || inc.estado === 'EN_ATENCION' ? (
                        <button
                          type="button"
                          className="admin-btn admin-btn--leaf admin-btn--resolve"
                          onClick={() => handleResolve(inc.id)}
                          disabled={resolvingId === inc.id}
                        >
                          <Check size={14} aria-hidden="true" />
                          {resolvingId === inc.id ? 'Marcando…' : 'Resolver'}
                        </button>
                      ) : (
                        <span className="admin-muted" style={{ fontSize: '0.85rem' }}>Resuelto</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Alertas Automáticas (Umbrales de Rendimiento) */}
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

function ServiceCard({
  service,
  restarting,
  onRestart,
}: {
  service: ServiceHealth
  restarting?: boolean
  onRestart: () => void
}) {
  const Icon = STATE_ICON[service.estado]
  const facts = serviceFacts(service)
  const puedeReiniciarse = ['base', 'ia', 'api'].includes(service.clave)
  const esCaido = service.estado === 'CAIDO'

  return (
    <li className={`admin-service admin-service--${service.estado.toLowerCase()}`}>
      <div className="admin-service-head">
        <h3>{service.nombre}</h3>
        <span className="admin-service-state">
          <Icon size={18} aria-hidden="true" />
          {STATE_TEXT[service.estado]}
        </span>
      </div>
      {service.latenciaMs !== null && service.estado !== 'CAIDO' && (
        <p className="admin-service-latency">{ms(service.latenciaMs)}<span> en responder</span></p>
      )}
      {typeof service.detalle.error === 'string' && (
        <p className="admin-service-error">{service.detalle.error}</p>
      )}
      {facts.length > 0 && (
        <dl className="admin-facts">
          {facts.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}
        </dl>
      )}
      {typeof service.detalle.segundosActiva === 'number' && service.clave === 'consola' && (
        <p className="admin-muted">Encendida hace {uptime(service.detalle.segundosActiva)}</p>
      )}

      {puedeReiniciarse && (
        <button
          type="button"
          className={`admin-btn admin-btn--mini-restart ${esCaido ? 'admin-btn--mini-danger' : 'admin-btn--quiet'}`}
          onClick={onRestart}
          disabled={restarting}
        >
          <RotateCcw size={14} className={restarting ? 'admin-spin' : undefined} aria-hidden="true" />
          {restarting
            ? 'Reiniciando…'
            : esCaido
            ? 'Reiniciar de emergencia'
            : 'Reiniciar servicio'}
        </button>
      )}
    </li>
  )
}
