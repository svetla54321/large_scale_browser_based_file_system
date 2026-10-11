import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { createFile, deleteNode, getChildren, getFolder } from '../../api/folders'
import type { NodeResponse } from '../../api/types'
import { FolderRoute } from './FolderRoute'

vi.mock('../../api/folders', () => ({
  getRoot: vi.fn(),
  getFolder: vi.fn(),
  getChildren: vi.fn(),
  deleteNode: vi.fn(),
  createFolder: vi.fn(),
  createFile: vi.fn(),
  renameNode: vi.fn(),
}))

function node(id: number, name: string, type: NodeResponse['type'] = 'FILE'): NodeResponse {
  return { id, name, type, parentId: 2, createdAt: '2026-10-11T08:00:00Z' }
}

const WORK = node(3, 'Work', 'FOLDER')
const NOTES = node(4, 'notes.txt')

function renderFolder(id: number) {
  return render(
    <MemoryRouter initialEntries={[`/folders/${id}`]}>
      <Routes>
        <Route path="/folders/:id" element={<FolderRoute />} />
      </Routes>
    </MemoryRouter>,
  )
}

function rowOf(text: string) {
  return screen.getByText(text).closest('li')
}

function rows() {
  return Array.from(document.querySelectorAll('.node-list .name'), (name) => name.textContent)
}

async function createFileNamed(name: string) {
  const field = await screen.findByLabelText('New file')
  fireEvent.change(field, { target: { value: name } })
  fireEvent.click(within(field.closest('form')!).getByRole('button', { name: 'Create' }))
  await screen.findByText(`File "${name}" created.`)
}

describe('FolderRoute', () => {
  beforeEach(() => {
    vi.mocked(getFolder).mockResolvedValue({ id: 2, name: 'Docs', parentId: 1, breadcrumbs: [{ id: 1, name: 'Home' }] })
    vi.mocked(getChildren).mockResolvedValue({ items: [WORK, NOTES], nextCursor: null, totalItems: 2 })
    vi.spyOn(window, 'confirm').mockReturnValue(true)
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
    vi.mocked(deleteNode).mockReset()
    vi.mocked(getChildren).mockReset()
    vi.mocked(createFile).mockReset()
  })

  it('asks before deleting a folder, deletes that folder and moves focus to the next row', async () => {
    vi.mocked(deleteNode).mockResolvedValue(undefined)
    renderFolder(2)

    fireEvent.click(await screen.findByRole('button', { name: 'Delete Work' }))

    expect(window.confirm).toHaveBeenCalledWith('Delete folder "Work" and everything in it?')
    expect(deleteNode).toHaveBeenCalledWith(expect.objectContaining({ id: 3, type: 'FOLDER' }))
    expect(await screen.findByText('Folder "Work" deleted.')).toBeTruthy()
    expect(screen.queryByText('Work/')).toBeNull()
    expect(document.activeElement).toBe(rowOf('notes.txt'))
  })

  it('treats an item that is already gone as deleted', async () => {
    vi.mocked(deleteNode).mockRejectedValue(new ApiError(404, null, { status: 404, code: 'NODE_NOT_FOUND' }))
    renderFolder(2)

    fireEvent.click(await screen.findByRole('button', { name: 'Delete notes.txt' }))

    await waitFor(() => expect(screen.queryByText('notes.txt')).toBeNull())
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('puts a new file in its place among the loaded rows', async () => {
    vi.mocked(createFile).mockResolvedValue(node(9, 'm.txt'))
    renderFolder(2)

    await createFileNamed('m.txt')

    expect(rows()).toEqual(['Work/', 'm.txt', 'notes.txt'])
  })
})
