import { useMemo } from 'react'
import { Check, MapPin, Sparkles, Star } from 'lucide-react'
import { RupiCharacter } from '../../components/RupiCharacter'
import { createTrailPath, lessons, type Lesson } from './lessons'

type Props = {
  guideMessage: string
  onGuideClick: () => void
  onSelectLesson: (lesson: Lesson) => void
}

export function Roadmap({ guideMessage, onGuideClick, onSelectLesson }: Props) {
  const trailPath = useMemo(() => createTrailPath(lessons), [])

  return (
    <section className="map-panel" id="explorar" aria-labelledby="roadmap-title">
      <header className="unit-heading">
        <span className="unit-number">01</span>
        <div className="unit-copy">
          <span className="eyebrow">UN PASEO ENTRE PORTALES Y RETABLOS</span>
          <h3 id="roadmap-title">La plaza de los arcos</h3>
          <span className="unit-subtitle">Diez paradas para descubrir y aprender</span>
        </div>
        <span className="unit-status">EN CURSO</span>
      </header>

      <div className="map-prompt">
        <button className="map-guide" onClick={onGuideClick} aria-label="Pedir un consejo a Rupi">
          <span className="guide-portrait"><RupiCharacter /></span>
          <span className="guide-bubble">
            <b>Rupi <span>TU COMPAÑERO</span></b>
            <span className="guide-copy">{guideMessage}</span>
            <small>Toca para otro consejo</small>
          </span>
        </button>
        <span className="map-progress">2 <span>/</span> 10 <small>niveles</small></span>
      </div>

      <div className="map-area" aria-label="Recorrido de diez actividades">
        <svg className="path-line" viewBox="0 0 600 1000" preserveAspectRatio="none" aria-hidden="true">
          <path className="road-shadow" d={trailPath} />
          <path className="road-edge" d={trailPath} />
          <path className="road-surface" d={trailPath} />
          <path className="road-center" d={trailPath} />
        </svg>

        {lessons.map((lesson) => {
          const canOpen = lesson.state !== 'locked'
          const stateLabel = lesson.state === 'locked' ? 'bloqueado' : lesson.state === 'active' ? 'jugar ahora' : 'completado'
          return (
            <div
              className={`trail-stop ${lesson.state}`}
              id={lesson.id === 3 ? 'retos' : undefined}
              key={lesson.id}
              style={{ left: `${lesson.x}%`, top: `${lesson.y}%` }}
            >
              <button
                className="lesson-node"
                type="button"
                onClick={() => canOpen && onSelectLesson(lesson)}
                disabled={!canOpen}
                aria-label={`Nivel ${lesson.id}: ${lesson.title}, ${stateLabel}`}
              >
                <span className="node-number">
                  {lesson.state === 'complete' ? <Check aria-hidden="true" /> : lesson.state === 'active' ? <Star aria-hidden="true" /> : lesson.id}
                </span>
              </button>
              <button
                className={`node-label ${lesson.labelSide} ${lesson.state}`}
                type="button"
                onClick={() => canOpen && onSelectLesson(lesson)}
                disabled={!canOpen}
                aria-label={`Abrir nivel ${lesson.id}: ${lesson.title}`}
              >
                <span className="stop-place"><MapPin aria-hidden="true" />{lesson.context}</span>
                <b>{lesson.title}</b>
                <span>{lesson.activity}</span>
              </button>
            </div>
          )
        })}

        <Sparkles className="map-decoration sparkle sparkle-one" aria-hidden="true" />
        <Sparkles className="map-decoration sparkle sparkle-two" aria-hidden="true" />
      </div>

      <div className="map-legend" id="logros">
        <span><i className="legend-dot done" />Completado</span>
        <span><i className="legend-dot now" />Siguiente reto</span>
        <span><i className="legend-dot locked" />Por desbloquear</span>
      </div>
      <a className="image-credit" href="https://commons.wikimedia.org/wiki/File:PLAZA_MAYOR_DE_AYACUCHO.jpg" target="_blank" rel="noreferrer">
        Foto: Pollinhhsano · Wikimedia Commons · CC BY-SA 4.0
      </a>
    </section>
  )
}
