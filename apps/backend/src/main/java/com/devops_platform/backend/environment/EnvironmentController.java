package com.devops_platform.backend.environment;

import com.devops_platform.backend.environment.dto.EnvironmentRequest;
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
@RequestMapping("/api/environments")
public class EnvironmentController {

    private final EnvironmentService service;

    public EnvironmentController(EnvironmentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<EnvironmentResponse> create(@Valid @RequestBody EnvironmentRequest request) {
        EnvironmentResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/environments/" + created.id())).body(created);
    }

    @GetMapping
    public List<EnvironmentResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public EnvironmentResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public EnvironmentResponse update(@PathVariable Long id, @Valid @RequestBody EnvironmentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
