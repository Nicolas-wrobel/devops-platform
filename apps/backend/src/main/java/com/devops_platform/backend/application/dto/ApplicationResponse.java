package com.devops_platform.backend.application.dto;

import java.time.Instant;

public record ApplicationResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
