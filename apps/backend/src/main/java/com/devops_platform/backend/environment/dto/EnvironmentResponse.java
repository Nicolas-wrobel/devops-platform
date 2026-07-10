package com.devops_platform.backend.environment.dto;

import com.devops_platform.backend.environment.EnvironmentType;
import java.time.Instant;

public record EnvironmentResponse(
        Long id,
        String name,
        EnvironmentType type,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
