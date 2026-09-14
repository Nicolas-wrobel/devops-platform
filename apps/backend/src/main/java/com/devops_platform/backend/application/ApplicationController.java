package com.devops_platform.backend.application;

import com.devops_platform.backend.application.dto.ApplicationRequest;
import com.devops_platform.backend.application.dto.ApplicationResponse;
import com.devops_platform.backend.environment.dto.EnvironmentResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService service;
    private final ApplicationEnvironmentService environmentService;

    public ApplicationController(ApplicationService service, ApplicationEnvironmentService environmentService) {
        this.service = service;
        this.environmentService = environmentService;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> create(@Valid @RequestBody ApplicationRequest request) {
        ApplicationResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/applications/" + created.id())).body(created);
    }

    @GetMapping
    public List<ApplicationResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ApplicationResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public ApplicationResponse update(@PathVariable Long id, @Valid @RequestBody ApplicationRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/environments")
    public List<EnvironmentResponse> findEnvironments(@PathVariable Long id) {
        return environmentService.findEnvironmentsForApplication(id);
    }

    @PostMapping("/{id}/environments/{environmentId}")
    public ResponseEntity<EnvironmentResponse> linkEnvironment(
            @PathVariable Long id, @PathVariable Long environmentId) {
        EnvironmentResponse linked = environmentService.link(id, environmentId);
        return ResponseEntity.created(
                URI.create("/api/applications/" + id + "/environments/" + environmentId)).body(linked);
    }

    @DeleteMapping("/{id}/environments/{environmentId}")
    public ResponseEntity<Void> unlinkEnvironment(@PathVariable Long id, @PathVariable Long environmentId) {
        environmentService.unlink(id, environmentId);
        return ResponseEntity.noContent().build();
    }
}
