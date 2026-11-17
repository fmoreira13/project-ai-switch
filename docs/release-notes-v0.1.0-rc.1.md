# Matriz 0.1.0-rc.1 - Superseded Candidate

**Status:** superseded by `0.1.0-rc.2`; never tagged or published.
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
- CI scans operating-system packages in the built container image with Trivy and blocks on HIGH or CRITICAL vulnerabilities.
- Console logs use structured JSON; health probes expose liveness and readiness status without component details, and Compose checks readiness including H2 availability.

The observability increment was validated locally on 2026-11-16: Java tests passed, the new Docker image built, both probes and the compatibility health endpoint returned HTTP 200 in an isolated container, and startup logs parsed as JSON. The existing Compose container was not restarted.
- The runtime image upgrades its base operating-system packages during build to include available security updates.

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
- A local Trivy 0.70.0 scan of the rebuilt image found no HIGH/CRITICAL OS findings after the Dockerfile security update. GitHub Actions run [44120868378](https://github.com/fmoreira13/project-ai-switch/actions/runs/44120868378) subsequently passed the Trivy scan; the release-only GHCR workflow is introduced in `0.1.0-rc.2`.
- This initial scan does not cover application-library dependencies.
- No configured static-analysis/SAST scanner was run; see [`quality-security-review.md`](quality-security-review.md).
- The Gradle artifact version is still `0.0.1-SNAPSHOT`. The proposed candidate version is documentation only and has not been applied to the build or tagged in Git.

This candidate was superseded before tagging or publication. No production release is implied; see the [0.1.0-rc.2 notes](release-notes-v0.1.0-rc.2.md) for the current candidate.
