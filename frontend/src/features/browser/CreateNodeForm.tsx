import { useState, type FormEvent } from 'react'
import { isNameProblem, isNotFound, nameMessage, userMessage } from '../../api/client'
import { createFile, createFolder } from '../../api/folders'
import type { NodeResponse, NodeType } from '../../api/types'
import { log } from '../../logger'
import { validateName } from '../../shared/validateName'

interface Props {
  type: NodeType
  parentId: number
  onCreated: (node: NodeResponse) => void
  onParentMissing: () => void
}

interface FormError {
  message: string
  aboutName: boolean
}

const LABELS: Record<NodeType, string> = { FOLDER: 'Folder', FILE: 'File' }

export function CreateNodeForm({ type, parentId, onCreated, onParentMissing }: Props) {
  const [name, setName] = useState('')
  const [error, setError] = useState<FormError | null>(null)
  const [created, setCreated] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const inputId = `new-${type.toLowerCase()}-name`
  const errorId = `${inputId}-error`

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
    let node: NodeResponse
    try {
      node = type === 'FOLDER' ? await createFolder(parentId, name) : await createFile(parentId, name)
    } catch (failure) {
      if (isNotFound(failure)) {
        onParentMissing()
        return
      }
      log.warn('Creating node failed', { type, parentId, error: failure })
      setError({ message: userMessage(failure), aboutName: isNameProblem(failure) })
      return
    } finally {
      setSaving(false)
    }
    setName('')
    setCreated(`${LABELS[type]} "${node.name}" created.`)
    onCreated(node)
  }

  return (
    <form className="create-node" onSubmit={handleSubmit} noValidate>
      <label htmlFor={inputId}>New {LABELS[type].toLowerCase()}</label>
      <input
        id={inputId}
        value={name}
        onChange={(event) => setName(event.target.value)}
        readOnly={saving}
        aria-invalid={error?.aboutName ?? false}
        aria-describedby={error?.aboutName ? errorId : undefined}
        autoComplete="off"
      />
      <button type="submit" disabled={saving}>
        {saving ? 'Creating...' : 'Create'}
      </button>
      {error && (
        <p id={errorId} className="error" role="alert">
          {error.message}
        </p>
      )}
      <p className="status" role="status">
        {created}
      </p>
    </form>
  )
}
