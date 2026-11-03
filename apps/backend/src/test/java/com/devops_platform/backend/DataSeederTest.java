package com.devops_platform.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.devops_platform.backend.application.ApplicationEnvironmentRepository;
import com.devops_platform.backend.application.ApplicationRepository;
import com.devops_platform.backend.common.PostgresContainerConfig;
import com.devops_platform.backend.deployment.DeploymentRepository;
import com.devops_platform.backend.environment.EnvironmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

// DataSeeder is disabled under the "test" profile (active for every other
// test, see pom.xml's surefire config) precisely so it doesn't seed data
// into every other Testcontainers-based test's empty database. This is
// the one test that needs it actually enabled, so it opts out of that
// profile instead.
@SpringBootTest
@Import(PostgresContainerConfig.class)
@ActiveProfiles("data-seeder-test")
class DataSeederTest {

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationEnvironmentRepository linkRepository;

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private DataSeeder seeder;

    @Test
    void run_seedsDemoData_onAnEmptyDatabase() {
        assertThat(environmentRepository.count()).isEqualTo(3);
        assertThat(applicationRepository.count()).isEqualTo(2);
        assertThat(linkRepository.count()).isEqualTo(6);
        assertThat(deploymentRepository.count()).isEqualTo(50);
    }

    @Test
    void run_doesNothing_whenDataAlreadyExists() {
        long environmentCountBefore = environmentRepository.count();

        seeder.run(null);

        assertThat(environmentRepository.count()).isEqualTo(environmentCountBefore);
    }
}
