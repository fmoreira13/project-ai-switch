# Matriz

## Project presentation

Matriz is a web application for work teams to organize projects and tasks, track progress, and coordinate work. It is implemented as a Spring Boot application that serves both the browser interface and the HTTP API.

## Problem it aims to solve

Work teams often need to coordinate multiple projects, distribute tasks, track states, and ensure that work is active and organized. Without a suitable tool, information can be scattered across messages, documents, and manual lists, making management and progress control difficult.

## Main objectives

- organize projects by objective or initiative;
- manage tasks associated with each project;
- track the status of work;
- improve the team's visibility into what is in progress.

## Implemented capabilities

- project creation;
- project and task listing, consultation, editing, and removal;
- task status and priority updates, search and filters, and project/task overview counts;
- project membership with exercise-only manager/member access rules;
- multiple task assignees and task comments, as implementation-exercise extensions.

The access rules, comments, and assignments are implementation-exercise scope; they are not confirmed production requirements. See [docs/requirements.md](docs/requirements.md), [docs/user-stories.md](docs/user-stories.md), and [docs/constraints.md](docs/constraints.md).

## Documentation organization

The project documentation is organized as follows:

- [AGENTS.md](AGENTS.md): project-specific implementation and verification commands.
- [openapi.yaml](openapi.yaml): HTTP API contract, session security, validation, and response schemas.
- [docs/requirements.md](docs/requirements.md): overview, goals, requirements, and scope of the MVP.
- [docs/user-stories.md](docs/user-stories.md): user stories, acceptance criteria, and edge cases.
- [docs/constraints.md](docs/constraints.md): constraints, assumptions, and scope limitations.
- [docs/architecture.md](docs/architecture.md): implemented architecture, data flow, deployment, and Mermaid diagram.
- [docs/database.md](docs/database.md): MVP data model.
- [CHANGELOG.md](CHANGELOG.md): project changes and proposed candidate status.
- [docs/release-notes-v0.1.0-rc.2.md](docs/release-notes-v0.1.0-rc.2.md): release-candidate notes and verification status.
- [docs/implementation-plan.md](docs/implementation-plan.md): incremental implementation plan, decisions, acceptance criteria, and verification.
- [docs/test-plan.md](docs/test-plan.md): requirement-based task validation, creation/consultation, and access-control scenarios.
- [docs/quality-security-review.md](docs/quality-security-review.md): bounded code/security review results, authorization evidence, and known verification limits.
- [playwright.config.js](playwright.config.js) and [e2e/](e2e/): repeatable browser-based end-to-end checks using an isolated in-memory H2 database.
- [Dockerfile](Dockerfile), [compose.yaml](compose.yaml), and [.github/workflows/ci.yml](.github/workflows/ci.yml): container build/run and automated GitHub verification.
- [docs/verification.md](docs/verification.md): expected and observed Exercise 8 results, automated test evidence, and user-reported manual checks.
- [docs/ui/wireframe.png](docs/ui/wireframe.png): visual prototype of the MVP project and task management flow.

## Technology

- Java 25 (the machine also has Java 26 installed; use the installed Java 25 JDK for this project).
- Spring Boot 4.0.9.
- Gradle Wrapper; use `./gradlew` rather than a system Maven or Gradle installation.
- Gradle Wrapper version 9.8.2.
- Spring Data JPA and H2.
- Plain HTML, CSS, and JavaScript for the frontend.
- Node.js 22+ and Playwright 1.65.0 for frontend regression and browser E2E tests.
- Docker and Docker Compose for a reproducible single-container local run.

## Requirements

- Java Development Kit (JDK) 25.
- Git to clone the repository.
- Network access on the first Gradle Wrapper run to download Gradle and dependencies.
- Node.js 22 or later is needed for the frontend auth regression and Playwright end-to-end test commands. The application itself needs no Node.js runtime.
- Docker Engine and the Docker Compose CLI plugin are needed only for container commands.

## Architecture and external dependencies

The application is a single Spring Boot modular monolith:

- The static HTML/CSS/JavaScript UI lives under `src/main/resources/static` and is served by Spring Boot.
- REST controllers in `com.matriz.api` expose the project, task, and authentication APIs. Services apply validation and project-scoped access rules; Spring Security uses server-side HTTP sessions.
- Spring Data JPA repositories persist the domain entities. `src/main/resources/schema.sql` initializes the schema, and `DemoDataInitializer` creates missing fictitious demo records.
- H2 is embedded. Local execution uses a file database under `data/`; integration tests and E2E use isolated in-memory databases. Docker Compose uses a named volume for its file-backed H2 database.
- JUnit 5/MockMvc cover backend unit and integration behavior; Node's test runner covers frontend auth regressions; Playwright covers the browser journey.

There are no third-party services required at application runtime. Build/test setup downloads dependencies from Gradle/Maven Central, npm, Playwright's browser distribution, and the Docker image registry. GitHub Actions runs CI but is not a runtime dependency. The Gradle plugin pins Spring Boot 4.0.9; the Wrapper pins Gradle 9.8.2; Java toolchains target 25; the E2E development dependency is Playwright 1.65.0.

## HTTP API

The HTTP API contract, including endpoints, request/response schemas, session-cookie security, and CSRF requirements, is documented in [openapi.yaml](openapi.yaml). It uses OpenAPI 3.1.0; its contract version (`1.0.0`) is separate from the proposed Matriz application version.

## Install and configure

Clone the repository and enter its root directory. On macOS, select Java 25 and verify it:

```sh
git clone <repository-url> Matriz
cd Matriz
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

Create `.env` from the fictitious example if it does not exist, then load it into the shell where the application will run:

```sh
test -f .env || cp .env.example .env
set -a
source .env
set +a
```

`.env.example` contains variables for the H2 URL and credentials, the two fictitious demo passwords, and the session-cookie Secure flag. `.env` is ignored by Git; do not commit real credentials. The example values are for local demonstration only.

The application configuration variables are `MATRIZ_DB_URL`, `MATRIZ_DB_USERNAME`, `MATRIZ_DB_PASSWORD`, `MATRIZ_DEMO_MANAGER_PASSWORD`, `MATRIZ_DEMO_MEMBER_PASSWORD`, and `MATRIZ_SESSION_COOKIE_SECURE`. Defaults and demo-only values are shown in `.env.example`; change the demo passwords before use and do not treat them as production credentials.

## Schema and demonstration data

On the first application start, Spring executes `src/main/resources/schema.sql`, which creates or updates the H2 schema idempotently; Hibernate validates the mapped schema. No separate migration command is required. `DemoDataInitializer` creates two demo accounts, memberships, projects, and sample task data only when those records are absent. It does not reset or delete user data on subsequent starts.

The default H2 database is file-backed under `data/`, which is ignored by Git and persists between Backend restarts. The database path can be changed through `MATRIZ_DB_URL`. Integration tests use an isolated in-memory H2 database.

## Run locally

Start the application from the repository root with Java 25 selected and `.env` loaded:

```sh
./gradlew bootRun
```

The local application is available at `http://localhost:8080`; the frontend is served by Spring Boot at `/` and `/login.html`. Availability endpoints are `GET /actuator/health/liveness` (process liveness) and `GET /actuator/health/readiness` (application readiness, including H2 connectivity); both return only an overall status, without component details. The existing `GET /api/health` endpoint remains available for compatibility.

Application logs are written as one structured JSON object per console line. They include framework/application log fields; the application does not enable HTTP request-body or credential logging.

### Run with Docker Compose

Docker Compose runs the same Spring Boot application in one container. H2 is embedded, so no separate database service is needed; Compose supplies its default bridge network, which is sufficient for this single service. The named `matriz-data` volume preserves the file-backed H2 database across container recreation and ordinary `docker compose down`. The service healthcheck requests `/actuator/health/readiness`; the container is healthy only when the application is ready and H2 is accessible.

Create the ignored `.env` file and review/change the fictitious demo passwords before starting:

```sh
test -f .env || cp .env.example .env
docker compose config --quiet
docker compose build
docker compose up -d
docker compose ps
docker compose logs --tail=50
curl --fail http://localhost:8080/actuator/health/liveness
curl --fail http://localhost:8080/actuator/health/readiness
curl --fail http://localhost:8080/api/health
```

The Compose file injects only the configured runtime environment variables; `.env` is excluded from the Docker build context and is never copied into the image. Container data is stored in the named volume mounted at `/app/data`, not the repository's `data/` directory. To stop the app while preserving its database, run:

```sh
docker compose down
```

Do not use `docker compose down --volumes` unless intentionally deleting the container database. The sample credentials are for local demonstration only, not production deployment.

### Academic demo on Render Free

The application can be deployed as a Docker web service for a short academic demonstration:

1. Create a Render Web Service from this GitHub repository and select the `Docker` runtime and `main` branch.
2. Set the health check path to `/actuator/health/readiness`.
3. Configure `MATRIZ_DB_URL` as `jdbc:h2:mem:matriz;DB_CLOSE_DELAY=-1`, set both demo passwords in Render's environment settings, and set `MATRIZ_SESSION_COOKIE_SECURE` to `true`. `PORT` is supplied by Render; Spring Boot uses it and defaults to `8080` for local runs.
4. Deploy and use the service URL. The fictitious manager/member demo accounts are initialized at startup.

This is suitable only for a temporary demonstration. Render Free services can sleep when idle, and their filesystem is ephemeral; this configuration uses in-memory H2, so demo records are lost when the service restarts or redeploys. Do not use real or sensitive data. The application is not production-ready.

### VS Code Java setup

Open the repository root (the directory containing `settings.gradle` and `build.gradle`), not `src/` by itself. The workspace enables Gradle Wrapper import and automatic Java build-configuration updates in `.vscode/settings.json`, and recommends the Java extension pack.

If Spring imports still show as unresolved, install the recommended Java extensions, run **Java: Import Java Projects into Workspace** from the Command Palette, and wait for Gradle synchronization. If the diagnostic remains after synchronization, run **Java: Clean Java Language Server Workspace** and allow VS Code to restart and reimport the project. The `src/` “contains emphasized items” text is an Explorer accessibility/decorations hint that child items have diagnostics; it is not a source-code statement.

To run checks from another terminal, select Java 25 there as well:

```sh
./gradlew clean test
./gradlew test --tests 'com.matriz.MatrizAccessIntegrationTests.exercise5ManagerCannotReadOrUpdateAnotherUsersTaskById'
node --check src/main/resources/static/js/auth.js
node --check src/main/resources/static/js/login.js
node --check src/main/resources/static/js/index.js
node --experimental-default-type=module --test src/test/js/auth.test.mjs
git diff --check
```

Run the end-to-end browser test (first install dependencies and the Chromium browser once):

```sh
npm ci
npx playwright install chromium
npm run test:e2e
```

The E2E runner starts a separate application instance on port `18080` using an isolated in-memory database and fictitious test credentials. It does not use or alter the local `data/` database or `.env`.

### GitHub Actions

The [CI workflow](.github/workflows/ci.yml) runs on pushes to `main`, pull requests, and published GitHub releases. Its `verify` job uses Java 25 and Node.js 22 to run JUnit, frontend syntax/auth checks, Playwright Chromium E2E, and Compose configuration validation. The separate `build-image` job has `needs: verify`, builds the image, then scans its operating-system packages with Trivy. The scan fails the job for HIGH or CRITICAL findings, including vulnerabilities without a known fix. It does not scan application-library dependencies.

For published pre-releases only, `publish-image` waits for both `verify` and `build-image`, then publishes to `ghcr.io/fmoreira13/project-ai-switch` using the built-in `GITHUB_TOKEN` and the job's minimum `packages: write` permission. It tags the image with the release version (without the leading `v`) and full commit SHA; it does not set `latest`. Push and pull-request workflows remain read-only (`contents: read`) and do not publish images.

The first local Trivy scan found two HIGH entries for `CVE-2026-109518` in the Java runtime base image's `apt` packages. The Dockerfile now applies available OS package updates during the image build; rescanning the rebuilt image with Trivy 0.70.0 reported no HIGH or CRITICAL OS findings. GitHub Actions run [44120868378](https://github.com/fmoreira13/project-ai-switch/actions/runs/44120868378) passed `verify` and `build-image`, including the Trivy scan, on 2026-11-17. That push-triggered run predates this release workflow and does not exercise its pre-release-only publication job.

On 2026-11-11, the first Actions run failed on an exact timestamp-string comparison in a comment integration test, and correctly skipped `build-image`. The test was made tolerant of equivalent timestamp serialization; the subsequent run passed both jobs. See the [successful Actions run](https://github.com/fmoreira13/project-ai-switch/actions/runs/43092399460). A separate temporary-failure check was also performed locally; it is not a claim that the Actions run itself used an injected failure.

After signing in, use `http://localhost:8080/` to create, list, search, inspect, edit, and remove projects and tasks. Project names must contain 3-100 characters and are unique after trimming, without regard to case; descriptions are limited to 1000 characters. Projects with tasks cannot be removed and return a conflict, preserving all dependent records. Task titles must contain 3-150 non-whitespace characters; descriptions are limited to 2000 characters. Tasks use statuses `TODO`, `IN_PROGRESS`, and `DONE` and priorities `LOW`, `MEDIUM`, and `HIGH`; defaults are `TODO` and `MEDIUM`. Members can view/update tasks, while only managers can create/remove them or manage their assignments. Multiple active project members can be assigned to a task, with no duplicate assignment for the same user. Task comments show the author and date; members can add comments containing non-whitespace text of 1-2000 characters. Task lists support text, status, and priority filters, return current status counts, and are not paginated by the API contract.

The frontend reads the Backend origin from the `matriz-api-base-url` meta tag in both `src/main/resources/static/index.html` and `src/main/resources/static/login.html`. The default is empty, which uses the current origin and is the recommended same-origin setup. If the Backend origin changes, set the same HTTP(S) origin in both files; cross-origin session cookies also require server-side CORS and cookie configuration.

Demo users (fictitious credentials configured by these environment variables in `.env.example`):

- `manager@matriz.local` / `MATRIZ_DEMO_MANAGER_PASSWORD`
- `member@matriz.local` / `MATRIZ_DEMO_MEMBER_PASSWORD`

The demo passwords are sample-only. Change them in `.env` before using the demo accounts. The password values are applied when each demo user is first created; changing `.env` later does not change an existing password. Never commit `.env` or real credentials. The accounts and seed records are not deleted or reset on startup.

## Sign-in and session lifecycle

The local application uses server-side HTTP sessions:

1. The browser requests `GET /auth/csrf` to establish a pre-authentication session and receive its CSRF token.
2. It sends `POST /auth/login` with the email/password JSON body and the token in `X-CSRF-TOKEN`.
3. On success, the server creates an authenticated session and returns an HttpOnly `JSESSIONID` cookie. Passwords are stored only as BCrypt hashes.
4. `GET /auth/me` returns the profile for the current session. Project ownership and comment authorship are derived from that authenticated identity, not from client-supplied identity fields.
5. `POST /auth/logout`, with a CSRF token, invalidates the server-side session. Protected API calls without a valid session return `401`.

The `JSESSIONID` session cookie is HttpOnly and SameSite=Lax; the CSRF token is bound to the server-side session and returned in the `/auth/csrf` response body. For local HTTP, `MATRIZ_SESSION_COOKIE_SECURE` defaults to `false`; set it to `true` when serving over HTTPS. The sign-in page is `/login.html`; the protected landing page redirects to sign-in when the session is missing or expired and displays a clear message.

## Current project status

The Spring Boot project, H2 persistence schema, session authentication, project/task management interface, overview counts, assignee management, comments, and project-scoped access checks are implemented. Automated API/frontend checks and user-reported browser/restart checks are documented in [docs/verification.md](docs/verification.md). The access policy for this exercise is documented in [docs/architecture.md](docs/architecture.md). The `0.1.0-rc.2` candidate scope and release status are recorded in the [release notes](docs/release-notes-v0.1.0-rc.2.md).
