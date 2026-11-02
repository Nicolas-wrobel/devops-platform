package com.devops_platform.backend.deployment;

import com.devops_platform.backend.common.NotFoundException;

public class DeploymentNotFoundException extends NotFoundException {

    public DeploymentNotFoundException(Long id) {
        super("Deployment not found with id " + id);
    }
}
