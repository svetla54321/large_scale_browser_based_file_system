import { apiFetch } from './client'
import type { SearchPageResponse, SuggestionsResponse } from './types'

export function searchFiles(name: string, folderId: number | null, cursor: string | null, signal?: AbortSignal) {
  const params = new URLSearchParams({ name })
  if (folderId !== null) params.set('folderId', String(folderId))
  if (cursor) params.set('cursor', cursor)
  return apiFetch<SearchPageResponse>(`/search/files?${params}`, { signal })
}

export function suggest(prefix: string, signal?: AbortSignal) {
  return apiFetch<SuggestionsResponse>(`/search/suggestions?${new URLSearchParams({ prefix })}`, { signal })
}
