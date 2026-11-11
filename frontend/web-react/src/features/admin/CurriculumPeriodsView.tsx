import { useEffect, useState } from 'react'
import { CalendarDays, CheckCircle2, RefreshCw } from 'lucide-react'
import { AdminApiError, fetchPeriodOptions, fetchPeriods, savePeriods, previewPeriods, applyPeriods, type Administrator, type PeriodCalendar, type PeriodOptions, type PeriodPreview, type SchoolPeriod } from '../../services/adminApi'
import './CurriculumPeriods.css'

const emptyPeriods = (type: PeriodCalendar['tipo']): SchoolPeriod[] => Array.from({ length: type === 'BIMESTRE' ? 4 : 3 }, (_, i) => ({ nombre: `${['I','II','III','IV'][i]} ${type.toLowerCase()}`, comienza: '', termina: '' }))
const months = ['Ene','Feb','Mar','Abr','May','Jun','Jul','Ago','Sep','Oct','Nov','Dic']
const dayNumber = (value: string) => Date.parse(`${value}T00:00:00Z`) / 86400000

export function CurriculumPeriodsView({ admin, onSessionLost }: { admin: Administrator; onSessionLost: (message: string) => void }) {
  const [options, setOptions] = useState<PeriodOptions | null>(null)
  const [yearId, setYearId] = useState('')
  const [calendar, setCalendar] = useState<PeriodCalendar | null>(null)
  const [type, setType] = useState<PeriodCalendar['tipo']>('BIMESTRE')
  const [periods, setPeriods] = useState<SchoolPeriod[]>(emptyPeriods('BIMESTRE'))
  const [grade, setGrade] = useState('')
  const [areas, setAreas] = useState<string[]>([])
  const [preview, setPreview] = useState<PeriodPreview | null>(null)
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [retry, setRetry] = useState(0)
  const [dirty, setDirty] = useState(false)
  const fail = (e: unknown) => { if (e instanceof AdminApiError && e.status === 401) onSessionLost(e.message); else setError(e instanceof Error ? e.message : 'No pudimos completar la operación.') }
  useEffect(() => {
    const c = new AbortController(); setLoading(true); setError('')
    fetchPeriodOptions(c.signal).then(data => { if (!c.signal.aborted) { setOptions(data); setYearId(id => id || data.calendarios.find(y => y.estado === 'ACTIVO')?.id || data.calendarios[0]?.id || ''); setAreas(data.areas.map(a => a.id)) } })
      .catch(e => { if (!c.signal.aborted) fail(e) }).finally(() => { if (!c.signal.aborted) setLoading(false) })
    return () => c.abort()
  }, [retry, onSessionLost])
  useEffect(() => {
    setCalendar(null); setPreview(null); setMessage(''); setError(''); setDirty(false)
    if (!yearId) return
    const c = new AbortController()
    fetchPeriods(yearId, c.signal).then(data => { if (!c.signal.aborted) { setCalendar(data); setType(data.tipo); setPeriods(data.periodos.length ? data.periodos : emptyPeriods(data.tipo)) } }).catch(e => { if (!c.signal.aborted) fail(e) })
    return () => c.abort()
  }, [yearId, retry, onSessionLost])
  const year = options?.calendarios.find(y => y.id === yearId)
  const writable = admin.permisos.includes('curriculo.gestionar') && calendar?.editable
  const validation = periods.some(p => !p.comienza || !p.termina) ? 'Completa las fechas de todos los periodos.'
    : periods.some((p, i) => p.termina < p.comienza || (i > 0 && p.comienza <= periods[i - 1].termina)) ? 'Revisa el orden y los cruces de fechas.'
    : periods.some(p => Number(p.comienza.slice(0, 4)) !== year?.anio || Number(p.termina.slice(0, 4)) !== year?.anio || (year?.comienza && p.comienza < year.comienza) || (year?.termina && p.termina > year.termina)) ? 'Las fechas deben estar dentro del calendario escolar.' : ''
  function changed() { setDirty(true); setPreview(null); setMessage('') }
  async function save() {
    if (!calendar || busy || validation) return
    setBusy(true); setError(''); setMessage('')
    try { const data = await savePeriods(yearId, { tipo: type, periodos: periods, revision: calendar.revision }); setCalendar(data); setPeriods(data.periodos); setDirty(false); setPreview(null); setMessage('Borrador guardado en la base compartida. Revisa la planificación para aplicarlo.') } catch (e) { fail(e) } finally { setBusy(false) }
  }
  async function review() {
    setBusy(true); setError(''); setMessage('')
    try { setPreview(await previewPeriods(yearId, grade, areas)) } catch (e) { fail(e) } finally { setBusy(false) }
  }
  async function apply() {
    if (!preview || busy) return
    setBusy(true); setError(''); setMessage('')
    try { const result = await applyPeriods(yearId, grade, areas, preview.token); setMessage(`Periodos aplicados a ${result.aplicadas} planificaciones. ${result.omitidas} fuera de rango se conservaron sin cambios.`); setPreview(null) } catch (e) { setPreview(null); fail(e) } finally { setBusy(false) }
  }
  const startOfYear = dayNumber(`${year?.anio}-01-01`), daysInYear = dayNumber(`${(year?.anio ?? 0) + 1}-01-01`) - startOfYear
  return <section className="admin-periods" aria-labelledby="periods-title">
    <header className="periods-heading"><div><h1 id="periods-title">Periodos escolares <small>HU-52</small></h1><p>Configura bimestres o trimestres y aplícalos a la planificación.</p></div><button className="admin-btn" disabled={busy || loading} onClick={() => { if (!dirty || window.confirm('Hay cambios sin guardar. ¿Quieres recargar?')) setRetry(n => n + 1) }}><RefreshCw size={15} />Recargar</button></header>
    {error && <p role="alert" className="admin-feedback admin-feedback--error">{error}</p>}
    {message && <p role="status" className="admin-feedback admin-feedback--info">{message}</p>}
    {loading && <p role="status">Cargando calendarios…</p>}
    {!loading && options?.calendarios.length === 0 && <p className="admin-card">Todavía no hay años escolares registrados. Registra el año y sus aulas antes de configurar periodos.</p>}
    {options && options.calendarios.length > 0 && <>
      <fieldset className="periods-filters" disabled={busy || loading}><legend className="goals-sr">Calendario escolar</legend>
        <label>Calendario y año escolar<select aria-label="Calendario y año escolar" value={yearId} onChange={e => { if (!dirty || window.confirm('Hay cambios sin guardar. ¿Quieres cambiar de calendario?')) setYearId(e.target.value) }}>{options.calendarios.map(y => <option key={y.id} value={y.id}>{y.anio} · {y.institucion} · {y.estado}</option>)}</select></label>
        <div><span>Organización</span><div className="periods-toggle">{(['BIMESTRE','TRIMESTRE'] as const).map(t => <button type="button" key={t} disabled={!writable} aria-pressed={type === t} onClick={() => { if (t !== type && (!periods.some(p => p.comienza) || window.confirm('Cambiar la organización reinicia las fechas del borrador. ¿Continuar?'))) { setType(t); setPeriods(emptyPeriods(t)); changed() } }}>{t === 'BIMESTRE' ? 'Bimestres' : 'Trimestres'}</button>)}</div></div>
      </fieldset>
      {!calendar && !error && <p role="status">Cargando periodos…</p>}
      {calendar && <div className="periods-layout"><section className="admin-card periods-editor"><h2><CalendarDays size={20} />Configurar periodos</h2>
        {!writable && <p>Solo consulta{!calendar.editable ? ': el año escolar está cerrado.' : '.'}</p>}
        <div className="periods-rows"><div className="periods-row periods-row-heading"><span>Periodo</span><span>Inicio</span><span>Fin</span></div>{periods.map((p, i) => <div className="periods-row" key={i}><strong>{p.nombre}</strong>{(['comienza','termina'] as const).map(field => <label key={field}><span className="periods-mobile-label">{field === 'comienza' ? 'Inicio' : 'Fin'}</span><input aria-label={`${field === 'comienza' ? 'Inicio' : 'Fin'} ${p.nombre}`} type="date" min={year?.comienza || `${year?.anio}-01-01`} max={year?.termina || `${year?.anio}-12-31`} disabled={!writable || busy} value={p[field]} onChange={e => { setPeriods(periods.map((item, n) => n === i ? { ...item, [field]: e.target.value } : item)); changed() }} /></label>)}</div>)}</div>
        <p className={validation ? 'periods-hint' : 'periods-valid'}>{!validation && <CheckCircle2 size={18} />}{validation || 'Sin cruces de fechas entre periodos.'}</p>
        <p className="admin-muted">Usa las fechas del calendario escolar aprobado. Los espacios entre periodos quedan sin asignar.</p>
        {!validation && <div className="periods-timeline" aria-label="Distribución de periodos durante el año"><div className="periods-months">{months.map(m => <span key={m}>{m}</span>)}</div><div className="periods-bands">{periods.map((p, i) => <span className={`period-band period-band-${i}`} key={i} style={{ left: `${100 * (dayNumber(p.comienza) - startOfYear) / daysInYear}%`, width: `${100 * (dayNumber(p.termina) - dayNumber(p.comienza) + 1) / daysInYear}%` }} title={`${p.nombre}: ${p.comienza} a ${p.termina}`}>{p.nombre}</span>)}</div></div>}
        <p className="admin-muted">{calendar.revision ? `Borrador · revisión ${calendar.revision}` : 'Sin guardar'}{dirty ? ' · Cambios pendientes' : ''}</p>
        {writable && <button className="admin-btn" disabled={busy || !!validation || (!dirty && !!calendar.id)} onClick={save}>{busy ? 'Procesando…' : 'Guardar borrador'}</button>}
      </section><aside className="admin-card periods-apply"><h2>Aplicar a la planificación</h2><p>Relaciona unidades y evaluaciones con el periodo que contiene sus fechas.</p>
        <fieldset disabled={busy}><legend className="goals-sr">Alcance de la aplicación</legend><label>Grado<select aria-label="Grado para aplicar" value={grade} onChange={e => { setGrade(e.target.value); setPreview(null) }}><option value="">Todos los grados</option>{options.grados.map(g => <option key={g.id} value={g.id}>{g.nombre}</option>)}</select></label>
        <div className="periods-areas">{Array.from(new Set(options.areas.map(a => a.nombre))).map(name => { const ids = options.areas.filter(a => a.nombre === name).map(a => a.id); return <label key={name}><input type="checkbox" checked={ids.every(id => areas.includes(id))} onChange={e => { setAreas(old => e.target.checked ? [...new Set([...old, ...ids])] : old.filter(id => !ids.includes(id))); setPreview(null) }} />{name}</label> })}</div></fieldset>
        <p className="periods-warning">Revisa las planificaciones antes de aplicar. Sus fechas y los recordatorios existentes se conservan.</p>
        {dirty && <p>Guarda el borrador para revisar su aplicación.</p>}
        <button className="admin-btn" disabled={busy || dirty || !calendar.id || !areas.length} onClick={review}>Ver planificación afectada</button>
        {preview && <div className="periods-preview" aria-live="polite"><strong>{preview.aplicables} de {preview.planificaciones.length} planificaciones dentro de los periodos</strong>{preview.planificaciones.length === 0 && <p>No hay unidades o evaluaciones programadas para estos filtros.</p>}<ul>{preview.planificaciones.map(p => <li key={p.id}><strong>{p.titulo}</strong><span>{p.aula} · {p.ruta}</span><b>{p.periodo?.nombre ?? 'Fuera de rango: se conserva sin cambios'}</b></li>)}</ul></div>}
        {writable && <button className="admin-btn admin-btn--primary" disabled={busy || dirty || !preview?.aplicables} onClick={apply}>Aplicar periodos</button>}
        <p className="admin-muted">Los periodos aplicados se muestran en el calendario del aula. Las nuevas planificaciones requieren revisar y aplicar otra vez.</p>
      </aside></div>}
    </>}
  </section>
}
