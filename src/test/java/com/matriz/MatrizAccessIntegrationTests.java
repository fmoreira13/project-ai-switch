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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                                {"status":"review"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("review"));

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

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/tasks/{taskId}/assignee", task.getId())
                        .session(member)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assigneeEmail":"manager@matriz.local"}
                                """))
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

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/tasks/{taskId}/assignee", task.getId())
                        .session(manager)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assigneeEmail":"member@matriz.local"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee.email").value("member@matriz.local"));

        Project beta = projects.findByName("Matriz Demo - Beta").orElseThrow();
        Task betaTask = tasks.findAllByProject_IdInOrderByCreatedAtDesc(java.util.List.of(beta.getId()))
                .stream().findFirst().orElseThrow();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/tasks/{taskId}/assignee", betaTask.getId())
                        .session(member)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assigneeEmail":"manager@matriz.local"}
                                """))
                .andExpect(status().isForbidden());
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
