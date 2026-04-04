# Investment Monitor

Monorepo for an investor relations monitoring MVP focused on authentication, RBAC, PostgreSQL-backed portfolio data, and a modern Angular frontend.

## Tech stack

- `backend/` Java 21, Spring Boot 3, Spring Security, JPA, Flyway, PostgreSQL, JWT, WebSocket support
- `frontend/` Angular 20.x application
- `docker-compose.yml` local PostgreSQL and Redis services

## Repository structure

- `backend/` Spring Boot API
- `frontend/` Angular 20.x application
- `docker-compose.yml` local infrastructure for development

## Deployment

Deployment documentation and templates are available in:

- `DEPLOYMENT.md`
- `backend/.env.example`
- `frontend/src/assets/runtime-config.example.js`

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

The frontend Angular application is scaffolded and connected to the backend authentication API.

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
- `CORS_ALLOWED_ORIGINS`
- `PORT`

The backend is also Docker-ready for container deployment.

## Next planned steps

- Deploy backend to a hosted container platform
- Deploy frontend to static hosting
- Validate end-to-end production auth and portfolio flows
- Extend JWT auth with refresh and verification flows
