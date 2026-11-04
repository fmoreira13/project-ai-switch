# Matriz - Constraints and assumptions

## Known constraints

- The client has not yet defined the application's features in detail.
- There are no formalized business rules or complete acceptance criteria.
- The type of interface has not been defined.
- User types, permissions, and access levels have not been defined.
- The need for persistence has not been confirmed.
- The maturity level of the solution has not been defined: MVP or full product.

## Assumptions made

The following points are working hypotheses and not confirmed requirements:
- the application will be used by work teams to manage projects and tasks;
- each project will, in principle, have several associated tasks;
- tasks may evolve over time;
- users may need to view the current status of ongoing work;
- the MVP will focus on the core of project and task management.

## Risks and pending security and privacy requirements

The following aspects have not yet been confirmed by the client, but they represent risks and requirements to validate before development in a real environment:
- access control to data by user or team;
- protection of sensitive data;
- validation of inputs to avoid inconsistent data;
- clear error messages without exposing excessive information.

These points should be discussed with the client and confirmed before any implementation in a production environment.

## Pending technical decisions

- programming language and framework;
- interface type (web, desktop, mobile, or terminal);
- data storage technology;
- authentication and authorization level;
- data model for projects, tasks, and users;
- backup, history, and recovery mechanisms.

## Scope limitations

At this stage, the project should keep a controlled scope and focus only on:
- creating and managing projects;
- creating and managing tasks;
- reading and updating status;
- simple search.

More advanced features such as authentication, permissions, integrations, advanced reports, or resource management are not part of the current scope and should be discussed and validated in later phases.
