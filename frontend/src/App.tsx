import { Link, Route, Routes } from 'react-router'
import { FolderMissing, FolderRoute } from './features/browser/FolderRoute'

export default function App() {
  return (
    <>
      <header>
        <h1>
          <Link to="/">Files</Link>
        </h1>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<FolderRoute />} />
          <Route path="/folders/:id" element={<FolderRoute />} />
          <Route path="*" element={<FolderMissing />} />
        </Routes>
      </main>
    </>
  )
}
