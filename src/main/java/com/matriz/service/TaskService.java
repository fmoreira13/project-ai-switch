package com.matriz.service;

import com.matriz.api.ApiModels;
import com.matriz.domain.AppUser;
import com.matriz.domain.Project;
import com.matriz.domain.Task;
import com.matriz.domain.TaskAssignment;
import com.matriz.domain.TaskComment;
import com.matriz.domain.TaskPriority;
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
    public ApiModels.TaskListResponse list(
            String email,
            UUID projectId,
            String query,
            TaskStatus status,
            TaskPriority priority) {
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
        List<Task> allTasks = tasks.findAllByProject_IdInOrderByCreatedAtDesc(projectIds);
        ApiModels.TaskCounts counts = new ApiModels.TaskCounts(
                allTasks.size(),
                allTasks.stream().filter(task -> task.getStatus() == TaskStatus.TODO).count(),
                allTasks.stream().filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS).count(),
                allTasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count());
        List<ApiModels.TaskResponse> items = allTasks.stream()
                .filter(task -> normalizedQuery.isEmpty()
                        || task.getTitle().toLowerCase().contains(normalizedQuery)
                        || (task.getDescription() != null
                        && task.getDescription().toLowerCase().contains(normalizedQuery)))
                .filter(task -> status == null || task.getStatus() == status)
                .filter(task -> priority == null || task.getPriority() == priority)
                .map(this::toResponse)
                .toList();
        return new ApiModels.TaskListResponse(items, counts);
    }

    @Transactional(readOnly = true)
    public ApiModels.TaskResponse get(String email, UUID taskId) {
        AppUser user = access.requireUser(email);
        Task task = access.requireTask(taskId);
        access.requireTaskMembership(task, user);
        return toResponse(task);
    }

    @Transactional
    public ApiModels.TaskResponse create(String email, ApiModels.CreateTaskRequest request) {
        AppUser user = access.requireUser(email);
        Project project = access.requireProject(request.projectId());
        access.requireManager(project, user);
        String title = requireValidTitle(request.title());
        TaskStatus status = request.status() == null ? TaskStatus.TODO : request.status();
        TaskPriority priority = request.priority() == null ? TaskPriority.MEDIUM : request.priority();
        Task task = tasks.save(new Task(project, title, request.description(), status, priority));
        return toResponse(task);
    }

    @Transactional
    public ApiModels.TaskResponse update(String email, UUID taskId, ApiModels.UpdateTaskRequest request) {
        AppUser user = access.requireUser(email);
        Task task = access.requireTask(taskId);
        access.requireTaskMembership(task, user);
        if (request.title() == null && request.description() == null
                && request.status() == null && request.priority() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_UPDATE", "At least one task field must be provided.");
        }
        task.update(
                request.title() == null ? null : requireValidTitle(request.title()),
                request.description(),
                request.status(),
                request.priority());
        return toResponse(task);
    }

    @Transactional
    public ApiModels.TaskResponse replace(String email, UUID taskId, ApiModels.ReplaceTaskRequest request) {
        AppUser user = access.requireUser(email);
        Task task = access.requireTask(taskId);
        access.requireTaskMembership(task, user);
        task.replace(
                requireValidTitle(request.title()),
                request.description(),
                request.status(),
                request.priority());
        return toResponse(task);
    }

    @Transactional
    public void delete(String email, UUID taskId) {
        AppUser user = access.requireUser(email);
        Task task = access.requireTask(taskId);
        access.requireManager(task.getProject(), user);
        comments.deleteAll(comments.findAllByTask_IdOrderByCreatedAtAsc(taskId));
        assignments.deleteAll(assignments.findAllByTask_IdOrderByAssignedAtAsc(taskId));
        tasks.delete(task);
    }

    @Transactional
    public ApiModels.CommentResponse addComment(String email, UUID taskId, ApiModels.CreateCommentRequest request) {
        AppUser author = access.requireUser(email);
        Task task = access.requireTask(taskId);
        access.requireTaskMembership(task, author);
        TaskComment comment = comments.save(new TaskComment(task, author, request.body().strip()));
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
    public ApiModels.TaskResponse assign(String email, UUID taskId, UUID assigneeId) {
        AppUser manager = access.requireUser(email);
        Task task = access.requireTask(taskId);
        access.requireManager(task.getProject(), manager);
        AppUser assignee = users.findById(assigneeId)
                .filter(AppUser::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ASSIGNEE_NOT_FOUND", "Active assignee not found."));
        access.requireMembership(task.getProject(), assignee);

        if (assignments.findByTask_IdAndAssignee_Id(taskId, assigneeId).isPresent()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "TASK_ASSIGNEE_ALREADY_ASSIGNED",
                    "This user is already assigned to the task.");
        }
        assignments.save(new TaskAssignment(task, assignee, manager));
        return toResponse(task);
    }

    @Transactional
    public void unassign(String email, UUID taskId, UUID assignmentId) {
        AppUser manager = access.requireUser(email);
        Task task = access.requireTask(taskId);
        access.requireManager(task.getProject(), manager);
        TaskAssignment assignment = assignments.findByIdAndTask_Id(assignmentId, taskId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "TASK_ASSIGNMENT_NOT_FOUND",
                        "Task assignment not found."));
        assignments.delete(assignment);
    }

    private ApiModels.TaskResponse toResponse(Task task) {
        List<ApiModels.TaskAssignmentResponse> assignees =
                assignments.findAllByTask_IdOrderByAssignedAtAsc(task.getId()).stream()
                        .map(assignment -> new ApiModels.TaskAssignmentResponse(
                                assignment.getId(),
                                new ApiModels.UserSummary(
                                        assignment.getAssignee().getId(),
                                        assignment.getAssignee().getEmail(),
                                        assignment.getAssignee().getDisplayName()),
                                assignment.getAssignedAt()))
                        .toList();
        return new ApiModels.TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getCreatedAt(),
                task.getCompletedAt(),
                assignees);
    }

    private String requireValidTitle(String title) {
        String normalized = title.strip();
        if (normalized.length() < 3) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Task title must contain at least 3 non-whitespace characters.");
        }
        return normalized;
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
