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

- [docs/requirements.md](docs/requirements.md): overview, goals, requirements, and scope of the MVP.
- [docs/user-stories.md](docs/user-stories.md): user stories, acceptance criteria, and edge cases.
- [docs/constraints.md](docs/constraints.md): constraints, assumptions, and scope limitations.
- [docs/architecture.md](docs/architecture.md): simplified architecture pack aligned with the MVP.
- [docs/database.md](docs/database.md): MVP data model.
- [docs/implementation-plan.md](docs/implementation-plan.md): incremental implementation plan, decisions, acceptance criteria, and verification.
- [docs/ui/wireframe.png](docs/ui/wireframe.png): visual prototype of the MVP project and task management flow.

## Technology

- Java 25 (the machine also has Java 26 installed; use the installed Java 25 JDK for this project).
- Spring Boot 4.0.9.
- Gradle Wrapper; use `./gradlew` rather than a system Maven or Gradle installation.
- Spring Data JPA and H2.
- Plain HTML, CSS, and JavaScript for the frontend.

## Run locally

On macOS, select the installed Java 25 JDK and verify it:

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

Create a local environment file from the fictitious example and load it into the current shell:

```sh
cp .env.example .env
set -a
source .env
set +a
```

Start the application:

```sh
./gradlew bootRun
```

In another terminal, with Java 25 selected, run the tests:

```sh
./gradlew test
```

The local API is available at `http://localhost:8080`; health check: `http://localhost:8080/api/health`. H2 uses a file under `data/`, which is ignored by Git and preserved between application starts. The schema is initialized idempotently from `src/main/resources/schema.sql`; demo data is inserted only when its records are absent.

After signing in, use `http://localhost:8080/` to create, list, search, inspect, edit, and remove projects and tasks. Project names must contain 3-100 characters and are unique after trimming, without regard to case; descriptions are limited to 1000 characters. Projects with tasks cannot be removed and return a conflict, preserving all dependent records. Task titles must contain 3-150 non-whitespace characters; descriptions are limited to 2000 characters. Tasks use statuses `TODO`, `IN_PROGRESS`, and `DONE` and priorities `LOW`, `MEDIUM`, and `HIGH`; defaults are `TODO` and `MEDIUM`. Members can view/update tasks, while only managers can create/remove them. Task lists support text, status, and priority filters, return current status counts, and are not paginated by the API contract.

Demo users (fictitious credentials in `.env.example`):

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

The Spring Boot project, H2 persistence schema, session authentication, project and task management interface, and project-scoped access checks are implemented. The application uses Java 25 and the Gradle Wrapper. Remaining verification steps are tracked in [docs/implementation-plan.md](docs/implementation-plan.md); access policy for this exercise is documented in [docs/architecture.md](docs/architecture.md).
