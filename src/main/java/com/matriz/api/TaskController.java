package com.matriz.api;

import com.matriz.domain.TaskPriority;
import com.matriz.domain.TaskStatus;
import com.matriz.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class TaskController {
    private final TaskService tasks;

    public TaskController(TaskService tasks) {
        this.tasks = tasks;
    }

    @GetMapping
    public ApiModels.TaskListResponse list(
            Authentication authentication,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority) {
        return tasks.list(authentication.getName(), projectId, q, status, priority);
    }

    @GetMapping("/{taskId}")
    public ApiModels.TaskResponse get(
            Authentication authentication,
            @PathVariable UUID taskId) {
        return tasks.get(authentication.getName(), taskId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiModels.TaskResponse create(
            Authentication authentication,
            @Valid @RequestBody ApiModels.CreateTaskRequest request) {
        return tasks.create(authentication.getName(), request);
    }

    @PatchMapping
    public ApiModels.TaskResponse update(
            Authentication authentication,
            @RequestParam UUID taskId,
            @Valid @RequestBody ApiModels.UpdateTaskRequest request) {
        return tasks.update(authentication.getName(), taskId, request);
    }

    @PutMapping("/{taskId}")
    public ApiModels.TaskResponse replace(
            Authentication authentication,
            @PathVariable UUID taskId,
            @Valid @RequestBody ApiModels.ReplaceTaskRequest request) {
        return tasks.replace(authentication.getName(), taskId, request);
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(Authentication authentication, @PathVariable UUID taskId) {
        tasks.delete(authentication.getName(), taskId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @RequestParam UUID taskId) {
        tasks.delete(authentication.getName(), taskId);
    }

    @GetMapping("/{taskId}/comments")
    public List<ApiModels.CommentResponse> comments(
            Authentication authentication,
            @PathVariable UUID taskId) {
        return tasks.listComments(authentication.getName(), taskId);
    }

    @PostMapping("/{taskId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiModels.CommentResponse addComment(
            Authentication authentication,
            @PathVariable UUID taskId,
            @Valid @RequestBody ApiModels.CreateCommentRequest request) {
        return tasks.addComment(authentication.getName(), taskId, request);
    }

    @PutMapping("/{taskId}/assignee")
    public ApiModels.TaskResponse assign(
            Authentication authentication,
            @PathVariable UUID taskId,
            @Valid @RequestBody ApiModels.AssignTaskRequest request) {
        return tasks.assign(authentication.getName(), taskId, request.assigneeEmail());
    }

    @DeleteMapping("/{taskId}/assignee")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(Authentication authentication, @PathVariable UUID taskId) {
        tasks.unassign(authentication.getName(), taskId);
    }
}
