import { useState, type FormEvent } from 'react'
import { isNameProblem, isNotFound, nameMessage, userMessage } from '../../api/client'
import { createFolder } from '../../api/folders'
import type { NodeResponse } from '../../api/types'
import { log } from '../../logger'
import { validateName } from '../../shared/validateName'

interface Props {
  parentId: number
  onCreated: () => void
  onParentMissing: () => void
}

interface FormError {
  message: string
  aboutName: boolean
}

export function CreateFolderForm({ parentId, onCreated, onParentMissing }: Props) {
  const [name, setName] = useState('')
  const [error, setError] = useState<FormError | null>(null)
  const [created, setCreated] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setCreated(null)
    const invalid = validateName(name)
    if (invalid) {
      setError({ message: nameMessage(invalid), aboutName: true })
      return
    }

    setSaving(true)
    setError(null)
    let folder: NodeResponse
    try {
      folder = await createFolder(parentId, name)
    } catch (failure) {
      if (isNotFound(failure)) {
        onParentMissing()
        return
      }
      log.warn('Creating folder failed', { parentId, error: failure })
      setError({ message: userMessage(failure), aboutName: isNameProblem(failure) })
      return
    } finally {
      setSaving(false)
    }
    setName('')
    setCreated(`Folder "${folder.name}" created.`)
    onCreated()
  }

  return (
    <form className="create-folder" onSubmit={handleSubmit} noValidate>
      <label htmlFor="new-folder-name">New folder</label>
      <input
        id="new-folder-name"
        value={name}
        onChange={(event) => setName(event.target.value)}
        readOnly={saving}
        aria-invalid={error?.aboutName ?? false}
        aria-describedby={error?.aboutName ? 'new-folder-error' : undefined}
        autoComplete="off"
      />
      <button type="submit" disabled={saving}>
        {saving ? 'Creating...' : 'Create'}
      </button>
      {error && (
        <p id="new-folder-error" className="error" role="alert">
          {error.message}
        </p>
      )}
      <p className="status" role="status">
        {created}
      </p>
    </form>
  )
}
