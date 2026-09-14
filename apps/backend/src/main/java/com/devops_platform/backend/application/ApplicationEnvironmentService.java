package com.devops_platform.backend.application;

import com.devops_platform.backend.environment.Environment;
import com.devops_platform.backend.environment.EnvironmentNotFoundException;
import com.devops_platform.backend.environment.EnvironmentRepository;
import com.devops_platform.backend.environment.dto.EnvironmentMapper;
import com.devops_platform.backend.environment.dto.EnvironmentResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApplicationEnvironmentService {

    private final ApplicationEnvironmentRepository linkRepository;
    private final ApplicationRepository applicationRepository;
    private final EnvironmentRepository environmentRepository;
    private final EnvironmentMapper environmentMapper;

    public ApplicationEnvironmentService(
            ApplicationEnvironmentRepository linkRepository,
            ApplicationRepository applicationRepository,
            EnvironmentRepository environmentRepository,
            EnvironmentMapper environmentMapper) {
        this.linkRepository = linkRepository;
        this.applicationRepository = applicationRepository;
        this.environmentRepository = environmentRepository;
        this.environmentMapper = environmentMapper;
    }

    @Transactional(readOnly = true)
    public List<EnvironmentResponse> findEnvironmentsForApplication(Long applicationId) {
        requireApplication(applicationId);
        return linkRepository.findByApplicationId(applicationId).stream()
                .map(link -> environmentMapper.toResponse(link.getEnvironment()))
                .toList();
    }

    public EnvironmentResponse link(Long applicationId, Long environmentId) {
        Application application = requireApplication(applicationId);
        Environment environment = requireEnvironment(environmentId);
        if (linkRepository.existsByApplicationIdAndEnvironmentId(applicationId, environmentId)) {
            throw new ApplicationEnvironmentAlreadyExistsException(applicationId, environmentId);
        }
        linkRepository.save(new ApplicationEnvironment(application, environment));
        return environmentMapper.toResponse(environment);
    }

    public void unlink(Long applicationId, Long environmentId) {
        ApplicationEnvironment link = linkRepository
                .findByApplicationIdAndEnvironmentId(applicationId, environmentId)
                .orElseThrow(() -> new EnvironmentNotFoundException(environmentId));
        linkRepository.delete(link);
    }

    private Application requireApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
    }

    private Environment requireEnvironment(Long environmentId) {
        return environmentRepository.findById(environmentId)
                .orElseThrow(() -> new EnvironmentNotFoundException(environmentId));
    }
}
