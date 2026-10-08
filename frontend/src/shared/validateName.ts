import type { NameRule } from '../api/client'

const MAX_LENGTH = 255
const SURROUNDING_BLANKS = /^[\s\p{Z}\p{Cf}]+|[\s\p{Z}\p{Cf}]+$/gu
const FORBIDDEN_CHARACTER = /[\\/:*?"<>|]/

export function validateName(raw: string): NameRule | null {
  const name = raw.replace(SURROUNDING_BLANKS, '').normalize('NFC')
  if (name.length === 0) return 'NAME_REQUIRED'
  if ([...name].length > MAX_LENGTH) return 'NAME_TOO_LONG'
  if (name === '.' || name === '..') return 'NAME_RESERVED'
  if (FORBIDDEN_CHARACTER.test(name)) return 'NAME_FORBIDDEN_CHARACTER'
  return null
}
