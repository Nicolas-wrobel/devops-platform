package com.devops_platform.backend.deployment.dto;

import com.devops_platform.backend.deployment.Deployment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DeploymentMapper {

    @Mapping(source = "applicationEnvironment.application.id", target = "applicationId")
    @Mapping(source = "applicationEnvironment.application.name", target = "applicationName")
    @Mapping(source = "applicationEnvironment.environment.id", target = "environmentId")
    @Mapping(source = "applicationEnvironment.environment.name", target = "environmentName")
    DeploymentResponse toResponse(Deployment deployment);
}
