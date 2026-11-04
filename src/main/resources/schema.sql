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
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(30) NOT NULL CHECK (status IN ('backlog', 'in_progress', 'review', 'done', 'cancelled')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_tasks_project_id ON tasks(project_id);

CREATE TABLE IF NOT EXISTS task_assignments (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL UNIQUE REFERENCES tasks(id),
    assignee_id UUID NOT NULL REFERENCES app_users(id),
    assigned_by_id UUID NOT NULL REFERENCES app_users(id),
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS task_comments (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id),
    author_id UUID NOT NULL REFERENCES app_users(id),
    body VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_task_comments_task_id ON task_comments(task_id);
