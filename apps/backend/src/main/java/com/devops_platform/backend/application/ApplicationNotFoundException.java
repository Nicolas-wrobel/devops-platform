package com.devops_platform.backend.application;

import com.devops_platform.backend.common.NotFoundException;

public class ApplicationNotFoundException extends NotFoundException {

    public ApplicationNotFoundException(Long id) {
        super("Application not found with id " + id);
    }
}
