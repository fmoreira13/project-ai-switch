# Matriz - Task and Access Test Plan

## Objective and scope

Define essential, requirement-based tests for task title validation, task creation and consultation, and denial of access to a task by a user who is not a member of its project. These cases cover the selected Exercise 1 scope; they do not represent exhaustive task or authorization testing.

## Basis and reviewed decisions

- [US-02](user-stories.md) requires a task title with 3-150 non-whitespace characters, a valid project association, manager-only task creation, and task consultation by project members.
- [US-03](user-stories.md) requires tasks to be listed under their project.
- The approved exercise access policy is documented in [architecture.md](architecture.md): a manager creates tasks for their project; project members can consult tasks; a non-member cannot access project resources. This is the exercise policy, not a production security requirement.
- [openapi.yaml](../openapi.yaml) defines `POST /tasks`, `GET /tasks/{taskId}`, the `201`, `400`, `403`, and `200` response classes, and defaults of `TODO` and `MEDIUM`.
- The OpenAPI `minLength: 3` constraint does not by itself reject a three-character whitespace-only title. The user-story rule and the OpenAPI description explicitly require non-whitespace content, so the whitespace case must be validated by the application, not inferred from `minLength` alone.

The test plan follows the agreed access rules instead of treating authentication or roles as original product requirements: those were not defined in the initial requirements document and were approved specifically for this implementation exercise.

## Test environment and commands

- Use Java 25 and the Gradle Wrapper from the repository root.
- Backend integration tests use Spring Boot, MockMvc, JUnit, and an isolated in-memory H2 database. The test context includes fictitious demo users and projects; it does not use or alter the local file-backed database under `data/`.
- Use test-context sessions and CSRF tokens for authenticated state-changing requests. Do not rely on the local `.env`, real credentials, or manually created records in the persistent demo database.
- Run the focused existing suite with:

  ```sh
  ./gradlew test --tests com.matriz.MatrizAccessIntegrationTests
  ```

- Run the full backend suite with:

  ```sh
  ./gradlew clean test
  ```

- To start the local application for a separate manual walkthrough, follow the Java 25, `.env`, and `./gradlew bootRun` instructions in [README.md](../README.md). Manual checks should use only the fictitious demo accounts, and must not reset or delete `data/`.

## Selected scenarios

| ID | Scenario and level | Inputs and setup | Expected result |
|---|---|---|---|
| TP-TASK-01 | Reject an invalid title — API integration (MockMvc + in-memory H2) | Authenticate as the manager of an existing test project; send `POST /tasks` with that project's valid `projectId` and `title: "   "` (also cover a title shorter than 3 non-whitespace characters, such as `" a "`). Include CSRF. | HTTP `400` with the API's structured validation error. No task is created for the invalid request. |
| TP-TASK-02 | Create and consult a task — API integration (MockMvc + in-memory H2) | Authenticate as a project manager; send `POST /tasks` with a valid project ID and a unique title, omitting status and priority. Capture the returned task ID and use it in a subsequent `GET /tasks/{taskId}` within the same test. | Creation returns HTTP `201`; the response identifies the selected project and has the trimmed title with defaults `TODO` and `MEDIUM`. The subsequent GET returns HTTP `200` and the same task ID, project, title, status, and priority. Confirm the row exists through the test repository where useful. |
| TP-AUTH-01 | Deny task access to a non-member — API integration (MockMvc + in-memory H2) | Create a task in a project owned by test user A. Authenticate as test user B, who has no membership in that project, and call `GET /tasks/{taskId}` using the real task ID. | HTTP `403` with the API's structured forbidden error; the task representation is not returned. This verifies project authorization on direct API access, not only hiding the task in the UI. |

## Unit test: task title validation

The rule selected for Exercise 2 is the request-level title constraint in `ApiModels.CreateTaskRequest`, which uses Jakarta Bean Validation (`@NotBlank` and `@Size(min = 3, max = 150)`). `TaskTitleValidationTest` uses JUnit 5 and a standalone Jakarta `Validator`; it does not start Spring or access a database.

| Test | Input | Requirement asserted |
|---|---|---|
| `acceptsTitleAtMinimumLength` | `"Fix"` (3 characters) | A non-blank title at the documented minimum is accepted. |
| `rejectsWhitespaceOnlyTitle` | `"   "` | A title containing only spaces is rejected, even though its raw length is 3. |
| `enforcesMaximumTitleLength` | 150 characters, then 151 characters | The documented maximum is accepted and a value one character over the maximum is rejected. |

Focused command:

```sh
./gradlew test --tests com.matriz.TaskTitleValidationTest
```

Result: passed on 2026-11-11 (3 JUnit tests, 0 failures). The assertions exercise the declared request constraints, not `TaskService`'s trimming behavior; the separate API integration tests cover service-level rejection of a short title after trimming. The rule was not changed to satisfy the tests.

## Integration test: create and consult a task

`exercise3CreatesTaskReadsItBackAndRejectsInvalidTitle` in [MatrizAccessIntegrationTests.java](../src/test/java/com/matriz/MatrizAccessIntegrationTests.java) exercises the API using MockMvc and the project's Spring Boot test context with in-memory H2. It logs in as the manager of the seeded Alpha project, submits a valid task through `POST /tasks` with CSRF, then requests the returned ID through `GET /tasks/{taskId}`.

The test checks HTTP `201` then `200`, the ID, project association, title, and default `TODO` status and `MEDIUM` priority in the API responses; it also confirms the saved entity is present in the repository. Its negative step submits a whitespace-only title and expects HTTP `400` with `VALIDATION_ERROR`, while asserting that the invalid request added no task. The test method is transactional and rolls back its changes after execution, so its created task does not accumulate between runs; the in-memory database is isolated from local `data/`.

Focused command:

```sh
./gradlew test --tests 'com.matriz.MatrizAccessIntegrationTests.exercise3CreatesTaskReadsItBackAndRejectsInvalidTitle'
```

Result: passed on 2026-11-11 (1 JUnit integration test, 0 failures). This verifies database persistence and retrieval through the API within a controlled test context; it does not claim that this test restarts the Backend or verifies the local file-backed database across restarts.

## Review notes and limits

- The invalid-title input is deliberately whitespace-only: a title such as `"   "` may satisfy a simple string-length check while violating the user story's non-whitespace requirement. A short mixed-space title additionally checks trimming does not turn an otherwise too-short value into a valid title.
- The create-and-consult case verifies persistence across separate API requests in one test context. Because that context uses in-memory H2, it does not prove persistence after restarting the application; restart persistence is a separate manual scenario.
- The unauthorized case uses an authenticated user without project membership. An unauthenticated request (`401`) and a project member attempting a manager-only operation are useful additional cases, but were not selected as one of the three essential cases for this exercise.
- Existing coverage is in [MatrizAccessIntegrationTests.java](../src/test/java/com/matriz/MatrizAccessIntegrationTests.java): `taskCrudFiltersValidationAndCountsFollowMvpContract` checks trimmed-short task titles, `exercise3CreatesTaskReadsItBackAndRejectsInvalidTitle` covers the Exercise 3 create/read/invalid-title flow, and `exercise8RejectsAccessToAnotherUsersProjectEvenWithItsId` covers denial on a project resource. The project-level authorization test is not a substitute for the task-specific `GET /tasks/{taskId}` scenario.
