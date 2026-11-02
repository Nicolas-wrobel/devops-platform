package com.devops_platform.backend.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devops_platform.backend.application.Application;
import com.devops_platform.backend.application.ApplicationEnvironment;
import com.devops_platform.backend.application.ApplicationEnvironmentRepository;
import com.devops_platform.backend.common.ConflictException;
import com.devops_platform.backend.common.NotFoundException;
import com.devops_platform.backend.deployment.dto.DeploymentMapper;
import com.devops_platform.backend.deployment.dto.DeploymentRequest;
import com.devops_platform.backend.deployment.dto.DeploymentResponse;
import com.devops_platform.backend.environment.Environment;
import com.devops_platform.backend.environment.EnvironmentType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeploymentServiceTest {

    @Mock
    private DeploymentRepository repository;

    @Mock
    private ApplicationEnvironmentRepository linkRepository;

    @Mock
    private DeploymentMapper mapper;

    @Mock
    private Random random;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private DeploymentService service;

    private ApplicationEnvironment link;

    @BeforeEach
    void setUp() {
        service = new DeploymentService(repository, linkRepository, mapper, fixedClock, random, 10, 30, 0.85);
        Application application = new Application("checkout", null);
        Environment environment = new Environment("production", EnvironmentType.PRODUCTION, null);
        link = new ApplicationEnvironment(application, environment);
    }

    @Test
    void create_savesDeployment_whenLinkExists() {
        DeploymentRequest request = new DeploymentRequest(1L, 2L, "1.0.0");
        DeploymentResponse response = new DeploymentResponse(
                1L, 1L, "checkout", 2L, "production", "1.0.0", DeploymentStatus.PENDING, "alice",
                null, null, null, null);

        when(linkRepository.findByApplicationIdAndEnvironmentId(1L, 2L)).thenReturn(Optional.of(link));
        when(repository.save(any(Deployment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Deployment.class))).thenReturn(response);

        DeploymentResponse result = service.create(request, "alice");

        assertThat(result).isEqualTo(response);
    }

    @Test
    void create_throwsNotFound_whenApplicationNotLinkedToEnvironment() {
        DeploymentRequest request = new DeploymentRequest(1L, 2L, "1.0.0");
        when(linkRepository.findByApplicationIdAndEnvironmentId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request, "alice")).isInstanceOf(NotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void start_transitionsToRunning_whenPending() {
        Deployment deployment = new Deployment(link, "1.0.0", "alice");
        setId(link, 42L);
        when(repository.findById(1L)).thenReturn(Optional.of(deployment));
        when(repository.existsByApplicationEnvironmentIdAndStatus(42L, DeploymentStatus.RUNNING)).thenReturn(false);
        when(random.nextInt(21)).thenReturn(5);
        when(mapper.toResponse(deployment)).thenReturn(mockResponse());

        service.start(1L);

        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.RUNNING);
        assertThat(deployment.getStartedAt()).isEqualTo(fixedClock.instant());
        assertThat(deployment.getPlannedFinishAt()).isEqualTo(fixedClock.instant().plusSeconds(15));
    }

    @Test
    void start_throwsConflict_whenNotPending() {
        Deployment deployment = new Deployment(link, "1.0.0", "alice");
        setId(link, 42L);
        deployment.start(fixedClock.instant(), java.time.Duration.ofSeconds(10));
        when(repository.findById(1L)).thenReturn(Optional.of(deployment));

        assertThatThrownBy(() -> service.start(1L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void start_throwsConflict_whenAnotherDeploymentIsAlreadyRunning() {
        Deployment deployment = new Deployment(link, "1.0.0", "alice");
        setId(link, 42L);
        when(repository.findById(1L)).thenReturn(Optional.of(deployment));
        when(repository.existsByApplicationEnvironmentIdAndStatus(42L, DeploymentStatus.RUNNING)).thenReturn(true);

        assertThatThrownBy(() -> service.start(1L)).isInstanceOf(ConflictException.class);

        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.PENDING);
    }

    @Test
    void completeDueDeployments_transitionsToSuccess_whenRandomDrawIsBelowSuccessProbability() {
        Deployment running = new Deployment(link, "1.0.0", "alice");
        running.start(fixedClock.instant().minusSeconds(60), java.time.Duration.ofSeconds(10));
        when(repository.findByStatusAndPlannedFinishAtBefore(DeploymentStatus.RUNNING, fixedClock.instant()))
                .thenReturn(List.of(running));
        when(random.nextDouble()).thenReturn(0.5);

        service.completeDueDeployments();

        assertThat(running.getStatus()).isEqualTo(DeploymentStatus.SUCCESS);
        assertThat(running.getFinishedAt()).isEqualTo(fixedClock.instant());
    }

    @Test
    void completeDueDeployments_transitionsToFailed_whenRandomDrawIsAboveSuccessProbability() {
        Deployment running = new Deployment(link, "1.0.0", "alice");
        running.start(fixedClock.instant().minusSeconds(60), java.time.Duration.ofSeconds(10));
        when(repository.findByStatusAndPlannedFinishAtBefore(DeploymentStatus.RUNNING, fixedClock.instant()))
                .thenReturn(List.of(running));
        when(random.nextDouble()).thenReturn(0.99);

        service.completeDueDeployments();

        assertThat(running.getStatus()).isEqualTo(DeploymentStatus.FAILED);
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void findAll_resolvesLink_whenApplicationAndEnvironmentProvided() {
        setId(link, 42L);
        when(linkRepository.findByApplicationIdAndEnvironmentId(1L, 2L)).thenReturn(Optional.of(link));
        when(repository.findByApplicationEnvironmentId(42L, org.springframework.data.domain.Sort.by("createdAt").descending()))
                .thenReturn(List.of());

        assertThat(service.findAll(1L, 2L)).isEmpty();
    }

    private DeploymentResponse mockResponse() {
        return new DeploymentResponse(1L, 1L, "checkout", 2L, "production", "1.0.0", DeploymentStatus.RUNNING,
                "alice", null, null, null, null);
    }

    private void setId(ApplicationEnvironment link, Long id) {
        try {
            var field = ApplicationEnvironment.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(link, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
