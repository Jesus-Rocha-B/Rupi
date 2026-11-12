import { useEffect, useState } from 'react'
import { ArrowRight, ExternalLink, FileText, Plus, RefreshCw, Route } from 'lucide-react'
import {
  AdminApiError, fetchResolution, fetchResolutions, publishResolution, registerResolution, updateCorrespondence,
  type Administrator, type CorrespondenceChange, type CorrespondenceInput, type CorrespondenceItem,
  type CorrespondenceType, type ResolutionDetail, type ResolutionInput, type ResolutionList,
} from '../../services/adminApi'
import './CurriculumResolutions.css'

const CHANGE_TEXT: Record<CorrespondenceChange, string> = {
  SE_MANTIENE: 'Se mantiene', MODIFICADA: 'Modificada', RETIRADA: 'Retirada', NUEVA: 'Nueva',
}
const CHANGE_TONE: Record<CorrespondenceChange, string> = {
  SE_MANTIENE: 'ok', MODIFICADA: 'warn', RETIRADA: 'off', NUEVA: 'neutral',
}
const PLURAL: Record<CorrespondenceChange, string> = {
  SE_MANTIENE: 'se mantienen', MODIFICADA: 'modificadas', RETIRADA: 'retiradas', NUEVA: 'nuevas',
}
const TYPE_TEXT: Record<CorrespondenceType, string> = { AREA: 'Áreas', COMPETENCIA: 'Competencias', UNIDAD: 'Unidades' }
const limaDate = (value: string) => new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeZone: 'UTC' }).format(new Date(`${value}T00:00:00Z`))
const emptyForm: ResolutionInput = { numero: '', fechaEmision: '', resumen: '', enlace: '', etiqueta: '', vigenteDesde: '' }

interface Props { admin: Administrator; onSessionLost: (message: string) => void }

/** HU-51: registrar una resolución ministerial y revisar la correspondencia entre la versión vigente y la nueva. */
export function CurriculumResolutionsView({ admin, onSessionLost }: Props) {
  const [list, setList] = useState<ResolutionList | null>(null)
  const [selected, setSelected] = useState<string | null>(null)
  const [detail, setDetail] = useState<ResolutionDetail | null>(null)
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState<ResolutionInput>(emptyForm)
  const [type, setType] = useState<CorrespondenceType>('UNIDAD')
  const [editing, setEditing] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const [confirm, setConfirm] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [reload, setReload] = useState(0)
  const canManage = admin.permisos.includes('curriculo.gestionar')
  const fail = (e: unknown) => {
    if (e instanceof AdminApiError && e.status === 401) onSessionLost(e.message)
    else setError(e instanceof Error ? e.message : 'No pudimos completar la operación.')
  }

  useEffect(() => {
    const c = new AbortController()
    fetchResolutions(c.signal).then(data => {
      setList(data)
      setSelected(current => current ?? data.resoluciones[0]?.id ?? null)
    }).catch(e => { if (!c.signal.aborted) fail(e) })
    return () => c.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reload])

  useEffect(() => {
    if (!selected) return
    const c = new AbortController()
    fetchResolution(selected, c.signal).then(setDetail).catch(e => { if (!c.signal.aborted) fail(e) })
    return () => c.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selected, reload])

  async function register() {
    setBusy(true); setError(''); setMessage('')
    try {
      const data = await registerResolution({ ...form, enlace: form.enlace?.trim() || undefined })
      setDetail(data); setSelected(data.resolucion.id); setCreating(false); setForm(emptyForm)
      setMessage(`Se creó el borrador ${data.resolucion.destino.etiqueta} como copia de ${data.resolucion.origen.etiqueta}. Revisa qué cambia.`)
      setReload(n => n + 1)
    } catch (e) { fail(e) } finally { setBusy(false) }
  }

  async function save(item: CorrespondenceItem, data: CorrespondenceInput) {
    if (!detail || !item.id) return
    setBusy(true); setError(''); setMessage('')
    try { setDetail(await updateCorrespondence(detail.resolucion.id, item.id, data)); setEditing(null) }
    catch (e) { fail(e) } finally { setBusy(false) }
  }

  async function publish() {
    if (!detail) return
    setBusy(true); setError(''); setMessage('')
    try {
      const data = await publishResolution(detail.resolucion.id, confirm.trim())
      setDetail(data); setConfirm('')
      setMessage(`${data.resolucion.destino.etiqueta} ya es la versión vigente. Los docentes verán el aviso para actualizar sus rutas.`)
      setReload(n => n + 1)
    } catch (e) { fail(e) } finally { setBusy(false) }
  }

  const r = detail?.resolucion
  const needle = query.trim().toLocaleLowerCase('es')
  const items = (detail?.elementos[type] ?? []).filter(item => !needle
    || [item.codigo, item.origen, item.destino, item.grado].some(text => text?.toLocaleLowerCase('es').includes(needle)))
  return <section className="admin-resolutions" aria-labelledby="resolutions-title">
    <header className="res-heading">
      <div><h1 id="resolutions-title">Resoluciones ministeriales <small>HU-51</small></h1>
        <p>Cuando el MINEDU actualiza el currículo, registra la norma y revisa qué se mantiene, cambia o se retira.</p></div>
      <div className="res-heading-actions">
        <button className="admin-btn admin-btn--quiet" disabled={busy} onClick={() => setReload(n => n + 1)}><RefreshCw size={16} aria-hidden="true" />Recargar</button>
        {canManage && list?.puedeRegistrar && !creating && <button className="admin-btn admin-btn--primary" onClick={() => { setCreating(true); setMessage('') }}><Plus size={18} aria-hidden="true" />Registrar resolución</button>}
      </div>
    </header>
    {error && <p role="alert" className="admin-feedback admin-feedback--error">{error}</p>}
    {message && <p role="status" className="admin-feedback admin-feedback--ok">{message}</p>}
    {!list && !error && <p className="admin-loading">Buscando resoluciones…</p>}

    {creating && list?.vigente && <form className="admin-card res-form" onSubmit={e => { e.preventDefault(); void register() }}>
      <h2><FileText size={20} aria-hidden="true" />Nueva resolución</h2>
      <p className="admin-muted">Se copiará {list.vigente.etiqueta} a un borrador. Nada cambia para docentes ni estudiantes hasta que publiques.</p>
      <div className="res-form-grid">
        <label className="admin-field">Número de la norma<input required value={form.numero} placeholder="RM 587-2026-MINEDU" onChange={e => setForm({ ...form, numero: e.target.value })} /></label>
        <label className="admin-field">Fecha de emisión<input required type="date" value={form.fechaEmision} onChange={e => setForm({ ...form, fechaEmision: e.target.value })} /></label>
        <label className="admin-field res-full">Qué cambia<textarea required minLength={10} maxLength={600} rows={3} value={form.resumen} placeholder="Resume los cambios que dispone la norma." onChange={e => setForm({ ...form, resumen: e.target.value })} /></label>
        <label className="admin-field res-full">Enlace oficial <em>(opcional)</em><input type="url" value={form.enlace} placeholder="https://www.gob.pe/…" onChange={e => setForm({ ...form, enlace: e.target.value })} /></label>
        <label className="admin-field">Nombre de la nueva versión<input required value={form.etiqueta} placeholder="CNEB-2027" onChange={e => setForm({ ...form, etiqueta: e.target.value })} /></label>
        <label className="admin-field">Vigente desde<input required type="date" value={form.vigenteDesde} onChange={e => setForm({ ...form, vigenteDesde: e.target.value })} /></label>
      </div>
      <div className="res-actions"><button type="button" className="admin-btn admin-btn--quiet" disabled={busy} onClick={() => setCreating(false)}>Cancelar</button>
        <button className="admin-btn admin-btn--leaf" disabled={busy}>{busy ? 'Copiando el catálogo…' : 'Crear borrador'}</button></div>
    </form>}

    {list && list.resoluciones.length === 0 && !creating && <div className="admin-card res-empty">
      <p><strong>Todavía no hay resoluciones registradas.</strong></p>
      <p className="admin-muted">La versión vigente es {list.vigente?.etiqueta ?? 'ninguna'}. Registra la norma cuando el MINEDU publique cambios en el currículo.</p>
    </div>}

    {list && list.resoluciones.length > 0 && <div className="res-layout">
      <nav className="admin-card res-list" aria-label="Resoluciones registradas">
        <h2>Registradas</h2>
        <ul>{list.resoluciones.map(item => <li key={item.id}>
          <button type="button" className={item.id === selected ? 'is-active' : ''} aria-current={item.id === selected ? 'true' : undefined} onClick={() => { setSelected(item.id); setEditing(null); setMessage('') }}>
            <strong>{item.numero}</strong>
            <span>{item.origen.etiqueta} <ArrowRight size={13} aria-hidden="true" /> {item.destino.etiqueta}</span>
            <span className={`admin-chip admin-chip--${item.estado === 'PUBLICADA' ? 'ok' : 'warn'}`}>{item.estado === 'PUBLICADA' ? 'Publicada' : 'Borrador'}</span>
          </button></li>)}</ul>
      </nav>

      {!detail && <p className="admin-loading">Abriendo la resolución…</p>}
      {detail && r && <div className="res-detail">
        <article className="admin-card res-summary">
          <div className="res-summary-head">
            <div><h2>{r.numero}</h2><p className="admin-muted">Emitida el {limaDate(r.fechaEmision)} · {r.origen.etiqueta} → {r.destino.etiqueta}{r.destino.vigenteDesde ? `, vigente desde ${limaDate(r.destino.vigenteDesde)}` : ''}</p></div>
            <span className={`admin-chip admin-chip--${r.estado === 'PUBLICADA' ? 'ok' : 'warn'}`}>{r.estado === 'PUBLICADA' ? 'Publicada' : 'Borrador'}</span>
          </div>
          <p>{r.resumen}</p>
          {r.enlace && <a className="res-link" href={r.enlace} target="_blank" rel="noreferrer noopener"><ExternalLink size={15} aria-hidden="true" />Ver la norma oficial</a>}
          <dl className="res-counts">{(['AREA', 'COMPETENCIA', 'UNIDAD'] as CorrespondenceType[]).map(t => <div key={t}>
            <dt>{TYPE_TEXT[t]}</dt>
            <dd>{(['SE_MANTIENE', 'MODIFICADA', 'RETIRADA', 'NUEVA'] as CorrespondenceChange[]).filter(c => detail.resumen[t][c] > 0).map(c => <span key={c}>{detail.resumen[t][c]} {detail.resumen[t][c] === 1 ? CHANGE_TEXT[c].toLowerCase() : PLURAL[c]}</span>)}</dd>
          </div>)}</dl>
          <p className="res-impact"><Route size={18} aria-hidden="true" /><span>
            <strong>{detail.impacto.rutas} {detail.impacto.rutas === 1 ? 'ruta sigue' : 'rutas siguen'} en {r.origen.etiqueta}</strong> ({detail.impacto.rutasPublicadas} publicadas).
            {detail.impacto.paradasSinUnidad > 0 ? ` ${detail.impacto.paradasSinUnidad} paradas quedarán sin unidad por lo retirado.` : ''}
            {r.estado === 'PUBLICADA' ? ' Cada docente actualiza su ruta desde el editor.' : ' Al publicar, cada docente verá un aviso para actualizar su ruta.'}
          </span></p>
        </article>

        <section className="admin-card res-items" aria-labelledby="res-items-title">
          <h2 id="res-items-title">Correspondencia</h2>
          <div className="res-tabs" role="group" aria-label="Tipo de elemento">{(['UNIDAD', 'COMPETENCIA', 'AREA'] as CorrespondenceType[]).map(t =>
            <button key={t} type="button" aria-pressed={type === t} onClick={() => { setType(t); setEditing(null) }}>{TYPE_TEXT[t]} <span>{detail.elementos[t].length}</span></button>)}</div>
          <label className="admin-field res-search"><span>Buscar por código, título o grado</span><input type="search" value={query} placeholder="Ej. MAT2 o medidas" onChange={e => setQuery(e.target.value)} /></label>
          {type !== 'UNIDAD' && detail.editable && <p className="admin-muted">Para agregar o renombrar {type === 'AREA' ? 'áreas' : 'competencias'}, elige {r.destino.etiqueta} en «Grados y competencias». Aquí se marcan como nuevas o modificadas.</p>}
          <ul className="res-rows">{items.map(item => <li key={item.id ?? `nuevo-${item.codigo}-${item.destino}`}>
            <div className="res-row-main">
              <span className="res-code">{item.codigo}{item.grado ? ` · ${item.grado}` : ''}</span>
              {item.cambio === 'NUEVA' ? <p>{item.destino}</p>
                : item.cambio === 'MODIFICADA' && item.destino && item.destino !== item.origen ? <p><del>{item.origen}</del> <ArrowRight size={14} aria-label="ahora" /> {item.destino}</p>
                : <p>{item.origen}</p>}
              {item.nota && <small>Sustento: {item.nota}</small>}
            </div>
            <span className={`admin-chip admin-chip--${CHANGE_TONE[item.cambio]}`}>{CHANGE_TEXT[item.cambio]}</span>
            {canManage && detail.editable && item.editable && editing !== item.id && <button type="button" className="admin-btn admin-btn--quiet res-edit" disabled={busy} onClick={() => setEditing(item.id)}>Revisar</button>}
            {editing === item.id && <CorrespondenceEditor item={item} type={type} busy={busy} onCancel={() => setEditing(null)} onSave={data => void save(item, data)} />}
          </li>)}</ul>
          {items.length === 0 && <p className="admin-empty-line">{needle ? 'Ningún elemento coincide con la búsqueda.' : `No hay ${TYPE_TEXT[type].toLowerCase()} en esta versión.`}</p>}
        </section>

        {canManage && detail.editable && <section className="admin-card res-publish" aria-labelledby="res-publish-title">
          <h2 id="res-publish-title">Publicar la nueva versión</h2>
          <p>{r.destino.etiqueta} pasará a ser la versión vigente y {r.origen.etiqueta} quedará retirada como registro. Lo marcado como retirado se quitará del borrador. No se puede deshacer.</p>
          <label className="admin-field"><span>Escribe <strong>{r.numero}</strong> para confirmar</span><input value={confirm} autoComplete="off" onChange={e => setConfirm(e.target.value)} /></label>
          <button type="button" className="admin-btn admin-btn--primary" disabled={busy || confirm.trim() !== r.numero} onClick={() => void publish()}>{busy ? 'Publicando…' : `Publicar ${r.destino.etiqueta}`}</button>
        </section>}
      </div>}
    </div>}
  </section>
}

function CorrespondenceEditor({ item, type, busy, onCancel, onSave }: {
  item: CorrespondenceItem; type: CorrespondenceType; busy: boolean; onCancel: () => void; onSave: (data: CorrespondenceInput) => void
}) {
  const initial: CorrespondenceInput['accion'] = item.cambio === 'RETIRADA' ? 'RETIRAR' : item.cambio === 'MODIFICADA' && type === 'UNIDAD' ? 'CORREGIR' : 'MANTENER'
  const [action, setAction] = useState<CorrespondenceInput['accion']>(initial)
  const [title, setTitle] = useState(item.destino ?? item.origen ?? '')
  const [note, setNote] = useState(item.nota ?? '')
  const choices: [CorrespondenceInput['accion'], string][] = [['MANTENER', 'Se mantiene'], ...(type === 'UNIDAD' ? [['CORREGIR', 'Cambia el título'] as [CorrespondenceInput['accion'], string]] : []), ['RETIRAR', 'Se retira']]
  return <form className="res-editor" onSubmit={e => { e.preventDefault(); onSave({ accion: action, ...(action === 'CORREGIR' ? { titulo: title } : {}), ...(action !== 'MANTENER' || note.trim() ? { nota: note } : {}) }) }}>
    <fieldset><legend>Según la resolución, este elemento…</legend>
      <div className="res-choices">{choices.map(([value, text]) => <label key={value}><input type="radio" name={`accion-${item.id}`} checked={action === value} onChange={() => setAction(value)} />{text}</label>)}</div>
    </fieldset>
    {action === 'CORREGIR' && <label className="admin-field">Nuevo título<input required minLength={3} maxLength={180} value={title} onChange={e => setTitle(e.target.value)} /></label>}
    {action === 'RETIRAR' && <p className="res-warning">Las paradas de esta {type === 'UNIDAD' ? 'unidad' : 'competencia'} quedarán en «Otras paradas» cuando el docente actualice su ruta.</p>}
    <label className="admin-field">Sustento {action === 'MANTENER' ? <em>(opcional)</em> : null}<textarea rows={2} maxLength={500} required={action !== 'MANTENER'} minLength={action !== 'MANTENER' ? 5 : undefined} value={note} placeholder="Ej. Anexo 2, numeral 4.3" onChange={e => setNote(e.target.value)} /></label>
    <div className="res-actions"><button type="button" className="admin-btn admin-btn--quiet" disabled={busy} onClick={onCancel}>Cancelar</button><button className="admin-btn admin-btn--leaf" disabled={busy}>Guardar</button></div>
  </form>
}
