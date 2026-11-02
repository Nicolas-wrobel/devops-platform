package com.devops_platform.backend.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import com.devops_platform.backend.application.Application;
import com.devops_platform.backend.application.ApplicationEnvironment;
import com.devops_platform.backend.application.ApplicationEnvironmentRepository;
import com.devops_platform.backend.application.ApplicationRepository;
import com.devops_platform.backend.common.PostgresContainerConfig;
import com.devops_platform.backend.environment.Environment;
import com.devops_platform.backend.environment.EnvironmentRepository;
import com.devops_platform.backend.environment.EnvironmentType;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresContainerConfig.class)
class DeploymentRepositoryTest {

    @Autowired
    private DeploymentRepository repository;

    @Autowired
    private ApplicationEnvironmentRepository linkRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EnvironmentRepository environmentRepository;

    private ApplicationEnvironment link;

    @BeforeEach
    void setUp() {
        Application application = applicationRepository.save(new Application("checkout", null));
        Environment environment = environmentRepository.save(
                new Environment("production", EnvironmentType.PRODUCTION, null));
        link = linkRepository.save(new ApplicationEnvironment(application, environment));
    }

    @Test
    void existsByApplicationEnvironmentIdAndStatus_reflectsPersistedDeployments() {
        Deployment running = new Deployment(link, "1.0.0", "alice");
        running.start(Instant.now(), Duration.ofSeconds(10));
        repository.save(running);

        assertThat(repository.existsByApplicationEnvironmentIdAndStatus(link.getId(), DeploymentStatus.RUNNING))
                .isTrue();
        assertThat(repository.existsByApplicationEnvironmentIdAndStatus(link.getId(), DeploymentStatus.FAILED))
                .isFalse();
    }

    @Test
    void findByStatusAndPlannedFinishAtBefore_returnsOnlyDueRunningDeployments() {
        Deployment due = new Deployment(link, "1.0.0", "alice");
        due.start(Instant.now().minusSeconds(60), Duration.ofSeconds(10));
        repository.save(due);

        Deployment notYetDue = new Deployment(link, "1.0.1", "alice");
        notYetDue.start(Instant.now(), Duration.ofSeconds(600));
        repository.save(notYetDue);

        assertThat(repository.findByStatusAndPlannedFinishAtBefore(DeploymentStatus.RUNNING, Instant.now()))
                .extracting(Deployment::getVersion)
                .containsExactly("1.0.0");
    }

    @Test
    void findByApplicationEnvironmentId_ordersByCreatedAtDescending() {
        repository.save(new Deployment(link, "1.0.0", "alice"));
        repository.save(new Deployment(link, "1.0.1", "alice"));

        assertThat(repository.findByApplicationEnvironmentId(link.getId(), Sort.by("createdAt").descending()))
                .extracting(Deployment::getVersion)
                .containsExactly("1.0.1", "1.0.0");
    }

    @Test
    void countByStatus_countsOnlyMatchingDeployments() {
        repository.save(new Deployment(link, "1.0.0", "alice"));
        Deployment running = new Deployment(link, "1.0.1", "alice");
        running.start(Instant.now(), Duration.ofSeconds(10));
        repository.save(running);

        assertThat(repository.countByStatus(DeploymentStatus.PENDING)).isEqualTo(1);
        assertThat(repository.countByStatus(DeploymentStatus.RUNNING)).isEqualTo(1);
        assertThat(repository.countByStatus(DeploymentStatus.FAILED)).isZero();
    }
}
