import { useState } from 'react'
import {
  ArrowRight,
  Bird,
  BookOpen,
  BrainCircuit,
  Check,
  ChevronDown,
  Compass,
  Heart,
  House,
  Lightbulb,
  Sparkles,
  Star,
  Target,
  Trophy,
  X,
} from 'lucide-react'
import { RupiCharacter } from './components/RupiCharacter'
import { Roadmap } from './features/roadmap/Roadmap'
import { lessons, type Lesson } from './features/roadmap/lessons'
import './App.css'

const guideMessages = [
  '¡Ese reto de restas te espera! Puedes ganar 20 XP.',
  'Cada parada que completas te acerca al siguiente nivel.',
  '¿Listo para superar tu propio récord? ¡Tú marcas el ritmo!',
]

function App() {
  const [selectedLesson, setSelectedLesson] = useState<Lesson | null>(null)
  const [guideIndex, setGuideIndex] = useState(0)
  const [tutorHintOpen, setTutorHintOpen] = useState(false)

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#inicio" aria-label="Rupi, inicio">
          <img className="brand-logo" src="/rupi-logo.jpg" alt="" />
          <span>rupi<span className="brand-dot">.</span></span>
        </a>
        <div className="school-label">MI ESPACIO</div>
        <nav className="main-nav" aria-label="Navegación principal">
          <a className="nav-item selected" href="#ruta"><House aria-hidden="true" />Mi ruta</a>
          <a className="nav-item" href="#explorar"><Compass aria-hidden="true" />Explorar rutas</a>
          <a className="nav-item" href="#repaso"><BookOpen aria-hidden="true" />Repaso IA</a>
          <a className="nav-item" href="#tutor"><BrainCircuit aria-hidden="true" />Tutor IA</a>
          <a className="nav-item" href="#retos"><Target aria-hidden="true" />Retos</a>
          <a className="nav-item" href="#logros"><Trophy aria-hidden="true" />Logros</a>
        </nav>
        <div className="sidebar-bottom">
          <div className="help-bubble">
            ¿Necesitas ayuda?<br />
            <span>Rupi está aquí para ti.</span>
            <div className="help-face"><Bird aria-hidden="true" /></div>
          </div>
          <button className="profile-button" type="button">
            <span className="avatar small">A</span>
            <span><b>Alex</b><small>2.º grado</small></span>
            <span className="dots">···</span>
          </button>
        </div>
      </aside>

      <main className="main-content" id="ruta">
        <section className="course-heading" aria-labelledby="course-title">
          <div>
            <div className="eyebrow">TU RUTA DE APRENDIZAJE</div>
            <h1 id="course-title">Matemática <span className="grade-pill">2.º grado</span></h1>
            <p>Avanza a tu ritmo. ¡Tú puedes!</p>
          </div>
          <button className="term-button" type="button">Periodo 1 <ChevronDown aria-hidden="true" /></button>
        </section>

        <section className="progress-card" aria-label="Progreso del curso">
          <div className="progress-top">
            <div><Sparkles className="progress-star" aria-hidden="true" /><b>Tu aventura va tomando forma</b></div>
            <span className="progress-numbers">2 de 10 niveles <b>·</b> 20%</span>
          </div>
          <div className="progress-track"><div className="progress-fill" style={{ width: '20%' }} /></div>
        </section>

        <div className="learning-layout">
          <Roadmap
            guideMessage={guideMessages[guideIndex]}
            onGuideClick={() => setGuideIndex((index) => (index + 1) % guideMessages.length)}
            onSelectLesson={setSelectedLesson}
          />

          <aside className="right-column">
            <section className="continue-card" id="repaso" aria-labelledby="continue-title">
              <div className="continue-top"><Sparkles className="mini-spark" aria-hidden="true" /><span>CONTINÚA TU AVENTURA</span></div>
              <div className="continue-art">
                <span className="place-art-label">PORTALES Y CAMPANAS</span>
              </div>
              <div className="continue-body">
                <span className="lesson-tag">RESTA · 8 MIN</span>
                <h2 id="continue-title">La aventura de restar</h2>
                <p>Calcula cuánto te devuelven en un puesto de la feria.</p>
                <button className="start-button" type="button" onClick={() => setSelectedLesson(lessons[2])}>
                  Continuar aprendiendo <ArrowRight aria-hidden="true" />
                </button>
              </div>
            </section>

            <section className="mascot-card" id="tutor" aria-labelledby="tutor-title">
              <div className="mascot-copy">
                <span className="eyebrow" id="tutor-title">RUPI, TU TUTOR</span>
                <p>¿Te atascaste? Pide una pista y sigue intentando.</p>
                <button className="hint-button" type="button" aria-expanded={tutorHintOpen} onClick={() => setTutorHintOpen((open) => !open)}>
                  <Lightbulb aria-hidden="true" />{tutorHintOpen ? 'Ocultar pista' : 'Pedir una pista'}
                </button>
                {tutorHintOpen && <div className="hint-panel" role="status">Prueba separar las cantidades en decenas y unidades. ¿Qué monedas podrías juntar?</div>}
              </div>
              <RupiCharacter className="mascot" />
            </section>
            <div className="curriculum-note"><Sparkles aria-hidden="true" />Ruta alineada al currículo escolar</div>
          </aside>
        </div>

        <footer className="page-footer">Hecho para aprender, crecer y descubrir <Heart aria-label="con cariño" /></footer>
      </main>

      {selectedLesson && (
        <div className="modal-backdrop" role="presentation" onClick={() => setSelectedLesson(null)}>
          <section className="lesson-modal" role="dialog" aria-modal="true" aria-labelledby="modal-title" onClick={(event) => event.stopPropagation()}>
            <button className="modal-close" type="button" onClick={() => setSelectedLesson(null)} aria-label="Cerrar"><X aria-hidden="true" /></button>
            <div className="modal-icon">{selectedLesson.state === 'complete' ? <Check aria-hidden="true" /> : <Star aria-hidden="true" />}</div>
            <span className="eyebrow">{selectedLesson.activity.toUpperCase()}</span>
            <h2 id="modal-title">{selectedLesson.title}</h2>
            <p>{selectedLesson.state === 'complete' ? '¡Ya completaste esta actividad! Puedes volver a explorarla cuando quieras.' : '¡Es hora de aprender! En esta actividad practicarás con pistas y pequeños desafíos.'}</p>
            <button className="start-button" type="button" onClick={() => setSelectedLesson(null)}>
              {selectedLesson.state === 'complete' ? '¡Entendido!' : '¡Vamos allá!'} <ArrowRight aria-hidden="true" />
            </button>
          </section>
        </div>
      )}
    </div>
  )
}

export default App
