import { useEffect, useRef, useState, type FormEvent } from 'react'
import { isNotFound, nameMessage, userMessage } from '../../api/client'
import { renameNode } from '../../api/folders'
import type { NodeResponse } from '../../api/types'
import { log } from '../../logger'
import { validateName } from '../../shared/validateName'

interface Props {
  node: NodeResponse
  onRenamed: (renamed: NodeResponse) => void
  onCancel: () => void
  onGone: () => void
}

export function RenameForm({ node, onRenamed, onCancel, onGone }: Props) {
  const [name, setName] = useState(node.name)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)
  const inputId = `rename-${node.id}`
  const errorId = `${inputId}-error`

  useEffect(() => {
    inputRef.current?.focus()
    inputRef.current?.select()
  }, [])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (name === node.name) {
      onCancel()
      return
    }
    const invalid = validateName(name)
    if (invalid) {
      setError(nameMessage(invalid))
      return
    }

    setSaving(true)
    setError(null)
    try {
      onRenamed(await renameNode(node, name))
    } catch (failure) {
      if (isNotFound(failure)) {
        onGone()
        return
      }
      log.warn('Renaming node failed', { id: node.id, type: node.type, error: failure })
      setError(userMessage(failure))
      setSaving(false)
    }
  }

  return (
    <form className="rename" onSubmit={handleSubmit} noValidate>
      <label htmlFor={inputId} className="visually-hidden">
        New name for {node.name}
      </label>
      <input
        ref={inputRef}
        id={inputId}
        value={name}
        onChange={(event) => setName(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === 'Escape') onCancel()
        }}
        readOnly={saving}
        aria-invalid={error !== null}
        aria-describedby={error ? errorId : undefined}
        autoComplete="off"
      />
      <button type="submit" disabled={saving}>
        {saving ? 'Saving...' : 'Save'}
      </button>
      <button type="button" onClick={onCancel}>
        Cancel
      </button>
      {error && (
        <p id={errorId} className="error" role="alert">
          {error}
        </p>
      )}
    </form>
  )
}
