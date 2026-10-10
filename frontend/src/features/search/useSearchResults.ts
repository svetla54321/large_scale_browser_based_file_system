import { useCallback, useEffect, useState } from 'react'
import { searchFiles } from '../../api/search'
import type { FileHitResponse } from '../../api/types'
import { log } from '../../logger'

type SearchState =
  | { status: 'loading' }
  | { status: 'failed'; error: unknown }
  | { status: 'ready'; items: FileHitResponse[]; nextCursor: string | null }

export function useSearchResults(name: string, folderId: number | null) {
  const [state, setState] = useState<SearchState>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()

    searchFiles(name, folderId, null, controller.signal)
      .then((page) => setState({ status: 'ready', items: page.items, nextCursor: page.nextCursor }))
      .catch((error) => {
        if (controller.signal.aborted) return
        log.warn('Searching files failed', { folderId, error })
        setState({ status: 'failed', error })
      })

    return () => controller.abort()
  }, [name, folderId])

  const loadMore = useCallback(
    async (cursor: string) => {
      const page = await searchFiles(name, folderId, cursor)
      setState((current) =>
        current.status === 'ready' && current.nextCursor === cursor
          ? { status: 'ready', items: [...current.items, ...page.items], nextCursor: page.nextCursor }
          : current,
      )
      return page
    },
    [name, folderId],
  )

  return { state, loadMore }
}
