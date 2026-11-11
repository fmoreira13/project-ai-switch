# Matriz

## Project presentation

Matriz is a proposal for a project and task management application for work teams. The main goal is to facilitate work organization, improve team coordination, and allow monitoring of progress for tasks and projects.

## Problem it aims to solve

Work teams often need to coordinate multiple projects, distribute tasks, track states, and ensure that work is active and organized. Without a suitable tool, information can be scattered across messages, documents, and manual lists, making management and progress control difficult.

## Main objectives

- organize projects by objective or initiative;
- manage tasks associated with each project;
- track the status of work;
- improve the team's visibility into what is in progress.

## Features planned for the MVP

- project creation;
- task creation per project;
- project and task listing;
- task status update;
- project and task removal;
- simple task search;
- general overview of ongoing work.

## Documentation organization

The project documentation is organized as follows:

- [AGENTS.md](AGENTS.md): project-specific implementation and verification commands.
- [openapi.yaml](openapi.yaml): HTTP API contract, session security, validation, and response schemas.
- [docs/requirements.md](docs/requirements.md): overview, goals, requirements, and scope of the MVP.
- [docs/user-stories.md](docs/user-stories.md): user stories, acceptance criteria, and edge cases.
- [docs/constraints.md](docs/constraints.md): constraints, assumptions, and scope limitations.
- [docs/architecture.md](docs/architecture.md): simplified architecture pack aligned with the MVP.
- [docs/database.md](docs/database.md): MVP data model.
- [docs/implementation-plan.md](docs/implementation-plan.md): incremental implementation plan, decisions, acceptance criteria, and verification.
- [docs/test-plan.md](docs/test-plan.md): requirement-based task validation, creation/consultation, and access-control scenarios.
- [docs/verification.md](docs/verification.md): expected and observed Exercise 8 results, automated test evidence, and user-reported manual checks.
- [docs/ui/wireframe.png](docs/ui/wireframe.png): visual prototype of the MVP project and task management flow.

## Technology

- Java 25 (the machine also has Java 26 installed; use the installed Java 25 JDK for this project).
- Spring Boot 4.0.9.
- Gradle Wrapper; use `./gradlew` rather than a system Maven or Gradle installation.
- Spring Data JPA and H2.
- Plain HTML, CSS, and JavaScript for the frontend.

## Requirements

- Java Development Kit (JDK) 25.
- Git to clone the repository.
- Network access on the first Gradle Wrapper run to download Gradle and dependencies.
- Node.js 22 or later is needed only for the frontend auth regression-test command. The application itself needs no Node.js runtime or frontend package manager.

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

## Schema and demonstration data

On the first application start, Spring executes `src/main/resources/schema.sql`, which creates or updates the H2 schema idempotently; Hibernate validates the mapped schema. No separate migration command is required. `DemoDataInitializer` creates two demo accounts, memberships, projects, and sample task data only when those records are absent. It does not reset or delete user data on subsequent starts.

The default H2 database is file-backed under `data/`, which is ignored by Git and persists between Backend restarts. The database path can be changed through `MATRIZ_DB_URL`. Integration tests use an isolated in-memory H2 database.

## Run locally

Start the application from the repository root with Java 25 selected and `.env` loaded:

```sh
./gradlew bootRun
```

The local application is available at `http://localhost:8080`; health check: `http://localhost:8080/api/health`. The frontend is served by Spring Boot at `/` and `/login.html`.

### VS Code Java setup

Open the repository root (the directory containing `settings.gradle` and `build.gradle`), not `src/` by itself. The workspace enables Gradle Wrapper import and automatic Java build-configuration updates in `.vscode/settings.json`, and recommends the Java extension pack.

If Spring imports still show as unresolved, install the recommended Java extensions, run **Java: Import Java Projects into Workspace** from the Command Palette, and wait for Gradle synchronization. If the diagnostic remains after synchronization, run **Java: Clean Java Language Server Workspace** and allow VS Code to restart and reimport the project. The `src/` “contains emphasized items” text is an Explorer accessibility/decorations hint that child items have diagnostics; it is not a source-code statement.

To run checks from another terminal, select Java 25 there as well:

```sh
./gradlew clean test
node --check src/main/resources/static/js/auth.js
node --check src/main/resources/static/js/login.js
node --check src/main/resources/static/js/index.js
node --experimental-default-type=module --test src/test/js/auth.test.mjs
git diff --check
```

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

The Spring Boot project, H2 persistence schema, session authentication, project/task management interface, overview counts, assignee management, comments, and project-scoped access checks are implemented. Automated API/frontend checks and user-reported browser/restart checks are documented in [docs/verification.md](docs/verification.md). The user confirmed project edit/deletion, task filters, assignment add/remove, status/priority/count updates, validation/error feedback, comment author/date after reopening, and data surviving a Backend restart. API authorization is covered by automated integration tests. The access policy for this exercise is documented in [docs/architecture.md](docs/architecture.md).
