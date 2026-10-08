export const API_BASE = '/api/v1'

interface FieldViolation {
  field: string
  code: string
  message: string
}

interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  code?: string
  requestId?: string
  errors?: FieldViolation[]
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string | undefined
  readonly requestId: string | null
  readonly errors: FieldViolation[]
  readonly problem: ProblemDetail | undefined

  constructor(status: number, requestId: string | null, problem?: ProblemDetail) {
    super(problem?.detail ?? problem?.title ?? `HTTP ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.code = typeof problem?.code === 'string' ? problem.code : undefined
    this.requestId = typeof problem?.requestId === 'string' ? problem.requestId : requestId
    this.errors = Array.isArray(problem?.errors) ? problem.errors : []
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

const MESSAGES = new Map([
  ['NODE_NOT_FOUND', 'The folder does not exist or was deleted.'],
  ['NAME_ALREADY_EXISTS', 'Something with this name already exists in this folder.'],
  ['CONCURRENT_CHANGE', 'The folder was changed at the same time. Reload and try again.'],
  ['MAX_DEPTH_EXCEEDED', 'Folders cannot be nested any deeper.'],
  ['INVALID_CURSOR', 'Could not load more items. Reload the folder.'],
  ['NAME_REQUIRED', 'Enter a name.'],
  ['NAME_TOO_LONG', 'The name can have at most 255 characters.'],
  ['NAME_RESERVED', 'The name cannot be "." or "..".'],
  ['NAME_FORBIDDEN_CHARACTER', 'The name cannot contain \\ / : * ? " < > |'],
  ['NAME_INVISIBLE_CHARACTER', 'The name contains an invisible, control or unsupported character.'],
])

export function userMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 0) return 'Cannot reach the server. Check your connection.'
    if (error.status >= 500) return 'Something went wrong on our side. Please try again.'
    const code = error.errors[0]?.code ?? error.code
    return (code && MESSAGES.get(code)) || 'The request could not be completed.'
  }
  return 'Unexpected error.'
}
