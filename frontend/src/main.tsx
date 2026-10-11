import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import './index.css'
import App from './App.tsx'
import { ErrorBoundary } from './ErrorBoundary.tsx'
import { log } from './logger.ts'

createRoot(document.getElementById('root')!, {
  onCaughtError(error, errorInfo) {
    log.error('Render error caught by boundary', error, { componentStack: errorInfo.componentStack })
  },
  onUncaughtError(error, errorInfo) {
    log.error('Render error not caught by any boundary', error, { componentStack: errorInfo.componentStack })
  },
  onRecoverableError(error) {
    log.warn('React recovered from an error', { error })
  },
}).render(
  <StrictMode>
    <ErrorBoundary>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </ErrorBoundary>
  </StrictMode>,
)
