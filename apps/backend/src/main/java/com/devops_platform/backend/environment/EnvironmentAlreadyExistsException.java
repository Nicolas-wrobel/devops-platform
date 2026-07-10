package com.devops_platform.backend.environment;

import com.devops_platform.backend.common.ConflictException;

public class EnvironmentAlreadyExistsException extends ConflictException {

    public EnvironmentAlreadyExistsException(String name) {
        super("Environment already exists with name " + name);
    }
}
