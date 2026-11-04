package com.matriz.service;

import com.matriz.domain.AppUser;
import com.matriz.domain.Project;
import com.matriz.domain.ProjectMembership;
import com.matriz.domain.ProjectRole;
import com.matriz.domain.Task;
import com.matriz.repository.ProjectMembershipRepository;
import com.matriz.repository.ProjectRepository;
import com.matriz.repository.TaskRepository;
import com.matriz.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AccessService {
    private final UserRepository users;
    private final ProjectMembershipRepository memberships;
    private final ProjectRepository projects;
    private final TaskRepository tasks;

    public AccessService(
            UserRepository users,
            ProjectMembershipRepository memberships,
            ProjectRepository projects,
            TaskRepository tasks) {
        this.users = users;
        this.memberships = memberships;
        this.projects = projects;
        this.tasks = tasks;
    }

    public AppUser requireUser(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Authenticated user was not found."));
    }

    public Project requireProject(UUID projectId) {
        return projects.findById(projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found."));
    }

    public Task requireTask(UUID taskId) {
        return tasks.findById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task not found."));
    }

    public ProjectMembership requireMembership(Project project, AppUser user) {
        return memberships.findByProject_IdAndUser_Id(project.getId(), user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "PROJECT_ACCESS_DENIED", "You are not a member of this project."));
    }

    public ProjectMembership requireTaskMembership(Task task, AppUser user) {
        return requireMembership(task.getProject(), user);
    }

    public ProjectMembership requireManager(Project project, AppUser user) {
        ProjectMembership membership = requireMembership(project, user);
        if (membership.getRole() != ProjectRole.MANAGER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "MANAGER_REQUIRED", "Project manager access is required.");
        }
        return membership;
    }
}
