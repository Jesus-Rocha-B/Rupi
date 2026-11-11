import { useEffect, useRef, useState } from 'react'
import { BookOpen, Pencil, Plus } from 'lucide-react'
import {
  AdminApiError,
  fetchCurriculumCatalog,
  type Administrator,
  type CatalogState,
  type CurriculumArea,
  type CurriculumCatalog,
} from '../../services/adminApi'
import { SESSION_LOST } from './accountStatus'
import { AreaForm, CompetencyForm } from './CurriculumForms'

interface Props {
  admin: Administrator
  onSessionLost: (message: string) => void
}

type Editing =
  | { kind: 'none' }
  | { kind: 'new-area' }
  | { kind: 'area' }
  | { kind: 'new-competency' }
  | { kind: 'competency'; id: string }

const STATE_TEXT: Record<CatalogState, string> = {
  ACTIVO: 'Vigente',
  BORRADOR: 'Borrador',
  RETIRADO: 'Retirada',
}

const STATE_TONE: Record<CatalogState, string> = {
  ACTIVO: 'ok',
  BORRADOR: 'warn',
  RETIRADO: 'off',
}

function plural(count: number, one: string, many: string) {
  return `${count} ${count === 1 ? one : many}`
}

/** Catálogo general del MINEDU (HU-49): grados, áreas en el orden del programa y competencias del CNEB. */
export function CurriculumView({ admin, onSessionLost }: Props) {
  const [versionId, setVersionId] = useState<string | null>(null)
  const [catalog, setCatalog] = useState<CurriculumCatalog | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [areaId, setAreaId] = useState<string | null>(null)
  const [editing, setEditing] = useState<Editing>({ kind: 'none' })
  const canManage = admin.permisos.includes('curriculo.gestionar')

  useEffect(() => {
    const controller = new AbortController()
    fetchCurriculumCatalog(versionId, controller.signal)
      .then(found => {
        setCatalog(found)
        setError(null)
      })
      .catch(failure => {
        if (controller.signal.aborted) return
        if (failure instanceof AdminApiError && failure.status === 401) onSessionLost(failure.message || SESSION_LOST)
        else setError(failure instanceof AdminApiError ? failure.message : 'No pudimos cargar el catálogo curricular.')
      })
    return () => controller.abort()
  }, [versionId, onSessionLost])

  const area = catalog?.areas.find(item => item.id === areaId) ?? null
  const writable = canManage && Boolean(catalog?.editable)

  // En pantallas angostas el panel queda bajo la lista: se lleva la vista hasta él al abrirlo.
  const panelRef = useRef<HTMLDivElement>(null)
  const panelKey = editing.kind === 'new-area' ? 'new-area' : areaId
  useEffect(() => {
    if (!panelKey || !window.matchMedia('(max-width: 900px)').matches) return
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    panelRef.current?.scrollIntoView({ behavior: reduce ? 'auto' : 'smooth', block: 'start' })
  }, [panelKey])

  function handleSaved(updated: CurriculumCatalog, selectAreaCode?: string) {
    setCatalog(updated)
    setEditing({ kind: 'none' })
    if (selectAreaCode) setAreaId(updated.areas.find(item => item.codigo === selectAreaCode)?.id ?? null)
  }

  function openArea(next: CurriculumArea) {
    setAreaId(next.id)
    setEditing({ kind: 'none' })
  }

  const version = catalog?.version ?? null

  return (
    <div className="admin-accounts admin-curriculum">
      <section className="admin-card" aria-labelledby="admin-curriculum-title">
        <div className="admin-section-head">
          <div>
            <h1 id="admin-curriculum-title">Catálogo curricular</h1>
            <p className="admin-muted">
              Grados, áreas y competencias del Currículo Nacional. Las rutas de Rupi siguen este orden, igual que
              las clases del colegio.
            </p>
          </div>
          {writable && (
            <button
              type="button"
              className="admin-btn admin-btn--primary"
              onClick={() => {
                setAreaId(null)
                setEditing({ kind: 'new-area' })
              }}
            >
              <Plus size={18} aria-hidden="true" />
              Nueva área
            </button>
          )}
        </div>

        {error && <p className="admin-feedback admin-feedback--error" role="alert">{error}</p>}
        {!catalog && !error && <p className="admin-loading">Abriendo el catálogo…</p>}
        {catalog && !version && (
          <p className="admin-empty-line">Todavía no hay una versión del catálogo cargada en la base.</p>
        )}

        {catalog && version && (
          <>
            <div className="admin-curriculum-version">
              {catalog.versiones.length > 1 ? (
                <label className="admin-field">
                  <span>Versión del catálogo</span>
                  <select
                    value={version.id}
                    onChange={event => {
                      setVersionId(event.target.value)
                      setAreaId(null)
                      setEditing({ kind: 'none' })
                    }}
                  >
                    {catalog.versiones.map(item => (
                      <option key={item.id} value={item.id}>{item.etiqueta} · {STATE_TEXT[item.estado]}</option>
                    ))}
                  </select>
                </label>
              ) : (
                <p className="admin-curriculum-label">
                  <BookOpen size={20} aria-hidden="true" />
                  <strong>{version.etiqueta}</strong>
                </p>
              )}
              <span className={`admin-chip admin-chip--${STATE_TONE[version.estado]}`}>{STATE_TEXT[version.estado]}</span>
              {version.referencia && <p className="admin-muted admin-curriculum-source">{version.referencia}</p>}
            </div>
            {!catalog.editable && (
              <p className="admin-feedback admin-feedback--info">
                Esta versión está retirada: se consulta como registro y ya no se corrige.
              </p>
            )}

            <ul className="admin-curriculum-totals" aria-label="Resumen del catálogo">
              <li><strong>{catalog.resumen.grados}</strong> grados</li>
              <li><strong>{catalog.resumen.areas}</strong> áreas</li>
              <li><strong>{catalog.resumen.competencias}</strong> competencias</li>
              <li><strong>{catalog.resumen.capacidades}</strong> capacidades</li>
            </ul>

            <h2 className="admin-curriculum-subtitle">Grados de primaria</h2>
            <ul className="admin-chips admin-curriculum-grades">
              {catalog.grados.map(grade => (
                <li key={grade.id} className="admin-chip admin-chip--neutral">{grade.nombre}</li>
              ))}
            </ul>

            <h2 className="admin-curriculum-subtitle">Áreas curriculares</h2>
            <ul className="admin-account-rows">
              {catalog.areas.map(item => (
                <li key={item.id}>
                  <button
                    type="button"
                    className={`admin-account-row admin-area-row${areaId === item.id ? ' is-selected' : ''}`}
                    aria-pressed={areaId === item.id}
                    onClick={() => openArea(item)}
                  >
                    <span className="admin-area-seal" aria-hidden="true">{item.orden ?? '·'}</span>
                    <span className="admin-account-name">
                      <strong>{item.nombre}</strong>
                      <span>{item.codigo}</span>
                    </span>
                    <span className="admin-chips">
                      <span className={`admin-chip admin-chip--${item.competencias.length ? 'ok' : 'warn'}`}>
                        {plural(item.competencias.length, 'competencia', 'competencias')}
                      </span>
                      <span className="admin-chip admin-chip--off">{plural(item.unidades, 'unidad', 'unidades')}</span>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
            {catalog.areas.length === 0 && <p className="admin-empty-line">Esta versión todavía no tiene áreas.</p>}
          </>
        )}
      </section>

      <div ref={panelRef} className="admin-panel-slot">
        {editing.kind === 'new-area' && version && (
          <AreaForm
            versionId={version.id}
            onCancel={() => setEditing({ kind: 'none' })}
            onSaved={handleSaved}
            onSessionLost={onSessionLost}
          />
        )}
        {area && (
          <section className="admin-card admin-panel" aria-labelledby="admin-area-title">
            <div className="admin-panel-head">
              <div>
                <h2 id="admin-area-title">{area.nombre}</h2>
                <p className="admin-muted">
                  Área {area.orden ?? 'sin orden'} · código <code>{area.codigo}</code> ·{' '}
                  {plural(area.unidades, 'unidad curricular', 'unidades curriculares')}
                </p>
              </div>
              {writable && editing.kind !== 'area' && (
                <button
                  type="button"
                  className="admin-icon-btn"
                  aria-label={`Corregir el área ${area.nombre}`}
                  onClick={() => setEditing({ kind: 'area' })}
                >
                  <Pencil size={18} aria-hidden="true" />
                </button>
              )}
            </div>

            {editing.kind === 'area' && (
              <AreaForm
                versionId={version?.id ?? ''}
                area={area}
                onCancel={() => setEditing({ kind: 'none' })}
                onSaved={handleSaved}
                onSessionLost={onSessionLost}
              />
            )}

            <div className="admin-panel-block">
              <div className="admin-block-head">
                <h3>Competencias</h3>
                {writable && editing.kind !== 'new-competency' && (
                  <button
                    type="button"
                    className="admin-btn admin-btn--quiet"
                    onClick={() => setEditing({ kind: 'new-competency' })}
                  >
                    <Plus size={18} aria-hidden="true" />
                    Agregar
                  </button>
                )}
              </div>

              {editing.kind === 'new-competency' && (
                <CompetencyForm
                  area={area}
                  onCancel={() => setEditing({ kind: 'none' })}
                  onSaved={handleSaved}
                  onSessionLost={onSessionLost}
                />
              )}

              {area.competencias.length === 0 && editing.kind !== 'new-competency' && (
                <p className="admin-empty-line">Esta área todavía no tiene competencias registradas.</p>
              )}

              <ol className="admin-competency-list">
                {area.competencias.map(competency => (
                  <li key={competency.id} className="admin-competency">
                    {editing.kind === 'competency' && editing.id === competency.id ? (
                      <CompetencyForm
                        area={area}
                        competency={competency}
                        onCancel={() => setEditing({ kind: 'none' })}
                        onSaved={handleSaved}
                        onSessionLost={onSessionLost}
                      />
                    ) : (
                      <>
                        <div className="admin-competency-head">
                          <span
                            className="admin-cneb-seal"
                            title="Número de la competencia en el CNEB"
                            aria-label={
                              competency.numeroCneb
                                ? `Competencia número ${competency.numeroCneb} del CNEB`
                                : 'Sin número CNEB asignado'
                            }
                          >
                            {competency.numeroCneb ?? '—'}
                          </span>
                          <div className="admin-competency-title">
                            <h4>{competency.nombre}</h4>
                            <p>
                              <code>{competency.codigo}</code> · {plural(competency.unidades, 'unidad', 'unidades')}
                            </p>
                          </div>
                          {writable && (
                            <button
                              type="button"
                              className="admin-icon-btn"
                              aria-label={`Corregir ${competency.codigo}`}
                              onClick={() => setEditing({ kind: 'competency', id: competency.id })}
                            >
                              <Pencil size={18} aria-hidden="true" />
                            </button>
                          )}
                        </div>
                        {competency.capacidades.length > 0 ? (
                          <ol className="admin-capacity-list" aria-label={`Capacidades de ${competency.codigo}`}>
                            {competency.capacidades.map(capacity => <li key={capacity.id}>{capacity.nombre}</li>)}
                          </ol>
                        ) : (
                          <p className="admin-muted">Sin capacidades registradas.</p>
                        )}
                      </>
                    )}
                  </li>
                ))}
              </ol>
            </div>
          </section>
        )}
      </div>
    </div>
  )
}
