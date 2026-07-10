package com.devops_platform.backend.environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devops_platform.backend.common.ConflictException;
import com.devops_platform.backend.common.NotFoundException;
import com.devops_platform.backend.environment.dto.EnvironmentMapper;
import com.devops_platform.backend.environment.dto.EnvironmentRequest;
import com.devops_platform.backend.environment.dto.EnvironmentResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnvironmentServiceTest {

    @Mock
    private EnvironmentRepository repository;

    @Mock
    private EnvironmentMapper mapper;

    private EnvironmentService service;

    @BeforeEach
    void setUp() {
        service = new EnvironmentService(repository, mapper);
    }

    @Test
    void create_savesEnvironment_whenNameIsUnique() {
        EnvironmentRequest request = new EnvironmentRequest("production", EnvironmentType.PRODUCTION, null);
        Environment entity = new Environment("production", EnvironmentType.PRODUCTION, null);
        Environment saved = new Environment("production", EnvironmentType.PRODUCTION, null);
        EnvironmentResponse response = new EnvironmentResponse(1L, "production", EnvironmentType.PRODUCTION, null, null, null);

        when(repository.existsByName("production")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(response);

        EnvironmentResponse result = service.create(request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void create_throwsConflict_whenNameAlreadyExists() {
        EnvironmentRequest request = new EnvironmentRequest("production", EnvironmentType.PRODUCTION, null);
        when(repository.existsByName("production")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(ConflictException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void findAll_returnsMappedResponses() {
        Environment entity = new Environment("staging", EnvironmentType.STAGING, null);
        EnvironmentResponse response = new EnvironmentResponse(2L, "staging", EnvironmentType.STAGING, null, null, null);

        when(repository.findAll()).thenReturn(List.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        assertThat(service.findAll()).containsExactly(response);
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_throwsConflict_whenRenamingToAnExistingName() {
        Environment existing = new Environment("staging", EnvironmentType.STAGING, null);
        EnvironmentRequest request = new EnvironmentRequest("production", EnvironmentType.PRODUCTION, null);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByName("production")).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void delete_removesEnvironment_whenFound() {
        Environment existing = new Environment("staging", EnvironmentType.STAGING, null);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        doNothing().when(repository).delete(existing);

        service.delete(1L);

        verify(repository, times(1)).delete(existing);
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(NotFoundException.class);

        verify(repository, never()).delete(any());
    }
}
