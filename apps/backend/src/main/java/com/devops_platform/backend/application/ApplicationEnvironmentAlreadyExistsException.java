package com.devops_platform.backend.application;

import com.devops_platform.backend.common.ConflictException;

public class ApplicationEnvironmentAlreadyExistsException extends ConflictException {

    public ApplicationEnvironmentAlreadyExistsException(Long applicationId, Long environmentId) {
        super("Application " + applicationId + " is already linked to environment " + environmentId);
    }
}
