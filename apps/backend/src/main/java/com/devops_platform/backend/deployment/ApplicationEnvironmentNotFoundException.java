package com.devops_platform.backend.deployment;

import com.devops_platform.backend.common.NotFoundException;

public class ApplicationEnvironmentNotFoundException extends NotFoundException {

    public ApplicationEnvironmentNotFoundException(Long applicationId, Long environmentId) {
        super("Application " + applicationId + " is not linked to environment " + environmentId);
    }
}
