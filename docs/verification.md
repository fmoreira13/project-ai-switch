# Matriz - MVP Verification

## Purpose and scope

This report records the expected and observed behavior for the nine Exercise 8 scenarios. Automated checks use Spring Boot integration tests, MockMvc, and an in-memory H2 database. Manual UI results below are based on the user's reports.

The current access policy is project-scoped: managers can manage projects and create/remove tasks; members can view projects and view/update tasks. Both roles can comment; only managers can manage assignments.

## Scenario results

| Case | Scenario | Expected result | Observed result and evidence | Verification |
|---|---|---|---|---|
| 01 | Application access | Valid credentials start a session; invalid credentials and protected requests without a session are rejected. | **Passed.** The user opened the application and signed in to run the reported browser checks. `loginProfileAndLogoutUseAnHttpSession`, `loginRejectsInvalidCredentialsAndStateChangesRequireCsrf`, and `unauthenticatedRequestsAreRejected` verify session lifecycle, invalid credentials (`401`), missing CSRF (`403`), and unauthenticated API access (`401`). | User-reported successful browser access plus automated authentication tests. |
| 02 | Project management | Create, search, update, and delete work according to project membership and role. | **Passed manually and automatically.** The user tested project editing and deletion. Automated tests verify create, case-insensitive search, manager permissions, and the dependent-task conflict. | Manual user confirmation plus integration tests. |
| 03 | Task persistence | A created task appears without reloading and remains after restarting the Backend. | **Passed manually, as reported by the user.** The user confirmed that the task remained available after stopping and restarting the Backend. Automated API evidence is provided by `exercise3CreatesTaskReadsItBackAndRejectsInvalidTitle`: it checks the create and detail responses, repository persistence, default values, and rejection of a whitespace-only title. | Manual user confirmation; the automated test uses in-memory H2 and does not itself prove restart persistence. The exact task identifier and restart logs were not recorded. |
| 04 | Data validation | Empty, whitespace-only, and invalid values are rejected. | **Passed manually and automatically.** The user reports testing invalid input and error behavior in the interface. `exercise8RejectsWhitespaceOnlyProjectNames`, `projectValidationAndCaseInsensitiveDuplicateNamesAreEnforced`, `taskCrudFiltersValidationAndCountsFollowMvpContract`, and `taskTitleAndDescriptionHonorExactLengthBoundaries` cover API validation rules. | Manual user confirmation plus automated backend tests. |
| 05 | Status and priority | Edits update the task, filters, and status counts consistently. | **Passed manually and automatically.** The user reports successfully testing filters and editing status/priority, then checking that the interface and counts update. `taskCrudFiltersValidationAndCountsFollowMvpContract` verifies API edits, filtered results, and count fields. | Manual user confirmation plus integration tests. |
| 06 | Assignees | Assign/remove works and duplicate task/user associations are rejected. | **Passed manually and automatically.** The user reports successfully testing assignment add/remove. `managerCanAssignOnlyAnActiveMemberOfTheProject` verifies eligible selection, duplicate rejection (`409`), and removal. | Manual user confirmation plus integration tests; duplicate rejection is verified automatically at the API. |
| 07 | Comments | Content, authenticated author, and server date are persisted and visible. | **Passed manually and automatically.** The user reports testing comments, reopening the task and checking the author/date. `commentsAreValidatedServerAuthoredAndAvailableWhenTaskIsReopened` verifies trimming, validation, authenticated author, server timestamp, and subsequent API retrieval. | Manual user confirmation plus integration test. |
| 08 | API authorization | Another user cannot access or change a resource by substituting its identifier. | **Passed automatically.** `exercise8RejectsAccessToAnotherUsersProjectEvenWithItsId` sends the other user's project ID to both `GET` and `PUT`; both return `403 PROJECT_ACCESS_DENIED`. `projectMemberCanCommentAndUpdateTaskButCannotCreateOrAssignTasks` checks role restrictions. | Automated direct API integration tests; manual API manipulation was not separately reported. |
| 09 | Deletion and errors | Dependencies are respected; API failures do not show success or discard form input. | **Passed manually and automatically.** The user reports testing invalid input, error feedback, and project deletion behavior. Automated tests verify the project-with-tasks conflict (`409 PROJECT_HAS_TASKS`) and task dependent-record cleanup. | Manual user confirmation plus integration tests. |

## Automated tests added for Exercise 8

Three focused integration tests were added to `MatrizAccessIntegrationTests`:

1. `exercise8RejectsWhitespaceOnlyProjectNames` — submits an all-whitespace name and expects `400 VALIDATION_ERROR`.
2. `exercise8RejectsAccessToAnotherUsersProjectEvenWithItsId` — attempts to read and update another user's project using its real identifier and expects `403 PROJECT_ACCESS_DENIED`.
3. `exercise3CreatesTaskReadsItBackAndRejectsInvalidTitle` — creates a task through the API, verifies its fields and database row, reads it back through the API, then confirms a whitespace-only title returns `400 VALIDATION_ERROR` without inserting another task. The test runs transactionally against in-memory H2, so its mutations roll back after the test.

The first full run exposed an incorrect expected error code in the new authorization test: the observed response was `PROJECT_ACCESS_DENIED`, while the test expected `ACCESS_DENIED`. `AccessService.requireMembership` confirms `PROJECT_ACCESS_DENIED` is the established response. The assertion was corrected; the application authorization behavior did not require a change. The full suite then passed.

## Code review finding and regression test

Reviewing the login UI against the API behavior exposed a frontend bug: `apiFetch` redirected every `401` to the expired-session page, including `POST /auth/login`. That prevented `login.js` from displaying its existing invalid-credentials message. The correction leaves login `401` responses with the login form while preserving redirects for unauthorized protected API requests.

The regression tests in `src/test/js/auth.test.mjs` reproduce both branches with mocked HTTP responses: invalid credentials remain on the sign-in flow, while a protected API `401` redirects to sign-in. No production credentials or network calls are used by these tests.

The clean-checkout review also found that the H2 URL in `.env.example` contains a semicolon and was unquoted even though the README sources the file as shell code. Quoting the value ensures the complete JDBC URL is loaded. A shell assertion verifies the sourced value without printing credentials.

The code-quality opportunity was the duplicate `PathVariable` import in `ProjectController`; it was removed in a separate cleanup change. A larger frontend or service refactor was not applied because it would increase delivery risk without a focused behavior or maintainability gain for this exercise.

## Examples of AI-assisted work

| AI request objective | Programmer decision | Verification |
|---|---|---|
| Identify gaps in the nine Exercise 8 scenarios and propose focused tests. | Added separate tests for whitespace-only validation, authorization with another user's project ID, and create-then-read persistence. Kept in-memory test evidence separate from the user's later manual confirmation of restart persistence. | `./gradlew clean test`; the JUnit integration suite passed. The manual restart result was reported by the user and is recorded separately in Case 03. |
| Review the new authorization test and expected denial response. | Corrected the suggested generic `ACCESS_DENIED` expectation after the observed response and `AccessService` showed the established code is `PROJECT_ACCESS_DENIED`. This changed the test expectation, not the authorization implementation. | Re-ran the full JUnit suite; it passed with both unauthorized read and update checks returning `403 PROJECT_ACCESS_DENIED`. |
| Review the sign-in and session flow for errors that could be misreported by the interface. | Confirmed and fixed the issue where a login `401` was treated as an expired authenticated session. Kept the redirect for `401` responses from protected resources. | `node --experimental-default-type=module --test src/test/js/auth.test.mjs` checks both login and protected-resource branches. |

## Commands and results

Executed from the repository root on macOS:

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
PATH="$(/usr/libexec/java_home -v 25)/bin:$PATH" \
./gradlew clean test
```

**Final result:** `BUILD SUCCESSFUL`; all 19 tests passed using Java 25.0.3. The tests use the configured in-memory H2 database.

JavaScript syntax and diff checks:

```sh
node --check src/main/resources/static/js/index.js
node --check src/main/resources/static/js/auth.js
node --check src/main/resources/static/js/login.js
git diff --check
```

**Result:** all commands passed. These checks do not substitute for browser-based verification.

Frontend auth regression tests:

```sh
node --experimental-default-type=module --test src/test/js/auth.test.mjs
```

**Result:** 2 tests passed, including the invalid-login regression and the protected-resource expired-session redirect.

OpenAPI and review checks:

```sh
ruby -e 'require "yaml"; d = YAML.load_file("openapi.yaml"); abort "missing OpenAPI 3.1" unless d["openapi"] == "3.1.0"; abort "missing paths" unless d["paths"].is_a?(Hash) && d["paths"].any?; puts "OpenAPI YAML parsed: #{d["paths"].length} paths"'
git diff --check
```

**Result:** OpenAPI YAML parsed with 13 paths, including the health check and implemented legacy task deletion route. The diff whitespace check passed.

Environment example load check:

```sh
bash -c 'set -a; source .env.example; test "$MATRIZ_DB_URL" = "jdbc:h2:file:./data/matriz;DB_CLOSE_ON_EXIT=FALSE" && echo ".env.example shell load passed"'
```

**Result:** `.env.example` loads with the complete H2 JDBC URL.

## Manual checks and limitations

The user has since confirmed that a task remained available after restarting the Backend. This report treats that as a manual user-reported result, not as an independently repeated test. The user reports that the four remaining UI groups were all tested successfully: assignee add/remove, status/priority edit and count/filter refresh, validation/error feedback, and comment author/date after reopening. Combined with earlier user reports, browser-level application access, project editing/deletion, filtering, comments, and task persistence after Backend restart are confirmed by the user. Automated API integration tests independently cover validation, authorization, assignment duplication, dependent deletion rules, comments, and create/read persistence.

The user-reported manual checks are recorded as evidence; detailed browser logs, screenshots, and test-data identifiers were not captured in this report. API authorization cases are verified by automated tests rather than an independently reported direct-API browser test.

An application startup was previously attempted through the Java debugger on a temporary port and did not remain active in that attempt. The user later confirmed that `http://localhost:8080` worked; that user report supersedes the earlier unsuccessful attempt as evidence that the local app can start. The earlier attempt is retained here as history, not as a current blocker.

No known Exercise 8 acceptance scenario remains without either user-reported browser evidence or automated API verification. Manual API identifier tampering was not separately reported; that authorization case is covered by the automated integration test. Automated integration tests use in-memory H2, while the separate user-confirmed restart test demonstrates file-backed persistence.
