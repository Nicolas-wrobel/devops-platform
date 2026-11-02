package com.devops_platform.backend.stats.dto;

public record StatsResponse(
        long applicationCount,
        long environmentCount,
        long deploymentCount,
        Double successRatePercent) {
}
