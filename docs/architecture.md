# Architecture

## Runtime

With Docker Compose the browser talks only to nginx. nginx serves the built React app and forwards `/api/` to the
backend, so the UI and the API share one origin and no CORS configuration is needed. Only nginx and the database
publish ports, and both bind to `127.0.0.1`.

```mermaid
flowchart LR
    browser["Browser<br/>React SPA"]
    subgraph compose["docker compose"]
        nginx["frontend<br/>nginx :8080<br/>static files, /api proxy"]
        backend["backend<br/>Spring Boot :8080"]
        db[("db<br/>PostgreSQL 18")]
    end
    browser -->|"http://localhost:3000"| nginx
    nginx -->|"/api/*"| backend
    backend -->|JDBC| db
```

During development the same split runs on the host: the Vite dev server on `:5173` proxies `/api` to the backend
started from the IDE on `:8080`, and only the database runs in Docker (`docker compose up -d db`).

```mermaid
flowchart LR
    browser["Browser"] -->|":5173"| vite["Vite dev server<br/>proxy /api"]
    vite -->|"/api/*"| backend["Backend in the IDE<br/>profile local, :8080"]
    backend -->|"127.0.0.1:5432"| db[("db container")]
```

## Backend

Packages are split by feature (`node`, `search`, `ping`) and layered inside each feature. Shared building blocks
live in `common`, and HTTP concerns that apply to every request live in `web`.

```mermaid
flowchart TB
    subgraph webLayer["web"]
        filters["RequestIdFilter, AccessLogFilter"]
        handler["ApiExceptionHandler<br/>ProblemDetail + code + requestId"]
    end
    subgraph nodeFeature["node"]
        nodeApi["FolderController, FileController"]
        nodeService["NodeService"]
        nodeValidator["NodeNameValidator + rules"]
        nodeRepo["NodeRepository<br/>Spring Data JPA"]
    end
    subgraph searchFeature["search"]
        searchApi["SearchController"]
        searchService["FileSearchService"]
        searchValidator["SearchTermValidator"]
        searchRepo["FileSearchRepository<br/>JdbcClient"]
    end
    db[("node table")]

    filters --> nodeApi
    filters --> searchApi
    nodeApi --> nodeService
    nodeService --> nodeValidator
    nodeService --> nodeRepo
    searchApi --> searchService
    searchService --> searchValidator
    searchService --> searchRepo
    nodeRepo --> db
    searchRepo --> db
    nodeService -. ApplicationException .-> handler
    searchService -. ApplicationException .-> handler
```

- Writes and folder listings go through JPA, because they work with whole entities and benefit from auditing and
  optimistic locking.
- Search reads only the columns it needs with hand-written SQL through `JdbcClient`, so the query matches the
  partial index exactly and `EXPLAIN` shows what runs.
- Every error leaves through one handler, so every 4xx has the same shape and a stable `code`.

## Frontend

```mermaid
flowchart TB
    app["App<br/>header, routes"]
    searchBox["SearchBox<br/>combobox, scope"]
    folderRoute["FolderRoute<br/>/ and /folders/:id"]
    results["SearchResultsPage<br/>/search"]
    useFolder["useFolder"]
    useSuggestions["useSuggestions<br/>debounce, abort"]
    useResults["useSearchResults"]
    api["api/client, folders, search<br/>fetch, ApiError"]

    app --> searchBox
    app --> folderRoute
    app --> results
    searchBox --> useSuggestions
    folderRoute --> useFolder
    results --> useResults
    useFolder --> api
    useSuggestions --> api
    useResults --> api
```

State that the user can share or bookmark (the open folder, the search) lives in the URL. Everything else is local
component state; there is no global store.
