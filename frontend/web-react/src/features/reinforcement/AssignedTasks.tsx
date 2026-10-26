import { useEffect, useRef, useState } from 'react'
import { assignmentsRequest, assignmentState, type Assignment, type AssignmentBoard, type StudentAssignmentBoard } from '../../services/assignmentsApi'
import './AssignedTasks.css'

function useAssignments<T>(role: 'teacher' | 'student', path: string) {
  const [revision, setRevision] = useState(0)
  const [result, setResult] = useState<{ key: string; data?: T; error?: string }>()
  const key = role + path + revision
  useEffect(() => {
    const abort = new AbortController()
    assignmentsRequest<T>(role, path, 'GET', undefined, abort.signal)
      .then(data => { if (!abort.signal.aborted) setResult({ key, data }) })
      .catch((e: Error) => { if (!abort.signal.aborted) setResult({ key, error: e.message }) })
    return () => abort.abort()
  }, [role, path, key])
  return { data: result?.key === key ? result.data : undefined, error: result?.key === key ? result.error : undefined, reload: () => setRevision(r => r + 1) }
}
function AssignmentInfo({ task }: { task: Assignment }) {
  return <>
    <h3>{task.title}</h3><p><strong>{task.nodeTitle}</strong> · {assignmentState(task)}</p>
    <p className="assigned-instructions">{task.instructions}</p>
    <p>Material: <a href={task.url} target="_blank" rel="noopener noreferrer">{task.resourceTitle} ↗</a></p>
    <p className="assigned-note">Asignado: {new Date(task.assignedAt).toLocaleString('es-PE', { timeZone: 'America/Lima' })} (Perú).</p>
    {task.reflection && <p><strong>Lo que practiqué:</strong> {task.reflection}</p>}
  </>
}
export function TeacherAssignments({ room, route, student }: { room: string; route: string; student: string }) {
  const query = '?' + new URLSearchParams({ classroomId: room, routeId: route })
  const { data, error, reload } = useAssignments<AssignmentBoard>('teacher', query)
  const [node, setNode] = useState('')
  const [resource, setResource] = useState('')
  const [selected, setSelected] = useState<string[]>([])
  const [title, setTitle] = useState('')
  const [instructions, setInstructions] = useState('')
  const [reason, setReason] = useState('')
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [saveError, setSaveError] = useState('')
  const requestId = useRef<string | null>(null)
  const lastPayload = useRef('')
  const inFlight = useRef(false)
  const lifetime = useRef<AbortController | null>(null)
  useEffect(() => { lifetime.current = new AbortController(); return () => lifetime.current?.abort() }, [])
  if (error) return <section className="assigned-tasks" role="alert"><p>{error}</p><button onClick={reload}>Reintentar refuerzos</button></section>
  if (!data) return <p role="status">Consultando necesidades y tareas de refuerzo…</p>
  const candidates = data.candidates.filter(c => !student || c.studentId === student)
  const needs = candidates.filter(c => c.status === 'REQUIERE_ATENCION')
  const nodes = [...new Map(needs.map(c => [c.nodeId, c])).values()]
  const current = nodes.find(c => c.nodeId === node)
  const options = current?.resources ?? []
  const rows = needs.filter(c => c.nodeId === node)
  const tasks = data.assignments.filter(t => !student || t.studentId === student)
  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (inFlight.current) return
    const payload = { nodeId: node, resourceId: resource, students: selected, title, instructions, reason }
    const serialized = JSON.stringify(payload)
    if (lastPayload.current !== serialized || !requestId.current) { requestId.current = crypto.randomUUID(); lastPayload.current = serialized }
    inFlight.current = true; setSaving(true); setMessage(''); setSaveError('')
    try {
      const result = await assignmentsRequest<{ created: number; existing: number }>('teacher', query, 'POST', { ...payload, requestId: requestId.current }, lifetime.current?.signal)
      if (lifetime.current?.signal.aborted) return
      setMessage(result.created + ' asignaciones creadas. ' + result.existing + ' ya estaban registradas.')
      setSelected([]); requestId.current = null; reload()
    } catch (e) { if (!lifetime.current?.signal.aborted) setSaveError((e as Error).message) }
    finally { inFlight.current = false; if (!lifetime.current?.signal.aborted) setSaving(false) }
  }
  return <section className="assigned-tasks" aria-labelledby="teacher-assigned-title">
    <h2 id="teacher-assigned-title">Refuerzos para cada estudiante</h2>
    <p>Al menos dos respuestas y menos del 70 % de aciertos en la parada. Confirma la dificultad y elige qué ejercicios practicarán.</p>
    {!data.available && <p role="status">Las asignaciones todavía no están habilitadas. El administrador debe revisar y aplicar la migración HU-20.</p>}
    {message && <p role="status">{message}</p>}{saveError && <p role="alert">{saveError}</p>}
    {!needs.length && <p>No hay dificultades detectadas con evidencia suficiente en esta selección. Las actividades sin respuestas no se califican con cero.</p>}
    <details><summary>Evidencia disponible ({candidates.length} registros)</summary>
      <ul>{candidates.map(c => <li key={c.studentId + c.nodeId}>{c.studentName} · {c.unitTitle} · {c.nodeTitle}: {c.answered ? c.correct + '/' + c.answered + ' aciertos' : 'Sin respuestas'} · {({
        SIN_INSCRIPCION: 'Sin inscripción activa', SIN_RESPUESTAS: 'Sin respuestas', EVIDENCIA_INSUFICIENTE: 'Falta evidencia', REQUIERE_ATENCION: 'Requiere atención', EN_AVANCE: 'En avance',
      } as Record<string, string>)[c.status]}</li>)}</ul>
    </details>
    {needs.length > 0 && <form onSubmit={event => void submit(event)}>
      <fieldset disabled={saving || !data.available}><legend>Preparar una tarea</legend>
        <label>Parada que necesita refuerzo<select required value={node} onChange={e => { setNode(e.target.value); setResource(''); setSelected([]) }}>
          <option value="">Elige una parada</option>{nodes.map(n => <option key={n.nodeId} value={n.nodeId}>{n.unitTitle} · {n.nodeTitle}</option>)}
        </select></label>
        {current && !options.length && <p>No hay materiales MINEDU adecuados en el catálogo para este grado y área.</p>}
        <label>Material MINEDU<select required value={resource} onChange={e => setResource(e.target.value)}><option value="">Elige un material</option>
          {options.map(r => <option key={r.id} value={r.id}>{r.title}</option>)}</select></label>
        <fieldset><legend>Estudiantes que recibirán esta tarea</legend>{rows.map(c => <label className="assigned-check" key={c.studentId}>
          <input type="checkbox" checked={selected.includes(c.studentId)} onChange={e => setSelected(old => e.target.checked ? [...old, c.studentId] : old.filter(id => id !== c.studentId))} />
          {c.studentName} · {c.correct}/{c.answered} aciertos</label>)}</fieldset>
        <label>Título de la tarea<input required maxLength={180} value={title} onChange={e => setTitle(e.target.value)} /></label>
        <label>Instrucciones para el alumno (indica páginas o ejercicios del material)<textarea required maxLength={2000} value={instructions} onChange={e => setInstructions(e.target.value)} /></label>
        <label>Motivo del refuerzo<textarea required maxLength={1000} value={reason} onChange={e => setReason(e.target.value)} /></label>
        <p className="assigned-note">Para otra necesidad, prepara otra tarea con distintos ejercicios o una parada diferente. No se cambiarán las calificaciones anteriores.</p>
        <button disabled={!selected.length || !resource || saving} type="submit">{saving ? 'Asignando…' : 'Confirmar asignación'}</button>
      </fieldset>
    </form>}
    <h3>Seguimiento de tareas ({tasks.length})</h3><button type="button" disabled={saving} onClick={reload}>Actualizar seguimiento</button>
    {!tasks.length && <p>No hay tareas asignadas en esta selección.</p>}
    {tasks.map(task => <article key={task.id}><strong>{task.studentName}</strong><AssignmentInfo task={task} />
      <p>Motivo: {task.reason}</p><p>Evidencia al asignar: {task.correct}/{task.answered} aciertos.</p>
      <p className="assigned-note">Práctica con material externo: sin nota automática. La reflexión del alumno orienta la revisión del docente; no demuestra por sí sola dominio.</p>
    </article>)}
  </section>
}
function StudentTask({ task, reload }: { task: Assignment; reload: () => void }) {
  const [reflection, setReflection] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const inFlight = useRef(false)
  const lifetime = useRef<AbortController | null>(null)
  useEffect(() => { lifetime.current = new AbortController(); return () => lifetime.current?.abort() }, [])
  async function update(action: 'start' | 'complete') {
    if (inFlight.current) return
    inFlight.current = true; setBusy(true); setError('')
    try {
      await assignmentsRequest('student', '/' + encodeURIComponent(task.id) + '/' + action, 'PUT', action === 'complete' ? { reflection } : undefined, lifetime.current?.signal)
      if (!lifetime.current?.signal.aborted) reload()
    } catch (e) { if (!lifetime.current?.signal.aborted) setError((e as Error).message) }
    finally { inFlight.current = false; if (!lifetime.current?.signal.aborted) setBusy(false) }
  }
  return <article><AssignmentInfo task={task} />
    {error && <p role="alert">{error}</p>}
    {!task.completedAt && (!task.startedAt ? <button disabled={busy} onClick={() => void update('start')}>Comenzar mi práctica</button> : <>
      <label>Cuéntale a tu profe qué practicaste y qué te costó<textarea minLength={10} maxLength={1000} value={reflection} onChange={e => setReflection(e.target.value)} /></label>
      <button disabled={busy || reflection.trim().length < 10} onClick={() => void update('complete')}>{busy ? 'Guardando…' : 'Ya terminé mi práctica'}</button>
    </>)}
    <p className="assigned-note">Abrir el material no completa la tarea. Practica siguiendo las instrucciones y comparte cómo te fue. Esta práctica no cambia tu nota.</p>
  </article>
}
export function StudentAssignments() {
  const { data, error, reload } = useAssignments<StudentAssignmentBoard>('student', '')
  return <details className="assigned-tasks"><summary>Mis tareas de refuerzo</summary>
    {error && <div role="alert"><p>{error}</p><button onClick={reload}>Volver a consultar</button></div>}
    {!data && !error && <p role="status">Buscando tus tareas…</p>}
    {data && (!data.available || !data.assignments.length) && <p>Por ahora no tienes tareas de refuerzo asignadas.</p>}
    {data?.assignments.map(task => <StudentTask key={task.id} task={task} reload={reload} />)}
    {data && <button onClick={reload}>Actualizar mis tareas</button>}
  </details>
}
