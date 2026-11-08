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

### Prerequisites

- JDK 21
- Docker (for the local PostgreSQL instance)

### 1. Start the database

```bash
docker compose up -d
```

This starts PostgreSQL 17 on `localhost:5432` with database `security`, user `postgres` and password `password`.

### 2. Run the application

```bash
export JWT_SECRET="replace-with-a-long-random-secret-at-least-32-bytes"
./mvnw spring-boot:run
```

The API is served at `http://localhost:8080`. Hibernate creates and updates the `users` and `tokens` tables automatically (`ddl-auto: update`).

### 3. Run the tests

```bash
./mvnw test
```

## Configuration

Settings live in `src/main/resources/application.yaml`.

| Property                     | Default                                         | Description                                   |
|------------------------------|-------------------------------------------------|-----------------------------------------------|
| `spring.datasource.url`      | `jdbc:postgresql://localhost:5432/security`     | JDBC URL                                      |
| `spring.datasource.username` | `postgres`                                      | DB user                                       |
| `spring.datasource.password` | `password`                                      | DB password                                   |
| `jwt.secret`                 | `${JWT_SECRET}`, falling back to a dev secret   | HMAC signing key (must be ≥ 256 bits)         |
| `jwt.refreshTTL`             | `2592000`                                       | Refresh token lifetime in seconds (30 days)   |

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
