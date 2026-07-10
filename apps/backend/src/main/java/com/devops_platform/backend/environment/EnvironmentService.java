package com.devops_platform.backend.environment;

import com.devops_platform.backend.environment.dto.EnvironmentMapper;
import com.devops_platform.backend.environment.dto.EnvironmentRequest;
import com.devops_platform.backend.environment.dto.EnvironmentResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EnvironmentService {

    private final EnvironmentRepository repository;
    private final EnvironmentMapper mapper;

    public EnvironmentService(EnvironmentRepository repository, EnvironmentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public EnvironmentResponse create(EnvironmentRequest request) {
        if (repository.existsByName(request.name())) {
            throw new EnvironmentAlreadyExistsException(request.name());
        }
        Environment environment = mapper.toEntity(request);
        return mapper.toResponse(repository.save(environment));
    }

    @Transactional(readOnly = true)
    public List<EnvironmentResponse> findAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EnvironmentResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    public EnvironmentResponse update(Long id, EnvironmentRequest request) {
        Environment environment = getOrThrow(id);
        if (!environment.getName().equals(request.name()) && repository.existsByName(request.name())) {
            throw new EnvironmentAlreadyExistsException(request.name());
        }
        mapper.updateEntityFromRequest(request, environment);
        return mapper.toResponse(environment);
    }

    public void delete(Long id) {
        Environment environment = getOrThrow(id);
        repository.delete(environment);
    }

    private Environment getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new EnvironmentNotFoundException(id));
    }
}
