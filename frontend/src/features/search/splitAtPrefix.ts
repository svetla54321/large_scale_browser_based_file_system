const LEADING_BLANKS = /^[\s\p{Cf}]+/u
const graphemes = new Intl.Segmenter(undefined, { granularity: 'grapheme' })

export function splitAtPrefix(name: string, prefix: string): [string, string] {
  const wanted = Array.from(prefix.normalize('NFC').replace(LEADING_BLANKS, '')).length
  let taken = 0
  let cut = 0
  for (const { segment, index } of graphemes.segment(name)) {
    if (taken >= wanted) break
    taken += Array.from(segment).length
    cut = index + segment.length
  }
  return [name.slice(0, cut), name.slice(cut)]
}
