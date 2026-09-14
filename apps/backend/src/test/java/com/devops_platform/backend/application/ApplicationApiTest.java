package com.devops_platform.backend.application;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devops_platform.backend.common.PostgresContainerConfig;
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
class ApplicationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void fullCrudLifecycle() throws Exception {
        String createBody = """
                {"name":"billing-service","description":"Handles invoicing"}
                """;

        String location = mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("billing-service"))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='billing-service')]").exists());

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Handles invoicing"));

        String updateBody = """
                {"name":"billing-service","description":"Handles invoicing and refunds"}
                """;

        mockMvc.perform(put(location)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Handles invoicing and refunds"));

        mockMvc.perform(delete(location)).andExpect(status().isNoContent());

        mockMvc.perform(get(location)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void create_returnsBadRequest_whenNameIsBlank() throws Exception {
        String invalidBody = """
                {"name":"","description":"Missing a name"}
                """;

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void create_returnsConflict_whenNameAlreadyExists() throws Exception {
        String body = """
                {"name":"payments-service","description":"Handles payments"}
                """;

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void getById_returnsNotFound_whenApplicationDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/applications/{id}", 999_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void create_returnsForbidden_whenCallerLacksAdminRole() throws Exception {
        String body = """
                {"name":"forbidden-app","description":"Should not be created"}
                """;

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}
