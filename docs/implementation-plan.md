# Matriz - Implementation Plan

## Status

Implementation is in progress. The task CRUD/filter increment and both Exercise 6 increments (multi-assignee management and comments) are implemented. Java 25 tests, JavaScript syntax, OpenAPI structure/path checks, and disposable H2 migration checks pass; the diff has been reviewed. A browser walkthrough and Redocly lint remain.

## Scope and evidence

This plan is based on [README.md](../README.md), [AGENTS.md](../AGENTS.md), [requirements.md](requirements.md), [user-stories.md](user-stories.md), [constraints.md](constraints.md), [architecture.md](architecture.md), [database.md](database.md), [openapi.yaml](../openapi.yaml), and the [wireframe](ui/wireframe.png).

The documented MVP covers project and task creation, listing, task status updates, removal, simple task search, and an overview of ongoing work. This exercise additionally requires persistence for users, comments, and assignments and sample data for distinct access tests. The repository now includes application source and integration tests.

## Findings and gaps to resolve

| Topic | Current evidence | Gap or contradiction | Action / status |
|---|---|---|---|
| Class 02 technology | The user confirmed Java and Spring Boot as the Class 02 stack and requested the installed Java 25 with Gradle Wrapper. | The shell default is Java 26.0.1, with Java 25.0.3 also installed. | The project targets Java 25, uses Gradle Wrapper, Spring Data JPA, and H2. README documents selecting Java 25. |
| Two Ficha 02 operations | The user delegated the choice of two simple operations based on existing documentation and OpenAPI. | The Ficha 02 baseline is narrower than the documented MVP. | Select `POST /projects` (create a project) and `POST /tasks` (create a task in an existing project). Other API operations support broader MVP/exercise scope. |
| Project update | Exercise 4 requires project CRUD. | Previous API contract omitted project detail and update operations. | Added `GET /projects/{projectId}` and `PUT /projects/{projectId}` with manager authorization. |
| Authentication and access | Original MVP docs left access policy undefined. | Access must be defined to test distinct user access. | User approved project-scoped MANAGER/MEMBER access. For Exercise 3, HTTP session authentication provides login, profile, and logout; roles remain scoped to memberships. |
| Comments and assignees | Not part of the original MVP. | Exercise 2 requires their schema and reproducible demo records; assignment also needs eligible-user selection. | Included as exercise scope only. Schema, entities, API endpoints, and demo data are present; this is not a new confirmed product requirement. |
| Project deletion | Tasks, assignments, and comments depend on projects through tasks. | Cascading deletion risks removing dependent work. | Keep the existing safe rule: return `409 PROJECT_HAS_TASKS` while tasks exist; only delete memberships when a task-free project is deleted. |
| Duplicate project names | US-01 prohibits duplicate names and the API has a `409` response. | Comparison semantics needed confirmation. | User confirmed trimmed, case-insensitive uniqueness; API validation and a normalized-name unique index enforce it. |
| Task field rules | The Exercise 5 brief specifies title 3-150, description up to 2000, statuses TODO/IN_PROGRESS/DONE, and priorities LOW/MEDIUM/HIGH. | Previous implementation used older status values and had no priority or task detail endpoint. | Updated task entity, schema, API, interface, and acceptance tests to the Exercise 5 field contract. Existing `review`/`cancelled` values have no approved equivalent and are rejected by the schema migration rather than reinterpreted silently. |
| Wireframe coverage | The wireframe shows project selection, task status, search, and task details. | It does not show task creation/removal confirmations, authentication, comments, member selection, or assignment flows. It also displays relative dates although deadlines are not in the MVP data model or requirements. | Use it as a structural reference only. Do not implement deadline fields based on the wireframe; revise the wireframe before completing the UI increment. |

## Decisions

### Confirmed by repository documentation

- The product MVP remains project/task focused; comments, assignments, and authentication are exercise-specific extensions.
- The project/task MVP and the fields/status values in the current data model and API are the documented baseline.
- Authentication, advanced user management, comments, and detailed change history are listed outside the initial MVP.
- The current OpenAPI contract has twelve paths and twenty HTTP operations, including the four session lifecycle endpoints; the two core project/task resources remain the MVP focus.

### Confirmed by the user for this plan

- Java and Spring Boot are the stack confirmed in Class 02.
- The user delegated the database choice and selection of the two simple Ficha 02 operations.
- The machine has Java 25.0.3 installed (Java 26.0.1 is the shell default); the user requested using Java 25 and Gradle Wrapper.
- The user approved project-scoped MANAGER/MEMBER permissions and active project-member eligibility for task assignments.
- For Exercise 6, the user approved multiple assignees per task, preventing duplicate user/task pairs, manager-only assignment management under the previously approved access policy, and the proposed per-assignment API routes.
- The user selected HTTP session authentication for Exercise 3 so the application can support a real login/profile/logout lifecycle.
- For Exercise 4, the user confirmed project names should be compared case-insensitively after trimming.
- Exercise 5 requires task title length 3-150, description up to 2000 characters, statuses `TODO`, `IN_PROGRESS`, `DONE`, and priorities `LOW`, `MEDIUM`, `HIGH`, with defaults `TODO` and `MEDIUM`.

### Selected for this exercise

- H2 is selected as the database, with Spring Data JPA, file-based local persistence, and in-memory storage for automated tests.
- `POST /projects` and `POST /tasks` are selected as the two simple Ficha 02 operations. They form a proposed baseline subset, not the full MVP API.
- A plain HTML/CSS/JavaScript frontend served by Spring Boot is selected as the simplest UI approach; no frontend framework is introduced.
- Session authentication uses email/password, a server-side session, an HttpOnly `JSESSIONID` cookie, and CSRF tokens for state-changing requests.
- Project names must be 3-100 characters, descriptions are limited to 1000 characters, names are trimmed and case-insensitively unique, and project list search is available by name.
- There is no pagination requirement in the current API contract.
- Users, memberships, projects, tasks, assignments, and comments are represented in the schema; demo seeding is idempotent and non-destructive.

### Requires instructor/client confirmation

- Whether the exercise's session and access policy are suitable for any future production release.
- Whether the larger CRUD/search scope is part of this implementation beyond the two Ficha 02 operations.
- Whether projects can be edited and how project deletion behaves when tasks exist.
- Duplicate-name comparison rules and how to map historical `review` or `cancelled` tasks if either is found. The current local database contained only `backlog` and `in_progress`.

These are decision gates, not assumptions. Do not implement the affected behavior until the decision is recorded.

## Incremental implementation sequence

### Increment 0 - Confirm exercise scope and access decisions

**Status:** Done for the Exercise 2 baseline. Unresolved product decisions remain parked and must not be inferred for later features.

**Components/files:** `docs/requirements.md`, `docs/user-stories.md`, `docs/constraints.md`, `docs/architecture.md`, `docs/database.md`, `openapi.yaml`.

**Dependencies:** None.

**Acceptance criteria:**
- Record the selected Ficha 02 baseline: `POST /projects`, then `POST /tasks`.
- Record the user-approved access policy and session authentication lifecycle.
- Record comments/assignments as Exercise 2 persistence scope, with active project members as eligible assignees.
- Record the project CRUD contract, no-task deletion rule, case-insensitive duplicate-name rule, and Exercise 5 task field/status/priority contract.
- Update affected requirements, data model, and API contract so they do not contradict one another.

**Verification:** Review the recorded decisions against instructor/client approval; lint `openapi.yaml`; cross-check field names and operations between the API and data model.

### Increment 1 - Initialize the Spring Boot application

**Status:** Done for local startup; `./gradlew clean test` passes on Java 25.0.3, `/api/health` returned HTTP 200, and the static application pages were served by the backend.

**Components/files:** `build.gradle`, `settings.gradle`, `gradlew`, `gradlew.bat`, `gradle/wrapper/`, Spring Boot entry point, application configuration, and test configuration.

**Dependencies:** Increment 0.

**Acceptance criteria:**
- Application builds with the Java 25 toolchain and Gradle Wrapper.
- A simple health/root response is available for local verification.
- H2 file-based configuration is used for local runs and in-memory H2 for tests.
- Configuration is documented; no credentials or secrets are committed.
- Static frontend can be served without adding a frontend framework.

**Verification:** Select the installed Java 25 JDK, run `./gradlew test`, start with `./gradlew bootRun`, and request `/api/health`.

### Increment 2 - Implement the confirmed data model and persistence

**Status:** Done; H2 schema migration, JPA entities/repositories, and non-destructive demo-data initializer are implemented. Integration tests validate seeded data and initializer idempotence; the task-status/priority upgrade was also verified on a disposable copy of the existing file-backed database.

**Components/files:** `src/main/resources/schema.sql`, `application.properties`, `.env.example`, domain entities, repositories, `DemoDataInitializer`, and persistence tests.

**Dependencies:** Increment 1; decisions on uniqueness, status values, and project deletion behavior from Increment 0.

**Acceptance criteria:**
- Persist User, Project, ProjectMembership, Task, TaskAssignment, and TaskComment records.
- Enforce the Task-to-Project relationship.
- Enforce membership uniqueness and manager/member role values.
- Seed at least two users and projects with distinct memberships, tasks, an assignment, and a comment only when records are absent.
- Preserve existing data across restarts; never drop tables or reset seed records.
- Enforce project-name uniqueness only according to the confirmed comparison rule.
- Apply the approved behavior when deleting a project with associated tasks.

**Verification:** Run Gradle integration tests; start and stop twice against the file-backed database, then verify records remain present. Check database connectivity/schema via `/api/health` and application startup logs.

### Increment 3 - Implement the approved API operations

**Status:** In progress; project/task operations, multi-assignee selection/removal, task comments, and the session lifecycle are implemented. Integration tests cover assignment eligibility, duplicates, individual removal, comment validation, backend-derived authorship/time, and reload queries.

**Components/files:** Spring Boot REST controllers and request/response DTOs for the approved operations; `openapi.yaml` remains the contract source for review.

**Dependencies:** Increment 2; exact Ficha 02 scope, access policy, and operation list from Increment 0.

**Acceptance criteria:**
- Implement exactly the operations approved for the exercise and MVP.
- For the Ficha 02 baseline, implement `POST /projects` and `POST /tasks`; require a project to exist before creating its task.
- Complete project operations with `GET /projects`, `GET /projects/{projectId}`, `POST /projects`, `PUT /projects/{projectId}`, and the documented deletion operation.
- Filter project lists by case-insensitive name query; the contract does not define pagination.
- Validate names (3-100 characters) and descriptions (up to 1000 characters), enforce trimmed case-insensitive uniqueness, and return `409` for duplicates.
- Associate newly created projects with the authenticated creator as manager; authorize reads by membership and edits/removals by manager role.
- Retain the documented safe deletion behavior: a project with tasks returns `409` and no dependent data is deleted.
- Enforce task title 3-150, description up to 2000, defaults TODO/MEDIUM, and the specified status/priority enums.
- Filter tasks by text, status, and priority; return total/status counts and keep the list unpaginated as the contract does not define pagination.
- Validate required fields, references, status values, and not-found/conflict cases.
- Keep response codes and response shapes consistent with the OpenAPI contract.
- Implement login, profile, and logout with server-side sessions and CSRF protection.
- Enforce session authentication and the approved project membership policy.
- Derive project ownership and comment authorship from the authenticated principal.
- Implement project-member selection, task comments, and task assignment for the exercise.
- Allow multiple assignees per task, reject duplicate user/task pairs in the backend/database, and allow managers to remove one assignment at a time.
- Redirect unauthenticated/expired UI sessions to sign-in and show a readable message.

**Verification:** Java 25 API integration tests pass; the OpenAPI YAML parses and its templated paths have matching path parameters. Redocly lint was not available in the environment. Manually verify redirect and expired-session messaging in a browser.

### Increment 4 - Implement the MVP interface

**Status:** In progress; sign-in/session guard, project/task management, assignee selection/removal, and task comment interface are implemented. Final browser walkthrough remains.

**Components/files:** Static HTML/CSS/JavaScript views based on [wireframe.png](ui/wireframe.png).

**Dependencies:** Increment 3; user-approved project access policy and assignee eligibility.

**Acceptance criteria:**
- User can create, list, search, consult, and edit accessible projects; project selection survives page reloads.
- The project list and detail views are consistent with membership-scoped API access.
- Only managers see project edit/removal controls; non-managers receive clear API errors for direct unauthorized requests.
- Project removal requires browser confirmation and displays the documented conflict when tasks exist.
- Project data is reloaded from the API on page load so it remains visible after a backend restart.
- Project managers can create/delete tasks; project members can update task fields as permitted by the approved exercise policy.
- Task title, description, status, priority, filters, and counts match the API contract and update without a page reload.
- Managers can add multiple eligible assignees and remove one without affecting the others; the list reappears after reloading the project/task list.
- Project members can add comments and view each comment's author and date; whitespace-only and over-2000-character content is rejected.
- Comment authorship and timestamps come from the backend, and comments are returned again after fetching/reopening the task.
- Do not show deadline/date fields unless they are added to the approved requirements and data model.
- Destructive actions ask for confirmation before issuing removal requests.
- Empty results and API validation errors are presented clearly.
- Do not add a UI for roles, comments, or assignments beyond the approved exercise scope.

**Verification:** Earlier project create/list/detail, reload, and backend-restart behavior was checked in a browser. JavaScript syntax validation passes. The Exercise 6 assignee/comment flows and remaining project error/confirmation states still need a manual browser walkthrough.

### Increment 5 - End-to-end verification and handoff

**Status:** In progress; automated task CRUD/access/filter, multi-assignee, and comment checks pass. Final browser walkthrough and optional Redocly lint remain.

**Components/files:** Automated test suite, OpenAPI contract, README, architecture, data model, and implementation plan.

**Dependencies:** Increments 2, 3, and 4.

**Acceptance criteria:**
- All MVP acceptance criteria map to a test or a documented manual check.
- API, persistence, and UI agree on field names, status values, and operation behavior.
- No unresolved decision is represented as an implemented business rule.
- Documentation reflects the delivered behavior and current plan status.

**Verification:** Run the full configured test suite, OpenAPI lint, and a documented end-to-end manual walkthrough; review `git diff` for scope and consistency.

## Plan maintenance

Update each increment's status (`Not started`, `In progress`, `Blocked`, or `Done`) in this document as work proceeds. Mark an increment `Done` only after all acceptance criteria and verification steps pass. Record new decisions with their source (instructor/client confirmation) before changing implementation scope.
