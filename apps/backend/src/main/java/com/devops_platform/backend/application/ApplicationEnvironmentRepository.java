package com.devops_platform.backend.application;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationEnvironmentRepository extends JpaRepository<ApplicationEnvironment, Long> {

    List<ApplicationEnvironment> findByApplicationId(Long applicationId);

    Optional<ApplicationEnvironment> findByApplicationIdAndEnvironmentId(Long applicationId, Long environmentId);

    boolean existsByApplicationIdAndEnvironmentId(Long applicationId, Long environmentId);
}
