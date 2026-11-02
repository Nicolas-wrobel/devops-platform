package com.devops_platform.backend.deployment;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component 
public class DeploymentCompletionScheduler {
    private final DeploymentService service;

    public DeploymentCompletionScheduler(DeploymentService service) {
        this.service = service;
    }

    @Scheduled(fixedDelay = 10000)
    public void completeDueDeployments() {
        service.completeDueDeployments();
    }
}
