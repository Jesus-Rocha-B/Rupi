import { useEffect, useState } from 'react'
import { BookOpenCheck, Footprints } from 'lucide-react'
import { fetchClassroomTutorSupport, saveClassroomTutorSupport, type ClassroomTutorSupport } from '../../services/tutorSupportApi'
import type { TutorAssistanceMode } from '../../services/aiApi'

const options: { mode: TutorAssistanceMode; title: string; text: string; example: string }[] = [
  {
    mode: 'PISTAS_GUIADAS', title: 'Pistas guiadas',
    text: 'Rupi no da la respuesta: ofrece una pista y una pregunta para que el estudiante dé el siguiente paso.',
    example: '«Imagina 4 cajas con 8 chirimoyas cada una. ¿Cuántas hay en dos cajas?»',
  },
  {
    mode: 'EXPLICACION_DIRECTA', title: 'Explicación directa',
    text: 'Rupi explica el procedimiento en pasos cortos, muestra el resultado y propone un ejercicio parecido.',
    example: '«1. Sumamos 8 cuatro veces. 2. 8 + 8 + 8 + 8 = 32. Resultado: 32.»',
  },
]

const limaDate = (value: string) => new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Lima' }).format(new Date(value))

/** HU-15: el docente elige cómo ayuda el tutor Rupi a los estudiantes de esta aula. */
export function TutorSupportSetting({ room }: { room: string }) {
  const [current, setCurrent] = useState<ClassroomTutorSupport | null>(null)
  const [choice, setChoice] = useState<TutorAssistanceMode | null>(null)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    fetchClassroomTutorSupport(room, controller.signal)
      .then(data => { setCurrent(data); setChoice(data.mode); setError('') })
      .catch(() => { if (!controller.signal.aborted) setError('No pudimos consultar la ayuda del tutor para esta aula.') })
    return () => controller.abort()
  }, [room, attempt])

  const save = async () => {
    if (!choice) return
    setSaving(true); setSaved(false); setError('')
    try {
      const data = await saveClassroomTutorSupport(room, choice)
      setCurrent(data); setChoice(data.mode); setSaved(true)
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : 'No pudimos guardar el cambio.')
    } finally {
      setSaving(false)
    }
  }

  if (error && !current) {
    return <div className="report-error" role="alert"><p>{error}</p><button type="button" onClick={() => setAttempt(value => value + 1)}>Volver a consultar</button></div>
  }
  if (!current || !choice) return <p role="status">Consultando la ayuda del tutor Rupi…</p>

  const changed = choice !== current.mode
  return <section className="tutor-support" aria-labelledby="tutor-support-title">
    <div className="tutor-support-head">
      <h2 id="tutor-support-title">Ayuda del tutor Rupi</h2>
      <p>Elige cómo responde Rupi cuando un estudiante de <strong>{current.classroomName}</strong> le pide ayuda con un ejercicio.</p>
    </div>
    <fieldset>
      <legend className="visually-hidden">Modo de ayuda del tutor</legend>
      <div className="tutor-support-options">
        {options.map(option => {
          const Icon = option.mode === 'PISTAS_GUIADAS' ? Footprints : BookOpenCheck
          return <label key={option.mode} className={`tutor-support-option ${choice === option.mode ? 'is-selected' : ''}`}>
            <input type="radio" name={`tutor-mode-${room}`} value={option.mode} checked={choice === option.mode}
              onChange={() => { setChoice(option.mode); setSaved(false) }} />
            <span className="tutor-support-option-title"><Icon size={20} aria-hidden="true" /> {option.title}</span>
            <span>{option.text}</span>
            <span className="tutor-support-example">{option.example}</span>
          </label>
        })}
      </div>
    </fieldset>
    <div className="tutor-support-foot">
      <p className="report-note" aria-live="polite">
        {saved ? 'Guardado. Se aplica en la próxima pregunta que hagan tus estudiantes.'
          : current.configured && current.updatedAt
            ? `Última actualización: ${limaDate(current.updatedAt)} (Perú)${current.updatedBy ? ` · ${current.updatedBy}` : ''}`
            : 'Esta aula usa pistas guiadas mientras no elijas otra opción.'}
      </p>
      <button type="button" onClick={save} disabled={!changed || saving}>{saving ? 'Guardando…' : 'Guardar ayuda del tutor'}</button>
    </div>
    {error && <p className="report-error" role="alert">{error}</p>}
  </section>
}
