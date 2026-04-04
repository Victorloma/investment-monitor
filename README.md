# Investment Monitor

Monorepo for an investor relations monitoring MVP focused on JWT authentication, RBAC, PostgreSQL-backed portfolio data, and a modern Angular frontend. The project is being shaped toward a production-ready Azure deployment as described in the product requirements document.

## Tech stack

- `backend/` Java 21, Spring Boot 3.5, Spring Security 6, Spring Data JPA, Flyway, PostgreSQL, JWT
- `frontend/` Angular 20 standalone application with Angular Material and CDK
- `docker-compose.yml` local PostgreSQL 16 and Redis 7 services

## Repository structure

- `backend/` Spring Boot API
- `frontend/` Angular 20 application
- `docker-compose.yml` local infrastructure for development
- `DEPLOYMENT.md` deployment notes and environment configuration guidance

## Product direction

The updated MVP requirements document positions this project as a resume-quality investor relations monitoring platform that highlights:

- Azure-oriented cloud deployment with Docker
- JWT authentication and role-based access control
- Angular Material/CDK-based frontend development
- PostgreSQL schema design for portfolios, users, and updates
- Redis-backed rate limiting and real-time architecture as upcoming phases

The README below distinguishes between what is already implemented in the repository and what remains on the roadmap.

## Deployment

Deployment documentation and templates are available in:

- `DEPLOYMENT.md`
- `backend/.env.example`
- `frontend/src/assets/runtime-config.example.js`

## Current implementation status

### Backend

The backend currently includes:

- Spring Boot 3 / Java 21 application bootstrap
- Flyway migration for the core schema, users, roles, and portfolio tables
- JWT-based authentication with Spring Security
- Role-backed authorization using `ADMIN`, `PREMIUM_USER`, `BASIC_USER`, and `READ_ONLY`
- Authentication endpoints:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
- Health endpoint:
  - `GET /api/v1/health`
- Protected user endpoint:
  - `GET /api/v1/users/me`
- Portfolio endpoints:
  - `GET /api/v1/portfolio`
  - `POST /api/v1/portfolio`
  - `DELETE /api/v1/portfolio/{portfolioEntryId}`

### Frontend

The frontend is no longer just a scaffold. It currently includes:

- Angular 20 standalone app structure
- Angular Material-based UI shell
- Authentication forms for registration and login
- Token storage and authenticated `me` lookup
- Portfolio form and portfolio list integration with the backend API
- Runtime-config based API URL support

### Infrastructure

Local infrastructure currently includes:

- PostgreSQL 16 via Docker Compose
- Redis 7 via Docker Compose
- Deployment templates for backend and frontend environment configuration

## Planned roadmap

Based on the updated PRD, the next major milestones are:

- Email verification, password reset, and token refresh flows
- Expanded portfolio/update monitoring features
- Real-time status updates via WebSockets
- Rate limiting with Redis and role-based quotas
- Richer Angular Material/CDK experiences such as virtual scrolling and drag-and-drop
- Azure Static Web Apps + Azure Container Apps deployment pipeline
- Neon PostgreSQL, Key Vault, and Application Insights integration

## Local prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+
- npm 10+
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
- Redis host: `localhost`
- Redis port: `6379`
- Port: `8080`

Optional environment variables:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `CORS_ALLOWED_ORIGINS`
- `PORT`

The backend is also Docker-ready for container deployment.

### Frontend

From `frontend/`:

```bash
npm install
npm start
```

Default development URL:

- Frontend: `http://localhost:4200`

The frontend reads its runtime API base URL from the runtime config asset. See:

- `frontend/src/assets/runtime-config.example.js`

## Current API summary

### Public endpoints

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/health`

### Authenticated endpoints

- `GET /api/v1/users/me`
- `GET /api/v1/portfolio`
- `POST /api/v1/portfolio`
- `DELETE /api/v1/portfolio/{portfolioEntryId}`

## Current role model

The repository already uses role-backed authorization with these roles:

- `ADMIN`
- `PREMIUM_USER`
- `BASIC_USER`
- `READ_ONLY`

New users are currently registered with the `BASIC_USER` role by default.

## Notes on scope

- Some capabilities described in the PRD are **target architecture** rather than implemented features today.
- In particular, WebSockets, Redis-backed rate limiting, email flows, advanced Angular CDK interactions, and Azure production infrastructure are planned but not yet fully implemented in this repository.

## References

- Product requirements: `.claude/worktrees/thirsty-wiles/.claude/docs/prd.md`
- Deployment guide: `DEPLOYMENT.md`
