# Matriz - Requirements

## Vision and goals of the application

The Matriz application aims to support work teams in organizing projects and managing their associated tasks. The solution is intended to centralize relevant information to track work progress, improve coordination between team members, and facilitate monitoring of progress.

The application must allow users to create projects, add tasks, track execution status, and maintain a clear view of ongoing work.

## Types of users

User profiles, permissions, and access levels have not yet been defined, so they will not be assumed as requirements at this stage.

## Functional requirements

1. Project management
   - The system must allow creating, consulting, updating, and removing projects.
   - Each project must have an identifier and basic contextual information.

2. Task management
   - The system must allow creating, consulting, updating, and removing tasks.
   - Each task must be associated with a project.

3. Progress tracking
   - The system must allow tracking the status of each task.
   - The system must make it easier to understand the overall status of the project.

4. Work organization
   - The system must allow distinguishing tasks by status and other categories when this is confirmed as a requirement for the MVP.
   - Priority will not be treated as a confirmed requirement in this phase.

5. Search and consultation
   - The system must allow locating tasks by keyword or simple criteria.

6. Overview
   - The system must provide an aggregated view of projects and tasks.

## Non-functional requirements

- The application must be intuitive and easy to use.
- The interface must enable efficient use by work teams.
- The system must present clear messages for errors and validations.
- The solution must be consistent and predictable in the management of projects and tasks.
- The application must facilitate organization and traceability of work.
- The solution must be conceptually scalable to support more projects and teams in the future.

## MVP scope

The Matriz MVP must include:
- creating projects;
- creating tasks within a project;
- listing projects and tasks;
- updating the task status;
- removing projects and tasks, subject to user confirmation;
- simple task search;
- overview of ongoing work.

The focus of the MVP is the basic and verifiable functional management of the workflow.

## Features outside the initial scope

The following functionalities are not part of the initial MVP and should not be treated as essential product requirements:
- authentication and advanced user management;
- multiple permission levels;
- automatic notifications;
- advanced reports and analytical dashboards;
- integration with external tools;
- comments and detailed change history;
- hours, costs, or budget management;
- AI to support prioritization or work organization.
