import { useState, type FormEvent } from 'react'
import { X } from 'lucide-react'
import {
  AdminApiError,
  createCompetency,
  createCurriculumArea,
  updateCompetency,
  updateCurriculumArea,
  type CurriculumArea,
  type CurriculumCatalog,
  type CurriculumCompetency,
} from '../../services/adminApi'
import { SESSION_LOST } from './accountStatus'

interface Shared {
  onCancel: () => void
  onSaved: (catalog: CurriculumCatalog, selectAreaCode?: string) => void
  onSessionLost: (message: string) => void
}

function toNumber(value: string) {
  const trimmed = value.trim()
  if (trimmed === '') return null
  const parsed = Number(trimmed)
  return Number.isInteger(parsed) ? parsed : null
}

function useSubmit(onSessionLost: (message: string) => void, fallback: string) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function run(action: () => Promise<void>) {
    setBusy(true)
    setError(null)
    try {
      await action()
    } catch (failure) {
      if (failure instanceof AdminApiError && failure.status === 401) onSessionLost(failure.message || SESSION_LOST)
      else setError(failure instanceof AdminApiError ? failure.message : fallback)
    } finally {
      setBusy(false)
    }
  }

  return { busy, error, setError, run }
}

interface AreaFormProps extends Shared {
  versionId: string
  /** Sin área se registra una nueva; con área se corrige su nombre y su orden. */
  area?: CurriculumArea
}

export function AreaForm({ versionId, area, onCancel, onSaved, onSessionLost }: AreaFormProps) {
  const [codigo, setCodigo] = useState(area?.codigo ?? '')
  const [nombre, setNombre] = useState(area?.nombre ?? '')
  const [orden, setOrden] = useState(area?.orden ? String(area.orden) : '')
  const { busy, error, setError, run } = useSubmit(onSessionLost, 'No pudimos guardar el área.')

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!nombre.trim() || (!area && !codigo.trim())) {
      setError('Completa el código y el nombre del área.')
      return
    }
    run(async () => {
      if (area) {
        onSaved(await updateCurriculumArea(area.id, { nombre, orden: toNumber(orden) }))
      } else {
        const code = codigo.trim().toUpperCase()
        onSaved(await createCurriculumArea({ version: versionId, codigo: code, nombre, orden: toNumber(orden) }), code)
      }
    })
  }

  const form = (
    <form className="admin-form" onSubmit={handleSubmit} noValidate>
      <div className="admin-form-row">
        <label className="admin-field">
          <span>Código</span>
          <input
            value={codigo}
            onChange={event => setCodigo(event.target.value.toUpperCase())}
            maxLength={10}
            autoCapitalize="characters"
            spellCheck={false}
            disabled={Boolean(area)}
          />
          <small>{area ? 'El código no cambia: las rutas lo usan.' : 'Ej. MAT, COM o EF.'}</small>
        </label>
        <label className="admin-field">
          <span>Orden <em>(opcional)</em></span>
          <input type="number" inputMode="numeric" min={1} max={20} value={orden} onChange={event => setOrden(event.target.value)} />
          <small>Como en el programa curricular.</small>
        </label>
      </div>
      <label className="admin-field">
        <span>Nombre del área</span>
        <input value={nombre} onChange={event => setNombre(event.target.value)} maxLength={120} />
      </label>
      {error && <p className="admin-feedback admin-feedback--error" role="alert">{error}</p>}
      <div className="admin-actions">
        <button type="button" className="admin-btn admin-btn--quiet" onClick={onCancel} disabled={busy}>Cancelar</button>
        <button type="submit" className="admin-btn admin-btn--primary" disabled={busy}>
          {busy ? 'Guardando…' : area ? 'Guardar cambios' : 'Registrar área'}
        </button>
      </div>
    </form>
  )

  if (area) return <div className="admin-panel-block">{form}</div>

  return (
    <section className="admin-card admin-panel" aria-labelledby="admin-new-area-title">
      <div className="admin-panel-head">
        <h2 id="admin-new-area-title">Nueva área curricular</h2>
        <button type="button" className="admin-icon-btn" onClick={onCancel} aria-label="Cerrar formulario">
          <X size={20} aria-hidden="true" />
        </button>
      </div>
      {form}
    </section>
  )
}

interface CompetencyFormProps extends Shared {
  area: CurriculumArea
  /** Sin competencia se registra una nueva en el área. */
  competency?: CurriculumCompetency
}

export function CompetencyForm({ area, competency, onCancel, onSaved, onSessionLost }: CompetencyFormProps) {
  const [codigo, setCodigo] = useState(competency?.codigo ?? `${area.codigo}-`)
  const [numero, setNumero] = useState(competency?.numeroCneb ? String(competency.numeroCneb) : '')
  const [nombre, setNombre] = useState(competency?.nombre ?? '')
  const [descripcion, setDescripcion] = useState(competency?.descripcion ?? '')
  const [capacidades, setCapacidades] = useState(competency?.capacidades.map(item => item.nombre).join('\n') ?? '')
  const { busy, error, setError, run } = useSubmit(onSessionLost, 'No pudimos guardar la competencia.')

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const list = capacidades.split('\n').map(line => line.trim()).filter(Boolean)
    if (!nombre.trim() || list.length === 0) {
      setError('Escribe el nombre de la competencia y al menos una capacidad.')
      return
    }
    const changes = { numeroCneb: toNumber(numero), nombre, descripcion, capacidades: list }
    run(async () => {
      onSaved(competency
        ? await updateCompetency(competency.id, changes)
        : await createCompetency({ area: area.id, codigo: codigo.trim().toUpperCase(), ...changes }))
    })
  }

  const titleId = `admin-competency-form-${competency?.id ?? 'new'}`

  return (
    <form className="admin-form admin-competency-form" onSubmit={handleSubmit} noValidate aria-labelledby={titleId}>
      <h4 id={titleId}>{competency ? `Corregir ${competency.codigo}` : `Nueva competencia de ${area.nombre}`}</h4>
      <div className="admin-form-row">
        <label className="admin-field">
          <span>Código</span>
          <input
            value={codigo}
            onChange={event => setCodigo(event.target.value.toUpperCase())}
            maxLength={60}
            spellCheck={false}
            disabled={Boolean(competency)}
          />
        </label>
        <label className="admin-field">
          <span>N.° en el CNEB <em>(opcional)</em></span>
          <input type="number" inputMode="numeric" min={1} max={31} value={numero} onChange={event => setNumero(event.target.value)} />
        </label>
      </div>
      <label className="admin-field">
        <span>Nombre de la competencia</span>
        <input value={nombre} onChange={event => setNombre(event.target.value)} maxLength={255} />
      </label>
      <label className="admin-field">
        <span>Capacidades</span>
        <textarea
          rows={Math.min(10, Math.max(4, capacidades.split('\n').length + 1))}
          value={capacidades}
          onChange={event => setCapacidades(event.target.value)}
        />
        <small>Una por línea, en el orden del CNEB. Hasta ocho.</small>
      </label>
      <label className="admin-field">
        <span>Descripción <em>(opcional)</em></span>
        <textarea rows={2} value={descripcion} onChange={event => setDescripcion(event.target.value)} maxLength={2000} />
      </label>
      {error && <p className="admin-feedback admin-feedback--error" role="alert">{error}</p>}
      <div className="admin-actions">
        <button type="button" className="admin-btn admin-btn--quiet" onClick={onCancel} disabled={busy}>Cancelar</button>
        <button type="submit" className="admin-btn admin-btn--leaf" disabled={busy}>
          {busy ? 'Guardando…' : competency ? 'Guardar cambios' : 'Registrar competencia'}
        </button>
      </div>
    </form>
  )
}
