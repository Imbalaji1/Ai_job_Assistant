# AI Job Assistant — Part 1 design

## Scope

Part 1 replaces the console-only tracker with a Spring Boot REST API backed by MySQL. It includes application CRUD, validation, pagination, filtering by company and status, status counts, and status-change history. Ollama integration and a web interface are planned for later parts.

## Request flow

An API controller receives an HTTP request, the service applies the tracker rules, and a Spring Data repository reads or writes the MySQL database. Controllers return JSON and do not print to the console.

## Main packages

- `controller`: HTTP endpoints.
- `dto`: validated request and response shapes.
- `entity`: Java objects mapped to database tables.
- `repository`: database queries.
- `service`: tracker business logic.
- `enums`: allowed application statuses.
- `exception`: consistent API errors.
- `src/main/resources/db/migration`: Flyway SQL scripts that create and update the database schema.

## Database

Flyway creates `job_applications` and `application_status_history` from versioned SQL migrations. The existing console program stored data in memory, so there is no old database data to import. Deleting an application also deletes its status history.

## Later parts

Part 2 adds an Ollama service behind a separate AI package. Part 3 can add the user interface under Spring's `templates` and `static` resources.
