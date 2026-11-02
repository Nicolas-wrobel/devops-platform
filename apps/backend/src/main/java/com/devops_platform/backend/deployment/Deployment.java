package com.devops_platform.backend.deployment;

import com.devops_platform.backend.application.ApplicationEnvironment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "deployment")
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_environment_id", nullable = false)
    private ApplicationEnvironment applicationEnvironment;

    @Column(nullable = false, length = 50)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeploymentStatus status;

    @Column(name = "triggered_by", nullable = false, length = 100)
    private String triggeredBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "planned_finish_at")
    private Instant plannedFinishAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected Deployment() {
    }

    public Deployment(ApplicationEnvironment applicationEnvironment, String version, String triggeredBy) {
        this.applicationEnvironment = applicationEnvironment;
        this.version = version;
        this.triggeredBy = triggeredBy;
        this.status = DeploymentStatus.PENDING;
    }

    public void start(Instant now, Duration simulatedDuration) {
        this.startedAt = now;
        this.plannedFinishAt = now.plus(simulatedDuration);
        this.status = DeploymentStatus.RUNNING;
    }

    public void complete(DeploymentStatus finalStatus, Instant now) {
        this.status = finalStatus;
        this.finishedAt = now;
    }

    public Long getId() {
        return id;
    }

    public ApplicationEnvironment getApplicationEnvironment() {
        return applicationEnvironment;
    }

    public String getVersion() {
        return version;
    }

    public DeploymentStatus getStatus() {
        return status;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getPlannedFinishAt() {
        return plannedFinishAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
