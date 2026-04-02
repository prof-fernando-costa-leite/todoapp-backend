# Todo App API

Kotlin + Spring Boot 4 API for groups, boards, tasks, workflow transitions, and JWT auth.

## Stack

- Java 21
- Kotlin 2.2
- Spring Boot 4.0.5
- Spring Data JDBC + PostgreSQL
- Testcontainers + MockMvc + JUnit 5

## Quick start

```bash
./gradlew clean test
```

To run the app locally:

```bash
./gradlew bootRun
```

## Frontend integration (CORS)

The API accepts cross-origin requests based on `app.cors.allowed-origins`.

- Default (dev): `http://localhost:5173,http://localhost:3000`
- Override with env var: `CORS_ALLOWED_ORIGINS`

Example:

```bash
export CORS_ALLOWED_ORIGINS="http://localhost:5173,http://localhost:3000"
./gradlew bootRun
```

## Jackson 3 migration notes (`tools.jackson.*`)

This project is on Spring Boot 4, so direct Jackson usage should follow Jackson 3 packages.

- Use `tools.jackson.*` imports in code that references Jackson classes directly.
- Keep `jackson-module-kotlin` on the Jackson 3 coordinate:
  - `tools.jackson.module:jackson-module-kotlin`
- Avoid reintroducing Jackson 2 imports (`com.fasterxml.jackson.*`) in tests/helpers.

For `JsonNode` field access in tests, prefer strict access:

- `node.required("field").asString()`

Avoid tolerant access when validating API contracts:

- `node.path("field")` (can hide missing fields)

## Test scope convention

- `src/test/kotlin/**/integration/*`: endpoint wiring, auth boundaries, persistence, happy-path flows.
- `src/test/kotlin/**/(auth|group|task)/*`: unit-level business rules and branch-heavy logic.

Keeping this split helps fast feedback in unit tests while integration tests cover end-to-end behavior.

## Documentação

| Arquivo | Conteúdo |
|---------|----------|
| [`docs/API.md`](docs/API.md) | Referência completa de todos os endpoints, payloads, respostas e fluxos de negócio |
| [`docs/FRONTEND_PROMPT.md`](docs/FRONTEND_PROMPT.md) | Prompt detalhado para geração do frontend por outra IA (stack, tipos TS, rotas, componentes) |
| [`docs/FRONTEND_PROMPT_TASK_DISPLAY_NAMES.md`](docs/FRONTEND_PROMPT_TASK_DISPLAY_NAMES.md) | Prompt objetivo para adaptar frontend ao novo contrato de nomes (`creatorDisplayName` e `assigneeDisplayName`) |
| [`HELP.md`](HELP.md) | Referências Spring/Gradle e notas de migração Jackson 3 |

