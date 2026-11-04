package com.matriz.repository;

import com.matriz.domain.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, UUID> {
    List<TaskAssignment> findAllByTask_IdOrderByAssignedAtAsc(UUID taskId);

    Optional<TaskAssignment> findByTask_IdAndAssignee_Id(UUID taskId, UUID assigneeId);

    Optional<TaskAssignment> findByIdAndTask_Id(UUID assignmentId, UUID taskId);
}
