import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { createFile, createFolder } from '../../api/folders'
import { CreateNodeForm } from './CreateNodeForm'

vi.mock('../../api/folders', () => ({ createFolder: vi.fn(), createFile: vi.fn() }))

function submit(label: RegExp, name: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value: name } })
  fireEvent.click(screen.getByRole('button', { name: 'Create' }))
}

describe('CreateNodeForm', () => {
  afterEach(() => {
    cleanup()
    vi.mocked(createFolder).mockReset()
    vi.mocked(createFile).mockReset()
  })

  it('shows a duplicate name next to the field', async () => {
    vi.mocked(createFolder).mockRejectedValue(
      new ApiError(409, 'r-1', { status: 409, code: 'NAME_ALREADY_EXISTS', detail: 'exists' }),
    )
    render(<CreateNodeForm type="FOLDER" parentId={1} onCreated={vi.fn()} onParentMissing={vi.fn()} />)

    submit(/new folder/i, 'Docs')

    expect(await screen.findByRole('alert')).toHaveProperty(
      'textContent',
      'Something with this name already exists in this folder.',
    )
    expect(screen.getByLabelText(/new folder/i).getAttribute('aria-invalid')).toBe('true')
  })

  it('rejects a forbidden character without calling the server', () => {
    render(<CreateNodeForm type="FILE" parentId={1} onCreated={vi.fn()} onParentMissing={vi.fn()} />)

    submit(/new file/i, 'a/b.txt')

    expect(screen.getByRole('alert').textContent).toBe('The name cannot contain \\ / : * ? " < > |')
    expect(createFile).not.toHaveBeenCalled()
  })
})
