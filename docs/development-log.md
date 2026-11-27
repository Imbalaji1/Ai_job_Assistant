# Development log

## 2026-11-27 — Resume and AI backend foundation

- Added MySQL-backed resume versions for PDF and DOCX uploads, with download, deletion, and application-link endpoints.
- Added PDF/DOCX text extraction and generated revised resume documents in the original format.
- Added saved AI conversations and messages, a live streaming chat endpoint, and a local Ollama client configured for `qwen2.5:3b`.
- Added activity-history counts and event endpoints, plus profile read/update endpoints.
- Added the V3 Flyway migration for resume versions, resume/application links, chats, messages, activity events, and profile data.
- Reviewed resume and AI handling: uploaded documents are parsed before storage, model context is bounded, the resume keep-choice question is retained in chat history, and Ollama requests have a three-minute timeout.
- Improved PDF text handling with an optional TrueType font path so unsupported characters are reported instead of silently replaced.
- This backend work has not yet been tested against MySQL or Ollama.

## 2026-11-27 — Interview date tracking

- Added an optional interview date to job applications and API responses.
- Added a Flyway migration so existing databases receive the nullable interview-date column without losing application records.
- Added a focused API route to set or clear an interview date.

## 2026-11-27 — Backend verification

- `mvn test` passed: 25 tests, 0 failures, 0 errors, and 0 skipped.
- Added HTTP checks for application and resume APIs, error responses, profile/history/chat routes, and resume download, linking, and deletion.
- Added PDF/DOCX text-extraction checks and an H2-backed resume-to-application-to-chat-to-revision flow.
- Checked the real `qwen2.5:3b` model with synthetic data: it returned the requested off-topic message and preserved all 6 expected resume facts in the analysis and generated version. This is a small sample, not a general accuracy score.
- The app used H2 for these checks. MySQL migrations were not exercised because no MySQL listener or database credentials were available in the test environment.
- Authentication, job matching, skill extraction, and browser testing remain pending because those features or the frontend are not implemented yet.

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
