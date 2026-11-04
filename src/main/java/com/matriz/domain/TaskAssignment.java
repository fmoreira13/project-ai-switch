package com.matriz.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_assignments")
public class TaskAssignment {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id", nullable = false, unique = true)
    private Task task;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assignee_id", nullable = false)
    private AppUser assignee;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private AppUser assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    protected TaskAssignment() {
    }

    public TaskAssignment(Task task, AppUser assignee, AppUser assignedBy) {
        this.id = UUID.randomUUID();
        this.task = task;
        this.assignee = assignee;
        this.assignedBy = assignedBy;
        this.assignedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Task getTask() {
        return task;
    }

    public AppUser getAssignee() {
        return assignee;
    }

    public AppUser getAssignedBy() {
        return assignedBy;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void reassign(AppUser assignee, AppUser assignedBy) {
        this.assignee = assignee;
        this.assignedBy = assignedBy;
        this.assignedAt = Instant.now();
    }
}
