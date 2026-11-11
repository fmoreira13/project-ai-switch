# Matriz - MVP Verification

## Purpose and scope

This report records the expected and observed behavior for the nine Exercise 8 scenarios. Automated checks use Spring Boot integration tests, MockMvc, and an in-memory H2 database. They verify backend/API behavior, not a real browser session or persistence across a separate Backend process restart.

The current access policy is project-scoped: managers can manage projects and create/remove tasks; members can view projects and view/update tasks. Both roles can comment; only managers can manage assignments.

## Scenario results

| Case | Scenario | Expected result | Observed result and evidence | Verification |
|---|---|---|---|---|
| 01 | Application access | Valid credentials start a session; invalid credentials and protected requests without a session are rejected. | **Passed automatically.** `loginProfileAndLogoutUseAnHttpSession`, `loginRejectsInvalidCredentialsAndStateChangesRequireCsrf`, and `unauthenticatedRequestsAreRejected` verify sign-in/profile/logout, invalid credentials (`401`), missing CSRF (`403`), and unauthenticated API access (`401`). | Automated; browser sign-in and expired-session messaging remain unverified. |
| 02 | Project management | Create, search, update, and delete work according to project membership and role. | **Passed automatically.** `managerCanCreateListSearchConsultAndUpdateProjects` verifies create, case-insensitive search, detail, update, and deletion. `projectDetailsAndMutationsRespectMembershipAndManagerRole` verifies manager-only mutations and the dependent-task conflict. | Automated API tests; the user interface workflow has not been manually walked through for this exercise. |
| 03 | Task persistence | A created task appears without reloading and remains after restarting the Backend. | **Partially passed automatically.** `exercise8CreatedTaskCanBeReadBackFromPersistence` creates a task, finds it through the repository, and reads it through `GET /tasks/{taskId}`. The test uses in-memory H2 and does not restart a separate Backend or prove file-database survival across process restart. | Automated create/read check passed; real UI refresh and Backend-restart verification not run. |
| 04 | Data validation | Empty, whitespace-only, and invalid values are rejected. | **Passed automatically at the API boundary.** `exercise8RejectsWhitespaceOnlyProjectNames`, `projectValidationAndCaseInsensitiveDuplicateNamesAreEnforced`, `taskCrudFiltersValidationAndCountsFollowMvpContract`, and `taskTitleAndDescriptionHonorExactLengthBoundaries` cover whitespace, required/length rules, and invalid status/filter values. | Automated backend tests; browser error presentation is pending. |
| 05 | Status and priority | Edits update the task, filters, and status counts consistently. | **Partially passed automatically.** `taskCrudFiltersValidationAndCountsFollowMvpContract` updates a task to `DONE`/`LOW`, confirms the filtered result, and checks that count fields are returned as numbers. The UI's no-reload behavior and exact count reconciliation across filters were not verified in a browser. | Automated API test; manual interface check pending. |
| 06 | Assignees | Assign/remove works and duplicate task/user associations are rejected. | **Passed automatically.** `managerCanAssignOnlyAnActiveMemberOfTheProject` verifies eligible-member selection, assignment, duplicate rejection (`409`), and removal. `taskCrudFiltersValidationAndCountsFollowMvpContract` also checks associated records are cleaned up when a task is deleted. | Automated API tests; browser interaction pending. |
| 07 | Comments | Content, authenticated author, and server date are persisted and visible. | **Passed automatically.** `commentsAreValidatedServerAuthoredAndAvailableWhenTaskIsReopened` verifies trimming, whitespace and length validation, authenticated author identity, server timestamp, and subsequent retrieval. | Automated API test; browser presentation pending. |
| 08 | API authorization | Another user cannot access or change a resource by substituting its identifier. | **Passed automatically.** `exercise8RejectsAccessToAnotherUsersProjectEvenWithItsId` sends the other user's project ID to both `GET` and `PUT`; both return `403 PROJECT_ACCESS_DENIED`. `projectMemberCanCommentAndUpdateTaskButCannotCreateOrAssignTasks` checks role restrictions. | Automated API tests; no real-browser/API proxy walkthrough performed. |
| 09 | Deletion and errors | Dependencies are respected; API failures do not show success or discard form input. | **Partially passed automatically.** `projectDetailsAndMutationsRespectMembershipAndManagerRole` verifies deleting a project with tasks returns `409 PROJECT_HAS_TASKS`; task deletion and dependent comment/assignment cleanup are checked in `taskCrudFiltersValidationAndCountsFollowMvpContract`. Frontend preservation of entered values and truthful error messages were not exercised in a browser. | Automated API tests; frontend failure-state check pending. |

## Automated tests added for Exercise 8

Three focused integration tests were added to `MatrizAccessIntegrationTests`:

1. `exercise8RejectsWhitespaceOnlyProjectNames` — submits an all-whitespace name and expects `400 VALIDATION_ERROR`.
2. `exercise8RejectsAccessToAnotherUsersProjectEvenWithItsId` — attempts to read and update another user's project using its real identifier and expects `403 PROJECT_ACCESS_DENIED`.
3. `exercise8CreatedTaskCanBeReadBackFromPersistence` — creates a task, confirms it is present in the repository, then retrieves it through the API.

The first full run exposed an incorrect expected error code in the new authorization test: the observed response was `PROJECT_ACCESS_DENIED`, while the test expected `ACCESS_DENIED`. `AccessService.requireMembership` confirms `PROJECT_ACCESS_DENIED` is the established response. The assertion was corrected; the application authorization behavior did not require a change. The full suite then passed.

## Code review finding and regression test

Reviewing the login UI against the API behavior exposed a frontend bug: `apiFetch` redirected every `401` to the expired-session page, including `POST /auth/login`. That prevented `login.js` from displaying its existing invalid-credentials message. The correction leaves login `401` responses with the login form while preserving redirects for unauthorized protected API requests.

The regression tests in `src/test/js/auth.test.mjs` reproduce both branches with mocked HTTP responses: invalid credentials remain on the sign-in flow, while a protected API `401` redirects to sign-in. No production credentials or network calls are used by these tests.

The clean-checkout review also found that the H2 URL in `.env.example` contains a semicolon and was unquoted even though the README sources the file as shell code. Quoting the value ensures the complete JDBC URL is loaded. A shell assertion verifies the sourced value without printing credentials.

The code-quality opportunity was the duplicate `PathVariable` import in `ProjectController`; it was removed in a separate cleanup change. A larger frontend or service refactor was not applied because it would increase delivery risk without a focused behavior or maintainability gain for this exercise.

## Examples of AI-assisted work

| AI request objective | Programmer decision | Verification |
|---|---|---|
| Identify gaps in the nine Exercise 8 scenarios and propose focused tests. | Added separate tests for whitespace-only validation, authorization with another user's project ID, and create-then-read persistence. Did not claim that the in-memory persistence test proves survival across a process restart. | `./gradlew clean test`; the JUnit integration suite passed. Restart persistence remains explicitly marked unverified. |
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

No manual browser walkthrough or file-backed database restart was performed for Exercise 8. Therefore the following results remain unconfirmed in this report:

- sign-in, project and task management, assignment, and comment flows in a real browser;
- UI updates without a reload, including filtered and aggregate counts;
- browser display of loading, empty, validation, authorization, and network error states;
- retaining form input after API failures and suppressing success messages on failed requests;
- task survival after stopping and restarting the Backend against the file-backed H2 database.

An application startup was attempted through the Java debugger on a temporary port with an in-memory database. The tool reported that the debug session started, but the health request could not connect and a subsequent session check found no active session. No startup log was available to diagnose the exit, so startup is not claimed as verified and no retry was attempted.

The automated integration suite is strong evidence for the tested API/service behavior, but the in-memory database means it cannot establish persistence across process restarts. Complete these manual checks against the local application before marking the delivery fully verified.
