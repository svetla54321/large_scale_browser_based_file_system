import { Link, Route, Routes } from 'react-router'
import { FolderMissing, FolderRoute } from './features/browser/FolderRoute'
import { SearchBox } from './features/search/SearchBox'
import { SearchResultsPage } from './features/search/SearchResultsPage'

export default function App() {
  return (
    <>
      <header>
        <h1>
          <Link to="/">Files</Link>
        </h1>
        <SearchBox />
      </header>
      <main>
        <Routes>
          <Route path="/" element={<FolderRoute />} />
          <Route path="/folders/:id" element={<FolderRoute />} />
          <Route path="/search" element={<SearchResultsPage />} />
          <Route path="*" element={<FolderMissing />} />
        </Routes>
      </main>
    </>
  )
}
