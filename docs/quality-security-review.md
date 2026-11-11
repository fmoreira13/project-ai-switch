# Matriz - Code and Security Review

## Review scope and reference

- Review date: 2026-11-11.
- Branch: `main`.
- Reviewed E2E increment: commit `48d441e` (`ficha 4 - exercicio 4`).
- The review was performed against a clean worktree at that commit. The task-level authorization regression test and this report were added afterward and remain uncommitted; they are validation/documentation changes, not a new finding or production-code correction.
- Scope: the latest Playwright E2E increment and the project/task access-control flows, including direct resource access, role restrictions, and obvious exposure of secrets.

## Code review

**Result:** no significant actionable bug was found in the reviewed E2E increment.

The test uses existing login labels, project/task selectors and user-visible outcomes; it creates a UUID-suffixed task and Playwright waits on locator assertions instead of fixed sleeps. The runner starts its own Spring Boot process on port `18080`, uses a separate in-memory H2 database and fictitious test passwords, and does not load `.env` or use the local file-backed `data/` database. The review found no production-source changes in that increment.

No code correction was recommended, so no production regression fix was needed. Additional task-level access coverage was identified as a test gap during the follow-up security validation and added as `exercise5ManagerCannotReadOrUpdateAnotherUsersTaskById`; this does not assert a known vulnerability.

## Security review

**Result:** no confirmed IDOR, broken access control, or unauthorized data exposure was identified in the reviewed access flows.

The documented exercise policy is project-scoped. The tests verify that a manager cannot read or edit another user's project, members may update tasks but cannot create or assign them, and project managers control assignments. Allowing a project member to update a task is intentional and matches the approved exercise policy; it is not a finding.

### Two-account task authorization regression

The seeded demo owner account (`member@matriz.local`) can read its task in project Beta. The other demo account (`manager@matriz.local`), which has no membership in Beta, is then denied both:

- `GET /tasks/{taskId}` with HTTP `403 PROJECT_ACCESS_DENIED`;
- `PUT /tasks/{taskId}` with HTTP `403 PROJECT_ACCESS_DENIED`.

The test also confirms the stored task title is unchanged after the rejected update. This test is `exercise5ManagerCannotReadOrUpdateAnotherUsersTaskById` in [MatrizAccessIntegrationTests.java](../src/test/java/com/matriz/MatrizAccessIntegrationTests.java). The authorization check is automated through MockMvc with the two demo identities; it is not a manual browser/API-session exercise.

The existing project-level cross-account test is `exercise8RejectsAccessToAnotherUsersProjectEvenWithItsId` in [MatrizAccessIntegrationTests.java](../src/test/java/com/matriz/MatrizAccessIntegrationTests.java).

### Static analysis

No configured ESLint, Semgrep, SonarQube, SpotBugs, or equivalent static-analysis/SAST tool was found in the project configuration. No scanner was installed or run. This is a tooling limitation, not evidence that the code passed a SAST scan.

## Executed verification

Results below are actual command results from 2026-11-11:

| Command/check | Result |
|---|---|
| `./gradlew test --tests 'com.matriz.MatrizAccessIntegrationTests.exercise5ManagerCannotReadOrUpdateAnotherUsersTaskById'` | Passed: 1 integration test, 0 failures. |
| `./gradlew test --tests 'com.matriz.MatrizAccessIntegrationTests.exercise8RejectsAccessToAnotherUsersProjectEvenWithItsId' --tests 'com.matriz.MatrizAccessIntegrationTests.projectMemberCanCommentAndUpdateTaskButCannotCreateOrAssignTasks'` | Passed: 2 integration tests, 0 failures. |
| `./gradlew clean test` (final run after the task-ID regression test) | Passed: 23 JUnit tests, 0 failures (19 access integration, 1 application context, 3 unit). |
| `npm run test:e2e` | Passed in three consecutive runs (1 Playwright test each); latest run followed `npm ci`. |
| Frontend `node --check` commands and `node --experimental-default-type=module --test src/test/js/auth.test.mjs` | Passed: syntax checks succeeded and 2 Node tests passed. |
| `git diff --check` | Passed after all current test/documentation edits. |

The unit, integration and E2E commands, including isolated test setup, are documented in [README.md](../README.md). The final Java suite, frontend checks, E2E run and diff check passed after the task-level regression test and report were added.

## Findings, decisions, and pending checks

- **Confirmed vulnerabilities:** none.
- **Code review findings:** none requiring a production change.
- **Correction applied:** added an integration regression test for direct task-ID read/update attempts across the two demo accounts. The test passed; application behavior was already denying these requests, so no production authorization code was changed.
- **Static analysis:** not run because no static-analysis tool is configured.
- **Manual two-account UI/API session test:** not performed; the equivalent task authorization checks were run as automated MockMvc integration tests.
- **Full post-change verification:** complete; 23 JUnit tests, 2 Node auth tests, 1 Playwright E2E, JavaScript syntax checks, and `git diff --check` all passed.
- **Final source reference:** branch `main`; reviewed E2E increment `48d441e`. The follow-up task-ID regression test and documentation updates remain uncommitted; no final commit has been created.
