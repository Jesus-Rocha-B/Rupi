import { Award, BookOpen, Calculator, Check, Feather, Lock, Sparkles, Star, Volume2, X } from 'lucide-react'
import type { BadgeDto } from '../services/learningRoutesApi'
import { RupiCharacter } from './RupiCharacter'
import { speakText } from '../utils/speech'
import './StudentBadgesModal.css'

type Props = {
  studentName: string
  schoolName?: string
  gradeName?: string
  earnedBadges: BadgeDto[]
  totalXp?: number
  highlightBadge?: BadgeDto | null
  onClose: () => void
}

interface CatalogBadge {
  code: string
  name: string
  description: string
  icon: 'feather' | 'medal' | 'sparkles' | 'math' | 'book'
  xpBonus: number
}

const PRIMARY_BADGES_CATALOG: CatalogBadge[] = [
  {
    code: 'GRAN_TUNQUI_ORO',
    name: 'Gran Gallito de Oro',
    description: '¡Alcanzaste la cima de la ruta escolar! Volaste como el gallito de las rocas por toda la aventura.',
    icon: 'feather',
    xpBonus: 100,
  },
  {
    code: 'EXPLORADOR_AYACUCHO',
    name: 'Explorador de Ayacucho',
    description: 'Completaste tu primera parada escolar y superaste los desafíos de la lección.',
    icon: 'medal',
    xpBonus: 50,
  },
  {
    code: 'RETO_DIARIO',
    name: 'Guardián del Reto Diario',
    description: 'Resolviste el desafío del día y mantuviste tu práctica constante.',
    icon: 'sparkles',
    xpBonus: 30,
  },
  {
    code: 'RETABLO_MATEMATICO',
    name: 'Maestro del Retablo',
    description: 'Completaste ejercicios de sumas, conteo y patrones con figuras tradicionales.',
    icon: 'math',
    xpBonus: 40,
  },
  {
    code: 'PALABRAS_DEL_VALLE',
    name: 'Lector del Valle',
    description: 'Exploraste historias andinas, cuentos y palabras mágicas del Perú.',
    icon: 'book',
    xpBonus: 40,
  },
]

export function StudentBadgesModal({
  studentName,
  schoolName = 'I.E. 38001 Mariscal Sucre',
  gradeName = '2.° de primaria',
  earnedBadges = [],
  totalXp = 0,
  highlightBadge,
  onClose,
}: Props) {
  const earnedCodes = new Set(earnedBadges.map((b) => b.code))
  const grandBadgeEarned = earnedCodes.has('GRAN_TUNQUI_ORO') || highlightBadge?.code === 'GRAN_TUNQUI_ORO'

  // Si highlightBadge viene del servidor y no está en el catálogo, asegurarse de mostrarla
  const displayCatalog = [...PRIMARY_BADGES_CATALOG]
  for (const b of earnedBadges) {
    if (!displayCatalog.some((c) => c.code === b.code)) {
      displayCatalog.push({
        code: b.code,
        name: b.name,
        description: b.description,
        icon: b.icon === 'feather' ? 'feather' : 'medal',
        xpBonus: 50,
      })
    }
  }

  const unlockedCount = displayCatalog.filter((b) => earnedCodes.has(b.code)).length

  function handleListenSummary() {
    const speech = `Vitrina de insignias de ${studentName}. Has ganado ${unlockedCount} de ${displayCatalog.length} insignias escolares y tienes ${totalXp} puntos de experiencia acumulados. ¡Sigue aprendiendo con entusiasmo!`
    speakText(speech)
  }

  function handleBadgeClick(badge: CatalogBadge, isEarned: boolean) {
    const text = isEarned
      ? `Insignia ganada: ${badge.name}. ${badge.description}`
      : `Insignia por descubrir: ${badge.name}. ${badge.description} Completa más lecciones para desbloquearla.`
    speakText(text)
  }

  return (
    <div className="badges-modal-overlay" role="dialog" aria-modal="true" aria-labelledby="badges-modal-title">
      <div className="badges-modal-container">
        {/* Cabecera con datos del estudiante */}
        <header className="badges-modal-header">
          <div className="badges-header-title-group">
            <div className="badges-header-avatar" aria-hidden="true">
              {studentName ? studentName.charAt(0).toUpperCase() : 'E'}
            </div>
            <div className="badges-header-text">
              <h2 id="badges-modal-title">Mis Insignias de Perfil</h2>
              <p className="badges-header-sub">
                {studentName} · {gradeName} · {schoolName}
              </p>
            </div>
          </div>

          <div className="badges-header-actions">
            <button
              type="button"
              className="badges-action-btn"
              onClick={handleListenSummary}
              aria-label="Escuchar resumen de insignias con Rupi"
              title="Escuchar a Rupi"
            >
              <Volume2 size={20} aria-hidden="true" />
            </button>
            <button
              type="button"
              className="badges-close-btn"
              onClick={onClose}
              aria-label="Cerrar vitrina de insignias"
              title="Cerrar"
            >
              <X size={20} aria-hidden="true" />
            </button>
          </div>
        </header>

        {/* Cuerpo de la vitrina */}
        <main className="badges-modal-body">
          {/* Banner de estadísticas */}
          <div className="badges-summary-banner" role="region" aria-label="Estadísticas de insignias">
            <div className="badges-stats-group">
              <span className="badges-stat-pill">
                <Award size={18} className="badges-stat-icon" aria-hidden="true" />
                <span>
                  {unlockedCount} de {displayCatalog.length} insignias
                </span>
              </span>
              <span className="badges-stat-pill xp">
                <Star size={18} className="badges-stat-icon" aria-hidden="true" />
                <span>{totalXp} XP Acumulados</span>
              </span>
            </div>
          </div>

          {/* Gran Insignia de Culminación de Ruta (Hero Badge) si está desbloqueada */}
          {grandBadgeEarned && (
            <section className="hero-badge-card" aria-label="Gran Insignia de Oro">
              <div className="hero-badge-icon-box">
                <Feather size={34} aria-hidden="true" />
              </div>
              <div className="hero-badge-info">
                <h3>Gran Gallito de Oro</h3>
                <p>
                  ¡Completaste todas las paradas de tu ruta escolar! Volaste como el gallito de las rocas superando
                  cada desafío con perseverancia.
                </p>
                <span className="hero-badge-tag">
                  <Sparkles size={14} aria-hidden="true" />
                  +100 XP Bono de Culminación
                </span>
              </div>
            </section>
          )}

          {/* Cuadrícula de insignias de la colección */}
          <div className="badges-grid-title">
            <Award size={16} aria-hidden="true" />
            <span>Colección de Insignias Escolares</span>
          </div>

          <div className="badges-collection-grid" role="list">
            {displayCatalog.map((b) => {
              const isEarned = earnedCodes.has(b.code)
              return (
                <article
                  key={b.code}
                  className={`badge-item-card ${isEarned ? 'unlocked' : 'locked'}`}
                  aria-label={`${b.name}: ${isEarned ? 'Insignia obtenida' : 'Insignia por descubrir'}`}
                >
                  <div className="badge-item-icon-box" aria-hidden="true">
                    {b.icon === 'feather' ? (
                      <Feather size={22} />
                    ) : b.icon === 'math' ? (
                      <Calculator size={22} />
                    ) : b.icon === 'book' ? (
                      <BookOpen size={22} />
                    ) : b.icon === 'sparkles' ? (
                      <Sparkles size={22} />
                    ) : (
                      <Award size={22} />
                    )}
                  </div>
                  <div className="badge-item-details">
                    <div className="badge-title-row">
                      <h4 className="badge-item-name">{b.name}</h4>
                      <button
                        type="button"
                        className="badge-listen-btn"
                        onClick={() => handleBadgeClick(b, isEarned)}
                        title="Escuchar detalle"
                        aria-label={`Escuchar detalle de ${b.name}`}
                      >
                        <Volume2 size={15} aria-hidden="true" />
                      </button>
                    </div>
                    <p className="badge-item-desc">{b.description}</p>
                    <span className={`badge-status-chip ${isEarned ? 'earned' : 'pending'}`}>
                      {isEarned ? (
                        <>
                          <Check size={12} aria-hidden="true" /> Obtenida (+{b.xpBonus} XP)
                        </>
                      ) : (
                        <>
                          <Lock size={12} aria-hidden="true" /> Por descubrir
                        </>
                      )}
                    </span>
                  </div>
                </article>
              )
            })}
          </div>
        </main>

        {/* Pie con Rupi animando */}
        <footer className="badges-modal-footer">
          <RupiCharacter mood="cheering" className="badges-footer-mascot" />
          <p className="badges-footer-text">
            ¡Cada insignia representa un nuevo paso en tu aprendizaje escolar! Sigue practicando en tu ruta para
            descubrir más medallas.
          </p>
        </footer>
      </div>
    </div>
  )
}
