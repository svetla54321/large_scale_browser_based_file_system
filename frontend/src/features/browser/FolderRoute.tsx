import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router'
import { isNotFound, userMessage } from '../../api/client'
import { deleteNode } from '../../api/folders'
import type { NodeResponse } from '../../api/types'
import { log } from '../../logger'
import { Breadcrumbs } from './Breadcrumbs'
import { CreateNodeForm } from './CreateNodeForm'
import { NodeList, type FocusRequest } from './NodeList'
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
  const { state, retry, loadMore, insertItem, removeItem } = useFolder(folderId)
  const [missing, setMissing] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [moreError, setMoreError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [focusRequest, setFocusRequest] = useState<FocusRequest | null>(null)
  const [deleting, setDeleting] = useState<ReadonlySet<number>>(new Set())
  const headingRef = useRef<HTMLHeadingElement>(null)
  const shownItems = useRef<NodeResponse[]>([])

  useEffect(() => {
    if (state.status === 'ready') headingRef.current?.focus()
  }, [state.status])

  useEffect(() => {
    if (state.status === 'ready') shownItems.current = state.items
  })

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
    setLoadingMore(true)
    setMoreError(null)
    const shownBefore = new Set(items.map((item) => item.id))
    try {
      const page = await loadMore(folder.id, cursor)
      const firstNew = page.items.find((item) => !shownBefore.has(item.id))
      if (firstNew) setFocusRequest({ id: firstNew.id })
    } catch (error) {
      log.warn('Loading more items failed', { folderId: folder.id, error })
      setMoreError(userMessage(error))
    } finally {
      setLoadingMore(false)
    }
  }

  function markDeleting(id: number, busy: boolean) {
    setDeleting((current) => {
      const next = new Set(current)
      if (busy) next.add(id)
      else next.delete(id)
      return next
    })
  }

  function moveFocusAway(item: NodeResponse) {
    const current = shownItems.current
    const index = current.findIndex((shown) => shown.id === item.id)
    const neighbour = current[index + 1] ?? current[index - 1]
    if (index !== -1 && neighbour) setFocusRequest({ id: neighbour.id })
    else headingRef.current?.focus()
  }

  async function handleDelete(item: NodeResponse) {
    const kind = item.type === 'FOLDER' ? 'Folder' : 'File'
    const question =
      item.type === 'FOLDER' ? `Delete folder "${item.name}" and everything in it?` : `Delete file "${item.name}"?`
    if (!window.confirm(question)) return

    markDeleting(item.id, true)
    setMoreError(null)
    setNotice(null)
    try {
      await deleteNode(item)
    } catch (error) {
      if (!isNotFound(error)) {
        log.warn('Deleting node failed', { id: item.id, type: item.type, error })
        setMoreError(userMessage(error))
        return
      }
    } finally {
      markDeleting(item.id, false)
    }
    moveFocusAway(item)
    removeItem(item.id)
    setNotice(`${kind} "${item.name}" deleted.`)
  }

  function handleCreated(node: NodeResponse) {
    setMoreError(null)
    setNotice(null)
    insertItem(node)
  }

  return (
    <section>
      <title>{`${folder.name} - File system`}</title>
      <Breadcrumbs breadcrumbs={folder.breadcrumbs} current={folder.name} />
      <h2 ref={headingRef} tabIndex={-1}>
        {folder.name}
      </h2>
      <NodeList items={items} focusRequest={focusRequest} deleting={deleting} onDelete={handleDelete} />
      {items.length === 0 && nextCursor === null && <p className="empty">This folder is empty.</p>}
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
      <p className="status" role="status">
        {notice}
      </p>
      <div className="create">
        <CreateNodeForm type="FOLDER" parentId={folder.id} onCreated={handleCreated} onParentMissing={() => setMissing(true)} />
        <CreateNodeForm type="FILE" parentId={folder.id} onCreated={handleCreated} onParentMissing={() => setMissing(true)} />
      </div>
    </section>
  )
}
