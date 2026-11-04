import { useEffect, useState } from 'react'
import {
  Activity,
  AlertOctagon,
  BarChart3,
  CheckCircle2,
  Clock,
  Database,
  Pause,
  Play,
  RefreshCw,
  Server,
  Smartphone,
  Sparkles,
} from 'lucide-react'
import {
  AdminApiError,
  fetchArchitectureDashboard,
  formatDateTime,
  type ArchitectureDashboardData,
  type TimeSeriesPoint,
} from '../../services/adminApi'
import { SESSION_LOST } from './accountStatus'

interface Props {
  onSessionLost: (message: string) => void
}

type RefreshInterval = 5000 | 15000 | 30000 | 0
type TimeWindow = '24h' | '7d'

export function ArchitectureDashboardView({ onSessionLost }: Props) {
  const [data, setData] = useState<ArchitectureDashboardData | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [refreshInterval, setRefreshInterval] = useState<RefreshInterval>(15000)
  const [timeWindow, setTimeWindow] = useState<TimeWindow>('24h')
  const [tick, setTick] = useState(0)
  const [answeredTick, setAnsweredTick] = useState(-1)
  const [activePointIndex, setActivePointIndex] = useState<number | null>(null)

  const checking = answeredTick !== tick

  // Sondeo en tiempo real según intervalo seleccionado
  useEffect(() => {
    if (refreshInterval === 0) return
    const timer = setInterval(() => {
      if (document.visibilityState === 'visible') {
        setTick(v => v + 1)
      }
    }, refreshInterval)
    return () => clearInterval(timer)
  }, [refreshInterval])

  // Carga de datos de arquitectura y uptime
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
          setError(err instanceof AdminApiError ? err.message : 'No se pudo cargar el tablero de arquitectura.')
        }
      })
    return () => controller.abort()
  }, [tick, onSessionLost])

  const resumen = data?.resumen
  const serie = timeWindow === '24h' ? (data?.serieTemporal24h ?? []) : (data?.serieTemporal7d ?? [])

  return (
    <div className="admin-architecture-dashboard">
      {/* Encabezado Principal y Controles de Tiempo Real (HU-48) */}
      <section className="admin-card" aria-labelledby="admin-arch-title">
        <div className="admin-section-head">
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <h1 id="admin-arch-title">Tablero de Arquitectura y Uptime</h1>
              <span className={`admin-live-badge ${refreshInterval === 0 ? 'is-paused' : ''}`}>
                <span className="admin-live-dot" />
                {refreshInterval === 0 ? 'Pausado' : 'En vivo'}
              </span>
            </div>
            <p className="admin-muted">
              {data ? `Actualizado ${formatDateTime(data.revisadoEn)}` : 'Conectando con el servidor…'}
              {' · '}Monitoreo constante de calidad técnica y fluidez escolar en aulas y tabletas.
            </p>
          </div>

          <div className="admin-actions admin-arch-controls">
            <div className="admin-pill-selector" role="group" aria-label="Intervalo de actualización">
              <button
                type="button"
                className={`admin-pill-btn ${refreshInterval === 5000 ? 'is-active' : ''}`}
                onClick={() => setRefreshInterval(5000)}
                title="Actualizar cada 5 segundos"
              >
                5s
              </button>
              <button
                type="button"
                className={`admin-pill-btn ${refreshInterval === 15000 ? 'is-active' : ''}`}
                onClick={() => setRefreshInterval(15000)}
                title="Actualizar cada 15 segundos"
              >
                15s
              </button>
              <button
                type="button"
                className={`admin-pill-btn ${refreshInterval === 30000 ? 'is-active' : ''}`}
                onClick={() => setRefreshInterval(30000)}
                title="Actualizar cada 30 segundos"
              >
                30s
              </button>
              <button
                type="button"
                className={`admin-pill-btn ${refreshInterval === 0 ? 'is-active' : ''}`}
                onClick={() => setRefreshInterval(v => (v === 0 ? 15000 : 0))}
                title={refreshInterval === 0 ? 'Reanudar actualización' : 'Pausar actualización'}
              >
                {refreshInterval === 0 ? <Play size={13} aria-hidden="true" /> : <Pause size={13} aria-hidden="true" />}
              </button>
            </div>

            <button
              type="button"
              className="admin-btn admin-btn--sky"
              onClick={() => setTick(v => v + 1)}
              disabled={checking}
            >
              <RefreshCw size={18} className={checking ? 'admin-spin' : undefined} aria-hidden="true" />
              {checking ? 'Actualizando…' : 'Actualizar ahora'}
            </button>
          </div>
        </div>

        {error && <p className="admin-feedback admin-feedback--error" role="alert">{error}</p>}

        {/* 4 KPIs de Alto Nivel: Uptime 24h, 7d/30d, Errores y Servidor */}
        {resumen && (
          <div className="admin-kpi-grid">
            {/* KPI 1: Uptime 24 Horas */}
            <div className="admin-kpi-card admin-kpi-card--primary">
              <div className="admin-kpi-header">
                <span className="admin-kpi-label">Uptime 24 Horas</span>
                <span className={`admin-sla-badge admin-sla-badge--${resumen.slaCalidad.toLowerCase()}`}>
                  {resumen.slaCalidad}
                </span>
              </div>
              <div className="admin-kpi-value">
                {resumen.uptime24h.toFixed(2)}
                <span className="admin-kpi-unit">%</span>
              </div>
              <p className="admin-kpi-sub">
                {resumen.segundosCaida24h === 0
                  ? '✓ 100 % de fluidez en clases'
                  : `Fuera de servicio: ${resumen.tiempoCaidaTexto24h}`}
              </p>
            </div>

            {/* KPI 2: Uptime 7d y 30d */}
            <div className="admin-kpi-card">
              <div className="admin-kpi-header">
                <span className="admin-kpi-label">Disponibilidad Histórica</span>
                <Clock size={18} className="admin-kpi-icon" aria-hidden="true" />
              </div>
              <div className="admin-kpi-split">
                <div>
                  <span className="admin-split-label">7 Días</span>
                  <strong className="admin-split-val">{resumen.uptime7d.toFixed(2)}%</strong>
                </div>
                <div className="admin-split-divider" />
                <div>
                  <span className="admin-split-label">30 Días</span>
                  <strong className="admin-split-val">{resumen.uptime30d.toFixed(2)}%</strong>
                </div>
              </div>
              <p className="admin-kpi-sub">Compromiso SLA escolar: ≥ 99.00 %</p>
            </div>

            {/* KPI 3: Errores Reportados */}
            <div className="admin-kpi-card">
              <div className="admin-kpi-header">
                <span className="admin-kpi-label">Errores Reportados</span>
                <AlertOctagon size={18} className="admin-kpi-icon" aria-hidden="true" />
              </div>
              <div className="admin-kpi-value">
                {resumen.totalErroresReportados}
                <span className="admin-kpi-unit">totales</span>
              </div>
              <p className="admin-kpi-sub">
                {resumen.errores24h} en las últimas 24 horas · {resumen.incidentesAbiertos} incidente(s) abierto(s)
              </p>
            </div>

            {/* KPI 4: Estado del Servidor Principal */}
            <div className="admin-kpi-card">
              <div className="admin-kpi-header">
                <span className="admin-kpi-label">Servidor Principal</span>
                <Server size={18} className="admin-kpi-icon" aria-hidden="true" />
              </div>
              <div className="admin-kpi-server-status">
                <span className={`admin-server-dot admin-server-dot--${resumen.estadoServidor.toLowerCase()}`} />
                <span className="admin-server-text">{resumen.estadoServidor}</span>
              </div>
              <p className="admin-kpi-sub">
                Latencia promedio: <strong>{resumen.latenciaPromedioMs} ms</strong>
              </p>
            </div>
          </div>
        )}
      </section>

      {/* Gráficos en Tiempo Real (Uptime y Conteo de Errores) */}
      {data && (
        <section className="admin-card" aria-labelledby="admin-charts-title">
          <div className="admin-section-head">
            <div>
              <h2 id="admin-charts-title">Métricas de Fluidez y Errores en Tiempo Real</h2>
              <p className="admin-muted">
                Visualización continua de estabilidad y fallos detectados por intervalo de tiempo.
              </p>
            </div>

            {/* Selector de Ventana Temporal (24h vs 7d) */}
            <div className="admin-actions">
              <div className="admin-pill-selector">
                <button
                  type="button"
                  className={`admin-pill-btn ${timeWindow === '24h' ? 'is-active' : ''}`}
                  onClick={() => setTimeWindow('24h')}
                >
                  Últimas 24 horas (Horas)
                </button>
                <button
                  type="button"
                  className={`admin-pill-btn ${timeWindow === '7d' ? 'is-active' : ''}`}
                  onClick={() => setTimeWindow('7d')}
                >
                  Últimos 7 días (Días)
                </button>
              </div>
            </div>
          </div>

          <div className="admin-charts-container">
            {/* Gráfico 1: Curva de Uptime y Disponibilidad */}
            <div className="admin-chart-box">
              <div className="admin-chart-box-head">
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Activity size={18} color="var(--adm-leaf)" aria-hidden="true" />
                  <h3>Porcentaje de Uptime ({timeWindow === '24h' ? '24 horas' : '7 días'})</h3>
                </div>
                <span className="admin-chart-legend">
                  <span className="admin-legend-dot admin-legend-dot--leaf" />
                  Disponibilidad (%)
                </span>
              </div>

              <UptimeSvgChart
                points={serie}
                activeIndex={activePointIndex}
                onHoverPoint={setActivePointIndex}
              />
            </div>

            {/* Gráfico 2: Conteo de Errores por Intervalo */}
            <div className="admin-chart-box">
              <div className="admin-chart-box-head">
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <BarChart3 size={18} color="var(--adm-orange)" aria-hidden="true" />
                  <h3>Frecuencia de Errores ({timeWindow === '24h' ? 'por hora' : 'por día'})</h3>
                </div>
                <span className="admin-chart-legend">
                  <span className="admin-legend-dot admin-legend-dot--orange" />
                  Errores detectados
                </span>
              </div>

              <ErrorsBarChart
                points={serie}
                activeIndex={activePointIndex}
                onHoverPoint={setActivePointIndex}
              />
            </div>
          </div>

          {/* Desglose de Errores por Categoría de Arquitectura */}
          <div className="admin-error-breakdown">
            <h3 style={{ margin: '0 0 12px', fontSize: '1.05rem', color: 'var(--adm-forest)' }}>
              Composición de errores por subsistema técnico
            </h3>
            <div className="admin-breakdown-grid">
              {data.desgloseErrores.map(cat => (
                <div key={cat.categoria} className="admin-breakdown-card">
                  <div className="admin-breakdown-head">
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <CategoryIcon name={cat.categoria} />
                      <strong style={{ fontSize: '0.92rem' }}>{cat.categoria}</strong>
                    </div>
                    <span className="admin-breakdown-count">{cat.cantidad}</span>
                  </div>

                  <div className="admin-breakdown-bar-bg" aria-hidden="true">
                    <div
                      className="admin-breakdown-bar-fill"
                      style={{ width: `${cat.porcentaje}%`, background: cat.color }}
                    />
                  </div>

                  <div className="admin-breakdown-footer">
                    <span className="admin-muted" style={{ fontSize: '0.8rem' }}>
                      {cat.descripcion}
                    </span>
                    <span style={{ fontWeight: 800, fontSize: '0.85rem', color: 'var(--adm-forest)' }}>
                      {cat.porcentaje}%
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* Historial de Caídas del Servidor Principal (Criterio 3) */}
      {data && (
        <section className="admin-card" aria-labelledby="admin-outages-title">
          <div className="admin-section-head">
            <div>
              <h2 id="admin-outages-title">Historial de Caídas del Servidor Principal (HU-48)</h2>
              <p className="admin-muted">
                Registro inmutable de interrupciones, duración exacta y evaluación de impacto en la jornada escolar.
              </p>
            </div>
            <span className="admin-badge admin-badge--success">
              {data.historialCaidas.length} evento(s) registrado(s)
            </span>
          </div>

          {data.historialCaidas.length === 0 ? (
            <div className="admin-alert-empty" role="status">
              <CheckCircle2 size={24} aria-hidden="true" />
              <div>
                <strong>Servidor principal sin caídas registradas</strong>
                <p>No se han presentado interrupciones operativas en la infraestructura de las escuelas.</p>
              </div>
            </div>
          ) : (
            <div className="admin-incident-table-wrap">
              <table className="admin-matrix">
                <thead>
                  <tr>
                    <th scope="col">Servidor</th>
                    <th scope="col">Tipo</th>
                    <th scope="col">Inicio</th>
                    <th scope="col">Restablecido</th>
                    <th scope="col">Duración</th>
                    <th scope="col">Motivo Técnico</th>
                    <th scope="col">Impacto en Colegios</th>
                  </tr>
                </thead>
                <tbody>
                  {data.historialCaidas.map(outage => (
                    <tr key={outage.id}>
                      <td>
                        <span className="admin-service-code">{outage.servidor}</span>
                      </td>
                      <td>
                        <span
                          className={`admin-incident-badge ${
                            outage.tipoEvento === 'CAIDA'
                              ? 'admin-incident-badge--critica'
                              : 'admin-incident-badge--alta'
                          }`}
                        >
                          {outage.tipoEvento}
                        </span>
                      </td>
                      <td>{formatDateTime(outage.iniciadoEn)}</td>
                      <td>{formatDateTime(outage.restablecidoEn)}</td>
                      <td>
                        <strong style={{ color: 'var(--adm-forest)' }}>{outage.duracionTexto}</strong>
                      </td>
                      <td>
                        <div style={{ maxWidth: '320px' }}>
                          <span style={{ fontSize: '0.88rem', fontWeight: 700 }}>{outage.motivo}</span>
                          {outage.codigoError && (
                            <code style={{ display: 'block', fontSize: '0.78rem', color: 'var(--adm-muted)' }}>
                              Ref: {outage.codigoError}
                            </code>
                          )}
                        </div>
                      </td>
                      <td>
                        <span className="admin-muted" style={{ fontSize: '0.85rem' }}>
                          {outage.impactoColegios}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}
    </div>
  )
}

function CategoryIcon({ name }: { name: string }) {
  if (name.includes('API')) return <Server size={16} color="var(--adm-orange)" />
  if (name.includes('Base')) return <Database size={16} color="var(--adm-forest)" />
  if (name.includes('Inteligencia')) return <Sparkles size={16} color="var(--adm-sun-dark)" />
  return <Smartphone size={16} color="var(--adm-sky)" />
}

/**
 * Gráfico SVG interactivo de curva de Uptime (Disponibilidad).
 * Hecho con precisión matemática, responsivo y sin dependencias externas pesadas.
 */
function UptimeSvgChart({
  points,
  activeIndex,
  onHoverPoint,
}: {
  points: TimeSeriesPoint[]
  activeIndex: number | null
  onHoverPoint: (idx: number | null) => void
}) {
  if (!points || points.length === 0) {
    return <div className="admin-chart-empty">Sin datos suficientes para graficar.</div>
  }

  const width = 600
  const height = 220
  const paddingX = 40
  const paddingY = 24
  const chartW = width - paddingX * 2
  const chartH = height - paddingY * 2

  // Escala Y de 90 % a 100 % (o mínimo si hay caída mayor)
  const minUptime = Math.min(90.0, ...points.map(p => p.uptimePercent))
  const maxUptime = 100.0

  const getX = (i: number) => paddingX + (i / Math.max(1, points.length - 1)) * chartW
  const getY = (val: number) => {
    const ratio = (val - minUptime) / Math.max(0.1, maxUptime - minUptime)
    return height - paddingY - ratio * chartH
  }

  const polyPoints = points.map((p, i) => `${getX(i)},${getY(p.uptimePercent)}`).join(' ')
  const areaPoints = `${getX(0)},${height - paddingY} ${polyPoints} ${getX(points.length - 1)},${height - paddingY}`

  const activePoint = activeIndex !== null && points[activeIndex] ? points[activeIndex] : null

  return (
    <div className="admin-svg-wrapper">
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="admin-chart-svg"
        preserveAspectRatio="none"
        onMouseLeave={() => onHoverPoint(null)}
      >
        <defs>
          <linearGradient id="uptimeGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#31724b" stopOpacity="0.25" />
            <stop offset="100%" stopColor="#31724b" stopOpacity="0.02" />
          </linearGradient>
        </defs>

        {/* Líneas guía horizontales (100%, 98%, 95%) */}
        {[100, 98, 95].map(tickVal => {
          if (tickVal < minUptime) return null
          const y = getY(tickVal)
          return (
            <g key={tickVal}>
              <line
                x1={paddingX}
                y1={y}
                x2={width - paddingX}
                y2={y}
                stroke="var(--adm-line)"
                strokeDasharray="4 4"
                strokeWidth="1"
              />
              <text
                x={paddingX - 6}
                y={y + 4}
                textAnchor="end"
                fontSize="10"
                fill="var(--adm-muted)"
                fontWeight="700"
              >
                {tickVal}%
              </text>
            </g>
          )
        })}

        {/* Área rellena bajo la curva */}
        <polygon points={areaPoints} fill="url(#uptimeGrad)" />

        {/* Línea poligonal principal */}
        <polyline
          points={polyPoints}
          fill="none"
          stroke="var(--adm-leaf)"
          strokeWidth="3"
          strokeLinecap="round"
          strokeLinejoin="round"
        />

        {/* Puntos interactivos con hover */}
        {points.map((p, i) => {
          const cx = getX(i)
          const cy = getY(p.uptimePercent)
          const isSelected = activeIndex === i
          return (
            <g key={p.timestamp || i} onMouseEnter={() => onHoverPoint(i)}>
              <circle
                cx={cx}
                cy={cy}
                r={isSelected ? 6 : 4}
                fill={isSelected ? '#fff' : 'var(--adm-leaf)'}
                stroke="var(--adm-leaf)"
                strokeWidth={isSelected ? 3 : 2}
                style={{ cursor: 'pointer', transition: 'all 0.15s ease' }}
              />
              {/* Etiqueta X cada ciertos puntos para no saturar */}
              {i % Math.ceil(points.length / 6) === 0 && (
                <text
                  x={cx}
                  y={height - 6}
                  textAnchor="middle"
                  fontSize="10"
                  fill="var(--adm-muted)"
                  fontWeight="700"
                >
                  {p.etiqueta}
                </text>
              )}
            </g>
          )
        })}
      </svg>

      {/* Tooltip flotante al pasar el mouse */}
      {activePoint && activeIndex !== null && (
        <div
          className="admin-chart-tooltip"
          style={{
            left: `${(getX(activeIndex) / width) * 100}%`,
            top: `${(getY(activePoint.uptimePercent) / height) * 100}%`,
          }}
        >
          <strong>{activePoint.etiqueta}</strong>
          <div>Uptime: {activePoint.uptimePercent}%</div>
          <div>Latencia: {activePoint.latenciaMs} ms</div>
          {activePoint.errores > 0 && <div style={{ color: '#b8381c' }}>Errores: {activePoint.errores}</div>}
        </div>
      )}
    </div>
  )
}

/**
 * Gráfico SVG de barras de conteo de errores por intervalo.
 */
function ErrorsBarChart({
  points,
  activeIndex,
  onHoverPoint,
}: {
  points: TimeSeriesPoint[]
  activeIndex: number | null
  onHoverPoint: (idx: number | null) => void
}) {
  if (!points || points.length === 0) {
    return <div className="admin-chart-empty">Sin datos de errores suficientes.</div>
  }

  const width = 600
  const height = 220
  const paddingX = 40
  const paddingY = 24
  const chartW = width - paddingX * 2
  const chartH = height - paddingY * 2

  const maxErrors = Math.max(3, ...points.map(p => p.errores))
  const barWidth = Math.max(6, Math.min(22, (chartW / points.length) * 0.65))

  return (
    <div className="admin-svg-wrapper">
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="admin-chart-svg"
        preserveAspectRatio="none"
        onMouseLeave={() => onHoverPoint(null)}
      >
        {/* Guías horizontales de errores */}
        {[0, Math.ceil(maxErrors / 2), maxErrors].map(tickVal => {
          const y = height - paddingY - (tickVal / maxErrors) * chartH
          return (
            <g key={tickVal}>
              <line
                x1={paddingX}
                y1={y}
                x2={width - paddingX}
                y2={y}
                stroke="var(--adm-line)"
                strokeDasharray="4 4"
                strokeWidth="1"
              />
              <text
                x={paddingX - 6}
                y={y + 4}
                textAnchor="end"
                fontSize="10"
                fill="var(--adm-muted)"
                fontWeight="700"
              >
                {tickVal}
              </text>
            </g>
          )
        })}

        {/* Barras de errores con volumen físico */}
        {points.map((p, i) => {
          const cx = paddingX + (i / Math.max(1, points.length - 1)) * chartW
          const barHeight = Math.max(p.errores > 0 ? 8 : 2, (p.errores / maxErrors) * chartH)
          const y = height - paddingY - barHeight
          const isSelected = activeIndex === i
          const hasError = p.errores > 0

          return (
            <g key={p.timestamp || i} onMouseEnter={() => onHoverPoint(i)}>
              <rect
                x={cx - barWidth / 2}
                y={y}
                width={barWidth}
                height={barHeight}
                rx={4}
                fill={hasError ? 'var(--adm-orange)' : 'var(--adm-leaf)'}
                opacity={isSelected ? 1 : hasError ? 0.9 : 0.45}
                stroke={hasError ? '#b8381c' : '#235838'}
                strokeWidth={isSelected ? 2 : 1}
                style={{ cursor: 'pointer', transition: 'all 0.15s ease' }}
              />
              {i % Math.ceil(points.length / 6) === 0 && (
                <text
                  x={cx}
                  y={height - 6}
                  textAnchor="middle"
                  fontSize="10"
                  fill="var(--adm-muted)"
                  fontWeight="700"
                >
                  {p.etiqueta}
                </text>
              )}
            </g>
          )
        })}
      </svg>
    </div>
  )
}
