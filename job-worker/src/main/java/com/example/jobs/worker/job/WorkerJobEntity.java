package com.example.jobs.worker.job;

import com.example.jobs.domain.JobStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class WorkerJobEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private Instant startedAt;

    private Instant completedAt;

    private int attempts;

    private int maxAttempts;

    private String lastError;

    protected WorkerJobEntity() {
    }

    public UUID getId() {
        return id;
    }

    public JobStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void markRunning() {
        this.status = JobStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void markSucceeded() {
        this.status = JobStatus.SUCCEEDED;
        this.completedAt = Instant.now();
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public String getLastError() {
        return lastError;
    }

    public void markFailed(String errorMessage) {
        this.attempts++;
        this.lastError = errorMessage;

        if (this.attempts >= this.maxAttempts) {
            this.status = JobStatus.DEAD_LETTERED;
            this.completedAt = Instant.now();
        } else {
            this.status = JobStatus.FAILED;
        }
    }
    
    public boolean canRetry() {
        return this.attempts < this.maxAttempts;
    }
}