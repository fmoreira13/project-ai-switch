# AGENTS.md

## Project objective

Matriz is an application for managing projects and tasks in work teams. The objective of the project at this stage is to support work organization, track progress, and coordinate among team members.

## Project stack

- Java 25 with Spring Boot and the Gradle Wrapper.
- Spring Data JPA and file-backed H2 for local use; integration tests use in-memory H2.
- Plain HTML, CSS, and JavaScript served by Spring Boot.
- Node.js 22 or later is needed for the frontend auth regression tests and Playwright end-to-end tests.
- Keep changes within the documented MVP and implementation exercises. The local demo access policy is not a production security policy.

## General rules

- Read and respect the project documentation before making changes.
- Consult at least the relevant requirement, user story, and constraint files.
- Do not invent functionalities, business rules, technologies, or decisions that are not documented and approved.
- Do not assume requirements that have not been confirmed by the client.
- Keep the task scope aligned with the project objective and the defined MVP.
- Avoid changes outside the task scope.
- If a task requires undocumented decisions, ask for confirmation before proceeding.

## Recommended procedure before changing code

1. Identify the task and expected impact.
2. Consult the relevant documentation in `docs/` and in `README.md`.
3. Verify functional context, constraints, and assumptions before any change.
4. Present a brief plan before making significant changes.
5. Make only the changes necessary to complete the task.
6. Validate quality, consistency, and alignment with the documentation.

## Security and confidentiality rules

- Do not expose credentials, tokens, secrets, keys, or sensitive information.
- Do not introduce logs, temporary files, or confidential data into the repository.
- Ask for confirmation before destructive actions such as deleting important files, resets, changes to critical documentation, or modifications that affect project structure.
- Maintain a responsible and minimal approach to all operations.

## Expected communication

- Explain assumptions when needed.
- Clearly indicate when a decision was assumed due to lack of confirmation.
- Communicate changes made, checks performed, and results obtained.
- If there are ambiguities, ask for clarification instead of assuming a solution.

## Local setup and verification commands

On macOS, select Java 25 and create the local environment file only if it does not already exist:

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export PATH="$JAVA_HOME/bin:$PATH"
java -version
test -f .env || cp .env.example .env
set -a
source .env
set +a
```

Run the application from the repository root. Spring initializes the idempotent schema in `src/main/resources/schema.sql` and the non-destructive demo data when the application starts:

```sh
./gradlew bootRun
```

Run the backend tests and frontend JavaScript checks:

```sh
./gradlew clean test
node --check src/main/resources/static/js/auth.js
node --check src/main/resources/static/js/login.js
node --check src/main/resources/static/js/index.js
node --experimental-default-type=module --test src/test/js/auth.test.mjs
git diff --check
```

Install the Playwright development dependency once, then run the end-to-end browser test:

```sh
npm ci
npx playwright install chromium
npm run test:e2e
```

The Playwright configuration starts the application on port 18080 with an isolated in-memory H2 database and fictitious test passwords. It does not use or alter the local `data/` database or `.env`.

Run the Docker Compose app after creating and reviewing `.env` from `.env.example`:

```sh
docker compose config --quiet
docker compose up --build -d
curl --fail http://localhost:8080/api/health
docker compose down
```

Compose uses a named volume for its file-backed H2 data. `docker compose down` preserves it; do not use `docker compose down --volumes` unless intentionally deleting that database. The `.env` file is runtime-only and excluded from the image build context.

GitHub Actions runs the same Java, frontend, and Playwright checks and builds the container image for pushes to `main` and pull requests. It does not publish images.

The file-backed local H2 database is stored under the ignored `data/` directory. Do not remove or reset it unless the user explicitly requests that action. Test configuration uses an in-memory database and does not change the local database.

## Scope limits

- The current focus is a verifiable implementation of the documented MVP and implementation exercises.
- Do not implement features outside the MVP without explicit approval.
- Do not propose technologies or detailed architecture without prior documentation.
- Do not change requirement documents, user stories, or constraints without a clear justification and alignment with the project context.

## Expected outcome

Agents must work consistently with the project context, respect the documentation, avoid unapproved decisions, and maintain the repository sustainable for the next phases of development.
