import { useEffect, useState } from 'react'
import { fetchStudentRoutes, fetchRouteDetail, type Student, type RouteDetail, type QuestionDto, type AnswerFeedbackResponse } from '../../services/learningRoutesApi'
import { quizRequest } from '../../services/quizzesApi'
import { LessonReview } from './LessonReview'
import { ExplorerBackpack } from './ReviewIllustrations'
import { RupiCharacter } from '../../components/RupiCharacter'
import { REVIEW_SUBJECTS, reviewSubject } from './reviewSubjects'
import './ReviewLibrary.css'

type Topic = { route: RouteDetail; node: RouteDetail['nodes'][number] }
export function ReviewLibrary({ student }: { student: Student }) {
  const [topics, setTopics] = useState<Topic[]>([])
  const [subject, setSubject] = useState('')
  const [selected, setSelected] = useState<Topic | null>(null)
  const [review, setReview] = useState<{ questions: QuestionDto[]; feedback: Record<string, AnswerFeedbackResponse> } | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)
  const [round, setRound] = useState(0)
  const grade = student.gradeNumber
  useEffect(() => {
    const c = new AbortController(); setLoading(true); setError(''); setTopics([])
    fetchStudentRoutes(c.signal).then(async list => {
      const own = list.filter(r => r.grade.number === grade && reviewSubject(r.area.code))
        .sort((a, b) => (b.lastVisitedAt ?? '').localeCompare(a.lastVisitedAt ?? ''))
      if (c.signal.aborted) return
      const details: RouteDetail[] = []
      for (const route of own.filter(r => r.completedNodes > 0)) {
        const detail = await fetchRouteDetail(route.versionRouteId, c.signal)
        const area = reviewSubject(detail.area.code)
        if (area) details.push({ ...detail, area: { ...detail.area, ...area } })
      }
      if (!c.signal.aborted) setTopics(details.flatMap(route => route.nodes.filter(n => n.state === 'COMPLETADO').sort((a, b) => Number(b.id === route.enrollment.lastVisitedNodeId) - Number(a.id === route.enrollment.lastVisitedNodeId) || a.sequence - b.sequence).map(node => ({ route, node }))))
    }).catch(() => { if (!c.signal.aborted) setError('No pudimos cargar tus temas. Inténtalo de nuevo.') })
      .finally(() => { if (!c.signal.aborted) setLoading(false) })
    return () => c.abort()
  }, [grade, retry])
  useEffect(() => {
    setReview(null); setError('')
    if (!selected) return
    const c = new AbortController()
    quizRequest<{ questions: QuestionDto[]; feedback: Record<string, AnswerFeedbackResponse> }>(`student/learning-routes/${selected.route.versionRouteId}/nodes/${selected.node.id}/review`, 'GET', undefined, c.signal)
      .then(data => {
        if (c.signal.aborted) return
        const key = `rupi-review-round:${student.id}:${grade}:${selected.route.area.code}`
        let next = 0
        try { next = Number(sessionStorage.getItem(key)) || 0; sessionStorage.setItem(key, String(next + 1)) } catch { /* Practice remains usable when browser storage is unavailable. */ }
        setRound(next); setReview(data)
      })
      .catch(() => { if (!c.signal.aborted) setError('No pudimos abrir este repaso. Comprueba que la ruta siga disponible y vuelve a intentarlo.') })
    return () => c.abort()
  }, [selected, retry])
  const subjects = REVIEW_SUBJECTS
  const visible = topics.filter(t => !subject || t.route.area.code === subject)
  const bank = review?.questions ?? []
  const quizQuestion = bank.length ? [bank[(round * 2) % bank.length]] : []
  const correctionQuestion = bank.length > 1 ? [bank[(round * 2 + 1) % bank.length]] : []
  return <section className="review-library" aria-labelledby="review-library-title">
    <header className="review-library-heading">
      <div className="review-explorers" aria-hidden="true"><ExplorerBackpack /><RupiCharacter accessories={{ sombrero: 'SOMBRERO_EXPLORADOR' }} /></div>
      <div className="review-hero-content"><h1 id="review-library-title">¡Repasar y jugar!</h1><p className="review-grade">{grade ? `${grade}.º de primaria` : 'Tu grado aún no está asignado'} • Aprende a tu ritmo</p>
        {!selected && !loading && !error && visible[0] && <div className="review-resume"><div><strong>¡Sigue repasando…</strong><p>{visible[0].node.title} · {visible[0].route.area.name}</p></div><button className="review-action" onClick={() => setSelected(visible[0])}>¡Repasar! <span aria-hidden="true">→</span></button></div>}
      </div>
    </header>
    {selected && <nav aria-label="Ubicación del repaso" className="review-breadcrumb">Repasar y jugar → {grade ? `${grade}.º de primaria` : 'Mi grado'} → {selected.route.area.name} → {selected.node.title}</nav>}
    {error && <div role="alert"><p>{error}</p><button onClick={() => setRetry(n => n + 1)}>Volver a intentar</button></div>}
    {selected ? <><button onClick={() => setSelected(null)}>← Volver a mis temas</button><h2>{selected.node.title}</h2><p>{selected.route.title}</p>
      {!review && !error && <p role="status">Preparando tu repaso…</p>}
      {review && <><p>Otros temas de {selected.route.area.name} para tu grado. Son ejercicios de repaso, no las preguntas de tu lección.</p><LessonReview key={`${selected.route.versionRouteId}:${selected.node.id}:${round}`} mode="practice" routeId={selected.route.versionRouteId} nodeId={selected.node.id} questions={quizQuestion} correctionQuestions={correctionQuestion} feedback={review.feedback} />{bank.length > 2 && <button type="button" onClick={() => {
        setRound(previous => { const next = previous + 1; try { sessionStorage.setItem(`rupi-review-round:${student.id}:${grade}:${selected.route.area.code}`, String(next + 1)) } catch { /* Optional round persistence. */ } return next })
      }}>Probar otros ejercicios →</button>}</>}
    </> : <>
      {loading && <p role="status">Buscando los temas que ya estudiaste…</p>}
      {!loading && !error && <>
        <label className="review-subject">Materia<select value={subject} onChange={e => setSubject(e.target.value)}><option value="">Todas mis materias</option>{subjects.map(s => <option key={s.code} value={s.code}>{s.name}</option>)}</select></label>
        <h2>Mis temas estudiados</h2>
        {visible.length === 0 && <p>Al completar una parada de tu grado, su tema aparecerá aquí para que puedas repasarlo.</p>}
        <div className="review-topic-grid">{visible.map((t, index) => <article key={`${t.route.versionRouteId}:${t.node.id}`}><small>{t.route.area.name}</small><h3>{t.node.title}</h3><p>{t.route.title}</p><button className={`review-action${index % 3 === 0 ? '' : ' review-action-green'}`} aria-label={`Abrir repaso: ${t.node.title}`} onClick={() => setSelected(t)}>{index % 3 === 0 ? '¡Abrir repaso!' : '¡Vamos!'} <span aria-hidden="true">→</span></button></article>)}</div>
      </>}
    </>}
  </section>
}
