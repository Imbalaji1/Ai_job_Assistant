# Development log

## 2026-11-26 — Part 1 backend implementation

- Added the Maven project definition and Spring Boot application entry point.
- Replaced the console and in-memory tracker with a MySQL-backed REST API.
- Added application validation, pagination, case-insensitive company filtering, status filtering, and consistent API errors.
- Added status-change history and dashboard counts by status.
- Added the initial Flyway migration for the application and status-history tables.
- Added automated unit tests for service behavior and status parsing, plus an HTTP integration test that uses an isolated in-memory database.
- The old tracker stored records only in memory, so there is no existing application data to import.
- Maven compilation and automated tests passed (11 tests total).
- Started against MySQL 8.0.46; Flyway applied migration V1 and Hibernate validated the mapped schema.
- A live HTTP smoke test passed create, filter, status update, history, dashboard count, and delete. The temporary record was removed; the user's sample record remains.
