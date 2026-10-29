import { useEffect, useRef, useState } from 'react'
import { ChevronLeft, ChevronRight, Search, UserPlus } from 'lucide-react'
import {
  AdminApiError,
  ROLE_LABELS,
  displayName,
  fetchAccounts,
  type AccountDetail,
  type AccountPage,
  type Administrator,
} from '../../services/adminApi'
import { AccountPanel } from './AccountPanel'
import { NewAccountForm } from './NewAccountForm'
import { SESSION_LOST, stateLabel, stateTone } from './accountStatus'

interface Props {
  admin: Administrator
  onSessionLost: (message: string) => void
}

type Panel = { kind: 'none' } | { kind: 'new' } | { kind: 'account'; id: string; created?: AccountDetail }

export function AccountsView({ admin, onSessionLost }: Props) {
  const [search, setSearch] = useState('')
  const [query, setQuery] = useState('')
  const [role, setRole] = useState('')
  const [state, setState] = useState('')
  const [pageNumber, setPageNumber] = useState(1)
  // Cada respuesta guarda la consulta que la produjo: «cargando» es que la vigente aún no llegó.
  const [result, setResult] = useState<{ key: string; page: AccountPage | null; error: string | null } | null>(null)
  const [panel, setPanel] = useState<Panel>({ kind: 'none' })
  const [reloadKey, setReloadKey] = useState(0)
  const canManage = admin.permisos.includes('cuentas.gestionar')

  // La búsqueda espera a que se deje de escribir para no consultar la base en cada tecla.
  useEffect(() => {
    const timer = setTimeout(() => {
      setQuery(search)
      setPageNumber(1)
    }, 300)
    return () => clearTimeout(timer)
  }, [search])

  const requestKey = JSON.stringify([query, role, state, pageNumber, reloadKey])

  useEffect(() => {
    const controller = new AbortController()
    fetchAccounts({ q: query, rol: role, estado: state, pagina: pageNumber }, controller.signal)
      .then(found => setResult({ key: requestKey, page: found, error: null }))
      .catch(failure => {
        if (controller.signal.aborted) return
        if (failure instanceof AdminApiError && failure.status === 401) onSessionLost(SESSION_LOST)
        setResult(previous => ({
          key: requestKey,
          page: previous?.page ?? null,
          error: failure instanceof AdminApiError ? failure.message : 'No pudimos cargar las cuentas.',
        }))
      })
    return () => controller.abort()
  }, [requestKey, query, role, state, pageNumber, onSessionLost])

  const loading = result?.key !== requestKey
  const page = result?.page ?? null
  const error = result?.key === requestKey ? result.error : null

  // En pantallas angostas el panel queda bajo la lista: se lleva la vista hasta él al abrirlo.
  const panelRef = useRef<HTMLDivElement>(null)
  const panelKey = panel.kind === 'account' ? panel.id : panel.kind
  useEffect(() => {
    if (panelKey === 'none' || !window.matchMedia('(max-width: 900px)').matches) return
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    panelRef.current?.scrollIntoView({ behavior: reduce ? 'auto' : 'smooth', block: 'start' })
  }, [panelKey])

  const totalPages = page ? Math.max(1, Math.ceil(page.total / page.porPagina)) : 1
  const selectedId = panel.kind === 'account' ? panel.id : null

  function handleChanged() {
    setReloadKey(value => value + 1)
  }

  return (
    <div className="admin-accounts">
      <section className="admin-card admin-accounts-list" aria-labelledby="admin-accounts-title">
        <div className="admin-section-head">
          <div>
            <h1 id="admin-accounts-title">Cuentas</h1>
            <p className="admin-muted">Docentes y estudiantes de la plataforma. Ninguna cuenta se borra: se da de baja.</p>
          </div>
          {canManage && (
            <button type="button" className="admin-btn admin-btn--primary" onClick={() => setPanel({ kind: 'new' })}>
              <UserPlus size={18} aria-hidden="true" />
              Nueva cuenta
            </button>
          )}
        </div>

        <div className="admin-filters" role="search">
          <label className="admin-field admin-field--search">
            <span>Buscar</span>
            <span className="admin-input-icon">
              <Search size={18} aria-hidden="true" />
              <input
                type="search"
                value={search}
                placeholder="Nombre, usuario o correo"
                onChange={event => setSearch(event.target.value)}
              />
            </span>
          </label>
          <label className="admin-field">
            <span>Rol</span>
            <select value={role} onChange={event => { setRole(event.target.value); setPageNumber(1) }}>
              <option value="">Todos</option>
              {Object.entries(ROLE_LABELS).map(([code, label]) => <option key={code} value={code}>{label}</option>)}
            </select>
          </label>
          <label className="admin-field">
            <span>Estado</span>
            <select value={state} onChange={event => { setState(event.target.value); setPageNumber(1) }}>
              <option value="">Todos</option>
              <option value="ACTIVO">Activas</option>
              <option value="BLOQUEADO">Bloqueadas</option>
              <option value="DESHABILITADO">De baja</option>
            </select>
          </label>
        </div>

        {error && <p className="admin-feedback admin-feedback--error" role="alert">{error}</p>}

        <p className="admin-count" aria-live="polite">
          {loading ? 'Buscando…' : page ? `${page.total} ${page.total === 1 ? 'cuenta' : 'cuentas'}` : ''}
        </p>

        <ul className="admin-account-rows">
          {page?.items.map(account => (
            <li key={account.id}>
              <button
                type="button"
                className={`admin-account-row${selectedId === account.id ? ' is-selected' : ''}`}
                aria-pressed={selectedId === account.id}
                onClick={() => setPanel({ kind: 'account', id: account.id })}
              >
                <span className="admin-account-name">
                  <strong>{displayName(account)}</strong>
                  <span>@{account.usuario}</span>
                </span>
                <span className="admin-chips">
                  {account.roles.map(code => (
                    <span key={code} className={`admin-chip admin-chip--role-${code.toLowerCase()}`}>
                      {ROLE_LABELS[code] ?? code}
                    </span>
                  ))}
                  <span className={`admin-chip admin-chip--${stateTone(account)}`}>{stateLabel(account)}</span>
                </span>
              </button>
            </li>
          ))}
        </ul>

        {!loading && page && page.items.length === 0 && (
          <p className="admin-empty-line">No hay cuentas con esos filtros.</p>
        )}

        {totalPages > 1 && (
          <div className="admin-pager">
            <button
              type="button"
              className="admin-btn admin-btn--quiet"
              disabled={pageNumber <= 1 || loading}
              onClick={() => setPageNumber(value => value - 1)}
            >
              <ChevronLeft size={18} aria-hidden="true" />
              Anterior
            </button>
            <span>Página {pageNumber} de {totalPages}</span>
            <button
              type="button"
              className="admin-btn admin-btn--quiet"
              disabled={pageNumber >= totalPages || loading}
              onClick={() => setPageNumber(value => value + 1)}
            >
              Siguiente
              <ChevronRight size={18} aria-hidden="true" />
            </button>
          </div>
        )}
      </section>

      <div ref={panelRef} className="admin-panel-slot">
        {panel.kind === 'new' && (
          <NewAccountForm
            onCancel={() => setPanel({ kind: 'none' })}
            onCreated={account => {
              setPanel({ kind: 'account', id: account.id, created: account })
              handleChanged()
            }}
            onSessionLost={onSessionLost}
          />
        )}
        {panel.kind === 'account' && (
          <AccountPanel
            key={panel.id}
            accountId={panel.id}
            created={panel.created}
            admin={admin}
            onClose={() => setPanel({ kind: 'none' })}
            onChanged={handleChanged}
            onSessionLost={onSessionLost}
          />
        )}
      </div>
    </div>
  )
}
