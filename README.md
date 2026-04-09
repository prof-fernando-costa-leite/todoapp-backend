# Todo App API

Kotlin + Spring Boot 4 API for collaborative task management with groups, boards, workflow transitions, and JWT auth.

## Stack

- **Java** 21
- **Kotlin** 2.2
- **Spring Boot** 4.0.5
- **Spring Data JDBC** + PostgreSQL
- **Jackson 3** (via `tools.jackson.*`)
- **Testcontainers** + MockMvc + JUnit 5

## Quick Start

### Prerequisites

- Java 21+
- Docker (for local PostgreSQL via docker-compose)
- Gradle 8.x+

### Development Setup

```bash
# Clone and navigate to project
cd /home/mister-storm/development/projects/todoapp

# Start PostgreSQL container
docker compose up -d

# Run tests
./gradlew clean test

# Run application locally
./gradlew bootRun
```

The API will be available at `http://localhost:8080/api/v1`

## Frontend Integration (CORS)

The API accepts cross-origin requests. Configure allowed origins:

- **Default (dev):** `http://localhost:5173,http://localhost:3000`
- **Environment variable:** `CORS_ALLOWED_ORIGINS`

Example:

```bash
export CORS_ALLOWED_ORIGINS="http://localhost:5173,http://localhost:3000"
./gradlew bootRun
```

## Architecture: Ports & Adapters

The project follows hexagonal architecture patterns, organized by feature with clear separation between layers:

```
src/main/kotlin/org/misterstorm/eventplatform/todoapp/
├── auth/                          # Authentication & User Management
│   ├── domain/                   # Domain models
│   ├── application/
│   │   ├── port/out/            # Output ports (repositories)
│   │   └── service/             # Business logic (UserService)
│   ├── adapters/
│   │   ├── out/persistence/     # Database adapters
│   │   └── out/security/        # JWT/Security adapters
│   ├── entrypoint/
│   │   └── http/                # REST controllers
│   └── config/                  # Spring configuration
├── group/                        # Group Management
│   ├── domain/
│   ├── application/service/
│   ├── adapters/out/persistence/
│   ├── entrypoint/http/
│   └── config/
├── board/                        # Board & Workflow Management
│   ├── domain/
│   ├── application/service/
│   ├── adapters/out/persistence/
│   ├── entrypoint/http/
│   └── config/
├── task/                         # Task Management
│   ├── domain/
│   ├── application/service/
│   ├── adapters/out/persistence/
│   ├── entrypoint/http/
│   └── config/
└── common/                       # Cross-cutting Concerns
    ├── error/                   # Error handling (exceptions, error responses)
    ├── security/                # Security context & utilities
    ├── logging/                 # Structured logging
    └── config/                  # Global Spring configuration
```

### Layer Responsibilities

- **Domain:** Pure business logic, validation rules, and entity definitions
- **Application Service:** Orchestration of use cases, transaction boundaries, authorization checks
- **Adapters (Out):** Implementation of ports (repositories, external services, security)
- **Entrypoint (HTTP):** REST controllers, request/response mapping, input validation
- **Config:** Spring bean definitions, interceptors, error handlers

## Key Features

### 1. Authentication & Authorization
- JWT-based authentication (Jackson 3 compatible tokens)
- User registration and login
- Role-based access control per group/board
- Current user context injection

### 2. Group Management
- Create groups with members
- Add/remove members from groups
- Visibility and permission boundaries

### 3. Board Management
- Create boards within groups
- Custom workflow statuses (TODO, DOING, DONE, QA, etc.)
- Transition rules (only allowed paths)
- Member visibility on boards with display names

### 4. Task Management
- Create, update, delete tasks
- Task status transitions with blocker validation
- Task assignment to members
- Task history tracking (who changed what, when)
- Blocker relationships (task A blocks task B)
- Task priority and story points

### 5. Workflow Management
- Customizable status definitions per board
- Configurable status transitions
- Blocker prevention rules (can't move blocked task until blocker is resolved)

## API Documentation

Complete API reference: [`docs/API.md`](docs/API.md)

Base URL: `http://localhost:8080/api/v1`

All endpoints (except auth register/login) require:
```
Authorization: Bearer <accessToken>
```

### Main Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| **Auth** |
| POST | `/auth/register` | Register new user |
| POST | `/auth/login` | Login and get JWT token |
| GET | `/auth/me` | Get current user profile |
| GET | `/auth/users` | List all users (for member picker) |
| **Groups** |
| POST | `/groups` | Create group |
| GET | `/groups` | List user's groups |
| GET | `/groups/{id}` | Get group details |
| POST | `/groups/{id}/members` | Add member to group |
| DELETE | `/groups/{id}/members/{userId}` | Remove member from group |
| **Boards** |
| POST | `/groups/{groupId}/boards` | Create board |
| GET | `/groups/{groupId}/boards` | List group boards |
| GET | `/boards/{id}` | Get board details with members & statuses |
| DELETE | `/boards/{id}` | Delete board |
| **Board Statuses** |
| GET | `/boards/{id}/statuses` | List statuses |
| POST | `/boards/{id}/statuses` | Create custom status |
| DELETE | `/boards/{id}/statuses/{statusId}` | Delete status |
| **Board Transitions** |
| GET | `/boards/{id}/transitions` | List allowed transitions |
| PUT | `/boards/{id}/transitions` | Update transitions |
| **Tasks** |
| POST | `/boards/{boardId}/tasks` | Create task |
| GET | `/boards/{boardId}/tasks` | List board tasks |
| GET | `/tasks/{id}` | Get task details with creator & assignee names |
| GET | `/tasks/{id}/history` | Get task history |
| GET | `/tasks/mine` | Get current user's tasks |
| PATCH | `/tasks/{id}` | Update task (title, points, priority, assignee, blocker) |
| PATCH | `/tasks/{id}/status` | Change task status |
| PATCH | `/tasks/{id}/assignee` | Assign task to member |
| PATCH | `/tasks/{id}/blocker` | Set/remove blocker |
| DELETE | `/tasks/{id}` | Delete task |

## Testing

### Test Organization

```
src/test/kotlin/org/misterstorm/eventplatform/todoapp/
├── integration/
│   └── TodoApiIntegrationTest.kt     # Full end-to-end flows, auth boundaries, persistence
├── auth/                              # User service & auth unit tests
├── group/                             # Group service unit tests
├── task/                              # Task service & workflow unit tests
└── board/                             # Board service unit tests
```

### Test Scope Convention

- **Integration Tests** (`integration/*`): 
  - End-to-end API wiring
  - Authentication boundaries
  - Permission enforcement
  - Happy-path flows across multiple features
  
- **Unit Tests** (`auth/*`, `group/*`, `task/*`, `board/*`):
  - Service layer business rules
  - Error conditions and edge cases
  - Branch-heavy validation logic
  - Mock external dependencies

### Running Tests

```bash
# Run all tests
./gradlew test

# Run with code coverage report
./gradlew test jacocoTestReport

# View coverage (opens in browser)
open build/reports/jacoco/test/html/index.html
```

## Jackson 3 Migration Notes

This project uses **Spring Boot 4.x**, which uses **Jackson 3** APIs.

### Guidelines

- **Import Pattern:** Always use `tools.jackson.*` for Jackson types
  ```kotlin
  import tools.jackson.databind.JsonNode
  import tools.jackson.databind.ObjectMapper
  ```

- **Avoid:** Jackson 2 imports (`com.fasterxml.jackson.*`) to prevent classpath conflicts

- **Dependencies:** Keep Jackson modules aligned with Jackson 3
  ```gradle
  tools.jackson.module:jackson-module-kotlin
  ```

- **JsonNode Access (Strict):**
  ```kotlin
  // Preferred - will throw if field missing
  node.required("field").asString()
  
  // Avoid - silently returns null/empty
  node.path("field").asText()
  ```

## Environment Variables

### Database

```bash
DB_HOST=localhost          # Default: localhost
DB_PORT=5432              # Default: 5432
DB_NAME=todoapp           # Default: todoapp
DB_USER=todoapp           # Default: todoapp
DB_PASSWORD=todoapp       # Default: todoapp
```

### Security & JWT

```bash
JWT_SECRET=<base64-encoded-secret>  # 32+ bytes base64
JWT_EXPIRATION=PT2H                 # Duration format (PT2H = 2 hours)
```

### CORS

```bash
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

## Database & Migrations

The project uses **Flyway** for schema management.

- Migrations: `src/main/resources/db/migration/`
- Pattern: `V{number}__{description}.sql`
- Auto-executed on application startup

Disable migrations (for tests):
```yaml
spring:
  flyway:
    enabled: false
```

## Docker & Deployment

### Local Development with Docker Compose

```bash
# Start PostgreSQL
docker-compose up -d

# Stop
docker-compose down

# Remove data volume
docker-compose down -v
```

### Build Docker Image

```bash
./gradlew bootBuildImage
docker run -it --network=host --env DB_HOST=localhost todoapp:0.0.1-SNAPSHOT
```

## References

- [Spring Boot 4.0.5 Documentation](https://docs.spring.io/spring-boot/4.0.5)
- [Spring Data JDBC](https://docs.spring.io/spring-boot/4.0.5/reference/data/sql.html#data.sql.jdbc)
- [Jackson 3 Migration Guide](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0)
- [JWT.io](https://jwt.io) - Understanding JWT tokens

## Troubleshooting

### Database Connection Issues

```
ERROR: relation "app_users" already exists
```

**Solution:** Run migrations on a fresh database or clean the volume:
```bash
docker-compose down -v
docker-compose up -d
```

### CORS Errors

```
Access-Control-Allow-Origin header missing
```

**Solution:** Ensure frontend origin is in `CORS_ALLOWED_ORIGINS`:
```bash
export CORS_ALLOWED_ORIGINS="http://localhost:5173"
./gradlew bootRun
```

### JWT Token Issues

```
Invalid or expired token
```

**Solution:** 
- Check token is in `Authorization: Bearer <token>` format
- Verify `JWT_SECRET` matches issuing and validating instances
- Check token hasn't exceeded `JWT_EXPIRATION`

