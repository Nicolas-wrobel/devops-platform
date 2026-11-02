package com.devops_platform.backend.deployment;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class DeploymentMetricsRegistry {

    private final MeterRegistry meterRegistry;

    public DeploymentMetricsRegistry(MeterRegistry meterRegistry, DeploymentRepository deploymentRepository) {
        this.meterRegistry = meterRegistry;

        Gauge.builder("deployments.PENDING", deploymentRepository,
                repository -> repository.countByStatus(DeploymentStatus.PENDING))
                .description("Number of deployments in PENDING status")
                .register(meterRegistry);
        Gauge.builder("deployments.RUNNING", deploymentRepository,
                repository -> repository.countByStatus(DeploymentStatus.RUNNING))
                .description("Number of deployments in RUNNING status")
                .register(meterRegistry);
        Gauge.builder("deployments.SUCCESS", deploymentRepository,
                repository -> repository.countByStatus(DeploymentStatus.SUCCESS))
                .description("Number of deployments in SUCCESS status")
                .register(meterRegistry);
        Gauge.builder("deployments.FAILED", deploymentRepository,
                repository -> repository.countByStatus(DeploymentStatus.FAILED))
                .description("Number of deployments in FAILED status")
                .register(meterRegistry);
    }

    public void recordCompletion(DeploymentStatus status, Duration duration) {
        Counter.builder("deployments.completed")
                .description("Total number of completed deployments")
                .tag("status", status.name())
                .register(meterRegistry)
                .increment();

        Timer.builder("deployments.duration")
                .description("Duration of completed deployments")
                .tag("status", status.name())
                .register(meterRegistry)
                .record(duration);
    }
}
