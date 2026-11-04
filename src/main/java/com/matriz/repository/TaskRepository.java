package com.matriz.repository;

import com.matriz.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findAllByProject_IdInOrderByCreatedAtDesc(Collection<UUID> projectIds);

    long countByProject_Id(UUID projectId);
}
