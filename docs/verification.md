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

## Commands and results

Executed from the repository root on macOS:

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
PATH="$(/usr/libexec/java_home -v 25)/bin:$PATH" \
./gradlew test
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

## Manual checks and limitations

No manual browser walkthrough or file-backed database restart was performed for Exercise 8. Therefore the following results remain unconfirmed in this report:

- sign-in, project and task management, assignment, and comment flows in a real browser;
- UI updates without a reload, including filtered and aggregate counts;
- browser display of loading, empty, validation, authorization, and network error states;
- retaining form input after API failures and suppressing success messages on failed requests;
- task survival after stopping and restarting the Backend against the file-backed H2 database.

The automated integration suite is strong evidence for the tested API/service behavior, but the in-memory database means it cannot establish persistence across process restarts. Complete these manual checks against the local application before marking Exercise 8 fully verified.
