# Matriz - Implementation Plan

## Status

Implementation is in progress. The Gradle/Spring Boot application, H2 schema, entities, security, and demo initializer are present. A clean Java 25 Gradle test run and OpenAPI lint pass; local HTTP startup and persistence across separate restarts remain unverified.

## Scope and evidence

This plan is based on [README.md](../README.md), [AGENTS.md](../AGENTS.md), [requirements.md](requirements.md), [user-stories.md](user-stories.md), [constraints.md](constraints.md), [architecture.md](architecture.md), [database.md](database.md), [openapi.yaml](../openapi.yaml), and the [wireframe](ui/wireframe.png).

The documented MVP covers project and task creation, listing, task status updates, removal, simple task search, and an overview of ongoing work. This exercise additionally requires persistence for users, comments, and assignments and sample data for distinct access tests. The repository now includes application source and integration tests.

## Findings and gaps to resolve

| Topic | Current evidence | Gap or contradiction | Action / status |
|---|---|---|---|
| Class 02 technology | The user confirmed Java and Spring Boot as the Class 02 stack and requested the installed Java 25 with Gradle Wrapper. | The shell default is Java 26.0.1, with Java 25.0.3 also installed. | The project targets Java 25, uses Gradle Wrapper, Spring Data JPA, and H2. README documents selecting Java 25. |
| Two Ficha 02 operations | The user delegated the choice of two simple operations based on existing documentation and OpenAPI. | The Ficha 02 baseline is narrower than the documented MVP. | Select `POST /projects` (create a project) and `POST /tasks` (create a task in an existing project). Other API operations support broader MVP/exercise scope. |
| Project update | Requirements include create, consult, update, and remove projects. | OpenAPI has no project update operation. | Confirm whether project editing is in the implementation scope; if so, add it to the contract and plan before coding. |
| Authentication and access | Original MVP docs left access policy undefined. | Access must be defined to test distinct user access. | User approved project-scoped MANAGER/MEMBER access: managers manage projects/tasks and assign; members view/update tasks and comment; only active project members are eligible assignees. HTTP Basic is used for this local exercise. |
| Comments and assignees | Not part of the original MVP. | Exercise 2 requires their schema and reproducible demo records; assignment also needs eligible-user selection. | Included as exercise scope only. Schema, entities, API endpoints, and demo data are present; this is not a new confirmed product requirement. |
| Project deletion | Project-task relationship is documented, but deletion behavior for a project with tasks is not. | Cascading deletion versus preventing deletion is a business/data-loss decision. | Confirm with the instructor before implementing project removal. |
| Duplicate project names | US-01 prohibits duplicate names and the API has a `409` response. | Database model does not define uniqueness or case/whitespace comparison behavior. | Confirm comparison semantics; implement the matching database constraint and tests. |
| Status lifecycle | API and data model define `backlog`, `in_progress`, `review`, `done`, and `cancelled`; user stories only say the value must be valid. | Status values and any transition restrictions are not clearly approved by the requirements. | Confirm the enum and whether transitions are unrestricted; otherwise keep API and tests aligned to the approved set. |
| Wireframe coverage | The wireframe shows project selection, task status, search, and task details. | It does not show task creation/removal confirmations, authentication, comments, member selection, or assignment flows. It also displays relative dates although deadlines are not in the MVP data model or requirements. | Use it as a structural reference only. Do not implement deadline fields based on the wireframe; revise the wireframe before completing the UI increment. |

## Decisions

### Confirmed by repository documentation

- The product MVP remains project/task focused; comments, assignments, and authentication are exercise-specific extensions.
- The project/task MVP and the fields/status values in the current data model and API are the documented baseline.
- Authentication, advanced user management, comments, and detailed change history are listed outside the initial MVP.
- The current OpenAPI contract has five paths and twelve HTTP operations; the two base resources remain `/projects` and `/tasks`.

### Confirmed by the user for this plan

- Java and Spring Boot are the stack confirmed in Class 02.
- The user delegated the database choice and selection of the two simple Ficha 02 operations.
- The machine has Java 25.0.3 installed (Java 26.0.1 is the shell default); the user requested using Java 25 and Gradle Wrapper.
- The user approved project-scoped MANAGER/MEMBER permissions and active project-member eligibility for task assignments.

### Selected for this exercise

- H2 is selected as the database, with Spring Data JPA, file-based local persistence, and in-memory storage for automated tests.
- `POST /projects` and `POST /tasks` are selected as the two simple Ficha 02 operations. They form a proposed baseline subset, not the full MVP API.
- A plain HTML/CSS/JavaScript frontend served by Spring Boot is selected as the simplest UI approach; no frontend framework is introduced.
- HTTP Basic is used for local exercise authentication.
- Users, memberships, projects, tasks, assignments, and comments are represented in the schema; demo seeding is idempotent and non-destructive.

### Requires instructor/client confirmation

- Whether the exercise's Basic Auth and access policy are suitable for any future production release.
- Whether the larger CRUD/search scope is part of this implementation beyond the two Ficha 02 operations.
- Whether projects can be edited and how project deletion behaves when tasks exist.
- Duplicate-name comparison rules and approved task statuses/transitions.

These are decision gates, not assumptions. Do not implement the affected behavior until the decision is recorded.

## Incremental implementation sequence

### Increment 0 - Confirm exercise scope and access decisions

**Status:** Done for the Exercise 2 baseline. Unresolved product decisions remain parked and must not be inferred for later features.

**Components/files:** `docs/requirements.md`, `docs/user-stories.md`, `docs/constraints.md`, `docs/architecture.md`, `docs/database.md`, `openapi.yaml`.

**Dependencies:** None.

**Acceptance criteria:**
- Record the selected Ficha 02 baseline: `POST /projects`, then `POST /tasks`.
- Record the user-approved access policy and HTTP Basic local exercise authentication.
- Record comments/assignments as Exercise 2 persistence scope, with active project members as eligible assignees.
- Record project update/deletion semantics, duplicate-name behavior, and task status rules as pending decisions; do not extend implementation based on assumptions.
- Update affected requirements, data model, and API contract so they do not contradict one another.

**Verification:** Review the recorded decisions against instructor/client approval; lint `openapi.yaml`; cross-check field names and operations between the API and data model.

### Increment 1 - Initialize the Spring Boot application

**Status:** In progress; `./gradlew clean test` passes on Java 25.0.3. A debugger session reported startup, but the subsequent HTTP health probe could not connect and the session was no longer active. Do not treat startup as verified until `/api/health` responds from a persistent local server.

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

**Status:** In progress; idempotent H2 schema, JPA entities/repositories, and demo-data initializer are present. Integration tests validate schema and seeded access data using in-memory H2, including that rerunning the initializer neither duplicates nor deletes records. The file-backed database was created during the startup attempt, but persistence across separate application restarts has not yet been verified.

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

**Status:** In progress; core project/task endpoints and member/comment/assignment endpoints are implemented. `./gradlew clean test` passes on Java 25.0.3, and Redocly lint validates `openapi.yaml`; a manual HTTP walkthrough remains outstanding.

**Components/files:** Spring Boot REST controllers and request/response DTOs for the approved operations; `openapi.yaml` remains the contract source for review.

**Dependencies:** Increment 2; exact Ficha 02 scope, access policy, and operation list from Increment 0.

**Acceptance criteria:**
- Implement exactly the operations approved for the exercise and MVP.
- For the Ficha 02 baseline, implement `POST /projects` and `POST /tasks`; require a project to exist before creating its task.
- Validate required fields, references, status values, and not-found/conflict cases.
- Keep response codes and response shapes consistent with the OpenAPI contract.
- Enforce HTTP Basic authentication and the approved project membership policy.
- Implement project-member selection, task comments, and task assignment for the exercise.

**Verification:** API tests cover each approved operation and its error cases; run Redocly lint on `openapi.yaml`; compare implemented routes against documented operations.

### Increment 4 - Implement the MVP interface

**Status:** Not started; depends on Increment 3.

**Components/files:** Static HTML/CSS/JavaScript views based on [wireframe.png](ui/wireframe.png).

**Dependencies:** Increment 3; user-approved project access policy and assignee eligibility.

**Acceptance criteria:**
- User can perform the approved project/task flows represented by the MVP.
- Project selection, task status, and simple search are represented consistently with the API.
- Do not show deadline/date fields unless they are added to the approved requirements and data model.
- Destructive actions ask for confirmation before issuing removal requests.
- Empty results and API validation errors are presented clearly.
- Do not add a UI for roles, comments, or assignments beyond the approved exercise scope.

**Verification:** Manual walkthrough of the approved flows and browser checks for empty, success, and error states; update the wireframe if approved decisions materially change the UI.

### Increment 5 - End-to-end verification and handoff

**Status:** Not started; depends on Increments 2-4.

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
