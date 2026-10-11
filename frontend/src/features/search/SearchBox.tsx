import { useId, useState, type FormEvent, type KeyboardEvent } from 'react'
import { useLocation, useMatch, useNavigate } from 'react-router'
import type { FileHitResponse } from '../../api/types'
import { splitAtPrefix } from './splitAtPrefix'
import { useSuggestions } from './useSuggestions'

type Scope = 'everywhere' | 'folder'

function searchedName(search: string) {
  return new URLSearchParams(search).get('name') ?? ''
}

export function SearchBox() {
  const navigate = useNavigate()
  const location = useLocation()
  const onSearchPage = useMatch('/search') !== null
  const currentFolderId = useMatch('/folders/:id')?.params.id
  const [input, setInput] = useState(() => (onSearchPage ? searchedName(location.search) : ''))
  const [shownLocation, setShownLocation] = useState(location.key)
  const [open, setOpen] = useState(false)
  const [activeId, setActiveId] = useState<number | null>(null)
  const [scope, setScope] = useState<Scope>('everywhere')
  const suggestions = useSuggestions(input)
  const id = useId()
  const listId = `${id}-list`
  const labelId = `${id}-label`
  const items = suggestions.items
  const active = items.findIndex((hit) => hit.id === activeId)
  const showList = open && items.length > 0

  if (shownLocation !== location.key) {
    setShownLocation(location.key)
    if (onSearchPage) setInput(searchedName(location.search))
  }

  function close() {
    setOpen(false)
    setActiveId(null)
  }

  function choose(hit: FileHitResponse) {
    close()
    setInput('')
    navigate(`/folders/${hit.parentId}`)
  }

  function moveTo(index: number) {
    setActiveId(index >= 0 ? items[index].id : null)
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const name = input.trim()
    if (name.length === 0) return
    const params = new URLSearchParams({ name })
    if (scope === 'folder' && currentFolderId) params.set('folderId', currentFolderId)
    close()
    navigate(`/search?${params}`)
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'ArrowDown' && items.length > 0) {
      event.preventDefault()
      setOpen(true)
      moveTo(Math.min(active + 1, items.length - 1))
    } else if (event.key === 'ArrowUp' && showList) {
      event.preventDefault()
      moveTo(Math.max(active - 1, -1))
    } else if (event.key === 'Escape' && open) {
      event.preventDefault()
      close()
    } else if (event.key === 'Enter' && showList && active >= 0) {
      event.preventDefault()
      choose(items[active])
    }
  }

  return (
    <form role="search" className="search-box" onSubmit={handleSubmit}>
      <label id={labelId} htmlFor={`${id}-input`} className="visually-hidden">
        Search files
      </label>
      <input
        id={`${id}-input`}
        type="text"
        placeholder="Search files"
        value={input}
        onChange={(event) => {
          setInput(event.target.value)
          setOpen(true)
          setActiveId(null)
        }}
        onKeyDown={handleKeyDown}
        onBlur={close}
        role="combobox"
        aria-autocomplete="list"
        aria-expanded={showList}
        aria-controls={listId}
        aria-activedescendant={showList && active >= 0 ? `${id}-option-${activeId}` : undefined}
        autoComplete="off"
      />
      {currentFolderId && (
        <select aria-label="Where to search" value={scope} onChange={(event) => setScope(event.target.value as Scope)}>
          <option value="everywhere">Everywhere</option>
          <option value="folder">This folder</option>
        </select>
      )}
      <button type="submit">Search</button>
      <ul id={listId} role="listbox" aria-labelledby={labelId} className="suggestions" hidden={!showList}>
        {items.map((hit, index) => {
          const [typed, rest] = splitAtPrefix(hit.name, suggestions.prefix)
          return (
            <li
              key={hit.id}
              id={`${id}-option-${hit.id}`}
              role="option"
              aria-selected={index === active}
              aria-label={`${hit.name} in ${hit.folderPath}`}
              onMouseDown={(event) => {
                if (event.button !== 0) return
                event.preventDefault()
                choose(hit)
              }}
            >
              <span className="name">
                <strong>{typed}</strong>
                {rest}
              </span>
              <span className="path">{hit.folderPath}</span>
            </li>
          )
        })}
      </ul>
      {open && suggestions.prefix !== '' && items.length === 0 && (
        <p className="suggestions no-suggestions" role="status">
          No files start with "{suggestions.prefix.trim()}"
        </p>
      )}
    </form>
  )
}
