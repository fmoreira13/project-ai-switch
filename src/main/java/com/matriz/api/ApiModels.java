package com.matriz.api;

import com.matriz.domain.ProjectRole;
import com.matriz.domain.TaskStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ApiModels {
    private ApiModels() {
    }

    public record CreateProjectRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 1000) String description) {
    }

    public record ProjectResponse(
            UUID id,
            String name,
            String description,
            Instant createdAt) {
    }

    public record ProjectListResponse(List<ProjectResponse> items) {
    }

    public record CreateTaskRequest(
            @NotNull UUID projectId,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 1000) String description,
            TaskStatus status) {
    }

    public record UpdateTaskRequest(
            @Size(min = 1, max = 200) String title,
            @Size(max = 1000) String description,
            TaskStatus status) {
    }

    public record TaskResponse(
            UUID id,
            UUID projectId,
            String title,
            String description,
            TaskStatus status,
            Instant createdAt,
            Instant completedAt,
            UserSummary assignee) {
    }

    public record TaskListResponse(List<TaskResponse> items) {
    }

    public record UserSummary(UUID id, String email, String displayName) {
    }

    public record ProjectMemberResponse(
            UUID id,
            String email,
            String displayName,
            ProjectRole role) {
    }

    public record CreateCommentRequest(@NotBlank @Size(max = 2000) String body) {
    }

    public record CommentResponse(
            UUID id,
            UUID taskId,
            UUID authorId,
            String authorName,
            String body,
            Instant createdAt) {
    }

    public record AssignTaskRequest(@NotBlank @Email String assigneeEmail) {
    }

    public record ErrorResponse(String code, String message) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 1000) String password) {
    }

    public record AuthUserResponse(UUID id, String email, String displayName) {
    }

    public record CsrfTokenResponse(String token) {
    }
}
