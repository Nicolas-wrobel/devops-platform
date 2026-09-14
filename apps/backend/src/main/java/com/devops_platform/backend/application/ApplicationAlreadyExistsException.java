package com.devops_platform.backend.application;

import com.devops_platform.backend.common.ConflictException;

public class ApplicationAlreadyExistsException extends ConflictException {

    public ApplicationAlreadyExistsException(String name) {
        super("Application already exists with name " + name);
    }
}
