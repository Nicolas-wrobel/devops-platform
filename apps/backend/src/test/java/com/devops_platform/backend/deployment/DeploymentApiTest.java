package com.devops_platform.backend.deployment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devops_platform.backend.application.Application;
import com.devops_platform.backend.application.ApplicationEnvironment;
import com.devops_platform.backend.application.ApplicationEnvironmentRepository;
import com.devops_platform.backend.application.ApplicationRepository;
import com.devops_platform.backend.common.PostgresContainerConfig;
import com.devops_platform.backend.environment.Environment;
import com.devops_platform.backend.environment.EnvironmentRepository;
import com.devops_platform.backend.environment.EnvironmentType;
import org.junit.jupiter.api.BeforeEach;
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
class DeploymentApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private ApplicationEnvironmentRepository linkRepository;

    private Long applicationId;
    private Long environmentId;

    @BeforeEach
    void setUp() {
        Application application = applicationRepository.save(new Application("checkout-" + System.nanoTime(), null));
        Environment environment = environmentRepository.save(
                new Environment("production-" + System.nanoTime(), EnvironmentType.PRODUCTION, null));
        linkRepository.save(new ApplicationEnvironment(application, environment));
        applicationId = application.getId();
        environmentId = environment.getId();
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void createThenStart_movesDeploymentFromPendingToRunning() throws Exception {
        String createBody = """
                {"applicationId":%d,"environmentId":%d,"version":"1.0.0"}
                """.formatted(applicationId, environmentId);

        String location = mockMvc.perform(post("/api/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(post(location + "/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));

        mockMvc.perform(post(location + "/start")).andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void start_returnsConflict_whenAnotherDeploymentAlreadyRunningForSameLink() throws Exception {
        String createBody = """
                {"applicationId":%d,"environmentId":%d,"version":"1.0.0"}
                """.formatted(applicationId, environmentId);

        String firstLocation = mockMvc.perform(post("/api/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getHeader("Location");
        mockMvc.perform(post(firstLocation + "/start")).andExpect(status().isOk());

        String secondLocation = mockMvc.perform(post("/api/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody.replace("1.0.0", "1.0.1")))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(post(secondLocation + "/start")).andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void create_returnsNotFound_whenApplicationIsNotLinkedToEnvironment() throws Exception {
        Application unlinkedApplication = applicationRepository.save(new Application("standalone-" + System.nanoTime(), null));
        String body = """
                {"applicationId":%d,"environmentId":%d,"version":"1.0.0"}
                """.formatted(unlinkedApplication.getId(), environmentId);

        mockMvc.perform(post("/api/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void create_returnsBadRequest_whenVersionIsBlank() throws Exception {
        String body = """
                {"applicationId":%d,"environmentId":%d,"version":""}
                """.formatted(applicationId, environmentId);

        mockMvc.perform(post("/api/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.version").exists());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void findAll_isAllowedForNonAdminAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/deployments")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void create_returnsForbidden_whenCallerLacksAdminRole() throws Exception {
        String body = """
                {"applicationId":%d,"environmentId":%d,"version":"1.0.0"}
                """.formatted(applicationId, environmentId);

        mockMvc.perform(post("/api/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void findById_returnsNotFound_whenDeploymentDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/deployments/{id}", 999_999L))
                .andExpect(status().isNotFound());
    }
}
