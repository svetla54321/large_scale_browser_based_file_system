import { useEffect, useState } from 'react'
import { suggest } from '../../api/search'
import type { SuggestionsResponse } from '../../api/types'
import { log } from '../../logger'
import { useDebouncedValue } from '../../shared/useDebouncedValue'

const DEBOUNCE_MS = 250
const NOTHING: SuggestionsResponse = { prefix: '', items: [] }

export function useSuggestions(input: string): SuggestionsResponse {
  const prefix = useDebouncedValue(input, DEBOUNCE_MS)
  const [latest, setLatest] = useState<SuggestionsResponse>(NOTHING)

  useEffect(() => {
    if (prefix.trim().length === 0) return
    const controller = new AbortController()

    suggest(prefix, controller.signal)
      .then((response) => {
        if (!controller.signal.aborted) setLatest(response)
      })
      .catch((error) => {
        if (controller.signal.aborted) return
        log.debug('Loading suggestions failed', { error })
        setLatest({ prefix, items: [] })
      })

    return () => controller.abort()
  }, [prefix])

  return input.trim().length > 0 && latest.prefix === prefix ? latest : NOTHING
}
