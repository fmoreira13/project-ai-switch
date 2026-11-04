package com.matriz.repository;

import com.matriz.domain.ProjectMembership;
import com.matriz.domain.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMembershipRepository extends JpaRepository<ProjectMembership, UUID> {
    List<ProjectMembership> findAllByUser_Id(UUID userId);

    List<ProjectMembership> findAllByProject_Id(UUID projectId);

    Optional<ProjectMembership> findByProject_IdAndUser_Id(UUID projectId, UUID userId);

    boolean existsByProject_IdAndUser_IdAndRole(UUID projectId, UUID userId, ProjectRole role);
}
