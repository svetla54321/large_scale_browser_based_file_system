import { Link } from 'react-router'
import type { BreadcrumbResponse } from '../../api/types'

interface Props {
  breadcrumbs: BreadcrumbResponse[]
  current: string
}

export function Breadcrumbs({ breadcrumbs, current }: Props) {
  return (
    <nav aria-label="Breadcrumb">
      <ol className="breadcrumbs">
        {breadcrumbs.map((crumb) => (
          <li key={crumb.id}>
            <Link to={`/folders/${crumb.id}`}>{crumb.name}</Link>
          </li>
        ))}
        <li aria-current="page">{current}</li>
      </ol>
    </nav>
  )
}
