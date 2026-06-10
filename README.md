# Get Jobs Backend

Java 21 Spring Boot backend for the Get Jobs application. The service exposes authenticated REST APIs and coordinates Playwright-based automation for job platforms including Boss, Liepin, 51job, and Zhilian.

## Stack

- Java 21
- Spring Boot 3.5.x, Spring MVC, Spring Security
- JWT authentication with `jjwt`
- MyBatis-Plus persistence
- PostgreSQL in the `dev` profile
- Flyway migrations in `src/main/resources/db/migration`
- Playwright Java for browser automation
- JUnit 5, Mockito, H2 for tests

## Configuration

The default active Spring profile is `dev`.

Important environment variables:

- `DB_URL`, default `jdbc:postgresql://127.0.0.1:5432/jobs_backend`
- `DB_USERNAME`, default `jobs_backend`
- `DB_PASSWORD`, default `jobs_backend`
- `JWT_SECRET`, required for production deployments

The default backend port is `8889`.

## Local Commands

Run all tests:

```powershell
mvn "-Dmaven.repo.local=D:\Maven\apache-maven-3.8.6\maven_repository" test
```

Run focused tests:

```powershell
mvn "-Dmaven.repo.local=D:\Maven\apache-maven-3.8.6\maven_repository" "-Dtest=UserAutomationRegistryTest,ProgressStreamServiceTest,UserScopedSseControllerTest,PlatformJobServiceTemplateTest,UserTaskServiceTest,UserIsolationMigrationTest" test
```

Start locally:

```powershell
$env:JAVA_HOME='E:\jdk21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn --% -Dmaven.repo.local=D:\Maven\apache-maven-3.8.6\maven_repository -DskipTests spring-boot:run
```

## User Isolation

The backend is being upgraded from single-user automation to multi-user isolation:

- private data queries are scoped by `CurrentUserService`
- cookies and configuration are stored per `user_id`
- Playwright runtime state is resolved by `userId + platform`
- SSE progress channels are keyed by `userId + topic`
- platform task state is persisted in `user_job_task`
