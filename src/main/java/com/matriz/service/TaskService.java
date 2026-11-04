package com.matriz.service;

import com.matriz.api.ApiModels;
import com.matriz.domain.AppUser;
import com.matriz.domain.Project;
import com.matriz.domain.Task;
import com.matriz.domain.TaskAssignment;
import com.matriz.domain.TaskComment;
import com.matriz.domain.TaskStatus;
import com.matriz.repository.ProjectMembershipRepository;
import com.matriz.repository.TaskAssignmentRepository;
import com.matriz.repository.TaskCommentRepository;
import com.matriz.repository.TaskRepository;
import com.matriz.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TaskService {
    private final AccessService access;
    private final ProjectMembershipRepository memberships;
    private final TaskRepository tasks;
    private final TaskAssignmentRepository assignments;
    private final TaskCommentRepository comments;
    private final UserRepository users;

    public TaskService(
            AccessService access,
            ProjectMembershipRepository memberships,
            TaskRepository tasks,
            TaskAssignmentRepository assignments,
            TaskCommentRepository comments,
            UserRepository users) {
        this.access = access;
        this.memberships = memberships;
        this.tasks = tasks;
        this.assignments = assignments;
        this.comments = comments;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ApiModels.TaskResponse> list(String email, UUID projectId, String query) {
        AppUser user = access.requireUser(email);
        List<UUID> projectIds = memberships.findAllByUser_Id(user.getId()).stream()
                .map(membership -> membership.getProject().getId())
                .toList();

        if (projectId != null) {
            if (!projectIds.contains(projectId)) {
                access.requireProject(projectId);
                throw new ApiException(HttpStatus.FORBIDDEN, "PROJECT_ACCESS_DENIED", "You are not a member of this project.");
            }
            projectIds = List.of(projectId);
        }

        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        return tasks.findAllByProject_IdInOrderByCreatedAtDesc(projectIds).stream()
                .filter(task -> normalizedQuery.isEmpty()
                        || task.getTitle().toLowerCase().contains(normalizedQuery)
                        || (task.getDescription() != null
                        && task.getDescription().toLowerCase().contains(normalizedQuery)))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ApiModels.TaskResponse create(String email, ApiModels.CreateTaskRequest request) {
        AppUser user = access.requireUser(email);
        Project project = access.requireProject(request.projectId());
        access.requireManager(project, user);
        TaskStatus status = request.status() == null ? TaskStatus.backlog : request.status();
        Task task = tasks.save(new Task(project, request.title().trim(), request.description(), status));
        return toResponse(task);
    }

    @Transactional
    public ApiModels.TaskResponse update(String email, UUID taskId, ApiModels.UpdateTaskRequest request) {
        AppUser user = access.requireUser(email);
        Task task = access.requireTask(taskId);
        access.requireTaskMembership(task, user);
        if (request.title() == null && request.description() == null && request.status() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_UPDATE", "At least one task field must be provided.");
        }
        task.update(request.title() == null ? null : request.title().trim(), request.description(), request.status());
        return toResponse(task);
    }

    @Transactional
    public void delete(String email, UUID taskId) {
        AppUser user = access.requireUser(email);
        Task task = access.requireTask(taskId);
        access.requireManager(task.getProject(), user);
        comments.deleteAll(comments.findAllByTask_IdOrderByCreatedAtAsc(taskId));
        assignments.findByTask_Id(taskId).ifPresent(assignments::delete);
        tasks.delete(task);
    }

    @Transactional
    public ApiModels.CommentResponse addComment(String email, UUID taskId, ApiModels.CreateCommentRequest request) {
        AppUser author = access.requireUser(email);
        Task task = access.requireTask(taskId);
        access.requireTaskMembership(task, author);
        TaskComment comment = comments.save(new TaskComment(task, author, request.body().trim()));
        return toCommentResponse(comment);
    }

    @Transactional(readOnly = true)
    public List<ApiModels.CommentResponse> listComments(String email, UUID taskId) {
        AppUser user = access.requireUser(email);
        Task task = access.requireTask(taskId);
        access.requireTaskMembership(task, user);
        return comments.findAllByTask_IdOrderByCreatedAtAsc(taskId).stream()
                .map(this::toCommentResponse)
                .toList();
    }

    @Transactional
    public ApiModels.TaskResponse assign(String email, UUID taskId, String assigneeEmail) {
        AppUser manager = access.requireUser(email);
        Task task = access.requireTask(taskId);
        access.requireManager(task.getProject(), manager);
        AppUser assignee = users.findByEmailIgnoreCase(assigneeEmail)
                .filter(AppUser::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ASSIGNEE_NOT_FOUND", "Active assignee not found."));
        access.requireMembership(task.getProject(), assignee);

        TaskAssignment assignment = assignments.findByTask_Id(taskId)
                .map(existing -> {
                    existing.reassign(assignee, manager);
                    return existing;
                })
                .orElseGet(() -> new TaskAssignment(task, assignee, manager));
        assignments.save(assignment);
        return toResponse(task);
    }

    @Transactional
    public void unassign(String email, UUID taskId) {
        AppUser manager = access.requireUser(email);
        Task task = access.requireTask(taskId);
        access.requireManager(task.getProject(), manager);
        assignments.findByTask_Id(taskId).ifPresent(assignments::delete);
    }

    private ApiModels.TaskResponse toResponse(Task task) {
        ApiModels.UserSummary assignee = assignments.findByTask_Id(task.getId())
                .map(TaskAssignment::getAssignee)
                .map(user -> new ApiModels.UserSummary(user.getId(), user.getEmail(), user.getDisplayName()))
                .orElse(null);
        return new ApiModels.TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getCompletedAt(),
                assignee);
    }

    private ApiModels.CommentResponse toCommentResponse(TaskComment comment) {
        return new ApiModels.CommentResponse(
                comment.getId(),
                comment.getTask().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getDisplayName(),
                comment.getBody(),
                comment.getCreatedAt());
    }
}
