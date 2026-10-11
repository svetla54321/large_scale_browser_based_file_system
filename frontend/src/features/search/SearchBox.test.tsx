import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { FileHitResponse, SuggestionsResponse } from '../../api/types'
import { SearchBox } from './SearchBox'
import { useSuggestions } from './useSuggestions'

vi.mock('./useSuggestions', () => ({ useSuggestions: vi.fn() }))

const REPORT: FileHitResponse = { id: 5, name: 'report.pdf', parentId: 7, folderPath: '/Docs' }
const REPLY: FileHitResponse = { id: 6, name: 'reply.txt', parentId: 8, folderPath: '/Mail' }

function suggestions(prefix: string, ...items: FileHitResponse[]): SuggestionsResponse {
  return { prefix, items }
}

function CurrentLocation() {
  const location = useLocation()
  return <output aria-label="location">{location.pathname + location.search}</output>
}

function page(path: string) {
  return (
    <MemoryRouter initialEntries={[path]}>
      <SearchBox />
      <Routes>
        <Route path="*" element={<CurrentLocation />} />
      </Routes>
    </MemoryRouter>
  )
}

function input() {
  return screen.getByRole<HTMLInputElement>('combobox', { name: 'Search files' })
}

function location() {
  return screen.getByRole('status', { name: 'location' }).textContent
}

function type(text: string) {
  fireEvent.change(input(), { target: { value: text } })
}

function press(key: string) {
  fireEvent.keyDown(input(), { key })
}

describe('SearchBox', () => {
  beforeEach(() => {
    vi.mocked(useSuggestions).mockReturnValue(suggestions('rep', REPORT, REPLY))
  })

  afterEach(() => {
    cleanup()
    vi.mocked(useSuggestions).mockReset()
  })

  it('moves through suggestions with the arrow keys and opens the folder of the chosen one', () => {
    render(page('/'))
    type('rep')

    press('ArrowDown')
    press('ArrowDown')
    press('ArrowUp')

    const report = screen.getByRole('option', { name: 'report.pdf in /Docs' })
    expect(report.getAttribute('aria-selected')).toBe('true')
    expect(input().getAttribute('aria-activedescendant')).toBe(report.id)

    press('Enter')

    expect(location()).toBe('/folders/7')
    expect(input().value).toBe('')
    expect(screen.queryByRole('listbox')).toBeNull()
  })

  it('searches in the current folder when asked', () => {
    render(page('/folders/2'))
    fireEvent.change(screen.getByRole('combobox', { name: 'Where to search' }), { target: { value: 'folder' } })
    type('report.pdf')

    fireEvent.submit(input().form!)

    expect(location()).toBe('/search?name=report.pdf&folderId=2')
  })

  it('forgets the highlighted suggestion when a new list arrives', () => {
    const view = render(page('/'))
    type('re')
    press('ArrowDown')
    press('ArrowDown')

    vi.mocked(useSuggestions).mockReturnValue(suggestions('rep', REPORT))
    view.rerender(page('/'))
    press('Enter')

    expect(input().getAttribute('aria-activedescendant')).toBeNull()
    expect(screen.getByRole('option').getAttribute('aria-selected')).toBe('false')
    expect(location()).toBe('/')
  })
})
