# Matriz - Code and Security Review

## Review scope and reference

- Review date: 2026-11-11.
- Branch: `main`.
- Reviewed E2E increment: commit `48d441e` (`ficha 4 - exercicio 4`).
- The review was performed against a clean worktree at that commit. The task-level authorization regression test was subsequently committed as `0e3e832`; it is a validation change, not a new finding or production-code correction. This report has since been updated to reflect the final CI evidence.
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

### Static analysis and image scanning

No configured ESLint, Semgrep, SonarQube, SpotBugs, or equivalent source-code static-analysis/SAST tool was found in the project configuration. The CI now scans the built container image with Trivy for operating-system package vulnerabilities, failing on HIGH and CRITICAL findings (including unfixed ones). Application-library dependencies are not included; this image scan does not replace dependency analysis or source-code SAST.

The first local Trivy scan found two HIGH entries for the same issue, `CVE-2026-109518`, in `apt` and `libapt-pkg7.0` in the Java runtime base image (installed `3.2.0`, fixed `3.2.1ubuntu0.1`). The runtime image build now applies available OS package upgrades before copying in the application. After a clean no-cache rebuild, Trivy 0.70.0 reported zero HIGH/CRITICAL OS findings for the rebuilt Ubuntu 26.04 image. GitHub Actions run [44120868378](https://github.com/fmoreira13/project-ai-switch/actions/runs/44120868378) passed `build-image`, including Trivy, on 2026-11-17. That run does not include the new release-only GHCR publication job.

## Observability controls

Spring Boot Actuator exposes only public liveness and readiness health groups; component details are disabled. Liveness does not include database checks. Readiness includes the application readiness state and H2 datasource health, and Compose uses readiness for its container healthcheck. Console logs use structured JSON; no request-body or credential logging was added. These configuration and access rules are covered by `MatrizApplicationTests`.

Validation on 2026-11-16:

| Check | Result |
|---|---|
| `./gradlew --no-daemon clean test` | Passed after adding Actuator and probe tests. |
| Compose configuration and OpenAPI/Compose/workflow YAML parsing | Passed. |
| Docker image build with Actuator | Passed as `matriz:observability`. |
| Temporary isolated container probes on port `18081` | Liveness, readiness, and compatibility `/api/health` all returned HTTP 200; Actuator responses contained no component details. |
| Runtime console log parsing | All 26 startup log events parsed as individual JSON objects. |
| Existing Compose application | Remained healthy on port `8080`; it was not restarted or modified. |

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
| GitHub Actions for commit `0e3e832f1a5e58c17007eae6ad47a4b8555e0ea9` | Passed: `verify` and dependent `build-image` jobs; [run 43092399460](https://github.com/fmoreira13/project-ai-switch/actions/runs/43092399460). |

The unit, integration and E2E commands, including isolated test setup, are documented in [README.md](../README.md). The final Java suite, frontend checks, E2E run and diff check passed after the task-level regression test and report were added.

## Findings, decisions, and pending checks

- **Confirmed vulnerabilities:** none.
- **Code review findings:** none requiring a production change.
- **Correction applied:** added an integration regression test for direct task-ID read/update attempts across the two demo accounts. The test passed; application behavior was already denying these requests, so no production authorization code was changed.
- **Static analysis:** not run because no static-analysis tool is configured.
- **Manual two-account UI/API session test:** not performed; the equivalent task authorization checks were run as automated MockMvc integration tests.
- **Full post-change verification:** complete; 23 JUnit tests, 2 Node auth tests, 1 Playwright E2E, JavaScript syntax checks, Compose validation, and Docker image build passed in the referenced GitHub Actions run.
- **Review reference:** the code review scope remains the E2E increment `48d441e`; the subsequent authorization regression test is in commit `0e3e832`. Documentation-only release preparation is tracked separately in the proposed changelog and release notes.

## Full application follow-up review (2026-11-17)

The follow-up review covered the Java API/controllers, services, domain entities, repositories, schema initialization, authentication and authorization flow, browser UI, automated tests, Docker/Compose, GitHub Actions, OpenAPI, and current technical documentation.

### Finding and correction

The default JVM locale was used for case normalization in task search and email storage. On locales such as Turkish (`tr-TR`), uppercase `I` can normalize to dotless `ı`, making case-insensitive search inconsistent and changing stored email normalization. Both code paths now use `Locale.ROOT`. `taskSearchIsIndependentOfTheServerLocale` reproduces the Turkish-locale condition and verifies task search and email normalization; the test restores the original JVM locale even on failure.

No additional confirmed functional or access-control defect was found in the reviewed paths. This review does not certify production security or replace dependency/SAST analysis.

### Verification

| Check | Result |
|---|---|
| `./gradlew --no-daemon test` | Passed: 25 JUnit tests, 0 failures. |
| Frontend `node --check` and auth tests | Passed: syntax checks succeeded and 2 Node tests passed. |
| `npm run test:e2e` | Passed: 1 Playwright browser test. |
| `docker compose --env-file .env.example config --quiet` | Passed. |
| `docker build --tag matriz:review .` | Passed. |
| `git diff --check` | Passed. |

This candidate's Render port wiring was verified by a temporary container on port `18082` returning readiness status `UP`; the E2E test also uses the `PORT` environment variable. The release-specific GHCR job remains to be verified by the published pre-release workflow.
