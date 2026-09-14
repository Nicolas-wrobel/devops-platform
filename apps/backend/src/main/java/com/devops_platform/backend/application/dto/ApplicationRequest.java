package com.devops_platform.backend.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplicationRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description
) {
}
