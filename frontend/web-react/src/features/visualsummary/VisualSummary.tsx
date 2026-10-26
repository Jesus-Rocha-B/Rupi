import { useEffect, useRef, useState } from 'react'
import { quizRequest } from '../../services/quizzesApi'
import { downloadStudyPackage } from '../../services/learningRoutesApi'
import '../reinforcement/AssignedTasks.css'
import './VisualSummary.css'
export interface Summary { id: string; state: string; version: string; topic: string; grade: number; area: string; source: string; provider: string; model: string; content: { title: string; concepts: { title: string; text: string; evidence: string }[]; steps: string[]; example: string; remember: string } | null }
export function SummaryContent({ item }: { item: Summary }) {
 const d = item.content
 if (!d) return null
 return <div className="visual-sheet">
  <p className="visual-label">Contenido asistido por IA · {item.grade}.º de primaria</p>
  <h3>{d.title}</h3><p>{item.state === 'REVISADO_DOCENTE' ? 'Revisado por docente' : 'Validación automática: puede contener errores. No sustituye la revisión docente.'}</p>
  <div className="visual-concepts">{d.concepts.map((c, i) => <article key={i}><span aria-hidden="true" className="visual-number">{i + 1}</span><h4>{c.title}</h4><p>{c.text}</p></article>)}</div>
  <h4>Paso a paso</h4><ol className="visual-steps">{d.steps.map((s, i) => <li key={i}>{s}</li>)}</ol>
  <article><h4>Ejemplo explicado</h4><p>{d.example}</p></article>
  <aside><h4>Para recordar</h4><p>{d.remember}</p></aside>
  <details><summary>Leer como texto continuo</summary><p>{d.concepts.map(c => c.title + ': ' + c.text).join(' ')}</p><p>{d.steps.join(' ')}</p><p>{d.example}</p><p>{d.remember}</p></details>
  <p className="assigned-note">Tema: {item.topic} · Fuente: {item.source}</p>
 </div>
}
export function VisualSummary({ routeId, nodeId }: { routeId: string; nodeId: string }) {
 const path = 'student/learning-routes/' + routeId + '/nodes/' + nodeId + '/visual-summary'
 const [resource, setResource] = useState<Summary | null>(null), [configured, setConfigured] = useState(false), [loaded, setLoaded] = useState(false)
 const [error, setError] = useState(''), [busy, setBusy] = useState(false), [reason, setReason] = useState(''), [refresh, setRefresh] = useState(0)
 const [offline, setOffline] = useState(!navigator.onLine)
 const lock = useRef(false), request = useRef<string | null>(null), life = useRef<AbortController | null>(null)
 useEffect(() => { const c = new AbortController(); life.current = c; return () => c.abort() }, [])
 useEffect(() => { const online = () => { setOffline(false); setRefresh(n => n + 1) }; const off = () => setOffline(true); window.addEventListener('online', online); window.addEventListener('offline', off); return () => { window.removeEventListener('online', online); window.removeEventListener('offline', off) } }, [])
 useEffect(() => {
  const c = new AbortController()
  quizRequest<{ configured: boolean; resource: Summary | null }>(path, 'GET', undefined, c.signal).then(d => {
   if (!c.signal.aborted) { setResource(d.resource); setConfigured(d.configured); setLoaded(true); setError('') }
  }).catch((e: Error) => { if (!c.signal.aborted) { setError(e.message); setLoaded(true) } })
  return () => c.abort()
 }, [path, refresh])
 useEffect(() => { if (resource?.state !== 'GENERANDO') return; const timer = setTimeout(() => setRefresh(n => n + 1), 3000); return () => clearTimeout(timer) }, [resource, refresh])
 async function run(work: () => Promise<void>) {
  if (lock.current) return
  lock.current = true; setBusy(true); setError('')
  try { await work() } catch (e) { if (!life.current?.signal.aborted) setError((e as Error).message) }
  finally { lock.current = false; if (!life.current?.signal.aborted) setBusy(false) }
 }
 function generate() {
  request.current ??= crypto.randomUUID()
  void run(async () => { const d = await quizRequest<Summary>(path, 'POST', { requestId: request.current }, life.current?.signal); if (!life.current?.signal.aborted) { setResource(d); request.current = null } })
 }
 function download() {
  if (!resource) return
  void run(async () => { const pack = await downloadStudyPackage(path + '/' + resource.id + '/package', 'resumen-rupi.zip', life.current?.signal)
   if (pack.verified === false) throw new Error('La descarga no pasó la verificación. Vuelve a intentarlo.')
   if (life.current?.signal.aborted) return
   const url = URL.createObjectURL(pack.blob), a = document.createElement('a'); a.href = url; a.download = pack.fileName; a.click(); setTimeout(() => URL.revokeObjectURL(url), 30000)
  })
 }
 return <details className="assigned-tasks visual-summary"><summary>Ver resumen visual</summary>
  <p>Ideas clave para repasar este tema. Consultarlas no cambia tus notas.</p>
  {offline && <p role="status">Sin conexión: el contenido en pantalla puede estar desactualizado. Para abrir una descarga, extrae el ZIP y abre ABRIR.html.</p>}
  {error && <p role="alert">{error} No se pudo comprobar la vigencia del recurso.</p>}
  {!loaded && <p role="status">Consultando el resumen…</p>}
  {loaded && !configured && !resource && <p>La IA todavía no está configurada y no hay un resumen disponible. Puedes continuar tu lección.</p>}
  {resource?.state === 'GENERANDO' && <p role="status">Preparando el resumen. Puedes continuar estudiando.</p>}
  {resource && ['REPORTADO', 'RECHAZADO', 'DESACTUALIZADO'].includes(resource.state) && <p role="status">Este recurso está retirado o necesita revisión. No uses una copia anterior para estudiar.</p>}
  {resource?.state === 'FALLIDO' && <p>No pudimos preparar el resumen. Continúa tu lección o vuelve a intentarlo más tarde.</p>}
  {resource?.content && <><SummaryContent item={resource} /><button disabled={busy || offline || !!error} onClick={download}>Descargar para consultar sin conexión</button>
   <p>El ZIP incluye texto y diseño sin archivos externos. Al volver a conectarte, abre este tema para comprobar cambios o retiradas. Una copia descargada no puede retirarse de inmediato sin conexión.</p>
   <details><summary>Reportar contenido incorrecto</summary><label>¿Qué debemos revisar?<textarea maxLength={500} value={reason} onChange={e => setReason(e.target.value)} /></label>
    <button disabled={busy || offline || !reason.trim()} onClick={() => void run(async () => { await quizRequest(path + '/' + resource.id + '/report', 'POST', { reason }, life.current?.signal); if (!life.current?.signal.aborted) setRefresh(n => n + 1) })}>Enviar a revisión</button></details>
  </>}
  {loaded && configured && (!resource || resource.state === 'FALLIDO') && <button disabled={busy || offline} onClick={generate}>Preparar resumen visual</button>}
  <button disabled={busy || offline} onClick={() => setRefresh(n => n + 1)}>Comprobar versión y revisión</button>
 </details>
}
