import { ArrowDown, Check, Flag, Footprints } from 'lucide-react'
import { RupiCharacter } from './RupiCharacter'
import type { RouteDetail } from '../services/learningRoutesApi'

export function RouteOverview({ route, name }: { route: RouteDetail; name: string }) {
  const completed = route.progress.completedNodes
  const total = route.progress.totalNodes
  const percent = total ? Math.round(completed / total * 100) : 0
  const available = route.nodes.filter(node => node.state === 'DISPONIBLE').length
  const inProgress = route.nodes.filter(node => node.state === 'EN_CURSO').length
  return <section className="adventure-welcome" aria-label="Resumen de tu aventura">
    <div className="welcome-banner">
      <div className="welcome-copy"><span className="welcome-greeting">¡Qué bueno verte, {name}!</span><h2>Un pequeño paso.<br />Un gran descubrimiento.</h2><p>Explora, aprende y descubre lo que eres capaz de hacer.<br className="desktop-break" /> Rupi te acompaña en cada parada.</p><a href="#explorar" className="welcome-link">Vamos a mi mapa <ArrowDown size={17} aria-hidden="true" /></a></div>
      <div className="welcome-art" aria-hidden="true"><span className="art-sun" /><span className="art-orbit orbit-one" /><span className="art-orbit orbit-two" /><span className="art-star star-a">✦</span><span className="art-star star-b">✦</span><RupiCharacter /><span className="mascot-sign">¡Vamos juntos!</span></div>
    </div>
    <div className="welcome-stats">
      <div className="overall-progress"><div><span>Tu recorrido</span><strong>{percent}<small>%</small></strong></div><div className="overall-track"><div className="progress-track" role="progressbar" aria-valuenow={percent} aria-valuemin={0} aria-valuemax={100} aria-label="Porcentaje de avance en la ruta"><div className="progress-fill" style={{width:`${percent}%`}} /></div><p>{completed} de {total} actividades completadas</p></div></div>
      <div className="stat-item"><span className="stat-icon done"><Check aria-hidden="true" /></span><div><b>{completed}</b><span>Completadas</span></div></div>
      <div className="stat-item"><span className="stat-icon underway"><Footprints aria-hidden="true" /></span><div><b>{inProgress}</b><span>En curso</span></div></div>
      <div className="stat-item"><span className="stat-icon ready"><Flag aria-hidden="true" /></span><div><b>{available}</b><span>Disponibles</span></div></div>
    </div>
  </section>
}
