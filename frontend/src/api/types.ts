export type NodeType = 'FOLDER' | 'FILE'

export interface FolderResponse {
  id: number
  name: string
  parentId: number | null
}

export interface NodeResponse {
  id: number
  name: string
  type: NodeType
  parentId: number
  createdAt: string
}

export interface FolderPage {
  items: NodeResponse[]
  nextCursor: string | null
}
