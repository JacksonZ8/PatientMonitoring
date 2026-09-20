# Patient Monitoring Server (Backend)

This repository contains the **backend server** for the Patient Monitoring System.  
It provides REST APIs for **doctor authentication**, **patient management**, and **data persistence**, and is packaged as a Dockerized Java Servlet application with a **PostgreSQL database**.

<br>

## Overview

The backend is responsible for:

- Doctor registration and login
- Secure storage of doctor accounts in a server-side database
- Patient data management per authenticated doctor
- Serving data to the Java Swing client via RESTful APIs
- Database schema creation and migration on deployment

The server is implemented using **Java Servlets**, **JDBC**, and **PostgreSQL**, and follows a layered architecture (Servlet → DAO → Database).

<br>

## Technology Stack

- Java (Servlets)
- PostgreSQL
- JDBC
- Gson (JSON serialization)
- Docker-compatible web hosting
- Environment-variable based configuration

<br>

## Architecture

The backend follows a standard layered design:

- **Servlet Layer**
  - Handles HTTP requests and responses
  - Performs validation and JSON parsing
  - Exposes REST API endpoints

- **DAO Layer**
  - Encapsulates all database access logic
  - Uses prepared statements to prevent SQL injection
  - Provides clear separation between business logic and persistence

- **Database Layer**
- PostgreSQL database provided by the deployment environment
- Automatic schema initialization and migration on startup

<br>

## Docker Deployment

The server is built as a WAR and run with `webapp-runner.jar` inside a Java 17 container.

### Local Docker Compose

Run the backend with a local PostgreSQL container:

```bash
docker compose up --build
```

The API will be available at:

```text
http://localhost:8080
```

Health check endpoint:

```text
http://localhost:8080/health
```

The compose file sets:

```text
DATABASE_URL=postgresql://patient:patient@db:5432/patient_monitoring?sslmode=disable
APP_BASE_URL=http://localhost:8080
```

### Build and Run the Server Image

```bash
docker build -t patient-server .
docker run --rm -p 8080:8080 \
  -e PORT=8080 \
  -e DATABASE_URL='postgresql://USER:PASSWORD@HOST:5432/DB?sslmode=require' \
  -e APP_BASE_URL='https://YOUR-SERVICE-URL' \
  patient-server
```

For Render, Railway, Fly.io, or similar platforms, deploy from this repository using the Dockerfile and set the platform-provided `DATABASE_URL`. The server listens on the `PORT` environment variable.

<br>

## Database Schema

### Doctors Table

Stores clinician account information.

| Column               | Type             | Description                     |
|----------------------|------------------|---------------------------------|
| id                   | SERIAL           | Primary key                     |
| email                | TEXT (UNIQUE)    | Doctor login email              |
| given_name           | TEXT             | First name                      |
| family_name          | TEXT             | Last name                       |
| password_hash        | TEXT             | Hashed password                 |
| verified             | BOOLEAN          | Account verification status     |
| verification_token   | TEXT             | Email verification token        |
| reset_token          | TEXT             | Password reset token            |
| reset_token_expires  | TIMESTAMP        | Reset token expiry              |

<br>

### Patients Table

Stores patients associated with a doctor account.

| Column          | Type    | Description                               |
|-----------------|---------|-------------------------------------------|
| id              | SERIAL  | Primary key                               |
| doctor          | TEXT    | Owning doctor email                      |
| given_name      | TEXT    | Patient first name                       |
| family_name     | TEXT    | Patient last name                        |
| gender          | TEXT    | Gender                                   |
| age             | INT     | Age                                      |
| blood_pressure  | TEXT    | Blood pressure (e.g. 120/80)             |

<br>

## API Endpoints

The backend exposes RESTful API endpoints for authentication and patient management.
All endpoints consume and return **JSON** and follow standard HTTP status conventions.

<br>

### Authentication

Authentication endpoints handle doctor registration, login, and account verification.
Passwords are **never stored in plaintext** and are securely hashed before persistence.

<br>

#### Register Doctor

`curl -sS -X POST "http://localhost:8080/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"[YOUREMAIL]","password":"[YOURPASSWORD]","givenName":"[GIVENNAME]","familyName":"[FAMILYNAME]"}'`

Registers a new doctor account in the system.

#### Empty demo patient database
`curl -X POST "http://localhost:8080/api/patients?action=clearDemo&doctor=demo"`

<br>

## Configuration (environment variables)

All configuration is provided through environment variables — no secrets are hardcoded.

| Variable | Required | Description |
|----------|----------|-------------|
| `PORT` | prod | HTTP port the server listens on (provided by the hosting platform). |
| `DATABASE_URL` | yes* | PostgreSQL connection URL, e.g. `postgresql://user:pass@host:5432/db?sslmode=require`. |
| `PGHOST` / `PGPORT` / `PGDATABASE` / `PGUSER` / `PGPASSWORD` | yes* | Alternative to `DATABASE_URL` (standard `libpq` variables). |
| `APP_BASE_URL` | recommended | Public base URL used to build the email verification link, e.g. `https://your-service.example.com`. Falls back to `http://localhost:8080/PatientServer`. |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_USER` / `SMTP_PASS` / `SMTP_FROM` | for email | SMTP credentials used to send verification emails. If unset, registration auto-verifies as a fallback. |
| `SKIP_EMAIL_VERIFICATION` | no | Set to `true` to skip sending verification emails and auto-verify new accounts (local dev / demo only). |

\* Provide either `DATABASE_URL` **or** the `PG*` set.

A ready-to-copy template lives at [`.env.example`](../.env.example) in the repository root.

<br>

## Authentication & authorization

- **Login** (`POST /login`) verifies the password and returns a **session token**
  (valid for 30 days), stored on the `doctors` row (`session_token`, `session_token_expires`).
- **All `/api/*` endpoints are protected by `Servlet/AuthFilter`.** Requests must carry
  `Authorization: Bearer <token>`. The authenticated doctor identity is derived
  **server-side** from the token; the client-supplied `doctor` / `email` parameters are
  ignored (a mismatch returns `403`).
- The only unauthenticated access allowed is the **public demo data set** (`doctor=demo`),
  which backs the demo UI.
- This removes the previous IDOR risk where any caller could read or modify another
  doctor's patients just by changing the `doctor` parameter.

Example authenticated call:

```bash
curl -sS "http://localhost:8080/api/patients?doctor=me@example.com" \
  -H "Authorization: Bearer <token>"
```

<br>

## Password storage

Passwords are hashed with **bcrypt** (`org.mindrot:jbcrypt`, per-hash salt, 12 rounds) in
`RegisterServlet`, `LoginServlet`, `ResetPasswordServlet`, and `DeleteAccountServlet`.

> **Migration note:** accounts created before the bcrypt change stored a non-cryptographic
> `hashCode` value and can no longer authenticate — those users must re-register (or reset
> their password via the reset flow, which now writes a bcrypt hash).

<br>

## Email verification flow

1. `POST /register` creates the account with `verified = false` and a random `verification_token`.
2. A verification email (using `SMTP_*` + `APP_BASE_URL`) is sent with a link to `/verifyEmail?token=...`.
3. `GET /verifyEmail?token=...` marks the account verified.
4. `POST /login` rejects accounts that are not yet verified.
5. If SMTP is not configured (or sending fails), the server logs a warning and auto-verifies
   the account as a fallback so the app stays usable. Set `SKIP_EMAIL_VERIFICATION=true` to do
   this deliberately for local development.

<br>

## Build & run locally

```bash
export JAVA_HOME=/path/to/jdk-17     # Gradle 7.6 requires Java 11–17 to run Gradle itself
./gradlew build                      # compile + produce the WAR in build/libs/
./gradlew appRun                     # run locally via Gretty (http://localhost:8080)
```

Export the variables above in your shell before `./gradlew appRun`.

