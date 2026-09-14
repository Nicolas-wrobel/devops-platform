package com.devops_platform.backend.application;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByName(String name);
}
