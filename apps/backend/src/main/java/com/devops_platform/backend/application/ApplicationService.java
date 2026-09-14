package com.devops_platform.backend.application;

import com.devops_platform.backend.application.dto.ApplicationMapper;
import com.devops_platform.backend.application.dto.ApplicationRequest;
import com.devops_platform.backend.application.dto.ApplicationResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApplicationService {

    private final ApplicationRepository repository;
    private final ApplicationMapper mapper;

    public ApplicationService(ApplicationRepository repository, ApplicationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public ApplicationResponse create(ApplicationRequest request) {
        if (repository.existsByName(request.name())) {
            throw new ApplicationAlreadyExistsException(request.name());
        }
        Application application = mapper.toEntity(request);
        return mapper.toResponse(repository.save(application));
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> findAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApplicationResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    public ApplicationResponse update(Long id, ApplicationRequest request) {
        Application application = getOrThrow(id);
        if (!application.getName().equals(request.name()) && repository.existsByName(request.name())) {
            throw new ApplicationAlreadyExistsException(request.name());
        }
        mapper.updateEntityFromRequest(request, application);
        return mapper.toResponse(application);
    }

    public void delete(Long id) {
        Application application = getOrThrow(id);
        repository.delete(application);
    }

    private Application getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id));
    }
}
