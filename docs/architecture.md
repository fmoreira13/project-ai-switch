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
- list projects;
- create projects;
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
- the task title and project name are mandatory;
- the task status follows the values defined for the MVP;
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
- title: string
- description: string nullable
- status: backlog | in_progress | review | done | cancelled
- createdAt: datetime
- completedAt: datetime nullable

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
