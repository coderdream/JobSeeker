# Get Jobs

Get Jobs is a full-stack application for job-platform automation. This repository contains a Java 21 Spring Boot backend and a Next.js frontend under `front/`. The backend exposes JWT-protected REST APIs, manages platform configuration, cookies, job data, task state, and progress streams, and coordinates Playwright workers for login, job collection, and application workflows.

Supported platforms:

- Boss Zhipin
- 51job
- Liepin
- Zhilian
- Yupao page and API entry points

## Features

- User registration, login, and JWT authentication
- User-scoped platform configuration, cookies, AI settings, and task state
- Platform login-state checks, cookie persistence, task start and stop actions
- SSE task progress for Boss and 51job
- Status polling for Liepin and Zhilian
- Job lists, analytics, and reload workflows
- Reference data management
- Frontend pages for login, platform setup, task execution, analytics, reference data, environment configuration, and AI configuration

## Tech Stack

Backend:

- Java 21
- Spring Boot 3.5.x, Spring MVC, Spring Security, Bean Validation
- MyBatis-Plus
- PostgreSQL for the development profile
- Flyway database migrations
- JWT authentication with `jjwt`
- Playwright Java for browser automation
- JUnit 5, Mockito, and H2 for tests

Frontend:

- Next.js 16, React 19, TypeScript
- Tailwind CSS
- Framer Motion
- Chart.js
- lucide-react

## Project Layout

- `src/main/java/com/wh/jobsbackend`: backend application source
- `src/main/resources/db/migration`: Flyway migration scripts
- `src/test/java/com/wh/jobsbackend`: backend tests
- `front/app`: Next.js App Router pages
- `front/components`: shared frontend components
- `front/lib`: frontend API, auth, SSE, and platform request helpers
- `docs/frontend-backend-integration.md`: frontend/backend integration notes

## Runtime Configuration

The default Spring profile is `dev`.

Important environment variables:

- `DB_URL`, default `jdbc:postgresql://127.0.0.1:5432/jobs_backend`
- `DB_USERNAME`, default `jobs_backend`
- `DB_PASSWORD`, default `jobs_backend`
- `JWT_SECRET`, required to be replaced with a secure secret in production

Default ports:

- Backend: `8889`
- Frontend dev server: `6866`
- Frontend API base URL: `http://localhost:8889`

## Local Development

Install backend dependencies and run all tests:

```powershell
mvn "-Dmaven.repo.local=D:\Maven\apache-maven-3.8.6\maven_repository" test
```

Run focused backend tests:

```powershell
mvn "-Dmaven.repo.local=D:\Maven\apache-maven-3.8.6\maven_repository" "-Dtest=UserAutomationRegistryTest,ProgressStreamServiceTest" test
```

Start the backend:

```powershell
$env:JAVA_HOME='E:\jdk21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn --% -Dmaven.repo.local=D:\Maven\apache-maven-3.8.6\maven_repository -DskipTests spring-boot:run
```

Start the frontend:

```powershell
cd front
npm install
npm run dev
```

Build the frontend:

```powershell
cd front
npm run build
```

Build the frontend and copy the static output into backend resources:

```powershell
cd front
npm run build:prod
```

## Tests and Regression Checks

Common backend test command:

```powershell
mvn "-Dmaven.repo.local=D:\Maven\apache-maven-3.8.6\maven_repository" test
```

Common frontend checks:

```powershell
cd front
npm run lint
npm run test:auth-registration
npm run test:auth-provider-session
npm run test:authed-request-token
npm run test:api-base-url
npm run test:api-client-auth
npm run test:sse-auth
npm run test:platform-contract
```

## Development Notes

- Private data must stay scoped to the current user. Do not read configuration, cookies, job data, or task state across users.
- Add a new versioned Flyway migration for schema changes. Do not edit already-applied migrations.
- When changing platform behavior, check the backend controller, service, worker, frontend page, and `front/lib` request wrapper together.
- Ordinary unit tests should not launch visible browser automation.
- `target/`, `front/.next`, `front/out`, `front/node_modules`, `src/main/resources/dist`, and local database files are runtime or build artifacts and should usually not be committed.

## Chinese Documentation

中文文档见 [README.md](README.md).
