# Matriz - Constraints and assumptions

## Known constraints

- The client has not yet defined the application's features in detail.
- There are no formalized business rules or complete acceptance criteria.
- The type of interface has not been defined.
- For the implementation exercise, the user approved project-scoped `MANAGER` and `MEMBER` access. This does not establish a production access policy.
- H2 has been selected for the implementation exercise; deployment and access requirements still require confirmation.
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

- the Java and Spring Boot stack has been confirmed for Class 02;
- H2 has been selected as the database for this exercise;
- interface type (web, desktop, mobile, or terminal);
- production authentication and authorization level;
- production data model for users and access control (the implementation-exercise schema is documented in `database.md`);
- backup, history, and recovery mechanisms.

## Scope limitations

At this stage, the project should keep a controlled scope and focus only on:
- creating and managing projects;
- creating and managing tasks;
- reading and updating status;
- simple search.

More advanced features such as authentication, permissions, integrations, advanced reports, or resource management are not part of the current scope and should be discussed and validated in later phases.

For this implementation exercise only, HTTP Basic authentication and the project-scoped manager/member policy approved by the user are used to seed and test distinct access. Comments and task assignments are also included because the exercise requires their persistence; they remain outside the initially documented product MVP.
