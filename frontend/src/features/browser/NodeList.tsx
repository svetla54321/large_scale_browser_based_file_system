import { useEffect, useRef } from 'react'
import { Link } from 'react-router'
import type { NodeResponse } from '../../api/types'

interface Props {
  parentId: number | null
  items: NodeResponse[]
  focusIndex: number | null
}

export function NodeList({ parentId, items, focusIndex }: Props) {
  const listRef = useRef<HTMLUListElement>(null)
  const offset = parentId === null ? 0 : 1

  useEffect(() => {
    if (focusIndex === null) return
    const item = listRef.current?.children[focusIndex + offset]
    if (item instanceof HTMLElement) item.focus()
  }, [focusIndex, offset])

  return (
    <ul className="node-list" ref={listRef}>
      {parentId !== null && (
        <li>
          <Link to={`/folders/${parentId}`} aria-label="Parent folder">
            ..
          </Link>
        </li>
      )}
      {items.map((item) => (
        <li key={item.id} className={item.type === 'FOLDER' ? 'folder' : 'file'} tabIndex={-1}>
          {item.type === 'FOLDER' ? <Link to={`/folders/${item.id}`}>{item.name}/</Link> : item.name}
        </li>
      ))}
    </ul>
  )
}
