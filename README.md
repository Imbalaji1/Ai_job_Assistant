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
  "appliedDate": "2026-11-26"
}
```

Valid statuses are `Applied`, `Interview`, `Selected`, `Rejected`, and `Withdrawn`. The API accepts these values without regard to letter case.

## Run the API

After starting the application, use `http://localhost:8080/api/applications` as the base URL. For example, `GET /api/applications?status=Applied&page=0&size=20` lists the first page of applications with Applied status. A successful create returns HTTP 201, a successful delete returns HTTP 204, and validation/not-found errors return a JSON error response.
