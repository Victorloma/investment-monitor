# Investment Monitor

Monorepo for an investor relations monitoring MVP focused on authentication, RBAC, PostgreSQL-backed portfolio data, and a modern Angular frontend.

## Tech stack

- `backend/` Java 21, Spring Boot 3, Spring Security, JPA, Flyway, PostgreSQL, JWT, WebSocket support
- `frontend/` Angular 18.2.x application
- `docker-compose.yml` local PostgreSQL and Redis services

## Repository structure

- `backend/` Spring Boot API
- `frontend/` Angular 18.2.x application
- `docker-compose.yml` local infrastructure for development

## Backend status

The backend scaffold currently includes:

- Spring Boot 3 project configuration with Java 21
- Flyway migration for core schema and RBAC tables
- JWT security skeleton
- Authentication endpoints:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
- Health endpoint:
  - `GET /api/v1/health`
- Protected user endpoint:
  - `GET /api/v1/users/me`

The frontend has not been generated yet.

## Local prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop

## Local development

### Infrastructure

Start PostgreSQL 16 and Redis:

```bash
docker compose up -d
```

### Backend

From `backend/`:

```bash
mvn spring-boot:run
```

The API uses these local defaults:

- Database URL: `jdbc:postgresql://localhost:5432/investment_monitor`
- Database user: `postgres`
- Database password: `postgres`
- Port: `8080`

Optional environment variables:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`

## Current blockers

- Maven still needs to be installed and available on `PATH` locally
- Backend compile/run verification still needs to be completed after Maven is available

## Next planned steps

- Finish backend compile verification and fix any issues
- Extend JWT auth with refresh and verification flows
- Generate Angular frontend with Material/CDK
- Add Docker support for backend and frontend services
