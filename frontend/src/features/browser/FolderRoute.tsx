import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router'
import { isNotFound, userMessage } from '../../api/client'
import { log } from '../../logger'
import { CreateFolderForm } from './CreateFolderForm'
import { NodeList } from './NodeList'
import { useFolder } from './useFolder'

const FOLDER_ID = /^[1-9]\d{0,14}$/

export function FolderRoute() {
  const { id } = useParams()
  if (id === undefined) return <FolderView key="root" folderId={null} />
  if (!FOLDER_ID.test(id)) return <FolderMissing />
  return <FolderView key={id} folderId={Number(id)} />
}

export function FolderMissing() {
  return (
    <section>
      <title>Not found - File system</title>
      <p role="alert">This folder does not exist or was deleted.</p>
      <Link to="/">Go to Home</Link>
    </section>
  )
}

function FolderView({ folderId }: { folderId: number | null }) {
  const { state, retry, loadMore, reload } = useFolder(folderId)
  const [missing, setMissing] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [moreError, setMoreError] = useState<string | null>(null)
  const [focusIndex, setFocusIndex] = useState<number | null>(null)
  const headingRef = useRef<HTMLHeadingElement>(null)

  useEffect(() => {
    if (state.status === 'ready') headingRef.current?.focus()
  }, [state.status])

  if (missing) return <FolderMissing />
  if (state.status === 'loading') return <p>Loading...</p>
  if (state.status === 'failed') {
    if (isNotFound(state.error)) return <FolderMissing />
    return (
      <section>
        <p role="alert">{userMessage(state.error)}</p>
        <button type="button" onClick={retry}>
          Try again
        </button>{' '}
        <Link to="/">Go to Home</Link>
      </section>
    )
  }

  const { folder, items, nextCursor } = state

  async function handleLoadMore(cursor: string) {
    const firstNew = items.length
    setLoadingMore(true)
    setMoreError(null)
    try {
      await loadMore(folder.id, cursor)
      setFocusIndex(firstNew)
    } catch (error) {
      log.warn('Loading more items failed', { folderId: folder.id, error })
      setMoreError(userMessage(error))
    } finally {
      setLoadingMore(false)
    }
  }

  function handleCreated() {
    setMoreError(null)
    setFocusIndex(null)
    reload(folder.id).catch((error) => {
      if (isNotFound(error)) {
        setMissing(true)
        return
      }
      log.warn('Reloading folder failed', { folderId: folder.id, error })
      setMoreError(userMessage(error))
    })
  }

  return (
    <section>
      <title>{`${folder.name} - File system`}</title>
      <h2 ref={headingRef} tabIndex={-1}>
        {folder.name}
      </h2>
      <NodeList parentId={folder.parentId} items={items} focusIndex={focusIndex} />
      {items.length === 0 && <p className="empty">This folder is empty.</p>}
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
      <CreateFolderForm parentId={folder.id} onCreated={handleCreated} onParentMissing={() => setMissing(true)} />
    </section>
  )
}
