# AI Job Assistant

Part 1 is a Spring Boot REST API for tracking job applications. It stores application data in MySQL and provides endpoints for creating, viewing, searching, updating, and deleting applications, recording status changes, and showing dashboard counts.

## Requirements

- Java 26 or a compatible Java version
- Maven 3.10 or later
- MySQL Server 8.0 or later

## Create the database

In MySQL Workbench, connect to your MySQL Server and run:

```sql
CREATE DATABASE ai_job_assistant;
```

The application runs the versioned SQL migrations in `src/main/resources/db/migration` when it starts. The database itself must exist first.

## Configure the connection

The default database URL expects MySQL on `localhost:3306` and database `ai_job_assistant`. Set the username and password to match your MySQL Workbench connection. Keep your MySQL password out of Git:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/ai_job_assistant"
$env:DB_USERNAME = "your-mysql-username"
$env:DB_PASSWORD = "your-local-mysql-password"
```

## Run the application

From the project folder, run:

```powershell
mvn spring-boot:run
```

The API starts at `http://localhost:8080`.

## Run the automated tests

From the project folder, run:

```powershell
mvn test
```

The unit tests use mocked repositories, and the HTTP integration test uses a separate in-memory H2 database. This command does not need your MySQL credentials or change your MySQL database. A live check with MySQL still requires the username and password environment variables above.

## Part 1 API endpoints

- `POST /api/applications` — create an application
- `GET /api/applications` — list applications; optional `company`, `status`, `page`, and `size` query parameters. Pages start at 0; page size defaults to 20 and can be from 1 to 100.
- `GET /api/applications/{id}` — view one application
- `PUT /api/applications/{id}` — edit an application
- `PATCH /api/applications/{id}/status` — change its status
- `PATCH /api/applications/{id}/interview-date` — set or clear its optional interview date
- `GET /api/applications/{id}/history` — view status changes
- `DELETE /api/applications/{id}` — delete an application
- `GET /api/dashboard/status-counts` — count applications by status

Application dates use ISO format, for example `2026-11-26`.

## Example request

Send this JSON to `POST /api/applications`:

```json
{
  "companyName": "Example Company",
  "jobRole": "Java Developer",
  "salaryLpa": 8.50,
  "location": "Bengaluru",
  "status": "Applied",
  "appliedDate": "2026-11-26",
  "interviewDate": "2026-12-04"
}
```

`interviewDate` is optional. Omit it or send `null` if no interview is scheduled. To set or clear only this date later, send a `PATCH` request to `/api/applications/{id}/interview-date` with `{"interviewDate":"2026-12-04"}` or `{"interviewDate":null}`.

Valid statuses are `Applied`, `Interview`, `Selected`, `Rejected`, and `Withdrawn`. The API accepts these values without regard to letter case.

## Resume, chat, history, and profile APIs

Resume files can be uploaded as PDF or DOCX documents up to 10 MB. The original file bytes are stored in MySQL. Text-based PDFs and DOCX files can be read for analysis; scanned image-only PDFs need OCR, which is not included yet. A deleted resume is removed from the active file list and its stored bytes are cleared, while a history event remains. Chats are stored separately and are not deleted when a resume is deleted.

The generated PDF writer uses a built-in font for common Western characters. To include other scripts in generated PDFs, set `RESUME_PDF_FONT_PATH` to a TrueType font file that supports the resume's language. PDF script shaping depends on PDFBox's language support; DOCX is the safer format for complex scripts.

- `POST /api/resumes` — upload a PDF or DOCX as multipart form data using the field name `file`
- `GET /api/resumes` — list saved resume versions
- `GET /api/resumes/{id}/download` — download a saved resume
- `DELETE /api/resumes/{id}` — delete its stored file content and record the deletion in History
- `POST /api/resumes/{id}/applications` — create an application from JSON field `application` and link this resume version
- `POST /api/applications/{id}/resume-submissions` — link a saved resume version to an existing application
- `GET /api/applications/{id}/resume-submissions` — list the resume versions linked to an application
- `POST /api/ai/chats` — create a career chat, optionally linked to a resume version and application
- `GET /api/ai/chats` — list saved chats
- `GET /api/ai/chats/{id}/messages` — read a chat transcript
- `POST /api/ai/chats/{id}/messages` — send a message and wait for the complete reply
- `POST /api/ai/chats/{id}/messages/stream` — receive the reply as it is generated
- `POST /api/ai/chats/{id}/resume-revisions` — generate a revised resume in the original PDF or DOCX format
- `POST /api/ai/chats/{id}/resume-revisions/{generatedVersionId}/keep-choice` — keep `ORIGINAL`, `GENERATED`, or `BOTH`; both versions are saved by default, so no response leaves both saved
- `DELETE /api/ai/chats/{id}` — delete the conversation independently of its resume
- `GET /api/history` and `GET /api/history/summary` — view events and upload/deletion/chat counts
- `GET /api/profile` and `PUT /api/profile` — read or save the local profile name, email, and optional phone number

The chat is focused on resumes, job roles, job applications, interviews, skills, and career development. It uses the off-topic response agreed for this project. Chat messages and AI replies are saved in MySQL so a conversation can continue across requests.

## Local Ollama model

The AI backend sends resume text and chat messages to Ollama on `http://localhost:11434` using the `qwen2.5:3b` model by default. Keep Ollama running when using the AI endpoints. You can override the default with `OLLAMA_BASE_URL` and `OLLAMA_MODEL`. No model training is required.

## Run the API

After starting the application, use `http://localhost:8080/api/applications` as the base URL. For example, `GET /api/applications?status=Applied&page=0&size=20` lists the first page of applications with Applied status. A successful create returns HTTP 201, a successful delete returns HTTP 204, and validation/not-found errors return a JSON error response.
