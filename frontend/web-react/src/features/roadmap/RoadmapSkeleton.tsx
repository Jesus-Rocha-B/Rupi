export function RoadmapSkeleton() {
  return (
    <div className="learning-layout skeleton-layout" aria-busy="true" aria-label="Cargando ruta de aprendizaje">
      {/* Columna izquierda: Mapa interactivo esqueleto */}
      <section className="map-panel skeleton-panel">
        <header className="unit-heading skeleton-unit-heading">
          <div className="skeleton-box skeleton-unit-number" />
          <div className="skeleton-unit-copy">
            <div className="skeleton-line skeleton-eyebrow" />
            <div className="skeleton-line skeleton-title" />
            <div className="skeleton-line skeleton-subtitle" />
          </div>
          <div className="skeleton-box skeleton-status-pill" />
        </header>

        <div className="map-prompt skeleton-prompt">
          <div className="skeleton-guide-box">
            <div className="skeleton-circle skeleton-mascot-avatar" />
            <div className="skeleton-bubble">
              <div className="skeleton-line skeleton-bubble-title" />
              <div className="skeleton-line skeleton-bubble-text" />
            </div>
          </div>
          <div className="skeleton-box skeleton-progress-pill" />
        </div>

        <div className="map-area skeleton-map-area">
          <div className="skeleton-trail-track" />
          {/* Nodos simulados del recorrido con espaciado equilibrado */}
          <div className="skeleton-node-item node-1">
            <div className="skeleton-circle skeleton-stop-circle" />
            <div className="skeleton-box skeleton-stop-card" />
          </div>
          <div className="skeleton-node-item node-2">
            <div className="skeleton-circle skeleton-stop-circle" />
            <div className="skeleton-box skeleton-stop-card" />
          </div>
          <div className="skeleton-node-item node-3">
            <div className="skeleton-circle skeleton-stop-circle active-shimmer" />
            <div className="skeleton-box skeleton-stop-card active-card" />
          </div>
          <div className="skeleton-node-item node-4">
            <div className="skeleton-circle skeleton-stop-circle" />
            <div className="skeleton-box skeleton-stop-card" />
          </div>
          <div className="skeleton-node-item node-5">
            <div className="skeleton-circle skeleton-stop-circle" />
            <div className="skeleton-box skeleton-stop-card" />
          </div>
        </div>

        <div className="map-legend skeleton-legend">
          <div className="skeleton-legend-item"><div className="skeleton-circle skeleton-dot" /><div className="skeleton-line skeleton-legend-text" /></div>
          <div className="skeleton-legend-item"><div className="skeleton-circle skeleton-dot" /><div className="skeleton-line skeleton-legend-text" /></div>
          <div className="skeleton-legend-item"><div className="skeleton-circle skeleton-dot" /><div className="skeleton-line skeleton-legend-text" /></div>
        </div>
      </section>

      {/* Columna derecha: Tarjetas de actividad y tutor */}
      <aside className="right-column skeleton-right-column">
        <section className="continue-card skeleton-continue-card">
          <div className="skeleton-top-bar">
            <div className="skeleton-line skeleton-card-eyebrow" />
          </div>
          <div className="skeleton-image-block" />
          <div className="skeleton-card-body">
            <div className="skeleton-line skeleton-badge-line" />
            <div className="skeleton-line skeleton-heading-line" />
            <div className="skeleton-line skeleton-desc-line" />
            <div className="skeleton-line skeleton-desc-line short" />
            <div className="skeleton-box skeleton-btn" />
          </div>
        </section>

        <section className="mascot-card skeleton-mascot-card">
          <div className="skeleton-card-body">
            <div className="skeleton-line skeleton-badge-line" />
            <div className="skeleton-line skeleton-heading-line short" />
            <div className="skeleton-line skeleton-desc-line" />
            <div className="skeleton-box skeleton-btn small" />
          </div>
          <div className="skeleton-circle skeleton-mascot-side" />
        </section>
      </aside>
    </div>
  )
}
