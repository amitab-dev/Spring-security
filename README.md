# Security

A Spring Boot REST API for stateless JWT authentication. It uses short-lived access tokens and rotating refresh tokens, and it detects refresh-token reuse.

## Features

- **User registration**: emails are normalized to lowercase and passwords are hashed with BCrypt (cost 12).
- **Login**: issues a signed JWT access token and an opaque refresh token.
- **Refresh token rotation**: every refresh revokes the old token and issues a new pair.
- **Reuse detection**: presenting a revoked refresh token is treated as theft, and the user's active refresh tokens are revoked.
- **Hashed refresh tokens at rest**: only the SHA-256 hash of each refresh token is stored in the database.
- **HttpOnly cookies**: both tokens are set as `HttpOnly`, `Secure`, `SameSite=Lax` cookies.
- **Logout**: revokes the current refresh token and clears the cookies.
- **Global exception handling**: errors come back as a consistent JSON body.

## Tech Stack

| Component  | Version / Library                     |
|------------|---------------------------------------|
| Java       | 21                                    |
| Framework  | Spring Boot 4.1.2 (Web MVC, Security, Data JPA, Validation) |
| JWT        | JJWT 0.12.6 (HMAC-SHA)                |
| Database   | PostgreSQL 17                         |
| Build      | Maven (wrapper included)              |
| Other      | Lombok                                |

## Getting Started

You can run everything in Docker, or run only the database in Docker and the app on your machine.

### Option A: Run everything with Docker Compose

**Prerequisites:** Docker with Compose v2.

```bash
docker compose up -d --build
```

This starts two containers:

| Container      | Service    | Port   | Description                                              |
|----------------|------------|--------|----------------------------------------------------------|
| `security-db`  | `database` | `5432` | PostgreSQL 17, data kept in the `data` volume            |
| `security-app` | `app`      | `8080` | The API, built from the `Dockerfile`                     |

The app starts only after the database passes its `pg_isready` healthcheck. The API is served at `http://localhost:8080`.

Useful commands:

```bash
docker compose logs -f app      # follow app logs
docker compose up -d --build app  # rebuild and restart the app after code changes
docker compose down             # stop containers (data is kept)
docker compose down -v          # stop containers and delete the database volume
```

#### Overriding credentials and secrets

Compose reads these variables from your shell or from a `.env` file next to `docker-compose.yml`. The `.env` file is git-ignored. Every variable has a dev default, so the stack runs without one.

```bash
# .env
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change-me
POSTGRES_DB=security
JWT_SECRET=replace-with-a-long-random-secret-at-least-32-bytes
```

| Variable            | Default                                          | Used by           |
|---------------------|--------------------------------------------------|-------------------|
| `POSTGRES_USER`     | `postgres`                                       | database, app     |
| `POSTGRES_PASSWORD` | `password`                                       | database, app     |
| `POSTGRES_DB`       | `security`                                       | database, app     |
| `JWT_SECRET`        | `this-is-dev-secret-make-sure-to-change-in-prod` | app               |

> **Note:** Postgres only applies `POSTGRES_*` when it creates a new data volume. To change them on an existing setup, run `docker compose down -v` first. That deletes all data.

#### About the image

The `Dockerfile` is a multi-stage build:

1. **Build stage** (`eclipse-temurin:21-jdk`): downloads dependencies with the Maven wrapper first, so they stay cached until `pom.xml` changes. Then it builds the jar with tests skipped.
2. **Runtime stage** (`eclipse-temurin:21-jre`): copies only the jar and runs it as the non-root `spring` user.

The JVM uses up to 75% of the container's memory (`JAVA_OPTS="-XX:MaxRAMPercentage=75.0"`). It runs as PID 1, so `docker stop` triggers a graceful shutdown.

To build and run the image without Compose:

```bash
docker build -t security-app .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://<db-host>:5432/security \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=password \
  -e JWT_SECRET=replace-with-a-long-random-secret \
  security-app
```

### Option B: Run the app locally

**Prerequisites:** JDK 21 and Docker (for PostgreSQL).

1. Start only the database:

   ```bash
   docker compose up -d database
   ```

2. Run the application:

   ```bash
   export JWT_SECRET="replace-with-a-long-random-secret-at-least-32-bytes"
   ./mvnw spring-boot:run
   ```

The API is served at `http://localhost:8080`. If the `security-app` container is running, stop it first (`docker compose stop app`) because it uses the same port.

Either way, Hibernate creates and updates the `users` and `tokens` tables automatically (`ddl-auto: update`).

### Run the tests

```bash
./mvnw test
```

## Configuration

Settings live in `src/main/resources/application.yaml`. Each value can be overridden with the environment variable shown.

| Property                     | Environment variable          | Default                                       | Description                                 |
|------------------------------|-------------------------------|-----------------------------------------------|---------------------------------------------|
| `spring.datasource.url`      | `SPRING_DATASOURCE_URL`       | `jdbc:postgresql://localhost:5432/security`   | JDBC URL                                    |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME`  | `postgres`                                    | DB user                                     |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD`  | `password`                                    | DB password                                 |
| `spring.jpa.show-sql`        | `SHOW_SQL`                    | `false`                                       | Log SQL statements                          |
| `jwt.secret`                 | `JWT_SECRET`                  | A public dev secret                           | HMAC signing key (must be ≥ 256 bits)       |
| `jwt.refreshTTL`             | `JWT_REFRESH_TTL`             | `2592000`                                     | Refresh token lifetime in seconds (30 days) |

Graceful shutdown is enabled (`server.shutdown: graceful`). In-flight requests get up to 20 seconds to finish.

> **Warning:** Always set `JWT_SECRET` outside of local development. The fallback secret is public.

## API Reference

Base path: `/api/v1/auth`. All endpoints under this path are public. Every other route requires a valid access token.

### Register

`POST /api/v1/auth/`

```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "secret123"
}
```

**201 Created**

```json
{
  "id": "6f1c…",
  "name": "Jane Doe",
  "email": "jane@example.com",
  "role": "USER",
  "createdAt": "2026-11-08T10:00:00Z"
}
```

Returns **409 Conflict** if the email is already registered.

### Login

`POST /api/v1/auth/login`

```json
{
  "email": "jane@example.com",
  "password": "secret123"
}
```

**200 OK**

```json
{ "accessToken": "eyJhbGciOi…" }
```

It also sets the `access_token` and `refresh_token` cookies.

### Refresh

`GET /api/v1/auth/refresh`

Reads the `refresh_token` cookie, revokes it, and returns a new token pair. The response body has the same shape as login and the cookies are replaced.

### Logout

`GET /api/v1/auth/logout`

Revokes the refresh token from the cookie (if there is one) and clears both cookies.

```json
{ "message": "User logged out" }
```

### Calling protected endpoints

Send the access token in the `Authorization` header:

```
Authorization: Bearer <accessToken>
```

### Error format

```json
{
  "status": "CONFLICT",
  "message": "Email is taken",
  "timestamp": "2026-11-08T10:00:00Z"
}
```

| Exception             | HTTP status |
|-----------------------|-------------|
| `BadRequestException` | 400         |
| `TokenReuseException` | 401         |
| `NotFoundException`   | 404         |
| `ConflictException`   | 409         |
| Anything else         | 500         |

## How Authentication Works

```
 Client                         API                              DB
   │  POST /login               │                                 │
   │───────────────────────────▶│ verify password (BCrypt)        │
   │                            │ sign JWT (10 min)               │
   │                            │ generate 64-byte refresh token  │
   │                            │ store SHA-256(refresh) ────────▶│
   │◀── accessToken + cookies ──│                                 │
   │                            │                                 │
   │  GET /refresh (cookie)     │                                 │
   │───────────────────────────▶│ look up SHA-256(refresh) ──────▶│
   │                            │ revoked? → revoke all, reject   │
   │                            │ expired? → reject               │
   │                            │ revoke old, issue new pair ────▶│
   │◀── new tokens ─────────────│                                 │
```

| Token         | Format                              | Lifetime   | Storage                     |
|---------------|-------------------------------------|------------|-----------------------------|
| Access token  | JWT (`sub` = user ID, `email`, `role`) | 10 minutes | Client only (stateless)  |
| Refresh token | 64 random bytes, Base64URL          | 30 days    | SHA-256 hash in `tokens` table |

## Project Structure

```
src/main/java/amitab/security
├── SecurityApplication.java      # Entry point
├── config/SecurityConfig.java    # Filter chain, password encoder, auth manager
├── controller/AuthController.java# Auth endpoints and cookie handling
├── dto/                          # Request/response records
├── exception/                    # Custom exceptions and GlobalExceptionHandler
├── models/                       # User, Token entities and Role enum
├── repository/                   # Spring Data JPA repositories
├── security/                     # JWT filter and UserDetailsService
└── service/                      # AuthService, JwtService, TokenService, RefreshTokenService
```
