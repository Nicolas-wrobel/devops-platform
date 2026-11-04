package com.devops_platform.backend.deployment;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

public interface DeploymentRepository extends JpaRepository<Deployment, Long> {

    boolean existsByApplicationEnvironmentIdAndStatus(Long applicationEnvironmentId, DeploymentStatus status);

    // SELECT ... FOR UPDATE SKIP LOCKED (lock timeout -2 = Hibernate's SKIP_LOCKED):
    // with several replicas running the completion job, each row is claimed by
    // exactly one of them, and the others skip it instead of waiting. The locks
    // only last as long as the caller's transaction — see ADR-0027.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<Deployment> findByStatusAndPlannedFinishAtBefore(DeploymentStatus status, Instant instant);

    List<Deployment> findByApplicationEnvironmentId(Long applicationEnvironmentId, Sort sort);

    long countByStatus(DeploymentStatus status);
}
