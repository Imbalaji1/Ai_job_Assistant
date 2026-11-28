# AI Job Assistant — Part 1 design

## Scope

The project replaces the console-only tracker with a Spring Boot REST API backed by MySQL and a React web app. It includes application tracking, resume file management, activity history, profile settings, and an Ollama-backed career assistant. The React app brings these features into one dashboard with light and dark themes.

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

Flyway creates the application and status-history tables, then adds resume versions, application-to-resume links, AI conversations and messages, activity events, profiles, and user accounts. Passwords are BCrypt hashes. Applications, resumes, conversations, activity events, and profiles are assigned to an account; child records are accessed through their owned application, resume, or conversation. The first account registered after authentication is enabled receives any older records that have no account owner, preserving the existing local data. Deleting an application also deletes its status history.

## Authentication

Spring Security protects the API with signed JWT bearer tokens. Registration and login return a 30-minute access token and a 30-day `HttpOnly` refresh cookie. The client renews the access token in the background; each refresh rotates the cookie and extends the session. Logging out revokes the refresh token. The JWT signing key is read from `JWT_SECRET_BASE64`, and the cookie is marked `Secure` when `AUTH_COOKIE_SECURE=true`. The API checks ownership in its database queries so a token cannot be used to access another account's records.

The React client lives in `frontend/`. Its authentication provider checks the saved sign-in on page load, refreshes access shortly before expiry, and retries a request once if the access token expires unexpectedly. Vite proxies `/api` to Spring Boot during local development. Feature pages call the backend through a shared API client; it adds the current bearer token, includes the refresh cookie, retries expired sessions, and surfaces backend errors. AI chat uses the backend's streaming response route.

## Later parts

The AI service uses Ollama locally with the configured Qwen model. Resume documents are parsed before their text is included in an AI chat. Chat messages are stored so the backend can resend recent conversation context for each request. The React UI runs separately from the Spring Boot API during local development.
