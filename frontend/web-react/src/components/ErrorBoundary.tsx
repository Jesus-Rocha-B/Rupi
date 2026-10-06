import { Component, type ReactNode } from 'react'

export class ErrorBoundary extends Component<{ children: ReactNode }, { failed: boolean }> {
  state = { failed: false }
  static getDerivedStateFromError() { return { failed: true } }
  render() {
    if (this.state.failed) return <main className="status-panel" role="alert"><h1>No pudimos mostrar tu ruta</h1><p>Vuelve a cargar la página para consultar tu progreso.</p><button className="status-action-btn" onClick={() => window.location.reload()}>Volver a cargar</button></main>
    return this.props.children
  }
}
