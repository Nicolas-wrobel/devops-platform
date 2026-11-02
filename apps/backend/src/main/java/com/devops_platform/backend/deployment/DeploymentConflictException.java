package com.devops_platform.backend.deployment;

import com.devops_platform.backend.common.ConflictException;

public class DeploymentConflictException extends ConflictException {

    public DeploymentConflictException(String message) {
        super(message);
    }

    public static DeploymentConflictException notPending(Long id, DeploymentStatus currentStatus) {
        return new DeploymentConflictException(
                "Deployment " + id + " cannot be started because it is " + currentStatus + ", not PENDING");
    }

    public static DeploymentConflictException anotherDeploymentRunning(Long applicationEnvironmentId) {
        return new DeploymentConflictException(
                "Another deployment is already RUNNING for application/environment link "
                        + applicationEnvironmentId);
    }
}
