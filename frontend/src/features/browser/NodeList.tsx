import { useEffect, useRef } from 'react'
import { Link } from 'react-router'
import type { NodeResponse } from '../../api/types'

export interface FocusRequest {
  id: number
}

interface Props {
  items: NodeResponse[]
  focusRequest: FocusRequest | null
  deleting: ReadonlySet<number>
  onDelete: (item: NodeResponse) => void
}

export function NodeList({ items, focusRequest, deleting, onDelete }: Props) {
  const listRef = useRef<HTMLUListElement>(null)

  useEffect(() => {
    if (focusRequest === null) return
    listRef.current?.querySelector<HTMLElement>(`[data-id="${focusRequest.id}"]`)?.focus()
  }, [focusRequest])

  return (
    <ul className="node-list" ref={listRef}>
      {items.map((item) => (
        <li key={item.id} data-id={item.id} className={item.type === 'FOLDER' ? 'folder' : 'file'} tabIndex={-1}>
          {item.type === 'FOLDER' ? <Link to={`/folders/${item.id}`}>{item.name}/</Link> : <span>{item.name}</span>}
          <button
            type="button"
            className="delete"
            onClick={() => onDelete(item)}
            disabled={deleting.has(item.id)}
            aria-label={`Delete ${item.name}`}
          >
            {deleting.has(item.id) ? 'Deleting...' : 'Delete'}
          </button>
        </li>
      ))}
    </ul>
  )
}
