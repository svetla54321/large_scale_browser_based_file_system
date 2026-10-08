import { useCallback, useEffect, useRef, useState } from 'react'
import { getChildren, getFolder, getRoot } from '../../api/folders'
import type { FolderResponse, NodeResponse } from '../../api/types'
import { log } from '../../logger'

type FolderState =
  | { status: 'loading' }
  | { status: 'failed'; error: unknown }
  | { status: 'ready'; folder: FolderResponse; items: NodeResponse[]; nextCursor: string | null }

export function useFolder(folderId: number | null) {
  const [state, setState] = useState<FolderState>({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)
  const latestReload = useRef(0)

  useEffect(() => {
    const controller = new AbortController()
    const { signal } = controller

    const folderRequest = folderId === null ? getRoot(signal) : getFolder(folderId, signal)
    folderRequest
      .then(async (folder) => {
        const page = await getChildren(folder.id, null, signal)
        setState({ status: 'ready', folder, items: page.items, nextCursor: page.nextCursor })
      })
      .catch((error) => {
        if (signal.aborted) return
        log.warn('Loading folder failed', { folderId, error })
        setState({ status: 'failed', error })
      })

    return () => controller.abort()
  }, [folderId, attempt])

  const retry = useCallback(() => {
    setState({ status: 'loading' })
    setAttempt((current) => current + 1)
  }, [])

  const loadMore = useCallback(async (id: number, cursor: string) => {
    const page = await getChildren(id, cursor)
    setState((current) =>
      current.status === 'ready' && current.folder.id === id && current.nextCursor === cursor
        ? { ...current, items: [...current.items, ...page.items], nextCursor: page.nextCursor }
        : current,
    )
  }, [])

  const reload = useCallback(async (id: number) => {
    const request = ++latestReload.current
    const page = await getChildren(id, null)
    if (request !== latestReload.current) return
    setState((current) =>
      current.status === 'ready' && current.folder.id === id
        ? { ...current, items: page.items, nextCursor: page.nextCursor }
        : current,
    )
  }, [])

  return { state, retry, loadMore, reload }
}
