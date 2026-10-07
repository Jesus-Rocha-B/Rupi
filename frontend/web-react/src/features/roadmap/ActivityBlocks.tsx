import type { ActivityContent } from '../../services/learningRoutesApi'

function safeResource(value: string | null) {
  if (!value) return null
  try {
    const url = new URL(value, window.location.origin)
    return ['http:', 'https:'].includes(url.protocol) ? url.href : null
  } catch { return null }
}
export function ActivityBlocks({ activity }: { activity: ActivityContent }) {
  return <section className="activity-content" aria-label="Contenido de la actividad">
    {activity.instructions && <p>{activity.instructions}</p>}
    {activity.blocks.map(block => {
      const url = safeResource(block.url)
      return <div key={block.id} className={block.type === 'DESTACADO' ? 'activity-highlight' : 'activity-block'}>
        {block.text && <p>{block.text}</p>}
        {url && block.type === 'IMAGEN' && <img src={url} alt={block.accessibleText ?? ''} />}
        {url && block.type === 'AUDIO' && <audio controls src={url} aria-label={block.accessibleText ?? 'Audio de la actividad'} />}
        {url && block.type === 'VIDEO' && <video controls src={url} aria-label={block.accessibleText ?? 'Video de la actividad'} />}
        {url && block.type === 'ADJUNTO' && <a href={url} target="_blank" rel="noreferrer">{block.accessibleText ?? 'Abrir material'}</a>}
        {!url && !block.text && <p>Este material aún no está disponible. Consulta con tu docente.</p>}
      </div>
    })}
    <p className="activity-saved">Tu visita quedó guardada. Puedes volver al mapa y continuar después.</p>
  </section>
}
