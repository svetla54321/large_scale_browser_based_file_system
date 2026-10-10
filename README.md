# Browser File System

A small Dropbox-like file system that runs in the browser. You can browse a tree of folders, create folders and
files, delete them (a folder together with everything in it), find files by their exact name in one folder and
everything below it or across the whole tree, and get the first ten files whose name starts with what you type.
Files are names only, without content. The design is meant to stay fast with millions of entries, and the
performance section shows the query plans measured on one million files.

[![CI](https://github.com/svetla54321/large_scale_browser_based_file_system/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/svetla54321/large_scale_browser_based_file_system/actions/workflows/ci.yml)

## Contents

- [Tech stack](#tech-stack)
- [Quick start with Docker](#quick-start-with-docker)
- [Local development and debugging](#local-development-and-debugging)
- [Running the tests](#running-the-tests)
- [Configuration](#configuration)
- [API](#api)
- [Assumptions](#assumptions)
- [Design decisions](#design-decisions)
- [Performance](#performance)
- [Scaling further](#scaling-further)
- [Production notes](#production-notes)
- [Not implemented](#not-implemented)
- [Troubleshooting](#troubleshooting)

## Tech stack

| Part | Technology |
|---|---|
| Backend | Java 25, Spring Boot 4.1 (Spring MVC on virtual threads, Spring Data JPA, `JdbcClient`, Bean Validation, Actuator), Flyway, MapStruct, Lombok, springdoc-openapi |
| Database | PostgreSQL 18 |
| Frontend | React 19, TypeScript, Vite 8, React Router, plain `fetch`, no component library |
| Tests | JUnit 6, AssertJ, Mockito, `MockMvcTester`, `RestTestClient`, Testcontainers 2; Vitest with Testing Library |
| Delivery | Docker multi-stage images (non-root), Docker Compose, nginx, GitHub Actions |

## Quick start with Docker

Needs only Docker (Docker Desktop on Windows and macOS).

```bash
git clone https://github.com/svetla54321/large_scale_browser_based_file_system.git
cd large_scale_browser_based_file_system
docker compose up --build
```

Open http://localhost:3000. The first build takes a few minutes, because it downloads Maven and npm dependencies.

- nginx serves the UI and forwards `/api` to the backend, so everything is on one origin.
- The database is also published on `127.0.0.1:5432` for local development.
- `docker compose down` stops everything and keeps the data; `docker compose down -v` also deletes the data.
- Ports and the database password can be changed in a `.env` file next to `compose.yaml` (see `.env.example`).
  Without it the defaults are used.

## Local development and debugging

Prerequisites: JDK 25, Node 24 LTS, Docker, and an IDE with Java 25 support (IntelliJ IDEA 2025.2 or newer).

1. Start only the database:

   ```bash
   docker compose up -d db
   ```

2. Run or debug the backend from the IDE: open `backend/` as a Maven project and start
   `com.svetlecic.filesystem.FileSystemApplication` with the Spring profile `local`.
   - IntelliJ Ultimate: Run configuration, Active profiles: `local`.
   - IntelliJ Community: Run configuration, Environment variables: `SPRING_PROFILES_ACTIVE=local`.
   - Without an IDE: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` in `backend/`
     (PowerShell: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"`).

   The profile points the backend at the compose database, turns on Swagger UI at
   http://localhost:8080/swagger-ui.html and logs every SQL statement. Without a profile or the
   `SPRING_DATASOURCE_*` variables the backend refuses to start, so it never connects to a database by accident.

3. Start the frontend:

   ```bash
   cd frontend
   npm ci
   npm run dev
   ```

   Open http://localhost:5173. Vite proxies `/api` to `localhost:8080` and serves source maps, so breakpoints in
   the browser dev tools work on the TypeScript code.

**Other ways to run the backend**

- **Without Docker Compose:** run `TestFileSystemApplication` from `src/test/java`, or
  `./mvnw spring-boot:test-run`. Testcontainers starts a throwaway PostgreSQL that disappears when the application
  stops.
- **Debugging the backend inside Docker:**

  ```bash
  docker compose -f compose.yaml -f compose.debug.yaml up --build
  ```

  Then attach the IDE's Remote JVM Debug to `localhost:5005`.
- **Debugging a Maven run:** `./mvnw spring-boot:run -Pdebug -Dspring-boot.run.profiles=local` opens the same
  debug port 5005.

Spring Boot's own `--debug` flag only prints the auto-configuration report; it does not enable a debugger.

## Running the tests

| Command (in `backend/`) | What runs | Needs Docker |
|---|---|---|
| `./mvnw package` | unit tests (`*Test`): name normalization, name rules, error mapping | no |
| `./mvnw verify` | the above plus integration tests (`*IT`) against a real PostgreSQL 18 through Testcontainers: search, folders and files, concurrency, and an end-to-end flow over HTTP | yes |

The tests cover the critical behaviour only: the rules a reviewer can break by accident, the queries the design
depends on, and races. On Windows use `mvnw.cmd`. In `frontend/`, `npm test` runs the Vitest tests for suggestions
(debounce and cancelling), forms, deleting and the search box, `npm run lint` runs oxlint and `npm run build`
type-checks and builds. CI runs all of these and builds both Docker images on every push to `main` and `develop` and
on every pull request.

## Configuration

The backend reads its database from environment variables, so the same image runs everywhere:

| Variable | Example |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db-host:5432/filesystem` |
| `SPRING_DATASOURCE_USERNAME` | `filesystem_app` |
| `SPRING_DATASOURCE_PASSWORD` | from a secret store |

Docker Compose and the `local` profile read these optional variables, from the environment or from `.env`:

| Variable | Default | Used for |
|---|---|---|
| `DB_USERNAME` | `admin` | database user |
| `DB_PASSWORD` | `test1234` | database password; it only protects a local container bound to `127.0.0.1` |
| `DB_PORT` | `5432` | database port on the host |
| `APP_PORT` | `3000` | UI port on the host |

PostgreSQL applies the user and password only when it creates the volume, so after changing them run
`docker compose down -v` once (this deletes the data).

## API

All endpoints are under `/api/v1`. Errors use RFC 9457 Problem Details (`application/problem+json`) with two extra
fields: a stable `code` for programs and a `requestId` that also appears in the `X-Request-Id` header and in the
log. Validation errors list every broken rule in `errors[{field, code, message}]`.

| Method and path | Success | Errors |
|---|---|---|
| `GET /folders/root` | 200 folder | |
| `GET /folders/{id}` | 200 folder with `breadcrumbs` (root to parent) | 404 |
| `GET /folders/{id}/children?cursor=&size=50` | 200 `{items, nextCursor}`, folders first, then files, by name | 400 `INVALID_CURSOR`, 404 |
| `POST /folders/{parentId}/folders` `{"name"}` | 201 with `Location` | 400 `VALIDATION_FAILED` / `MAX_DEPTH_EXCEEDED`, 404, 409 `NAME_ALREADY_EXISTS` / `CONCURRENT_CHANGE` |
| `POST /folders/{parentId}/files` `{"name"}` | 201 with `Location` | 400, 404, 409 as above |
| `GET /files/{id}` | 200 file | 404 |
| `DELETE /folders/{id}` | 204, the whole subtree is deleted | 404, 409 `ROOT_FOLDER_NOT_DELETABLE` / `CONCURRENT_CHANGE` |
| `DELETE /files/{id}` | 204 | 404 |
| `GET /search/files?name=&folderId=&cursor=&size=50` | 200 `{items, nextCursor}`, each hit with its `folderPath` | 400 `VALIDATION_FAILED` (empty, too long or invisible characters, listed in `errors`) / `INVALID_CURSOR`, 404 for an unknown folder |
| `GET /search/suggestions?prefix=` | 200 `{prefix, items}`, at most 10 files; `prefix` is echoed exactly as sent | 400 `VALIDATION_FAILED` |

`backend/api.http` walks through every endpoint, including the error cases, with the IntelliJ HTTP Client. Pick the
`local` environment for a backend on port 8080 or `docker` for the compose stack on port 3000. With the `local`
profile, Swagger UI at http://localhost:8080/swagger-ui.html lists every endpoint and its status codes.

## Assumptions

The task leaves some behaviour open. These are the choices made, with the reasoning:

- **Names are unique within a folder, ignoring letter case, and the typed form is kept.** `Report.pdf` and
  `report.PDF` cannot sit in the same folder, and the list shows the name as it was typed. This is how the Dropbox
  web app, Windows and macOS behave. Names are compared after trimming surrounding spaces and Unicode NFC
  normalization, so `é` typed in either form is the same name.
- **Files and folders share one namespace.** A folder `docs` and a file `Docs` cannot exist side by side.
- **Name rules:** 1 to 255 characters; none of `\ / : * ? " < > |`; no control, bidirectional override or
  unassigned characters; not `.` or `..`. Names starting with a dot are allowed. Folders can be nested 32 levels
  deep.
- **"Exact name" means the whole name including the extension**, ignoring letter case. `report` does not find
  `report.pdf`.
- **Searching in a folder covers the folder and all its subfolders.** With unique names, searching only the
  direct children would return at most one file. Without a folder, the search covers the whole tree.
- **Search and suggestions return files only**, never folders.
- **"Top 10" means the first ten files in alphabetical order** (then by folder path), across the whole tree. That
  order is stable and comes straight from an index; ranking by anything else would mean sorting every match.
  Ten files can share a name if they are in different folders, and each comes with its folder path.
- **There is one root folder, `Home`.** It can hold files and cannot be deleted.
- **Deleting a folder deletes everything in it, permanently,** after a confirmation in the UI.
- **There are no users.** Everyone shares one tree, because authentication is out of scope.

## Design decisions

The diagrams are in [`docs/`](docs): [architecture](docs/architecture.md), [data model](docs/data-model.md) and
[request flows](docs/flows.md) for create, search, suggestions and delete.

- **One `node` table for folders and files.** Each row has a `parent_id` (an adjacency list) and an
  `ancestor_path` with the ids of all its ancestors, for example `/1/42/`. The path never changes, because nodes
  cannot be moved or renamed. Listing a folder uses `parent_id`; searching a subtree uses a range on
  `ancestor_path`, without recursive queries.
- **A separate comparison key.** `name_key` holds the case-folded, NFC-normalized name with the byte-wise `C`
  collation. Uniqueness, exact search and prefix search all use it. `name` keeps the typed form and sorts with an
  ICU collation for display.
- **Prefixes as ranges.** "Starts with `rep`" becomes `name_key >= 'rep' and name_key < 'rep' || U+10FFFF`, and
  "below folder 42" becomes `ancestor_path >= '/1/42/' and ancestor_path < '/1/420'`. Range conditions on a `C`
  collation index need no `LIKE`, so `%` and `_` in a name are ordinary characters and the plan never falls back
  to a sequential scan.
- **One partial index for all searches:** `(name_key, ancestor_path) where type = 'FILE'`. Exact search walks one
  key in path order, which is also the cursor order. Suggestions walk the key range and stop after ten rows.
- **Keyset pagination everywhere.** Listings and search results return an opaque cursor. Page 1,000 costs the same
  as page 1, and deletes between pages never skip or repeat rows. A folder with 100,000 children loads in pages of
  50 with "Load more".
- **Deletes happen in the database.** Deleting a folder is a single `delete ... where id = :id`; the foreign key's
  `on delete cascade` removes the subtree in the same statement and transaction. Nothing is loaded into Java, and
  no orphan can survive a race with a concurrent create.
- **Concurrency is left to the database.** A unique constraint stops two identical creates; the second gets 409.
  Every connection sets `lock_timeout = 5s`, so a request that would wait behind a long delete fails with 409
  `CONCURRENT_CHANGE` instead of holding a pooled connection. Reads never block.
- **Validation as small rules.** Each name rule is its own class with a stable code, and the API reports every
  broken rule at once. The frontend runs the same basic rules before sending and maps codes, not messages, to
  text.
- **Same origin instead of CORS.** nginx in Docker and Vite in development forward `/api`, so the browser only ever
  talks to one origin.
- **The frontend keeps state in the URL** (`/folders/42`, `/search?name=...&folderId=42`), so back, refresh and
  shared links work. Suggestions wait for a 250 ms pause in typing, cancel the previous request and ignore any
  answer that no longer matches the input.

## Performance

`scripts/seed-large.sql` fills the database with 1,010,101 rows: 100 team folders, each with 100 project folders,
each with 100 files, one of which is always `README.md`. `scripts/explain-search.sql` then prints the query plans.
Both run against the compose database:

```bash
docker compose exec -T db psql -U admin -d filesystem < scripts/seed-large.sql
docker compose exec -T db psql -U admin -d filesystem < scripts/explain-search.sql
```

In PowerShell pipe the file instead: `Get-Content scripts/seed-large.sql | docker compose exec -T db psql -U admin -d filesystem`.
The seed adds a million rows to whatever database it runs against, so use a database you can throw away
(`docker compose down -v` resets it).

Measured with `EXPLAIN (ANALYZE, BUFFERS)` on a laptop, PostgreSQL 18, warm cache:

| Query | Plan | Time |
|---|---|---|
| Suggestions for `rep` (200,000 files start with it) | `Limit` over `Index Scan using ix_node_file_search`, no sort | 0.075 ms |
| `README.md` everywhere (10,000 hits), first page of 50 | `Index Scan using ix_node_file_search`, condition on `name_key` and the path range | 0.085 ms |
| `README.md` below one team folder | the same index, narrower path range | 0.093 ms |
| `README.md` everywhere, a page deep in the results | the same, the cursor is one more index condition | 0.072 ms |
| A folder's children, first page | `Index Scan using ix_node_children` | 0.074 ms |

The planner reads only the rows it returns, so the time does not grow with the number of matches. The same plans
hold for prepared statements with generic plans. Deleting one million rows with a single cascading delete took
5.1 seconds in one transaction.

## Scaling further

The current design targets about ten million entries on one PostgreSQL instance. Beyond that, or with more users:

- **Large deletes in the background.** Mark the folder as deleted and answer at once, hide marked subtrees from
  reads, and let a worker delete in batches of a few thousand rows. This keeps transactions short (less locking,
  replication lag and vacuum work) and makes a trash with restore almost free.
- **Search for "contains" or typos:** a `pg_trgm` GIN index, or a search engine such as OpenSearch fed from an
  outbox table.
- **Read replicas** for search and listings, which tolerate a little replication lag.
- **Users and sharding.** With accounts, an `owner_id` would lead every index and be the partitioning or sharding
  key, which is how large file-sync services split their metadata.
- **Caching** of the root and hot folders, and **push updates** over WebSocket so open tabs see changes.

## Production notes

- Build the two images (`docker compose build`, or the Dockerfiles in `backend/` and `frontend/`), push them to a
  registry, and run them next to a managed PostgreSQL. The backend gets its database only through the
  `SPRING_DATASOURCE_*` variables; secrets belong in the platform's secret store.
- The backend is stateless, so it scales horizontally behind a load balancer. Flyway migrates the schema on
  startup.
- Health probes: `/actuator/health/liveness` and `/actuator/health/readiness`. Only `health` is exposed.
- Logs are structured JSON (ECS) with a request id on every line; every request gets one access log line.
- Both containers run as non-root users. nginx sets security headers, caches hashed assets forever and never caches
  `index.html`.

## Not implemented

Rename, move and copy; file contents, upload and download; users, authentication and sharing; trash and restore;
version history; live updates between tabs; selecting several items at once; searching for folders; "contains" and
fuzzy search; a polished visual design.

## Troubleshooting

| Problem | Fix |
|---|---|
| Port 3000 or 5432 is already in use | Set `APP_PORT` or `DB_PORT` in `.env`, or stop the other service. |
| `Connection to localhost:5432 refused` when the backend starts | The database is not running: `docker compose up -d db`. |
| `Failed to configure a DataSource` | Start the backend with the `local` profile or set the `SPRING_DATASOURCE_*` variables. |
| `./mvnw: Permission denied` or `/bin/sh^M: bad interpreter` | The repository keeps `mvnw` executable with LF line endings; re-clone, or run `chmod +x mvnw` and check that Git did not convert line endings. |
| PowerShell: `Unknown lifecycle phase ".run.profiles=local"` | Quote `-D` arguments: `"-Dspring-boot.run.profiles=local"`. |
| PowerShell: `npm.ps1 cannot be loaded` | Use `npm.cmd` instead of `npm`, or allow local scripts for your user. |
| Password authentication failed after changing `DB_PASSWORD` | The volume still has the old user: `docker compose down -v` (deletes the data). |
| Integration tests fail with a Docker error | `./mvnw verify` needs a running Docker; `./mvnw package` does not. |
