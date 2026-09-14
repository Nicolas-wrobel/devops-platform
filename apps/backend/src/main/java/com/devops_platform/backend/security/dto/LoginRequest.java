package com.devops_platform.backend.security.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank
        String username,
        
        @NotBlank
        String password
) {
}
