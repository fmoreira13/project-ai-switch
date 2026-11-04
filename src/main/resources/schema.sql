CREATE TABLE IF NOT EXISTS app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(1000),
    normalized_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE projects ADD COLUMN IF NOT EXISTS normalized_name VARCHAR(100);
UPDATE projects SET normalized_name = UPPER(TRIM(name)) WHERE normalized_name IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_projects_normalized_name ON projects(normalized_name);

CREATE TABLE IF NOT EXISTS project_memberships (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id),
    user_id UUID NOT NULL REFERENCES app_users(id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('MANAGER', 'MEMBER')),
    CONSTRAINT uq_project_membership UNIQUE (project_id, user_id)
);

CREATE TABLE IF NOT EXISTS tasks (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id),
    title VARCHAR(150) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_tasks_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT ck_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'))
);

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS priority VARCHAR(10) DEFAULT 'MEDIUM' NOT NULL;
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS CONSTRAINT_4BE8;
ALTER TABLE tasks ALTER COLUMN title VARCHAR(150);
ALTER TABLE tasks ALTER COLUMN description VARCHAR(2000);
UPDATE tasks SET status = CASE status
    WHEN 'backlog' THEN 'TODO'
    WHEN 'in_progress' THEN 'IN_PROGRESS'
    WHEN 'done' THEN 'DONE'
    ELSE status
END;
ALTER TABLE tasks ADD CONSTRAINT IF NOT EXISTS ck_tasks_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE'));
ALTER TABLE tasks ADD CONSTRAINT IF NOT EXISTS ck_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));

CREATE INDEX IF NOT EXISTS idx_tasks_project_id ON tasks(project_id);

CREATE TABLE IF NOT EXISTS task_assignments (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id),
    assignee_id UUID NOT NULL REFERENCES app_users(id),
    assigned_by_id UUID NOT NULL REFERENCES app_users(id),
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_task_assignments_task_assignee UNIQUE (task_id, assignee_id)
);

ALTER TABLE task_assignments DROP CONSTRAINT IF EXISTS CONSTRAINT_B3;
ALTER TABLE task_assignments ADD CONSTRAINT IF NOT EXISTS uq_task_assignments_task_assignee
    UNIQUE (task_id, assignee_id);

CREATE TABLE IF NOT EXISTS task_comments (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id),
    author_id UUID NOT NULL REFERENCES app_users(id),
    body VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_task_comments_task_id ON task_comments(task_id);
