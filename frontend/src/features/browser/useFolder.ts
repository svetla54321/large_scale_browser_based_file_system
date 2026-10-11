import { useCallback, useEffect, useState } from 'react'
import { getChildren, getFolder, getRoot } from '../../api/folders'
import type { FolderResponse, NodeResponse } from '../../api/types'
import { log } from '../../logger'

type FolderState =
  | { status: 'loading' }
  | { status: 'failed'; error: unknown }
  | { status: 'ready'; folder: FolderResponse; items: NodeResponse[]; nextCursor: string | null; totalItems: number }

const names = new Intl.Collator('und')

export function compareNodes(a: NodeResponse, b: NodeResponse) {
  if (a.type !== b.type) return a.type === 'FOLDER' ? -1 : 1
  return names.compare(a.name, b.name)
}

export function useFolder(folderId: number | null) {
  const [state, setState] = useState<FolderState>({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    const { signal } = controller

    const folderRequest = folderId === null ? getRoot(signal) : getFolder(folderId, signal)
    folderRequest
      .then(async (folder) => {
        const page = await getChildren(folder.id, null, signal)
        setState({ status: 'ready', folder, items: page.items, nextCursor: page.nextCursor, totalItems: page.totalItems })
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
    setState((current) => {
      if (current.status !== 'ready' || current.folder.id !== id || current.nextCursor !== cursor) return current
      const shown = new Set(current.items.map((item) => item.id))
      const added = page.items.filter((item) => !shown.has(item.id))
      return { ...current, items: [...current.items, ...added], nextCursor: page.nextCursor, totalItems: page.totalItems }
    })
    return page
  }, [])

  const insertItem = useCallback((node: NodeResponse) => {
    setState((current) => {
      if (current.status !== 'ready' || current.items.some((item) => item.id === node.id)) return current
      const last = current.items.at(-1)
      const totalItems = current.totalItems + 1
      if (current.nextCursor !== null && (last === undefined || compareNodes(node, last) > 0)) return { ...current, totalItems }
      const index = current.items.findIndex((item) => compareNodes(node, item) < 0)
      const items = index === -1 ? [...current.items, node] : current.items.toSpliced(index, 0, node)
      return { ...current, items, totalItems }
    })
  }, [])

  const removeItem = useCallback((id: number) => {
    setState((current) => {
      if (current.status !== 'ready' || !current.items.some((item) => item.id === id)) return current
      return { ...current, items: current.items.filter((item) => item.id !== id), totalItems: current.totalItems - 1 }
    })
  }, [])

  return { state, retry, loadMore, insertItem, removeItem }
}
