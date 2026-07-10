package com.devops_platform.backend.environment.dto;

import com.devops_platform.backend.environment.Environment;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EnvironmentMapper {

    Environment toEntity(EnvironmentRequest request);

    EnvironmentResponse toResponse(Environment environment);

    void updateEntityFromRequest(EnvironmentRequest request, @MappingTarget Environment environment);
}
