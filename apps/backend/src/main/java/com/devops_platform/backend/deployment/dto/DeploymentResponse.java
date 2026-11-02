package com.devops_platform.backend.deployment.dto;

import com.devops_platform.backend.deployment.DeploymentStatus;
import java.time.Instant;

public record DeploymentResponse(
        Long id,
        Long applicationId,
        String applicationName,
        Long environmentId,
        String environmentName,
        String version,
        DeploymentStatus status,
        String triggeredBy,
        Instant createdAt,
        Instant startedAt,
        Instant plannedFinishAt,
        Instant finishedAt
) {
}
