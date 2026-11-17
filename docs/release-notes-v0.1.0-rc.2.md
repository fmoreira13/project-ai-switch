# Matriz 0.1.0-rc.2 - Release Candidate

**Status:** published as a GitHub pre-release on 2026-11-17.
**Semantic Versioning:** `0.1.0-rc.2`.
**Purpose:** an academic demonstration candidate, not a production release.

GitHub release: [v0.1.0-rc.2](https://github.com/fmoreira13/project-ai-switch/releases/tag/v0.1.0-rc.2).

## Highlights

- Browser-based project and task management: create, consult, update, and remove projects and tasks.
- Task status and priority updates, text/status/priority filters, and project/task overview counts.
- Exercise-only project membership roles, server-side session authentication, task assignment, and comments.
- Structured JSON console logs and public liveness/readiness probes with health details hidden.
- Docker image publication to GHCR is gated on successful verification, image build, and Trivy scan; only published pre-releases trigger publication.
- Render Free deployment is documented for short academic demonstrations using the Dockerfile and in-memory H2.

## Changes in this candidate

- Normalized task-search text and stored email addresses using `Locale.ROOT`, avoiding behavior changes under Turkish and other locale settings.
- Added a regression test that sets the JVM locale to `tr-TR` and verifies both task search and email normalization.
- Configured Spring Boot to use Render's `PORT` environment variable while retaining `8080` as the local default; Compose explicitly uses port `8080`.
- Added the GHCR release job with minimal `packages: write` permission. Published release candidates receive a version tag and a full commit-SHA tag; no `latest` tag is produced.

## Verification

Local checks completed before publication:

- Java/JUnit suite: 25 tests passed.
- JavaScript syntax/auth tests: passed, including 2 Node tests.
- Playwright E2E: 1 browser test passed.
- Docker Compose configuration validation: passed.
- Docker image build: passed.
- Container started with `PORT=18082` and its readiness endpoint returned `UP`.
- `git diff --check`: passed.

GitHub Actions release run [44124699323](https://github.com/fmoreira13/project-ai-switch/actions/runs/44124699323) passed `verify`, `build-image` (including Trivy), and `publish-image` after package workflow access was granted.

Public image: [`ghcr.io/fmoreira13/project-ai-switch:0.1.0-rc.2`](https://github.com/users/fmoreira13/packages/container/package/project-ai-switch). The full-SHA tag is `ghcr.io/fmoreira13/project-ai-switch:sha-9480e59b7dfbd1205c78e7d651ae6dec99cbbbcd`.

OCI image index digest: `sha256:29066badf3a5d243145de33e940dc2f160b170b5894108531295570f6765e4e5`.

## Academic Render demonstration

Configure Render with the public GHCR image above, `/actuator/health/readiness` as the health check path, and environment values for `MATRIZ_DB_URL=jdbc:h2:mem:matriz;DB_CLOSE_DELAY=-1`, both demo passwords, and `MATRIZ_SESSION_COOKIE_SECURE=true`. Render supplies `PORT`. The Render service itself has not been created or deployed.

This deployment is temporary and uses in-memory H2. Render Free can sleep while idle and has ephemeral storage; records are not durable and may be lost after restart or redeployment. Use fictitious data only. This candidate is not production-ready.

## Limitations

- H2, demo accounts, the access policy, comments, and task assignments are implementation-exercise choices; production requirements remain unconfirmed.
- Trivy scans operating-system packages in the container image; application-library dependency analysis and source-code SAST are not configured.
- The Gradle artifact version remains `0.0.1-SNAPSHOT`; the candidate version is represented by the Git tag and release, not by changing the artifact version.
