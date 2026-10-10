import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { isNotFound, userMessage } from '../../api/client'
import { log } from '../../logger'
import { useSearchResults } from './useSearchResults'

const FOLDER_ID = /^[1-9]\d{0,14}$/

export function SearchResultsPage() {
  const [params] = useSearchParams()
  const name = params.get('name')?.trim() ?? ''
  const rawFolderId = params.get('folderId')
  const folderId = rawFolderId && FOLDER_ID.test(rawFolderId) ? Number(rawFolderId) : null

  if (name.length === 0) {
    return (
      <section>
        <title>Search - File system</title>
        <p>Type a file name in the search box and press Enter.</p>
      </section>
    )
  }
  return <SearchResults key={`${name}:${folderId}`} name={name} folderId={folderId} />
}

function SearchResults({ name, folderId }: { name: string; folderId: number | null }) {
  const { state, loadMore } = useSearchResults(name, folderId)
  const [loadingMore, setLoadingMore] = useState(false)
  const [moreError, setMoreError] = useState<string | null>(null)
  const [focusId, setFocusId] = useState<{ id: number } | null>(null)
  const headingRef = useRef<HTMLHeadingElement>(null)
  const listRef = useRef<HTMLUListElement>(null)

  useEffect(() => {
    if (state.status === 'ready') headingRef.current?.focus()
  }, [state.status])

  useEffect(() => {
    if (focusId === null) return
    listRef.current?.querySelector<HTMLElement>(`[data-id="${focusId.id}"]`)?.focus()
  }, [focusId])

  if (state.status === 'loading') return <p>Searching...</p>
  if (state.status === 'failed') {
    const message = isNotFound(state.error)
      ? 'The folder you searched in does not exist or was deleted.'
      : userMessage(state.error)
    return (
      <section>
        <p role="alert">{message}</p>
        <Link to="/">Go to Home</Link>
      </section>
    )
  }

  const { items, nextCursor } = state

  async function handleLoadMore(cursor: string) {
    setLoadingMore(true)
    setMoreError(null)
    try {
      const page = await loadMore(cursor)
      if (page.items.length > 0) setFocusId({ id: page.items[0].id })
    } catch (error) {
      log.warn('Loading more results failed', { error })
      setMoreError(userMessage(error))
    } finally {
      setLoadingMore(false)
    }
  }

  return (
    <section>
      <title>{`${name} - Search - File system`}</title>
      <h2 ref={headingRef} tabIndex={-1}>
        Files named "{name}"{folderId !== null && ' in this folder and below'}
      </h2>
      {items.length === 0 ? (
        <p className="empty">No file has this exact name. Names must match fully, for example report.pdf.</p>
      ) : (
        <ul className="results" ref={listRef}>
          {items.map((hit) => (
            <li key={hit.id} data-id={hit.id} tabIndex={-1}>
              <span className="name">{hit.name}</span> in <Link to={`/folders/${hit.parentId}`}>{hit.folderPath}</Link>
            </li>
          ))}
        </ul>
      )}
      {nextCursor && (
        <button type="button" onClick={() => handleLoadMore(nextCursor)} disabled={loadingMore}>
          {loadingMore ? 'Loading...' : 'Load more'}
        </button>
      )}
      {moreError && (
        <p className="error" role="alert">
          {moreError}
        </p>
      )}
    </section>
  )
}
