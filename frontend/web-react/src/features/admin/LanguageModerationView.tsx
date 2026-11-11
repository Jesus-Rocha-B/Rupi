import { useEffect, useRef, useState } from 'react'
import { CheckCircle2, LockKeyhole, Play, RefreshCw, ShieldCheck } from 'lucide-react'
import { AdminApiError, fetchLanguageRules, saveLanguageRules, evaluateLanguage, testLanguageRules, publishLanguageRules, type Administrator, type LanguageState, type LanguageDecision } from '../../services/adminApi'
import './LanguageModeration.css'

export function LanguageModerationView({ admin, onSessionLost }: { admin: Administrator; onSessionLost: (message: string) => void }) {
  const [data, setData] = useState<LanguageState | null>(null)
  const [terms, setTerms] = useState<Record<string, string>>({})
  const [alternative, setAlternative] = useState('')
  const [text, setText] = useState('')
  const [example, setExample] = useState('')
  const [decision, setDecision] = useState<LanguageDecision | null>(null)
  const [results, setResults] = useState<{ caso: string; correcto: boolean }[]>([])
  const [tab, setTab] = useState('rules')
  const [dirty, setDirty] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [retry, setRetry] = useState(0)
  const testPanel = useRef<HTMLElement>(null)
  const writable = admin.permisos.includes('moderacion.gestionar')
  function accept(value: LanguageState) { setData(value); setTerms(Object.fromEntries(Object.entries(value.configuracion.extras).map(([k,v]) => [k,v.join('\n')]))); setAlternative(value.configuracion.alternativa); setDirty(false) }
  function fail(e: unknown) { if (e instanceof AdminApiError && e.status === 401) onSessionLost(e.message); else setError(e instanceof Error ? e.message : 'No se pudo completar la operación.') }
  useEffect(() => {
    const c = new AbortController(); setError(''); setData(null)
    fetchLanguageRules(c.signal).then(value => { if (!c.signal.aborted) { accept(value); setText(value.casos[1]?.texto || ''); setExample(value.casos[1]?.nombre || '') } }).catch(e => { if (!c.signal.aborted) fail(e) })
    return () => c.abort()
  }, [retry, onSessionLost])
  function changed() { setDirty(true); setDecision(null); setResults([]); setMessage('') }
  async function act(action: 'save' | 'test' | 'publish' | 'evaluate') {
    if (!data || busy) return
    setBusy(true); setError(''); setMessage('')
    try {
      if (action === 'save') {
        accept(await saveLanguageRules(data.revision, { extras: Object.fromEntries(Object.entries(terms).map(([k,v]) => [k, v.split('\n').map(t => t.trim()).filter(Boolean)])), alternativa: alternative }))
        setResults([]); setDecision(null); setMessage('Borrador guardado en la base compartida. Ejecuta las pruebas antes de publicar.')
      } else if (action === 'test') {
        const value = await testLanguageRules(data.revision); accept(value.estado); setResults(value.resultados); setMessage(value.estado.probada ? 'Pruebas del borrador superadas. Puedes publicar esta revisión.' : 'Hay casos que fallan. Revisa las frases adicionales.')
      } else if (action === 'publish') {
        accept(await publishLanguageRules(data.revision)); setMessage('Reglas publicadas. Se aplican a las nuevas respuestas generadas por IA.')
      } else setDecision(await evaluateLanguage(data.revision, text))
    } catch (e) { fail(e) } finally { setBusy(false) }
  }
  return <section className="language-admin" aria-labelledby="language-title">
    <nav className="admin-section-nav" aria-label="Secciones de moderación">{[{id:'rules',name:'Reglas de lenguaje'},{id:'tests',name:'Pruebas de respuestas'},{id:'history',name:'Historial de cambios'}].map(item => <button type="button" className={`admin-section-link ${tab === item.id ? 'is-active' : ''}`} aria-current={tab === item.id ? 'page' : undefined} key={item.id} onClick={() => { setTab(item.id); if (item.id === 'tests') setTimeout(() => { testPanel.current?.scrollIntoView({ block:'start', behavior:'auto' }); testPanel.current?.querySelector('textarea')?.focus() },0) }}>{item.name}</button>)}</nav>
    <header className="language-heading"><div><h1 id="language-title">Reglas de lenguaje <small>HU-53</small></h1><p>Configura reglas de evaluación seguras para estudiantes de primaria.</p></div><button className="admin-btn" disabled={busy} onClick={() => { if (!dirty || window.confirm('Hay cambios sin guardar. ¿Quieres recargar?')) { setRetry(n => n+1); setDecision(null); setResults([]); setMessage('') } }}><RefreshCw size={15} />Recargar</button></header>
    {error && <p role="alert" className="admin-feedback admin-feedback--error">{error}</p>}
    {message && <p role="status" className="admin-feedback admin-feedback--info">{message}</p>}
    {!data && !error && <p role="status">Cargando reglas…</p>}
    {data && <>
      <div className="language-scope"><span><b>Alcance</b> Primaria · Todos los grados</span><span><b>Aplicación</b> Nuevas respuestas de IA</span><span><b>Estado</b> Borrador · r{data.revision}</span><span className="language-tag">{data.versionPublicada ? `Publicada: v${data.versionPublicada}` : 'Protección base activa'}</span></div>
      {tab === 'history' ? <section className="admin-card"><h2>Historial de publicaciones</h2>{!data.historial.length ? <p>Todavía no se publicaron configuraciones. La protección base permanece activa.</p> : <ol className="language-history">{data.historial.map(h => <li key={h.version}><strong>Versión {h.version}</strong><span>{h.autor}</span><time dateTime={h.fecha}>{new Intl.DateTimeFormat('es-PE',{dateStyle:'medium',timeStyle:'short',timeZone:'America/Lima'}).format(new Date(h.fecha))}</time></li>)}</ol>}</section> : <div className="language-layout">
        <section className="admin-card language-rules"><h2>Configurar reglas</h2><div className="language-row language-row-heading"><span>Regla</span><span>Acción</span><span>Estado</span></div>
          {data.reglas.map(rule => <div className="language-rule" key={rule.codigo}><div className="language-row"><div><strong>{rule.nombre}</strong><p>{rule.descripcion}</p></div><span className="language-action">Bloquear</span><span className="language-active"><CheckCircle2 size={17} />Activa</span></div><details><summary>Frases adicionales ({terms[rule.codigo]?.split('\n').filter(Boolean).length || 0})</summary><label>Una frase por línea<textarea aria-label={`Frases adicionales: ${rule.nombre}`} disabled={!writable || busy} rows={3} maxLength={1700} value={terms[rule.codigo] || ''} onChange={e => { setTerms({...terms,[rule.codigo]:e.target.value}); changed() }} /></label><small>Hasta 20 frases de 3 a 80 caracteres. Se bloquean coincidencias completas.</small></details></div>)}
          <p className="language-safe"><ShieldCheck size={18} />Las respuestas bloqueadas no se muestran al estudiante.</p>
          <p className="language-note"><LockKeyhole size={14} />Las cuatro protecciones base son obligatorias. Puedes añadir frases; no desactivarlas.</p>
          <label>Mensaje alternativo<select aria-label="Mensaje alternativo" disabled={!writable || busy} value={alternative} onChange={e => { setAlternative(e.target.value); changed() }}>{data.alternativas.map(a => <option key={a}>{a}</option>)}</select></label>
          <div className="language-badges"><span>Reglas: {data.reglas.length}</span><span>Borrador{dirty ? ' · Sin guardar' : ''}</span><span>{data.probada && !dirty ? 'Pruebas superadas' : 'Pruebas pendientes'}</span></div>
          <p className="language-note">Filtro de patrones, sin evaluación semántica. Puede omitir lenguaje dañino o bloquear citas educativas: mantén la revisión docente.</p>
        </section>
        <aside className="admin-card language-tests" ref={testPanel}><h2>Probar una respuesta</h2><p>Revisa el resultado antes de publicar las reglas.</p>
          <label>Caso de prueba<select aria-label="Caso de prueba" disabled={busy} value={example} onChange={e => { setExample(e.target.value); setText(data.casos.find(c => c.nombre === e.target.value)?.texto || ''); setDecision(null) }}><option value="">Caso personalizado</option>{data.casos.map(c => <option key={c.nombre}>{c.nombre}</option>)}</select></label>
          <label>Respuesta de ejemplo<textarea aria-label="Respuesta de ejemplo" rows={4} maxLength={3000} value={text} disabled={busy} onChange={e => { setText(e.target.value); setExample(''); setDecision(null) }} /></label>
          <button className="admin-btn language-evaluate" disabled={busy || dirty || !text.trim()} onClick={() => act('evaluate')}><Play size={15} />Evaluar ejemplo</button>
          {decision && <div className="language-decision" aria-live="polite"><strong>{decision.permitida ? 'Sin coincidencias de bloqueo' : 'Respuesta bloqueada'}</strong>{decision.reglas.map(r => <p key={r}>Incumple: {r}</p>)}{decision.alternativa && <><b>Alternativa segura</b><p>{decision.alternativa}</p></>}<small>{decision.aviso}</small></div>}
          <p className="language-note">El texto de prueba no se guarda ni se muestra a estudiantes.</p>
          {results.length > 0 && <ul className="language-results" aria-live="polite">{results.map(r => <li key={r.caso}>{r.correcto ? '✓' : '✕'} {r.caso}: {r.correcto ? 'Correcto' : 'Revisar'}</li>)}</ul>}
          {dirty && <p>Guarda el borrador para probarlo y publicarlo.</p>}
          {writable ? <div className="language-buttons"><button className="admin-btn" disabled={busy || !dirty} onClick={() => act('save')}>Guardar borrador</button><button className="admin-btn" disabled={busy || dirty} onClick={() => act('test')}>Ejecutar pruebas</button><button className="admin-btn language-publish" disabled={busy || dirty || !data.probada} onClick={() => act('publish')}>Publicar reglas</button></div> : <p>Tu rol permite consultar y evaluar ejemplos. No permite guardar ni publicar.</p>}
        </aside>
      </div>}
      <p className="language-footer">Cada publicación registra versión, autor y fecha en la base compartida. Las reglas no activan un proveedor de IA.</p>
    </>}
  </section>
}
