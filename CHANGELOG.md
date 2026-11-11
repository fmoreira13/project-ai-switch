# Changelog

Notable project changes are recorded here. This file tracks the proposed application release; it does not change the Gradle artifact version or indicate that a release has been published.

## Proposed `0.1.0-rc.1` - Unreleased

### Added

- Documented the implemented modular-monolith architecture and request/persistence flow, including a Mermaid diagram.
- Added release-candidate notes and linked the OpenAPI contract and technical documentation from the README.
- Added project-scoped session authentication, manager/member access, task assignments, and comments as implementation-exercise capabilities.
- Added Docker Compose execution with persistent H2 storage and a health check.
- Added GitHub Actions verification for Java, frontend checks, Playwright E2E, Compose configuration, and a dependent Docker image build.
- Added a Trivy container-image operating-system package scan to CI; the image-build job fails on HIGH or CRITICAL findings.
- Updated operating-system packages in the runtime image to address the HIGH apt vulnerability found by the initial Trivy scan.

### Changed

- Updated the README and architecture documentation to distinguish implemented exercise extensions from the confirmed product MVP.
- Stabilized the comment integration test's timestamp assertion so equivalent timestamp serialization does not fail the test.

### Verification

- GitHub Actions run for commit `0e3e832f1a5e58c17007eae6ad47a4b8555e0ea9` passed both `verify` and `build-image` on 2026-11-11: [run details](https://github.com/fmoreira13/project-ai-switch/actions/runs/43092399460).
- This is a proposed, unpublished release candidate. The Gradle artifact remains `0.0.1-SNAPSHOT`; no production release or image publication was performed.
- A local Trivy 0.70.0 scan of the rebuilt image reported no HIGH or CRITICAL operating-system findings after the Dockerfile security update.
- The Trivy CI integration is pending its first GitHub Actions run; its GitHub-hosted result is not yet verified.
- Application-library dependency vulnerabilities are not covered by this initial scan.
