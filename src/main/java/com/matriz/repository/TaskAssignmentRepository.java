package com.matriz.repository;

import com.matriz.domain.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, UUID> {
    Optional<TaskAssignment> findByTask_Id(UUID taskId);
}
