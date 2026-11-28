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

- `mvn test` passed before authentication: 25 tests, 0 failures, 0 errors, and 0 skipped.
- Added HTTP checks for application and resume APIs, error responses, profile/history/chat routes, and resume download, linking, and deletion.
- Added PDF/DOCX text-extraction checks and an H2-backed resume-to-application-to-chat-to-revision flow.
- Checked the real `qwen2.5:3b` model with synthetic data: it returned the requested off-topic message and preserved all 6 expected resume facts in the analysis and generated version. This is a small sample, not a general accuracy score.
- The app used H2 for these checks. MySQL migrations were not exercised because no MySQL listener or database credentials were available in the test environment.
- Job matching and structured skill extraction remain pending. Cross-browser verification is still to be done.

## 2026-11-27 — Account authentication

- Added account registration, login, and current-account endpoints with BCrypt password hashes and 30-minute signed JWT bearer tokens.
- Protected the existing API routes and scoped applications, resumes, chats, activity history, dashboard counts, and profiles to the signed-in account.
- Added Flyway migration V4 for user accounts and account ownership columns. The first registered account adopts records created before ownership was added; later accounts cannot access those records.
- Added API coverage for missing and invalid tokens, successful login, incorrect passwords, duplicate email, password hashing, cross-account access to applications/resumes/chats, and preservation of legacy records.
- `mvn test` passed: 29 tests, 0 failures, 0 errors, and 0 skipped. Tests used H2; the V4 and V5 migrations still need a live MySQL check.
- Added rotating refresh tokens in an `HttpOnly`, `SameSite=Lax` cookie with a 30-day inactivity lifetime, plus refresh and logout API routes. Refresh tokens are stored in the database as SHA-256 hashes.
- Added a short-lifetime integration test that proves an expired access token is rejected, refreshed, and replaced; it also verifies rotation and logout revocation.
- Added the first React authentication slice with registration, login, sign-out, proactive token renewal, and automatic 401 refresh/retry. The Vite `/api` proxy connects it to Spring Boot.
- `frontend/npm test` passed the automatic refresh-and-retry client check, and `frontend/npm run build` produced the initial authentication bundle.

## 2026-11-27 — React application

- Built the dashboard, application management, resume Files, streamed AI chat, History, and profile Settings pages on top of the existing backend routes.
- Added light and dark themes, persisted in the browser, plus responsive navigation and mobile layouts.
- Connected screens to the shared authenticated API client, including uploads, downloads, pagination, activity history, profile edits, streaming chat, and resume version retention choices.
- `npm run build` succeeds and produces the complete React bundle. Manual browser and live MySQL/API integration checks are still pending.

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
