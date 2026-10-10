export type NodeType = 'FOLDER' | 'FILE'

export interface BreadcrumbResponse {
  id: number
  name: string
}

export interface FolderResponse {
  id: number
  name: string
  parentId: number | null
  breadcrumbs: BreadcrumbResponse[]
}

export interface NodeResponse {
  id: number
  name: string
  type: NodeType
  parentId: number
  createdAt: string
}

export interface FolderPageResponse {
  items: NodeResponse[]
  nextCursor: string | null
}

export interface FileHitResponse {
  id: number
  name: string
  parentId: number
  folderPath: string
}

export interface SearchPageResponse {
  items: FileHitResponse[]
  nextCursor: string | null
}

export interface SuggestionsResponse {
  prefix: string
  items: FileHitResponse[]
}
