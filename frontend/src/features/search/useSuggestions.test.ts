import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { suggest } from '../../api/search'
import type { FileHitResponse, SuggestionsResponse } from '../../api/types'
import { useSuggestions } from './useSuggestions'

vi.mock('../../api/search', () => ({ suggest: vi.fn() }))

const suggestMock = vi.mocked(suggest)

function hit(name: string): FileHitResponse {
  return { id: name.length, name, parentId: 1, folderPath: '/' }
}

function answer(prefix: string, ...names: string[]): SuggestionsResponse {
  return { prefix, items: names.map(hit) }
}

function names(response: SuggestionsResponse) {
  return response.items.map((item) => item.name)
}

describe('useSuggestions', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    suggestMock.mockReset()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('asks the server once the user stops typing for 250 ms', async () => {
    suggestMock.mockResolvedValue(answer('re', 'report.pdf'))
    const { result, rerender } = renderHook(({ input }) => useSuggestions(input), { initialProps: { input: '' } })

    rerender({ input: 'r' })
    await act(() => vi.advanceTimersByTimeAsync(100))
    rerender({ input: 're' })
    await act(() => vi.advanceTimersByTimeAsync(249))
    expect(suggestMock).not.toHaveBeenCalled()

    await act(() => vi.advanceTimersByTimeAsync(1))
    expect(suggestMock).toHaveBeenCalledTimes(1)
    expect(suggestMock).toHaveBeenCalledWith('re', expect.any(AbortSignal))
    expect(result.current).toEqual(answer('re', 'report.pdf'))
  })

  it('aborts the previous request and ignores an answer for older input', async () => {
    let answerOld: (response: SuggestionsResponse) => void = () => {}
    suggestMock
      .mockImplementationOnce(() => new Promise((resolve) => (answerOld = resolve)))
      .mockResolvedValueOnce(answer('rep', 'report.pdf'))
    const { result, rerender } = renderHook(({ input }) => useSuggestions(input), { initialProps: { input: '' } })
    rerender({ input: 're' })
    await act(() => vi.advanceTimersByTimeAsync(250))
    const firstSignal = suggestMock.mock.calls[0][1]

    rerender({ input: 'rep' })
    await act(() => vi.advanceTimersByTimeAsync(250))
    await act(async () => answerOld(answer('re', 'readme.md')))

    expect(firstSignal?.aborted).toBe(true)
    expect(names(result.current)).toEqual(['report.pdf'])
  })

  it('shows nothing for a new prefix until its own answer arrives', async () => {
    let answerNew: (response: SuggestionsResponse) => void = () => {}
    suggestMock
      .mockResolvedValueOnce(answer('re', 'readme.md', 'report.pdf'))
      .mockImplementationOnce(() => new Promise((resolve) => (answerNew = resolve)))
    const { result, rerender } = renderHook(({ input }) => useSuggestions(input), { initialProps: { input: '' } })
    rerender({ input: 're' })
    await act(() => vi.advanceTimersByTimeAsync(250))
    expect(names(result.current)).toEqual(['readme.md', 'report.pdf'])

    rerender({ input: 'rep' })
    expect(names(result.current)).toEqual(['readme.md', 'report.pdf'])
    await act(() => vi.advanceTimersByTimeAsync(250))
    expect(result.current.items).toEqual([])

    await act(async () => answerNew(answer('rep', 'report.pdf')))
    expect(result.current).toEqual(answer('rep', 'report.pdf'))
  })
})
