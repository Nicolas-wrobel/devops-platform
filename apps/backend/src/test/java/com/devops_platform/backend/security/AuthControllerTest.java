package com.devops_platform.backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devops_platform.backend.common.PostgresContainerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresContainerConfig.class)
class AuthControllerTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Value("${APP_ADMIN_USER:admin}")
    private String adminUser;

    @Value("${APP_ADMIN_PASSWORD:adminpass}")
    private String adminPassword;

    @Test
    void login_returnsToken_whenCredentialsAreValid() throws Exception {
        String body = """
                {"username":"%s","password":"%s"}
                """.formatted(adminUser, adminPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_returnsUnauthorized_whenPasswordIsWrong() throws Exception {
        String body = """
                {"username":"%s","password":"not-the-real-password"}
                """.formatted(adminUser);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenFromLogin_grantsAccessToProtectedEndpoint() throws Exception {
        String body = """
                {"username":"%s","password":"%s"}
                """.formatted(adminUser, adminPassword);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = JSON.readTree(response).get("token").asText();

        mockMvc.perform(get("/api/environments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }
}
