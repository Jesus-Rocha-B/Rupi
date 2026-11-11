import { useEffect, useRef, useState } from 'react'
import { Activity, ArrowLeft, ArrowRight, CheckCheck, Clock3, LockKeyhole, Search, ShieldCheck, Sparkles, X } from 'lucide-react'
import { AdminApiError, fetchInteractions, fetchInteraction, reviewInteraction, formatDateTime, type Administrator, type InteractionPage, type InteractionDetail } from '../../services/adminApi'
import './InteractionHistory.css'

const resources: Record<string,string> = { TUTOR:'Tutor Rupi', RECURSO_JSON:'Recurso educativo / revisión', CUESTIONARIO:'Cuestionario', REVISION_AUTOMATICA:'Revisión automática', GENERACION_IA:'Generación de IA' }
const traces: Record<string,string> = { IA_INTERACCION_CONSULTADA:'Consulta autorizada del detalle', IA_INTERACCION_REVISADA:'Revisión registrada' }
const duration = (n: number | null) => n === null ? 'No disponible' : `${(n/1000).toFixed(1)} s`

export function InteractionHistoryView({ admin, onSessionLost }: { admin: Administrator; onSessionLost: (message:string)=>void }) {
  const [filters,setFilters] = useState({q:'',desde:'',hasta:'',resultado:'',revision:''})
  const [query,setQuery] = useState(new URLSearchParams())
  const [page,setPage] = useState<InteractionPage | null>(null)
  const [error,setError] = useState('')
  const [refresh,setRefresh] = useState(0)
  const [selected,setSelected] = useState<string | null>(null)
  const [detail,setDetail] = useState<InteractionDetail | null>(null)
  const [detailError,setDetailError] = useState('')
  const [tab,setTab] = useState('conversation')
  const [busy,setBusy] = useState(false)
  const dialog = useRef<HTMLDialogElement>(null)
  const close = useRef<HTMLButtonElement>(null)
  const opener = useRef<HTMLElement | null>(null)
  const activeId = useRef<string | null>(null)
  useEffect(() => {
    const c = new AbortController()
    fetchInteractions(query,c.signal).then(p => { if (!c.signal.aborted) setPage(p) }).catch(e => {
      if (!c.signal.aborted) { if (e instanceof AdminApiError && e.status === 401) onSessionLost(e.message); else setError(e instanceof Error ? e.message : 'No pudimos cargar el historial.') }
    })
    return () => c.abort()
  },[query,refresh,onSessionLost])
  useEffect(() => {
    if (!selected) return
    const c = new AbortController(); activeId.current=selected
    const modal=dialog.current; modal?.showModal(); close.current?.focus()
    const previousOverflow=document.body.style.overflow; document.body.style.overflow='hidden'
    fetchInteraction(selected,c.signal).then(d => { if (!c.signal.aborted) setDetail(d) }).catch(e => {
      if (!c.signal.aborted) { if (e instanceof AdminApiError && e.status===401) onSessionLost(e.message); else setDetailError(e instanceof Error ? e.message : 'No pudimos abrir el detalle.') }
    })
    return () => { c.abort(); activeId.current=null; modal?.close(); document.body.style.overflow=previousOverflow; const returnTo = opener.current?.isConnected ? opener.current : document.querySelector<HTMLButtonElement>(`[data-interaction-id="${selected}"]`); returnTo?.focus() }
  },[selected,onSessionLost])
  async function review() {
    if (!selected || busy) return
    const id=selected; setBusy(true); setDetailError('')
    try { const result=await reviewInteraction(id); if(activeId.current===id) { setDetail(result); setRefresh(x=>x+1) } }
    catch(e) { if(e instanceof AdminApiError && e.status===401) onSessionLost(e.message); else if(activeId.current===id) setDetailError(e instanceof Error ? e.message : 'No se guardó la revisión.') }
    finally { if(activeId.current===id) setBusy(false) }
  }
  function search(q:URLSearchParams) { setPage(null); setError(''); setQuery(q) }
  function navigate(n:number) { const q=new URLSearchParams(query); q.set('pagina',String(n));search(q) }
  return <section className="interaction-history" aria-labelledby="interaction-title">
    <header className="interaction-heading"><div><p className="interaction-eyebrow"><ShieldCheck size={16}/>MODERACIÓN ÉTICA</p><h1 id="interaction-title">Historial IA <small>HU-54</small></h1><p>Revisa las respuestas de Rupi y las reglas que se aplicaron.</p></div><span className="interaction-access"><LockKeyhole size={15}/>Acceso restringido</span></header>
    <div className="interaction-summary" aria-label="Resumen de resultados"><span><Activity size={20}/><b>{page?.total ?? '—'}</b> interacciones</span><span><ShieldCheck size={20}/><b>{page?.pendientes ?? '—'}</b> por revisar</span><span><Clock3 size={20}/><b>{page ? duration(page.promedio) : '—'}</b> respuesta media</span></div>
    <form className="interaction-filters" onSubmit={e=>{e.preventDefault();search(new URLSearchParams(filters))}}>
      <label className="interaction-search">Buscar interacción<div><Search size={17}/><input aria-label="Código de interacción" placeholder="Código o inicio del código…" maxLength={80} value={filters.q} onChange={e=>setFilters({...filters,q:e.target.value})}/></div></label>
      <label>Desde<input type="date" value={filters.desde} onChange={e=>setFilters({...filters,desde:e.target.value})}/></label>
      <label>Hasta<input type="date" min={filters.desde || undefined} value={filters.hasta} onChange={e=>setFilters({...filters,hasta:e.target.value})}/></label>
      <label>Resultado<select value={filters.resultado} onChange={e=>setFilters({...filters,resultado:e.target.value})}><option value="">Todos</option><option value="permitida">Sin bloqueo</option><option value="bloqueada">Bloqueada</option></select></label>
      <label>Revisión<select value={filters.revision} onChange={e=>setFilters({...filters,revision:e.target.value})}><option value="">Todas</option><option value="pendiente">Pendiente</option><option value="revisada">Revisada</option></select></label>
      <button className="admin-btn interaction-primary" type="submit">Buscar</button><button className="admin-btn" type="button" onClick={()=>{setFilters({q:'',desde:'',hasta:'',resultado:'',revision:''});search(new URLSearchParams())}}>Limpiar</button>
    </form>
    <p className="interaction-caption">Fechas en hora de Perú. Los indicadores corresponden a los filtros aplicados.</p>
    {error && <div role="alert" className="admin-feedback admin-feedback--error">{error} <button className="admin-btn" onClick={()=>{setError('');setPage(null);setRefresh(n=>n+1)}}>Reintentar</button></div>}
    {!page && !error && <p role="status">Consultando interacciones…</p>}
    {page && <section className="interaction-list"><header><h2>Interacciones registradas</h2><span>{page.total} resultados</span></header>
      {!page.items.length ? <div className="interaction-empty"><Sparkles size={28}/><h3>{page.total ? 'Esta página no tiene registros' : 'Sin interacciones para mostrar'}</h3><p>Las nuevas respuestas evaluadas se registrarán aquí cuando se utilice un proveedor de IA configurado.</p><p>No se generan conversaciones de ejemplo.</p></div> : <div className="interaction-table-wrap"><table><thead><tr><th>Interacción</th><th>Recurso</th><th>Resultado</th><th>Revisión</th><th>Tiempo</th><th><span className="interaction-sr">Acciones</span></th></tr></thead><tbody>{page.items.map(row=><tr key={row.id}><td><code>{row.id.slice(0,8)}</code><time>{formatDateTime(row.creada_en)}</time></td><td>{resources[row.recurso] || row.recurso}</td><td><span className={`interaction-pill ${row.permitida ? '' : 'is-blocked'}`}>{row.permitida ? 'Sin bloqueo' : 'Bloqueada'}</span></td><td>{row.revisada_en ? 'Revisada' : 'Pendiente'}</td><td>{duration(row.duracion_ms)}</td><td><button className="admin-btn" data-interaction-id={row.id} aria-label={`Ver interacción ${row.id.slice(0,8)}`} onClick={e=>{opener.current=e.currentTarget;setDetail(null);setDetailError('');setBusy(false);setTab('conversation');setSelected(row.id)}}>Ver detalle <ArrowRight size={14}/></button></td></tr>)}</tbody></table></div>}
      <footer><button className="admin-btn" disabled={page.pagina<=1} onClick={()=>navigate(page.pagina-1)}><ArrowLeft size={14}/>Anterior</button><span>Página {page.pagina} de {Math.max(1,Math.ceil(page.total/page.porPagina))}</span><button className="admin-btn" disabled={page.pagina*page.porPagina>=page.total} onClick={()=>navigate(page.pagina+1)}>Siguiente<ArrowRight size={14}/></button></footer>
    </section>}
    <p className="interaction-caption"><LockKeyhole size={14}/>Las consultas y revisiones quedan auditadas. El filtro de patrones no sustituye la revisión humana.</p>
    {selected && <dialog ref={dialog} className="interaction-drawer" aria-labelledby="interaction-detail-title" onCancel={e=>{e.preventDefault();setSelected(null)}}>
      <header><div><p className="interaction-eyebrow">DETALLE DE INTERACCIÓN</p><h2 id="interaction-detail-title">Registro {selected.slice(0,8)}</h2></div><button ref={close} className="admin-btn" aria-label="Cerrar detalle" onClick={()=>setSelected(null)}><X size={18}/></button></header>
      <nav aria-label="Detalle del historial">{[{id:'conversation',label:'Conversación'},{id:'analysis',label:'Análisis IA'},{id:'trace',label:'Trazabilidad'}].map(t=><button className={tab===t.id ? 'is-active' : ''} key={t.id} aria-pressed={tab===t.id} onClick={()=>setTab(t.id)}>{t.label}</button>)}</nav>
      {detailError && <p role="alert" className="admin-feedback admin-feedback--error">{detailError}</p>}
      {!detail && !detailError && <p role="status">Abriendo registro…</p>}
      {detail && <><div className="interaction-detail-content">
        {tab==='conversation' && <><p className="interaction-caption">Sin identidad vinculada. Este registro no identifica al estudiante.</p><article className="interaction-message"><b>Consulta</b><p>{detail.pregunta || 'Este recurso no registra una pregunta individual. No se conserva el prompt de sistema.'}</p></article><article className="interaction-message is-rupi"><b>Respuesta generada {detail.permitida ? '' : '· Bloqueada'}</b><p>{detail.respuesta}</p></article>{detail.alternativa && <article className="interaction-message"><b>Alternativa prevista por la política</b><p>{detail.alternativa}</p></article>}<p className="interaction-caption">Se ocultan patrones de correo, números personales y secretos etiquetados. Esta protección no garantiza anonimización completa. El resultado de moderación no confirma la entrega al estudiante.</p></>}
        {tab==='analysis' && <><h3><Sparkles size={18}/>Evaluación de la respuesta</h3><dl><dt>Proveedor</dt><dd>{detail.proveedor || 'No disponible'}</dd><dt>Modelo solicitado</dt><dd>{detail.modelo || 'No disponible'}</dd><dt>Duración del proveedor</dt><dd>{duration(detail.duracion_ms)}</dd><dt>Política aplicada</dt><dd>{detail.version_reglas ? `Versión ${detail.version_reglas}` : 'Protección base'}</dd><dt>Método</dt><dd>Coincidencia de patrones</dd><dt>Resultado</dt><dd>{detail.permitida ? 'Sin coincidencias de bloqueo' : 'Bloqueada'}</dd></dl><h3>Reglas detectadas</h3>{detail.reglas.length ? <ul>{detail.reglas.map(x=><li key={x}>{x}</li>)}</ul> : <p>No hubo coincidencias. Esto no garantiza que la respuesta sea adecuada.</p>}<p className="interaction-caption">No se calcula confianza ni evaluación semántica con IA. El modelo mostrado es el solicitado al proveedor.</p></>}
        {tab==='trace' && <><dl><dt>Identificador</dt><dd><code>{detail.id}</code></dd><dt>Servicio de origen</dt><dd>{detail.origen}</dd><dt>Registro</dt><dd>{formatDateTime(detail.creada_en)}</dd><dt>Revisión</dt><dd>{detail.revisada_en ? formatDateTime(detail.revisada_en) : 'Pendiente'}</dd>{detail.revisor_id && <><dt>Cuenta revisora</dt><dd><code>{detail.revisor_id}</code></dd></>}</dl><h3>Últimos accesos y acciones</h3><ol className="interaction-timeline">{detail.traza.map((t,i)=><li key={i}><strong>{traces[t.accion] || t.accion}</strong><time>{formatDateTime(t.fecha)}</time></li>)}</ol><p className="interaction-caption">Se muestran hasta 50 eventos recientes de este registro.</p></>}
      </div><footer>{detail.revisada_en ? <span><CheckCheck size={18}/>Revisión registrada</span> : admin.permisos.includes('interacciones.revisar') ? <button className="admin-btn interaction-primary" disabled={busy} onClick={review}>{busy ? 'Guardando…' : 'Marcar como revisada'}</button> : <span>Acceso de consulta</span>}<small>Revisar no cambia ni libera la respuesta.</small></footer></>}
    </dialog>}
  </section>
}
