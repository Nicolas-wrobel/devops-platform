package com.devops_platform.backend.application;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devops_platform.backend.common.PostgresContainerConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresContainerConfig.class)
class ApplicationEnvironmentApiTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void linkAndUnlinkEnvironmentLifecycle() throws Exception {
        long applicationId = createApplication("checkout-service");
        long environmentId = createEnvironment("checkout-prod");

        mockMvc.perform(post("/api/applications/{id}/environments/{envId}", applicationId, environmentId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("checkout-prod"));

        mockMvc.perform(get("/api/applications/{id}/environments", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='checkout-prod')]").exists());

        mockMvc.perform(delete("/api/applications/{id}/environments/{envId}", applicationId, environmentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/applications/{id}/environments", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void link_returnsConflict_whenAlreadyLinked() throws Exception {
        long applicationId = createApplication("invoicing-service");
        long environmentId = createEnvironment("invoicing-staging");

        mockMvc.perform(post("/api/applications/{id}/environments/{envId}", applicationId, environmentId))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/applications/{id}/environments/{envId}", applicationId, environmentId))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void link_returnsNotFound_whenEnvironmentDoesNotExist() throws Exception {
        long applicationId = createApplication("orphan-service");

        mockMvc.perform(post("/api/applications/{id}/environments/{envId}", applicationId, 999_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void link_returnsForbidden_whenCallerLacksAdminRole() throws Exception {
        mockMvc.perform(post("/api/applications/{id}/environments/{envId}", 1L, 1L))
                .andExpect(status().isForbidden());
    }

    private long createApplication(String name) throws Exception {
        String body = """
                {"name":"%s","description":"Test application"}
                """.formatted(name);

        String response = mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return idFrom(response);
    }

    private long createEnvironment(String name) throws Exception {
        String body = """
                {"name":"%s","type":"STAGING"}
                """.formatted(name);

        String response = mockMvc.perform(post("/api/environments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return idFrom(response);
    }

    private long idFrom(String jsonResponse) throws Exception {
        JsonNode node = JSON.readTree(jsonResponse);
        return node.get("id").asLong();
    }
}
