import { Award, CheckCircle2, Feather, Printer, Sparkles, Volume2, X } from 'lucide-react'
import type { RouteDiplomaResponse } from '../services/learningRoutesApi'
import { RupiCharacter } from './RupiCharacter'
import { speakText } from '../utils/speech'

type Props = {
  diploma: RouteDiplomaResponse
  onClose: () => void
}

export function RouteDiplomaModal({ diploma, onClose }: Props) {
  function handlePrint() {
    window.print()
  }

  function handleListen() {
    const textToRead = `Diploma de Logro Escolar. Otorgado con honor a ${diploma.studentName}, de ${diploma.gradeName} del ${diploma.schoolName}. Por haber completado todas las paradas de la ruta ${diploma.routeTitle} de ${diploma.areaName}. Mensaje de Rupi: ${diploma.rupiHonorMessage}`
    speakText(textToRead)
  }

  const formattedDate = (() => {
    try {
      const date = new Date(diploma.completedAt)
      if (isNaN(date.getTime())) return diploma.completedAt
      return date.toLocaleDateString('es-PE', {
        day: 'numeric',
        month: 'long',
        year: 'numeric',
      })
    } catch {
      return diploma.completedAt
    }
  })()

  return (
    <div className="diploma-overlay" role="dialog" aria-modal="true" aria-labelledby="diploma-title">
      <div className="diploma-modal-container">
        {/* Barra superior de herramientas (no se imprime) */}
        <div className="diploma-toolbar" no-print="true">
          <button
            type="button"
            className="diploma-action-btn primary"
            onClick={handlePrint}
            aria-label="Imprimir o guardar diploma en PDF"
          >
            <Printer size={18} aria-hidden="true" />
            <span>Imprimir / Guardar PDF</span>
          </button>

          <button
            type="button"
            className="diploma-action-btn"
            onClick={handleListen}
            aria-label="Escuchar diploma por voz"
          >
            <Volume2 size={18} aria-hidden="true" />
            <span>Escuchar a Rupi</span>
          </button>

          <button
            type="button"
            className="diploma-close-btn"
            onClick={onClose}
            aria-label="Cerrar diploma"
          >
            <X size={20} aria-hidden="true" />
          </button>
        </div>

        {/* Certificado físico imprimible */}
        <div className="diploma-sheet" id="printable-diploma">
          <div className="diploma-border-outer">
            <div className="diploma-border-inner">
              {/* Esquinas decorativas andinas */}
              <div className="diploma-corner top-left" aria-hidden="true">◆</div>
              <div className="diploma-corner top-right" aria-hidden="true">◆</div>
              <div className="diploma-corner bottom-left" aria-hidden="true">◆</div>
              <div className="diploma-corner bottom-right" aria-hidden="true">◆</div>

              {/* Encabezado institucional */}
              <header className="diploma-header">
                <div className="diploma-republica">
                  <span>REPÚBLICA DEL PERÚ</span>
                  <span className="diploma-dot">·</span>
                  <span>MINISTERIO DE EDUCACIÓN</span>
                </div>
                <div className="diploma-subhead">PLATAFORMA EDUCATIVA DIGITAL RUPI</div>
                <h1 id="diploma-title" className="diploma-main-title">
                  DIPLOMA DE LOGRO ESCOLAR
                </h1>
                <div className="diploma-badge-title">
                  <Feather size={16} className="tunqui-feather-icon" aria-hidden="true" />
                  <span>Gran Vuelo del Gallito de las Rocas</span>
                </div>
              </header>

              {/* Cuerpo del diploma */}
              <main className="diploma-body">
                <p className="diploma-confer-text">
                  Se otorga con el más alto honor y reconocimiento a:
                </p>

                <h2 className="diploma-student-name">
                  {diploma.studentName}
                </h2>

                <p className="diploma-student-meta">
                  Estudiante de <strong>{diploma.gradeName}</strong> en el <strong>{diploma.schoolName}</strong>
                </p>

                <p className="diploma-achievement-paragraph">
                  Por haber culminado con éxito, perseverancia y dedicación todas las{' '}
                  <strong>{diploma.totalCompletedNodes} paradas</strong> de la ruta escolar{' '}
                  <strong>«{diploma.routeTitle}»</strong> en el área de{' '}
                  <strong>{diploma.areaName}</strong>, demostrando el logro destacado en las competencias oficiales del Currículo Nacional:
                </p>

                {/* Listado de competencias del MINEDU */}
                <div className="diploma-competencies-grid">
                  {diploma.competencies.map((comp) => (
                    <div key={comp.code} className="diploma-competency-item">
                      <CheckCircle2 size={16} className="competency-check" aria-hidden="true" />
                      <span>{comp.name}</span>
                    </div>
                  ))}
                </div>

                {/* Mensaje de aliento de Rupi */}
                <div className="diploma-rupi-quote">
                  <Sparkles size={16} className="rupi-sparkle-icon" aria-hidden="true" />
                  <span>«{diploma.rupiHonorMessage}»</span>
                </div>
              </main>

              {/* Pie del diploma: Medalla dorada y firmas */}
              <footer className="diploma-footer">
                <div className="diploma-signature-col">
                  <div className="diploma-signature-line"></div>
                  <span className="signature-role">Dirección / Docente de Aula</span>
                  <span className="signature-sub">{diploma.schoolName}</span>
                </div>

                <div className="diploma-seal-col">
                  <div className="diploma-gold-medal" aria-label="Medalla Gran Gallito de Oro">
                    <div className="medal-ring">
                      <Award size={36} className="medal-tunqui-award" aria-hidden="true" />
                    </div>
                    <span className="medal-ribbon-tag">GRAN GALLITO DE ORO</span>
                    <span className="medal-xp-tag">+{diploma.totalExperienceEarned} XP Acumulados</span>
                  </div>
                </div>

                <div className="diploma-signature-col">
                  <div className="diploma-rupi-signature">
                    <RupiCharacter mood="cheering" className="diploma-rupi-stamp" />
                    <span className="rupi-signature-text">Rupi</span>
                  </div>
                  <div className="diploma-signature-line"></div>
                  <span className="signature-role">Rupi · Gallito de las Rocas</span>
                  <span className="signature-sub">Compañero y Tutor Escolar</span>
                </div>
              </footer>

              {/* Fecha y código de certificación */}
              <div className="diploma-bottom-meta">
                <span>Expedido el {formattedDate}</span>
                <span className="diploma-dot">·</span>
                <span>Ayacucho, Perú</span>
                <span className="diploma-dot">·</span>
                <span>Acreditación Curricular CNEB</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
