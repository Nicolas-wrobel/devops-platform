package com.devops_platform.backend.deployment;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentRepository extends JpaRepository<Deployment, Long> {

    boolean existsByApplicationEnvironmentIdAndStatus(Long applicationEnvironmentId, DeploymentStatus status);

    List<Deployment> findByStatusAndPlannedFinishAtBefore(DeploymentStatus status, Instant instant);

    List<Deployment> findByApplicationEnvironmentId(Long applicationEnvironmentId, Sort sort);

    long countByStatus(DeploymentStatus status);
}
