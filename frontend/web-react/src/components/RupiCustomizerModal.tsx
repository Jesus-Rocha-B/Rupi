import React, { useState, useEffect } from 'react'
import {
  X,
  Sparkles,
  Award,
  Check,
  Lock,
  Shirt,
  RotateCcw,
  Star,
} from 'lucide-react'
import {
  fetchWardrobe,
  unlockAccessory,
  equipAccessory,
  type AccessoryItem,
} from '../services/accessoriesApi'
import { RupiCharacter, type EquippedAccessories } from './RupiCharacter'
import './RupiCustomizerModal.css'

interface Props {
  isOpen: boolean
  onClose: () => void
  onEquippedChange?: (equipped: EquippedAccessories) => void
}

type FilterCategory = 'TODOS' | 'SOMBRERO' | 'VESTIMENTA' | 'FONDO'

export const RupiCustomizerModal: React.FC<Props> = ({
  isOpen,
  onClose,
  onEquippedChange,
}) => {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)
  const [totalXp, setTotalXp] = useState(0)
  const [items, setItems] = useState<AccessoryItem[]>([])
  const [filter, setFilter] = useState<FilterCategory>('TODOS')
  const [actionBusy, setActionBusy] = useState<string | null>(null)

  // Vista previa interactiva de accesorios
  const [previewAccessories, setPreviewAccessories] = useState<EquippedAccessories>({})

  const refreshWardrobe = async () => {
    try {
      const data = await fetchWardrobe()
      setTotalXp(data.totalXp)
      setItems(data.items)
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message)
      }
    }
  }

  useEffect(() => {
    if (!isOpen) return
    const controller = new AbortController()
    fetchWardrobe(controller.signal)
      .then((data) => {
        setTotalXp(data.totalXp)
        setItems(data.items)

        // Inicializar accesorios equipados guardados
        const initialEquipped: EquippedAccessories = {}
        data.items.forEach((item) => {
          if (item.equipado) {
            if (item.tipo === 'SOMBRERO') initialEquipped.sombrero = item.codigo
            if (item.tipo === 'VESTIMENTA') initialEquipped.vestimenta = item.codigo
            if (item.tipo === 'FONDO') initialEquipped.fondo = item.codigo
          }
        })
        setPreviewAccessories(initialEquipped)
        onEquippedChange?.(initialEquipped)
      })
      .catch((err: unknown) => {
        if (err instanceof Error && err.name !== 'AbortError') {
          setError(err.message || 'Error al cargar los accesorios.')
        }
      })
      .finally(() => {
        setLoading(false)
      })
    return () => controller.abort()
  }, [isOpen, onEquippedChange])

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) onClose()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  if (!isOpen) return null

  const handleUnlock = async (item: AccessoryItem) => {
    try {
      setActionBusy(item.codigo)
      setError(null)
      setSuccessMsg(null)
      await unlockAccessory(item.codigo)
      setSuccessMsg(`¡Desbloqueaste "${item.nombre}" con éxito!`)
      await refreshWardrobe()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Error al desbloquear accesorio.')
    } finally {
      setActionBusy(null)
    }
  }

  const handleToggleEquip = async (item: AccessoryItem) => {
    try {
      setActionBusy(item.codigo)
      setError(null)
      setSuccessMsg(null)
      const shouldEquip = !item.equipado
      await equipAccessory(item.codigo, shouldEquip)

      // Actualizar vista previa
      setPreviewAccessories((prev) => {
        const next = { ...prev }
        if (item.tipo === 'SOMBRERO') next.sombrero = shouldEquip ? item.codigo : null
        if (item.tipo === 'VESTIMENTA') next.vestimenta = shouldEquip ? item.codigo : null
        if (item.tipo === 'FONDO') next.fondo = shouldEquip ? item.codigo : null
        onEquippedChange?.(next)
        return next
      })

      await refreshWardrobe()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Error al equipar accesorio.')
    } finally {
      setActionBusy(null)
    }
  }

  const handlePreviewTryOn = (item: AccessoryItem) => {
    setPreviewAccessories((prev) => {
      const next = { ...prev }
      if (item.tipo === 'SOMBRERO') {
        next.sombrero = prev.sombrero === item.codigo ? null : item.codigo
      }
      if (item.tipo === 'VESTIMENTA') {
        next.vestimenta = prev.vestimenta === item.codigo ? null : item.codigo
      }
      if (item.tipo === 'FONDO') {
        next.fondo = prev.fondo === item.codigo ? null : item.codigo
      }
      return next
    })
  }

  const handleResetPreview = () => {
    // Restaurar a lo que realmente está equipado en BD
    const saved: EquippedAccessories = {}
    items.forEach((item) => {
      if (item.equipado) {
        if (item.tipo === 'SOMBRERO') saved.sombrero = item.codigo
        if (item.tipo === 'VESTIMENTA') saved.vestimenta = item.codigo
        if (item.tipo === 'FONDO') saved.fondo = item.codigo
      }
    })
    setPreviewAccessories(saved)
  }

  const filteredItems = items.filter((item) => {
    if (filter === 'TODOS') return true
    return item.tipo === filter
  })

  return (
    <div
      className="customizer-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="customizer-title"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div className="customizer-modal-box">
        {/* Encabezado */}
        <header className="customizer-header">
          <div className="customizer-title-group">
            <span className="customizer-icon-badge" aria-hidden="true">
              <Shirt size={22} />
            </span>
            <div>
              <h2 id="customizer-title" className="customizer-title">
                Tienda y Armario de Rupi
              </h2>
              <p className="customizer-subtitle">
                Usa tus puntos de experiencia (XP) para desbloquear accesorios y vestir a tu compañero.
              </p>
            </div>
          </div>

          <div className="customizer-header-right">
            <div className="customizer-xp-pill" title="Tus puntos de experiencia acumulados">
              <Star size={16} className="star-icon" fill="#ffd26a" stroke="#d49a24" aria-hidden="true" />
              <span><strong>{totalXp}</strong> XP</span>
            </div>
            <button
              type="button"
              className="customizer-close-btn"
              onClick={onClose}
              title="Cerrar tienda (Esc)"
              aria-label="Cerrar tienda de accesorios"
            >
              <X size={20} />
            </button>
          </div>
        </header>

        {/* Mensajes de estado */}
        {error && (
          <div className="customizer-banner error" role="alert">
            <span>{error}</span>
          </div>
        )}
        {successMsg && (
          <div className="customizer-banner success" role="status">
            <Check size={16} />
            <span>{successMsg}</span>
          </div>
        )}

        {/* Contenido principal dividido: Vista previa interactiva (izquierda) y Tienda (derecha) */}
        <div className="customizer-body">
          {/* Columna Izquierda: Probador en vivo de Rupi */}
          <aside className="customizer-preview-col">
            <div className="customizer-avatar-stage">
              <RupiCharacter
                mood="cheering"
                accessories={previewAccessories}
                className="customizer-stage-rupi"
              />
              <span className="customizer-stage-badge">Vista previa en vivo</span>
            </div>

            <div className="customizer-stage-info">
              <p className="stage-info-text">
                Toca cualquier accesorio para probárselo a Rupi antes de comprarlo o equiparlo.
              </p>
              <button
                type="button"
                className="stage-reset-btn"
                onClick={handleResetPreview}
                title="Restablecer probador a tus accesorios actualmente equipados"
              >
                <RotateCcw size={14} />
                <span>Restablecer probador</span>
              </button>
            </div>
          </aside>

          {/* Columna Derecha: Catálogo de Accesorios */}
          <section className="customizer-catalog-col">
            {/* Pestañas de categoría */}
            <div className="customizer-tabs" role="tablist" aria-label="Categorías de accesorios">
              <button
                type="button"
                role="tab"
                aria-selected={filter === 'TODOS'}
                className={`customizer-tab ${filter === 'TODOS' ? 'active' : ''}`}
                onClick={() => setFilter('TODOS')}
              >
                Todos ({items.length})
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={filter === 'SOMBRERO'}
                className={`customizer-tab ${filter === 'SOMBRERO' ? 'active' : ''}`}
                onClick={() => setFilter('SOMBRERO')}
              >
                Sombreros ({items.filter((i) => i.tipo === 'SOMBRERO').length})
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={filter === 'VESTIMENTA'}
                className={`customizer-tab ${filter === 'VESTIMENTA' ? 'active' : ''}`}
                onClick={() => setFilter('VESTIMENTA')}
              >
                Vestimentas ({items.filter((i) => i.tipo === 'VESTIMENTA').length})
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={filter === 'FONDO'}
                className={`customizer-tab ${filter === 'FONDO' ? 'active' : ''}`}
                onClick={() => setFilter('FONDO')}
              >
                Efectos ({items.filter((i) => i.tipo === 'FONDO').length})
              </button>
            </div>

            {/* Cuadrícula de tarjetas */}
            {loading ? (
              <div className="customizer-loading">
                <span>Cargando armario de accesorios…</span>
              </div>
            ) : (
              <div className="customizer-grid">
                {filteredItems.map((item) => {
                  const isBusy = actionBusy === item.codigo
                  const isEquipped = item.equipado
                  const isUnlocked = item.desbloqueado
                  const canAfford = totalXp >= item.costoExperiencia

                  return (
                    <article
                      key={item.codigo}
                      className={`accessory-card ${isEquipped ? 'is-equipped' : ''} ${!isUnlocked ? 'is-locked' : ''}`}
                      onClick={() => handlePreviewTryOn(item)}
                      title="Haz clic para probarlo en Rupi"
                    >
                      <div className="accessory-card-header">
                        <span className="accessory-type-badge">
                          {item.tipo === 'SOMBRERO' ? 'Sombrero' : item.tipo === 'VESTIMENTA' ? 'Ropa' : 'Efecto'}
                        </span>
                        <span className={`accessory-cost-badge ${item.costoExperiencia === 0 ? 'free' : ''}`}>
                          {item.costoExperiencia === 0 ? (
                            'Gratis'
                          ) : (
                            <>
                              <Star size={12} fill="#ffd26a" stroke="#d49a24" />
                              <span>{item.costoExperiencia} XP</span>
                            </>
                          )}
                        </span>
                      </div>

                      <div className="accessory-card-body">
                        <h4 className="accessory-card-title">{item.nombre}</h4>
                        <p className="accessory-card-desc">{item.descripcion}</p>
                      </div>

                      <div className="accessory-card-actions" onClick={(e) => e.stopPropagation()}>
                        {isEquipped ? (
                          <button
                            type="button"
                            className="acc-btn equipped"
                            disabled={isBusy}
                            onClick={() => handleToggleEquip(item)}
                          >
                            <Check size={14} />
                            <span>Equipado</span>
                          </button>
                        ) : isUnlocked ? (
                          <button
                            type="button"
                            className="acc-btn equip"
                            disabled={isBusy}
                            onClick={() => handleToggleEquip(item)}
                          >
                            <span>Equipar</span>
                          </button>
                        ) : canAfford ? (
                          <button
                            type="button"
                            className="acc-btn unlock"
                            disabled={isBusy}
                            onClick={() => handleUnlock(item)}
                          >
                            <Sparkles size={14} />
                            <span>Desbloquear</span>
                          </button>
                        ) : (
                          <button type="button" className="acc-btn locked" disabled>
                            <Lock size={13} />
                            <span>Faltan {item.costoExperiencia - totalXp} XP</span>
                          </button>
                        )}
                      </div>
                    </article>
                  )
                })}
              </div>
            )}
          </section>
        </div>

        {/* Pie del modal */}
        <footer className="customizer-footer">
          <div className="customizer-footer-tip">
            <Award size={16} className="tip-icon" />
            <span>
              <strong>Tip escolar:</strong> Cada parada completada en tus rutas didácticas te otorga <strong>+50 XP</strong>. ¡Supera tus retos diarios para desbloquear todos los atuendos!
            </span>
          </div>
          <button type="button" className="customizer-done-btn" onClick={onClose}>
            <Check size={18} />
            <span>Listo</span>
          </button>
        </footer>
      </div>
    </div>
  )
}
