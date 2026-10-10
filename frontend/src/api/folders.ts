import { apiFetch } from './client'
import type { FolderPageResponse, FolderResponse, NodeResponse } from './types'

export function getRoot(signal?: AbortSignal) {
  return apiFetch<FolderResponse>('/folders/root', { signal })
}

export function getFolder(id: number, signal?: AbortSignal) {
  return apiFetch<FolderResponse>(`/folders/${id}`, { signal })
}

export function getChildren(folderId: number, cursor: string | null, signal?: AbortSignal) {
  const params = new URLSearchParams()
  if (cursor) params.set('cursor', cursor)
  const query = params.toString()
  return apiFetch<FolderPageResponse>(`/folders/${folderId}/children${query ? `?${query}` : ''}`, { signal })
}

export function createFolder(parentId: number, name: string) {
  return apiFetch<NodeResponse>(`/folders/${parentId}/folders`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  })
}

export function createFile(parentId: number, name: string) {
  return apiFetch<NodeResponse>(`/folders/${parentId}/files`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  })
}

export function deleteNode(node: Pick<NodeResponse, 'id' | 'type'>) {
  const resource = node.type === 'FOLDER' ? 'folders' : 'files'
  return apiFetch<void>(`/${resource}/${node.id}`, { method: 'DELETE' })
}
