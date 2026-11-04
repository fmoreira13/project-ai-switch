package com.matriz.api;

import com.matriz.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectService projects;

    public ProjectController(ProjectService projects) {
        this.projects = projects;
    }

    @GetMapping
    public ApiModels.ProjectListResponse list(
            Authentication authentication,
            @RequestParam(required = false) String q) {
        return new ApiModels.ProjectListResponse(projects.list(authentication.getName(), q));
    }

    @GetMapping("/{projectId}")
    public ApiModels.ProjectResponse get(Authentication authentication, @PathVariable UUID projectId) {
        return projects.get(authentication.getName(), projectId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiModels.ProjectResponse create(
            Authentication authentication,
            @Valid @RequestBody ApiModels.CreateProjectRequest request) {
        return projects.create(authentication.getName(), request);
    }

    @PutMapping("/{projectId}")
    public ApiModels.ProjectResponse update(
            Authentication authentication,
            @PathVariable UUID projectId,
            @Valid @RequestBody ApiModels.UpdateProjectRequest request) {
        return projects.update(authentication.getName(), projectId, request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @RequestParam UUID projectId) {
        projects.delete(authentication.getName(), projectId);
    }

    @GetMapping("/{projectId}/members")
    public List<ApiModels.ProjectMemberResponse> members(
            Authentication authentication,
            @PathVariable UUID projectId) {
        return projects.listMembers(authentication.getName(), projectId);
    }
}
