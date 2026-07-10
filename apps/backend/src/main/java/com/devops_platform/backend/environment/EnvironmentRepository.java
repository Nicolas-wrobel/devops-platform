package com.devops_platform.backend.environment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    boolean existsByName(String name);
}
