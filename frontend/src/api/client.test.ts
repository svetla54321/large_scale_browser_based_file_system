import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch, isNameProblem, isNotFound, userMessage } from './client'

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

  it('falls back to the header request id and a generic message', async () => {
    const error = await failureOf(problem(405, { status: 405, code: 'METHOD_NOT_ALLOWED' }))

    expect(error).toMatchObject({ requestId: 'header-id', errors: [] })
    expect(userMessage(error)).toBe('The request could not be completed.')
  })

  it('maps a conflict to its message', async () => {
    const error = await failureOf(problem(409, { status: 409, code: 'NAME_ALREADY_EXISTS' }))

    expect(userMessage(error)).toBe('Something with this name already exists in this folder.')
  })

  it('ignores an errors value that is not a list', async () => {
    const error = await failureOf(problem(400, { status: 400, code: 'VALIDATION_FAILED', errors: {} }))

    expect(userMessage(error)).toBe('The request could not be completed.')
  })

  it('treats a 404 as not found and a 500 as a server problem', async () => {
    const notFound = await failureOf(problem(404, { status: 404, code: 'NODE_NOT_FOUND' }))
    const serverError = await failureOf(problem(500, { status: 500, detail: 'Unexpected error' }))

    expect(isNotFound(notFound)).toBe(true)
    expect(userMessage(notFound)).toBe('The folder does not exist or was deleted.')
    expect(userMessage(serverError)).toBe('Something went wrong on our side. Please try again.')
  })

  it('tells name problems apart from other failures', async () => {
    const duplicate = await failureOf(problem(409, { status: 409, code: 'NAME_ALREADY_EXISTS' }))
    const badName = await failureOf(
      problem(400, { status: 400, code: 'VALIDATION_FAILED', errors: [{ field: 'name', code: 'NotNull', message: 'x' }] }),
    )
    const tooDeep = await failureOf(problem(400, { status: 400, code: 'MAX_DEPTH_EXCEEDED' }))

    expect([isNameProblem(duplicate), isNameProblem(badName), isNameProblem(tooDeep)]).toEqual([true, true, false])
  })

  it('tells an unavailable backend behind the proxy apart from a server error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('<html>502 Bad Gateway</html>', { status: 502 })))

    const error = await apiFetch('/folders/1').catch((failure: unknown) => failure)

    expect(userMessage(error)).toBe('The server is not available right now. Please try again in a moment.')
  })

  it('reports an unreachable server as status 0', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    const error = await apiFetch('/folders/1').catch((failure: unknown) => failure)

    expect(error).toMatchObject({ status: 0 })
    expect(userMessage(error)).toBe('Cannot reach the server. Check your connection.')
  })
})
