import { useEffect, useRef, useState } from 'react'
import { quizRequest } from '../../services/quizzesApi'
import '../reinforcement/AssignedTasks.css'
import './Flashcards.css'
export interface Card { id: string; question: string; answer: string; source: string; state: string }
interface Deck { cards: Card[]; current: string | null; revealed: boolean; revision: number }
export function Flashcards({ routeId, nodeId }: { routeId: string; nodeId: string }) {
 const path = 'student/learning-routes/' + routeId + '/nodes/' + nodeId + '/flashcards'
 const [deck, setDeck] = useState<Deck>()
 const [error, setError] = useState('')
 const [busy, setBusy] = useState(false)
 const [reload, setReload] = useState(0)
 const [hasPending, setHasPending] = useState(false)
 const lock = useRef(false)
 const pending = useRef<{ requestId: string; revision: number; action: string; cardId: string | null } | null>(null)
 const life = useRef<AbortController | null>(null)
 const touch = useRef<{ x: number; y: number } | null>(null)
 const suppressClick = useRef(false)
 useEffect(() => { const c = new AbortController(); life.current = c; return () => c.abort() }, [])
 useEffect(() => {
  const c = new AbortController()
  quizRequest<Deck>(path, 'GET', undefined, c.signal).then(d => { if (!c.signal.aborted) { setDeck(d); setError(''); pending.current = null; setHasPending(false) } })
   .catch((e: Error) => { if (!c.signal.aborted) setError(e.message) })
  return () => c.abort()
 }, [path, reload])
 const index = deck?.cards.findIndex(c => c.id === deck.current) ?? -1
 const card = deck?.cards[index]
 async function action(name: string, id = deck?.current ?? null) {
  if (!deck || lock.current) return
  lock.current = true; setBusy(true); setError('')
  pending.current ??= { requestId: crypto.randomUUID(), revision: deck.revision, action: name, cardId: id }
  setHasPending(true)
  try {
   const next = await quizRequest<Deck>(path, 'POST', pending.current, life.current?.signal)
   if (!life.current?.signal.aborted) { setDeck(next); pending.current = null; setHasPending(false) }
  } catch (e) { if (!life.current?.signal.aborted) setError((e as Error).message) }
  finally { lock.current = false; if (!life.current?.signal.aborted) setBusy(false) }
 }
 function move(delta: number) {
  if (!deck || index < 0) return
  const next = index + delta
  if (next >= 0 && next < deck.cards.length) void action('NAVIGATE', deck.cards[next].id)
 }
 return <section className="assigned-tasks flashcards" aria-label="Repasar con tarjetas" aria-busy={busy}>
  <h2>Repasar con tarjetas</h2><p>Piensa en la respuesta y voltea la tarjeta. Este repaso no cambia tus notas.</p>
  {error && <div role="alert"><p>{error}</p><button disabled={busy} onClick={() => setReload(n => n + 1)}>Recuperar mi repaso</button>
   {hasPending && <button disabled={busy} onClick={() => void action('RETRY')}>Reintentar guardado</button>}</div>}
  {!deck && !error && <p role="status">Preparando tus tarjetas…</p>}
  {deck?.cards.length === 0 && <p>Todavía no hay tarjetas revisadas para esta actividad. Puedes continuar tu lección.</p>}
  {card && deck && <>
   <p aria-live="polite">Tarjeta {index + 1} de {deck.cards.length} · {card.state === 'PRACTICAR' ? 'Necesito practicar' : card.state === 'RECUERDO' ? 'Lo recuerdo' : 'Por descubrir'}</p>
   <button className={'memory-card' + (deck.revealed ? ' is-revealed' : '')} disabled={busy || !!error}
    onTouchStart={e => { const t = e.touches[0]; touch.current = { x: t.clientX, y: t.clientY }; suppressClick.current = false }}
    onTouchMove={e => { if (touch.current) { const t = e.touches[0]; if (Math.abs(t.clientX - touch.current.x) > 10 || Math.abs(t.clientY - touch.current.y) > 10) suppressClick.current = true } }}
    onTouchCancel={() => { touch.current = null; suppressClick.current = true }}
    onTouchEnd={e => { if (!touch.current) return; const t = e.changedTouches[0]; const dx = t.clientX - touch.current.x, dy = t.clientY - touch.current.y; touch.current = null; if (Math.abs(dx) > 60 && Math.abs(dx) > Math.abs(dy) * 1.5) { suppressClick.current = true; move(dx < 0 ? 1 : -1) } }}
    onClick={e => { if (suppressClick.current && e.detail !== 0) { suppressClick.current = false; return } void action(deck.revealed ? 'NAVIGATE' : 'REVEAL') }}>
    <span key={card.id + String(deck.revealed)} className="memory-face"><small>{deck.revealed ? 'Respuesta · Toca para ver la pregunta' : 'Pregunta · Toca para ver la respuesta'}</small>
    <strong>{deck.revealed ? card.answer : card.question}</strong></span>
   </button>
   <div className="memory-actions">
    <button disabled={busy || !!error || index === 0} onClick={() => move(-1)}>Anterior</button>
    <button disabled={busy || !!error} onClick={() => void action(deck.revealed ? 'NAVIGATE' : 'REVEAL')}>{deck.revealed ? 'Ver pregunta' : 'Ver respuesta'}</button>
    <button disabled={busy || !!error || index === deck.cards.length - 1} onClick={() => move(1)}>Siguiente</button>
   </div>
   {deck.revealed && <div className="memory-actions" aria-label="Cómo te fue">
    <button disabled={busy || !!error} onClick={() => void action('REMEMBER')}>Lo recuerdo</button>
    <button disabled={busy || !!error} onClick={() => void action('PRACTICE')}>Necesito practicar</button>
   </div>}
   <p className="assigned-note">Fuente indicada por tu docente: {card.source}</p>
   <p>Primero repasamos lo que necesita práctica, después las tarjetas nuevas y al final lo que recuerdas.</p>
   {deck.cards.every(c => c.state === 'RECUERDO') && <p role="status">¡Repasaste todos estos conceptos! Puedes volver a revisarlos cuando quieras.</p>}
   <button disabled={busy || !!error} onClick={() => void action('RESTART')}>Volver a repasar</button>
  </>}
 </section>
}
