package com.devops_platform.backend.environment;

import com.devops_platform.backend.common.NotFoundException;

public class EnvironmentNotFoundException extends NotFoundException {

    public EnvironmentNotFoundException(Long id) {
        super("Environment not found with id " + id);
    }
}
