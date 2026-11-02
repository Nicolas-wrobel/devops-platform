package com.devops_platform.backend.deployment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeploymentRequest(

        @NotNull
        Long applicationId,

        @NotNull
        Long environmentId,

        @NotBlank
        @Size(max = 50)
        String version
) {
}
