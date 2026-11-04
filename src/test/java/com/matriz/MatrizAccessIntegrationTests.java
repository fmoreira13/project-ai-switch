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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
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
        mockMvc.perform(get("/projects").with(httpBasic("manager@matriz.local", "test-manager-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.name == 'Matriz Demo - Alpha')]").exists())
                .andExpect(jsonPath("$.items[?(@.name == 'Matriz Demo - Beta')]").doesNotExist());

        mockMvc.perform(get("/projects").with(httpBasic("member@matriz.local", "test-member-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.name == 'Matriz Demo - Alpha')]").exists())
                .andExpect(jsonPath("$.items[?(@.name == 'Matriz Demo - Beta')]").exists());
    }

    @Test
    void projectMemberCanCommentAndUpdateTaskButCannotCreateOrAssignTasks() throws Exception {
        Project alpha = projects.findByName("Matriz Demo - Alpha").orElseThrow();
        Task task = tasks.findAllByProject_IdInOrderByCreatedAtDesc(java.util.List.of(alpha.getId()))
                .stream().findFirst().orElseThrow();

        mockMvc.perform(post("/tasks")
                        .with(httpBasic("member@matriz.local", "test-member-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","title":"Member task"}
                                """.formatted(alpha.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/tasks")
                        .with(httpBasic("member@matriz.local", "test-member-password"))
                        .queryParam("taskId", task.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"review"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("review"));

        mockMvc.perform(post("/tasks/{taskId}/comments", task.getId())
                        .with(httpBasic("member@matriz.local", "test-member-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Member comment"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Member comment"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/tasks/{taskId}/assignee", task.getId())
                        .with(httpBasic("member@matriz.local", "test-member-password"))
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

        mockMvc.perform(get("/projects/{projectId}/members", alpha.getId())
                        .with(httpBasic("manager@matriz.local", "test-manager-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'member@matriz.local')]").exists());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/tasks/{taskId}/assignee", task.getId())
                        .with(httpBasic("manager@matriz.local", "test-manager-password"))
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
                        .with(httpBasic("member@matriz.local", "test-member-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assigneeEmail":"manager@matriz.local"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().isUnauthorized());
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
}
