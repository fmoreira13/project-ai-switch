import { apiFetch, requireSession } from "./auth.js";

const profile = document.querySelector("#profile");
const message = document.querySelector("#message");
const logoutButton = document.querySelector("#logout");
const projectList = document.querySelector("#project-list");
const emptyProjects = document.querySelector("#empty-projects");
const searchForm = document.querySelector("#search-form");
const createPanel = document.querySelector("#create-panel");
const createForm = document.querySelector("#create-form");
const editForm = document.querySelector("#edit-form");
const details = document.querySelector("#project-details");
const managerActions = document.querySelector("#manager-actions");
const welcome = document.querySelector("#welcome");
const taskList = document.querySelector("#task-list");
const taskCounts = document.querySelector("#task-counts");
const emptyTasks = document.querySelector("#empty-tasks");
const taskFilterForm = document.querySelector("#task-filter-form");
const createTaskForm = document.querySelector("#create-task-form");
let selectedProject = null;

const user = await requireSession();
if (user) {
    profile.textContent = `${user.displayName} (${user.email})`;
    await loadProjects();
    const savedProjectId = localStorage.getItem("matriz.selectedProjectId");
    if (savedProjectId) {
        await selectProject(savedProjectId, false);
    }
}

async function readError(response, fallback) {
    try {
        const body = await response.json();
        return body.message || fallback;
    } catch {
        return fallback;
    }
}

async function loadProjects(selectId) {
    const query = document.querySelector("#search").value.trim();
    const suffix = query ? `?q=${encodeURIComponent(query)}` : "";
    const response = await apiFetch(`/projects${suffix}`);
    if (!response) {
        return;
    }
    if (!response.ok) {
        message.textContent = await readError(response, "Projects could not be loaded.");
        return;
    }

    const result = await response.json();
    projectList.replaceChildren();
    emptyProjects.hidden = result.items.length > 0;
    for (const project of result.items) {
        const item = document.createElement("li");
        const button = document.createElement("button");
        button.type = "button";
        button.className = "project-list-item";
        button.textContent = project.name;
        button.setAttribute("aria-current", selectedProject?.id === project.id ? "true" : "false");
        button.addEventListener("click", () => selectProject(project.id));
        item.append(button);
        projectList.append(item);
    }
    if (selectId) {
        await selectProject(selectId);
    }
}

async function selectProject(projectId, persist = true) {
    const response = await apiFetch(`/projects/${encodeURIComponent(projectId)}`);
    if (!response) {
        return;
    }
    if (!response.ok) {
        message.textContent = await readError(response, "Project details could not be loaded.");
        selectedProject = null;
        return;
    }

    selectedProject = await response.json();
    if (persist) {
        localStorage.setItem("matriz.selectedProjectId", selectedProject.id);
    }
    message.textContent = "";
    document.querySelector("#detail-name").textContent = selectedProject.name;
    document.querySelector("#detail-description").textContent = selectedProject.description || "No description.";
    document.querySelector("#detail-role").textContent = `Your role: ${selectedProject.role.toLowerCase()}`;
    managerActions.hidden = selectedProject.role !== "MANAGER";
    document.querySelector("#new-task").hidden = selectedProject.role !== "MANAGER";
    createTaskForm.hidden = true;
    createTaskForm.reset();
    editForm.hidden = true;
    details.hidden = false;
    welcome.hidden = true;
    createPanel.hidden = true;
    await loadProjects();
    await loadTasks();
}

async function loadTasks() {
    if (!selectedProject) {
        return;
    }

    taskList.replaceChildren();
    emptyTasks.hidden = true;
    taskCounts.textContent = "";
    const params = new URLSearchParams({ projectId: selectedProject.id });
    const query = taskFilterForm.elements.q.value.trim();
    const status = taskFilterForm.elements.status.value;
    const priority = taskFilterForm.elements.priority.value;
    if (query) params.set("q", query);
    if (status) params.set("status", status);
    if (priority) params.set("priority", priority);

    const response = await apiFetch(`/tasks?${params}`);
    if (!response) {
        return;
    }
    if (!response.ok) {
        message.textContent = await readError(response, "Tasks could not be loaded.");
        return;
    }

    const result = await response.json();
    const counts = result.counts;
    taskCounts.textContent = `All tasks: ${counts.total} · To do: ${counts.todo} · In progress: ${counts.inProgress} · Done: ${counts.done}`;
    taskList.replaceChildren();
    emptyTasks.hidden = result.items.length > 0;
    for (const task of result.items) {
        taskList.append(createTaskCard(task));
    }
}

function createTaskCard(task) {
    const item = document.createElement("li");
    item.className = "task-card";
    const form = document.createElement("form");
    form.className = "task-edit-form";

    const titleLabel = document.createElement("label");
    titleLabel.textContent = "Title";
    const title = document.createElement("input");
    title.name = "title";
    title.minLength = 3;
    title.maxLength = 150;
    title.required = true;
    title.value = task.title;

    const descriptionLabel = document.createElement("label");
    descriptionLabel.textContent = "Description";
    const description = document.createElement("textarea");
    description.name = "description";
    description.maxLength = 2000;
    description.rows = 2;
    description.value = task.description || "";

    const statusLabel = document.createElement("label");
    statusLabel.textContent = "Status";
    const status = makeSelect("status", [
        ["TODO", "To do"],
        ["IN_PROGRESS", "In progress"],
        ["DONE", "Done"]
    ], task.status);

    const priorityLabel = document.createElement("label");
    priorityLabel.textContent = "Priority";
    const priority = makeSelect("priority", [
        ["LOW", "Low"],
        ["MEDIUM", "Medium"],
        ["HIGH", "High"]
    ], task.priority);

    const actions = document.createElement("div");
    actions.className = "form-actions";
    const save = document.createElement("button");
    save.type = "submit";
    save.textContent = "Save task";
    actions.append(save);
    if (selectedProject.role === "MANAGER") {
        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "danger";
        remove.textContent = "Delete task";
        remove.addEventListener("click", async () => {
            if (!window.confirm(`Delete task "${task.title}"? This cannot be undone.`)) {
                return;
            }
            const response = await apiFetch(`/tasks/${encodeURIComponent(task.id)}`, { method: "DELETE" });
            if (!response) return;
            if (!response.ok) {
                message.textContent = await readError(response, "Task could not be deleted.");
                return;
            }
            message.textContent = "Task deleted.";
            await loadTasks();
        });
        actions.append(remove);
    }

    form.append(titleLabel, title, descriptionLabel, description, statusLabel, status,
        priorityLabel, priority, actions);
    form.addEventListener("submit", async event => {
        event.preventDefault();
        const response = await apiFetch(`/tasks/${encodeURIComponent(task.id)}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                title: title.value,
                description: description.value || null,
                status: status.value,
                priority: priority.value
            })
        });
        if (!response) return;
        if (!response.ok) {
            message.textContent = await readError(response, "Task could not be updated.");
            return;
        }
        message.textContent = "Task updated.";
        await loadTasks();
    });

    item.append(form);
    return item;
}

function makeSelect(name, options, selected) {
    const select = document.createElement("select");
    select.name = name;
    for (const [value, label] of options) {
        const option = document.createElement("option");
        option.value = value;
        option.textContent = label;
        option.selected = value === selected;
        select.append(option);
    }
    return select;
}

document.querySelector("#new-project").addEventListener("click", () => {
    selectedProject = null;
    editForm.hidden = true;
    details.hidden = true;
    welcome.hidden = true;
    createForm.reset();
    createPanel.hidden = false;
    document.querySelector("#create-name").focus();
});

document.querySelector("#cancel-create").addEventListener("click", () => {
    createPanel.hidden = true;
    welcome.hidden = !selectedProject;
    details.hidden = !selectedProject;
});

searchForm.addEventListener("submit", async event => {
    event.preventDefault();
    await loadProjects();
});

createForm.addEventListener("submit", async event => {
    event.preventDefault();
    const response = await apiFetch("/projects", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            name: createForm.elements.name.value,
            description: createForm.elements.description.value || null
        })
    });
    if (!response) {
        return;
    }
    if (response.status !== 201) {
        message.textContent = await readError(response, "Project could not be created.");
        return;
    }

    const project = await response.json();
    createForm.reset();
    createPanel.hidden = true;
    await loadProjects(project.id);
    message.textContent = "Project created.";
});

document.querySelector("#edit-project").addEventListener("click", () => {
    document.querySelector("#edit-name").value = selectedProject.name;
    document.querySelector("#edit-description").value = selectedProject.description || "";
    editForm.hidden = false;
    document.querySelector("#edit-name").focus();
});

document.querySelector("#cancel-edit").addEventListener("click", () => {
    editForm.hidden = true;
});

editForm.addEventListener("submit", async event => {
    event.preventDefault();
    const response = await apiFetch(`/projects/${encodeURIComponent(selectedProject.id)}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            name: editForm.elements.name.value,
            description: editForm.elements.description.value || null
        })
    });
    if (!response) {
        return;
    }
    if (!response.ok) {
        message.textContent = await readError(response, "Project could not be updated.");
        return;
    }

    await selectProject(selectedProject.id);
    message.textContent = "Project updated.";
});

document.querySelector("#delete-project").addEventListener("click", async () => {
    if (!selectedProject || !window.confirm(`Delete project "${selectedProject.name}"? This cannot be undone.`)) {
        return;
    }
    const response = await apiFetch(`/projects?projectId=${encodeURIComponent(selectedProject.id)}`, {
        method: "DELETE"
    });
    if (!response) {
        return;
    }
    if (!response.ok) {
        message.textContent = await readError(response, "Project could not be deleted.");
        return;
    }

    localStorage.removeItem("matriz.selectedProjectId");
    selectedProject = null;
    details.hidden = true;
    welcome.hidden = false;
    message.textContent = "Project deleted.";
    await loadProjects();
});

document.querySelector("#new-task").addEventListener("click", () => {
    createTaskForm.reset();
    createTaskForm.elements.status.value = "TODO";
    createTaskForm.elements.priority.value = "MEDIUM";
    createTaskForm.hidden = false;
    createTaskForm.elements.title.focus();
});

document.querySelector("#cancel-create-task").addEventListener("click", () => {
    createTaskForm.hidden = true;
});

createTaskForm.addEventListener("submit", async event => {
    event.preventDefault();
    const response = await apiFetch("/tasks", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            projectId: selectedProject.id,
            title: createTaskForm.elements.title.value,
            description: createTaskForm.elements.description.value || null,
            status: createTaskForm.elements.status.value,
            priority: createTaskForm.elements.priority.value
        })
    });
    if (!response) return;
    if (response.status !== 201) {
        message.textContent = await readError(response, "Task could not be created.");
        return;
    }
    createTaskForm.reset();
    createTaskForm.hidden = true;
    message.textContent = "Task created.";
    await loadTasks();
});

taskFilterForm.addEventListener("submit", async event => {
    event.preventDefault();
    await loadTasks();
});

logoutButton.addEventListener("click", async () => {
    logoutButton.disabled = true;
    try {
        const response = await apiFetch("/auth/logout", { method: "POST" });
        if (response?.status === 204) {
            window.location.replace("/login.html?reason=logout");
        } else if (response) {
            message.textContent = await readError(response, "Sign out could not be completed.");
        }
    } catch {
        message.textContent = "Unable to connect. Check your connection and try again.";
    } finally {
        logoutButton.disabled = false;
    }
});
