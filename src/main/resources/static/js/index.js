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
const dashboard = document.querySelector("#dashboard");
const dashboardLoading = document.querySelector("#dashboard-loading");
const dashboardError = document.querySelector("#dashboard-error");
const dashboardCounts = document.querySelector("#dashboard-counts");
const dashboardProjectTotal = document.querySelector("#dashboard-project-total");
const taskList = document.querySelector("#task-list");
const taskCounts = document.querySelector("#task-counts");
const emptyTasks = document.querySelector("#empty-tasks");
const tasksLoading = document.querySelector("#tasks-loading");
const taskFilterForm = document.querySelector("#task-filter-form");
const createTaskForm = document.querySelector("#create-task-form");
const projectLoading = document.querySelector("#projects-loading");
let selectedProject = null;
let eligibleMembers = [];
let accessibleProjects = [];
let taskCountsByProject = new Map();

const user = await requireSession();
if (user) {
    profile.textContent = `${user.displayName} (${user.email})`;
    await Promise.all([loadProjects(), loadDashboard()]);
    const savedProjectId = localStorage.getItem("matriz.selectedProjectId");
    if (savedProjectId) {
        await selectProject(savedProjectId, false);
    }
}

async function request(url, options) {
    try {
        return await apiFetch(url, options);
    } catch (error) {
        console.error("Matriz API request failed.", error);
        message.textContent = "Unable to connect to the Backend. Check the connection and try again.";
        return null;
    }
}

async function readError(response, fallback) {
    try {
        const body = await response.json();
        const validationDetails = Array.isArray(body.errors)
            ? body.errors.map(error => error.defaultMessage).filter(Boolean).join(" ")
            : "";
        return validationDetails || body.message || fallback;
    } catch {
        return fallback;
    }
}

async function withBusy(element, action) {
    if (element.dataset.busy === "true") {
        return;
    }
    element.dataset.busy = "true";
    element.setAttribute("aria-busy", "true");
    const buttons = [
        ...(element.matches("button") ? [element] : []),
        ...element.querySelectorAll("button")
    ];
    const originalDisabled = buttons.map(button => button.disabled);
    buttons.forEach(button => { button.disabled = true; });
    try {
        await action();
    } catch (error) {
        console.error("Matriz frontend action failed.", error);
        message.textContent = "The operation could not be completed. Your entered data has been kept.";
    } finally {
        buttons.forEach((button, index) => {
            if (button.isConnected) {
                button.disabled = originalDisabled[index];
            }
        });
        delete element.dataset.busy;
        element.removeAttribute("aria-busy");
    }
}

async function loadProjects(selectId) {
    const query = document.querySelector("#search").value.trim();
    const suffix = query ? `?q=${encodeURIComponent(query)}` : "";
    projectLoading.hidden = false;
    const response = await request(`/projects${suffix}`);
    if (!response) {
        projectLoading.hidden = true;
        return;
    }
    if (!response.ok) {
        message.textContent = await readError(response, "Projects could not be loaded.");
        projectLoading.hidden = true;
        return;
    }

    const result = await response.json();
    if (!query) {
        accessibleProjects = result.items;
    }
    projectList.replaceChildren();
    emptyProjects.hidden = result.items.length > 0;
    projectLoading.hidden = true;
    for (const project of result.items) {
        const item = document.createElement("li");
        const button = document.createElement("button");
        button.type = "button";
        button.className = "project-list-item";
        const name = document.createElement("span");
        name.textContent = project.name;
        const projectTaskCounts = taskCountsByProject.get(project.id) || {
            total: 0,
            todo: 0,
            inProgress: 0,
            done: 0
        };
        const count = document.createElement("small");
        count.className = "project-task-count";
        count.textContent = `${projectTaskCounts.total} tasks · ${projectTaskCounts.todo} to do · ${projectTaskCounts.inProgress} in progress · ${projectTaskCounts.done} done`;
        button.dataset.projectId = project.id;
        button.append(name, count);
        button.setAttribute("aria-current", selectedProject?.id === project.id ? "true" : "false");
        button.addEventListener("click", () => selectProject(project.id));
        item.append(button);
        projectList.append(item);
    }
    dashboardProjectTotal.textContent = `${accessibleProjects.length} accessible project${accessibleProjects.length === 1 ? "" : "s"}.`;
    if (selectId) {
        await selectProject(selectId);
    }
}

async function loadDashboard() {
    dashboardLoading.hidden = false;
    dashboardError.hidden = true;
    const response = await request("/tasks");
    dashboardLoading.hidden = true;
    if (!response) {
        dashboardError.textContent = message.textContent;
        dashboardError.hidden = false;
        return;
    }
    if (!response.ok) {
        dashboardError.textContent = await readError(response, "Overview could not be loaded.");
        dashboardError.hidden = false;
        return;
    }

    const result = await response.json();
    renderTaskCounts(dashboardCounts, result.counts);
    taskCountsByProject = new Map();
    for (const task of result.items) {
        const counts = taskCountsByProject.get(task.projectId) || {
            total: 0,
            todo: 0,
            inProgress: 0,
            done: 0
        };
        counts.total += 1;
        if (task.status === "TODO") counts.todo += 1;
        if (task.status === "IN_PROGRESS") counts.inProgress += 1;
        if (task.status === "DONE") counts.done += 1;
        taskCountsByProject.set(task.projectId, counts);
    }
    dashboardProjectTotal.textContent = `${accessibleProjects.length} accessible project${accessibleProjects.length === 1 ? "" : "s"}.`;
    updateProjectTaskCounts();
}

function updateProjectTaskCounts() {
    for (const project of accessibleProjects) {
        const count = projectList.querySelector(
            `[data-project-id="${project.id}"] .project-task-count`);
        if (!count) continue;
        const counts = taskCountsByProject.get(project.id) || {
            total: 0,
            todo: 0,
            inProgress: 0,
            done: 0
        };
        count.textContent = `${counts.total} tasks · ${counts.todo} to do · ${counts.inProgress} in progress · ${counts.done} done`;
    }
}

function renderTaskCounts(container, counts) {
    const entries = [
        ["All tasks", counts.total],
        ["To do", counts.todo],
        ["In progress", counts.inProgress],
        ["Done", counts.done]
    ];
    container.replaceChildren();
    for (const [label, value] of entries) {
        const card = document.createElement("div");
        card.className = "count-card";
        const count = document.createElement("strong");
        count.textContent = value;
        const caption = document.createElement("span");
        caption.textContent = label;
        card.append(count, caption);
        container.append(card);
    }
}

function showDashboard() {
    selectedProject = null;
    localStorage.removeItem("matriz.selectedProjectId");
    dashboard.hidden = false;
    details.hidden = true;
    createPanel.hidden = true;
    welcome.hidden = true;
    editForm.hidden = true;
    loadProjects();
}

async function selectProject(projectId, persist = true) {
    message.textContent = "Loading project...";
    const response = await request(`/projects/${encodeURIComponent(projectId)}`);
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
    dashboard.hidden = true;
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
    await loadEligibleMembers();
    await loadTasks();
}

async function loadEligibleMembers() {
    eligibleMembers = [];
    if (!selectedProject || selectedProject.role !== "MANAGER") {
        return;
    }

    const response = await request(`/projects/${encodeURIComponent(selectedProject.id)}/members`);
    if (!response) {
        return;
    }
    if (!response.ok) {
        message.textContent = await readError(response, "Eligible project members could not be loaded.");
        return;
    }
    eligibleMembers = await response.json();
}

async function loadTasks() {
    if (!selectedProject) {
        return;
    }

    tasksLoading.hidden = false;
    emptyTasks.hidden = true;
    taskCounts.textContent = "";
    const params = new URLSearchParams({ projectId: selectedProject.id });
    const query = taskFilterForm.elements.q.value.trim();
    const status = taskFilterForm.elements.status.value;
    const priority = taskFilterForm.elements.priority.value;
    if (query) params.set("q", query);
    if (status) params.set("status", status);
    if (priority) params.set("priority", priority);

    const response = await request(`/tasks?${params}`);
    if (!response) {
        tasksLoading.hidden = true;
        return;
    }
    if (!response.ok) {
        message.textContent = await readError(response, "Tasks could not be loaded.");
        tasksLoading.hidden = true;
        return;
    }

    const result = await response.json();
    renderTaskCounts(taskCounts, result.counts);
    taskList.replaceChildren();
    emptyTasks.hidden = result.items.length > 0;
    tasksLoading.hidden = true;
    for (const task of result.items) {
        taskList.append(createTaskCard(task));
    }
}

async function refreshTaskViews() {
    await Promise.all([loadTasks(), loadDashboard()]);
}

function createTaskCard(task, open = false) {
    const item = document.createElement("li");
    const taskDetails = document.createElement("details");
    taskDetails.className = "task-card";
    taskDetails.open = open;
    const summary = document.createElement("summary");
    summary.textContent = `${task.title} · ${task.status.replace("_", " ")} · ${task.priority}`;
    const body = document.createElement("div");
    body.className = "task-card-body";
    taskDetails.append(summary, body);
    item.append(taskDetails);
    let taskLoaded = open;
    taskDetails.addEventListener("toggle", async () => {
        if (!taskDetails.open) {
            taskLoaded = false;
            return;
        }
        if (taskLoaded) return;
        taskLoaded = true;
        const response = await request(`/tasks/${encodeURIComponent(task.id)}`);
        if (!response) {
            taskLoaded = false;
            taskDetails.open = false;
            return;
        }
        if (!response.ok) {
            message.textContent = await readError(response, "Task details could not be loaded.");
            taskLoaded = false;
            taskDetails.open = false;
            return;
        }
        item.replaceWith(createTaskCard(await response.json(), true));
    });
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
            await withBusy(remove, async () => {
                const response = await request(`/tasks/${encodeURIComponent(task.id)}`, { method: "DELETE" });
                if (!response) return;
                if (!response.ok) {
                    message.textContent = await readError(response, "Task could not be deleted.");
                    return;
                }
                message.textContent = "Task deleted.";
                await refreshTaskViews();
            });
        });
        actions.append(remove);
    }

    form.append(titleLabel, title, descriptionLabel, description, statusLabel, status,
        priorityLabel, priority, actions);

    const assigneeSection = document.createElement("section");
    assigneeSection.className = "task-assignees";
    const assigneeHeading = document.createElement("h5");
    assigneeHeading.textContent = "Assignees";
    const assigneeList = document.createElement("ul");
    assigneeList.className = "assignee-list";
    for (const assignment of task.assignees) {
        const row = document.createElement("li");
        const assigneeName = document.createElement("span");
        assigneeName.textContent = `${assignment.assignee.displayName} (${assignment.assignee.email})`;
        row.append(assigneeName);
        if (selectedProject.role === "MANAGER") {
            const remove = document.createElement("button");
            remove.type = "button";
            remove.className = "secondary";
            remove.textContent = "Remove";
            remove.addEventListener("click", async () => {
                await withBusy(remove, async () => {
                    const response = await request(
                        `/tasks/${encodeURIComponent(task.id)}/assignees/${encodeURIComponent(assignment.id)}`,
                        { method: "DELETE" });
                    if (!response) return;
                    if (!response.ok) {
                        message.textContent = await readError(response, "Assignee could not be removed.");
                        return;
                    }
                    message.textContent = "Assignee removed.";
                    await refreshTaskViews();
                });
            });
            row.append(remove);
        }
        assigneeList.append(row);
    }
    assigneeSection.append(assigneeHeading, assigneeList);

    if (selectedProject.role === "MANAGER") {
        const assignedUserIds = new Set(task.assignees.map(assignment => assignment.assignee.id));
        const availableMembers = eligibleMembers.filter(member => !assignedUserIds.has(member.id));
        const assignmentForm = document.createElement("form");
        assignmentForm.className = "assignee-form";
        const memberSelect = document.createElement("select");
        memberSelect.name = "assigneeId";
        memberSelect.required = true;
        const placeholder = document.createElement("option");
        placeholder.value = "";
        placeholder.textContent = availableMembers.length ? "Select an eligible member" : "No eligible members available";
        placeholder.disabled = true;
        placeholder.selected = true;
        memberSelect.append(placeholder);
        for (const member of availableMembers) {
            const option = document.createElement("option");
            option.value = member.id;
            option.textContent = `${member.displayName} (${member.email})`;
            memberSelect.append(option);
        }
        const assignButton = document.createElement("button");
        assignButton.type = "submit";
        assignButton.textContent = "Add assignee";
        assignButton.disabled = availableMembers.length === 0;
        assignmentForm.append(memberSelect, assignButton);
        assignmentForm.addEventListener("submit", async event => {
            event.preventDefault();
            await withBusy(assignmentForm, async () => {
                const response = await request(`/tasks/${encodeURIComponent(task.id)}/assignees`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ assigneeId: memberSelect.value })
                });
                if (!response) return;
                if (!response.ok) {
                    message.textContent = await readError(response, "Assignee could not be added.");
                    return;
                }
                message.textContent = "Assignee added.";
                await refreshTaskViews();
            });
        });
        assigneeSection.append(assignmentForm);
    }
    item.append(assigneeSection);

    const commentsSection = document.createElement("details");
    commentsSection.className = "task-comments";
    const commentsSummary = document.createElement("summary");
    commentsSummary.textContent = "Comments";
    const commentsLoading = document.createElement("p");
    commentsLoading.className = "muted";
    commentsLoading.textContent = "Loading comments...";
    commentsLoading.hidden = true;
    const commentsEmpty = document.createElement("p");
    commentsEmpty.className = "muted";
    commentsEmpty.textContent = "No comments yet.";
    commentsEmpty.hidden = true;
    const commentsList = document.createElement("ul");
    commentsList.className = "comment-list";
    const commentForm = document.createElement("form");
    commentForm.className = "comment-form";
    const commentLabel = document.createElement("label");
    commentLabel.textContent = "Add a comment";
    const commentBody = document.createElement("textarea");
    commentBody.name = "body";
    commentBody.maxLength = 2000;
    commentBody.rows = 3;
    commentBody.required = true;
    const addComment = document.createElement("button");
    addComment.type = "submit";
    addComment.textContent = "Add comment";
    commentForm.append(commentLabel, commentBody, addComment);
    commentsSection.append(commentsSummary, commentsLoading, commentsEmpty, commentsList, commentForm);
    commentsSection.addEventListener("toggle", async () => {
        if (commentsSection.open) {
            await loadTaskComments(task.id, commentsList, commentsLoading, commentsEmpty);
        }
    });
    commentForm.addEventListener("submit", async event => {
        event.preventDefault();
        if (!commentBody.value.trim()) {
            message.textContent = "A comment must contain non-whitespace text.";
            commentBody.focus();
            return;
        }
        await withBusy(commentForm, async () => {
            const response = await request(`/tasks/${encodeURIComponent(task.id)}/comments`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ body: commentBody.value })
            });
            if (!response) return;
            if (response.status !== 201) {
                message.textContent = await readError(response, "Comment could not be added.");
                return;
            }
            const createdComment = await response.json();
            commentForm.reset();
            message.textContent = "Comment added.";
            commentsEmpty.hidden = true;
            appendComment(commentsList, createdComment);
        });
    });
    body.append(commentsSection);

    form.addEventListener("submit", async event => {
        event.preventDefault();
        await withBusy(form, async () => {
            const response = await request(`/tasks/${encodeURIComponent(task.id)}`, {
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
            await refreshTaskViews();
        });
    });

    body.prepend(form);
    return item;
}

async function loadTaskComments(taskId, list, loading, empty) {
    loading.hidden = false;
    empty.hidden = true;
    const response = await request(`/tasks/${encodeURIComponent(taskId)}/comments`);
    if (!response) {
        loading.hidden = true;
        return;
    }
    if (!response.ok) {
        message.textContent = await readError(response, "Comments could not be loaded.");
        loading.hidden = true;
        return;
    }

    const comments = await response.json();
    list.replaceChildren();
    empty.hidden = comments.length > 0;
    for (const comment of comments) {
        appendComment(list, comment);
    }
    loading.hidden = true;
}

function appendComment(list, comment) {
    const item = document.createElement("li");
    const metadata = document.createElement("p");
    const author = document.createElement("strong");
    author.textContent = comment.authorName;
    const date = document.createElement("time");
    date.dateTime = comment.createdAt;
    date.textContent = new Date(comment.createdAt).toLocaleString();
    metadata.append(author, document.createTextNode(" · "), date);
    const body = document.createElement("p");
    body.className = "comment-body";
    body.textContent = comment.body;
    item.append(metadata, body);
    list.append(item);
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
    dashboard.hidden = true;
    editForm.hidden = true;
    details.hidden = true;
    welcome.hidden = true;
    createForm.reset();
    createPanel.hidden = false;
    document.querySelector("#create-name").focus();
});

document.querySelector("#cancel-create").addEventListener("click", () => {
    createPanel.hidden = true;
    dashboard.hidden = Boolean(selectedProject);
    welcome.hidden = true;
    details.hidden = !selectedProject;
});

document.querySelector("#show-dashboard").addEventListener("click", showDashboard);
document.querySelector("#back-to-dashboard").addEventListener("click", showDashboard);

searchForm.addEventListener("submit", async event => {
    event.preventDefault();
    await withBusy(searchForm, loadProjects);
});

createForm.addEventListener("submit", async event => {
    event.preventDefault();
    await withBusy(createForm, async () => {
        const response = await request("/projects", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                name: createForm.elements.name.value,
                description: createForm.elements.description.value || null
            })
        });
        if (!response) return;
        if (response.status !== 201) {
            message.textContent = await readError(response, "Project could not be created.");
            return;
        }

        const project = await response.json();
        createForm.reset();
        createPanel.hidden = true;
        await Promise.all([loadProjects(), loadDashboard()]);
        await selectProject(project.id);
        message.textContent = "Project created.";
    });
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
    await withBusy(editForm, async () => {
        const response = await request(`/projects/${encodeURIComponent(selectedProject.id)}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                name: editForm.elements.name.value,
                description: editForm.elements.description.value || null
            })
        });
        if (!response) return;
        if (!response.ok) {
            message.textContent = await readError(response, "Project could not be updated.");
            return;
        }

        selectedProject = await response.json();
        document.querySelector("#detail-name").textContent = selectedProject.name;
        document.querySelector("#detail-description").textContent = selectedProject.description || "No description.";
        editForm.hidden = true;
        await loadProjects();
        message.textContent = "Project updated.";
    });
});

document.querySelector("#delete-project").addEventListener("click", async event => {
    if (!selectedProject || !window.confirm(`Delete project "${selectedProject.name}"? This cannot be undone.`)) {
        return;
    }
    await withBusy(event.currentTarget, async () => {
        const response = await request(`/projects?projectId=${encodeURIComponent(selectedProject.id)}`, {
            method: "DELETE"
        });
        if (!response) return;
        if (!response.ok) {
            message.textContent = await readError(response, "Project could not be deleted.");
            return;
        }

        localStorage.removeItem("matriz.selectedProjectId");
        selectedProject = null;
        details.hidden = true;
        dashboard.hidden = false;
        welcome.hidden = true;
        message.textContent = "Project deleted.";
        await Promise.all([loadProjects(), loadDashboard()]);
    });
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
    await withBusy(createTaskForm, async () => {
        const response = await request("/tasks", {
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
        await refreshTaskViews();
    });
});

taskFilterForm.addEventListener("submit", async event => {
    event.preventDefault();
    await withBusy(taskFilterForm, loadTasks);
});

logoutButton.addEventListener("click", async () => {
    await withBusy(logoutButton, async () => {
        const response = await request("/auth/logout", { method: "POST" });
        if (response?.status === 204) {
            window.location.replace("/login.html?reason=logout");
        } else if (response) {
            message.textContent = await readError(response, "Sign out could not be completed.");
        }
    });
});
