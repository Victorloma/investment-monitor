# Deployment Guide

This project is prepared for a hosted setup with:

- `backend/` deployed as a Docker container
- `frontend/` deployed as a static Angular app
- PostgreSQL provided by a managed database such as Neon

## Architecture

- `frontend/` serves the Angular application
- `backend/` exposes the Spring Boot API under `/api/v1`
- PostgreSQL stores users, roles, and portfolio data

## Required secrets and environment variables

### Backend

Set these for the deployed backend service:

- `PORT`
- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `CORS_ALLOWED_ORIGINS`

Recommended values:

- `PORT=8080`
- `JWT_EXPIRATION_MS=3600000`
- `CORS_ALLOWED_ORIGINS=https://<your-frontend-domain>`

Notes:

- `JWT_SECRET` must be a strong base64-encoded secret
- `DATABASE_URL` must be a valid PostgreSQL JDBC URL
- if your managed Postgres provider requires SSL, include the required JDBC parameters in `DATABASE_URL`

Example JDBC URL pattern:

```text
jdbc:postgresql://<host>/<database>?sslmode=require
```

## Backend deployment

### Build the backend locally

From `backend/`:

```bash
mvn package -DskipTests
```

### Build the Docker image

From `backend/`:

```bash
docker build -t <dockerhub-username>/investment-monitor-api:latest .
```

### Push the Docker image

```bash
docker push <dockerhub-username>/investment-monitor-api:latest
```

### Container runtime expectations

The backend container:

- listens on port `8080` internally
- reads all runtime configuration from environment variables
- runs Flyway migrations on startup

## Frontend deployment

The Angular frontend already supports runtime API configuration through `src/assets/runtime-config.js`.

### Build the frontend

From `frontend/`:

```bash
npm install
npm run build
```

### Runtime API base URL

The production app can use either:

- a relative API base URL such as `/api/v1`
- an absolute API base URL such as `https://api.example.com/api/v1`

Use `frontend/src/assets/runtime-config.js` for deployed values.

Example:

```javascript
window.__env = window.__env || {};
window.__env.apiBaseUrl = 'https://api.example.com/api/v1';
```

### Static hosting behavior

`frontend/src/staticwebapp.config.json` is included in the Angular build output and provides SPA navigation fallback.

## Pre-deploy checklist

- backend image builds successfully
- frontend production build succeeds
- backend CORS allows the deployed frontend origin
- backend `JWT_SECRET` is set in the hosting platform
- backend database credentials point to the hosted PostgreSQL instance
- Flyway can run against the production database
- frontend runtime config points to the deployed backend API

## Smoke test checklist

After deployment, verify:

- `GET /api/v1/health`
- user registration
- user login
- `GET /api/v1/users/me`
- portfolio list/create/delete flows
- CORS works from the deployed frontend

## Current repo artifacts

Deployment-related files currently in the repo:

- `backend/Dockerfile`
- `backend/.dockerignore`
- `backend/src/main/resources/application.yml`
- `frontend/src/assets/runtime-config.js`
- `frontend/src/staticwebapp.config.json`
