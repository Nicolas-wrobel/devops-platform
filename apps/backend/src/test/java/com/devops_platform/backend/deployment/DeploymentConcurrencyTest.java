package com.devops_platform.backend.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reproduces the check-then-act race on "one RUNNING deployment per link"
 * deterministically: transaction 1 starts deployment A without committing,
 * then transaction 2 starts deployment B on the same link — its existence
 * check can't see A (READ_COMMITTED), so only uk_deployment_one_running_per_link
 * can stop it. Not @Transactional: both transactions must really commit.
 */
@SpringBootTest
@Import(PostgresContainerConfig.class)
class DeploymentConcurrencyTest {

    private static final long LOCK_WAIT_TIMEOUT_MS = 10_000;

    @Autowired
    private DeploymentService service;

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private ApplicationEnvironmentRepository linkRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Long deploymentA;
    private Long deploymentB;

    @BeforeEach
    void setUp() {
        Application application = applicationRepository.save(new Application("race-app", null));
        Environment environment = environmentRepository.save(
                new Environment("race-env", EnvironmentType.PRODUCTION, null));
        ApplicationEnvironment link = linkRepository.save(new ApplicationEnvironment(application, environment));
        deploymentA = deploymentRepository.save(new Deployment(link, "1.0.0", "alice")).getId();
        deploymentB = deploymentRepository.save(new Deployment(link, "1.0.1", "bob")).getId();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        // Committed data would leak into other test classes sharing the container.
        deploymentRepository.deleteAll();
        linkRepository.deleteAll();
        applicationRepository.deleteAll();
        environmentRepository.deleteAll();
    }

    @Test
    void concurrentStartsOnTheSameLink_onlyOneWins() throws Exception {
        Future<?> secondStart = new TransactionTemplate(transactionManager).execute(status -> {
            service.start(deploymentA);

            Future<?> concurrent = executor.submit(() -> service.start(deploymentB));
            awaitATransactionBlockedOnALock();
            return concurrent;
        });

        assertThatThrownBy(() -> secondStart.get(LOCK_WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(DeploymentConflictException.class);
        assertThat(deploymentRepository.findById(deploymentA).orElseThrow().getStatus())
                .isEqualTo(DeploymentStatus.RUNNING);
        assertThat(deploymentRepository.findById(deploymentB).orElseThrow().getStatus())
                .isEqualTo(DeploymentStatus.PENDING);
    }

    private void awaitATransactionBlockedOnALock() {
        Instant deadline = Instant.now().plus(Duration.ofMillis(LOCK_WAIT_TIMEOUT_MS));
        while (Instant.now().isBefore(deadline)) {
            Integer blocked = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_stat_activity "
                            + "WHERE datname = current_database() AND wait_event_type = 'Lock'",
                    Integer.class);
            if (blocked != null && blocked > 0) {
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new AssertionError("The concurrent start never blocked on the unique index — "
                + "is uk_deployment_one_running_per_link missing?");
    }
}
