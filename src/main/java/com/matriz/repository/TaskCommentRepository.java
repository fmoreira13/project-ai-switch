package com.matriz.repository;

import com.matriz.domain.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskCommentRepository extends JpaRepository<TaskComment, UUID> {
    List<TaskComment> findAllByTask_IdOrderByCreatedAtAsc(UUID taskId);
}
