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
    editForm.hidden = true;
    details.hidden = false;
    welcome.hidden = true;
    createPanel.hidden = true;
    await loadProjects();
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
