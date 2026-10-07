import { useEffect, useState } from 'react'
import { ApiError, apiFetch, userMessage } from './api'
import { log } from './logger'

interface PingResponse {
  message: string
}

type BackendStatus =
  | { state: 'checking' }
  | { state: 'up' }
  | { state: 'down'; message: string; requestId: string | null }

export default function App() {
  const [status, setStatus] = useState<BackendStatus>({ state: 'checking' })

  useEffect(() => {
    const controller = new AbortController()

    apiFetch<PingResponse>('/ping', { signal: controller.signal })
      .then(() => setStatus({ state: 'up' }))
      .catch((error) => {
        if (controller.signal.aborted) return
        log.warn('Ping failed', { error })
        setStatus({
          state: 'down',
          message: userMessage(error),
          requestId: error instanceof ApiError ? error.requestId : null,
        })
      })

    return () => controller.abort()
  }, [])

  return (
    <main>
      <h1>Files</h1>
      {status.state === 'checking' && <p>Backend: checking...</p>}
      {status.state === 'up' && <p>Backend: connected</p>}
      {status.state === 'down' && (
        <p role="alert">
          Backend: {status.message}
          {status.requestId && <small> Reference: {status.requestId}</small>}
        </p>
      )}
    </main>
  )
}
