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
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a few demo environments/applications/deployments on an empty
 * database, so a fresh deploy (dev or prod — this is a portfolio demo app,
 * not a real internal tool) shows real-looking data instead of a blank
 * slate. Deliberately a runtime seeder, not a Flyway migration — Flyway
 * stays schema-only (ADR-0006); see ADR-0024 for the full rationale.
 *
 * Gated by the app.demo-data.enabled property, off when absent (ADR-0025):
 * enabled in application.properties, disabled in application-test.properties
 * so every Testcontainers-based test starts from an empty database
 * (ADR-0009/0010).
 */
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private static final List<String> TRIGGERED_BY_OPTIONS = List.of("admin", "ci-bot", "alice", "bob");

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

        List<ApplicationEnvironment> links = List.of(
                checkoutDev, checkoutStaging, checkoutProd,
                notificationsDev, notificationsStaging, notificationsProd);

        List<DeploymentStatus> statuses = buildDeploymentStatusSequence();
        for (int i = 0; i < statuses.size(); i++) {
            ApplicationEnvironment link = links.get(i % links.size());
            String version = "1." + i + ".0";
            String triggeredBy = TRIGGERED_BY_OPTIONS.get(i % TRIGGERED_BY_OPTIONS.size());
            seedDeployment(link, version, triggeredBy, statuses.get(i));
        }
    }

    /**
     * 50 deployments: 43 historical/terminal ones at roughly the configured
     * 85% success rate (36 SUCCESS, 7 FAILED), so the dashboard's
     * "Deployments by status" chart is well filled instead of near-empty;
     * plus a handful left non-terminal so the UI has something to demo
     * right away after a fresh deploy (5 PENDING for the "Start" button,
     * 2 RUNNING for the live-completion scheduler).
     */
    private List<DeploymentStatus> buildDeploymentStatusSequence() {
        List<DeploymentStatus> statuses = new ArrayList<>();
        for (int block = 0; block < 7; block++) {
            statuses.add(DeploymentStatus.SUCCESS);
            statuses.add(DeploymentStatus.SUCCESS);
            statuses.add(DeploymentStatus.SUCCESS);
            statuses.add(DeploymentStatus.SUCCESS);
            statuses.add(DeploymentStatus.SUCCESS);
            statuses.add(DeploymentStatus.FAILED);
        }
        statuses.add(DeploymentStatus.SUCCESS);
        for (int i = 0; i < 5; i++) {
            statuses.add(DeploymentStatus.PENDING);
        }
        for (int i = 0; i < 2; i++) {
            statuses.add(DeploymentStatus.RUNNING);
        }
        return statuses;
    }

    private void seedDeployment(ApplicationEnvironment link, String version, String triggeredBy, DeploymentStatus status) {
        Deployment deployment = new Deployment(link, version, triggeredBy);
        switch (status) {
            case PENDING -> deploymentRepository.save(deployment);
            case RUNNING -> {
                // Long simulated duration so it stays visibly RUNNING for a
                // while after a fresh deploy, instead of the scheduler
                // completing it within seconds like the terminal ones below.
                deployment.start(clock.instant(), Duration.ofMinutes(5));
                deploymentRepository.save(deployment);
            }
            case SUCCESS, FAILED -> {
                Instant now = clock.instant();
                deployment.start(now, Duration.ofSeconds(20));
                deployment.complete(status, now.plusSeconds(20));
                deploymentRepository.save(deployment);
            }
        }
    }
}
