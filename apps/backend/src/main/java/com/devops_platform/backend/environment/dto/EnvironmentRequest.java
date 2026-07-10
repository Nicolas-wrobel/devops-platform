package com.devops_platform.backend.environment.dto;

import com.devops_platform.backend.environment.EnvironmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnvironmentRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        EnvironmentType type,

        @Size(max = 500)
        String description
) {
}
