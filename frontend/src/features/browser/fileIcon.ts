import type { NodeResponse } from '../../api/types'

const GROUPS: [string, string[]][] = [
  ['🖼️', ['jpg', 'jpeg', 'png', 'gif', 'svg', 'webp', 'bmp']],
  ['📝', ['txt', 'md', 'doc', 'docx', 'rtf', 'odt']],
  ['📊', ['xls', 'xlsx', 'csv', 'ods']],
  ['📽️', ['ppt', 'pptx', 'odp']],
  ['📕', ['pdf']],
  ['🗜️', ['zip', 'rar', '7z', 'tar', 'gz']],
  ['🎵', ['mp3', 'wav', 'flac', 'ogg']],
  ['🎬', ['mp4', 'mov', 'avi', 'mkv', 'webm']],
  ['💻', ['js', 'ts', 'java', 'py', 'html', 'css', 'json', 'xml', 'sql']],
]

const ICONS = new Map(GROUPS.flatMap(([icon, extensions]) => extensions.map((extension) => [extension, icon])))

export function iconFor(node: Pick<NodeResponse, 'name' | 'type'>) {
  if (node.type === 'FOLDER') return '📁'
  const dot = node.name.lastIndexOf('.')
  return ICONS.get(dot > 0 ? node.name.slice(dot + 1).toLowerCase() : '') ?? '📄'
}
