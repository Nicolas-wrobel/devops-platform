package com.devops_platform.backend.deployment;

import com.devops_platform.backend.deployment.dto.DeploymentRequest;
import com.devops_platform.backend.deployment.dto.DeploymentResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deployments")
public class DeploymentController {

    private final DeploymentService service;

    public DeploymentController(DeploymentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DeploymentResponse> create(
            @Valid @RequestBody DeploymentRequest request, Authentication authentication) {
        DeploymentResponse created = service.create(request, authentication.getName());
        return ResponseEntity.created(URI.create("/api/deployments/" + created.id())).body(created);
    }

    @PostMapping("/{id}/start")
    public DeploymentResponse start(@PathVariable Long id) {
        return service.start(id);
    }

    @GetMapping
    public List<DeploymentResponse> findAll(
            @RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) Long environmentId) {
        return service.findAll(applicationId, environmentId);
    }

    @GetMapping("/{id}")
    public DeploymentResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }
}
