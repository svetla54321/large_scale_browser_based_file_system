export const API_BASE = '/api/v1'

interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
}

export class ApiError extends Error {
  readonly status: number
  readonly requestId: string | null
  readonly problem: ProblemDetail | undefined

  constructor(status: number, requestId: string | null, problem?: ProblemDetail) {
    super(problem?.detail ?? problem?.title ?? `HTTP ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.requestId = requestId
    this.problem = problem
  }
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, init)
  } catch (error) {
    if (init?.signal?.aborted) throw error
    throw new ApiError(0, null)
  }

  if (!response.ok) {
    const isProblem = response.headers.get('Content-Type')?.includes('application/problem+json')
    const problem = isProblem ? await response.json().catch(() => undefined) : undefined
    throw new ApiError(response.status, response.headers.get('X-Request-Id'), problem)
  }

  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

export function userMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 0) return 'Cannot reach the server. Check your connection.'
    if (error.status >= 500) return 'Something went wrong on our side. Please try again.'
    return error.problem?.detail ?? 'The request could not be completed.'
  }
  return 'Unexpected error.'
}
