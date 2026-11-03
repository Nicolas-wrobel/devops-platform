package com.devops_platform.backend.stats;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devops_platform.backend.common.PostgresContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresContainerConfig.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getStats_isReachable_withNoAuthentication() throws Exception {
        mockMvc.perform(get("/api/public/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationCount").isNumber())
                .andExpect(jsonPath("$.environmentCount").isNumber())
                .andExpect(jsonPath("$.deploymentCount").isNumber())
                .andExpect(jsonPath("$.pendingCount").isNumber())
                .andExpect(jsonPath("$.runningCount").isNumber())
                .andExpect(jsonPath("$.successCount").isNumber())
                .andExpect(jsonPath("$.failedCount").isNumber());
    }
}
