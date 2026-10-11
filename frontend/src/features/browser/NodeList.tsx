import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import type { NodeResponse } from '../../api/types'
import { iconFor } from './fileIcon'
import { RenameForm } from './RenameForm'

export interface FocusRequest {
  id: number
}

interface Props {
  items: NodeResponse[]
  focusRequest: FocusRequest | null
  deleting: ReadonlySet<number>
  onDelete: (item: NodeResponse) => void
  onRenamed: (previous: NodeResponse, renamed: NodeResponse) => void
  onGone: (item: NodeResponse) => void
}

const created = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' })

export function NodeList({ items, focusRequest, deleting, onDelete, onRenamed, onGone }: Props) {
  const listRef = useRef<HTMLUListElement>(null)
  const [editingId, setEditingId] = useState<number | null>(null)

  useEffect(() => {
    if (focusRequest === null) return
    listRef.current?.querySelector<HTMLElement>(`[data-id="${focusRequest.id}"]`)?.focus()
  }, [focusRequest])

  return (
    <ul className="node-list" ref={listRef}>
      {items.map((item) => (
        <li key={item.id} data-id={item.id} className={item.type === 'FOLDER' ? 'folder' : 'file'} tabIndex={-1}>
          {editingId === item.id ? (
            <RenameForm
              node={item}
              onRenamed={(renamed) => {
                setEditingId(null)
                onRenamed(item, renamed)
              }}
              onCancel={() => setEditingId(null)}
              onGone={() => {
                setEditingId(null)
                onGone(item)
              }}
            />
          ) : (
            <>
              <span className="entry">
                <span className="icon" aria-hidden="true">
                  {iconFor(item)}
                </span>
                {item.type === 'FOLDER' ? (
                  <Link className="name" to={`/folders/${item.id}`}>
                    {item.name}/
                  </Link>
                ) : (
                  <span className="name">{item.name}</span>
                )}
              </span>
              <time className="date" dateTime={item.createdAt}>
                {created.format(new Date(item.createdAt))}
              </time>
              <span className="actions">
                <button type="button" onClick={() => setEditingId(item.id)} aria-label={`Rename ${item.name}`}>
                  Rename
                </button>
                <button
                  type="button"
                  className="delete"
                  onClick={() => onDelete(item)}
                  disabled={deleting.has(item.id)}
                  aria-label={`Delete ${item.name}`}
                >
                  {deleting.has(item.id) ? 'Deleting...' : 'Delete'}
                </button>
              </span>
            </>
          )}
        </li>
      ))}
    </ul>
  )
}
