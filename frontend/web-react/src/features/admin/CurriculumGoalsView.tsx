import { useEffect, useRef, useState, type FormEvent } from 'react'
import { LockKeyhole, Pencil, Plus, RefreshCw, History } from 'lucide-react'
import { AdminApiError, fetchCurriculumCatalog, fetchCurriculumUnits, fetchUnitGoals, createUnitGoal, updateUnitGoal, type Administrator, type CurriculumCatalog, type CurriculumUnit, type CurriculumGoal, type UnitGoals, type GoalInput } from '../../services/adminApi'
import './CurriculumGoals.css'

type Props = { admin: Administrator; onSessionLost: (message: string) => void }
const label = (tipo: string) => tipo === 'META' ? 'Meta' : 'Estándar'

export function CurriculumGoalsView({ admin, onSessionLost }: Props) {
  const [catalog, setCatalog] = useState<CurriculumCatalog | null>(null)
  const [version, setVersion] = useState<string | null>(null)
  const [units, setUnits] = useState<CurriculumUnit[]>([])
  const [grade, setGrade] = useState('')
  const [area, setArea] = useState('')
  const [unitId, setUnitId] = useState('')
  const [data, setData] = useState<UnitGoals | null>(null)
  const [editing, setEditing] = useState<CurriculumGoal | 'new' | null>(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [retry, setRetry] = useState(0)
  const editorRef = useRef<HTMLElement>(null)
  useEffect(() => {
    if (!editing) return
    if (window.matchMedia('(max-width: 1000px)').matches) editorRef.current?.scrollIntoView({ block: 'start' })
    editorRef.current?.querySelector<HTMLTextAreaElement>('textarea:not(:disabled)')?.focus({ preventScroll: true })
  }, [editing])
  useEffect(() => {
    const abort = new AbortController(); setLoading(true); setError(''); setUnits([]); setUnitId(''); setEditing(null); setData(null)
    fetchCurriculumCatalog(version, abort.signal).then(async found => {
      if (abort.signal.aborted) return
      setCatalog(found)
      const available = found.version ? await fetchCurriculumUnits(found.version.id, abort.signal) : []
      if (!abort.signal.aborted) setUnits(available)
    }).catch(e => { if (!abort.signal.aborted) { if (e instanceof AdminApiError && e.status === 401) onSessionLost(e.message); else setError(e.message) } })
      .finally(() => { if (!abort.signal.aborted) setLoading(false) })
    return () => abort.abort()
  }, [version, retry, onSessionLost])
  useEffect(() => {
    setData(null); setEditing(null); setMessage('')
    if (!unitId) return
    const abort = new AbortController(); setError('')
    fetchUnitGoals(unitId, abort.signal).then(found => { if (!abort.signal.aborted) setData(found) })
      .catch(e => { if (!abort.signal.aborted) { if (e instanceof AdminApiError && e.status === 401) onSessionLost(e.message); else setError(e.message) } })
    return () => abort.abort()
  }, [unitId, onSessionLost])
  const unit = units.find(u => u.id === unitId)
  const available = units.filter(u => (!grade || u.gradoId === grade) && (!area || u.areaId === area))
  const writable = admin.permisos.includes('curriculo.gestionar') && Boolean(data?.editable)
  async function save(input: GoalInput, reason: string) {
    if (!editing || busy) return
    setBusy(true); setError(''); setMessage('')
    try {
      const updated = editing === 'new' ? await createUnitGoal(unitId, input) : await updateUnitGoal(unitId, editing, input.descripcion, reason)
      setData(updated); setEditing(null); setMessage('Guardado. La unidad y la competencia se conservaron.')
    } catch (e) {
      if (e instanceof AdminApiError && e.status === 401) onSessionLost(e.message)
      else setError(e instanceof Error ? e.message : 'No pudimos guardar. Vuelve a intentarlo.')
    } finally { setBusy(false) }
  }
  return <section className="admin-goals" aria-labelledby="goals-title">
    <header className="goals-heading"><div><p className="admin-muted">Catálogo curricular / Unidades</p><h1 id="goals-title">Metas y estándares</h1><p>Registra los aprendizajes esperados y corrígelos conservando su relación curricular.</p></div><button className="admin-btn" disabled={busy || loading} onClick={() => setRetry(n => n + 1)}><RefreshCw size={15} />Recargar</button></header>
    {error && <p role="alert" className="admin-feedback admin-feedback--error">{error}</p>}
    {message && <p role="status" className="admin-feedback admin-feedback--info">{message}</p>}
    <fieldset className="goals-filters" disabled={busy || loading}>
      <legend className="goals-sr">Filtrar unidades curriculares</legend>
      <label>Catálogo<select aria-label="Catálogo" value={catalog?.version?.id ?? ''} onChange={e => { setGrade(''); setArea(''); setVersion(e.target.value) }}><option value="" disabled>Selecciona catálogo</option>{catalog?.versiones.map(v => <option key={v.id} value={v.id}>{v.etiqueta} · {v.estado}</option>)}</select></label>
      <label>Grado<select aria-label="Grado" value={grade} onChange={e => { setGrade(e.target.value); setUnitId('') }}><option value="">Todos los grados</option>{catalog?.grados.map(g => <option key={g.id} value={g.id}>{g.nombre}</option>)}</select></label>
      <label>Área<select aria-label="Área" value={area} onChange={e => { setArea(e.target.value); setUnitId('') }}><option value="">Todas las áreas</option>{catalog?.areas.map(a => <option key={a.id} value={a.id}>{a.nombre}</option>)}</select></label>
      <label>Unidad<select aria-label="Unidad" value={unitId} onChange={e => setUnitId(e.target.value)}><option value="">Selecciona una unidad</option>{available.map(u => <option key={u.id} value={u.id}>{u.grado} · {u.titulo} ({u.codigo})</option>)}</select></label>
    </fieldset>
    {loading && <p role="status">Cargando unidades…</p>}
    {!loading && !unit && <p className="admin-card">{available.length ? 'Selecciona una unidad para consultar sus metas y estándares.' : 'No hay unidades para estos filtros.'}</p>}
    {unit && !data && !error && <p role="status">Cargando registros de la unidad…</p>}
    {unit && data && <div className="goals-layout"><div>
      <section className="goals-context"><h2>Relación curricular</h2><p><strong>{unit.titulo}</strong></p><p>{unit.grado} · {unit.area}</p><ul>{data.competencias.map(c => <li key={c.id}>{c.nombre}</li>)}</ul>{!data.competencias.length && <p>Esta unidad aún no tiene competencias vinculadas. Deben vincularse antes de registrar metas o estándares.</p>}{!data.editable && <p>Catálogo retirado: solo consulta.</p>}</section>
      <section className="admin-card goals-records"><div className="goals-heading"><h2>Registros de la unidad <small>({data.registros.length})</small></h2>{writable && <button className="admin-btn admin-btn--primary" disabled={busy || !data.competencias.length} onClick={() => setEditing('new')}><Plus size={16} />Nuevo registro</button>}</div>
        {!data.registros.length && <p className="admin-muted">Todavía no se registraron metas ni estándares en esta unidad.</p>}
        <ul className="goals-list">{data.registros.map(r => <li key={r.tipo + r.id}><div><span className={`goals-tag ${r.tipo === 'ESTANDAR' ? 'is-standard' : ''}`}>{label(r.tipo)}</span><small>{r.codigo}</small><p className="goals-description-preview">{r.descripcion}</p><small>{r.competencia}</small>{r.unidadesVinculadas > 1 && <p className="admin-muted">Compartido con {r.unidadesVinculadas} unidades. La corrección se verá en todas ellas.</p>}</div><button className="admin-btn" disabled={busy} aria-label={`${writable ? 'Editar' : 'Ver'} ${r.codigo}`} onClick={() => setEditing(r)}><Pencil size={14} />{writable ? 'Editar' : 'Ver'}</button></li>)}</ul>
      </section>
    </div><aside ref={editorRef} className="admin-card goals-editor">{editing ? <GoalForm key={editing === 'new' ? `${unitId}-new` : editing.tipo + editing.id} record={editing} unit={unit} data={data} writable={writable} busy={busy} save={save} cancel={() => setEditing(null)} /> : <div className="goals-editor-empty"><LockKeyhole size={26} /><h2>Vínculos protegidos</h2><p>Elige un registro para editar su descripción. Su código, grado, unidad y competencia se conservan.</p><p className="admin-muted">Las correcciones quedan en el historial.</p></div>}</aside></div>}
  </section>
}

function GoalForm({ record, unit, data, writable, busy, save, cancel }: { record: CurriculumGoal | 'new'; unit: CurriculumUnit; data: UnitGoals; writable: boolean; busy: boolean; save: (input: GoalInput, reason: string) => Promise<void>; cancel: () => void }) {
  const existing = record === 'new' ? null : record
  const [input, setInput] = useState<GoalInput>({ tipo: existing?.tipo ?? 'META', codigo: existing?.codigo ?? '', descripcion: existing?.descripcion ?? '', competenciaId: existing?.competenciaId ?? data.competencias[0]?.id ?? '' })
  const [reason, setReason] = useState('')
  const submit = (event: FormEvent) => { event.preventDefault(); if (writable && !busy) void save(input, reason) }
  return <><h2>{existing ? `${writable ? 'Editar' : 'Consultar'} ${label(existing.tipo).toLowerCase()}` : 'Nuevo registro'}</h2><div className="goals-link"><LockKeyhole size={17} /><span>{unit.grado} · {unit.area}<br /><strong>{unit.titulo}</strong></span></div>
    <form onSubmit={submit}><fieldset disabled={busy || !writable} className="goals-form"><label>Tipo<select aria-label="Tipo" disabled={!!existing} value={input.tipo} onChange={e => setInput({ ...input, tipo: e.target.value as GoalInput['tipo'] })}><option value="META">Meta</option><option value="ESTANDAR">Estándar</option></select></label><label>Código<input required disabled={!!existing} maxLength={60} pattern="[A-Za-z0-9][A-Za-z0-9_-]{1,59}" value={input.codigo} onChange={e => setInput({ ...input, codigo: e.target.value.toUpperCase() })} placeholder="Ej. MAT-U1-M01" /></label><label className="goals-full">Competencia<select aria-label="Competencia" required disabled={!!existing} value={input.competenciaId} onChange={e => setInput({ ...input, competenciaId: e.target.value })}>{data.competencias.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}{existing && !data.competencias.some(c => c.id === existing.competenciaId) && <option value={existing.competenciaId}>{existing.competencia}</option>}</select></label><label className="goals-full">Descripción<textarea aria-label="Descripción" required minLength={10} maxLength={4000} rows={5} value={input.descripcion} onChange={e => setInput({ ...input, descripcion: e.target.value })} /></label>{existing && <label className="goals-full">Motivo de la corrección<textarea aria-label="Motivo de la corrección" required minLength={5} maxLength={500} rows={2} value={reason} onChange={e => setReason(e.target.value)} /></label>}</fieldset>
      {existing && existing.unidadesVinculadas > 1 && <p className="admin-muted">Este registro se usa en {existing.unidadesVinculadas} unidades; guardar corrige su descripción en todas ellas.</p>}
      <div className="goals-actions"><button type="button" className="admin-btn" disabled={busy} onClick={cancel}>Cancelar</button>{writable && <button type="submit" className="admin-btn admin-btn--primary" disabled={busy || (existing !== null && existing.descripcion === input.descripcion.trim())}>{busy ? 'Guardando…' : 'Guardar cambios'}</button>}</div>
    </form>{existing && <details className="goals-history"><summary><History size={15} />Historial de cambios ({existing.historial.length})</summary>{!existing.historial.length && <p>Sin correcciones registradas.</p>}{existing.historial.map((h, i) => <article key={i}><strong>{h.autor}</strong><time>{new Date(h.fecha).toLocaleString('es-PE')}</time><p>{h.motivo}</p>{h.anterior !== null && <p><b>Antes:</b> {h.anterior}</p>}<p><b>Después:</b> {h.nueva}</p></article>)}</details>}</>
}

