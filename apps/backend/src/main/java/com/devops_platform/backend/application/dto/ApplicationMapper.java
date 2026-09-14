package com.devops_platform.backend.application.dto;

import com.devops_platform.backend.application.Application;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    Application toEntity(ApplicationRequest request);

    ApplicationResponse toResponse(Application application);

    void updateEntityFromRequest(ApplicationRequest request, @MappingTarget Application application);
}
