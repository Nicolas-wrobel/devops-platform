package com.devops_platform.backend;

import com.devops_platform.backend.application.Application;
import com.devops_platform.backend.application.ApplicationEnvironment;
import com.devops_platform.backend.application.ApplicationEnvironmentRepository;
import com.devops_platform.backend.application.ApplicationRepository;
import com.devops_platform.backend.deployment.Deployment;
import com.devops_platform.backend.deployment.DeploymentRepository;
import com.devops_platform.backend.deployment.DeploymentStatus;
import com.devops_platform.backend.environment.Environment;
import com.devops_platform.backend.environment.EnvironmentRepository;
import com.devops_platform.backend.environment.EnvironmentType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a few demo environments/applications/deployments on an empty
 * database, so a fresh deploy (dev or prod — this is a portfolio demo app,
 * not a real internal tool) shows real-looking data instead of a blank
 * slate. Deliberately a runtime seeder, not a Flyway migration — Flyway
 * stays schema-only (ADR-0006); see ADR-0024 for the full rationale.
 *
 * Disabled under the "test" profile (activated for every Maven test run
 * via the surefire plugin, see pom.xml) — every Testcontainers-based test
 * starts from a genuinely empty database on purpose (ADR-0009/0010), and
 * this runner would otherwise seed it before any test method runs.
 */
@Component
@Profile("!test")
public class DataSeeder implements ApplicationRunner {

    private static final String TRIGGERED_BY = "admin";

    private final EnvironmentRepository environmentRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationEnvironmentRepository linkRepository;
    private final DeploymentRepository deploymentRepository;
    private final Clock clock;

    public DataSeeder(
            EnvironmentRepository environmentRepository,
            ApplicationRepository applicationRepository,
            ApplicationEnvironmentRepository linkRepository,
            DeploymentRepository deploymentRepository,
            Clock clock) {
        this.environmentRepository = environmentRepository;
        this.applicationRepository = applicationRepository;
        this.linkRepository = linkRepository;
        this.deploymentRepository = deploymentRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (environmentRepository.count() > 0) {
            return;
        }

        Environment development = environmentRepository.save(
                new Environment("development", EnvironmentType.DEVELOPMENT, "Shared development environment."));
        Environment staging = environmentRepository.save(
                new Environment("staging", EnvironmentType.STAGING, "Pre-production staging environment."));
        Environment production = environmentRepository.save(
                new Environment("production", EnvironmentType.PRODUCTION, "Live production environment."));

        Application checkout = applicationRepository.save(
                new Application("checkout-service", "Handles cart checkout and payment processing."));
        Application notifications = applicationRepository.save(
                new Application("notifications-service", "Sends transactional and marketing notifications."));

        ApplicationEnvironment checkoutDev = linkRepository.save(new ApplicationEnvironment(checkout, development));
        ApplicationEnvironment checkoutStaging = linkRepository.save(new ApplicationEnvironment(checkout, staging));
        ApplicationEnvironment checkoutProd = linkRepository.save(new ApplicationEnvironment(checkout, production));
        ApplicationEnvironment notificationsDev = linkRepository.save(new ApplicationEnvironment(notifications, development));
        ApplicationEnvironment notificationsStaging = linkRepository.save(new ApplicationEnvironment(notifications, staging));
        ApplicationEnvironment notificationsProd = linkRepository.save(new ApplicationEnvironment(notifications, production));

        seedCompleted(checkoutProd, "2.4.1", DeploymentStatus.SUCCESS);
        seedCompleted(checkoutStaging, "2.5.0-rc1", DeploymentStatus.SUCCESS);
        seedCompleted(checkoutDev, "2.5.0-dev", DeploymentStatus.SUCCESS);
        seedCompleted(notificationsProd, "1.8.2", DeploymentStatus.SUCCESS);
        seedCompleted(notificationsStaging, "1.9.0-rc2", DeploymentStatus.FAILED);
        seedCompleted(checkoutStaging, "2.5.0-rc2", DeploymentStatus.FAILED);
        seedCompleted(notificationsDev, "1.9.0-dev", DeploymentStatus.SUCCESS);

        // Left PENDING on purpose, so the "Start" button has something to
        // demo right away.
        deploymentRepository.save(new Deployment(checkoutProd, "2.5.0", TRIGGERED_BY));
    }

    private void seedCompleted(ApplicationEnvironment link, String version, DeploymentStatus finalStatus) {
        Instant now = clock.instant();
        Deployment deployment = new Deployment(link, version, TRIGGERED_BY);
        deployment.start(now, Duration.ofSeconds(20));
        deployment.complete(finalStatus, now.plusSeconds(20));
        deploymentRepository.save(deployment);
    }
}
