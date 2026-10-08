import { describe, expect, it } from 'vitest'
import { validateName } from './validateName'

describe('validateName', () => {
  it.each([
    ['', 'NAME_REQUIRED'],
    ['   ', 'NAME_REQUIRED'],
    ['.', 'NAME_RESERVED'],
    [' .. ', 'NAME_RESERVED'],
    ['a/b', 'NAME_FORBIDDEN_CHARACTER'],
    ['report?.pdf', 'NAME_FORBIDDEN_CHARACTER'],
    ['x'.repeat(256), 'NAME_TOO_LONG'],
  ])('rejects %j with %s', (name, code) => {
    expect(validateName(name)).toBe(code)
  })

  it('counts characters, not UTF-16 units', () => {
    expect(validateName(String.fromCodePoint(0x1f600).repeat(255))).toBeNull()
  })

  it('counts characters after composing accents, like the server', () => {
    const decomposed = String.fromCodePoint(0x65, 0x301).repeat(200)

    expect(validateName(decomposed)).toBeNull()
  })

  it('ignores invisible format characters around the name, like the server', () => {
    expect(validateName('a'.repeat(255) + String.fromCodePoint(0x200b))).toBeNull()
  })

  it('accepts a normal name with spaces and dots', () => {
    expect(validateName('  Quarterly report 2026.v2  ')).toBeNull()
  })
})
