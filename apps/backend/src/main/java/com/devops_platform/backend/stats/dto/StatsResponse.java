package com.devops_platform.backend.stats.dto;

public record StatsResponse(
        long applicationCount,
        long environmentCount,
        long deploymentCount,
        Double successRatePercent,
        long pendingCount,
        long runningCount,
        long successCount,
        long failedCount) {
}
