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

import java.util.Comparator;
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
    public List<ApiModels.ProjectResponse> list(String email, String query) {
        AppUser user = access.requireUser(email);
        String normalizedQuery = query == null ? "" : Project.normalizeName(query);
        return memberships.findAllByUser_Id(user.getId()).stream()
                .filter(membership -> membership.getProject().getNormalizedName().contains(normalizedQuery))
                .sorted(Comparator.comparing(membership -> membership.getProject().getNormalizedName()))
                .map(membership -> toResponse(membership.getProject(), membership.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ApiModels.ProjectResponse get(String email, UUID projectId) {
        AppUser user = access.requireUser(email);
        Project project = access.requireProject(projectId);
        ProjectMembership membership = access.requireMembership(project, user);
        return toResponse(project, membership.getRole());
    }

    @Transactional
    public ApiModels.ProjectResponse create(String email, ApiModels.CreateProjectRequest request) {
        AppUser manager = access.requireUser(email);
        String name = request.name().trim();
        validateTrimmedName(name);
        ensureNameAvailable(name, null);
        Project project = projects.save(new Project(name, request.description()));
        ProjectMembership membership = memberships.save(new ProjectMembership(project, manager, ProjectRole.MANAGER));
        return toResponse(project, membership.getRole());
    }

    @Transactional
    public ApiModels.ProjectResponse update(
            String email,
            UUID projectId,
            ApiModels.UpdateProjectRequest request) {
        AppUser user = access.requireUser(email);
        Project project = access.requireProject(projectId);
        ProjectMembership membership = access.requireManager(project, user);
        String name = request.name().trim();
        validateTrimmedName(name);
        ensureNameAvailable(name, projectId);
        project.update(name, request.description());
        return toResponse(project, membership.getRole());
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

    private void ensureNameAvailable(String name, UUID exceptProjectId) {
        String normalizedName = Project.normalizeName(name);
        boolean exists = exceptProjectId == null
                ? projects.existsByNormalizedName(normalizedName)
                : projects.existsByNormalizedNameAndIdNot(normalizedName, exceptProjectId);
        if (exists) {
            throw new ApiException(HttpStatus.CONFLICT, "PROJECT_NAME_ALREADY_EXISTS", "A project with this name already exists.");
        }
    }

    private void validateTrimmedName(String name) {
        if (name.length() < 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Project name must contain at least 3 characters.");
        }
    }

    private ApiModels.ProjectResponse toResponse(Project project, ProjectRole role) {
        return new ApiModels.ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt(),
                role);
    }
}
