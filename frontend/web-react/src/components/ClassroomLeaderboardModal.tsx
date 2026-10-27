import React, { useState, useEffect } from 'react'
import {
  X,
  Trophy,
  Flame,
  Star,
  Shield,
  Check,
  RotateCcw,
  UserCheck,
  Eye,
  EyeOff,
  Sparkles,
} from 'lucide-react'
import {
  fetchClassroomLeaderboard,
  updateLeaderboardPrivacy,
  type ClassroomLeaderboard,
  type PrivacySettings,
} from '../services/leaderboardApi'
import { RupiCharacter } from './RupiCharacter'
import './ClassroomLeaderboardModal.css'

interface Props {
  isOpen: boolean
  onClose: () => void
}

export const ClassroomLeaderboardModal: React.FC<Props> = ({ isOpen, onClose }) => {
  const [sortBy, setSortBy] = useState<'XP' | 'RACHA'>('XP')
  const [data, setData] = useState<ClassroomLeaderboard | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Sub-panel de opciones de privacidad
  const [showPrivacyPanel, setShowPrivacyPanel] = useState(false)
  const [privacyDraft, setPrivacyDraft] = useState<PrivacySettings>({
    mostrarEnClasificacion: true,
    aliasPublico: '',
    mostrarComo: 'ALIAS',
  })
  const [privacySaving, setPrivacySaving] = useState(false)
  const [privacyMsg, setPrivacyMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null)

  const refreshLeaderboard = (criteria: 'XP' | 'RACHA') => {
    fetchClassroomLeaderboard(criteria)
      .then((board) => {
        setData(board)
        setPrivacyDraft(board.myPrivacy)
      })
      .catch((err: unknown) => {
        if (err instanceof Error) {
          setError(err.message)
        }
      })
  }

  useEffect(() => {
    if (!isOpen) return
    const controller = new AbortController()
    fetchClassroomLeaderboard(sortBy, controller.signal)
      .then((board) => {
        setData(board)
        setPrivacyDraft(board.myPrivacy)
      })
      .catch((err: unknown) => {
        if (err instanceof Error && err.name !== 'AbortError') {
          setError(err.message || 'Error al cargar la tabla de logros.')
        }
      })
      .finally(() => {
        setLoading(false)
      })
    return () => controller.abort()
  }, [isOpen, sortBy])

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) onClose()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  if (!isOpen) return null

  const handleSavePrivacy = async (e: React.FormEvent) => {
    e.preventDefault()
    setPrivacySaving(true)
    setPrivacyMsg(null)
    try {
      const updated = await updateLeaderboardPrivacy(privacyDraft)
      setPrivacyDraft(updated)
      setPrivacyMsg({ type: 'success', text: '¡Tus opciones de privacidad se guardaron correctamente!' })
      // Recargar leaderboard con la nueva preferencia
      refreshLeaderboard(sortBy)
    } catch (err: unknown) {
      setPrivacyMsg({
        type: 'error',
        text: err instanceof Error ? err.message : 'No se pudo guardar la privacidad.',
      })
    } finally {
      setPrivacySaving(false)
    }
  }

  const entries = data?.entries || []
  const top1 = entries[0]
  const top2 = entries[1]
  const top3 = entries[2]
  const restEntries = entries.slice(3)

  return (
    <div
      className="leaderboard-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="leaderboard-title"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div className="leaderboard-modal-box">
        {/* Cabecera */}
        <header className="leaderboard-header">
          <div className="leaderboard-title-group">
            <span className="leaderboard-icon-badge" aria-hidden="true">
              <Trophy size={24} />
            </span>
            <div>
              <h2 id="leaderboard-title" className="leaderboard-title">
                Tabla de Logros del Aula
              </h2>
              <p className="leaderboard-subtitle">
                {data?.classroomName || 'Tu aula escolar'} · Superación y constancia sana con tus compañeros
              </p>
            </div>
          </div>

          <div className="leaderboard-header-right">
            {data && data.myRank > 0 && (
              <div className="leaderboard-my-rank-pill" title="Tu posición en el aula">
                <Sparkles size={16} aria-hidden="true" />
                <span>Puesto <strong>#{data.myRank}</strong> de {data.totalStudents}</span>
              </div>
            )}

            <button
              type="button"
              className={`leaderboard-privacy-toggle-btn ${showPrivacyPanel ? 'active' : ''}`}
              onClick={() => setShowPrivacyPanel(!showPrivacyPanel)}
              title="Configurar privacidad y seudónimo"
              aria-label="Configurar privacidad y seudónimo"
            >
              <Shield size={16} aria-hidden="true" />
              <span>Privacidad</span>
            </button>

            <button
              type="button"
              className="leaderboard-close-btn"
              onClick={onClose}
              title="Cerrar tabla de logros (Esc)"
              aria-label="Cerrar tabla de logros"
            >
              <X size={20} />
            </button>
          </div>
        </header>

        {/* Panel Desplegable de Privacidad Escolar */}
        {showPrivacyPanel && (
          <section className="leaderboard-privacy-panel" aria-label="Ajustes de privacidad y seudónimo">
            <form onSubmit={handleSavePrivacy} className="privacy-form">
              <div className="privacy-header-row">
                <div className="privacy-title-wrap">
                  <Shield size={18} className="shield-icon" aria-hidden="true" />
                  <strong>Privacidad y Nombre Escolar</strong>
                </div>
                <small className="privacy-hint">
                  En RUPI cuidamos tu seguridad. Puedes competir usando un seudónimo amigable o tu nombre de pila.
                </small>
              </div>

              {privacyMsg && (
                <div className={`privacy-alert-box ${privacyMsg.type}`} role="alert">
                  {privacyMsg.type === 'success' ? <Check size={16} /> : <X size={16} />}
                  <span>{privacyMsg.text}</span>
                </div>
              )}

              <div className="privacy-options-grid">
                {/* 1. Visibilidad en el ranking */}
                <label className="privacy-checkbox-label">
                  <input
                    type="checkbox"
                    checked={privacyDraft.mostrarEnClasificacion}
                    onChange={(e) =>
                      setPrivacyDraft((prev) => ({ ...prev, mostrarEnClasificacion: e.target.checked }))
                    }
                  />
                  <span>
                    {privacyDraft.mostrarEnClasificacion ? (
                      <span className="vis-tag visible"><Eye size={14} /> Visible en la tabla del aula</span>
                    ) : (
                      <span className="vis-tag hidden"><EyeOff size={14} /> Modo incógnito (no apareces en la lista de otros)</span>
                    )}
                  </span>
                </label>

                {/* 2. Nombre vs Seudónimo */}
                <div className="privacy-radio-group">
                  <span className="radio-group-title">¿Cómo quieres que te vean tus compañeros?</span>
                  <div className="radio-pills">
                    <label className={`radio-pill ${privacyDraft.mostrarComo === 'ALIAS' ? 'selected' : ''}`}>
                      <input
                        type="radio"
                        name="mostrarComo"
                        value="ALIAS"
                        checked={privacyDraft.mostrarComo === 'ALIAS'}
                        onChange={() => setPrivacyDraft((prev) => ({ ...prev, mostrarComo: 'ALIAS' }))}
                      />
                      <span>Usar mi Seudónimo (Recomendado)</span>
                    </label>

                    <label className={`radio-pill ${privacyDraft.mostrarComo === 'NOMBRE' ? 'selected' : ''}`}>
                      <input
                        type="radio"
                        name="mostrarComo"
                        value="NOMBRE"
                        checked={privacyDraft.mostrarComo === 'NOMBRE'}
                        onChange={() => setPrivacyDraft((prev) => ({ ...prev, mostrarComo: 'NOMBRE' }))}
                      />
                      <span>Usar mi Nombre escolar</span>
                    </label>
                  </div>
                </div>

                {/* 3. Input de Seudónimo */}
                <div className="privacy-alias-row">
                  <label htmlFor="input-alias" className="alias-label">
                    Tu seudónimo escolar:
                  </label>
                  <div className="alias-input-wrapper">
                    <input
                      id="input-alias"
                      type="text"
                      maxLength={30}
                      value={privacyDraft.aliasPublico || ''}
                      onChange={(e) =>
                        setPrivacyDraft((prev) => ({ ...prev, aliasPublico: e.target.value }))
                      }
                      placeholder="Ej. chaski_3a, puma_lector"
                      className="privacy-alias-input"
                    />
                    <button
                      type="submit"
                      disabled={privacySaving}
                      className="privacy-save-btn"
                    >
                      <Check size={16} aria-hidden="true" />
                      <span>{privacySaving ? 'Guardando…' : 'Guardar'}</span>
                    </button>
                  </div>
                </div>
              </div>
            </form>
          </section>
        )}

        {/* Mensaje de error general */}
        {error && (
          <div className="leaderboard-error-banner" role="alert">
            <span>{error}</span>
          </div>
        )}

        {/* Barra de Filtros y Orden */}
        <div className="leaderboard-controls-row">
          <div className="leaderboard-tabs" role="tablist" aria-label="Criterios de ordenación">
            <button
              type="button"
              role="tab"
              aria-selected={sortBy === 'XP'}
              className={`leaderboard-tab ${sortBy === 'XP' ? 'active' : ''}`}
              onClick={() => setSortBy('XP')}
            >
              <Star size={16} aria-hidden="true" />
              <span>Por Puntos XP</span>
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={sortBy === 'RACHA'}
              className={`leaderboard-tab ${sortBy === 'RACHA' ? 'active' : ''}`}
              onClick={() => setSortBy('RACHA')}
            >
              <Flame size={16} aria-hidden="true" />
              <span>Por Racha de Estudio</span>
            </button>
          </div>

          <div className="leaderboard-summary-chip">
            <span>{entries.length} estudiantes activos</span>
          </div>
        </div>

        {/* Contenido principal */}
        <div className="leaderboard-content-body">
          {loading ? (
            <div className="leaderboard-loading">
              <RotateCcw className="spinning-icon" size={24} />
              <span>Cargando tabla de logros del salón…</span>
            </div>
          ) : entries.length === 0 ? (
            <div className="leaderboard-empty">
              <p>Aún no hay compañeros en esta tabla. ¡Sé el primero en completar actividades hoy!</p>
            </div>
          ) : (
            <>
              {/* Podio de los 3 Primeros Puestos */}
              <section className="podium-section" aria-label="Podio de los tres primeros lugares">
                {/* 2.° Lugar (Plata) */}
                {top2 && (
                  <article className={`podium-card rank-2 ${top2.esUsuarioActual ? 'is-self' : ''}`}>
                    <div className="podium-medal-badge silver" aria-label="Segundo puesto">
                      <span>2.°</span>
                    </div>
                    <div className="podium-rupi-wrap">
                      <RupiCharacter
                        mood="cheering"
                        accessories={top2.equipped}
                        className="podium-rupi-char"
                      />
                    </div>
                    <h3 className="podium-name" title={top2.displayName}>
                      {top2.displayName}
                    </h3>
                    <div className="podium-stats-row">
                      <span className="stat-pill xp" title="Puntos de experiencia">
                        <Star size={13} fill="#ffd26a" stroke="#d49a24" />
                        <strong>{top2.totalXp}</strong> XP
                      </span>
                      <span className="stat-pill streak" title="Días consecutivos de estudio">
                        <Flame size={13} />
                        <strong>{top2.streakDays}</strong> d
                      </span>
                    </div>
                    {top2.esUsuarioActual && <span className="podium-self-tag"><UserCheck size={12} /> ¡Eres tú!</span>}
                  </article>
                )}

                {/* 1.° Lugar (Oro) - Centro y más grande */}
                {top1 && (
                  <article className={`podium-card rank-1 ${top1.esUsuarioActual ? 'is-self' : ''}`}>
                    <div className="podium-crown-kicker" aria-hidden="true">
                      <Trophy size={20} />
                    </div>
                    <div className="podium-medal-badge gold" aria-label="Primer puesto">
                      <span>1.°</span>
                    </div>
                    <div className="podium-rupi-wrap leader">
                      <RupiCharacter
                        mood="cheering"
                        accessories={top1.equipped}
                        className="podium-rupi-char leader"
                      />
                    </div>
                    <h3 className="podium-name leader" title={top1.displayName}>
                      {top1.displayName}
                    </h3>
                    <div className="podium-stats-row">
                      <span className="stat-pill xp" title="Puntos de experiencia">
                        <Star size={14} fill="#ffd26a" stroke="#d49a24" />
                        <strong>{top1.totalXp}</strong> XP
                      </span>
                      <span className="stat-pill streak" title="Días consecutivos de estudio">
                        <Flame size={14} />
                        <strong>{top1.streakDays}</strong> d
                      </span>
                    </div>
                    {top1.esUsuarioActual && <span className="podium-self-tag"><UserCheck size={12} /> ¡Eres tú!</span>}
                  </article>
                )}

                {/* 3.° Lugar (Bronce) */}
                {top3 && (
                  <article className={`podium-card rank-3 ${top3.esUsuarioActual ? 'is-self' : ''}`}>
                    <div className="podium-medal-badge bronze" aria-label="Tercer puesto">
                      <span>3.°</span>
                    </div>
                    <div className="podium-rupi-wrap">
                      <RupiCharacter
                        mood="cheering"
                        accessories={top3.equipped}
                        className="podium-rupi-char"
                      />
                    </div>
                    <h3 className="podium-name" title={top3.displayName}>
                      {top3.displayName}
                    </h3>
                    <div className="podium-stats-row">
                      <span className="stat-pill xp" title="Puntos de experiencia">
                        <Star size={13} fill="#ffd26a" stroke="#d49a24" />
                        <strong>{top3.totalXp}</strong> XP
                      </span>
                      <span className="stat-pill streak" title="Días consecutivos de estudio">
                        <Flame size={13} />
                        <strong>{top3.streakDays}</strong> d
                      </span>
                    </div>
                    {top3.esUsuarioActual && <span className="podium-self-tag"><UserCheck size={12} /> ¡Eres tú!</span>}
                  </article>
                )}
              </section>

              {/* Lista para el resto de posiciones (4.° en adelante) */}
              {restEntries.length > 0 && (
                <section className="leaderboard-table-section" aria-label="Posiciones del aula">
                  <div className="table-header-kicker">
                    <span>Posiciones siguientes</span>
                  </div>

                  <div className="leaderboard-rows-list" role="list">
                    {restEntries.map((item) => (
                      <div
                        key={item.studentId}
                        role="listitem"
                        className={`leaderboard-row-item ${item.esUsuarioActual ? 'is-current-user' : ''}`}
                      >
                        <div className="row-rank-col">
                          <span className="row-rank-number">#{item.rank}</span>
                        </div>

                        <div className="row-avatar-col">
                          <RupiCharacter
                            mood="idle"
                            accessories={item.equipped}
                            className="row-mini-rupi"
                          />
                        </div>

                        <div className="row-info-col">
                          <span className="row-display-name">
                            {item.displayName}
                            {item.esUsuarioActual && (
                              <span className="self-inline-chip">¡Tú!</span>
                            )}
                          </span>
                          {item.aliasPublico && item.displayName !== item.aliasPublico && (
                            <small className="row-alias-sub">@{item.aliasPublico}</small>
                          )}
                        </div>

                        <div className="row-stats-col">
                          <span className="row-stat-xp" title="Puntos XP">
                            <Star size={14} fill="#ffd26a" stroke="#d49a24" aria-hidden="true" />
                            <strong>{item.totalXp}</strong> XP
                          </span>
                          <span className="row-stat-streak" title="Racha de estudio">
                            <Flame size={14} aria-hidden="true" />
                            <strong>{item.streakDays}</strong> d
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                </section>
              )}
            </>
          )}
        </div>

        {/* Pie del modal */}
        <footer className="leaderboard-footer">
          <p className="leaderboard-footer-note">
            💡 <strong>Motivación en equipo:</strong> La tabla se actualiza automáticamente con cada lección y reto diario que completas.
          </p>
          <button type="button" className="leaderboard-done-btn" onClick={onClose}>
            <Check size={18} />
            <span>Volver a mis rutas</span>
          </button>
        </footer>
      </div>
    </div>
  )
}
