package com.example.jobs.api.job;

import com.example.jobs.domain.JobStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String type,
        Map<String, Object> payload,
        JobStatus status,
        int attempts,
        int maxAttempts,
        Instant scheduledAt,
        Instant startedAt,
        Instant completedAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt
) {

    public static JobResponse from(JobEntity job) {
        return new JobResponse(
                job.getId(),
                job.getType(),
                job.getPayload(),
                job.getStatus(),
                job.getAttempts(),
                job.getMaxAttempts(),
                job.getScheduledAt(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getLastError(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}