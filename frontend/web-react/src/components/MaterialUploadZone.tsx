import { useState, useRef, type ChangeEvent, type FormEvent } from 'react'
import {
  FileText,
  FileSpreadsheet,
  Link as LinkIcon,
  Upload,
  CheckCircle2,
  AlertCircle,
  ExternalLink,
  FileUp,
} from 'lucide-react'
import {
  uploadTeacherMaterial,
  type AttachedResource,
  type AttachResourcePayload,
} from '../services/teacherApi'
import './MaterialUploadZone.css'

interface Props {
  nodeId: string
  nodeTitle: string
  resources: AttachedResource[]
  onResourceAttached: (payload: AttachResourcePayload) => Promise<void>
}

export function MaterialUploadZone({ nodeId, nodeTitle, resources, onResourceAttached }: Props) {
  const [activeTab, setActiveTab] = useState<'FILE' | 'LINK'>('FILE')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [fileTitle, setFileTitle] = useState('')
  const [linkTitle, setLinkTitle] = useState('')
  const [linkUrl, setLinkUrl] = useState('')
  const [uploading, setUploading] = useState(false)
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null)

  const fileInputRef = useRef<HTMLInputElement>(null)

  function handleFileSelect(e: ChangeEvent<HTMLInputElement>) {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0]
      setSelectedFile(file)
      if (!fileTitle) {
        // Auto-asignar el nombre sin extensión como título inicial
        const cleanName = file.name.replace(/\.[^/.]+$/, '')
        setFileTitle(cleanName)
      }
      setFeedback(null)
    }
  }

  async function handleFileUpload(e: FormEvent) {
    e.preventDefault()
    if (!selectedFile) {
      setFeedback({ type: 'error', message: 'Selecciona un archivo PDF, PPT o imagen para adjuntar.' })
      return
    }

    setUploading(true)
    setFeedback(null)

    try {
      // 1. Subir al servidor seguro
      const uploadResult = await uploadTeacherMaterial(selectedFile)

      // 2. Determinar tipo de recurso
      let type: 'DOCUMENTO' | 'PRESENTACION' | 'IMAGEN' = 'DOCUMENTO'
      const lowerName = selectedFile.name.toLowerCase()
      if (lowerName.endsWith('.ppt') || lowerName.endsWith('.pptx')) {
        type = 'PRESENTACION'
      } else if (lowerName.endsWith('.jpg') || lowerName.endsWith('.jpeg') || lowerName.endsWith('.png')) {
        type = 'IMAGEN'
      }

      // 3. Vincular permanentemente al nodo
      await onResourceAttached({
        type,
        title: fileTitle.trim() || selectedFile.name,
        url: uploadResult.downloadUrl,
        multimediaId: uploadResult.id,
      })

      setFeedback({ type: 'success', message: '¡Archivo subido y enlazado exitosamente a la lección!' })
      setSelectedFile(null)
      setFileTitle('')
      if (fileInputRef.current) fileInputRef.current.value = ''
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'No se pudo subir el archivo. Intenta nuevamente.'
      setFeedback({ type: 'error', message: msg })
    } finally {
      setUploading(false)
    }
  }

  async function handleLinkSubmit(e: FormEvent) {
    e.preventDefault()
    const trimmedTitle = linkTitle.trim()
    const trimmedUrl = linkUrl.trim()

    if (!trimmedTitle || !trimmedUrl) {
      setFeedback({ type: 'error', message: 'Ingresa el título y la URL del enlace educativo.' })
      return
    }

    if (!trimmedUrl.startsWith('http://') && !trimmedUrl.startsWith('https://')) {
      setFeedback({ type: 'error', message: 'La dirección web debe comenzar con https:// o http://' })
      return
    }

    setUploading(true)
    setFeedback(null)

    try {
      await onResourceAttached({
        type: 'ENLACE',
        title: trimmedTitle,
        url: trimmedUrl,
        multimediaId: null,
      })

      setFeedback({ type: 'success', message: '¡Enlace educativo enlazado a la parada del mapa!' })
      setLinkTitle('')
      setLinkUrl('')
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'No se pudo guardar el enlace.'
      setFeedback({ type: 'error', message: msg })
    } finally {
      setUploading(false)
    }
  }

  return (
    <div className="material-upload-container" aria-label={`Materiales para ${nodeTitle}`}>
      <div className="material-zone-header">
        <h4 className="material-zone-title">
          <FileUp size={20} className="zone-icon" aria-hidden="true" />
          <span>Materiales y Recursos de la Lección</span>
        </h4>
        <span className="material-zone-subtitle">
          Parada: <strong>{nodeTitle}</strong>
        </span>
      </div>

      {/* Pestañas: Subir Documento vs Pegar Enlace */}
      <div className="material-tab-switcher" role="tablist">
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'FILE'}
          className={`material-tab-btn ${activeTab === 'FILE' ? 'is-active' : ''}`}
          onClick={() => { setActiveTab('FILE'); setFeedback(null); }}
        >
          <FileText size={16} aria-hidden="true" />
          <span>Subir Archivo (PDF, PPT, Imagen)</span>
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'LINK'}
          className={`material-tab-btn ${activeTab === 'LINK' ? 'is-active' : ''}`}
          onClick={() => { setActiveTab('LINK'); setFeedback(null); }}
        >
          <LinkIcon size={16} aria-hidden="true" />
          <span>Enlace Web Externo</span>
        </button>
      </div>

      {/* Formulario de subida de archivo */}
      {activeTab === 'FILE' && (
        <form className="material-file-form" onSubmit={handleFileUpload}>
          <div className="file-drop-area">
            <input
              ref={fileInputRef}
              type="file"
              id={`file-input-${nodeId}`}
              className="file-hidden-input"
              accept=".pdf,.ppt,.pptx,.jpg,.jpeg,.png"
              onChange={handleFileSelect}
              disabled={uploading}
            />
            <label htmlFor={`file-input-${nodeId}`} className="file-drop-label">
              <Upload size={32} className="drop-icon" aria-hidden="true" />
              {selectedFile ? (
                <div className="selected-file-info">
                  <span className="file-name">{selectedFile.name}</span>
                  <span className="file-size">({(selectedFile.size / 1024 / 1024).toFixed(2)} MB)</span>
                </div>
              ) : (
                <div className="drop-prompt">
                  <strong>Toca aquí para seleccionar un archivo</strong>
                  <span>Formatos soportados: PDF, PowerPoint (.ppt, .pptx) o Imágenes (máx. 15 MB)</span>
                </div>
              )}
            </label>
          </div>

          {selectedFile && (
            <div className="file-title-field">
              <label htmlFor={`file-title-${nodeId}`}>Título pedagógico para los estudiantes:</label>
              <input
                id={`file-title-${nodeId}`}
                type="text"
                className="material-text-input"
                placeholder="Ej. Ficha de lectura y preguntas"
                value={fileTitle}
                onChange={(e) => setFileTitle(e.target.value)}
                disabled={uploading}
                required
              />
            </div>
          )}

          <button
            type="submit"
            className="material-submit-btn"
            disabled={!selectedFile || uploading}
          >
            <Upload size={18} aria-hidden="true" />
            <span>{uploading ? 'Subiendo documento seguro…' : 'Adjuntar Documento a esta Parada'}</span>
          </button>
        </form>
      )}

      {/* Formulario de enlace externo */}
      {activeTab === 'LINK' && (
        <form className="material-link-form" onSubmit={handleLinkSubmit}>
          <div className="link-form-group">
            <label htmlFor={`link-title-${nodeId}`}>Título del recurso web:</label>
            <input
              id={`link-title-${nodeId}`}
              type="text"
              className="material-text-input"
              placeholder="Ej. Video explicativo en PerúEduca"
              value={linkTitle}
              onChange={(e) => setLinkTitle(e.target.value)}
              disabled={uploading}
              required
            />
          </div>

          <div className="link-form-group">
            <label htmlFor={`link-url-${nodeId}`}>Dirección web (URL):</label>
            <input
              id={`link-url-${nodeId}`}
              type="url"
              className="material-text-input"
              placeholder="https://..."
              value={linkUrl}
              onChange={(e) => setLinkUrl(e.target.value)}
              disabled={uploading}
              required
            />
          </div>

          <button
            type="submit"
            className="material-submit-btn"
            disabled={uploading || !linkTitle.trim() || !linkUrl.trim()}
          >
            <LinkIcon size={18} aria-hidden="true" />
            <span>{uploading ? 'Guardando enlace…' : 'Vincular Enlace a esta Parada'}</span>
          </button>
        </form>
      )}

      {/* Mensajes de feedback */}
      {feedback && (
        <div className={`material-feedback-banner ${feedback.type}`} role="alert">
          {feedback.type === 'success' ? (
            <CheckCircle2 size={18} aria-hidden="true" />
          ) : (
            <AlertCircle size={18} aria-hidden="true" />
          )}
          <span>{feedback.message}</span>
        </div>
      )}

      {/* Lista de recursos ya enlazados a la parada */}
      <div className="attached-resources-list">
        <h5 className="resources-list-heading">
          Materiales enlazados actualmente ({resources.length})
        </h5>
        {resources.length === 0 ? (
          <p className="no-resources-hint">
            Esta parada aún no tiene documentos adjuntos. Sube un PDF, PPT o enlace para tus alumnos.
          </p>
        ) : (
          <ul className="resources-items">
            {resources.map((res) => (
              <li key={res.id} className="resource-item-chip">
                <div className="resource-item-lead">
                  {res.type === 'PRESENTACION' ? (
                    <FileSpreadsheet size={18} className="item-icon ppt" aria-hidden="true" />
                  ) : res.type === 'ENLACE' ? (
                    <LinkIcon size={18} className="item-icon link" aria-hidden="true" />
                  ) : (
                    <FileText size={18} className="item-icon pdf" aria-hidden="true" />
                  )}
                  <span className="resource-title">{res.title}</span>
                  <span className="resource-type-tag">{res.type}</span>
                </div>
                <a
                  href={res.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="resource-download-link"
                  title="Abrir o descargar recurso"
                >
                  <ExternalLink size={15} aria-hidden="true" />
                  <span>Ver</span>
                </a>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
