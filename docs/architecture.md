# Architecture Pack - Matriz

## Status

Revised and simplified for the MVP.

## Context

Matriz is a tool for managing projects and tasks in teams. The current MVP scope was defined as creating, consulting, updating, and removing projects and tasks, in addition to simple task search.

To maintain consistency with the documentation, the architecture must be simple, predictable, and appropriate for the MVP. User capabilities, permissions, integrations, and advanced reports should not be introduced before client validation.

## Architecture decision

Choice: modular monolith with a minimal REST API, composed of two main resources: /projects and /tasks.

Justification:
- it responds to the MVP defined in requirements and user stories;
- it avoids over-engineering and unnecessary infrastructure;
- it keeps the solution clear and easy to evolve in stages;
- it supports clear separation between presentation, business rules, and persistence;
- it respects the rule of keeping the MVP simple and limited to the two essential resources.

## Implementation stack

The language and backend framework confirmed for Class 02 are Java and Spring Boot. The machine has Java 25.0.3 and Java 26.0.1 installed; the project targets Java 25. H2 is selected for the database because it integrates directly with Spring Boot, is straightforward to run locally, and does not require a separate database server. Use file-based storage for local persistence and an in-memory H2 database for automated tests.

The planned application stack is:
- Language: Java
- Java version: 25
- Backend framework: Spring Boot
- Persistence integration: Spring Data JPA
- Database: H2
- Build tool: Gradle Wrapper (`./gradlew`)
- Frontend: plain HTML, CSS, and JavaScript served by the Spring Boot application; no separate frontend framework is selected.

The database choice, build tool, Java target, and frontend approach are implementation decisions for this exercise, not decisions confirmed in Class 02. See [docs/implementation-plan.md](implementation-plan.md) for the recorded scope and remaining decision gates.

## Access model status

The initial MVP documents did not define user accounts, roles, or permissions. For this implementation exercise, the user approved the following project-scoped access proposal:

- users sign in with email/password and a server-side HTTP session for this exercise;
- each project has a manager and members;
- the authenticated user who creates a project becomes its manager;
- a manager can consult, edit, and remove an accessible project, manage its tasks, and assign tasks;
- a member can view and update tasks and add comments, but cannot manage the project or assign tasks;
- only active users who are members of the same project are eligible assignees.

This is a user-approved exercise policy, not a pre-existing MVP requirement. The login endpoint issues an HttpOnly session cookie, the profile endpoint returns the authenticated user, and logout invalidates the session. Demo data and integration tests cover manager/member visibility and operation restrictions.

Project names are trimmed and unique case-insensitively. Names must contain 3-100 characters and descriptions may contain up to 1000 characters. Project deletion is rejected with HTTP 409 while tasks exist; memberships are removed only when a task-free project is deleted.

Comments and assignees were not part of the original MVP, but the current exercise explicitly requests their schema, demo data, and user selection. They are implemented as exercise scope extensions; do not treat them as confirmed production requirements.

## System objective

The system must allow:
- creating and listing projects;
- creating and listing tasks by project;
- updating the status and basic task data;
- removing projects and tasks after confirmation;
- searching tasks by free text;
- maintaining an overview of current work.

## Main components

### 1. Web frontend
Responsible for the user experience.

Functions:
- list, search, create, consult, edit, and remove projects;
- open a project and view its tasks;
- create, edit, and remove tasks;
- change task status;
- search tasks.

### 2. REST API
Responsible for the integration interface between frontend and application.

Functions:
- expose endpoints for projects and tasks;
- validate inputs and return structured errors;
- orchestrate use-case actions.

### 3. Application layer
Responsible for business logic.

Use cases:
- create project;
- list projects;
- create task;
- list tasks by project;
- update task;
- remove task or project;
- search by term.

### 4. Domain layer
Main entities:
- Project
- Task

Essential rules:
- each task belongs to a valid project;
- task titles contain 3-150 non-whitespace characters;
- task descriptions contain at most 2000 characters;
- the task status is TODO, IN_PROGRESS, or DONE; priority is LOW, MEDIUM, or HIGH;
- task list filters cover text, status, and priority; pagination is not defined;
- removals require explicit confirmation.

### 5. Persistence
Responsible for storing and retrieving data while preserving consistency between projects and tasks.

## Summary data model

Project
- id: UUID
- name: string
- description: string nullable
- createdAt: datetime

Task
- id: UUID
- projectId: UUID
- title: string (3-150 characters)
- description: string nullable (up to 2000 characters)
- status: TODO | IN_PROGRESS | DONE (default TODO)
- priority: LOW | MEDIUM | HIGH (default MEDIUM)
- createdAt: datetime
- completedAt: datetime nullable

Exercise-only persistence additions:
- User with unique email, display name, BCrypt password hash, active flag, and creation time.
- ProjectMembership linking users to projects with MANAGER or MEMBER role.
- TaskAssignment linking a task to one active project member and recording who assigned it.
- TaskComment linking task, author, text, and creation time.

These additions satisfy the implementation exercise and the user-approved access scenario; they do not expand the originally documented product MVP.

## Main flows

1. Create project
   - the user sends a name and optional description;
   - the API validates the data;
   - the project is created and listed immediately.

2. Create task
   - the user selects a project;
   - fills in the title and optional description;
   - the task is associated with the existing project.

3. Update progress
   - the user changes the task status;
   - the information is persisted and reflected in the work panel.

4. Search tasks
   - the user types a term;
   - the API returns tasks whose title or description match that value.

## Simplified wireframe (structural view)

Suggested layout:
- top: search bar and quick actions;
- left sidebar: project list;
- center area: tasks for the selected project;
- details panel: title, description, status, and update/remove actions.

Visual flow:
- the user chooses a project;
- the tasks for that project appear in the center;
- search filters the current task set;
- the status can be updated without leaving the project context.

## Alignment with project artifacts

This pack is consistent with:
- [docs/requirements.md](requirements.md): MVP objectives and scope;
- [docs/user-stories.md](user-stories.md): creation, viewing, progress, and removal flows;
- [docs/database.md](database.md): basic entities and relationships;
- [openapi.yaml](../openapi.yaml): endpoints for projects and tasks in the MVP;
- [docs/ui/wireframe.png](ui/wireframe.png): visual structure of the main MVP flow.

## What was corrected in this review

- the ambiguity around project status was removed, since it was not required by the MVP;
- the task model was reinforced as the central entity for progress control;
- MVP endpoints were aligned with the mandatory operations from the requirements and user stories;
- architecture was simplified to avoid over-specification.

## Final notes

The current architecture is sufficient for the MVP and maintains the necessary base for future evolution without introducing premature complexity. Any additional advance (authentication, permissions, history, integrations, and advanced reports) should be treated as a new requirement and validated before entering technical design.
