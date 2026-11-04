package com.matriz.config;

import com.matriz.domain.AppUser;
import com.matriz.domain.Project;
import com.matriz.domain.ProjectMembership;
import com.matriz.domain.ProjectRole;
import com.matriz.domain.Task;
import com.matriz.domain.TaskAssignment;
import com.matriz.domain.TaskComment;
import com.matriz.domain.TaskStatus;
import com.matriz.repository.ProjectMembershipRepository;
import com.matriz.repository.ProjectRepository;
import com.matriz.repository.TaskAssignmentRepository;
import com.matriz.repository.TaskCommentRepository;
import com.matriz.repository.TaskRepository;
import com.matriz.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoDataInitializer {
    @Bean
    ApplicationRunner initializeDemoData(
            UserRepository users,
            ProjectRepository projects,
            ProjectMembershipRepository memberships,
            TaskRepository tasks,
            TaskAssignmentRepository assignments,
            TaskCommentRepository comments,
            PasswordEncoder passwordEncoder,
            @Value("${matriz.demo.manager-password}") String managerPassword,
            @Value("${matriz.demo.member-password}") String memberPassword) {
        return args -> {
            AppUser manager = users.findByEmailIgnoreCase("manager@matriz.local")
                    .orElseGet(() -> users.save(new AppUser(
                            "manager@matriz.local", "Demo Manager", passwordEncoder.encode(managerPassword))));
            AppUser member = users.findByEmailIgnoreCase("member@matriz.local")
                    .orElseGet(() -> users.save(new AppUser(
                            "member@matriz.local", "Demo Member", passwordEncoder.encode(memberPassword))));

            Project alpha = projects.findByName("Matriz Demo - Alpha")
                    .orElseGet(() -> projects.save(new Project(
                            "Matriz Demo - Alpha", "Demo project managed by the manager account.")));
            Project beta = projects.findByName("Matriz Demo - Beta")
                    .orElseGet(() -> projects.save(new Project(
                            "Matriz Demo - Beta", "Demo project managed by the member account.")));

            ensureMembership(memberships, alpha, manager, ProjectRole.MANAGER);
            ensureMembership(memberships, alpha, member, ProjectRole.MEMBER);
            ensureMembership(memberships, beta, member, ProjectRole.MANAGER);

            Task alphaTask = tasks.findAll().stream()
                    .filter(task -> task.getProject().getId().equals(alpha.getId()))
                    .findFirst()
                    .orElseGet(() -> tasks.save(new Task(
                            alpha, "Prepare MVP demo", "Seed task for access and assignment checks.", TaskStatus.in_progress)));
            Task betaTask = tasks.findAll().stream()
                    .filter(task -> task.getProject().getId().equals(beta.getId()))
                    .findFirst()
                    .orElseGet(() -> tasks.save(new Task(
                            beta, "Review project setup", "Seed task for project-scoped visibility checks.", TaskStatus.backlog)));

            if (assignments.findByTask_Id(alphaTask.getId()).isEmpty()) {
                assignments.save(new TaskAssignment(alphaTask, member, manager));
            }
            if (comments.findAllByTask_IdOrderByCreatedAtAsc(alphaTask.getId()).isEmpty()) {
                comments.save(new TaskComment(alphaTask, manager, "Demo comment for the MVP."));
            }
        };
    }

    private void ensureMembership(
            ProjectMembershipRepository memberships,
            Project project,
            AppUser user,
            ProjectRole role) {
        if (memberships.findByProject_IdAndUser_Id(project.getId(), user.getId()).isEmpty()) {
            memberships.save(new ProjectMembership(project, user, role));
        }
    }
}
