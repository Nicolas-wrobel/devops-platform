package com.devops_platform.backend.environment;

import static org.assertj.core.api.Assertions.assertThat;

import com.devops_platform.backend.common.PostgresContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresContainerConfig.class)
class EnvironmentRepositoryTest {

    @Autowired
    private EnvironmentRepository repository;

    @Test
    void existsByName_reflectsPersistedEnvironments() {
        repository.save(new Environment("production", EnvironmentType.PRODUCTION, "Main production environment"));

        assertThat(repository.existsByName("production")).isTrue();
        assertThat(repository.existsByName("staging")).isFalse();
    }

    @Test
    void save_persistsTimestampsAndGeneratesId() {
        Environment saved = repository.save(new Environment("staging", EnvironmentType.STAGING, null));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
