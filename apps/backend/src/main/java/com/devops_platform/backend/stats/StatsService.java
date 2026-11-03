package com.devops_platform.backend.stats;

import com.devops_platform.backend.application.ApplicationRepository;
import com.devops_platform.backend.deployment.DeploymentRepository;
import com.devops_platform.backend.deployment.DeploymentStatus;
import com.devops_platform.backend.environment.EnvironmentRepository;
import com.devops_platform.backend.stats.dto.StatsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final ApplicationRepository applicationRepository;
    private final EnvironmentRepository environmentRepository;
    private final DeploymentRepository deploymentRepository;

    public StatsService(
            ApplicationRepository applicationRepository,
            EnvironmentRepository environmentRepository,
            DeploymentRepository deploymentRepository) {
        this.applicationRepository = applicationRepository;
        this.environmentRepository = environmentRepository;
        this.deploymentRepository = deploymentRepository;
    }

    public StatsResponse getStats() {
        long pendingCount = deploymentRepository.countByStatus(DeploymentStatus.PENDING);
        long runningCount = deploymentRepository.countByStatus(DeploymentStatus.RUNNING);
        long successCount = deploymentRepository.countByStatus(DeploymentStatus.SUCCESS);
        long failedCount = deploymentRepository.countByStatus(DeploymentStatus.FAILED);
        long completedCount = successCount + failedCount;

        Double successRatePercent = completedCount == 0
                ? null
                : 100.0 * successCount / completedCount;

        return new StatsResponse(
                applicationRepository.count(),
                environmentRepository.count(),
                deploymentRepository.count(),
                successRatePercent,
                pendingCount,
                runningCount,
                successCount,
                failedCount);
    }
}
