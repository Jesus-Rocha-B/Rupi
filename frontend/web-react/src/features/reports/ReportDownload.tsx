import { useEffect, useRef, useState } from 'react'
import { Download } from 'lucide-react'
import { reportsResponse } from '../../services/reportsApi'
export function ReportDownload({ path, filename, available }: { path: string; filename: string; available: boolean }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const request = useRef<AbortController | null>(null)
  useEffect(() => () => { request.current?.abort() }, [path])
  async function download() {
    const controller = new AbortController(); request.current?.abort(); request.current = controller
    setBusy(true); setError('')
    try {
      const response = await reportsResponse(path, controller.signal)
      if (response.status === 204) { setError('Todavía no hay resultados exportables para esta selección.'); return }
      const blob = await response.blob()
      if (controller.signal.aborted) return
      const url = URL.createObjectURL(blob); const link = document.createElement('a')
      link.href = url; link.download = filename; document.body.appendChild(link); link.click(); link.remove()
      setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch { if (!controller.signal.aborted) setError('No pudimos descargar el reporte. Vuelve a intentarlo.') }
    finally { if (!controller.signal.aborted) setBusy(false) }
  }
  return <div><button type="button" onClick={() => void download()} disabled={busy || !available}><Download size={20} aria-hidden="true" />{busy ? 'Descargando…' : 'Descargar CSV'}</button>
    {!available && <p className="report-note">Todavía no hay resultados exportables para esta selección.</p>}{error && <p role="alert">{error}</p>}</div>
}
