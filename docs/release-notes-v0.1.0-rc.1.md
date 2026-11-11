# Matriz 0.1.0-rc.1 - Proposed Release Candidate

**Status:** proposed candidate; not tagged or published.  
**Semantic Versioning proposal:** `0.1.0-rc.1`.  
**Purpose:** identify a reviewable pre-release of the current MVP implementation, not declare production readiness.

The `0.x` version indicates an initial pre-1.0 product milestone; `rc.1` identifies the first proposed release-candidate iteration. This proposal does not change the current Gradle version.

## Candidate highlights

- Browser-based project and task management with project/task creation, consultation, editing, and removal.
- Task status and priority updates, text/status/priority filtering, and project/task overview counts.
- Exercise-only project membership roles, server-side session authentication, and access checks.
- Exercise-only task assignment and comments.
- File-backed H2 persistence for local use and a named persistent volume for Docker Compose.
- OpenAPI contract for the HTTP API at [`../openapi.yaml`](../openapi.yaml).

## Verification evidence

The GitHub Actions workflow passed for commit `0e3e832f1a5e58c17007eae6ad47a4b8555e0ea9` on 2026-11-11:

- Java/JUnit test suite;
- frontend JavaScript syntax and auth regression checks;
- Playwright Chromium E2E journey;
- Docker Compose configuration validation;
- Docker image build, run only after the verification job succeeded.

See the [GitHub Actions run](https://github.com/fmoreira13/project-ai-switch/actions/runs/43092399460). The earlier run failed on a test's exact timestamp-string comparison and correctly skipped the image build; the test was corrected before the successful run. A local controlled-failure check was performed separately and was not part of this Actions run.

## Known limitations and review items

- This is not a production-ready release. Production authentication and authorization policy, privacy requirements, backups/recovery, and deployment requirements remain unconfirmed.
- H2, demo accounts, and manager/member access are exercise choices. Demo credentials must not be used as production credentials.
- Comments and task assignments are exercise extensions, not confirmed product MVP requirements.
- The CI workflow builds an image but does not publish it to a registry.
- No configured static-analysis/SAST scanner was run; see [`quality-security-review.md`](quality-security-review.md).
- The Gradle artifact version is still `0.0.1-SNAPSHOT`. The proposed candidate version is documentation only and has not been applied to the build or tagged in Git.

## Candidate release decision

Review the remaining product/security decisions and approve the candidate before creating a Git tag or publishing any release. No production release is requested or implied here.
