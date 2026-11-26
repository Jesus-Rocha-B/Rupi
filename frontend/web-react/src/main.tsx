import '@fontsource/nunito/latin-700.css'
import '@fontsource/nunito/latin-800.css'
import '@fontsource/nunito/latin-900.css'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { LearningPage } from './pages/LearningPage'
import { AdminPage } from './pages/AdminPage'
import { ErrorBoundary } from './components/ErrorBoundary'

// La consola de administración (HU-41 a HU-45) vive en /admin y solo habla con la API Django.
const isAdminConsole = window.location.pathname === '/admin' || window.location.pathname.startsWith('/admin/')

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ErrorBoundary>{isAdminConsole ? <AdminPage /> : <LearningPage />}</ErrorBoundary>
  </StrictMode>,
)

