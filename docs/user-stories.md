# Matriz - User Stories

## User stories for the MVP

### US-01
As a user, I want to create a project so that I can organize work around a common goal.

Acceptance criteria:
- The system allows creating a project with a name of 3-100 characters and an optional description of at most 1000 characters.
- The project is stored and listed correctly.
- The system rejects creation without a name or with invalid data.
- Project names are trimmed and unique without regard to case.
- An authenticated user's new project is associated with that user as its manager.
- A project is visible only to users with project membership.
- A manager can consult, update, and remove an accessible project; members can consult but cannot update or remove it.
- Projects with tasks cannot be removed; the API reports a conflict rather than orphaning dependent data.
- The project list can be filtered by a case-insensitive name search.

Edge cases:
- duplicated project name;
- project without a name;
- attempt to create a project with empty data.

### US-02
As a user, I want to add tasks to a project so that I can distribute the necessary work to achieve the defined goals.

Acceptance criteria:
- The system allows creating a task associated with an existing project.
- The system rejects tasks without a title or with invalid minimum data.
- The task appears in the task list for the correct project.
- The task must remain associated with the correct project after creation.

Edge cases:
- non-existent project;
- task without a title;
- task created for an unselected or invalid project.

### US-03
As a user, I want to view projects and their associated tasks so that I can know the work in progress and its current status.

Acceptance criteria:
- The system displays the list of available projects.
- When selecting a project, the system displays its associated tasks.
- Each displayed task must show its current status using a value allowed by the system definition.

Edge cases:
- empty project list;
- project without tasks;
- non-existent project when consulting details.

### US-04
As a user, I want to update the status of a task so that it reflects the real progress of the work.

Acceptance criteria:
- The system allows changing the task status to a valid value.
- The update is reflected immediately in the task presentation.
- The system rejects invalid or unknown values.

Edge cases:
- attempt to update a non-existent task;
- unsupported status;
- updating a task in a non-existent project.

### US-05
As a user, I want to remove a project or a task to keep the workspace organized and aligned with the current context.

Acceptance criteria:
- The system only removes the project or task after user confirmation.
- The system informs the user when the action is not possible.
- Removal must not affect other projects or tasks without explicit confirmation.

Edge cases:
- attempt to remove a non-existent task;
- attempt to remove a non-existent project;
- operation without user confirmation.

### US-06
As a user, I want to search for tasks by keyword to quickly find the relevant work.

Acceptance criteria:
- The system allows searching by text term.
- Results display tasks matching the search.
- If there are no matches, the system clearly informs the user that no results were found.

Edge cases:
- empty search;
- term with no match;
- multiple results with partial similarity.

## General acceptance criteria

- Each user story must be objectively verifiable.
- Each feature must have a clear behavior in case of error.
- The backlog must contain only confirmed and relevant requirements for the MVP.
