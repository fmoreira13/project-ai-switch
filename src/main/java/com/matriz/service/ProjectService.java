package com.matriz.service;

import com.matriz.api.ApiModels;
import com.matriz.domain.AppUser;
import com.matriz.domain.Project;
import com.matriz.domain.ProjectMembership;
import com.matriz.domain.ProjectRole;
import com.matriz.repository.ProjectMembershipRepository;
import com.matriz.repository.ProjectRepository;
import com.matriz.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {
    private final AccessService access;
    private final ProjectRepository projects;
    private final ProjectMembershipRepository memberships;
    private final TaskRepository tasks;

    public ProjectService(
            AccessService access,
            ProjectRepository projects,
            ProjectMembershipRepository memberships,
            TaskRepository tasks) {
        this.access = access;
        this.projects = projects;
        this.memberships = memberships;
        this.tasks = tasks;
    }

    @Transactional(readOnly = true)
    public List<ApiModels.ProjectResponse> list(String email) {
        AppUser user = access.requireUser(email);
        return memberships.findAllByUser_Id(user.getId()).stream()
                .map(ProjectMembership::getProject)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ApiModels.ProjectResponse create(String email, ApiModels.CreateProjectRequest request) {
        access.requireUser(email);
        Project project = projects.save(new Project(request.name().trim(), request.description()));
        AppUser manager = access.requireUser(email);
        memberships.save(new ProjectMembership(project, manager, ProjectRole.MANAGER));
        return toResponse(project);
    }

    @Transactional
    public void delete(String email, UUID projectId) {
        AppUser user = access.requireUser(email);
        Project project = access.requireProject(projectId);
        access.requireManager(project, user);
        if (tasks.countByProject_Id(projectId) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "PROJECT_HAS_TASKS", "Remove the project's tasks before removing the project.");
        }
        memberships.deleteAll(memberships.findAllByProject_Id(projectId));
        projects.delete(project);
    }

    @Transactional(readOnly = true)
    public List<ApiModels.ProjectMemberResponse> listMembers(String email, UUID projectId) {
        AppUser user = access.requireUser(email);
        Project project = access.requireProject(projectId);
        access.requireManager(project, user);
        return memberships.findAllByProject_Id(projectId).stream()
                .filter(membership -> membership.getUser().isActive())
                .map(membership -> new ApiModels.ProjectMemberResponse(
                        membership.getUser().getId(),
                        membership.getUser().getEmail(),
                        membership.getUser().getDisplayName(),
                        membership.getRole()))
                .toList();
    }

    private ApiModels.ProjectResponse toResponse(Project project) {
        return new ApiModels.ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt());
    }
}
