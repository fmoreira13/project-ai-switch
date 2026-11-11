package com.matriz;

import com.matriz.domain.Project;
import com.matriz.domain.Task;
import com.matriz.repository.ProjectMembershipRepository;
import com.matriz.repository.ProjectRepository;
import com.matriz.repository.TaskAssignmentRepository;
import com.matriz.repository.TaskCommentRepository;
import com.matriz.repository.TaskRepository;
import com.matriz.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:matriz-test;DB_CLOSE_DELAY=-1",
        "matriz.demo.manager-password=test-manager-password",
        "matriz.demo.member-password=test-member-password"
})
class MatrizAccessIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projects;

    @Autowired
    private TaskRepository tasks;

    @Autowired
    private UserRepository users;

    @Autowired
    private ProjectMembershipRepository memberships;

    @Autowired
    private TaskAssignmentRepository assignments;

    @Autowired
    private TaskCommentRepository comments;

    @Autowired
    private ApplicationRunner initializeDemoData;

    @Test
    void managerAndMemberOnlySeeProjectsTheyBelongTo() throws Exception {
        MockHttpSession manager = login("manager@matriz.local", "test-manager-password");
        MockHttpSession member = login("member@matriz.local", "test-member-password");

        mockMvc.perform(get("/projects").session(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.name == 'Matriz Demo - Alpha')]").exists())
                .andExpect(jsonPath("$.items[?(@.name == 'Matriz Demo - Beta')]").doesNotExist());

        mockMvc.perform(get("/projects").session(member))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.name == 'Matriz Demo - Alpha')]").exists())
                .andExpect(jsonPath("$.items[?(@.name == 'Matriz Demo - Beta')]").exists());
    }

    @Test
    void projectMemberCanCommentAndUpdateTaskButCannotCreateOrAssignTasks() throws Exception {
        Project alpha = projects.findByName("Matriz Demo - Alpha").orElseThrow();
        Task task = tasks.findAllByProject_IdInOrderByCreatedAtDesc(java.util.List.of(alpha.getId()))
                .stream().findFirst().orElseThrow();
        MockHttpSession member = login("member@matriz.local", "test-member-password");

        mockMvc.perform(post("/tasks")
                        .session(member)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","title":"Member task"}
                                """.formatted(alpha.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/tasks")
                        .session(member)
                        .with(csrf())
                        .queryParam("taskId", task.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_PROGRESS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(post("/tasks/{taskId}/comments", task.getId())
                        .session(member)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Member comment","authorId":"00000000-0000-0000-0000-000000000000"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Member comment"))
                .andExpect(jsonPath("$.authorId").value(users.findByEmailIgnoreCase("member@matriz.local")
                        .orElseThrow().getId().toString()));

        mockMvc.perform(post("/tasks/{taskId}/assignees", task.getId())
                        .session(member)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assigneeId":"%s"}
                                """.formatted(users.findByEmailIgnoreCase("manager@matriz.local")
                                .orElseThrow().getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanAssignOnlyAnActiveMemberOfTheProject() throws Exception {
        Project alpha = projects.findByName("Matriz Demo - Alpha").orElseThrow();
        Task task = tasks.findAllByProject_IdInOrderByCreatedAtDesc(java.util.List.of(alpha.getId()))
                .stream().findFirst().orElseThrow();
        MockHttpSession manager = login("manager@matriz.local", "test-manager-password");
        MockHttpSession member = login("member@matriz.local", "test-member-password");

        mockMvc.perform(get("/projects/{projectId}/members", alpha.getId())
                        .session(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'member@matriz.local')]").exists());

        mockMvc.perform(post("/tasks/{taskId}/assignees", task.getId())
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assigneeId":"%s"}
                                """.formatted(users.findByEmailIgnoreCase("manager@matriz.local")
                                .orElseThrow().getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignees[?(@.assignee.email == 'manager@matriz.local')]").exists())
                .andExpect(jsonPath("$.assignees[?(@.assignee.email == 'member@matriz.local')]").exists());

        mockMvc.perform(post("/tasks/{taskId}/assignees", task.getId())
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assigneeId":"%s"}
                                """.formatted(users.findByEmailIgnoreCase("manager@matriz.local")
                                .orElseThrow().getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TASK_ASSIGNEE_ALREADY_ASSIGNED"));

        String managerAssignmentId = assignments.findByTask_IdAndAssignee_Id(
                        task.getId(),
                        users.findByEmailIgnoreCase("manager@matriz.local").orElseThrow().getId())
                .orElseThrow()
                .getId()
                .toString();
        mockMvc.perform(delete("/tasks/{taskId}/assignees/{assignmentId}", task.getId(), managerAssignmentId)
                        .session(manager)
                        .with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/tasks/{taskId}", task.getId()).session(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignees.length()").value(1))
                .andExpect(jsonPath("$.assignees[0].assignee.email").value("member@matriz.local"));

        mockMvc.perform(post("/tasks/{taskId}/assignees", task.getId())
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assigneeId":"%s"}
                                """.formatted(java.util.UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ASSIGNEE_NOT_FOUND"));

        Project beta = projects.findByName("Matriz Demo - Beta").orElseThrow();
        Task betaTask = tasks.findAllByProject_IdInOrderByCreatedAtDesc(java.util.List.of(beta.getId()))
                .stream().findFirst().orElseThrow();
        mockMvc.perform(post("/tasks/{taskId}/assignees", betaTask.getId())
                        .session(member)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assigneeId":"%s"}
                                """.formatted(users.findByEmailIgnoreCase("manager@matriz.local")
                                .orElseThrow().getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void commentsAreValidatedServerAuthoredAndAvailableWhenTaskIsReopened() throws Exception {
        Project alpha = projects.findByName("Matriz Demo - Alpha").orElseThrow();
        MockHttpSession manager = login("manager@matriz.local", "test-manager-password");
        MockHttpSession member = login("member@matriz.local", "test-member-password");
        MvcResult createdTask = mockMvc.perform(post("/tasks")
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","title":"Comment flow %s"}
                                """.formatted(alpha.getId(), java.util.UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn();
        String taskId = com.jayway.jsonpath.JsonPath.read(
                createdTask.getResponse().getContentAsString(), "$.id");
        String memberId = users.findByEmailIgnoreCase("member@matriz.local").orElseThrow().getId().toString();

        MvcResult createdComment = mockMvc.perform(post("/tasks/{taskId}/comments", taskId)
                        .session(member)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"  Persisted comment  ","authorId":"00000000-0000-0000-0000-000000000000","createdAt":"2000-01-01T00:00:00Z"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Persisted comment"))
                .andExpect(jsonPath("$.authorId").value(memberId))
                .andExpect(jsonPath("$.authorName").value("Demo Member"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();
        String commentId = com.jayway.jsonpath.JsonPath.read(
                createdComment.getResponse().getContentAsString(), "$.id");
        String createdAt = com.jayway.jsonpath.JsonPath.read(
                createdComment.getResponse().getContentAsString(), "$.createdAt");
        assertNotEquals("2000-01-01T00:00:00Z", createdAt);

        mockMvc.perform(get("/tasks/{taskId}", taskId).session(member))
                .andExpect(status().isOk());
        MvcResult listedComments = mockMvc.perform(get("/tasks/{taskId}/comments", taskId).session(member))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(commentId))
                .andExpect(jsonPath("$[0].body").value("Persisted comment"))
                .andExpect(jsonPath("$[0].authorId").value(memberId))
                .andExpect(jsonPath("$[0].authorName").value("Demo Member"))
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty())
                .andReturn();
        String listedCreatedAt = com.jayway.jsonpath.JsonPath.read(
                listedComments.getResponse().getContentAsString(), "$[0].createdAt");
        assertTrue(Duration.between(Instant.parse(createdAt), Instant.parse(listedCreatedAt))
                .abs().compareTo(Duration.ofSeconds(1)) <= 0);

        mockMvc.perform(post("/tasks/{taskId}/comments", taskId)
                        .session(member)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":" \\t\\n "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        MvcResult oneCharacterComment = mockMvc.perform(post("/tasks/{taskId}/comments", taskId)
                        .session(member)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"x"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        assertEquals("x", com.jayway.jsonpath.JsonPath.read(
                oneCharacterComment.getResponse().getContentAsString(), "$.body"));

        String maxComment = "c".repeat(2000);
        MvcResult maxLengthComment = mockMvc.perform(post("/tasks/{taskId}/comments", taskId)
                        .session(member)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"%s"}
                                """.formatted(maxComment)))
                .andExpect(status().isCreated())
                .andReturn();
        assertEquals(2000, ((String) com.jayway.jsonpath.JsonPath.read(
                maxLengthComment.getResponse().getContentAsString(), "$.body")).length());

        mockMvc.perform(post("/tasks/{taskId}/comments", taskId)
                        .session(member)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"%s"}
                                """.formatted("c".repeat(2001))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signInAndLandingPagesAreAvailableToTheSessionGuard() throws Exception {
        mockMvc.perform(get("/login.html")).andExpect(status().isOk());
        mockMvc.perform(get("/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/js/auth.js")).andExpect(status().isOk());
    }

    @Test
    void loginProfileAndLogoutUseAnHttpSession() throws Exception {
        assertTrue(users.findByEmailIgnoreCase("manager@matriz.local")
                .orElseThrow().getPassword().startsWith("$2a$"));

        MvcResult csrfResult = mockMvc.perform(get("/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        String csrfToken = com.jayway.jsonpath.JsonPath.read(
                csrfResult.getResponse().getContentAsString(), "$.token");
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        String anonymousSessionId = session.getId();

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .session(session)
                        .header("X-CSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"manager@matriz.local","password":"test-manager-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("manager@matriz.local"))
                .andReturn();
        session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotEquals(anonymousSessionId, session.getId());

        mockMvc.perform(get("/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("manager@matriz.local"))
                .andExpect(jsonPath("$.displayName").value("Demo Manager"));

        mockMvc.perform(post("/auth/logout")
                        .session(session)
                        .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isNoContent());
        assertTrue(session.isInvalid());

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRejectsInvalidCredentialsAndStateChangesRequireCsrf() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"manager@matriz.local","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"manager@matriz.local","password":"test-manager-password"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void projectCreatorIsInferredFromTheAuthenticatedUser() throws Exception {
        MockHttpSession manager = login("manager@matriz.local", "test-manager-password");
        mockMvc.perform(post("/projects")
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Session-owned project","ownerEmail":"member@matriz.local"}
                                """))
                .andExpect(status().isCreated());

        Project created = projects.findByName("Session-owned project").orElseThrow();
        var managerUser = users.findByEmailIgnoreCase("manager@matriz.local").orElseThrow();
        var memberUser = users.findByEmailIgnoreCase("member@matriz.local").orElseThrow();
        assertTrue(memberships.findByProject_IdAndUser_Id(created.getId(), managerUser.getId()).isPresent());
        assertTrue(memberships.findByProject_IdAndUser_Id(created.getId(), memberUser.getId()).isEmpty());
    }

    @Test
    void managerCanCreateListSearchConsultAndUpdateProjects() throws Exception {
        MockHttpSession manager = login("manager@matriz.local", "test-manager-password");
        String suffix = java.util.UUID.randomUUID().toString();
        String projectName = "Exercise Project " + suffix;

        MvcResult createdResult = mockMvc.perform(post("/projects")
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  %s  ","description":"Exercise description"}
                                """.formatted(projectName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(projectName))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andReturn();
        String projectId = com.jayway.jsonpath.JsonPath.read(
                createdResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/projects").session(manager).queryParam("q", suffix.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(projectId));

        mockMvc.perform(get("/projects/{projectId}", projectId).session(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(projectName))
                .andExpect(jsonPath("$.description").value("Exercise description"))
                .andExpect(jsonPath("$.role").value("MANAGER"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/projects/{projectId}", projectId)
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"matriz demo - alpha","description":"Duplicate"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_NAME_ALREADY_EXISTS"));

        String updatedName = "Updated Exercise " + suffix;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/projects/{projectId}", projectId)
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","description":"Updated description"}
                                """.formatted(updatedName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(updatedName))
                .andExpect(jsonPath("$.description").value("Updated description"));

        mockMvc.perform(get("/projects").session(manager).queryParam("q", suffix.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value(updatedName));

        mockMvc.perform(post("/projects")
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  updated exercise %s  "}
                                """.formatted(suffix)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_NAME_ALREADY_EXISTS"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/projects")
                        .session(manager)
                        .with(csrf())
                        .queryParam("projectId", projectId))
                .andExpect(status().isNoContent());
    }

    @Test
    void projectValidationAndCaseInsensitiveDuplicateNamesAreEnforced() throws Exception {
        MockHttpSession manager = login("manager@matriz.local", "test-manager-password");

        mockMvc.perform(post("/projects")
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"ab"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/projects")
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   "}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/projects")
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}
                                """.formatted("x".repeat(101))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/projects")
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Valid project name","description":"%s"}
                                """.formatted("x".repeat(1001))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/projects")
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  matriz demo - alpha  "}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_NAME_ALREADY_EXISTS"));

        mockMvc.perform(post("/projects")
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" a "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void projectDetailsAndMutationsRespectMembershipAndManagerRole() throws Exception {
        Project alpha = projects.findByName("Matriz Demo - Alpha").orElseThrow();
        Project beta = projects.findByName("Matriz Demo - Beta").orElseThrow();
        MockHttpSession manager = login("manager@matriz.local", "test-manager-password");
        MockHttpSession member = login("member@matriz.local", "test-member-password");

        mockMvc.perform(get("/projects/{projectId}", alpha.getId()).session(member))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"));

        mockMvc.perform(get("/projects/{projectId}", beta.getId()).session(manager))
                .andExpect(status().isForbidden());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/projects/{projectId}", alpha.getId())
                        .session(member)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Unauthorized update","description":null}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/projects")
                        .session(manager)
                        .with(csrf())
                        .queryParam("projectId", alpha.getId().toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_HAS_TASKS"));

        mockMvc.perform(get("/projects/{projectId}", java.util.UUID.randomUUID()).session(manager))
                .andExpect(status().isNotFound());
    }

    @Test
    void taskCrudFiltersValidationAndCountsFollowMvpContract() throws Exception {
        Project alpha = projects.findByName("Matriz Demo - Alpha").orElseThrow();
        MockHttpSession manager = login("manager@matriz.local", "test-manager-password");
        String title = "Ship task " + java.util.UUID.randomUUID();

        MvcResult created = mockMvc.perform(post("/tasks")
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","title":"  %s  ","description":"Task description"}
                                """.formatted(alpha.getId(), title)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andReturn();
        String taskId = com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/tasks/{taskId}", taskId).session(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId));

        mockMvc.perform(get("/tasks")
                        .session(manager)
                        .queryParam("projectId", alpha.getId().toString())
                        .queryParam("q", title)
                        .queryParam("status", "TODO")
                        .queryParam("priority", "MEDIUM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(taskId))
                .andExpect(jsonPath("$.counts.total").isNumber())
                .andExpect(jsonPath("$.counts.todo").isNumber())
                .andExpect(jsonPath("$.counts.inProgress").isNumber())
                .andExpect(jsonPath("$.counts.done").isNumber());

        mockMvc.perform(post("/tasks/{taskId}/assignees", taskId)
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assigneeId":"%s"}
                                """.formatted(users.findByEmailIgnoreCase("member@matriz.local")
                                .orElseThrow().getId())))
                .andExpect(status().isCreated());
        MockHttpSession member = login("member@matriz.local", "test-member-password");
        mockMvc.perform(post("/tasks/{taskId}/comments", taskId)
                        .session(member)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Task deletion cleanup check"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/tasks/{taskId}", taskId)
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Updated task title","description":null,"status":"DONE","priority":"LOW"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        mockMvc.perform(get("/tasks")
                        .session(manager)
                        .queryParam("projectId", alpha.getId().toString())
                        .queryParam("status", "DONE")
                        .queryParam("priority", "LOW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == '%s')]".formatted(taskId)).exists());

        mockMvc.perform(delete("/tasks/{taskId}", taskId)
                        .session(manager)
                        .with(csrf()))
                .andExpect(status().isNoContent());
        assertTrue(assignments.findAllByTask_IdOrderByAssignedAtAsc(java.util.UUID.fromString(taskId)).isEmpty());
        assertTrue(comments.findAllByTask_IdOrderByCreatedAtAsc(java.util.UUID.fromString(taskId)).isEmpty());
        mockMvc.perform(get("/tasks/{taskId}", taskId).session(manager))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/tasks")
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","title":"   "}
                                """.formatted(alpha.getId())))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/tasks")
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","title":" a "}
                                """.formatted(alpha.getId())))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/tasks")
                        .session(manager)
                        .queryParam("projectId", alpha.getId().toString())
                        .queryParam("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void taskTitleAndDescriptionHonorExactLengthBoundaries() throws Exception {
        Project alpha = projects.findByName("Matriz Demo - Alpha").orElseThrow();
        MockHttpSession manager = login("manager@matriz.local", "test-manager-password");
        String title = "T".repeat(150);
        String description = "D".repeat(2000);

        MvcResult accepted = mockMvc.perform(post("/tasks")
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","title":"%s","description":"%s"}
                                """.formatted(alpha.getId(), title, description)))
                .andExpect(status().isCreated())
                .andReturn();
        String taskId = com.jayway.jsonpath.JsonPath.read(
                accepted.getResponse().getContentAsString(), "$.id");
        assertEquals(150, ((String) com.jayway.jsonpath.JsonPath.read(
                accepted.getResponse().getContentAsString(), "$.title")).length());
        assertEquals(2000, ((String) com.jayway.jsonpath.JsonPath.read(
                accepted.getResponse().getContentAsString(), "$.description")).length());

        mockMvc.perform(delete("/tasks/{taskId}", taskId)
                        .session(manager)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/tasks")
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","title":"%s"}
                                """.formatted(alpha.getId(), "T".repeat(151))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/tasks")
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","title":"Valid title","description":"%s"}
                                """.formatted(alpha.getId(), "D".repeat(2001))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exercise8RejectsWhitespaceOnlyProjectNames() throws Exception {
        MockHttpSession manager = login("manager@matriz.local", "test-manager-password");

        mockMvc.perform(post("/projects")
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void exercise8RejectsAccessToAnotherUsersProjectEvenWithItsId() throws Exception {
        Project beta = projects.findByName("Matriz Demo - Beta").orElseThrow();
        MockHttpSession manager = login("manager@matriz.local", "test-manager-password");

        mockMvc.perform(get("/projects/{projectId}", beta.getId()).session(manager))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROJECT_ACCESS_DENIED"));

        mockMvc.perform(put("/projects/{projectId}", beta.getId())
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Unauthorized project change"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROJECT_ACCESS_DENIED"));
    }

    @Test
    void exercise5ManagerCannotReadOrUpdateAnotherUsersTaskById() throws Exception {
        Project beta = projects.findByName("Matriz Demo - Beta").orElseThrow();
        Task betaTask = tasks.findAllByProject_IdInOrderByCreatedAtDesc(java.util.List.of(beta.getId()))
                .stream().findFirst().orElseThrow();
        MockHttpSession betaOwner = login("member@matriz.local", "test-member-password");
        MockHttpSession manager = login("manager@matriz.local", "test-manager-password");
        String originalTitle = betaTask.getTitle();

        mockMvc.perform(get("/tasks/{taskId}", betaTask.getId()).session(betaOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(betaTask.getId().toString()))
                .andExpect(jsonPath("$.title").value(originalTitle));

        mockMvc.perform(get("/tasks/{taskId}", betaTask.getId()).session(manager))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROJECT_ACCESS_DENIED"));

        mockMvc.perform(put("/tasks/{taskId}", betaTask.getId())
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Unauthorized task update","status":"DONE","priority":"HIGH"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROJECT_ACCESS_DENIED"));

        assertEquals(originalTitle, tasks.findById(betaTask.getId()).orElseThrow().getTitle());
    }

    @Test
    @Transactional
    void exercise3CreatesTaskReadsItBackAndRejectsInvalidTitle() throws Exception {
        Project alpha = projects.findByName("Matriz Demo - Alpha").orElseThrow();
        MockHttpSession manager = login("manager@matriz.local", "test-manager-password");
        String title = "Persistence check " + java.util.UUID.randomUUID();
        long tasksBefore = tasks.count();

        MvcResult created = mockMvc.perform(post("/tasks")
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","title":"%s"}
                                """.formatted(alpha.getId(), title)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(alpha.getId().toString()))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andReturn();
        String taskId = com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.id");
        java.util.UUID taskUuid = java.util.UUID.fromString(taskId);

        Task persisted = tasks.findById(taskUuid).orElseThrow();
        assertEquals(title, persisted.getTitle());
        assertEquals(alpha.getId(), persisted.getProject().getId());
        assertEquals(1, tasks.count() - tasksBefore);

        mockMvc.perform(get("/tasks/{taskId}", taskId).session(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.projectId").value(alpha.getId().toString()))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"));

        mockMvc.perform(post("/tasks")
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","title":"   "}
                                """.formatted(alpha.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        assertEquals(1, tasks.count() - tasksBefore);
    }

    @Test
    void demoDataIsPresentAndInitializerDoesNotDuplicateOrDeleteIt() throws Exception {
        assertTrue(users.count() >= 2);
        assertTrue(projects.count() >= 2);
        assertTrue(tasks.count() >= 2);
        assertTrue(memberships.count() >= 3);
        assertTrue(assignments.count() >= 1);
        assertTrue(comments.count() >= 1);

        long usersBefore = users.count();
        long projectsBefore = projects.count();
        long tasksBefore = tasks.count();
        long membershipsBefore = memberships.count();
        long assignmentsBefore = assignments.count();
        long commentsBefore = comments.count();

        initializeDemoData.run(new DefaultApplicationArguments(new String[0]));

        assertEquals(usersBefore, users.count());
        assertEquals(projectsBefore, projects.count());
        assertEquals(tasksBefore, tasks.count());
        assertEquals(membershipsBefore, memberships.count());
        assertEquals(assignmentsBefore, assignments.count());
        assertEquals(commentsBefore, comments.count());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
