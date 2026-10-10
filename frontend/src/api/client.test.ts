import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch, isNotFound, userMessage } from './client'

function problem(status: number, body: unknown, requestId = 'header-id') {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/problem+json', 'X-Request-Id': requestId },
  })
}

async function failureOf(response: Response) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response))
  return apiFetch('/folders/1').catch((error: unknown) => error)
}

describe('apiFetch', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('reads code, request id and field errors from a problem', async () => {
    const error = await failureOf(
      problem(400, {
        status: 400,
        code: 'VALIDATION_FAILED',
        requestId: 'body-id',
        errors: [{ field: 'name', code: 'NAME_RESERVED', message: "must not be '.' or '..'" }],
      }),
    )

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 400, code: 'VALIDATION_FAILED', requestId: 'body-id' })
    expect(userMessage(error)).toBe('The name cannot be "." or "..".')
  })

  it('treats a 404 as not found and a 500 as a server problem', async () => {
    const notFound = await failureOf(problem(404, { status: 404, code: 'NODE_NOT_FOUND' }))
    const serverError = await failureOf(problem(500, { status: 500, detail: 'Unexpected error' }))

    expect(isNotFound(notFound)).toBe(true)
    expect(userMessage(notFound)).toBe('The folder does not exist or was deleted.')
    expect(userMessage(serverError)).toBe('Something went wrong on our side. Please try again. Reference: header-id')
  })
})
