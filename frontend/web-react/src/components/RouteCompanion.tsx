import { ArrowUpRight, Clock3, MapPin, Lightbulb, Check, Lock, CircleDot, Flag } from 'lucide-react'
import type { RouteDetail, RouteNode } from '../services/learningRoutesApi'

export function RouteCompanion({route,onSelect}:{route:RouteDetail;onSelect:(node:RouteNode)=>void}) {
  const next = route.nodes.find(node=>node.state==='EN_CURSO') ?? route.nodes.find(node=>node.state==='DISPONIBLE')
  return <aside className="route-companion" aria-label="Sobre tu recorrido">
    {next && <section className="next-stop-card"><span className="card-kicker"><CircleDot size={15} />Tu siguiente paso</span><div className="next-stop-symbol"><Flag size={30} /></div><span className="next-stop-index">Parada {String(next.sequence).padStart(2,'0')}</span><h3>{next.title}</h3><p>{next.state==='EN_CURSO' ? 'Ya comenzaste esta aventura. Consulta cómo vas.' : 'Una nueva actividad te está esperando.'}</p>{next.estimatedMinutes != null && <span className="time-label"><Clock3 size={14} />{next.estimatedMinutes} minutos</span>}<button onClick={()=>onSelect(next)} className="next-stop-button">Ver actividad <ArrowUpRight size={18}/></button></section>}
    <section className="place-card"><img src="/images/ayacucho-plaza.jpg" alt="Plaza Mayor de Ayacucho, rodeada de portales y arquitectura histórica"/><div><span><MapPin size={14}/>Hecho con raíces peruanas</span><h3>Aprender nos lleva lejos.</h3><p>La plaza de Ayacucho inspira nuestra aventura. Hay mucho por descubrir a nuestro alrededor.</p><a href="https://commons.wikimedia.org/wiki/File:PLAZA_MAYOR_DE_AYACUCHO.jpg" target="_blank" rel="noreferrer">Foto: Pollinhhsano · CC BY-SA 4.0</a></div></section>
    <section className="map-key-card"><h3>Así se lee tu mapa</h3><p><Check/>Completado <span>¡Lo lograste!</span></p><p><CircleDot/>En curso <span>Ya empezaste</span></p><p><Flag/>Disponible <span>Puedes explorarlo</span></p><p><Lock/>Bloqueado <span>Un paso a la vez</span></p></section>
    <div className="gentle-note"><Lightbulb size={22}/><p>No hay prisa.<br/><strong>Lo importante es seguir aprendiendo.</strong></p></div>
  </aside>
}
