# Browser File System

A small Dropbox-like file system in the browser. You can browse folders, create, rename and delete folders and
files, find files by exact name in a folder and everything below it or across the whole tree, and get the first ten
files whose name starts with what you type. Files are names only, without content. The design targets millions of
entries; the performance section shows query plans measured on one million files.

[![CI](https://github.com/svetla54321/large_scale_browser_based_file_system/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/svetla54321/large_scale_browser_based_file_system/actions/workflows/ci.yml)

Java 25, Spring Boot 4.1, PostgreSQL 18, Flyway · React 19, TypeScript, Vite · Docker Compose, GitHub Actions

## Quick start

Needs only Docker.

```bash
docker compose up --build
```

Open http://localhost:3000. Ports and the database password can be changed in a `.env` file (see `.env.example`).

## Local development

Needs JDK 25 (a newer JDK fails to compile with Lombok), Node 24 and Docker.

1. Start the database: `docker compose up -d db`
2. Run or debug `FileSystemApplication` from the IDE with the Spring profile `local`. Swagger UI is then at
   http://localhost:8080/swagger-ui.html.
3. Start the frontend: `cd frontend`, `npm ci`, `npm run dev`, then open http://localhost:5173.

To debug the backend inside Docker, start it with `docker compose -f compose.yaml -f compose.debug.yaml up --build`
and attach the IDE to port 5005. `backend/api.http` has every endpoint ready to run from IntelliJ.

## Tests

- `./mvnw package` in `backend/`: unit tests, no Docker needed.
- `./mvnw verify`: also integration tests against a real PostgreSQL 18 through Testcontainers, including an
  end-to-end flow over HTTP.
- `npm test` in `frontend/`.

CI runs all of them and builds both Docker images on every push.

## Assumptions

- Names are unique within a folder, ignoring letter case, and keep the form they were typed in. Files and folders
  share one namespace.
- Search by exact name means the whole name with the extension, ignoring letter case: `report` does not find
  `report.pdf`.
- Searching in a folder also searches all its subfolders.
- Search and suggestions return files only.
- Suggestions are the first ten files in alphabetical order, from the whole tree.
- There is one root folder, `Home`, which cannot be deleted. Deleting a folder deletes everything in it.
- Names have 1 to 255 characters, none of `\ / : * ? " < > |`, and are not `.` or `..`. Folders go 32 levels deep.
- There are no users; everyone shares one tree.

## Design decisions

- **One table for folders and files.** Each row has its `parent_id` and an `ancestor_path` with the ids of all its
  ancestors (`/1/42/`). Listing a folder uses `parent_id`, searching a subtree is a range on `ancestor_path`, and a
  rename changes one row.
- **Search as index ranges.** A `name_key` column (lowercase, NFC, `C` collation) serves uniqueness and search. One
  partial index on `(name_key, ancestor_path)` for files answers exact search, subtree search and suggestions as
  index range scans, without sorting.
- **"Load more" instead of page numbers.** Keyset pagination costs the same on every page and does not skip or
  repeat items when something is added or deleted in between.
- **Deletes in the database.** Deleting a folder is one `DELETE`; the foreign key cascade removes the subtree.
- **Concurrency through the database.** A unique constraint stops duplicate names and a lock timeout stops long
  waits; both return 409.

Diagrams: [architecture](docs/architecture.md), [data model](docs/data-model.md), [request flows](docs/flows.md).

## Performance

`scripts/seed-large.sql` adds 1,010,100 rows (100 teams × 100 projects × 100 files) and `scripts/explain-search.sql`
prints the query plans:

```bash
docker compose exec -T db psql -U admin -d filesystem < scripts/seed-large.sql
docker compose exec -T db psql -U admin -d filesystem < scripts/explain-search.sql
```

| Query | Plan | Time |
|---|---|---|
| Suggestions for `rep` (200,000 files start with it) | index scan, no sort | 0.075 ms |
| `README.md` everywhere (10,000 hits), first page | index scan | 0.085 ms |
| `README.md` below one team folder | index scan | 0.093 ms |
| `README.md` everywhere, a page deep in the results | index scan | 0.072 ms |
| A folder's contents, first page | index scan | 0.074 ms |

Deleting one million rows with a single cascading delete took 5.1 seconds. The seed cannot be undone, so use a
database you can reset with `docker compose down -v`.

## Not implemented and next steps

Move and copy, file contents, users and sharing, trash, and searching by part of a name or for folders. At a larger
scale the next steps would be deleting big folders in the background in batches, a trigram index or a search engine
for partial names, read replicas, and partitioning by owner once there are users.
