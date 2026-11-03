package com.devops_platform.backend.deployment;

import com.devops_platform.backend.application.ApplicationEnvironment;
import com.devops_platform.backend.application.ApplicationEnvironmentRepository;
import com.devops_platform.backend.deployment.dto.DeploymentMapper;
import com.devops_platform.backend.deployment.dto.DeploymentRequest;
import com.devops_platform.backend.deployment.dto.DeploymentResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeploymentService {

    private final DeploymentRepository repository;
    private final DeploymentMetricsRegistry deploymentMetricsRegistry;
    private final ApplicationEnvironmentRepository linkRepository;
    private final DeploymentMapper mapper;
    private final Clock clock;
    private final Random random;
    private final int minDurationSeconds;
    private final int maxDurationSeconds;
    private final double successProbability;

    public DeploymentService(
            DeploymentRepository repository,
            DeploymentMetricsRegistry deploymentMetricsRegistry,
            ApplicationEnvironmentRepository linkRepository,
            DeploymentMapper mapper,
            Clock clock,
            Random random,
            @Value("${deployment.simulation.min-duration-seconds:10}") int minDurationSeconds,
            @Value("${deployment.simulation.max-duration-seconds:30}") int maxDurationSeconds,
            @Value("${deployment.simulation.success-probability:0.85}") double successProbability) {
        this.repository = repository;
        this.deploymentMetricsRegistry = deploymentMetricsRegistry;
        this.linkRepository = linkRepository;
        this.mapper = mapper;
        this.clock = clock;
        this.random = random;
        this.minDurationSeconds = minDurationSeconds;
        this.maxDurationSeconds = maxDurationSeconds;
        this.successProbability = successProbability;
    }

    public DeploymentResponse create(DeploymentRequest request, String triggeredBy) {
        ApplicationEnvironment link = requireLink(request.applicationId(), request.environmentId());
        Deployment deployment = new Deployment(link, request.version(), triggeredBy);
        return mapper.toResponse(repository.save(deployment));
    }

    public DeploymentResponse start(Long id) {
        Deployment deployment = getOrThrow(id);
        if (deployment.getStatus() != DeploymentStatus.PENDING) {
            throw DeploymentConflictException.notPending(id, deployment.getStatus());
        }
        Long linkId = deployment.getApplicationEnvironment().getId();
        if (repository.existsByApplicationEnvironmentIdAndStatus(linkId, DeploymentStatus.RUNNING)) {
            throw DeploymentConflictException.anotherDeploymentRunning(linkId);
        }
        Duration simulatedDuration = Duration.ofSeconds(randomDurationSeconds());
        deployment.start(clock.instant(), simulatedDuration);
        // The check above is a fast path only: two concurrent requests can both
        // pass it. uk_deployment_one_running_per_link is the real guarantee —
        // flush now so its violation surfaces here, not at commit time.
        try {
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw DeploymentConflictException.anotherDeploymentRunning(linkId);
        }
        return mapper.toResponse(deployment);
    }

    public void completeDueDeployments() {
        List<Deployment> due = repository.findByStatusAndPlannedFinishAtBefore(DeploymentStatus.RUNNING, clock.instant());
        for (Deployment deployment : due) {
            DeploymentStatus finalStatus = random.nextDouble() < successProbability
                    ? DeploymentStatus.SUCCESS
                    : DeploymentStatus.FAILED;
            Instant finishedAt = clock.instant();
            deployment.complete(finalStatus, finishedAt);
            Duration actualDuration = Duration.between(deployment.getStartedAt(), finishedAt);
            deploymentMetricsRegistry.recordCompletion(finalStatus, actualDuration);
        }
    }

    @Transactional(readOnly = true)
    public List<DeploymentResponse> findAll(Long applicationId, Long environmentId) {
        Sort byCreatedAtDesc = Sort.by("createdAt").descending();
        List<Deployment> deployments;
        if (applicationId != null && environmentId != null) {
            ApplicationEnvironment link = requireLink(applicationId, environmentId);
            deployments = repository.findByApplicationEnvironmentId(link.getId(), byCreatedAtDesc);
        } else {
            deployments = repository.findAll(byCreatedAtDesc);
        }
        return deployments.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DeploymentResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    private int randomDurationSeconds() {
        return minDurationSeconds + random.nextInt(maxDurationSeconds - minDurationSeconds + 1);
    }

    private ApplicationEnvironment requireLink(Long applicationId, Long environmentId) {
        return linkRepository.findByApplicationIdAndEnvironmentId(applicationId, environmentId)
                .orElseThrow(() -> new ApplicationEnvironmentNotFoundException(applicationId, environmentId));
    }

    private Deployment getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new DeploymentNotFoundException(id));
    }
}
