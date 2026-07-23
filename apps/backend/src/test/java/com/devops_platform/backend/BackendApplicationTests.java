package com.devops_platform.backend;

import com.devops_platform.backend.common.PostgresContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresContainerConfig.class)
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
